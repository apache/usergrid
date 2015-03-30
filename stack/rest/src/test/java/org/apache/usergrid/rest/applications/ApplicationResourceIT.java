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
package org.apache.usergrid.rest.applications;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.representation.Form;
import org.apache.shiro.codec.Base64;
import org.apache.usergrid.rest.test.resource2point0.AbstractRestIT;
import org.apache.usergrid.rest.test.resource2point0.endpoints.mgmt.OrganizationResource;
import org.apache.usergrid.rest.test.resource2point0.model.*;
import org.apache.usergrid.utils.MapUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import java.io.IOException;

import static org.apache.usergrid.utils.MapUtils.hashMap;
import static org.junit.Assert.*;


/**
 * Invokes methods on ApplicationResource
 */
public class ApplicationResourceIT extends AbstractRestIT {
    private static final Logger logger = LoggerFactory.getLogger(ApplicationResourceIT.class);

    /**
     * Retrieve an application using the organization client credentials
     */
    @Test
    public void applicationWithOrgCredentials() throws Exception {
        //retrieve the credentials
        Credentials orgCredentials = getOrgCredentials();

        //retrieve the app using only the org credentials
        ApiResponse apiResponse = this.org().app(clientSetup.getAppName()).getResource(false)
            .queryParam("grant_type", "client_credentials")
            .queryParam("client_id", orgCredentials.getClientId())
            .queryParam("client_secret", orgCredentials.getClientSecret())
            .accept(MediaType.APPLICATION_JSON)
            .type(MediaType.APPLICATION_JSON_TYPE)
            .get(ApiResponse.class);
        //assert that a valid response is returned without error
        assertNotNull(apiResponse);
        assertNull(apiResponse.getError());
    }

    /**
     * Retrieve an application using the application client credentials
     */
    @Test
    public void applicationWithAppCredentials() throws Exception {

        //retrieve the credentials
        Credentials appCredentials = getAppCredentials();

        //retrieve the app using only the org credentials
        ApiResponse apiResponse = this.app().getResource(false)
            .queryParam("grant_type", "client_credentials")
            .queryParam("client_id", appCredentials.getClientId())
            .queryParam("client_secret", appCredentials.getClientSecret())
            .accept(MediaType.APPLICATION_JSON)
            .type(MediaType.APPLICATION_JSON_TYPE)
            .get(ApiResponse.class);
        //assert that a valid response is returned without error
        assertNotNull(apiResponse);
        assertNull(apiResponse.getError());
    }

    /**
     * Verifies that we return JSON even when no accept header is specified.
     * (for backwards compatibility)
     */
    @Test
    public void jsonForNoAccepts() throws Exception {
        //retrieve the credentials
        Credentials orgCredentials = getOrgCredentials();

        //retrieve the users collection without setting the "Accept" header
        WebResource.Builder builder = this.app().collection("users").getResource(false)
            //Add the org credentials to the query
            .queryParam("grant_type", "client_credentials")
            .queryParam("client_id", orgCredentials.getClientId())
            .queryParam("client_secret", orgCredentials.getClientSecret())
            .type(MediaType.APPLICATION_JSON_TYPE);

        ApiResponse apiResponse = builder.get(ApiResponse.class);
        Collection users = new Collection(apiResponse);
        //assert that a valid response is returned without error
        assertNotNull(users);
        assertNull(users.getResponse().getError());

    }

    /**
     * Verifies that we return JSON even when text/html is requested.
     * (for backwards compatibility)
     */
    @Test
    public void jsonForAcceptsTextHtml() throws Exception {

        //Create the organization resource
        OrganizationResource orgResource = clientSetup.getRestClient()
            .management().orgs().organization(clientSetup.getOrganizationName());

        //retrieve the credentials
        Credentials orgCredentials = orgResource.credentials().get();
        String clientId = orgCredentials.getClientId();
        String clientSecret = orgCredentials.getClientSecret();

        //retrieve the users collection, setting the "Accept" header to text/html
        WebResource.Builder builder = this.app().collection("users").getResource(false)
            //Add the org credentials to the query
            .queryParam("grant_type", "client_credentials")
            .queryParam("client_id", clientId)
            .queryParam("client_secret", clientSecret)
            .accept(MediaType.TEXT_HTML)
            .type(MediaType.APPLICATION_JSON_TYPE);

        ApiResponse apiResponse = builder.get(ApiResponse.class);
        Collection users = new Collection(apiResponse);
        //make sure that a valid response is returned without error
        assertNotNull(users);
        assertNull(users.getResponse().getError());
    }

