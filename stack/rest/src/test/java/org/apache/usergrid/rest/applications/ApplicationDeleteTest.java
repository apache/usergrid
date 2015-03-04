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
import com.sun.jersey.api.client.UniformInterfaceException;
import org.apache.usergrid.rest.test.resource2point0.AbstractRestIT;
import org.apache.usergrid.rest.test.resource2point0.endpoints.mgmt.ManagementResponse;
import org.apache.usergrid.rest.test.resource2point0.model.*;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.ws.rs.core.MediaType;
import java.io.StringReader;
import java.util.*;


public class ApplicationDeleteTest  extends AbstractRestIT {
    private static final Logger logger = LoggerFactory.getLogger(ApplicationDeleteTest.class);


    @Test
    public void testBasicOperation() throws Exception {

        // create app with a collection of "things"

        String orgName = clientSetup.getOrganization().getName();
        String appToDeleteName = clientSetup.getAppName() + "_appToDelete";
        Token orgAdminToken = getAdminToken( clientSetup.getUsername(), clientSetup.getUsername());

        ApiResponse appCreateResponse = clientSetup.getRestClient()
            .management().orgs().organization( orgName ).app().getResource()
            .queryParam( "access_token", orgAdminToken.getAccessToken() )
            .type( MediaType.APPLICATION_JSON )
            .post( ApiResponse.class, new Application( appToDeleteName ) );
        UUID appToDeleteId = appCreateResponse.getEntities().get(0).getUuid();

        List<Entity> entities = new ArrayList<>();
        for ( int i=0; i<10; i++ ) {

            final String entityName = "entity" + i;
            Entity entity = new Entity();
            entity.setProperties(new HashMap<String, Object>() {{
                put("name", entityName );
            }});

            ApiResponse createResponse = clientSetup.getRestClient()
                .org(orgName).app( appToDeleteName ).collection("things").getResource()
                .queryParam("access_token", orgAdminToken.getAccessToken())
                .type(MediaType.APPLICATION_JSON)
                .post( ApiResponse.class, entity );

            entities.add( createResponse.getEntities().get(0) );
        }

        // delete the app

        clientSetup.getRestClient()
            .org(orgName).app(appToDeleteId.toString() ).getResource()
            .queryParam("access_token", orgAdminToken.getAccessToken() )
            .delete();

        // test that we can no longer get the app

        try {
            clientSetup.getRestClient()
                .org(orgName).app(appToDeleteName).getResource()
                .queryParam("access_token", orgAdminToken.getAccessToken())
                .type(MediaType.APPLICATION_JSON)
                .get(ApiResponse.class);

            Assert.fail("Must not be able to get deleted app");

        } catch ( UniformInterfaceException expected ) {
            Assert.assertEquals("Error must be 400", 400, expected.getResponse().getStatus() );
            JsonNode node = mapper.readTree( expected.getResponse().getEntity( String.class ));
            Assert.assertEquals("organization_application_not_found", node.get("error").textValue());
        }

        // test that we can no longer get deleted app's collection

        try {
            clientSetup.getRestClient()
                .org(orgName).app(appToDeleteName).collection("things").getResource()
                .queryParam("access_token", orgAdminToken.getAccessToken() )
                .type(MediaType.APPLICATION_JSON )
                .get(ApiResponse.class);

            Assert.fail("Must not be able to get deleted app's collection");

        } catch ( UniformInterfaceException expected ) {
            Assert.assertEquals("Error must be 400", 400, expected.getResponse().getStatus() );
            JsonNode node = mapper.readTree( expected.getResponse().getEntity( String.class ));
            Assert.assertEquals("organization_application_not_found", node.get("error").textValue());
        }

        // test that we can no longer get an app entity

        try {
            UUID entityId = entities.get(0).getUuid();
            clientSetup.getRestClient()
                .org(orgName).app(appToDeleteName).collection("things").entity( entityId ).getResource()
                .queryParam( "access_token", orgAdminToken.getAccessToken())
                .type( MediaType.APPLICATION_JSON)
                .get(ApiResponse.class);

            Assert.fail("Must not be able to get deleted app entity");

        } catch ( UniformInterfaceException expected ) {
            // TODO: why not a 404?
            Assert.assertEquals("Error must be 400", 400, expected.getResponse().getStatus() );
            JsonNode node = mapper.readTree( expected.getResponse().getEntity( String.class ));
            Assert.assertEquals("organization_application_not_found", node.get("error").textValue());
        }

        // test that we cannot see the application in the list of applications

        refreshIndex();

        ManagementResponse orgAppResponse = clientSetup.getRestClient()
            .management().orgs().organization( orgName ).apps().getOrganizationApplications();

        for ( String appName : orgAppResponse.getData().keySet() ) {
            if ( orgAppResponse.getData().get( appName ).equals( appToDeleteId.toString() )) {
                Assert.fail("Deleted app must not be included in list of org apps");
            }
        }

        // test that we cannot delete the application a second time

        try {
            clientSetup.getRestClient()
                .org(orgName).app(appToDeleteId.toString()).getResource()
                .queryParam("access_token", orgAdminToken.getAccessToken())
                .delete();

        } catch ( UniformInterfaceException expected ) {
            Assert.assertEquals("Error must be 404", 404, expected.getResponse().getStatus() );
            JsonNode node = mapper.readTree( expected.getResponse().getEntity( String.class ));
            Assert.assertEquals("not_found", node.get("error").textValue());
        }

        // test that we can create a new application with the same name

        ApiResponse appCreateAgainResponse = clientSetup.getRestClient()
            .management().orgs().organization( orgName ).app().getResource()
            .queryParam( "access_token", orgAdminToken.getAccessToken() )
            .type( MediaType.APPLICATION_JSON )
            .post( ApiResponse.class, new Application( appToDeleteName ) );

        Assert.assertEquals("Must be able to create app with same name as deleted app",
            (orgName + "/" + appToDeleteName).toLowerCase(),
            appCreateAgainResponse.getEntities().get(0).get("name"));
    }


    @Test
    public void testAppRestore() throws Exception {

        // create and delete app

        // restore the app

        // test that app appears in list of apps

        // test that application's collection exists
    }



    @Test
    public void testAppRestoreConflict() throws Exception {

        // create and delete app

        // create new app with same name

        // attempt to restore original app

        // test that HTTP 409 CONFLICT and informative error message is received

        // create a collection with two entities

    }
}

