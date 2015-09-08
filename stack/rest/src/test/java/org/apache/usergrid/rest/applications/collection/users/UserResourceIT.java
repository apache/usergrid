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
package org.apache.usergrid.rest.applications.collection.users;


import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.usergrid.rest.test.resource.AbstractRestIT;
import org.apache.usergrid.rest.test.resource.endpoints.CollectionEndpoint;
import org.apache.usergrid.rest.test.resource.model.*;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import org.apache.usergrid.rest.applications.utils.UserRepo;
import org.apache.usergrid.utils.UUIDUtils;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.Response;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


/**
 * @author zznate
 * @author tnine
 */

public class UserResourceIT extends AbstractRestIT {

    private static Logger log = LoggerFactory.getLogger(UserResourceIT.class);
    UserRepo userRepo;
    CollectionEndpoint usersResource;
    CollectionEndpoint userResource;

    @Before
    public void setup() {
        userRepo = new UserRepo(clientSetup);
        userRepo.load();
        usersResource = this.app().collection("users");
        userResource = this.app().collection("user");

        clientSetup.refreshIndex();
    }

    @Test
    public void usernameQuery() throws IOException {
        String ql = "username = 'user*'";
        Collection collection = usersResource.get(new QueryParameters().setQuery(ql));
        assertEquals(userRepo.getByUserName("user1"), getIdFromSearchResults(collection, 2));
        assertEquals(userRepo.getByUserName("user2"), getIdFromSearchResults(collection, 1));
        assertEquals(userRepo.getByUserName("user3"), getIdFromSearchResults(collection, 0));
    }


    @Test
    public void nameQuery() throws IOException {
        String ql = "name = 'John*'";

        Collection collection = usersResource.get(new QueryParameters().setQuery(ql));
        assertEquals(userRepo.getByUserName("user2"), getIdFromSearchResults(collection, 1));
        assertEquals(userRepo.getByUserName("user3"), getIdFromSearchResults(collection, 0));
    }


    @Test
    public void nameQueryByUUIDs() throws Exception {
        String ql = "select uuid name = 'John*'";
        Collection response = this.app().collection("users").get(new QueryParameters().setQuery(ql));
        assertNotNull(response.getResponse().getEntities());
    }


    @Test
    public void nameFullTextQuery() throws IOException {
        String ql = "name contains 'Smith' order by name ";
        Collection collection = usersResource.get(new QueryParameters().setQuery(ql));
        assertEquals(userRepo.getByUserName("user1"), getIdFromSearchResults(collection, 0));
        assertEquals(userRepo.getByUserName("user2"), getIdFromSearchResults(collection, 1));
        assertEquals(userRepo.getByUserName("user3"), getIdFromSearchResults(collection, 2));
    }

    /** Get the uuid at the given index for the root node.  If it doesn't exist, null is returned */
    static UUID getIdFromSearchResults( Collection collection, int index ) {


        if ( collection == null ) {
            return null;
        }

        Entity entity = (Entity)collection.getResponse().getEntities().get(index);

        if ( entity == null ) {
            return null;
        }

        return UUIDUtils.tryExtractUUID( entity.get( "uuid" ).toString() );
    }

    /**
     * Test that when activity is pushed with not actor, it's set to the user who created it
     */
    @Test
    public void emtpyActorActivity() throws IOException {

        UUID userId = userRepo.getByUserName("user1");


        ActivityEntity activity = new ActivityEntity("rod@rodsimpson.com", "POST", "Look! more new content");


        Entity entity = usersResource.entity(userId.toString()).activities().post(activity);


        UUID activityId = entity.getUuid();

        assertNotNull(activityId);
        Map<String, Object> actor = (Map<String, Object>) entity.get("actor");


        UUID actorId = UUIDUtils.tryGetUUID(actor.get("uuid").toString());

        assertEquals(userId, actorId);

        assertEquals("user1@apigee.com", actor.get("email").toString());
    }


