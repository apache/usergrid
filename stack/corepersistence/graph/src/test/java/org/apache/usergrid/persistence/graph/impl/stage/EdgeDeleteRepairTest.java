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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.jukito.JukitoRunner;
import org.jukito.UseModules;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.inject.Inject;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createEdge;
import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 *
 *
 */
@RunWith(JukitoRunner.class)
@UseModules({ TestGraphModule.class })
public class EdgeDeleteRepairTest {

    private static final Logger LOG = LoggerFactory.getLogger( EdgeDeleteRepairTest.class );

    @ClassRule
    public static CassandraRule rule = new CassandraRule();


    @Inject
    @Rule
    public MigrationManagerRule migrationManagerRule;


    @Inject
    protected EdgeSerialization edgeSerialization;

    @Inject
    protected EdgeDeleteRepair edgeDeleteRepair;


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

        Iterator<MarkedEdge> edges = edgeDeleteRepair.repair( scope, edge ).toBlockingObservable().getIterator();

        assertFalse( "No edges cleaned", edges.hasNext() );
    }


    /**
     * Test repairing with no edges TODO: TN.  There appears to be a race condition here with ordering.  Not sure if
     * this is intentional as part of the impl or if it's an issue
     */
    @Test
    public void versionTest() throws ConnectionException {
        final int size = 3;

        final List<Edge> versions = new ArrayList<Edge>( size );

        final Id sourceId = createId( "source" );
        final Id targetId = createId( "target" );
        final String edgeType = "edge";

        Set<Edge> deletedEdges = new HashSet<Edge>();
        int deleteIndex = size / 2;


        for ( int i = 0; i < size; i++ ) {
            final Edge edge = createEdge( sourceId, edgeType, targetId );

            versions.add( edge );

            edgeSerialization.writeEdge( scope, edge ).execute();

            LOG.info( "Writing edge at index [{}] {}", i, edge );

            if ( i <= deleteIndex ) {
                deletedEdges.add( edge );
            }
        }


        Edge keep = versions.get( deleteIndex );

        Iterable<MarkedEdge> edges = edgeDeleteRepair.repair( scope, keep ).toBlockingObservable().toIterable();


        Multiset<Edge> deletedStream = HashMultiset.create();

        for ( MarkedEdge edge : edges ) {

            LOG.info( "Returned edge {} for repair", edge );


            final boolean shouldBeDeleted = deletedEdges.contains( edge );

            assertTrue( "Removed matches saved index", shouldBeDeleted );

            deletedStream.add( edge );
        }

        deletedEdges.removeAll( deletedStream.elementSet() );

        assertEquals( 0, deletedEdges.size() );


        //now verify we get all the versions we expect back
        Iterator<MarkedEdge> iterator = edgeSerialization.getEdgeVersions( scope,
                new SimpleSearchByEdge( sourceId, edgeType, targetId, UUIDGenerator.newTimeUUID(), null ) );

        int count = 0;

        for ( MarkedEdge edge : new IterableWrapper<MarkedEdge>( iterator ) ) {

            LOG.info( "Returned edge {} to verify", edge );

            final int index = size - count - 1;

            LOG.info( "Checking for correct version at index {}", index );

            final Edge saved = versions.get( index );

            assertEquals( "Retained edge correct", saved, edge );

            count++;
        }

        final int keptCount = size - deleteIndex;

        assertEquals( "Kept edge version was the minimum", keptCount, count + 1 );
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
