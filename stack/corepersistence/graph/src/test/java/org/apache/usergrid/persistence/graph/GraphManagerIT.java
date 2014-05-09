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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jukito.All;
import org.jukito.JukitoRunner;
import org.jukito.UseModules;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.usergrid.persistence.core.scope.OrganizationScope;
import org.apache.usergrid.persistence.core.cassandra.CassandraRule;
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



public abstract class GraphManagerIT {


    @ClassRule
    public static CassandraRule rule = new CassandraRule();


    @Inject
    @Rule
    public MigrationManagerRule migrationManagerRule;


    @Inject
    protected GraphManagerFactory emf;

    protected OrganizationScope scope;


    /**
     * Get the helper for performing our tests
     * @param gm
     * @return  The helper to use when writing/synchronizing tests
     */
    protected abstract GraphManager getHelper(GraphManager gm);

    @Before
    public void setup() {
        scope = mock( OrganizationScope.class );

        Id orgId = mock( Id.class );

        when( orgId.getType() ).thenReturn( "organization" );
        when( orgId.getUuid() ).thenReturn( UUIDGenerator.newTimeUUID() );

        when( scope.getOrganization() ).thenReturn( orgId );
    }


    @Test
    public void testWriteReadEdgeTypeSource() throws TimeoutException, InterruptedException {

        GraphManager gm = getHelper(emf.createEdgeManager( scope ));

        Edge edge = createEdge( "source", "test", "target" );

        gm.writeEdge( edge ).toBlockingObservable().last();

        //now test retrieving it

        SearchByEdgeType search = createSearchByEdge( edge.getSourceNode(), edge.getType(), edge.getVersion(), null );

        Observable<Edge> edges = gm.loadEdgesFromSource( search );

        //implicitly blows up if more than 1 is returned from "single"
        Edge returned = edges.toBlockingObservable().last();

        assertEquals( "Correct edge returned", edge, returned );

        //change edge type to be invalid, shouldn't get a result
        search = createSearchByEdge( edge.getSourceNode(), edge.getType() + "invalid", edge.getVersion(), null );

        edges = gm.loadEdgesFromSource( search );

        //implicitly blows up if more than 1 is returned from "single"
        returned = edges.toBlockingObservable().singleOrDefault( null );

        assertNull( "Invalid type should not be returned", returned );
    }


    @Test
    public void testWriteReadEdgeTypeTarget() throws TimeoutException, InterruptedException {

        GraphManager gm =  getHelper(emf.createEdgeManager( scope ));

        Edge edge = createEdge( "source", "test", "target" );

        gm.writeEdge( edge ).toBlockingObservable().last();

        //now test retrieving it

        SearchByEdgeType search = createSearchByEdge( edge.getTargetNode(), edge.getType(), edge.getVersion(), null );

        Observable<Edge> edges = gm.loadEdgesToTarget( search );

        //implicitly blows up if more than 1 is returned from "single"
        Edge returned = edges.toBlockingObservable().single();

        assertEquals( "Correct edge returned", edge, returned );

        //change edge type to be invalid, shouldn't get a result
        search = createSearchByEdge( edge.getTargetNode(), edge.getType() + "invalid", edge.getVersion(), null );

        edges = gm.loadEdgesToTarget( search );

        //implicitly blows up if more than 1 is returned from "single"
        returned = edges.toBlockingObservable().singleOrDefault( null );

        assertNull( "Invalid type should not be returned", returned );
    }


    @Test
    public void testWriteReadEdgeTypeVersionSource() throws TimeoutException, InterruptedException {

        GraphManager gm =  getHelper(emf.createEdgeManager( scope ));

        final UUID earlyVersion = UUIDGenerator.newTimeUUID();

        Edge edge = createEdge( "source", "test", "target" );

        gm.writeEdge( edge ).toBlockingObservable().last();

        //now test retrieving it

        SearchByEdgeType search = createSearchByEdge( edge.getSourceNode(), edge.getType(), edge.getVersion(), null );

        Observable<Edge> edges = gm.loadEdgesFromSource( search );

        //implicitly blows up if more than 1 is returned from "single"
        Edge returned = edges.toBlockingObservable().single();

        assertEquals( "Correct edge returned", edge, returned );

        //now test with an earlier version, we shouldn't get the edge back
        search = createSearchByEdge( edge.getSourceNode(), edge.getType(), earlyVersion, null );

        edges = gm.loadEdgesFromSource( search );

        //implicitly blows up if more than 1 is returned from "single"
        returned = edges.toBlockingObservable().singleOrDefault( null );

        assertNull( "Earlier version should not be returned", returned );
    }


    @Test
    public void testWriteReadEdgeTypeVersionTarget() throws TimeoutException, InterruptedException {

        GraphManager gm = getHelper(emf.createEdgeManager( scope ));

        final UUID earlyVersion = UUIDGenerator.newTimeUUID();


        Edge edge = createEdge( "source", "test", "target" );

        gm.writeEdge( edge ).toBlockingObservable().last();

        //now test retrieving it

        SearchByEdgeType search = createSearchByEdge( edge.getTargetNode(), edge.getType(), edge.getVersion(), null );

        Observable<Edge> edges = gm.loadEdgesToTarget( search );

        //implicitly blows up if more than 1 is returned from "single"
        Edge returned = edges.toBlockingObservable().single();

        assertEquals( "Correct edge returned", edge, returned );

        //change edge type to be invalid, shouldn't get a result
        search = createSearchByEdge( edge.getTargetNode(), edge.getType(), earlyVersion, null );

        edges = gm.loadEdgesToTarget( search );

        //implicitly blows up if more than 1 is returned from "single"
        returned = edges.toBlockingObservable().singleOrDefault( null );

        assertNull( "Earlier version should not be returned", returned );
    }


