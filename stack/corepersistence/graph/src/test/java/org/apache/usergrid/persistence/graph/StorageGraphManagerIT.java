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
import org.junit.Before;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.core.cassandra.ITRunner;
import org.apache.usergrid.persistence.core.consistency.AsyncProcessor;
import org.apache.usergrid.persistence.core.consistency.AsyncProcessorFactory;
import org.apache.usergrid.persistence.core.consistency.AsynchronousMessage;
import org.apache.usergrid.persistence.core.consistency.CompleteListener;
import org.apache.usergrid.persistence.core.consistency.ErrorListener;
import org.apache.usergrid.persistence.graph.guice.TestGraphModule;
import org.apache.usergrid.persistence.graph.impl.EdgeDeleteEvent;
import org.apache.usergrid.persistence.graph.impl.EdgeWriteEvent;
import org.apache.usergrid.persistence.graph.impl.NodeDeleteEvent;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.inject.Inject;

import rx.Observable;


/**
 * Integration test that will block reads until all post processing has completed.  This ensures once our data has made
 * it to the permanent storage.  Tests that our view is immediately consistent to our users, even if we have yet to
 * perform background processing
 */
@RunWith( ITRunner.class )
@UseModules( { TestGraphModule.class } )
public class StorageGraphManagerIT extends GraphManagerIT {

    private static final Logger LOG = LoggerFactory.getLogger( StorageGraphManagerIT.class );

    @Inject
    protected AsyncProcessor<EdgeDeleteEvent> edgeDelete;


    @Inject
    public AsyncProcessor<NodeDeleteEvent> nodeDelete;

    @Inject
    public AsyncProcessor<EdgeWriteEvent> edgeWrite;

    @Inject
    public AsyncProcessorFactory factory;

    @Before
    public void setup(){
        edgeDelete = factory.getProcessor( EdgeDeleteEvent.class );
        edgeWrite = factory.getProcessor( EdgeWriteEvent.class );
        nodeDelete = factory.getProcessor( NodeDeleteEvent.class );
    }



    @Override
    protected GraphManager getHelper( final GraphManager gm ) {


        final ComittedGraphTestHelper helper = new ComittedGraphTestHelper( gm );

        edgeDelete.addCompleteListener( new CompleteListener<EdgeDeleteEvent>() {
            @Override
            public void onComplete( final AsynchronousMessage<EdgeDeleteEvent> event ) {
                helper.complete();
            }
        } );

        edgeDelete.addErrorListener( new ErrorListener<EdgeDeleteEvent>() {
            @Override
            public void onError( final AsynchronousMessage<EdgeDeleteEvent> event, final Throwable t ) {
                helper.complete();
                helper.error();
            }
        } );

        nodeDelete.addCompleteListener( new CompleteListener<NodeDeleteEvent>() {
            @Override
            public void onComplete( final AsynchronousMessage<NodeDeleteEvent> event ) {
                helper.complete();
            }
        } );

        nodeDelete.addErrorListener( new ErrorListener<NodeDeleteEvent>() {
            @Override
            public void onError( final AsynchronousMessage<NodeDeleteEvent> event, final Throwable t ) {
                helper.complete();
                helper.error();
            }
        } );

        edgeWrite.addCompleteListener( new CompleteListener<EdgeWriteEvent>() {
            @Override
            public void onComplete( final AsynchronousMessage<EdgeWriteEvent> event ) {
                helper.complete();
            }
        } );

        edgeWrite.addErrorListener( new ErrorListener<EdgeWriteEvent>() {
            @Override
            public void onError( final AsynchronousMessage<EdgeWriteEvent> event, final Throwable t ) {
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
            completeInvocations.incrementAndGet();
            return graphManager.writeEdge( edge );
        }


        @Override
        public Observable<Edge> deleteEdge( final Edge edge ) {
            completeInvocations.incrementAndGet();
            return graphManager.deleteEdge( edge );
        }


        @Override
        public Observable<Id> deleteNode( final Id node, final long timestamp ) {
            completeInvocations.incrementAndGet();
            return graphManager.deleteNode( node, timestamp );
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
            completeInvocations.decrementAndGet();
            tryWake();
        }


        public void error() {
            errorInvocations.decrementAndGet();
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
            while (  completeInvocations.get() > 0 ) {

                LOG.info( "Waiting for more invocations, count is {} ", completeInvocations.get() );

                synchronized ( mutex ) {
                    try {
                        mutex.wait(1000 );
                    }
                    catch ( InterruptedException e ) {
                        //no op
                    }
                }
            }
        }
    }
}