    /**
     * Insert the uuid and email if they're empty in the request
     */
    @Test
    public void noUUIDorEmail() throws IOException {

        UUID userId = userRepo.getByUserName("user1");

        ActivityEntity activity = new ActivityEntity("rod@rodsimpson.com", "POST", "Look! more new content");

        // same as above, but with actor partially filled out

        Map<String, Object> actorPost = new HashMap<>();
        actorPost.put("displayName", "Dino");

        activity.putActor(actorPost);


        Entity entity = usersResource.entity(userId.toString()).activities().post(activity);

        UUID activityId = entity.getUuid();

        assertNotNull(activityId);

        Map<String, Object> actor = (Map<String, Object>) entity.get("actor");

        UUID actorId = UUIDUtils.tryGetUUID(actor.get("uuid").toString());

        assertEquals(userId, actorId);

        assertEquals("user1@apigee.com", actor.get("email").toString());
    }


    /**
     * Don't touch the UUID when it's already set in the JSON
     */
    @Test
    public void ignoreUUIDandEmail() throws IOException {

        UUID userId = userRepo.getByUserName("user1");


        UUID testUUID = UUIDUtils.newTimeUUID();
        String testEmail = "foo@bar.com";


        ActivityEntity activity = new ActivityEntity("rod@rodsimpson.com", "POST", "Look! more new content");

        // same as above, but with actor partially filled out

        Map<String, Object> actorPost = new HashMap<>();
        actorPost.put("displayName", "Dino");
        actorPost.put("uuid", testUUID);
        actorPost.put("email", testEmail);
        activity.putActor(actorPost);
        // same as above, but with actor partially filled out


        Entity entity = usersResource.entity(userId.toString()).activities().post(activity);

        UUID activityId = entity.getUuid();

        assertNotNull(activityId);

        Map<String, Object> actor = new ActivityEntity(entity).getActor();

        UUID actorId = UUIDUtils.tryGetUUID(actor.get("uuid").toString());

        assertEquals(testUUID, actorId);

        assertEquals(testEmail, actor.get("email").toString());
    }


    /**
     * Test that when activity is pushed with not actor, it's set to the user who created it
     */
    @Test
    public void userActivitiesDefaultOrder() throws IOException {

        UUID userId = userRepo.getByUserName("user1");

        ActivityEntity activity = new ActivityEntity("rod@rodsimpson.com", "POST", "Look! more new content");

        // same as above, but with actor partially filled out

        Entity entity = usersResource.entity(userId.toString()).activities().post(activity);
        refreshIndex();

        UUID firstActivityId = entity.getUuid();

        activity = new ActivityEntity("rod@rodsimpson.com", "POST", "activity 2");
        entity = usersResource.entity(userId.toString()).activities().post(activity);

        refreshIndex();

        UUID secondActivityId = entity.getUuid();

        Collection activities = usersResource.entity(userId.toString()).activities().get();

        entity = activities.getResponse().getEntities().get(0);

        assertEquals(secondActivityId, entity.getUuid());

        entity = activities.getResponse().getEntities().get(1);

        assertEquals(firstActivityId, entity.getUuid());
    }


    @Test
    public void getUserWIthEmailUsername() throws IOException {
        UUID id = UUIDUtils.newTimeUUID();

        String username = "username-email" + "@usergrid.org";
        String name = "name" + id;
        String email = "email" + id + "@usergrid.org";

        User map = new User(username, name, email, null);
        map.put("email", email);

        Entity userEntity = usersResource.post(new Entity(map));
        refreshIndex();

        // get the user with username property that has an email value
        Entity testUser = usersResource.entity(username).get();

        assertEquals(username, testUser.get("username").toString());
        assertEquals(name, testUser.get("name").toString());
        assertEquals(email, testUser.get("email").toString());

        // get the user with email property value
        // get the user with username property that has an email value
        testUser = usersResource.entity(email).get();

        assertEquals(username, testUser.get("username").toString());
        assertEquals(name, testUser.get("name").toString());
        assertEquals(email, testUser.get("email").toString());

    }