    /**
     * Tests that if multiple versions of an edge exist, only the distinct edges with a version <= max are returned
     */
    @Test
    public void testWriteReadEdgeTypeVersionSourceDistinct() throws TimeoutException, InterruptedException {

        GraphManager gm = getHelper(emf.createEdgeManager( scope ));

        final UUID earlyVersion = UUIDGenerator.newTimeUUID();


        Edge edge1 = createEdge( "source", "test", "target" );

        final Id sourceId = edge1.getSourceNode();
        final Id targetId = edge1.getTargetNode();


        gm.writeEdge( edge1 ).toBlockingObservable().last();

        Edge edge2 = createEdge( sourceId, edge1.getType(), targetId );

        gm.writeEdge( edge2 ).toBlockingObservable().last();

        Edge edge3 = createEdge( sourceId, edge1.getType(), targetId );

        gm.writeEdge( edge3 ).toBlockingObservable().last();


        //now test retrieving it, we should only get edge3, since it's the latest

        SearchByEdgeType search =
                createSearchByEdge( edge1.getSourceNode(), edge1.getType(), edge3.getVersion(), null );

        Observable<Edge> edges = gm.loadEdgesFromSource( search );

        //implicitly blows up if more than 1 is returned from "single"
        Iterator<Edge> returned = edges.toBlockingObservable().getIterator();

        assertEquals( "Correct edge returned", edge3, returned.next() );
        assertEquals( "Correct edge returned", edge2, returned.next() );
        assertEquals( "Correct edge returned", edge1, returned.next() );
        assertFalse( "No more edges", returned.hasNext() );

        //now test with an earlier version, we shouldn't get the edge back
        search = createSearchByEdge( edge1.getSourceNode(), edge1.getType(), edge2.getVersion(), null );

        edges = gm.loadEdgesFromSource( search );

        returned = edges.toBlockingObservable().getIterator();

        assertEquals( "Correct edge returned", edge2, returned.next() );
        assertEquals( "Correct edge returned", edge1, returned.next() );
        assertFalse( "No more edges", returned.hasNext() );

        search = createSearchByEdge( edge1.getSourceNode(), edge1.getType(), edge1.getVersion(), null );

        edges = gm.loadEdgesFromSource( search );

        returned = edges.toBlockingObservable().getIterator();

        assertEquals( "Correct edge returned", edge1, returned.next() );
        assertFalse( "No more edges", returned.hasNext() );


        search = createSearchByEdge( edge1.getSourceNode(), edge1.getType(), earlyVersion, null );

        edges = gm.loadEdgesFromSource( search );

        returned = edges.toBlockingObservable().getIterator();

        assertFalse( "No more edges", returned.hasNext() );
    }


    @Test
    public void testWriteReadEdgeTypeVersionTargetDistinct() throws TimeoutException, InterruptedException {


        GraphManager gm = getHelper(emf.createEdgeManager( scope ));;


        final UUID earlyVersion = UUIDGenerator.newTimeUUID();


        Edge edge1 = createEdge( "source", "test", "target" );

        final Id sourceId = edge1.getSourceNode();
        final Id targetId = edge1.getTargetNode();


        gm.writeEdge( edge1 ).toBlockingObservable().last();

        Edge edge2 = createEdge( sourceId, edge1.getType(), targetId );

        gm.writeEdge( edge2 ).toBlockingObservable().last();

        Edge edge3 = createEdge( sourceId, edge1.getType(), targetId );

        gm.writeEdge( edge3 ).toBlockingObservable().last();


        //now test retrieving it, we should only get edge3, since it's the latest

        SearchByEdgeType search =
                createSearchByEdge( edge1.getTargetNode(), edge1.getType(), edge3.getVersion(), null );

        Observable<Edge> edges = gm.loadEdgesToTarget( search );

        //implicitly blows up if more than 1 is returned from "single"
        Iterator<Edge> returned = edges.toBlockingObservable().getIterator();

        assertEquals( "Correct edge returned", edge3, returned.next() );
        assertEquals( "Correct edge returned", edge2, returned.next() );
        assertEquals( "Correct edge returned", edge1, returned.next() );
        assertFalse( "No more edges", returned.hasNext() );

        //now test with an earlier version, we shouldn't get the edge back
        search = createSearchByEdge( edge1.getTargetNode(), edge1.getType(), edge2.getVersion(), null );

        edges = gm.loadEdgesToTarget( search );

        returned = edges.toBlockingObservable().getIterator();

        assertEquals( "Correct edge returned", edge2, returned.next() );
        assertEquals( "Correct edge returned", edge1, returned.next() );
        assertFalse( "No more edges", returned.hasNext() );

        search = createSearchByEdge( edge1.getTargetNode(), edge1.getType(), edge1.getVersion(), null );

        edges = gm.loadEdgesToTarget( search );

        returned = edges.toBlockingObservable().getIterator();

        assertEquals( "Correct edge returned", edge1, returned.next() );
        assertFalse( "No more edges", returned.hasNext() );


        search = createSearchByEdge( edge1.getTargetNode(), edge1.getType(), earlyVersion, null );

        edges = gm.loadEdgesToTarget( search );

        returned = edges.toBlockingObservable().getIterator();

        assertFalse( "No more edges", returned.hasNext() );
    }


