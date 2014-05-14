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
public class EdgeDeleteListener implements MessageListener<EdgeEvent<Edge>, EdgeEvent<Edge>> {


    private final EdgeDeleteRepair edgeDeleteRepair;
    private final EdgeMetaRepair edgeMetaRepair;


    @Inject
    public EdgeDeleteListener( @EdgeDelete final AsyncProcessor edgeDelete, final EdgeDeleteRepair edgeDeleteRepair,
                               final EdgeMetaRepair edgeMetaRepair ) {

        this.edgeDeleteRepair = edgeDeleteRepair;
        this.edgeMetaRepair = edgeMetaRepair;

        edgeDelete.addListener( this );
    }


    @Override
    public Observable<EdgeEvent<Edge>> receive( final EdgeEvent<Edge> delete ) {

        final Edge edge = delete.getData();
        final OrganizationScope scope = delete.getOrganizationScope();
        final UUID maxVersion = edge.getVersion();

        return edgeDeleteRepair.repair( scope, edge )

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
                               } ).map( new Func1<Integer, EdgeEvent<Edge>>() {

                    @Override
                    public EdgeEvent<Edge> call( final Integer integer ) {
                        return delete;
                    }
                } );
    }
}
