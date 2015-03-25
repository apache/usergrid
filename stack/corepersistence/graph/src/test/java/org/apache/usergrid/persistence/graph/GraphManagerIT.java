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
import java.util.concurrent.TimeoutException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.apache.usergrid.persistence.core.guice.MigrationManagerRule;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchEdgeType;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchIdType;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;

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
import static org.junit.Assert.assertTrue;


public abstract class GraphManagerIT {


    @Inject
    @Rule
    public MigrationManagerRule migrationManagerRule;


    @Inject
    protected GraphManagerFactory emf;

    protected ApplicationScope scope;


    /**
     * Get the helper for performing our tests
     *
     * @return The helper to use when writing/synchronizing tests
     */
    protected abstract GraphManager getHelper( GraphManager gm );


    @Before
    public void mockApp() {
        this.scope = new ApplicationScopeImpl(createId("application")  );
    }


    @Test
    public void testWriteReadEdgeTypeSource() throws TimeoutException, InterruptedException {

        GraphManager gm = getHelper( emf.createEdgeManager( scope ) );

        Edge edge = createEdge( "source", "test", "target" );

        gm.writeEdge( edge ).toBlocking().last();

        //now test retrieving it

        SearchByEdgeType search = createSearchByEdge( edge.getSourceNode(), edge.getType(), edge.getTimestamp(), null );

        Observable<Edge> edges = gm.loadEdgesFromSource( search );

        //implicitly blows up if more than 1 is returned from "single"
        Edge returned = edges.toBlocking().last();

        assertEquals( "Correct edge returned", edge, returned );

        //change edge type to be invalid, shouldn't get a result
        search = createSearchByEdge( edge.getSourceNode(), edge.getType() + "invalid", edge.getTimestamp(), null );

        edges = gm.loadEdgesFromSource( search );

        //implicitly blows up if more than 1 is returned from "single"
        returned = edges.toBlocking().singleOrDefault( null );

        assertNull( "Invalid type should not be returned", returned );
    }


    @Test
    public void testWriteReadEdgeTypeTarget() throws TimeoutException, InterruptedException {

        GraphManager gm = getHelper( emf.createEdgeManager( scope ) );

        Edge edge = createEdge( "source", "test", "target" );

        gm.writeEdge( edge ).toBlocking().last();

        //now test retrieving it

        SearchByEdgeType search = createSearchByEdge( edge.getTargetNode(), edge.getType(), edge.getTimestamp(), null );

        Observable<Edge> edges = gm.loadEdgesToTarget( search );

        //implicitly blows up if more than 1 is returned from "single"
        Edge returned = edges.toBlocking().single();

        assertEquals( "Correct edge returned", edge, returned );

        //change edge type to be invalid, shouldn't get a result
        search = createSearchByEdge( edge.getTargetNode(), edge.getType() + "invalid", edge.getTimestamp(), null );

        edges = gm.loadEdgesToTarget( search );

        //implicitly blows up if more than 1 is returned from "single"
        returned = edges.toBlocking().singleOrDefault( null );

        assertNull( "Invalid type should not be returned", returned );
    }


    @Test
    public void testWriteReadEdgeTypeVersionSource() throws TimeoutException, InterruptedException {

        GraphManager gm = getHelper( emf.createEdgeManager( scope ) );

        final long earlyVersion = 1000l;

        Edge edge = createEdge( "source", "test", "target", earlyVersion );

        gm.writeEdge( edge ).toBlocking().last();

        //now test retrieving it

        SearchByEdgeType search = createSearchByEdge( edge.getSourceNode(), edge.getType(), edge.getTimestamp(), null );

        Observable<Edge> edges = gm.loadEdgesFromSource( search );

        //implicitly blows up if more than 1 is returned from "single"
        Edge returned = edges.toBlocking().single();

        assertEquals( "Correct edge returned", edge, returned );

        //now test with an earlier version, we shouldn't get the edge back
        search = createSearchByEdge( edge.getSourceNode(), edge.getType(), earlyVersion - 1, null );

        edges = gm.loadEdgesFromSource( search );

        //implicitly blows up if more than 1 is returned from "single"
        returned = edges.toBlocking().singleOrDefault( null );

        assertNull( "Earlier version should not be returned", returned );
    }


    @Test
    public void testWriteReadEdgeTypeVersionTarget() throws TimeoutException, InterruptedException {

        GraphManager gm = getHelper( emf.createEdgeManager( scope ) );

        final long earlyVersion = 10000l;


        Edge edge = createEdge( "source", "test", "target", earlyVersion );

        gm.writeEdge( edge ).toBlocking().last();

        //now test retrieving it

        SearchByEdgeType search = createSearchByEdge( edge.getTargetNode(), edge.getType(), edge.getTimestamp(), null );

        Observable<Edge> edges = gm.loadEdgesToTarget( search );

        //implicitly blows up if more than 1 is returned from "single"
        Edge returned = edges.toBlocking().single();

        assertEquals( "Correct edge returned", edge, returned );

        //change edge type to be invalid, shouldn't get a result
        search = createSearchByEdge( edge.getTargetNode(), edge.getType(), earlyVersion - 1, null );

        edges = gm.loadEdgesToTarget( search );

        //implicitly blows up if more than 1 is returned from "single"
        returned = edges.toBlocking().singleOrDefault( null );

        assertNull( "Earlier version should not be returned", returned );
    }


