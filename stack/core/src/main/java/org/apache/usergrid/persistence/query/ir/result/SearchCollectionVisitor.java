package org.apache.usergrid.persistence.query.ir.result;


import java.util.UUID;

import org.apache.usergrid.persistence.EntityRef;
import org.apache.usergrid.persistence.IndexBucketLocator;
import org.apache.usergrid.persistence.cassandra.CassandraService;
import org.apache.usergrid.persistence.cassandra.QueryProcessor;
import org.apache.usergrid.persistence.cassandra.index.IndexBucketScanner;
import org.apache.usergrid.persistence.cassandra.index.IndexScanner;
import org.apache.usergrid.persistence.cassandra.index.NoOpIndexScanner;
import org.apache.usergrid.persistence.geo.CollectionGeoSearch;
import org.apache.usergrid.persistence.geo.model.Point;
import org.apache.usergrid.persistence.query.ir.AllNode;
import org.apache.usergrid.persistence.query.ir.NameIdentifierNode;
import org.apache.usergrid.persistence.query.ir.QueryNode;
import org.apache.usergrid.persistence.query.ir.QuerySlice;
import org.apache.usergrid.persistence.query.ir.SearchVisitor;
import org.apache.usergrid.persistence.query.ir.WithinNode;
import org.apache.usergrid.persistence.schema.CollectionInfo;

import me.prettyprint.hector.api.beans.DynamicComposite;

import static org.apache.usergrid.persistence.Schema.DICTIONARY_COLLECTIONS;
import static org.apache.usergrid.persistence.cassandra.ApplicationCF.ENTITY_INDEX;
import static org.apache.usergrid.persistence.cassandra.CassandraPersistenceUtils.key;


/**
 * Simple search visitor that performs all the joining
 *
 * @author tnine
 */
public class SearchCollectionVisitor extends SearchVisitor {


    private static final UUIDIndexSliceParser UUID_PARSER = new UUIDIndexSliceParser();


    private final CollectionInfo collection;


    /**
     * @param queryProcessor
     */
    public SearchCollectionVisitor( final CassandraService cassandraService,
                                    final IndexBucketLocator indexBucketLocator, final QueryProcessor queryProcessor,
                                    final UUID applicationId, final EntityRef headEntity, final String bucket ) {
        super( cassandraService, indexBucketLocator, applicationId, headEntity, queryProcessor, bucket );
        this.collection = queryProcessor.getCollectionInfo();
    }


    /* (non-Javadoc)
 * @see org.apache.usergrid.persistence.query.ir.SearchVisitor#secondaryIndexScan(org.apache.usergrid.persistence
 * .query.ir
 * .QueryNode, org.apache.usergrid.persistence.query.ir.QuerySlice)
 */
    @Override
    protected IndexScanner secondaryIndexScan( QueryNode node, QuerySlice slice ) throws Exception {
        // NOTE we explicitly do not append the slice value here. This
        // is done in the searchIndex method below
        Object indexKey = key( headEntity.getUuid(), collection.getName() );

        // update the cursor and order before we perform the slice
        // operation. Should be done after subkeying since this can
        // change the hash value of the slice
        queryProcessor.applyCursorAndSort( slice );

        IndexScanner columns = null;

        // nothing left to search for this range
        if ( slice.isComplete() ) {
            columns = new NoOpIndexScanner();
        }
        // perform the search
        else {
            columns =
                    searchIndexBuckets( indexKey, slice, collection.getName(), queryProcessor.getPageSizeHint( node ) );
        }

        return columns;
    }


    public void visit( AllNode node ) throws Exception {

        String collectionName = collection.getName();

        QuerySlice slice = node.getSlice();

        queryProcessor.applyCursorAndSort( slice );

        UUID startId = null;

        if ( slice.hasCursor() ) {
            startId = UUID_PARSER.parse( slice.getCursor() ).getUUID();
        }


        IndexScanner indexScanner = cassandraService
                .getIdList( cassandraService.getApplicationKeyspace( applicationId ),
                        key( headEntity.getUuid(), DICTIONARY_COLLECTIONS, collectionName ), startId, null,
                        queryProcessor.getPageSizeHint( node ), query.isReversed(), indexBucketLocator, applicationId,
                        collectionName, node.isForceKeepFirst() );

        this.results.push( new SliceIterator( slice, indexScanner, UUID_PARSER ) );
    }


    /*
 * (non-Javadoc)
 *
 * @see org.apache.usergrid.persistence.query.ir.NodeVisitor#visit(org.apache.usergrid.
 * persistence.query.ir.WithinNode)
 */
    @Override
    public void visit( WithinNode node ) throws Exception {

        QuerySlice slice = node.getSlice();

        queryProcessor.applyCursorAndSort( slice );

        GeoIterator itr = new GeoIterator(
                new CollectionGeoSearch( em, indexBucketLocator, cassandraService, headEntity, collection.getName() ),
                query.getLimit(), slice, node.getPropertyName(), new Point( node.getLattitude(), node.getLongitude() ),
                node.getDistance() );

        results.push( itr );
    }


    @Override
    public void visit( NameIdentifierNode nameIdentifierNode ) throws Exception {
        EntityRef ref = em.getAlias( headEntity.getUuid(), collection.getType(), nameIdentifierNode.getName() );

        if ( ref == null ) {
            this.results.push( new EmptyIterator() );
            return;
        }

        this.results.push( new StaticIdIterator( ref.getUuid() ) );
    }


    /**
     * Search the collection index using all the buckets for the given collection
     *
     * @param indexKey The index key to read
     * @param slice Slice set in the query
     * @param collectionName The name of the collection to search
     * @param pageSize The page size to load when iterating
     */
    private IndexScanner searchIndexBuckets( Object indexKey, QuerySlice slice, String collectionName, int pageSize )
            throws Exception {

        DynamicComposite[] range = slice.getRange();

        Object keyPrefix = key( indexKey, slice.getPropertyName() );

        IndexScanner scanner =
                new IndexBucketScanner( cassandraService, ENTITY_INDEX, applicationId, keyPrefix, bucket, range[0],
                        range[1], slice.isReversed(), pageSize, slice.hasCursor() );

        return scanner;
    }
}
