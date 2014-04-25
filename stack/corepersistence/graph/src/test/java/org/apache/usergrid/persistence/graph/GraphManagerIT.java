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
package org.apache.usergrid.persistence.graph;


import java.util.Iterator;
import java.util.UUID;

import org.jukito.All;
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
import org.apache.usergrid.persistence.graph.impl.SimpleSearchEdgeType;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchIdType;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.google.inject.Inject;

import rx.Observable;

import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createEdge;
import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createGetByEdge;
import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createId;
import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createSearchByEdge;
import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createSearchByEdgeAndId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@RunWith(JukitoRunner.class)
@UseModules({ TestGraphModule.class })
public class GraphManagerIT {


    @ClassRule
    public static CassandraRule rule = new CassandraRule();


    @Inject
    @Rule
    public MigrationManagerRule migrationManagerRule;


    @Inject
    protected GraphManagerFactory emf;

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
    public void testWriteReadEdgeTypeSource() {

        GraphManager em = emf.createEdgeManager( scope );


        Edge edge = createEdge( "source", "test", "target" );

        em.writeEdge( edge ).toBlockingObservable().last();

        //now test retrieving it

        SearchByEdgeType search = createSearchByEdge( edge.getSourceNode(), edge.getType(), edge.getVersion(), null );

        Observable<Edge> edges = em.loadEdgesFromSource( search );

        //implicitly blows up if more than 1 is returned from "single"
        Edge returned = edges.toBlockingObservable().last();

        assertEquals( "Correct edge returned", edge, returned );

        //change edge type to be invalid, shouldn't get a result
        search = createSearchByEdge( edge.getSourceNode(), edge.getType() + "invalid", edge.getVersion(), null );

        edges = em.loadEdgesFromSource( search );

        //implicitly blows up if more than 1 is returned from "single"
        returned = edges.toBlockingObservable().singleOrDefault( null );

        assertNull( "Invalid type should not be returned", returned );
    }


    @Test
    public void testWriteReadEdgeTypeTarget() {

        GraphManager em = emf.createEdgeManager( scope );


        Edge edge = createEdge( "source", "test", "target" );

        em.writeEdge( edge ).toBlockingObservable().last();

        //now test retrieving it

        SearchByEdgeType search = createSearchByEdge( edge.getTargetNode(), edge.getType(), edge.getVersion(), null );

        Observable<Edge> edges = em.loadEdgesToTarget( search );

        //implicitly blows up if more than 1 is returned from "single"
        Edge returned = edges.toBlockingObservable().single();

        assertEquals( "Correct edge returned", edge, returned );

        //change edge type to be invalid, shouldn't get a result
        search = createSearchByEdge( edge.getTargetNode(), edge.getType() + "invalid", edge.getVersion(), null );

        edges = em.loadEdgesToTarget( search );

        //implicitly blows up if more than 1 is returned from "single"
        returned = edges.toBlockingObservable().singleOrDefault( null );

        assertNull( "Invalid type should not be returned", returned );
    }


    @Test
    public void testWriteReadEdgeTypeVersionSource() {

        GraphManager em = emf.createEdgeManager( scope );

        final UUID earlyVersion = UUIDGenerator.newTimeUUID();


        Edge edge = createEdge( "source", "test", "target" );

        em.writeEdge( edge ).toBlockingObservable().last();

        //now test retrieving it

        SearchByEdgeType search = createSearchByEdge( edge.getSourceNode(), edge.getType(), edge.getVersion(), null );

        Observable<Edge> edges = em.loadEdgesFromSource( search );

        //implicitly blows up if more than 1 is returned from "single"
        Edge returned = edges.toBlockingObservable().single();

        assertEquals( "Correct edge returned", edge, returned );

        //now test with an earlier version, we shouldn't get the edge back
        search = createSearchByEdge( edge.getSourceNode(), edge.getType(), earlyVersion, null );

        edges = em.loadEdgesFromSource( search );

        //implicitly blows up if more than 1 is returned from "single"
        returned = edges.toBlockingObservable().singleOrDefault( null );

        assertNull( "Earlier version should not be returned", returned );
    }


    @Test
    public void testWriteReadEdgeTypeVersionTarget() {

        GraphManager em = emf.createEdgeManager( scope );


        final UUID earlyVersion = UUIDGenerator.newTimeUUID();


        Edge edge = createEdge( "source", "test", "target" );

        em.writeEdge( edge ).toBlockingObservable().last();

        //now test retrieving it

        SearchByEdgeType search = createSearchByEdge( edge.getTargetNode(), edge.getType(), edge.getVersion(), null );

        Observable<Edge> edges = em.loadEdgesToTarget( search );

        //implicitly blows up if more than 1 is returned from "single"
        Edge returned = edges.toBlockingObservable().single();

        assertEquals( "Correct edge returned", edge, returned );

        //change edge type to be invalid, shouldn't get a result
        search = createSearchByEdge( edge.getTargetNode(), edge.getType(), earlyVersion, null );

        edges = em.loadEdgesToTarget( search );

        //implicitly blows up if more than 1 is returned from "single"
        returned = edges.toBlockingObservable().singleOrDefault( null );

        assertNull( "Earlier version should not be returned", returned );
    }


