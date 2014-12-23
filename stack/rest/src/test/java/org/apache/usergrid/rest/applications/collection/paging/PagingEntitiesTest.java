/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.usergrid.rest.applications.collection.paging;


import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;

import org.apache.usergrid.persistence.index.utils.UUIDUtils;
import org.apache.usergrid.rest.test.resource2point0.AbstractRestIT;
import org.apache.usergrid.rest.test.resource2point0.model.Collection;
import org.apache.usergrid.rest.test.resource2point0.model.Entity;
import org.apache.usergrid.rest.test.resource2point0.model.QueryParameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;


/**
 * Holds tests that handles creation of tons of entities and then paging through them
 * using a cursor.
 */
public class PagingEntitiesTest extends AbstractRestIT {


    /**
     * Tests that we can create 100 entities and then get them back in the order that they were created 10 at a time and
     * retrieving the next 10 with a cursor.
     */
    @Test
    public void pagingEntities() throws IOException {
        long created = 0;
        int maxSize = 100;
        Map<String, Object> entityPayload = new HashMap<String, Object>();
        String collectionName = "merp" + UUIDUtils.newTimeUUID();

        for ( created = 0; created < maxSize; created++ ) {

            entityPayload.put( "name", "value" + created );
            Entity entity = new Entity( entityPayload );
            this.app().collection( collectionName ).post( entity );
        }

        this.refreshIndex();

        Collection sandboxCollection = this.app().collection( collectionName ).get();
        assertNotNull( sandboxCollection );

        created = 0;
        for ( int i = 0; i < 10; i++ ) {
            while ( sandboxCollection.hasNext() ) {
                Entity returnedEntity = sandboxCollection.next();
                assertEquals( "value" + created++, returnedEntity.get( "name" ) );
            }
            sandboxCollection = this.app().collection( collectionName ).getNextPage( sandboxCollection, true );
        }
    }


    @Ignore( "This test only checks activities and if we can retrieve them using a limit"
            + "Doesn't check to make sure we can page through entities that are connected using connections." )
    @Test //USERGRID-266
    public void pageThroughConnectedEntities() throws IOException {

        //        CustomCollection activities = context.collection( "activities" );
        //
        //        long created = 0;
        //        int maxSize = 100;
        //        long[] verifyCreated = new long[maxSize];
        //        Map actor = hashMap( "displayName", "Erin" );
        //        Map props = new HashMap();
        //
        //
        //        props.put( "actor", actor );
        //        props.put( "verb", "go" );
        //
        //        for ( int i = 0; i < maxSize; i++ ) {
        //
        //            props.put( "ordinal", i );
        //            JsonNode activity = activities.create( props );
        //            verifyCreated[i] = activity.findValue( "created" ).longValue();
        //            if ( i == 0 ) {
        //                created = activity.findValue( "created" ).longValue();
        //            }
        //        }
        //        ArrayUtils.reverse( verifyCreated );
        //
        //        refreshIndex( context.getOrgName(), context.getAppName() );
        //
        //        String query = "select * where created >= " + created;
        //
        //
        //        JsonNode node = activities.query( query, "limit", "2" ); //activities.query(query,"");
        //        int index = 0;
        //        while ( node.get( "entities" ).get( "created" ) != null ) {
        //            assertEquals( 2, node.get( "entities" ).size() );
        //
        //            if ( node.get( "cursor" ) != null ) {
        //                node = activities.query( query, "cursor", node.get( "cursor" ).toString() );
        //            }
        //
        //            else {
        //                break;
        //            }
        //        }
    }


    /**
     * Checks to make sure the query gives us the correct result set.
     * @throws Exception
     */
    @Test //USERGRID-1253
    public void pagingQueryReturnCorrectResults() throws Exception {

        long created = 0;
        int maxSize = 20;
        Map<String, Object> entityPayload = new HashMap<String, Object>();
        List<Entity> entityPayloadVerifier = new LinkedList<>(  );
        long createdTimestamp = 0;
        String collectionName = "merp" + createdTimestamp;

        for ( created = 0; created < maxSize; created++ ) {

            if ( created >= 15 && created < 20 ) {

                entityPayload.put( "verb", "stop" );
            }
            else {
                entityPayload.put( "verb", "go" );
            }

            entityPayload.put( "name", "value" + created );
            Entity entity = new Entity( entityPayload );
            entityPayloadVerifier.add( entity );
            if(created == 15)
                createdTimestamp = System.currentTimeMillis();
            this.app().collection( collectionName ).post( entity );
        }

        this.refreshIndex();

        String query = "select * where created >= " + createdTimestamp + " or verb = 'stop'";

        QueryParameters queryParameters = new QueryParameters();
        queryParameters.setQuery( query );
        Collection queryCollection = this.app().collection( collectionName ).get( queryParameters );

        assertNotNull( queryCollection );
        assertNull( queryCollection.getCursor() );
        assertEquals( 5, queryCollection.getResponse().getEntities().size() );

        for(int i = 15;i<maxSize;i++){
            Entity correctEntity = entityPayloadVerifier.get( i );
            Entity returnedEntity = queryCollection.next();
            assertEquals( correctEntity.get( "name" ), returnedEntity.get( "name" ) );
            assertEquals( correctEntity.get( "verb" ), returnedEntity.get( "verb" ) );

        }
    }
}



