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


import org.junit.Before;
import org.junit.Test;

import org.apache.usergrid.persistence.graph.serialization.EdgeSerialization;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createEdge;
import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;


public class MergedEdgeReaderImplComparatorTest {

    private EdgeSerialization edgeSerialization;


    @Before
    public void setup() {
        edgeSerialization = mock( EdgeSerialization.class );
    }


    @Test
    public void testSameSourceEdges() {


        MergedEdgeReaderImpl.SourceAwareMarkedEdge first =
                new MergedEdgeReaderImpl.SourceAwareMarkedEdge( createEdge( "source", "edge", "target" ),
                        edgeSerialization );


        int compare = new MergedEdgeReaderImpl.SourceEdgeComparator( edgeSerialization ).compare( first, first );

        assertEquals( 0, compare );
    }


    @Test
    public void testDifferentTargetSourceEdges() {

        MergedEdgeReaderImpl.SourceAwareMarkedEdge first =
                new MergedEdgeReaderImpl.SourceAwareMarkedEdge( createEdge( "source", "edge", "target" ),
                        edgeSerialization );
        //same as first, just with large target node
        MergedEdgeReaderImpl.SourceAwareMarkedEdge second = new MergedEdgeReaderImpl.SourceAwareMarkedEdge(
                createEdge( first.edge.getSourceNode(), first.edge.getType(), createId( "target" ),
                        first.edge.getVersion() ), edgeSerialization );


        int compare = new MergedEdgeReaderImpl.SourceEdgeComparator( edgeSerialization ).compare( first, second );

        assertTrue( "Second node has new target", compare < 0 );
    }


    @Test
    public void testDifferentVersionSourceEdges() {

        MergedEdgeReaderImpl.SourceAwareMarkedEdge first =
                new MergedEdgeReaderImpl.SourceAwareMarkedEdge( createEdge( "source", "edge", "target" ),
                        edgeSerialization );
        //same as first, just with large target node
        MergedEdgeReaderImpl.SourceAwareMarkedEdge second = new MergedEdgeReaderImpl.SourceAwareMarkedEdge(
                createEdge( first.edge.getSourceNode(), first.edge.getType(), first.edge.getTargetNode(),
                        UUIDGenerator.newTimeUUID() ), edgeSerialization );


        int compare = new MergedEdgeReaderImpl.SourceEdgeComparator( edgeSerialization ).compare( first, second );

        assertTrue( "Second node has greater version", compare < 0 );
    }


    @Test
    public void testDifferentSourceIteratorsSourceEdges() {

        EdgeSerialization other = mock( EdgeSerialization.class );

        MergedEdgeReaderImpl.SourceAwareMarkedEdge first =
                new MergedEdgeReaderImpl.SourceAwareMarkedEdge( createEdge( "source", "edge", "target" ),
                        edgeSerialization );
        //same as first, just with large target node
        MergedEdgeReaderImpl.SourceAwareMarkedEdge second = new MergedEdgeReaderImpl.SourceAwareMarkedEdge(
                createEdge( first.edge.getSourceNode(), first.edge.getType(), first.edge.getTargetNode(),
                        first.edge.getVersion() ), other );

        MergedEdgeReaderImpl.SourceEdgeComparator comparator =
                new MergedEdgeReaderImpl.SourceEdgeComparator( edgeSerialization );

        int compare = comparator.compare( first, second );

        assertTrue( "Edges are equal, comes from higher weighted iterator", compare > 0 );

        compare = comparator.compare( second, first );

        assertTrue( "Edges are equal, comes from less weighted iterator", compare < 0 );
    }


    /**
     * Test when reading from targets
     */

    @Test
    public void testSameTargetEdges() {

        MergedEdgeReaderImpl.SourceAwareMarkedEdge first =
                new MergedEdgeReaderImpl.SourceAwareMarkedEdge( createEdge( "source", "edge", "target" ),
                        edgeSerialization );

        int compare = new MergedEdgeReaderImpl.TargetEdgeComparator( edgeSerialization ).compare( first, first );

        assertEquals( 0, compare );
    }