    /**
     * Tests that if multiple versions of an edge exist, only the distinct edges with a version <= max are returned
     */
    @Test
    public void testWriteReadEdgeTypeVersionSourceDistinct() {

        GraphManager em = emf.createEdgeManager( scope );

        final UUID earlyVersion = UUIDGenerator.newTimeUUID();


        Edge edge1 = createEdge( "source", "test", "target" );

        final Id sourceId = edge1.getSourceNode();
        final Id targetId = edge1.getTargetNode();


        em.writeEdge( edge1 ).toBlockingObservable().last();

        Edge edge2 = createEdge( sourceId, edge1.getType(), targetId );

        em.writeEdge( edge2 ).toBlockingObservable().last();

        Edge edge3 = createEdge( sourceId, edge1.getType(), targetId );

        em.writeEdge( edge3 ).toBlockingObservable().last();


        //now test retrieving it, we should only get edge3, since it's the latest

        SearchByEdgeType search =
                createSearchByEdge( edge1.getSourceNode(), edge1.getType(), edge3.getVersion(), null );

        Observable<Edge> edges = em.loadEdgesFromSource( search );

        //implicitly blows up if more than 1 is returned from "single"
        Iterator<Edge> returned = edges.toBlockingObservable().getIterator();

        assertEquals( "Correct edge returned", edge3, returned.next() );
        assertEquals( "Correct edge returned", edge2, returned.next() );
        assertEquals( "Correct edge returned", edge1, returned.next() );
        assertFalse( "No more edges", returned.hasNext() );

        //now test with an earlier version, we shouldn't get the edge back
        search = createSearchByEdge( edge1.getSourceNode(), edge1.getType(), edge2.getVersion(), null );

        edges = em.loadEdgesFromSource( search );

        returned = edges.toBlockingObservable().getIterator();

        assertEquals( "Correct edge returned", edge2, returned.next() );
        assertEquals( "Correct edge returned", edge1, returned.next() );
        assertFalse( "No more edges", returned.hasNext() );

        search = createSearchByEdge( edge1.getSourceNode(), edge1.getType(), edge1.getVersion(), null );

        edges = em.loadEdgesFromSource( search );

        returned = edges.toBlockingObservable().getIterator();

        assertEquals( "Correct edge returned", edge1, returned.next() );
        assertFalse( "No more edges", returned.hasNext() );


        search = createSearchByEdge( edge1.getSourceNode(), edge1.getType(), earlyVersion, null );

        edges = em.loadEdgesFromSource( search );

        returned = edges.toBlockingObservable().getIterator();

        assertFalse( "No more edges", returned.hasNext() );
    }


    @Test
    public void testWriteReadEdgeTypeVersionTargetDistinct() {


        GraphManager em = emf.createEdgeManager( scope );

        final UUID earlyVersion = UUIDGenerator.newTimeUUID();


        Edge edge1 = createEdge( "source", "test", "target" );

        final Id sourceId = edge1.getSourceNode();
        final Id targetId = edge1.getTargetNode();


        em.writeEdge( edge1 ).toBlockingObservable().last();

        Edge edge2 = createEdge( sourceId, edge1.getType(), targetId );

        em.writeEdge( edge2 ).toBlockingObservable().last();

        Edge edge3 = createEdge( sourceId, edge1.getType(), targetId );

        em.writeEdge( edge3 ).toBlockingObservable().last();


        //now test retrieving it, we should only get edge3, since it's the latest

        SearchByEdgeType search =
                createSearchByEdge( edge1.getTargetNode(), edge1.getType(), edge3.getVersion(), null );

        Observable<Edge> edges = em.loadEdgesToTarget( search );

        //implicitly blows up if more than 1 is returned from "single"
        Iterator<Edge> returned = edges.toBlockingObservable().getIterator();

        assertEquals( "Correct edge returned", edge3, returned.next() );
        assertEquals( "Correct edge returned", edge2, returned.next() );
        assertEquals( "Correct edge returned", edge1, returned.next() );
        assertFalse( "No more edges", returned.hasNext() );

        //now test with an earlier version, we shouldn't get the edge back
        search = createSearchByEdge( edge1.getTargetNode(), edge1.getType(), edge2.getVersion(), null );

        edges = em.loadEdgesToTarget( search );

        returned = edges.toBlockingObservable().getIterator();

        assertEquals( "Correct edge returned", edge2, returned.next() );
        assertEquals( "Correct edge returned", edge1, returned.next() );
        assertFalse( "No more edges", returned.hasNext() );

        search = createSearchByEdge( edge1.getTargetNode(), edge1.getType(), edge1.getVersion(), null );

        edges = em.loadEdgesToTarget( search );

        returned = edges.toBlockingObservable().getIterator();

        assertEquals( "Correct edge returned", edge1, returned.next() );
        assertFalse( "No more edges", returned.hasNext() );


        search = createSearchByEdge( edge1.getTargetNode(), edge1.getType(), earlyVersion, null );

        edges = em.loadEdgesToTarget( search );

        returned = edges.toBlockingObservable().getIterator();

        assertFalse( "No more edges", returned.hasNext() );
    }


    @Test
    public void testWriteReadEdgeTypePagingSource() {

        GraphManager em = emf.createEdgeManager( scope );

        final Id sourceId = createId( "source" );


        Edge edge1 = createEdge( sourceId, "test", createId( "target" ) );

        em.writeEdge( edge1 ).toBlockingObservable().last();

        Edge edge2 = createEdge( sourceId, "test", createId( "target" ) );

        em.writeEdge( edge2 ).toBlockingObservable().last();

        Edge edge3 = createEdge( sourceId, "test", createId( "target" ) );

        em.writeEdge( edge3 ).toBlockingObservable().last();


        //now test retrieving it

        SearchByEdgeType search =
                createSearchByEdge( edge1.getSourceNode(), edge1.getType(), edge3.getVersion(), null );

        Observable<Edge> edges = em.loadEdgesFromSource( search );

        //implicitly blows up if more than 1 is returned from "single"
        Iterator<Edge> returned = edges.toBlockingObservable().getIterator();


        //we have 3 edges, but we specified our first edge as the max, we shouldn't get any more results than the first
        assertEquals( "Correct edge returned", edge3, returned.next() );

        assertEquals( "Correct edge returned", edge2, returned.next() );

        assertEquals( "Correct edge returned", edge1, returned.next() );

        assertFalse( "No more edges", returned.hasNext() );

        //still edge 3 is our max version, but we start with edge 2 as our last read
        search = createSearchByEdge( edge1.getSourceNode(), edge1.getType(), edge3.getVersion(), edge2 );

        edges = em.loadEdgesFromSource( search );

        //implicitly blows up if more than 1 is returned from "single"
        returned = edges.toBlockingObservable().getIterator();

        assertEquals( "Paged correctly", edge1, returned.next() );

        assertFalse( "End of stream", returned.hasNext() );
    }