    @Test
    public void testWriteReadEdgeTypePagingSource() throws TimeoutException, InterruptedException {

        GraphManager gm = getHelper(emf.createEdgeManager( scope ));
        final Id sourceId = createId( "source" );


        Edge edge1 = createEdge( sourceId, "test", createId( "target" ) );

        gm.writeEdge( edge1 ).toBlockingObservable().last();

        Edge edge2 = createEdge( sourceId, "test", createId( "target" ) );

        gm.writeEdge( edge2 ).toBlockingObservable().last();

        Edge edge3 = createEdge( sourceId, "test", createId( "target" ) );

        gm.writeEdge( edge3 ).toBlockingObservable().last();


        //now test retrieving it

        SearchByEdgeType search =
                createSearchByEdge( edge1.getSourceNode(), edge1.getType(), edge3.getVersion(), null );

        Observable<Edge> edges = gm.loadEdgesFromSource( search );

        //implicitly blows up if more than 1 is returned from "single"
        Iterator<Edge> returned = edges.toBlockingObservable().getIterator();


        //we have 3 edges, but we specified our first edge as the max, we shouldn't get any more results than the first
        assertEquals( "Correct edge returned", edge3, returned.next() );

        assertEquals( "Correct edge returned", edge2, returned.next() );

        assertEquals( "Correct edge returned", edge1, returned.next() );

        assertFalse( "No more edges", returned.hasNext() );

        //still edge 3 is our max version, but we start with edge 2 as our last read
        search = createSearchByEdge( edge1.getSourceNode(), edge1.getType(), edge3.getVersion(), edge2 );

        edges = gm.loadEdgesFromSource( search );

        //implicitly blows up if more than 1 is returned from "single"
        returned = edges.toBlockingObservable().getIterator();

        assertEquals( "Paged correctly", edge1, returned.next() );

        assertFalse( "End of stream", returned.hasNext() );
    }


    @Test
    public void testWriteReadEdgeTypePagingTarget() {


        GraphManager gm = getHelper(emf.createEdgeManager( scope ));


        final Id targetId = createId( "target" );

        Edge edge1 = createEdge( createId( "source" ), "test", targetId );

        gm.writeEdge( edge1 ).toBlockingObservable().last();

        Edge edge2 = createEdge( createId( "source" ), "test", targetId );

        gm.writeEdge( edge2 ).toBlockingObservable().last();

        Edge edge3 = createEdge( createId( "source" ), "test", targetId );

        gm.writeEdge( edge3 ).toBlockingObservable().last();


        //now test retrieving it

        SearchByEdgeType search =
                createSearchByEdge( edge1.getTargetNode(), edge1.getType(), edge3.getVersion(), null );

        Observable<Edge> edges = gm.loadEdgesToTarget( search );

        //implicitly blows up if more than 1 is returned from "single"
        Iterator<Edge> returned = edges.toBlockingObservable().getIterator();


        //we have 3 edges, but we specified our first edge as the max, we shouldn't get any more results than the first
        assertEquals( "Correct edge returned", edge3, returned.next() );

        assertEquals( "Correct edge returned", edge2, returned.next() );

        assertEquals( "Correct edge returned", edge1, returned.next() );


        assertFalse( "No more edges", returned.hasNext() );

        search = createSearchByEdge( edge1.getTargetNode(), edge1.getType(), edge3.getVersion(), edge2 );

        edges = gm.loadEdgesToTarget( search );

        //implicitly blows up if more than 1 is returned from "single"
        returned = edges.toBlockingObservable().getIterator();

        assertEquals( "Paged correctly", edge1, returned.next() );

        assertFalse( "End of stream", returned.hasNext() );
    }


    @Test
    public void testWriteReadEdgeTypeTargetTypeSource() {

        GraphManager gm = getHelper(emf.createEdgeManager( scope ));


        Edge edge = createEdge( "source", "test", "target" );

        gm.writeEdge( edge ).toBlockingObservable().last();

        //now test retrieving it

        SearchByIdType search = createSearchByEdgeAndId( edge.getSourceNode(), edge.getType(), edge.getVersion(),
                edge.getTargetNode().getType(), null );

        Observable<Edge> edges = gm.loadEdgesFromSourceByType( search );

        //implicitly blows up if more than 1 is returned from "single"
        Edge returned = edges.toBlockingObservable().single();

        assertEquals( "Correct edge returned", edge, returned );


        //change edge type to be invalid, shouldn't get a result
        search = createSearchByEdgeAndId( edge.getSourceNode(), edge.getType(), edge.getVersion(),
                edge.getTargetNode().getType() + "invalid", null );

        edges = gm.loadEdgesFromSourceByType( search );

        //implicitly blows up if more than 1 is returned from "single"
        returned = edges.toBlockingObservable().singleOrDefault( null );

        assertNull( "Invalid type should not be returned", returned );
    }


    @Test
    public void testWriteReadEdgeTypeTargetTypeTarget() {

        GraphManager gm = getHelper(emf.createEdgeManager( scope ));;


        Edge edge = createEdge( "source", "test", "target" );

        gm.writeEdge( edge ).toBlockingObservable().last();

        //now test retrieving it

        SearchByIdType search = createSearchByEdgeAndId( edge.getTargetNode(), edge.getType(), edge.getVersion(),
                edge.getSourceNode().getType(), null );

        Observable<Edge> edges = gm.loadEdgesToTargetByType( search );

        //implicitly blows up if more than 1 is returned from "single"
        Edge returned = edges.toBlockingObservable().single();

        assertEquals( "Correct edge returned", edge, returned );


        //change edge type to be invalid, shouldn't get a result
        search = createSearchByEdgeAndId( edge.getTargetNode(), edge.getType(), edge.getVersion(),
                edge.getSourceNode().getType() + "invalid", null );

        edges = gm.loadEdgesToTargetByType( search );

        //implicitly blows up if more than 1 is returned from "single"
        returned = edges.toBlockingObservable().singleOrDefault( null );

        assertNull( "Invalid type should not be returned", returned );
    }


