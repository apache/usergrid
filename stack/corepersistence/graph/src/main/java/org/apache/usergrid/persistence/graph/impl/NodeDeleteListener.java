package org.apache.usergrid.persistence.graph.impl;


import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.apache.usergrid.persistence.collection.OrganizationScope;
import org.apache.usergrid.persistence.graph.EdgeManagerFactory;
import org.apache.usergrid.persistence.graph.GraphFig;
import org.apache.usergrid.persistence.graph.MarkedEdge;
import org.apache.usergrid.persistence.graph.SearchByEdgeType;
import org.apache.usergrid.persistence.graph.SearchEdgeType;
import org.apache.usergrid.persistence.graph.consistency.AsyncProcessor;
import org.apache.usergrid.persistence.graph.consistency.MessageListener;
import org.apache.usergrid.persistence.graph.guice.NodeDelete;
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
import rx.Scheduler;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;


/**
 * Construct the asynchronous node delete from the q
 */
public class NodeDeleteListener implements MessageListener<EdgeEvent<Id>, EdgeEvent<Id>> {


    private final NodeSerialization nodeSerialization;
    private final EdgeSerialization edgeSerialization;
    private final EdgeMetadataSerialization edgeMetadataSerialization;
    private final GraphFig graphFig;
    private final Keyspace keyspace;

    private final Scheduler scheduler;
    private final EdgeManagerFactory edgeManagerFactory;


    /**
     * Wire the serialization dependencies
     */
    @Inject
    public NodeDeleteListener( final NodeSerialization nodeSerialization, final EdgeSerialization edgeSerialization,
                               final EdgeMetadataSerialization edgeMetadataSerialization, final GraphFig graphFig,
                               final Keyspace keyspace, final Scheduler scheduler,
                               final EdgeManagerFactory edgeManagerFactory,
                               @NodeDelete final AsyncProcessor nodeDelete ) {


        this.nodeSerialization = nodeSerialization;
        this.edgeSerialization = edgeSerialization;
        this.edgeMetadataSerialization = edgeMetadataSerialization;
        this.graphFig = graphFig;
        this.keyspace = keyspace;
        this.scheduler = scheduler;
        this.edgeManagerFactory = edgeManagerFactory;

        nodeDelete.addListener( this );
    }


    @Override
    public Observable<EdgeEvent<Id>> receive( final EdgeEvent<Id> edgeEvent ) {

        final Id node = edgeEvent.getData();
        final OrganizationScope scope = edgeEvent.getOrganizationScope();
        final UUID version = edgeEvent.getVersion();


        return Observable.from( node ).subscribeOn( scheduler ).map( new Func1<Id, Optional<UUID>>() {
            @Override
            public Optional<UUID> call( final Id id ) {
                return nodeSerialization.getMaxVersion( scope, node );
            }
        } ) //only continue if it's present.  Otherwise stop
                .takeWhile( new Func1<Optional<UUID>, Boolean>() {
                    @Override
                    public Boolean call( final Optional<UUID> uuidOptional ) {
                        return uuidOptional.isPresent();
                    }
                } )

                        //delete source and targets in parallel and merge them into a single observable
                .flatMap( new Func1<Optional<UUID>, Observable<List<MarkedEdge>>>() {
                    @Override
                    public Observable<List<MarkedEdge>> call( final Optional<UUID> uuidOptional ) {

                        //get all edges pointing to the target node and buffer then into groups for deletion
                        Observable<MarkedEdge> targetEdges =
                                getEdgesTypesToTarget( scope, new SimpleSearchEdgeType( node, null ) )
                                        .flatMap( new Func1<String, Observable<MarkedEdge>>() {
                                            @Override
                                            public Observable<MarkedEdge> call( final String edgeType ) {
                                                return loadEdgesToTarget( scope,
                                                        new SimpleSearchByEdgeType( node, edgeType, uuidOptional.get(),
                                                                null ) );
                                            }
                                        } );


                        //get all edges pointing to the source node and buffer them into groups for deletion
                        Observable<MarkedEdge> sourceEdges =
                                getEdgesTypesFromSource( scope, new SimpleSearchEdgeType( node, null ) )
                                        .flatMap( new Func1<String, Observable<MarkedEdge>>() {
                                            @Override
                                            public Observable<MarkedEdge> call( final String edgeType ) {
                                                return loadEdgesFromSource( scope,
                                                        new SimpleSearchByEdgeType( node, edgeType, uuidOptional.get(),
                                                                null ) );
                                            }
                                        } );


                        //each time an edge is emitted, delete it via batch mutation since we'll already be buffered
                        return Observable.merge( targetEdges, sourceEdges ).buffer( graphFig.getScanPageSize() )
                                         .doOnNext( new Action1<List<MarkedEdge>>() {
                                             @Override
                                             public void call( final List<MarkedEdge> markedEdges ) {
                                                 MutationBatch batch = keyspace.prepareMutationBatch();


                                                 for ( MarkedEdge marked : markedEdges ) {
                                                     batch.mergeShallow(
                                                             edgeSerialization.deleteEdge( scope, marked ) );
                                                 }

                                                 try {
                                                     batch.execute();
                                                 }
                                                 catch ( ConnectionException e ) {
                                                     throw new RuntimeException( "Unable to execute mutation" );
                                                 }
                                             }
                                         } );
                    }
                } ).last().flatMap( new Func1<List<MarkedEdge>, Observable<String>>() {
                    @Override
                    public Observable<String> call( final List<MarkedEdge> markedEdges ) {

                        //delete all meta for edges to this node
                        Observable<String> targets =
                                getEdgesTypesToTarget( scope, new SimpleSearchEdgeType( node, null ) )
                                        .doOnNext( new Action1<String>() {
                                            @Override
                                            public void call( final String type ) {

                                                final MutationBatch batch = keyspace.prepareMutationBatch();

                                                final Iterator<String> types = edgeMetadataSerialization
                                                        .getIdTypesToTarget( scope,
                                                                new SimpleSearchIdType( node, type, null ) );

                                                while ( types.hasNext() ) {
                                                    batch.mergeShallow( edgeMetadataSerialization
                                                            .removeIdTypeToTarget( scope, node, type, types.next(),
                                                                    version ) );
                                                }

                                                batch.mergeShallow( edgeMetadataSerialization
                                                        .removeEdgeTypeToTarget( scope, node, type, version ) );

                                                try {
                                                    batch.execute();
                                                }
                                                catch ( ConnectionException e ) {
                                                    throw new RuntimeException( "Unable to execute cassandra call" );
                                                }
                                            }
                                        } );

                        //delete all meta for edges from this node
                        Observable<String> sources =
                                getEdgesTypesFromSource( scope, new SimpleSearchEdgeType( node, null ) )
                                        .doOnNext( new Action1<String>() {
                                            @Override
                                            public void call( final String type ) {

                                                final MutationBatch batch = keyspace.prepareMutationBatch();

                                                final Iterator<String> types = edgeMetadataSerialization
                                                        .getIdTypesFromSource( scope,
                                                                new SimpleSearchIdType( node, type, null ) );

                                                while ( types.hasNext() ) {
                                                    batch.mergeShallow( edgeMetadataSerialization
                                                            .removeIdTypeFromSource( scope, node, type, types.next(),
                                                                    version ) );
                                                }

                                                batch.mergeShallow( edgeMetadataSerialization
                                                        .removeEdgeTypeFromSource( scope, node, type, version ) );

                                                try {
                                                    batch.execute();
                                                }
                                                catch ( ConnectionException e ) {
                                                    throw new RuntimeException( "Unable to execute cassandra call" );
                                                }
                                            }
                                        } );


                        //no op, just zip them up  so we can do our action on our last
                        return Observable.merge( targets, sources )
                                //when we're done emitting, merge them
                                .doOnCompleted( new Action0() {
                                    @Override
                                    public void call() {
                                        try {
                                            nodeSerialization.delete( scope, node, edgeEvent.getVersion() ).execute();
                                        }
                                        catch ( ConnectionException e ) {
                                            throw new RuntimeException( "Unable to execute mutation" );
                                        }
                                    }
                                } );
                    }
                } )
                        //when we're completed, we need to delete our id from types, then remove our mark

                        //return our event to the caller
                .map( new Func1<String, EdgeEvent<Id>>() {
                    @Override
                    public EdgeEvent<Id> call( final String mark ) {
                        return edgeEvent;
                    }
                } );
    }


