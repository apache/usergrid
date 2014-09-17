package org.apache.usergrid.rest.management;


import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.junit.Rule;
import org.junit.Test;

import org.apache.usergrid.rest.AbstractRestIT;
import org.apache.usergrid.rest.TestContextSetup;
import org.apache.usergrid.rest.management.organizations.OrganizationsResource;
import org.apache.usergrid.rest.test.security.TestAdminUser;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.representation.Form;

import static org.apache.usergrid.utils.MapUtils.hashMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


/**
 * Created by ApigeeCorporation on 9/17/14.
 */


/**
 * Contains all tests that related to the Access Tokens on the management endpoint.
 */
public class AccessTokenIT extends AbstractRestIT {

    public AccessTokenIT() throws Exception{

    }

    @Test
    public void tokenTtl() throws Exception {

        long ttl = 2000;

        JsonNode node = mapper.readTree( resource().path( "/management/token" ).queryParam( "grant_type", "password" )
                                                   .queryParam( "username", "test@usergrid.com" ).queryParam( "password", "test" )
                                                   .queryParam( "ttl", String.valueOf( ttl ) ).accept( MediaType.APPLICATION_JSON )
                                                   .get( String.class ));

        long startTime = System.currentTimeMillis();

        String token = node.get( "access_token" ).textValue();

        assertNotNull( token );

        JsonNode userdata = mapper.readTree( resource().path( "/management/users/test@usergrid.com" ).queryParam( "access_token", token )
                                                       .accept( MediaType.APPLICATION_JSON ).get( String.class ));

        assertEquals( "test@usergrid.com", userdata.get( "data" ).get( "email" ).asText() );

        // wait for the token to expire
        Thread.sleep( ttl - ( System.currentTimeMillis() - startTime ) + 1000 );

        ClientResponse.Status responseStatus = null;
        try {
            userdata = mapper.readTree( resource().path( "/management/users/test@usergrid.com" ).accept( MediaType.APPLICATION_JSON )
                                                  .type( MediaType.APPLICATION_JSON_TYPE ).get( String.class ));
        }
        catch ( UniformInterfaceException uie ) {
            responseStatus = uie.getResponse().getClientResponseStatus();
        }

        assertEquals( ClientResponse.Status.UNAUTHORIZED, responseStatus );
    }


    @Test
    public void token() throws Exception {
        JsonNode node = mapper.readTree( resource().path( "/management/token" ).queryParam( "grant_type", "password" )
                                                   .queryParam( "username", "test@usergrid.com" ).queryParam( "password", "test" )
                                                   .accept( MediaType.APPLICATION_JSON ).get( String.class ));

        logNode( node );
        String token = node.get( "access_token" ).textValue();
        assertNotNull( token );

        // set an organization property
        HashMap<String, Object> payload = new HashMap<String, Object>();
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put( "securityLevel", 5 );
        payload.put( OrganizationsResource.ORGANIZATION_PROPERTIES, properties );
        node = mapper.readTree( resource().path( "/management/organizations/test-organization" )
                                          .queryParam( "access_token", superAdminToken() ).accept( MediaType.APPLICATION_JSON )
                                          .type( MediaType.APPLICATION_JSON_TYPE ).put( String.class, payload ));

        refreshIndex("test-organization", "test-app");

        // ensure the organization property is included
        node = mapper.readTree( resource().path( "/management/token" ).queryParam( "access_token", token )
                                          .accept( MediaType.APPLICATION_JSON ).get( String.class ));
        logNode( node );

        JsonNode securityLevel = node.findValue( "securityLevel" );
        assertNotNull( securityLevel );
        assertEquals( 5L, securityLevel.asLong() );
    }


    @Test
    public void meToken() throws Exception {
        JsonNode node = mapper.readTree( resource().path( "/management/me" ).queryParam( "grant_type", "password" )
                                                   .queryParam( "username", "test@usergrid.com" ).queryParam( "password", "test" )
                                                   .accept( MediaType.APPLICATION_JSON ).get( String.class ));

        logNode( node );
        String token = node.get( "access_token" ).textValue();
        assertNotNull( token );

        node = mapper.readTree( resource().path( "/management/me" ).queryParam( "access_token", token )
                                          .accept( MediaType.APPLICATION_JSON ).get( String.class ));
        logNode( node );

        assertNotNull( node.get( "passwordChanged" ) );
        assertNotNull( node.get( "access_token" ) );
        assertNotNull( node.get( "expires_in" ) );
        JsonNode userNode = node.get( "user" );
        assertNotNull( userNode );
        assertNotNull( userNode.get( "uuid" ) );
        assertNotNull( userNode.get( "username" ) );
        assertNotNull( userNode.get( "email" ) );
        assertNotNull( userNode.get( "name" ) );
        assertNotNull( userNode.get( "properties" ) );
        JsonNode orgsNode = userNode.get( "organizations" );
        assertNotNull( orgsNode );
        JsonNode orgNode = orgsNode.get( "test-organization" );
        assertNotNull( orgNode );
        assertNotNull( orgNode.get( "name" ) );
        assertNotNull( orgNode.get( "properties" ) );
    }


