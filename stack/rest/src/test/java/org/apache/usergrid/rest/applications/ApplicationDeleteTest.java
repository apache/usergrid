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
import org.apache.usergrid.corepersistence.ApplicationIdCacheImpl;
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.persistence.EntityManagerFactory;
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
import java.util.regex.Pattern;

import static org.junit.Assert.fail;


public class ApplicationDeleteTest  extends AbstractRestIT {
    private static final Logger logger = LoggerFactory.getLogger(ApplicationDeleteTest.class);


    /**
     * Test most common use cases.
     * <pre>
     *  - create app with collection of things
     *  - delete the app
     *  - test that attempts to get the app, its collections and entities throw 400 with message
     *  - test that we cannot delete the app a second time
     *  - test that we can create a new app with the same name as the deleted app
     * </pre>
     */
    @Test
    public void testBasicOperation() throws Exception {

        // create app with a collection of "things"

        String orgName = clientSetup.getOrganization().getName();
        String appToDeleteName = clientSetup.getAppName() + "_appToDelete";
        Token orgAdminToken = getAdminToken( clientSetup.getUsername(), clientSetup.getUsername());

        List<Entity> entities = new ArrayList<>();

        UUID appToDeleteId = createAppWithCollection(orgName, appToDeleteName, orgAdminToken, entities);

        // delete the app

        try {
            clientSetup.getRestClient()
                .org(orgName).app(appToDeleteId.toString()).getResource()
                .queryParam("access_token", orgAdminToken.getAccessToken())
                .delete();

            fail("Delete must fail without app_delete_confirm parameter");

        } catch ( Exception e ) {
            logger.error("Error", e);
        }

        clientSetup.getRestClient()
            .org(orgName).app(appToDeleteId.toString() ).getResource()
            .queryParam("access_token", orgAdminToken.getAccessToken() )
            .queryParam("app_delete_confirm", "confirm_delete_of_application_and_data")
            .delete();

        // test that we can no longer get the app

        try {
            clientSetup.getRestClient()
                .org(orgName).app(appToDeleteName).getResource()
                .queryParam("access_token", orgAdminToken.getAccessToken())
                .type(MediaType.APPLICATION_JSON)
                .get(ApiResponse.class);

            fail("Must not be able to get deleted app");

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

            fail("Must not be able to get deleted app's collection");

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

            fail("Must not be able to get deleted app entity");

        } catch ( UniformInterfaceException expected ) {
            // TODO: why not a 404?
            Assert.assertEquals("Error must be 400", 400, expected.getResponse().getStatus() );
            JsonNode node = mapper.readTree( expected.getResponse().getEntity( String.class ));
            Assert.assertEquals("organization_application_not_found", node.get("error").textValue());
        }

        // test that we cannot see the application in the list of applications returned
        // by the management resource's get organization's applications end-point

        refreshIndex();

        ManagementResponse orgAppResponse = clientSetup.getRestClient()
            .management().orgs().organization( orgName ).apps().getOrganizationApplications();

        for ( String appName : orgAppResponse.getData().keySet() ) {
            if ( orgAppResponse.getData().get( appName ).equals( appToDeleteId.toString() )) {
                fail("Deleted app must not be included in list of org apps");
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


   /**
    * Test restore of deleted app.
    * <pre>
    *  - create app with collection of things
    *  - delete the app
    *  - restore the app
    *  - test that we can get the app, its collections and an entity
    * </pre>
    */
    @Test
    public void testAppRestore() throws Exception {

        // create app with a collection of "things"

        String orgName = clientSetup.getOrganization().getName();
        String appToDeleteName = clientSetup.getAppName() + "_appToDelete";
        Token orgAdminToken = getAdminToken( clientSetup.getUsername(), clientSetup.getUsername());

        List<Entity> entities = new ArrayList<>();

        UUID appToDeleteId = createAppWithCollection(orgName, appToDeleteName, orgAdminToken, entities);

        // delete the app

        logger.debug("\n\nDeleting app\n");

        clientSetup.getRestClient()
            .org(orgName).app( appToDeleteName ).getResource()
            .queryParam("access_token", orgAdminToken.getAccessToken() )
            .queryParam("app_delete_confirm", "confirm_delete_of_application_and_data")
            .delete();

        Thread.sleep(1000);

        // restore the app

        logger.debug("\n\nRestoring app\n");

        clientSetup.getRestClient()
            .org(orgName).app( appToDeleteId.toString() ).getResource()
            .queryParam("access_token", orgAdminToken.getAccessToken() )
            .put();

        // test that we can see the application in the list of applications

        logger.debug("\n\nGetting app list from management end-point\n");

        ManagementResponse orgAppResponse = clientSetup.getRestClient()
            .management().orgs().organization( orgName ).apps().getOrganizationApplications();

        boolean found = false;
        for ( String appName : orgAppResponse.getData().keySet() ) {
            if ( orgAppResponse.getData().get( appName ).equals( appToDeleteId.toString() )) {
                found = true;
                break;
            }
        }

        Assert.assertTrue( found );

        // test that we can get an app entity

        logger.debug("\n\nGetting entities from app\n");

        UUID entityId = entities.get(0).getUuid();
        ApiResponse entityResponse = clientSetup.getRestClient()
            .org(orgName).app(appToDeleteName).collection("things").entity( entityId ).getResource()
            .queryParam("access_token", orgAdminToken.getAccessToken())
            .type(MediaType.APPLICATION_JSON)
            .get(ApiResponse.class);
        Assert.assertEquals( entityId, entityResponse.getEntities().get(0).getUuid() );

        // test that we can get deleted app's collection

        ApiResponse collectionReponse = clientSetup.getRestClient()
            .org(orgName).app(appToDeleteName).collection("things").getResource()
            .queryParam("access_token", orgAdminToken.getAccessToken())
            .type(MediaType.APPLICATION_JSON)
            .get(ApiResponse.class);
        Assert.assertEquals( entities.size(), collectionReponse.getEntityCount() );
    }


    /**
     * Test that we cannot restore deleted app with same name as
     */
    @Test
    public void testAppRestoreConflict() throws Exception {

        // create app with a collection of "things"

        String orgName = clientSetup.getOrganization().getName();
        String appToDeleteName = clientSetup.getAppName() + "_appToDelete";
        Token orgAdminToken = getAdminToken( clientSetup.getUsername(), clientSetup.getUsername());

        List<Entity> entities = new ArrayList<>();

        UUID appToDeleteId = createAppWithCollection(orgName, appToDeleteName, orgAdminToken, entities);

        // delete the app

        clientSetup.getRestClient()
            .org( orgName ).app(appToDeleteId.toString() ).getResource()
            .queryParam( "access_token", orgAdminToken.getAccessToken() )
            .queryParam("app_delete_confirm", "confirm_delete_of_application_and_data")
            .delete();

        // create new app with same name

        createAppWithCollection(orgName, appToDeleteName, orgAdminToken, entities);

        // attempt to restore original app, should get 409

        try {

            clientSetup.getRestClient()
                .org(orgName).app(appToDeleteId.toString()).getResource()
                .queryParam("access_token", orgAdminToken.getAccessToken())
                .put();

            fail("Must fail to restore app with same name as existing app");

        } catch ( UniformInterfaceException e ) {
            Assert.assertEquals(409, e.getResponse().getStatus());
        }
    }


    /**
     * Test that we cannot delete an app with same name as an app that is already deleted.
     * TODO: investigate way to support this, there should be no such restriction.
     */
    @Test
    public void testAppDeleteConflict() throws Exception {

        // create app with a collection of "things"

        String orgName = clientSetup.getOrganization().getName();
        String appToDeleteName = clientSetup.getAppName() + "_appToDelete";
        Token orgAdminToken = getAdminToken( clientSetup.getUsername(), clientSetup.getUsername());

        List<Entity> entities = new ArrayList<>();

        UUID appToDeleteId = createAppWithCollection(orgName, appToDeleteName, orgAdminToken, entities);

        // delete the app

        clientSetup.getRestClient()
            .org( orgName ).app(appToDeleteId.toString() ).getResource()
            .queryParam( "access_token", orgAdminToken.getAccessToken() )
            .queryParam("app_delete_confirm", "confirm_delete_of_application_and_data")
            .delete();

        // create new app with same name

        UUID newAppId = createAppWithCollection(orgName, appToDeleteName, orgAdminToken, entities);

        // attempt to delete new app, it should fail

        try {

            clientSetup.getRestClient()
                .org(orgName).app( newAppId.toString() ).getResource()
                .queryParam("access_token", orgAdminToken.getAccessToken())
                .queryParam("app_delete_confirm", "confirm_delete_of_application_and_data")
                .delete();

            fail("Must fail to delete app with same name as deleted app");

        } catch ( UniformInterfaceException e ) {
            Assert.assertEquals( 409, e.getResponse().getStatus() );
        }
    }


    private UUID createAppWithCollection(
        String orgName, String appName, Token orgAdminToken, List<Entity> entities) {

        ApiResponse appCreateResponse = clientSetup.getRestClient()
            .management().orgs().organization( orgName ).app().getResource()
            .queryParam( "access_token", orgAdminToken.getAccessToken() )
            .type( MediaType.APPLICATION_JSON )
            .post( ApiResponse.class, new Application( appName ) );
        UUID appId = appCreateResponse.getEntities().get(0).getUuid();

        for ( int i=0; i<10; i++ ) {

            final String entityName = "entity" + i;
            Entity entity = new Entity();
            entity.setProperties(new HashMap<String, Object>() {{
                put("name", entityName );
            }});

            ApiResponse createResponse = clientSetup.getRestClient()
                .org(orgName).app( appName ).collection("things").getResource()
                .queryParam("access_token", orgAdminToken.getAccessToken())
                .type(MediaType.APPLICATION_JSON)
                .post( ApiResponse.class, entity );

            entities.add( createResponse.getEntities().get(0) );
        }
        return appId;
    }
}

