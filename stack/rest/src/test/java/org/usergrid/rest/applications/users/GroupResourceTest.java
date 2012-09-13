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

import java.util.UUID;

import org.codehaus.jackson.JsonNode;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.java.client.Client.Query;
import org.usergrid.java.client.entities.Entity;
import org.usergrid.java.client.entities.User;
import org.usergrid.java.client.response.ApiResponse;
import org.usergrid.rest.AbstractRestTest;
import org.usergrid.utils.UUIDUtils;

import javax.ws.rs.core.MediaType;

import static org.junit.Assert.*;

/**
 * @author tnine
 */
public class GroupResourceTest extends AbstractRestTest {
    private static Logger log = LoggerFactory
            .getLogger(GroupResourceTest.class);

    private static final String GROUP = "testGroup";

    private static final String USER = "edanuff";

    private static boolean groupCreated = false;

    public GroupResourceTest() throws Exception {

    }

    @Before
    public void setupGroup() {
        if (groupCreated) {
            return;
        }

        client.createGroup(GROUP);

        groupCreated = true;
    }

    @Ignore
    public void failGroupNameValidation() {
      ApiResponse response = client.createGroup("groupName/withslash");
      assertNotNull(response.getError());

      response = client.createGroup("groupName withspace");
      assertNotNull(response.getError());
    }

    @Test
    public void postGroupActivity() {

        // don't populate the user, it will use the currently authenticated
        // user.

        UUID id = UUIDUtils.newTimeUUID();

        String groupPath = "groupPath" + id;
        String groupTitle = "groupTitle " + id;
        String groupName = "groupName" + id;

        ApiResponse response = client.createGroup(groupPath, groupTitle,
                groupName);

        assertNull("Error was: " + response.getErrorDescription(),
                response.getError());

        UUID newId = response.getEntities().get(0).getUuid();

        Query results = client.queryGroups(String
                .format("name='%s'", groupName));

        response = results.getResponse();

        UUID entityId = response.getEntities().get(0).getUuid();

        assertEquals(newId, entityId);

        results = client.queryGroups(String.format("title='%s'", groupTitle));

        response = results.getResponse();

        entityId = response.getEntities().get(0).getUuid();

        assertEquals(newId, entityId);
        
        results = client.queryGroups(String.format("title contains '%s'", id));

        response = results.getResponse();

        entityId = response.getEntities().get(0).getUuid();

        assertEquals(newId, entityId);

        results = client.queryGroups(String.format("path='%s'", groupPath));

        response = results.getResponse();

        entityId = response.getEntities().get(0).getUuid();

        assertEquals(newId, entityId);

    }

  @Test
  public void addRemovePermission() {

    UUID id = UUIDUtils.newTimeUUID();

    String groupName = "groupname" + id;

    ApiResponse response = client.createGroup(groupName);
    assertNull("Error was: " + response.getErrorDescription(), response.getError());

    UUID createdId = response.getEntities().get(0).getUuid();

    // add Permission

    String json = "{\"permission\":\"delete:/test\"}";
    JsonNode node = resource()
            .path("/test-organization/test-app/groups/" + createdId + "/permissions")
            .queryParam("access_token", access_token)
            .accept(MediaType.APPLICATION_JSON)
            .type(MediaType.APPLICATION_JSON_TYPE)
            .post(JsonNode.class, json);

    // check it
    assertNull(node.get("errors"));
    assertEquals(node.get("data").get(0).asText(), "delete:/test");

    node = resource()
            .path("/test-organization/test-app/groups/" + createdId + "/permissions")
            .queryParam("access_token", access_token)
            .accept(MediaType.APPLICATION_JSON)
            .type(MediaType.APPLICATION_JSON_TYPE)
            .get(JsonNode.class);
    assertNull(node.get("errors"));
    assertEquals(node.get("data").get(0).asText(), "delete:/test");


    // remove Permission

    node = resource()
            .path("/test-organization/test-app/groups/" + createdId + "/permissions")
            .queryParam("access_token", access_token)
            .queryParam("permission", "delete%3A%2Ftest")
            .accept(MediaType.APPLICATION_JSON)
            .type(MediaType.APPLICATION_JSON_TYPE)
            .delete(JsonNode.class);

    // check it
    assertNull(node.get("errors"));
    assertTrue(node.get("data").size() == 0);

    node = resource()
            .path("/test-organization/test-app/groups/" + createdId + "/permissions")
            .queryParam("access_token", access_token)
            .accept(MediaType.APPLICATION_JSON)
            .type(MediaType.APPLICATION_JSON_TYPE)
            .get(JsonNode.class);
    assertNull(node.get("errors"));
    assertTrue(node.get("data").size() == 0);
  }


  @Test
  public void addRemoveRole() {

    UUID id = UUIDUtils.newTimeUUID();

    String groupName = "groupname" + id;
    String roleName = "rolename" + id;

    ApiResponse response = client.createGroup(groupName);
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
            .path("/test-organization/test-app/groups/" + createdId + "/roles/" + roleName)
            .queryParam("access_token", access_token)
            .accept(MediaType.APPLICATION_JSON)
            .type(MediaType.APPLICATION_JSON_TYPE)
            .post(JsonNode.class);

    // check it
    assertNull(node.get("errors"));
    assertEquals(node.get("entities").get(0).get("name").asText(), roleName);

    node = resource()
            .path("/test-organization/test-app/groups/" + createdId + "/roles")
            .queryParam("access_token", access_token)
            .accept(MediaType.APPLICATION_JSON)
            .type(MediaType.APPLICATION_JSON_TYPE)
            .get(JsonNode.class);
    assertNull(node.get("errors"));
    assertEquals(node.get("entities").get(0).get("name").asText(), roleName);


    // remove Role

    node = resource()
            .path("/test-organization/test-app/groups/" + createdId + "/roles/" + roleName)
            .queryParam("access_token", access_token)
            .accept(MediaType.APPLICATION_JSON)
            .type(MediaType.APPLICATION_JSON_TYPE)
            .delete(JsonNode.class);

    // check it
    assertNull(node.get("errors"));

    node = resource()
            .path("/test-organization/test-app/groups/" + createdId + "/roles")
            .queryParam("access_token", access_token)
            .accept(MediaType.APPLICATION_JSON)
            .type(MediaType.APPLICATION_JSON_TYPE)
            .get(JsonNode.class);
    assertNull(node.get("errors"));
    assertTrue(node.get("entities").size() == 0);
  }

}
