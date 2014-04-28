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

import org.apache.usergrid.persistence.core.consistency.AsyncProcessor;
import org.apache.usergrid.persistence.core.consistency.AsynchronousMessage;
import org.apache.usergrid.persistence.core.consistency.ConsistencyFig;
import org.apache.usergrid.persistence.core.hystrix.HystrixGraphObservable;
import org.apache.usergrid.persistence.core.rx.ObservableIterator;
import org.apache.usergrid.persistence.core.scope.OrganizationScope;
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
import org.apache.usergrid.persistence.graph.guice.CommitLogEdgeSerialization;
import org.apache.usergrid.persistence.graph.guice.EdgeDelete;
import org.apache.usergrid.persistence.graph.guice.NodeDelete;
import org.apache.usergrid.persistence.graph.serialization.EdgeMetadataSerialization;
import org.apache.usergrid.persistence.graph.serialization.EdgeSerialization;
import org.apache.usergrid.persistence.graph.serialization.NodeSerialization;
import org.apache.usergrid.persistence.graph.serialization.impl.MergedEdgeReader;
import org.apache.usergrid.persistence.graph.serialization.util.EdgeUtils;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.fasterxml.uuid.UUIDComparator;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import rx.Observable;
import rx.functions.Func1;
import rx.schedulers.Schedulers;


/**
 * Implementation of graph edges
 */
public class GraphManagerImpl implements GraphManager {


    private final OrganizationScope scope;

    private final EdgeMetadataSerialization edgeMetadataSerialization;


    private final MergedEdgeReader mergedEdgeReader;
    private final EdgeSerialization commitLogSerialization;

    private final NodeSerialization nodeSerialization;

    private final AsyncProcessor<Edge> edgeDeleteAsyncProcessor;
    private final AsyncProcessor<Edge> edgeWriteAsyncProcessor;
    private final AsyncProcessor<Id> nodeDeleteAsyncProcessor;

    private final GraphFig graphFig;
    private final ConsistencyFig consistencyFig;


    @Inject
    public GraphManagerImpl( final EdgeMetadataSerialization edgeMetadataSerialization,
                             @CommitLogEdgeSerialization final EdgeSerialization commitLogSerialization,
                             final NodeSerialization nodeSerialization, final GraphFig graphFig,
                             @EdgeDelete final AsyncProcessor edgeDelete, @NodeDelete final AsyncProcessor nodeDelete,
                             @NodeDelete final AsyncProcessor edgeWrite, @Assisted final OrganizationScope scope,
                             final MergedEdgeReader mergedEdgeReader, final ConsistencyFig consistencyFig ) {



        ValidationUtils.validateOrganizationScope( scope );
        Preconditions.checkNotNull( edgeMetadataSerialization, "edgeMetadataSerialization must not be null" );
        Preconditions.checkNotNull( mergedEdgeReader, "mergedEdgeReader must not be null" );
        Preconditions.checkNotNull( commitLogSerialization, "commitLogSerialization must not be null" );
        Preconditions.checkNotNull( nodeSerialization, "nodeSerialization must not be null" );
        Preconditions.checkNotNull( graphFig, "consistencyFig must not be null" );
        Preconditions.checkNotNull( edgeDelete, "edgeDelete must not be null" );
        Preconditions.checkNotNull( nodeDelete, "nodeDelete must not be null" );
        Preconditions.checkNotNull( edgeWrite, "edgeWrite must not be null" );
        Preconditions.checkNotNull( scope, "scope must not be null" );
        Preconditions.checkNotNull( consistencyFig, "consistencyFig must not be null" );

        this.scope = scope;
        this.edgeMetadataSerialization = edgeMetadataSerialization;
        this.mergedEdgeReader = mergedEdgeReader;
        this.commitLogSerialization = commitLogSerialization;
        this.nodeSerialization = nodeSerialization;
        this.graphFig = graphFig;
        this.consistencyFig = consistencyFig;


        this.edgeDeleteAsyncProcessor = edgeDelete;


        this.nodeDeleteAsyncProcessor = nodeDelete;

        this.edgeWriteAsyncProcessor = edgeWrite;
    }


