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
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import org.apache.usergrid.persistence.collection.OrganizationScope;
import org.apache.usergrid.persistence.graph.impl.SimpleEdge;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchByEdgeType;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchByIdType;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchEdgeIdType;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchEdgeType;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.google.inject.Inject;

import rx.Observable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 *
 *
 */
public class EdgeManagerTest {


    @Inject
    protected EdgeManagerFactory emf;

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

        EdgeManager em = emf.createEdgeManager( scope );


        Edge edge = createRealEdge( "source", "test", "target" );

        em.writeEdge( edge );

        //now test retrieving it

        SearchByEdgeType search = createSearchByEdge( edge.getSourceNode(), edge.getType(), edge.getVersion(), null );

        Observable<Edge> edges = em.loadSourceEdges( search );

        //implicitly blows up if more than 1 is returned from "single"
        Edge returned = edges.toBlockingObservable().single();

        assertEquals( "Correct edge returned", edge, returned );

        //change edge type to be invalid, shouldn't get a result
        search = createSearchByEdge( edge.getSourceNode(), edge.getType() + "invalid", edge.getVersion(), null );

        edges = em.loadSourceEdges( search );

        //implicitly blows up if more than 1 is returned from "single"
        returned = edges.toBlockingObservable().singleOrDefault( null );

