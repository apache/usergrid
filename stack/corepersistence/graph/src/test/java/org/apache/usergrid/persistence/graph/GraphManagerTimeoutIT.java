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

package org.apache.usergrid.persistence.graph;


import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import org.jukito.JukitoRunner;
import org.jukito.UseModules;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.usergrid.persistence.core.scope.OrganizationScope;
import org.apache.usergrid.persistence.collection.cassandra.CassandraRule;
import org.apache.usergrid.persistence.collection.guice.MigrationManagerRule;
import org.apache.usergrid.persistence.graph.guice.TestGraphModule;
import org.apache.usergrid.persistence.graph.serialization.EdgeSerialization;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.google.inject.Inject;
import com.netflix.hystrix.exception.HystrixRuntimeException;

import rx.Observable;
import rx.Subscriber;

import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createEdge;
import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createId;
import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createSearchByEdge;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@RunWith( JukitoRunner.class )
@UseModules( { TestGraphModule.class } )
@Ignore("Mockings fail with mutliple threads, need to resolve this before enabling")
//@UseModules( { TestGraphModule.class, GraphManagerIT.InvalidInput.class } )
public class GraphManagerTimeoutIT {

    /**
     * Test timeout in millis
     */
    private static final long TIMEOUT = 30000;

    @ClassRule
    public static CassandraRule rule = new CassandraRule();


    @Inject
    @Rule
    public MigrationManagerRule migrationManagerRule;


    @Inject
    protected GraphManagerFactory emf;

    @Inject
    protected GraphFig graphFig;

    protected OrganizationScope scope;


    @Before
    public void setup() {
        scope = mock( OrganizationScope.class );

        Id orgId = mock( Id.class );

        when( orgId.getType() ).thenReturn( "organization" );
        when( orgId.getUuid() ).thenReturn( UUIDGenerator.newTimeUUID() );

        when( scope.getOrganization() ).thenReturn( orgId );

        if ( graphFig.getReadTimeout() > TIMEOUT ) {
            fail( "Graph read timeout must be <= " + TIMEOUT + ".  Otherwise tests are invalid" );
        }
    }


    //    @Test(timeout = TIMEOUT, expected = TimeoutException.class)
    @Test
    public void testWriteReadEdgeTypeSource( EdgeSerialization serialization ) throws InterruptedException {


        final GraphManager em = emf.createEdgeManager( scope );


        final MarkedEdge edge = createEdge( "source", "edge", "target" );

        //now test retrieving it

        SearchByEdgeType search = createSearchByEdge( edge.getSourceNode(), edge.getType(), edge.getVersion(), null );


        final MockingIterator<MarkedEdge> itr = new MockingIterator<>( Collections.singletonList( edge ) );


        //TODO, T.N. replace this with a different mock, the spies don't work with multi threading like RX
        //https://code.google.com/p/mockito/wiki/FAQ#Is_Mockito_thread-safe?
        when( serialization.getEdgesFromSource( same(scope), same(search) ) ).thenReturn( itr );

        Observable<Edge> edges = em.loadEdgesFromSource( search );

        //retrieve the edge, ensure that if we block indefinitely, it times out

        final AtomicInteger onNextCounter = new AtomicInteger();
        final CountDownLatch errorLatch = new CountDownLatch( 1 );

        final Throwable[] thrown = new Throwable[1];



        edges.subscribe( new Subscriber<Edge>() {
            @Override
            public void onCompleted() {

            }


            @Override
            public void onError( final Throwable e ) {
                thrown[0] = e;
                errorLatch.countDown();
            }


            @Override
            public void onNext( final Edge edge ) {
                {
                    onNextCounter.incrementAndGet();
                }
            }
        } );


        errorLatch.await();


        assertEquals( "One lement was produced", 1,onNextCounter.intValue() );
        assertTrue(thrown[0] instanceof HystrixRuntimeException);

    }





    private class MockingIterator<T> implements Iterator<T> {

        private final Iterator<T> items;

        private final Semaphore semaphore = new Semaphore( 0 );


        private MockingIterator( final Collection<T> items ) {
            this.items = items.iterator();
        }


        @Override
        public boolean hasNext() {
            return true;
        }


        @Override
        public T next() {
            if ( items.hasNext() ) {
                return items.next();
            }

            //block indefinitely
            try {
                semaphore.acquire();
            }
            catch ( InterruptedException e ) {
                throw new RuntimeException( e );
            }

            return null;
        }


        @Override
        public void remove() {
            throw new UnsupportedOperationException( "Cannot remove" );
        }
    }


}





