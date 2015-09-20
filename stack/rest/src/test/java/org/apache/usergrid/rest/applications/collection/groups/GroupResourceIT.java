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
package org.apache.usergrid.rest.applications.collection.groups;


import com.fasterxml.jackson.databind.JsonNode;
import org.apache.usergrid.rest.test.resource.AbstractRestIT;
import org.apache.usergrid.rest.test.resource.model.Collection;
import org.apache.usergrid.rest.test.resource.model.Entity;
import org.junit.Ignore;
import org.junit.Test;

import javax.ws.rs.ClientErrorException;
import java.io.IOException;

import static org.junit.Assert.*;

/**
 * @author rockerston
 *
 * REST tests for /groups endpoint
 *
 * */

public class GroupResourceIT extends AbstractRestIT {

    public GroupResourceIT() throws Exception { }

    /***
     *
     * helper method to create a group
     *
     */
    private Entity createGroup(String groupName, String groupPath) throws IOException{
        String title = "title";
        return createGroup(groupName, groupPath, title);
    }
    private Entity createGroup(String groupName, String groupPath, String groupTitle) throws IOException{
        Entity payload = new Entity();
        payload.put("name", groupName);
        payload.put("path", groupPath);
        payload.put("title", groupTitle);
        Entity entity = this.app().collection("groups").post(payload);
        assertEquals(entity.get("name"), groupName);
        assertEquals(entity.get("path"), groupPath);
        this.refreshIndex();
        return entity;
    }

    /***
     *
     * helper method to create a role
     *
     */
    private Entity createRole(String roleName, String roleTitle) throws IOException{
        Entity payload = new Entity();
        payload.put("name", roleName);
        payload.put("title", roleTitle);
        Entity entity = this.app().collection("roles").post(payload);
        assertEquals(entity.get("name"), roleName);
        assertEquals(entity.get("title"), roleTitle);
        this.refreshIndex();
        return entity;
    }

    /***
     *
     * helper method to create an app level user
     *
     */
    private Entity createUser(String username, String email, String password) throws IOException{
        Entity payload = new Entity();
        payload.put("username", username);
        payload.put("email", email);
        payload.put("password", password);
        Entity entity = this.app().collection("users").post(payload);
        assertEquals(entity.get("username"), username);
        assertEquals(entity.get("email"), email);
        this.refreshIndex();
        return entity;
    }

    /***
     *
     * Verify that we can create a group with a standard string in the name and path
     *
     */
    @Test()
    public void createGroupValidation() throws IOException {

        String groupName = "testgroup";
        String groupPath = "testgroup";
        this.createGroup( groupName, groupPath );

    }

    /***
     *
     * Verify that we can create a group with a slash in the name and path
     *
     */
    @Test()
    public void createGroupSlashInNameAndPathValidation() throws IOException {

        String groupNameSlash = "test/group";
        String groupPathSlash = "test/group";
        this.createGroup(groupNameSlash, groupPathSlash);

    }

    /***
     *
     * Verify that we can create a group with a space in the name
     *
     */
    @Test()
    public void createGroupSpaceInNameValidation() throws IOException {

        String groupSpaceName = "test group";
        String groupPath = "testgroup";
        this.createGroup(groupSpaceName, groupPath);

    }

    /***
     *
     * Verify that we cannot create a group with a space in the path
     *
     */
    @Test()
    public void createGroupSpaceInPathValidation() throws IOException {

        String groupName = "testgroup";
        String groupSpacePath = "test group";
        try {
            this.createGroup(groupName, groupSpacePath);
            fail("Should not be able to create a group with a space in the path");
        } catch (ClientErrorException e) {
            //verify the correct error was returned
            JsonNode node = mapper.readTree( e.getResponse().readEntity( String.class ));
            assertEquals( "illegal_argument", node.get( "error" ).textValue() );
        }

    }

