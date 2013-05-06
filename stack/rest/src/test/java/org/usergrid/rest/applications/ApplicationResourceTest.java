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
package org.usergrid.rest.applications;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.usergrid.utils.MapUtils.hashMap;

import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.apache.shiro.codec.Base64;
import org.codehaus.jackson.JsonNode;
import org.junit.Ignore;
import org.junit.Test;
import org.usergrid.management.ApplicationInfo;
import org.usergrid.management.OrganizationInfo;
import org.usergrid.rest.AbstractRestTest;

import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.representation.Form;

/**
 * Invokes methods on ApplicationResource
 *
 * @author zznate
 */
public class ApplicationResourceTest extends AbstractRestTest {
	@Test
    public void applicationWithOrgCredentials() throws Exception {

        OrganizationInfo orgInfo = managementService.getOrganizationByName("test-organization");

        String clientId = managementService.getClientIdForOrganization(orgInfo.getUuid());
        String clientSecret = managementService.getClientSecretForOrganization(orgInfo.getUuid());

        JsonNode node = resource().path("/test-organization/test-app/users").queryParam("client_id", clientId)
                .queryParam("client_secret", clientSecret).accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).get(JsonNode.class);

        assertNotNull(node.get("entities"));

    }

	@Test
    public void applicationWithAppCredentials() throws Exception {

        ApplicationInfo appInfo = managementService.getApplicationInfo("test-organization/test-app");

        String clientId = managementService.getClientIdForApplication(appInfo.getId());
        String clientSecret = managementService.getClientSecretForApplication(appInfo.getId());

        JsonNode node = resource().path("/test-organization/test-app/users").queryParam("client_id", clientId)
                .queryParam("client_secret", clientSecret).accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).get(JsonNode.class);

        assertNotNull(node.get("entities"));

    }

	@Test
    public void applicationWithJsonCreds() throws Exception {

        ApplicationInfo appInfo = managementService.getApplicationInfo("test-organization/test-app");

        String clientId = managementService.getClientIdForApplication(appInfo.getId());
        String clientSecret = managementService.getClientSecretForApplication(appInfo.getId());

        Map<String, String> payload = hashMap("email", "applicationWithJsonCreds@usergrid.org")
                .map("username", "applicationWithJsonCreds").map("name", "applicationWithJsonCreds")
                .map("password", "applicationWithJsonCreds").map("pin", "1234");

        JsonNode node = resource().path("/test-organization/test-app/users").queryParam("client_id", clientId)
                .queryParam("client_secret", clientSecret).accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).post(JsonNode.class, payload);

        assertNotNull(getEntity(node, 0));

        payload = hashMap("username", "applicationWithJsonCreds").map("password", "applicationWithJsonCreds").map(
                "grant_type", "password");

        node = resource().path("/test-organization/test-app/token").accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).post(JsonNode.class, payload);

        JsonNode token = node.get("access_token");

        assertNotNull(token);

    }

	@Test
	public void rootApplicationWithOrgCredentials() throws Exception {

		OrganizationInfo orgInfo = managementService
				.getOrganizationByName("test-organization");
		ApplicationInfo appInfo = managementService
				.getApplicationInfo("test-organization/test-app");

		String clientId = managementService.getClientIdForOrganization(orgInfo
				.getUuid());
		String clientSecret = managementService
				.getClientSecretForOrganization(orgInfo.getUuid());

		JsonNode node = resource().path("/" + appInfo.getId())
				.queryParam("client_id", clientId)
				.queryParam("client_secret", clientSecret)
				.accept(MediaType.APPLICATION_JSON)
				.type(MediaType.APPLICATION_JSON_TYPE).get(JsonNode.class);

		// ensure the URI uses the properties file as a base
		assertEquals(node.get("uri").getTextValue(),
				"http://sometestvalue/test-organization/test-app");

		node = getEntity(node, 0);
		assertEquals("test-organization/test-app", node.get("name").asText());
		assertEquals(
				"Roles",
				node.get("metadata").get("collections").get("roles")
						.get("title").asText());
		assertEquals(3, node.get("metadata").get("collections").get("roles")
				.get("count").asInt());

	}

	@Test
    public void test_GET_credentials_ok() {
        String mgmtToken = adminToken();

        JsonNode node = resource().path("/test-organization/test-app/credentials")
                .queryParam("access_token", mgmtToken).accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).get(JsonNode.class);
        assertEquals("ok", node.get("status").getTextValue());
        logNode(node);
    }

	@Test
    public void noAppDelete() {
        String mgmtToken = adminToken();

        Status status = null;
        JsonNode node = null;

        try {
             node = resource().path("/test-organization/test-app")
                    .queryParam("access_token", mgmtToken).accept(MediaType.APPLICATION_JSON)
                    .type(MediaType.APPLICATION_JSON_TYPE).delete(JsonNode.class);
        } catch (UniformInterfaceException uie) {
            status = uie.getResponse().getClientResponseStatus();
        }

        assertEquals(Status.BAD_REQUEST, status);
//        assertEquals("Application delete is not allowed yet", node.get("error_description").asText());

    }

    @Test
    public void ttlOverMax() throws Exception {

        Map<String, String> payload = hashMap("grant_type", "password").map("username", "test@usergrid.com")
                .map("password", "test").map("ttl", Long.MAX_VALUE+"");

        Status responseStatus = null;


        try {
            resource().path("/test-organization/test-app/token").accept(MediaType.APPLICATION_JSON)
                    .type(MediaType.APPLICATION_JSON_TYPE).post(JsonNode.class, payload);

        } catch (UniformInterfaceException uie) {
            responseStatus = uie.getResponse().getClientResponseStatus();
        }

        assertEquals(Status.BAD_REQUEST, responseStatus);

    }

	@Test
    public void tokenTtl() throws Exception {

        long ttl = 2000;

        JsonNode node = resource().path("/test-organization/test-app/token").queryParam("grant_type", "password")
                .queryParam("username", "ed@anuff.com").queryParam("password", "sesame")
                .queryParam("ttl", String.valueOf(ttl)).accept(MediaType.APPLICATION_JSON).get(JsonNode.class);

        long startTime = System.currentTimeMillis();

        String token = node.get("access_token").getTextValue();

        assertNotNull(token);

        long expires_in = node.get("expires_in").getLongValue();
        assertEquals(ttl, expires_in * 1000);

        JsonNode userdata = resource().path("/test-organization/test-app/users/ed@anuff.com")
                .queryParam("access_token", token).accept(MediaType.APPLICATION_JSON).get(JsonNode.class);

        assertEquals("ed@anuff.com", getEntity(userdata, 0).get("email").asText());

        // wait for the token to expire
        Thread.sleep(ttl - (System.currentTimeMillis() - startTime) + 1000);

        Status responseStatus = null;
        try {
            userdata = resource().path("/test-organization/test-app/users/ed@anuff.com")
                    .accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON_TYPE).get(JsonNode.class);
        } catch (UniformInterfaceException uie) {
            responseStatus = uie.getResponse().getClientResponseStatus();
        }

        assertEquals(Status.UNAUTHORIZED, responseStatus);

    }

	@Test
    public void ttlNan() throws Exception {

        Map<String, String> payload = hashMap("grant_type", "password").map("username", "ed@anuff.com")
                .map("password", "sesame").map("ttl", "derp");

        Status responseStatus = null;
        try {
            resource().path("/test-organization/test-app/token").accept(MediaType.APPLICATION_JSON)
                    .type(MediaType.APPLICATION_JSON_TYPE).post(JsonNode.class, payload);

        } catch (UniformInterfaceException uie) {
            responseStatus = uie.getResponse().getClientResponseStatus();
        }

        assertEquals(Status.BAD_REQUEST, responseStatus);

    }

	@Test
    public void updateAccessTokenTtl() throws Exception {

        JsonNode node = resource().path("/test-organization/test-app/token").queryParam("grant_type", "password")
                        .queryParam("username", "ed@anuff.com").queryParam("password", "sesame")
                        .accept(MediaType.APPLICATION_JSON).get(JsonNode.class);


        String token = node.get("access_token").getTextValue();
        logNode(node);
        assertNotNull(token);

        long expires_in = node.get("expires_in").getLongValue();
        assertEquals(604800, expires_in);


        Map<String, String> payload = hashMap("accesstokenttl", "31536000000");

        node = resource().path("/test-organization/test-app")
                .queryParam("access_token", adminAccessToken)
                .type(MediaType.APPLICATION_JSON_TYPE).put(JsonNode.class, payload);
        logNode(node);



        node = resource().path("/test-organization/test-app/token").queryParam("grant_type", "password")
                                .queryParam("username", "ed@anuff.com").queryParam("password", "sesame")
                                .accept(MediaType.APPLICATION_JSON).get(JsonNode.class);

        assertEquals(31536000, node.get("expires_in").getLongValue());
        logNode(node);
    }

    @Test
    @Ignore("We need to fix JSPs in our test harness")
    public void authorizationCodeWithWrongCredentials() throws Exception {
        ApplicationInfo appInfo = managementService.getApplicationInfo("test-organization/test-app");
        String clientId = managementService.getClientIdForApplication(appInfo.getId());

		Form payload = new Form();
		payload.add("username", "wrong_user");
		payload.add("password", "wrong_password");
		payload.add("response_type", "code");
		payload.add("client_id", clientId);
		payload.add("scope", "none");
		payload.add("redirect_uri", "http://www.my_test.com");

        String result = resource().path("/test-organization/test-app/authorize").type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).accept(MediaType.TEXT_HTML).post(String.class, payload);

        assertTrue(result.contains("Username or password do not match"));
    }

    @Test
    public void authorizeWithInvalidClientIdRaisesError() throws Exception {
        String result = resource().path("/test-organization/test-app/authorize").queryParam("response_type", "token").queryParam("client_id", "invalid_client_id").queryParam("redirect_uri", "http://www.my_test.com").get(String.class);

        assertTrue(result.contains("Unable to authenticate (OAuth). Invalid client_id."));
    }

    @Test
    public void authorizationCodeWithValidCredentials() throws Exception {
        ApplicationInfo appInfo = managementService.getApplicationInfo("test-organization/test-app");
        String clientId = managementService.getClientIdForApplication(appInfo.getId());

		Form payload = new Form();
		payload.add("username", "ed@anuff.com");
		payload.add("password", "sesame");
		payload.add("response_type", "code");
		payload.add("client_id", clientId);
		payload.add("scope", "none");
		payload.add("redirect_uri", "http://www.my_test.com");

		client().setFollowRedirects(false);

		Status status = null;
		try{
	        String result = resource().path("/test-organization/test-app/authorize").type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).accept(MediaType.TEXT_HTML).post(String.class, payload);
        } catch (UniformInterfaceException uie) {
            status = uie.getResponse().getClientResponseStatus();
        }

		assertEquals(Status.TEMPORARY_REDIRECT, status);
    }

    @Test
    public void clientCredentialsFlowWithHeaderAuthorization() throws Exception{
        ApplicationInfo appInfo = managementService.getApplicationInfo("test-organization/test-app");
        String clientId = managementService.getClientIdForApplication(appInfo.getId());
        String clientSecret = managementService.getClientSecretForApplication(appInfo.getId());

        String clientCredentials = clientId + ":" + clientSecret;
        String token = Base64.encodeToString(clientCredentials.getBytes());

        Form payload = new Form();
        payload.add("grant_type", "client_credentials");

        JsonNode node = resource().path("/test-organization/test-app/token")
        						.header("Authorization", "Basic "+ token)
        						.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE)
        						.accept(MediaType.APPLICATION_JSON)
        						.post(JsonNode.class, payload);

        assertNotNull("It has access_token.", node.get("access_token").getTextValue());
        assertNotNull("It has expires_in.", node.get("expires_in").getIntValue());
    }

    @Test
    public void clientCredentialsFlowWithPayload() throws Exception{
        ApplicationInfo appInfo = managementService.getApplicationInfo("test-organization/test-app");
        String clientId = managementService.getClientIdForApplication(appInfo.getId());
        String clientSecret = managementService.getClientSecretForApplication(appInfo.getId());

        Form payload = new Form();
        payload.add("grant_type", "client_credentials");
        payload.add("client_id", clientId);
        payload.add("client_secret", clientSecret);

        JsonNode node = resource().path("/test-organization/test-app/token")
        						.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE)
        						.accept(MediaType.APPLICATION_JSON)
        						.post(JsonNode.class, payload);

        assertNotNull("It has access_token.", node.get("access_token").getTextValue());
        assertNotNull("It has expires_in.", node.get("expires_in").getIntValue());
    }

    @Test
    public void clientCredentialsFlowWithHeaderAuthorizationAndPayload() throws Exception {
      ApplicationInfo appInfo = managementService.getApplicationInfo("test-organization/test-app");
      String clientId = managementService.getClientIdForApplication(appInfo.getId());
      String clientSecret = managementService.getClientSecretForApplication(appInfo.getId());

      String clientCredentials = clientId + ":" + clientSecret;
      String token = Base64.encodeToString(clientCredentials.getBytes());

      Map<String, String> payload = hashMap("grant_type", "client_credentials");

      JsonNode node = resource().path("/test-organization/test-app/token")
      		.header("Authorization", "Basic " + token)
      		.type(MediaType.APPLICATION_JSON_TYPE)
      		.accept(MediaType.APPLICATION_JSON)
      		.post(JsonNode.class, payload);

      assertNotNull("It has access_token.", node.get("access_token").getTextValue());
      assertNotNull("It has expires_in.", node.get("expires_in").getIntValue());
    }
}