    /**
     * Get all existing edge types to the target node
     */
    private Observable<String> getEdgesTypesToTarget( final OrganizationScope scope, final SearchEdgeType search ) {

        return Observable.create( new ObservableIterator<String>() {
            @Override
            protected Iterator<String> getIterator() {
                return edgeMetadataSerialization.getEdgeTypesToTarget( scope, search );
            }
        } ).subscribeOn( scheduler );
    }


    /**
     * Get all existing edge types to the target node
     */
    private Observable<String> getEdgesTypesFromSource( final OrganizationScope scope, final SearchEdgeType search ) {

        return Observable.create( new ObservableIterator<String>() {
            @Override
            protected Iterator<String> getIterator() {
                return edgeMetadataSerialization.getEdgeTypesFromSource( scope, search );
            }
        } ).subscribeOn( scheduler );
    }


    /**
     * Load all edges pointing to this target
     */
    private Observable<MarkedEdge> loadEdgesToTarget( final OrganizationScope scope, final SearchByEdgeType search ) {

        return Observable.create( new ObservableIterator<MarkedEdge>() {
            @Override
            protected Iterator<MarkedEdge> getIterator() {
                return edgeSerialization.getEdgesToTarget( scope, search );
            }
        } ).subscribeOn( scheduler );
    }


    /**
     * Load all edges pointing to this target
     */
    private Observable<MarkedEdge> loadEdgesFromSource( final OrganizationScope scope, final SearchByEdgeType search ) {

        return Observable.create( new ObservableIterator<MarkedEdge>() {
            @Override
            protected Iterator<MarkedEdge> getIterator() {
                return edgeSerialization.getEdgesFromSource( scope, search );
            }
        } ).subscribeOn( scheduler );
    }


    private class Delete implements Action1<List<MarkedEdge>> {

        private final OrganizationScope scope;


        private Delete( final OrganizationScope scope ) {
            this.scope = scope;
        }


        @Override
        public void call( final List<MarkedEdge> markedEdges ) {

            //do nothing
            if ( markedEdges.size() == 0 ) {
                return;
            }

            Iterator<MarkedEdge> marked = markedEdges.iterator();

            MutationBatch batch = edgeSerialization.deleteEdge( scope, marked.next() );

            while ( marked.hasNext() ) {
                final MutationBatch newDelete = edgeSerialization.deleteEdge( scope, marked.next() );
                batch.mergeShallow( newDelete );
            }

            try {
                batch.execute();
            }
            catch ( ConnectionException e ) {
                throw new RuntimeException( "Unable to execute batch mutation", e );
            }
        }
    }
}
