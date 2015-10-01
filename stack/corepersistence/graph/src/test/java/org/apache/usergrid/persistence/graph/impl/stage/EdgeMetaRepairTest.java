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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.usergrid.persistence.core.guice.MigrationManagerRule;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.test.ITRunner;
import org.apache.usergrid.persistence.core.test.UseModules;
import org.apache.usergrid.persistence.core.util.IdGenerator;
import org.apache.usergrid.persistence.graph.GraphFig;
import org.apache.usergrid.persistence.graph.MarkedEdge;
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
public class EdgeMetaRepairTest {


    @Inject
    @Rule
    public MigrationManagerRule migrationManagerRule;


    @Inject
    protected EdgeMetaRepair edgeMetaRepair;

    @Inject
    protected EdgeSerialization storageEdgeSerialization;

    @Inject
    protected EdgeMetadataSerialization edgeMetadataSerialization;

    @Inject
    protected GraphFig graphFig;

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
    public void cleanTargetNoEdgesNoMeta() {
        //do no writes, then execute a cleanup with no meta data

        final Id targetId = IdGenerator.createId( "target" );
        final String test = "test";
        final long version = System.currentTimeMillis();

        int value = edgeMetaRepair.repairTargets( scope, targetId, test, version ).toBlocking().single();

        assertEquals( "No subtypes found", 0, value );
    }


    @Test
    public void cleanTargetSingleEdge() throws ConnectionException {
        MarkedEdge edge = createEdge( "source", "test", "target" );

        storageEdgeSerialization.writeEdge( scope, edge, UUIDGenerator.newTimeUUID() ).execute();

        edgeMetadataSerialization.writeEdge( scope, edge ).execute();

        int value = edgeMetaRepair.repairTargets( scope, edge.getTargetNode(), edge.getType(), edge.getTimestamp() )
                                  .toBlocking().single();

        assertEquals( "No subtypes removed, edge exists", 1, value );

        //now delete the edge

        storageEdgeSerialization.deleteEdge( scope, edge, UUIDGenerator.newTimeUUID() ).execute();

        value = edgeMetaRepair.repairTargets( scope, edge.getTargetNode(), edge.getType(), edge.getTimestamp() )
                              .toBlocking().single();

        assertEquals( "Single subtype should be removed", 0, value );

        //now verify they're gone

        Iterator<String> edgeTypes = edgeMetadataSerialization
                .getEdgeTypesToTarget( scope, new SimpleSearchEdgeType( edge.getTargetNode(), null, null ) );

        assertFalse( "No edge types exist", edgeTypes.hasNext() );


        Iterator<String> sourceTypes = edgeMetadataSerialization
                .getIdTypesToTarget( scope, new SimpleSearchIdType( edge.getTargetNode(), edge.getType(), null, null ) );

        assertFalse( "No edge types exist", sourceTypes.hasNext() );
    }


    @Test
    public void cleanTargetMultipleEdge() throws ConnectionException {

        Id targetId = IdGenerator.createId( "target" );

        MarkedEdge edge1 = createEdge( IdGenerator.createId( "source1" ), "test", targetId );


        storageEdgeSerialization.writeEdge( scope, edge1, UUIDGenerator.newTimeUUID() ).execute();

        edgeMetadataSerialization.writeEdge( scope, edge1 ).execute();

        MarkedEdge edge2 = createEdge( IdGenerator.createId( "source2" ), "test", targetId );

        storageEdgeSerialization.writeEdge( scope, edge2, UUIDGenerator.newTimeUUID() ).execute();

        edgeMetadataSerialization.writeEdge( scope, edge2 ).execute();

        MarkedEdge edge3 = createEdge( IdGenerator.createId( "source3" ), "test", targetId );

        storageEdgeSerialization.writeEdge( scope, edge3, UUIDGenerator.newTimeUUID() ).execute();

        edgeMetadataSerialization.writeEdge( scope, edge3 ).execute();


        long cleanupVersion = System.currentTimeMillis();

        int value = edgeMetaRepair.repairTargets( scope, edge1.getTargetNode(), edge1.getType(), cleanupVersion )
                                  .toBlocking().single();

        assertEquals( "No subtypes removed, edges exist", 3, value );

        //now delete the edge

        storageEdgeSerialization.deleteEdge( scope, edge1, UUIDGenerator.newTimeUUID() ).execute();

        value = edgeMetaRepair.repairTargets( scope, edge1.getTargetNode(), edge1.getType(), cleanupVersion )
                              .toBlocking().single();

        assertEquals( "No subtypes removed, edges exist", 2, value );

        storageEdgeSerialization.deleteEdge( scope, edge2, UUIDGenerator.newTimeUUID() ).execute();

        value = edgeMetaRepair.repairTargets( scope, edge1.getTargetNode(), edge1.getType(), cleanupVersion )
                              .toBlocking().single();

        assertEquals( "No subtypes removed, edges exist", 1, value );

        storageEdgeSerialization.deleteEdge( scope, edge3, UUIDGenerator.newTimeUUID() ).execute();

        value = edgeMetaRepair.repairTargets( scope, edge1.getTargetNode(), edge1.getType(), cleanupVersion )
                              .toBlocking().single();


        assertEquals( "Single subtype should be removed", 0, value );

        //now verify they're gone

        Iterator<String> edgeTypes = edgeMetadataSerialization
                .getEdgeTypesToTarget( scope, new SimpleSearchEdgeType( edge1.getTargetNode(), null, null ) );

        assertFalse( "No edge types exist", edgeTypes.hasNext() );


        Iterator<String> sourceTypes = edgeMetadataSerialization
                .getIdTypesToTarget( scope, new SimpleSearchIdType( edge1.getTargetNode(), edge1.getType(), null, null ) );

        assertFalse( "No edge types exist", sourceTypes.hasNext() );
    }


