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

import org.jukito.UseModules;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.usergrid.persistence.core.cassandra.ITRunner;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.cassandra.CassandraRule;
import org.apache.usergrid.persistence.collection.guice.MigrationManagerRule;
import org.apache.usergrid.persistence.graph.guice.TestGraphModule;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@RunWith( ITRunner.class )
@UseModules( { TestGraphModule.class } )
public class EdgeShardSerializationTest {

    @ClassRule
    public static CassandraRule rule = new CassandraRule();


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

        final Id now = createId( "test" );

        final long slice1 = 1000l;

        final long slice2 = slice1 * 2;

        final long slice3 = slice2 * 2;

        String[] types = { "edgeType", "subType" };

        MutationBatch batch = edgeShardSerialization.writeEdgeMeta( scope, now, slice1, types );

        batch.mergeShallow( edgeShardSerialization.writeEdgeMeta( scope, now, slice2, types ) );

        batch.mergeShallow( edgeShardSerialization.writeEdgeMeta( scope, now, slice3, types ) );

        batch.execute();


        Iterator<Long> results = edgeShardSerialization.getEdgeMetaData( scope, now, Optional.<Long>absent(), types );

        assertEquals( slice3, results.next().longValue() );

        assertEquals( slice2, results.next().longValue() );

        assertEquals( slice1, results.next().longValue() );

        assertFalse( results.hasNext() );

        //test paging and size
        results = edgeShardSerialization.getEdgeMetaData( scope, now, Optional.of( slice2 ), types );

        assertEquals( slice2, results.next().longValue() );

        assertEquals( slice1, results.next().longValue() );


        assertFalse( results.hasNext() );
    }


    @Test
    public void saveReturnDelete() throws ConnectionException {

        final Id now = createId( "test" );

        final long slice1 = 1000l;

        final long slice2 = slice1 * 2;

        final long slice3 = slice2 * 2;

        String[] types = { "edgeType", "subType" };

        MutationBatch batch = edgeShardSerialization.writeEdgeMeta( scope, now, slice1, types );

        batch.mergeShallow( edgeShardSerialization.writeEdgeMeta( scope, now, slice2, types ) );

        batch.mergeShallow( edgeShardSerialization.writeEdgeMeta( scope, now, slice3, types ) );

        batch.execute();


        Iterator<Long> results = edgeShardSerialization.getEdgeMetaData( scope, now, Optional.<Long>absent(), types );

        assertEquals( slice3, results.next().longValue() );

        assertEquals( slice2, results.next().longValue() );

        assertEquals( slice1, results.next().longValue() );

        assertFalse( results.hasNext() );

        //test paging and size
        edgeShardSerialization.removeEdgeMeta( scope, now, slice1, types ).execute();

        results = edgeShardSerialization.getEdgeMetaData( scope, now,Optional.<Long>absent(), types );

        assertEquals( slice3, results.next().longValue() );

        assertEquals( slice2, results.next().longValue() );

        assertFalse( results.hasNext() );


        edgeShardSerialization.removeEdgeMeta( scope, now, slice2, types ).execute();

        edgeShardSerialization.removeEdgeMeta( scope, now, slice3, types ).execute();

        results = edgeShardSerialization.getEdgeMetaData( scope, now, Optional.<Long>absent(), types );


        assertFalse( results.hasNext() );
    }
}