    @Test
    public void testWriteReadEdgeTypePagingTarget() {


        GraphManager em = emf.createEdgeManager( scope );


        final Id targetId = createId( "target" );

        Edge edge1 = createEdge( createId( "source" ), "test", targetId );

        em.writeEdge( edge1 ).toBlockingObservable().last();

        Edge edge2 = createEdge( createId( "source" ), "test", targetId );

        em.writeEdge( edge2 ).toBlockingObservable().last();

        Edge edge3 = createEdge( createId( "source" ), "test", targetId );

        em.writeEdge( edge3 ).toBlockingObservable().last();


        //now test retrieving it

        SearchByEdgeType search =
                createSearchByEdge( edge1.getTargetNode(), edge1.getType(), edge3.getVersion(), null );

        Observable<Edge> edges = em.loadEdgesToTarget( search );

        //implicitly blows up if more than 1 is returned from "single"
        Iterator<Edge> returned = edges.toBlockingObservable().getIterator();


        //we have 3 edges, but we specified our first edge as the max, we shouldn't get any more results than the first
        assertEquals( "Correct edge returned", edge3, returned.next() );

        assertEquals( "Correct edge returned", edge2, returned.next() );

        assertEquals( "Correct edge returned", edge1, returned.next() );


        assertFalse( "No more edges", returned.hasNext() );

        search = createSearchByEdge( edge1.getTargetNode(), edge1.getType(), edge3.getVersion(), edge2 );

        edges = em.loadEdgesToTarget( search );

        //implicitly blows up if more than 1 is returned from "single"
        returned = edges.toBlockingObservable().getIterator();

        assertEquals( "Paged correctly", edge1, returned.next() );

        assertFalse( "End of stream", returned.hasNext() );
    }


    @Test
    public void testWriteReadEdgeTypeTargetTypeSource() {

        GraphManager em = emf.createEdgeManager( scope );


        Edge edge = createEdge( "source", "test", "target" );

        em.writeEdge( edge ).toBlockingObservable().last();

        //now test retrieving it

        SearchByIdType search = createSearchByEdgeAndId( edge.getSourceNode(), edge.getType(), edge.getVersion(),
                edge.getTargetNode().getType(), null );

        Observable<Edge> edges = em.loadEdgesFromSourceByType( search );

        //implicitly blows up if more than 1 is returned from "single"
        Edge returned = edges.toBlockingObservable().single();

        assertEquals( "Correct edge returned", edge, returned );


        //change edge type to be invalid, shouldn't get a result
        search = createSearchByEdgeAndId( edge.getSourceNode(), edge.getType(), edge.getVersion(),
                edge.getTargetNode().getType() + "invalid", null );

        edges = em.loadEdgesFromSourceByType( search );

        //implicitly blows up if more than 1 is returned from "single"
        returned = edges.toBlockingObservable().singleOrDefault( null );

        assertNull( "Invalid type should not be returned", returned );
    }


    @Test
    public void testWriteReadEdgeTypeTargetTypeTarget() {

        GraphManager em = emf.createEdgeManager( scope );


        Edge edge = createEdge( "source", "test", "target" );

        em.writeEdge( edge ).toBlockingObservable().last();

        //now test retrieving it

        SearchByIdType search = createSearchByEdgeAndId( edge.getTargetNode(), edge.getType(), edge.getVersion(),
                edge.getSourceNode().getType(), null );

        Observable<Edge> edges = em.loadEdgesToTargetByType( search );

        //implicitly blows up if more than 1 is returned from "single"
        Edge returned = edges.toBlockingObservable().single();

        assertEquals( "Correct edge returned", edge, returned );


        //change edge type to be invalid, shouldn't get a result
        search = createSearchByEdgeAndId( edge.getTargetNode(), edge.getType(), edge.getVersion(),
                edge.getSourceNode().getType() + "invalid", null );

        edges = em.loadEdgesToTargetByType( search );

        //implicitly blows up if more than 1 is returned from "single"
        returned = edges.toBlockingObservable().singleOrDefault( null );

        assertNull( "Invalid type should not be returned", returned );
    }