    /**
     * Tests that when querying all users, we get the same result size when using "order by"
     */
    @Test
    public void resultSizeSame() throws IOException {

        UUID userId1 = userRepo.getByUserName("user1");
        UUID userId2 = userRepo.getByUserName("user2");
        UUID userId3 = userRepo.getByUserName("user3");

        Collection collection = usersResource.get();

        int nonOrderedSize = collection.getResponse().getEntities().size();

        collection = usersResource.get(new QueryParameters().setQuery("order by username"));

        int orderedSize = collection.getResponse().getEntities().size();

        assertEquals("Sizes match", nonOrderedSize, orderedSize);

        int firstEntityIndex = getEntityIndex(userId1, collection);

        int secondEntityIndex = getEntityIndex(userId2, collection);

        int thirdEntityIndex = getEntityIndex(userId3, collection);

        assertTrue("Ordered correctly", firstEntityIndex < secondEntityIndex);

        assertTrue("Ordered correctly", secondEntityIndex < thirdEntityIndex);
    }


    private int getEntityIndex(UUID entityId, Collection collection) {
        List<Entity> entities = collection.getResponse().getEntities();

        for (int i = 0; i < entities.size(); i++) {
            if (entityId.equals(entities.get(i).getUuid())) {
                return i;
            }
        }

        return -1;
    }


    @Test
    public void clientNameQuery() {


        String username = "username";
        String name = "name";

        User user = new User(username, name, username + "@usergrid.org", "password");

        Entity entity = usersResource.post(user);
        UUID createdId = entity.getUuid();

        refreshIndex();
        Collection results = usersResource.get(new QueryParameters().setQuery(String.format("name = '%s'", name)));
        entity = new User(results.getResponse().getEntities().get(0));
        assertEquals(createdId, entity.getUuid());
    }


    @Test
    public void deleteUser() throws IOException {

        UUID id = UUIDUtils.newTimeUUID();

        String username = "username" + id;
        String name = "name" + id;
        User entity = new User(username, name, id + "@usergrid.org", "password");

        entity = new User(usersResource.post(entity));

        UUID createdId = entity.getUuid();

        refreshIndex();

        Entity newEntity = usersResource.entity(createdId.toString()).get();

        userResource.entity(newEntity).delete();

        refreshIndex();

        Collection results = usersResource.get(
            new QueryParameters().setQuery(String.format("username = '%s'", username)));
        assertEquals(0, results.getResponse().getEntities().size());

        // now create that same user again, it should work
        entity = new User(usersResource.post(entity));

        createdId = entity.getUuid();

        assertNotNull(createdId);
    }


    @Test
    public void singularCollectionName() throws IOException {

        String username = "username1";
        String name = "name1";
        String email = "email1" + "@usergrid.org";

        User entity = new User(username, name, email, "password");

        entity = new User(usersResource.post(entity));
        refreshIndex();

        UUID firstCreatedId = entity.getUuid();
        username = "username2";
        name = "name2";
        email = "email2" + "@usergrid.org";

        entity = new User(username, name, email, "password");

        entity = new User(usersResource.post(entity));
        refreshIndex();

        UUID secondCreatedId = entity.getUuid();

        // now create a connection of "likes" between the first user and the
        // second using pluralized form

        // plural collection name

        Entity conn1 = usersResource.entity(
            firstCreatedId.toString()).connection("conn1").entity(secondCreatedId.toString()).post();

        assertEquals(secondCreatedId.toString(), conn1.getUuid().toString());

        refreshIndex();


        Entity conn2 = usersResource.entity(
            firstCreatedId.toString()).connection("conn2").entity(secondCreatedId.toString()).post();

        assertEquals(secondCreatedId.toString(), conn2.getUuid().toString());

        refreshIndex();

        Collection conn1Connections = usersResource.entity(firstCreatedId.toString()).connection("conn1").get();

        assertEquals(secondCreatedId.toString(),
            ((Entity) conn1Connections.getResponse().getEntities().get(0)).getUuid().toString());

        conn1Connections = userResource.entity(firstCreatedId.toString()).connection("conn1").get();

        assertEquals(secondCreatedId.toString(),
            ((Entity) conn1Connections.getResponse().getEntities().get(0)).getUuid().toString());

        Collection conn2Connections = usersResource.entity(firstCreatedId.toString()).connection("conn1").get();

        assertEquals(secondCreatedId.toString(),
            ((Entity) conn2Connections.getResponse().getEntities().get(0)).getUuid().toString());

        conn2Connections = userResource.entity(firstCreatedId.toString()).connection("conn1").get();

        assertEquals(secondCreatedId.toString(),
            ((Entity) conn2Connections.getResponse().getEntities().get(0)).getUuid().toString());
    }


