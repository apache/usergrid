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


import org.apache.usergrid.persistence.index.utils.MapUtils;
import org.apache.usergrid.rest.test.resource.AbstractRestIT;
import org.apache.usergrid.rest.test.resource.endpoints.CollectionEndpoint;
import org.apache.usergrid.rest.test.resource.model.*;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.utils.UUIDUtils;

import javax.ws.rs.ClientErrorException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/** @author tnine */

public class ActivityResourceIT extends AbstractRestIT {
    private static Logger log = LoggerFactory.getLogger( ActivityResourceIT.class );

    private static final String GROUP = "testGroup";

    private static final String USER = "edanuff";

    private static boolean groupCreated = false;
    private CollectionEndpoint groupsResource;
    private CollectionEndpoint groupActivityResource;
    private CollectionEndpoint usersResource;
    private User current;
    private Entity activity;
    private String activityTitle;
    private String activityDesc;


    @Before
    public void setup() {
        this.groupsResource = this.app().collection("groups");
        this.usersResource = this.app().collection("users");
        Entity entity = groupsResource.post(new Entity().chainPut("name",GROUP).chainPut("path","/"+GROUP));
        current = new User("user1","user1","user1","user1");
        current = new User( this.app().collection("users").post(current));
        this.activityTitle = "testTitle" ;
        this.activityDesc = "testActivity" ;
        this.activity = new ActivityEntity().putActor(current).chainPut("title", activityTitle).chainPut("content", activityDesc).chainPut("category", "testCategory").chainPut("verb", "POST");
        this.groupActivityResource = groupsResource.entity(entity).activities();
        refreshIndex();
    }


    @Test
    public void postNullActivityToGroup() {

        boolean fail = false;
        try {
            Entity groupActivity = groupActivityResource.post(new Entity());
        }
        catch ( Exception e ) {
            fail = true;
        }
        assertTrue( fail );
    }


    @Test
    public void postGroupActivity() {

        // don't populate the user, it will use the currently authenticated user.
        try {
            groupActivityResource.post(activity);
        }catch (ClientErrorException e)
        {
            throw e;
        }
        refreshIndex();

        Collection results = groupActivityResource.get();

        ApiResponse response = results.getResponse();

        Entity result = response.getEntities().get( 0 );

        assertEquals("POST", result.get("verb").toString());
        assertEquals( activityTitle, result.get("title").toString() );
        assertEquals( activityDesc, result.get("content").toString() );

    }


    @Test
    public void postUserActivity() {

        // don't populate the user, it will use the currently authenticated
        // user.

        usersResource.entity(current).activities().post(activity);


        refreshIndex();

        Collection results = usersResource.entity(current).activities().get();

        ApiResponse response = results.getResponse();

        ActivityEntity result =new ActivityEntity( response.getEntities().get( 0 ));

        assertEquals("POST", result.get("verb").toString());
        assertEquals(activityTitle, result.get("title").toString());
        assertEquals(activityDesc, result.get("content").toString());
        assertEquals( current.getUuid().toString(), result.getActor().get("uuid").toString() );


    }


    @Test
    public void postActivity() {

        // don't populate the user, it will use the currently authenticated
        // user.

        this.app().collection("activities").post(activity);

        refreshIndex();

        Collection results = this.app().collection("activities").get();

        ApiResponse response = results.getResponse();

        ActivityEntity result =new  ActivityEntity( response.getEntities().get( 0 ));

        assertEquals("POST", result.get("verb").toString());
        assertEquals(activityTitle, result.get("title").toString());
        assertEquals(activityDesc, result.get("content").toString());
        //ACTOR isn't coming back, why?
        assertEquals(current.getUuid().toString(), result.getActor().get("uuid").toString());
    }
}
