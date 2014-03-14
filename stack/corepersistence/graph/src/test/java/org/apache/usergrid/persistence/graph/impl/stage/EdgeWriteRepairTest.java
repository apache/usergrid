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


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
import org.apache.usergrid.persistence.graph.MarkedEdge;
import org.apache.usergrid.persistence.graph.guice.TestGraphModule;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchByEdge;
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
@RunWith( JukitoRunner.class )
@UseModules( { TestGraphModule.class } )
public class EdgeWriteRepairTest {

    @ClassRule
    public static CassandraRule rule = new CassandraRule();


    @Inject
    @Rule
    public MigrationManagerRule migrationManagerRule;


    @Inject
    protected EdgeSerialization edgeSerialization;

    @Inject
    protected EdgeWriteRepair edgeWriteRepair;


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
     * Test repairing with no edges
     */
    @Test
    public void noEdges() {
        Edge edge = createEdge( "source", "test", "target" );

        Iterator<MarkedEdge> edges = edgeWriteRepair.repair( scope, edge ).toBlockingObservable().getIterator();

        assertFalse( "No edges cleaned", edges.hasNext() );
    }


    /**
     * Test repairing with no edges
     */
    @Test
    public void versionTest() throws ConnectionException {
        final int size = 10;

        final List<Edge> versions = new ArrayList<Edge>( size );

        final Id sourceId = createId( "source" );
        final Id targetId = createId( "target" );
        final String edgeType = "edge";

        for ( int i = 0; i < size; i++ ) {
            final Edge edge = createEdge( sourceId, edgeType, targetId );

            versions.add( edge );

            edgeSerialization.writeEdge( scope, edge ).execute();

            System.out.println(String.format("[%d] %s", i, edge));
        }


        int keepIndex = size / 2;

        Edge keep = versions.get( keepIndex );

        Iterable<MarkedEdge> edges = edgeWriteRepair.repair( scope, keep ).toBlockingObservable().toIterable();


        int index = 0;

        for ( MarkedEdge edge : edges ) {

            final Edge removed = versions.get( keepIndex - index -1 );

            assertEquals( "Removed matches saved index", removed, edge );

            index++;
        }

        //now verify we get all the versions we expect back
        Iterator<MarkedEdge> iterator = edgeSerialization.getEdgeFromSource( scope,
                new SimpleSearchByEdge( sourceId, edgeType, targetId, UUIDGenerator.newTimeUUID(), null ) );

        index = 0;

        for(MarkedEdge edge: new IterableWrapper<MarkedEdge>( iterator )){

            final Edge saved = versions.get( size - index -1 );

            assertEquals(saved, edge);

            index++;
        }

        assertEquals("Kept edge version was the minimum", keepIndex, index);
    }


    private class IterableWrapper<T> implements Iterable<T> {
        private final Iterator<T> sourceIterator;


        private IterableWrapper( final Iterator<T> sourceIterator ) {
            this.sourceIterator = sourceIterator;
        }


        @Override
        public Iterator<T> iterator() {
            return this.sourceIterator;
        }
    }
}
