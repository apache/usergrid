package org.apache.usergrid.persistence.graph.impl;


import java.util.Iterator;
import java.util.UUID;

import org.apache.usergrid.persistence.collection.OrganizationScope;
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

import rx.Observable;
import rx.util.functions.Action1;
import rx.util.functions.Func1;


/**
 * Construct the asynchronous node delete from the q
 */
public class NodeDeleteListener implements MessageListener<EdgeEvent<Id>, EdgeEvent<Id>> {

    private final NodeSerialization nodeSerialization;
    private final EdgeSerialization edgeSerialization;
    private final EdgeMetadataSerialization edgeMetadataSerialization;


    /**
     * Wire the serialization dependencies
     */
    @Inject
    public NodeDeleteListener( final NodeSerialization nodeSerialization, final EdgeSerialization edgeSerialization,
                               final EdgeMetadataSerialization edgeMetadataSerialization, @NodeDelete final AsyncProcessor nodeDelete) {


        this.nodeSerialization = nodeSerialization;
        this.edgeSerialization = edgeSerialization;
        this.edgeMetadataSerialization = edgeMetadataSerialization;
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
        } ).flatMap( new Func1<Optional<UUID>, Observable<MarkedEdge>>() {
            @Override
            public Observable<MarkedEdge> call( final Optional<UUID> uuidOptional ) {

                return getEdgesTypesToTarget( scope, new SimpleSearchEdgeType( node, null ) )
                        .flatMap( new Func1<String, Observable<MarkedEdge>>() {
                            @Override
                            public Observable<MarkedEdge> call( final String edgeType ) {

                                //for each edge type, we want to search all edges < this version to the node and
                                // delete them. We might want to batch this up for efficiency
                                return loadEdgesToTarget( scope,
                                        new SimpleSearchByEdgeType( node, edgeType, uuidOptional.get(), null ) )
                                        .doOnEach( new Action1<MarkedEdge>() {
                                            @Override
                                            public void call( final MarkedEdge markedEdge ) {
                                                edgeSerialization.deleteEdge( scope, markedEdge );
                                            }
                                        } );
                            }
                        } );
            }
        } ).map( new Func1<MarkedEdge, EdgeEvent<Id>>() {
            @Override
            public EdgeEvent<Id> call( final MarkedEdge edge ) {
                return edgeEvent;
            }
        } );
    }


    /**
     * Get all existing edge types to the target node
     * @param scope
     * @param search
     * @return
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
     * Load all edges pointing to this target
     * @param scope
     * @param search
     * @return
     */
    private Observable<MarkedEdge> loadEdgesToTarget( final OrganizationScope scope, final SearchByEdgeType search ) {

        return Observable.create( new ObservableIterator<MarkedEdge>() {
            @Override
            protected Iterator<MarkedEdge> getIterator() {
                return edgeSerialization.getEdgesToTarget( scope, search );
            }
        } );
    }
}
