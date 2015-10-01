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


import java.util.Iterator;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.usergrid.persistence.core.guice.MigrationManagerRule;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.test.ITRunner;
import org.apache.usergrid.persistence.core.test.UseModules;
import org.apache.usergrid.persistence.core.util.IdGenerator;
import org.apache.usergrid.persistence.graph.guice.TestGraphModule;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import static org.apache.usergrid.persistence.core.util.IdGenerator.createId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@RunWith(ITRunner.class)
@UseModules({ TestGraphModule.class })
public class EdgeShardSerializationTest {


    @Inject
    @Rule
    public MigrationManagerRule migrationManagerRule;


    @Inject
    private EdgeShardSerialization edgeShardSerialization;

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
    public void saveReturn() throws ConnectionException {

        final Id now = IdGenerator.createId( "test" );

        final long timestamp = 10000l;

        final Shard shard1 = new Shard( 1000l, timestamp, false );

        final Shard shard2 = new Shard( shard1.getShardIndex() * 2, timestamp, true );

        final Shard shard3 = new Shard( shard2.getShardIndex() * 2, timestamp, false );

        final DirectedEdgeMeta sourceEdgeMeta = DirectedEdgeMeta.fromSourceNodeTargetType( now, "edgeType", "subType" );

        MutationBatch batch = edgeShardSerialization.writeShardMeta( scope, shard1, sourceEdgeMeta );

        batch.mergeShallow( edgeShardSerialization.writeShardMeta( scope, shard2, sourceEdgeMeta ) );

        batch.mergeShallow( edgeShardSerialization.writeShardMeta( scope, shard3, sourceEdgeMeta ) );

        batch.execute();


        Iterator<Shard> results =
                edgeShardSerialization.getShardMetaData( scope, Optional.<Shard>absent(), sourceEdgeMeta );


        assertEquals( shard3, results.next() );

        assertEquals( shard2, results.next() );

        assertEquals( shard1, results.next() );


        assertFalse( results.hasNext() );

        final DirectedEdgeMeta targetEdgeMeta = DirectedEdgeMeta.fromTargetNodeSourceType( now, "edgeType", "subType" );

        //test we get nothing with the other node type
        results = edgeShardSerialization.getShardMetaData( scope, Optional.<Shard>absent(), targetEdgeMeta );

        assertFalse( results.hasNext() );


        //test paging and size
        results = edgeShardSerialization.getShardMetaData( scope, Optional.of( shard2 ), sourceEdgeMeta );

        assertEquals( shard2, results.next() );


        assertEquals( shard1, results.next() );


        assertFalse( results.hasNext() );
    }


    @Test
    public void saveReturnDelete() throws ConnectionException {

        final Id now = IdGenerator.createId( "test" );


        final long timestamp = 10000l;

        final Shard shard1 = new Shard( 1000l, timestamp, false );

        final Shard shard2 = new Shard( shard1.getShardIndex() * 2, timestamp, true );

        final Shard shard3 = new Shard( shard2.getShardIndex() * 2, timestamp, false );


        final DirectedEdgeMeta sourceEdgeMeta = DirectedEdgeMeta.fromSourceNodeTargetType( now, "edgeType", "subType" );


        MutationBatch batch = edgeShardSerialization.writeShardMeta( scope, shard1, sourceEdgeMeta );

        batch.mergeShallow( edgeShardSerialization.writeShardMeta( scope, shard2, sourceEdgeMeta ) );

        batch.mergeShallow( edgeShardSerialization.writeShardMeta( scope, shard3, sourceEdgeMeta ) );

        batch.execute();


        Iterator<Shard> results =
                edgeShardSerialization.getShardMetaData( scope, Optional.<Shard>absent(), sourceEdgeMeta );

        assertEquals( shard3, results.next() );

        assertEquals( shard2, results.next() );

        assertEquals( shard1, results.next() );

        assertFalse( results.hasNext() );

        //test nothing with other type

        final DirectedEdgeMeta targetEdgeMeta = DirectedEdgeMeta.fromTargetNodeSourceType( now, "edgeType", "subType" );

        results = edgeShardSerialization.getShardMetaData( scope, Optional.<Shard>absent(), targetEdgeMeta );

        assertFalse( results.hasNext() );


        //test paging and size
        edgeShardSerialization.removeShardMeta( scope, shard1, sourceEdgeMeta ).execute();

        results = edgeShardSerialization.getShardMetaData( scope, Optional.<Shard>absent(), sourceEdgeMeta );

        assertEquals( shard3, results.next() );

        assertEquals( shard2, results.next() );

        assertFalse( results.hasNext() );


        edgeShardSerialization.removeShardMeta( scope, shard2, sourceEdgeMeta ).execute();

        edgeShardSerialization.removeShardMeta( scope, shard3, sourceEdgeMeta ).execute();

        results = edgeShardSerialization.getShardMetaData( scope, Optional.<Shard>absent(), sourceEdgeMeta );


        assertFalse( results.hasNext() );
    }
}
