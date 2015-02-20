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


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import org.apache.usergrid.rest.AbstractRestIT;
import org.apache.usergrid.rest.TestContextSetup;
import org.apache.usergrid.utils.JsonUtils;
import org.apache.usergrid.utils.UUIDUtils;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.util.EntityUtils;

import static org.junit.Assert.assertEquals;
import static org.apache.usergrid.utils.MapUtils.hashMap;


/** @author tnine */

// @Ignore("Client login is causing tests to fail due to socket closure by grizzly.  Need to re-enable once we're not
// using grizzly to test")
public class ContentTypeResourceIT extends AbstractRestIT {

    @Rule
    public TestContextSetup context = new TestContextSetup( this );


    /**
     * Creates a simple entity of type game. Does not set the content type. The type should be set to json to match the
     * body
     */
    @Test
    public void correctHeaders() throws Exception {


        Map<String, String> data = hashMap( "name", "Solitaire1" );

        String json = JsonUtils.mapToFormattedJsonString( data );

        DefaultHttpClient client = new DefaultHttpClient();

        HttpHost host = new HttpHost( super.getBaseURI().getHost(), super.getBaseURI().getPort() );

        HttpPost post = new HttpPost( String.format( "/%s/%s/games", context.getOrgUuid(), context.getAppUuid() ) );
        post.setEntity( new StringEntity( json ) );
        post.setHeader( HttpHeaders.AUTHORIZATION, "Bearer " + context.getActiveUser().getToken() );
        post.setHeader( HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON );
        post.setHeader( HttpHeaders.CONTENT_TYPE, "*/*" );

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
        Map<String, String> data = hashMap( "name", "Solitaire2" );

        String json = JsonUtils.mapToFormattedJsonString( data );

        DefaultHttpClient client = new DefaultHttpClient();

        HttpHost host = new HttpHost( super.getBaseURI().getHost(), super.getBaseURI().getPort() );

        HttpPost post = new HttpPost( String.format( "/%s/%s/games", context.getOrgUuid(), context.getAppUuid() ) );
        post.setEntity( new StringEntity( json ) );
        post.setHeader( HttpHeaders.AUTHORIZATION, "Bearer " + context.getActiveUser().getToken() );
        post.setHeader( HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON );
        post.setHeader( HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN );

        HttpResponse rsp = client.execute( host, post );

        printResponse( rsp );

        assertEquals( 200, rsp.getStatusLine().getStatusCode() );

        Header[] headers = rsp.getHeaders( HttpHeaders.CONTENT_TYPE );

        assertEquals( 1, headers.length );

        assertEquals( MediaType.APPLICATION_JSON, headers[0].getValue() );
    }


    /** Tests that application/x-www-url-form-encoded works correctly */
    @Test
    public void formEncodedContentType() throws Exception {

        List<NameValuePair> pairs = new ArrayList<NameValuePair>();

        pairs.add( new BasicNameValuePair( "organization", "formContentOrg" ) );
        pairs.add( new BasicNameValuePair( "username", "formContentOrg" ) );
        pairs.add( new BasicNameValuePair( "name", "Test User" ) );
        pairs.add( new BasicNameValuePair( "email", UUIDUtils.newTimeUUID() + "@usergrid.org" ) );
        pairs.add( new BasicNameValuePair( "password", "foobar" ) );

        UrlEncodedFormEntity entity = new UrlEncodedFormEntity( pairs, "UTF-8" );

        DefaultHttpClient client = new DefaultHttpClient();

        HttpHost host = new HttpHost( super.getBaseURI().getHost(), super.getBaseURI().getPort() );

        HttpPost post = new HttpPost( "/management/orgs" );
        post.setEntity( entity );
        // post.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + context.getActiveUser().getToken());

        post.setHeader( HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED );

        HttpResponse rsp = client.execute( host, post );

        printResponse( rsp );

        // should be an error, no content type was set
        assertEquals( 200, rsp.getStatusLine().getStatusCode() );

        Header[] headers = rsp.getHeaders( HttpHeaders.CONTENT_TYPE );

        assertEquals( 1, headers.length );

        assertEquals( MediaType.APPLICATION_JSON, headers[0].getValue() );
    }


