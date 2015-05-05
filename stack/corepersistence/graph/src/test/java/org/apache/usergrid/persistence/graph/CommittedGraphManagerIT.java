/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one
 *  * or more contributor license agreements.  See the NOTICE file
 *  * distributed with this work for additional information
 *  * regarding copyright ownership.  The ASF licenses this file
 *  * to you under the Apache License, Version 2.0 (the
 *  * "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied.  See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 *
 */
package org.apache.usergrid.persistence.graph;


import org.junit.runner.RunWith;

import org.apache.usergrid.persistence.core.test.ITRunner;
import org.apache.usergrid.persistence.core.test.UseModules;
import org.apache.usergrid.persistence.graph.guice.TestGraphModule;
import org.apache.usergrid.persistence.model.entity.Id;

import rx.Observable;


/**
 * Integration test that performs all calls immediately after writes without blocking.  Tests that our
 * view is immediately consistent to our users, even if we have yet to perform background processing
 */
@RunWith(ITRunner.class)
@UseModules({ TestGraphModule.class })
public class CommittedGraphManagerIT extends GraphManagerIT {


    @Override
    protected GraphManager getHelper(GraphManager gm) {
        return new ComittedGraphTestHelper( gm );
    }


    /**
     * Doesn't wait for the async process to happen before returning.  Simply executes and immediately returns.
     */
    public static class ComittedGraphTestHelper implements GraphManager {

        private final GraphManager graphManager;


        public ComittedGraphTestHelper( final GraphManager graphManager ) {
            this.graphManager = graphManager;
        }


        @Override
        public Observable<Edge> writeEdge( final Edge edge ) {
            return graphManager.writeEdge( edge );
        }


        @Override
        public Observable<Edge> markEdge( final Edge edge ) {
            return graphManager.markEdge( edge );
        }


        @Override
        public Observable<Id> markNode( final Id node, final long timestamp ) {
            return graphManager.markNode( node, timestamp );
        }


        @Override
        public Observable<Edge> loadEdgeVersions( final SearchByEdge edge ) {
            return graphManager.loadEdgeVersions( edge );
        }


        @Override
        public Observable<Edge> loadEdgesFromSource( final SearchByEdgeType search ) {
            return graphManager.loadEdgesFromSource( search );
        }


        @Override
        public Observable<Edge> loadEdgesToTarget( final SearchByEdgeType search ) {
            return graphManager.loadEdgesToTarget( search );
        }


        @Override
        public Observable<Edge> loadEdgesFromSourceByType( final SearchByIdType search ) {
            return  graphManager.loadEdgesFromSourceByType(search);
        }


        @Override
        public Observable<Edge> loadEdgesToTargetByType( final SearchByIdType search ) {
            return graphManager.loadEdgesToTargetByType( search );
        }


        @Override
        public Observable<String> getEdgeTypesFromSource( final SearchEdgeType search ) {
            return graphManager.getEdgeTypesFromSource( search );
        }


        @Override
        public Observable<String> getIdTypesFromSource( final SearchIdType search ) {
            return graphManager.getIdTypesFromSource( search );
        }


        @Override
        public Observable<String> getEdgeTypesToTarget( final SearchEdgeType search ) {
            return graphManager.getEdgeTypesToTarget( search );
        }


        @Override
        public Observable<String> getIdTypesToTarget( final SearchIdType search ) {
            return graphManager.getIdTypesToTarget( search );
        }


    }
}
