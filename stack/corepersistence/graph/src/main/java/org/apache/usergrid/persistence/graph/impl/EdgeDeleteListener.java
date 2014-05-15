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


import java.util.UUID;

import org.apache.usergrid.persistence.core.consistency.AsyncProcessor;
import org.apache.usergrid.persistence.core.consistency.MessageListener;
import org.apache.usergrid.persistence.core.scope.OrganizationScope;
import org.apache.usergrid.persistence.graph.MarkedEdge;
import org.apache.usergrid.persistence.graph.guice.EdgeDelete;
import org.apache.usergrid.persistence.graph.impl.stage.EdgeDeleteRepair;
import org.apache.usergrid.persistence.graph.impl.stage.EdgeMetaRepair;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import rx.Observable;
import rx.functions.Func1;
import rx.functions.Func2;


/**
 * Construct the asynchronous delete operation from the listener
 */
@Singleton
public class EdgeDeleteListener implements MessageListener<EdgeEvent<MarkedEdge>, EdgeEvent<MarkedEdge>> {


    private final EdgeDeleteRepair edgeDeleteRepair;
    private final EdgeMetaRepair edgeMetaRepair;


    @Inject
<<<<<<< HEAD
    public EdgeDeleteListener(  final EdgeMetadataSerialization edgeMetadataSerialization,
                                final GraphManagerFactory graphManagerFactory, final Keyspace keyspace,
                                @EdgeDelete final AsyncProcessor edgeDelete, final GraphFig graphFig ) {
        this.edgeMetadataSerialization = edgeMetadataSerialization;
        this.graphManagerFactory = graphManagerFactory;
        this.keyspace = keyspace;
        this.graphFig = graphFig;
=======
    public EdgeDeleteListener( @EdgeDelete final AsyncProcessor edgeDelete, final EdgeDeleteRepair edgeDeleteRepair,
                               final EdgeMetaRepair edgeMetaRepair ) {

        this.edgeDeleteRepair = edgeDeleteRepair;
        this.edgeMetaRepair = edgeMetaRepair;
>>>>>>> 74f95ac4c39c6d2e48dcee56d45a5b69d9b4ea5e

        edgeDelete.addListener( this );
    }


    @Override
    public Observable<EdgeEvent<MarkedEdge>> receive( final EdgeEvent<MarkedEdge> delete ) {

        final MarkedEdge edge = delete.getData();
        final OrganizationScope scope = delete.getOrganizationScope();
        final UUID maxVersion = edge.getVersion();
<<<<<<< HEAD
        final GraphManager graphManager = graphManagerFactory.createEdgeManager( scope );


        return Observable.from( edge ).flatMap( new Func1<Edge, Observable<MutationBatch>>() {
                                                    @Override
                                                    public Observable<MutationBatch> call( final Edge edge ) {

                                                        final MutationBatch batch = keyspace.prepareMutationBatch();


                                                        //search by edge type and target type.  If any other edges with this target type exist,
                                                        // we can't delete it
                                                        Observable<Integer> sourceIdType = graphManager.loadEdgesFromSourceByType(
                                                                new SimpleSearchByIdType( edge.getSourceNode(), edge.getType(), maxVersion,
                                                                        edge.getTargetNode().getType(), null ) ).take( 2 ).count()
                                                                .doOnNext( new Action1<Integer>() {
                                                                    @Override
                                                                    public void call( final Integer count ) {
                                                                        //There's nothing to do,
                                                                        // we have 2 different edges with the
                                                                        // same edge type and
                                                                        // target type.  Don't delete meta data
                                                                        if ( count == 1 ) {
                                                                            final MutationBatch delete =
                                                                                    edgeMetadataSerialization
                                                                                            .removeEdgeTypeFromSource(
                                                                                                    scope, edge );
                                                                            batch.mergeShallow( delete );
                                                                        }
                                                                    }
                                                                } );


                                                        Observable<Integer> targetIdType = graphManager.loadEdgesToTargetByType(
                                                                new SimpleSearchByIdType( edge.getTargetNode(), edge.getType(), maxVersion,
                                                                        edge.getSourceNode().getType(), null ) ).take( 2 ).count()
                                                                .doOnNext( new Action1<Integer>() {
                                                                    @Override
                                                                    public void call( final Integer count ) {
                                                                        //There's nothing to do,
                                                                        // we have 2 different edges with the
                                                                        // same edge type and
                                                                        // target type.  Don't delete meta data
                                                                        if ( count == 1 ) {
                                                                            final MutationBatch delete =
                                                                                    edgeMetadataSerialization
                                                                                            .removeEdgeTypeToTarget(
                                                                                                    scope, edge );
                                                                            batch.mergeShallow( delete );
                                                                        }
                                                                    }
                                                                } );


                                                        //search by edge type and target type.  If any other edges with this target type exist,
                                                        // we can't delete it
                                                        Observable<Integer> sourceType = graphManager.loadEdgesFromSource(
                                                                new SimpleSearchByEdgeType( edge.getSourceNode(), edge.getType(), maxVersion, null ) ).take( 2 )
                                                                .count().doOnNext( new Action1<Integer>() {
                                                                    @Override
                                                                    public void call( final Integer count ) {
                                                                        //There's nothing to do,
                                                                        // we have 2 different edges with the
                                                                        // same edge type and
                                                                        // target type.  Don't delete meta data
                                                                        if ( count == 1 ) {
                                                                            final MutationBatch delete =
                                                                                    edgeMetadataSerialization.removeEdgeTypeFromSource( scope, edge );
                                                                            batch.mergeShallow( delete );
                                                                        }
                                                                    }
                                                                } );


                                                        Observable<Integer> targetType = graphManager.loadEdgesToTarget(
                                                                new SimpleSearchByEdgeType( edge.getTargetNode(), edge.getType(), maxVersion, null ) ).take( 2 )
                                                                .count().doOnNext( new Action1<Integer>() {
                                                                    @Override
                                                                    public void call( final Integer count ) {
                                                                        //There's nothing to do,
                                                                        // we have 2 different edges with the
                                                                        // same edge type and
                                                                        // target type.  Don't delete meta data
                                                                        if ( count == 1 ) {
                                                                            final MutationBatch delete =
                                                                                    edgeMetadataSerialization.removeEdgeTypeToTarget( scope, edge );
                                                                            batch.mergeShallow( delete );
                                                                        }
                                                                    }
                                                                } );


                                                        //no op, just wait for each observable to populate the mutation before returning it
                                                        return Observable.zip(sourceIdType, targetIdType, sourceType, targetType,
                                                                new Func4<Integer, Integer, Integer, Integer, MutationBatch>() {
                                                                    @Override
                                                                    public MutationBatch call( final Integer integer,
                                                                                               final Integer integer2, final Integer integer3,
                                                                                               final Integer integer4 ) {
                                                                        return batch;
                                                                    }
                                                                } );
                                                    }
                                                }


        ).map( new Func1<MutationBatch, EdgeEvent<Edge>>() {
            @Override
            public EdgeEvent<Edge> call( final MutationBatch mutationBatch ) {

                //actually delete the edge from both the commit log and
                try {
                    mutationBatch.execute();
                }
                catch ( ConnectionException e ) {
                    throw new RuntimeException( "Unable to execute mutation", e );
                }

                return delete;
            }
        } );
=======

        return edgeDeleteRepair.repair( scope, edge, delete.getTimestamp() )

                               .flatMap( new Func1<MarkedEdge, Observable<Integer>>() {
                                   @Override
                                   public Observable<Integer> call( final MarkedEdge markedEdge ) {
                                       Observable<Integer> sourceDelete = edgeMetaRepair
                                               .repairSources( scope, edge.getSourceNode(), edge.getType(),
                                                       maxVersion );

                                       Observable<Integer> targetDelete = edgeMetaRepair
                                               .repairTargets( scope, edge.getTargetNode(), edge.getType(),
                                                       maxVersion );

                                       return Observable.zip( sourceDelete, targetDelete,
                                               new Func2<Integer, Integer, Integer>() {
                                                   @Override
                                                   public Integer call( final Integer sourceCount,
                                                                        final Integer targetCount ) {
                                                       return sourceCount + targetCount;
                                                   }
                                               } );
                                   }
                               } ).map( new Func1<Integer, EdgeEvent<MarkedEdge>>() {

                    @Override
                    public EdgeEvent<MarkedEdge> call( final Integer integer ) {
                        return delete;
                    }
                } );
>>>>>>> 74f95ac4c39c6d2e48dcee56d45a5b69d9b4ea5e
    }
}