    @Override
    public Observable<Edge> writeEdge( final Edge edge ) {
        EdgeUtils.validateEdge( edge );

        return HystrixGraphObservable
                .user( Observable.from( edge ).subscribeOn( Schedulers.io() ).map( new Func1<Edge, Edge>() {
                    @Override
                    public Edge call( final Edge edge ) {
                        final AsynchronousMessage<Edge> event =
                                                   edgeWriteAsyncProcessor.setVerification( edge, getTimeout() );

                        final MutationBatch mutation = edgeMetadataSerialization.writeEdge( scope, edge );

                        final MutationBatch edgeMutation = commitLogSerialization.writeEdge( scope, edge );

                        mutation.mergeShallow( edgeMutation );

                        try {
                            mutation.execute();
                        }
                        catch ( ConnectionException e ) {
                            throw new RuntimeException( "Unable to connect to cassandra", e );
                        }

                        edgeWriteAsyncProcessor.start( event );


                        return edge;
                    }
                } ) );
    }


    @Override
    public Observable<Edge> deleteEdge( final Edge edge ) {
        EdgeUtils.validateEdge( edge );

        return HystrixGraphObservable
                .user( Observable.from( edge ).subscribeOn( Schedulers.io() ).map( new Func1<Edge, Edge>() {
                    @Override
                    public Edge call( final Edge edge ) {
                        final MutationBatch edgeMutation = commitLogSerialization.markEdge( scope, edge );

                        final AsynchronousMessage<Edge> event =
                                edgeDeleteAsyncProcessor.setVerification( edge, getTimeout() );


                        try {
                            edgeMutation.execute();
                        }
                        catch ( ConnectionException e ) {
                            throw new RuntimeException( "Unable to connect to cassandra", e );
                        }

                        edgeDeleteAsyncProcessor.start( event );


                        return edge;
                    }
                } ) );
    }


    @Override
    public Observable<Id> deleteNode( final Id node ) {
        return HystrixGraphObservable
                .user( Observable.from( node ).subscribeOn( Schedulers.io() ).map( new Func1<Id, Id>() {
                    @Override
                    public Id call( final Id id ) {

                        //mark the node as deleted
                        final UUID deleteTime = UUIDGenerator.newTimeUUID();

                        final MutationBatch nodeMutation = nodeSerialization.mark( scope, id, deleteTime );

                        final AsynchronousMessage<Id> event =
                                nodeDeleteAsyncProcessor.setVerification( node, getTimeout() );


                        try {
                            nodeMutation.execute();
                        }
                        catch ( ConnectionException e ) {
                            throw new RuntimeException( "Unable to connect to cassandra", e );
                        }

                        nodeDeleteAsyncProcessor.start( event );

                        return id;
                    }
                } ) );
    }


    @Override
    public Observable<Edge> loadEdgeVersions( final SearchByEdge searchByEdge ) {
        return HystrixGraphObservable.user( mergedEdgeReader.getEdgeVersions( scope, searchByEdge ).buffer( graphFig.getScanPageSize() ).flatMap( new EdgeBufferFilter( searchByEdge.getMaxVersion() ) )
                                                      .cast( Edge.class ) );
    }


    @Override
    public Observable<Edge> loadEdgesFromSource( final SearchByEdgeType search ) {
        return HystrixGraphObservable
                .user( mergedEdgeReader.getEdgesFromSource( scope, search ).buffer( graphFig.getScanPageSize() ).flatMap( new EdgeBufferFilter( search.getMaxVersion() ) )
                                 .cast( Edge.class ) );
    }


    @Override
    public Observable<Edge> loadEdgesToTarget( final SearchByEdgeType search ) {
        return HystrixGraphObservable
                .user( mergedEdgeReader.getEdgesToTarget( scope, search ).buffer( graphFig.getScanPageSize() ).flatMap( new EdgeBufferFilter( search.getMaxVersion() ) )
                                 .cast( Edge.class ) );
    }


    @Override
    public Observable<Edge> loadEdgesFromSourceByType( final SearchByIdType search ) {
        return HystrixGraphObservable
                .user(mergedEdgeReader.getEdgesFromSourceByTargetType( scope, search ).buffer( graphFig.getScanPageSize() ).flatMap( new EdgeBufferFilter( search.getMaxVersion() ) )

                                 .cast( Edge.class ) );
    }


    @Override
    public Observable<Edge> loadEdgesToTargetByType( final SearchByIdType search ) {
        return HystrixGraphObservable
                .user(mergedEdgeReader.getEdgesToTargetBySourceType( scope, search ).buffer( graphFig.getScanPageSize() ).flatMap( new EdgeBufferFilter( search.getMaxVersion() ) )
                                 .cast( Edge.class ) );
    }


