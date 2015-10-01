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
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createEdge;
import static org.apache.usergrid.persistence.core.util.IdGenerator.createId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 *
 *
 */
@RunWith(ITRunner.class)
@UseModules({ TestGraphModule.class })
public class NodeSerializationTest {

    @Inject
    @Rule
    public MigrationManagerRule migrationManagerRule;


    @Inject
    protected NodeSerialization serialization;

    protected ApplicationScope scope;


    @Before
    public void setup() {
        scope = mock( ApplicationScope.class );

        Id orgId = mock( Id.class );

        when( orgId.getType() ).thenReturn( "organization" );
        when( orgId.getUuid() ).thenReturn( UUIDGenerator.newTimeUUID() );

        when( scope.getApplication() ).thenReturn( orgId );
    }


    /**
     * Happy path
     */
    @Test
    public void writeReadDelete() throws ConnectionException {

        final Id nodeId = IdGenerator.createId( "test" );
        final long version = System.currentTimeMillis();

        serialization.mark( scope, nodeId, version ).execute();

        Optional<Long> returned = serialization.getMaxVersion( scope, nodeId );

        assertEquals( version, returned.get().longValue() );

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

        final Id nodeId = IdGenerator.createId( "test" );

        Optional<Long> returned = serialization.getMaxVersion( scope, nodeId );

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

        final Id nodeId = IdGenerator.createId( "test" );
        final long version1 = System.currentTimeMillis();
        final long version2 = version1 + 1;

        serialization.mark( scope, nodeId, version2 ).execute();

        Optional<Long> returned = serialization.getMaxVersion( scope, nodeId );

        assertEquals( version2, returned.get().longValue() );

        //now write version1, it should be discarded

        serialization.mark( scope, nodeId, version1 ).execute();

        returned = serialization.getMaxVersion( scope, nodeId );

        /**
         * Verifies that it is deleted
         */
        assertEquals( version2, returned.get().longValue() );

        //perform a delete with v1, we shouldn't lose the column
        serialization.delete( scope, nodeId, version1 ).execute();

        returned = serialization.getMaxVersion( scope, nodeId );

        assertEquals( version2, returned.get().longValue() );

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

        final Id nodeId1 = IdGenerator.createId( "test" );
        final Id nodeId2 = IdGenerator.createId( "test" );
        final Id nodeId3 = IdGenerator.createId( "test" );


        final long version = System.currentTimeMillis();

        serialization.mark( scope, nodeId1, version ).execute();
        serialization.mark( scope, nodeId2, version ).execute();

        Map<Id, Long> marks = serialization.getMaxVersions( scope,
                Arrays.asList( createEdge( nodeId1, "test", nodeId2 ), createEdge( nodeId2, "test", nodeId3 ) ) );


        assertEquals( version, marks.get( nodeId1 ).longValue() );
        assertEquals( version, marks.get( nodeId2 ).longValue() );
        assertFalse( marks.containsKey( nodeId3 ) );
    }
}


