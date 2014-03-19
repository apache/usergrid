package org.apache.usergrid.persistence.graph.impl;


import java.util.Iterator;
import java.util.UUID;

import org.apache.usergrid.persistence.collection.OrganizationScope;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.EdgeManager;
import org.apache.usergrid.persistence.graph.EdgeManagerFactory;
import org.apache.usergrid.persistence.graph.GraphFig;
import org.apache.usergrid.persistence.graph.MarkedEdge;
import org.apache.usergrid.persistence.graph.consistency.AsyncProcessor;
import org.apache.usergrid.persistence.graph.consistency.MessageListener;
import org.apache.usergrid.persistence.graph.guice.EdgeDelete;
import org.apache.usergrid.persistence.graph.serialization.EdgeMetadataSerialization;
import org.apache.usergrid.persistence.graph.serialization.EdgeSerialization;
import org.apache.usergrid.persistence.graph.serialization.impl.parse.ObservableIterator;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func5;


/**
 * Construct the asynchronous delete operation from the listener
 */
@Singleton
public class EdgeDeleteListener implements MessageListener<EdgeEvent<Edge>, EdgeEvent<Edge>> {


    private final EdgeSerialization edgeSerialization;
    private final EdgeMetadataSerialization edgeMetadataSerialization;
    private final EdgeManagerFactory edgeManagerFactory;
    private final Keyspace keyspace;
    private final GraphFig graphFig;


    @Inject
    public EdgeDeleteListener( final EdgeSerialization edgeSerialization,
                               final EdgeMetadataSerialization edgeMetadataSerialization,
                               final EdgeManagerFactory edgeManagerFactory, final Keyspace keyspace,
                               final GraphFig graphFig,
                               @EdgeDelete final AsyncProcessor edgeDelete ) {
        this.edgeSerialization = edgeSerialization;
        this.edgeMetadataSerialization = edgeMetadataSerialization;
        this.edgeManagerFactory = edgeManagerFactory;
        this.keyspace = keyspace;
        this.graphFig = graphFig;

        edgeDelete.addListener( this );
    }


    @Override
    public Observable<EdgeEvent<Edge>> receive( final EdgeEvent<Edge> delete ) {

        final Edge edge = delete.getData();
        final OrganizationScope scope = delete.getOrganizationScope();
        final UUID maxVersion = edge.getVersion();
        final EdgeManager edgeManager = edgeManagerFactory.createEdgeManager( scope );


        return Observable.from( edge ).flatMap( new Func1<Edge, Observable<MutationBatch>>() {
            @Override
            public Observable<MutationBatch> call( final Edge edge ) {

                final MutationBatch batch = keyspace.prepareMutationBatch();


                //go through every version of this edge <= the current version and remove it
                Observable<MarkedEdge> edges = Observable.create( new ObservableIterator<MarkedEdge>( ) {
                    @Override
                    protected Iterator<MarkedEdge> getIterator() {
                        return edgeSerialization.getEdgeToTarget( scope,
                                new SimpleSearchByEdge( edge.getSourceNode(), edge.getType(), edge.getTargetNode(),
                                        edge.getVersion(), null ) );
                    }
                } ).doOnNext( new Action1<MarkedEdge>() {
                    @Override
                    public void call( final MarkedEdge markedEdge ) {
                        final MutationBatch delete = edgeSerialization.deleteEdge( scope, markedEdge );
                        batch.mergeShallow( delete );
                    }
                } );


                //search by edge type and target type.  If any other edges with this target type exist,
                // we can't delete it
                Observable<Integer> sourceIdType = edgeManager.loadEdgesFromSourceByType(
                        new SimpleSearchByIdType( edge.getSourceNode(), edge.getType(), maxVersion,
                                edge.getTargetNode().getType(), null ) ).take( 2 ).count()
                                                              .doOnNext( new Action1<Integer>() {
                                                                  @Override
                                                                  public void call( final Integer count ) {
                                                                      //There's nothing to do,
                                                                      // we have 2 different edges with the
                                                                      // same edge type and
                                                                      // target type.  Don't delete meta data
                                                                      if ( count == 1 ) {
                                                                          final MutationBatch delete =
                                                                                  edgeMetadataSerialization
                                                                                          .removeEdgeTypeFromSource(
                                                                                                  scope, edge );
                                                                          batch.mergeShallow( delete );
                                                                      }
                                                                  }
                                                              } );


                Observable<Integer> targetIdType = edgeManager.loadEdgesToTargetByType(
                        new SimpleSearchByIdType( edge.getTargetNode(), edge.getType(), maxVersion,
                                edge.getSourceNode().getType(), null ) ).take( 2 ).count()
                                                              .doOnNext( new Action1<Integer>() {
                                                                  @Override
                                                                  public void call( final Integer count ) {
                                                                      //There's nothing to do,
                                                                      // we have 2 different edges with the
                                                                      // same edge type and
                                                                      // target type.  Don't delete meta data
                                                                      if ( count == 1 ) {
                                                                          final MutationBatch delete =
                                                                                  edgeMetadataSerialization
                                                                                          .removeEdgeTypeToTarget(
                                                                                                  scope, edge );
                                                                          batch.mergeShallow( delete );
                                                                      }
                                                                  }
                                                              } );


                //search by edge type and target type.  If any other edges with this target type exist,
                // we can't delete it
                Observable<Integer> sourceType = edgeManager.loadEdgesFromSource(
                        new SimpleSearchByEdgeType( edge.getSourceNode(), edge.getType(), maxVersion, null ) ).take( 2 )
                                                            .count().doOnNext( new Action1<Integer>() {
                            @Override
                            public void call( final Integer count ) {
                                //There's nothing to do,
                                // we have 2 different edges with the
                                // same edge type and
                                // target type.  Don't delete meta data
                                if ( count == 1 ) {
                                    final MutationBatch delete =
                                            edgeMetadataSerialization.removeEdgeTypeFromSource( scope, edge );
                                    batch.mergeShallow( delete );
                                }
                            }
                        } );


                Observable<Integer> targetType = edgeManager.loadEdgesToTarget(
                        new SimpleSearchByEdgeType( edge.getTargetNode(), edge.getType(), maxVersion, null ) ).take( 2 )
                                                            .count().doOnNext( new Action1<Integer>() {
                            @Override
                            public void call( final Integer count ) {
                                //There's nothing to do,
                                // we have 2 different edges with the
                                // same edge type and
                                // target type.  Don't delete meta data
                                if ( count == 1 ) {
                                    final MutationBatch delete =
                                            edgeMetadataSerialization.removeEdgeTypeToTarget( scope, edge );
                                    batch.mergeShallow( delete );
                                }
                            }
                        } );


                //no op, just wait for each observable to populate the mutation before returning it
                return Observable.zip( edges, sourceIdType, targetIdType, sourceType, targetType,
                        new Func5<MarkedEdge, Integer, Integer, Integer, Integer, MutationBatch>() {
                            @Override
                            public MutationBatch call( final MarkedEdge markedEdge, final Integer integer,
                                                       final Integer integer2, final Integer integer3,
                                                       final Integer integer4 ) {
                                return batch;
                            }
                        } );
            }
        }


                                              ).map( new Func1<MutationBatch, EdgeEvent<Edge>>() {
            @Override
            public EdgeEvent<Edge> call( final MutationBatch mutationBatch ) {
                try {
                    mutationBatch.execute();
                }
                catch ( ConnectionException e ) {
                    throw new RuntimeException( "Unable to execute mutation", e );
                }

                return delete;
            }
        } );
    }
}