    @Override
    public Observable<String> getEdgeTypesFromSource( final SearchEdgeType search ) {

        return HystrixGraphObservable
                .user( Observable.create( new ObservableIterator<String>( "getEdgeTypesFromSource" ) {
                    @Override
                    protected Iterator<String> getIterator() {
                        return edgeMetadataSerialization.getEdgeTypesFromSource( scope, search );
                    }
                } ) );
    }


    @Override
    public Observable<String> getIdTypesFromSource( final SearchIdType search ) {
        return HystrixGraphObservable
                .user( Observable.create( new ObservableIterator<String>( "getIdTypesFromSource" ) {
                    @Override
                    protected Iterator<String> getIterator() {
                        return edgeMetadataSerialization.getIdTypesFromSource( scope, search );
                    }
                } ) );
    }


    @Override
    public Observable<String> getEdgeTypesToTarget( final SearchEdgeType search ) {

        return HystrixGraphObservable
                .user( Observable.create( new ObservableIterator<String>( "getEdgeTypesToTarget" ) {
                    @Override
                    protected Iterator<String> getIterator() {
                        return edgeMetadataSerialization.getEdgeTypesToTarget( scope, search );
                    }
                } ) );
    }


    @Override
    public Observable<String> getIdTypesToTarget( final SearchIdType search ) {
        return HystrixGraphObservable.user( Observable.create( new ObservableIterator<String>( "getIdTypesToTarget" ) {
            @Override
            protected Iterator<String> getIterator() {
                return edgeMetadataSerialization.getIdTypesToTarget( scope, search );
            }
        } ) );
    }


    /**
     * Get our timeout for write org.apache.usergrid.persistence.core.consistency
     */
    private long getTimeout() {
        return consistencyFig.getRepairTimeout() * 2;
    }


    /**
     * Helper filter to perform mapping and return an observable of pre-filtered edges
     */
    private class EdgeBufferFilter implements Func1<List<MarkedEdge>, Observable<MarkedEdge>> {

        private final UUID maxVersion;


        private EdgeBufferFilter( final UUID maxVersion ) {
            this.maxVersion = maxVersion;
        }


        /**
         * Takes a buffered list of marked edges.  It then does a single round trip to fetch marked ids These are then
         * used in conjunction with the max version filter to filter any edges that should not be returned
         *
         * @return An observable that emits only edges that can be consumed.  There could be multiple versions of the
         *         same edge so those need de-duped.
         */
        @Override
        public Observable<MarkedEdge> call( final List<MarkedEdge> markedEdges ) {

            final Map<Id, UUID> markedVersions = nodeSerialization.getMaxVersions( scope, markedEdges );
            return Observable.from( markedEdges ).filter( new EdgeFilter( this.maxVersion, markedVersions ) );
        }
    }


    /**
     * Filter the returned values based on the max uuid and if it's been marked for deletion or not
     */
    private static class EdgeFilter implements Func1<MarkedEdge, Boolean> {

        private final UUID maxVersion;

        private final Map<Id, UUID> markCache;


        private EdgeFilter( final UUID maxVersion, Map<Id, UUID> markCache ) {
            this.maxVersion = maxVersion;
            this.markCache = markCache;
        }


        @Override
        public Boolean call( final MarkedEdge edge ) {


            final UUID edgeVersion = edge.getVersion();

            //our edge needs to not be deleted and have a version that's > max Version
            if ( edge.isDeleted() || UUIDComparator.staticCompare( edgeVersion, maxVersion ) > 0 ) {
                return false;
            }


            final UUID sourceVersion = markCache.get( edge.getSourceNode() );

            //the source Id has been marked for deletion.  It's version is <= to the marked version for deletion,
            // so we need to discard it
            if ( sourceVersion != null && UUIDComparator.staticCompare( edgeVersion, sourceVersion ) < 1 ) {
                return false;
            }

            final UUID targetVersion = markCache.get( edge.getTargetNode() );

            //the target Id has been marked for deletion.  It's version is <= to the marked version for deletion,
            // so we need to discard it
            if ( targetVersion != null && UUIDComparator.staticCompare( edgeVersion, targetVersion ) < 1 ) {
                return false;
            }


            return true;
        }
    }
}
