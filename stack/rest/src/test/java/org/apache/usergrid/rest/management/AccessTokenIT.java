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
package org.apache.usergrid.rest.management;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.representation.Form;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.core.MediaType;
import org.apache.usergrid.rest.test.resource2point0.AbstractRestIT;
import org.apache.usergrid.rest.management.organizations.OrganizationsResource;
import org.apache.usergrid.rest.test.resource2point0.model.ApiResponse;
import org.apache.usergrid.rest.test.resource2point0.model.Entity;
import org.apache.usergrid.rest.test.resource2point0.model.Organization;
import org.apache.usergrid.rest.test.resource2point0.model.QueryParameters;
import org.apache.usergrid.rest.test.resource2point0.model.Token;

import static org.apache.usergrid.utils.MapUtils.hashMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.jclouds.rest.annotations.Api;
import org.junit.Ignore;
import org.junit.Test;


/**
 * Contains all tests that related to the Access Tokens on the management endpoint.
 */
public class AccessTokenIT extends AbstractRestIT {

    public AccessTokenIT() throws Exception {

    }

    @Test
    public void tokenTtl() throws Exception {

        long ttl = 2000;

        tokenSetup( ttl );

        long startTime = System.currentTimeMillis();
        Entity user = new Entity(management().users().user( clientSetup.getUsername() ).get(ApiResponse.class));


        assertEquals(clientSetup.getUsername(), user.get( "username" ));

        // wait for the token to expire
        Thread.sleep(ttl - (System.currentTimeMillis() - startTime) + 1000);

        ClientResponse.Status responseStatus = null;
        try {
            management().users().user( clientSetup.getUsername() ).get( ApiResponse.class);
        } catch (UniformInterfaceException uie) {
            responseStatus = uie.getResponse().getClientResponseStatus();
        }

        assertEquals(ClientResponse.Status.UNAUTHORIZED, responseStatus);
    }


    private Token tokenSetup( final long ttl ) {
        QueryParameters queryParameters = getQueryParameters( ttl );

        Token adminToken = management().token().
            get(Token.class, queryParameters );
        management().token().setToken( adminToken );

        return adminToken;
    }

    private Token tokenMeSetup( final long ttl ) {
        QueryParameters queryParameters = getQueryParameters( ttl );

        Token adminToken = management().me().
            get(Token.class, queryParameters );
        management().token().setToken( adminToken );

        return adminToken;
    }


    private QueryParameters getQueryParameters( final long ttl ) {
        QueryParameters queryParameters = new QueryParameters();
        queryParameters.addParam( "grant_type", "password" );
        queryParameters.addParam( "username", clientSetup.getUsername() );
        queryParameters.addParam( "password", clientSetup.getPassword());
        if(ttl != 0)
            queryParameters.addParam( "ttl", String.valueOf(ttl) );
        return queryParameters;
    }
    
