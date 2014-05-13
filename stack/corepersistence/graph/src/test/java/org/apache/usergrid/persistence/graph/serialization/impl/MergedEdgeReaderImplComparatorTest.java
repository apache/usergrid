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
package org.apache.usergrid.persistence.graph.serialization.impl;


import org.junit.Test;

import org.apache.usergrid.persistence.graph.MarkedEdge;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createEdge;
import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class MergedEdgeReaderImplComparatorTest {

    @Test
    public void testSameSourceEdges() {

        MarkedEdge first = createEdge( "source", "edge", "target" );

        int compare = MergedEdgeReaderImpl.SourceEdgeComparator.INSTANCE.compare( first, first );

        assertEquals( 0, compare );
    }


    @Test
    public void testDifferentTargetSourceEdges() {

        MarkedEdge first = createEdge( "source", "edge", "target" );
        //same as first, just with large target node
        MarkedEdge second =
                createEdge( first.getSourceNode(), first.getType(), createId( "target" ), first.getVersion() );


        int compare = MergedEdgeReaderImpl.SourceEdgeComparator.INSTANCE.compare( first, second );

        assertTrue( "Second node has new target", compare < 0 );
    }


    @Test
    public void testDifferentVersionSourceEdges() {

        MarkedEdge first = createEdge( "source", "edge", "target" );
        //same as first, just with large target node
        MarkedEdge second = createEdge( first.getSourceNode(), first.getType(), first.getTargetNode(),
                UUIDGenerator.newTimeUUID() );


        int compare = MergedEdgeReaderImpl.SourceEdgeComparator.INSTANCE.compare( first, second );

        assertTrue( "Second node has greater version", compare < 0 );
    }


    @Test
    public void testDifferentDeleteSourceEdges() {

        MarkedEdge first = createEdge( "source", "edge", "target" );
        //same as first, just with large target node
        MarkedEdge second =
                createEdge( first.getSourceNode(), first.getType(), first.getTargetNode(), first.getVersion(), true );

        int compare = MergedEdgeReaderImpl.SourceEdgeComparator.INSTANCE.compare( first, second );

        assertTrue( "Second node is deleted, therefore greater", compare < 0 );
    }


    /**
     * Test when reading from targets
     */

    @Test
    public void testSameTargetEdges() {

        MarkedEdge first = createEdge( "source", "edge", "target" );

        int compare = MergedEdgeReaderImpl.TargetEdgeComparator.INSTANCE.compare( first, first );

        assertEquals( 0, compare );
    }


    @Test
    public void testDifferentTargetTargetEdges() {

        MarkedEdge first = createEdge( "source", "edge", "target" );
        //same as first, just with large target node
        MarkedEdge second =
                createEdge( createId( "source" ), first.getType(), first.getTargetNode(), first.getVersion() );


        int compare = MergedEdgeReaderImpl.TargetEdgeComparator.INSTANCE.compare( first, second );

        assertTrue( "Second node has new target", compare < 0 );
    }


    @Test
    public void testDifferentVersionTargetEdges() {

        MarkedEdge first = createEdge( "source", "edge", "target" );
        //same as first, just with large target node
        MarkedEdge second = createEdge( first.getSourceNode(), first.getType(), first.getTargetNode(),
                UUIDGenerator.newTimeUUID() );


        int compare = MergedEdgeReaderImpl.TargetEdgeComparator.INSTANCE.compare( first, second );

        assertTrue( "Second node has greater version", compare < 0 );
    }


    @Test
    public void testDifferentDeleteTargetEdges() {

        MarkedEdge first = createEdge( "source", "edge", "target" );
        //same as first, just with large target node
        MarkedEdge second =
                createEdge( first.getSourceNode(), first.getType(), first.getTargetNode(), first.getVersion(), true );

        int compare = MergedEdgeReaderImpl.TargetEdgeComparator.INSTANCE.compare( first, second );

        assertTrue( "Second node is deleted, therefore greater", compare < 0 );
    }


    @Test
    public void testDifferentVersion() {

        MarkedEdge first = createEdge( "source", "edge", "target" );
        //same as first, just with large target node
        MarkedEdge second = createEdge( first.getSourceNode(), first.getType(), first.getTargetNode(),
                UUIDGenerator.newTimeUUID() );

        int compare = MergedEdgeReaderImpl.EdgeVersionComparator.INSTANCE.compare( first, second );

        assertTrue( "Second node has newer version therefore greater", compare < 0 );
    }


    @Test
    public void testDifferentMark() {

        MarkedEdge first = createEdge( "source", "edge", "target" );
        //same as first, just with large target node
        MarkedEdge second =
                createEdge( first.getSourceNode(), first.getType(), first.getTargetNode(), first.getVersion(), true );

        int compare = MergedEdgeReaderImpl.EdgeVersionComparator.INSTANCE.compare( first, second );

        assertTrue( "Second node is deleted, therefore greater", compare < 0 );
    }
}
