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


import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.collection.mvcc.event.PostProcessObserver;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.EdgeManager;
import org.apache.usergrid.persistence.graph.EdgeManagerFactory;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;


/**
 * @author tnine
 */
@Singleton
public class CollectionIndexObserver implements PostProcessObserver {

    private final EdgeManagerFactory edgeManagerFactory;


    @Inject
    public CollectionIndexObserver( final EdgeManagerFactory edgeManagerFactory ) {
        Preconditions.checkNotNull( edgeManagerFactory, "edgeManagerFactory cannot be null" );
        this.edgeManagerFactory = edgeManagerFactory;
    }



    @Override
    public void postCommit( final CollectionScope scope, final MvccEntity entity ) {

        //get the edge manager for the org scope
        EdgeManager em = edgeManagerFactory.createEdgeManager( scope );

        /**
         * create an edge from owner->entity of the type name in the scope.
         *
         * Ex: application--users-->user
         *
         * Ex: user--devices->device
         *
         * We're essentially mapping a tree structure in to a graph edge
         */
        Edge edge = new SimpleEdge(scope.getOwner(), scope.getName(), entity.getId(), entity.getVersion() );

        //entity exists, write the edge
        if(entity.getEntity().isPresent()){
            em.writeEdge( edge );
        }
        //entity does not exist, it's been removed, clear the edge
        else{
            em.deleteEdge( edge );
        }
    }
}
