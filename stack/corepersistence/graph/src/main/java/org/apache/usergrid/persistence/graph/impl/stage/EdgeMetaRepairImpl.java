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
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.collection.OrganizationScope;
import org.apache.usergrid.persistence.collection.mvcc.entity.ValidationUtils;
import org.apache.usergrid.persistence.graph.GraphFig;
import org.apache.usergrid.persistence.graph.MarkedEdge;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchByIdType;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchIdType;
import org.apache.usergrid.persistence.graph.serialization.EdgeMetadataSerialization;
import org.apache.usergrid.persistence.graph.serialization.EdgeSerialization;
import org.apache.usergrid.persistence.graph.serialization.impl.parse.ObservableIterator;
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
import rx.schedulers.Schedulers;


/**
 * Implementation of the cleanup of edge meta data
 */
@Singleton
public class EdgeMetaRepairImpl implements EdgeMetaRepair {


    private static final Logger LOG = LoggerFactory.getLogger( EdgeMetaRepairImpl.class );
    private final EdgeMetadataSerialization edgeMetadataSerialization;
    private final EdgeSerialization edgeSerialization;
    private final Keyspace keyspace;
    private final GraphFig graphFig;


    @Inject
    public EdgeMetaRepairImpl( final EdgeMetadataSerialization edgeMetadataSerialization,
                               final EdgeSerialization edgeSerialization, final Keyspace keyspace,
                               final GraphFig graphFig ) {

        Preconditions.checkNotNull( "edgeMetadataSerialization is required", edgeMetadataSerialization );
        Preconditions.checkNotNull( "edgeSerialization is required", edgeSerialization );
        Preconditions.checkNotNull( "graphFig is required", graphFig );
        Preconditions.checkNotNull( "keyspace is required", keyspace );

        this.edgeMetadataSerialization = edgeMetadataSerialization;
        this.edgeSerialization = edgeSerialization;
        this.keyspace = keyspace;
        this.graphFig = graphFig;
    }


    @Override
    public Observable<Integer> repairSources( final OrganizationScope scope, final Id sourceId, final String edgeType,
                                              final UUID version ) {


        return clearTypes( scope, sourceId, edgeType, version, source );
    }


    @Override
    public Observable<Integer> repairTargets( final OrganizationScope scope, final Id targetId, final String edgeType,
                                              final UUID version ) {
        return clearTypes( scope, targetId, edgeType, version, target );
    }


    private Observable<Integer> clearTypes( final OrganizationScope scope, final Id node, final String edgeType,
                                            final UUID version, final CleanSerialization serialization ) {

        ValidationUtils.validateOrganizationScope( scope );
        ValidationUtils.verifyIdentity( node );
        Preconditions.checkNotNull( edgeType, "edge type is required" );
        Preconditions.checkNotNull( version, "version is required" );


        Observable<Integer> deleteCounts = serialization.loadEdgeSubTypes( scope, node, edgeType, version )
                .buffer( graphFig.getRepairConcurrentSize() )
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
                                    serialization.loadEdges( scope, node, edgeType, subType, version )
                                                 .subscribeOn( Schedulers.io() ).take( 1 ).count()
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
                                                             LOG.debug(
                                                                     "Found edge with nodeId {}, type {}, " +
                                                                             "and subtype {}. Not removing subtype. ",
                                                                     node, edgeType, subType );
                                                             return;
                                                         }


                                                         LOG.debug(
                                                                 "No edges with nodeId {}, type {}, " +
                                                                         "and subtype {}. Removing subtype.",
                                                                 node, edgeType, subType );
                                                         batch.mergeShallow( serialization
                                                                 .removeEdgeSubType( scope, node, edgeType, subType,
                                                                         version ) );
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


                                                     LOG.debug( "Executing batch for subtype deletion with type {}.  "
                                                             + "Mutation has {} rows to mutate ", edgeType,
                                                             batch.getRowCount() );

