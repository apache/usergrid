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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.usergrid.rest.applications.utils.TestUtils.getIdFromSearchResults;

import java.util.UUID;

import javax.ws.rs.core.MediaType;

import org.codehaus.jackson.JsonNode;
import org.junit.Test;
import org.usergrid.java.client.Client.Query;
import org.usergrid.java.client.entities.Activity;
import org.usergrid.java.client.entities.Activity.ActivityObject;
import org.usergrid.java.client.entities.Entity;
import org.usergrid.java.client.response.ApiResponse;
import org.usergrid.rest.AbstractRestTest;
import org.usergrid.rest.applications.utils.UserRepo;
import org.usergrid.utils.UUIDUtils;

import com.sun.jersey.api.client.UniformInterfaceException;

/**
 * @author zznate
 * @author tnine
 */
public class UserResourceTest extends AbstractRestTest {

    
    
    @Test
    public void test_PUT_password_fail() {
        ApiResponse response = client.changePassword("edanuff", "foo", "bar");

        assertEquals("auth_invalid_username_or_password", response.getError());
    }

    @Test
    public void test_PUT_password_ok() {

        ApiResponse response = client.changePassword("edanuff", "sesame",
                "sesame1");

        assertNull(response.getError());

    }

    @Test
    public void test_GET_user_ok() {

        // TODO figure out what is being overridden? why 400?
        JsonNode node = resource().path("/test-app/users")
                .queryParam("access_token", access_token)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).get(JsonNode.class);

        String uuid = node.get("entities").get(0).get("uuid").getTextValue();