    @Test
    public void testWriteReadEdgeDeleteSource() {

        GraphManager em = emf.createEdgeManager( scope );


        Edge edge = createEdge( "source", "test", "target" );

        em.writeEdge( edge ).toBlockingObservable().last();

        //now test retrieving it


        SearchByEdgeType search = createSearchByEdge( edge.getSourceNode(), edge.getType(), edge.getVersion(), null );

        Observable<Edge> edges = em.loadEdgesFromSource( search );

        //implicitly blows up if more than 1 is returned from "single"
        Edge returned = edges.toBlockingObservable().single();

        assertEquals( "Correct edge returned", edge, returned );

        SearchByIdType searchById = createSearchByEdgeAndId( edge.getSourceNode(), edge.getType(), edge.getVersion(),
                edge.getTargetNode().getType(), null );

        edges = em.loadEdgesFromSourceByType( searchById );

        //implicitly blows up if more than 1 is returned from "single"
        returned = edges.toBlockingObservable().single();

        assertEquals( "Correct edge returned", edge, returned );

        final SearchByEdge searchByEdge = createGetByEdge(edge.getSourceNode(), edge.getType(), edge.getTargetNode(), edge.getVersion(), null);

        returned = em.loadEdgeVersions(searchByEdge).toBlockingObservable().single();


        assertEquals( "Correct edge returned", edge, returned );


        //now delete it
        returned = em.deleteEdge( edge ).toBlockingObservable().last();


        assertEquals( "Correct edge returned", edge, returned );


        //now test retrieval, should be null
        edges = em.loadEdgesFromSource( search );

        //implicitly blows up if more than 1 is returned from "single"
        returned = edges.toBlockingObservable().singleOrDefault( null );

        assertNull( "No edge returned", returned );



        //no search by type, should be null as well

        edges = em.loadEdgesFromSourceByType( searchById );

        //implicitly blows up if more than 1 is returned from "single"
        returned = edges.toBlockingObservable().singleOrDefault( null );

        assertNull( "No edge returned", returned );

        returned = em.loadEdgeVersions(searchByEdge).toBlockingObservable().singleOrDefault(null);

        assertNull( "No edge returned", returned );
    }


    @Test
    public void testWriteReadEdgeDeleteTarget() {

        GraphManager em = emf.createEdgeManager( scope );


        Edge edge = createEdge( "source", "test", "target" );

        em.writeEdge( edge ).toBlockingObservable().last();

        //now test retrieving it


        SearchByEdgeType search = createSearchByEdge( edge.getTargetNode(), edge.getType(), edge.getVersion(), null );

        Observable<Edge> edges = em.loadEdgesToTarget( search );

        //implicitly blows up if more than 1 is returned from "single"
        Edge returned = edges.toBlockingObservable().single();

        assertEquals( "Correct edge returned", edge, returned );

        SearchByIdType searchById = createSearchByEdgeAndId( edge.getTargetNode(), edge.getType(), edge.getVersion(),
                edge.getSourceNode().getType(), null );

        edges = em.loadEdgesToTargetByType( searchById );

        //implicitly blows up if more than 1 is returned from "single"
        returned = edges.toBlockingObservable().single();

        assertEquals( "Correct edge returned", edge, returned );


        //now delete it
        em.deleteEdge( edge ).toBlockingObservable().last();

        //now test retrieval, should be null
        edges = em.loadEdgesToTarget( search );

        //implicitly blows up if more than 1 is returned from "single"
        returned = edges.toBlockingObservable().singleOrDefault( null );

        assertNull( "No edge returned", returned );


        //no search by type, should be null as well

        edges = em.loadEdgesToTargetByType( searchById );

        //implicitly blows up if more than 1 is returned from "single"
        returned = edges.toBlockingObservable().singleOrDefault( null );

        assertNull( "No edge returned", returned );
    }


    @Test
    public void testWriteReadEdgeTypesSourceTypes() {

        final GraphManager em = emf.createEdgeManager( scope );

        Id sourceId = new SimpleId( "source" );
        Id targetId1 = new SimpleId( "target" );
        Id targetId2 = new SimpleId( "target2" );

        Edge testTargetEdge = createEdge( sourceId, "test", targetId1, UUIDGenerator.newTimeUUID() );

        em.writeEdge( testTargetEdge ).toBlockingObservable().singleOrDefault( null );

        Edge testTarget2Edge = createEdge( sourceId, "test", targetId2, UUIDGenerator.newTimeUUID() );

        em.writeEdge( testTarget2Edge ).toBlockingObservable().singleOrDefault( null );


        Edge test2TargetEdge = createEdge( sourceId, "test2", targetId1, UUIDGenerator.newTimeUUID() );

        em.writeEdge( test2TargetEdge ).toBlockingObservable().singleOrDefault( null );


        //get our 2 edge types
        Observable<String> edges =
                em.getEdgeTypesFromSource( new SimpleSearchEdgeType( testTargetEdge.getSourceNode(), null ) );


        Iterator<String> results = edges.toBlockingObservable().getIterator();


        assertEquals( "Edges correct", "test", results.next() );

        assertEquals( "Edges correct", "test2", results.next() );

        assertFalse( "No more edges", results.hasNext() );


        //now test sub edges

        edges = em.getIdTypesFromSource( new SimpleSearchIdType( testTargetEdge.getSourceNode(), "test", null ) );

        results = edges.toBlockingObservable().getIterator();


        assertEquals( "Types correct", targetId1.getType(), results.next() );

        assertEquals( "Types correct", targetId2.getType(), results.next() );

        assertFalse( "No results", results.hasNext() );

        //now get types for test2
        edges = em.getIdTypesFromSource( new SimpleSearchIdType( testTargetEdge.getSourceNode(), "test2", null ) );

        results = edges.toBlockingObservable().getIterator();

        assertEquals( "Types correct", targetId1.getType(), results.next() );

        assertFalse( "No results", results.hasNext() );
    }