    /***
     *
     * Verify that we can create a group, change the name, then delete it
     *
     */
    @Test()
    public void groupCRUDOperations() throws IOException {

        //1. create a group
        String groupName = "testgroup";
        String groupPath = "testgroup";
        Entity group = this.createGroup(groupName, groupPath);

        //2. do a GET to verify the property really was set
        Entity groupResponseGET = this.app().collection("groups").entity(group).get();
        assertEquals(groupResponseGET.get("path"), groupPath);

        //3. change the name
        String newGroupPath = "newtestgroup";
        group.put("path", newGroupPath);
        Entity groupResponse = this.app().collection("groups").entity(group).put(group);
        assertEquals(groupResponse.get("path"), newGroupPath);
        this.refreshIndex();

        //4. do a GET to verify the property really was set
        groupResponseGET = this.app().collection("groups").entity(group).get();
        assertEquals(groupResponseGET.get("path"), newGroupPath);

        //5. now delete the group
        this.app().collection("groups").entity(group).delete();

        //6. do a GET to make sure the entity was deleted
        try {
            this.app().collection("groups").uniqueID(groupName).get();
            fail("Entity still exists");
        } catch (ClientErrorException e) {
            //verify the correct error was returned
            JsonNode node = mapper.readTree( e.getResponse().readEntity( String.class ));
            assertEquals( "entity_not_found", node.get( "error" ).textValue() );
        }

    }

    /***
     *
     * Verify that we can create a group, user, add user to group, delete connection
     *
     */
    @Test()
    public void addRemoveUserGroup() throws IOException {

        //1. create a group
        String groupName = "testgroup";
        String groupPath = "testgroup";
        Entity group = this.createGroup(groupName, groupPath);

        // 2. create a user
        String username = "fred";
        String email = "fred@usergrid.com";
        String password = "password";
        Entity user = this.createUser(username, email, password);

        // 3. add the user to the group
        Entity response = this.app().collection("users").entity(user).connection().collection("groups").entity(group).post();
        assertEquals(response.get("name"), groupName);
        this.refreshIndex();

        // 4. make sure the user is in the group
        Collection collection = this.app().collection("groups").entity(group).connection().collection("users").get();
        Entity entity = collection.next();
        assertEquals(entity.get("username"), username);

        //5. try it the other way around
        collection = this.app().collection("users").entity(user).connection().collection("groups").get();
        entity = collection.next();
        assertEquals(entity.get("name"), groupName);

        //6. remove the user from the group
        this.app().collection("group").entity(group).connection().collection("users").entity(user).delete();
        this.refreshIndex();

        //6. make sure the connection no longer exists
        collection = this.app().collection("group").entity(group).connection().collection("users").get();
        assertEquals(collection.hasNext(), false);

        //8. do a GET to make sure the user still exists and did not get deleted with the collection delete
        Entity userEntity = this.app().collection("user").entity(user).get();
        assertEquals(userEntity.get("username"), username);

    }

    /***
     *
     * Verify that we can create a group, role, add role to group, delete connection
     *
     */
    @Test
    public void addRemoveRoleGroup() throws Exception {

        //1. create a group
        String groupName = "testgroup";
        String groupPath = "testgroup";
        Entity group = this.createGroup(groupName, groupPath);

        //2. create a role
        String roleName = "tester";
        String roleTitle = "tester";
        Entity role = this.createRole(roleName, roleTitle);
        this.refreshIndex();

        //3. add role to the group
        Entity response = this.app().collection("roles").entity(role).connection().collection("groups").entity(group).post();
        assertEquals(response.get("name"), groupName);
        this.refreshIndex();

        //4. make sure the role is in the group
        Collection collection = this.app().collection("groups").entity(group).connection().collection("roles").get();
        Entity entity = collection.next();
        assertEquals(entity.get("name"), roleName);

        //5. remove Role from the group (should only delete the connection)
        this.app().collection("groups").entity(group).connection().collection("roles").entity(role).delete();
        this.refreshIndex();

        //6. make sure the connection no longer exists
        collection = this.app().collection("groups").entity(group).connection().collection("roles").get();
        if (collection.hasNext()) {
            fail("Entity still exists");
        }

        //7. check root roles to make sure role still exists
        role = this.app().collection("roles").uniqueID(roleName).get();
        assertEquals(role.get("name"), roleName);

        //8. delete the role
        this.app().collection("role").entity(role).delete();
        this.refreshIndex();
        Thread.sleep(5000);

        //9. do a GET to make sure the role was deleted
        try {
            Entity entity1 = this.app().collection("role").entity(role).get();
            fail("Entity still exists");
        } catch (ClientErrorException e) {
            //verify the correct error was returned
            JsonNode node = mapper.readTree( e.getResponse().readEntity( String.class ));
            assertEquals( "entity_not_found", node.get( "error" ).textValue() );
        }

    }


