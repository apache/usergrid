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
import org.apache.usergrid.rest.test.resource.AbstractRestIT;
import org.apache.usergrid.rest.test.resource.endpoints.mgmt.ManagementResponse;
import org.apache.usergrid.rest.test.resource.model.ApiResponse;
import org.apache.usergrid.rest.test.resource.model.Application;
import org.apache.usergrid.rest.test.resource.model.Entity;
import org.apache.usergrid.rest.test.resource.model.Token;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.fail;

import static org.apache.usergrid.rest.management.organizations.applications
    .ApplicationResource.CONFIRM_APPLICATION_IDENTIFIER;

public class ApplicationDeleteIT extends AbstractRestIT {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationDeleteIT.class);

    public static final int INDEXING_WAIT = 3000;


    @Test
    public void testBasicOperation() throws Exception {

        // create app with a collection of "things"

        String orgName = clientSetup.getOrganization().getName();
        String appToDeleteName = clientSetup.getAppName() + "_appToDelete";
        Token orgAdminToken = getAdminToken( clientSetup.getUsername(), clientSetup.getUsername());

        List<Entity> entities = new ArrayList<>();

        UUID appToDeleteId = createAppWithCollection(orgName, appToDeleteName, orgAdminToken, entities);

        // delete the app

        clientSetup.getRestClient().management().orgs()
            .org(orgName).apps().app(appToDeleteId.toString() ).getTarget()
            .queryParam("access_token", orgAdminToken.getAccessToken() )
            .queryParam(CONFIRM_APPLICATION_IDENTIFIER, appToDeleteId)
            .request().delete();

        // test that we can create a new application with the same name

        ApiResponse appCreateAgainResponse = null;

        int retries = 0;
        while ( retries++ < 30 ) {
            try {
                appCreateAgainResponse = clientSetup.getRestClient()
                    .management().orgs().org( orgName ).app().getTarget()
                    .queryParam( "access_token", orgAdminToken.getAccessToken() ).request()
                    .post( javax.ws.rs.client.Entity.json( new Application( appToDeleteName ) ), ApiResponse.class );

                break;

            } catch (Exception e) {
                logger.error("App not deleted yet. Waiting ... ({})", retries);
                Thread.sleep( 1000 );
            }
        }

        Assert.assertNotNull( appCreateAgainResponse );

        Assert.assertEquals("Must be able to create app with same name as deleted app",
            (orgName + "/" + appToDeleteName).toLowerCase(),
            appCreateAgainResponse.getEntities().get(0).get( "name" ));
    }


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
    public void testCommonUseCases() throws Exception {

        // create app with a collection of "things"

        String orgName = clientSetup.getOrganization().getName();
        String appToDeleteName = clientSetup.getAppName() + "_appToDelete";
        Token orgAdminToken = getAdminToken( clientSetup.getUsername(), clientSetup.getUsername());

        List<Entity> entities = new ArrayList<>();

        UUID appToDeleteId = createAppWithCollection(orgName, appToDeleteName, orgAdminToken, entities);

        // delete the app without specifying confirm_application_identifier

        final Response response = clientSetup.getRestClient().management().orgs()
            .org( orgName ).apps().app( appToDeleteId.toString() ).getTarget()
            .queryParam( "access_token", orgAdminToken.getAccessToken() )
            .request()
            .delete();

        Assert.assertEquals("Error must be 400", 400, response.getStatus() );

        clientSetup.getRestClient().management().orgs()
            .org(orgName).apps().app(appToDeleteId.toString() ).getTarget()
            .queryParam("access_token", orgAdminToken.getAccessToken() )
            .queryParam(CONFIRM_APPLICATION_IDENTIFIER, appToDeleteId)
            .request()
            .delete();


        // test that we can no longer get the app

        try { // using /management/orgs/{org-name}/app/{app-name}

            clientSetup.getRestClient().management().orgs()
                .org(orgName).apps().app(appToDeleteName).getTarget()
                .queryParam("access_token", orgAdminToken.getAccessToken())
                .request()
                .get(ApiResponse.class);

            fail("Must not be able to get deleted app");

        } catch ( ClientErrorException expected ) {
            Assert.assertEquals("Error must be 404", 404, expected.getResponse().getStatus() );
            JsonNode node = mapper.readTree( expected.getResponse().readEntity( String.class ));
            Assert.assertEquals("entity_not_found", node.get("error").textValue());
        }


        try { // using /{org-name}/{app-name} path

            clientSetup.getRestClient().org( orgName ).app( appToDeleteName ).getTarget()
                       .queryParam( "access_token", orgAdminToken.getAccessToken() ).request()
                       .get( ApiResponse.class );

            fail( "Must not be able to get deleted app" );
        }
        catch ( ClientErrorException expected ) {
            Assert.assertEquals( "Error must be 404", 404, expected.getResponse().getStatus() );
            JsonNode node = mapper.readTree( expected.getResponse().readEntity( String.class ) );
            Assert.assertEquals( "organization_application_not_found", node.get( "error" ).textValue() );
        }


        // test that we can no longer get deleted app's collection

        try {
            clientSetup.getRestClient()
                .org(orgName).app(appToDeleteName).collection("things").getTarget()
                .queryParam("access_token", orgAdminToken.getAccessToken() )
                .request()
                .get( ApiResponse.class );

            fail("Must not be able to get deleted app's collection");

        } catch ( ClientErrorException expected ) {
            Assert.assertEquals("Error must be 400", 404, expected.getResponse().getStatus() );
            JsonNode node = mapper.readTree( expected.getResponse().readEntity( String.class ));
            Assert.assertEquals("organization_application_not_found", node.get("error").textValue());
        }

        // test that we can no longer get an app entity

        try {
            UUID entityId = entities.get(0).getUuid();
            clientSetup.getRestClient()
                .org(orgName).app(appToDeleteName).collection("things").entity( entityId ).getTarget()
                .queryParam( "access_token", orgAdminToken.getAccessToken())
                .request()
                .get( ApiResponse.class );

            fail("Must not be able to get deleted app entity");

        } catch ( ClientErrorException expected ) {
            // TODO: why not a 404?
            Assert.assertEquals("Error must be 400", 404, expected.getResponse().getStatus() );
            JsonNode node = mapper.readTree( expected.getResponse().readEntity( String.class ));
            Assert.assertEquals("organization_application_not_found", node.get("error").textValue());
        }

        // test that we cannot see the application in the list of applications returned
        // by the management resource's get organization's applications end-point

        waitForQueueDrainAndRefreshIndex();

        ManagementResponse orgAppResponse = clientSetup.getRestClient()
            .management().orgs().org( orgName ).apps().getOrganizationApplications();

        for ( String appName : orgAppResponse.getData().keySet() ) {
            if ( orgAppResponse.getData().get( appName ).equals( appToDeleteId.toString() )) {
                fail("Deleted app must not be included in list of org apps");
            }
        }

        // test that we cannot delete the application a second time

        final Response response1 = clientSetup.getRestClient().management()
            .orgs().org( orgName ).apps().app( appToDeleteId.toString() )
            .getTarget().queryParam( "access_token", orgAdminToken.getAccessToken() )
            .queryParam( CONFIRM_APPLICATION_IDENTIFIER, appToDeleteId )
            .request()
            .delete();
        Assert.assertEquals( "Error must be 404", 404, response1.getStatus() );

        // test that we can create a new application with the same name

        ApiResponse appCreateAgainResponse = null;

        int retries = 0;
        while ( retries++ < 20 ) {
            try {
                appCreateAgainResponse = clientSetup.getRestClient()
                    .management().orgs().org( orgName ).app().getTarget()
                    .queryParam( "access_token", orgAdminToken.getAccessToken() ).request()
                    .post( javax.ws.rs.client.Entity.json( new Application( appToDeleteName ) ), ApiResponse.class );

                break;

            } catch (Exception e) {
                logger.error("App not deleted yet. Waiting ... ({})", retries);
                Thread.sleep( 1000 );
            }
        }

        Assert.assertNotNull( appCreateAgainResponse );

        Assert.assertEquals("Must be able to create app with same name as deleted app",
            (orgName + "/" + appToDeleteName).toLowerCase(),
            appCreateAgainResponse.getEntities().get(0).get( "name" ));
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

        logger.debug( "\n\nDeleting app\n" );

        clientSetup.getRestClient().management().orgs()
            .org( orgName ).apps().app( appToDeleteName ).getTarget()
            .queryParam( "access_token", orgAdminToken.getAccessToken() )
            .queryParam(CONFIRM_APPLICATION_IDENTIFIER, appToDeleteName)
            .request()
            .delete();

        waitForQueueDrainAndRefreshIndex();


        // restore the app

        logger.debug( "\n\nRestoring app\n" );

        clientSetup.getRestClient().management().orgs()
            .org( orgName ).apps().app( appToDeleteId.toString() ).getTarget()
            .queryParam( "access_token", orgAdminToken.getAccessToken() )
            .request()
            .put( javax.ws.rs.client.Entity.entity( "", MediaType.APPLICATION_JSON )); // must send body

        waitForQueueDrainAndRefreshIndex();

        // test that we can see the application in the list of applications

        logger.debug("\n\nGetting app list from management end-point\n");

        ManagementResponse orgAppResponse = clientSetup.getRestClient()
            .management().orgs().org( orgName ).apps().getOrganizationApplications();

        boolean found = false;
        for ( String appName : orgAppResponse.getData().keySet() ) {
            if ( orgAppResponse.getData().get( appName ).equals( appToDeleteId.toString() )) {
                found = true;
                break;
            }
        }

        Assert.assertTrue( found );

        // test that we can get an app entity

        logger.debug( "\n\nGetting entities from app\n" );

        UUID entityId = entities.get( 0 ).getUuid();
        ApiResponse entityResponse = clientSetup.getRestClient()
            .org( orgName ).app( appToDeleteName ).collection( "things" ).entity( entityId ).getTarget()
            .queryParam( "access_token", orgAdminToken.getAccessToken() )
            .request()
            .get( ApiResponse.class );
        Assert.assertEquals( entityId, entityResponse.getEntities().get(0).getUuid() );

        // test that we can get deleted app's collection

        ApiResponse collectionReponse = clientSetup.getRestClient()
            .org( orgName ).app( appToDeleteName ).collection( "things" ).getTarget()
            .queryParam( "access_token", orgAdminToken.getAccessToken() )
            .request()
            .get( ApiResponse.class );
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

        clientSetup.getRestClient().management().orgs()
            .org( orgName ).apps().app( appToDeleteId.toString() ).getTarget()
            .queryParam( "access_token", orgAdminToken.getAccessToken() )
            .queryParam(CONFIRM_APPLICATION_IDENTIFIER, appToDeleteId)
            .request()
            .delete();

        // create new app with same name

        UUID newAppId = null;
        int retries = 0;
        while ( retries++ < 20 ) {
            try {
                newAppId = createAppWithCollection( orgName, appToDeleteName, orgAdminToken, entities );
                break;

            } catch ( Exception e ) {
                logger.error("Application not deleted yet. Waiting... ({})", retries);
                Thread.sleep(1000);
            }
        }

        Assert.assertNotNull( newAppId );

        // attempt to restore original app, should get 409

        final Response response = clientSetup.getRestClient().management().orgs()
            .org( orgName ).apps().app( appToDeleteId.toString() ).getTarget()
            .queryParam( "access_token", orgAdminToken.getAccessToken() )
            .request()
            .put( javax.ws.rs.client.Entity.entity( "", MediaType.TEXT_PLAIN ) );// must send body with put

        Assert.assertEquals( 409, response.getStatus() );
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

        clientSetup.getRestClient().management()
            .orgs().org( orgName ).apps().app( appToDeleteId.toString() ).getTarget()
            .queryParam( "access_token", orgAdminToken.getAccessToken() )
            .queryParam( CONFIRM_APPLICATION_IDENTIFIER, appToDeleteId )
            .request().delete();

        // create new app with same name

        UUID newAppId = null;
        int retries = 0;
        while ( retries++ < 20 ) {
            try {
                newAppId = createAppWithCollection( orgName, appToDeleteName, orgAdminToken, entities );
                break;

            } catch ( Exception e ) {
                logger.error("Application not deleted yet. Waiting... ({})", retries);
                Thread.sleep(1000);
            }
        }

        Assert.assertNotNull( newAppId );

        // attempt to delete new app, it should fail

        final Response response = clientSetup.getRestClient().management()
            .orgs().org( orgName ).apps().app( newAppId.toString() ).getTarget()
            .queryParam( "access_token", orgAdminToken.getAccessToken() )
            .queryParam( CONFIRM_APPLICATION_IDENTIFIER, newAppId )
            .request()
            .delete();

        Assert.assertEquals( 409, response.getStatus() );
    }


    private UUID createAppWithCollection(
        String orgName, String appName, Token orgAdminToken, List<Entity> entities) {

        ApiResponse appCreateResponse = clientSetup.getRestClient()
            .management().orgs().org( orgName ).app().getTarget()
            .queryParam( "access_token", orgAdminToken.getAccessToken() )
            .request()
            .post( javax.ws.rs.client.Entity.json(new Application( appName )), ApiResponse.class );
        UUID appId = appCreateResponse.getEntities().get(0).getUuid();

        try { Thread.sleep(INDEXING_WAIT); } catch (InterruptedException ignored ) { }

        for ( int i=0; i<10; i++ ) {

            final String entityName = "entity" + i;
            Entity entity = new Entity();
            entity.setProperties( new HashMap<String, Object>() {{
                put( "name", entityName );
            }} );

            ApiResponse createResponse = clientSetup.getRestClient()
                .org(orgName).app( appName ).collection("things").getTarget()
                .queryParam("access_token", orgAdminToken.getAccessToken())
                .request()
                .post( javax.ws.rs.client.Entity.json(entity), ApiResponse.class );

            entities.add( createResponse.getEntities().get(0) );
        }
        return appId;
    }
}

