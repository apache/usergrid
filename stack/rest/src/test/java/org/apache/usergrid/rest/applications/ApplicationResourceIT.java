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


import java.util.Map;

import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.jersey.api.client.WebResource;
import org.apache.usergrid.rest.test.resource.CollectionResource;
import org.apache.usergrid.rest.test.resource2point0.endpoints.ApplicationsResource;
import org.apache.usergrid.rest.test.resource2point0.endpoints.CollectionEndpoint;
import org.apache.usergrid.rest.test.resource2point0.endpoints.mgmt.OrganizationResource;
import org.apache.usergrid.rest.test.resource2point0.model.*;
import org.apache.usergrid.utils.MapUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.apache.usergrid.cassandra.Concurrent;
import org.apache.usergrid.management.ApplicationInfo;
import org.apache.usergrid.management.OrganizationInfo;

import org.apache.shiro.codec.Base64;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.representation.Form;
import java.io.IOException;

import static org.apache.usergrid.utils.MapUtils.hashMap;
import static org.junit.Assert.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.usergrid.rest.test.resource2point0.AbstractRestIT;


/**
 * Invokes methods on ApplicationResource
 *
 * @author zznate
 */
@Concurrent()
public class ApplicationResourceIT extends AbstractRestIT {
    private static final Logger logger = LoggerFactory.getLogger( ApplicationResourceIT.class );

    @Test
    public void applicationWithOrgCredentials() throws Exception {

        //Create the organization resource
        OrganizationResource orgResource=clientSetup.getRestClient().management().orgs().organization(clientSetup.getOrganizationName());

        //retrieve the credentials
        Credentials orgCredentials=orgResource.credentials().get();
        String clientId = orgCredentials.getClientId();
        String clientSecret = orgCredentials.getClientSecret();
        //Add the org credentials to the query
        QueryParameters params = new QueryParameters();
        params.addParam("client_id", clientId);
        params.addParam("client_secret", clientSecret);
        //retrieve the users collection using only the org credentials
        Collection users = this.app().collection("users").get(params, false);
        //make sure that a valid response is returned without error
        assertNotNull(users);
        assertNull(users.getResponse().getError());
    }

    @Test
    public void applicationWithAppCredentials() throws Exception {

        //retrieve the credentials
        Credentials appCredentials=this.app().credentials().get();
        String clientId = appCredentials.getClientId();
        String clientSecret = appCredentials.getClientSecret();
        //add the app credentials to the query
        QueryParameters params = new QueryParameters();
        params.addParam("client_id", clientId);
        params.addParam("client_secret", clientSecret);
        //retrieve the users collection using only the app credentials
        Collection users = this.app().collection("users").get(params, false);
        //make sure that a valid response is returned without error
        assertNotNull(users);
        assertNull(users.getResponse().getError());
    }

    /**
     * Verifies that we return JSON even when no accept header is specified.
     * (for backwards compatibility)
     */
    @Test
    public void jsonForNoAccepts() throws Exception {

        //Create the organization resource
        OrganizationResource orgResource=clientSetup.getRestClient().management().orgs().organization(clientSetup.getOrganizationName());
        CollectionEndpoint usersResource=this.app().collection("users");
        //retrieve the credentials
        Credentials orgCredentials=orgResource.credentials().get();
        String clientId = orgCredentials.getClientId();
        String clientSecret = orgCredentials.getClientSecret();
        //retrieve the users collection without setting the "Accept" header
        WebResource.Builder builder=resource().path(usersResource.getResource().getURI().getPath())
            //Add the org credentials to the query
            .queryParam("client_id", clientId)
            .queryParam("client_secret", clientSecret)
            .type(MediaType.APPLICATION_JSON_TYPE);

        ApiResponse apiResponse=builder.get(ApiResponse.class);
        Collection users = new Collection(apiResponse);
        //make sure that a valid response is returned without error
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
        OrganizationResource orgResource=clientSetup.getRestClient().management().orgs().organization(clientSetup.getOrganizationName());
        //create the "users" resource
        CollectionEndpoint usersResource=this.app().collection("users");
        //retrieve the credentials
        Credentials orgCredentials=orgResource.credentials().get();
        String clientId = orgCredentials.getClientId();
        String clientSecret = orgCredentials.getClientSecret();
        //Add the org credentials to the query
        QueryParameters params = new QueryParameters();
        params.addParam("client_id", clientId);
        params.addParam("client_secret", clientSecret);
        //retrieve the users collection, setting the "Accept" header to text/html
        ApiResponse apiResponse=resource().path(usersResource.getResource().getURI().getPath())
            .queryParam( "client_id", clientId )
            .queryParam( "client_secret", clientSecret )
            .accept( MediaType.TEXT_HTML )
            .type( MediaType.APPLICATION_JSON_TYPE )
            .get(ApiResponse.class);
        Collection users = new Collection(apiResponse);
        //make sure that a valid response is returned without error
        assertNotNull(users);
        assertNull(users.getResponse().getError());
    }