    /**
     * Retrieve an application using password credentials
     *
     * @throws Exception
     */
    @Test
    public void applicationWithJsonCreds() throws Exception {

        User user = new User(
            "applicationWithJsonCreds",
            "applicationWithJsonCreds",
            "applicationWithJsonCreds@usergrid.org",
            "applicationWithJsonCreds");
        Entity entity = this.app().collection("users").post(user);

        assertNotNull(entity);

        refreshIndex();

        //retrieve the app using a username and password
        QueryParameters params = new QueryParameters()
            .addParam("grant_type", "password")
            .addParam("username", "applicationWithJsonCreds")
            .addParam("password", "applicationWithJsonCreds");
        Token apiResponse = this.app().token().post(params);

        //assert that a valid response is returned without error
        assertNotNull(apiResponse);
        assertNull(apiResponse.getResponse().getError());
    }

    /**
     * Retrieve the root application using client credentials
     *
     * @throws Exception
     */
    @Test
    public void rootApplicationWithOrgCredentials() throws Exception {

        String orgName = clientSetup.getOrganizationName().toLowerCase();
        String appName = clientSetup.getAppName().toLowerCase();
        //retrieve the credentials
        Credentials orgCredentials = getOrgCredentials();

        ApiResponse apiResponse = this.app().getResource(false)
            .queryParam("grant_type", "client_credentials")
            .queryParam("client_id", orgCredentials.getClientId())
            .queryParam("client_secret", orgCredentials.getClientSecret())
            .accept(MediaType.APPLICATION_JSON)
            .type(MediaType.APPLICATION_JSON_TYPE)
            .get(ApiResponse.class);

        // assert that the response returns the correct URI
        assertEquals(apiResponse.getUri(), String.format("http://sometestvalue/%s/%s", orgName, appName));

        //unmarshal the application from the response
        Application application = new Application(apiResponse);

        //assert that the application name is correct
        assertEquals(String.format("%s/%s", orgName, appName), application.get("name"));

        //retrieve the application's roles collection
        apiResponse = this.app().collection("roles").getResource(false)
            .queryParam("grant_type", "client_credentials")
            .queryParam("client_id", orgCredentials.getClientId())
            .queryParam("client_secret", orgCredentials.getClientSecret())
            .accept(MediaType.APPLICATION_JSON)
            .type(MediaType.APPLICATION_JSON_TYPE)
            .get(ApiResponse.class);
        Collection roles = new Collection(apiResponse);
        //assert that we have the correct number of default roles
        assertEquals(3, roles.getNumOfEntities());
    }

    /**
     * Retrieve the client credentials for an application
     */
    @Test
    public void testGetAppCredentials() throws IOException {
        Credentials credentials = getAppCredentials();

        assertNotNull(credentials.getClientId());
        assertNotNull(credentials.getClientSecret());
    }

    /**
     * retrieve the client credentials for an organization
     */
    @Test
    public void testGetOrgCredentials() throws IOException {
        Credentials credentials = getOrgCredentials();

        assertNotNull(credentials.getClientId());
        assertNotNull(credentials.getClientSecret());
    }


    /**
     * Reset an application's client credentials
     */
    @Test
    public void testResetAppCredentials() throws IOException {
        Credentials credentials = this.app().credentials()
            .get(new QueryParameters().addParam("access_token", this.getAdminToken().getAccessToken()), false);

//        assertNull(credentials.entrySet().toString());
        assertNotNull(credentials.getClientId());
        assertNotNull(credentials.getClientSecret());
    }


    @Test
    @Ignore //This is implemented now
    public void noAppDelete() throws IOException {
        String orgName = clientSetup.getOrganizationName().toLowerCase();
        String appName = clientSetup.getAppName().toLowerCase();

        ApiResponse apiResponse = resource().path(String.format("/%s/%s", orgName, appName))
            .queryParam("access_token", this.getAdminToken().getAccessToken())
            .accept(MediaType.APPLICATION_JSON)
            .type(MediaType.APPLICATION_JSON_TYPE)
            .delete(ApiResponse.class);

        assertNotNull(apiResponse.getError());
    }

