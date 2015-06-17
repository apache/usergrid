package org.apache.usergrid.persistence.query.ir.result;


import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Iterator;
import java.util.UUID;

import org.apache.usergrid.persistence.ConnectedEntityRef;
import org.apache.usergrid.persistence.EntityRef;
import org.apache.usergrid.persistence.IndexBucketLocator;
import org.apache.usergrid.persistence.cassandra.CassandraService;
import org.apache.usergrid.persistence.cassandra.ConnectionRefImpl;
import org.apache.usergrid.persistence.cassandra.QueryProcessor;
import org.apache.usergrid.persistence.cassandra.index.ConnectedIndexScanner;
import org.apache.usergrid.persistence.cassandra.index.IndexBucketScanner;
import org.apache.usergrid.persistence.cassandra.index.IndexScanner;
import org.apache.usergrid.persistence.cassandra.index.NoOpIndexScanner;
import org.apache.usergrid.persistence.geo.ConnectionGeoSearch;
import org.apache.usergrid.persistence.geo.model.Point;
import org.apache.usergrid.persistence.query.ir.AllNode;
import org.apache.usergrid.persistence.query.ir.NameIdentifierNode;
import org.apache.usergrid.persistence.query.ir.QueryNode;
import org.apache.usergrid.persistence.query.ir.QuerySlice;
import org.apache.usergrid.persistence.query.ir.SearchVisitor;
import org.apache.usergrid.persistence.query.ir.WithinNode;

import me.prettyprint.hector.api.beans.DynamicComposite;

import static org.apache.usergrid.persistence.Schema.DICTIONARY_CONNECTED_ENTITIES;
import static org.apache.usergrid.persistence.Schema.DICTIONARY_CONNECTING_ENTITIES;
import static org.apache.usergrid.persistence.Schema.INDEX_CONNECTIONS;
import static org.apache.usergrid.persistence.cassandra.ApplicationCF.ENTITY_INDEX;
import static org.apache.usergrid.persistence.cassandra.CassandraPersistenceUtils.key;


/**
 * Simple search visitor that performs all the joining
 *
 * @author tnine
 */
public class SearchConnectionVisitor extends SearchVisitor {

    private final ConnectionRefImpl connection;

    /** True if we should search from source->target edges.  False if we should search from target<-source edges */
    private final boolean outgoing;



    /**
     * @param queryProcessor They query processor to use
     * @param applicationId
     * @param connection The connection refernce
     * @param outgoing The direction to search.  True if we should search from source->target edges.  False if we
     */
    public SearchConnectionVisitor( final CassandraService cassandraService,
                                    final IndexBucketLocator indexBucketLocator, final QueryProcessor queryProcessor,
                                    final UUID applicationId, final EntityRef headEntity, ConnectionRefImpl connection,
                                    boolean outgoing, final String bucket ) {
        super( cassandraService, indexBucketLocator, applicationId, headEntity, queryProcessor, bucket );
        this.connection = connection;
        this.outgoing = outgoing;
    }


    /* (non-Javadoc)
 * @see org.apache.usergrid.persistence.query.ir.SearchVisitor#secondaryIndexScan(org.apache.usergrid.persistence
 * .query.ir
 * .QueryNode, org.apache.usergrid.persistence.query.ir.QuerySlice)
 */
    @Override
    protected IndexScanner secondaryIndexScan( QueryNode node, QuerySlice slice ) throws Exception {

        UUID
                id = ConnectionRefImpl.getIndexId( ConnectionRefImpl.BY_CONNECTION_AND_ENTITY_TYPE, headEntity,
                connection.getConnectionType(), connection.getConnectedEntityType(), new ConnectedEntityRef[0] );

        Object key = key( id, INDEX_CONNECTIONS );

        // update the cursor and order before we perform the slice
        // operation
        queryProcessor.applyCursorAndSort( slice );

        IndexScanner columns = null;

        if ( slice.isComplete() ) {
            columns = new NoOpIndexScanner();
        }
        else {
            columns = searchIndex( key, slice, queryProcessor.getPageSizeHint( node ), bucket );
        }

        return columns;
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

        GeoIterator itr =
                new GeoIterator( new ConnectionGeoSearch( em, indexBucketLocator, cassandraService, connection.getIndexId() ),
                        query.getLimit(), slice, node.getPropertyName(),
                        new Point( node.getLattitude(), node.getLongitude() ), node.getDistance() );

        results.push( itr );
    }


    @Override
    public void visit( AllNode node ) throws Exception {
        QuerySlice slice = node.getSlice();

        queryProcessor.applyCursorAndSort( slice );

        int size = queryProcessor.getPageSizeHint( node );

        ByteBuffer start = null;

        if ( slice.hasCursor() ) {
            start = slice.getCursor();
        }


        boolean skipFirst = node.isForceKeepFirst() ? false : slice.hasCursor();

        UUID entityIdToUse;

        //change our type depending on which direction we're loading
        String dictionaryType;

        //the target type
        String targetType;

        //this is on the "source" side of the edge
        if ( outgoing ) {
            entityIdToUse = connection.getConnectingEntityId();
            dictionaryType = DICTIONARY_CONNECTED_ENTITIES;
            targetType = connection.getConnectedEntityType();
        }

        //we're on the target side of the edge
        else {
            entityIdToUse = connection.getConnectedEntityId();
            dictionaryType = DICTIONARY_CONNECTING_ENTITIES;
            targetType = connection.getConnectingEntityType();
        }

        final String connectionType = connection.getConnectionType();


        final ConnectionIndexSliceParser connectionParser = new ConnectionIndexSliceParser( targetType );


        final Iterator<String> connectionTypes;

        //use the provided connection type
        if ( connectionType != null ) {
            connectionTypes = Collections.singleton( connectionType ).iterator();
        }

        //we need to iterate all connection types
        else {
            connectionTypes = new ConnectionTypesIterator( cassandraService, applicationId, entityIdToUse, outgoing, size );
        }

        IndexScanner connectionScanner =
                new ConnectedIndexScanner( cassandraService, dictionaryType, applicationId, entityIdToUse, connectionTypes,
                        start, slice.isReversed(), size, skipFirst );

        this.results.push( new SliceIterator( slice, connectionScanner, connectionParser ) );
    }


    @Override
    public void visit( NameIdentifierNode nameIdentifierNode ) throws Exception {
        //TODO T.N. USERGRID-1919 actually validate this is connected
        EntityRef ref =
                em.getAlias( applicationId, connection.getConnectedEntityType(), nameIdentifierNode.getName() );

        if ( ref == null ) {
            this.results.push( new EmptyIterator() );
            return;
        }

        this.results.push( new StaticIdIterator( ref.getUuid() ) );
    }

    private IndexScanner searchIndex( Object indexKey, QuerySlice slice, int pageSize, final String shardBucket ) throws Exception {

          DynamicComposite[] range = slice.getRange();

          Object keyPrefix = key( indexKey, slice.getPropertyName() );



          IndexScanner scanner =
                  new IndexBucketScanner( cassandraService, ENTITY_INDEX, applicationId,
                          keyPrefix, shardBucket, range[0], range[1], slice.isReversed(), pageSize, slice.hasCursor());

          return scanner;
      }
}
