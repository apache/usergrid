/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.usergrid.persistence.graph.impl;


import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.core.metrics.MetricsFactory;
import org.apache.usergrid.persistence.core.metrics.ObservableTimer;
import org.apache.usergrid.persistence.core.rx.ObservableIterator;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.util.ValidationUtils;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.GraphFig;
import org.apache.usergrid.persistence.graph.GraphManager;
import org.apache.usergrid.persistence.graph.MarkedEdge;
import org.apache.usergrid.persistence.graph.SearchByEdge;
import org.apache.usergrid.persistence.graph.SearchByEdgeType;
import org.apache.usergrid.persistence.graph.SearchByIdType;
import org.apache.usergrid.persistence.graph.SearchEdgeType;
import org.apache.usergrid.persistence.graph.SearchIdType;
import org.apache.usergrid.persistence.graph.impl.stage.EdgeDeleteListener;
import org.apache.usergrid.persistence.graph.impl.stage.NodeDeleteListener;
import org.apache.usergrid.persistence.graph.serialization.EdgeMetadataSerialization;
import org.apache.usergrid.persistence.graph.serialization.EdgeSerialization;
import org.apache.usergrid.persistence.graph.serialization.NodeSerialization;
import org.apache.usergrid.persistence.graph.serialization.util.GraphValidation;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.codahale.metrics.Timer;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import rx.Observable;


/**
 * Implementation of graph edges
 */
public class GraphManagerImpl implements GraphManager {

    private static final Logger LOG = LoggerFactory.getLogger( GraphManagerImpl.class );

    private final ApplicationScope scope;

    private final EdgeMetadataSerialization edgeMetadataSerialization;


    private final EdgeSerialization storageEdgeSerialization;

    private final NodeSerialization nodeSerialization;

    private final EdgeDeleteListener edgeDeleteListener;
    private final NodeDeleteListener nodeDeleteListener;
    private final Timer writeEdgeTimer;
    private final Timer markEdgeTimer;
    private final Timer markNodeTimer;
    private final Timer loadEdgesFromSourceTimer;
    private final Timer loadEdgesToTargetTimer;
    private final Timer loadEdgesVersionsTimer;
    private final Timer loadEdgesFromSourceByTypeTimer;
    private final Timer loadEdgesToTargetByTypeTimer;
    private final Timer getEdgeTypesFromSourceTimer;
    private final Timer getIdTypesFromSourceTimer;
    private final Timer getEdgeTypesToTargetTimer;
    private final Timer getIdTypesToTargetTimer;
    private final Timer deleteNodeTimer;
    private final Timer deleteEdgeTimer;


    private final GraphFig graphFig;


    @Inject
    public GraphManagerImpl( final EdgeMetadataSerialization edgeMetadataSerialization,
                             final EdgeSerialization storageEdgeSerialization,
                             final NodeSerialization nodeSerialization, final GraphFig graphFig,
                             final EdgeDeleteListener edgeDeleteListener, final NodeDeleteListener nodeDeleteListener,
                             final ApplicationScope scope, MetricsFactory metricsFactory ) {


        ValidationUtils.validateApplicationScope( scope );
        Preconditions.checkNotNull( edgeMetadataSerialization, "edgeMetadataSerialization must not be null" );
        Preconditions.checkNotNull( storageEdgeSerialization, "storageEdgeSerialization must not be null" );
        Preconditions.checkNotNull( nodeSerialization, "nodeSerialization must not be null" );
        Preconditions.checkNotNull( graphFig, "consistencyFig must not be null" );
        Preconditions.checkNotNull( scope, "scope must not be null" );
        Preconditions.checkNotNull( nodeDeleteListener, "nodeDeleteListener must not be null" );

        this.scope = scope;
        this.edgeMetadataSerialization = edgeMetadataSerialization;
        this.storageEdgeSerialization = storageEdgeSerialization;
        this.nodeSerialization = nodeSerialization;
        this.graphFig = graphFig;
        this.edgeDeleteListener = edgeDeleteListener;
        this.nodeDeleteListener = nodeDeleteListener;

        this.markNodeTimer = metricsFactory.getTimer( GraphManagerImpl.class, "node.mark" );
        this.deleteNodeTimer = metricsFactory.getTimer( GraphManagerImpl.class, "node.delete" );
        this.writeEdgeTimer = metricsFactory.getTimer( GraphManagerImpl.class, "edge.write" );

        this.markEdgeTimer = metricsFactory.getTimer( GraphManagerImpl.class, "edge.mark" );
        this.deleteEdgeTimer = metricsFactory.getTimer( GraphManagerImpl.class, "edge.delete" );
        this.loadEdgesFromSourceTimer = metricsFactory.getTimer( GraphManagerImpl.class, "edge.load_from" );
        this.loadEdgesToTargetTimer = metricsFactory.getTimer( GraphManagerImpl.class, "edge.load_to" );
        this.loadEdgesVersionsTimer = metricsFactory.getTimer( GraphManagerImpl.class, "edge.load_versions" );
        this.loadEdgesFromSourceByTypeTimer = metricsFactory.getTimer( GraphManagerImpl.class, "edge.load_from_type" );
        this.loadEdgesToTargetByTypeTimer = metricsFactory.getTimer( GraphManagerImpl.class, "edge.load_to_type" );
        this.getEdgeTypesFromSourceTimer = metricsFactory.getTimer( GraphManagerImpl.class, "edge.get_edge_from" );
        this.getEdgeTypesToTargetTimer = metricsFactory.getTimer( GraphManagerImpl.class, "edge.get_to" );

        this.getIdTypesFromSourceTimer = metricsFactory.getTimer( GraphManagerImpl.class, "idtype.get_from" );
        this.getIdTypesToTargetTimer = metricsFactory.getTimer( GraphManagerImpl.class, "idtype.get_to" );


    }