    /**
     * Test for an exception when a token's TTL is set greater than the maximum
     */
    @Test
    public void ttlOverMax() throws Exception {

        String orgName = clientSetup.getOrganizationName().toLowerCase();
        String appName = clientSetup.getAppName().toLowerCase();
        String username = "username";
        String name = "name";

        //Create a new user entity
        User user = new User(username, name, username + "@usergrid.org", "password");

        //save the user entity
        Entity entity = this.app().collection("users").post(user);
        //assert that it was saved correctly
        assertNotNull(entity);
        refreshIndex();

        //add a ttl to the entity that is greater than the maximum
        entity.chainPut("grant_type", "password").chainPut("ttl", Long.MAX_VALUE);

        try {
            //POST the updated TTL, anticipating an exception
            resource().path(String.format("/%s/%s/token", orgName, appName))
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .post(ApiResponse.class, entity);
            fail("This should cause an exception");
        } catch (UniformInterfaceException uie) {
            assertEquals(
                String.valueOf(Status.BAD_REQUEST.getStatusCode()),
                String.valueOf(uie.getResponse().getStatus()));
        }
    }

    /**
     * Set a token's TTL
     */
    @Test
    public void tokenTtl() throws Exception {

        long ttl = 2000;

        String orgName = clientSetup.getOrganizationName().toLowerCase();
        String appName = clientSetup.getAppName().toLowerCase();
        String username = "username";
        String name = "name";

        //Create a new user entity
        User user = new User(username, name, username + "@usergrid.org", "password");

        //save the entity
        Entity entity = this.app().collection("users").post(user);
        assertNotNull(entity);
        refreshIndex();

        //Retrieve an authentication token for the user, setting the TTL
        Token apiResponse = resource().path(String.format("/%s/%s/token", orgName, appName))
            .queryParam("grant_type", "password")
            .queryParam("username", username)
            .queryParam("password", "password")
            .queryParam("ttl", String.valueOf(ttl))
            .accept(MediaType.APPLICATION_JSON)
            .type(MediaType.APPLICATION_JSON_TYPE)
            .get(Token.class);

        //Set a start time so we can calculate then the token should expire
        long startTime = System.currentTimeMillis();

        //Get the string value of the token
        String token = apiResponse.getAccessToken();
        assertNotNull(token);

        //Get the expiration time of the token (in seconds)
        long expires_in = apiResponse.getExpirationDate();

        //assert that the token's ttl was set correctly
        assertEquals(ttl, expires_in * 1000);

        //retrieve the user entity using the new token
        entity = this.app().collection("users").entity(entity).get(
            new QueryParameters().addParam("access_token", token), false);

        //assert that we got the correct user
        assertEquals(username + "@usergrid.org", entity.get("email"));

        // wait for the token to expire
        Thread.sleep(ttl - (System.currentTimeMillis() - startTime) + 1000);

        try {
            //attempt to retrieve the user again. At this point, the token should have expired
            this.app().collection("users").entity(entity).get(
                new QueryParameters().addParam("access_token", token), false);
            fail("The expired token should cause an exception");
        } catch (UniformInterfaceException uie) {
            assertEquals(Status.UNAUTHORIZED.getStatusCode(), uie.getResponse().getStatus());
        }

    }

    /**
     * Attempt to set the TTL to an invalid value
     *
     * @throws Exception
     */
    @Test
    public void ttlNan() throws Exception {

        String orgName = clientSetup.getOrganizationName().toLowerCase();
        String appName = clientSetup.getAppName().toLowerCase();
        String username = "username";
        String name = "name";

        //Create a new user entity
        User user = new User(username, name, username + "@usergrid.org", "password");

        //save the entity
        Entity entity = this.app().collection("users").post(user);
        assertNotNull(entity);
        refreshIndex();

        try {
            //Retrieve a token for the new user, setting the TTL to an invalid value
            resource().path(String.format("/%s/%s/token", orgName, appName))
                .queryParam("grant_type", "password")
                .queryParam("username", username)
                .queryParam("password", "password")
                .queryParam("ttl", "derp")
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .get(ApiResponse.class);
            fail("The invalid TTL should cause an exception");

        } catch (UniformInterfaceException uie) {
            //TODO should this be handled and returned as a Status.BAD_REQUEST?
            //Status.INTERNAL_SERVER_ERROR is thrown because Jersey throws a NumberFormatException
            assertEquals(Status.INTERNAL_SERVER_ERROR, uie.getResponse().getClientResponseStatus());
        }

    }

