package org.apache.usergrid.persistence.graph.impl;


import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.cassandra.thrift.Mutation;

import org.apache.usergrid.persistence.collection.OrganizationScope;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.GraphFig;
import org.apache.usergrid.persistence.graph.MarkedEdge;
import org.apache.usergrid.persistence.graph.SearchByEdgeType;
import org.apache.usergrid.persistence.graph.SearchEdgeType;
import org.apache.usergrid.persistence.graph.consistency.AsyncProcessor;
import org.apache.usergrid.persistence.graph.consistency.MessageListener;
import org.apache.usergrid.persistence.graph.guice.NodeDelete;
import org.apache.usergrid.persistence.graph.impl.stage.EdgeDeleteRepair;
import org.apache.usergrid.persistence.graph.impl.stage.EdgeMetaRepair;
import org.apache.usergrid.persistence.graph.serialization.EdgeMetadataSerialization;
import org.apache.usergrid.persistence.graph.serialization.EdgeSerialization;
import org.apache.usergrid.persistence.graph.serialization.NodeSerialization;
import org.apache.usergrid.persistence.graph.serialization.impl.parse.ObservableIterator;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import rx.Observable;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;


/**
 * Construct the asynchronous node delete from the q
 */
public class NodeDeleteListener implements MessageListener<EdgeEvent<Id>, Integer> {


    private static final Logger LOG = LoggerFactory.getLogger( NodeDeleteListener.class );

    private final NodeSerialization nodeSerialization;
    private final EdgeSerialization edgeSerialization;
    private final EdgeMetadataSerialization edgeMetadataSerialization;
    private final EdgeDeleteRepair edgeDeleteRepair;
    private final EdgeMetaRepair edgeMetaRepair;
    private final GraphFig graphFig;
    protected final Keyspace keyspace;


    /**
     * Wire the serialization dependencies
     */
    @Inject
    public NodeDeleteListener( final NodeSerialization nodeSerialization, final EdgeSerialization edgeSerialization,

                               final EdgeMetadataSerialization edgeMetadataSerialization,
                               final EdgeDeleteRepair edgeDeleteRepair, final EdgeMetaRepair edgeMetaRepair,
                               final GraphFig graphFig, @NodeDelete final AsyncProcessor nodeDelete,
                               final Keyspace keyspace ) {


        this.nodeSerialization = nodeSerialization;
        this.edgeSerialization = edgeSerialization;
        this.edgeMetadataSerialization = edgeMetadataSerialization;
        this.edgeDeleteRepair = edgeDeleteRepair;
        this.edgeMetaRepair = edgeMetaRepair;
        this.graphFig = graphFig;
        this.keyspace = keyspace;

        nodeDelete.addListener( this );
    }


