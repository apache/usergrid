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

import org.apache.usergrid.persistence.core.hystrix.HystrixCassandra;
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

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.functions.Func1;
import rx.schedulers.Schedulers;


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

    private Observer<Integer> edgeDeleteSubcriber;
    private Observer<Integer> nodeDelete;


    private final GraphFig graphFig;


    @Inject
    public GraphManagerImpl( final EdgeMetadataSerialization edgeMetadataSerialization,
                             final EdgeSerialization storageEdgeSerialization,
                             final NodeSerialization nodeSerialization, final GraphFig graphFig,
                             @Assisted final ApplicationScope scope, final EdgeDeleteListener edgeDeleteListener,
                             final NodeDeleteListener nodeDeleteListener ) {


        ValidationUtils.validateApplicationScope( scope );
        Preconditions.checkNotNull( edgeMetadataSerialization, "edgeMetadataSerialization must not be null" );
        Preconditions.checkNotNull( storageEdgeSerialization, "storageEdgeSerialization must not be null" );
        Preconditions.checkNotNull( nodeSerialization, "nodeSerialization must not be null" );
        Preconditions.checkNotNull( graphFig, "consistencyFig must not be null" );
        Preconditions.checkNotNull( scope, "scope must not be null" );

        this.scope = scope;
        this.edgeMetadataSerialization = edgeMetadataSerialization;
        this.storageEdgeSerialization = storageEdgeSerialization;
        this.nodeSerialization = nodeSerialization;
        this.graphFig = graphFig;
        this.edgeDeleteListener = edgeDeleteListener;
        this.nodeDeleteListener = nodeDeleteListener;

        this.edgeDeleteSubcriber = MetricSubscriber.INSTANCE;
        this.nodeDelete = MetricSubscriber.INSTANCE;
    }


    @Override
    public Observable<Edge> writeEdge( final Edge edge ) {
        GraphValidation.validateEdge( edge );

        final MarkedEdge markedEdge = new SimpleMarkedEdge( edge, false );

        return Observable.from( markedEdge ).subscribeOn( Schedulers.io() ).map( new Func1<MarkedEdge, Edge>() {
            @Override
            public Edge call( final MarkedEdge edge ) {

                final UUID timestamp = UUIDGenerator.newTimeUUID();


                final MutationBatch mutation = edgeMetadataSerialization.writeEdge( scope, edge );

                final MutationBatch edgeMutation = storageEdgeSerialization.writeEdge( scope, edge, timestamp );

                mutation.mergeShallow( edgeMutation );

                HystrixCassandra.user( mutation );

                return edge;
            }
        } );
    }


    @Override
    public Observable<Edge> deleteEdge( final Edge edge ) {
        GraphValidation.validateEdge( edge );

        final MarkedEdge markedEdge = new SimpleMarkedEdge( edge, true );


        return Observable.from( markedEdge ).subscribeOn( Schedulers.io() ).map( new Func1<MarkedEdge, Edge>() {
            @Override
            public Edge call( final MarkedEdge edge ) {

                final UUID timestamp = UUIDGenerator.newTimeUUID();


                final MutationBatch edgeMutation = storageEdgeSerialization.writeEdge( scope, edge, timestamp );


                LOG.debug( "Marking edge {} as deleted to commit log", edge );
                HystrixCassandra.user( edgeMutation );


                //HystrixCassandra.async( edgeDeleteListener.receive( scope, markedEdge,
                // timestamp )).subscribeOn( Schedulers.io() ).subscribe( edgeDeleteSubcriber );
                edgeDeleteListener.receive( scope, markedEdge, timestamp ).subscribeOn( Schedulers.io() )
                                  .subscribe( edgeDeleteSubcriber );


                return edge;
            }
        } );
    }


    @Override
    public Observable<Id> deleteNode( final Id node, final long timestamp ) {
        return Observable.from( node ).subscribeOn( Schedulers.io() ).map( new Func1<Id, Id>() {
            @Override
            public Id call( final Id id ) {

                //mark the node as deleted


                final UUID eventTimestamp = UUIDGenerator.newTimeUUID();

                final MutationBatch nodeMutation = nodeSerialization.mark( scope, id, timestamp );


                LOG.debug( "Marking node {} as deleted to node mark", node );
                HystrixCassandra.user( nodeMutation );


                //HystrixCassandra.async(nodeDeleteListener.receive(scope, id, eventTimestamp  )).subscribeOn(
                // Schedulers.io() ).subscribe( nodeDelete );
                nodeDeleteListener.receive( scope, id, eventTimestamp ).subscribeOn( Schedulers.io() )
                                  .subscribe( nodeDelete );

                return id;
            }
        } );
    }


    @Override
    public Observable<Edge> loadEdgeVersions( final SearchByEdge searchByEdge ) {
        return Observable.create( new ObservableIterator<MarkedEdge>( "getEdgeTypesFromSource" ) {
            @Override
            protected Iterator<MarkedEdge> getIterator() {
                return storageEdgeSerialization.getEdgeVersions( scope, searchByEdge );
            }
        } ).buffer( graphFig.getScanPageSize() ).flatMap( new EdgeBufferFilter( searchByEdge.getMaxTimestamp() ) )
                         .cast( Edge.class );
    }


    @Override
    public Observable<Edge> loadEdgesFromSource( final SearchByEdgeType search ) {
        return Observable.create( new ObservableIterator<MarkedEdge>( "getEdgeTypesFromSource" ) {
            @Override
            protected Iterator<MarkedEdge> getIterator() {
                return storageEdgeSerialization.getEdgesFromSource( scope, search );
            }
        } ).buffer( graphFig.getScanPageSize() ).flatMap( new EdgeBufferFilter( search.getMaxTimestamp() ) )
                         .cast( Edge.class );
    }


    @Override
    public Observable<Edge> loadEdgesToTarget( final SearchByEdgeType search ) {
        return Observable.create( new ObservableIterator<MarkedEdge>( "getEdgeTypesFromSource" ) {
            @Override
            protected Iterator<MarkedEdge> getIterator() {
                return storageEdgeSerialization.getEdgesToTarget( scope, search );
            }
        } ).buffer( graphFig.getScanPageSize() ).flatMap( new EdgeBufferFilter( search.getMaxTimestamp() ) )
                         .cast( Edge.class );
    }


    @Override
    public Observable<Edge> loadEdgesFromSourceByType( final SearchByIdType search ) {
        return Observable.create( new ObservableIterator<MarkedEdge>( "getEdgeTypesFromSource" ) {
            @Override
            protected Iterator<MarkedEdge> getIterator() {
                return storageEdgeSerialization.getEdgesFromSourceByTargetType( scope, search );
            }
        } ).buffer( graphFig.getScanPageSize() ).flatMap( new EdgeBufferFilter( search.getMaxTimestamp() ) )

                         .cast( Edge.class );
    }


    @Override
    public Observable<Edge> loadEdgesToTargetByType( final SearchByIdType search ) {
        return Observable.create( new ObservableIterator<MarkedEdge>( "getEdgeTypesFromSource" ) {
            @Override
            protected Iterator<MarkedEdge> getIterator() {
                return storageEdgeSerialization.getEdgesToTargetBySourceType( scope, search );
            }
        } ).buffer( graphFig.getScanPageSize() ).flatMap( new EdgeBufferFilter( search.getMaxTimestamp() ) )
                         .cast( Edge.class );
    }


    @Override
    public Observable<String> getEdgeTypesFromSource( final SearchEdgeType search ) {

        return Observable.create( new ObservableIterator<String>( "getEdgeTypesFromSource" ) {
            @Override
            protected Iterator<String> getIterator() {
                return edgeMetadataSerialization.getEdgeTypesFromSource( scope, search );
            }
        } );
    }


    @Override
    public Observable<String> getIdTypesFromSource( final SearchIdType search ) {
        return Observable.create( new ObservableIterator<String>( "getIdTypesFromSource" ) {
            @Override
            protected Iterator<String> getIterator() {
                return edgeMetadataSerialization.getIdTypesFromSource( scope, search );
            }
        } );
    }


    @Override
    public Observable<String> getEdgeTypesToTarget( final SearchEdgeType search ) {

        return Observable.create( new ObservableIterator<String>( "getEdgeTypesToTarget" ) {
            @Override
            protected Iterator<String> getIterator() {
                return edgeMetadataSerialization.getEdgeTypesToTarget( scope, search );
            }
        } );
    }


    @Override
    public Observable<String> getIdTypesToTarget( final SearchIdType search ) {
        return Observable.create( new ObservableIterator<String>( "getIdTypesToTarget" ) {
            @Override
            protected Iterator<String> getIterator() {
                return edgeMetadataSerialization.getIdTypesToTarget( scope, search );
            }
        } );
    }


    /**
     * Helper filter to perform mapping and return an observable of pre-filtered edges
     */
    private class EdgeBufferFilter implements Func1<List<MarkedEdge>, Observable<MarkedEdge>> {

        private final long maxVersion;


        private EdgeBufferFilter( final long maxVersion ) {
            this.maxVersion = maxVersion;
        }


        /**
         * Takes a buffered list of marked edges.  It then does a single round trip to fetch marked ids These are then
         * used in conjunction with the max version filter to filter any edges that should not be returned
         *
         * @return An observable that emits only edges that can be consumed.  There could be multiple versions of the
         * same edge so those need de-duped.
         */
        @Override
        public Observable<MarkedEdge> call( final List<MarkedEdge> markedEdges ) {

            final Map<Id, Long> markedVersions = nodeSerialization.getMaxVersions( scope, markedEdges );
            return Observable.from( markedEdges ).filter( new EdgeFilter( this.maxVersion, markedVersions ) );
        }
    }


    /**
     * Filter the returned values based on the max uuid and if it's been marked for deletion or not
     */
    private static class EdgeFilter implements Func1<MarkedEdge, Boolean> {

        private final long maxTimestamp;

        private final Map<Id, Long> markCache;


        private EdgeFilter( final long maxTimestamp, Map<Id, Long> markCache ) {
            this.maxTimestamp = maxTimestamp;
            this.markCache = markCache;
        }


        @Override
        public Boolean call( final MarkedEdge edge ) {


            final long edgeTimestamp = edge.getTimestamp();

            //our edge needs to not be deleted and have a version that's > max Version
            if ( edge.isDeleted() || Long.compare( edgeTimestamp, maxTimestamp ) > 0 ) {
                return false;
            }


            final Long sourceTimestamp = markCache.get( edge.getSourceNode() );

            //the source Id has been marked for deletion.  It's version is <= to the marked version for deletion,
            // so we need to discard it
            if ( sourceTimestamp != null && Long.compare( edgeTimestamp, sourceTimestamp ) < 1 ) {
                return false;
            }

            final Long targetTimestamp = markCache.get( edge.getTargetNode() );

            //the target Id has been marked for deletion.  It's version is <= to the marked version for deletion,
            // so we need to discard it
            if ( targetTimestamp != null && Long.compare( edgeTimestamp, targetTimestamp ) < 1 ) {
                return false;
            }


            return true;
        }
    }


    /**
     * Set the subscription for the edge delete
     */
    public void setEdgeDeleteSubcriber( final Observer<Integer> edgeDeleteSubcriber ) {
        Preconditions.checkNotNull( edgeDeleteSubcriber, "Subscriber cannot be null" );
        this.edgeDeleteSubcriber = edgeDeleteSubcriber;
    }


    /**
     * Set the subscription for the node delete
     */
    public void setNodeDelete( final Observer<Integer> nodeDelete ) {
        Preconditions.checkNotNull( nodeDelete, "Subscriber cannot be null" );
        this.nodeDelete = nodeDelete;
    }


    /**
     * Simple subscriber that can be used to gather metrics.  Needs to be refactored to use codehale metrics
     */
    private static class MetricSubscriber extends Subscriber<Integer> {


        private static final MetricSubscriber INSTANCE = new MetricSubscriber();

        private final Logger logger = LoggerFactory.getLogger( MetricSubscriber.class );


        @Override
        public void onCompleted() {
            logger.debug( "Event completed" );
        }


        @Override
        public void onError( final Throwable e ) {
            logger.error( "Failed to execute event", e );
        }


        @Override
        public void onNext( final Integer integer ) {
            logger.debug( "Next received {}", integer );
        }
    }
}