    /***
     *
     * Verify that group / role permissions work
     *
     */
    @Test()
    public void addRolePermissionToGroupVerifyPermission() throws IOException {

        //1. create a group
        String groupName = "testgroup";
        String groupPath = "testgroup";
        Entity group = this.createGroup(groupName, groupPath);

        //2. create a user
        String username = "fred";
        String email = "fred@usergrid.com";
        String password = "password";
        Entity user = this.createUser(username, email, password);

        //3. create a role
        String roleName = "tester";
        String roleTitle = "tester";
        Entity role = this.createRole(roleName, roleTitle);

        //4. add permissions to role
        Entity payload = new Entity();
        payload.put("permission","get,post:/cats/*");
        Entity permission = this.app().collection("roles").uniqueID(roleName).connection("permissions").post(payload);
        assertEquals(permission.get("data"), "get,post:/cats/*");

        //5. add role to the group
        Entity addRoleResponse = this.app().collection("groups").entity(group).connection().collection("roles").entity(role).post();
        assertEquals(addRoleResponse.get("name"), roleName);

        //6. add user to group
        Entity addUserResponse = this.app().collection("users").entity(user).connection().collection("groups").entity(group).post();
        assertEquals(addUserResponse.get("name"), groupName);

        //7. delete the default role
        this.app().collection("role").uniqueID("Default").delete();

        //8. log user in, should then be using the app user's token not the admin token
        this.getAppUserToken(username, password);
        //9. create a cat - permissions should allow this
        String catName = "fluffy";
        payload = new Entity();
        payload.put("name", catName);
        Entity fluffy = this.app().collection("cats").post(payload);
        assertEquals(fluffy.get("name"), catName);
        this.refreshIndex();

        //10. get the cat - permissions should allow this
        fluffy = this.app().collection("cats").uniqueID(catName).get();
        assertEquals(fluffy.get("name"), catName);

        //11. edit the cat - permissions should not allow this
        fluffy.put("color", "brown");
        try {
            this.app().collection("cats").uniqueID(catName).put(fluffy);
            fail("permissions should not allow this");
        } catch (ClientErrorException e) {
            //verify the correct error was returned
            JsonNode node = mapper.readTree( e.getResponse().readEntity( String.class ) );
            assertEquals( "unauthorized", node.get( "error" ).textValue() );
        }

        //12. delete the cat - permissions should not allow this
        try {
            this.app().collection("cats").uniqueID(catName).delete();
            fail("permissions should not allow this");
        } catch (ClientErrorException e) {
            //verify the correct error was returned
            JsonNode node = mapper.readTree( e.getResponse().readEntity( String.class ));
            assertEquals( "unauthorized", node.get( "error" ).textValue() );
        }

    }