    @Test
    public void connectionByNameAndType() throws IOException {
        UUID id = UUIDUtils.newTimeUUID();

        String username1 = "username1";
        String name1 = "name1";
        String email1 = "email1" + "@usergrid.org";

        User entity = new User(username1, name1, email1, "password");

        entity = new User(usersResource.post(entity));

        UUID firstCreatedId = entity.getUuid();

        String username2 = "username2";
        String name2 = "name2";
        String email2 = "email2" + "@usergrid.org";

        entity = new User(username2, name2, email2, "password");

        entity = new User(usersResource.post(entity));

        UUID secondCreatedId = entity.getUuid();

        // now create a connection of "likes" between the first user and the
        // second using pluralized form
        refreshIndex();

        // named entity in collection name
        Entity conn1 = usersResource.entity(firstCreatedId.toString()).connection("conn1", "users")
            .entity(secondCreatedId.toString()).post();

        assertEquals(secondCreatedId.toString(), conn1.getUuid().toString());

        // named entity in collection name

        Entity conn2 = usersResource.entity(username1).connection("conn2", "users").entity(username2).post();

        assertEquals(secondCreatedId.toString(), conn2.getUuid().toString());
    }


    /**
     * Usergrid-1222 test
     */
    @Test
    public void connectionQuerybyEmail() throws IOException {
        UUID id = UUIDUtils.newTimeUUID();

        String name = "name1" + id;
        String email = "email1" + id + "@usergrid.org";

        User entity = new User(email, name, email, "password");

        entity = new User(usersResource.post(entity));

        UUID userId = entity.getUuid();

        Entity role = new Entity();
        role.put("name", "connectionQuerybyEmail1");

        role = this.app().collection("roles").post(role);

        UUID roleId1 = role.getUuid();

        //add permissions to the role

        Map<String, Object> perms = new HashMap<>();
        perms.put("permission", "get:/stuff/**");


        Entity perms1 = this.app().collection("roles").entity(roleId1.toString()).connection("permissions")
            .post(new Entity(perms));


        //Create the second role
        role = new Entity();
        role.put("name", "connectionQuerybyEmail2");
        role = this.app().collection("roles").post(role);


        UUID roleId2 = role.getUuid();

        //add permissions to the role

        perms = new HashMap<>();
        perms.put("permission", "get:/stuff/**");
        Entity perms2 = this.app().collection("roles").entity(roleId2.toString()).connection("permissions")
            .post(new Entity(perms));
        refreshIndex();
        //connect the entities where role is the root
        Entity perms3 = this.app().collection("roles").entity(roleId1.toString()).connection("users")
            .entity(userId.toString()).post();

        // now create a connection of "likes" between the first user and the
        // second using pluralized form

        assertEquals(userId.toString(), perms3.getUuid().toString());


        //connect the second role

        Entity perms4 = this.app().collection("roles").entity(roleId2).connection("users").entity(userId).post();

        assertEquals(userId.toString(), perms4.getUuid().toString());

        refreshIndex();
        //query the second role, it should work
        Collection userRoles = this.app().collection("roles").entity(roleId2).connection("users")
            .get(new QueryParameters().setQuery("select%20*%20where%20username%20=%20'" + email + "'"));
        assertEquals(userId.toString(), ((Entity) userRoles.iterator().next()).getUuid().toString());


        //query the first role, it should work
        userRoles = this.app().collection("roles").entity(roleId1).connection("users")
            .get(new QueryParameters().setQuery("select%20*%20where%20username%20=%20'" + email + "'"));
        assertEquals(userId.toString(), ((Entity) userRoles.iterator().next()).getUuid().toString());


        //now delete the first role

        this.app().collection("roles").entity(roleId1).delete();

        //query the first role, it should 404
        try {
            userRoles = this.app().collection("roles").entity(roleId1).connection("users")
                .get(new QueryParameters().setQuery("select%20*%20where%20username%20=%20'" + email + "'"));
            assertNull(userRoles);
        } catch (ClientErrorException e) {
            assertEquals( Response.Status.NOT_FOUND.getStatusCode(), e.getResponse().getStatus());
        }

        //query the second role, it should work
        userRoles = this.app().collection("roles").entity(roleId2).connection("users")
            .get(new QueryParameters().setQuery("select%20*%20where%20username%20=%20'" + email + "'"));

        assertEquals(userId.toString(), userRoles.getResponse().getEntities().get(0).getUuid().toString());
    }


