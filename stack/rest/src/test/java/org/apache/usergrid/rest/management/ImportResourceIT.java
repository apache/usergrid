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

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;

import org.apache.usergrid.persistence.index.utils.UUIDUtils;
import org.apache.usergrid.rest.test.resource2point0.AbstractRestIT;
import org.apache.usergrid.rest.test.resource2point0.model.Collection;
import org.apache.usergrid.rest.test.resource2point0.model.Entity;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.usergrid.rest.test.resource2point0.model.Organization;
import org.apache.usergrid.rest.test.resource2point0.model.Token;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


public class ImportResourceIT extends AbstractRestIT {

    public ImportResourceIT() throws Exception {

    }

    @Test
    public void importCallSuccessful() throws Exception {

        String org = clientSetup.getOrganizationName();
        String app = clientSetup.getAppName();
        Entity payload = payloadBuilder();
        ///management/orgs/orgname/apps/appname/collection/users/import
        Entity entity = this.management().orgs().organization( org ).app().addToPath( app ).addToPath( "collection" ).addToPath( "users" ).addToPath( "import" ).post( payload );

        assertNotNull( entity );
        assertNotNull( entity.getString( "Import Entity" ));

    }



    @Test
    public void importApplicationUUIDRetTest() throws Exception {

        String org = clientSetup.getOrganizationName();
        String app = clientSetup.getAppName();
        Entity payload = payloadBuilder();
        ///management/orgs/orgname/apps/appname/import
        Entity entity = this.management().orgs().organization(org).app().addToPath( app ).addToPath("import").post(payload);

        assertNotNull( entity );
        assertNotNull( entity.getString( "Import Entity" ) );


    }


    @Test
    public void importOrganizationUUIDRetTest() throws Exception {

        String org = clientSetup.getOrganizationName();
        String app = clientSetup.getAppName();
        Entity payload = payloadBuilder();
        ///management/orgs/orgname/import
        Entity entity = this.management().orgs().organization(org).addToPath("import").post(payload);

        assertNotNull( entity );
        assertNotNull( entity.getString( "Import Entity" ) );

    }

    @Test
    public void importGetOrganizationJobStatTest() throws Exception {

        String org = clientSetup.getOrganizationName();
        String app = clientSetup.getAppName();
        Entity payload = payloadBuilder();
        ///management/orgs/orgname/import
        Entity entity = this.management().orgs().organization(org).addToPath("import").post(payload);
        String importEntity = entity.getString( "Import Entity" );


        assertNotNull( entity );
        assertNotNull( importEntity );

        entity = this.management().orgs().organization(org).addToPath("import").addToPath(importEntity).get();

        assertEquals( "SCHEDULED", entity.getString( "state" ) );//TODO: do tests for other states in service tier

    }


    @Test
    public void importGetApplicationJobStatTest() throws Exception {

        String org = clientSetup.getOrganizationName();
        String app = clientSetup.getAppName();
        Entity payload = payloadBuilder();
        ///management/orgs/orgname/apps/appname/import
        Entity entity = this.management().orgs().organization( org ).app().addToPath( app ).addToPath("import").post(payload);
        String importEntity = entity.getString( "Import Entity" );

        assertNotNull( entity );
        assertNotNull( importEntity );

        entity = this.management().orgs().organization( org ).addToPath("import").addToPath(importEntity).get();

        assertEquals( "SCHEDULED", entity.getString( "state" ) );//TODO: do tests for other states in service tier
    }


    @Test
    public void importGetCollectionJobStatTest() throws Exception {

        String org = clientSetup.getOrganizationName();
        String app = clientSetup.getAppName();
        Entity payload = payloadBuilder();
        ///management/orgs/orgname/apps/appname/import
        Entity entity = this.management().orgs().organization( org ).app().addToPath( app ).addToPath("collection")
            .addToPath("users").addToPath("import").post(payload);

        String importEntity = entity.getString( "Import Entity" );

        assertNotNull( entity );
        assertNotNull( importEntity );

        entity = this.management().orgs().organization(org).addToPath("import").addToPath(importEntity).get();

        assertEquals( "SCHEDULED", entity.getString( "state" ) );//TODO: do tests for other states in service tier

    }