    /**
     * Update the default auth token TTL for an application
     */
    @Test
    public void updateAccessTokenTtl() throws Exception {

        String orgName = clientSetup.getOrganizationName().toLowerCase();
        String appName = clientSetup.getAppName().toLowerCase();
        String username = "username";
        String name = "name";

        //Create a new user entity
        User user = new User(username, name, username + "@usergrid.org", "password");

        //save the entity
        Entity entity = this.app().collection("users").post(user);
        assertNotNull(entity);
        refreshIndex();

        //Retrieve an authentication token for the user
        Token tokenResponse = resource().path(String.format("/%s/%s/token", orgName, appName))
            .queryParam("grant_type", "password")
            .queryParam("username", username)
            .queryParam("password", "password")
            .accept(MediaType.APPLICATION_JSON)
            .type(MediaType.APPLICATION_JSON_TYPE)
            .get(Token.class);

        String token = tokenResponse.getAccessToken();
        assertNotNull(token);

        //Retrieve the expiration time of the token. Should be set to the default of 1 day
        long expires_in = tokenResponse.getExpirationDate();
        assertEquals(604800, expires_in);

        //Set the default TTL of the application to a date far in the future
        this.app().getResource(false)
            .queryParam("access_token", token)
            .accept(MediaType.APPLICATION_JSON)
            .type(MediaType.APPLICATION_JSON_TYPE)
            .put(Token.class, new MapUtils.HashMapBuilder<String, String>().map("accesstokenttl", "31536000000"));

        //Create a new token for the user
        tokenResponse = this.app().token().getResource(false)
            .queryParam("grant_type", "password")
            .queryParam("username", username)
            .queryParam("password", "password")
            .accept(MediaType.APPLICATION_JSON)
            .type(MediaType.APPLICATION_JSON_TYPE)
            .get(Token.class);

        //assert that the new token has the new default TTL
        assertEquals(31536000, tokenResponse.getExpirationDate().intValue());

    }

    /**
     * Retrieve an oauth authorization using invalid credentials
     */
    @Test
    public void authorizationCodeWithWrongCredentials() throws Exception {
        //Create form input with bogus credentials
        Form payload = new Form();
        payload.add("username", "wrong_user");
        payload.add("password", "wrong_password");
        payload.add("response_type", "code");
        payload.add("scope", "none");
        payload.add("redirect_uri", "http://www.my_test.com");

        //POST the form to the authorization endpoint
        String apiResponse = clientSetup.getRestClient().management().authorize().post(String.class, payload);

        //Assert that an appropriate error message is returned
        assertTrue(apiResponse.contains("Username or password do not match"));
    }


    /**
     * retrieve an oauth authorization using invalid application client credentials
     */
    @Test
    public void authorizeWithInvalidClientIdRaisesError() throws Exception {
        //GET the application authorization endpoint using bogus client credentials
        String apiResponse = clientSetup.getRestClient().management().authorize().getResource(false)
            .queryParam("response_type", "code")
            .queryParam("client_id", "invalid_client_id")
            .queryParam("redirect_uri", "http://www.my_test.com")
            .accept(MediaType.APPLICATION_JSON)
            .type(MediaType.APPLICATION_JSON_TYPE)
            .get(String.class);
        //Assert that an appropriate error message is returned
        assertTrue(apiResponse.contains("Unable to authenticate (OAuth). Invalid client_id."));
    }