    @Test
    public void connectionByNameAndDynamicType() throws IOException {
        UUID id = UUIDUtils.newTimeUUID();

        String username1 = "username1" + id;
        String name1 = "name1" + id;
        String email1 = "email1" + id + "@usergrid.org";
        User entity = new User(username1, name1, email1, "password");

        entity = new User(usersResource.post(entity));

        UUID firstCreatedId = entity.getUuid();

        String name = "pepperoni";

        Entity pizza = new Entity();
        pizza.put("name", name);
        pizza.put("type", "pizza");

        Entity pizzaEntity = this.app().collection("pizzas").post(pizza);

        UUID secondCreatedId = pizzaEntity.getUuid();
        refreshIndex();

        // now create a connection of "likes" between the first user and the
        // second using pluralized form

        // named entity in collection name
        Entity conn1 = usersResource.entity(firstCreatedId).connection("conn1").collection("pizzas")
            .entity(secondCreatedId).post();

        assertEquals(secondCreatedId.toString(), conn1.getUuid().toString());

        // named entity in collection name
        Entity conn2 = usersResource.entity(username1).connection("conn2").collection("pizzas").entity(name).post();


        assertEquals(secondCreatedId.toString(), conn2.getUuid().toString());
    }


    @Test
    public void nameUpdate() throws IOException {
        UUID id = UUIDUtils.newTimeUUID();

        String username = "username" + id;
        String name = "name" + id;
        String email = "email" + id + "@usergrid.org";
        User entity = new User(username, name, email, "password");

        Entity userEntity = usersResource.post(entity);

        refreshIndex();
        // attempt to log in
        Token token = this.app().token().post(new Token(username, "password"));

        assertEquals(username, token.getUser().getUsername());
        assertEquals(name, token.getUser().getName());
        assertEquals(email, token.getUser().getEmail());

        // now update the name and email
        String newName = "newName";
        String newEmail = "newEmail" + UUIDUtils.newTimeUUID() + "@usergrid.org";

        userEntity.put("name", newName);
        userEntity.put("email", newEmail);
        userEntity.put("password", "newp2ssword");
        userEntity.put("pin", "newp1n");

        userEntity = usersResource.entity(username).put(userEntity);


        refreshIndex();
        // now see if we've updated


        token = this.app().token().post(new Token(username, "password"));


        assertEquals(username, token.getUser().getUsername());
        assertEquals(newName, token.getUser().getName());
        assertEquals(newEmail, token.getUser().getEmail());
        assertNull(token.getUser().get("password"));
        assertNull(newEmail, token.getUser().get("pin"));
    }


    @Test
    public void test_POST_batch() throws IOException {

        log.info("UserResourceIT.test_POST_batch");


        List<Entity> batch = new ArrayList<>();

        Entity properties = new Entity();
        properties.put("username", "test_user_1");
        properties.put("email", "user1@test.com");
        batch.add(properties);

        properties = new Entity();
        properties.put("username", "test_user_2");
        batch.add(properties);

        properties = new Entity();
        properties.put("username", "test_user_3");
        batch.add(properties);

        ApiResponse response = usersResource.post(batch);

        assertNotNull(response);
    }


