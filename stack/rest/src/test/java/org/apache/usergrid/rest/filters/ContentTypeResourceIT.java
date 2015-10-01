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
package org.apache.usergrid.rest.filters;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.usergrid.rest.test.resource.AbstractRestIT;
import org.apache.usergrid.rest.test.resource.model.Organization;
import org.apache.usergrid.rest.test.resource.model.Token;
import org.apache.usergrid.rest.test.resource.model.User;
import org.apache.usergrid.utils.JsonUtils;
import org.apache.usergrid.utils.UUIDUtils;
import org.glassfish.jersey.client.ClientResponse;
import org.junit.Test;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.*;
import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.usergrid.utils.MapUtils.hashMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


/**
 * @author tnine
 */

// @Ignore("Client login is causing tests to fail due to socket closure by grizzly.  Need to re-enable once we're not
// using grizzly to test")
public class ContentTypeResourceIT extends AbstractRestIT {


    /**
     * Creates a simple entity of type game. Does not set the content type. The type should be set to json to match the
     * body
     */
    @Test
    public void correctHeaders() throws Exception {

        User user = new User("shawn","shawn","shawn@email.com","aliensquirrel");
        this.app().collection("users").post(user);
        Token token = this.app().token().post(new Token("shawn","aliensquirrel"));

        Map<String, String> data = hashMap( "name", "Solitaire1" );

        String json = JsonUtils.mapToFormattedJsonString(data);

        DefaultHttpClient client = new DefaultHttpClient();

        HttpHost host = new HttpHost( super.getBaseURI().getHost(), super.getBaseURI().getPort() );

        HttpPost post = new HttpPost( String.format("/%s/%s/games",
            this.clientSetup.getOrganization().getName(), this.clientSetup.getAppName()) );
        post.setEntity(new StringEntity(json));
        post.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token.getAccessToken());
        post.setHeader( HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON );
        post.setHeader(HttpHeaders.CONTENT_TYPE, "*/*");

        HttpResponse rsp = client.execute( host, post );

        printResponse( rsp );

        assertEquals( 200, rsp.getStatusLine().getStatusCode() );

        Header[] headers = rsp.getHeaders( HttpHeaders.CONTENT_TYPE );

        assertEquals( 1, headers.length );

