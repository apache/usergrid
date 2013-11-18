package org.usergrid.persistence.query.ir;


import java.util.Stack;

import org.usergrid.persistence.EntityManager;
import org.usergrid.persistence.EntityRef;
import org.usergrid.persistence.Query;
import org.usergrid.persistence.cassandra.QueryProcessor;
import org.usergrid.persistence.cassandra.index.IndexScanner;
import org.usergrid.persistence.cassandra.index.NoOpIndexScanner;
import org.usergrid.persistence.query.ir.result.EmptyIterator;
import org.usergrid.persistence.query.ir.result.IntersectionIterator;
import org.usergrid.persistence.query.ir.result.OrderByIterator;
import org.usergrid.persistence.query.ir.result.ResultIterator;
import org.usergrid.persistence.query.ir.result.SecondaryIndexSliceParser;
import org.usergrid.persistence.query.ir.result.SliceIterator;
import org.usergrid.persistence.query.ir.result.StaticIdIterator;
import org.usergrid.persistence.query.ir.result.SubtractionIterator;
import org.usergrid.persistence.query.ir.result.UnionIterator;


/**
 * Simple search visitor that performs all the joining in memory for results.
 * <p/>
 * Subclasses will want to implement visiting SliceNode and WithinNode to actually perform the search on the Cassandra
 * indexes. This class can perform joins on all index entries that conform to the Results object
 *
 * @author tnine
 */
public abstract class SearchVisitor implements NodeVisitor {

    private static final SecondaryIndexSliceParser COLLECTION_PARSER = new SecondaryIndexSliceParser();

    protected final Query query;

    protected final QueryProcessor queryProcessor;

    protected final EntityManager em;

    protected final Stack<ResultIterator> results = new Stack<ResultIterator>();


    /**
     * @param queryProcessor
     */
    public SearchVisitor( QueryProcessor queryProcessor ) {
        this.query = queryProcessor.getQuery();
        this.queryProcessor = queryProcessor;
        this.em = queryProcessor.getEntityManager();
    }


    /** Return the results if they exist, null otherwise */
    public ResultIterator getResults() {
        return results.isEmpty() ? null : results.pop();
    }


    /*
     * (non-Javadoc)
     *
     * @see org.usergrid.persistence.query.ir.NodeVisitor#visit(org.usergrid.
     * persistence.query.ir.AndNode)
     */
    @Override
    public void visit( AndNode node ) throws Exception {
        node.getLeft().visit( this );
        node.getRight().visit( this );

        ResultIterator right = results.pop();
        ResultIterator left = results.pop();

        /**
         * NOTE: TN We should always maintain post order traversal of the tree. It
         * is required for sorting to work correctly
         */
        IntersectionIterator intersection = new IntersectionIterator( queryProcessor.getPageSizeHint( node ) );
        intersection.addIterator( left );
        intersection.addIterator( right );

        results.push( intersection );
    }


    /*
     * (non-Javadoc)
     *
     * @see org.usergrid.persistence.query.ir.NodeVisitor#visit(org.usergrid.
     * persistence.query.ir.NotNode)
     */
    @Override
    public void visit( NotNode node ) throws Exception {
        node.getSubtractNode().visit( this );
        ResultIterator not = results.pop();

        node.getKeepNode().visit( this );
        ResultIterator keep = results.pop();

        SubtractionIterator subtraction = new SubtractionIterator( queryProcessor.getPageSizeHint( node ) );
        subtraction.setSubtractIterator( not );
        subtraction.setKeepIterator( keep );

        results.push( subtraction );
    }


    /*
     * (non-Javadoc)
     *
     * @see org.usergrid.persistence.query.ir.NodeVisitor#visit(org.usergrid.
     * persistence.query.ir.OrNode)
     */
    @Override
    public void visit( OrNode node ) throws Exception {
        node.getLeft().visit( this );
        node.getRight().visit( this );

        ResultIterator right = results.pop();
        ResultIterator left = results.pop();

        UnionIterator union = new UnionIterator( queryProcessor.getPageSizeHint( node ) );

        if ( left != null ) {
            union.addIterator( left );
        }
        if ( right != null ) {
            union.addIterator( right );
        }

        results.push( union );
    }


