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


import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import org.junit.Rule;
import org.junit.Test;

import org.apache.usergrid.persistence.index.utils.UUIDUtils;
import org.apache.usergrid.rest.TestContextSetup;
import org.apache.usergrid.rest.test.resource.CustomCollection;
import org.apache.usergrid.rest.test.resource2point0.AbstractRestIT;
import org.apache.usergrid.rest.test.resource2point0.model.Collection;
import org.apache.usergrid.rest.test.resource2point0.model.Entity;

import org.apache.commons.lang.ArrayUtils;

import static org.junit.Assert.assertEquals;
import static org.apache.usergrid.utils.MapUtils.hashMap;
import static org.junit.Assert.assertNotNull;


/**
 * // TODO: Document this
 *
 * @author ApigeeCorporation
 * @since 4.0
 */
public class PagingEntitiesTest extends AbstractRestIT {


    @Test //USERGRID-266
    public void pageThroughConnectedEntities() throws IOException {


        long created = 0;
        int maxSize = 100;
        Map<String, Object> entityPayload = new HashMap<String,Object>(  );
        String collectionName = "merp"+ UUIDUtils.newTimeUUID();

        for( created = 0; created < maxSize; created++) {

            entityPayload.put( "name","value"+created );
            Entity entity = new Entity(entityPayload );
            this.app().collection( collectionName).post( entity );

        }

        this.refreshIndex();

        Collection sandboxCollection = this.app().collection( collectionName).get();
        assertNotNull( sandboxCollection );

        created = 0;
        for(int i = 0; i< 10; i++) {
            while ( sandboxCollection.hasNext() ) {
                Entity returnedEntity = sandboxCollection.next();
                assertEquals( "value" + created++, returnedEntity.get( "name" ) );
            }
            sandboxCollection = this.app().collection( collectionName ).getNextPage( sandboxCollection, true );
        }



    }

//
//    @Test //USERGRID-1253
//    public void pagingQueryReturnCorrectResults() throws Exception {
//
//        CustomCollection activities = context.customCollection( "activities" );
//
//        long created = 0;
//        int maxSize = 23;
//        long[] verifyCreated = new long[maxSize];
//        Map actor = hashMap( "displayName", "Erin" );
//        Map props = new HashMap();
//
//        props.put( "actor", actor );
//        props.put( "content", "bragh" );
//
//        for ( int i = 0; i < maxSize; i++ ) {
//
//            if ( i > 17 && i < 23 ) {
//                props.put( "verb", "stop" );
//            }
//            else {
//                props.put( "verb", "go" );
//            }
//            props.put( "ordinal", i );
//            JsonNode activity = activities.create( props );
//            verifyCreated[i] = activity.findValue( "created" ).longValue();
//            if ( i == 18 ) {
//                created = activity.findValue( "created" ).longValue();
//            }
//        }
//
//        refreshIndex(context.getOrgName(), context.getAppName());
//
//        String query = "select * where created >= " + created + " or verb = 'stop'";
//
//        JsonNode node = activities.withQuery( query ).get();
//
//        for ( int index = 0; index < 5; index++ ) {
//            assertEquals( verifyCreated[maxSize - 1 - index],
//                    node.get( "entities" ).get( index ).get( "created" ).longValue() );
//        }
//
//        int totalEntitiesContained = activities.countEntities( query );
//
//        assertEquals( 5, totalEntitiesContained );
//    }
}
