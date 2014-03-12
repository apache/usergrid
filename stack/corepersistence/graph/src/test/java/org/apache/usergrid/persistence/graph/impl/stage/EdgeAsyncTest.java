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

package org.apache.usergrid.persistence.graph.impl.stage;


import java.util.Iterator;
import java.util.UUID;

import org.jukito.JukitoRunner;
import org.jukito.UseModules;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.usergrid.persistence.collection.OrganizationScope;
import org.apache.usergrid.persistence.collection.cassandra.CassandraRule;
import org.apache.usergrid.persistence.collection.guice.MigrationManagerRule;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.guice.TestGraphModule;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchEdgeType;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchIdType;
import org.apache.usergrid.persistence.graph.serialization.EdgeMetadataSerialization;
import org.apache.usergrid.persistence.graph.serialization.EdgeSerialization;
import org.apache.usergrid.persistence.graph.serialization.NodeSerialization;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

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
public class EdgeAsyncTest {


    @ClassRule
    public static CassandraRule rule = new CassandraRule();


    @Inject
    @Rule
    public MigrationManagerRule migrationManagerRule;


    @Inject
    protected NodeSerialization serialization;

    @Inject
    protected EdgeAsync edgeAsync;

    @Inject
    protected EdgeSerialization edgeSerialization;

    @Inject
    protected EdgeMetadataSerialization edgeMetadataSerialization;

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
    public void cleanTargetNoEdgesNoMeta(){
       //do no writes, then execute a cleanup with no meta data

        final Id targetId = createId ("target" );
        final String test = "test";
        final UUID version = UUIDGenerator.newTimeUUID();

        int value = edgeAsync.clearTargets( scope, targetId, test, version ).toBlockingObservable().single();

        assertEquals("No subtypes found", 0, value);
    }

    @Test
    public void cleanTargetSingleEge() throws ConnectionException {
        Edge edge = createEdge( "source", "test", "target" );

        edgeSerialization.writeEdge( scope, edge ).execute();

        edgeMetadataSerialization.writeEdge( scope, edge ).execute();

        int value = edgeAsync.clearTargets( scope, edge.getTargetNode(), edge.getType(), edge.getVersion() ).toBlockingObservable().single();

        assertEquals("No subtypes removed, edge exists", 1, value);

        //now delete the edge

        edgeSerialization.deleteEdge( scope, edge ).execute();

        value = edgeAsync.clearTargets( scope, edge.getTargetNode(), edge.getType(), edge.getVersion() ).toBlockingObservable().single();

        assertEquals("Single subtype should be removed", 0, value);

        //now verify they're gone

        Iterator<String> edgeTypes = edgeMetadataSerialization.getEdgeTypesToTarget( scope,
                new SimpleSearchEdgeType( edge.getTargetNode(), null ) );

        assertFalse("No edge types exist", edgeTypes.hasNext());


        Iterator<String> sourceTypes = edgeMetadataSerialization.getIdTypesToTarget( scope, new SimpleSearchIdType( edge.getTargetNode(), edge.getType(), null ) );

        assertFalse("No edge types exist", sourceTypes.hasNext());



    }


}
