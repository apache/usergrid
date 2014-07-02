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
package org.apache.usergrid.rest.applications.collection.activities;


import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import org.junit.Rule;
import org.junit.Test;
import org.apache.usergrid.rest.AbstractRestIT;
import org.apache.usergrid.rest.TestContextSetup;
import org.apache.usergrid.rest.test.resource.CustomCollection;

import org.apache.commons.lang.ArrayUtils;

import static org.junit.Assert.assertEquals;
import static org.apache.usergrid.utils.MapUtils.hashMap;


/**
 * // TODO: Document this
 *
 * @author ApigeeCorporation
 * @since 4.0
 */
public class PagingEntitiesTest extends AbstractRestIT {

    @Rule
    public TestContextSetup context = new TestContextSetup( this );


    @Test //USERGRID-266
    public void pageThroughConnectedEntities() throws IOException {

        CustomCollection activities = context.collection( "activities" );

        long created = 0;
        int maxSize = 1500;
        long[] verifyCreated = new long[maxSize];
        Map actor = hashMap( "displayName", "Erin" );
        Map props = new HashMap();


        props.put( "actor", actor );
        props.put( "verb", "go" );

        for ( int i = 0; i < maxSize; i++ ) {

            props.put( "ordinal", i );
            JsonNode activity = activities.create( props );
            verifyCreated[i] = activity.findValue( "created" ).longValue();
            if ( i == 0 ) {
                created = activity.findValue( "created" ).longValue();
            }
        }
        ArrayUtils.reverse( verifyCreated );
        String query = "select * where created >= " + created;


        JsonNode node = activities.query( query, "limit", "2" ); //activities.query(query,"");
        int index = 0;
        while ( node.get( "entities" ).get( "created" ) != null ) {
            assertEquals( 2, node.get( "entities" ).size() );

            if ( node.get( "cursor" ) != null ) {
                node = activities.query( query, "cursor", node.get( "cursor" ).toString() );
            }

            else {
                break;
            }
        }
    }


    @Test //USERGRID-1253
    public void pagingQueryReturnCorrectResults() throws Exception {

        CustomCollection activities = context.collection( "activities" );

        long created = 0;
        int maxSize = 23;
        long[] verifyCreated = new long[maxSize];
        Map actor = hashMap( "displayName", "Erin" );
        Map props = new HashMap();

        props.put( "actor", actor );
        props.put( "content", "bragh" );

        for ( int i = 0; i < maxSize; i++ ) {

            if ( i > 17 && i < 23 ) {
                props.put( "verb", "stop" );
            }
            else {
                props.put( "verb", "go" );
            }
            props.put( "ordinal", i );
            JsonNode activity = activities.create( props );
            verifyCreated[i] = activity.findValue( "created" ).longValue();
            if ( i == 18 ) {
                created = activity.findValue( "created" ).longValue();
            }
        }

        String query = "select * where created >= " + created + " or verb = 'stop'";

        JsonNode node = activities.withQuery( query ).get();

        for ( int index = 0; index < 5; index++ ) {
            assertEquals( verifyCreated[maxSize - 1 - index],
                    node.get( "entities" ).get( index ).get( "created" ).longValue() );
        }

        int totalEntitiesContained = activities.countEntities( query );

        assertEquals( 5, totalEntitiesContained );
    }
}