    @Test
    public void meTokenPost() throws Exception {
        Map<String, String> payload =
                hashMap( "grant_type", "password" ).map( "username", "test@usergrid.com" ).map( "password", "test" );

        JsonNode node = mapper.readTree( resource().path( "/management/me" ).accept( MediaType.APPLICATION_JSON )
                                                   .type( MediaType.APPLICATION_JSON_TYPE ).post( String.class, payload ));

        logNode( node );
        String token = node.get( "access_token" ).textValue();

        assertNotNull( token );

        refreshIndex("test-organization", "test-app");

        node = mapper.readTree( resource().path( "/management/me" ).queryParam( "access_token", token )
                                          .accept( MediaType.APPLICATION_JSON ).get( String.class ));
        logNode( node );
    }


    @Test
    public void meTokenPostForm() throws IOException {

        Form form = new Form();
        form.add( "grant_type", "password" );
        form.add( "username", "test@usergrid.com" );
        form.add( "password", "test" );

        JsonNode node = mapper.readTree( resource().path( "/management/me" ).accept( MediaType.APPLICATION_JSON )
                                                   .type( MediaType.APPLICATION_FORM_URLENCODED_TYPE )
                                                   .entity( form, MediaType.APPLICATION_FORM_URLENCODED_TYPE ).post( String.class ));

        logNode( node );
        String token = node.get( "access_token" ).textValue();

        assertNotNull( token );

        refreshIndex("test-organization", "test-app");

        node = mapper.readTree( resource().path( "/management/me" ).queryParam( "access_token", token )
                                          .accept( MediaType.APPLICATION_JSON ).get( String.class ));
        logNode( node );
    }


    @Test
    public void ttlNan() throws Exception {

        Map<String, String> payload =
                hashMap( "grant_type", "password" ).map( "username", "test@usergrid.com" ).map( "password", "test" )
                                                   .map( "ttl", "derp" );

        ClientResponse.Status responseStatus = null;
        try {
            resource().path( "/management/token" ).accept( MediaType.APPLICATION_JSON )
                      .type( MediaType.APPLICATION_JSON_TYPE ).post( String.class, payload );
        }
        catch ( UniformInterfaceException uie ) {
            responseStatus = uie.getResponse().getClientResponseStatus();
        }

        assertEquals( ClientResponse.Status.BAD_REQUEST, responseStatus );
    }


    @Test
    public void ttlOverMax() throws Exception {

        Map<String, String> payload =
                hashMap( "grant_type", "password" ).map( "username", "test@usergrid.com" ).map( "password", "test" )
                                                   .map( "ttl", Long.MAX_VALUE + "" );

        ClientResponse.Status responseStatus = null;

        try {
            resource().path( "/management/token" ).accept( MediaType.APPLICATION_JSON )
                      .type( MediaType.APPLICATION_JSON_TYPE ).post( String.class, payload );
        }
        catch ( UniformInterfaceException uie ) {
            responseStatus = uie.getResponse().getClientResponseStatus();
        }

        assertEquals( ClientResponse.Status.BAD_REQUEST, responseStatus );
    }


    @Test
    public void revokeToken() throws Exception {
        String token1 = super.adminToken();
        String token2 = super.adminToken();

        JsonNode response = mapper.readTree( resource().path( "/management/users/test" ).queryParam( "access_token", token1 )
                                                       .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE )
                                                       .get( String.class ));

        assertEquals( "test@usergrid.com", response.get( "data" ).get( "email" ).asText() );

