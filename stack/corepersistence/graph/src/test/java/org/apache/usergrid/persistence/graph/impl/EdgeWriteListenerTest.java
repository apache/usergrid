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
package org.apache.usergrid.persistence.graph.impl;


import java.util.Collections;
import java.util.Iterator;
import java.util.UUID;

import org.jukito.UseModules;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.collection.guice.MigrationManagerRule;
import org.apache.usergrid.persistence.core.cassandra.ITRunner;
import org.apache.usergrid.persistence.core.consistency.AsyncProcessor;
import org.apache.usergrid.persistence.core.consistency.AsyncProcessorFactory;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.graph.GraphFig;
import org.apache.usergrid.persistence.graph.GraphManagerFactory;
import org.apache.usergrid.persistence.graph.MarkedEdge;
import org.apache.usergrid.persistence.graph.SearchByEdge;
import org.apache.usergrid.persistence.graph.guice.CommitLogEdgeSerialization;
import org.apache.usergrid.persistence.graph.guice.StorageEdgeSerialization;
import org.apache.usergrid.persistence.graph.guice.TestGraphModule;
import org.apache.usergrid.persistence.graph.impl.stage.EdgeWriteCompact;
import org.apache.usergrid.persistence.graph.impl.stage.EdgeWriteCompactImpl;
import org.apache.usergrid.persistence.graph.serialization.EdgeSerialization;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.google.inject.Inject;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createEdge;
import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createGetByEdge;
import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createId;
import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createSearchByEdge;
import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createSearchByEdgeAndId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@RunWith( ITRunner.class )
@UseModules( { TestGraphModule.class } )
public class EdgeWriteListenerTest {

    private static final Logger log = LoggerFactory.getLogger( NodeDeleteListenerTest.class );

    @Inject
    @Rule
    public MigrationManagerRule migrationManagerRule;


    @Inject
    protected EdgeWriteListener edgeWriteListener;

    @Inject
    @CommitLogEdgeSerialization
    protected EdgeSerialization commitLogEdgeSerialization;


    @Inject
    @StorageEdgeSerialization
    protected EdgeSerialization storageEdgeSerialization;


    @Inject
    protected GraphManagerFactory emf;


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
    public void testWriteIT() throws ConnectionException {

        //write several versions to the commit log

        final Id sourceId = createId( "source" );
        final String edgeType = "test";
        final Id targetId = createId( "target" );


        MarkedEdge edgeV1 = createEdge( sourceId, edgeType, targetId );
        MarkedEdge edgeV2 = createEdge( sourceId, edgeType, targetId );
        MarkedEdge edgeV3 = createEdge( sourceId, edgeType, targetId );

        final UUID timestamp = UUIDGenerator.newTimeUUID();


        commitLogEdgeSerialization.writeEdge( scope, edgeV1, timestamp ).execute();
        commitLogEdgeSerialization.writeEdge( scope, edgeV2, timestamp).execute();
        commitLogEdgeSerialization.writeEdge( scope, edgeV3, timestamp ).execute();

        EdgeWriteEvent edgeWriteEvent = new EdgeWriteEvent( scope, UUIDGenerator.newTimeUUID(), edgeV3 );

        //now perform the listener execution
        Integer returned = edgeWriteListener.receive( edgeWriteEvent ).toBlockingObservable().single();

        assertEquals( 3, returned.intValue() );

        //now validate there's nothing in the commit log.
        long now = System.currentTimeMillis();

        /******
         * Ensure everything is removed from the commit log
         */

        /**
         * Search all versions of the edge
         */

        Iterator<MarkedEdge> edges = commitLogEdgeSerialization
                .getEdgeVersions( scope, createGetByEdge( sourceId, edgeType, targetId, now, null ) );


        /**
         * Search from source
         */
        assertFalse( "No edges exist", edges.hasNext() );

        edges = commitLogEdgeSerialization
                .getEdgesFromSource( scope, createSearchByEdge( sourceId, edgeType, now, null ) );

        assertFalse( "No edges exist", edges.hasNext() );

        edges = commitLogEdgeSerialization.getEdgesFromSourceByTargetType( scope,
                createSearchByEdgeAndId( sourceId, edgeType, now, targetId.getType(), null ) );

        assertFalse( "No edges exist", edges.hasNext() );

        /**
         * Search to target
         */


        edges = commitLogEdgeSerialization
                .getEdgesToTarget( scope, createSearchByEdge( targetId, edgeType, now, null ) );

        assertFalse( "No edges exist", edges.hasNext() );

        edges = commitLogEdgeSerialization.getEdgesToTargetBySourceType( scope,
                createSearchByEdgeAndId( targetId, edgeType, now, sourceId.getType(), null ) );

        assertFalse( "No edges exist", edges.hasNext() );

        /**
         * Ensure all the edges exist in the permanent storage
         */

        /**
         * Search all versions of the edge
         */

        edges = storageEdgeSerialization
                .getEdgeVersions( scope, createGetByEdge( sourceId, edgeType, targetId, now, null ) );


        /**
         * Search from source
         */
        assertEquals( edgeV3, edges.next() );
        assertEquals( edgeV2, edges.next() );
        assertEquals( edgeV1, edges.next() );
        assertFalse( edges.hasNext() );

        edges = storageEdgeSerialization
                .getEdgesFromSource( scope, createSearchByEdge( sourceId, edgeType, now, null ) );

        assertEquals( edgeV3, edges.next() );
        assertEquals( edgeV2, edges.next() );
        assertEquals( edgeV1, edges.next() );
        assertFalse( edges.hasNext() );

        edges = storageEdgeSerialization.getEdgesFromSourceByTargetType( scope,
                createSearchByEdgeAndId( sourceId, edgeType, now, targetId.getType(), null ) );

        assertEquals( edgeV3, edges.next() );
        assertEquals( edgeV2, edges.next() );
        assertEquals( edgeV1, edges.next() );
        assertFalse( edges.hasNext() );

        /**
         * Search to target
         */


        edges = storageEdgeSerialization.getEdgesToTarget( scope, createSearchByEdge( targetId, edgeType, now, null ) );

        assertEquals( edgeV3, edges.next() );
        assertEquals( edgeV2, edges.next() );
        assertEquals( edgeV1, edges.next() );
        assertFalse( edges.hasNext() );

        edges = storageEdgeSerialization.getEdgesToTargetBySourceType( scope,
                createSearchByEdgeAndId( targetId, edgeType, now, sourceId.getType(), null ) );

        assertEquals( edgeV3, edges.next() );
        assertEquals( edgeV2, edges.next() );
        assertEquals( edgeV1, edges.next() );
        assertFalse( edges.hasNext() );
    }