    @Override
    public Observable<Edge> writeEdge( final Edge edge ) {
        GraphValidation.validateEdge( edge );

        final MarkedEdge markedEdge = new SimpleMarkedEdge( edge, false );

        final Observable<Edge> observable = Observable.just( markedEdge ).map( edge1 -> {

            final UUID timestamp = UUIDGenerator.newTimeUUID();


            final MutationBatch mutation = edgeMetadataSerialization.writeEdge( scope, edge1 );

            final MutationBatch edgeMutation = storageEdgeSerialization.writeEdge( scope, edge1, timestamp );

            mutation.mergeShallow( edgeMutation );

            try {
                mutation.execute();
            }
            catch ( ConnectionException e ) {
                throw new RuntimeException( "Unable to execute mutation", e );
            }

            return edge1;
        } );

        return ObservableTimer.time( observable, writeEdgeTimer );
    }


    @Override
    public Observable<Edge> markEdge( final Edge edge ) {
        GraphValidation.validateEdge( edge );

        final MarkedEdge markedEdge = new SimpleMarkedEdge( edge, true );

        final Observable<Edge> observable = Observable.just( markedEdge ).map( edge1 -> {

            final UUID timestamp = UUIDGenerator.newTimeUUID();


            final MutationBatch edgeMutation = storageEdgeSerialization.writeEdge( scope, edge1, timestamp );


            LOG.debug( "Marking edge {} as deleted to commit log", edge1 );
            try {
                edgeMutation.execute();
            }
            catch ( ConnectionException e ) {
                throw new RuntimeException( "Unable to execute mutation", e );
            }


            return edge1;
        } );

        return ObservableTimer.time( observable, markEdgeTimer );
    }


    @Override
    public Observable<Edge> deleteEdge( final Edge edge ) {

        GraphValidation.validateEdge( edge );
        final UUID startTimestamp = UUIDGenerator.newTimeUUID();


        final Observable<Edge> observable =
            Observable.create( new ObservableIterator<MarkedEdge>( "read edge versions" ) {
                @Override
                protected Iterator<MarkedEdge> getIterator() {
                    return storageEdgeSerialization.getEdgeVersions( scope,
                        new SimpleSearchByEdge( edge.getSourceNode(), edge.getType(), edge.getTargetNode(),
                            Long.MAX_VALUE, SearchByEdgeType.Order.DESCENDING, Optional.absent() ) );
                }
            } ).filter( markedEdge -> markedEdge.isDeleted() ).flatMap( marked ->
                //fire our delete listener and wait for the results
                edgeDeleteListener.receive( scope, marked, startTimestamp ).doOnNext(
                    //log them
                    count -> LOG.debug( "removed {} types for edge {} ", count, edge ) )
                    //return the marked edge
                    .map( count -> marked ) );


        return ObservableTimer.time( observable, deleteEdgeTimer );
    }


