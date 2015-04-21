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

package org.apache.usergrid.corepersistence.index;


import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.usergrid.corepersistence.TestIndexModule;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.test.UseModules;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.GraphManager;
import org.apache.usergrid.persistence.graph.GraphManagerFactory;
import org.apache.usergrid.persistence.index.IndexEdge;
import org.apache.usergrid.persistence.index.SearchEdge;
import org.apache.usergrid.persistence.index.impl.EsRunner;
import org.apache.usergrid.persistence.index.impl.IndexOperationMessage;
import org.apache.usergrid.persistence.index.impl.IndexRequest;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.field.StringField;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.google.inject.Inject;

import rx.Observable;

import static org.apache.usergrid.corepersistence.util.CpNamingUtils.createCollectionEdge;
import static org.apache.usergrid.corepersistence.util.CpNamingUtils.getApplicationScope;
import static org.apache.usergrid.persistence.core.util.IdGenerator.createId;
import static org.junit.Assert.*;


@RunWith(EsRunner.class)
@UseModules({ TestIndexModule.class })
public class IndexServiceTest {

    @Inject
    public IndexService indexService;


    @Inject
    public GraphManagerFactory graphManagerFactory;

    public GraphManager graphManager;

    public ApplicationScope applicationScope;

    @Before
    public void setup(){
        applicationScope = getApplicationScope( UUIDGenerator.newTimeUUID());

        graphManager = graphManagerFactory.createEdgeManager( applicationScope );
    }


    @Test
    public void testSingleIndexFromSource(){
        final Entity entity = new Entity( createId( "test" ), UUIDGenerator.newTimeUUID());
        entity.setField( new StringField( "string", "foo" ) );

        final Edge collectionEdge =  createCollectionEdge( applicationScope.getApplication(), "tests", entity.getId() );

        //write the edge
        graphManager.writeEdge( collectionEdge ).toBlocking().last();


        //index the edge
        final Observable<IndexOperationMessage> indexed = indexService.indexEntity( applicationScope, entity );


        //real users should never call to blocking, we're not sure what we'll get
        final IndexOperationMessage results =  indexed.toBlocking().last();

        final Set<IndexRequest> indexRequests = results.getIndexRequests();

        //ensure our value made it to the index request
        final IndexRequest indexRequest = indexRequests.iterator().next();

        assertNotNull(indexRequest);



//        assertEquals(applicationScope.getApplication(), indexRequest.);
//        assertEquals(collectionEdge.getTimestamp(), edge.getTimestamp());
//        assertEquals(collectionEdge.getType(), edge.getEdgeName());
//        assertEquals( SearchEdge.NodeType.TARGET, edge.getNodeType());
    }


}