    /**
     * Tests that if multiple versions of an edge exist, only the distinct edges with a version <= max are returned
     */
    @Test
    public void testWriteReadEdgeTypeVersionSourceDistinct() throws TimeoutException, InterruptedException {

        GraphManager gm = getHelper( emf.createEdgeManager( scope ) );

        final long earlyVersion = 10000l;


        Edge edge1 = createEdge( "source", "test", "target", earlyVersion + 1 );

        final Id sourceId = edge1.getSourceNode();
        final Id targetId = edge1.getTargetNode();


        gm.writeEdge( edge1 ).toBlocking().last();

        Edge edge2 = createEdge( sourceId, edge1.getType(), targetId, earlyVersion + 2 );

        gm.writeEdge( edge2 ).toBlocking().last();

        Edge edge3 = createEdge( sourceId, edge1.getType(), targetId, earlyVersion + 3 );

        gm.writeEdge( edge3 ).toBlocking().last();


        //now test retrieving it, we should only get edge3, since it's the latest

        SearchByEdgeType search =
                createSearchByEdge( edge1.getSourceNode(), edge1.getType(), edge3.getTimestamp(), null );

        Observable<Edge> edges = gm.loadEdgesFromSource( search );

        //implicitly blows up if more than 1 is returned from "single"
        Iterator<Edge> returned = edges.toBlocking().getIterator();

        assertEquals( "Correct edge returned", edge3, returned.next() );
        assertEquals( "Correct edge returned", edge2, returned.next() );
        assertEquals( "Correct edge returned", edge1, returned.next() );
        assertFalse( "No more edges", returned.hasNext() );

        //now test with an earlier version, we shouldn't get the edge back
        search = createSearchByEdge( edge1.getSourceNode(), edge1.getType(), edge2.getTimestamp(), null );

        edges = gm.loadEdgesFromSource( search );

        returned = edges.toBlocking().getIterator();

        assertEquals( "Correct edge returned", edge2, returned.next() );
        assertEquals( "Correct edge returned", edge1, returned.next() );
        assertFalse( "No more edges", returned.hasNext() );

        search = createSearchByEdge( edge1.getSourceNode(), edge1.getType(), edge1.getTimestamp(), null );

        edges = gm.loadEdgesFromSource( search );

        returned = edges.toBlocking().getIterator();

        assertEquals( "Correct edge returned", edge1, returned.next() );
        assertFalse( "No more edges", returned.hasNext() );


        search = createSearchByEdge( edge1.getSourceNode(), edge1.getType(), earlyVersion, null );

        edges = gm.loadEdgesFromSource( search );

        returned = edges.toBlocking().getIterator();

        assertFalse( "No more edges", returned.hasNext() );
    }


    @Test
    public void testWriteReadEdgeTypeVersionTargetDistinct() throws TimeoutException, InterruptedException {


        GraphManager gm = getHelper( emf.createEdgeManager( scope ) );


        final long earlyVersion = 10000l;


        Edge edge1 = createEdge( "source", "test", "target", earlyVersion + 1 );

        final Id sourceId = edge1.getSourceNode();
        final Id targetId = edge1.getTargetNode();


        gm.writeEdge( edge1 ).toBlocking().last();

        Edge edge2 = createEdge( sourceId, edge1.getType(), targetId, earlyVersion + 2 );

        gm.writeEdge( edge2 ).toBlocking().last();

        Edge edge3 = createEdge( sourceId, edge1.getType(), targetId, earlyVersion + 3 );

        gm.writeEdge( edge3 ).toBlocking().last();


        //now test retrieving it, we should only get edge3, since it's the latest

        SearchByEdgeType search =
                createSearchByEdge( edge1.getTargetNode(), edge1.getType(), edge3.getTimestamp(), null );

        Observable<Edge> edges = gm.loadEdgesToTarget( search );

        //implicitly blows up if more than 1 is returned from "single"
        Iterator<Edge> returned = edges.toBlocking().getIterator();

        assertEquals( "Correct edge returned", edge3, returned.next() );
        assertEquals( "Correct edge returned", edge2, returned.next() );
        assertEquals( "Correct edge returned", edge1, returned.next() );
        assertFalse( "No more edges", returned.hasNext() );

        //now test with an earlier version, we shouldn't get the edge back
        search = createSearchByEdge( edge1.getTargetNode(), edge1.getType(), edge2.getTimestamp(), null );

        edges = gm.loadEdgesToTarget( search );

        returned = edges.toBlocking().getIterator();

        assertEquals( "Correct edge returned", edge2, returned.next() );
        assertEquals( "Correct edge returned", edge1, returned.next() );
        assertFalse( "No more edges", returned.hasNext() );

        search = createSearchByEdge( edge1.getTargetNode(), edge1.getType(), edge1.getTimestamp(), null );

        edges = gm.loadEdgesToTarget( search );

        returned = edges.toBlocking().getIterator();

        assertEquals( "Correct edge returned", edge1, returned.next() );
        assertFalse( "No more edges", returned.hasNext() );


        search = createSearchByEdge( edge1.getTargetNode(), edge1.getType(), earlyVersion, null );

        edges = gm.loadEdgesToTarget( search );

        returned = edges.toBlocking().getIterator();

        assertFalse( "No more edges", returned.hasNext() );
    }


    @Test
    public void testWriteReadEdgeTypePagingSource() throws TimeoutException, InterruptedException {

        GraphManager gm = getHelper( emf.createEdgeManager( scope ) );
        final Id sourceId = createId( "source" );


        Edge edge1 = createEdge( sourceId, "test", createId( "target" ) );

        gm.writeEdge( edge1 ).toBlocking().last();

        Edge edge2 = createEdge( sourceId, "test", createId( "target" ) );

        gm.writeEdge( edge2 ).toBlocking().last();

        Edge edge3 = createEdge( sourceId, "test", createId( "target" ) );

        gm.writeEdge( edge3 ).toBlocking().last();


        //now test retrieving it

        SearchByEdgeType search =
                createSearchByEdge( edge1.getSourceNode(), edge1.getType(), edge3.getTimestamp(), null );

        Observable<Edge> edges = gm.loadEdgesFromSource( search );

        //implicitly blows up if more than 1 is returned from "single"
        Iterator<Edge> returned = edges.toBlocking().getIterator();


        //we have 3 edges, but we specified our first edge as the max, we shouldn't get any more results than the first
        assertEquals( "Correct edge returned", edge3, returned.next() );

        assertEquals( "Correct edge returned", edge2, returned.next() );

        assertEquals( "Correct edge returned", edge1, returned.next() );

        assertFalse( "No more edges", returned.hasNext() );

        //still edge 3 is our max version, but we start with edge 2 as our last read
        search = createSearchByEdge( edge1.getSourceNode(), edge1.getType(), edge3.getTimestamp(), edge2 );

        edges = gm.loadEdgesFromSource( search );

        //implicitly blows up if more than 1 is returned from "single"
        returned = edges.toBlocking().getIterator();

        assertEquals( "Paged correctly", edge1, returned.next() );

        assertFalse( "End of stream", returned.hasNext() );
    }