        node = resource().path("/test-app/users/" + uuid)
                .queryParam("access_token", access_token)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).get(JsonNode.class);
        logNode(node);
        assertEquals("ed@anuff.com", node.get("entities").get(0).get("email")
                .getTextValue());
    }

    @Test
    public void usernameQuery() {

        UserRepo.INSTANCE.load(resource(), access_token);

        String ql = "username = 'user*'";

        JsonNode node = resource().path("/test-app/users").queryParam("ql", ql)
                .queryParam("access_token", access_token)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).get(JsonNode.class);

        assertEquals(UserRepo.INSTANCE.getByUserName("user1"),
                getIdFromSearchResults(node, 0));
        assertEquals(UserRepo.INSTANCE.getByUserName("user2"),
                getIdFromSearchResults(node, 1));
        assertEquals(UserRepo.INSTANCE.getByUserName("user3"),
                getIdFromSearchResults(node, 2));

    }

    @Test
    public void nameQuery() {

        UserRepo.INSTANCE.load(resource(), access_token);

        String ql = "name = 'John*'";

        JsonNode node = resource().path("/test-app/users").queryParam("ql", ql)
                .queryParam("access_token", access_token)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).get(JsonNode.class);

        assertEquals(UserRepo.INSTANCE.getByUserName("user2"),
                getIdFromSearchResults(node, 0));
        assertEquals(UserRepo.INSTANCE.getByUserName("user3"),
                getIdFromSearchResults(node, 1));

    }

    @Test
    public void nameFullTextQuery() {

        UserRepo.INSTANCE.load(resource(), access_token);

        String ql = "name contains 'Smith' order by name ";

        JsonNode node = resource().path("/test-app/users").queryParam("ql", ql)
                .queryParam("access_token", access_token)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).get(JsonNode.class);

        assertEquals(UserRepo.INSTANCE.getByUserName("user1"),
                getIdFromSearchResults(node, 0));
        assertEquals(UserRepo.INSTANCE.getByUserName("user2"),
                getIdFromSearchResults(node, 1));
        assertEquals(UserRepo.INSTANCE.getByUserName("user3"),
                getIdFromSearchResults(node, 2));

    }

    /**
     * Tests that when a full text index is run on a field that isn't full text
     * indexed an error is thrown
     */
    @Test(expected = UniformInterfaceException.class)
    public void fullTextQueryNotFullTextIndexed() {

        UserRepo.INSTANCE.load(resource(), access_token);

        String ql = "username contains 'user' ";

        resource().path("/test-app/users").queryParam("ql", ql)
                .queryParam("access_token", access_token)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).get(JsonNode.class);

    }

    /**
     * Tests that when a full text index is run on a field that isn't full text
     * indexed an error is thrown
     */
    @Test(expected = UniformInterfaceException.class)
    public void fullQueryNotIndexed() {

        UserRepo.INSTANCE.load(resource(), access_token);

        String ql = "picture = 'foo' ";

        resource().path("/test-app/users").queryParam("ql", ql)
                .queryParam("access_token", access_token)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).get(JsonNode.class);

    }

    /**
     * Test that when activity is pushed with not actor, it's set to the user
     * who created it
     */
    @Test
    public void emtpyActorActivity() {
        UserRepo.INSTANCE.load(resource(), access_token);
        UUID userId = UserRepo.INSTANCE.getByUserName("user1");

        Activity activity = new Activity();
        activity.setProperty("email", "rod@rodsimpson.com");
        activity.setProperty("verb", "POST");
        activity.setProperty("content", "Look! more new content");

        ApiResponse response = client.postUserActivity(userId.toString(), activity);

        assertNull("Error was: " + response.getErrorDescription(), response.getError());
        
        Entity entity = response.getEntities().get(0);

        UUID activityId = entity.getUuid();

        assertNotNull(activityId);

        JsonNode actor = getActor(entity);

        UUID actorId = UUIDUtils.tryGetUUID(actor.get("uuid").getTextValue());

        assertEquals(userId, actorId);

        assertEquals("user1@apigee.com", actor.get("email")
                .asText());
    }

    /**
     * Insert the uuid and email if they're empty in the request
     */
    @Test
    public void noUUIDorEmail() {

        UserRepo.INSTANCE.load(resource(), access_token);
        UUID userId = UserRepo.INSTANCE.getByUserName("user1");

        Activity activity = new Activity();
        activity.setProperty("email", "rod@rodsimpson.com");
        activity.setProperty("verb", "POST");
        activity.setProperty("content", "Look! more new content");

        // same as above, but with actor partially filled out

        ActivityObject actorPost = new ActivityObject();
        actorPost.setDisplayName("Dino");

        activity.setActor(actorPost);

        ApiResponse response = client.postUserActivity(userId.toString(),
                activity);
        
        assertNull("Error was: " + response.getErrorDescription(), response.getError());
        

        Entity entity = response.getEntities().get(0);

        UUID activityId = entity.getUuid();

        assertNotNull(activityId);

        JsonNode actor = getActor(entity);

        UUID actorId = UUIDUtils.tryGetUUID(actor.get("uuid").getTextValue());

        assertEquals(userId, actorId);

        assertEquals("user1@apigee.com", actor.get("email")
                .asText());

    }

    /**
     * Don't touch the UUID when it's already set in the JSON
     */
    @Test
    public void ignoreUUIDandEmail() {
        UserRepo.INSTANCE.load(resource(), access_token);
        UUID userId = UserRepo.INSTANCE.getByUserName("user1");

        UUID testUUID = UUIDUtils.newTimeUUID();
        String testEmail = "foo@bar.com";

        // same as above, but with actor partially filled out
        Activity activity = new Activity();
        activity.setProperty("email", "rod@rodsimpson.com");
        activity.setProperty("verb", "POST");
        activity.setProperty("content", "Look! more new content");

        // same as above, but with actor partially filled out

        ActivityObject actorPost = new ActivityObject();
        actorPost.setDisplayName("Dino");
        actorPost.setUuid(testUUID);
        actorPost.setDynamicProperty("email", testEmail);

        activity.setActor(actorPost);

        ApiResponse response = client.postUserActivity(userId.toString(),
                activity);
        
        assertNull("Error was: " + response.getErrorDescription(), response.getError());
        

        Entity entity = response.getEntities().get(0);

        UUID activityId = entity.getUuid();

        assertNotNull(activityId);

        JsonNode actor = getActor(entity);

        UUID actorId = UUIDUtils.tryGetUUID(actor.get("uuid").getTextValue());

        assertEquals(testUUID, actorId);

        assertEquals(testEmail, actor.get("email").asText());

    }

    /**
     * Test that when activity is pushed with not actor, it's set to the user
     * who created it
     */
    @Test
    public void userActivitiesDefaultOrder() {
        UserRepo.INSTANCE.load(resource(), access_token);
        UUID userId = UserRepo.INSTANCE.getByUserName("user1");

        Activity activity = new Activity();
        activity.setProperty("email", "rod@rodsimpson.com");
        activity.setProperty("verb", "POST");
        activity.setProperty("content", "activity 1");

        ApiResponse response = client.postUserActivity(userId.toString(),
                activity);
        
        assertNull("Error was: " + response.getErrorDescription(), response.getError());

        Entity entity = response.getFirstEntity();

        UUID firstActivityId = entity.getUuid();

        activity = new Activity();
        activity.setProperty("email", "rod@rodsimpson.com");
        activity.setProperty("verb", "POST");
        activity.setProperty("content", "activity 2");

        response = client.postUserActivity(userId.toString(), activity);
        
        assertNull("Error was: " + response.getErrorDescription(), response.getError());

        entity = response.getFirstEntity();

        UUID secondActivityId = entity.getUuid();

        Query query = client.queryActivity();

        entity = query.getResponse().getEntities().get(0);

        assertEquals(secondActivityId, entity.getUuid());

        entity = query.getResponse().getEntities().get(1);

        assertEquals(firstActivityId, entity.getUuid());

    }

    /**
     * 
     * @param putResponse
     * @return
     */
    public JsonNode getActor(Entity entity) {
        return entity.getProperties().get("actor");
    }

}