    @Test
    public void applicationWithJsonCreds() throws Exception {

        //Create the organization resource
        OrganizationResource orgResource=clientSetup.getRestClient().management().orgs().organization(clientSetup.getOrganizationName());
        CollectionEndpoint usersResource=this.app().collection("users");
        //retrieve the credentials
        Credentials orgCredentials=orgResource.credentials().get();
        String clientId = orgCredentials.getClientId();
        String clientSecret = orgCredentials.getClientSecret();
        //Add the org credentials to the query
        QueryParameters params = new QueryParameters();
        params.addParam("client_id", clientId);
        params.addParam("client_secret", clientSecret);

        User user = new User("applicationWithJsonCreds", "applicationWithJsonCreds", "applicationWithJsonCreds@usergrid.org", "applicationWithJsonCreds");
        user.put("pin", "1234");
        Entity entity = this.app().collection("users").post(user);

        assertNotNull( entity );

        refreshIndex();
        Token token=this.app().token().post(new Token("password", "applicationWithJsonCreds", "applicationWithJsonCreds"));

        assertNotNull( token );
    }


    @Test
//    @Ignore("When run with all tests it fails with expected 3 but got 4, "
//            + "but alone it succeeds: ApplicationResourceIT."
//            + "rootApplicationWithOrgCredentials:139 expected:<3> but was:<4>")
    public void rootApplicationWithOrgCredentials() throws Exception {

        String orgName=clientSetup.getOrganizationName().toLowerCase();
        String appName=clientSetup.getAppName().toLowerCase();
        //Create the organization resource
        OrganizationResource orgResource=clientSetup.getRestClient().management().orgs().organization(clientSetup.getOrganizationName());

        //retrieve the credentials
        Credentials orgCredentials=orgResource.credentials().get();
        String clientId = orgCredentials.getClientId();
        String clientSecret = orgCredentials.getClientSecret();

        ApiResponse apiResponse=resource().path(resource().path(String.format("/%s/%s",orgName, appName)).getURI().getPath())
            .queryParam( "client_id", clientId )
            .queryParam( "client_secret", clientSecret )
            .accept( MediaType.APPLICATION_JSON )
            .type( MediaType.APPLICATION_JSON_TYPE )
            .get(ApiResponse.class);

        // ensure the URI uses the properties file as a base
        assertEquals( apiResponse.getUri(), String.format("http://sometestvalue/%s/%s",orgName, appName) );

        Application application=new Application(apiResponse);
        Map<String, Object> roles = ((Map<String, Object>) application.getMap( "metadata" ).get( "collections" ).get( "roles" ));
        assertEquals( String.format("%s/%s",orgName, appName), application.get("name") );
        assertEquals( "Roles", (String) roles.get( "title" ) );

        // TODO - when run together with many tests this sees 4 instead of expected 3
        assertEquals( 3, Integer.parseInt(roles.get( "count" ).toString()) );
    }