    @Test
    public void testWriteReadEdgeTypePagingTarget() {


        GraphManager gm = getHelper( emf.createEdgeManager( scope ) );


        final Id targetId = createId( "target" );

        Edge edge1 = createEdge( createId( "source" ), "test", targetId );

        gm.writeEdge( edge1 ).toBlocking().last();

        Edge edge2 = createEdge( createId( "source" ), "test", targetId );

        gm.writeEdge( edge2 ).toBlocking().last();

        Edge edge3 = createEdge( createId( "source" ), "test", targetId );

        gm.writeEdge( edge3 ).toBlocking().last();


        //now test retrieving it

        SearchByEdgeType search =
                createSearchByEdge( edge1.getTargetNode(), edge1.getType(), edge3.getTimestamp(), null );

        Observable<Edge> edges = gm.loadEdgesToTarget( search );

        //implicitly blows up if more than 1 is returned from "single"
        Iterator<Edge> returned = edges.toBlocking().getIterator();


        //we have 3 edges, but we specified our first edge as the max, we shouldn't get any more results than the first
        assertEquals( "Correct edge returned", edge3, returned.next() );

        assertEquals( "Correct edge returned", edge2, returned.next() );

        assertEquals( "Correct edge returned", edge1, returned.next() );


        assertFalse( "No more edges", returned.hasNext() );

        search = createSearchByEdge( edge1.getTargetNode(), edge1.getType(), edge3.getTimestamp(), edge2 );

        edges = gm.loadEdgesToTarget( search );

        //implicitly blows up if more than 1 is returned from "single"
        returned = edges.toBlocking().getIterator();

        assertEquals( "Paged correctly", edge1, returned.next() );

        assertFalse( "End of stream", returned.hasNext() );
    }


    @Test
    public void testWriteReadEdgeTypeTargetTypeSource() {

        GraphManager gm = getHelper( emf.createEdgeManager( scope ) );


        Edge edge = createEdge( "source", "test", "target" );

        gm.writeEdge( edge ).toBlocking().last();

        //now test retrieving it

        SearchByIdType search = createSearchByEdgeAndId( edge.getSourceNode(), edge.getType(), edge.getTimestamp(),
                edge.getTargetNode().getType(), null );

        Observable<Edge> edges = gm.loadEdgesFromSourceByType( search );

        //implicitly blows up if more than 1 is returned from "single"
        Edge returned = edges.toBlocking().single();

        assertEquals( "Correct edge returned", edge, returned );


        //change edge type to be invalid, shouldn't get a result
        search = createSearchByEdgeAndId( edge.getSourceNode(), edge.getType(), edge.getTimestamp(),
                edge.getTargetNode().getType() + "invalid", null );

        edges = gm.loadEdgesFromSourceByType( search );

        //implicitly blows up if more than 1 is returned from "single"
        returned = edges.toBlocking().singleOrDefault( null );

        assertNull( "Invalid type should not be returned", returned );
    }


    @Test
    public void testWriteReadEdgeTypeTargetTypeTarget() {

        GraphManager gm = getHelper( emf.createEdgeManager( scope ) );
        ;


        Edge edge = createEdge( "source", "test", "target" );

        gm.writeEdge( edge ).toBlocking().last();

        //now test retrieving it

        SearchByIdType search = createSearchByEdgeAndId( edge.getTargetNode(), edge.getType(), edge.getTimestamp(),
                edge.getSourceNode().getType(), null );

        Observable<Edge> edges = gm.loadEdgesToTargetByType( search );

        //implicitly blows up if more than 1 is returned from "single"
        Edge returned = edges.toBlocking().single();

        assertEquals( "Correct edge returned", edge, returned );


        //change edge type to be invalid, shouldn't get a result
        search = createSearchByEdgeAndId( edge.getTargetNode(), edge.getType(), edge.getTimestamp(),
                edge.getSourceNode().getType() + "invalid", null );

        edges = gm.loadEdgesToTargetByType( search );

        //implicitly blows up if more than 1 is returned from "single"
        returned = edges.toBlocking().singleOrDefault( null );

        assertNull( "Invalid type should not be returned", returned );
    }


    @Test
    public void testWriteReadEdgeDeleteSource() {

        GraphManager gm = getHelper( emf.createEdgeManager( scope ) );


        Edge edge = createEdge( "source", "test", "target" );

        gm.writeEdge( edge ).toBlocking().last();

        //now test retrieving it


        SearchByEdgeType search = createSearchByEdge( edge.getSourceNode(), edge.getType(), edge.getTimestamp(), null );

        Observable<Edge> edges = gm.loadEdgesFromSource( search );

        //implicitly blows up if more than 1 is returned from "single"
        Edge returned = edges.toBlocking().single();

        assertEquals( "Correct edge returned", edge, returned );

        SearchByIdType searchById = createSearchByEdgeAndId( edge.getSourceNode(), edge.getType(), edge.getTimestamp(),
                edge.getTargetNode().getType(), null );

        edges = gm.loadEdgesFromSourceByType( searchById );

        //implicitly blows up if more than 1 is returned from "single"
        returned = edges.toBlocking().single();

        assertEquals( "Correct edge returned", edge, returned );

        final SearchByEdge searchByEdge =
                createGetByEdge( edge.getSourceNode(), edge.getType(), edge.getTargetNode(), edge.getTimestamp(),
                        null );

        returned = gm.loadEdgeVersions( searchByEdge ).toBlocking().single();


        assertEquals( "Correct edge returned", edge, returned );


        //now delete it
        returned = gm.deleteEdge( edge ).toBlocking().last();


        //now test retrieval, should be null
        edges = gm.loadEdgesFromSource( search );

        //implicitly blows up if more than 1 is returned from "single"
        returned = edges.toBlocking().singleOrDefault( null );

        assertNull( "No edge returned", returned );


        //no search by type, should be null as well

        edges = gm.loadEdgesFromSourceByType( searchById );

        //implicitly blows up if more than 1 is returned from "single"
        returned = edges.toBlocking().singleOrDefault( null );

        assertNull( "No edge returned", returned );

        returned = gm.loadEdgeVersions( searchByEdge ).toBlocking().singleOrDefault( null );

        assertNull( "No edge returned", returned );
    }


