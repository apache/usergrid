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


import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.usergrid.utils.MapUtils.hashMap;

import java.util.Map;
import java.util.UUID;

import javax.ws.rs.core.MediaType;

import org.codehaus.jackson.JsonNode;
import org.junit.Test;
import org.usergrid.java.client.entities.Group;
import org.usergrid.rest.AbstractRestTest;
import org.usergrid.utils.UUIDUtils;

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

        JsonNode node = resource()
                .path("/test-organization/test-app/rolenames")
                .queryParam("access_token", access_token)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .post(JsonNode.class, data);

        assertNull(node.get("error"));

        assertNotNull(node.get("data").get(ROLE));

        // add the user to the role
        node = resource()
                .path("/test-organization/test-app/roles/" + ROLE + "/users/"
                        + USER).queryParam("access_token", access_token)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).post(JsonNode.class);

        assertNull(node.get("error"));

        // now check the user has the role
        node = resource()
                .path("/test-organization/test-app/users/" + USER
                        + "/rolenames")
                .queryParam("access_token", access_token)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).get(JsonNode.class);

        // check if the role was assigned
        assertNotNull(node.get("data").get(ROLE));

        // now delete the role
        node = resource()
                .path("/test-organization/test-app/users/" + USER + "/roles/"
                        + ROLE).queryParam("access_token", access_token)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).delete(JsonNode.class);

        // check if the role was deleted

        node = resource()
                .path("/test-organization/test-app/users/" + USER
                        + "/rolenames")
                .queryParam("access_token", access_token)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).get(JsonNode.class);

        // check if the role was assigned
        assertNull(node.get("data").get(ROLE));

    }

    @Test
    public void deleteUserGroup() {

        // don't populate the user, it will use the currently authenticated
        // user.

        UUID id = UUIDUtils.newTimeUUID();

        String groupPath = "groupPath" + id;

        Map<String, String> data = hashMap("type", "group").map("path",
                groupPath);

        JsonNode node = resource().path("/test-organization/test-app/groups")
                .queryParam("access_token", access_token)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .post(JsonNode.class, data);

        assertNull(node.get("error"));

        node = resource()
                .path("/test-organization/test-app/groups/" + groupPath
                        + "/users/" + USER)
                .queryParam("access_token", access_token)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).post(JsonNode.class);

        assertNull(node.get("error"));

        Map<String, Group> groups = client.getGroupsForUser(USER);

        assertNotNull(groups.get(groupPath));

        // now delete the group

        node = resource()
                .path("/test-organization/test-app/groups/" + groupPath
                        + "/users/" + USER)
                .queryParam("access_token", access_token)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).delete(JsonNode.class);

        assertNull(node.get("error"));

        groups = client.getGroupsForUser(USER);

        assertNull(groups.get(groupPath));

    }

}
