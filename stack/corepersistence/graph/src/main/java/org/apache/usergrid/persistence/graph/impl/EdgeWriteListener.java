package org.apache.usergrid.persistence.graph.impl;


import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.apache.usergrid.persistence.collection.OrganizationScope;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.GraphFig;
import org.apache.usergrid.persistence.graph.MarkedEdge;
import org.apache.usergrid.persistence.graph.consistency.AsyncProcessor;
import org.apache.usergrid.persistence.graph.consistency.MessageListener;
import org.apache.usergrid.persistence.graph.guice.EdgeWrite;
import org.apache.usergrid.persistence.graph.serialization.EdgeSerialization;
import org.apache.usergrid.persistence.graph.serialization.impl.parse.ObservableIterator;

import com.fasterxml.uuid.UUIDComparator;
import com.google.inject.Singleton;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import rx.Observable;
import rx.util.functions.Func1;


/**
 * Construct the asynchronous edge lister for the repair operation.
 */
@Singleton
public class EdgeWriteListener implements MessageListener<EdgeEvent<Edge>, EdgeEvent<Edge>> {

    private final EdgeSerialization edgeSerialization;
    private final GraphFig graphFig;
    private final Keyspace keyspace;


    public EdgeWriteListener( final EdgeSerialization edgeSerialization, final GraphFig graphFig,
                              final Keyspace keyspace, @EdgeWrite final AsyncProcessor edgeWrite ) {
        this.edgeSerialization = edgeSerialization;
        this.graphFig = graphFig;
        this.keyspace = keyspace;
        edgeWrite.addListener( this );
    }


    @Override
    public Observable<EdgeEvent<Edge>> receive( final EdgeEvent<Edge> write ) {

        final Edge edge = write.getData();
        final OrganizationScope scope = write.getOrganizationScope();
        final UUID maxVersion = edge.getVersion();

        return Observable.create( new ObservableIterator<MarkedEdge>() {
            @Override
            protected Iterator<MarkedEdge> getIterator() {

                final SimpleSearchByEdge search =
                        new SimpleSearchByEdge( edge.getSourceNode(), edge.getType(), edge.getTargetNode(), maxVersion,
                                null );

                return edgeSerialization.getEdgeFromSource( scope, search );
            }
        } ).filter( new Func1<MarkedEdge, Boolean>() {

            //TODO, reuse this for delete operation


            /**
             * We only want to return edges < this version so we remove them
             * @param markedEdge
             * @return
             */
            @Override
            public Boolean call( final MarkedEdge markedEdge ) {
                return UUIDComparator.staticCompare( markedEdge.getVersion(), maxVersion ) < 0;
            }
            //buffer the deletes and issue them in a single mutation
        } ).buffer( graphFig.getScanPageSize() ).map( new Func1<List<MarkedEdge>, EdgeEvent<Edge>>() {
            @Override
            public EdgeEvent<Edge> call( final List<MarkedEdge> markedEdges ) {

                final MutationBatch batch = keyspace.prepareMutationBatch();

                for ( MarkedEdge edge : markedEdges ) {
                    final MutationBatch delete = edgeSerialization.deleteEdge( scope, edge );

                    batch.mergeShallow( delete );
                }

                try {
                    batch.execute();
                }
                catch ( ConnectionException e ) {
                    throw new RuntimeException( "Unable to issue write to cassandra", e );
                }

                return write;
            }
        } );
    }
}
