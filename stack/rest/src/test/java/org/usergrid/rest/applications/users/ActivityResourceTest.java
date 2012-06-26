/*******************************************************************************
 * Copyright 2012 Apigee Corporation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.usergrid.rest.applications.users;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.java.client.Client.Query;
import org.usergrid.java.client.entities.Entity;
import org.usergrid.java.client.entities.User;
import org.usergrid.java.client.response.ApiResponse;
import org.usergrid.rest.AbstractRestTest;
import org.usergrid.utils.UUIDUtils;

/**
 * @author tnine
 */
public class ActivityResourceTest extends AbstractRestTest {
    private static Logger log = LoggerFactory
            .getLogger(ActivityResourceTest.class);

    private static final String GROUP = "testGroup";

    private static final String USER = "edanuff";

    private static boolean groupCreated = false;

    public ActivityResourceTest() throws Exception {

    }

    @Before
    public void setupGroup() {
        if (groupCreated) {
            return;
        }

        client.createGroup(GROUP);

        groupCreated = true;
    }

    @Test
    public void postNullActivityToGroup() {
      String activityTitle = "testTitle" + UUIDUtils.newTimeUUID();
      String activityDesc = "testActivity" + UUIDUtils.newTimeUUID();

      ApiResponse response = client.postGroupActivity(GROUP, null);
      assertEquals("required_property_not_found",response.getError()); //
      assertTrue(response.getException().contains("RequiredPropertyNotFoundException")); // when should we leave this out
    }

    @Test
    public void postGroupActivity() {

        // don't populate the user, it will use the currently authenticated
        // user.

        String activityTitle = "testTitle" + UUIDUtils.newTimeUUID();
        String activityDesc = "testActivity" + UUIDUtils.newTimeUUID();

        client.postGroupActivity(GROUP, "POST", activityTitle, activityDesc,
                "testCategory", null, null, null, null, null);

        Query results = client.queryActivityFeedForGroup(GROUP);

        ApiResponse response = results.getResponse();

        Entity result = response.getEntities().get(0);

        assertEquals("POST", result.getProperties().get("verb").asText());
        assertEquals(activityTitle, result.getProperties().get("title")
                .asText());
        assertEquals(activityDesc, result.getProperties().get("content")
                .asText());

        // now pull the activity directly, we should find it

        results = client.queryActivity();

        response = results.getResponse();

        result = response.getEntities().get(0);

        assertEquals("POST", result.getProperties().get("verb").asText());
        assertEquals(activityTitle, result.getProperties().get("title")
                .asText());
        assertEquals(activityDesc, result.getProperties().get("content")
                .asText());

    }

    @Test
    public void postUserActivity() {

        // don't populate the user, it will use the currently authenticated
        // user.

        User current =  client.getLoggedInUser();
        
        String activityTitle = "testTitle" + UUIDUtils.newTimeUUID();
        String activityDesc = "testActivity" + UUIDUtils.newTimeUUID();

        client.postUserActivity("POST", activityTitle, activityDesc,
                "testCategory", current, null, null, null, null);

        Query results = client.queryActivityFeedForUser(USER);

        ApiResponse response = results.getResponse();

        Entity result = response.getEntities().get(0);

        assertEquals("POST", result.getProperties().get("verb").asText());
        assertEquals(activityTitle, result.getProperties().get("title")
                .asText());
        assertEquals(activityDesc, result.getProperties().get("content")
                .asText());
        assertEquals(current.getUuid().toString(), result.getProperties().get("actor").get("uuid").asText());

        // now pull the activity directly, we should find it

        results = client.queryActivity();

        response = results.getResponse();

        result = response.getEntities().get(0);

        assertEquals("POST", result.getProperties().get("verb").asText());
        assertEquals(activityTitle, result.getProperties().get("title")
                .asText());
        assertEquals(activityDesc, result.getProperties().get("content")
                .asText());

    }
    
    @Test
    public void postActivity() {

        // don't populate the user, it will use the currently authenticated
        // user.

        User current =  client.getLoggedInUser();
        
        String activityTitle = "testTitle" + UUIDUtils.newTimeUUID();
        String activityDesc = "testActivity" + UUIDUtils.newTimeUUID();

        client.postActivity("POST", activityTitle, activityDesc,
                "testCategory", current, null, null, null, null);

        Query results = client.queryActivity();

        ApiResponse response = results.getResponse();

        Entity result = response.getEntities().get(0);

        assertEquals("POST", result.getProperties().get("verb").asText());
        assertEquals(activityTitle, result.getProperties().get("title")
                .asText());
        assertEquals(activityDesc, result.getProperties().get("content")
                .asText());
        
        //ACTOR isn't coming back, why?
        assertEquals(current.getUuid().toString(), result.getProperties().get("actor").get("uuid").asText());

      
    }
}