    @Test
    public void testWriteReadEdgeDeleteTarget() {

        GraphManager gm = getHelper( emf.createEdgeManager( scope ) );


        Edge edge = createEdge( "source", "test", "target" );

        gm.writeEdge( edge ).toBlocking().last();

        //now test retrieving it


        SearchByEdgeType search = createSearchByEdge( edge.getTargetNode(), edge.getType(), edge.getTimestamp(), null );

        Observable<Edge> edges = gm.loadEdgesToTarget( search );

        //implicitly blows up if more than 1 is returned from "single"
        Edge returned = edges.toBlocking().single();

        assertEquals( "Correct edge returned", edge, returned );

        SearchByIdType searchById = createSearchByEdgeAndId( edge.getTargetNode(), edge.getType(), edge.getTimestamp(),
                edge.getSourceNode().getType(), null );

        edges = gm.loadEdgesToTargetByType( searchById );

        //implicitly blows up if more than 1 is returned from "single"
        returned = edges.toBlocking().single();

        assertEquals( "Correct edge returned", edge, returned );


        //now delete it
        gm.deleteEdge( edge ).toBlocking().last();

        //now test retrieval, should be null
        edges = gm.loadEdgesToTarget( search );

        //implicitly blows up if more than 1 is returned from "single"
        returned = edges.toBlocking().singleOrDefault( null );

        assertNull( "No edge returned", returned );


        //no search by type, should be null as well

        edges = gm.loadEdgesToTargetByType( searchById );

        //implicitly blows up if more than 1 is returned from "single"
        returned = edges.toBlocking().singleOrDefault( null );

        assertNull( "No edge returned", returned );
    }


    @Test
    public void testWriteReadEdgeTypesSourceTypes() {

        final GraphManager gm = getHelper( emf.createEdgeManager( scope ) );

        Id sourceId = new SimpleId( "source" );
        Id targetId1 = new SimpleId( "target" );
        Id targetId2 = new SimpleId( "target2" );

        Edge testTargetEdge = createEdge( sourceId, "test", targetId1, System.currentTimeMillis() );

        gm.writeEdge( testTargetEdge ).toBlocking().singleOrDefault( null );

        Edge testTarget2Edge = createEdge( sourceId, "test", targetId2, System.currentTimeMillis() );

        gm.writeEdge( testTarget2Edge ).toBlocking().singleOrDefault( null );


        Edge test2TargetEdge = createEdge( sourceId, "test2", targetId1, System.currentTimeMillis() );

        gm.writeEdge( test2TargetEdge ).toBlocking().singleOrDefault( null );


        //get our 2 edge types
        Observable<String> edges =
                gm.getEdgeTypesFromSource( new SimpleSearchEdgeType( testTargetEdge.getSourceNode(), null, null ) );


        Iterator<String> results = edges.toBlocking().getIterator();


        assertEquals( "Edges correct", "test", results.next() );

        assertEquals( "Edges correct", "test2", results.next() );

        assertFalse( "No more edges", results.hasNext() );


        //now test sub edges

        edges = gm.getIdTypesFromSource( new SimpleSearchIdType( testTargetEdge.getSourceNode(), "test", null, null ) );

        results = edges.toBlocking().getIterator();


        assertEquals( "Types correct", targetId1.getType(), results.next() );

        assertEquals( "Types correct", targetId2.getType(), results.next() );

        assertFalse( "No results", results.hasNext() );

        //now get types for test2
        edges = gm.getIdTypesFromSource(
                new SimpleSearchIdType( testTargetEdge.getSourceNode(), "test2", null, null ) );

        results = edges.toBlocking().getIterator();

        assertEquals( "Types correct", targetId1.getType(), results.next() );

        assertFalse( "No results", results.hasNext() );
    }


    @Test
    public void testWriteReadEdgeTypesTargetTypes() {

        final GraphManager gm = getHelper( emf.createEdgeManager( scope ) );

        Id sourceId1 = new SimpleId( "source" );
        Id sourceId2 = new SimpleId( "source2" );
        Id targetId1 = new SimpleId( "target" );


        Edge testTargetEdge = createEdge( sourceId1, "test", targetId1, System.currentTimeMillis() );

        gm.writeEdge( testTargetEdge ).toBlocking().singleOrDefault( null );

        Edge testTarget2Edge = createEdge( sourceId2, "test", targetId1, System.currentTimeMillis() );

        gm.writeEdge( testTarget2Edge ).toBlocking().singleOrDefault( null );


        Edge test2TargetEdge = createEdge( sourceId1, "test2", targetId1, System.currentTimeMillis() );

        gm.writeEdge( test2TargetEdge ).toBlocking().singleOrDefault( null );


        //get our 2 edge types
        final SearchEdgeType edgeTypes = new SimpleSearchEdgeType( testTargetEdge.getTargetNode(), null, null );

        Observable<String> edges = gm.getEdgeTypesToTarget( edgeTypes );


        Iterator<String> results = edges.toBlocking().getIterator();


        assertEquals( "Edges correct", "test", results.next() );

        assertEquals( "Edges correct", "test2", results.next() );

        assertFalse( "No more edges", results.hasNext() );


        //now test sub edges

        edges = gm.getIdTypesToTarget( new SimpleSearchIdType( testTargetEdge.getTargetNode(), "test", null, null ) );

        results = edges.toBlocking().getIterator();

        assertEquals( "Types correct", sourceId1.getType(), results.next() );

        assertEquals( "Types correct", sourceId2.getType(), results.next() );

        assertFalse( "No more edges", results.hasNext() );


        //now get types for test2
        edges = gm.getIdTypesToTarget( new SimpleSearchIdType( testTargetEdge.getTargetNode(), "test2", null, null ) );

        results = edges.toBlocking().getIterator();


        assertEquals( "Types correct", sourceId1.getType(), results.next() );

        assertFalse( "No more edges", results.hasNext() );
    }