    @Override
    public Observable<Id> markNode( final Id node, final long timestamp ) {
        final Observable<Id> idObservable = Observable.just( node ).map( id -> {

            //mark the node as deleted

            final MutationBatch nodeMutation = nodeSerialization.mark( scope, id, timestamp );


            LOG.debug( "Marking node {} as deleted to node mark", node );
            try {
                nodeMutation.execute();
            }
            catch ( ConnectionException e ) {
                throw new RuntimeException( "Unable to execute mutation", e );
            }


            return id;
        } );

        return ObservableTimer.time( idObservable, markNodeTimer );
    }


    @Override
    public Observable<Id> compactNode( final Id inputNode ) {


        final UUID startTime = UUIDGenerator.newTimeUUID();


        final Observable<Id> nodeObservable =
            Observable.just( inputNode ).map( node -> nodeSerialization.getMaxVersion( scope, inputNode ) ).takeWhile(
                maxTimestamp -> maxTimestamp.isPresent() )

                //map our delete listener
                .flatMap( timestamp -> nodeDeleteListener.receive( scope, inputNode, startTime ) )
                    //set to 0 if nothing is emitted
                .lastOrDefault( 0 )
                    //log for posterity
                .doOnNext( count -> LOG.debug( "Removed {} edges from node {}", count, inputNode ) )
                    //return our id
                .map( count -> inputNode );

        return ObservableTimer.time( nodeObservable, this.deleteNodeTimer );
    }


    @Override
    public Observable<Edge> loadEdgeVersions( final SearchByEdge searchByEdge ) {

        final Observable<Edge> edges =
            Observable.create( new ObservableIterator<MarkedEdge>( "getEdgeTypesFromSource" ) {
                @Override
                protected Iterator<MarkedEdge> getIterator() {
                    return storageEdgeSerialization.getEdgeVersions( scope, searchByEdge );
                }
            } ).buffer( graphFig.getScanPageSize() )
                      .compose( new EdgeBufferFilter( searchByEdge.getMaxTimestamp(), searchByEdge.filterMarked() ) );

        return ObservableTimer.time( edges, loadEdgesVersionsTimer );
    }


    @Override
    public Observable<Edge> loadEdgesFromSource( final SearchByEdgeType search ) {
        final Observable<Edge> edges =
            Observable.create( new ObservableIterator<MarkedEdge>( "getEdgeTypesFromSource" ) {
                @Override
                protected Iterator<MarkedEdge> getIterator() {
                    return storageEdgeSerialization.getEdgesFromSource( scope, search );
                }
            } ).buffer( graphFig.getScanPageSize() )
                      .compose( new EdgeBufferFilter( search.getMaxTimestamp(), search.filterMarked() ) );

        return ObservableTimer.time( edges, loadEdgesFromSourceTimer );
    }


    @Override
    public Observable<Edge> loadEdgesToTarget( final SearchByEdgeType search ) {
        final Observable<Edge> edges =
            Observable.create( new ObservableIterator<MarkedEdge>( "getEdgeTypesFromSource" ) {
                @Override
                protected Iterator<MarkedEdge> getIterator() {
                    return storageEdgeSerialization.getEdgesToTarget( scope, search );
                }
            } ).buffer( graphFig.getScanPageSize() )
                      .compose( new EdgeBufferFilter( search.getMaxTimestamp(), search.filterMarked() ) );


        return ObservableTimer.time( edges, loadEdgesToTargetTimer );
    }


    @Override
    public Observable<Edge> loadEdgesFromSourceByType( final SearchByIdType search ) {
        final Observable<Edge> edges =
            Observable.create( new ObservableIterator<MarkedEdge>( "getEdgeTypesFromSource" ) {
                @Override
                protected Iterator<MarkedEdge> getIterator() {
                    return storageEdgeSerialization.getEdgesFromSourceByTargetType( scope, search );
                }
            } ).buffer( graphFig.getScanPageSize() )
                      .compose( new EdgeBufferFilter( search.getMaxTimestamp(), search.filterMarked() ) );

        return ObservableTimer.time( edges, loadEdgesFromSourceByTypeTimer );
    }


    @Override
    public Observable<Edge> loadEdgesToTargetByType( final SearchByIdType search ) {
        final Observable<Edge> edges =
            Observable.create( new ObservableIterator<MarkedEdge>( "getEdgeTypesFromSource" ) {
                @Override
                protected Iterator<MarkedEdge> getIterator() {
                    return storageEdgeSerialization.getEdgesToTargetBySourceType( scope, search );
                }
            } ).buffer( graphFig.getScanPageSize() )
                      .compose( new EdgeBufferFilter( search.getMaxTimestamp(), search.filterMarked() ) );

        return ObservableTimer.time( edges, loadEdgesToTargetByTypeTimer );
    }