    @Test
    public void testDifferentTargetTargetEdges() {

        MergedEdgeReaderImpl.SourceAwareMarkedEdge first =
                new MergedEdgeReaderImpl.SourceAwareMarkedEdge( createEdge( "source", "edge", "target" ),
                        edgeSerialization );
        //same as first, just with large target node
        MergedEdgeReaderImpl.SourceAwareMarkedEdge second = new MergedEdgeReaderImpl.SourceAwareMarkedEdge(
                createEdge( createId( "source" ), first.edge.getType(), first.edge.getTargetNode(),
                        first.edge.getVersion() ), edgeSerialization );
        ;


        int compare = new MergedEdgeReaderImpl.TargetEdgeComparator( edgeSerialization ).compare( first, second );

        assertTrue( "Second node has new target", compare < 0 );
    }


    @Test
    public void testDifferentVersionTargetEdges() {

        MergedEdgeReaderImpl.SourceAwareMarkedEdge first =
                new MergedEdgeReaderImpl.SourceAwareMarkedEdge( createEdge( "source", "edge", "target" ),
                        edgeSerialization );
        //same as first, just with large target node
        MergedEdgeReaderImpl.SourceAwareMarkedEdge second = new MergedEdgeReaderImpl.SourceAwareMarkedEdge(
                createEdge( first.edge.getSourceNode(), first.edge.getType(), first.edge.getTargetNode(),
                        UUIDGenerator.newTimeUUID() ), edgeSerialization );


        int compare = new MergedEdgeReaderImpl.TargetEdgeComparator( edgeSerialization ).compare( first, second );

        assertTrue( "Second node has greater version", compare < 0 );
    }


    @Test
    public void testDifferentDeleteTargetEdges() {

        EdgeSerialization other = mock( EdgeSerialization.class );

        MergedEdgeReaderImpl.SourceAwareMarkedEdge first =
                new MergedEdgeReaderImpl.SourceAwareMarkedEdge( createEdge( "source", "edge", "target" ),
                        edgeSerialization );

        MergedEdgeReaderImpl.SourceAwareMarkedEdge second = new MergedEdgeReaderImpl.SourceAwareMarkedEdge(
                createEdge( first.edge.getSourceNode(), first.edge.getType(), first.edge.getTargetNode(),
                        first.edge.getVersion() ), other );

        MergedEdgeReaderImpl.TargetEdgeComparator comparator =
                new MergedEdgeReaderImpl.TargetEdgeComparator( edgeSerialization );

        int compare = comparator.compare( first, second );

        assertTrue( "Second node is deleted, therefore greater", compare > 0 );

        compare = comparator.compare( second, first );

        assertTrue( "Second node is deleted, therefore greater", compare < 0 );
    }


    @Test
    public void testDifferentVersion() {

        MergedEdgeReaderImpl.SourceAwareMarkedEdge first =
                new MergedEdgeReaderImpl.SourceAwareMarkedEdge( createEdge( "source", "edge", "target" ),
                        edgeSerialization );
        //same as first, just with large target node
        MergedEdgeReaderImpl.SourceAwareMarkedEdge second = new MergedEdgeReaderImpl.SourceAwareMarkedEdge(
                createEdge( first.edge.getSourceNode(), first.edge.getType(), first.edge.getTargetNode(),
                        UUIDGenerator.newTimeUUID() ), edgeSerialization );

        int compare = new MergedEdgeReaderImpl.EdgeVersionComparator( edgeSerialization ).compare( first, second );

        assertTrue( "Second node has newer version therefore greater", compare < 0 );
    }


    @Test
    public void testDifferentMark() {

        EdgeSerialization otherSerialization = mock( EdgeSerialization.class );

        MergedEdgeReaderImpl.SourceAwareMarkedEdge first =
                new MergedEdgeReaderImpl.SourceAwareMarkedEdge( createEdge( "source", "edge", "target" ),
                        edgeSerialization );
        //same as first, just with large target node
        MergedEdgeReaderImpl.SourceAwareMarkedEdge second = new MergedEdgeReaderImpl.SourceAwareMarkedEdge(
                createEdge( first.edge.getSourceNode(), first.edge.getType(), first.edge.getTargetNode(),
                        first.edge.getVersion() ), otherSerialization );

        MergedEdgeReaderImpl.EdgeVersionComparator comparator =
                new MergedEdgeReaderImpl.EdgeVersionComparator( edgeSerialization );

        int compare = comparator.compare( first, second );

        assertTrue( "Edges are equal, comes from higher weighted iterator", compare > 0 );

        compare = comparator.compare( second, first );

        assertTrue( "Edges are equal, comes from less weighted iterator", compare < 0 );
    }
}