    @Test
    public void testWriteReadEdgeDeleteSource() {

        GraphManager gm = getHelper(emf.createEdgeManager( scope ));


        Edge edge = createEdge( "source", "test", "target" );

        gm.writeEdge( edge ).toBlockingObservable().last();

        //now test retrieving it


        SearchByEdgeType search = createSearchByEdge( edge.getSourceNode(), edge.getType(), edge.getVersion(), null );

        Observable<Edge> edges = gm.loadEdgesFromSource( search );

        //implicitly blows up if more than 1 is returned from "single"
        Edge returned = edges.toBlockingObservable().single();

        assertEquals( "Correct edge returned", edge, returned );

        SearchByIdType searchById = createSearchByEdgeAndId( edge.getSourceNode(), edge.getType(), edge.getVersion(),
                edge.getTargetNode().getType(), null );

        edges = gm.loadEdgesFromSourceByType( searchById );

        //implicitly blows up if more than 1 is returned from "single"
        returned = edges.toBlockingObservable().single();

        assertEquals( "Correct edge returned", edge, returned );

        final SearchByEdge searchByEdge = createGetByEdge(edge.getSourceNode(), edge.getType(), edge.getTargetNode(), edge.getVersion(), null);

        returned = gm.loadEdgeVersions(searchByEdge).toBlockingObservable().single();


        assertEquals( "Correct edge returned", edge, returned );


        //now delete it
        returned = gm.deleteEdge( edge ).toBlockingObservable().last();


        assertEquals( "Correct edge returned", edge, returned );


        //now test retrieval, should be null
        edges = gm.loadEdgesFromSource( search );

        //implicitly blows up if more than 1 is returned from "single"
        returned = edges.toBlockingObservable().singleOrDefault( null );

        assertNull( "No edge returned", returned );



        //no search by type, should be null as well

        edges = gm.loadEdgesFromSourceByType( searchById );

        //implicitly blows up if more than 1 is returned from "single"
        returned = edges.toBlockingObservable().singleOrDefault( null );

        assertNull( "No edge returned", returned );

        returned = gm.loadEdgeVersions(searchByEdge).toBlockingObservable().singleOrDefault(null);

        assertNull( "No edge returned", returned );
    }


    @Test
    public void testWriteReadEdgeDeleteTarget() {

        GraphManager gm = getHelper(emf.createEdgeManager( scope ));


        Edge edge = createEdge( "source", "test", "target" );

        gm.writeEdge( edge ).toBlockingObservable().last();

        //now test retrieving it


        SearchByEdgeType search = createSearchByEdge( edge.getTargetNode(), edge.getType(), edge.getVersion(), null );

        Observable<Edge> edges = gm.loadEdgesToTarget( search );

        //implicitly blows up if more than 1 is returned from "single"
        Edge returned = edges.toBlockingObservable().single();

        assertEquals( "Correct edge returned", edge, returned );

        SearchByIdType searchById = createSearchByEdgeAndId( edge.getTargetNode(), edge.getType(), edge.getVersion(),
                edge.getSourceNode().getType(), null );

        edges = gm.loadEdgesToTargetByType( searchById );

        //implicitly blows up if more than 1 is returned from "single"
        returned = edges.toBlockingObservable().single();

        assertEquals( "Correct edge returned", edge, returned );


        //now delete it
        gm.deleteEdge( edge ).toBlockingObservable().last();

        //now test retrieval, should be null
        edges = gm.loadEdgesToTarget( search );

        //implicitly blows up if more than 1 is returned from "single"
        returned = edges.toBlockingObservable().singleOrDefault( null );

        assertNull( "No edge returned", returned );


        //no search by type, should be null as well

        edges = gm.loadEdgesToTargetByType( searchById );

        //implicitly blows up if more than 1 is returned from "single"
        returned = edges.toBlockingObservable().singleOrDefault( null );

        assertNull( "No edge returned", returned );
    }


    @Test
    public void testWriteReadEdgeTypesSourceTypes() {

        final GraphManager gm = getHelper(emf.createEdgeManager( scope ));

        Id sourceId = new SimpleId( "source" );
        Id targetId1 = new SimpleId( "target" );
        Id targetId2 = new SimpleId( "target2" );

        Edge testTargetEdge = createEdge( sourceId, "test", targetId1, UUIDGenerator.newTimeUUID() );

        gm.writeEdge( testTargetEdge ).toBlockingObservable().singleOrDefault( null );

        Edge testTarget2Edge = createEdge( sourceId, "test", targetId2, UUIDGenerator.newTimeUUID() );

        gm.writeEdge( testTarget2Edge ).toBlockingObservable().singleOrDefault( null );


        Edge test2TargetEdge = createEdge( sourceId, "test2", targetId1, UUIDGenerator.newTimeUUID() );

        gm.writeEdge( test2TargetEdge ).toBlockingObservable().singleOrDefault( null );


        //get our 2 edge types
        Observable<String> edges =
                gm.getEdgeTypesFromSource( new SimpleSearchEdgeType( testTargetEdge.getSourceNode(), null ) );


        Iterator<String> results = edges.toBlockingObservable().getIterator();


        assertEquals( "Edges correct", "test", results.next() );

        assertEquals( "Edges correct", "test2", results.next() );

        assertFalse( "No more edges", results.hasNext() );


        //now test sub edges

        edges = gm.getIdTypesFromSource( new SimpleSearchIdType( testTargetEdge.getSourceNode(), "test", null ) );

        results = edges.toBlockingObservable().getIterator();


        assertEquals( "Types correct", targetId1.getType(), results.next() );

        assertEquals( "Types correct", targetId2.getType(), results.next() );

        assertFalse( "No results", results.hasNext() );

        //now get types for test2
        edges = gm.getIdTypesFromSource( new SimpleSearchIdType( testTargetEdge.getSourceNode(), "test2", null ) );

        results = edges.toBlockingObservable().getIterator();

        assertEquals( "Types correct", targetId1.getType(), results.next() );

        assertFalse( "No results", results.hasNext() );
    }