    @Test
    public void testWriteReadEdgeTypesSourceTypesPaging() {

        final GraphManager gm = getHelper( emf.createEdgeManager( scope ) );

        Id sourceId1 = new SimpleId( "source" );
        Id targetId1 = new SimpleId( "target" );
        Id targetId2 = new SimpleId( "target2" );


        Edge testTargetEdge = createEdge( sourceId1, "test", targetId1, System.currentTimeMillis() );

        gm.writeEdge( testTargetEdge ).toBlocking().singleOrDefault( null );


        Edge testTargetEdge2 = createEdge( sourceId1, "test", targetId2, System.currentTimeMillis() );

        gm.writeEdge( testTargetEdge2 ).toBlocking().singleOrDefault( null );


        Edge test2TargetEdge = createEdge( sourceId1, "test2", targetId2, System.currentTimeMillis() );

        gm.writeEdge( test2TargetEdge ).toBlocking().singleOrDefault( null );


        //get our 2 edge types
        SearchEdgeType edgeTypes = new SimpleSearchEdgeType( testTargetEdge.getSourceNode(), null, null );

        Observable<String> edges = gm.getEdgeTypesFromSource( edgeTypes );


        Iterator<String> results = edges.toBlocking().getIterator();


        assertEquals( "Edges correct", "test", results.next() );
        assertEquals( "Edges correct", "test2", results.next() );
        assertFalse( "No more edges", results.hasNext() );

        //now load the next page

        //tests that even if a prefix is specified, the last takes precedence
        edgeTypes = new SimpleSearchEdgeType( testTargetEdge.getSourceNode(), null, "test" );

        edges = gm.getEdgeTypesFromSource( edgeTypes );


        results = edges.toBlocking().getIterator();

        assertEquals( "Edges correct", "test2", results.next() );
        assertFalse( "No more edges", results.hasNext() );


        //now test sub edges

        edges = gm.getIdTypesFromSource( new SimpleSearchIdType( testTargetEdge.getSourceNode(), "test", null, null ) );

        results = edges.toBlocking().getIterator();


        assertEquals( "Types correct", targetId1.getType(), results.next() );
        assertEquals( "Types correct", targetId2.getType(), results.next() );
        assertFalse( "No more edges", results.hasNext() );


        //now get the next page

        edges = gm.getIdTypesFromSource(
                new SimpleSearchIdType( testTargetEdge.getSourceNode(), "test", null, targetId1.getType() ) );

        results = edges.toBlocking().getIterator();


        assertEquals( "Types correct", targetId2.getType(), results.next() );

        assertFalse( "No more results", results.hasNext() );
    }


    @Test
    public void testWriteReadEdgeTypesTargetTypesPaging() {

        final GraphManager gm = getHelper( emf.createEdgeManager( scope ) );

        Id sourceId1 = new SimpleId( "source" );
        Id sourceId2 = new SimpleId( "source2" );
        Id targetId = new SimpleId( "target" );


        Edge testTargetEdge = createEdge( sourceId1, "test", targetId, System.currentTimeMillis() );

        gm.writeEdge( testTargetEdge ).toBlocking().singleOrDefault( null );


        Edge testTargetEdge2 = createEdge( sourceId2, "test", targetId, System.currentTimeMillis() );

        gm.writeEdge( testTargetEdge2 ).toBlocking().singleOrDefault( null );

        Edge test2TargetEdge = createEdge( sourceId2, "test2", targetId, System.currentTimeMillis() );

        gm.writeEdge( test2TargetEdge ).toBlocking().singleOrDefault( null );


        //get our 2 edge types
        SearchEdgeType edgeTypes = new SimpleSearchEdgeType( testTargetEdge.getTargetNode(), null, null );

        Observable<String> edges = gm.getEdgeTypesToTarget( edgeTypes );


        Iterator<String> results = edges.toBlocking().getIterator();


        assertEquals( "Edges correct", "test", results.next() );
        assertEquals( "Edges correct", "test2", results.next() );

        assertFalse( "No more edges", results.hasNext() );


        //now load the next page

        edgeTypes = new SimpleSearchEdgeType( testTargetEdge2.getTargetNode(), null, "test" );

        edges = gm.getEdgeTypesToTarget( edgeTypes );


        results = edges.toBlocking().getIterator();


        assertEquals( "Edges correct", "test2", results.next() );


        assertFalse( "No more edges", results.hasNext() );

        //now test sub edges

        edges = gm.getIdTypesToTarget( new SimpleSearchIdType( testTargetEdge.getTargetNode(), "test", null, null ) );

        results = edges.toBlocking().getIterator();


        assertEquals( "Types correct", sourceId1.getType(), results.next() );

        assertEquals( "Types correct", sourceId2.getType(), results.next() );

        assertFalse( "No more edges", results.hasNext() );

        //now get the next page

        edges = gm.getIdTypesToTarget(
                new SimpleSearchIdType( testTargetEdge.getTargetNode(), "test", null, sourceId1.getType() ) );

        results = edges.toBlocking().getIterator();


        assertEquals( "Types correct", sourceId2.getType(), results.next() );

        assertFalse( "No more results", results.hasNext() );
    }


    @Test
    public void testMarkSourceEdges() throws InterruptedException {

        final GraphManager gm = getHelper( emf.createEdgeManager( scope ) );

        Id sourceId = new SimpleId( "source" );
        Id targetId1 = new SimpleId( "target" );
        Id targetId2 = new SimpleId( "target2" );


        final long edge1Time = System.currentTimeMillis();
        final long edge2Time = edge1Time+1;

        Edge edge1 = createEdge( sourceId, "test", targetId1, edge1Time );

        gm.writeEdge( edge1 ).toBlocking().singleOrDefault( null );

        Edge edge2 = createEdge( sourceId, "test", targetId2, edge2Time );

        gm.writeEdge( edge2 ).toBlocking().singleOrDefault( null );


        final long maxVersion = edge2Time+1;


        assertTrue( Long.compare( maxVersion, edge2.getTimestamp() ) > 0 );
        assertTrue( Long.compare( maxVersion, edge1.getTimestamp() ) > 0 );


        //get our 2 edges
        Observable<Edge> edges = gm.loadEdgesFromSource(
                createSearchByEdge( edge1.getSourceNode(), edge1.getType(), maxVersion, null ) );


        Iterator<Edge> results = edges.toBlocking().getIterator();


        System.out.println( "\n\n\n\n\n\n\n\n\n\n" );

        assertEquals( "Edges correct", edge2, results.next() );

        assertEquals( "Edges correct", edge1, results.next() );

        assertFalse( "No more edges", results.hasNext() );

        //now delete one of the edges

        System.out.println( "\n\n\n\n\n\n\n\n\n\n" );

        gm.deleteEdge( edge1 ).toBlocking().last();

        System.out.println( "\n\n\n\n\n\n\n\n\n\n" );


        edges = gm.loadEdgesFromSource(
                createSearchByEdge( edge1.getSourceNode(), edge1.getType(), maxVersion, null ) );


        results = edges.toBlocking().getIterator();


        assertEquals( "Edges correct", edge2, results.next() );

        assertFalse( "No more edges", results.hasNext() );

        System.out.println( "\n\n\n\n\n\n\n\n\n\n" );

        //now delete one of the edges

        gm.deleteEdge( edge2 ).toBlocking().last();

        System.out.println( "\n\n\n\n\n\n\n\n\n\n" );

        edges = gm.loadEdgesFromSource(
                createSearchByEdge( edge1.getSourceNode(), edge1.getType(), maxVersion, null ) );


        results = edges.toBlocking().getIterator();


        assertFalse( "No more edges", results.hasNext() );

        //now delete one of the edges

    }


