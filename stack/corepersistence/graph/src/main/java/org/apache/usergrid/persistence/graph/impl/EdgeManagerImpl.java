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
import java.util.UUID;
import java.util.concurrent.ExecutionException;

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
import org.apache.usergrid.persistence.graph.serialization.EdgeMetadataSerialization;
import org.apache.usergrid.persistence.graph.serialization.EdgeSerialization;
import org.apache.usergrid.persistence.graph.serialization.NodeSerialization;
import org.apache.usergrid.persistence.graph.serialization.impl.parse.ObservableIterator;
import org.apache.usergrid.persistence.graph.serialization.util.EdgeUtils;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.fasterxml.uuid.UUIDComparator;
import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import rx.Observable;
import rx.Scheduler;
import rx.util.functions.Func1;


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

    private final GraphFig graphFig;


    @Inject
    public EdgeManagerImpl( final Scheduler scheduler, final EdgeMetadataSerialization edgeMetadataSerialization,
                            final EdgeSerialization edgeSerialization, final NodeSerialization nodeSerialization,
                            final GraphFig graphFig, @Assisted final OrganizationScope scope ) {
        this.scheduler = scheduler;
        this.edgeMetadataSerialization = edgeMetadataSerialization;
        this.edgeSerialization = edgeSerialization;
        this.nodeSerialization = nodeSerialization;
        this.graphFig = graphFig;


        ValidationUtils.validateOrganizationScope( scope );


        this.scope = scope;
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

                try {
                    mutation.execute();
                }
                catch ( ConnectionException e ) {
                    throw new RuntimeException( "Unable to connect to cassandra", e );
                }

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

                try {
                    edgeMutation.execute();
                }
                catch ( ConnectionException e ) {
                    throw new RuntimeException( "Unable to connect to cassandra", e );
                }

                return edge;
            }
        } );

        //TODO, fork the background repair scheduling here
    }


    @Override
    public Observable<Id> deleteNode( final Id node ) {
        return Observable.from( node ).subscribeOn( scheduler ).map( new Func1<Id, Id>() {
            @Override
            public Id call( final Id id ) {

                //mark the node as deleted
                final UUID deleteTime = UUIDGenerator.newTimeUUID();

                final MutationBatch nodeMutation = nodeSerialization.mark( scope, id, deleteTime );

                try {
                    nodeMutation.execute();
                }
                catch ( ConnectionException e ) {
                    throw new RuntimeException( "Unable to connect to cassandra", e );
                }

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
        } ).buffer( graphFig.getScanPageSize() ).flatMap( new Func1<List<MarkedEdge>, Observable<MarkedEdge>>() {
            @Override
            public Observable<MarkedEdge> call( final List<MarkedEdge> markedEdges ) {
                return Observable.from( markedEdges ).filter( new EdgeFilter( search.getMaxVersion() ) );
            }
        } )


                //                .filter( new EdgeFilter( search.getMaxVersion() ) )
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
        } ).filter( new EdgeFilter( search.getMaxVersion() ) )
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
        } ).filter( new EdgeFilter( search.getMaxVersion() ) ).takeFirst().cast( Edge.class );
    }


    @Override
    public Observable<Edge> loadEdgesToTargetByType( final SearchByIdType search ) {
        return Observable.create( new ObservableIterator<MarkedEdge>() {
            @Override
            protected Iterator<MarkedEdge> getIterator() {
                return edgeSerialization.getEdgesToTargetBySourceType( scope, search );
            }
        } ).filter( new EdgeFilter( search.getMaxVersion() ) ).takeFirst().cast( Edge.class );
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
     * Filter the returned values based on the max uuid and if it's been marked for deletion or not
     */
    private class EdgeFilter implements Func1<MarkedEdge, Boolean> {

        private final UUID maxVersion;
        private final LoadingCache<Id, Optional<UUID>> markCache =
                CacheBuilder.newBuilder().maximumSize( graphFig.getScanPageSize()*2 ).build( new CacheLoader<Id, Optional<UUID>>() {


                    @Override
                    public Optional<UUID> load( final Id key ) throws Exception {
                        return nodeSerialization.getMaxVersion( scope, key );
                    }
                } );


        private EdgeFilter( final UUID maxVersion ) {
            this.maxVersion = maxVersion;
        }


        @Override
        public Boolean call( final MarkedEdge edge ) {

            final UUID edgeVersion = edge.getVersion();

            //our edge needs to not be deleted and have a version that's > max Version
            if ( edge.isDeleted() || UUIDComparator.staticCompare( edgeVersion, maxVersion ) > 0 ) {
                return false;
            }

            try {
                final Optional<UUID> sourceId = markCache.get( edge.getSourceNode() );

                //the source Id has been marked for deletion.  It's version is <= to the marked version for deletion,
                // so we need to discard it
                if ( sourceId.isPresent() && UUIDComparator.staticCompare( edgeVersion, sourceId.get() ) < 1  ) {
                    return false;
                }

                final Optional<UUID> targetId = markCache.get( edge.getTargetNode() );

                //the target Id has been marked for deletion.  It's version is <= to the marked version for deletion,
                // so we need to discard it
                if ( targetId.isPresent() && UUIDComparator.staticCompare( edgeVersion, targetId.get() ) < 1 ) {
                    return false;
                }
            }
            catch ( ExecutionException e ) {
                throw new RuntimeException( "Unable to evaluate edge for filtering", e );
            }

            return true;
        }
    }
}