    @Test
    public void test_GET_credentials_ok() throws IOException {
        String orgName=clientSetup.getOrganizationName().toLowerCase();
        String appName=clientSetup.getAppName().toLowerCase();
        String mgmtToken = this.getAdminToken().getAccessToken();
        ApiResponse apiResponse=resource().path(String.format("/%s/%s/credentials",orgName, appName))
            .queryParam( "access_token", mgmtToken )
            .accept( MediaType.APPLICATION_JSON )
            .type( MediaType.APPLICATION_JSON_TYPE )
            .get(ApiResponse.class);

        assertEquals( "ok", apiResponse.getStatus() );
    }


    @Test
    public void testResetAppCredentials() throws IOException {
        String orgName=clientSetup.getOrganizationName().toLowerCase();
        String appName=clientSetup.getAppName().toLowerCase();
        String mgmtToken = this.getAdminToken().getAccessToken();
        ApiResponse apiResponse=resource().path(String.format("/%s/%s/credentials",orgName, appName))
            .queryParam( "access_token", mgmtToken )
            .accept( MediaType.APPLICATION_JSON )
            .type( MediaType.APPLICATION_JSON_TYPE )
            .post(ApiResponse.class);
        assertEquals( "ok", apiResponse.getStatus() );
    }


    @Test
    @Ignore //This is implemented now
    public void noAppDelete() throws IOException {
        String orgName=clientSetup.getOrganizationName().toLowerCase();
        String appName=clientSetup.getAppName().toLowerCase();
        String mgmtToken = this.getAdminToken().getAccessToken();

        ApiResponse apiResponse=resource().path(String.format("/%s/%s",orgName, appName))
            .queryParam( "access_token", mgmtToken )
            .accept( MediaType.APPLICATION_JSON )
            .type( MediaType.APPLICATION_JSON_TYPE )
            .delete( ApiResponse.class );

        assertNotNull(apiResponse.getError());
    }
//
//
    @Test
    public void ttlOverMax() throws Exception {

        String orgName=clientSetup.getOrganizationName().toLowerCase();
        String appName=clientSetup.getAppName().toLowerCase();
        String username = "username";
        String name = "name";

        User user = new User(username, name, username + "@usergrid.org", "password");

        Entity entity = this.app().collection("users").post(user);
        assertNotNull(entity);

        entity.chainPut("grant_type", "password").chainPut("ttl", Long.MAX_VALUE);

        try {
            ApiResponse apiResponse=resource().path(String.format("/%s/%s/token",orgName, appName))
                .accept( MediaType.APPLICATION_JSON )
                .type( MediaType.APPLICATION_JSON_TYPE )
                .post(ApiResponse.class,entity);
            fail("This should cause an exception");
        }
        catch ( UniformInterfaceException uie ) {
            assertEquals(Status.BAD_REQUEST, uie.getResponse().getClientResponseStatus());
        }
    }


    @Test
    public void tokenTtl() throws Exception {

        long ttl = 2000;

        String orgName=clientSetup.getOrganizationName().toLowerCase();
        String appName=clientSetup.getAppName().toLowerCase();
        String username = "username";
        String name = "name";

        User user = new User(username, name, username + "@usergrid.org", "password");

        Entity entity = this.app().collection("users").post(user);
        assertNotNull(entity);

        ApiResponse apiResponse=resource().path(String.format("/%s/%s/token",orgName, appName))
            .queryParam( "grant_type", "password" )
            .queryParam( "username", username )
            .queryParam( "password", "password" )
            .queryParam( "ttl", String.valueOf( ttl ) )
            .accept( MediaType.APPLICATION_JSON )
            .type( MediaType.APPLICATION_JSON_TYPE )
            .get(ApiResponse.class);

        long startTime = System.currentTimeMillis();

        String token = apiResponse.getAccessToken();

        assertNotNull( token );

        long expires_in = Long.parseLong(apiResponse.getProperties().get("expires_in").toString());
        assertEquals( ttl, expires_in * 1000 );

        entity = this.app().collection("users").entity(entity).get(new QueryParameters().addParam("access_token", token), false);

        assertEquals( username + "@usergrid.org", (String)entity.get( "email" ) );

        // wait for the token to expire
        Thread.sleep( ttl - ( System.currentTimeMillis() - startTime ) + 1000 );

        try {
            entity = this.app().collection("users").entity(entity).get(new QueryParameters().addParam("access_token", token), false);
            fail("The expired token should cause an exception");
        }
        catch ( UniformInterfaceException uie ) {
            assertEquals( Status.UNAUTHORIZED.getStatusCode(), uie.getResponse().getStatus());
        }

    }