    @Test
    public void testWriteReadEdgeTypesTargetTypes() {

        final GraphManager em = emf.createEdgeManager( scope );

        Id sourceId1 = new SimpleId( "source" );
        Id sourceId2 = new SimpleId( "source2" );
        Id targetId1 = new SimpleId( "target" );


        Edge testTargetEdge = createEdge( sourceId1, "test", targetId1, UUIDGenerator.newTimeUUID() );

        em.writeEdge( testTargetEdge ).toBlockingObservable().singleOrDefault( null );

        Edge testTarget2Edge = createEdge( sourceId2, "test", targetId1, UUIDGenerator.newTimeUUID() );

        em.writeEdge( testTarget2Edge ).toBlockingObservable().singleOrDefault( null );


        Edge test2TargetEdge = createEdge( sourceId1, "test2", targetId1, UUIDGenerator.newTimeUUID() );

        em.writeEdge( test2TargetEdge ).toBlockingObservable().singleOrDefault( null );


        //get our 2 edge types
        final SearchEdgeType edgeTypes = new SimpleSearchEdgeType( testTargetEdge.getTargetNode(), null );

        Observable<String> edges = em.getEdgeTypesToTarget( edgeTypes );


        Iterator<String> results = edges.toBlockingObservable().getIterator();


        assertEquals( "Edges correct", "test", results.next() );

        assertEquals( "Edges correct", "test2", results.next() );

        assertFalse( "No more edges", results.hasNext() );


        //now test sub edges

        edges = em.getIdTypesToTarget( new SimpleSearchIdType( testTargetEdge.getTargetNode(), "test", null ) );

        results = edges.toBlockingObservable().getIterator();

        assertEquals( "Types correct", sourceId1.getType(), results.next() );

        assertEquals( "Types correct", sourceId2.getType(), results.next() );

        assertFalse( "No more edges", results.hasNext() );


        //now get types for test2
        edges = em.getIdTypesToTarget( new SimpleSearchIdType( testTargetEdge.getTargetNode(), "test2", null ) );

        results = edges.toBlockingObservable().getIterator();


        assertEquals( "Types correct", sourceId1.getType(), results.next() );

        assertFalse( "No more edges", results.hasNext() );
    }


    @Test
    public void testWriteReadEdgeTypesSourceTypesPaging() {

        final GraphManager em = emf.createEdgeManager( scope );

        Id sourceId1 = new SimpleId( "source" );
        Id targetId1 = new SimpleId( "target" );
        Id targetId2 = new SimpleId( "target2" );


        Edge testTargetEdge = createEdge( sourceId1, "test", targetId1, UUIDGenerator.newTimeUUID() );

        em.writeEdge( testTargetEdge ).toBlockingObservable().singleOrDefault( null );


        Edge testTargetEdge2 = createEdge( sourceId1, "test", targetId2, UUIDGenerator.newTimeUUID() );

        em.writeEdge( testTargetEdge2 ).toBlockingObservable().singleOrDefault( null );


        Edge test2TargetEdge = createEdge( sourceId1, "test2", targetId2, UUIDGenerator.newTimeUUID() );

        em.writeEdge( test2TargetEdge ).toBlockingObservable().singleOrDefault( null );


        //get our 2 edge types
        SearchEdgeType edgeTypes = new SimpleSearchEdgeType( testTargetEdge.getSourceNode(), null );

        Observable<String> edges = em.getEdgeTypesFromSource( edgeTypes );


        Iterator<String> results = edges.toBlockingObservable().getIterator();


        assertEquals( "Edges correct", "test", results.next() );
        assertEquals( "Edges correct", "test2", results.next() );
        assertFalse( "No more edges", results.hasNext() );

        //now load the next page

        edgeTypes = new SimpleSearchEdgeType( testTargetEdge.getSourceNode(), "test" );

        edges = em.getEdgeTypesFromSource( edgeTypes );


        results = edges.toBlockingObservable().getIterator();


        assertEquals( "Edges correct", "test2", results.next() );
        assertFalse( "No more edges", results.hasNext() );


        //now test sub edges

        edges = em.getIdTypesFromSource( new SimpleSearchIdType( testTargetEdge.getSourceNode(), "test", null ) );

        results = edges.toBlockingObservable().getIterator();


        assertEquals( "Types correct", targetId1.getType(), results.next() );
        assertEquals( "Types correct", targetId2.getType(), results.next() );
        assertFalse( "No more edges", results.hasNext() );


        //now get the next page

        edges = em.getIdTypesFromSource(
                new SimpleSearchIdType( testTargetEdge.getSourceNode(), "test", targetId1.getType() ) );

        results = edges.toBlockingObservable().getIterator();


        assertEquals( "Types correct", targetId2.getType(), results.next() );

        assertFalse( "No more results", results.hasNext() );
    }


    @Test
    public void testWriteReadEdgeTypesTargetTypesPaging() {

        final GraphManager em = emf.createEdgeManager( scope );

        Id sourceId1 = new SimpleId( "source" );
        Id sourceId2 = new SimpleId( "source2" );
        Id targetId = new SimpleId( "target" );


        Edge testTargetEdge = createEdge( sourceId1, "test", targetId, UUIDGenerator.newTimeUUID() );

        em.writeEdge( testTargetEdge ).toBlockingObservable().singleOrDefault( null );


        Edge testTargetEdge2 = createEdge( sourceId2, "test", targetId, UUIDGenerator.newTimeUUID() );

        em.writeEdge( testTargetEdge2 ).toBlockingObservable().singleOrDefault( null );

        Edge test2TargetEdge = createEdge( sourceId2, "test2", targetId, UUIDGenerator.newTimeUUID() );

        em.writeEdge( test2TargetEdge ).toBlockingObservable().singleOrDefault( null );


        //get our 2 edge types
        SearchEdgeType edgeTypes = new SimpleSearchEdgeType( testTargetEdge.getTargetNode(), null );

        Observable<String> edges = em.getEdgeTypesToTarget( edgeTypes );


        Iterator<String> results = edges.toBlockingObservable().getIterator();


        assertEquals( "Edges correct", "test", results.next() );
        assertEquals( "Edges correct", "test2", results.next() );

        assertFalse( "No more edges", results.hasNext() );


        //now load the next page

        edgeTypes = new SimpleSearchEdgeType( testTargetEdge2.getTargetNode(), "test" );

        edges = em.getEdgeTypesToTarget( edgeTypes );


        results = edges.toBlockingObservable().getIterator();


        assertEquals( "Edges correct", "test2", results.next() );


        assertFalse( "No more edges", results.hasNext() );

        //now test sub edges

        edges = em.getIdTypesToTarget( new SimpleSearchIdType( testTargetEdge.getTargetNode(), "test", null ) );

        results = edges.toBlockingObservable().getIterator();


        assertEquals( "Types correct", sourceId1.getType(), results.next() );

        assertEquals( "Types correct", sourceId2.getType(), results.next() );

        assertFalse( "No more edges", results.hasNext() );

        //now get the next page

        edges = em.getIdTypesToTarget(
                new SimpleSearchIdType( testTargetEdge.getTargetNode(), "test", sourceId1.getType() ) );

        results = edges.toBlockingObservable().getIterator();


        assertEquals( "Types correct", sourceId2.getType(), results.next() );

        assertFalse( "No more results", results.hasNext() );
    }