    @Test
    public void testWriteReadEdgeTypesTargetTypes() {

        final GraphManager gm = getHelper(emf.createEdgeManager( scope ));

        Id sourceId1 = new SimpleId( "source" );
        Id sourceId2 = new SimpleId( "source2" );
        Id targetId1 = new SimpleId( "target" );


        Edge testTargetEdge = createEdge( sourceId1, "test", targetId1, UUIDGenerator.newTimeUUID() );

        gm.writeEdge( testTargetEdge ).toBlockingObservable().singleOrDefault( null );

        Edge testTarget2Edge = createEdge( sourceId2, "test", targetId1, UUIDGenerator.newTimeUUID() );

        gm.writeEdge( testTarget2Edge ).toBlockingObservable().singleOrDefault( null );


        Edge test2TargetEdge = createEdge( sourceId1, "test2", targetId1, UUIDGenerator.newTimeUUID() );

        gm.writeEdge( test2TargetEdge ).toBlockingObservable().singleOrDefault( null );


        //get our 2 edge types
        final SearchEdgeType edgeTypes = new SimpleSearchEdgeType( testTargetEdge.getTargetNode(), null );

        Observable<String> edges = gm.getEdgeTypesToTarget( edgeTypes );


        Iterator<String> results = edges.toBlockingObservable().getIterator();


        assertEquals( "Edges correct", "test", results.next() );

        assertEquals( "Edges correct", "test2", results.next() );

        assertFalse( "No more edges", results.hasNext() );


        //now test sub edges

        edges = gm.getIdTypesToTarget( new SimpleSearchIdType( testTargetEdge.getTargetNode(), "test", null ) );

        results = edges.toBlockingObservable().getIterator();

        assertEquals( "Types correct", sourceId1.getType(), results.next() );

        assertEquals( "Types correct", sourceId2.getType(), results.next() );

        assertFalse( "No more edges", results.hasNext() );


        //now get types for test2
        edges = gm.getIdTypesToTarget( new SimpleSearchIdType( testTargetEdge.getTargetNode(), "test2", null ) );

        results = edges.toBlockingObservable().getIterator();


        assertEquals( "Types correct", sourceId1.getType(), results.next() );

        assertFalse( "No more edges", results.hasNext() );
    }


    @Test
    public void testWriteReadEdgeTypesSourceTypesPaging() {

        final GraphManager gm = getHelper(emf.createEdgeManager( scope ));

        Id sourceId1 = new SimpleId( "source" );
        Id targetId1 = new SimpleId( "target" );
        Id targetId2 = new SimpleId( "target2" );


        Edge testTargetEdge = createEdge( sourceId1, "test", targetId1, UUIDGenerator.newTimeUUID() );

        gm.writeEdge( testTargetEdge ).toBlockingObservable().singleOrDefault( null );


        Edge testTargetEdge2 = createEdge( sourceId1, "test", targetId2, UUIDGenerator.newTimeUUID() );

        gm.writeEdge( testTargetEdge2 ).toBlockingObservable().singleOrDefault( null );


        Edge test2TargetEdge = createEdge( sourceId1, "test2", targetId2, UUIDGenerator.newTimeUUID() );

        gm.writeEdge( test2TargetEdge ).toBlockingObservable().singleOrDefault( null );


        //get our 2 edge types
        SearchEdgeType edgeTypes = new SimpleSearchEdgeType( testTargetEdge.getSourceNode(), null );

        Observable<String> edges = gm.getEdgeTypesFromSource( edgeTypes );


        Iterator<String> results = edges.toBlockingObservable().getIterator();


        assertEquals( "Edges correct", "test", results.next() );
        assertEquals( "Edges correct", "test2", results.next() );
        assertFalse( "No more edges", results.hasNext() );

        //now load the next page

        edgeTypes = new SimpleSearchEdgeType( testTargetEdge.getSourceNode(), "test" );

        edges = gm.getEdgeTypesFromSource( edgeTypes );


        results = edges.toBlockingObservable().getIterator();


        assertEquals( "Edges correct", "test2", results.next() );
        assertFalse( "No more edges", results.hasNext() );


        //now test sub edges

        edges = gm.getIdTypesFromSource( new SimpleSearchIdType( testTargetEdge.getSourceNode(), "test", null ) );

        results = edges.toBlockingObservable().getIterator();


        assertEquals( "Types correct", targetId1.getType(), results.next() );
        assertEquals( "Types correct", targetId2.getType(), results.next() );
        assertFalse( "No more edges", results.hasNext() );


        //now get the next page

        edges = gm.getIdTypesFromSource(
                new SimpleSearchIdType( testTargetEdge.getSourceNode(), "test", targetId1.getType() ) );

        results = edges.toBlockingObservable().getIterator();


        assertEquals( "Types correct", targetId2.getType(), results.next() );

        assertFalse( "No more results", results.hasNext() );
    }