    @Test
    public void testMarkTargetEdges() {

        final GraphManager gm = getHelper( emf.createEdgeManager( scope ) );

        Id sourceId1 = new SimpleId( "source" );
        Id sourceId2 = new SimpleId( "source2" );
        Id targetId = new SimpleId( "target" );

        Edge edge1 = createEdge( sourceId1, "test", targetId, System.currentTimeMillis() );

        gm.writeEdge( edge1 ).toBlocking().last();

        Edge edge2 = createEdge( sourceId2, "test", targetId, System.currentTimeMillis() );

        gm.writeEdge( edge2 ).toBlocking().last();


        final long maxVersion = System.currentTimeMillis();


        //get our 2 edges
        Observable<Edge> edges =
                gm.loadEdgesToTarget( createSearchByEdge( edge1.getTargetNode(), edge1.getType(), maxVersion, null ) );


        Iterator<Edge> results = edges.toBlocking().getIterator();


        assertEquals( "Edges correct", edge2, results.next() );

        assertEquals( "Edges correct", edge1, results.next() );


        assertFalse( "No more edges", results.hasNext() );

        //now delete one of the edges

        gm.deleteEdge( edge1 ).toBlocking().last();


        edges = gm.loadEdgesToTarget( createSearchByEdge( edge1.getTargetNode(), edge1.getType(), maxVersion, null ) );


        results = edges.toBlocking().getIterator();


        assertEquals( "Edges correct", edge2, results.next() );

        assertFalse( "No more edges", results.hasNext() );

        //now delete one of the edges

        gm.deleteEdge( edge2 ).toBlocking().last();

        edges = gm.loadEdgesToTarget( createSearchByEdge( edge1.getTargetNode(), edge1.getType(), maxVersion, null ) );


        results = edges.toBlocking().getIterator();


        assertFalse( "No more edges", results.hasNext() );

        //now delete one of the edges

    }


    @Test
    public void testMarkSourceEdgesType() {

        final GraphManager gm = getHelper( emf.createEdgeManager( scope ) );

        Id sourceId = new SimpleId( "source" );
        Id targetId1 = new SimpleId( "target" );
        Id targetId2 = new SimpleId( "target2" );

        Edge edge1 = createEdge( sourceId, "test", targetId1, System.currentTimeMillis() );

        gm.writeEdge( edge1 ).toBlocking().singleOrDefault( null );

        Edge edge2 = createEdge( sourceId, "test", targetId2, System.currentTimeMillis() );

        gm.writeEdge( edge2 ).toBlocking().singleOrDefault( null );


        final long maxVersion = System.currentTimeMillis();


        //get our 2 edges
        Observable<Edge> edges = gm.loadEdgesFromSourceByType(
                createSearchByEdgeAndId( sourceId, edge1.getType(), maxVersion, targetId1.getType(), null ) );


        Iterator<Edge> results = edges.toBlocking().getIterator();


        assertEquals( "Edges correct", edge1, results.next() );

        assertFalse( "No more edges", results.hasNext() );

        //now delete one of the edges

        gm.deleteEdge( edge1 ).toBlocking().last();


        edges = gm.loadEdgesFromSourceByType(
                createSearchByEdgeAndId( sourceId, edge1.getType(), maxVersion, targetId1.getType(), null ) );

        results = edges.toBlocking().getIterator();


        assertFalse( "No more edges", results.hasNext() );


        edges = gm.loadEdgesFromSourceByType(
                createSearchByEdgeAndId( sourceId, edge1.getType(), maxVersion, targetId2.getType(), null ) );


        results = edges.toBlocking().getIterator();


        assertEquals( "Edges correct", edge2, results.next() );

        assertFalse( "No more edges", results.hasNext() );

        //now delete one of the edges

        gm.deleteEdge( edge2 ).toBlocking().last();


        edges = gm.loadEdgesFromSourceByType(
                createSearchByEdgeAndId( sourceId, edge1.getType(), maxVersion, targetId2.getType(), null ) );


        results = edges.toBlocking().getIterator();


        assertFalse( "No more edges", results.hasNext() );


        //now delete one of the edges

    }


    @Test
    public void testMarkTargetEdgesType() {

        final GraphManager gm = getHelper( emf.createEdgeManager( scope ) );

        Id sourceId1 = new SimpleId( "source" );
        Id sourceId2 = new SimpleId( "source2" );
        Id targetId = new SimpleId( "target" );

        Edge edge1 = createEdge( sourceId1, "test", targetId, System.currentTimeMillis() );

        gm.writeEdge( edge1 ).toBlocking().last();

        Edge edge2 = createEdge( sourceId2, "test", targetId, System.currentTimeMillis() );

        gm.writeEdge( edge2 ).toBlocking().last();


        final long maxVersion = System.currentTimeMillis();

        //get our 2 edges
        Observable<Edge> edges = gm.loadEdgesToTargetByType(
                createSearchByEdgeAndId( targetId, edge1.getType(), maxVersion, sourceId1.getType(), null ) );


        Iterator<Edge> results = edges.toBlocking().getIterator();


        assertEquals( "Edges correct", edge1, results.next() );

        assertFalse( "No more edges", results.hasNext() );

        //now delete one of the edges

        gm.deleteEdge( edge1 ).toBlocking().last();


        edges = gm.loadEdgesToTargetByType(
                createSearchByEdgeAndId( edge1.getSourceNode(), edge1.getType(), maxVersion, sourceId1.getType(),
                        null ) );

        results = edges.toBlocking().getIterator();


        assertFalse( "No more edges", results.hasNext() );


        edges = gm.loadEdgesToTargetByType(
                createSearchByEdgeAndId( targetId, edge1.getType(), maxVersion, sourceId2.getType(), null ) );


        results = edges.toBlocking().getIterator();


        assertEquals( "Edges correct", edge2, results.next() );

        assertFalse( "No more edges", results.hasNext() );

        //now delete one of the edges

        gm.deleteEdge( edge2 ).toBlocking().last();


        edges = gm.loadEdgesToTargetByType(
                createSearchByEdgeAndId( targetId, edge1.getType(), maxVersion, sourceId2.getType(), null ) );


        results = edges.toBlocking().getIterator();


        assertFalse( "No more edges", results.hasNext() );


        //now delete one of the edges

    }


