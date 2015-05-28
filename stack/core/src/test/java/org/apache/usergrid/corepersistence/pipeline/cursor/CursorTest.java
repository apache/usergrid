/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.usergrid.corepersistence.pipeline.cursor;




import org.junit.Test;

import org.apache.usergrid.corepersistence.pipeline.read.EdgePath;
import org.apache.usergrid.corepersistence.pipeline.read.search.ElasticsearchCursorSerializer;
import org.apache.usergrid.corepersistence.pipeline.read.traverse.EdgeCursorSerializer;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.impl.SimpleEdge;

import com.google.common.base.Optional;

import static org.apache.usergrid.persistence.core.util.IdGenerator.createId;
import static org.junit.Assert.*;


public class CursorTest {

    @Test
    public void testCursors(){







        //test encoding edge

        final Edge edge1 = new SimpleEdge( createId("source1"), "edgeType1",  createId("target1"), 100  );


        final Edge edge2 = new SimpleEdge( createId("source2"), "edgeType2",  createId("target2"), 110  );



        final Integer query1 = 10;

        final Integer query2 = 20;



        final EdgePath<Integer> filter3Path = new EdgePath<>( 3, query2, ElasticsearchCursorSerializer.INSTANCE, Optional.absent() );

        final EdgePath<Edge> filter2Path = new EdgePath<Edge>(2, edge2, EdgeCursorSerializer.INSTANCE, Optional.of( filter3Path ));

        final EdgePath<Integer> filter1Path = new EdgePath<>( 1, query1, ElasticsearchCursorSerializer.INSTANCE, Optional.of(filter2Path) );

        final EdgePath<Edge> filter0Path = new EdgePath<>( 0, edge1, EdgeCursorSerializer.INSTANCE, Optional.of( filter1Path ) );



        ResponseCursor responseCursor = new ResponseCursor( Optional.of(filter0Path) );

        final Optional<String> cursor = responseCursor.encodeAsString();



        //now parse it

        final RequestCursor requestCursor = new RequestCursor(  cursor  );

        //get everything else out.  We reversed the order for because we can, order shouldn't matter.




        final Integer parsedQuery2 = requestCursor.getCursor( 3, ElasticsearchCursorSerializer.INSTANCE );

        assertEquals(query2, parsedQuery2);

        final Edge parsedEdge2 = requestCursor.getCursor( 2, EdgeCursorSerializer.INSTANCE );

        assertEquals( edge2, parsedEdge2 );

        final Integer parsedQuery1 = requestCursor.getCursor( 1, ElasticsearchCursorSerializer.INSTANCE );

        assertEquals( query1, parsedQuery1 );


        final Edge parsedEdge1 = requestCursor.getCursor( 0, EdgeCursorSerializer.INSTANCE );

        assertEquals(edge1, parsedEdge1);

    }

}
