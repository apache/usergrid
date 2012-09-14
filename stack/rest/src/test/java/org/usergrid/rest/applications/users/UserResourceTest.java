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
import static org.junit.Assert.assertTrue;
import static org.usergrid.rest.applications.utils.TestUtils.getIdFromSearchResults;
import static org.usergrid.utils.MapUtils.hashMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.core.MediaType;

import me.prettyprint.cassandra.utils.TimeUUIDUtils;

import org.codehaus.jackson.JsonNode;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.java.client.Client.Query;
import org.usergrid.java.client.entities.Activity;
import org.usergrid.java.client.entities.Activity.ActivityObject;
import org.usergrid.java.client.entities.Entity;
import org.usergrid.java.client.entities.User;
import org.usergrid.java.client.response.ApiResponse;
import org.usergrid.management.ApplicationInfo;
import org.usergrid.management.OrganizationInfo;
import org.usergrid.rest.AbstractRestTest;
import org.usergrid.rest.applications.utils.UserRepo;
import org.usergrid.utils.UUIDUtils;

import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.api.client.UniformInterfaceException;

/**
 * @author zznate
 * @author tnine
 */
public class UserResourceTest extends AbstractRestTest {

    private static Logger log = LoggerFactory.getLogger(UserResourceTest.class);

    @Test
    public void usernameQuery() {

        UserRepo.INSTANCE.load(resource(), access_token);

        String ql = "username = 'user*'";

        JsonNode node = resource().path("/test-organization/test-app/users").queryParam("ql", ql)
                .queryParam("access_token", access_token).accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).get(JsonNode.class);

