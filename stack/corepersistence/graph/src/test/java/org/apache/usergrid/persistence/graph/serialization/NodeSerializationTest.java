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
package org.apache.usergrid.persistence.graph.serialization;


import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import org.jukito.JukitoRunner;
import org.jukito.UseModules;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.usergrid.persistence.core.scope.OrganizationScope;
import org.apache.usergrid.persistence.collection.cassandra.CassandraRule;
import org.apache.usergrid.persistence.collection.guice.MigrationManagerRule;
import org.apache.usergrid.persistence.graph.guice.TestGraphModule;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createEdge;
import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 *
 *
 */
@RunWith(JukitoRunner.class)
@UseModules({ TestGraphModule.class })
public class NodeSerializationTest {


    @ClassRule
    public static CassandraRule rule = new CassandraRule();


    @Inject
    @Rule
    public MigrationManagerRule migrationManagerRule;


    @Inject
    protected NodeSerialization serialization;

    protected OrganizationScope scope;


    @Before
    public void setup() {
        scope = mock( OrganizationScope.class );

        Id orgId = mock( Id.class );

        when( orgId.getType() ).thenReturn( "organization" );
        when( orgId.getUuid() ).thenReturn( UUIDGenerator.newTimeUUID() );

        when( scope.getOrganization() ).thenReturn( orgId );
    }


    /**
     * Happy path
     */
    @Test
    public void writeReadDelete() throws ConnectionException {

        final Id nodeId = createId( "test" );
        final UUID version = UUIDGenerator.newTimeUUID();

        serialization.mark( scope, nodeId, version ).execute();

        Optional<UUID> returned = serialization.getMaxVersion( scope, nodeId );

        assertEquals( version, returned.get() );

        serialization.delete( scope, nodeId, returned.get() ).execute();

        returned = serialization.getMaxVersion( scope, nodeId );

        /**
         * Verifies that it is deleted
         */
        assertFalse( returned.isPresent() );
    }


    /**
     * Tests returning no edge
     */
    @Test
    public void noDeleteVersion() {

        final Id nodeId = createId( "test" );

        Optional<UUID> returned = serialization.getMaxVersion( scope, nodeId );

        /**
         * Verifies we didnt' get anything back when nothing has been marked
         */
        assertFalse( returned.isPresent() );
    }


    /**
     * Tests a latent write from a previous version is discarded
     */
    @Test
    public void oldVersionDiscarded() throws ConnectionException {

        final Id nodeId = createId( "test" );
        final UUID version1 = UUIDGenerator.newTimeUUID();
        final UUID version2 = UUIDGenerator.newTimeUUID();

        serialization.mark( scope, nodeId, version2 ).execute();

        Optional<UUID> returned = serialization.getMaxVersion( scope, nodeId );

        assertEquals( version2, returned.get() );

        //now write version1, it should be discarded

        serialization.mark( scope, nodeId, version1 ).execute();

        returned = serialization.getMaxVersion( scope, nodeId );

        /**
         * Verifies that it is deleted
         */
        assertEquals( version2, returned.get() );

        //perform a delete with v1, we shouldn't lose the column
        serialization.delete( scope, nodeId, version1 ).execute();

        returned = serialization.getMaxVersion( scope, nodeId );

        assertEquals( version2, returned.get() );

        //now delete v2
        serialization.delete( scope, nodeId, version2 ).execute();

        returned = serialization.getMaxVersion( scope, nodeId );

        assertFalse( returned.isPresent() );
    }


    /**
     * Tests a latent write from a previous version is discarded
     */
    @Test
    public void multiGet() throws ConnectionException {

        final Id nodeId1 = createId( "test" );
        final Id nodeId2 = createId( "test" );
        final Id nodeId3 = createId( "test" );


        final UUID version = UUIDGenerator.newTimeUUID();

        serialization.mark( scope, nodeId1, version ).execute();
        serialization.mark( scope, nodeId2, version ).execute();

        Map<Id, UUID> marks = serialization.getMaxVersions( scope,
                Arrays.asList( createEdge( nodeId1, "test", nodeId2 ), createEdge( nodeId2, "test", nodeId3 ) ) );


        assertEquals( version, marks.get( nodeId1 ) );
        assertEquals( version, marks.get( nodeId2 ) );
        assertFalse( marks.containsKey( nodeId3 ) );
    }
}


