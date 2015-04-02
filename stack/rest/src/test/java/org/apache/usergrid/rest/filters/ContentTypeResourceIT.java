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


import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.representation.Form;
import org.apache.usergrid.rest.test.resource2point0.AbstractRestIT;
import org.apache.usergrid.utils.JsonUtils;
import org.apache.usergrid.utils.UUIDUtils;
import org.junit.Ignore;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.util.List;

import static org.apache.usergrid.utils.MapUtils.hashMap;
import static org.junit.Assert.assertEquals;


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

        String json = JsonUtils.mapToFormattedJsonString(hashMap("name", "Solitaire1"));

        WebResource.Builder builder = app().collection("games").getResource(true)
            .queryParam("access_token", this.getAdminToken().getAccessToken())
            .type(MediaType.APPLICATION_JSON_TYPE)
            .accept(MediaType.APPLICATION_JSON);

        ClientResponse clientResponse = builder.post(ClientResponse.class, json);

        assertEquals(200, clientResponse.getStatus());

        MultivaluedMap<String, String> headers = clientResponse.getHeaders();

        List<String> contentType = headers.get("Content-Type");
        assertEquals(1, contentType.size());
        assertEquals(MediaType.APPLICATION_JSON, contentType.get(0));
    }


    /**
     * Creates a simple entity of type game. Does not set the content type. The type should be set to json to match the
     * body
     */
    @Test
    public void textPlainContentType() throws Exception {
        String json = JsonUtils.mapToFormattedJsonString(hashMap("name", "Solitaire2"));
        WebResource.Builder builder = app().getResource(true)
            .queryParam("access_token", this.getAdminToken().getAccessToken())
            .accept(MediaType.APPLICATION_JSON)
            .type(MediaType.TEXT_PLAIN_TYPE);

        ClientResponse clientResponse = builder.post(ClientResponse.class, json);

        assertEquals(200, clientResponse.getStatus());

        MultivaluedMap<String, String> headers = clientResponse.getHeaders();

        List<String> contentType = headers.get("Content-Type");
        assertEquals(1, contentType.size());
        assertEquals(MediaType.APPLICATION_JSON, contentType.get(0));

    }


    /**
     * Tests that application/x-www-url-form-encoded works correctly
     */
    @Test
    public void formEncodedContentType() throws Exception {


        Form payload = new Form();
        payload.add("organization", "formContentOrg");
        payload.add("username", "formContentOrg");
        payload.add("name", "Test User");
        payload.add("email", UUIDUtils.newTimeUUID() + "@usergrid.org");
        payload.add("password", "foobar");

        WebResource.Builder builder = app().getResource(true, this.getAdminToken(clientSetup.getSuperuserName(), clientSetup.getSuperuserPassword()))
            .accept(MediaType.APPLICATION_JSON)
            .type(MediaType.APPLICATION_FORM_URLENCODED_TYPE);

        ClientResponse clientResponse = builder.post(ClientResponse.class, payload);

        assertEquals(200, clientResponse.getStatus());

        MultivaluedMap<String, String> headers = clientResponse.getHeaders();

        List<String> contentType = headers.get("Content-Type");
        assertEquals(1, contentType.size());
        assertEquals(MediaType.APPLICATION_JSON, contentType.get(0));
    }


    /**
     * Tests that application/x-www-url-form-encoded works correctly
     */
    @Test
    @Ignore("This will only pass in tomcat, and shouldn't pass in grizzly")
    public void formEncodedUrlContentType() throws Exception {
        String orgName = clientSetup.getOrganizationName().toLowerCase();
        String appName = clientSetup.getAppName().toLowerCase();

        WebResource.Builder builder = resource().path(String.format("/%s/%s", orgName, appName))
            .queryParam("organization", "formUrlContentOrg")
            .queryParam("username", "formUrlContentOrg")
            .queryParam("name", "Test User")
            .queryParam("email", UUIDUtils.newTimeUUID() + "@usergrid.org")
            .queryParam("password", "foobar")
            .queryParam("grant_type", "password")
            .type(MediaType.APPLICATION_FORM_URLENCODED_TYPE);

        ClientResponse clientResponse = builder.post(ClientResponse.class);

        assertEquals(200, clientResponse.getStatus());

        MultivaluedMap<String, String> headers = clientResponse.getHeaders();

        List<String> contentType = headers.get("Content-Type");
        assertEquals(1, contentType.size());
        assertEquals(MediaType.APPLICATION_JSON, contentType.get(0));

    }


    /**
     * Creates a simple entity of type game. Does not set the content type or accept. The type should be set to json to
     * match the body
     */
    @Test
    public void missingAcceptAndContent() throws Exception {

        WebResource.Builder builder = app().collection("games").getResource(true)
            .queryParam("access_token", this.getAdminToken().getAccessToken())
            .type(MediaType.APPLICATION_JSON_TYPE);

        ClientResponse clientResponse = builder.post(ClientResponse.class, JsonUtils.mapToJsonString(hashMap("name", "bar")));

        assertEquals(200, clientResponse.getStatus());

        MultivaluedMap<String, String> headers = clientResponse.getHeaders();

        List<String> contentType = headers.get("Content-Type");
        assertEquals(1, contentType.size());
        assertEquals(MediaType.APPLICATION_JSON, contentType.get(0));
    }


    /**
     * Creates a simple entity of type game. Does not set the Accepts header. The type should be set to json to match the
     * body.  Then does a get without Accept type, it should return application/json, not text/csv
     */
    @Test
    public void noAcceptGet() throws Exception {

        WebResource.Builder builder = app().collection("games").getResource(true)
            .queryParam("access_token", this.getAdminToken().getAccessToken())
            .type(MediaType.APPLICATION_JSON_TYPE);

        ClientResponse clientResponse = builder.post(ClientResponse.class, JsonUtils.mapToJsonString(hashMap("name", "bar")));

        assertEquals(200, clientResponse.getStatus());

        MultivaluedMap<String, String> headers = clientResponse.getHeaders();

        List<String> contentType = headers.get("Content-Type");
        assertEquals(1, contentType.size());
        assertEquals(MediaType.APPLICATION_JSON, contentType.get(0));

        //do the get with no content type, it should get set to application/json
        clientResponse = builder.get(ClientResponse.class);

        assertEquals(200, clientResponse.getStatus());

        headers = clientResponse.getHeaders();

        contentType = headers.get("Content-Type");
        assertEquals(1, contentType.size());
        assertEquals(MediaType.APPLICATION_JSON, contentType.get(0));
    }


}