    @Test
    public void meToken() throws Exception {
        tokenMeSetup( 0 );

        ApiResponse response = management().me().get(ApiResponse.class);

        assertNotNull( response );
        assertNotNull(response.getAccessToken());

        Map<String,Object> responseProperties = response.getProperties();


        assertNotNull( responseProperties.get( "passwordChanged" ) );
        assertNotNull(responseProperties.get("expires_in"));
        Map<String,Object> userProperties = ( Map<String, Object> ) responseProperties.get("user");
        assertNotNull( userProperties );
        //user verification
        assertNotNull(userProperties.get("uuid"));
        assertNotNull(userProperties.get("username"));
        assertNotNull(userProperties.get("email"));
        assertNotNull(userProperties.get("name"));
        assertNotNull(userProperties.get("properties"));

        Map<String,Object> org = ( Map<String, Object> ) userProperties.get("organizations");
        Map<String,Object> orgProperties = ( Map<String, Object> )
            org.get( clientSetup.getOrganizationName().toLowerCase() );

        assertNotNull(orgProperties);
        assertNotNull(orgProperties.get("name"));
        assertNotNull(orgProperties.get("properties"));
    }
//
//    @Test
//    public void meTokenPost() throws Exception {
//        Map<String, String> payload
//                = hashMap("grant_type", "password")
//                .map("username", "test@usergrid.com").map("password", "test");
//
//        JsonNode node = mapper.readTree(resource()
//                .path("/management/me")
//                .accept(MediaType.APPLICATION_JSON)
//                .type(MediaType.APPLICATION_JSON_TYPE)
//                .post(String.class, payload));
//
//        logNode(node);
//        String token = node.get("access_token").textValue();
//
//        assertNotNull(token);
//
//        refreshIndex("test-organization", "test-app");
//
//        node = mapper.readTree(resource()
//                .path("/management/me")
//                .queryParam("access_token", token)
//                .accept(MediaType.APPLICATION_JSON)
//                .get(String.class));
//        logNode(node);
//    }
//
//    @Test
//    public void meTokenPostForm() throws IOException {
//
//        Form form = new Form();
//        form.add("grant_type", "password");
//        form.add("username", "test@usergrid.com");
//        form.add("password", "test");
//
//        JsonNode node = mapper.readTree(resource()
//                .path("/management/me")
//                .accept(MediaType.APPLICATION_JSON)
//                .type(MediaType.APPLICATION_FORM_URLENCODED_TYPE)
//                .entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE)
//                .post(String.class));
//
//        logNode(node);
//        String token = node.get("access_token").textValue();
//
//        assertNotNull(token);
//
//        refreshIndex("test-organization", "test-app");
//
//        node = mapper.readTree(resource()
//                .path("/management/me")
//                .queryParam("access_token", token)
//                .accept(MediaType.APPLICATION_JSON).get(String.class));
//        logNode(node);
//    }
//
//    @Test
//    public void ttlNan() throws Exception {
//
//        Map<String, String> payload = hashMap("grant_type", "password")
//                .map("username", "test@usergrid.com")
//                .map("password", "test")
//                .map("ttl", "derp");
//
//        ClientResponse.Status responseStatus = null;
//        try {
//            resource().path("/management/token")
//                    .accept(MediaType.APPLICATION_JSON)
//                    .type(MediaType.APPLICATION_JSON_TYPE)
//                    .post(String.class, payload);
//        } catch (UniformInterfaceException uie) {
//            responseStatus = uie.getResponse().getClientResponseStatus();
//        }
//
//        assertEquals(ClientResponse.Status.BAD_REQUEST, responseStatus);
//    }
//
//    @Test
//    public void ttlOverMax() throws Exception {
//
//        Map<String, String> payload = hashMap("grant_type", "password")
//                .map("username", "test@usergrid.com")
//                .map("password", "test")
//                .map("ttl", Long.MAX_VALUE + "");
//
//        ClientResponse.Status responseStatus = null;
//
//        try {
//            resource().path("/management/token")
//                    .accept(MediaType.APPLICATION_JSON)
//                    .type(MediaType.APPLICATION_JSON_TYPE)
//                    .post(String.class, payload);
//        } catch (UniformInterfaceException uie) {
//            responseStatus = uie.getResponse().getClientResponseStatus();
//        }
//
//        assertEquals(ClientResponse.Status.BAD_REQUEST, responseStatus);
//    }
//
//    @Test
//    public void revokeToken() throws Exception {
//        String token1 = super.adminToken();
//        String token2 = super.adminToken();
//
//        JsonNode response = mapper.readTree(resource().path("/management/users/test")
//                .queryParam("access_token", token1)
//                .accept(MediaType.APPLICATION_JSON)
//                .type(MediaType.APPLICATION_JSON_TYPE)
//                .get(String.class));
//
//        assertEquals("test@usergrid.com", response.get("data").get("email").asText());
//
//        response = mapper.readTree(resource().path("/management/users/test")
//                .queryParam("access_token", token2)
//                .accept(MediaType.APPLICATION_JSON)
//                .type(MediaType.APPLICATION_JSON_TYPE)
//                .get(String.class));
//
//        assertEquals("test@usergrid.com", response.get("data").get("email").asText());
//
//        // now revoke the tokens
//        response = mapper.readTree(resource().path("/management/users/test/revoketokens")
//                .queryParam("access_token", superAdminToken())
//                .accept(MediaType.APPLICATION_JSON)
//                .type(MediaType.APPLICATION_JSON_TYPE)
//                .post(String.class));
//
//        refreshIndex("test-organization", "test-app");
//
//        // the tokens shouldn't work
//        ClientResponse.Status status = null;
//
//        try {
//            response = mapper.readTree(resource().path("/management/users/test")
//                    .queryParam("access_token", token1)
//                    .accept(MediaType.APPLICATION_JSON)
//                    .type(MediaType.APPLICATION_JSON_TYPE)
//                    .get(String.class));
//        } catch (UniformInterfaceException uie) {
//            status = uie.getResponse().getClientResponseStatus();
//        }
//
//        assertEquals(ClientResponse.Status.UNAUTHORIZED, status);
//
//        status = null;
//
//        try {
//            response = mapper.readTree(resource().path("/management/users/test")
//                    .queryParam("access_token", token2)
//                    .accept(MediaType.APPLICATION_JSON)
//                    .type(MediaType.APPLICATION_JSON_TYPE)
//                    .get(String.class));
//        } catch (UniformInterfaceException uie) {
//            status = uie.getResponse().getClientResponseStatus();
//        }
//
//        assertEquals(ClientResponse.Status.UNAUTHORIZED, status);
//
//        String token3 = super.adminToken();
//        String token4 = super.adminToken();
//
//        response = mapper.readTree(resource().path("/management/users/test")
//                .queryParam("access_token", token3)
//                .accept(MediaType.APPLICATION_JSON)
//                .type(MediaType.APPLICATION_JSON_TYPE)
//                .get(String.class));
//
//        assertEquals("test@usergrid.com", response.get("data").get("email").asText());
//
//        response = mapper.readTree(resource().path("/management/users/test")
//                .queryParam("access_token", token4)
//                .accept(MediaType.APPLICATION_JSON)
//                .type(MediaType.APPLICATION_JSON_TYPE)
//                .get(String.class));
//
//        assertEquals("test@usergrid.com", response.get("data").get("email").asText());
//
//        // now revoke the token3
//        response = mapper.readTree(resource().path("/management/users/test/revoketoken")
//                .queryParam("access_token", token3)
//                .queryParam("token", token3)
//                .accept(MediaType.APPLICATION_JSON)
//                .type(MediaType.APPLICATION_JSON_TYPE)
//                .post(String.class));
//
//        // the token3 shouldn't work
//        status = null;
//
//        try {
//            response = mapper.readTree(resource().path("/management/users/test")
//                    .queryParam("access_token", token3)
//                    .accept(MediaType.APPLICATION_JSON)
//                    .type(MediaType.APPLICATION_JSON_TYPE)
//                    .get(String.class));
//        } catch (UniformInterfaceException uie) {
//            status = uie.getResponse().getClientResponseStatus();
//        }
//
//        assertEquals(ClientResponse.Status.UNAUTHORIZED, status);
//
//        status = null;
//
//        try {
//            response = mapper.readTree(resource().path("/management/users/test")
//                    .queryParam("access_token", token4)
//                    .accept(MediaType.APPLICATION_JSON)
//                    .type(MediaType.APPLICATION_JSON_TYPE)
//                    .get(String.class));
//
//            status = ClientResponse.Status.OK;
//        } catch (UniformInterfaceException uie) {
//            status = uie.getResponse().getClientResponseStatus();
//        }
//
//        assertEquals(ClientResponse.Status.OK, status);
//    }
}