    /** Tests that application/x-www-url-form-encoded works correctly */
    @Test
    @Ignore("This will only pass in tomcat, and shouldn't pass in grizzly")
    public void formEncodedUrlContentType() throws Exception {
        BasicHttpParams params = new BasicHttpParams();

        params.setParameter( "organization", "formUrlContentOrg" );
        params.setParameter( "username", "formUrlContentOrg" );
        params.setParameter( "name", "Test User" );
        params.setParameter( "email", UUIDUtils.newTimeUUID() + "@usergrid.org" );
        params.setParameter( "password", "foobar" );
        params.setParameter( "grant_type", "password" );

        DefaultHttpClient client = new DefaultHttpClient();

        HttpHost host = new HttpHost( super.getBaseURI().getHost(), super.getBaseURI().getPort() );

        HttpPost post = new HttpPost( "/management/orgs" );
        post.setParams( params );

        post.setHeader( HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED );

        HttpResponse rsp = client.execute( host, post );

        printResponse( rsp );

        assertEquals( 200, rsp.getStatusLine().getStatusCode() );

        Header[] headers = rsp.getHeaders( HttpHeaders.CONTENT_TYPE );

        assertEquals( 1, headers.length );

        assertEquals( MediaType.APPLICATION_JSON, headers[0].getValue() );
    }


    /**
     * Creates a simple entity of type game. Does not set the content type or accept. The type should be set to json to
     * match the body
     */
    @Test
    public void missingAcceptAndContent() throws Exception {

        Map<String, String> data = hashMap( "name", "Solitaire3" );

        String json = JsonUtils.mapToFormattedJsonString( data );

        DefaultHttpClient client = new DefaultHttpClient();

        HttpHost host = new HttpHost( super.getBaseURI().getHost(), super.getBaseURI().getPort() );

        HttpPost post = new HttpPost( String.format( "/%s/%s/games", context.getOrgUuid(), context.getAppUuid() ) );
        post.setEntity( new StringEntity( json ) );
        post.setHeader( HttpHeaders.AUTHORIZATION, "Bearer " + context.getActiveUser().getToken() );

        HttpResponse rsp = client.execute( host, post );

        printResponse( rsp );

        assertEquals( 200, rsp.getStatusLine().getStatusCode() );

        Header[] headers = rsp.getHeaders( HttpHeaders.CONTENT_TYPE );

        assertEquals( 1, headers.length );

        assertEquals( MediaType.APPLICATION_JSON, headers[0].getValue() );
    }


    /**
     * Creates a simple entity of type game. Does not set the content type. The type should be set to json to match the
     * body.  Then does a get without Accept type, it should return application/json, not text/csv
     */
    @Test
    public void noAcceptGet() throws Exception {
        Map<String, String> data = hashMap( "name", "bar" );

        String json = JsonUtils.mapToFormattedJsonString( data );

        DefaultHttpClient client = new DefaultHttpClient();

        HttpHost host = new HttpHost( super.getBaseURI().getHost(), super.getBaseURI().getPort() );

        HttpPost post = new HttpPost( String.format( "/%s/%s/games", context.getOrgUuid(), context.getAppUuid() ) );
        post.setEntity( new StringEntity( json ) );
        post.setHeader( HttpHeaders.AUTHORIZATION, "Bearer " + context.getActiveUser().getToken() );
        post.setHeader( HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON );
        post.setHeader( HttpHeaders.CONTENT_TYPE, "*/*" );

        HttpResponse rsp = client.execute( host, post );

        printResponse( rsp );

        assertEquals( 200, rsp.getStatusLine().getStatusCode() );

        Header[] headers = rsp.getHeaders( HttpHeaders.CONTENT_TYPE );

        assertEquals( 1, headers.length );

        assertEquals( MediaType.APPLICATION_JSON, headers[0].getValue() );

        //do the get with no content type, it should get set to application/json
        HttpGet get = new HttpGet( String.format( "/%s/%s/games", context.getOrgUuid(), context.getAppUuid() ) );
        get.setHeader( HttpHeaders.AUTHORIZATION, "Bearer " + context.getActiveUser().getToken() );

        rsp = client.execute( host, get );

        printResponse( rsp );

        assertEquals( 200, rsp.getStatusLine().getStatusCode() );

        headers = rsp.getHeaders( HttpHeaders.CONTENT_TYPE );

        assertEquals( 1, headers.length );

        assertEquals( MediaType.APPLICATION_JSON, headers[0].getValue() );
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