    @Test
    public void testWritePreviousVersionIT() throws ConnectionException {

        //write several versions to the commit log

        final Id sourceId = createId( "source" );
        final String edgeType = "test";
        final Id targetId = createId( "target" );


        MarkedEdge edgeV1 = createEdge( sourceId, edgeType, targetId );
        MarkedEdge edgeV2 = createEdge( sourceId, edgeType, targetId );
        MarkedEdge edgeV3 = createEdge( sourceId, edgeType, targetId );


        final UUID timestamp = UUIDGenerator.newTimeUUID();
        commitLogEdgeSerialization.writeEdge( scope, edgeV1, timestamp ).execute();
        commitLogEdgeSerialization.writeEdge( scope, edgeV2, timestamp ).execute();
        commitLogEdgeSerialization.writeEdge( scope, edgeV3, timestamp ).execute();

        EdgeWriteEvent edgeWriteEvent = new EdgeWriteEvent( scope, UUIDGenerator.newTimeUUID(), edgeV2 );

        //now perform the listener execution, should only clean up to edge v2
        Integer returned = edgeWriteListener.receive( edgeWriteEvent ).toBlockingObservable().single();

        assertEquals( 2, returned.intValue() );

        //now validate there's nothing in the commit log.
        long now = System.currentTimeMillis();

        /******
         * Ensure everything is removed from the commit log
         */

        /**
         * Search all versions of the edge
         */

        Iterator<MarkedEdge> edges = commitLogEdgeSerialization
                .getEdgeVersions( scope, createGetByEdge( sourceId, edgeType, targetId, now, null ) );


        /**
         * Search from source
         */
        assertEquals( edgeV3, edges.next() );
        assertFalse( edges.hasNext() );

        edges = commitLogEdgeSerialization
                .getEdgesFromSource( scope, createSearchByEdge( sourceId, edgeType, now, null ) );

        assertEquals( edgeV3, edges.next() );
        assertFalse( edges.hasNext() );

        edges = commitLogEdgeSerialization.getEdgesFromSourceByTargetType( scope,
                createSearchByEdgeAndId( sourceId, edgeType, now, targetId.getType(), null ) );

        assertEquals( edgeV3, edges.next() );
        assertFalse( edges.hasNext() );

        /**
         * Search to target
         */


        edges = commitLogEdgeSerialization
                .getEdgesToTarget( scope, createSearchByEdge( targetId, edgeType, now, null ) );

        assertEquals( edgeV3, edges.next() );
        assertFalse( edges.hasNext() );

        edges = commitLogEdgeSerialization.getEdgesToTargetBySourceType( scope,
                createSearchByEdgeAndId( targetId, edgeType, now, sourceId.getType(), null ) );

        assertEquals( edgeV3, edges.next() );
        assertFalse( edges.hasNext() );

        /**
         * Ensure all the edges exist in the permanent storage
         */

        /**
         * Search all versions of the edge
         */

        edges = storageEdgeSerialization
                .getEdgeVersions( scope, createGetByEdge( sourceId, edgeType, targetId, now, null ) );


        /**
         * Search from source
         */

        assertEquals( edgeV2, edges.next() );
        assertEquals( edgeV1, edges.next() );
        assertFalse( edges.hasNext() );

        edges = storageEdgeSerialization
                .getEdgesFromSource( scope, createSearchByEdge( sourceId, edgeType, now, null ) );


        assertEquals( edgeV2, edges.next() );
        assertEquals( edgeV1, edges.next() );
        assertFalse( edges.hasNext() );

        edges = storageEdgeSerialization.getEdgesFromSourceByTargetType( scope,
                createSearchByEdgeAndId( sourceId, edgeType, now, targetId.getType(), null ) );


        assertEquals( edgeV2, edges.next() );
        assertEquals( edgeV1, edges.next() );
        assertFalse( edges.hasNext() );

        /**
         * Search to target
         */


        edges = storageEdgeSerialization.getEdgesToTarget( scope, createSearchByEdge( targetId, edgeType, now, null ) );


        assertEquals( edgeV2, edges.next() );
        assertEquals( edgeV1, edges.next() );
        assertFalse( edges.hasNext() );

        edges = storageEdgeSerialization.getEdgesToTargetBySourceType( scope,
                createSearchByEdgeAndId( targetId, edgeType, now, sourceId.getType(), null ) );


        assertEquals( edgeV2, edges.next() );
        assertEquals( edgeV1, edges.next() );
        assertFalse( edges.hasNext() );
    }


