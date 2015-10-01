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


import com.google.inject.Singleton;


/**
 * TODO, move this into the EM,it doesn't belong in graph
 * @author tnine
 */
@Singleton
public class CollectionIndexObserver{
//        implements PostProcessObserver {
//
//    private final GraphManagerFactory graphManagerFactory;
//
//
//    @Inject
//    public CollectionIndexObserver( final GraphManagerFactory graphManagerFactory ) {
//        Preconditions.checkNotNull( graphManagerFactory, "graphManagerFactory cannot be null" );
//        this.graphManagerFactory = graphManagerFactory;
//    }
//
//
//
//    @Override
//    public void postCommit( final CollectionScope scope, final MvccEntity entity ) {
//
//        //get the edge manager for the org scope
//        GraphManager em = graphManagerFactory.createEdgeManager( scope );
//
//        /**
//         * create an edge from owner->entity of the type name in the scope.
//         *
//         * Ex: application--users-->user
//         *
//         * Ex: user--devices->device
//         *
//         * We're essentially mapping a tree structure in to a graph edge
//         */
//        Edge edge = new SimpleMarkedEdge( scope.getOwner(), scope.getName(), entity.getId(), entity.getTimestamp(), false );
//
//        //entity exists, write the edge
//        if(entity.getEntity().isPresent()){
//            em.writeEdgeFromSource( edge ).toBlocking().last();
//        }
//        //entity does not exist, it's been removed, mark the edge
//        else{
//            em.deleteEdge( edge ).toBlocking().last();
//        }
//    }
}
