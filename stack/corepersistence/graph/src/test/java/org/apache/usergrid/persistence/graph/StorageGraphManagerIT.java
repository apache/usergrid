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


import java.util.concurrent.atomic.AtomicInteger;

import org.jukito.JukitoRunner;
import org.jukito.UseModules;
import org.junit.runner.RunWith;

import org.apache.usergrid.persistence.core.consistency.AsyncProcessor;
import org.apache.usergrid.persistence.core.consistency.AsynchronousMessage;
import org.apache.usergrid.persistence.core.consistency.CompleteListener;
import org.apache.usergrid.persistence.core.consistency.ErrorListener;
import org.apache.usergrid.persistence.graph.guice.EdgeDelete;
import org.apache.usergrid.persistence.graph.guice.EdgeWrite;
import org.apache.usergrid.persistence.graph.guice.NodeDelete;
import org.apache.usergrid.persistence.graph.guice.TestGraphModule;
import org.apache.usergrid.persistence.graph.impl.EdgeEvent;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.inject.Inject;

import rx.Observable;


/**
 * Integration test that will block reads until all post processing has completed.  This ensures once our data has made
 * it to the permanent storage.  Tests that our view is immediately consistent to our users, even if we have yet to
 * perform background processing
 */
@RunWith( JukitoRunner.class )
@UseModules( { TestGraphModule.class } )
public class StorageGraphManagerIT extends GraphManagerIT {


    @Inject
    @EdgeDelete
    protected AsyncProcessor<EdgeEvent<Edge>> edgeDelete;


    @Inject
    @NodeDelete
    public AsyncProcessor<EdgeEvent<Id>> nodeDelete;

    @Inject
    @EdgeWrite
    public AsyncProcessor<EdgeEvent<Edge>> edgeWrite;


    @Override
    protected GraphManager getHelper( final GraphManager gm ) {


        final ComittedGraphTestHelper helper = new ComittedGraphTestHelper( gm );

        edgeDelete.addCompleteListener( new CompleteListener<EdgeEvent<Edge>>() {
            @Override
            public void onComplete( final AsynchronousMessage<EdgeEvent<Edge>> event ) {
                helper.complete();
            }
        } );

        edgeDelete.addErrorListener( new ErrorListener<EdgeEvent<Edge>>() {
            @Override
            public void onError( final AsynchronousMessage<EdgeEvent<Edge>> event, final Throwable t ) {
                helper.complete();
                helper.error();
            }
        } );

        nodeDelete.addCompleteListener( new CompleteListener<EdgeEvent<Id>>() {
            @Override
            public void onComplete( final AsynchronousMessage<EdgeEvent<Id>> event ) {
                helper.complete();
            }
        } );

        nodeDelete.addErrorListener( new ErrorListener<EdgeEvent<Id>>() {
            @Override
            public void onError( final AsynchronousMessage<EdgeEvent<Id>> event, final Throwable t ) {
                helper.complete();
                helper.error();
            }
        } );

        edgeWrite.addCompleteListener( new CompleteListener<EdgeEvent<Edge>>() {
            @Override
            public void onComplete( final AsynchronousMessage<EdgeEvent<Edge>> event ) {
                helper.complete();
            }
        } );

        edgeWrite.addErrorListener( new ErrorListener<EdgeEvent<Edge>>() {
            @Override
            public void onError( final AsynchronousMessage<EdgeEvent<Edge>> event, final Throwable t ) {
                helper.complete();
                helper.error();
            }
        } );

        return helper;
    }


    /**
     * Doesn't wait for the async process to happen before returning.  Simply executes and immediately returns.
     */
    public static class ComittedGraphTestHelper implements GraphManager {

        private final GraphManager graphManager;
        private final AtomicInteger completeInvocations = new AtomicInteger( 0 );
        private final AtomicInteger errorInvocations = new AtomicInteger( 0 );
        private final Object mutex = new Object();


        public ComittedGraphTestHelper( final GraphManager graphManager ) {
            this.graphManager = graphManager;
        }


        @Override
        public Observable<Edge> writeEdge( final Edge edge ) {
            completeInvocations.decrementAndGet();
            return graphManager.writeEdge( edge );
        }


        @Override
        public Observable<Edge> deleteEdge( final Edge edge ) {
            completeInvocations.decrementAndGet();
            return graphManager.deleteEdge( edge );
        }


        @Override
        public Observable<Id> deleteNode( final Id node ) {
            completeInvocations.decrementAndGet();
            return graphManager.deleteNode( node );
        }


        @Override
        public Observable<Edge> loadEdgeVersions( final SearchByEdge edge ) {
            await();
            return graphManager.loadEdgeVersions( edge );
        }


        @Override
        public Observable<Edge> loadEdgesFromSource( final SearchByEdgeType search ) {
            await();
            return graphManager.loadEdgesFromSource( search );
        }


        @Override
        public Observable<Edge> loadEdgesToTarget( final SearchByEdgeType search ) {
            await();
            return graphManager.loadEdgesToTarget( search );
        }


        @Override
        public Observable<Edge> loadEdgesFromSourceByType( final SearchByIdType search ) {
            await();
            return graphManager.loadEdgesFromSourceByType( search );
        }


        @Override
        public Observable<Edge> loadEdgesToTargetByType( final SearchByIdType search ) {
            await();
            return graphManager.loadEdgesToTargetByType( search );
        }


        @Override
        public Observable<String> getEdgeTypesFromSource( final SearchEdgeType search ) {
            await();
            return graphManager.getEdgeTypesFromSource( search );
        }


        @Override
        public Observable<String> getIdTypesFromSource( final SearchIdType search ) {
            await();
            return graphManager.getIdTypesFromSource( search );
        }


        @Override
        public Observable<String> getEdgeTypesToTarget( final SearchEdgeType search ) {
            await();
            return graphManager.getEdgeTypesToTarget( search );
        }


        @Override
        public Observable<String> getIdTypesToTarget( final SearchIdType search ) {
            await();
            return graphManager.getIdTypesToTarget( search );
        }


        public void complete() {
            completeInvocations.incrementAndGet();
            tryWake();
        }


        public void error() {
            errorInvocations.incrementAndGet();
            tryWake();
        }


        public void tryWake() {
            synchronized ( mutex ) {
                mutex.notify();
            }
        }


        /**
         * Away for our invocations to be 0
         */
        public void await() {
            while (  completeInvocations.get() != 0 ) {

                synchronized ( mutex ) {
                    try {

                        mutex.wait();
                    }
                    catch ( InterruptedException e ) {
                        //no op
                    }
                }
            }
        }
    }
}