    @Test
    public void testMarkSourceEdges() {

        final GraphManager em = emf.createEdgeManager( scope );

        Id sourceId = new SimpleId( "source" );
        Id targetId1 = new SimpleId( "target" );
        Id targetId2 = new SimpleId( "target2" );

        Edge edge1 = createEdge( sourceId, "test", targetId1, UUIDGenerator.newTimeUUID() );

        em.writeEdge( edge1 ).toBlockingObservable().singleOrDefault( null );

        Edge edge2 = createEdge( sourceId, "test", targetId2, UUIDGenerator.newTimeUUID() );

        em.writeEdge( edge2 ).toBlockingObservable().singleOrDefault( null );


        final UUID maxVersion = UUIDGenerator.newTimeUUID();


        //get our 2 edges
        Observable<Edge> edges = em.loadEdgesFromSource(
                createSearchByEdge( edge1.getSourceNode(), edge1.getType(), maxVersion, null ) );


        Iterator<Edge> results = edges.toBlockingObservable().getIterator();


        assertEquals( "Edges correct", edge2, results.next() );

        assertEquals( "Edges correct", edge1, results.next() );

        assertFalse( "No more edges", results.hasNext() );

        //now delete one of the edges

        em.deleteEdge( edge1 ).toBlockingObservable().last();


        edges = em.loadEdgesFromSource(
                createSearchByEdge( edge1.getSourceNode(), edge1.getType(), maxVersion, null ) );


        results = edges.toBlockingObservable().getIterator();


        assertEquals( "Edges correct", edge2, results.next() );

        assertFalse( "No more edges", results.hasNext() );

        //now delete one of the edges

        em.deleteEdge( edge2 ).toBlockingObservable().last();

        edges = em.loadEdgesFromSource(
                createSearchByEdge( edge1.getSourceNode(), edge1.getType(), maxVersion, null ) );


        results = edges.toBlockingObservable().getIterator();


        assertFalse( "No more edges", results.hasNext() );

        //now delete one of the edges

    }


    @Test
    public void testMarkTargetEdges() {

        final GraphManager em = emf.createEdgeManager( scope );

        Id sourceId1 = new SimpleId( "source" );
        Id sourceId2 = new SimpleId( "source2" );
        Id targetId = new SimpleId( "target" );

        Edge edge1 = createEdge( sourceId1, "test", targetId, UUIDGenerator.newTimeUUID() );

        em.writeEdge( edge1 ).toBlockingObservable().last();

        Edge edge2 = createEdge( sourceId2, "test", targetId, UUIDGenerator.newTimeUUID() );

        em.writeEdge( edge2 ).toBlockingObservable().last();


        final UUID maxVersion = UUIDGenerator.newTimeUUID();


        //get our 2 edges
        Observable<Edge> edges =
                em.loadEdgesToTarget( createSearchByEdge( edge1.getTargetNode(), edge1.getType(), maxVersion, null ) );


        Iterator<Edge> results = edges.toBlockingObservable().getIterator();


        assertEquals( "Edges correct", edge2, results.next() );

        assertEquals( "Edges correct", edge1, results.next() );


        assertFalse( "No more edges", results.hasNext() );

        //now delete one of the edges

        em.deleteEdge( edge1 ).toBlockingObservable().last();


        edges = em.loadEdgesToTarget( createSearchByEdge( edge1.getTargetNode(), edge1.getType(), maxVersion, null ) );


        results = edges.toBlockingObservable().getIterator();


        assertEquals( "Edges correct", edge2, results.next() );

        assertFalse( "No more edges", results.hasNext() );

        //now delete one of the edges

        em.deleteEdge( edge2 ).toBlockingObservable().last();

        edges = em.loadEdgesToTarget( createSearchByEdge( edge1.getTargetNode(), edge1.getType(), maxVersion, null ) );


        results = edges.toBlockingObservable().getIterator();


        assertFalse( "No more edges", results.hasNext() );

        //now delete one of the edges

    }


