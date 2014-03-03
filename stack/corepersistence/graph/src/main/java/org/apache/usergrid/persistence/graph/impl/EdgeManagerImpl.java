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

import org.apache.usergrid.persistence.collection.OrganizationScope;
import org.apache.usergrid.persistence.collection.mvcc.entity.ValidationUtils;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.EdgeManager;
import org.apache.usergrid.persistence.graph.GraphFig;
import org.apache.usergrid.persistence.graph.MarkedEdge;
import org.apache.usergrid.persistence.graph.SearchByEdgeType;
import org.apache.usergrid.persistence.graph.SearchByIdType;
import org.apache.usergrid.persistence.graph.SearchEdgeType;
import org.apache.usergrid.persistence.graph.SearchIdType;
import org.apache.usergrid.persistence.graph.consistency.AsyncProcessor;
import org.apache.usergrid.persistence.graph.consistency.AsynchronousMessage;
import org.apache.usergrid.persistence.graph.consistency.MessageListener;
import org.apache.usergrid.persistence.graph.guice.EdgeDelete;
import org.apache.usergrid.persistence.graph.guice.EdgeWrite;
import org.apache.usergrid.persistence.graph.guice.NodeDelete;
import org.apache.usergrid.persistence.graph.serialization.EdgeMetadataSerialization;
import org.apache.usergrid.persistence.graph.serialization.EdgeSerialization;
import org.apache.usergrid.persistence.graph.serialization.NodeSerialization;
import org.apache.usergrid.persistence.graph.serialization.impl.parse.ObservableIterator;
import org.apache.usergrid.persistence.graph.serialization.util.EdgeUtils;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.fasterxml.uuid.UUIDComparator;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import rx.Observable;
import rx.Scheduler;
import rx.util.functions.Action1;
import rx.util.functions.Func1;
import rx.util.functions.Func4;


/**
 *
 *
 */
public class EdgeManagerImpl implements EdgeManager {


    private final OrganizationScope scope;

    private final Scheduler scheduler;

    private final EdgeMetadataSerialization edgeMetadataSerialization;


    private final EdgeSerialization edgeSerialization;

    private final NodeSerialization nodeSerialization;

    private final AsyncProcessor<Edge> edgeWriteAsyncProcessor;
    private final AsyncProcessor<Edge> edgeDeleteAsyncProcessor;
    private final AsyncProcessor<Id> nodeDeleteAsyncProcessor;

    private final GraphFig graphFig;


    @Inject
    public EdgeManagerImpl( final Scheduler scheduler, final EdgeMetadataSerialization edgeMetadataSerialization,
                            final EdgeSerialization edgeSerialization, final NodeSerialization nodeSerialization,
                            final GraphFig graphFig, @EdgeWrite final AsyncProcessor edgeWrite,
                            @EdgeDelete final AsyncProcessor edgeDelete, @NodeDelete final AsyncProcessor nodeDelete,
                            @Assisted final OrganizationScope scope ) {

        ValidationUtils.validateOrganizationScope( scope );


        this.scope = scope;
        this.scheduler = scheduler;
        this.edgeMetadataSerialization = edgeMetadataSerialization;
        this.edgeSerialization = edgeSerialization;
        this.nodeSerialization = nodeSerialization;
        this.graphFig = graphFig;


        this.edgeWriteAsyncProcessor = edgeWrite;


        this.edgeWriteAsyncProcessor.addListener( new EdgeWriteListener() );


        this.edgeDeleteAsyncProcessor = edgeDelete;

        this.edgeDeleteAsyncProcessor.addListener( new EdgeDeleteListener() );

        this.nodeDeleteAsyncProcessor = nodeDelete;

        this.nodeDeleteAsyncProcessor.addListener( new NodeDeleteListener() );
    }


    @Override
    public Observable<Edge> writeEdge( final Edge edge ) {
        EdgeUtils.validateEdge( edge );

        return Observable.from( edge ).subscribeOn( scheduler ).map( new Func1<Edge, Edge>() {
            @Override
            public Edge call( final Edge edge ) {
                final MutationBatch mutation = edgeMetadataSerialization.writeEdge( scope, edge );

                final MutationBatch edgeMutation = edgeSerialization.writeEdge( scope, edge );

                mutation.mergeShallow( edgeMutation );

                final AsynchronousMessage<Edge> event = edgeWriteAsyncProcessor.setVerification( edge, getTimeout() );

                try {
                    mutation.execute();
                }
                catch ( ConnectionException e ) {
                    throw new RuntimeException( "Unable to connect to cassandra", e );
                }

                edgeWriteAsyncProcessor.start( event );

                return edge;
            }
        } );
    }


