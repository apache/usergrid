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

import com.amazonaws.SDKGlobalConfiguration;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;

import org.apache.usergrid.persistence.index.utils.UUIDUtils;
import org.apache.usergrid.rest.test.resource2point0.AbstractRestIT;
import org.apache.usergrid.rest.test.resource2point0.model.Collection;
import org.apache.usergrid.rest.test.resource2point0.model.Entity;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.usergrid.rest.test.resource2point0.model.Organization;
import org.apache.usergrid.rest.test.resource2point0.model.Token;

import org.junit.Ignore;
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

    /**
     * Verify that we can get call the import endpoint and get the job state back.
     * @throws Exception
     */
    @Test
    public void importGetCollectionJobStatTest() throws Exception {

        String org = clientSetup.getOrganizationName();
        String app = clientSetup.getAppName();
        Entity payload = payloadBuilder();
        ///management/orgs/orgname/apps/appname/import
        Entity entity = this.management().orgs().organization( org ).app().addToPath( app ).addToPath( "import" ).post( payload );


        String importEntity = entity.getString( "Import Entity" );

        assertNotNull( entity );
        assertNotNull( importEntity );

        entity = this.management().orgs().organization( org ).app().addToPath( app ).addToPath("import").addToPath(importEntity).get();

        assertNotNull( entity.getString( "state" ) );
    }

    /**
     * Verify that import job can only be read with an authorized token and cannot be read
     * with an invalid/notAllowed token.
     */
    @Test
    public void importTokenAuthorizationTest() throws Exception {
        //this test should post one import job with one token, then try to read back the job with another token

        //create an import job
        String org = clientSetup.getOrganizationName();
        String app = clientSetup.getAppName();
        Entity payload = payloadBuilder();
        ///management/orgs/orgname/apps/appname/import
        Entity entity = this.management().orgs().organization( org ).app().addToPath( app ).addToPath( "import" ).post( payload );

        String importEntity = entity.getString( "Import Entity" );

        assertNotNull( entity );
        assertNotNull( importEntity );

        //Test that you can access the organization using the currently set token.
        this.management().orgs().organization(org).app().addToPath( app )
            .addToPath("import").addToPath(importEntity).get();

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
        context().setToken(newOrgToken);


        //try to read with the new token, which should fail as unauthorized
        try {
            this.management().orgs().organization(org).app().addToPath( app )
                                   .addToPath("import").addToPath(importEntity).get();
            fail("Should not be able to read import job with unauthorized token");
        } catch ( UniformInterfaceException ex ) {
            errorParse( 401,"unauthorized",ex);
        }

    }


    @Test
    public void importPostApplicationNullPointerProperties() throws Exception {
        String org = clientSetup.getOrganizationName();
        String app = clientSetup.getAppName();
        ClientResponse.Status responseStatus = ClientResponse.Status.OK;

        Entity payload = new Entity();

        try {
            this.management().orgs().organization( org ).app().addToPath( app ).addToPath( "import" ).post( payload );
        }
        catch ( UniformInterfaceException uie ) {
            responseStatus = uie.getResponse().getClientResponseStatus();
        }
        assertEquals( ClientResponse.Status.BAD_REQUEST, responseStatus );
    }
    @Test
    public void importPostApplicationNullPointerStorageInfo() throws Exception {
        String org = clientSetup.getOrganizationName();
        String app = clientSetup.getAppName();
        ClientResponse.Status responseStatus = ClientResponse.Status.OK;

        Entity payload = payloadBuilder();
        Entity properties = (Entity) payload.get( "properties" );
        //remove storage_info field
        properties.remove( "storage_info" );

        try {
            this.management().orgs().organization( org ).app().addToPath( app ).addToPath( "import" ).post( payload );
        }
        catch ( UniformInterfaceException uie ) {
            responseStatus = uie.getResponse().getClientResponseStatus();
        }
        assertEquals( ClientResponse.Status.BAD_REQUEST, responseStatus );
    }





        @Test
        public void importPostApplicationNullPointerStorageProvider() throws Exception {
            String org = clientSetup.getOrganizationName();
            String app = clientSetup.getAppName();
            ClientResponse.Status responseStatus = ClientResponse.Status.OK;

            Entity payload = payloadBuilder();
            Entity properties = (Entity) payload.get( "properties" );
            //remove storage_info field
            properties.remove( "storage_provider" );


            try {
                this.management().orgs().organization( org ).app().addToPath( app ).addToPath( "import" ).post( payload );
            }
            catch ( UniformInterfaceException uie ) {
                responseStatus = uie.getResponse().getClientResponseStatus();
            }
            assertEquals( ClientResponse.Status.BAD_REQUEST, responseStatus );
        }




        @Test
        public void importPostApplicationNullPointerStorageVerification() throws Exception {
            String org = clientSetup.getOrganizationName();
            String app = clientSetup.getAppName();
            ClientResponse.Status responseStatus = ClientResponse.Status.OK;

            Entity payload = payloadBuilder();

            Entity properties = (Entity) payload.get( "properties" );
            Entity storage_info = (Entity) properties.get( "storage_info" );
            //remove storage_key field
            storage_info.remove( SDKGlobalConfiguration.SECRET_KEY_ENV_VAR);

            try {
                this.management().orgs().organization( org ).app().addToPath( app ).addToPath( "import" ).post( payload );
            }
            catch ( UniformInterfaceException uie ) {
                responseStatus = uie.getResponse().getClientResponseStatus();
            }
            assertEquals( ClientResponse.Status.BAD_REQUEST, responseStatus );

            payload = payloadBuilder();
            properties = (Entity) payload.get( "properties" );
            storage_info = (Entity) properties.get( "storage_info" );
            //remove storage_key field
            storage_info.remove( SDKGlobalConfiguration.ACCESS_KEY_ENV_VAR );

            try {
                this.management().orgs().organization( org ).app().addToPath( app ).addToPath( "import" ).post( payload );
            }
            catch ( UniformInterfaceException uie ) {
                responseStatus = uie.getResponse().getClientResponseStatus();
            }
            assertEquals( ClientResponse.Status.BAD_REQUEST, responseStatus );

            payload = payloadBuilder();
            properties = (Entity) payload.get( "properties" );
            storage_info = (Entity) properties.get( "storage_info" );
            //remove storage_key field
            storage_info.remove( "bucket_location" );

            try {
                this.management().orgs().organization( org ).app().addToPath( app ).addToPath( "import" ).post( payload );
            }
            catch ( UniformInterfaceException uie ) {
                responseStatus = uie.getResponse().getClientResponseStatus();
            }
            assertEquals( ClientResponse.Status.BAD_REQUEST, responseStatus );
        }



    /*Creates fake payload for testing purposes.*/
    public Entity payloadBuilder() {
        Entity payload = new Entity();
        Entity properties = new Entity();
        Entity storage_info = new Entity();
        //TODO: always put dummy values here and ignore this test.
        //TODO: add a ret for when s3 values are invalid.
        storage_info.put( SDKGlobalConfiguration.SECRET_KEY_ENV_VAR, "insert key here" );
        storage_info.put( SDKGlobalConfiguration.ACCESS_KEY_ENV_VAR, "insert access id here" );
        storage_info.put( "bucket_location", "insert bucket name here" );
        properties.put( "storage_provider", "s3" );
        properties.put( "storage_info", storage_info );
        payload.put( "properties", properties );
        return payload;
    }
}