        assertEquals(UserRepo.INSTANCE.getByUserName("user1"), getIdFromSearchResults(node, 0));
        assertEquals(UserRepo.INSTANCE.getByUserName("user2"), getIdFromSearchResults(node, 1));
        assertEquals(UserRepo.INSTANCE.getByUserName("user3"), getIdFromSearchResults(node, 2));

    }

    @Test
    public void nameQuery() {

        UserRepo.INSTANCE.load(resource(), access_token);

        String ql = "name = 'John*'";

        JsonNode node = resource().path("/test-organization/test-app/users").queryParam("ql", ql)
                .queryParam("access_token", access_token).accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).get(JsonNode.class);

        assertEquals(UserRepo.INSTANCE.getByUserName("user2"), getIdFromSearchResults(node, 0));
        assertEquals(UserRepo.INSTANCE.getByUserName("user3"), getIdFromSearchResults(node, 1));

    }

    @Test
    public void nameQueryByUUIDs() throws Exception {
        UserRepo.INSTANCE.load(resource(), access_token);

        String ql = "name = 'John*'";

        ApplicationInfo appInfo = managementService.getApplicationInfo("test-organization/test-app");
        OrganizationInfo orgInfo = managementService.getOrganizationByName("test-organization");

        resource().path("/" + orgInfo.getUuid() + "/" + appInfo.getId() + "/users").queryParam("ql", ql)
                .queryParam("access_token", access_token).accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).get(JsonNode.class);

    }

    @Test
    public void nameFullTextQuery() {

        UserRepo.INSTANCE.load(resource(), access_token);

        String ql = "name contains 'Smith' order by name ";

        JsonNode node = resource().path("/test-organization/test-app/users").queryParam("ql", ql)
                .queryParam("access_token", access_token).accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).get(JsonNode.class);

        assertEquals(UserRepo.INSTANCE.getByUserName("user1"), getIdFromSearchResults(node, 0));
        assertEquals(UserRepo.INSTANCE.getByUserName("user2"), getIdFromSearchResults(node, 1));
        assertEquals(UserRepo.INSTANCE.getByUserName("user3"), getIdFromSearchResults(node, 2));

    }

    /**
     * Tests that when a full text index is run on a field that isn't full text
     * indexed an error is thrown
     */
    @Test(expected = UniformInterfaceException.class)
    public void fullTextQueryNotFullTextIndexed() {

        UserRepo.INSTANCE.load(resource(), access_token);

        String ql = "username contains 'user' ";

        resource().path("/test-organization/test-app/users").queryParam("ql", ql)
                .queryParam("access_token", access_token).accept(MediaType.APPLICATION_JSON)
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

        resource().path("/test-organization/test-app/users").queryParam("ql", ql)
                .queryParam("access_token", access_token).accept(MediaType.APPLICATION_JSON)
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

        ApiResponse response = client.postUserActivity(userId.toString(), activity);

        assertNull("Error was: " + response.getErrorDescription(), response.getError());

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

        ApiResponse response = client.postUserActivity(userId.toString(), activity);

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

        ApiResponse response = client.postUserActivity(userId.toString(), activity);

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

        assertNull("Error was: " + response.getErrorDescription(), response.getError());

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

        ApiResponse response = client.createUser(username, name, id + "@usergrid.org", "password");

        assertNull("Error was: " + response.getErrorDescription(), response.getError());

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

        ApiResponse response = client.createUser(username, name, id + "@usergrid.org", "password");

        assertNull("Error was: " + response.getErrorDescription(), response.getError());

        UUID createdId = response.getEntities().get(0).getUuid();

        JsonNode node = resource().path("/test-organization/test-app/users/" + createdId)
                .queryParam("access_token", access_token).accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).delete(JsonNode.class);

        assertNull(node.get("errors"));

        Query results = client.queryUsers(String.format("username = '%s'", name));
        assertEquals(0, results.getResponse().getEntities(User.class).size());

        // now create that same user again, it should work
        response = client.createUser(username, name, id + "@usergrid.org", "password");

        assertNull("Error was: " + response.getErrorDescription(), response.getError());

        createdId = response.getEntities().get(0).getUuid();

        assertNotNull(createdId);

    }

    @Test
    public void singularCollectionName() {
        UUID id = UUIDUtils.newTimeUUID();

        String username = "username1" + id;
        String name = "name1" + id;
        String email = "email1" + id + "@usergrid.org";

        ApiResponse response = client.createUser(username, name, email, "password");

        assertNull("Error was: " + response.getErrorDescription(), response.getError());

        UUID firstCreatedId = response.getEntities().get(0).getUuid();

        username = "username2" + id;
        name = "name2" + id;
        email = "email2" + id + "@usergrid.org";

        response = client.createUser(username, name, email, "password");

        assertNull("Error was: " + response.getErrorDescription(), response.getError());

        UUID secondCreatedId = response.getEntities().get(0).getUuid();

        // now create a connection of "likes" between the first user and the
        // second using pluralized form

        // plural collection name
        String path = String.format("/test-organization/test-app/users/%s/conn1/%s", firstCreatedId, secondCreatedId);

        JsonNode node = resource().path(path).queryParam("access_token", access_token)
                .accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON_TYPE).post(JsonNode.class);

        assertEquals(secondCreatedId.toString(), getEntity(node, 0).get("uuid").asText());

        // singular collection name
        path = String.format("/test-organization/test-app/user/%s/conn2/%s", firstCreatedId, secondCreatedId);

        node = resource().path(path).queryParam("access_token", access_token).accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).post(JsonNode.class);

        assertEquals(secondCreatedId.toString(), getEntity(node, 0).get("uuid").asText());

        path = String.format("/test-organization/test-app/users/%s/conn1", firstCreatedId);

        node = resource().path(path).queryParam("access_token", access_token).accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).get(JsonNode.class);

        assertEquals(secondCreatedId.toString(), getEntity(node, 0).get("uuid").asText());

        path = String.format("/test-organization/test-app/user/%s/conn1", firstCreatedId);

        node = resource().path(path).queryParam("access_token", access_token).accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).get(JsonNode.class);

        assertEquals(secondCreatedId.toString(), getEntity(node, 0).get("uuid").asText());

        path = String.format("/test-organization/test-app/users/%s/conn2", firstCreatedId);

        node = resource().path(path).queryParam("access_token", access_token).accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).get(JsonNode.class);

        assertEquals(secondCreatedId.toString(), getEntity(node, 0).get("uuid").asText());

        path = String.format("/test-organization/test-app/user/%s/conn2", firstCreatedId);

        node = resource().path(path).queryParam("access_token", access_token).accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).get(JsonNode.class);

        assertEquals(secondCreatedId.toString(), getEntity(node, 0).get("uuid").asText());

    }

    @Test
    public void connectionByNameAndType() {
        UUID id = UUIDUtils.newTimeUUID();

        String username1 = "username1" + id;
        String name1 = "name1" + id;
        String email1 = "email1" + id + "@usergrid.org";

        ApiResponse response = client.createUser(username1, name1, email1, "password");

        assertNull("Error was: " + response.getErrorDescription(), response.getError());

        UUID firstCreatedId = response.getEntities().get(0).getUuid();

        String username2 = "username2" + id;
        String name2 = "name2" + id;
        String email2 = "email2" + id + "@usergrid.org";

        response = client.createUser(username2, name2, email2, "password");

        assertNull("Error was: " + response.getErrorDescription(), response.getError());

        UUID secondCreatedId = response.getEntities().get(0).getUuid();

        // now create a connection of "likes" between the first user and the
        // second using pluralized form

        // named entity in collection name
        String path = String.format("/test-organization/test-app/users/%s/conn1/users/%s", firstCreatedId, username2);

        JsonNode node = resource().path(path).queryParam("access_token", access_token)
                .accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON_TYPE).post(JsonNode.class);

        assertEquals(secondCreatedId.toString(), getEntity(node, 0).get("uuid").asText());

        // named entity in collection name
        path = String.format("/test-organization/test-app/users/%s/conn2/users/%s", username1, username2);

        node = resource().path(path).queryParam("access_token", access_token).accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).post(JsonNode.class);

        assertEquals(secondCreatedId.toString(), getEntity(node, 0).get("uuid").asText());

    }

    @Test
    public void connectionByNameAndDynamicType() {
        UUID id = UUIDUtils.newTimeUUID();

        String username1 = "username1" + id;
        String name1 = "name1" + id;
        String email1 = "email1" + id + "@usergrid.org";

        ApiResponse response = client.createUser(username1, name1, email1, "password");

        assertNull("Error was: " + response.getErrorDescription(), response.getError());

        UUID firstCreatedId = response.getEntities().get(0).getUuid();

        String name = "pepperoni";

        Entity pizza = new Entity();
        pizza.setProperty("name", name);
        pizza.setType("pizza");

        response = client.createEntity(pizza);

        assertNull("Error was: " + response.getErrorDescription(), response.getError());

        UUID secondCreatedId = response.getEntities().get(0).getUuid();

        // now create a connection of "likes" between the first user and the
        // second using pluralized form

        // named entity in collection name
        String path = String.format("/test-organization/test-app/users/%s/conn1/pizzas/%s", firstCreatedId,
                secondCreatedId);

        JsonNode node = resource().path(path).queryParam("access_token", access_token)
                .accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON_TYPE).post(JsonNode.class);

        assertEquals(secondCreatedId.toString(), getEntity(node, 0).get("uuid").asText());

        // named entity in collection name
        path = String.format("/test-organization/test-app/users/%s/conn2/pizzas/%s", username1, name);

        node = resource().path(path).queryParam("access_token", access_token).accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).post(JsonNode.class);

        assertEquals(secondCreatedId.toString(), getEntity(node, 0).get("uuid").asText());

    }

    @Test
    public void nameUpdate() {
        UUID id = UUIDUtils.newTimeUUID();

        String username = "username" + id;
        String name = "name" + id;
        String email = "email" + id + "@usergrid.org";

        ApiResponse response = client.createUser(username, name, email, "password");

        assertNull("Error was: " + response.getErrorDescription(), response.getError());

        Entity userEntity = response.getEntities().get(0);

        // attempt to log in
        JsonNode node = resource().path("/test-organization/test-app/token").queryParam("username", username)
                .queryParam("password", "password").queryParam("grant_type", "password")
                .accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON_TYPE).get(JsonNode.class);

        assertEquals(username, node.get("user").get("username").asText());
        assertEquals(name, node.get("user").get("name").asText());
        assertEquals(email, node.get("user").get("email").asText());

        // now update the name and email
        String newName = "newName";
        String newEmail = "newEmail" + UUIDUtils.newTimeUUID() + "@usergrid.org";

        userEntity.setProperty("name", newName);
        userEntity.setProperty("email", newEmail);

        node = resource().path(String.format("/test-organization/test-app/users/%s", username))
                .queryParam("access_token", access_token).accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).put(JsonNode.class, userEntity.getProperties());

        // now see if we've updated
        node = resource().path("/test-organization/test-app/token").queryParam("username", username)
                .queryParam("password", "password").queryParam("grant_type", "password")
                .accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON_TYPE).get(JsonNode.class);

        assertEquals(username, node.get("user").get("username").asText());
        assertEquals(newName, node.get("user").get("name").asText());
        assertEquals(newEmail, node.get("user").get("email").asText());

    }

    /**
     * 
     * @return
     */
    public JsonNode getActor(Entity entity) {
        return entity.getProperties().get("actor");
    }

    @Test
    public void test_POST_batch() {

        log.info("UserResourceTest.test_POST_batch");

        JsonNode node = null;

        List<Map<String, Object>> batch = new ArrayList<Map<String, Object>>();

        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put("username", "test_user_1");
        properties.put("email", "user1@test.com");
        batch.add(properties);

        properties = new LinkedHashMap<String, Object>();
        properties.put("username", "test_user_2");
        batch.add(properties);

        properties = new LinkedHashMap<String, Object>();
        properties.put("username", "test_user_3");
        batch.add(properties);

        node = resource().path("/test-organization/test-app/users/").queryParam("access_token", access_token)
                .accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON_TYPE).post(JsonNode.class, batch);

        assertNotNull(node);
        logNode(node);

    }

    @Test
    public void deactivateUser() {

        UUID newUserUuid = UUIDUtils.newTimeUUID();

        String userName = String.format("test%s", newUserUuid);

        Map<String, String> payload = hashMap("email", String.format("%s@anuff.com", newUserUuid))
                .map("username", userName).map("name", "Ed Anuff").map("password", "sesame").map("pin", "1234");

        JsonNode node = resource().path("/test-organization/test-app/users").queryParam("access_token", access_token)
                .accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON_TYPE).post(JsonNode.class, payload);

        JsonNode response = resource().path("/test-organization/test-app/users")
                .queryParam("access_token", access_token).accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).get(JsonNode.class);

        // disable the user

        Map<String, String> data = new HashMap<String, String>();

        response = resource().path(String.format("/test-organization/test-app/users/%s/deactivate", userName))
                .queryParam("access_token", access_token).accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).post(JsonNode.class, data);

        JsonNode entity = getEntity(response, 0);

        assertFalse(entity.get("activated").asBoolean());
        assertNotNull(entity.get("deactivated"));

    }

    @Test
    public void test_PUT_password_fail() {
        ApiResponse response = client.changePassword("edanuff", "foo", "bar");

        assertEquals("auth_invalid_username_or_password", response.getError());
    }

    @Test
    public void test_GET_user_ok() throws InterruptedException {

        // TODO figure out what is being overridden? why 400?
        JsonNode node = resource().path("/test-organization/test-app/users").queryParam("access_token", access_token)
                .accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON_TYPE).get(JsonNode.class);

        String uuid = node.get("entities").get(0).get("uuid").getTextValue();

        node = resource().path("/test-organization/test-app/users/" + uuid).queryParam("access_token", access_token)
                .accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON_TYPE).get(JsonNode.class);
        logNode(node);
        assertEquals("ed@anuff.com", node.get("entities").get(0).get("email").getTextValue());
    }

    @Test
    public void test_PUT_password_ok() {

        ApiResponse response = client.changePassword("edanuff", "sesame", "sesame1");

        assertNull(response.getError());

        response = client.authorizeAppUser("ed@anuff.com", "sesame1");

        assertNull(response.getError());

        // if this was successful, we need to re-set the password for other
        // tests
        response = client.changePassword("edanuff", "sesame1", "sesame");

        assertNull(response.getError());

    }

    @Test
    public void setUserPasswordAsAdmin() {

        String newPassword = "foo";

        Map<String, String> data = new HashMap<String, String>();
        data.put("newpassword", newPassword);

        // change the password as admin. The old password isn't required
        JsonNode node = resource().path("/test-organization/test-app/users/edanuff/password")
                .queryParam("access_token", adminAccessToken).accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).post(JsonNode.class, data);

        assertNull(getError(node));

        ApiResponse response = client.authorizeAppUser("ed@anuff.com", newPassword);

        assertNull(response.getError());

    }

    @Test
    public void passwordMismatchErrorUser() {
        String origPassword = "foo";
        String newPassword = "bar";

        Map<String, String> data = new HashMap<String, String>();
        data.put("newpassword", origPassword);

        // now change the password, with an incorrect old password

        data.put("oldpassword", origPassword);
        data.put("newpassword", newPassword);

        Status responseStatus = null;
        try {
            resource().path("/test-organization/test-app/users/edanuff/password").accept(MediaType.APPLICATION_JSON)
                    .type(MediaType.APPLICATION_JSON_TYPE).post(JsonNode.class, data);
        } catch (UniformInterfaceException uie) {
            responseStatus = uie.getResponse().getClientResponseStatus();
        }

        assertNotNull(responseStatus);

        assertEquals(Status.BAD_REQUEST, responseStatus);

    }

  @Test
  public void addRemoveRole() {

    UUID id = UUIDUtils.newTimeUUID();

    String roleName = "rolename" + id;

    String username = "username" + id;
    String name = "name" + id;
    String email = "email" + id + "@usergrid.org";

    ApiResponse response = client.createUser(username, name, email, "password");
    assertNull("Error was: " + response.getErrorDescription(), response.getError());

    UUID createdId = response.getEntities().get(0).getUuid();

    // create Role

    String json = "{\"title\":\"" + roleName + "\",\"name\":\"" + roleName + "\"}";
    JsonNode node = resource()
        .path("/test-organization/test-app/rolenames")
        .queryParam("access_token", access_token)
        .accept(MediaType.APPLICATION_JSON)
        .type(MediaType.APPLICATION_JSON_TYPE)
        .post(JsonNode.class, json);

    // check it
    assertNull(node.get("errors"));


    // add Role

    node = resource()
        .path("/test-organization/test-app/users/" + createdId + "/roles/" + roleName)
        .queryParam("access_token", access_token)
        .accept(MediaType.APPLICATION_JSON)
        .type(MediaType.APPLICATION_JSON_TYPE)
        .post(JsonNode.class);

    // check it
    assertNull(node.get("errors"));
    assertEquals(node.get("entities").get(0).get("name").asText(), roleName);

    node = resource()
        .path("/test-organization/test-app/users/" + createdId + "/roles")
        .queryParam("access_token", access_token)
        .accept(MediaType.APPLICATION_JSON)
        .type(MediaType.APPLICATION_JSON_TYPE)
        .get(JsonNode.class);
    assertNull(node.get("errors"));
    assertEquals(node.get("entities").get(0).get("name").asText(), roleName);


    // remove Role

    node = resource()
        .path("/test-organization/test-app/users/" + createdId + "/roles/" + roleName)
        .queryParam("access_token", access_token)
        .accept(MediaType.APPLICATION_JSON)
        .type(MediaType.APPLICATION_JSON_TYPE)
        .delete(JsonNode.class);

    // check it
    assertNull(node.get("errors"));

    node = resource()
        .path("/test-organization/test-app/users/" + createdId + "/roles")
        .queryParam("access_token", access_token)
        .accept(MediaType.APPLICATION_JSON)
        .type(MediaType.APPLICATION_JSON_TYPE)
        .get(JsonNode.class);
    assertNull(node.get("errors"));
    assertTrue(node.get("entities").size() == 0);
  }

    @Test
    public void revokeToken() throws Exception {

        String token1 = super.userToken();
        String token2 = super.userToken();

        JsonNode response = resource().path("/test-organization/test-app/users/edanuff")
                .queryParam("access_token", token1).accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).get(JsonNode.class);

        assertNotNull(getEntity(response, 0));

        response = resource().path("/test-organization/test-app/users/edanuff").queryParam("access_token", token2)
                .accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON_TYPE).get(JsonNode.class);

        assertNotNull(getEntity(response, 0));

        // now revoke the tokens
        response = resource().path("/test-organization/test-app/users/edanuff/revoketokens")
                .queryParam("access_token", adminAccessToken).accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).post(JsonNode.class);

        // the tokens shouldn't work
        
        Status status = null;
         
        try{
            response = resource().path("/test-organization/test-app/users/edanuff").queryParam("access_token", token1)
                    .accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON_TYPE).get(JsonNode.class);
        }catch(UniformInterfaceException uie){
            status = uie.getResponse().getClientResponseStatus();
        }

        assertEquals(Status.UNAUTHORIZED, status);
        
        status = null;

        try{
            response = resource().path("/test-organization/test-app/users/edanuff").queryParam("access_token", token2)
                    .accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON_TYPE).get(JsonNode.class);
        }catch(UniformInterfaceException uie){
            status = uie.getResponse().getClientResponseStatus();
        }

        assertEquals(Status.UNAUTHORIZED, status);

    }

}
