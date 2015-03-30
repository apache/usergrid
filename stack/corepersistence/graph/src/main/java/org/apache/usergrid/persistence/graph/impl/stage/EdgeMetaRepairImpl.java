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

package org.apache.usergrid.persistence.graph.impl.stage;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.core.rx.ObservableIterator;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.util.ValidationUtils;
import org.apache.usergrid.persistence.graph.GraphFig;
import org.apache.usergrid.persistence.graph.MarkedEdge;
import org.apache.usergrid.persistence.graph.SearchByEdgeType;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchByIdType;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchIdType;
import org.apache.usergrid.persistence.graph.serialization.EdgeMetadataSerialization;
import org.apache.usergrid.persistence.graph.serialization.EdgeSerialization;
import org.apache.usergrid.persistence.graph.serialization.util.GraphValidation;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.observables.MathObservable;


/**
 * Implementation of the cleanup of edge meta data
 */
@Singleton
public class EdgeMetaRepairImpl implements EdgeMetaRepair {


    private static final Logger LOG = LoggerFactory.getLogger( EdgeMetaRepairImpl.class );
    private static final Log RX_LOG = new Log();

    private final EdgeMetadataSerialization edgeMetadataSerialization;
    private final EdgeSerialization storageEdgeSerialization;
    private final Keyspace keyspace;
    private final GraphFig graphFig;


    @Inject
    public EdgeMetaRepairImpl( final EdgeMetadataSerialization edgeMetadataSerialization, final Keyspace keyspace,
                               final GraphFig graphFig, final EdgeSerialization storageEdgeSerialization ) {


        Preconditions.checkNotNull( "edgeMetadataSerialization is required", edgeMetadataSerialization );
        Preconditions.checkNotNull( "storageEdgeSerialization is required", storageEdgeSerialization );
        Preconditions.checkNotNull( "consistencyFig is required", graphFig );
        Preconditions.checkNotNull( "cassandraConfig is required", graphFig );
        Preconditions.checkNotNull( "keyspace is required", keyspace );

        this.edgeMetadataSerialization = edgeMetadataSerialization;
        this.keyspace = keyspace;
        this.graphFig = graphFig;
        this.storageEdgeSerialization = storageEdgeSerialization;
    }


    @Override
    public Observable<Integer> repairSources( final ApplicationScope scope, final Id sourceId, final String edgeType,
                                              final long maxTimestamp ) {


        return clearTypes( scope, sourceId, edgeType, maxTimestamp, source );
    }


    @Override
    public Observable<Integer> repairTargets( final ApplicationScope scope, final Id targetId, final String edgeType,
                                              final long maxTimestamp ) {
        return clearTypes( scope, targetId, edgeType, maxTimestamp, target );
    }