    @Test
    public void ttlNan() throws Exception {

        String orgName=clientSetup.getOrganizationName().toLowerCase();
        String appName=clientSetup.getAppName().toLowerCase();
        String username = "username";
        String name = "name";

        User user = new User(username, name, username + "@usergrid.org", "password");

        Entity entity = this.app().collection("users").post(user);
        assertNotNull(entity);

        try {
            ApiResponse apiResponse=resource().path(String.format("/%s/%s/token",orgName, appName))
                .queryParam( "grant_type", "password" )
                .queryParam( "username", username )
                .queryParam( "password", "password" )
                .queryParam( "ttl", "derp" )
                .accept( MediaType.APPLICATION_JSON )
                .type( MediaType.APPLICATION_JSON_TYPE )
                .get(ApiResponse.class);
            fail("The invalid TTL should cause an exception");

        }
        catch ( UniformInterfaceException uie ) {
            //TODO should this be handled and returned as a Status.BAD_REQUEST?
            assertEquals(Status.INTERNAL_SERVER_ERROR, uie.getResponse().getClientResponseStatus());
        }

    }


    @Test
    public void updateAccessTokenTtl() throws Exception {

        String orgName=clientSetup.getOrganizationName().toLowerCase();
        String appName=clientSetup.getAppName().toLowerCase();
        String username = "username";
        String name = "name";

        User user = new User(username, name, username + "@usergrid.org", "password");

        Entity entity = this.app().collection("users").post(user);
        assertNotNull(entity);

        ApiResponse apiResponse=resource().path(String.format("/%s/%s/token",orgName, appName))
            .queryParam( "grant_type", "password" )
            .queryParam( "username", username )
            .queryParam( "password", "password" )
            .accept( MediaType.APPLICATION_JSON )
            .type( MediaType.APPLICATION_JSON_TYPE )
            .get(ApiResponse.class);

        long startTime = System.currentTimeMillis();

        String token = apiResponse.getAccessToken();

        assertNotNull( token );

        long expires_in = Long.parseLong(apiResponse.getProperties().get("expires_in").toString());
        assertEquals( 604800, expires_in );

        entity = this.app().collection("users").entity(entity).get(new QueryParameters().addParam("access_token", token), false);

        assertEquals( username + "@usergrid.org", (String)entity.get( "email" ) );

        apiResponse=resource().path(String.format("/%s/%s",orgName, appName))
            .queryParam( "access_token", this.getAdminToken().getAccessToken() )
            .accept( MediaType.APPLICATION_JSON )
            .type( MediaType.APPLICATION_JSON_TYPE )
            .put(ApiResponse.class, new MapUtils.HashMapBuilder<String, String>().map("accesstokenttl", "31536000000"));
//        this.app().token()
        apiResponse=resource().path(String.format("/%s/%s/token",orgName, appName))
            .queryParam( "grant_type", "password" )
            .queryParam( "username", username )
            .queryParam( "password", "password" )
            .accept( MediaType.APPLICATION_JSON )
            .type( MediaType.APPLICATION_JSON_TYPE )
            .get(ApiResponse.class);

        assertEquals( 31536000, Long.parseLong(apiResponse.getProperties().get( "expires_in" ).toString()) );

    }


