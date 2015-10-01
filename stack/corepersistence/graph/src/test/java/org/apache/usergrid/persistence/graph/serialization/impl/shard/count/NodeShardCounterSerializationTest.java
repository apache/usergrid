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
package org.apache.usergrid.persistence.graph.serialization.impl.shard.count;


import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.core.guice.MigrationManagerRule;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.test.ITRunner;
import org.apache.usergrid.persistence.core.test.UseModules;
import org.apache.usergrid.persistence.core.util.IdGenerator;
import org.apache.usergrid.persistence.graph.GraphFig;
import org.apache.usergrid.persistence.graph.guice.TestGraphModule;
import org.apache.usergrid.persistence.graph.serialization.EdgeSerialization;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.DirectedEdgeMeta;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.Shard;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.google.inject.Inject;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import static org.apache.usergrid.persistence.core.util.IdGenerator.createId;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@RunWith(ITRunner.class)
@UseModules({ TestGraphModule.class })
public class NodeShardCounterSerializationTest {

    private static final Logger log = LoggerFactory.getLogger( NodeShardCounterSerializationTest.class );


    @Inject
    @Rule
    public MigrationManagerRule migrationManagerRule;


    protected EdgeSerialization serialization;

    @Inject
    protected GraphFig graphFig;

    @Inject
    protected Keyspace keyspace;

    @Inject
    protected NodeShardCounterSerialization nodeShardCounterSerialization;

    protected ApplicationScope scope;


    @Before
    public void setup() {
        scope = mock( ApplicationScope.class );

        Id orgId = mock( Id.class );

        when( orgId.getType() ).thenReturn( "organization" );
        when( orgId.getUuid() ).thenReturn( UUIDGenerator.newTimeUUID() );

        when( scope.getApplication() ).thenReturn( orgId );
    }


    @Test
    public void testWritesRead() throws ConnectionException {


        final Id id = IdGenerator.createId( "test" );

        ShardKey key1 = new ShardKey( scope, new Shard( 0, 0, false ), DirectedEdgeMeta.fromSourceNode( id, "type1" ) );

        ShardKey key2 = new ShardKey( scope, new Shard( 0, 0, false ), DirectedEdgeMeta.fromSourceNode( id, "type2" ) );

        ShardKey key3 = new ShardKey( scope, new Shard( 1, 0, false ), DirectedEdgeMeta.fromSourceNode( id, "type1" ) );


        Counter counter = new Counter();
        counter.add( key1, 1000 );
        counter.add( key2, 2000 );
        counter.add( key3, 3000 );

        nodeShardCounterSerialization.flush( counter ).execute();


        final long time1 = nodeShardCounterSerialization.getCount( key1 );

        assertEquals( 1000, time1 );

        final long time2 = nodeShardCounterSerialization.getCount( key2 );

        assertEquals( 2000, time2 );

        final long time3 = nodeShardCounterSerialization.getCount( key3 );

        assertEquals( 3000, time3 );
    }
}
