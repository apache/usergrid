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

package org.apache.usergrid.persistence.index.impl;


import java.util.UUID;

import org.junit.Test;

import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.index.CandidateResult;
import org.apache.usergrid.persistence.index.SearchEdge;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import static org.apache.usergrid.persistence.index.impl.IndexingUtils.parseIndexDocId;
import static org.junit.Assert.assertEquals;


public class IndexingUtilsTest {

    @Test
    public void testCreateContextName() throws Exception {

        final ApplicationScopeImpl applicationScope = new ApplicationScopeImpl( new SimpleId( "application" ) );

        final SearchEdgeImpl searchEdge =
            new SearchEdgeImpl( new SimpleId( "source" ), "users", SearchEdge.NodeType.TARGET );

        final String output = IndexingUtils.createContextName( applicationScope, searchEdge );


        final String expected =
            "appId(" + applicationScope.getApplication().getUuid() + ",application).nodeId(" + searchEdge.getNodeId()
                                                                                                         .getUuid()
                + "," + searchEdge.getNodeId().getType() + ").edgeName(users)";


        assertEquals( output, expected );
    }


    @Test
    public void testDocumentId() {

        final ApplicationScopeImpl applicationScope = new ApplicationScopeImpl( new SimpleId( "application" ) );

        final Id id = new SimpleId( "id" );
        final UUID version = UUIDGenerator.newTimeUUID();

        final SearchEdgeImpl searchEdge =
            new SearchEdgeImpl( new SimpleId( "source" ), "users", SearchEdge.NodeType.TARGET );

        final String output = IndexingUtils.createIndexDocId( applicationScope, id, version, searchEdge );


        final String expected =
            "appId(" + applicationScope.getApplication().getUuid() + ",application).entityId(" + id.getUuid() + "," + id
                .getType() + ").version(" + version + ").nodeId(" + searchEdge.getNodeId().getUuid() + "," + searchEdge
                .getNodeId().getType() + ").edgeName(users).nodeType(TARGET)";


        assertEquals( output, expected );


        //now parse it

        final CandidateResult parsedId = parseIndexDocId( output );

        assertEquals(version, parsedId.getVersion());
        assertEquals(id, parsedId.getId());
    }


    @Test
    public void testDocumentIdPipes() {

        final ApplicationScopeImpl applicationScope = new ApplicationScopeImpl( new SimpleId( "application" ) );

        final Id id = new SimpleId( "id" );
        final UUID version = UUIDGenerator.newTimeUUID();

        final SearchEdgeImpl searchEdge =
            new SearchEdgeImpl( new SimpleId( "source" ), "zzzcollzzz|users", SearchEdge.NodeType.TARGET );

        final String output = IndexingUtils.createIndexDocId( applicationScope, id, version, searchEdge );


        final String expected =
            "appId(" + applicationScope.getApplication().getUuid() + ",application).entityId(" + id.getUuid() + "," + id
                .getType() + ").version(" + version + ").nodeId(" + searchEdge.getNodeId().getUuid() + "," + searchEdge
                .getNodeId().getType() + ").edgeName(zzzcollzzz|users).nodeType(TARGET)";


        assertEquals( output, expected );


        //now parse it

        final CandidateResult parsedId = parseIndexDocId( output );

        assertEquals(version, parsedId.getVersion());
        assertEquals(id, parsedId.getId());
    }


    @Test
    public void testEntityType() {

        final ApplicationScopeImpl applicationScope = new ApplicationScopeImpl( new SimpleId( "application" ) );

        final Id id = new SimpleId( "id" );

        final String output = IndexingUtils.getType( applicationScope, id );


        final String expected =
            "appId(" + applicationScope.getApplication().getUuid() + ",application).entityType(" + id.getType() + ")";


        assertEquals( output, expected );
    }
}