    @Test
    public void authorizationCodeWithWrongCredentials() throws Exception {
        String orgName=clientSetup.getOrganizationName().toLowerCase();
        String appName=clientSetup.getAppName().toLowerCase();
        //Create the organization resource
        OrganizationResource orgResource=clientSetup.getRestClient().management().orgs().organization(orgName);

        //retrieve the credentials
        Credentials orgCredentials=orgResource.credentials().get();
        String clientId = orgCredentials.getClientId();

        Form payload = new Form();
        payload.add( "username", "wrong_user" );
        payload.add( "password", "wrong_password" );
        payload.add( "response_type", "code" );
        payload.add( "client_id", clientId );
        payload.add( "scope", "none" );
        payload.add( "redirect_uri", "http://www.my_test.com" );

        String apiResponse=resource().path(String.format("/%s/%s/authorize",orgName, appName))
            .accept(MediaType.APPLICATION_JSON)
            .type(MediaType.APPLICATION_FORM_URLENCODED_TYPE)
            .post(String.class, payload);

        logger.debug("result: " + apiResponse);
        assertTrue( apiResponse.contains( "Username or password do not match" ) );
    }


    @Test
    public void authorizeWithInvalidClientIdRaisesError() throws Exception {
        String orgName=clientSetup.getOrganizationName().toLowerCase();
        String appName=clientSetup.getAppName().toLowerCase();
        String apiResponse=resource().path(String.format("/%s/%s/authorize",orgName, appName))
            .queryParam("response_type", "token")
            .queryParam("client_id", "invalid_client_id")
            .queryParam("redirect_uri", "http://www.my_test.com")
            .accept(MediaType.APPLICATION_JSON)
            .type(MediaType.APPLICATION_JSON_TYPE)
            .get(String.class);


        assertTrue( apiResponse.contains( "Unable to authenticate (OAuth). Invalid client_id." ) );
    }


    @Test
    public void authorizationCodeWithValidCredentials() throws Exception {
        String orgName=clientSetup.getOrganizationName().toLowerCase();
        String appName=clientSetup.getAppName().toLowerCase();
        //Create the organization resource
        OrganizationResource orgResource=clientSetup.getRestClient().management().orgs().organization(orgName);
        //retrieve the credentials
        Credentials orgCredentials=orgResource.credentials().get();
        String clientId = orgCredentials.getClientId();
        String clientSecret = orgCredentials.getClientSecret();

        String username = "username";
        String name = "name";

        User user = new User(username, name, username + "@usergrid.org", "password");

        Entity entity = this.app().collection("users").post(user);
        assertNotNull(entity);

        Form payload = new Form();
        payload.add( "username", username );
        payload.add( "password", "password" );
        payload.add( "response_type", "code" );
        payload.add( "grant_type","client_credentials" );
        payload.add( "client_id", clientId );
        payload.add( "client_secret", clientSecret );
        payload.add( "scope", "none" );
        payload.add( "redirect_uri", "http://www.my_test.com" );

        client().setFollowRedirects( false );

        try {
            resource().path(String.format("/%s/%s/authorize",orgName, appName))
                .accept( MediaType.APPLICATION_JSON )
                .type( MediaType.APPLICATION_FORM_URLENCODED_TYPE )
                .post(String.class, payload);
        }
        catch ( UniformInterfaceException uie ) {
            assertEquals( Status.TEMPORARY_REDIRECT, uie.getResponse().getClientResponseStatus() );
        }

    }