    @Override
    public Observable<Edge> deleteEdge( final Edge edge ) {
        EdgeUtils.validateEdge( edge );

        return Observable.from( edge ).subscribeOn( scheduler ).map( new Func1<Edge, Edge>() {
            @Override
            public Edge call( final Edge edge ) {
                final MutationBatch edgeMutation = edgeSerialization.markEdge( scope, edge );

                final AsynchronousMessage<Edge> event = edgeDeleteAsyncProcessor.setVerification( edge, getTimeout() );


                try {
                    edgeMutation.execute();
                }
                catch ( ConnectionException e ) {
                    throw new RuntimeException( "Unable to connect to cassandra", e );
                }

                edgeDeleteAsyncProcessor.start( event );


                return edge;
            }
        } );
    }


    @Override
    public Observable<Id> deleteNode( final Id node ) {
        return Observable.from( node ).subscribeOn( scheduler ).map( new Func1<Id, Id>() {
            @Override
            public Id call( final Id id ) {

                //mark the node as deleted
                final UUID deleteTime = UUIDGenerator.newTimeUUID();

                final MutationBatch nodeMutation = nodeSerialization.mark( scope, id, deleteTime );

                final AsynchronousMessage<Id> event = nodeDeleteAsyncProcessor.setVerification( node, getTimeout() );


                try {
                    nodeMutation.execute();
                }
                catch ( ConnectionException e ) {
                    throw new RuntimeException( "Unable to connect to cassandra", e );
                }

                nodeDeleteAsyncProcessor.start( event );

                return id;
            }
        } );
    }


    @Override
    public Observable<Edge> loadEdgesFromSource( final SearchByEdgeType search ) {
        return Observable.create( new ObservableIterator<MarkedEdge>() {
            @Override
            protected Iterator<MarkedEdge> getIterator() {
                return edgeSerialization.getEdgesFromSource( scope, search );
            }
        } ).buffer( graphFig.getScanPageSize() ).flatMap( new EdgeBufferFilter( search.getMaxVersion() ) )
                //we intentionally use distinct until changed.  This way we won't store all the keys since this
                //would hog far too much ram.
                .distinctUntilChanged( new Func1<Edge, Id>() {
                    @Override
                    public Id call( final Edge edge ) {
                        return edge.getTargetNode();
                    }
                } ).cast( Edge.class );
    }


    @Override
    public Observable<Edge> loadEdgesToTarget( final SearchByEdgeType search ) {
        return Observable.create( new ObservableIterator<MarkedEdge>() {
            @Override
            protected Iterator<MarkedEdge> getIterator() {
                return edgeSerialization.getEdgesToTarget( scope, search );
            }
        } ).buffer( graphFig.getScanPageSize() ).flatMap( new EdgeBufferFilter( search.getMaxVersion() ) )
                //we intentionally use distinct until changed.  This way we won't store all the keys since this
                //would hog far too much ram.
                .distinctUntilChanged( new Func1<Edge, Id>() {
                    @Override
                    public Id call( final Edge edge ) {
                        return edge.getSourceNode();
                    }
                } ).cast( Edge.class );
    }


    @Override
    public Observable<Edge> loadEdgesFromSourceByType( final SearchByIdType search ) {
        return Observable.create( new ObservableIterator<MarkedEdge>() {
            @Override
            protected Iterator<MarkedEdge> getIterator() {
                return edgeSerialization.getEdgesFromSourceByTargetType( scope, search );
            }
        } ).buffer( graphFig.getScanPageSize() ).flatMap( new EdgeBufferFilter( search.getMaxVersion() ) )
                         .distinctUntilChanged( new Func1<Edge, Id>() {
                             @Override
                             public Id call( final Edge edge ) {
                                 return edge.getTargetNode();
                             }
                         } )

                         .cast( Edge.class );
    }