    /**
     * Removes this node from the graph.
     *
     * @param edgeEvent The edge event that was fired.
     *
     * @return An observable that emits the total number of edges that have been removed with this node both as the
     *         target and source
     */
    @Override
    public Observable<Integer> receive( final EdgeEvent<Id> edgeEvent ) {

        final Id node = edgeEvent.getData();
        final OrganizationScope scope = edgeEvent.getOrganizationScope();
        final UUID version = edgeEvent.getVersion();


        return Observable.from( node ).subscribeOn( Schedulers.io() ).map( new Func1<Id, Optional<UUID>>() {
            @Override
            public Optional<UUID> call( final Id id ) {
                return nodeSerialization.getMaxVersion( scope, node );
            }
        } ) //only continue if it's present.  Otherwise stop
                .takeWhile( new Func1<Optional<UUID>, Boolean>() {

                    @Override
                    public Boolean call( final Optional<UUID> uuidOptional ) {

                        LOG.debug( "Node with id {} has max version of {}", node, uuidOptional.orNull() );

                        return uuidOptional.isPresent();
                    }
                } )

                        //delete source and targets in parallel and merge them into a single observable
                .flatMap( new Func1<Optional<UUID>, Observable<MarkedEdge>>() {
                    @Override
                    public Observable<MarkedEdge> call( final Optional<UUID> uuidOptional ) {

                        //get all edges pointing to the target node and buffer then into groups for deletion
                        Observable<MarkedEdge> targetEdges =
                                getEdgesTypesToTarget( scope, new SimpleSearchEdgeType( node, null ) )
                                        .flatMap( new Func1<String, Observable<MarkedEdge>>() {
                                            @Override
                                            public Observable<MarkedEdge> call( final String edgeType ) {
                                                return loadEdgesToTarget( scope,
                                                        new SimpleSearchByEdgeType( node, edgeType, version, null ) );
                                            }
                                        } );


                        //get all edges pointing to the source node and buffer them into groups for deletion
                        Observable<MarkedEdge> sourceEdges =
                                getEdgesTypesFromSource( scope, new SimpleSearchEdgeType( node, null ) )
                                        .flatMap( new Func1<String, Observable<MarkedEdge>>() {
                                            @Override
                                            public Observable<MarkedEdge> call( final String edgeType ) {
                                                return loadEdgesFromSource( scope,
                                                        new SimpleSearchByEdgeType( node, edgeType, version, null ) );
                                            }
                                        } );


                        //each time an edge is emitted, delete it via batch mutation since we'll already be buffered
                        return Observable.merge( targetEdges, sourceEdges );
                    }
                } )
                //buffer and delete marked edges in our buffer size
                .buffer( graphFig.getScanPageSize() ).flatMap(
                        new Func1<List<MarkedEdge>, Observable<MarkedEdge>>() {
                            @Override
                            public Observable<MarkedEdge> call( final List<MarkedEdge> markedEdges ) {

                                LOG.debug( "Batching {} edges for deletion" , markedEdges.size());

                                final MutationBatch batch = keyspace.prepareMutationBatch();

                                for(MarkedEdge edge: markedEdges){

                                    //delete the newest edge <= the version on the node delete
                                    LOG.debug( "Deleting edge {}", edge );
                                    final MutationBatch delete = edgeSerialization.deleteEdge( scope,  edge );

                                    batch.mergeShallow( delete );
                                }

                                try {
                                    batch.execute();
                                }
                                catch ( ConnectionException e ) {
                                    throw new RuntimeException( "Unable to delete edges", e );
                                }

                                return Observable.from(markedEdges);
                            }
                        } )
                        //TODO Fix this
//        .flatMap( new Func1<MarkedEdge, Observable<MarkedEdge>>() {
//            @Override
//            public Observable<MarkedEdge> call( final MarkedEdge edge ) {
//
//
//                return Observable.just( edge );




//                //delete both the source and target meta data in parallel for the edge we deleted in the previous step
//                //if nothing else is using them
//                Observable<Integer> sourceMetaRepaired =
//                        edgeMetaRepair.repairSources( scope, edge.getSourceNode(), edge.getType(), version );
//
//                Observable<Integer> targetMetaRepaired =
//                        edgeMetaRepair.repairTargets( scope, edge.getTargetNode(), edge.getType(), version );
//
//                //sum up the number of subtypes we retain
//                return Observable.concat( sourceMetaRepaired, targetMetaRepaired ).last()
//                                 .map( new Func1<Integer, MarkedEdge>() {
//                                     @Override
//                                     public MarkedEdge call( final Integer integer ) {
//
//                                         LOG.debug( "Retained {} subtypes for edge {}", integer, edge );
//
//                                         return edge;
//                                     }
//                                 } );


//            }
//        })

    .count()
                //if nothing is ever emitted, emit 0 so that we know no operations took place. Finally remove the
                // target node in the mark
                .defaultIfEmpty( 0 ).doOnCompleted( new Action0() {
                    @Override
                    public void call() {
                        try {
                            nodeSerialization.delete( scope, node, version ).execute();
                        }
                        catch ( ConnectionException e ) {
                            throw new RuntimeException( "Unable to delete marked graph node " + node, e );
                        }
                    }
                } );
    }


    /**
     * Get all existing edge types to the target node
     */
    private Observable<String> getEdgesTypesToTarget( final OrganizationScope scope, final SearchEdgeType search ) {

        return Observable.create( new ObservableIterator<String>(  ) {
            @Override
            protected Iterator<String> getIterator() {
                return edgeMetadataSerialization.getEdgeTypesToTarget( scope, search );
            }
        } ).subscribeOn( Schedulers.io() );
    }


    /**
     * Get all existing edge types to the target node
     */
    private Observable<String> getEdgesTypesFromSource( final OrganizationScope scope, final SearchEdgeType search ) {

        return Observable.create( new ObservableIterator<String>(  ) {
            @Override
            protected Iterator<String> getIterator() {
                return edgeMetadataSerialization.getEdgeTypesFromSource( scope, search );
            }
        } ).subscribeOn( Schedulers.io() );
    }


    /**
     * Load all edges pointing to this target
     */
    private Observable<MarkedEdge> loadEdgesToTarget( final OrganizationScope scope, final SearchByEdgeType search ) {

        return Observable.create( new ObservableIterator<MarkedEdge>(  ) {
            @Override
            protected Iterator<MarkedEdge> getIterator() {
                return edgeSerialization.getEdgesToTarget( scope, search );
            }
        } ).subscribeOn( Schedulers.io() );
    }


    /**
     * Load all edges pointing to this target
     */
    private Observable<MarkedEdge> loadEdgesFromSource( final OrganizationScope scope, final SearchByEdgeType search ) {

        return Observable.create( new ObservableIterator<MarkedEdge>(  ) {
            @Override
            protected Iterator<MarkedEdge> getIterator() {
                return edgeSerialization.getEdgesFromSource( scope, search );
            }
        } ).subscribeOn( Schedulers.io() );
    }
}