    @Test
    public void cleanTargetMultipleEdgeBuffer() throws ConnectionException {

        final Id targetId = IdGenerator.createId( "target" );
        final String edgeType = "test";

        final int size = graphFig.getRepairConcurrentSize() * 2;

        Set<MarkedEdge> writtenEdges = new HashSet<MarkedEdge>();


        for ( int i = 0; i < size; i++ ) {
            MarkedEdge edge = createEdge( IdGenerator.createId( "source" + i ), edgeType, targetId );

            storageEdgeSerialization.writeEdge( scope, edge, UUIDGenerator.newTimeUUID() ).execute();

            edgeMetadataSerialization.writeEdge( scope, edge ).execute();

            writtenEdges.add( edge );
        }


        long cleanupVersion = System.currentTimeMillis();

        int value = edgeMetaRepair.repairTargets( scope, targetId, edgeType, cleanupVersion ).toBlocking()
                                  .single();

        assertEquals( "No subtypes removed, edges exist", size, value );

        //now delete the edge

        for ( MarkedEdge created : writtenEdges ) {
            storageEdgeSerialization.deleteEdge( scope, created, UUIDGenerator.newTimeUUID() ).execute();
        }


        value = edgeMetaRepair.repairTargets( scope, targetId, edgeType, cleanupVersion ).toBlocking().last();

        assertEquals( "Subtypes removed", 0, value );

        //now verify they're gone

        Iterator<String> edgeTypes =
                edgeMetadataSerialization.getEdgeTypesToTarget( scope, new SimpleSearchEdgeType( targetId, null, null ) );

        assertFalse( "No edge types exist", edgeTypes.hasNext() );


        Iterator<String> sourceTypes = edgeMetadataSerialization
                .getIdTypesToTarget( scope, new SimpleSearchIdType( targetId, edgeType, null,  null ) );

        assertFalse( "No edge types exist", sourceTypes.hasNext() );
    }


    @Test
    public void cleanSourceSingleEdge() throws ConnectionException {
        MarkedEdge edge = createEdge( "source", "test", "target" );

        storageEdgeSerialization.writeEdge( scope, edge, UUIDGenerator.newTimeUUID() ).execute();

        edgeMetadataSerialization.writeEdge( scope, edge ).execute();

        int value = edgeMetaRepair.repairSources( scope, edge.getSourceNode(), edge.getType(), edge.getTimestamp() )
                                  .toBlocking().single();

        assertEquals( "No subtypes removed, edge exists", 1, value );

        //now delete the edge

        storageEdgeSerialization.deleteEdge( scope, edge, UUIDGenerator.newTimeUUID() ).execute();

        value = edgeMetaRepair.repairSources( scope, edge.getSourceNode(), edge.getType(), edge.getTimestamp() )
                              .toBlocking().single();

        assertEquals( "Single subtype should be removed", 0, value );

        //now verify they're gone

        Iterator<String> edgeTypes = edgeMetadataSerialization
                .getEdgeTypesFromSource( scope, new SimpleSearchEdgeType( edge.getSourceNode(), null, null ) );

        assertFalse( "No edge types exist", edgeTypes.hasNext() );


        Iterator<String> sourceTypes = edgeMetadataSerialization
                .getIdTypesFromSource( scope, new SimpleSearchIdType( edge.getSourceNode(), edge.getType(),null, null ) );

        assertFalse( "No edge types exist", sourceTypes.hasNext() );
    }