    @Test
    public void testWriteReadEdgeTypesTargetTypesPaging() {

        final GraphManager gm = getHelper(emf.createEdgeManager( scope ));

        Id sourceId1 = new SimpleId( "source" );
        Id sourceId2 = new SimpleId( "source2" );
        Id targetId = new SimpleId( "target" );


        Edge testTargetEdge = createEdge( sourceId1, "test", targetId, UUIDGenerator.newTimeUUID() );

        gm.writeEdge( testTargetEdge ).toBlockingObservable().singleOrDefault( null );


        Edge testTargetEdge2 = createEdge( sourceId2, "test", targetId, UUIDGenerator.newTimeUUID() );

        gm.writeEdge( testTargetEdge2 ).toBlockingObservable().singleOrDefault( null );

        Edge test2TargetEdge = createEdge( sourceId2, "test2", targetId, UUIDGenerator.newTimeUUID() );

        gm.writeEdge( test2TargetEdge ).toBlockingObservable().singleOrDefault( null );


        //get our 2 edge types
        SearchEdgeType edgeTypes = new SimpleSearchEdgeType( testTargetEdge.getTargetNode(), null );

        Observable<String> edges = gm.getEdgeTypesToTarget( edgeTypes );


        Iterator<String> results = edges.toBlockingObservable().getIterator();


        assertEquals( "Edges correct", "test", results.next() );
        assertEquals( "Edges correct", "test2", results.next() );

        assertFalse( "No more edges", results.hasNext() );


        //now load the next page

        edgeTypes = new SimpleSearchEdgeType( testTargetEdge2.getTargetNode(), "test" );

        edges = gm.getEdgeTypesToTarget( edgeTypes );


        results = edges.toBlockingObservable().getIterator();


        assertEquals( "Edges correct", "test2", results.next() );


        assertFalse( "No more edges", results.hasNext() );

        //now test sub edges

        edges = gm.getIdTypesToTarget( new SimpleSearchIdType( testTargetEdge.getTargetNode(), "test", null ) );

        results = edges.toBlockingObservable().getIterator();


        assertEquals( "Types correct", sourceId1.getType(), results.next() );

        assertEquals( "Types correct", sourceId2.getType(), results.next() );

        assertFalse( "No more edges", results.hasNext() );

        //now get the next page

        edges = gm.getIdTypesToTarget(
                new SimpleSearchIdType( testTargetEdge.getTargetNode(), "test", sourceId1.getType() ) );

        results = edges.toBlockingObservable().getIterator();


        assertEquals( "Types correct", sourceId2.getType(), results.next() );

        assertFalse( "No more results", results.hasNext() );
    }


    @Test
    public void testMarkSourceEdges() {

        final GraphManager gm = getHelper(emf.createEdgeManager( scope ));

        Id sourceId = new SimpleId( "source" );
        Id targetId1 = new SimpleId( "target" );
        Id targetId2 = new SimpleId( "target2" );

        Edge edge1 = createEdge( sourceId, "test", targetId1, UUIDGenerator.newTimeUUID() );

        gm.writeEdge( edge1 ).toBlockingObservable().singleOrDefault( null );

        Edge edge2 = createEdge( sourceId, "test", targetId2, UUIDGenerator.newTimeUUID() );

        gm.writeEdge( edge2 ).toBlockingObservable().singleOrDefault( null );


        final UUID maxVersion = UUIDGenerator.newTimeUUID();


        //get our 2 edges
        Observable<Edge> edges = gm.loadEdgesFromSource(
                createSearchByEdge( edge1.getSourceNode(), edge1.getType(), maxVersion, null ) );


        Iterator<Edge> results = edges.toBlockingObservable().getIterator();


        assertEquals( "Edges correct", edge2, results.next() );

        assertEquals( "Edges correct", edge1, results.next() );

        assertFalse( "No more edges", results.hasNext() );

        //now delete one of the edges

        gm.deleteEdge( edge1 ).toBlockingObservable().last();


        edges = gm.loadEdgesFromSource(
                createSearchByEdge( edge1.getSourceNode(), edge1.getType(), maxVersion, null ) );


        results = edges.toBlockingObservable().getIterator();


        assertEquals( "Edges correct", edge2, results.next() );

        assertFalse( "No more edges", results.hasNext() );

        //now delete one of the edges

        gm.deleteEdge( edge2 ).toBlockingObservable().last();

        edges = gm.loadEdgesFromSource(
                createSearchByEdge( edge1.getSourceNode(), edge1.getType(), maxVersion, null ) );


        results = edges.toBlockingObservable().getIterator();


        assertFalse( "No more edges", results.hasNext() );

        //now delete one of the edges

    }


