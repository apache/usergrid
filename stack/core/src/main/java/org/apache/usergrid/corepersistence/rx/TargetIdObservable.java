/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.usergrid.corepersistence.rx;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.GraphManager;
import org.apache.usergrid.persistence.model.entity.Id;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;


/**
 * Emits the id of all nodes that are target nodes from the given source node
 */
public class TargetIdObservable implements Observable.OnSubscribe<Id> {

    private static final Logger logger = LoggerFactory.getLogger( TargetIdObservable.class );

    private final ApplicationScope applicationScope;
    private final Id sourceNode;
    private final GraphManager gm;


    public TargetIdObservable( final ApplicationScope applicationScope, final Id sourceNode, final GraphManager gm ) {
        this.applicationScope = applicationScope;
        this.sourceNode = sourceNode;
        this.gm = gm;
    }


    @Override
    public void call( final Subscriber<? super Id> subscriber ) {


        //only search edge types that start with collections
        Observable.create( new EdgesFromSourceObservable( applicationScope, sourceNode, gm ) )
                  .doOnNext( new Action1<Edge>() {

                      @Override
                      public void call( Edge edge ) {

                          logger.info( "Emitting targetId of  {}", edge );

                          final Id targetNode = edge.getTargetNode();


                          subscriber.onNext( targetNode );
                      }
                  } ).toBlocking().lastOrDefault( null ); // end foreach on edges
    }
}