        assertNull( "Invalid type should not be returned", returned );
    }


    @Test
    public void testWriteReadEdgeTypeTarget() {

        EdgeManager em = emf.createEdgeManager( scope );


        Edge edge = createRealEdge( "source", "test", "target" );

        em.writeEdge( edge );

        //now test retrieving it

        SearchByEdgeType search = createSearchByEdge( edge.getTargetNode(), edge.getType(), edge.getVersion(), null );

        Observable<Edge> edges = em.loadTargetEdges( search );

        //implicitly blows up if more than 1 is returned from "single"
        Edge returned = edges.toBlockingObservable().single();

        assertEquals( "Correct edge returned", edge, returned );

        //change edge type to be invalid, shouldn't get a result
        search = createSearchByEdge( edge.getTargetNode(), edge.getType() + "invalid", edge.getVersion(), null );

        edges = em.loadTargetEdges( search );

        //implicitly blows up if more than 1 is returned from "single"
        returned = edges.toBlockingObservable().singleOrDefault( null );

        assertNull( "Invalid type should not be returned", returned );
    }


    @Test
    public void testWriteReadEdgeTypeVersionSource() {

        EdgeManager em = emf.createEdgeManager( scope );

        final UUID earlyVersion = UUIDGenerator.newTimeUUID();


        Edge edge = createRealEdge( "source", "test", "target" );

        em.writeEdge( edge );

        //now test retrieving it

        SearchByEdgeType search = createSearchByEdge( edge.getSourceNode(), edge.getType(), edge.getVersion(), null );

        Observable<Edge> edges = em.loadSourceEdges( search );

        //implicitly blows up if more than 1 is returned from "single"
        Edge returned = edges.toBlockingObservable().single();

        assertEquals( "Correct edge returned", edge, returned );

        //now test with an earlier version, we shouldn't get the edge back
        search = createSearchByEdge( edge.getSourceNode(), edge.getType(), earlyVersion, null );

        edges = em.loadSourceEdges( search );

        //implicitly blows up if more than 1 is returned from "single"
        returned = edges.toBlockingObservable().singleOrDefault( null );

        assertNull( "Earlier version should not be returned", returned );
    }


    @Test
    public void testWriteReadEdgeTypeVersionTarget() {

        EdgeManager em = emf.createEdgeManager( scope );


        final UUID earlyVersion = UUIDGenerator.newTimeUUID();


        Edge edge = createRealEdge( "source", "test", "target" );

        em.writeEdge( edge );

        //now test retrieving it

        SearchByEdgeType search = createSearchByEdge( edge.getTargetNode(), edge.getType(), edge.getVersion(), null );

        Observable<Edge> edges = em.loadTargetEdges( search );

        //implicitly blows up if more than 1 is returned from "single"
        Edge returned = edges.toBlockingObservable().single();

        assertEquals( "Correct edge returned", edge, returned );

        //change edge type to be invalid, shouldn't get a result
        search = createSearchByEdge( edge.getTargetNode(), edge.getType(), earlyVersion, null );

        edges = em.loadTargetEdges( search );

        //implicitly blows up if more than 1 is returned from "single"
        returned = edges.toBlockingObservable().singleOrDefault( null );

        assertNull( "Earlier version should not be returned", returned );
    }


    @Test
    public void testWriteReadEdgeTypePagingSource() {

        EdgeManager em = emf.createEdgeManager( scope );


        Edge edge1 = createRealEdge( "source", "test", "target" );

        em.writeEdge( edge1 );

        Edge edge2 = createRealEdge( "source", "test", "target" );

        em.writeEdge( edge2 );

        Edge edge3 = createRealEdge( "source", "test", "target" );

        em.writeEdge( edge3 );


        //now test retrieving it

        SearchByEdgeType search =
                createSearchByEdge( edge1.getSourceNode(), edge1.getType(), edge1.getVersion(), null );

        Observable<Edge> edges = em.loadSourceEdges( search );

        //implicitly blows up if more than 1 is returned from "single"
        Iterator<Edge> returned = edges.toBlockingObservable().next().iterator();


        //now start from the 2nd edge
        assertEquals( "Correct edge returned", edge1, returned.next() );

        assertEquals( "Correct edge returned", edge2, returned.next() );

        search = createSearchByEdge( edge1.getSourceNode(), edge1.getType(), edge1.getVersion(), edge2 );

        edges = em.loadSourceEdges( search );

        //implicitly blows up if more than 1 is returned from "single"
        returned = edges.toBlockingObservable().next().iterator();

        assertEquals( "Paged correctly", edge3, returned.next() );

        assertFalse( "End of stream", returned.hasNext() );
    }


    @Test
    public void testWriteReadEdgeTypePagingTarget() {


        EdgeManager em = emf.createEdgeManager( scope );


        Edge edge1 = createRealEdge( "source", "test", "target" );

        em.writeEdge( edge1 );

        Edge edge2 = createRealEdge( "source", "test", "target" );

        em.writeEdge( edge2 );

        Edge edge3 = createRealEdge( "source", "test", "target" );

        em.writeEdge( edge3 );


        //now test retrieving it

        SearchByEdgeType search =
                createSearchByEdge( edge1.getTargetNode(), edge1.getType(), edge1.getVersion(), null );

        Observable<Edge> edges = em.loadTargetEdges( search );

        //implicitly blows up if more than 1 is returned from "single"
        Iterator<Edge> returned = edges.toBlockingObservable().next().iterator();


        //now start from the 2nd edge
        assertEquals( "Correct edge returned", edge1, returned.next() );

        assertEquals( "Correct edge returned", edge2, returned.next() );

        search = createSearchByEdge( edge1.getTargetNode(), edge1.getType(), edge1.getVersion(), edge2 );

        edges = em.loadTargetEdges( search );

        //implicitly blows up if more than 1 is returned from "single"
        returned = edges.toBlockingObservable().next().iterator();

        assertEquals( "Paged correctly", edge3, returned.next() );

        assertFalse( "End of stream", returned.hasNext() );
    }


    @Test
    public void testWriteReadEdgeTypeTargetTypeSource() {

        EdgeManager em = emf.createEdgeManager( scope );


        Edge edge = createRealEdge( "source", "test", "target" );

        em.writeEdge( edge );

        //now test retrieving it

        SearchByIdType search = createSearchByEdgeAndId( edge.getSourceNode(), edge.getType(), edge.getVersion(),
                edge.getTargetNode().getType(), null );

        Observable<Edge> edges = em.loadSourceEdges( search );

        //implicitly blows up if more than 1 is returned from "single"
        Edge returned = edges.toBlockingObservable().single();

        assertEquals( "Correct edge returned", edge, returned );


        //change edge type to be invalid, shouldn't get a result
        search = createSearchByEdgeAndId( edge.getSourceNode(), edge.getType(), edge.getVersion(),
                edge.getTargetNode().getType() + "invalid", null );

        edges = em.loadSourceEdges( search );

        //implicitly blows up if more than 1 is returned from "single"
        returned = edges.toBlockingObservable().singleOrDefault( null );

        assertNull( "Invalid type should not be returned", returned );
    }


    @Test
    public void testWriteReadEdgeTypeTargetTypeTarget() {

        EdgeManager em = emf.createEdgeManager( scope );


        Edge edge = createRealEdge( "source", "test", "target" );

        em.writeEdge( edge );

        //now test retrieving it

        SearchByIdType search = createSearchByEdgeAndId( edge.getTargetNode(), edge.getType(), edge.getVersion(),
                edge.getSourceNode().getType(), null );

        Observable<Edge> edges = em.loadTargetEdges( search );

        //implicitly blows up if more than 1 is returned from "single"
        Edge returned = edges.toBlockingObservable().single();

        assertEquals( "Correct edge returned", edge, returned );


        //change edge type to be invalid, shouldn't get a result
        search = createSearchByEdgeAndId( edge.getTargetNode(), edge.getType(), edge.getVersion(),
                edge.getSourceNode().getType() + "invalid", null );

        edges = em.loadTargetEdges( search );

        //implicitly blows up if more than 1 is returned from "single"
        returned = edges.toBlockingObservable().singleOrDefault( null );

        assertNull( "Invalid type should not be returned", returned );
    }


    @Test
    public void testWriteReadEdgeDeleteSource() {

        EdgeManager em = emf.createEdgeManager( scope );


        Edge edge = createRealEdge( "source", "test", "target" );

        em.writeEdge( edge );

        //now test retrieving it


        SearchByEdgeType search = createSearchByEdge( edge.getSourceNode(), edge.getType(), edge.getVersion(), null );

        Observable<Edge> edges = em.loadSourceEdges( search );

        //implicitly blows up if more than 1 is returned from "single"
        Edge returned = edges.toBlockingObservable().single();

        assertEquals( "Correct edge returned", edge, returned );

        SearchByIdType searchById = createSearchByEdgeAndId( edge.getSourceNode(), edge.getType(), edge.getVersion(),
                edge.getTargetNode().getType(), null );

        edges = em.loadSourceEdges( searchById );

        //implicitly blows up if more than 1 is returned from "single"
        returned = edges.toBlockingObservable().single();

        assertEquals( "Correct edge returned", edge, returned );


        //now delete it
        em.deleteEdge( edge );

        //now test retrieval, should be null
        edges = em.loadSourceEdges( search );

        //implicitly blows up if more than 1 is returned from "single"
        returned = edges.toBlockingObservable().singleOrDefault( null );

        assertNull( "No edge returned", returned );


        //no search by type, should be null as well

        edges = em.loadSourceEdges( searchById );

        //implicitly blows up if more than 1 is returned from "single"
        returned = edges.toBlockingObservable().singleOrDefault( null );

        assertNull( "No edge returned", returned );
    }


    @Test
    public void testWriteReadEdgeDeleteTarget() {

        EdgeManager em = emf.createEdgeManager( scope );


        Edge edge = createRealEdge( "source", "test", "target" );

        em.writeEdge( edge );

        //now test retrieving it


        SearchByEdgeType search = createSearchByEdge( edge.getTargetNode(), edge.getType(), edge.getVersion(), null );

        Observable<Edge> edges = em.loadSourceEdges( search );

        //implicitly blows up if more than 1 is returned from "single"
        Edge returned = edges.toBlockingObservable().single();

        assertEquals( "Correct edge returned", edge, returned );

        SearchByIdType searchById = createSearchByEdgeAndId( edge.getTargetNode(), edge.getType(), edge.getVersion(),
                edge.getSourceNode().getType(), null );

        edges = em.loadSourceEdges( searchById );

        //implicitly blows up if more than 1 is returned from "single"
        returned = edges.toBlockingObservable().single();

        assertEquals( "Correct edge returned", edge, returned );


        //now delete it
        em.deleteEdge( edge );

        //now test retrieval, should be null
        edges = em.loadTargetEdges( search );

        //implicitly blows up if more than 1 is returned from "single"
        returned = edges.toBlockingObservable().singleOrDefault( null );

        assertNull( "No edge returned", returned );


        //no search by type, should be null as well

        edges = em.loadTargetEdges( searchById );

        //implicitly blows up if more than 1 is returned from "single"
        returned = edges.toBlockingObservable().singleOrDefault( null );

        assertNull( "No edge returned", returned );
    }


    @Test
    public void testWriteReadEdgeTypesTargetTypes() {

        final EdgeManager em = emf.createEdgeManager( scope );

        Id sourceId = new SimpleId( "source" );
        Id targetId1 = new SimpleId( "target" );
        Id targetId2 = new SimpleId( "target2" );

        Edge testTargetEdge = new SimpleEdge( sourceId, "test", targetId1, UUIDGenerator.newTimeUUID() );

        em.writeEdge( testTargetEdge );

        Edge testTarget2Edge = new SimpleEdge( sourceId, "test", targetId2, UUIDGenerator.newTimeUUID() );

        em.writeEdge( testTarget2Edge );


        Edge test2TargetEdge = new SimpleEdge( sourceId, "test2", targetId1, UUIDGenerator.newTimeUUID() );

        em.writeEdge( test2TargetEdge );


        //get our 2 edge types
        Observable<String> edges =
                em.getTargetEdgeTypes( new SimpleSearchEdgeType( testTargetEdge.getSourceNode(), null ) );


        List<String> results = edges.toList().toBlockingObservable().single();

        assertEquals( "Size correct", 2, results.size() );

        assertTrue( "Edges correct", results.contains( "test" ) );

        assertTrue( "Edges correct", results.contains( "test2" ) );

        //now test sub edges

        edges = em.getTargetEdgeIdTypes( new SimpleSearchEdgeIdType( testTargetEdge.getSourceNode(), "test", null ) );

        results = edges.toList().toBlockingObservable().single();

        assertEquals( "Size correct", 2, results.size() );

        assertTrue( "Types correct", results.contains( targetId1 ) );

        assertTrue( "Types correct", results.contains( targetId2 ) );

        //now get types for test2
        edges = em.getTargetEdgeIdTypes( new SimpleSearchEdgeIdType( testTargetEdge.getSourceNode(), "test2", null ) );

        results = edges.toList().toBlockingObservable().single();

        assertEquals( "Size correct", 1, results.size() );

        assertTrue( "Types correct", results.contains( targetId1 ) );

        //now delete our edges, we shouldn't get anything back
        em.deleteEdge( testTargetEdge );
        em.deleteEdge( testTarget2Edge );
        em.deleteEdge( test2TargetEdge );


        edges = em.getTargetEdgeTypes( new SimpleSearchEdgeType( testTargetEdge.getSourceNode(), null ) );

        results = edges.toList().toBlockingObservable().single();

        assertEquals( "No results", 0, results.size() );
    }


    @Test
    public void testWriteReadEdgeTypesSourceTypes() {

        final EdgeManager em = emf.createEdgeManager( scope );

        Id sourceId1 = new SimpleId( "source" );
        Id sourceId2 = new SimpleId( "source2" );
        Id targetId1 = new SimpleId( "target" );


        Edge testTargetEdge = new SimpleEdge( sourceId1, "test", targetId1, UUIDGenerator.newTimeUUID() );

        em.writeEdge( testTargetEdge );

        Edge testTarget2Edge = new SimpleEdge( sourceId2, "test", targetId1, UUIDGenerator.newTimeUUID() );

        em.writeEdge( testTarget2Edge );


        Edge test2TargetEdge = new SimpleEdge( sourceId1, "test2", targetId1, UUIDGenerator.newTimeUUID() );

        em.writeEdge( test2TargetEdge );


        //get our 2 edge types
        final SearchEdgeType edgeTypes = new SimpleSearchEdgeType( testTargetEdge.getTargetNode(), null );

        Observable<String> edges = em.getSourceEdgeTypes( edgeTypes );


        List<String> results = edges.toList().toBlockingObservable().single();

        assertEquals( "Size correct", 2, results.size() );

        assertTrue( "Edges correct", results.contains( "test" ) );

        assertTrue( "Edges correct", results.contains( "test2" ) );


        //now test sub edges

        edges = em.getSourceEdgeTypes( new SimpleSearchEdgeIdType( testTargetEdge.getTargetNode(), "test", null ) );

        results = edges.toList().toBlockingObservable().single();

        assertEquals( "Size correct", 2, results.size() );

        assertTrue( "Types correct", results.contains( sourceId1.getType() ) );

        assertTrue( "Types correct", results.contains( sourceId2.getType() ) );


        //now get types for test2
        edges = em.getSourceEdgeIdTypes( new SimpleSearchEdgeIdType( testTargetEdge.getTargetNode(), "test2", null ) );

        results = edges.toList().toBlockingObservable().single();

        assertEquals( "Size correct", 1, results.size() );

        assertTrue( "Types correct", results.contains( sourceId1.getType() ) );


        em.deleteEdge( testTargetEdge );
        em.deleteEdge( testTarget2Edge );
        em.deleteEdge( test2TargetEdge );


        edges = em.getTargetEdgeTypes( new SimpleSearchEdgeType( testTargetEdge.getSourceNode(), null ) );

        results = edges.toList().toBlockingObservable().single();

        assertEquals( "No results", 0, results.size() );
    }


    @Test
    public void testWriteReadEdgeTypesTargetTypesPaging() {

        final EdgeManager em = emf.createEdgeManager( scope );

        Id sourceId1 = new SimpleId( "source" );
        Id targetId1 = new SimpleId( "target" );
        Id targetId2 = new SimpleId( "target2" );


        Edge testTargetEdge = new SimpleEdge( sourceId1, "test", targetId1, UUIDGenerator.newTimeUUID() );

        em.writeEdge( testTargetEdge );


        Edge testTargetEdge2 = new SimpleEdge( sourceId1, "test", targetId2, UUIDGenerator.newTimeUUID() );

        em.writeEdge( testTargetEdge2 );


        Edge test2TargetEdge = new SimpleEdge( sourceId1, "test2", targetId2, UUIDGenerator.newTimeUUID() );

        em.writeEdge( test2TargetEdge );


        //get our 2 edge types
        SearchEdgeType edgeTypes = new SimpleSearchEdgeType( testTargetEdge.getSourceNode(), null );

        Observable<String> edges = em.getTargetEdgeTypes( edgeTypes );


        Iterator<String> results = edges.toBlockingObservable().getIterator();


        assertEquals( "Edges correct", "test", results.next() );

        //now load the next page

        edgeTypes = new SimpleSearchEdgeType( testTargetEdge.getTargetNode(), "test" );

        edges = em.getTargetEdgeTypes( edgeTypes );


        results = edges.toBlockingObservable().getIterator();


        assertEquals( "Edges correct", "test2", results.next() );


        //now test sub edges

        edges = em.getTargetEdgeIdTypes( new SimpleSearchEdgeIdType( testTargetEdge.getSourceNode(), "test", null ) );

        results = edges.toBlockingObservable().getIterator();


        assertEquals( "Types correct", targetId1.getType(), results.next() );

        //now get the next page

        edges = em.getTargetEdgeIdTypes(
                new SimpleSearchEdgeIdType( testTargetEdge.getSourceNode(), "test", targetId1.getType() ) );

        results = edges.toBlockingObservable().getIterator();


        assertEquals( "Types correct", targetId2.getType(), results.next() );

        assertFalse( "No more results", results.hasNext() );
    }


    @Test
    public void testWriteReadEdgeTypesSourceTypesPaging() {

        final EdgeManager em = emf.createEdgeManager( scope );

        Id sourceId1 = new SimpleId( "source" );
        Id sourceId2 = new SimpleId( "source2" );
        Id targetId = new SimpleId( "target" );


        Edge testTargetEdge = new SimpleEdge( sourceId1, "test", targetId, UUIDGenerator.newTimeUUID() );

        em.writeEdge( testTargetEdge );


        Edge testTargetEdge2 = new SimpleEdge( sourceId2, "test", targetId, UUIDGenerator.newTimeUUID() );

        em.writeEdge( testTargetEdge2 );

        Edge test2TargetEdge = new SimpleEdge( sourceId2, "test2", targetId, UUIDGenerator.newTimeUUID() );

        em.writeEdge( test2TargetEdge );


        //get our 2 edge types
        SearchEdgeType edgeTypes = new SimpleSearchEdgeType( testTargetEdge.getTargetNode(), null );

        Observable<String> edges = em.getSourceEdgeTypes( edgeTypes );


        Iterator<String> results = edges.toBlockingObservable().getIterator();


        assertEquals( "Edges correct", "test", results.next() );

        //now load the next page

        edgeTypes = new SimpleSearchEdgeType( testTargetEdge.getTargetNode(), "test" );

        edges = em.getSourceEdgeTypes( edgeTypes );


        results = edges.toBlockingObservable().getIterator();


        assertEquals( "Edges correct", "test2", results.next() );


        //now test sub edges

        edges = em.getSourceEdgeTypes( new SimpleSearchEdgeIdType( testTargetEdge.getTargetNode(), "test", null ) );

        results = edges.toBlockingObservable().getIterator();


        assertEquals( "Types correct", sourceId1.getType(), results.next() );

        //now get the next page

        edges = em.getSourceEdgeTypes(
                new SimpleSearchEdgeIdType( testTargetEdge.getTargetNode(), "test", sourceId1.getType() ) );

        results = edges.toBlockingObservable().getIterator();


        assertEquals( "Types correct", sourceId2.getType(), results.next() );

        assertFalse( "No more results", results.hasNext() );
    }


    //TODO invalid input testing with Jukito


    private Edge createRealEdge( final String sourceType, final String edgeType, final String targetType ) {


        Id sourceId = mock( Id.class );
        when( sourceId.getType() ).thenReturn( sourceType );
        when( sourceId.getUuid() ).thenReturn( UUIDGenerator.newTimeUUID() );


        Id targetId = mock( Id.class );
        when( targetId.getType() ).thenReturn( targetType );
        when( targetId.getUuid() ).thenReturn( UUIDGenerator.newTimeUUID() );

        return new SimpleEdge( sourceId, edgeType, targetId, UUIDGenerator.newTimeUUID() );
    }


    private SearchByEdgeType createSearchByEdge( final Id sourceId, final String type, final UUID maxVersion,
                                                 final Edge last ) {
        return new SimpleSearchByEdgeType( sourceId, type, maxVersion, last );
    }


    private SearchByIdType createSearchByEdgeAndId( final Id sourceId, final String type, final UUID maxVersion,
                                                    final String idType, final Edge last ) {
        return new SimpleSearchByIdType( sourceId, type, maxVersion, idType, last );
    }
}