    @Test
    public void cleanSourceMultipleEdge() throws ConnectionException {

        Id sourceId = IdGenerator.createId( "source" );

        MarkedEdge edge1 = createEdge( sourceId, "test", IdGenerator.createId( "target1" ) );


        storageEdgeSerialization.writeEdge( scope, edge1, UUIDGenerator.newTimeUUID() ).execute();

        edgeMetadataSerialization.writeEdge( scope, edge1 ).execute();

        MarkedEdge edge2 = createEdge( sourceId, "test", IdGenerator.createId( "target2" ) );

        storageEdgeSerialization.writeEdge( scope, edge2, UUIDGenerator.newTimeUUID() ).execute();

        edgeMetadataSerialization.writeEdge( scope, edge2).execute();

        MarkedEdge edge3 = createEdge( sourceId, "test", IdGenerator.createId( "target3" ) );

        storageEdgeSerialization.writeEdge( scope, edge3, UUIDGenerator.newTimeUUID() ).execute();

        edgeMetadataSerialization.writeEdge( scope, edge3 ).execute();


        long cleanupVersion = System.currentTimeMillis();

        int value = edgeMetaRepair.repairSources( scope, edge1.getSourceNode(), edge1.getType(), cleanupVersion )
                                  .toBlocking().single();

        assertEquals( "No subtypes removed, edges exist", 3, value );

        //now delete the edge

        storageEdgeSerialization.deleteEdge( scope, edge1, UUIDGenerator.newTimeUUID() ).execute();

        value = edgeMetaRepair.repairSources( scope, edge1.getSourceNode(), edge1.getType(), cleanupVersion )
                              .toBlocking().single();

        assertEquals( "No subtypes removed, edges exist", 2, value );

        storageEdgeSerialization.deleteEdge( scope, edge2, UUIDGenerator.newTimeUUID() ).execute();

        value = edgeMetaRepair.repairSources( scope, edge1.getSourceNode(), edge1.getType(), cleanupVersion )
                              .toBlocking().single();

        assertEquals( "No subtypes removed, edges exist", 1, value );

        storageEdgeSerialization.deleteEdge( scope, edge3, UUIDGenerator.newTimeUUID() ).execute();

        value = edgeMetaRepair.repairSources( scope, edge1.getSourceNode(), edge1.getType(), cleanupVersion )
                              .toBlocking().single();


        assertEquals( "Single subtype should be removed", 0, value );

        //now verify they're gone

        Iterator<String> edgeTypes = edgeMetadataSerialization
                .getEdgeTypesFromSource( scope, new SimpleSearchEdgeType( edge1.getSourceNode(),null, null ) );

        assertFalse( "No edge types exist", edgeTypes.hasNext() );


        Iterator<String> sourceTypes = edgeMetadataSerialization
                .getIdTypesFromSource( scope, new SimpleSearchIdType( edge1.getSourceNode(), edge1.getType(),null, null ) );

        assertFalse( "No edge types exist", sourceTypes.hasNext() );
    }


    @Test
    public void cleanSourceMultipleEdgeBuffer() throws ConnectionException {

        Id sourceId = IdGenerator.createId( "source" );

        final String edgeType = "test";

        final int size = graphFig.getRepairConcurrentSize() * 2;

        Set<MarkedEdge> writtenEdges = new HashSet<>();


        for ( int i = 0; i < size; i++ ) {
            MarkedEdge edge = createEdge( sourceId, edgeType, IdGenerator.createId( "target" + i ) );

            storageEdgeSerialization.writeEdge( scope, edge, UUIDGenerator.newTimeUUID() ).execute();

            edgeMetadataSerialization.writeEdge( scope, edge ).execute();

            writtenEdges.add( edge );
        }


        long cleanupVersion = System.currentTimeMillis();

        int value = edgeMetaRepair.repairSources( scope, sourceId, edgeType, cleanupVersion ).toBlocking()
                                  .single();

        assertEquals( "No subtypes removed, edges exist", size, value );

        //now delete the edge

        for ( MarkedEdge created : writtenEdges ) {
            storageEdgeSerialization.deleteEdge( scope, created, UUIDGenerator.newTimeUUID() ).execute();
        }


        value = edgeMetaRepair.repairSources( scope, sourceId, edgeType, cleanupVersion ).toBlocking()
                              .single();

        assertEquals( "Subtypes removed", 0, value );

        //now verify they're gone

        Iterator<String> edgeTypes =
                edgeMetadataSerialization.getEdgeTypesFromSource( scope, new SimpleSearchEdgeType( sourceId,null, null ) );

        assertFalse( "No edge types exist", edgeTypes.hasNext() );


        Iterator<String> sourceTypes = edgeMetadataSerialization
                .getIdTypesFromSource( scope, new SimpleSearchIdType( sourceId, edgeType,null, null ) );

        assertFalse( "No edge types exist", sourceTypes.hasNext() );
    }
}