        response = mapper.readTree( resource().path( "/management/users/test" ).queryParam( "access_token", token2 )
                                              .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE )
                                              .get( String.class ));

        assertEquals( "test@usergrid.com", response.get( "data" ).get( "email" ).asText() );

        // now revoke the tokens
        response = mapper.readTree( resource().path( "/management/users/test/revoketokens" ).queryParam( "access_token", superAdminToken() )
                                              .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE )
                                              .post( String.class ));

        refreshIndex("test-organization", "test-app");

        // the tokens shouldn't work

        ClientResponse.Status status = null;

        try {
            response = mapper.readTree( resource().path( "/management/users/test" ).queryParam( "access_token", token1 )
                                                  .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE )
                                                  .get( String.class ));
        }
        catch ( UniformInterfaceException uie ) {
            status = uie.getResponse().getClientResponseStatus();
        }

        assertEquals( ClientResponse.Status.UNAUTHORIZED, status );

        status = null;

        try {
            response = mapper.readTree( resource().path( "/management/users/test" ).queryParam( "access_token", token2 )
                                                  .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE )
                                                  .get( String.class ));
        }
        catch ( UniformInterfaceException uie ) {
            status = uie.getResponse().getClientResponseStatus();
        }

        assertEquals( ClientResponse.Status.UNAUTHORIZED, status );

        String token3 = super.adminToken();
        String token4 = super.adminToken();

        response = mapper.readTree( resource().path( "/management/users/test" ).queryParam( "access_token", token3 )
                                              .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE )
                                              .get( String.class ));

        assertEquals( "test@usergrid.com", response.get( "data" ).get( "email" ).asText() );

        response = mapper.readTree( resource().path( "/management/users/test" ).queryParam( "access_token", token4 )
                                              .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE )
                                              .get( String.class ));

        assertEquals( "test@usergrid.com", response.get( "data" ).get( "email" ).asText() );

        // now revoke the token3
        response = mapper.readTree( resource().path( "/management/users/test/revoketoken" ).queryParam( "access_token", token3 )
                                              .queryParam( "token", token3 ).accept( MediaType.APPLICATION_JSON )
                                              .type( MediaType.APPLICATION_JSON_TYPE ).post( String.class ));

        // the token3 shouldn't work

        status = null;

        try {
            response = mapper.readTree( resource().path( "/management/users/test" ).queryParam( "access_token", token3 )
                                                  .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE )
                                                  .get( String.class ));
        }
        catch ( UniformInterfaceException uie ) {
            status = uie.getResponse().getClientResponseStatus();
        }

        assertEquals( ClientResponse.Status.UNAUTHORIZED, status );

        status = null;

        try {
            response = mapper.readTree( resource().path( "/management/users/test" ).queryParam( "access_token", token4 )
                                                  .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE )
                                                  .get( String.class ));

            status = ClientResponse.Status.OK;
        }
        catch ( UniformInterfaceException uie ) {
            status = uie.getResponse().getClientResponseStatus();
        }

        assertEquals( ClientResponse.Status.OK, status );
    }

    //Done for AdminEmailEncoding Tests
    @Rule
    public TestContextSetup context = new TestContextSetup( this );


    @Test
    public void getTokenPlus() throws Exception {
        String org = "AdminEmailEncodingTestgetTokenPlus";
        String app = "Plus";

        doTest( "+", org, app );
    }


    @Test
    public void getTokenUnderscore() throws Exception {
        String org = "AdminEmailEncodingTestgetTokenUnderscore";
        String app = "Underscore";

        doTest( "_", org, app );
    }


    @Test
    public void getTokenDash() throws Exception {
        String org = "AdminEmailEncodingTestgetTokenDash";
        String app = "Dash";

        doTest( "-", org, app );
    }


    private void doTest( String symbol, String org, String app ) throws IOException {

        org = org.toLowerCase();
        app = app.toLowerCase();

        String email = String.format( "admin%sname@adminemailencodingtest.org", symbol );
        String user = email;
        String password = "password";

        TestAdminUser adminUser = new TestAdminUser( user, password, email );

        context.withApp( app ).withOrg( org ).withUser( adminUser );

        // create the org and app
        context.createNewOrgAndUser();

        // no need for refresh here as Service module does an index refresh when org/app created

        // now log in via a GET

        String getToken = context.management().tokenGet( email, password );

        assertNotNull( getToken );

        String postToken = context.management().tokenPost( email, password );

        assertNotNull( postToken );

        // not log in with our admin
        context.withUser( adminUser ).loginUser();

        //now get the "me" and ensure it's correct

        JsonNode data = context.management().me().get();

        assertNotNull( data.get( "access_token" ).asText() );

        data = context.management().users().user( email ).get();

        JsonNode admin = data.get( "data" ).get( "organizations" ).get( org ).get( "users" ).get( email );

        assertNotNull( admin );

        assertEquals( email, admin.get( "email" ).asText() );
        assertEquals( user, admin.get( "username" ).asText() );
    }


}