    @Override
    public Observable<Edge> loadEdgesToTargetByType( final SearchByIdType search ) {
        return Observable.create( new ObservableIterator<MarkedEdge>() {
            @Override
            protected Iterator<MarkedEdge> getIterator() {
                return edgeSerialization.getEdgesToTargetBySourceType( scope, search );
            }
        } ).buffer( graphFig.getScanPageSize() ).flatMap( new EdgeBufferFilter( search.getMaxVersion() ) )
                         .distinctUntilChanged( new Func1<Edge, Id>() {
                             @Override
                             public Id call( final Edge edge ) {
                                 return edge.getSourceNode();
                             }
                         } ).cast( Edge.class );
    }


    @Override
    public Observable<String> getEdgeTypesFromSource( final SearchEdgeType search ) {

        return Observable.create( new ObservableIterator<String>() {
            @Override
            protected Iterator<String> getIterator() {
                return edgeMetadataSerialization.getEdgeTypesFromSource( scope, search );
            }
        } );
    }


    @Override
    public Observable<String> getIdTypesFromSource( final SearchIdType search ) {
        return Observable.create( new ObservableIterator<String>() {
            @Override
            protected Iterator<String> getIterator() {
                return edgeMetadataSerialization.getIdTypesFromSource( scope, search );
            }
        } );
    }


    @Override
    public Observable<String> getEdgeTypesToTarget( final SearchEdgeType search ) {

        return Observable.create( new ObservableIterator<String>() {
            @Override
            protected Iterator<String> getIterator() {
                return edgeMetadataSerialization.getEdgeTypesToTarget( scope, search );
            }
        } );
    }


    @Override
    public Observable<String> getIdTypesToTarget( final SearchIdType search ) {
        return Observable.create( new ObservableIterator<String>() {
            @Override
            protected Iterator<String> getIterator() {
                return edgeMetadataSerialization.getIdTypesToTarget( scope, search );
            }
        } );
    }