    /**
     *
     * Verify that import job can only be read with authorized token
     *
     */
    @Test
    public void importTokenAuthorizationTest() throws Exception {
        //this test should post one import job with one token, then try to read back the job with another token

        //create an import job
        String org = clientSetup.getOrganizationName();
        String app = clientSetup.getAppName();
        Entity payload = payloadBuilder();
        ///management/orgs/orgname/apps/appname/import
        Entity entity = this.management().orgs().organization(org).app().addToPath( app ).addToPath("collection")
            .addToPath("users").addToPath("import").post(payload);

        String importEntity = entity.getString( "Import Entity" );

        assertNotNull( entity );
        assertNotNull( importEntity );

        //create a new org/app
        String newOrgName = "org"+UUIDUtils.newTimeUUID();
        String newOrgUsername = "orgusername"+UUIDUtils.newTimeUUID();
        String newOrgEmail = UUIDUtils.newTimeUUID()+"@usergrid.com";
        String newOrgPassword = "password1";
        Organization orgPayload = new Organization(newOrgName ,newOrgUsername ,newOrgEmail, newOrgName, newOrgPassword, null );
        Organization orgCreatedResponse = clientSetup.getRestClient().management().orgs().post( orgPayload );
        this.refreshIndex();
        assertNotNull( orgCreatedResponse );

        //log into the new org/app and get a token
        Token tokenPayload = new Token( "password", newOrgUsername, newOrgPassword );
        Token newOrgToken = clientSetup.getRestClient().management().token().post( tokenPayload );

        //save the old token and set the newly issued token as current
        Token oldToken = context().getToken();
        context().setToken(newOrgToken);


        //try to read with the new token, which should fail as unauthorized
        try {
            Entity newEntity = this.management().orgs().organization(org).apps(app).addToPath("collection")
                .addToPath("users").addToPath("import").get();
            fail("Should not be able to read import job with unauthorized token");
        } catch ( UniformInterfaceException ex ) {
            errorParse( 401,"unauthorized",ex);
        }

    }

    /**
     *
     * Verify that import job can only be read with authorized token
     *
     */

