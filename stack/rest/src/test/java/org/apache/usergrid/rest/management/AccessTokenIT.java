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

import java.io.IOException;
import java.util.Map;

import org.junit.Test;

import org.apache.usergrid.rest.test.resource.AbstractRestIT;
import org.apache.usergrid.rest.test.resource.model.ApiResponse;
import org.apache.usergrid.rest.test.resource.model.Entity;
import org.apache.usergrid.rest.test.resource.model.QueryParameters;
import org.apache.usergrid.rest.test.resource.model.Token;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.Response;

import static org.apache.usergrid.utils.MapUtils.hashMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;


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

        Response.Status responseStatus = null;
        try {
            management().users().user( clientSetup.getUsername() ).get( ApiResponse.class);
        } catch (ClientErrorException uie) {
            responseStatus = Response.Status.fromStatusCode( uie.getResponse().getStatus() );
        }

        assertEquals(Response.Status.UNAUTHORIZED, responseStatus);
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


    /**
     * Verify that we can POST and GET using the token that was returned.
     * @throws Exception
     */
    @Test
    public void meTokenPost() throws Exception {
        Map<String, String> payload
                = hashMap("grant_type", "password")
                .map("username", clientSetup.getUsername()).map("password", clientSetup.getPassword());

        Token token = management().me().post( Token.class, payload );

        assertNotNull( token );
        assertNotNull( token.getAccessToken() );
        management().token().setToken( token );

        refreshIndex();

        assertNotNull( management().me().get( Token.class ) );

    }


    /**
     * Verifies that we can POST using a form and GET using the token that was returned.
     * @throws IOException
     */
    @Test
    public void meTokenPostForm() throws IOException {

        Form form = new Form();
        form.param( "grant_type", "password" );
        form.param( "username", clientSetup.getUsername() );
        form.param( "password", clientSetup.getPassword() );

        Token adminToken = management().me().post( Token.class,form );

        assertNotNull( adminToken );
        assertNotNull( adminToken.getAccessToken() );

        refreshIndex();

        assertNotNull( management().me().get( Token.class ) );

    }


    /**
     * Checks we get approriate response when giving a bad ttl request
     * @throws Exception
     */
    @Test
    public void ttlNan() throws Exception {

        Map<String, String> payload = hashMap("grant_type", "password")
                .map("username", clientSetup.getUsername())
                .map("password", clientSetup.getPassword())
                .map("ttl", "derp");

        try {
            management().token().post( Token.class,payload );
        } catch (ClientErrorException uie) {
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), uie.getResponse().getStatus());
        }

    }

    /**
     * Checks we get approriate response when giving a bad ttl request
     * @throws Exception
     */
    @Test
    public void ttlOverMax() throws Exception {

        Map<String, String> payload = hashMap("grant_type", "password")
            .map("username", clientSetup.getUsername())
            .map("password", clientSetup.getPassword())
            .map("ttl", Long.MAX_VALUE + "");

        try {
            management().token().post( Token.class, payload );
        } catch (ClientErrorException uie) {
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), uie.getResponse().getStatus());
        }

    }

    /**
     * Tests that we can revoke all of the tokens that have been assigned to a specific user
     * @throws Exception
     */
    @Test
    public void revokeTokens() throws Exception {
        Token token1 = getAdminToken();
        Token token2 = getAdminToken();

        // using a superuser token, revoke all tokens associated with the admin user
        management().token().setToken( clientSetup.getSuperuserToken() );
        management().users().user( clientSetup.getUsername() ).revokeTokens().post(true , ApiResponse.class, null,null);

        refreshIndex();


        //test that token 1 doesn't work
        try {
            management().token().setToken( token1 );
            management().users().user( clientSetup.getUsername() ).get();
            fail( "Token1 should have been revoked" );
        }
        catch ( ClientErrorException uie ) {
            assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), uie.getResponse().getStatus());
        }


        //test that token 2 doesn't work
        try {
            management().token().setToken( token2 );
            management().users().user( clientSetup.getUsername() ).get();
            fail( "Token2 should have been revoked" );
        }
        catch ( ClientErrorException uie ) {
            assertEquals( Response.Status.UNAUTHORIZED.getStatusCode(), uie.getResponse().getStatus());
        }
    }

    /**
     * Tests that we can revoke a single token that has been assigned to a specific user
     * @throws Exception
     */
    @Test
    public void revokeSingleToken() throws Exception {
        Token token1 = getAdminToken();
        Token token2 = getAdminToken();

        // using a superuser token, revoke specific token associated with the admin user
        QueryParameters queryParameters = new QueryParameters();
        queryParameters.addParam( "token", token1.getAccessToken() );

        management().token().setToken( clientSetup.getSuperuserToken() );
        management().users().user( clientSetup.getUsername() ).revokeToken().post( false, ApiResponse.class,null,queryParameters );

        refreshIndex();


        //test that token 1 doesn't work
        try {
            management().token().setToken( token1 );
            management().users().user( clientSetup.getUsername() ).get();
            fail( "Token1 should have been revoked" );
        }
        catch ( ClientErrorException uie ) {
            assertEquals( Response.Status.UNAUTHORIZED.getStatusCode(), uie.getResponse().getStatus());
        }


        //test that token 2 still works
        try {
            management().token().setToken( token2 );
            management().users().user( clientSetup.getUsername() ).get();
        }
        catch ( ClientErrorException uie ) {
            fail( "Token2 shouldn't have been revoked" );

        }
    }
}
