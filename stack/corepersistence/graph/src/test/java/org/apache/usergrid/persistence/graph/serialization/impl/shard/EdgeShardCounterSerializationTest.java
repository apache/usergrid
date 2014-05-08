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

package org.apache.usergrid.persistence.graph.serialization.impl.shard;


import java.util.Stack;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.jukito.JukitoRunner;
import org.jukito.UseModules;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.usergrid.persistence.core.scope.OrganizationScope;
import org.apache.usergrid.persistence.core.cassandra.CassandraRule;
import org.apache.usergrid.persistence.collection.guice.MigrationManagerRule;
import org.apache.usergrid.persistence.graph.guice.TestGraphModule;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.google.inject.Inject;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createId;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@RunWith( JukitoRunner.class )
@UseModules( { TestGraphModule.class } )
public class EdgeShardCounterSerializationTest {


    @ClassRule
    public static CassandraRule rule = new CassandraRule();


    @Inject
    @Rule
    public MigrationManagerRule migrationManagerRule;


    @Inject
    private EdgeShardCounterSerialization edgeShardCounterSerialization;

    protected OrganizationScope scope;


    @Before
    public void setup() {
        scope = mock( OrganizationScope.class );

        Id orgId = mock( Id.class );

        when( orgId.getType() ).thenReturn( "organization" );
        when( orgId.getUuid() ).thenReturn( UUIDGenerator.newTimeUUID() );

        when( scope.getOrganization() ).thenReturn( orgId );
    }


    @Test
    public void testSingleCount() throws ConnectionException {

        final Id id = createId( "test" );
        final long shard = 1000l;
        final String[] types = { "type", "subtype" };

        final long toWrite = 1000l;

        edgeShardCounterSerialization.writeMetaDataLog( scope, id, shard, toWrite, types ).execute();


        final long count = edgeShardCounterSerialization.getCount( scope, id, shard, types );

        assertEquals( "Correct amount returned", toWrite, count );
    }


    @Test
    public void testConcurrentWrites() throws ConnectionException, ExecutionException, InterruptedException {

        final Id id = createId( "test" );
        final long shard = 1000l;
        final String[] types = { "type", "subtype" };

        final long toWrite = 1000l;


        final int workerCount = 2;
        final int iterations = 1000;

        ExecutorService executors = Executors.newFixedThreadPool( workerCount );

        Stack<Future<Void>> futures = new Stack<Future<Void>>();

        for ( int i = 0; i < workerCount; i++ ) {

           final Future<Void> future =  executors.submit( new Callable<Void>() {

                @Override
                public Void call() throws Exception {

                    for ( int i = 0; i < iterations; i++ ) {
                        edgeShardCounterSerialization.writeMetaDataLog( scope, id, shard, toWrite, types ).execute();
                    }

                    return null;
                }
            } );

            futures.push( future );
        }

        /**
         * Wait until they're all done
         */
        for(Future<Void> future: futures){
            future.get();
        }


        final long count = edgeShardCounterSerialization.getCount( scope, id, shard, types );

        final long expected = toWrite * iterations * workerCount;

        assertEquals( "Correct amount returned", expected, count );
    }
}
