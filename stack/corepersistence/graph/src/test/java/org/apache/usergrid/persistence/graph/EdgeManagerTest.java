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


import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import org.apache.usergrid.persistence.collection.OrganizationScope;
import org.apache.usergrid.persistence.graph.impl.SimpleEdge;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchByEdgeType;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchByIdType;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.google.inject.Inject;

import rx.Observable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
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
    public void testWriteReadEdgeType() {

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
    public void testWriteReadEdgeTypeTargetType() {

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
    public void testWriteReadEdgeDelete() {

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


    private Edge createRealEdge( final String sourceType, final String edgeType, final String targetType ) {


        Id sourceId = mock( Id.class );
        when( sourceId.getType() ).thenReturn( sourceType );
        when( sourceId.getUuid() ).thenReturn( UUIDGenerator.newTimeUUID() );


        Id targetId = mock( Id.class );
        when( targetId.getType() ).thenReturn( targetType );
        when( targetId.getUuid() ).thenReturn( UUIDGenerator.newTimeUUID() );

        return new SimpleEdge( sourceId, edgeType, targetId, UUIDGenerator.newTimeUUID() );
    }


    private Edge createMockEdge( final String sourceType, final String edgeType, final String targetType ) {

        Id sourceId = mock( Id.class );
        when( sourceId.getType() ).thenReturn( sourceType );
        when( sourceId.getUuid() ).thenReturn( UUIDGenerator.newTimeUUID() );


        Id targetId = mock( Id.class );
        when( targetId.getType() ).thenReturn( targetType );
        when( targetId.getUuid() ).thenReturn( UUIDGenerator.newTimeUUID() );


        Edge edge = mock( Edge.class );


        when( edge.getSourceNode() ).thenReturn( sourceId );
        when( edge.getType() ).thenReturn( edgeType );
        when( edge.getVersion() ).thenReturn( UUIDGenerator.newTimeUUID() );
        when( edge.getTargetNode() ).thenReturn( targetId );

        return edge;
    }


    private SearchByEdgeType createSearchByEdge( final Id sourceId, final String type, final UUID maxVersion,
                                                 final Edge last ) {
        return new SimpleSearchByEdgeType( sourceId, type, maxVersion, last );
    }


    private SearchByIdType createSearchByEdgeAndId( final Id sourceId, final String type, final UUID maxVersion,
                                                    final String idType, final Edge last ) {
        return new SimpleSearchByIdType( sourceId, type, maxVersion, idType, last );
    }


    /**
     * Create search criteria
     */
    private SearchByEdgeType createMockSearch( final Id sourceId, final String type, final UUID maxVersion,
                                               final Edge last ) {
        SearchByEdgeType search = mock( SearchByEdgeType.class );

        when( search.getNode() ).thenReturn( sourceId );
        when( search.getType() ).thenReturn( type );
        when( search.getMaxVersion() ).thenReturn( maxVersion );
        when( search.last() ).thenReturn( last );

        return search;
    }
}