    @Override
    public Observable<String> getEdgeTypesFromSource( final SearchEdgeType search ) {
        final Observable<String> edgeTypes =
            Observable.create( new ObservableIterator<String>( "getEdgeTypesFromSource" ) {
                    @Override
                    protected Iterator<String> getIterator() {
                        return edgeMetadataSerialization.getEdgeTypesFromSource( scope, search );
                    }
                } );

        return ObservableTimer.time( edgeTypes, getEdgeTypesFromSourceTimer );
    }


    @Override
    public Observable<String> getIdTypesFromSource( final SearchIdType search ) {
        final Observable<String> edgeTypes =
            Observable.create( new ObservableIterator<String>( "getIdTypesFromSource" ) {
                @Override
                protected Iterator<String> getIterator() {
                    return edgeMetadataSerialization.getIdTypesFromSource( scope, search );
                }
            } );

        return ObservableTimer.time( edgeTypes, getIdTypesFromSourceTimer );
    }


    @Override
    public Observable<String> getEdgeTypesToTarget( final SearchEdgeType search ) {
        final Observable<String> edgeTypes =
            Observable.create( new ObservableIterator<String>( "getEdgeTypesToTarget" ) {
                    @Override
                    protected Iterator<String> getIterator() {
                        return edgeMetadataSerialization.getEdgeTypesToTarget( scope, search );
                    }
                } );

        return ObservableTimer.time( edgeTypes, getEdgeTypesToTargetTimer );
    }


    @Override
    public Observable<String> getIdTypesToTarget( final SearchIdType search ) {
        final Observable<String> edgeTypes = Observable.create( new ObservableIterator<String>( "getIdTypesToTarget" ) {
                @Override
                protected Iterator<String> getIterator() {
                    return edgeMetadataSerialization.getIdTypesToTarget( scope, search );
                }
            } );

        return ObservableTimer.time( edgeTypes, getIdTypesToTargetTimer );
    }


    /**
     * Helper filter to perform mapping and return an observable of pre-filtered edges
     */
    private class EdgeBufferFilter implements
        Observable.Transformer<List<MarkedEdge>, MarkedEdge> {//implements Func1<List<MarkedEdge>,
        // Observable<MarkedEdge>> {

        private final long maxVersion;
        private final boolean filterMarked;


        private EdgeBufferFilter( final long maxVersion, final boolean filterMarked ) {
            this.maxVersion = maxVersion;
            this.filterMarked = filterMarked;
        }


        @Override

        /**
         * Takes a buffered list of marked edges.  It then does a single round trip to fetch marked ids These are then
         * used in conjunction with the max version filter to filter any edges that should not be returned
         *
         * @return An observable that emits only edges that can be consumed.  There could be multiple versions of the
         * same edge so those need de-duped.
         */ public Observable<MarkedEdge> call( final Observable<List<MarkedEdge>> markedEdgesObservable ) {

            return markedEdgesObservable.flatMap( markedEdges -> {

                final Observable<MarkedEdge> markedEdgeObservable = Observable.from( markedEdges );

                /**
                 * We aren't going to filter anything, return exactly what we're passed
                 */
                if(!filterMarked){
                    return markedEdgeObservable;
                }

                //We need to filter, perform that filter
                final Map<Id, Long> markedVersions = nodeSerialization.getMaxVersions( scope, markedEdges );

                return markedEdgeObservable.filter( edge -> {
                    final long edgeTimestamp = edge.getTimestamp();

                    //our edge needs to not be deleted and have a version that's > max Version
                    if ( edge.isDeleted() ) {
                        return false;
                    }


                    final Long sourceTimestamp = markedVersions.get( edge.getSourceNode() );

                    //the source Id has been marked for deletion.  It's version is <= to the marked version for
                    // deletion,
                    // so we need to discard it
                    if ( sourceTimestamp != null && Long.compare( edgeTimestamp, sourceTimestamp ) < 1 ) {
                        return false;
                    }

                    final Long targetTimestamp = markedVersions.get( edge.getTargetNode() );

                    //the target Id has been marked for deletion.  It's version is <= to the marked version for
                    // deletion,
                    // so we need to discard it
                    if ( targetTimestamp != null && Long.compare( edgeTimestamp, targetTimestamp ) < 1 ) {
                        return false;
                    }


                    return true;
                } );
            } );
        }
    }
}