    /**
     * Retrieve an oauth authorization using valid client credentials
     */
    @Test
    public void authorizationCodeWithValidCredentials() throws Exception {
        //retrieve the credentials
        Credentials orgCredentials = getOrgCredentials();

        //Create form input with valid credentials
        Form payload = new Form();
        payload.add("response_type", "code");
        payload.add("grant_type", "client_credentials");
        payload.add("client_id", orgCredentials.getClientId());
        payload.add("client_secret", orgCredentials.getClientSecret());
        payload.add("scope", "none");
        payload.add("redirect_uri", "http://www.my_test.com");

        //Set the client to not follow the initial redirect returned by the stack
        client().setFollowRedirects(false);

        try {
            //POST the form to the authorization endpoint
            clientSetup.getRestClient().management().authorize().post(String.class, payload);
        } catch (UniformInterfaceException uie) {
            assertEquals(String.valueOf(Status.TEMPORARY_REDIRECT.getStatusCode()), uie.getResponse().getStatus());
        }

    }

    /**
     * Retrieve an access token using HTTP Basic authentication
     */
    @Test
    public void clientCredentialsFlowWithBasicAuthentication() throws Exception {
        //retrieve the credentials
        Credentials orgCredentials = getOrgCredentials();
        String clientId = orgCredentials.getClientId();
        String clientSecret = orgCredentials.getClientSecret();

        //encode the credentials
        String clientCredentials = clientId + ":" + clientSecret;
        String token = Base64.encodeToString(clientCredentials.getBytes());

        //GET the token endpoint, adding the basic auth header
        Token apiResponse = clientSetup.getRestClient().management().token().getResource(false)
            //add the auth header
            .header("Authorization", "Basic " + token)
            .accept(MediaType.APPLICATION_JSON)
            .post(Token.class, hashMap("grant_type", "client_credentials"));

        //Assert that a valid token with a valid TTL is returned
        assertNotNull("A valid response was returned.", apiResponse);
        assertNull("There is no error.", apiResponse.getError());
        assertNotNull("It has access_token.", apiResponse.getAccessToken());
        assertNotNull("It has expires_in.", apiResponse.getExpirationDate());
    }

    /**
     * Retrieve an access token using HTTP Basic authentication
     */
    @Test
    public void clientCredentialsFlowWithHeaderAuthorization() throws Exception {
        //retrieve the credentials
        Credentials orgCredentials = getAppCredentials();
        String clientId = orgCredentials.getClientId();
        String clientSecret = orgCredentials.getClientSecret();

        Token token = clientSetup.getRestClient().management().token()
            .post(Token.class,new Token("client_credentials", clientId, clientSecret));

        //GET the token endpoint, adding authorization header
        Token apiResponse = this.app().token().getResource(false)
            //add the auth header
            .header("Authorization", "Bearer " + token.getAccessToken())
            .accept(MediaType.APPLICATION_JSON)
            .type(MediaType.APPLICATION_JSON_TYPE)
            .post(Token.class, hashMap("grant_type", "client_credentials"));

        //Assert that a valid token with a valid TTL is returned
        assertNotNull("A valid response was returned.", apiResponse);
        assertNull("There is no error.", apiResponse.getError());
        assertNotNull("It has access_token.", apiResponse.getAccessToken());
        assertNotNull("It has expires_in.", apiResponse.getExpirationDate());
    }

    /**
     * Retrieve an authentication token using form input
     */
    @Test
    public void clientCredentialsFlowWithPayload() throws Exception {
        //retrieve the credentials
        Credentials orgCredentials = getOrgCredentials();
        String clientId = orgCredentials.getClientId();
        String clientSecret = orgCredentials.getClientSecret();

        //Create form input
        Form payload = new Form();
        payload.add("grant_type", "client_credentials");
        payload.add("client_id", clientId);
        payload.add("client_secret", clientSecret);

        //POST the form to the application token endpoint
        Token apiResponse = this.app().token().getResource(false)
            .accept(MediaType.APPLICATION_JSON)
            .type(MediaType.APPLICATION_FORM_URLENCODED_TYPE)
            .post(Token.class, payload);

        //Assert that a valid token with a valid TTL is returned
        assertNotNull("It has access_token.", apiResponse.getAccessToken());
        assertNotNull("It has expires_in.", apiResponse.getExpirationDate());
    }