    @Test
    public void clientCredentialsFlowWithHeaderAuthorization() throws Exception {
        String orgName=clientSetup.getOrganizationName().toLowerCase();
        String appName=clientSetup.getAppName().toLowerCase();
        //Create the organization resource
        OrganizationResource orgResource=clientSetup.getRestClient().management().orgs().organization(clientSetup.getOrganizationName());

        //retrieve the credentials
        Credentials orgCredentials=orgResource.credentials().get();
        String clientId = orgCredentials.getClientId();
        String clientSecret = orgCredentials.getClientSecret();

        String clientCredentials = clientId + ":" + clientSecret;
        String token = Base64.encodeToString( clientCredentials.getBytes() );

        Form payload = new Form();
        payload.add( "grant_type", "client_credentials" );

        ApiResponse apiResponse=resource().path(String.format("/%s/%s/token", orgName, appName))
            .header("Authorization", "Basic " + token)
            .accept( MediaType.APPLICATION_JSON )
            .type( MediaType.APPLICATION_JSON_TYPE )
            .get(ApiResponse.class);
//        JsonNode node = mapper.readTree( resource().path( "/test-organization/test-app/token" ).header( "Authorization", "Basic " + token )
//                        .type( MediaType.APPLICATION_JSON_TYPE ).accept( MediaType.APPLICATION_JSON )
//                        .post( String.class, payload ));

        assertNotNull("It has access_token.", apiResponse.getAccessToken());
        assertNotNull("It has expires_in.", apiResponse.getProperties().get("expires_in"));
    }


    @Test
    public void clientCredentialsFlowWithPayload() throws Exception {
        String orgName=clientSetup.getOrganizationName().toLowerCase();
        String appName=clientSetup.getAppName().toLowerCase();
        //Create the organization resource
        OrganizationResource orgResource=clientSetup.getRestClient().management().orgs().organization(clientSetup.getOrganizationName());

        //retrieve the credentials
        Credentials orgCredentials=orgResource.credentials().get();
        String clientId = orgCredentials.getClientId();
        String clientSecret = orgCredentials.getClientSecret();

        Form payload = new Form();
        payload.add( "grant_type", "client_credentials" );
        payload.add( "client_id", clientId );
        payload.add( "client_secret", clientSecret );

        ApiResponse apiResponse=resource().path(String.format("/%s/%s/token",orgName, appName))
            .accept( MediaType.APPLICATION_JSON )
            .type( MediaType.APPLICATION_FORM_URLENCODED_TYPE )
            .post(ApiResponse.class, payload);

        assertNotNull( "It has access_token.", apiResponse.getAccessToken() );
        assertNotNull( "It has expires_in.", apiResponse.getProperties().get( "expires_in" ) );
    }


    @Test
    public void clientCredentialsFlowWithHeaderAuthorizationAndPayload() throws Exception {
        String orgName=clientSetup.getOrganizationName().toLowerCase();
        String appName=clientSetup.getAppName().toLowerCase();
        //Create the organization resource
        OrganizationResource orgResource=clientSetup.getRestClient().management().orgs().organization(clientSetup.getOrganizationName());

        //retrieve the credentials
        Credentials orgCredentials=orgResource.credentials().get();
        String clientId = orgCredentials.getClientId();
        String clientSecret = orgCredentials.getClientSecret();

        String clientCredentials = clientId + ":" + clientSecret;
        String token = Base64.encodeToString( clientCredentials.getBytes() );

        Map<String, String> payload = hashMap("grant_type", "client_credentials");

        ApiResponse apiResponse=resource().path(String.format("/%s/%s/token", orgName, appName))
            .header("Authorization", "Basic " + token)
            .accept(MediaType.APPLICATION_JSON)
            .type( MediaType.APPLICATION_JSON_TYPE )
            .post(ApiResponse.class, payload);

        assertNotNull( "It has access_token.", apiResponse.getAccessToken() );
        assertNotNull( "It has expires_in.", apiResponse.getProperties().get("expires_in") );
    }