    /***
     *
     * Post a group activity and make sure it can be read back only by group members
     *
     */
    @Ignore("Fails. See todo in the test itself.")
    @Test
    public void postGroupActivity() throws IOException {


        //1. create a group
        String groupName = "testgroup";
        String groupPath = "testgroup";
        Entity group = this.createGroup(groupName, groupPath);

        //2. create user 1
        String user1Username = "fred";
        String user1Email = "fred@usergrid.com";
        String password = "password";
        Entity user1 = this.createUser(user1Username, user1Email, password);

        //3. create user 2
        String user2Username = "barney";
        String user2Email = "barney@usergrid.com";
        password = "password";
        Entity user2 = this.createUser(user2Username, user2Email, password);

        //4. create user 3
        String user3Username = "wilma";
        String user3Email = "wilma@usergrid.com";
        password = "password";
        Entity user3 = this.createUser(user3Username, user3Email, password);

        //5. add user1 to the group
        Entity addUser1Response = this.app().collection("users").entity(user1).connection().collection("groups").entity(group).post();
        assertEquals(addUser1Response.get("name"), groupName);

        //6. add user2 to the group
        Entity addUser2Response = this.app().collection("users").entity(user2).connection().collection("groups").entity(group).post();
        assertEquals(addUser2Response.get("name"), groupName);

        // user 3 does not get added to the group

        //7. get all the users in the groups
        this.refreshIndex();
        Collection usersInGroup = this.app().collection("groups").uniqueID(groupName).connection("users").get();
        assertEquals(usersInGroup.getResponse().getEntityCount(), 2);


        //8. post an activity to the group
        //JSON should look like this:
        //{'{"actor":{"displayName":"fred","uuid":"2b70e83a-8a3f-11e4-9716-235107bcadb1","username":"fred"},
        // "verb":"post","content":"content"}'
        Entity payload = new Entity();
        payload.put("displayName", "fred");
        payload.put("uuid", user1.get("uuid"));
        payload.put("username", "fred");
        Entity activity = new Entity();
        activity.put("actor", payload);
        activity.put("verb", "post");
        String content = "content";
        activity.put("content", content);
        Entity activityResponse = this.app().collection("groups").uniqueID(groupName).connection("activities").post(activity);
        assertEquals(activityResponse.get("content"), content);
        this.refreshIndex();

        //9. delete the default role
        this.app().collection("role").uniqueID("Default").delete();
/*
        //10. add permissions to group: {"permission":"get,post,put,delete:/groups/${group}/**"}'
        payload = new Entity();
        String permission = "get,post,put,delete:/groups/${group}/**";
        payload.put("permission",permission);
        Entity permissionResponse = this.app().collection("groups").entity(group).connection("permissions").post(payload);
        assertEquals(permissionResponse.get("data"), permission);
*/
        //11. log user1 in, should then be using the app user's token not the admin token
        this.getAppUserToken(user1Username, password);


        //TODO: next failing currently because permissions seem to be borked in the stack

        //12. make sure the activity appears in the feed of user 1
        Collection user1ActivityResponse = this.app().collection("groups").entity(group).connection("activities").get();
        boolean found = false;
        while (user1ActivityResponse.hasNext()) {
            Entity tempActivity = user1ActivityResponse.next();
            if (tempActivity.get("type").equals("activity") && tempActivity.get("content").equals(content)) {
                found = true;
            }
        }
        assertTrue(found);

        //13. log user2 in, should then be using the app user's token not the admin token
        this.getAppUserToken(user2Username, password);

        //14. make sure the activity appears in the feed of user 2
        Collection user2ActivityResponse = this.app().collection("groups").entity(group).connection("activities").get();
        found = false;
        while (user2ActivityResponse.hasNext()) {
            Entity tempActivity = user2ActivityResponse.next();
            if (tempActivity.get("type").equals("activity") && tempActivity.get("content").equals(content)) {
                found = true;
            }
        }
        assertTrue(found);

        //15. log user3 in, should then be using the app user's token not the admin token
        this.getAppUserToken(user3Username, password);

        //16. make sure the activity does not appear in the feed of user 3, since they are not part of the group
        Collection user3ActivityResponse = this.app().collection("groups").entity(group).connection("activities").get();
        found = false;
        while (user3ActivityResponse.hasNext()) {
            Entity tempActivity = user3ActivityResponse.next();
            if (tempActivity.get("type").equals("activity") && tempActivity.get("content").equals("content")) {
                found = true;
            }
        }
        assertFalse(found);


    }

    public void updateGroupWithSameNameAsApp() throws IOException {


        // create groupMap with same name as app
        String groupPath = this.clientSetup.getAppName();
        String groupName = this.clientSetup.getAppName();
        String title = "Old Title";
        Entity group = this.createGroup(groupName, groupPath, title);

        String newTitle = "New Title";
        group.put("title", newTitle);
        Entity groupResponse = this.app().collection("groups").entity(group).put(group);
        assertEquals(groupResponse.get("title"), newTitle);
        this.refreshIndex();

        // update that group by giving it a new title and using UUID in URL
        String evenNewerTitle = "Even New Title";
        group.put("title", newTitle);
        String uuid = group.getAsString("uuid");
        groupResponse = this.app().collection("groups").uniqueID(uuid).put(group);
        assertEquals(groupResponse.get("title"), evenNewerTitle);
        this.refreshIndex();



    }


}