    /**
     * Retrieve an authentication token using a combination of form input and payload
     */
    @Test
    public void clientCredentialsFlowWithHeaderAuthorizationAndPayload() throws Exception {
        //retrieve the credentials
        Credentials orgCredentials = getOrgCredentials();
        String clientId = orgCredentials.getClientId();
        String clientSecret = orgCredentials.getClientSecret();

        //Encode the credentials
        String clientCredentials = clientId + ":" + clientSecret;
        String token = Base64.encodeToString(clientCredentials.getBytes());

        //POST the form to the application token endpoint along with the payload
        Token apiResponse = this.app().token().getResource(false)
            .header("Authorization", "Basic " + token)
            .accept(MediaType.APPLICATION_JSON)
            .type(MediaType.APPLICATION_JSON_TYPE)
            .post(Token.class, hashMap("grant_type", "client_credentials"));

        //Assert that a valid token with a valid TTL is returned
        assertNotNull("It has access_token.", apiResponse.getAccessToken());
        assertNotNull("It has expires_in.", apiResponse.getExpirationDate());
    }

    /**
     * Ensure that the Apigee Mobile Analytics config returns valid JSON
     */
    @Test
    public void validateApigeeApmConfigAPP() throws IOException {
        String orgName = clientSetup.getOrganizationName().toLowerCase();
        String appName = clientSetup.getAppName().toLowerCase();

        try {
            //GET the APM endpoint
            String response = resource().path(String.format("/%s/%s/apm/apigeeMobileConfig", orgName, appName))
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .get(String.class);
            //Parse the response
            JsonNode node = mapper.readTree(response);

            //if things are kosher then JSON should have value for instaOpsApplicationId
            assertTrue("it's valid json for APM", node.has("instaOpsApplicationId"));
        } catch (UniformInterfaceException uie) {
            //Validate that APM config exists
            assertNotEquals("APM Config API exists", Status.NOT_FOUND,
                uie.getResponse().getStatus()); //i.e It should not be "Not Found"
        }
    }


    /**
     * Retrieve an application token using organization credentials
     */
    @Test
    public void appTokenFromOrgCreds() throws Exception {
        //retrieve the organization credentials
        Credentials orgCredentials = getOrgCredentials();
        String clientId = orgCredentials.getClientId();
        String clientSecret = orgCredentials.getClientSecret();

        //use the org credentials to create an application token
        Token token = this.app().token().post(new Token("client_credentials", clientId, clientSecret));

        //Assert that we received an authorization token
        assertNotNull(token);

        int ttl = token.getExpirationDate().intValue();
        //check it's 1 day, should be the same as the default
        assertEquals(604800, ttl);

        //retrieve the users collection for the application using the new token
        ApiResponse response = this.app().collection("users").getResource(true, token).get(ApiResponse.class);
        //assert that we did not receive an error
        assertNull(response.getError());
    }


    /**
     * Retrieve an application token using application credentials
     */
    @Test
    public void appTokenFromAppCreds() throws Exception {
        //retrieve the app credentials
        Credentials appCredentials = getAppCredentials();
        String clientId = appCredentials.getClientId();
        String clientSecret = appCredentials.getClientSecret();

        Token token = this.app().token().post(new Token("client_credentials", clientId, clientSecret));
        //Assert that we received an authorization token
        assertNotNull(token);
        assertNotNull(token.getAccessToken());
        assertNotNull(token.getExpirationDate());

        int ttl = token.getExpirationDate().intValue();
        //check it's 1 day, should be the same as the default
        assertEquals(604800, ttl);

        //retrieve the users collection for the application using the new token
        ApiResponse response = this.app().collection("users").getResource(true, token).get(ApiResponse.class);
        //assert that we did not receive an error
        assertNull(response.getError());
    }

    /**
     * Get the client credentials for the current app
     * @return Credentials
     */
    public Credentials getAppCredentials() throws IOException {
        return this.app().credentials().get();
    }

    /**
     * Get the client credentials for the current organization
     * @return Credentials
     */
    public Credentials getOrgCredentials() throws IOException {
        String orgName = clientSetup.getOrganizationName().toLowerCase();
        return clientSetup.getRestClient().management().orgs().organization(orgName).credentials().get();

    }
}
