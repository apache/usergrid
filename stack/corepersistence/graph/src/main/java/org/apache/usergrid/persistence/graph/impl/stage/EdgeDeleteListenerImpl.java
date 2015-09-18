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


import java.util.UUID;

import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.graph.MarkedEdge;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import rx.Observable;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.observables.MathObservable;


/**
 * Construct the asynchronous delete operation from the listener
 */
@Singleton
public class EdgeDeleteListenerImpl implements EdgeDeleteListener {


    private final EdgeDeleteRepair edgeDeleteRepair;
    private final EdgeMetaRepair edgeMetaRepair;


    @Inject
    public EdgeDeleteListenerImpl(
                               final EdgeDeleteRepair edgeDeleteRepair, final EdgeMetaRepair edgeMetaRepair ) {
        this.edgeDeleteRepair = edgeDeleteRepair;
        this.edgeMetaRepair = edgeMetaRepair;

    }


    public Observable<Integer> receive( final ApplicationScope scope, final MarkedEdge edge,
                                        final UUID eventTimestamp ) {

        final long maxTimestamp = edge.getTimestamp();




        return edgeDeleteRepair.repair( scope, edge, eventTimestamp )
                               .flatMap( markedEdge -> {

                                   Observable<Integer> sourceDelete = edgeMetaRepair
                                           .repairSources( scope, edge.getSourceNode(), edge.getType(), maxTimestamp );

                                   Observable<Integer> targetDelete = edgeMetaRepair
                                           .repairTargets( scope, edge.getTargetNode(), edge.getType(), maxTimestamp );

                                   return MathObservable.sumInteger( Observable.merge( sourceDelete, targetDelete ) );
                               } );
    }
}