    @Test
    public void deactivateUser() throws IOException {

        UUID newUserUuid = UUIDUtils.newTimeUUID();

        String userName = String.format("test%s", newUserUuid);

        User entity =
                (User) new User(userName, "Ed Anuff", String.format("%s@anuff.com", newUserUuid), "sesame")
                    .chainPut("pin", "1234");

        usersResource.post(entity);
        refreshIndex();

        Collection response = usersResource.get();
        // disable the user
        Map<String, String> data = new HashMap<String, String>();
        Entity entityConn = usersResource.entity(userName).connection("deactivate").post(new Entity());

        assertFalse((boolean) entityConn.get("activated"));
        assertNotNull(entityConn.get("deactivated"));
    }


    @Test
    public void test_PUT_password_fail() {
        Entity entity = usersResource.post(new User("edanuff", "edanuff", "edanuff@email.com", "sesame"));
        this.app().token().post(new Token("edanuff", "sesame"));
        refreshIndex();
        boolean fail = false;
        try {
            Entity changeResponse = usersResource.entity("edanuff").collection("password")
                .post(new ChangePasswordEntity("foo", "bar"));
        } catch (Exception e) {
            fail = true;
        }
        assertTrue(fail);
    }


    @Test
    public void test_GET_user_ok() throws InterruptedException, IOException {

        // TODO figure out what is being overridden? why 400?
        Collection users = usersResource.get();

        String uuid = users.getResponse().getEntities().get(0).getUuid().toString();
        String email = users.getResponse().getEntities().get(0).get("email").toString();

        Entity user = usersResource.entity(uuid).get();

        assertEquals(email, user.get("email").toString());
    }


    @Test
    public void test_PUT_password_ok() {
        Entity entity = usersResource.post(new User("edanuff", "edanuff", "edanuff@email.com", "sesame"));
        refreshIndex();
        usersResource.entity(entity).collection("password").post(new ChangePasswordEntity("sesame", "sesame1"));

        refreshIndex();
        this.app().token().post(new Token("edanuff", "sesame1"));

        // if this was successful, we need to re-set the password for other
        // tests
        Entity changeResponse = usersResource.entity("edanuff").collection("password")
            .post(new ChangePasswordEntity("sesame1", "sesame"));
        refreshIndex();
        assertNotNull(changeResponse);

    }


    @Test
    public void setUserPasswordAsAdmin() throws IOException {
        usersResource.post(new User("edanuff", "edanuff", "edanuff@email.com", "sesame"));
        String newPassword = "foo";
        refreshIndex();

        // change the password as admin. The old password isn't required
        Entity node = usersResource.entity("edanuff").connection("password")
            .post(new ChangePasswordEntity(newPassword));
        assertNotNull(node);

        refreshIndex();
        Token response = this.app().token().post(new Token("edanuff", newPassword));
        assertNotNull(response);
    }


    @Test
    public void passwordMismatchErrorUser() {
        usersResource.post(new User("edanuff", "edanuff", "edanuff@email.com", "sesame"));

        String origPassword = "foo";
        String newPassword = "bar";

        ChangePasswordEntity data = new ChangePasswordEntity(origPassword, newPassword);

        int responseStatus = 0;
        try {
            usersResource.entity("edanuff").connection("password").post(data);
        } catch (ClientErrorException uie) {
            responseStatus = uie.getResponse().getStatus();
        }

        assertEquals(0, responseStatus);
    }