    @Test
    public void testMarkTargetEdges() {

        final GraphManager gm = getHelper(emf.createEdgeManager( scope ));

        Id sourceId1 = new SimpleId( "source" );
        Id sourceId2 = new SimpleId( "source2" );
        Id targetId = new SimpleId( "target" );

        Edge edge1 = createEdge( sourceId1, "test", targetId, UUIDGenerator.newTimeUUID() );

        gm.writeEdge( edge1 ).toBlockingObservable().last();

        Edge edge2 = createEdge( sourceId2, "test", targetId, UUIDGenerator.newTimeUUID() );

        gm.writeEdge( edge2 ).toBlockingObservable().last();


        final UUID maxVersion = UUIDGenerator.newTimeUUID();


        //get our 2 edges
        Observable<Edge> edges =
                gm.loadEdgesToTarget( createSearchByEdge( edge1.getTargetNode(), edge1.getType(), maxVersion, null ) );


        Iterator<Edge> results = edges.toBlockingObservable().getIterator();


        assertEquals( "Edges correct", edge2, results.next() );

        assertEquals( "Edges correct", edge1, results.next() );


        assertFalse( "No more edges", results.hasNext() );

        //now delete one of the edges

        gm.deleteEdge( edge1 ).toBlockingObservable().last();


        edges = gm.loadEdgesToTarget( createSearchByEdge( edge1.getTargetNode(), edge1.getType(), maxVersion, null ) );


        results = edges.toBlockingObservable().getIterator();


        assertEquals( "Edges correct", edge2, results.next() );

        assertFalse( "No more edges", results.hasNext() );

        //now delete one of the edges

        gm.deleteEdge( edge2 ).toBlockingObservable().last();

        edges = gm.loadEdgesToTarget( createSearchByEdge( edge1.getTargetNode(), edge1.getType(), maxVersion, null ) );


        results = edges.toBlockingObservable().getIterator();


        assertFalse( "No more edges", results.hasNext() );

        //now delete one of the edges

    }


    @Test
    public void testMarkSourceEdgesType() {

        final GraphManager gm = getHelper(emf.createEdgeManager( scope ));

        Id sourceId = new SimpleId( "source" );
        Id targetId1 = new SimpleId( "target" );
        Id targetId2 = new SimpleId( "target2" );

        Edge edge1 = createEdge( sourceId, "test", targetId1, UUIDGenerator.newTimeUUID() );

        gm.writeEdge( edge1 ).toBlockingObservable().singleOrDefault( null );

        Edge edge2 = createEdge( sourceId, "test", targetId2, UUIDGenerator.newTimeUUID() );

        gm.writeEdge( edge2 ).toBlockingObservable().singleOrDefault( null );


        final UUID maxVersion = UUIDGenerator.newTimeUUID();


        //get our 2 edges
        Observable<Edge> edges = gm.loadEdgesFromSourceByType(
                createSearchByEdgeAndId( sourceId, edge1.getType(), maxVersion, targetId1.getType(), null ) );


        Iterator<Edge> results = edges.toBlockingObservable().getIterator();


        assertEquals( "Edges correct", edge1, results.next() );

        assertFalse( "No more edges", results.hasNext() );

        //now delete one of the edges

        gm.deleteEdge( edge1 ).toBlockingObservable().last();


        edges = gm.loadEdgesFromSourceByType(
                createSearchByEdgeAndId( sourceId, edge1.getType(), maxVersion, targetId1.getType(), null ) );

        results = edges.toBlockingObservable().getIterator();


        assertFalse( "No more edges", results.hasNext() );


        edges = gm.loadEdgesFromSourceByType(
                createSearchByEdgeAndId( sourceId, edge1.getType(), maxVersion, targetId2.getType(), null ) );


        results = edges.toBlockingObservable().getIterator();


        assertEquals( "Edges correct", edge2, results.next() );

        assertFalse( "No more edges", results.hasNext() );

        //now delete one of the edges

        gm.deleteEdge( edge2 ).toBlockingObservable().last();


        edges = gm.loadEdgesFromSourceByType(
                createSearchByEdgeAndId( sourceId, edge1.getType(), maxVersion, targetId2.getType(), null ) );


        results = edges.toBlockingObservable().getIterator();


        assertFalse( "No more edges", results.hasNext() );


        //now delete one of the edges

    }


    @Test
    public void testMarkTargetEdgesType() {

        final GraphManager gm = getHelper(emf.createEdgeManager( scope ));

        Id sourceId1 = new SimpleId( "source" );
        Id sourceId2 = new SimpleId( "source2" );
        Id targetId = new SimpleId( "target" );

        Edge edge1 = createEdge( sourceId1, "test", targetId, UUIDGenerator.newTimeUUID() );

        gm.writeEdge( edge1 ).toBlockingObservable().last();

        Edge edge2 = createEdge( sourceId2, "test", targetId, UUIDGenerator.newTimeUUID() );

        gm.writeEdge( edge2 ).toBlockingObservable().last();


        final UUID maxVersion = UUIDGenerator.newTimeUUID();

        //get our 2 edges
        Observable<Edge> edges = gm.loadEdgesToTargetByType(
                createSearchByEdgeAndId( targetId, edge1.getType(), maxVersion, sourceId1.getType(), null ) );


        Iterator<Edge> results = edges.toBlockingObservable().getIterator();


        assertEquals( "Edges correct", edge1, results.next() );

        assertFalse( "No more edges", results.hasNext() );

        //now delete one of the edges

        gm.deleteEdge( edge1 ).toBlockingObservable().last();


        edges = gm.loadEdgesToTargetByType(
                createSearchByEdgeAndId( edge1.getSourceNode(), edge1.getType(), maxVersion, sourceId1.getType(),
                        null ) );

        results = edges.toBlockingObservable().getIterator();


        assertFalse( "No more edges", results.hasNext() );


        edges = gm.loadEdgesToTargetByType(
                createSearchByEdgeAndId( targetId, edge1.getType(), maxVersion, sourceId2.getType(), null ) );


        results = edges.toBlockingObservable().getIterator();


        assertEquals( "Edges correct", edge2, results.next() );

        assertFalse( "No more edges", results.hasNext() );

        //now delete one of the edges

        gm.deleteEdge( edge2 ).toBlockingObservable().last();


        edges = gm.loadEdgesToTargetByType(
                createSearchByEdgeAndId( targetId, edge1.getType(), maxVersion, sourceId2.getType(), null ) );


        results = edges.toBlockingObservable().getIterator();


        assertFalse( "No more edges", results.hasNext() );


        //now delete one of the edges

    }


