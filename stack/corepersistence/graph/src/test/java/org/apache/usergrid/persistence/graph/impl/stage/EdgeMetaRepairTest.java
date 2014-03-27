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


import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
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
import org.apache.usergrid.persistence.graph.GraphFig;
import org.apache.usergrid.persistence.graph.guice.TestGraphModule;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchEdgeType;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchIdType;
import org.apache.usergrid.persistence.graph.serialization.EdgeMetadataSerialization;
import org.apache.usergrid.persistence.graph.serialization.EdgeSerialization;
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
public class EdgeMetaRepairTest {


    @ClassRule
    public static CassandraRule rule = new CassandraRule();


    @Inject
    @Rule
    public MigrationManagerRule migrationManagerRule;


    @Inject
    protected EdgeMetaRepair edgeMetaRepair;

    @Inject
    protected EdgeSerialization edgeSerialization;

    @Inject
    protected EdgeMetadataSerialization edgeMetadataSerialization;

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
    }


    @Test
    public void cleanTargetNoEdgesNoMeta() {
        //do no writes, then execute a cleanup with no meta data

        final Id targetId = createId( "target" );
        final String test = "test";
        final UUID version = UUIDGenerator.newTimeUUID();

        int value = edgeMetaRepair.repairTargets( scope, targetId, test, version ).toBlockingObservable().single();

        assertEquals( "No subtypes found", 0, value );
    }


    @Test
    public void cleanTargetSingleEdge() throws ConnectionException {
        Edge edge = createEdge( "source", "test", "target" );

        edgeSerialization.writeEdge( scope, edge ).execute();

        edgeMetadataSerialization.writeEdge( scope, edge ).execute();

        int value = edgeMetaRepair.repairTargets( scope, edge.getTargetNode(), edge.getType(), edge.getVersion() )
                                  .toBlockingObservable().single();

        assertEquals( "No subtypes removed, edge exists", 1, value );

        //now delete the edge

        edgeSerialization.deleteEdge( scope, edge ).execute();

        value = edgeMetaRepair.repairTargets( scope, edge.getTargetNode(), edge.getType(), edge.getVersion() )
                              .toBlockingObservable().single();

        assertEquals( "Single subtype should be removed", 0, value );

        //now verify they're gone

        Iterator<String> edgeTypes = edgeMetadataSerialization
                .getEdgeTypesToTarget( scope, new SimpleSearchEdgeType( edge.getTargetNode(), null ) );

        assertFalse( "No edge types exist", edgeTypes.hasNext() );


        Iterator<String> sourceTypes = edgeMetadataSerialization
                .getIdTypesToTarget( scope, new SimpleSearchIdType( edge.getTargetNode(), edge.getType(), null ) );

        assertFalse( "No edge types exist", sourceTypes.hasNext() );
    }


    @Test
    public void cleanTargetMultipleEdge() throws ConnectionException {

        Id targetId = createId( "target" );

        Edge edge1 = createEdge( createId( "source1" ), "test", targetId );


        edgeSerialization.writeEdge( scope, edge1 ).execute();

        edgeMetadataSerialization.writeEdge( scope, edge1 ).execute();

        Edge edge2 = createEdge( createId( "source2" ), "test", targetId );

        edgeSerialization.writeEdge( scope, edge2 ).execute();

        edgeMetadataSerialization.writeEdge( scope, edge2 ).execute();

        Edge edge3 = createEdge( createId( "source3" ), "test", targetId );

        edgeSerialization.writeEdge( scope, edge3 ).execute();

        edgeMetadataSerialization.writeEdge( scope, edge3 ).execute();


        UUID cleanupVersion = UUIDGenerator.newTimeUUID();

        int value = edgeMetaRepair.repairTargets( scope, edge1.getTargetNode(), edge1.getType(), cleanupVersion )
                                  .toBlockingObservable().single();

        assertEquals( "No subtypes removed, edges exist", 3, value );

        //now delete the edge

        edgeSerialization.deleteEdge( scope, edge1 ).execute();

        value = edgeMetaRepair.repairTargets( scope, edge1.getTargetNode(), edge1.getType(), cleanupVersion )
                              .toBlockingObservable().single();

        assertEquals( "No subtypes removed, edges exist", 2, value );

        edgeSerialization.deleteEdge( scope, edge2 ).execute();

        value = edgeMetaRepair.repairTargets( scope, edge1.getTargetNode(), edge1.getType(), cleanupVersion )
                              .toBlockingObservable().single();

        assertEquals( "No subtypes removed, edges exist", 1, value );

        edgeSerialization.deleteEdge( scope, edge3 ).execute();

        value = edgeMetaRepair.repairTargets( scope, edge1.getTargetNode(), edge1.getType(), cleanupVersion )
                              .toBlockingObservable().single();


        assertEquals( "Single subtype should be removed", 0, value );

        //now verify they're gone

        Iterator<String> edgeTypes = edgeMetadataSerialization
                .getEdgeTypesToTarget( scope, new SimpleSearchEdgeType( edge1.getTargetNode(), null ) );

        assertFalse( "No edge types exist", edgeTypes.hasNext() );


        Iterator<String> sourceTypes = edgeMetadataSerialization
                .getIdTypesToTarget( scope, new SimpleSearchIdType( edge1.getTargetNode(), edge1.getType(), null ) );

        assertFalse( "No edge types exist", sourceTypes.hasNext() );
    }


    @Test
    public void cleanTargetMultipleEdgeBuffer() throws ConnectionException {

        final Id targetId = createId( "target" );
        final String edgeType = "test";

        final int size = graphFig.getRepairConcurrentSize() * 2;

        Set<Edge> writtenEdges = new HashSet<Edge>();


        for ( int i = 0; i < size; i++ ) {
            Edge edge = createEdge( createId( "source" + i ), edgeType, targetId );

            edgeSerialization.writeEdge( scope, edge ).execute();

            edgeMetadataSerialization.writeEdge( scope, edge ).execute();

            writtenEdges.add( edge );
        }


        UUID cleanupVersion = UUIDGenerator.newTimeUUID();

        int value = edgeMetaRepair.repairTargets( scope, targetId, edgeType, cleanupVersion ).toBlockingObservable()
                                  .single();

        assertEquals( "No subtypes removed, edges exist", size, value );

        //now delete the edge

        for ( Edge created : writtenEdges ) {
            edgeSerialization.deleteEdge( scope, created ).execute();
        }


        value = edgeMetaRepair.repairTargets( scope, targetId, edgeType, cleanupVersion ).toBlockingObservable().last();

        assertEquals( "Subtypes removed", 0, value );

        //now verify they're gone

        Iterator<String> edgeTypes =
                edgeMetadataSerialization.getEdgeTypesToTarget( scope, new SimpleSearchEdgeType( targetId, null ) );

        assertFalse( "No edge types exist", edgeTypes.hasNext() );


        Iterator<String> sourceTypes = edgeMetadataSerialization
                .getIdTypesToTarget( scope, new SimpleSearchIdType( targetId, edgeType, null ) );

        assertFalse( "No edge types exist", sourceTypes.hasNext() );
    }


    @Test
    public void cleanSourceSingleEdge() throws ConnectionException {
        Edge edge = createEdge( "source", "test", "target" );

        edgeSerialization.writeEdge( scope, edge ).execute();

        edgeMetadataSerialization.writeEdge( scope, edge ).execute();

        int value = edgeMetaRepair.repairSources( scope, edge.getSourceNode(), edge.getType(), edge.getVersion() )
                                  .toBlockingObservable().single();

        assertEquals( "No subtypes removed, edge exists", 1, value );

        //now delete the edge

        edgeSerialization.deleteEdge( scope, edge ).execute();

        value = edgeMetaRepair.repairSources( scope, edge.getSourceNode(), edge.getType(), edge.getVersion() )
                              .toBlockingObservable().single();

        assertEquals( "Single subtype should be removed", 0, value );

        //now verify they're gone

        Iterator<String> edgeTypes = edgeMetadataSerialization
                .getEdgeTypesFromSource( scope, new SimpleSearchEdgeType( edge.getSourceNode(), null ) );

        assertFalse( "No edge types exist", edgeTypes.hasNext() );


        Iterator<String> sourceTypes = edgeMetadataSerialization
                .getIdTypesFromSource( scope, new SimpleSearchIdType( edge.getSourceNode(), edge.getType(), null ) );

        assertFalse( "No edge types exist", sourceTypes.hasNext() );
    }


    @Test
    public void cleanSourceMultipleEdge() throws ConnectionException {

        Id sourceId = createId( "source" );

        Edge edge1 = createEdge( sourceId, "test", createId( "target1" ) );


        edgeSerialization.writeEdge( scope, edge1 ).execute();

        edgeMetadataSerialization.writeEdge( scope, edge1 ).execute();

        Edge edge2 = createEdge( sourceId, "test", createId( "target2" ) );

        edgeSerialization.writeEdge( scope, edge2 ).execute();

        edgeMetadataSerialization.writeEdge( scope, edge2 ).execute();

        Edge edge3 = createEdge( sourceId, "test", createId( "target3" ) );

        edgeSerialization.writeEdge( scope, edge3 ).execute();

        edgeMetadataSerialization.writeEdge( scope, edge3 ).execute();


        UUID cleanupVersion = UUIDGenerator.newTimeUUID();

        int value = edgeMetaRepair.repairSources( scope, edge1.getSourceNode(), edge1.getType(), cleanupVersion )
                                  .toBlockingObservable().single();

        assertEquals( "No subtypes removed, edges exist", 3, value );

        //now delete the edge

        edgeSerialization.deleteEdge( scope, edge1 ).execute();

        value = edgeMetaRepair.repairSources( scope, edge1.getSourceNode(), edge1.getType(), cleanupVersion )
                              .toBlockingObservable().single();

        assertEquals( "No subtypes removed, edges exist", 2, value );

        edgeSerialization.deleteEdge( scope, edge2 ).execute();

        value = edgeMetaRepair.repairSources( scope, edge1.getSourceNode(), edge1.getType(), cleanupVersion )
                              .toBlockingObservable().single();

        assertEquals( "No subtypes removed, edges exist", 1, value );

        edgeSerialization.deleteEdge( scope, edge3 ).execute();

        value = edgeMetaRepair.repairSources( scope, edge1.getSourceNode(), edge1.getType(), cleanupVersion )
                              .toBlockingObservable().single();


        assertEquals( "Single subtype should be removed", 0, value );

        //now verify they're gone

        Iterator<String> edgeTypes = edgeMetadataSerialization
                .getEdgeTypesFromSource( scope, new SimpleSearchEdgeType( edge1.getSourceNode(), null ) );

        assertFalse( "No edge types exist", edgeTypes.hasNext() );


        Iterator<String> sourceTypes = edgeMetadataSerialization
                .getIdTypesFromSource( scope, new SimpleSearchIdType( edge1.getSourceNode(), edge1.getType(), null ) );

        assertFalse( "No edge types exist", sourceTypes.hasNext() );
    }


    @Test
    public void cleanSourceMultipleEdgeBuffer() throws ConnectionException {

        Id sourceId = createId( "source" );

        final String edgeType = "test";

        final int size = graphFig.getRepairConcurrentSize() * 2;

        Set<Edge> writtenEdges = new HashSet<Edge>();


        for ( int i = 0; i < size; i++ ) {
            Edge edge = createEdge( sourceId, edgeType, createId( "target" + i ) );

            edgeSerialization.writeEdge( scope, edge ).execute();

            edgeMetadataSerialization.writeEdge( scope, edge ).execute();

            writtenEdges.add( edge );
        }


        UUID cleanupVersion = UUIDGenerator.newTimeUUID();

        int value = edgeMetaRepair.repairSources( scope, sourceId, edgeType, cleanupVersion ).toBlockingObservable()
                                  .single();

        assertEquals( "No subtypes removed, edges exist", size, value );

        //now delete the edge

        for ( Edge created : writtenEdges ) {
            edgeSerialization.deleteEdge( scope, created ).execute();
        }


        value = edgeMetaRepair.repairSources( scope, sourceId, edgeType, cleanupVersion ).toBlockingObservable()
                              .single();

        assertEquals( "Subtypes removed", 0, value );

        //now verify they're gone

        Iterator<String> edgeTypes =
                edgeMetadataSerialization.getEdgeTypesFromSource( scope, new SimpleSearchEdgeType( sourceId, null ) );

        assertFalse( "No edge types exist", edgeTypes.hasNext() );


        Iterator<String> sourceTypes = edgeMetadataSerialization
                .getIdTypesFromSource( scope, new SimpleSearchIdType( sourceId, edgeType, null ) );

        assertFalse( "No edge types exist", sourceTypes.hasNext() );
    }
}