    /**
     * Get our timeout for write consistency
     */
    private long getTimeout() {
        return graphFig.getWriteTimeout() * 2;
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
            return Observable.from( markedEdges ).subscribeOn( scheduler )
                             .filter( new EdgeFilter( this.maxVersion, markedVersions ) );
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


    /**
     * Construct the asynchronous edge lister for the repair operation.
     */
    public class EdgeWriteListener implements MessageListener<Edge, Edge> {

        @Override
        public Observable<Edge> receive( final Edge write ) {

            final UUID maxVersion = write.getVersion();

            return Observable.create( new ObservableIterator<MarkedEdge>() {
                @Override
                protected Iterator<MarkedEdge> getIterator() {

                    final SimpleSearchByEdge search =
                            new SimpleSearchByEdge( write.getSourceNode(), write.getType(), write.getTargetNode(),
                                    maxVersion, null );

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
            } ).buffer( graphFig.getScanPageSize() ).map( new Func1<List<MarkedEdge>, Edge>() {
                @Override
                public Edge call( final List<MarkedEdge> markedEdges ) {

                    final int size = markedEdges.size();

                    final MutationBatch batch = edgeSerialization.deleteEdge( scope, markedEdges.get( 0 ) );

                    for ( int i = 1; i < size; i++ ) {
                        final MutationBatch delete = edgeSerialization.deleteEdge( scope, markedEdges.get( i ) );

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


    /**
     * Construct the asynchronous delete operation from the listener
     */
    public class EdgeDeleteListener implements MessageListener<Edge, Edge> {

        @Override
        public Observable<Edge> receive( final Edge delete ) {

            final UUID maxVersion = delete.getVersion();

            return Observable.from( delete ).flatMap( new Func1<Edge, Observable<MutationBatch>>() {
                @Override
                public Observable<MutationBatch> call( final Edge edge ) {

                    //search by edge type and target type.  If any other edges with this target type exist,
                    // we can't delete it
                    Observable<MutationBatch> sourceIdType = loadEdgesFromSourceByType(
                            new SimpleSearchByIdType( edge.getSourceNode(), edge.getType(), maxVersion,
                                    edge.getTargetNode().getType(), null ) ).take( 2 ).count()
                            .map( new Func1<Integer, MutationBatch>() {
                                @Override
                                public MutationBatch call( final Integer count ) {
                                    //There's nothing to do, we have 2 different edges with the same edge type and
                                    // target type.  Don't delete meta data
                                    if ( count == 2 ) {
                                        return null;
                                    }

                                    return edgeMetadataSerialization.removeEdgeTypeFromSource( scope, delete );
                                }
                            } );


                    Observable<MutationBatch> targetIdType = loadEdgesToTargetByType(
                            new SimpleSearchByIdType( edge.getTargetNode(), edge.getType(), maxVersion,
                                    edge.getSourceNode().getType(), null ) ).take( 2 ).count()
                            .map( new Func1<Integer, MutationBatch>() {
                                @Override
                                public MutationBatch call( final Integer count ) {
                                    //There's nothing to do, we have 2 different edges with the same edge type and
                                    // target type.  Don't delete meta data
                                    if ( count == 2 ) {
                                        return null;
                                    }


                                    return edgeMetadataSerialization.removeEdgeTypeToTarget( scope, delete );
                                }
                            } );

                    //search by edge type and target type.  If any other edges with this target type exist,
                    // we can't delete it
                    Observable<MutationBatch> sourceType = loadEdgesFromSource(
                            new SimpleSearchByEdgeType( edge.getSourceNode(), edge.getType(), maxVersion, null ) )
                            .take( 2 ).count().map( new Func1<Integer, MutationBatch>() {
                                @Override
                                public MutationBatch call( final Integer count ) {


                                    //There's nothing to do, we have 2 different edges with the same edge type and
                                    // target type.  Don't delete meta data
                                    if ( count == 2 ) {
                                        return null;
                                    }


                                    return edgeMetadataSerialization.removeEdgeTypeFromSource( scope, delete );
                                }
                            } );


                    Observable<MutationBatch> targetType = loadEdgesToTarget(
                            new SimpleSearchByEdgeType( edge.getTargetNode(), edge.getType(), maxVersion, null ) )
                            .take( 2 ).count().map( new Func1<Integer, MutationBatch>() {


                                @Override
                                public MutationBatch call( final Integer count ) {


                                    //There's nothing to do, we have 2 different edges with the same edge type and
                                    // target type.  Don't delete meta data
                                    if ( count == 2 ) {
                                        return null;
                                    }

                                    return edgeMetadataSerialization.removeEdgeTypeToTarget( scope, delete );
                                }
                            } );


                    return Observable.zip( sourceIdType, targetIdType, sourceType, targetType,
                            new Func4<MutationBatch, MutationBatch, MutationBatch, MutationBatch, MutationBatch>() {


                                @Override
                                public MutationBatch call( final MutationBatch mutationBatch,
                                                           final MutationBatch mutationBatch2,
                                                           final MutationBatch mutationBatch3,
                                                           final MutationBatch mutationBatch4 ) {

                                    return join( join( join( mutationBatch, mutationBatch2 ), mutationBatch3 ),
                                            mutationBatch4 );
                                }


                                private MutationBatch join( MutationBatch first, MutationBatch second ) {
                                    if ( first == null ) {
                                        if ( second == null ) {
                                            return null;
                                        }

                                        return second;
                                    }


                                    else if ( second == null ) {
                                        return first;
                                    }

                                    first.mergeShallow( second );

                                    return first;
                                }
                            } );
                }
            } ).map( new Func1<MutationBatch, Edge>() {
                @Override
                public Edge call( final MutationBatch mutationBatch ) {
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


    /**
     * Construct the asynchronous node delete from the q
     */
    public class NodeDeleteListener implements MessageListener<Id, Id> {

        @Override
        public Observable<Id> receive( final Id node ) {


            return Observable.from( node ).map( new Func1<Id, Optional<UUID>>() {
                @Override
                public Optional<UUID> call( final Id id ) {
                    return nodeSerialization.getMaxVersion( scope, node );
                }
            } ).flatMap( new Func1<Optional<UUID>, Observable<Edge>>() {
                @Override
                public Observable<Edge> call( final Optional<UUID> uuidOptional ) {
                    return getEdgeTypesToTarget( new SimpleSearchEdgeType( node, null ) )
                            .flatMap( new Func1<String, Observable<Edge>>() {
                                @Override
                                public Observable<Edge> call( final String edgeType ) {

                                    //for each edge type, we want to search all edges < this version to the node and
                                    // delete them. We might want to batch this up for efficiency
                                    return loadEdgesToTarget(
                                            new SimpleSearchByEdgeType( node, edgeType, uuidOptional.get(), null ) )
                                            .doOnEach( new Action1<Edge>() {
                                                @Override
                                                public void call( final Edge edge ) {
                                                    deleteEdge( edge );
                                                }
                                            } );
                                }
                            } );
                }
            } ).map( new Func1<Edge, Id>() {
                @Override
                public Id call( final Edge edge ) {
                    return node;
                }
            } );
        }
    }
}