    @Test
    public void markSourceNode() {

        final GraphManager gm = getHelper(emf.createEdgeManager( scope ));

        Id sourceId = new SimpleId( "source" );
        Id targetId1 = new SimpleId( "target" );
        Id targetId2 = new SimpleId( "target2" );

        Edge edge1 = createEdge( sourceId, "test", targetId1, UUIDGenerator.newTimeUUID() );

        gm.writeEdge( edge1 ).toBlockingObservable().singleOrDefault( null );

        Edge edge2 = createEdge( sourceId, "test", targetId2, UUIDGenerator.newTimeUUID() );

        gm.writeEdge( edge2 ).toBlockingObservable().singleOrDefault( null );


        final UUID maxVersion = UUIDGenerator.newTimeUUID();

        Iterator<Edge> results =
                gm.loadEdgesFromSource( createSearchByEdge( sourceId, edge1.getType(), maxVersion, null ) )
                  .toBlockingObservable().getIterator();


        assertEquals( "Edge found", edge2, results.next() );

        assertEquals( "Edge found", edge1, results.next() );

        assertFalse( "No more edges", results.hasNext() );


        //get our 2 edges
        results = gm.loadEdgesFromSourceByType(
                createSearchByEdgeAndId( sourceId, edge1.getType(), maxVersion, targetId1.getType(), null ) )
                    .toBlockingObservable().getIterator();


        assertEquals( "Edges correct", edge1, results.next() );

        assertFalse( "No more edges", results.hasNext() );

        //now delete one of the edges
        results = gm.loadEdgesFromSourceByType(
                createSearchByEdgeAndId( sourceId, edge2.getType(), maxVersion, targetId2.getType(), null ) )
                    .toBlockingObservable().getIterator();


        assertEquals( "Edges correct", edge2, results.next() );

        assertFalse( "No more edges", results.hasNext() );

        //mark the source node
        gm.deleteNode( sourceId ).toBlockingObservable().last();


        //now re-read, nothing should be there since they're marked

        results = gm.loadEdgesFromSource( createSearchByEdge( sourceId, edge1.getType(), maxVersion, null ) )
                    .toBlockingObservable().getIterator();

        assertFalse( "No more edges", results.hasNext() );


        //get our 2 edges
        results = gm.loadEdgesFromSourceByType(
                createSearchByEdgeAndId( sourceId, edge1.getType(), maxVersion, targetId1.getType(), null ) )
                    .toBlockingObservable().getIterator();


        assertFalse( "No more edges", results.hasNext() );

        //now delete one of the edges
        results = gm.loadEdgesFromSourceByType(
                createSearchByEdgeAndId( sourceId, edge2.getType(), maxVersion, targetId2.getType(), null ) )
                    .toBlockingObservable().getIterator();


        assertFalse( "No more edges", results.hasNext() );
    }


    @Test
    public void markTargetNode() {

        final GraphManager gm = getHelper(emf.createEdgeManager( scope ));

        Id sourceId1 = new SimpleId( "source" );
        Id sourceId2 = new SimpleId( "source2" );
        Id targetId = new SimpleId( "target" );

        Edge edge1 = createEdge( sourceId1, "test", targetId, UUIDGenerator.newTimeUUID() );

        gm.writeEdge( edge1 ).toBlockingObservable().singleOrDefault( null );

        Edge edge2 = createEdge( sourceId2, "test", targetId, UUIDGenerator.newTimeUUID() );

        gm.writeEdge( edge2 ).toBlockingObservable().singleOrDefault( null );


        final UUID maxVersion = UUIDGenerator.newTimeUUID();

        Iterator<Edge> results =
                gm.loadEdgesToTarget( createSearchByEdge( targetId, edge1.getType(), maxVersion, null ) )
                  .toBlockingObservable().getIterator();


        assertEquals( "Edge found", edge2, results.next() );

        assertEquals( "Edge found", edge1, results.next() );

        assertFalse( "No more edges", results.hasNext() );


        //get our 2 edges
        results = gm.loadEdgesToTargetByType(
                createSearchByEdgeAndId( targetId, edge1.getType(), maxVersion, sourceId1.getType(), null ) )
                    .toBlockingObservable().getIterator();


        assertEquals( "Edges correct", edge1, results.next() );

        assertFalse( "No more edges", results.hasNext() );

        //now delete one of the edges
        results = gm.loadEdgesToTargetByType(
                createSearchByEdgeAndId( targetId, edge2.getType(), maxVersion, sourceId2.getType(), null ) )
                    .toBlockingObservable().getIterator();


        assertEquals( "Edges correct", edge2, results.next() );

        assertFalse( "No more edges", results.hasNext() );

        //mark the source node
        gm.deleteNode( targetId ).toBlockingObservable().last();


        //now re-read, nothing should be there since they're marked

        results = gm.loadEdgesToTarget( createSearchByEdge( targetId, edge1.getType(), maxVersion, null ) )
                    .toBlockingObservable().getIterator();

        assertFalse( "No more edges", results.hasNext() );


        //get our 2 edges
        results = gm.loadEdgesToTargetByType(
                createSearchByEdgeAndId( targetId, edge1.getType(), maxVersion, sourceId1.getType(), null ) )
                    .toBlockingObservable().getIterator();


        assertFalse( "No more edges", results.hasNext() );

        //now delete one of the edges
        results = gm.loadEdgesToTargetByType(
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