        assertEquals( MediaType.APPLICATION_JSON, headers[0].getValue() );

    }


    /**
     * Creates a simple entity of type game. Does not set the content type. The type should be set to json to match the
     * body
     */
    @Test
    public void textPlainContentType() throws Exception {
        User user = new User("shawn","shawn","shawn@email.com","aliensquirrel");
        this.app().collection("users").post( user );
        Token token = this.app().token().post(new Token("shawn","aliensquirrel"));
        Map<String, String> data = hashMap( "name", "Solitaire2" );

        String json = JsonUtils.mapToFormattedJsonString( data );

        DefaultHttpClient client = new DefaultHttpClient();

        HttpHost host = new HttpHost( super.getBaseURI().getHost(), super.getBaseURI().getPort() );

        HttpPost post = new HttpPost( String.format("/%s/%s/games",
            this.clientSetup.getOrganization().getName(), this.clientSetup.getAppName()) );

        post.setEntity( new StringEntity( json ) );
        post.setHeader( HttpHeaders.AUTHORIZATION, "Bearer " + token.getAccessToken() );
        post.setHeader( HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON );
        post.setHeader( HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN );

        HttpResponse rsp = client.execute( host, post );

        printResponse( rsp );

        assertEquals( 200, rsp.getStatusLine().getStatusCode() );

    }


    /**
     * Tests that application/x-www-url-form-encoded works correctly
     */
    @Test
    public void formEncodedContentType() throws Exception {


        Form payload = new Form();
        payload.param( "organization", "formContentOrg" + UUIDUtils.newTimeUUID() );
        payload.param( "username", "formContentOrg" + UUIDUtils.newTimeUUID() );
        payload.param( "name", "Test User" + UUIDUtils.newTimeUUID() );
        payload.param( "email", UUIDUtils.newTimeUUID() + "@usergrid.org" );
        payload.param( "password", "foobar" );

        //checks that the organization was created using a form encoded content type, this is checked else where so
        //this test should be depreciated eventually.
        Organization newlyCreatedOrganizationForm = management().orgs().post( payload );

        assertNotNull( newlyCreatedOrganizationForm );

    }

    /**
     * Creates a simple entity of type game. Does not set the content type or accept. The type should be set to json to
     * match the body
     */
    @Test
    public void missingAcceptAndContent() throws Exception {
        User user = new User("shawn","shawn","shawn@email.com","aliensquirrel");
        this.app().collection("users").post(user);
        Token token = this.app().token().post(new Token("shawn","aliensquirrel"));
        Map<String, String> data = hashMap( "name", "Solitaire3" );

        String json = JsonUtils.mapToFormattedJsonString( data );

        DefaultHttpClient client = new DefaultHttpClient();

        HttpHost host = new HttpHost( super.getBaseURI().getHost(), super.getBaseURI().getPort() );

        HttpPost post = new HttpPost( String.format("/%s/%s/games",
            this.clientSetup.getOrganization().getName(), this.clientSetup.getAppName()) );

        post.setEntity( new StringEntity( json ) );
        post.setHeader( HttpHeaders.AUTHORIZATION, "Bearer " + token.getAccessToken() );

        HttpResponse rsp = client.execute( host, post );

        printResponse( rsp );

        assertEquals( 200, rsp.getStatusLine().getStatusCode() );

        Header[] headers = rsp.getHeaders( HttpHeaders.CONTENT_TYPE );

        assertEquals( 1, headers.length );

        assertEquals( MediaType.APPLICATION_JSON, headers[0].getValue() );
    }


    /**
     * Creates a simple entity of type game. Does not set the Accepts header. The type should be set to json
     * to match the body.  Then does a get without Accept type, it should return application/json, not text/csv
     */
    @Test
    public void noAcceptGet() throws Exception {
        User user = new User("shawn","shawn","shawn@email.com","aliensquirrel");
        this.app().collection("users").post( user );
        Token token = this.app().token().post(new Token("shawn", "aliensquirrel"));
        Map<String, String> data = hashMap("name", "bar");

        String json = JsonUtils.mapToFormattedJsonString( data );

        DefaultHttpClient client = new DefaultHttpClient();

        HttpHost host = new HttpHost( super.getBaseURI().getHost(), super.getBaseURI().getPort() );

        HttpPost post = new HttpPost( String.format("/%s/%s/games",
            this.clientSetup.getOrganization().getName(), this.clientSetup.getAppName()) );

        post.setEntity( new StringEntity( json ) );
        post.setHeader( HttpHeaders.AUTHORIZATION, "Bearer " + token.getAccessToken() );
        post.setHeader( HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON );
        post.setHeader( HttpHeaders.CONTENT_TYPE, "*/*" );

        HttpResponse rsp = client.execute( host, post );


        Invocation.Builder builder = app().collection( "games" ).getTarget()
            .queryParam( "access_token", this.getAdminToken().getAccessToken() )
            .request();

        Response clientResponse = builder.post(
            javax.ws.rs.client.Entity.json( new HashMap() {{ put( "name", "bar2" ); }} ), Response.class );

        assertEquals(200, clientResponse.getStatus());

        MultivaluedMap<String, Object> headers = clientResponse.getHeaders();

        List contentType = headers.get( "Content-Type" );
        assertEquals(1, contentType.size());
        assertEquals(MediaType.APPLICATION_JSON, contentType.get(0));

        //do the get with no content type, it should get set to application/json

        builder = app().collection( "games" ).getTarget()
            .queryParam( "access_token", this.getAdminToken().getAccessToken() )
            .request();

        HttpGet get = new HttpGet( String.format("/%s/%s/games",
            this.clientSetup.getOrganization().getName(), this.clientSetup.getAppName()) );

        get.setHeader( HttpHeaders.AUTHORIZATION, "Bearer " + token.getAccessToken() );
        clientResponse = builder.get( Response.class );

        assertEquals(200, clientResponse.getStatus());

        headers = clientResponse.getHeaders();

        contentType = headers.get("Content-Type");
        assertEquals(1, contentType.size());
        assertEquals(MediaType.APPLICATION_JSON, contentType.get(0));
    }


    private void printResponse( HttpResponse rsp ) throws ParseException, IOException {
        HttpEntity entity = rsp.getEntity();

        System.out.println( "----------------------------------------" );
        System.out.println( rsp.getStatusLine() );

        Header[] headers = rsp.getAllHeaders();
        for ( int i = 0; i < headers.length; i++ ) {
            System.out.println( headers[i] );
        }
        System.out.println( "----------------------------------------" );

        if ( entity != null ) {
            System.out.println( EntityUtils.toString( entity ) );
        }
    }
}
