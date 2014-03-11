package org.apache.usergrid.persistence.graph.impl;


import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.apache.usergrid.persistence.collection.OrganizationScope;
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


    /**
     * Wire the serialization dependencies
     */
    @Inject
    public NodeDeleteListener( final NodeSerialization nodeSerialization, final EdgeSerialization edgeSerialization,
                               final EdgeMetadataSerialization edgeMetadataSerialization, final GraphFig graphFig,
                               final Keyspace keyspace, @NodeDelete final AsyncProcessor nodeDelete ) {


        this.nodeSerialization = nodeSerialization;
        this.edgeSerialization = edgeSerialization;
        this.edgeMetadataSerialization = edgeMetadataSerialization;
        this.graphFig = graphFig;
        this.keyspace = keyspace;
        nodeDelete.addListener( this );
    }


    @Override
    public Observable<EdgeEvent<Id>> receive( final EdgeEvent<Id> edgeEvent ) {

        final Id node = edgeEvent.getData();
        final OrganizationScope scope = edgeEvent.getOrganizationScope();


        return Observable.from( node ).map( new Func1<Id, Optional<UUID>>() {
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
                        Observable<List<MarkedEdge>> targetEdges =
                                getEdgesTypesToTarget( scope, new SimpleSearchEdgeType( node, null ) )
                                        .flatMap( new Func1<String, Observable<List<MarkedEdge>>>() {
                                            @Override
                                            public Observable<List<MarkedEdge>> call( final String edgeType ) {
                                                return loadEdgesToTarget( scope,
                                                        new SimpleSearchByEdgeType( node, edgeType, uuidOptional.get(),
                                                                null ) ).buffer( graphFig.getScanPageSize() );
                                            }
                                        } );


                        //get all edges pointing to the source node and buffer them into groups for deletion
                        Observable<List<MarkedEdge>> sourceEdges =
                                getEdgesTypesFromSource( scope, new SimpleSearchEdgeType( node, null ) )
                                        .flatMap( new Func1<String, Observable<List<MarkedEdge>>>() {
                                            @Override
                                            public Observable<List<MarkedEdge>> call( final String edgeType ) {
                                                return loadEdgesFromSource( scope,
                                                        new SimpleSearchByEdgeType( node, edgeType, uuidOptional.get(),
                                                                null ) ).buffer( graphFig.getScanPageSize() );
                                            }
                                        } );


                        return Observable.merge( targetEdges, sourceEdges );
                    }
                } ).doOnNext( new Action1<List<MarkedEdge>>() {
                    @Override
                    public void call( final List<MarkedEdge> markedEdges ) {
                        MutationBatch batch = keyspace.prepareMutationBatch();


                        for ( MarkedEdge marked : markedEdges ) {
                            final MutationBatch edgeBatch = edgeSerialization.deleteEdge( scope, marked );
                            batch.mergeShallow( edgeBatch );
                        }

                        try {
                            batch.execute();
                        }
                        catch ( ConnectionException e ) {
                            throw new RuntimeException( "Unable to execute mutation" );
                        }
                    }
                } ).map( new Func1<List<MarkedEdge>, EdgeEvent<Id>>() {
                    @Override
                    public EdgeEvent<Id> call( final List<MarkedEdge> markedEdges ) {
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
        } );
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
        } );
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
        } );
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
        } );
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
