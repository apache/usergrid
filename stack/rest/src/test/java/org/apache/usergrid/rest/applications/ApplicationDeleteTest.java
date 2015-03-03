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
import org.apache.usergrid.rest.test.resource2point0.model.*;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;


public class ApplicationDeleteTest  extends AbstractRestIT {
    private static final Logger logger = LoggerFactory.getLogger(ApplicationDeleteTest.class);


    @Test
    public void testBasicOperation() throws Exception {

        // create app with a collection of "things"

        String orgName = clientSetup.getOrganization().getName();
        String appToDelete = clientSetup.getAppName() + "_appToDelete";
        Token orgAdminToken = getAdminToken( clientSetup.getUsername(), clientSetup.getUsername());

        ApiResponse appCreateResponse = clientSetup.getRestClient()
            .management().orgs().organization( orgName ).app().getResource()
            .queryParam( "access_token", orgAdminToken.getAccessToken() )
            .type( MediaType.APPLICATION_JSON )
            .post( ApiResponse.class, new Application( appToDelete ) );
        UUID appToDeleteId = appCreateResponse.getEntities().get(0).getUuid();

        List<Entity> entities = new ArrayList<>();
        for ( int i=0; i<10; i++ ) {

            final String entityName = "entity" + i;
            Entity entity = new Entity();
            entity.setProperties(new HashMap<String, Object>() {{
                put("name", entityName );
            }});

            ApiResponse createResponse = clientSetup.getRestClient()
                .org(orgName).app( appToDelete ).collection("things").getResource()
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
                .org(orgName).app(appToDelete).getResource()
                .queryParam("access_token", orgAdminToken.getAccessToken())
                .type(MediaType.APPLICATION_JSON)
                .get(ApiResponse.class);

            Assert.fail("Must not be able to get deleted app");

        } catch ( UniformInterfaceException expected ) {
            Assert.assertEquals("Error must be 400", 400, expected.getResponse().getStatus() );
        }

        // test that we can no longer get deleted app's collection

        try {
            clientSetup.getRestClient()
                .org(orgName).app(appToDelete).collection("things").getResource()
                .queryParam("access_token", orgAdminToken.getAccessToken() )
                .type(MediaType.APPLICATION_JSON )
                .get(ApiResponse.class);

            Assert.fail("Must not be able to get deleted app's collection");

        } catch ( UniformInterfaceException expected ) {
            Assert.assertEquals("Error must be 400", 400, expected.getResponse().getStatus() );
        }

        // test that we can no longer get an app entity

        try {
            UUID entityId = entities.get(0).getUuid();
            clientSetup.getRestClient()
                .org(orgName).app(appToDelete).collection("things").entity( entityId ).getResource()
                .queryParam( "access_token", orgAdminToken.getAccessToken())
                .type( MediaType.APPLICATION_JSON)
                .get(ApiResponse.class);

            Assert.fail("Must not be able to get deleted app entity");

        } catch ( UniformInterfaceException expected ) {
            Assert.assertEquals("Error must be 400", 400, expected.getResponse().getStatus() );
        }

        // test that we cannot see the application in the list of applications

        ApiResponse getAppsResponse = clientSetup.getRestClient()
            .management().orgs().organization(orgName).apps().getResource()
            .queryParam( "access_token", orgAdminToken.getAccessToken())
            .type( MediaType.APPLICATION_JSON)
            .get(ApiResponse.class);

        for ( Entity appEntity : getAppsResponse.getEntities() ) {
            if ( appEntity.get("name").equals( appToDelete ) ) {
                Assert.fail("Application still exists in Organization's collection");
            }
        }

        // test that we cannot delete the application a second time

        clientSetup.getRestClient()
            .org(orgName).app(appToDeleteId.toString() ).getResource()
            .queryParam("access_token", orgAdminToken.getAccessToken() )
            .delete();

        // test that we can create a new application with the same name

        ApiResponse appCreateAgainResponse = clientSetup.getRestClient()
            .management().orgs().organization( orgName ).app().getResource()
            .queryParam( "access_token", orgAdminToken.getAccessToken() )
            .type( MediaType.APPLICATION_JSON )
            .post( ApiResponse.class, new Application( appToDelete ) );

        Assert.assertEquals("Must be able to create app with same name as deleted app",
            "CREATED", appCreateAgainResponse.getStatus().toUpperCase());
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
        String name1 = "thing1";
        String name2 = "thing2";
        String property = "one fish, two fish, red fish, blue fish";
        Entity payload = new Entity();
        payload.put("name", name1);
        payload.put("property", property);
        Entity entity1 = this.app().collection("things").post(payload);
        payload.put("name", name2);
        Entity entity2 = this.app().collection("things").post(payload);

        Assert.assertEquals(entity1.get("name"), name1);
        Assert.assertEquals(entity2.get("name"), name2);

        this.refreshIndex();

        // test that we can query those entities
        Collection collection = this.app().collection("things").get();
        Assert.assertEquals(2, collection.getNumOfEntities());

        // test that we can get the application entity
        ApiResponse appResponse = this.app().get();
        String retAppName = String.valueOf(appResponse.getProperties().get("applicationName")).toLowerCase();
        Assert.assertEquals(clientSetup.getAppName().toLowerCase(), retAppName);

        // delete the application
        try {
            this.app().delete();
        } catch ( UniformInterfaceException e ) {
            Assert.fail("Delete call threw exception status = " + e.getResponse().getStatus());
        }

        // try to get the application entity
        try {
            this.app().get();
            Assert.fail("should not be able to get app after it has been deleted");
        } catch (UniformInterfaceException e) {
            //verify the correct error was returned
            JsonNode node = mapper.readTree( e.getResponse().getEntity( String.class ));
            Assert.assertEquals("organization_application_not_found", node.get("error").textValue());
        }

        // test that we cannot delete the application a second time
        try {
            this.app().delete();
            Assert.fail("should not be able to delete app after it has been deleted");
        } catch (UniformInterfaceException e) {
            //verify the correct error was returned
            JsonNode node = mapper.readTree( e.getResponse().getEntity( String.class ));
            Assert.assertEquals("organization_application_not_found", node.get("error").textValue());
        }

        // test that we can no longer query for the entities in the collection
        try {
            this.app().collection("things").get();
            Assert.fail("should not be able to query for entities after app has been deleted");
        } catch (UniformInterfaceException e) {
            //verify the correct error was returned
            JsonNode node = mapper.readTree( e.getResponse().getEntity( String.class ));
            Assert.assertEquals("organization_application_not_found", node.get("error").textValue());
        }
    }
}