    @Test
    public void importPostToAppWithValidButUnauthorizedToken() throws Exception {
        //it should also post to an org app that doesn't belong to the token

        //fail();
        /*
        JsonNode node = null;
        ClientResponse.Status responseStatus = ClientResponse.Status.OK;
        UUID fake = UUID.fromString( "AAAAAAAA-FFFF-FFFF-FFFF-AAAAAAAAAAAA" );
        try {
            node = resource().path( "/management/orgs/test-organization/import/" + fake )
                .queryParam("access_token", superAdminToken()).accept( MediaType.APPLICATION_JSON )
                .type(MediaType.APPLICATION_JSON_TYPE).get( JsonNode.class );
        }
        catch ( UniformInterfaceException uie ) {
            responseStatus = uie.getResponse().getClientResponseStatus();
        }
        assertEquals( ClientResponse.Status.BAD_REQUEST, responseStatus );
        */
    }
/*
    @Test
    public void importPostApplicationNullPointerProperties() throws Exception {
        JsonNode node = null;
        ClientResponse.Status responseStatus = ClientResponse.Status.OK;

        HashMap<String, Object> payload = new HashMap<String, Object>();

        try {
            node = resource().path( "/management/orgs/test-organization/apps/test-app/import" )
                    .queryParam( "access_token", superAdminToken() ).accept( MediaType.APPLICATION_JSON )
                    .type( MediaType.APPLICATION_JSON_TYPE ).post( JsonNode.class, payload );
        }
        catch ( UniformInterfaceException uie ) {
            responseStatus = uie.getResponse().getClientResponseStatus();
        }
        assertEquals( ClientResponse.Status.BAD_REQUEST, responseStatus );
    }


    @Test
    public void importPostOrganizationNullPointerProperties() throws Exception {
        JsonNode node = null;
        ClientResponse.Status responseStatus = ClientResponse.Status.OK;

        HashMap<String, Object> payload = new HashMap<String, Object>();

        try {
            node = resource().path( "/management/orgs/test-organization/import" )
                    .queryParam( "access_token", superAdminToken() ).accept( MediaType.APPLICATION_JSON )
                    .type( MediaType.APPLICATION_JSON_TYPE ).post( JsonNode.class, payload );
        }
        catch ( UniformInterfaceException uie ) {
            responseStatus = uie.getResponse().getClientResponseStatus();
        }
        assertEquals( ClientResponse.Status.BAD_REQUEST, responseStatus );
    }

    @Test
    public void importPostCollectionNullPointer() throws Exception {
        JsonNode node = null;
        ClientResponse.Status responseStatus = ClientResponse.Status.OK;

        HashMap<String, Object> payload = new HashMap<String, Object>();

        try {
            node = resource().path( "/management/orgs/test-organization/apps/test-app/collection/users/import" )
                    .queryParam( "access_token", superAdminToken() ).accept( MediaType.APPLICATION_JSON )
                    .type( MediaType.APPLICATION_JSON_TYPE ).post( JsonNode.class, payload );
        }
        catch ( UniformInterfaceException uie ) {
            responseStatus = uie.getResponse().getClientResponseStatus();
        }
        assertEquals( ClientResponse.Status.BAD_REQUEST, responseStatus );
    }

        @Test
        public void importGetJobStatUnauthorized() throws Exception {
            JsonNode node = null;
            ClientResponse.Status responseStatus = ClientResponse.Status.OK;
            UUID fake = UUID.fromString( "AAAAAAAA-FFFF-FFFF-FFFF-AAAAAAAAAAAA" );
            try {
                node = resource().path( "/management/orgs/test-organization/import/" + fake )
                        .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE )
                        .get( JsonNode.class );
            }
            catch ( UniformInterfaceException uie ) {
                responseStatus = uie.getResponse().getClientResponseStatus();
            }
            assertEquals( ClientResponse.Status.UNAUTHORIZED, responseStatus );
        }

        @Test
        public void importPostOrganizationNullPointerStorageInfo() throws Exception {
            JsonNode node = null;
            ClientResponse.Status responseStatus = ClientResponse.Status.OK;

            HashMap<String, Object> payload = payloadBuilder();
            HashMap<String, Object> properties = ( HashMap<String, Object> ) payload.get( "properties" );
            //remove storage_info field
            properties.remove( "storage_info" );

            try {
                node = resource().path( "/management/orgs/test-organization/import" )
                        .queryParam( "access_token", superAdminToken() ).accept( MediaType.APPLICATION_JSON )
                        .type( MediaType.APPLICATION_JSON_TYPE ).post( JsonNode.class, payload );
            }
            catch ( UniformInterfaceException uie ) {
                responseStatus = uie.getResponse().getClientResponseStatus();
            }
            assertEquals( ClientResponse.Status.BAD_REQUEST, responseStatus );
        }

        @Test
        public void importPostApplicationNullPointerStorageInfo() throws Exception {
            JsonNode node = null;
            ClientResponse.Status responseStatus = ClientResponse.Status.OK;

            HashMap<String, Object> payload = payloadBuilder();
            HashMap<String, Object> properties = ( HashMap<String, Object> ) payload.get( "properties" );
            //remove storage_info field
            properties.remove( "storage_info" );

            try {
                node = resource().path( "/management/orgs/test-organization/apps/test-app/import" )
                        .queryParam( "access_token", superAdminToken() ).accept( MediaType.APPLICATION_JSON )
                        .type( MediaType.APPLICATION_JSON_TYPE ).post( JsonNode.class, payload );
            }
            catch ( UniformInterfaceException uie ) {
                responseStatus = uie.getResponse().getClientResponseStatus();
            }
            assertEquals( ClientResponse.Status.BAD_REQUEST, responseStatus );
        }

        @Test
        public void importPostCollectionNullPointerStorageInfo() throws Exception {
            JsonNode node = null;
            ClientResponse.Status responseStatus = ClientResponse.Status.OK;

            HashMap<String, Object> payload = payloadBuilder();
            HashMap<String, Object> properties = ( HashMap<String, Object> ) payload.get( "properties" );
            //remove storage_info field
            properties.remove( "storage_info" );

            try {
                node = resource().path( "/management/orgs/test-organization/apps/test-app/collection/users/import" )
                        .queryParam( "access_token", superAdminToken() ).accept( MediaType.APPLICATION_JSON )
                        .type( MediaType.APPLICATION_JSON_TYPE ).post( JsonNode.class, payload );
            }
            catch ( UniformInterfaceException uie ) {
                responseStatus = uie.getResponse().getClientResponseStatus();
            }
            assertEquals( ClientResponse.Status.BAD_REQUEST, responseStatus );
        }
*/
    /*
        @Test
        public void importPostOrganizationNullPointerStorageProvider() throws Exception {
            JsonNode node = null;
            ClientResponse.Status responseStatus = ClientResponse.Status.OK;

            HashMap<String, Object> payload = payloadBuilder();
            HashMap<String, Object> properties = ( HashMap<String, Object> ) payload.get( "properties" );
            //remove storage_info field
            properties.remove( "storage_provider" );


            try {
                node = resource().path( "/management/orgs/test-organization/import" )
                        .queryParam( "access_token", superAdminToken() ).accept( MediaType.APPLICATION_JSON )
                        .type( MediaType.APPLICATION_JSON_TYPE ).post( JsonNode.class, payload );
            }
            catch ( UniformInterfaceException uie ) {
                responseStatus = uie.getResponse().getClientResponseStatus();
            }
            assertEquals( ClientResponse.Status.BAD_REQUEST, responseStatus );
        }

        @Test
        public void importPostApplicationNullPointerStorageProvider() throws Exception {
            JsonNode node = null;
            ClientResponse.Status responseStatus = ClientResponse.Status.OK;

            HashMap<String, Object> payload = payloadBuilder();
            HashMap<String, Object> properties = ( HashMap<String, Object> ) payload.get( "properties" );
            //remove storage_info field
            properties.remove( "storage_provider" );


            try {
                node = resource().path( "/management/orgs/test-organization/apps/test-app/import" )
                        .queryParam( "access_token", superAdminToken() ).accept( MediaType.APPLICATION_JSON )
                        .type( MediaType.APPLICATION_JSON_TYPE ).post( JsonNode.class, payload );
            }
            catch ( UniformInterfaceException uie ) {
                responseStatus = uie.getResponse().getClientResponseStatus();
            }
            assertEquals( ClientResponse.Status.BAD_REQUEST, responseStatus );
        }

        @Test
        public void importPostCollectionNullPointerStorageProvider() throws Exception {
            JsonNode node = null;
            ClientResponse.Status responseStatus = ClientResponse.Status.OK;

            HashMap<String, Object> payload = payloadBuilder();
            HashMap<String, Object> properties = ( HashMap<String, Object> ) payload.get( "properties" );
            //remove storage_info field
            properties.remove( "storage_provider" );


            try {
                node = resource().path( "/management/orgs/test-organization/apps/test-app/collection/users/import" )
                        .queryParam( "access_token", superAdminToken() ).accept( MediaType.APPLICATION_JSON )
                        .type( MediaType.APPLICATION_JSON_TYPE ).post( JsonNode.class, payload );
            }
            catch ( UniformInterfaceException uie ) {
                responseStatus = uie.getResponse().getClientResponseStatus();
            }
            assertEquals( ClientResponse.Status.BAD_REQUEST, responseStatus );
        }

        @Test
        public void importPostOrganizationNullPointerStorageVerification() throws Exception {
            JsonNode node = null;
            ClientResponse.Status responseStatus = ClientResponse.Status.OK;

            HashMap<String, Object> payload = payloadBuilder();
            HashMap<String, Object> properties = ( HashMap<String, Object> ) payload.get( "properties" );
            HashMap<String, Object> storage_info = ( HashMap<String, Object> ) properties.get( "storage_info" );
            //remove storage_key field
            storage_info.remove( "s3_key" );

            try {
                node = resource().path( "/management/orgs/test-organization/import" )
                        .queryParam( "access_token", superAdminToken() ).accept( MediaType.APPLICATION_JSON )
                        .type( MediaType.APPLICATION_JSON_TYPE ).post( JsonNode.class, payload );
            }
            catch ( UniformInterfaceException uie ) {
                responseStatus = uie.getResponse().getClientResponseStatus();
            }
            assertEquals( ClientResponse.Status.BAD_REQUEST, responseStatus );

            payload = payloadBuilder();
            properties = ( HashMap<String, Object> ) payload.get( "properties" );
            storage_info = ( HashMap<String, Object> ) properties.get( "storage_info" );
            //remove storage_key field
            storage_info.remove( "s3_access_id" );

            try {
                node = resource().path( "/management/orgs/test-organization/import" )
                        .queryParam( "access_token", superAdminToken() ).accept( MediaType.APPLICATION_JSON )
                        .type( MediaType.APPLICATION_JSON_TYPE ).post( JsonNode.class, payload );
            }
            catch ( UniformInterfaceException uie ) {
                responseStatus = uie.getResponse().getClientResponseStatus();
            }
            assertEquals( ClientResponse.Status.BAD_REQUEST, responseStatus );

            payload = payloadBuilder();
            properties = ( HashMap<String, Object> ) payload.get( "properties" );
            storage_info = ( HashMap<String, Object> ) properties.get( "storage_info" );
            //remove storage_key field
            storage_info.remove( "bucket_location" );

            try {
                node = resource().path( "/management/orgs/test-organization/import" )
                        .queryParam( "access_token", superAdminToken() ).accept( MediaType.APPLICATION_JSON )
                        .type( MediaType.APPLICATION_JSON_TYPE ).post( JsonNode.class, payload );
            }
            catch ( UniformInterfaceException uie ) {
                responseStatus = uie.getResponse().getClientResponseStatus();
            }
            assertEquals( ClientResponse.Status.BAD_REQUEST, responseStatus );
        }
/*
        @Test
        public void importPostApplicationNullPointerStorageVerification() throws Exception {
            JsonNode node = null;
            ClientResponse.Status responseStatus = ClientResponse.Status.OK;

            HashMap<String, Object> payload = payloadBuilder();
            HashMap<String, Object> properties = ( HashMap<String, Object> ) payload.get( "properties" );
            HashMap<String, Object> storage_info = ( HashMap<String, Object> ) properties.get( "storage_info" );
            //remove storage_key field
            storage_info.remove( "s3_key" );

            try {
                node = resource().path( "/management/orgs/test-organization/apps/test-app/import" )
                        .queryParam( "access_token", superAdminToken() ).accept( MediaType.APPLICATION_JSON )
                        .type( MediaType.APPLICATION_JSON_TYPE ).post( JsonNode.class, payload );
            }
            catch ( UniformInterfaceException uie ) {
                responseStatus = uie.getResponse().getClientResponseStatus();
            }
            assertEquals( ClientResponse.Status.BAD_REQUEST, responseStatus );

            payload = payloadBuilder();
            properties = ( HashMap<String, Object> ) payload.get( "properties" );
            storage_info = ( HashMap<String, Object> ) properties.get( "storage_info" );
            //remove storage_key field
            storage_info.remove( "s3_access_id" );

            try {
                node = resource().path( "/management/orgs/test-organization/apps/test-app/import" )
                        .queryParam( "access_token", superAdminToken() ).accept( MediaType.APPLICATION_JSON )
                        .type( MediaType.APPLICATION_JSON_TYPE ).post( JsonNode.class, payload );
            }
            catch ( UniformInterfaceException uie ) {
                responseStatus = uie.getResponse().getClientResponseStatus();
            }
            assertEquals( ClientResponse.Status.BAD_REQUEST, responseStatus );

            payload = payloadBuilder();
            properties = ( HashMap<String, Object> ) payload.get( "properties" );
            storage_info = ( HashMap<String, Object> ) properties.get( "storage_info" );
            //remove storage_key field
            storage_info.remove( "bucket_location" );

            try {
                node = resource().path( "/management/orgs/test-organization/apps/test-app/import" )
                        .queryParam( "access_token", superAdminToken() ).accept( MediaType.APPLICATION_JSON )
                        .type( MediaType.APPLICATION_JSON_TYPE ).post( JsonNode.class, payload );
            }
            catch ( UniformInterfaceException uie ) {
                responseStatus = uie.getResponse().getClientResponseStatus();
            }
            assertEquals( ClientResponse.Status.BAD_REQUEST, responseStatus );
        }

        @Test
        public void importPostCollectionNullPointerStorageVerification() throws Exception {
            JsonNode node = null;
            ClientResponse.Status responseStatus = ClientResponse.Status.OK;

            HashMap<String, Object> payload = payloadBuilder();
            HashMap<String, Object> properties = ( HashMap<String, Object> ) payload.get( "properties" );
            HashMap<String, Object> storage_info = ( HashMap<String, Object> ) properties.get( "storage_info" );
            //remove storage_key field
            storage_info.remove( "s3_key" );

            try {
                node = resource().path( "/management/orgs/test-organization/apps/test-app/collection/users/import" )
                        .queryParam( "access_token", superAdminToken() ).accept( MediaType.APPLICATION_JSON )
                        .type( MediaType.APPLICATION_JSON_TYPE ).post( JsonNode.class, payload );
            }
            catch ( UniformInterfaceException uie ) {
                responseStatus = uie.getResponse().getClientResponseStatus();
            }
            assertEquals( ClientResponse.Status.BAD_REQUEST, responseStatus );

            payload = payloadBuilder();
            properties = ( HashMap<String, Object> ) payload.get( "properties" );
            storage_info = ( HashMap<String, Object> ) properties.get( "storage_info" );
            //remove storage_key field
            storage_info.remove( "s3_access_id" );

            try {
                node = resource().path( "/management/orgs/test-organization/apps/test-app/collection/users/import" )
                        .queryParam( "access_token", superAdminToken() ).accept( MediaType.APPLICATION_JSON )
                        .type( MediaType.APPLICATION_JSON_TYPE ).post( JsonNode.class, payload );
            }
            catch ( UniformInterfaceException uie ) {
                responseStatus = uie.getResponse().getClientResponseStatus();
            }
            assertEquals( ClientResponse.Status.BAD_REQUEST, responseStatus );

            payload = payloadBuilder();
            properties = ( HashMap<String, Object> ) payload.get( "properties" );
            storage_info = ( HashMap<String, Object> ) properties.get( "storage_info" );
            storage_info.remove( "bucket_location" );

            try {
                node = resource().path( "/management/orgs/test-organization/apps/test-app/collection/users/import" )
                        .queryParam( "access_token", superAdminToken() ).accept( MediaType.APPLICATION_JSON )
                        .type( MediaType.APPLICATION_JSON_TYPE ).post( JsonNode.class, payload );
            }
            catch ( UniformInterfaceException uie ) {
                responseStatus = uie.getResponse().getClientResponseStatus();
            }
            assertEquals( ClientResponse.Status.BAD_REQUEST, responseStatus );
        }
    */
    /*Creates fake payload for testing purposes.*/
    public Entity payloadBuilder() {
        Entity payload = new Entity();
        Entity properties = new Entity();
        Entity storage_info = new Entity();
        //TODO: always put dummy values here and ignore this test.
        //TODO: add a ret for when s3 values are invalid.
        storage_info.put( "s3_key", "insert key here" );
        storage_info.put( "s3_access_id", "insert access id here" );
        storage_info.put( "bucket_location", "insert bucket name here" );
        properties.put( "storage_provider", "s3" );
        properties.put( "storage_info", storage_info );
        payload.put( "properties", properties );
        return payload;
    }
}