    @Test
    public void addRemoveRole() throws IOException {
        String roleName = "rolename";

        String username = "username";
        String name = "name";
        String email = "email" + "@usergrid.org";

        User user = new User(username, name, email, "password");
        user = new User(usersResource.post(user));
        UUID createdId = user.getUuid();

        // create Role

        Entity role = new Entity().chainPut("title", roleName).chainPut("name", roleName);
        this.app().collection("roles").post(role);
        // check it

        refreshIndex();
        // add Role

        role = usersResource.entity(createdId).collection("roles").entity(roleName).post();

        refreshIndex();
        // check it
        assertNotNull(role);
        assertNotNull(role.get("name"));
        assertEquals(role.get("name").toString(), roleName);

        role = usersResource.entity(createdId).collection("roles").entity(roleName).get();

        assertNotNull(role);
        assertNotNull(role.get("name"));
        assertEquals(role.get("name").toString(), roleName);

        // remove Role
        ApiResponse response = usersResource.entity(createdId).collection("roles").entity(roleName).delete();

        // check it

        try {
            role = usersResource.entity(createdId).collection("roles").entity(roleName).get();

            assertNull(role);
        } catch (ClientErrorException e) {
            assertEquals(e.getResponse().getStatus(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }


    @Test
    public void revokeToken() throws Exception {

        this.app().collection("users").post(new User("edanuff", "edanuff", "edanuff@email.com", "sesame"));
        refreshIndex();
        Token token1 = this.app().token().post(new Token("edanuff", "sesame"));
        Token token2 = this.app().token().post(new Token("edanuff", "sesame"));

        this.app().token().setToken(token1);
        Entity entity1 = usersResource.entity("edanuff").get();
        this.app().token().setToken(token2);
        Entity entity2 = usersResource.entity("edanuff").get();

        assertNotNull(entity1);

        assertNotNull(entity2);
        Token adminToken = this.clientSetup.getRestClient().management().token()
            .post( false, Token.class, new Token( clientSetup.getUsername(), clientSetup.getUsername() ), null );
        // now revoke the tokens
        this.app().token().setToken(adminToken);

        usersResource.entity("edanuff").connection("revoketokens").post(new Entity().chainPut("token", token1));
        refreshIndex();
        // the tokens shouldn't work

        int status = 0;

        try {
            this.app().token().setToken(token1);

            usersResource.entity("edanuff").get();
            assertFalse(true);
        } catch (ClientErrorException uie) {
            status = uie.getResponse().getStatus();
        }

        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), status);

        status = 0;

        try {
            this.app().token().setToken(token2);

            usersResource.entity("edanuff").get();
        } catch (ClientErrorException uie) {
            status = uie.getResponse().getStatus();
        }

        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), status);

        Token token3 = this.app().token().post(new Token("edanuff", "sesame"));
        Token token4 = this.app().token().post(new Token("edanuff", "sesame"));

        this.app().token().setToken(token3);
        entity1 = usersResource.entity("edanuff").get();


        assertNotNull(entity1);

        this.app().token().setToken(token3);
        entity2 = usersResource.entity("edanuff").get();

        assertNotNull(entity2);

        // now revoke the token3
        adminToken = this.clientSetup.getRestClient().management().token()
            .post( false, Token.class, new Token( clientSetup.getUsername(), clientSetup.getUsername() ), null );
        // now revoke the tokens
        this.app().token().setToken(adminToken);
        usersResource.entity("edanuff").connection("revoketokens").post();
        refreshIndex();

        // the token3 shouldn't work

        status = 0;