    @Test
    public void testMarkSourceEdgesType() {

        final GraphManager em = emf.createEdgeManager( scope );

        Id sourceId = new SimpleId( "source" );
        Id targetId1 = new SimpleId( "target" );
        Id targetId2 = new SimpleId( "target2" );

        Edge edge1 = createEdge( sourceId, "test", targetId1, UUIDGenerator.newTimeUUID() );

        em.writeEdge( edge1 ).toBlockingObservable().singleOrDefault( null );

        Edge edge2 = createEdge( sourceId, "test", targetId2, UUIDGenerator.newTimeUUID() );

        em.writeEdge( edge2 ).toBlockingObservable().singleOrDefault( null );


        final UUID maxVersion = UUIDGenerator.newTimeUUID();


        //get our 2 edges
        Observable<Edge> edges = em.loadEdgesFromSourceByType(
                createSearchByEdgeAndId( sourceId, edge1.getType(), maxVersion, targetId1.getType(), null ) );


        Iterator<Edge> results = edges.toBlockingObservable().getIterator();


        assertEquals( "Edges correct", edge1, results.next() );

        assertFalse( "No more edges", results.hasNext() );

        //now delete one of the edges

        em.deleteEdge( edge1 ).toBlockingObservable().last();


        edges = em.loadEdgesFromSourceByType(
                createSearchByEdgeAndId( sourceId, edge1.getType(), maxVersion, targetId1.getType(), null ) );

        results = edges.toBlockingObservable().getIterator();


        assertFalse( "No more edges", results.hasNext() );


        edges = em.loadEdgesFromSourceByType(
                createSearchByEdgeAndId( sourceId, edge1.getType(), maxVersion, targetId2.getType(), null ) );


        results = edges.toBlockingObservable().getIterator();


        assertEquals( "Edges correct", edge2, results.next() );

        assertFalse( "No more edges", results.hasNext() );

        //now delete one of the edges

        em.deleteEdge( edge2 ).toBlockingObservable().last();


        edges = em.loadEdgesFromSourceByType(
                createSearchByEdgeAndId( sourceId, edge1.getType(), maxVersion, targetId2.getType(), null ) );


        results = edges.toBlockingObservable().getIterator();


        assertFalse( "No more edges", results.hasNext() );


        //now delete one of the edges

    }


    @Test
    public void testMarkTargetEdgesType() {

        final GraphManager em = emf.createEdgeManager( scope );

        Id sourceId1 = new SimpleId( "source" );
        Id sourceId2 = new SimpleId( "source2" );
        Id targetId = new SimpleId( "target" );

        Edge edge1 = createEdge( sourceId1, "test", targetId, UUIDGenerator.newTimeUUID() );

        em.writeEdge( edge1 ).toBlockingObservable().last();

        Edge edge2 = createEdge( sourceId2, "test", targetId, UUIDGenerator.newTimeUUID() );

        em.writeEdge( edge2 ).toBlockingObservable().last();


        final UUID maxVersion = UUIDGenerator.newTimeUUID();

        //get our 2 edges
        Observable<Edge> edges = em.loadEdgesToTargetByType(
                createSearchByEdgeAndId( targetId, edge1.getType(), maxVersion, sourceId1.getType(), null ) );


        Iterator<Edge> results = edges.toBlockingObservable().getIterator();


        assertEquals( "Edges correct", edge1, results.next() );

        assertFalse( "No more edges", results.hasNext() );

        //now delete one of the edges

        em.deleteEdge( edge1 ).toBlockingObservable().last();


        edges = em.loadEdgesToTargetByType(
                createSearchByEdgeAndId( edge1.getSourceNode(), edge1.getType(), maxVersion, sourceId1.getType(),
                        null ) );

        results = edges.toBlockingObservable().getIterator();


        assertFalse( "No more edges", results.hasNext() );


        edges = em.loadEdgesToTargetByType(
                createSearchByEdgeAndId( targetId, edge1.getType(), maxVersion, sourceId2.getType(), null ) );


        results = edges.toBlockingObservable().getIterator();


        assertEquals( "Edges correct", edge2, results.next() );

        assertFalse( "No more edges", results.hasNext() );

        //now delete one of the edges

        em.deleteEdge( edge2 ).toBlockingObservable().last();


        edges = em.loadEdgesToTargetByType(
                createSearchByEdgeAndId( targetId, edge1.getType(), maxVersion, sourceId2.getType(), null ) );


        results = edges.toBlockingObservable().getIterator();


        assertFalse( "No more edges", results.hasNext() );


        //now delete one of the edges

    }


    @Test
    public void markSourceNode() {

        final GraphManager em = emf.createEdgeManager( scope );

        Id sourceId = new SimpleId( "source" );
        Id targetId1 = new SimpleId( "target" );
        Id targetId2 = new SimpleId( "target2" );

        Edge edge1 = createEdge( sourceId, "test", targetId1, UUIDGenerator.newTimeUUID() );

        em.writeEdge( edge1 ).toBlockingObservable().singleOrDefault( null );

        Edge edge2 = createEdge( sourceId, "test", targetId2, UUIDGenerator.newTimeUUID() );

        em.writeEdge( edge2 ).toBlockingObservable().singleOrDefault( null );


        final UUID maxVersion = UUIDGenerator.newTimeUUID();

        Iterator<Edge> results =
                em.loadEdgesFromSource( createSearchByEdge( sourceId, edge1.getType(), maxVersion, null ) )
                  .toBlockingObservable().getIterator();


        assertEquals( "Edge found", edge2, results.next() );

        assertEquals( "Edge found", edge1, results.next() );

        assertFalse( "No more edges", results.hasNext() );


        //get our 2 edges
        results = em.loadEdgesFromSourceByType(
                createSearchByEdgeAndId( sourceId, edge1.getType(), maxVersion, targetId1.getType(), null ) )
                    .toBlockingObservable().getIterator();


        assertEquals( "Edges correct", edge1, results.next() );

        assertFalse( "No more edges", results.hasNext() );

        //now delete one of the edges
        results = em.loadEdgesFromSourceByType(
                createSearchByEdgeAndId( sourceId, edge2.getType(), maxVersion, targetId2.getType(), null ) )
                    .toBlockingObservable().getIterator();


        assertEquals( "Edges correct", edge2, results.next() );

        assertFalse( "No more edges", results.hasNext() );

        //mark the source node
        em.deleteNode( sourceId ).toBlockingObservable().last();


        //now re-read, nothing should be there since they're marked

        results = em.loadEdgesFromSource( createSearchByEdge( sourceId, edge1.getType(), maxVersion, null ) )
                    .toBlockingObservable().getIterator();

        assertFalse( "No more edges", results.hasNext() );


        //get our 2 edges
        results = em.loadEdgesFromSourceByType(
                createSearchByEdgeAndId( sourceId, edge1.getType(), maxVersion, targetId1.getType(), null ) )
                    .toBlockingObservable().getIterator();


        assertFalse( "No more edges", results.hasNext() );

        //now delete one of the edges
        results = em.loadEdgesFromSourceByType(
                createSearchByEdgeAndId( sourceId, edge2.getType(), maxVersion, targetId2.getType(), null ) )
                    .toBlockingObservable().getIterator();


        assertFalse( "No more edges", results.hasNext() );
    }