                                                     try {
                                                         batch.execute();
                                                     }
                                                     catch ( ConnectionException e ) {
                                                         throw new RuntimeException( "Unable to execute mutation", e );
                                                     }
                                                 }
                                             } );
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
                    LOG.debug( "Type {} has {} subtypes in use as of version {}.  Not deleting type.", edgeType,
                            subTypeUsedCount, version );
                    return;
                }

                try {

                    LOG.debug( "Type {} has no subtypes in use as of version {}.  Deleting type.", edgeType, version );
                    serialization.removeEdgeType( scope, node, edgeType, version ).execute();
                }
                catch ( ConnectionException e ) {
                    throw new RuntimeException( "Unable to execute mutation" );
                }
            }
        } );
    }


    /**
     * Simple edge serialization
     */
    private static interface CleanSerialization {

        /**
         * Load all subtypes for the edge with a version <= the provided version
         */
        Observable<String> loadEdgeSubTypes( final OrganizationScope scope, final Id nodeId, final String type,
                                             final UUID version );


        /**
         * Load an observable with edges from the details provided
         */
        Observable<MarkedEdge> loadEdges( final OrganizationScope scope, final Id nodeId, final String edgeType,
                                          final String subType, final UUID version );

        /**
         * Remove the sub type specified
         */
        MutationBatch removeEdgeSubType( final OrganizationScope scope, final Id nodeId, final String edgeType,
                                         final String subType, final UUID version );

        /**
         * Remove the edge type
         */
        MutationBatch removeEdgeType( final OrganizationScope scope, final Id nodeId, final String type,
                                      final UUID version );
    }


    /**
     * Target serialization i/o for cleaning target edges
     */
    private final CleanSerialization target = new CleanSerialization() {

        @Override
        public Observable<String> loadEdgeSubTypes( final OrganizationScope scope, final Id nodeId,
                                                    final String edgeType, final UUID version ) {
            return Observable.create( new ObservableIterator<String>( "edgeTargetIdTypes" ) {
                @Override
                protected Iterator<String> getIterator() {
                    return edgeMetadataSerialization
                            .getIdTypesToTarget( scope, new SimpleSearchIdType( nodeId, edgeType, null ) );
                }
            } );
        }


        @Override
        public Observable<MarkedEdge> loadEdges( final OrganizationScope scope, final Id nodeId, final String edgeType,
                                                 final String subType, final UUID version ) {
            return Observable.create( new ObservableIterator<MarkedEdge>( "edgeTargetSubTypes" ) {
                @Override
                protected Iterator<MarkedEdge> getIterator() {
                    return edgeSerialization.getEdgesToTargetBySourceType( scope,
                            new SimpleSearchByIdType( nodeId, edgeType, version, subType, null ) );
                }
            } );
        }


        @Override
        public MutationBatch removeEdgeSubType( final OrganizationScope scope, final Id nodeId, final String type,
                                                final String subType, final UUID version ) {
            return edgeMetadataSerialization.removeIdTypeToTarget( scope, nodeId, type, subType, version );
        }


        @Override
        public MutationBatch removeEdgeType( final OrganizationScope scope, final Id nodeId, final String type,
                                             final UUID version ) {
            return edgeMetadataSerialization.removeEdgeTypeToTarget( scope, nodeId, type, version );
        }
    };

    /**
     * Target serialization i/o for cleaning target edges
     */
    private final CleanSerialization source = new CleanSerialization() {

        @Override
        public Observable<String> loadEdgeSubTypes( final OrganizationScope scope, final Id nodeId,
                                                    final String edgeType, final UUID version ) {
            return Observable.create( new ObservableIterator<String>( "edgeSourceIdTypes" ) {
                @Override
                protected Iterator<String> getIterator() {
                    return edgeMetadataSerialization
                            .getIdTypesFromSource( scope, new SimpleSearchIdType( nodeId, edgeType, null ) );
                }
            } );
        }


        @Override
        public Observable<MarkedEdge> loadEdges( final OrganizationScope scope, final Id nodeId, final String edgeType,
                                                 final String subType, final UUID version ) {
            return Observable.create( new ObservableIterator<MarkedEdge>( "edgeSourceSubTypes" ) {
                @Override
                protected Iterator<MarkedEdge> getIterator() {
                    return edgeSerialization.getEdgesFromSourceByTargetType( scope,
                            new SimpleSearchByIdType( nodeId, edgeType, version, subType, null ) );
                }
            } );
        }


        @Override
        public MutationBatch removeEdgeSubType( final OrganizationScope scope, final Id nodeId, final String type,
                                                final String subType, final UUID version ) {
            return edgeMetadataSerialization.removeIdTypeFromSource( scope, nodeId, type, subType, version );
        }


        @Override
        public MutationBatch removeEdgeType( final OrganizationScope scope, final Id nodeId, final String type,
                                             final UUID version ) {
            return edgeMetadataSerialization.removeEdgeTypeFromSource( scope, nodeId, type, version );
        }
    };
}
