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

import org.apache.usergrid.persistence.collection.OrganizationScope;
import org.apache.usergrid.persistence.collection.mvcc.entity.ValidationUtils;
import org.apache.usergrid.persistence.graph.GraphFig;
import org.apache.usergrid.persistence.graph.MarkedEdge;
import org.apache.usergrid.persistence.graph.SearchByEdgeType;
import org.apache.usergrid.persistence.graph.SearchByIdType;
import org.apache.usergrid.persistence.graph.SearchEdgeType;
import org.apache.usergrid.persistence.graph.SearchIdType;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchByIdType;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchIdType;
import org.apache.usergrid.persistence.graph.serialization.EdgeMetadataSerialization;
import org.apache.usergrid.persistence.graph.serialization.EdgeSerialization;
import org.apache.usergrid.persistence.graph.serialization.impl.parse.ObservableIterator;
import org.apache.usergrid.persistence.graph.serialization.util.EdgeUtils;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import rx.Observable;
import rx.Scheduler;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.observables.MathObservable;


/**
 * Implementation of the cleanup of edge meta data
 */
@Singleton
public class EdgeAsyncImpl implements EdgeAsync {


    private final EdgeMetadataSerialization edgeMetadataSerialization;
    private final EdgeSerialization edgeSerialization;
    private final Keyspace keyspace;
    private final GraphFig graphFig;
    private final Scheduler scheduler;


    @Inject
    public EdgeAsyncImpl( final EdgeMetadataSerialization edgeMetadataSerialization,
                          final EdgeSerialization edgeSerialization, final Keyspace keyspace, final GraphFig graphFig,
                          final Scheduler scheduler ) {
        this.edgeMetadataSerialization = edgeMetadataSerialization;
        this.edgeSerialization = edgeSerialization;
        this.keyspace = keyspace;
        this.graphFig = graphFig;
        this.scheduler = scheduler;
    }


    @Override
    public Observable<Integer> cleanSources( final OrganizationScope scope, final Id sourceId, final String edgeType,
                                       final UUID version ) {


        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }


    @Override
    public Observable<Integer> clearTargets( final OrganizationScope scope, final Id targetId, final String edgeType,
                                             final UUID version ) {

        ValidationUtils.validateOrganizationScope( scope );
        ValidationUtils.verifyIdentity( targetId );
        Preconditions.checkNotNull( edgeType, "edge type is required" );
        Preconditions.checkNotNull( version, "version is required" );



        return loadEdgeIdsToTarget( scope, new SimpleSearchIdType( targetId, edgeType, null ) )
                .buffer( graphFig.getRepairConcurrentSize() )
                        //buffer them into concurrent groups based on the concurrent repair size
                .flatMap( new Func1<List<String>, Observable<Integer>>() {

                    @Override
                    public Observable<Integer> call( final List<String> types ) {


                        final MutationBatch batch = keyspace.prepareMutationBatch();

                        final List<Observable<Integer>> checks = new ArrayList<Observable<Integer>>( types.size() );

                        //for each id type, check if the exist in parallel to increase processing speed
                        for ( final String sourceIdType : types ) {

                            final SearchByIdType searchData =   new SimpleSearchByIdType( targetId, edgeType, version, sourceIdType, null );

                            Observable<Integer> search = getEdgesToTargetBySourceType( scope, searchData
                                    )
                                    .distinctUntilChanged( new Func1<MarkedEdge, Id>() {

                                        //get distinct by source node
                                        @Override
                                        public Id call( final MarkedEdge markedEdge ) {
                                            return markedEdge.getSourceNode();
                                        }
                                    } ).take( 1 ).count().doOnNext( new Action1<Integer>() {

                                        @Override
                                        public void call( final Integer count ) {
                                            /**
                                             * we only want to delete if no edges are in this class. If there are
                                             * still edges
                                             * we must retain the information in order to keep our index structure
                                             * correct for edge
                                             * iteration
                                             **/
                                            if ( count != 0 ) {
                                                return;
                                            }


                                            batch.mergeShallow( edgeMetadataSerialization
                                                    .removeIdTypeToTarget( scope, targetId, edgeType, sourceIdType,
                                                            version ) );
                                        }
                                    } );

                            checks.add( search );
                        }


                        /**
                         * Sum up the total number of edges we had, then execute the mutation if we have anything to do
                         */
                        return MathObservable.sumInteger(Observable.merge( checks )).doOnNext( new Action1<Integer>() {
                            @Override
                            public void call( final Integer count ) {

                                if(batch.isEmpty()){
                                    return;
                                }

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
                .map( new Func1<Integer, Integer>() {
                    @Override
                    public Integer call( final Integer subTypes ) {

                        /**
                         * We can only execute deleting this type if no sub types were deleted
                         */
                        if(subTypes != 0){
                            return subTypes;
                        }

                        try {
                            edgeMetadataSerialization.removeEdgeTypeToTarget( scope, targetId, edgeType, version )
                                                         .execute();
                        }
                        catch ( ConnectionException e ) {
                            throw new RuntimeException( "Unable to execute mutation" );
                        }

                        return subTypes;
                    }
                } )
                //if we get no edges, emit a 0 so the caller knows nothing was deleted
                .defaultIfEmpty( 0 );
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


    private Observable<MarkedEdge> getEdgesToTargetBySourceType( final OrganizationScope scope,
                                                                 final SearchByIdType search ) {

        return Observable.create( new ObservableIterator<MarkedEdge>() {
            @Override
            protected Iterator<MarkedEdge> getIterator() {
                return edgeSerialization.getEdgesToTargetBySourceType( scope, search );
            }
        } ).subscribeOn( scheduler );
    }


    private Observable<String> loadEdgeIdsToTarget( final OrganizationScope scope, final SearchIdType search ) {
        return Observable.create( new ObservableIterator<String>() {
            @Override
            protected Iterator<String> getIterator() {
                return edgeMetadataSerialization.getIdTypesToTarget( scope, search );
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
}