    /**
     * When our write to storage fails (for whatever reason) we want to ensure that we never mutate the deletes on the
     * commit log
     */
    @Test
    public void writeFailsCommitLogUnwritten() throws ConnectionException {

        //write several versions to the commit log

        final Id sourceId = createId( "source" );
        final String edgeType = "test";
        final Id targetId = createId( "target" );


        MarkedEdge edgeV1 = createEdge( sourceId, edgeType, targetId );

        EdgeSerialization commitLog = mock( EdgeSerialization.class );
        EdgeSerialization storage = mock( EdgeSerialization.class );

        AsyncProcessorFactory edgeProcessor = mock( AsyncProcessorFactory.class );

        AsyncProcessor<EdgeWriteEvent> processor = mock(AsyncProcessor.class);

        when(edgeProcessor.getProcessor( EdgeWriteEvent.class )).thenReturn( processor );


        EdgeWriteEvent edgeWriteEvent = new EdgeWriteEvent( scope, UUIDGenerator.newTimeUUID(), edgeV1 );

        Keyspace keyspace = mock( Keyspace.class );


        EdgeWriteCompact compact = new EdgeWriteCompactImpl( commitLog, storage, keyspace, graphFig );



        //now perform the listener execution, should only clean up to edge v2
        EdgeWriteListener listener = new EdgeWriteListener( compact, edgeProcessor );


        /**
         * Mock up returning a single edge
         */
        when( commitLog.getEdgeVersions( same(scope), any( SearchByEdge.class ) ) ).thenReturn( Collections
                .singletonList( createEdge( edgeV1.getSourceNode(), edgeV1.getType(), edgeV1.getTargetNode(),
                        edgeV1.getTimestamp() ) ).iterator() );


        /**
         * Mock up returning a fake batch when delete is called
         */
        MutationBatch commitLogBatch = mock( MutationBatch.class );
        MutationBatch storageBatch = mock( MutationBatch.class );


        when( keyspace.prepareMutationBatch() ).thenReturn( commitLogBatch );
        when( keyspace.prepareMutationBatch() ).thenReturn( storageBatch );


        RuntimeException exception = new RuntimeException( "Something nasty happened when mutating" );


       when( storageBatch.execute() ).thenThrow( exception );



        try {
           listener.receive( edgeWriteEvent ).toBlockingObservable().single();
            fail( "I should have thrown an exception" );
        }
        catch ( RuntimeException re ) {
            assertSame(exception, re);
        }



        //verify we never remove from the commit log
        verify(commitLogBatch, never()).execute();

    }
}
