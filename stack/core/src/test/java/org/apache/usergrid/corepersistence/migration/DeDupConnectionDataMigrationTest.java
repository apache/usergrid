/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.usergrid.corepersistence.migration;


import java.util.List;

import org.junit.Test;

import org.apache.usergrid.corepersistence.rx.impl.AllApplicationsObservable;
import org.apache.usergrid.corepersistence.service.ConnectionScope;
import org.apache.usergrid.corepersistence.service.ConnectionService;
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.persistence.core.migration.data.TestProgressObserver;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.graph.Edge;

import rx.Observable;
import rx.Subscriber;

import static org.apache.usergrid.persistence.core.util.IdGenerator.createId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class DeDupConnectionDataMigrationTest {

    @Test
    public void testVersion() {

        //mock up an initial system state
        final ConnectionService connectionService = mock( ConnectionService.class );

        final AllApplicationsObservable allApplicationsObservable = mock( AllApplicationsObservable.class );


        final DeDupConnectionDataMigration plugin =
            new DeDupConnectionDataMigration( connectionService, allApplicationsObservable );


        //anything less than 2 should be supported
        assertTrue( plugin.supports( 0 ) );

        assertTrue( plugin.supports( 1 ) );

        assertFalse( plugin.supports( plugin.getMaxVersion() ) );
    }


    /**
     * Test
     */
    @Test
    public void testEmitEdges() {

        //mock up an initial system state
        final int version = 0;

        final ConnectionService connectionService = mock( ConnectionService.class );


        final AllApplicationsObservable allApplicationsObservable = mock( AllApplicationsObservable.class );


        final int count = 3000;

        final Observable<ConnectionScope> edgeEmitter = Observable.create( new DupConnectionEmitter( count ) );

        when( connectionService.deDupeConnections( any( Observable.class ) ) ).thenReturn( edgeEmitter );


        final DeDupConnectionDataMigration plugin =
            new DeDupConnectionDataMigration( connectionService, allApplicationsObservable );


        final TestProgressObserver testProgressObserver = new TestProgressObserver();


        plugin.migrate( version, testProgressObserver );

        final List<String> updates = testProgressObserver.getUpdates();

        assertEquals( "Expected 3 updates", 3, updates.size() );


        assertEquals( "De duped 1000 edges", updates.get( 0 ) );
        assertEquals( "De duped 2000 edges", updates.get( 1 ) );
        assertEquals( "De duped 3000 edges", updates.get( 2 ) );


        assertTrue( "Should complete", testProgressObserver.isComplete() );
        assertTrue( "Should start", testProgressObserver.isStarted() );
        assertFalse( "Should not fail", testProgressObserver.isFailed() );
    }


    private final class DupConnectionEmitter implements Observable.OnSubscribe<ConnectionScope> {


        private final int count;


        private DupConnectionEmitter( final int count ) {this.count = count;}


        @Override
        public void call( final Subscriber<? super ConnectionScope> subscriber ) {

            final ApplicationScope applicationScope = new ApplicationScopeImpl( createId( "application" ) );
            final Edge edge = CpNamingUtils.createConnectionEdge( createId( "source" ), "test", createId( "target" ) );


            final ConnectionScope scope = new ConnectionScope( applicationScope, edge );


            subscriber.onStart();

            for ( int i = 0; i < count; i++ ) {
                subscriber.onNext( scope );
            }

            subscriber.onCompleted();
        }
    }
}