    /*
     * (non-Javadoc)
     *
     * @see
     * org.usergrid.persistence.query.ir.NodeVisitor#visit(org.usergrid.persistence
     * .query.ir.OrderByNode)
     */
    @Override
    public void visit( OrderByNode orderByNode ) throws Exception {

        QuerySlice slice = orderByNode.getFirstPredicate().getAllSlices().iterator().next();

        queryProcessor.applyCursorAndSort( slice );

        QueryNode subOperations = orderByNode.getQueryOperations();

        ResultIterator subResults = null;

        if ( subOperations != null ) {
            //visit our sub operation
            subOperations.visit( this );

            subResults = results.pop();
        }

        ResultIterator orderIterator;

        /**
         * We have secondary sorts, we need to evaluate the candidate results and sort them in memory
         */
        if ( orderByNode.hasSecondarySorts() ) {

            //only order by with no query, start scanning the first field
            if ( subResults == null ) {
                QuerySlice firstFieldSlice = new QuerySlice( slice.getPropertyName(), -1 );
                subResults =
                        new SliceIterator( slice, secondaryIndexScan( orderByNode, firstFieldSlice ), COLLECTION_PARSER,
                                slice.hasCursor() );
            }

            orderIterator = new OrderByIterator( slice, orderByNode.getSecondarySorts(), subResults, em,
                    queryProcessor.getPageSizeHint( orderByNode ) );
        }

        //we don't have multi field sorting, we can simply do intersection with a single scan range
        else {

            IndexScanner scanner;

            if ( slice.isComplete() ) {
                scanner = new NoOpIndexScanner();
            }
            else {
                scanner = secondaryIndexScan( orderByNode, slice );
            }

            SliceIterator joinSlice = new SliceIterator( slice, scanner, COLLECTION_PARSER, slice.hasCursor() );

            IntersectionIterator union = new IntersectionIterator( queryProcessor.getPageSizeHint( orderByNode ) );
            union.addIterator( joinSlice );

            if ( subResults != null ) {
                union.addIterator( subResults );
            }

            orderIterator = union;
        }

        // now create our intermediate iterator with our real results
        results.push( orderIterator );
    }


    /*
     * (non-Javadoc)
     *
     * @see
     * org.usergrid.persistence.query.ir.NodeVisitor#visit(org.usergrid.persistence
     * .query.ir.SliceNode)
     */
    @Override
    public void visit( SliceNode node ) throws Exception {
        IntersectionIterator intersections = new IntersectionIterator( queryProcessor.getPageSizeHint( node ) );

        for ( QuerySlice slice : node.getAllSlices() ) {
            IndexScanner scanner = secondaryIndexScan( node, slice );

            intersections.addIterator( new SliceIterator( slice, scanner, COLLECTION_PARSER, slice.hasCursor() ) );
        }

        results.push( intersections );
    }


    /**
     * Create a secondary index scan for the given slice node. DOES NOT apply to the "all" case. This should only
     * generate a slice for secondary property scanning
     */
    protected abstract IndexScanner secondaryIndexScan( QueryNode node, QuerySlice slice ) throws Exception;


    @Override
    public void visit( UuidIdentifierNode uuidIdentifierNode ) {
        this.results.push( new StaticIdIterator( uuidIdentifierNode.getUuid() ) );
    }


    @Override
    public void visit( EmailIdentifierNode emailIdentifierNode ) throws Exception {
        EntityRef user = queryProcessor.getEntityManager().getUserByIdentifier( emailIdentifierNode.getIdentifier() );

        if ( user == null ) {
            this.results.push( new EmptyIterator() );
            return;
        }

        this.results.push( new StaticIdIterator( user.getUuid() ) );
    }
}
