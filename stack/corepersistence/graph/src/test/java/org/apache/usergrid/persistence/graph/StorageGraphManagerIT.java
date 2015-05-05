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

import org.junit.Before;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.core.test.ITRunner;
import org.apache.usergrid.persistence.core.test.UseModules;
import org.apache.usergrid.persistence.graph.guice.TestGraphModule;
import org.apache.usergrid.persistence.graph.impl.GraphManagerImpl;
import org.apache.usergrid.persistence.model.entity.Id;

import rx.Observable;
import rx.Observer;


/**
 * Integration test that will block reads until all post processing has completed.  This ensures once our data has made
 * it to the permanent storage.  Tests that our view is immediately consistent to our users, even if we have yet to
 * perform background processing
 */
@RunWith( ITRunner.class )
@UseModules( { TestGraphModule.class } )
public class StorageGraphManagerIT extends GraphManagerIT {

    private static final Logger LOG = LoggerFactory.getLogger( StorageGraphManagerIT.class );


    @Before
    public void setup(){
    }



    @Override
    protected GraphManager getHelper( final GraphManager gm ) {


        final StorageGraphTestHelper helper = new StorageGraphTestHelper( gm );

        GraphManagerImpl gmi = ( GraphManagerImpl ) gm;


        Observer<Integer> subscriber = new Observer<Integer>() {
                    @Override
                    public void onCompleted() {
                        helper.complete();
                    }


                    @Override
                    public void onError( final Throwable e ) {
                        helper.complete();
                        helper.error();
                    }


                    @Override
                    public void onNext( final Integer integer ) {
                        //no op
                    }
                };

        gmi.setEdgeDeleteSubcriber(subscriber );
//        gmi.setEdgeWriteSubcriber( subscriber );
        gmi.setNodeDelete( subscriber );

        return helper;
    }


    /**
     * Doesn't wait for the async process to happen before returning.  Simply executes and immediately returns.
     */
    public static class StorageGraphTestHelper implements GraphManager {

        private final GraphManager graphManager;
        private final AtomicInteger completeInvocations = new AtomicInteger( 0 );
        private final AtomicInteger errorInvocations = new AtomicInteger( 0 );
        private final Object mutex = new Object();


        public StorageGraphTestHelper( final GraphManager graphManager ) {
            this.graphManager = graphManager;
        }


        @Override
        public Observable<Edge> writeEdge( final Edge edge ) {
            return graphManager.writeEdge( edge );
        }


        @Override
        public Observable<Edge> markEdge( final Edge edge ) {
            waitForComplete();
            return graphManager.markEdge( edge );
        }


        @Override
        public Observable<Id> markNode( final Id node, final long timestamp ) {
            waitForComplete();
            return graphManager.markNode( node, timestamp );
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


        public void waitForComplete(){
            LOG.info( "Complete incremented" );
            completeInvocations.incrementAndGet();
        }

        public void complete() {
            LOG.info( "Complete decremented" );
            completeInvocations.decrementAndGet();
            tryWake();
        }


        public void error() {
            LOG.info( "Error incremented" );
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
