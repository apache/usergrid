package org.usergrid.persistence.query.ir;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import me.prettyprint.hector.api.beans.DynamicComposite;

import org.usergrid.persistence.EntityManager;
import org.usergrid.persistence.Query;
import org.usergrid.persistence.cassandra.QueryProcessor;
import org.usergrid.persistence.cassandra.RelationManagerImpl;
import org.usergrid.persistence.cassandra.index.IndexScanner;
import org.usergrid.persistence.cassandra.index.NoOpIndexScanner;
import org.usergrid.persistence.query.ir.result.*;

/**
 * Simple search visitor that performs all the joining in memory for results.
 * 
 * Subclasses will want to implement visiting SliceNode and WithinNode to
 * actually perform the search on the Cassandra indexes. This class can perform
 * joins on all index entries that conform to the Results object
 * 
 * @author tnine
 * 
 */
public abstract class SearchVisitor implements NodeVisitor {

  private static final CollectionIndexSliceParser COLLECTION_PARSER = new CollectionIndexSliceParser();

  protected final Query query;

  protected final QueryProcessor queryProcessor;

  protected final EntityManager em;

  protected final Stack<ResultIterator> results = new Stack<ResultIterator>();

  /**
   * @param query
   */
  public SearchVisitor(Query query, QueryProcessor queryProcessor, EntityManager em) {
    this.query = query;
    this.queryProcessor = queryProcessor;
    this.em = em;
  }

  /**
   * Return the results if they exist, null otherwise
   * 
   * @return
   */
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
  public void visit(AndNode node) throws Exception {
    node.getLeft().visit(this);
    node.getRight().visit(this);

    ResultIterator right = results.pop();
    ResultIterator left = results.pop();

    /**
     * NOTE: TN We should always maintain post order traversal of the tree. It
     * is required for sorting to work correctly
     */
    IntersectionIterator intersection = new IntersectionIterator(queryProcessor.getPageSizeHint(node));
    intersection.addIterator(left);
    intersection.addIterator(right);

    results.push(intersection);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.usergrid.persistence.query.ir.NodeVisitor#visit(org.usergrid.
   * persistence.query.ir.NotNode)
   */
  @Override
  public void visit(NotNode node) throws Exception {
    node.getSubtractNode().visit(this);
    ResultIterator not = results.pop();

    node.getKeepNode().visit(this);
    ResultIterator keep = results.pop();

    SubtractionIterator subtraction = new SubtractionIterator(queryProcessor.getPageSizeHint(node));
    subtraction.setSubtractIterator(not);
    subtraction.setKeepIterator(keep);

    results.push(subtraction);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.usergrid.persistence.query.ir.NodeVisitor#visit(org.usergrid.
   * persistence.query.ir.OrNode)
   */
  @Override
  public void visit(OrNode node) throws Exception {
    node.getLeft().visit(this);
    node.getRight().visit(this);

    ResultIterator right = results.pop();
    ResultIterator left = results.pop();

    UnionIterator union = new UnionIterator(queryProcessor.getPageSizeHint(node));

    if (left != null) {
      union.addIterator(left);
    }
    if (right != null) {
      union.addIterator(right);
    }

    results.push(union);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.usergrid.persistence.query.ir.NodeVisitor#visit(org.usergrid.persistence
   * .query.ir.OrderByNode)
   */
  @Override
  public void visit(OrderByNode orderByNode) throws Exception {

    QuerySlice slice = orderByNode.getFirstPredicate().getAllSlices().iterator().next();

    queryProcessor.applyCursorAndSort(slice);

    IndexScanner scanner;

    // nothing left to search for this range
    if (slice.isComplete()) {
      scanner = new NoOpIndexScanner();
    } else {
      scanner = secondaryIndexScan(orderByNode, slice);
    }

    List<Query.SortPredicate> secondarySorts = orderByNode.getSecondarySorts();

    ResultIterator orderIterator;

    if (secondarySorts == null || secondarySorts.size() == 0) {
      orderIterator = new SliceIterator<DynamicComposite>(slice, scanner, COLLECTION_PARSER, slice.hasCursor());
    } else {
      orderIterator = new OrderByIterator(slice, scanner, COLLECTION_PARSER, secondarySorts, em,
          queryProcessor.getPageSizeHint(orderByNode));
    }

    // now create our intermediate iterator with our real results

    results.push(orderIterator);

  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.usergrid.persistence.query.ir.NodeVisitor#visit(org.usergrid.persistence
   * .query.ir.SliceNode)
   */
  @Override
  public void visit(SliceNode node) throws Exception {
    IntersectionIterator intersections = new IntersectionIterator(queryProcessor.getPageSizeHint(node));

    for (QuerySlice slice : node.getAllSlices()) {
      IndexScanner scanner = secondaryIndexScan(node, slice);

      intersections.addIterator(new SliceIterator<DynamicComposite>(slice, scanner, COLLECTION_PARSER, slice
          .hasCursor()));
    }

    results.push(intersections);

  }

  /**
   * Create a secondary index scan for the given slice node. DOES NOT apply to
   * the "all" case. This should only generate a slice for secondary property
   * scanning
   * 
   * @param node
   * @return
   * @throws Exception
   */
  protected abstract IndexScanner secondaryIndexScan(QueryNode node, QuerySlice slice) throws Exception;
  
  

}