    private Observable<Integer> clearTypes( final ApplicationScope scope, final Id node, final String edgeType,
                                            final long maxTimestamp, final CleanSerialization serialization ) {

        ValidationUtils.validateApplicationScope( scope );
        ValidationUtils.verifyIdentity( node );
        Preconditions.checkNotNull( edgeType, "edge type is required" );
        GraphValidation.validateTimestamp( maxTimestamp, "maxTimestamp" );
        Preconditions.checkNotNull( serialization, "serialization is required" );


        Observable<Integer> deleteCounts = serialization.loadEdgeSubTypes( scope, node, edgeType, maxTimestamp ).buffer(
                graphFig.getRepairConcurrentSize() )
                //buffer them into concurrent groups based on the concurrent repair size
                .flatMap( new Func1<List<String>, Observable<Integer>>() {

                    @Override
                    public Observable<Integer> call( final List<String> types ) {


                        final MutationBatch batch = keyspace.prepareMutationBatch();

                        final List<Observable<Integer>> checks = new ArrayList<Observable<Integer>>( types.size() );

                        //for each id type, check if the exist in parallel to increase processing speed
                        for ( final String subType : types ) {

                            LOG.debug( "Checking for edges with nodeId {}, type {}, and subtype {}", node, edgeType,
                                    subType );

                            Observable<Integer> search =
                                    //load each edge in it's own thread
                                    serialization.loadEdges( scope, node, edgeType, subType, maxTimestamp )
                                                 .doOnNext( RX_LOG ).take( 1 ).count()
                                                 .doOnNext( new Action1<Integer>() {

                                                     @Override
                                                     public void call( final Integer count ) {
                                                         /**
                                                          * we only want to delete if no edges are in this class. If
                                                          * there
                                                          * are
                                                          * still edges
                                                          * we must retain the information in order to keep our index
                                                          * structure
                                                          * correct for edge
                                                          * iteration
                                                          **/
                                                         if ( count != 0 ) {
                                                             LOG.debug( "Found edge with nodeId {}, type {}, "
                                                                             + "and subtype {}. Not removing subtype. ",
                                                                     node, edgeType, subType );
                                                             return;
                                                         }


                                                         LOG.debug( "No edges with nodeId {}, type {}, "
                                                                         + "and subtype {}. Removing subtype.", node,
                                                                 edgeType, subType );
                                                         batch.mergeShallow( serialization
                                                                 .removeEdgeSubType( scope, node, edgeType, subType,
                                                                         maxTimestamp ) );
                                                     }
                                                 } );

                            checks.add( search );
                        }


                        /**
                         * Sum up the total number of edges we had, then execute the mutation if we have
                         * anything to do
                         */


                        return MathObservable.sumInteger( Observable.merge( checks ) )
                                             .doOnNext( new Action1<Integer>() {
                                                            @Override
                                                            public void call( final Integer count ) {


                                                                LOG.debug(
                                                                        "Executing batch for subtype deletion with " +
                                                                                "type {}.  "
                                                                                + "Mutation has {} rows to mutate ",
                                                                        edgeType, batch.getRowCount() );

                                                                try {
                                                                    batch.execute();
                                                                }
                                                                catch ( ConnectionException e ) {
                                                                    throw new RuntimeException( "Unable to connect to casandra", e );
                                                                }
                                                            }
                                                        }


                                                      );
                    }
                } )
                        //if we get no edges, emit a 0 so the caller knows we can delete the type
                .defaultIfEmpty( 0 );


        //sum up everything emitted by sub types.  If there's no edges existing for all sub types,
        // then we can safely remove them
        return MathObservable.sumInteger( deleteCounts ).lastOrDefault( 0 ).doOnNext( new Action1<Integer>() {

            @Override
            public void call( final Integer subTypeUsedCount ) {

                /**
                 * We can only execute deleting this type if no sub types were deleted
                 */
                if ( subTypeUsedCount != 0 ) {
                    LOG.debug( "Type {} has {} subtypes in use as of maxTimestamp {}.  Not deleting type.", edgeType,
                            subTypeUsedCount, maxTimestamp );
                    return;
                }


                LOG.debug( "Type {} has no subtypes in use as of maxTimestamp {}.  Deleting type.", edgeType,
                        maxTimestamp );
                try {
                    serialization.removeEdgeType( scope, node, edgeType, maxTimestamp ).execute();
                }
                catch ( ConnectionException e ) {
                    throw new RuntimeException( "Unable to connect to casandra", e );
                }
            }
        } );
    }


    /**
     * Simple edge serialization
     */
    private static interface CleanSerialization {

        /**
         * Load all subtypes for the edge with a maxTimestamp <= the provided maxTimestamp
         */
        Observable<String> loadEdgeSubTypes( final ApplicationScope scope, final Id nodeId, final String type,
                                             final long maxTimestamp );


        /**
         * Load an observable with edges from the details provided
         */
        Observable<MarkedEdge> loadEdges( final ApplicationScope scope, final Id nodeId, final String edgeType,
                                          final String subType, final long maxTimestamp );

        /**
         * Remove the sub type specified
         */
        MutationBatch removeEdgeSubType( final ApplicationScope scope, final Id nodeId, final String edgeType,
                                         final String subType, final long maxTimestamp );