    @Test
    public void markSourceNode() {

        final GraphManager gm = getHelper( emf.createEdgeManager( scope ) );

        Id sourceId = new SimpleId( "source" );
        Id targetId1 = new SimpleId( "target" );
        Id targetId2 = new SimpleId( "target2" );

        Edge edge1 = createEdge( sourceId, "test", targetId1, System.currentTimeMillis() );

        gm.writeEdge( edge1 ).toBlocking().singleOrDefault( null );

        Edge edge2 = createEdge( sourceId, "test", targetId2, System.currentTimeMillis() );

        gm.writeEdge( edge2 ).toBlocking().singleOrDefault( null );


        final long maxVersion = System.currentTimeMillis();

        Iterator<Edge> results =
                gm.loadEdgesFromSource( createSearchByEdge( sourceId, edge1.getType(), maxVersion, null ) )
                  .toBlocking().getIterator();


        assertEquals( "Edge found", edge2, results.next() );

        assertEquals( "Edge found", edge1, results.next() );

        assertFalse( "No more edges", results.hasNext() );


        //get our 2 edges
        results = gm.loadEdgesFromSourceByType(
                createSearchByEdgeAndId( sourceId, edge1.getType(), maxVersion, targetId1.getType(), null ) )
                    .toBlocking().getIterator();


        assertEquals( "Edges correct", edge1, results.next() );

        assertFalse( "No more edges", results.hasNext() );

        //now delete one of the edges
        results = gm.loadEdgesFromSourceByType(
                createSearchByEdgeAndId( sourceId, edge2.getType(), maxVersion, targetId2.getType(), null ) )
                    .toBlocking().getIterator();


        assertEquals( "Edges correct", edge2, results.next() );

        assertFalse( "No more edges", results.hasNext() );

        //mark the source node
        gm.deleteNode( sourceId, edge2.getTimestamp() ).toBlocking().last();


        //now re-read, nothing should be there since they're marked

        results = gm.loadEdgesFromSource( createSearchByEdge( sourceId, edge1.getType(), maxVersion, null ) )
                    .toBlocking().getIterator();

        assertFalse( "No more edges", results.hasNext() );


        //get our 2 edges
        results = gm.loadEdgesFromSourceByType(
                createSearchByEdgeAndId( sourceId, edge1.getType(), maxVersion, targetId1.getType(), null ) )
                    .toBlocking().getIterator();


        assertFalse( "No more edges", results.hasNext() );

        //now delete one of the edges
        results = gm.loadEdgesFromSourceByType(
                createSearchByEdgeAndId( sourceId, edge2.getType(), maxVersion, targetId2.getType(), null ) )
                    .toBlocking().getIterator();


        assertFalse( "No more edges", results.hasNext() );
    }


    @Test
    public void markTargetNode() {

        final GraphManager gm = getHelper( emf.createEdgeManager( scope ) );

        Id sourceId1 = new SimpleId( "source" );
        Id sourceId2 = new SimpleId( "source2" );
        Id targetId = new SimpleId( "target" );

        Edge edge1 = createEdge( sourceId1, "test", targetId, System.currentTimeMillis() );

        gm.writeEdge( edge1 ).toBlocking().singleOrDefault( null );

        Edge edge2 = createEdge( sourceId2, "test", targetId, System.currentTimeMillis() );

        gm.writeEdge( edge2 ).toBlocking().singleOrDefault( null );


        final long maxVersion = System.currentTimeMillis();

        Iterator<Edge> results =
                gm.loadEdgesToTarget( createSearchByEdge( targetId, edge1.getType(), maxVersion, null ) )
                  .toBlocking().getIterator();


        assertEquals( "Edge found", edge2, results.next() );

        assertEquals( "Edge found", edge1, results.next() );

        assertFalse( "No more edges", results.hasNext() );


        //get our 2 edges
        results = gm.loadEdgesToTargetByType(
                createSearchByEdgeAndId( targetId, edge1.getType(), maxVersion, sourceId1.getType(), null ) )
                    .toBlocking().getIterator();


        assertEquals( "Edges correct", edge1, results.next() );

        assertFalse( "No more edges", results.hasNext() );

        //now delete one of the edges
        results = gm.loadEdgesToTargetByType(
                createSearchByEdgeAndId( targetId, edge2.getType(), maxVersion, sourceId2.getType(), null ) )
                    .toBlocking().getIterator();


        assertEquals( "Edges correct", edge2, results.next() );

        assertFalse( "No more edges", results.hasNext() );

        //mark the source node
        gm.deleteNode( targetId, edge2.getTimestamp() ).toBlocking().last();


        //now re-read, nothing should be there since they're marked

        results = gm.loadEdgesToTarget( createSearchByEdge( targetId, edge1.getType(), maxVersion, null ) )
                    .toBlocking().getIterator();

        assertFalse( "No more edges", results.hasNext() );


        //get our 2 edges
        results = gm.loadEdgesToTargetByType(
                createSearchByEdgeAndId( targetId, edge1.getType(), maxVersion, sourceId1.getType(), null ) )
                    .toBlocking().getIterator();


        assertFalse( "No more edges", results.hasNext() );

        //now delete one of the edges
        results = gm.loadEdgesToTargetByType(
                createSearchByEdgeAndId( targetId, edge2.getType(), maxVersion, sourceId2.getType(), null ) )
                    .toBlocking().getIterator();


        assertFalse( "No more edges", results.hasNext() );
    }


    @Test
    public void testWriteReadEdgeTypesSourceTypesPrefix() {

        final GraphManager gm = getHelper( emf.createEdgeManager( scope ) );

        Id sourceId = new SimpleId( "source" );
        Id targetId = new SimpleId( "target" );

        Edge testTargetEdge = createEdge( sourceId, "test1edge1", targetId, System.currentTimeMillis() );

        gm.writeEdge( testTargetEdge ).toBlocking().singleOrDefault( null );

        Edge testTarget2Edge = createEdge( sourceId, "test1edge2", targetId, System.currentTimeMillis() );

        gm.writeEdge( testTarget2Edge ).toBlocking().singleOrDefault( null );


        Edge test2TargetEdge = createEdge( sourceId, "test2edge1", targetId, System.currentTimeMillis() );

        gm.writeEdge( test2TargetEdge ).toBlocking().singleOrDefault( null );


        //get our 2 edge types
        Observable<String> edges =
                gm.getEdgeTypesFromSource( new SimpleSearchEdgeType( testTargetEdge.getSourceNode(), "test1", null ) );


        Iterator<String> results = edges.toBlocking().getIterator();


        assertEquals( "Edges correct", "test1edge1", results.next() );

        assertEquals( "Edges correct", "test1edge2", results.next() );

        assertFalse( "No more edges", results.hasNext() );


        edges = gm.getEdgeTypesFromSource( new SimpleSearchEdgeType( testTargetEdge.getSourceNode(), "test2", null ) );


        results = edges.toBlocking().getIterator();


        assertEquals( "Edges correct", "test2edge1", results.next() );

        assertFalse( "No more edges", results.hasNext() );
    }


