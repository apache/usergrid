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


import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.usergrid.cassandra.Concurrent;
import org.apache.usergrid.java.client.Client.Query;
import org.apache.usergrid.java.client.entities.Entity;
import org.apache.usergrid.java.client.entities.User;
import org.apache.usergrid.java.client.response.ApiResponse;
import org.apache.usergrid.rest.AbstractRestIT;
import org.apache.usergrid.utils.UUIDUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/** @author tnine */
@Concurrent()
public class ActivityResourceIT extends AbstractRestIT {
    private static Logger log = LoggerFactory.getLogger( ActivityResourceIT.class );

    private static final String GROUP = "testGroup";

    private static final String USER = "edanuff";

    private static boolean groupCreated = false;


    public ActivityResourceIT() throws Exception {

    }


    @Before
    public void setupGroup() {
        if ( groupCreated ) {
            return;
        }

        client.createGroup( GROUP );

        refreshIndex("test-organization", "test-app");

        groupCreated = true;
    }


    @Test
    public void postNullActivityToGroup() {

        boolean fail = false;
        try {
            ApiResponse groupActivity = client.postGroupActivity( GROUP, null );
            fail = (groupActivity.getError() != null);
            
        }
        catch ( Exception e ) {
            fail = true;
        }
        assertTrue( fail );
    }


    @Test
    public void postGroupActivity() {

        // don't populate the user, it will use the currently authenticated user.

        String activityTitle = "testTitle" + UUIDUtils.newTimeUUID();
        String activityDesc = "testActivity" + UUIDUtils.newTimeUUID();

        client.postGroupActivity( GROUP, "POST", 
            activityTitle, activityDesc, "testCategory", null, null, null, null, null );

        refreshIndex("test-organization", "test-app");

        Query results = client.queryActivityFeedForGroup( GROUP );

        ApiResponse response = results.getResponse();

        Entity result = response.getEntities().get( 0 );

        assertEquals( "POST", result.getProperties().get( "verb" ).asText() );
        assertEquals( activityTitle, result.getProperties().get( "title" ).asText() );
        assertEquals( activityDesc, result.getProperties().get( "content" ).asText() );

        // now pull the activity directly, we should find it

        results = client.queryActivity();

        response = results.getResponse();

        result = response.getEntities().get( 0 );

        assertEquals( "POST", result.getProperties().get( "verb" ).asText() );
        assertEquals( activityTitle, result.getProperties().get( "title" ).asText() );
        assertEquals( activityDesc, result.getProperties().get( "content" ).asText() );
    }


    @Test
    public void postUserActivity() {

        // don't populate the user, it will use the currently authenticated
        // user.

        User current = client.getLoggedInUser();

        String activityTitle = "testTitle" + UUIDUtils.newTimeUUID();
        String activityDesc = "testActivity" + UUIDUtils.newTimeUUID();

        client.postUserActivity( "POST", activityTitle, activityDesc, "testCategory", current, null, null, null, null );

        refreshIndex("test-organization", "test-app");

        Query results = client.queryActivityFeedForUser( USER );

        ApiResponse response = results.getResponse();

        Entity result = response.getEntities().get( 0 );

        assertEquals( "POST", result.getProperties().get( "verb" ).asText() );
        assertEquals( activityTitle, result.getProperties().get( "title" ).asText() );
        assertEquals( activityDesc, result.getProperties().get( "content" ).asText() );
        assertEquals( current.getUuid().toString(), result.getProperties().get( "actor" ).get( "uuid" ).asText() );

        // now pull the activity directly, we should find it

        results = client.queryActivity();

        response = results.getResponse();

        result = response.getEntities().get( 0 );

        assertEquals( "POST", result.getProperties().get( "verb" ).asText() );
        assertEquals( activityTitle, result.getProperties().get( "title" ).asText() );
        assertEquals( activityDesc, result.getProperties().get( "content" ).asText() );
    }


    @Test
    public void postActivity() {

        // don't populate the user, it will use the currently authenticated
        // user.

        User current = client.getLoggedInUser();

        String activityTitle = "testTitle" + UUIDUtils.newTimeUUID();
        String activityDesc = "testActivity" + UUIDUtils.newTimeUUID();

        client.postActivity( "POST", activityTitle, activityDesc, "testCategory", current, null, null, null, null );

        refreshIndex("test-organization", "test-app");

        Query results = client.queryActivity();

        ApiResponse response = results.getResponse();

        Entity result = response.getEntities().get( 0 );

        assertEquals( "POST", result.getProperties().get( "verb" ).asText() );
        assertEquals( activityTitle, result.getProperties().get( "title" ).asText() );
        assertEquals( activityDesc, result.getProperties().get( "content" ).asText() );

        //ACTOR isn't coming back, why?
        assertEquals( current.getUuid().toString(), result.getProperties().get( "actor" ).get( "uuid" ).asText() );
    }
}
