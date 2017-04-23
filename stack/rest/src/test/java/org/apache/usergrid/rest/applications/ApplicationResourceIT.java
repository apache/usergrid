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

import junit.framework.Assert;
import net.jcip.annotations.NotThreadSafe;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.shiro.codec.Base64;
import org.apache.usergrid.cassandra.SpringResource;
import org.apache.usergrid.management.ManagementService;
import org.apache.usergrid.rest.test.resource.AbstractRestIT;
import org.apache.usergrid.rest.test.resource.model.*;
import org.apache.usergrid.setup.ConcurrentProcessSingleton;
import org.apache.usergrid.utils.MapUtils;
import org.glassfish.jersey.client.ClientProperties;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.apache.usergrid.utils.MapUtils.hashMap;
import static org.junit.Assert.*;


/**
 * Invokes methods on ApplicationResource
 */
@NotThreadSafe
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
        ApiResponse apiResponse = this.org().app( clientSetup.getAppName() ).getTarget( false )
            .queryParam("grant_type", "client_credentials")
            .queryParam("client_id", orgCredentials.getClientId())
            .queryParam( "client_secret", orgCredentials.getClientSecret() )
            .request()
            .accept( MediaType.APPLICATION_JSON )
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
        ApiResponse apiResponse = this.app().getTarget( false )
            .queryParam("grant_type", "client_credentials")
            .queryParam("client_id", appCredentials.getClientId())
            .queryParam("client_secret", appCredentials.getClientSecret())
            .request()
            .get(ApiResponse.class);
        //assert that a valid response is returned without error
        assertNotNull(apiResponse);
        assertNull(apiResponse.getError());
    }

    /**
     * Retrieve an collection using the application client credentials
     */
    @Test
    public void applicationCollectionWithAppCredentials() throws Exception {

        //retrieve the credentials
        Credentials appCredentials = getAppCredentials();

        //retrieve the app using only the org credentials
        ApiResponse apiResponse = this.app().collection( "roles" ).getTarget( false )
            .queryParam( "grant_type", "client_credentials" )
            .queryParam("client_id", appCredentials.getClientId())
            .queryParam("client_secret", appCredentials.getClientSecret())
            .request()
            .accept(MediaType.APPLICATION_JSON)
            .get(ApiResponse.class);
        //assert that a valid response is returned without error
        assertNotNull(apiResponse);
        assertNull(apiResponse.getError());

        Collection roles = new Collection(apiResponse);
        //assert that we have the correct number of default roles
        assertEquals(3, roles.getNumOfEntities());
    }

    /**
     * Retrieve an collection using the application client credentials.
     */
    @Test
    public void applicationCollectionWithAppToken() throws Exception {

        String username = RandomStringUtils.randomAlphanumeric( 20 );
        String orgName = "MiXedApplicationResourceTest_" + username;
        String appName = "mgmt-org-app-test";

        // create new org with mixed case name

        Map payload = hashMap( "email", username + "@example.com" )
            .map( "username", username )
            .map( "name", "App Creds User" )
            .map( "password", "password" )
            .map( "organization", orgName );

        QueryParameters tokenParams = new QueryParameters();
        tokenParams.addParam( "access_token", getAdminToken("superuser","superpassword").getAccessToken() );

        pathResource( "management/organizations" ).post( false, payload, tokenParams );

        // create new app

        Map<String, String> data = new HashMap<String, String>();
        data.put( "name", appName );

        ApiResponse appResponse = pathResource( "management/orgs/" + orgName + "/applications" )
            .post( false, data, tokenParams );
        UUID appId = appResponse.getEntities().get(0).getUuid();

        // wait for app to become available and then get app creds

        String clientId = null;
        String clientSecret = null;

        Map<String, String> loginMap = new HashMap<String, String>() {{
            put("username", username);
            put("password", "password");
            put("grant_type", "password");
        }};
        ApiResponse authResponse = pathResource( "management/token" ).post( loginMap );

        // TODO: rewrite to use REST rather

        int tries = 0;
        SpringResource springResource = ConcurrentProcessSingleton.getInstance().getSpringResource();
        ManagementService mgmt = springResource.getBean( ManagementService.class );
        while ( tries++ < 20 ) {
            try {
                clientId = mgmt.getClientIdForApplication( appId );
                clientSecret = mgmt.getClientSecretForApplication( appId );
            } catch ( Exception intentionallyIgnored ) {}
            if ( clientId != null && clientSecret != null ) {
                break;
            }
            logger.info( "Waiting for app to become available" );
            Thread.sleep(500);
            waitForQueueDrainAndRefreshIndex();
        }
        assertNotNull( clientId );
        assertNotNull( clientSecret );

        QueryParameters adminTokenParams = new QueryParameters();
        adminTokenParams
            .addParam( "grant_type", "client_credentials" )
            .addParam( "client_id", clientId )
            .addParam( "client_secret", clientSecret );

        ApiResponse rolesResponse = pathResource( orgName.toLowerCase() + "/" + appName + "/roles" )
            .get( ApiResponse.class, adminTokenParams, false );

        assertTrue( rolesResponse.getEntityCount() > 0 );

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
        Invocation.Builder builder = this.app().collection( "users" ).getTarget( false )
            //Add the org credentials to the query
            .queryParam( "grant_type", "client_credentials" )
            .queryParam("client_id", orgCredentials.getClientId() )
            .queryParam( "client_secret", orgCredentials.getClientSecret() )
            .request();

        ApiResponse apiResponse = builder.get(ApiResponse.class);
        Collection users = new Collection(apiResponse);
        //assert that a valid response is returned without error
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

        waitForQueueDrainAndRefreshIndex();

        //retrieve the app using a username and password
        QueryParameters params = new QueryParameters()
            .addParam("grant_type", "password")
            .addParam("username", "applicationWithJsonCreds")
            .addParam("password", "applicationWithJsonCreds");
        Token apiResponse = this.app().token().post(params);

        //assert that a valid response is returned without error
        assertNotNull(apiResponse);
        assertNotNull(apiResponse.getAccessToken());
        assertNotNull(apiResponse.getExpirationDate());
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

        ApiResponse apiResponse = this.app().getTarget( false )
            .queryParam( "grant_type", "client_credentials" )
            .queryParam("client_id", orgCredentials.getClientId())
            .queryParam( "client_secret", orgCredentials.getClientSecret() )
            .request()
            .accept( MediaType.APPLICATION_JSON )
            .get(ApiResponse.class);

        // assert that the response returns the correct URI
        assertEquals(String.format("http://localhost:8080/%s/%s", orgName, appName), apiResponse.getUri());

        //unmarshal the application from the response
        Application application = new Application(apiResponse);

        //assert that the application name is correct
        assertEquals(String.format("%s/%s", orgName, appName), application.get("name"));

        //retrieve the application's roles collection
        apiResponse = this.app().collection( "roles" ).getTarget( false )
            .queryParam( "grant_type", "client_credentials" )
            .queryParam("client_id", orgCredentials.getClientId())
            .queryParam( "client_secret", orgCredentials.getClientSecret() )
            .request()
            .accept( MediaType.APPLICATION_JSON )
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
        waitForQueueDrainAndRefreshIndex();

        //add a ttl to the entity that is greater than the maximum
        entity.chainPut("grant_type", "password").chainPut("ttl", Long.MAX_VALUE);

        try {
            //POST the updated TTL, anticipating an exception
            target().path(String.format("/%s/%s/token", orgName, appName))
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .post( javax.ws.rs.client.Entity.entity(entity, MediaType.APPLICATION_JSON_TYPE ), ApiResponse.class);
            fail("This should cause an exception");
        } catch (ClientErrorException uie) {
            assertEquals(
                String.valueOf( Response.Status.BAD_REQUEST.getStatusCode()),
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
        waitForQueueDrainAndRefreshIndex();

        //Retrieve an authentication token for the user, setting the TTL
        Token apiResponse = target().path( String.format( "/%s/%s/token", orgName, appName ) )
            .queryParam("grant_type", "password")
            .queryParam("username", username)
            .queryParam("password", "password")
            .queryParam( "ttl", String.valueOf( ttl ) )
            .request()
            .accept( MediaType.APPLICATION_JSON )
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
        } catch (ClientErrorException uie) {
            assertEquals( Response.Status.UNAUTHORIZED.getStatusCode(), uie.getResponse().getStatus());
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
        waitForQueueDrainAndRefreshIndex();

        try {
            //Retrieve a token for the new user, setting the TTL to an invalid value
            target().path( String.format( "/%s/%s/token", orgName, appName ) )
                .queryParam( "grant_type", "password" )
                .queryParam("username", username)
                .queryParam("password", "password")
                .queryParam("ttl", "derp")
                .request()
                .accept( MediaType.APPLICATION_JSON )
                .get( ApiResponse.class );
            fail("The invalid TTL should cause an exception");

        } catch (InternalServerErrorException uie) {
            // TODO should this be handled and returned as a Status.BAD_REQUEST?
            //Status.INTERNAL_SERVER_ERROR is thrown because Jersey throws a NumberFormatException
            assertEquals( Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), uie.getResponse().getStatus());
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
        waitForQueueDrainAndRefreshIndex();
        //Retrieve an authentication token for the user
        Token tokenResponse = this.app().getTarget( false ).path( "token" )
            .queryParam( "grant_type", "password" )
            .queryParam( "username", username )
            .queryParam("password", "password")
            .request()
            .accept( MediaType.APPLICATION_JSON )
            .get( Token.class );

        String token = tokenResponse.getAccessToken();
        assertNotNull(token);

        //Retrieve the expiration time of the token. Should be set to the default of 1 day
        long expires_in = tokenResponse.getExpirationDate();
        assertEquals(604800, expires_in);

        //Set the default TTL of the application to a date far in the future
        final MapUtils.HashMapBuilder<String, String> map =
            new MapUtils.HashMapBuilder<String, String>().map( "accesstokenttl", "31536000000" );
        this.app().getTarget(true, clientSetup.getSuperuserToken())
            .queryParam( "access_token", token )
            .request()
            .accept( MediaType.APPLICATION_JSON )
            .put( javax.ws.rs.client.Entity.entity( map, MediaType.APPLICATION_JSON_TYPE ), Token.class );

        //Create a new token for the user
        tokenResponse = this.app().token().getTarget( false )
            .queryParam("grant_type", "password")
            .queryParam("username", username)
            .queryParam( "password", "password" )
            .request()
            .accept( MediaType.APPLICATION_JSON )
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
        payload.param( "username", "wrong_user" );
        payload.param( "password", "wrong_password" );
        payload.param( "response_type", "code" );
        payload.param( "scope", "none" );
        payload.param( "redirect_uri", "http://www.my_test.com" );

        //POST the form to the authorization endpoint
        String apiResponse = clientSetup.getRestClient().management().authorize().getTarget()
            .request()
            .accept( MediaType.TEXT_HTML )
            .post( javax.ws.rs.client.Entity.form( payload ), String.class );

        //Assert that an appropriate error message is returned
        assertTrue(apiResponse.contains("Username or password do not match"));
    }


    /**
     * retrieve an oauth authorization using invalid application client credentials
     */
    @Test
    //we have authorize response only with username/password - client_id/secret not considered
    public void authorizeWithInvalidClientIdRaisesError() throws Exception {
        //GET the application authorization endpoint using bogus client credentials
        String apiResponse = clientSetup.getRestClient().management().authorize().getTarget( true )
            .queryParam( "response_type", "code" )
            .queryParam("client_id", "invalid_client_id")
            .queryParam("redirect_uri", "http://www.my_test.com")
            .request()
            .accept( MediaType.TEXT_HTML )
            .get(String.class);
        //Assert that an appropriate error message is returned
        //assertTrue(apiResponse.contains("Unable to authenticate (OAuth). Invalid client_id."));
    }

    /**
     * Retrieve an oauth authorization using valid client credentials
     */
    @Test
    //we have authorize response only with username/password - client_id/secret not considered
    public void authorizationCodeWithValidCredentials() throws Exception {
        //retrieve the credentials
        Credentials orgCredentials = getOrgCredentials();

        //Create form input with valid credentials
        Form payload = new Form();
        payload.param( "response_type", "code" );
        payload.param( "grant_type", "client_credentials" );
        payload.param( "client_id", orgCredentials.getClientId() );
        payload.param( "client_secret", orgCredentials.getClientSecret() );
        payload.param( "scope", "none" );
        payload.param( "redirect_uri", "http://www.my_test.com" );

        //Set the client to not follow the initial redirect returned by the stack

        try {
            //POST the form to the authorization endpoint
            clientSetup.getRestClient().management().authorize()
                .getTarget()
                .property( ClientProperties.FOLLOW_REDIRECTS, Boolean.TRUE )
                .request()
                .accept( MediaType.TEXT_HTML )
                .post( javax.ws.rs.client.Entity.form( payload ), String.class );
        } catch (ClientErrorException uie) {
            assertEquals(String.valueOf( Response.Status.TEMPORARY_REDIRECT.getStatusCode()), uie.getResponse().getStatus());
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

        Map<String, String> map = new HashMap<>(1);
        map.put("grant_type", "client_credentials");
        //GET the token endpoint, adding the basic auth header
        Token apiResponse = clientSetup.getRestClient().management().token().getTarget( false )
            //add the auth header
            .request()
            .header( "Authorization", "Basic " + token )
            .accept(MediaType.APPLICATION_JSON)
            .post( javax.ws.rs.client.Entity.entity(map, MediaType.APPLICATION_JSON_TYPE), Token.class );

        //Assert that a valid token with a valid TTL is returned
        assertNotNull("A valid response was returned.", apiResponse);
        assertNull("There is no error.", apiResponse.getError());
        assertNotNull("It has access_token.", apiResponse.getAccessToken());
        assertNotNull("It has expires_in.", apiResponse.getExpirationDate());
    }

    /**
     * Retrieve an app user access token using HTTP Basic authentication
     */
    @Test
    public void clientCredentialsFlowWithHeaderAuthorization() throws Exception {

        // get app credentials from /<org>/<app>/credentials end-point (using admin credentials)
        Credentials appCredentials = getAppCredentials();
        String clientId = appCredentials.getClientId();
        String clientSecret = appCredentials.getClientSecret();

        // use app credentials to admin user access token
        Token token = clientSetup.getRestClient().management().token()
            .post(Token.class,new Token("client_credentials", clientId, clientSecret));

        String clientCredentials = clientId + ":" + clientSecret;
        String encodedToken = Base64.encodeToString( clientCredentials.getBytes() );

        Map<String, String> payload = hashMap( "grant_type", "client_credentials" );

        // use admin user access token to get app user access token
        Token apiResponse = this.app().token().getTarget( false ).request()
            //add the auth header
            .header( "Authorization", "Basic " + encodedToken )
            .accept( MediaType.APPLICATION_JSON )
            .post(javax.ws.rs.client.Entity.entity(payload, MediaType.APPLICATION_JSON_TYPE ), Token.class );

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
        payload.param( "grant_type", "client_credentials" );
        payload.param( "client_id", clientId );
        payload.param( "client_secret", clientSecret );

        //POST the form to the application token endpoint
        Token apiResponse = this.app().token().getTarget( false ).request()
            .accept( MediaType.APPLICATION_JSON )
            .post( javax.ws.rs.client.Entity.form(payload), Token.class);

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
        Token apiResponse = this.app().token().getTarget( false ).request()
            .header( "Authorization", "Basic " + token )
            .accept( MediaType.APPLICATION_JSON )
            .post(javax.ws.rs.client.Entity.entity(
                hashMap("grant_type", "client_credentials"), MediaType.APPLICATION_JSON_TYPE), Token.class);

        //Assert that a valid token with a valid TTL is returned
        assertNotNull("It has access_token.", apiResponse.getAccessToken());
        assertNotNull("It has expires_in.", apiResponse.getExpirationDate());
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
        ApiResponse response = this.app().collection( "users" ).getTarget( true, token ).request()
            .get(ApiResponse.class);
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
        ApiResponse response = this.app().collection( "users" ).getTarget( true, token ).request()
            .get( ApiResponse.class);
        //assert that we did not receive an error
        assertNull(response.getError());
    }

    @Test
    public void getApmConfig(){
        try {
            Collection collection = this.app().collection("apm/apigeeMobileConfig").get();
            fail();
        } catch (NotFoundException e){
            Assert.assertEquals(404, e.getResponse().getStatus());
        }
    }

    /**
     * Get the client credentials for the current app
     * @return Credentials
     */
    public Credentials getAppCredentials() throws IOException {
        return new Credentials (this.app().credentials().get(ApiResponse.class,null,true));
    }

    /**
     * Get the client credentials for the current organization
     * @return Credentials
     */
    public Credentials getOrgCredentials() throws IOException {
        String orgName = clientSetup.getOrganizationName().toLowerCase();
        return new Credentials( clientSetup.getRestClient().management()
            .orgs().org( orgName ).credentials().get(ApiResponse.class,null,true) );

    }
}