    @Test
    public void markTargetNode() {

        final GraphManager em = emf.createEdgeManager( scope );

        Id sourceId1 = new SimpleId( "source" );
        Id sourceId2 = new SimpleId( "source2" );
        Id targetId = new SimpleId( "target" );

        Edge edge1 = createEdge( sourceId1, "test", targetId, UUIDGenerator.newTimeUUID() );

        em.writeEdge( edge1 ).toBlockingObservable().singleOrDefault( null );

        Edge edge2 = createEdge( sourceId2, "test", targetId, UUIDGenerator.newTimeUUID() );

        em.writeEdge( edge2 ).toBlockingObservable().singleOrDefault( null );


        final UUID maxVersion = UUIDGenerator.newTimeUUID();

        Iterator<Edge> results =
                em.loadEdgesToTarget( createSearchByEdge( targetId, edge1.getType(), maxVersion, null ) )
                  .toBlockingObservable().getIterator();


        assertEquals( "Edge found", edge2, results.next() );

        assertEquals( "Edge found", edge1, results.next() );

        assertFalse( "No more edges", results.hasNext() );


        //get our 2 edges
        results = em.loadEdgesToTargetByType(
                createSearchByEdgeAndId( targetId, edge1.getType(), maxVersion, sourceId1.getType(), null ) )
                    .toBlockingObservable().getIterator();


        assertEquals( "Edges correct", edge1, results.next() );

        assertFalse( "No more edges", results.hasNext() );

        //now delete one of the edges
        results = em.loadEdgesToTargetByType(
                createSearchByEdgeAndId( targetId, edge2.getType(), maxVersion, sourceId2.getType(), null ) )
                    .toBlockingObservable().getIterator();


        assertEquals( "Edges correct", edge2, results.next() );

        assertFalse( "No more edges", results.hasNext() );

        //mark the source node
        em.deleteNode( targetId ).toBlockingObservable().last();


        //now re-read, nothing should be there since they're marked

        results = em.loadEdgesToTarget( createSearchByEdge( targetId, edge1.getType(), maxVersion, null ) )
                    .toBlockingObservable().getIterator();

        assertFalse( "No more edges", results.hasNext() );


        //get our 2 edges
        results = em.loadEdgesToTargetByType(
                createSearchByEdgeAndId( targetId, edge1.getType(), maxVersion, sourceId1.getType(), null ) )
                    .toBlockingObservable().getIterator();


        assertFalse( "No more edges", results.hasNext() );

        //now delete one of the edges
        results = em.loadEdgesToTargetByType(
                createSearchByEdgeAndId( targetId, edge2.getType(), maxVersion, sourceId2.getType(), null ) )
                    .toBlockingObservable().getIterator();


        assertFalse( "No more edges", results.hasNext() );
    }


    @Test(expected = NullPointerException.class)
    public void invalidEdgeTypesWrite( @All Edge edge ) {
        final GraphManager em = emf.createEdgeManager( scope );

        em.writeEdge( edge );
    }


    @Test(expected = NullPointerException.class)
    public void invalidEdgeTypesDelete( @All Edge edge ) {
        final GraphManager em = emf.createEdgeManager( scope );

        em.deleteEdge( edge );
    }

    //
    //    public static class InvalidInput extends JukitoModule {
    //
    //        @Override
    //        protected void configureTest() {
    //create all edge types of junk input
    //
    //            final UUID version = UUIDGenerator.newTimeUUID();
    //
    //            Id nullUuid = mock( Id.class );
    //            when( nullUuid.getUuid() ).thenReturn( null );
    //
    //
    //            Id nullType = mock( Id.class );
    //            when( nullType.getType() ).thenReturn( "type" );
    //
    //            Edge[] edges = new Edge[] {
    //                    mockEdge( nullUuid, "test", createId( "target" ), version ),
    //
    //                    mockEdge( nullType, "test", createId( "target" ), version ),
    //
    //                    mockEdge( createId( "source" ), null, createId( "target" ), version ),
    //
    //                    mockEdge( createId( "source" ), "test", nullUuid, version ),
    //
    //                    mockEdge( createId( "source" ), "test", nullType, version ),
    //
    //                    mockEdge( createId( "source" ), "test", createId( "target" ), null )
    //            };
    //
    //
    //            bindManyInstances( Edge.class, edges );
    //
    //        }
    //
    //
    //        private Edge mockEdge( final Id sourceId, final String type, final Id targetId, final UUID version ) {
    //            Edge edge = mock( Edge.class );
    //
    //            when( edge.getSourceNode() ).thenReturn( sourceId );
    //            when( edge.getType() ).thenReturn( type );
    //            when( edge.getTargetNode() ).thenReturn( targetId );
    //            when( edge.getVersion() ).thenReturn( version );
    //
    //            return edge;
    //        }
    //    }
}





