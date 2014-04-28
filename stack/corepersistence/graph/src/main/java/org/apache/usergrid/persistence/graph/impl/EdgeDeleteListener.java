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
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.GraphFig;
import org.apache.usergrid.persistence.graph.GraphManager;
import org.apache.usergrid.persistence.graph.GraphManagerFactory;
import org.apache.usergrid.persistence.graph.guice.EdgeDelete;
import org.apache.usergrid.persistence.graph.serialization.EdgeMetadataSerialization;
import org.apache.usergrid.persistence.graph.serialization.EdgeSerialization;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func4;


/**
 * Construct the asynchronous delete operation from the listener
 */
@Singleton
public class EdgeDeleteListener implements MessageListener<EdgeEvent<Edge>, EdgeEvent<Edge>> {


    private final EdgeMetadataSerialization edgeMetadataSerialization;
    private final GraphManagerFactory graphManagerFactory;
    private final Keyspace keyspace;
    private final GraphFig graphFig;


    @Inject
    public EdgeDeleteListener(  final EdgeMetadataSerialization edgeMetadataSerialization,
                               final GraphManagerFactory graphManagerFactory, final Keyspace keyspace,
                               @EdgeDelete final AsyncProcessor edgeDelete, final GraphFig graphFig ) {
        this.edgeMetadataSerialization = edgeMetadataSerialization;
        this.graphManagerFactory = graphManagerFactory;
        this.keyspace = keyspace;
        this.graphFig = graphFig;

        edgeDelete.addListener( this );
    }


    @Override
    public Observable<EdgeEvent<Edge>> receive( final EdgeEvent<Edge> delete ) {

        final Edge edge = delete.getData();
        final OrganizationScope scope = delete.getOrganizationScope();
        final UUID maxVersion = edge.getVersion();
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
    }
}
