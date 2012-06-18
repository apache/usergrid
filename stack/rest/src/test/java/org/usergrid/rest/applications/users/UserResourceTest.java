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

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.usergrid.rest.applications.utils.TestUtils.getIdFromSearchResults;

import java.util.List;
import java.util.UUID;

import javax.ws.rs.core.MediaType;

import org.codehaus.jackson.JsonNode;
import org.junit.Ignore;
import org.junit.Test;
import org.usergrid.java.client.Client.Query;
import org.usergrid.java.client.entities.Activity;
import org.usergrid.java.client.entities.Activity.ActivityObject;
import org.usergrid.java.client.entities.Entity;
import org.usergrid.java.client.entities.User;
import org.usergrid.java.client.response.ApiResponse;
import org.usergrid.management.ApplicationInfo;
import org.usergrid.management.OrganizationInfo;
import org.usergrid.management.OrganizationOwnerInfo;
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
    public void usernameQuery() {

        UserRepo.INSTANCE.load(resource(), access_token);

        String ql = "username = 'user*'";

        JsonNode node = resource().path("/test-organization/test-app/users")
                .queryParam("ql", ql).queryParam("access_token", access_token)
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

        JsonNode node = resource().path("/test-organization/test-app/users")
                .queryParam("ql", ql).queryParam("access_token", access_token)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).get(JsonNode.class);

        assertEquals(UserRepo.INSTANCE.getByUserName("user2"),
                getIdFromSearchResults(node, 0));
        assertEquals(UserRepo.INSTANCE.getByUserName("user3"),
                getIdFromSearchResults(node, 1));

    }

    @Test
    public void nameQueryByUUIDs() throws Exception {
        UserRepo.INSTANCE.load(resource(), access_token);

        String ql = "name = 'John*'";

        ApplicationInfo appInfo = managementService
                .getApplicationInfo("test-organization/test-app");
        OrganizationInfo orgInfo = managementService
                .getOrganizationByName("test-organization");

        JsonNode node = resource()
                .path("/" + orgInfo.getUuid() + "/" + appInfo.getId()
                        + "/users").queryParam("ql", ql)
                .queryParam("access_token", access_token)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).get(JsonNode.class);

    }

    @Test
    public void nameFullTextQuery() {

        UserRepo.INSTANCE.load(resource(), access_token);

        String ql = "name contains 'Smith' order by name ";

        JsonNode node = resource().path("/test-organization/test-app/users")
                .queryParam("ql", ql).queryParam("access_token", access_token)
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

        resource().path("/test-organization/test-app/users")
                .queryParam("ql", ql).queryParam("access_token", access_token)
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

        resource().path("/test-organization/test-app/users")
                .queryParam("ql", ql).queryParam("access_token", access_token)
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

        ApiResponse response = client.postUserActivity(userId.toString(),
                activity);

        assertNull("Error was: " + response.getErrorDescription(),
                response.getError());

        Entity entity = response.getEntities().get(0);

        UUID activityId = entity.getUuid();

        assertNotNull(activityId);

        JsonNode actor = getActor(entity);

        UUID actorId = UUIDUtils.tryGetUUID(actor.get("uuid").getTextValue());

        assertEquals(userId, actorId);

        assertEquals("user1@apigee.com", actor.get("email").asText());
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

        assertNull("Error was: " + response.getErrorDescription(),
                response.getError());

        Entity entity = response.getEntities().get(0);

        UUID activityId = entity.getUuid();

        assertNotNull(activityId);

        JsonNode actor = getActor(entity);

        UUID actorId = UUIDUtils.tryGetUUID(actor.get("uuid").getTextValue());

        assertEquals(userId, actorId);

        assertEquals("user1@apigee.com", actor.get("email").asText());

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

        assertNull("Error was: " + response.getErrorDescription(),
                response.getError());

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

        assertNull("Error was: " + response.getErrorDescription(),
                response.getError());

        Entity entity = response.getFirstEntity();

        UUID firstActivityId = entity.getUuid();

        activity = new Activity();
        activity.setProperty("email", "rod@rodsimpson.com");
        activity.setProperty("verb", "POST");
        activity.setProperty("content", "activity 2");

        response = client.postUserActivity(userId.toString(), activity);

        assertNull("Error was: " + response.getErrorDescription(),
                response.getError());

        entity = response.getFirstEntity();

        UUID secondActivityId = entity.getUuid();

        Query query = client.queryActivity();

        entity = query.getResponse().getEntities().get(0);

        assertEquals(secondActivityId, entity.getUuid());

        entity = query.getResponse().getEntities().get(1);

        assertEquals(firstActivityId, entity.getUuid());

    }

    /**
     * Tests that when querying all users, we get the same result size when
     * using "order by"
     */
    @Test
    public void resultSizeSame() {
        UserRepo.INSTANCE.load(resource(), access_token);
        UUID userId1 = UserRepo.INSTANCE.getByUserName("user1");
        UUID userId2 = UserRepo.INSTANCE.getByUserName("user2");
        UUID userId3 = UserRepo.INSTANCE.getByUserName("user3");

        Query query = client.queryUsers();

        ApiResponse response = query.getResponse();

        assertNull("Error was: " + response.getErrorDescription(),
                response.getError());

        int nonOrderedSize = response.getEntities().size();

        query = client.queryUsers("order by username");

        response = query.getResponse();

        int orderedSize = response.getEntities().size();

        assertEquals("Sizes match", nonOrderedSize, orderedSize);

        int firstEntityIndex = getEntityIndex(userId1, response);

        int secondEntityIndex = getEntityIndex(userId2, response);

        int thirdEntityIndex = getEntityIndex(userId3, response);

        assertTrue("Ordered correctly", firstEntityIndex < secondEntityIndex);

        assertTrue("Ordered correctly", secondEntityIndex < thirdEntityIndex);

    }

    private int getEntityIndex(UUID entityId, ApiResponse response) {
        List<Entity> entities = response.getEntities();

        for (int i = 0; i < entities.size(); i++) {
            if (entityId.equals(entities.get(i).getUuid())) {
                return i;
            }
        }

        return -1;
    }

    @Test
    public void clientNameQuery() {

        UUID id = UUIDUtils.newTimeUUID();

        String username = "username" + id;
        String name = "name" + id;

        ApiResponse response = client.createUser(username, name, id
                + "@usergrid.org", "password");

        assertNull("Error was: " + response.getErrorDescription(),
                response.getError());

        UUID createdId = response.getEntities().get(0).getUuid();

        Query results = client.queryUsers(String.format("name = '%s'", name));
        User user = results.getResponse().getEntities(User.class).get(0);

        assertEquals(createdId, user.getUuid());
    }

    @Test
    public void deleteUser() {

        UUID id = UUIDUtils.newTimeUUID();

        String username = "username" + id;
        String name = "name" + id;

        ApiResponse response = client.createUser(username, name, id
                + "@usergrid.org", "password");

        assertNull("Error was: " + response.getErrorDescription(),
                response.getError());

        UUID createdId = response.getEntities().get(0).getUuid();

        JsonNode node = resource()
                .path("/test-organization/test-app/users/" + createdId)
                .queryParam("access_token", access_token)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).delete(JsonNode.class);

        assertNull(node.get("errors"));

        Query results = client.queryUsers(String
                .format("username = '%s'", name));
        assertEquals(0, results.getResponse().getEntities(User.class).size());

        // now create that same user again, it should work
        response = client.createUser(username, name, id + "@usergrid.org",
                "password");

        assertNull("Error was: " + response.getErrorDescription(),
                response.getError());

        createdId = response.getEntities().get(0).getUuid();

        assertNotNull(createdId);

    }

    @Test
    public void singularCollectionName() {
        UUID id = UUIDUtils.newTimeUUID();

        String username = "username1" + id;
        String name = "name1" + id;
        String email = "email1" + id + "@usergrid.org";

        ApiResponse response = client.createUser(username, name, email,
                "password");

        assertNull("Error was: " + response.getErrorDescription(),
                response.getError());

        UUID firstCreatedId = response.getEntities().get(0).getUuid();

        username = "username2" + id;
        name = "name2" + id;
        email = "email2" + id + "@usergrid.org";

        response = client.createUser(username, name, email, "password");

        assertNull("Error was: " + response.getErrorDescription(),
                response.getError());

        UUID secondCreatedId = response.getEntities().get(0).getUuid();

        // now create a connection of "likes" between the first user and the
        // second using pluralized form

        // plural collection name
        String path = String.format(
                "/test-organization/test-app/users/%s/conn1/%s",
                firstCreatedId, secondCreatedId);

        JsonNode node = resource().path(path)
                .queryParam("access_token", access_token)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).post(JsonNode.class);

        
        assertEquals(secondCreatedId.toString(), getEntity(node, 0).get("uuid").asText());
        
      

        // singular collection name
        path = String.format("/test-organization/test-app/user/%s/conn2/%s",
                firstCreatedId, secondCreatedId);

        node = resource().path(path).queryParam("access_token", access_token)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).post(JsonNode.class);

        
        assertEquals(secondCreatedId.toString(), getEntity(node, 0).get("uuid").asText());
        
        
        
        path  = String.format(
                "/test-organization/test-app/users/%s/conn1",
                firstCreatedId);
        
        node = resource().path(path)
                .queryParam("access_token", access_token)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).get(JsonNode.class);
        
        assertEquals(secondCreatedId.toString(), getEntity(node, 0).get("uuid").asText());
        
        
        path  = String.format(
                "/test-organization/test-app/user/%s/conn1",
                firstCreatedId);
        
        node = resource().path(path)
                .queryParam("access_token", access_token)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).get(JsonNode.class);
        
        assertEquals(secondCreatedId.toString(), getEntity(node, 0).get("uuid").asText());
        
        
        path  = String.format(
                "/test-organization/test-app/users/%s/conn2",
                firstCreatedId);
        
        node = resource().path(path)
                .queryParam("access_token", access_token)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).get(JsonNode.class);
        
        assertEquals(secondCreatedId.toString(), getEntity(node, 0).get("uuid").asText());
        
        path  = String.format(
                "/test-organization/test-app/user/%s/conn2",
                firstCreatedId);
        
        node = resource().path(path)
                .queryParam("access_token", access_token)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).get(JsonNode.class);
        
        assertEquals(secondCreatedId.toString(), getEntity(node, 0).get("uuid").asText());
        
        
        

    }

    /**
     * 
     * @return
     */
    public JsonNode getActor(Entity entity) {
        return entity.getProperties().get("actor");
    }

    @Test
    public void test_PUT_password_fail() {
        ApiResponse response = client.changePassword("edanuff", "foo", "bar");

        assertEquals("auth_invalid_username_or_password", response.getError());
    }

    @Test
    public void test_GET_user_ok() throws InterruptedException {

        // TODO figure out what is being overridden? why 400?
        JsonNode node = resource().path("/test-organization/test-app/users")
                .queryParam("access_token", access_token)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).get(JsonNode.class);

        String uuid = node.get("entities").get(0).get("uuid").getTextValue();

        node = resource().path("/test-organization/test-app/users/" + uuid)
                .queryParam("access_token", access_token)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).get(JsonNode.class);
        logNode(node);
        assertEquals("ed@anuff.com", node.get("entities").get(0).get("email")
                .getTextValue());
    }

    @Test
    public void test_PUT_password_ok() {

        ApiResponse response = client.changePassword("edanuff", "sesame",
                "sesame1");

        assertNull(response.getError());

        response = client.authorizeAppUser("ed@anuff.com", "sesame1");

        assertNull(response.getError());

        // if this was successful, we need to re-set the password for other
        // tests
        response = client.changePassword("edanuff", "sesame1", "sesame");

        assertNull(response.getError());

    }

}