    @Test
    public void testSourceSubTypes() {

        //now test sub edges
        final GraphManager gm = getHelper( emf.createEdgeManager( scope ) );

        Id sourceId = new SimpleId( "source" );
        Id targetId1target1 = new SimpleId( "type1target1" );
        Id targetId1target2 = new SimpleId( "type1target2" );
        Id targetId2 = new SimpleId( "type2target2" );

        Edge testTargetEdge = createEdge( sourceId, "test", targetId1target1, System.currentTimeMillis() );

        gm.writeEdge( testTargetEdge ).toBlocking().singleOrDefault( null );

        Edge testTarget2Edge = createEdge( sourceId, "test", targetId1target2, System.currentTimeMillis() );

        gm.writeEdge( testTarget2Edge ).toBlocking().singleOrDefault( null );


        Edge test2TargetEdge = createEdge( sourceId, "test", targetId2, System.currentTimeMillis() );

        gm.writeEdge( test2TargetEdge ).toBlocking().singleOrDefault( null );


        Observable<String> edges = gm.getIdTypesFromSource(
                new SimpleSearchIdType( testTargetEdge.getSourceNode(), "test", "type1", null ) );

        Iterator<String> results = edges.toBlocking().getIterator();


        assertEquals( "Types correct", targetId1target1.getType(), results.next() );

        assertEquals( "Types correct", targetId1target2.getType(), results.next() );

        assertFalse( "No results", results.hasNext() );

        //now get types for test2
        edges = gm.getIdTypesFromSource(
                new SimpleSearchIdType( testTargetEdge.getSourceNode(), "test", "type2", null ) );

        results = edges.toBlocking().getIterator();


        assertEquals( "Types correct", targetId2.getType(), results.next() );

        assertFalse( "No results", results.hasNext() );
    }


    @Test
    public void testWriteReadEdgeTypesTargetTypesPrefix() {

        final GraphManager gm = getHelper( emf.createEdgeManager( scope ) );

        Id targetId = new SimpleId( "target" );
        Id sourceId = new SimpleId( "source" );

        Edge testTargetEdge = createEdge( sourceId, "test1edge1", targetId, System.currentTimeMillis() );

        gm.writeEdge( testTargetEdge ).toBlocking().singleOrDefault( null );

        Edge testTarget2Edge = createEdge( sourceId, "test1edge2", targetId, System.currentTimeMillis() );

        gm.writeEdge( testTarget2Edge ).toBlocking().singleOrDefault( null );


        Edge test2TargetEdge = createEdge( sourceId, "test2edge1", targetId, System.currentTimeMillis() );

        gm.writeEdge( test2TargetEdge ).toBlocking().singleOrDefault( null );


        //get our 2 edge types
        Observable<String> edges =
                gm.getEdgeTypesToTarget( new SimpleSearchEdgeType( testTargetEdge.getTargetNode(), "test1", null ) );


        Iterator<String> results = edges.toBlocking().getIterator();


        assertEquals( "Edges correct", "test1edge1", results.next() );

        assertEquals( "Edges correct", "test1edge2", results.next() );

        assertFalse( "No more edges", results.hasNext() );


        edges = gm.getEdgeTypesToTarget( new SimpleSearchEdgeType( testTargetEdge.getTargetNode(), "test2", null ) );


        results = edges.toBlocking().getIterator();


        assertEquals( "Edges correct", "test2edge1", results.next() );

        assertFalse( "No more edges", results.hasNext() );
    }


    @Test
    public void testTargetSubTypes() {

        //now test sub edges
        final GraphManager gm = getHelper( emf.createEdgeManager( scope ) );

        Id targetId = new SimpleId( "target" );
        Id sourceId1target1 = new SimpleId( "type1source1" );
        Id sourceId1target2 = new SimpleId( "type1source2" );
        Id sourceId2 = new SimpleId( "type2source2" );

        Edge testTargetEdge = createEdge( sourceId1target1, "test", targetId, System.currentTimeMillis() );

        gm.writeEdge( testTargetEdge ).toBlocking().singleOrDefault( null );

        Edge testTarget2Edge = createEdge( sourceId1target2, "test", targetId, System.currentTimeMillis() );

        gm.writeEdge( testTarget2Edge ).toBlocking().singleOrDefault( null );


        Edge test2TargetEdge = createEdge( sourceId2, "test", targetId, System.currentTimeMillis() );

        gm.writeEdge( test2TargetEdge ).toBlocking().singleOrDefault( null );


        Observable<String> edges = gm.getIdTypesToTarget(
                new SimpleSearchIdType( testTargetEdge.getTargetNode(), "test", "type1", null ) );

        Iterator<String> results = edges.toBlocking().getIterator();


        assertEquals( "Types correct", sourceId1target1.getType(), results.next() );

        assertEquals( "Types correct", sourceId1target2.getType(), results.next() );

        assertFalse( "No results", results.hasNext() );

        //now get types for test2
        edges = gm.getIdTypesToTarget(
                new SimpleSearchIdType( testTargetEdge.getTargetNode(), "test", "type2", null ) );

        results = edges.toBlocking().getIterator();


        assertEquals( "Types correct", sourceId2.getType(), results.next() );

        assertFalse( "No results", results.hasNext() );
    }


    @Test( expected = NullPointerException.class )
    public void invalidEdgeTypesWrite(  ) {
        final GraphManager em = emf.createEdgeManager( scope );

        em.writeEdge( null );
    }


    @Test( expected = NullPointerException.class )
    public void invalidEdgeTypesDelete( ) {
        final GraphManager em = emf.createEdgeManager( scope );

        em.deleteEdge( null );
    }
}