    @Test
    public void validateApigeeApmConfigAPP() throws IOException {
        String orgName=clientSetup.getOrganizationName().toLowerCase();
        String appName=clientSetup.getAppName().toLowerCase();

        try {
            JsonNode node = mapper.readTree(resource().path(String.format("/%s/%s/apm/apigeeMobileConfig",orgName, appName))
                .accept( MediaType.APPLICATION_JSON )
                .type( MediaType.APPLICATION_JSON_TYPE )
                .get(String.class));

            //if things are kosher then JSON should have value for instaOpsApplicationId
            assertTrue( "it's valid json for APM", node.has( "instaOpsApplicationId" ) );
        }
        catch ( UniformInterfaceException uie ) {
            ClientResponse response = uie.getResponse();
            //Validate that API exists
            assertNotEquals("APM Config API exists", Status.NOT_FOUND, uie.getResponse().getStatus()); //i.e It should not be "Not Found"
        }
    }


    @Test
    public void appTokenFromOrgCreds() throws Exception {

        String orgName=clientSetup.getOrganizationName().toLowerCase();
        String appName=clientSetup.getAppName().toLowerCase();
        //Create the organization resource
        OrganizationResource orgResource=clientSetup.getRestClient().management().orgs().organization(clientSetup.getOrganizationName());

        //retrieve the credentials
        Credentials orgCredentials=orgResource.credentials().get();
        String clientId = orgCredentials.getClientId();
        String clientSecret = orgCredentials.getClientSecret();

        TokenResponse tokenResponse=resource().path(String.format("/%s/%s/token",orgName, appName))
            .queryParam("client_id", clientId)
            .queryParam("client_secret", clientSecret)
            .queryParam("grant_type", "client_credentials")
            .accept( MediaType.APPLICATION_JSON )
            .type( MediaType.APPLICATION_JSON_TYPE )
            .get(TokenResponse.class);

//        Token token=this.app().token().post(new Token("client_credentials", clientId, clientSecret));

        String accessToken = tokenResponse.getAccessToken();

        int ttl = Long.valueOf(tokenResponse.getExpiresIn()).intValue();

        //check it's 1 day, should be the same as the default
        assertEquals( 604800, ttl );

        ApiResponse apiResponse=resource().path(String.format("/%s/%s/users",orgName, appName))
            .queryParam( "access_token", accessToken )
            .accept( MediaType.APPLICATION_JSON )
            .type( MediaType.APPLICATION_JSON_TYPE )
            .get(ApiResponse.class);

        assertNull(apiResponse.getError());
    }


    @Test
    public void appTokenFromAppCreds() throws Exception {
        String orgName=clientSetup.getOrganizationName().toLowerCase();
        String appName=clientSetup.getAppName().toLowerCase();

        //retrieve the credentials
        Credentials appCredentials=this.app().credentials().get();
        String clientId = appCredentials.getClientId();
        String clientSecret = appCredentials.getClientSecret();

        TokenResponse tokenResponse=resource().path(String.format("/%s/%s/token",orgName, appName))
            .queryParam( "client_id", clientId )
            .queryParam( "client_secret", clientSecret )
            .queryParam( "grant_type", "client_credentials" )
            .accept( MediaType.APPLICATION_JSON )
            .type( MediaType.APPLICATION_JSON_TYPE )
            .get(TokenResponse.class);

        assertNotNull(tokenResponse);
//        Token token=this.app().token().post(new Token("client_credentials", clientId, clientSecret));

        String accessToken = tokenResponse.getAccessToken();
//
        int ttl = (int)tokenResponse.getExpiresIn();

        //check it's 1 day, should be the same as the default
        assertEquals( 604800, ttl );

        refreshIndex();

//        Collection users=this.app().collection("users").get(new QueryParameters().addParam("access_token", accessToken), false);
        Collection users=resource().path(String.format("/%s/%s/users",orgName, appName))
            .queryParam( "access_token", accessToken )
            .accept( MediaType.APPLICATION_JSON )
            .type( MediaType.APPLICATION_JSON_TYPE )
            .get(Collection.class);

        assertNotNull( users );

    }
}