        try {
            this.app().token().setToken(token3);
            usersResource.entity("edanuff").get();

        } catch (ClientErrorException uie) {
            status = uie.getResponse().getStatus();
        }

        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), status);

        status = 0;

        try {
            this.app().token().setToken(token4);
            usersResource.entity("edanuff").get();


            status = Response.Status.OK.getStatusCode();
        } catch (ClientErrorException uie) {
            status = uie.getResponse().getStatus();
        }

        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), status);
    }


    @Test
    public void getToken() throws Exception {

        usersResource.post(new User("test_1", "Test1 User", "test_1@test.com", "test123")); // client.setApiUrl(apiUrl);
        usersResource.post(new User("test_2", "Test2 User", "test_2@test.com", "test123")); // client.setApiUrl(apiUrl);
        usersResource.post(new User("test_3", "Test3 User", "test_3@test.com", "test123")); // client.setApiUrl(apiUrl);
        refreshIndex();

        //Entity appInfo = this.app().get().getResponse().getEntities().get(0);

        Token token = this.app().token().post(new Token("test_1", "test123"));

        UUID userId = UUID.fromString(((Map<String, Object>) token.get("user")).get("uuid").toString());

        assertNotNull(token.getAccessToken());

        refreshIndex();

        int status = 0;

        // bad access token
        try {
            userResource.entity("test_1").connection("token").get(
                new QueryParameters().addParam("access_token", "blah"), false);
            assertTrue(false);
        } catch (ClientErrorException uie) {
            status = uie.getResponse().getStatus();
            log.info("Error Response Body: " + uie.getResponse().readEntity(String.class));
        }

        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), status);

        try {
            userResource.entity("test_2").connection("token").get(
                new QueryParameters().addParam("access_token", token.getAccessToken()), false);
            assertTrue(false);
        } catch (ClientErrorException uie) {
            status = uie.getResponse().getStatus();
            log.info("Error Response Body: " + uie.getResponse().readEntity(String.class));
        }

        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), status);


        String adminToken = this.getAdminToken().getAccessToken();
        Collection tokens = userResource.entity("test_1").connection("token").get(
            new QueryParameters().addParam("access_token", adminToken), false);


        assertTrue(tokens.getResponse().getProperties().get("user") != null);

        tokens = userResource.entity("test_1").connection("token").get(
            new QueryParameters().addParam("access_token", adminToken), false);

        assertTrue(tokens.getResponse().getProperties().get("user") != null);

        Entity entityConn = usersResource.entity(userId).connection("deactivate").post(new Entity());


        refreshIndex();

        try {
            this.app().token().post(new Token("test_1", "test123"));
            fail("request for deactivated user should fail");
        } catch (ClientErrorException uie) {
            status = uie.getResponse().getStatus();
            JsonNode body = mapper.readTree(uie.getResponse().readEntity(String.class));
            assertEquals("user not activated", body.findPath("error_description").textValue());
        }
    }


    @Test
    public void delegatePutOnNotFound() throws Exception {
        String randomName = "user1_" + UUIDUtils.newTimeUUID().toString();
        User user = new User(randomName, randomName, randomName + "@apigee.com", "password");
        usersResource.post(user);
        refreshIndex();

        // should update a field
        Entity response = usersResource.entity(randomName).get();
        assertNotNull(response);
        // PUT on user

        // PUT a new user
        randomName = "user2_" + UUIDUtils.newTimeUUID().toString();

        User user2 = (User) new User(randomName, randomName, randomName + "@apigee.com", "password").chainPut("pin", "1234");

        response = usersResource.post(user2);

        refreshIndex();

        Entity response2 = usersResource.entity(randomName).get();


        assertNotNull(response2);
    }


    /**
     * Test that property queries return properties and entity queries return entities.
     * https://apigeesc.atlassian.net/browse/USERGRID-1715?
     */
    @Test
    public void queryForUuids() throws Exception {

        {
            final Collection response = usersResource.get(new QueryParameters().setQuery("select *"));
            assertNotNull("Entities must exist", response.getResponse().getEntities());
            assertTrue("Must be some entities", response.getResponse().getEntities().size() > 0);
            assertEquals("Must be users", "user", response.getResponse().getEntities().get(0).get("type").toString());
        }

        {
            final Collection response = usersResource.get(new QueryParameters().setQuery("select uuid"));

            assertNotNull("List must exist", response.getResponse().getEntities());
            assertTrue("Must be some list items", response.getResponse().getEntities().size() > 0);
            assertTrue("Should have 4 items - [metadata, type, uuid]", response.getResponse().getEntities().get(0).keySet().size() == 3);

        }
    }


    @Test
    public void queryForUserUuids() throws Exception {


        int status = 0;


        String ql = "uuid = " + userRepo.getByUserName("user1");

        usersResource.get(new QueryParameters().setQuery(ql));

        Entity payload = new Entity().chainPut("name", "Austin").chainPut("state", "TX");

        Entity responseEntity = this.app().collection("curts").post(payload);

        UUID userId = UUID.fromString(responseEntity.getUuid().toString());

        assertNotNull(userId);

        refreshIndex();

        ql = "uuid = " + userId;

        Collection response = this.app().collection("curts").get(new QueryParameters().setQuery(ql));

        assertEquals(response.getResponse().getEntities().get(0).get("uuid").toString(), userId.toString());
    }
}
