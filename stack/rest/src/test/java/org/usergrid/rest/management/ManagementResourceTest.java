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
package org.usergrid.rest.management;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.usergrid.utils.MapUtils.hashMap;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.junit.Test;
import org.usergrid.management.OrganizationInfo;
import org.usergrid.management.OrganizationOwnerInfo;
import org.usergrid.rest.AbstractRestTest;

import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.api.client.UniformInterfaceException;

/**
 * @author tnine
 */
public class ManagementResourceTest extends AbstractRestTest {

    public ManagementResourceTest() throws Exception {

    }

    /**
     * Test if we can reset our password as an admin
     */
    @Test
    public void setSelfAdminPasswordAsAdmin() {

        String newPassword = "foo";

        Map<String, String> data = new HashMap<String, String>();
        data.put("newpassword", newPassword);
        data.put("oldpassword", "test");

        // change the password as admin. The old password isn't required
        JsonNode node = resource().path("/management/users/test/password").accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).post(JsonNode.class, data);

        assertNull(getError(node));

        adminAccessToken = mgmtToken("test", newPassword);

        data.put("oldpassword", newPassword);
        data.put("newpassword", "test");

        node = resource().path("/management/users/test/password").queryParam("access_token", adminAccessToken)
                .accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON_TYPE).post(JsonNode.class, data);

        assertNull(getError(node));

    }

    @Test
    public void passwordMismatchErrorAdmin() {
        String origPassword = "foo";
        String newPassword = "bar";

        Map<String, String> data = new HashMap<String, String>();
        data.put("newpassword", origPassword);

        // now change the password, with an incorrect old password

        data.put("oldpassword", origPassword);
        data.put("newpassword", newPassword);

        Status responseStatus = null;

        try {
            resource().path("/management/users/test/password").accept(MediaType.APPLICATION_JSON)
                    .type(MediaType.APPLICATION_JSON_TYPE).post(JsonNode.class, data);
        } catch (UniformInterfaceException uie) {
            responseStatus = uie.getResponse().getClientResponseStatus();
        }

        assertNotNull(responseStatus);

        assertEquals(Status.BAD_REQUEST, responseStatus);

    }

    @Test
    public void setAdminPasswordAsSysAdmin() {

        String superToken = superAdminToken();

        String newPassword = "foo";

        Map<String, String> data = new HashMap<String, String>();
        data.put("newpassword", newPassword);

        // change the password as admin. The old password isn't required
        JsonNode node = resource().path("/management/users/test/password").queryParam("access_token", superToken)
                .accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON_TYPE).post(JsonNode.class, data);

        assertNull(getError(node));

        // log in with the new password
        String token = mgmtToken("test", newPassword);

        assertNotNull(token);

        data.put("newpassword", "test");

        // now change the password back
        node = resource().path("/management/users/test/password").queryParam("access_token", superToken)
                .accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON_TYPE).post(JsonNode.class, data);

        assertNull(getError(node));

    }

    /**
     * Test that admins can't view organizations they're not authorized to view.
     *
     * @throws Exception
     */
    @Test
    public void crossOrgsNotViewable() throws Exception {

        OrganizationOwnerInfo orgInfo = managementService.createOwnerAndOrganization("crossOrgsNotViewable",
                "crossOrgsNotViewable", "TestName", "crossOrgsNotViewable@usergrid.org", "password");

        // check that the test admin cannot access the new org info

        Status status = null;

        try {
            resource().path(String.format("/management/orgs/%s", orgInfo.getOrganization().getName()))
                    .queryParam("access_token", adminAccessToken).accept(MediaType.APPLICATION_JSON)
                    .type(MediaType.APPLICATION_JSON_TYPE).get(JsonNode.class);
        } catch (UniformInterfaceException uie) {
            status = uie.getResponse().getClientResponseStatus();
        }

        assertNotNull(status);
        assertEquals(Status.UNAUTHORIZED, status);

        status = null;

        try {
            resource().path(String.format("/management/orgs/%s", orgInfo.getOrganization().getUuid()))
                    .queryParam("access_token", adminAccessToken).accept(MediaType.APPLICATION_JSON)
                    .type(MediaType.APPLICATION_JSON_TYPE).get(JsonNode.class);
        } catch (UniformInterfaceException uie) {
            status = uie.getResponse().getClientResponseStatus();
        }

        assertNotNull(status);
        assertEquals(Status.UNAUTHORIZED, status);

        // this admin should have access to test org
        status = null;
        try {
            resource().path("/management/orgs/test-organization").queryParam("access_token", adminAccessToken)
                    .accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON_TYPE).get(JsonNode.class);
        } catch (UniformInterfaceException uie) {
            status = uie.getResponse().getClientResponseStatus();
        }

        assertNull(status);

        OrganizationInfo org = managementService.getOrganizationByName("test-organization");

        status = null;
        try {
            resource().path(String.format("/management/orgs/%s", org.getUuid()))
                    .queryParam("access_token", adminAccessToken).accept(MediaType.APPLICATION_JSON)
                    .type(MediaType.APPLICATION_JSON_TYPE).get(JsonNode.class);
        } catch (UniformInterfaceException uie) {
            status = uie.getResponse().getClientResponseStatus();
        }

        assertNull(status);

    }

    @Test
    public void mgmtUserFeed() throws Exception {
        JsonNode userdata = resource().path("/management/users/test@usergrid.com/feed").queryParam("access_token", adminAccessToken)
                .accept(MediaType.APPLICATION_JSON).get(JsonNode.class);
        assertTrue(StringUtils.contains(this.getEntity(userdata, 0).get("title").asText(),"<a href=\"mailto:test@usergrid.com\">"));
    }

    @Test
    public void mgmtCreateAndGetApplication() throws Exception {

    	OrganizationInfo orgInfo = managementService.getOrganizationByName("test-organization");
        Map<String, String> data = new HashMap<String, String>();
        data.put("name", "mgmt-org-app");

        // POST /applications
        JsonNode appdata = resource().path("/management/orgs/"+orgInfo.getUuid()+"/applications")
        		.queryParam("access_token", adminToken())
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).post(JsonNode.class,data);
        logNode(appdata);
        appdata = getEntity(appdata, 0);

        assertEquals("test-organization/mgmt-org-app", appdata.get("name").asText());
        assertEquals("Roles", appdata.get("metadata").get("collections").get("roles").get("title").asText());
        assertEquals(3, appdata.get("metadata").get("collections").get("roles").get("count").asInt());

        // GET /applications/mgmt-org-app
        appdata = resource().path("/management/orgs/"+orgInfo.getUuid()+"/applications/mgmt-org-app")
        		.queryParam("access_token", adminToken())
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).get(JsonNode.class);
        logNode(appdata);

        assertEquals("test-organization", appdata.get("organization").asText());
        assertEquals("mgmt-org-app", appdata.get("applicationName").asText());
        assertEquals("http://sometestvalue/test-organization/mgmt-org-app", appdata.get("uri").getTextValue());
        appdata = getEntity(appdata, 0);

        assertEquals("test-organization/mgmt-org-app", appdata.get("name").asText());
        assertEquals("Roles", appdata.get("metadata").get("collections").get("roles").get("title").asText());
        assertEquals(3, appdata.get("metadata").get("collections").get("roles").get("count").asInt());
    }

    @Test
    public void tokenTtl() throws Exception {

        long ttl = 2000;

        JsonNode node = resource().path("/management/token").queryParam("grant_type", "password")
                .queryParam("username", "test@usergrid.com").queryParam("password", "test")
                .queryParam("ttl", String.valueOf(ttl)).accept(MediaType.APPLICATION_JSON).get(JsonNode.class);

        long startTime = System.currentTimeMillis();

        String token = node.get("access_token").getTextValue();

        assertNotNull(token);

        JsonNode userdata = resource().path("/management/users/test@usergrid.com").queryParam("access_token", token)
                .accept(MediaType.APPLICATION_JSON).get(JsonNode.class);

        assertEquals("test@usergrid.com", userdata.get("data").get("email").asText());

        // wait for the token to expire
        Thread.sleep(ttl - (System.currentTimeMillis() - startTime) + 1000);

        Status responseStatus = null;
        try {
            userdata = resource().path("/management/users/test@usergrid.com").accept(MediaType.APPLICATION_JSON)
                    .type(MediaType.APPLICATION_JSON_TYPE).get(JsonNode.class);
        } catch (UniformInterfaceException uie) {
            responseStatus = uie.getResponse().getClientResponseStatus();
        }

        assertEquals(Status.UNAUTHORIZED, responseStatus);

    }

    @Test
    public void meToken() throws Exception {
      JsonNode node = resource().path("/management/me").queryParam("grant_type", "password")
              .queryParam("username", "test@usergrid.com").queryParam("password", "test")
              .accept(MediaType.APPLICATION_JSON).get(JsonNode.class);

      logNode(node);
      String token = node.get("access_token").getTextValue();

      assertNotNull(token);

      node = resource().path("/management/me").queryParam("access_token", token)
              .accept(MediaType.APPLICATION_JSON).get(JsonNode.class);
      logNode(node);
    }

    @Test
    public void ttlNan() throws Exception {

        Map<String, String> payload = hashMap("grant_type", "password").map("username", "test@usergrid.com")
                .map("password", "test").map("ttl", "derp");

        Status responseStatus = null;
        try {
            resource().path("/management/token").accept(MediaType.APPLICATION_JSON)
                    .type(MediaType.APPLICATION_JSON_TYPE).post(JsonNode.class, payload);

        } catch (UniformInterfaceException uie) {
            responseStatus = uie.getResponse().getClientResponseStatus();
        }

        assertEquals(Status.BAD_REQUEST, responseStatus);

    }

    @Test
    public void ttlOverMax() throws Exception {

        Map<String, String> payload = hashMap("grant_type", "password").map("username", "test@usergrid.com")
                .map("password", "test").map("ttl", Long.MAX_VALUE + "");

        Status responseStatus = null;

        try {
            resource().path("/management/token").accept(MediaType.APPLICATION_JSON)
                    .type(MediaType.APPLICATION_JSON_TYPE).post(JsonNode.class, payload);

        } catch (UniformInterfaceException uie) {
            responseStatus = uie.getResponse().getClientResponseStatus();
        }

        assertEquals(Status.BAD_REQUEST, responseStatus);

    }

    @Test
    public void revokeToken() throws Exception {
    	String token1 = super.adminToken();
        String token2 = super.adminToken();

        JsonNode response = resource().path("/management/users/test").queryParam("access_token", token1)
                .accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON_TYPE).get(JsonNode.class);

        assertEquals("test@usergrid.com", response.get("data").get("email").asText());

        response = resource().path("/management/users/test").queryParam("access_token", token2)
                .accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON_TYPE).get(JsonNode.class);

        assertEquals("test@usergrid.com", response.get("data").get("email").asText());

        // now revoke the tokens
        response = resource().path("/management/users/test/revoketokens").queryParam("access_token", superAdminToken())
                .accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON_TYPE).post(JsonNode.class);

        // the tokens shouldn't work

        Status status = null;

        try {
            response = resource().path("/management/users/test").queryParam("access_token", token1)
                    .accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON_TYPE).get(JsonNode.class);
        } catch (UniformInterfaceException uie) {
            status = uie.getResponse().getClientResponseStatus();
        }

        assertEquals(Status.UNAUTHORIZED, status);

        status = null;

        try {
            response = resource().path("/management/users/test").queryParam("access_token", token2)
                    .accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON_TYPE).get(JsonNode.class);
        } catch (UniformInterfaceException uie) {
            status = uie.getResponse().getClientResponseStatus();
        }

        assertEquals(Status.UNAUTHORIZED, status);

        String token3 = super.adminToken();
        String token4 = super.adminToken();

        response = resource().path("/management/users/test").queryParam("access_token", token3)
                .accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON_TYPE).get(JsonNode.class);

        assertEquals("test@usergrid.com", response.get("data").get("email").asText());

        response = resource().path("/management/users/test").queryParam("access_token", token4)
                .accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON_TYPE).get(JsonNode.class);

        assertEquals("test@usergrid.com", response.get("data").get("email").asText());

        // now revoke the token3
        response = resource()
        		.path("/management/users/test/revoketoken")
        		.queryParam("access_token", token3)
        		.queryParam("token", token3)
        		.accept(MediaType.APPLICATION_JSON)
        		.type(MediaType.APPLICATION_JSON_TYPE)
        		.post(JsonNode.class);

        // the token3 shouldn't work

        status = null;

        try {
            response = resource()
            		.path("/management/users/test")
            		.queryParam("access_token", token3)
                    .accept(MediaType.APPLICATION_JSON)
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .get(JsonNode.class);
        } catch (UniformInterfaceException uie) {
            status = uie.getResponse().getClientResponseStatus();
        }

        assertEquals(Status.UNAUTHORIZED, status);

        status = null;

        try {
            response = resource().path("/management/users/test")
            		.queryParam("access_token", token4)
                    .accept(MediaType.APPLICATION_JSON)
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .get(JsonNode.class);

            status = Status.OK;
        } catch (UniformInterfaceException uie) {
            status = uie.getResponse().getClientResponseStatus();
        }

        assertEquals(Status.OK, status);
    }
}
