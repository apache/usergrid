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
import static org.junit.Assert.fail;
import static org.usergrid.utils.MapUtils.hashMap;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.core.MediaType;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.junit.Test;
import org.usergrid.java.client.entities.Group;
import org.usergrid.management.ApplicationInfo;
import org.usergrid.management.OrganizationOwnerInfo;
import org.usergrid.rest.AbstractRestTest;
import org.usergrid.utils.UUIDUtils;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.api.client.UniformInterfaceException;

/**
 * Tests permissions of adding and removing users from roles as well as groups
 * 
 * @author tnine
 */
public class PermissionsResourceTest extends AbstractRestTest {

    private static final String ROLE = "permtestrole";

    private static final String USER = "edanuff";

    public PermissionsResourceTest() throws Exception {

    }

    @Test
    public void deleteUserFromRole() {
        Map<String, String> data = hashMap("name", ROLE);

        JsonNode node = resource().path("/test-organization/test-app/roles").queryParam("access_token", access_token)
                .accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON_TYPE).post(JsonNode.class, data);

        assertNull(node.get("error"));

        assertEquals(ROLE, getEntity(node, 0).get("name").asText());

        // add the user to the role
        node = resource().path("/test-organization/test-app/roles/" + ROLE + "/users/" + USER)
                .queryParam("access_token", access_token).accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).post(JsonNode.class);

        assertNull(node.get("error"));

        // now check the user has the role
        node = resource().path("/test-organization/test-app/users/" + USER + "/roles")
                .queryParam("access_token", access_token).accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).get(JsonNode.class);

        // check if the role was assigned
        assertEquals(ROLE, getEntity(node, 0).get("name").asText());

        // now delete the role
        node = resource().path("/test-organization/test-app/users/" + USER + "/roles/" + ROLE)
                .queryParam("access_token", access_token).accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).delete(JsonNode.class);

        // check if the role was deleted

        node = resource().path("/test-organization/test-app/users/" + USER + "/roles")
                .queryParam("access_token", access_token).accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).get(JsonNode.class);

        // check if the role was assigned
        assertNull(getEntity(node, 0));

    }

    @Test
    public void deleteUserGroup() {

        // don't populate the user, it will use the currently authenticated
        // user.

        UUID id = UUIDUtils.newTimeUUID();

        String groupPath = "groupPath" + id;

        Map<String, String> data = hashMap("type", "group").map("path", groupPath);

        JsonNode node = resource().path("/test-organization/test-app/groups").queryParam("access_token", access_token)
                .accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON_TYPE).post(JsonNode.class, data);

        assertNull(node.get("error"));

        node = resource().path("/test-organization/test-app/groups/" + groupPath + "/users/" + USER)
                .queryParam("access_token", access_token).accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).post(JsonNode.class);

        assertNull(node.get("error"));

        Map<String, Group> groups = client.getGroupsForUser(USER);

        assertNotNull(groups.get(groupPath));

        // now delete the group

        node = resource().path("/test-organization/test-app/groups/" + groupPath + "/users/" + USER)
                .queryParam("access_token", access_token).accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).delete(JsonNode.class);

        assertNull(node.get("error"));

        groups = client.getGroupsForUser(USER);

        assertNull(groups.get(groupPath));

    }

    @Test
    public void dictionaryPermissions() throws Exception{
      UUID id = UUIDUtils.newTimeUUID();

      String applicationName = "testapp";
      String orgname = "dictionaryPermissions";
      String username = "permissionadmin" + id;
      String password = "password";
      String email = String.format("email%s@usergrid.com", id);

      OrganizationOwnerInfo orgs = managementService.createOwnerAndOrganization(orgname, username, "noname", email,
              password, true, false);

      // create the app
      ApplicationInfo appInfo = managementService
              .createApplication(orgs.getOrganization().getUuid(), applicationName);
      
      String adminToken = managementService.getAccessTokenForAdminUser(orgs.getOwner().getUuid(), 0);
      
      // now create the new role
      Map<String, String> data = hashMap("name", "roleadmin");

      
      JsonNode node = resource().path(String.format("/%s/%s/roles", orgname, applicationName))
          .queryParam("access_token", adminToken).accept(MediaType.APPLICATION_JSON)
          .type(MediaType.APPLICATION_JSON_TYPE).post(JsonNode.class, data);
      
      assertNull(getError(node));

      // add the perms to the roleadmin to allow users in the role to create roles themselves
      addPermission(orgname, applicationName, adminToken, "roleadmin", "get,put,post:/roles/**");
      

      //create an application user
      UUID userId = createRoleUser(orgs.getOrganization().getUuid(), appInfo.getId(), adminToken, "roleadminuser",
              "roleadminuser@usergrid.com");

      // grant this application user the "roleadmin" role
      node = resource().path(String.format("/%s/%s/users/roleadminuser/roles/roleadmin", orgname, applicationName))
              .queryParam("access_token", adminToken).accept(MediaType.APPLICATION_JSON)
              .type(MediaType.APPLICATION_JSON_TYPE).post(JsonNode.class);

      assertNull(getError(node));

      //get the app users token
      String reviewer1Token = managementService.getAccessTokenForAppUser(appInfo.getId(), userId, 0);
      

      data = hashMap("name", "usercreatedrole");

      //create a role as the user
      node = resource().path(String.format("/%s/%s/roles", orgname, applicationName))
          .queryParam("access_token", reviewer1Token).accept(MediaType.APPLICATION_JSON)
          .type(MediaType.APPLICATION_JSON_TYPE).post(JsonNode.class, data);
      
      assertNull(getError(node));
      
      //now try to add permission as the user, this should work
      addPermission(orgname, applicationName, reviewer1Token, "usercreatedrole", "get,put,post:/foo/**");

    }
    
    /**
     * Tests a real world example with the following steps. Creates an
     * application.
     * 
     * Creates a new role "reviewer"
     * 
     * Grants a permission to GET, POST, and PUT the reviews url for the
     * reviewer role
     * 
     * Grants a permission GET on the reviewer for the
     * 
     * Create a user reviewer1 and add them to the reviewer role
     * 
     * Test access with reviewer1
     * 
     * Create a group reviewergroup and add the "reviewer" group to it
     * 
     * Create a user reviewer 2 and add them to the "reveiwergroup"
     */
    @Test
    public void applicationPermissions() throws Exception {
        UUID id = UUIDUtils.newTimeUUID();

        String applicationName = "test";
        String orgname = "applicationpermissions";
        String username = "permissionadmin" + id;
        String password = "password";
        String email = String.format("email%s@usergrid.com", id);

        OrganizationOwnerInfo orgs = managementService.createOwnerAndOrganization(orgname, username, "noname", email,
                password, true, false);

        // create the app
        ApplicationInfo appInfo = managementService
                .createApplication(orgs.getOrganization().getUuid(), applicationName);

        // now create the new role
        Map<String, String> data = hashMap("name", "reviewer");

        String adminToken = managementService.getAccessTokenForAdminUser(orgs.getOwner().getUuid(), 0);

        JsonNode node = resource().path(String.format("/%s/%s/roles", orgname, applicationName))
                .queryParam("access_token", adminToken).accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).post(JsonNode.class, data);

        assertNull(getError(node));

        // delete the default role to test permissions later
        node = resource().path(String.format("/%s/%s/roles/default", orgname, applicationName))
                .queryParam("access_token", adminToken).accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).delete(JsonNode.class);

        assertNull(getError(node));

        // grant the perms to reviewer
        addPermission(orgname, applicationName, adminToken, "reviewer", "get,put,post:/reviews/**");

        // grant get to guest
        addPermission(orgname, applicationName, adminToken, "guest", "get:/reviews/**");

        UUID userId = createRoleUser(orgs.getOrganization().getUuid(), appInfo.getId(), adminToken, "reviewer1",
                "reviewer1@usergrid.com");

        // grant this user the "reviewer" role
        node = resource().path(String.format("/%s/%s/users/reviewer1/roles/reviewer", orgname, applicationName))
                .queryParam("access_token", adminToken).accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).post(JsonNode.class);

        assertNull(getError(node));

        String reviewer1Token = managementService.getAccessTokenForAppUser(appInfo.getId(), userId, 0);

        Map<String, String> review = hashMap("rating", "4").map("name", "noca").map("review",
                "Excellent service and food");

        // post a review as the reviewer1 user
        resource().path(String.format("/%s/%s/reviews", orgname, applicationName))
                .queryParam("access_token", reviewer1Token).accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).post(JsonNode.class, review);

        review = hashMap("rating", "4").map("name", "4peaks").map("review", "Huge beer selection");

        // put a review as the reviewer1 user
        resource().path(String.format("/%s/%s/reviews", orgname, applicationName))
                .queryParam("access_token", reviewer1Token).accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).put(JsonNode.class, review);

        // get the reviews

        node = resource().path(String.format("/%s/%s/reviews", orgname, applicationName))
                .queryParam("access_token", reviewer1Token).accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).get(JsonNode.class);

        assertEquals("noca", getEntity(node, 0).get("name").asText());
        assertEquals("4peaks", getEntity(node, 1).get("name").asText());

        // can't delete, not in the grants

        ClientResponse.Status status = null;

        try {
            resource().path(String.format("/%s/%s/reviews/noca", orgname, applicationName))
                    .queryParam("access_token", reviewer1Token).accept(MediaType.APPLICATION_JSON)
                    .type(MediaType.APPLICATION_JSON_TYPE).delete(JsonNode.class);
        } catch (UniformInterfaceException uie) {
            status = uie.getResponse().getClientResponseStatus();
        }

        assertEquals(Status.UNAUTHORIZED, status);

        status = null;

        try {
            resource().path(String.format("/%s/%s/reviews/4peaks", orgname, applicationName))
                    .queryParam("access_token", reviewer1Token).accept(MediaType.APPLICATION_JSON)
                    .type(MediaType.APPLICATION_JSON_TYPE).delete(JsonNode.class);

        } catch (UniformInterfaceException uie) {
            status = uie.getResponse().getClientResponseStatus();
        }

        assertEquals(Status.UNAUTHORIZED, status);

        // now test some groups
        UUID secondUserId = createRoleUser(orgs.getOrganization().getUuid(), appInfo.getId(), adminToken, "reviewer2",
                "reviewer2@usergrid.com");

        Map<String, String> group = hashMap("path", "reviewergroup");

        // /now create the group
        resource().path(String.format("/%s/%s/groups", orgname, applicationName))
                .queryParam("access_token", adminToken).accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).post(JsonNode.class, group);

        // link the group to the role
        resource().path(String.format("/%s/%s/groups/reviewergroup/roles/reviewer", orgname, applicationName))
                .queryParam("access_token", adminToken).accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).post(JsonNode.class, group);

        // add the user to the group
        resource().path(String.format("/%s/%s/users/reviewer2/groups/reviewergroup", orgname, applicationName))
                .queryParam("access_token", adminToken).accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).post(JsonNode.class);

        // post 2 reviews. Should get permissions from the group

        String secondUserToken = managementService.getAccessTokenForAppUser(appInfo.getId(), secondUserId, 0);

        review = hashMap("rating", "4").map("name", "cowboyciao").map("review", "Great atmosphoere");

        // post a review as the reviewer2 user
        resource().path(String.format("/%s/%s/reviews", orgname, applicationName))
                .queryParam("access_token", secondUserToken).accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).post(JsonNode.class, review);

        review = hashMap("rating", "4").map("name", "currycorner").map("review", "Authentic");

        // post a review as the reviewer2 user
        resource().path(String.format("/%s/%s/reviews", orgname, applicationName))
                .queryParam("access_token", secondUserToken).accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).post(JsonNode.class, review);

        // get all reviews as a user
        node = resource().path(String.format("/%s/%s/reviews", orgname, applicationName))
                .queryParam("access_token", secondUserToken).accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).get(JsonNode.class);

        assertEquals("noca", getEntity(node, 0).get("name").asText());
        assertEquals("4peaks", getEntity(node, 1).get("name").asText());
        assertEquals("cowboyciao", getEntity(node, 2).get("name").asText());
        assertEquals("currycorner", getEntity(node, 3).get("name").asText());

        // issue a delete, it shouldn't work, no permissions

        status = null;

        try {
            resource().path(String.format("/%s/%s/reviews/cowboyciao", orgname, applicationName))
                    .queryParam("access_token", secondUserToken).accept(MediaType.APPLICATION_JSON)
                    .type(MediaType.APPLICATION_JSON_TYPE).delete(JsonNode.class);
        } catch (UniformInterfaceException uie) {
            status = uie.getResponse().getClientResponseStatus();
        }

        assertEquals(Status.UNAUTHORIZED, status);

        status = null;

        try {
            resource().path(String.format("/%s/%s/reviews/currycorner", orgname, applicationName))
                    .queryParam("access_token", secondUserToken).accept(MediaType.APPLICATION_JSON)
                    .type(MediaType.APPLICATION_JSON_TYPE).delete(JsonNode.class);
        } catch (UniformInterfaceException uie) {
            status = uie.getResponse().getClientResponseStatus();
        }

        assertEquals(Status.UNAUTHORIZED, status);

    }

    /**
     * Create the user, check there are no errors
     * 
     * @param orgname
     * @param appname
     * @param adminToken
     * @param username
     * @param email
     * 
     * @return the userid
     * @throws Exception
     */
    private UUID createRoleUser(UUID orgId, UUID appId, String adminToken, String username, String email)
            throws Exception {

        Map<String, String> props = hashMap("email", email).map("username", username).map("name", username)
                .map("password", "password");

        JsonNode node = resource().path(String.format("/%s/%s/users", orgId, appId))
                .queryParam("access_token", adminToken).accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).put(JsonNode.class, props);

        assertNull(getError(node));

        UUID userId = UUID.fromString(getEntity(node, 0).get("uuid").asText());

        // manually activate user
        managementService.activateAppUser(appId, userId);

        return userId;

    }

    /**
     * Test adding the permission to the role
     * 
     * @param orgname
     * @param appname
     * @param adminToken
     * @param rolename
     * @param grant
     */
    private void addPermission(String orgname, String appname, String adminToken, String rolename, String grant) {
        Map<String, String> props = hashMap("permission", grant);

        String rolePath = String.format("/%s/%s/roles/%s/permissions", orgname, appname, rolename);

        JsonNode node = resource().path(rolePath).queryParam("access_token", adminToken)
                .accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON_TYPE).put(JsonNode.class, props);

        assertNull(getError(node));

        node = resource().path(rolePath).queryParam("access_token", adminToken).accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).get(JsonNode.class);

        ArrayNode data = (ArrayNode) node.get("data");

        Iterator<JsonNode> iterator = data.getElements();

        while (iterator.hasNext()) {
            if (grant.equals(iterator.next().asText())) {
                return;
            }
        }

        fail(String.format("didn't find grant %s in the results", grant));
    }
}