        /**
         * Remove the edge type
         */
        MutationBatch removeEdgeType( final ApplicationScope scope, final Id nodeId, final String type,
                                      final long maxTimestamp );
    }


    /**
     * Target serialization i/o for cleaning target edges
     */
    private final CleanSerialization target = new CleanSerialization() {


        @Override
        public Observable<String> loadEdgeSubTypes( final ApplicationScope scope, final Id nodeId,
                                                    final String edgeType, final long maxTimestamp ) {


            return Observable.create( new ObservableIterator<String>( "edgeTargetIdTypes" ) {
                @Override
                protected Iterator<String> getIterator() {
                    return edgeMetadataSerialization
                            .getIdTypesToTarget( scope, new SimpleSearchIdType( nodeId, edgeType, null, null ) );
                }
            } );
        }


        @Override
        public Observable<MarkedEdge> loadEdges( final ApplicationScope scope, final Id nodeId, final String edgeType,
                                                 final String subType, final long maxTimestamp ) {

            return Observable.create( new ObservableIterator<MarkedEdge>( "getEdgeTypesFromSource" ) {
                @Override
                protected Iterator<MarkedEdge> getIterator() {
                    return storageEdgeSerialization.getEdgesToTargetBySourceType( scope,
                            new SimpleSearchByIdType( nodeId, edgeType, maxTimestamp, SearchByEdgeType.Order.DESCENDING, subType,   null ) );
                }
            } );
        }


        @Override
        public MutationBatch removeEdgeSubType( final ApplicationScope scope, final Id nodeId, final String type,
                                                final String subType, final long maxTimestamp ) {
            return edgeMetadataSerialization.removeIdTypeToTarget( scope, nodeId, type, subType, maxTimestamp );
        }


        @Override
        public MutationBatch removeEdgeType( final ApplicationScope scope, final Id nodeId, final String type,
                                             final long maxTimestamp ) {
            return edgeMetadataSerialization.removeEdgeTypeToTarget( scope, nodeId, type, maxTimestamp );
        }
    };

    /**
     * Target serialization i/o for cleaning target edges
     */
    private final CleanSerialization source = new CleanSerialization() {

        @Override
        public Observable<String> loadEdgeSubTypes( final ApplicationScope scope, final Id nodeId,
                                                    final String edgeType, final long maxTimestamp ) {
            return Observable.create( new ObservableIterator<String>( "edgeSourceIdTypes" ) {
                @Override
                protected Iterator<String> getIterator() {
                    return edgeMetadataSerialization
                            .getIdTypesFromSource( scope, new SimpleSearchIdType( nodeId, edgeType, null, null ) );
                }
            } );
        }


        @Override
        public Observable<MarkedEdge> loadEdges( final ApplicationScope scope, final Id nodeId, final String edgeType,
                                                 final String subType, final long maxTimestamp ) {

            return Observable.create( new ObservableIterator<MarkedEdge>( "getEdgeTypesFromSource" ) {
                @Override
                protected Iterator<MarkedEdge> getIterator() {
                    return storageEdgeSerialization.getEdgesFromSourceByTargetType( scope,
                            new SimpleSearchByIdType( nodeId, edgeType, maxTimestamp, SearchByEdgeType.Order.DESCENDING, subType, null ) );
                }
            } );
        }


        @Override
        public MutationBatch removeEdgeSubType( final ApplicationScope scope, final Id nodeId, final String type,
                                                final String subType, final long maxTimestamp ) {
            return edgeMetadataSerialization.removeIdTypeFromSource( scope, nodeId, type, subType, maxTimestamp );
        }


        @Override
        public MutationBatch removeEdgeType( final ApplicationScope scope, final Id nodeId, final String type,
                                             final long maxTimestamp ) {
            return edgeMetadataSerialization.removeEdgeTypeFromSource( scope, nodeId, type, maxTimestamp );
        }
    };


    private static class Log implements Action1<MarkedEdge> {


        @Override
        public void call( final MarkedEdge markedEdge ) {
            LOG.debug( "Emitting edge {}", markedEdge );
        }
    }
}
