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
import org.junit.Ignore;
import org.junit.Test;
import org.apache.usergrid.cassandra.Concurrent;
import org.apache.usergrid.management.ApplicationInfo;
import org.apache.usergrid.management.OrganizationInfo;
import org.apache.usergrid.rest.AbstractRestIT;

import org.apache.shiro.codec.Base64;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.representation.Form;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.apache.usergrid.utils.MapUtils.hashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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

        OrganizationInfo orgInfo = setup.getMgmtSvc().getOrganizationByName( "test-organization" );

        String clientId = setup.getMgmtSvc().getClientIdForOrganization( orgInfo.getUuid() );
        String clientSecret = setup.getMgmtSvc().getClientSecretForOrganization( orgInfo.getUuid() );

        refreshIndex("test-organization", "test-app");

        JsonNode node = mapper.readTree( resource().path( "/test-organization/test-app/users" ).queryParam( "client_id", clientId )
                .queryParam( "client_secret", clientSecret ).accept( MediaType.APPLICATION_JSON )
                .type( MediaType.APPLICATION_JSON_TYPE ).get( String.class ));

        assertNotNull( node.get( "entities" ) );
    }

    @Test
    public void applicationWithAppCredentials() throws Exception {

        ApplicationInfo appInfo = setup.getMgmtSvc().getApplicationInfo( "test-organization/test-app" );

        String clientId = setup.getMgmtSvc().getClientIdForApplication( appInfo.getId() );
        String clientSecret = setup.getMgmtSvc().getClientSecretForApplication( appInfo.getId() );

        refreshIndex("test-organization", "test-app");

        JsonNode node = mapper.readTree( resource().path( "/test-organization/test-app/users" ).queryParam( "client_id", clientId )
                .queryParam( "client_secret", clientSecret ).accept( MediaType.APPLICATION_JSON )
                .type( MediaType.APPLICATION_JSON_TYPE ).get( String.class ));

        assertNotNull( node.get( "entities" ) );
    }

    /**
     * Verifies that we return JSON even when no accept header is specified.
     * (for backwards compatibility)
     */
    @Test
    public void jsonForNoAccepts() throws Exception {

        ApplicationInfo app = setup.getMgmtSvc().getApplicationInfo("test-organization/test-app");
        String clientId = setup.getMgmtSvc().getClientIdForApplication( app.getId() );
        String clientSecret = setup.getMgmtSvc().getClientSecretForApplication( app.getId() );

        refreshIndex("test-organization", "test-app");

        JsonNode node = mapper.readTree( resource()
                .path( "/test-organization/test-app" )
                .queryParam( "client_id", clientId )
                .queryParam( "client_secret", clientSecret )
                .get( String.class ));

        assertNotNull( node.get( "entities" ) );
    }

    /**
     * Verifies that we return JSON even when text/html is requested. 
     * (for backwards compatibility)
     */
    @Test
    public void jsonForAcceptsTextHtml() throws Exception {

        ApplicationInfo app = setup.getMgmtSvc().getApplicationInfo("test-organization/test-app");
        String clientId = setup.getMgmtSvc().getClientIdForApplication( app.getId() );
        String clientSecret = setup.getMgmtSvc().getClientSecretForApplication( app.getId() );

        refreshIndex("test-organization", "test-app");

        JsonNode node = mapper.readTree( resource()
                .path( "/test-organization/test-app" )
                .queryParam( "client_id", clientId )
                .queryParam( "client_secret", clientSecret )
                .accept( MediaType.TEXT_HTML )
                .get( String.class ));

        assertNotNull( node.get( "entities" ) );
    }

    @Test
    public void applicationWithJsonCreds() throws Exception {

        ApplicationInfo appInfo = setup.getMgmtSvc().getApplicationInfo( "test-organization/test-app" );

        String clientId = setup.getMgmtSvc().getClientIdForApplication( appInfo.getId() );
        String clientSecret = setup.getMgmtSvc().getClientSecretForApplication( appInfo.getId() );

        Map<String, String> payload = hashMap( "email", "applicationWithJsonCreds@usergrid.org" )
                .map( "username", "applicationWithJsonCreds" ).map( "name", "applicationWithJsonCreds" )
                .map( "password", "applicationWithJsonCreds" ).map( "pin", "1234" );

        JsonNode node = mapper.readTree( resource().path( "/test-organization/test-app/users" ).queryParam( "client_id", clientId )
                .queryParam( "client_secret", clientSecret ).accept( MediaType.APPLICATION_JSON )
                .type( MediaType.APPLICATION_JSON_TYPE ).post( String.class, payload ));

        assertNotNull( getEntity( node, 0 ) );

        refreshIndex("test-organization", "test-app");

        payload = hashMap( "username", "applicationWithJsonCreds" ).map( "password", "applicationWithJsonCreds" )
                .map( "grant_type", "password" );

        node = mapper.readTree( resource().path( "/test-organization/test-app/token" ).accept( MediaType.APPLICATION_JSON )
                .type( MediaType.APPLICATION_JSON_TYPE ).post( String.class, payload ));

        JsonNode token = node.get( "access_token" );

        assertNotNull( token );
    }


    @Test
    @Ignore("When run with all tests it fails with expected 3 but got 4, "
            + "but alone it succeeds: ApplicationResourceIT."
            + "rootApplicationWithOrgCredentials:139 expected:<3> but was:<4>")
    public void rootApplicationWithOrgCredentials() throws Exception {

        OrganizationInfo orgInfo = setup.getMgmtSvc().getOrganizationByName( "test-organization" );
        ApplicationInfo appInfo = setup.getMgmtSvc().getApplicationInfo( "test-organization/test-app" );

        String clientId = setup.getMgmtSvc().getClientIdForOrganization( orgInfo.getUuid() );
        String clientSecret = setup.getMgmtSvc().getClientSecretForOrganization( orgInfo.getUuid() );

        refreshIndex("test-organization", "test-app");

        JsonNode node = mapper.readTree( resource().path( "/" + appInfo.getId() ).queryParam( "client_id", clientId )
                .queryParam( "client_secret", clientSecret ).accept( MediaType.APPLICATION_JSON )
                .type( MediaType.APPLICATION_JSON_TYPE ).get( String.class ));

        // ensure the URI uses the properties file as a base
        assertEquals( node.get( "uri" ).textValue(), "http://sometestvalue/test-organization/test-app" );

        node = getEntity( node, 0 );
        assertEquals( "test-organization/test-app", node.get( "name" ).asText() );
        assertEquals( "Roles", node.get( "metadata" ).get( "collections" ).get( "roles" ).get( "title" ).asText() );

        // TODO - when run together with many tests this sees 4 instead of expected 3
        assertEquals( 3, node.get( "metadata" ).get( "collections" ).get( "roles" ).get( "count" ).asInt() );
    }


    @Test
    public void test_GET_credentials_ok() throws IOException {
        String mgmtToken = adminToken();

        refreshIndex("test-organization", "test-app");

        JsonNode node = mapper.readTree( resource().path( "/test-organization/test-app/credentials" ).queryParam( "access_token", mgmtToken )
                        .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE )
                        .get( String.class ));
        assertEquals( "ok", node.get( "status" ).textValue() );
        logNode( node );
    }


    @Test
    public void testResetAppCredentials() throws IOException {
        String mgmtToken = adminToken();

        refreshIndex("test-organization", "test-app");

        JsonNode node = mapper.readTree( resource().path( "/test-organization/test-app/credentials" ).queryParam( "access_token", mgmtToken )
                        .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE )
                        .post( String.class ));
        assertEquals( "ok", node.get( "status" ).textValue() );
        logNode( node );
    }


    @Test
    public void noAppDelete() throws IOException {
        String mgmtToken = adminToken();

        Status status = null;
        JsonNode node = null;

        refreshIndex("test-organization", "test-app");

        try {
            node = mapper.readTree( resource().path( "/test-organization/test-app" ).queryParam( "access_token", mgmtToken )
                    .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE )
                    .delete( String.class ));
        }
        catch ( UniformInterfaceException uie ) {
            status = uie.getResponse().getClientResponseStatus();
        }

        assertEquals( Status.NOT_IMPLEMENTED, status );
    }


    @Test
    public void ttlOverMax() throws Exception {

        Map<String, String> payload =
                hashMap( "grant_type", "password" ).map( "username", "test@usergrid.com" ).map( "password", "test" )
                        .map( "ttl", Long.MAX_VALUE + "" );

        Status responseStatus = null;

        try {
            resource().path( "/test-organization/test-app/token" ).accept( MediaType.APPLICATION_JSON )
                    .type( MediaType.APPLICATION_JSON_TYPE ).post( String.class, payload );
        }
        catch ( UniformInterfaceException uie ) {
            responseStatus = uie.getResponse().getClientResponseStatus();
        }

        assertEquals( Status.BAD_REQUEST, responseStatus );
    }


    @Test
    public void tokenTtl() throws Exception {

        long ttl = 2000;

        JsonNode node = mapper.readTree( resource().path( "/test-organization/test-app/token" ).queryParam( "grant_type", "password" )
                .queryParam( "username", "ed@anuff.com" ).queryParam( "password", "sesame" )
                .queryParam( "ttl", String.valueOf( ttl ) ).accept( MediaType.APPLICATION_JSON ).get( String.class ));

        long startTime = System.currentTimeMillis();

        String token = node.get( "access_token" ).textValue();

        assertNotNull( token );

        long expires_in = node.get( "expires_in" ).longValue();
        assertEquals( ttl, expires_in * 1000 );

        JsonNode userdata = mapper.readTree( resource().path( "/test-organization/test-app/users/ed@anuff.com" ).queryParam( "access_token", token )
                        .accept( MediaType.APPLICATION_JSON ).get( String.class ));

        assertEquals( "ed@anuff.com", getEntity( userdata, 0 ).get( "email" ).asText() );

        // wait for the token to expire
        Thread.sleep( ttl - ( System.currentTimeMillis() - startTime ) + 1000 );

        Status responseStatus = null;
        try {
            userdata = mapper.readTree( resource().path( "/test-organization/test-app/users/ed@anuff.com" )
                    .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE ).get( String.class ));
        }
        catch ( UniformInterfaceException uie ) {
            responseStatus = uie.getResponse().getClientResponseStatus();
        }

        assertEquals( Status.UNAUTHORIZED, responseStatus );
    }


    @Test
    public void ttlNan() throws Exception {

        Map<String, String> payload =
                hashMap( "grant_type", "password" ).map( "username", "ed@anuff.com" ).map( "password", "sesame" )
                        .map( "ttl", "derp" );

        Status responseStatus = null;
        try {
            resource().path( "/test-organization/test-app/token" ).accept( MediaType.APPLICATION_JSON )
                    .type( MediaType.APPLICATION_JSON_TYPE ).post( String.class, payload );
        }
        catch ( UniformInterfaceException uie ) {
            responseStatus = uie.getResponse().getClientResponseStatus();
        }

        assertEquals( Status.BAD_REQUEST, responseStatus );
    }


    @Test
    public void updateAccessTokenTtl() throws Exception {

        JsonNode node = mapper.readTree( resource().path( "/test-organization/test-app/token" ).queryParam( "grant_type", "password" )
                .queryParam( "username", "ed@anuff.com" ).queryParam( "password", "sesame" )
                .accept( MediaType.APPLICATION_JSON ).get( String.class ));

        String token = node.get( "access_token" ).textValue();
        logNode( node );
        assertNotNull( token );

        long expires_in = node.get( "expires_in" ).longValue();
        assertEquals( 604800, expires_in );

        Map<String, String> payload = hashMap( "accesstokenttl", "31536000000" );

        node = mapper.readTree( resource().path( "/test-organization/test-app" ).queryParam( "access_token", adminAccessToken )
                .type( MediaType.APPLICATION_JSON_TYPE ).put( String.class, payload ));
        logNode( node );

        node = mapper.readTree( resource().path( "/test-organization/test-app/token" ).queryParam( "grant_type", "password" )
                .queryParam( "username", "ed@anuff.com" ).queryParam( "password", "sesame" )
                .accept( MediaType.APPLICATION_JSON ).get( String.class ));

        assertEquals( 31536000, node.get( "expires_in" ).longValue() );
        logNode( node );
    }


    @Test
    public void authorizationCodeWithWrongCredentials() throws Exception {
        ApplicationInfo appInfo = setup.getMgmtSvc().getApplicationInfo( "test-organization/test-app" );
        String clientId = setup.getMgmtSvc().getClientIdForApplication( appInfo.getId() );

        refreshIndex("test-organization", "test-app");

        Form payload = new Form();
        payload.add( "username", "wrong_user" );
        payload.add( "password", "wrong_password" );
        payload.add( "response_type", "code" );
        payload.add( "client_id", clientId );
        payload.add( "scope", "none" );
        payload.add( "redirect_uri", "http://www.my_test.com" );

        String result = resource().path( "/test-organization/test-app/authorize" )
                .type( MediaType.APPLICATION_FORM_URLENCODED_TYPE ).accept( MediaType.TEXT_HTML )
                .post( String.class, payload );

    
        logger.debug("result: " + result);
        assertTrue( result.contains( "Username or password do not match" ) );
    }


    @Test
    public void authorizeWithInvalidClientIdRaisesError() throws Exception {
        String result =
                resource().path( "/test-organization/test-app/authorize" ).queryParam( "response_type", "token" )
                        .queryParam( "client_id", "invalid_client_id" )
                        .queryParam( "redirect_uri", "http://www.my_test.com" ).get( String.class );

        assertTrue( result.contains( "Unable to authenticate (OAuth). Invalid client_id." ) );
    }


    @Test
    public void authorizationCodeWithValidCredentials() throws Exception {
        ApplicationInfo appInfo = setup.getMgmtSvc().getApplicationInfo( "test-organization/test-app" );
        String clientId = setup.getMgmtSvc().getClientIdForApplication( appInfo.getId() );

        Form payload = new Form();
        payload.add( "username", "ed@anuff.com" );
        payload.add( "password", "sesame" );
        payload.add( "response_type", "code" );
        payload.add( "client_id", clientId );
        payload.add( "scope", "none" );
        payload.add( "redirect_uri", "http://www.my_test.com" );

        client().setFollowRedirects( false );

        Status status = null;
        try {
            String result = resource().path( "/test-organization/test-app/authorize" )
                    .type( MediaType.APPLICATION_FORM_URLENCODED_TYPE ).accept( MediaType.TEXT_HTML )
                    .post( String.class, payload );
        }
        catch ( UniformInterfaceException uie ) {
            status = uie.getResponse().getClientResponseStatus();
        }

        assertEquals( Status.TEMPORARY_REDIRECT, status );
    }


    @Test
    public void clientCredentialsFlowWithHeaderAuthorization() throws Exception {
        ApplicationInfo appInfo = setup.getMgmtSvc().getApplicationInfo( "test-organization/test-app" );
        String clientId = setup.getMgmtSvc().getClientIdForApplication( appInfo.getId() );
        String clientSecret = setup.getMgmtSvc().getClientSecretForApplication( appInfo.getId() );

        String clientCredentials = clientId + ":" + clientSecret;
        String token = Base64.encodeToString( clientCredentials.getBytes() );

        Form payload = new Form();
        payload.add( "grant_type", "client_credentials" );

        JsonNode node = mapper.readTree( resource().path( "/test-organization/test-app/token" ).header( "Authorization", "Basic " + token )
                        .type( MediaType.APPLICATION_FORM_URLENCODED_TYPE ).accept( MediaType.APPLICATION_JSON )
                        .post( String.class, payload ));

        assertNotNull( "It has access_token.", node.get( "access_token" ).textValue() );
        assertNotNull( "It has expires_in.", node.get( "expires_in" ).intValue() );
    }


    @Test
    public void clientCredentialsFlowWithPayload() throws Exception {
        ApplicationInfo appInfo = setup.getMgmtSvc().getApplicationInfo( "test-organization/test-app" );
        String clientId = setup.getMgmtSvc().getClientIdForApplication( appInfo.getId() );
        String clientSecret = setup.getMgmtSvc().getClientSecretForApplication( appInfo.getId() );

        Form payload = new Form();
        payload.add( "grant_type", "client_credentials" );
        payload.add( "client_id", clientId );
        payload.add( "client_secret", clientSecret );

        JsonNode node = mapper.readTree( resource().path( "/test-organization/test-app/token" )
                .type( MediaType.APPLICATION_FORM_URLENCODED_TYPE ).accept( MediaType.APPLICATION_JSON )
                .post( String.class, payload ));

        assertNotNull( "It has access_token.", node.get( "access_token" ).textValue() );
        assertNotNull( "It has expires_in.", node.get( "expires_in" ).intValue() );
    }


    @Test
    public void clientCredentialsFlowWithHeaderAuthorizationAndPayload() throws Exception {
        ApplicationInfo appInfo = setup.getMgmtSvc().getApplicationInfo( "test-organization/test-app" );
        String clientId = setup.getMgmtSvc().getClientIdForApplication( appInfo.getId() );
        String clientSecret = setup.getMgmtSvc().getClientSecretForApplication( appInfo.getId() );

        String clientCredentials = clientId + ":" + clientSecret;
        String token = Base64.encodeToString( clientCredentials.getBytes() );

        Map<String, String> payload = hashMap( "grant_type", "client_credentials" );

        JsonNode node = mapper.readTree( resource().path( "/test-organization/test-app/token" ).header( "Authorization", "Basic " + token )
                        .type( MediaType.APPLICATION_JSON_TYPE ).accept( MediaType.APPLICATION_JSON )
                        .post( String.class, payload ));

        assertNotNull( "It has access_token.", node.get( "access_token" ).textValue() );
        assertNotNull( "It has expires_in.", node.get( "expires_in" ).intValue() );
    }


    @Test
    public void validateApigeeApmConfigAPP() throws IOException {
        JsonNode node = null;

        try {
            node = mapper.readTree( resource().path( "/test-organization/test-app/apm/apigeeMobileConfig" ).get( String.class ));
            //if things are kosher then JSON should have value for instaOpsApplicationId
            assertTrue( "it's valid json for APM", node.has( "instaOpsApplicationId" ) );
        }
        catch ( UniformInterfaceException uie ) {
            ClientResponse response = uie.getResponse();
            //Validate that API exists
            assertTrue( "APM Config API exists", response.getStatus() != 404 ); //i.e It should not be "Not Found"
        }
    }


    @Test
    public void appTokenFromOrgCreds() throws Exception {

        OrganizationInfo orgInfo = setup.getMgmtSvc().getOrganizationByName( "test-organization" );

        String clientId = setup.getMgmtSvc().getClientIdForOrganization( orgInfo.getUuid() );
        String clientSecret = setup.getMgmtSvc().getClientSecretForOrganization( orgInfo.getUuid() );

        JsonNode node = mapper.readTree( resource().path( "/test-organization/test-app/token" ).queryParam( "client_id", clientId )
                .queryParam( "client_secret", clientSecret ).queryParam( "grant_type", "client_credentials" )
                .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE ).get( String.class ));

        assertNotNull( node.get( "access_token" ) );

        String accessToken = node.get( "access_token" ).asText();

        int ttl = node.get( "expires_in" ).asInt();

        //check it's 1 day, should be the same as the default
        assertEquals( 604800, ttl );

        node = mapper.readTree( resource().path( "/test-organization/test-app/users" ).queryParam( "access_token", accessToken )
                .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE ).get( String.class ));

        assertNotNull( node.get( "entities" ) );
    }


    @Test
    public void appTokenFromAppCreds() throws Exception {

        ApplicationInfo appInfo = setup.getMgmtSvc().getApplicationInfo( "test-organization/test-app" );

        String clientId = setup.getMgmtSvc().getClientIdForApplication( appInfo.getId() );
        String clientSecret = setup.getMgmtSvc().getClientSecretForApplication( appInfo.getId() );

        JsonNode node = mapper.readTree( resource().path( "/test-organization/test-app/token" ).queryParam( "client_id", clientId )
                .queryParam( "client_secret", clientSecret ).queryParam( "grant_type", "client_credentials" )
                .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE ).get( String.class ));

        assertNotNull( node.get( "access_token" ) );

        String accessToken = node.get( "access_token" ).asText();

        int ttl = node.get( "expires_in" ).asInt();

        //check it's 7 days, should be the same as the default
        assertEquals( 604800, ttl );

        node = mapper.readTree( resource().path( "/test-organization/test-app/users" ).queryParam( "access_token", accessToken )
                .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE ).get( String.class ));

        assertNotNull( node.get( "entities" ) );
    }
}
