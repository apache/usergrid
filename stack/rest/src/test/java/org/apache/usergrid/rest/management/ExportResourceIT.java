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


import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.core.MediaType;

import org.codehaus.jackson.JsonNode;
import org.junit.Ignore;
import org.junit.Test;

import org.apache.usergrid.cassandra.Concurrent;
import org.apache.usergrid.rest.AbstractRestIT;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;

import static org.apache.usergrid.utils.MapUtils.hashMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


/**
 *
 *
 */
@Concurrent
public class ExportResourceIT extends AbstractRestIT {


    public ExportResourceIT() throws Exception {

    }

    @Test
    public void exportCallSuccessful() throws Exception {
        ClientResponse.Status responseStatus = ClientResponse.Status.OK;
        JsonNode node = null;

        HashMap<String, Object> payload = payloadBuilder();

        try {
            node = resource().path( "/management/orgs/test-organization/apps/test-app/collection/users/export" )
                             .queryParam( "access_token", superAdminToken() ).accept( MediaType.APPLICATION_JSON )
                             .type( MediaType.APPLICATION_JSON_TYPE ).post( JsonNode.class, payload );
        }
        catch ( UniformInterfaceException uie ) {
            responseStatus = uie.getResponse().getClientResponseStatus();
        }

        assertEquals( ClientResponse.Status.OK, responseStatus );
    }


    //is this test still valid knowing that the sch. won't run in intelliJ?
    @Ignore
    public void exportCallCreationEntities100() throws Exception {
        ClientResponse.Status responseStatus = ClientResponse.Status.OK;
        JsonNode node = null;

        HashMap<String, Object> payload = new HashMap<String, Object>();
        Map<String, Object> properties = new HashMap<String, Object>();
        Map<String, Object> storage_info = new HashMap<String, Object>();
        //TODO: make sure to put a valid admin token here.
        //TODO: always put dummy values here and ignore this test.


        properties.put( "storage_provider", "s3" );
        properties.put( "storage_info", storage_info );

        payload.put( "properties", properties );

        for ( int i = 0; i < 100; i++ ) {
            Map<String, String> userCreation = hashMap( "type", "app_user" ).map( "name", "fred" + i );

            node = resource().path( "/test-organization/test-app/app_users" ).queryParam( "access_token", access_token )
                             .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE )
                             .post( JsonNode.class, userCreation );
        }

        try {
            node = resource().path( "/management/orgs/test-organization/apps/test-app/export" )
                             .queryParam( "access_token", adminToken() ).accept( MediaType.APPLICATION_JSON )
                             .type( MediaType.APPLICATION_JSON_TYPE ).post( JsonNode.class, payload );
        }
        catch ( UniformInterfaceException uie ) {
            responseStatus = uie.getResponse().getClientResponseStatus();
        }

        assertEquals( ClientResponse.Status.OK, responseStatus );
    }


    @Test
    public void exportApplicationUUIDRetTest() throws Exception {
        ClientResponse.Status responseStatus = ClientResponse.Status.ACCEPTED;
        String uuid;
        UUID jobUUID = null;
        JsonNode node = null;


        HashMap<String, Object> payload = payloadBuilder();

        try {
            node = resource().path( "/management/orgs/test-organization/apps/test-app/export" )
                             .queryParam( "access_token", superAdminToken() ).accept( MediaType.APPLICATION_JSON )
                             .type( MediaType.APPLICATION_JSON_TYPE ).post( JsonNode.class, payload );
        }
        catch ( UniformInterfaceException uie ) {
            responseStatus = uie.getResponse().getClientResponseStatus();
        }

        assertEquals( ClientResponse.Status.ACCEPTED, responseStatus );
        assertNotNull( node.get( "Export Entity" ) );
    }


    //
    @Test
    public void exportCollectionUUIDRetTest() throws Exception {
        ClientResponse.Status responseStatus = ClientResponse.Status.ACCEPTED;
        String uuid;
        UUID jobUUID = null;
        JsonNode node = null;


        HashMap<String, Object> payload = payloadBuilder();

        try {
            node = resource().path( "/management/orgs/test-organization/apps/test-app/collection/users/export" )
                             .queryParam( "access_token", superAdminToken() ).accept( MediaType.APPLICATION_JSON )
                             .type( MediaType.APPLICATION_JSON_TYPE ).post( JsonNode.class, payload );
        }
        catch ( UniformInterfaceException uie ) {
            responseStatus = uie.getResponse().getClientResponseStatus();
        }

        assertEquals( ClientResponse.Status.ACCEPTED, responseStatus );
        assertNotNull( node.get( "Export Entity" ) );
    }


    @Test
    public void exportGetOrganizationJobStatTest() throws Exception {
        JsonNode node = null;
        ClientResponse.Status responseStatus = ClientResponse.Status.OK;

        HashMap<String, Object> payload = payloadBuilder();

        try {
            node = resource().path( "/management/orgs/test-organization/export" )
                             .queryParam( "access_token", superAdminToken() ).accept( MediaType.APPLICATION_JSON )
                             .type( MediaType.APPLICATION_JSON_TYPE ).post( JsonNode.class, payload );
        }
        catch ( UniformInterfaceException uie ) {
            responseStatus = uie.getResponse().getClientResponseStatus();
        }
        assertEquals( ClientResponse.Status.OK,responseStatus);

        String uuid = String.valueOf( node.get( "Export Entity" ) );
        uuid = uuid.replaceAll( "\"", "" );

        try {
            node = resource().path( "/management/orgs/test-organization/export/" + uuid )
                             .queryParam( "access_token", superAdminToken() ).accept( MediaType.APPLICATION_JSON )
                             .type( MediaType.APPLICATION_JSON_TYPE ).get( JsonNode.class );
        }
        catch ( UniformInterfaceException uie ) {
            responseStatus = uie.getResponse().getClientResponseStatus();
        }


        assertEquals( ClientResponse.Status.OK, responseStatus );
        assertEquals( "SCHEDULED", node.get( "state" ).getTextValue() );//TODO: do tests for other states in service tier
    }


    //all tests should be moved to OrganizationResourceIT ( *not* Organizations there is a difference)
    @Test
    public void exportGetApplicationJobStatTest() throws Exception {
        JsonNode node = null;
        ClientResponse.Status responseStatus = ClientResponse.Status.OK;

        HashMap<String, Object> payload = payloadBuilder();

        node = resource().path( "/management/orgs/test-organization/apps/test-app/export" )
                         .queryParam( "access_token", superAdminToken() ).accept( MediaType.APPLICATION_JSON )
                         .type( MediaType.APPLICATION_JSON_TYPE ).post( JsonNode.class, payload );
        String uuid = String.valueOf( node.get( "Export Entity" ) );
        uuid = uuid.replaceAll( "\"", "" );

        try {
            node = resource().path( "/management/orgs/test-organization/export/" + uuid )
                             .queryParam( "access_token", superAdminToken() ).accept( MediaType.APPLICATION_JSON )
                             .type( MediaType.APPLICATION_JSON_TYPE ).get( JsonNode.class );
        }
        catch ( UniformInterfaceException uie ) {
            responseStatus = uie.getResponse().getClientResponseStatus();
        }


        assertEquals( ClientResponse.Status.OK, responseStatus );
        assertEquals( "SCHEDULED", node.get( "state" ).getTextValue() );//TODO: do tests for other states in service tier
    }


    @Test
    public void exportGetCollectionJobStatTest() throws Exception {
        JsonNode node = null;
        ClientResponse.Status responseStatus = ClientResponse.Status.OK;

        HashMap<String, Object> payload = payloadBuilder();

        node = resource().path( "/management/orgs/test-organization/apps/test-app/collection/users/export" )
                         .queryParam( "access_token", superAdminToken() ).accept( MediaType.APPLICATION_JSON )
                         .type( MediaType.APPLICATION_JSON_TYPE ).post( JsonNode.class, payload );
        String uuid = String.valueOf( node.get( "Export Entity" ) );
        uuid = uuid.replaceAll( "\"", "" );

        try {
            node = resource().path( "/management/orgs/test-organization/export/" + uuid )
                             .queryParam( "access_token", superAdminToken() ).accept( MediaType.APPLICATION_JSON )
                             .type( MediaType.APPLICATION_JSON_TYPE ).get( JsonNode.class );
        }
        catch ( UniformInterfaceException uie ) {
            responseStatus = uie.getResponse().getClientResponseStatus();
        }


        assertEquals( ClientResponse.Status.OK, responseStatus );
        assertEquals( "SCHEDULED", node.get( "state" ).getTextValue() );//TODO: do tests for other states in service tier
    }


    //    //do an unauthorized test for both post and get
    @Test
    public void exportGetWrongUUID() throws Exception {
        JsonNode node = null;
        ClientResponse.Status responseStatus = ClientResponse.Status.OK;
        UUID fake = UUID.fromString( "AAAAAAAA-FFFF-FFFF-FFFF-AAAAAAAAAAAA" );
        try {
            node = resource().path( "/management/orgs/test-organization/export/" + fake )
                             .queryParam( "access_token", superAdminToken() ).accept( MediaType.APPLICATION_JSON )
                             .type( MediaType.APPLICATION_JSON_TYPE ).get( JsonNode.class );
        }
        catch ( UniformInterfaceException uie ) {
            responseStatus = uie.getResponse().getClientResponseStatus();
        }
        assertEquals( ClientResponse.Status.BAD_REQUEST, responseStatus );
    }


    //
    @Test
    public void exportPostApplicationNullPointerProperties() throws Exception {
        JsonNode node = null;
        ClientResponse.Status responseStatus = ClientResponse.Status.OK;

        HashMap<String, Object> payload = new HashMap<String, Object>();
        Map<String, Object> properties = new HashMap<String, Object>();
        Map<String, Object> storage_info = new HashMap<String, Object>();
        //TODO: always put dummy values here and ignore this test.
        //TODO: add a ret for when s3 values are invalid.
        storage_info.put( "bucket_location", "insert bucket name here" );


        properties.put( "storage_provider", "s3" );
        properties.put( "storage_info", storage_info );


        try {
            node = resource().path( "/management/orgs/test-organization/apps/test-app/export" )
                             .queryParam( "access_token", superAdminToken() ).accept( MediaType.APPLICATION_JSON )
                             .type( MediaType.APPLICATION_JSON_TYPE ).post( JsonNode.class, payload );
        }
        catch ( UniformInterfaceException uie ) {
            responseStatus = uie.getResponse().getClientResponseStatus();
        }
        assertEquals( ClientResponse.Status.BAD_REQUEST, responseStatus );
    }


    @Test
    public void exportPostOrganizationNullPointerProperties() throws Exception {
        JsonNode node = null;
        ClientResponse.Status responseStatus = ClientResponse.Status.OK;

        HashMap<String, Object> payload = new HashMap<String, Object>();
        Map<String, Object> properties = new HashMap<String, Object>();
        Map<String, Object> storage_info = new HashMap<String, Object>();
        //TODO: always put dummy values here and ignore this test.
        //TODO: add a ret for when s3 values are invalid.
        storage_info.put( "bucket_location", "insert bucket name here" );


        properties.put( "storage_provider", "s3" );
        properties.put( "storage_info", storage_info );


        try {
            node = resource().path( "/management/orgs/test-organization/export" )
                             .queryParam( "access_token", superAdminToken() ).accept( MediaType.APPLICATION_JSON )
                             .type( MediaType.APPLICATION_JSON_TYPE ).post( JsonNode.class, payload );
        }
        catch ( UniformInterfaceException uie ) {
            responseStatus = uie.getResponse().getClientResponseStatus();
        }
        assertEquals( ClientResponse.Status.BAD_REQUEST, responseStatus );
    }


    //
    @Test
    public void exportPostCollectionNullPointer() throws Exception {
        JsonNode node = null;
        ClientResponse.Status responseStatus = ClientResponse.Status.OK;

        HashMap<String, Object> payload = new HashMap<String, Object>();
        Map<String, Object> properties = new HashMap<String, Object>();
        Map<String, Object> storage_info = new HashMap<String, Object>();
        //TODO: always put dummy values here and ignore this test.
        //TODO: add a ret for when s3 values are invalid.
        storage_info.put( "bucket_location", "insert bucket name here" );


        properties.put( "storage_provider", "s3" );
        properties.put( "storage_info", storage_info );


        try {
            node = resource().path( "/management/orgs/test-organization/apps/test-app/collection/users/export" )
                             .queryParam( "access_token", superAdminToken() ).accept( MediaType.APPLICATION_JSON )
                             .type( MediaType.APPLICATION_JSON_TYPE ).post( JsonNode.class, payload );
        }
        catch ( UniformInterfaceException uie ) {
            responseStatus = uie.getResponse().getClientResponseStatus();
        }
        assertEquals( ClientResponse.Status.BAD_REQUEST, responseStatus );
    }


    //
    //
    @Test
    public void exportGetCollectionUnauthorized() throws Exception {
        JsonNode node = null;
        ClientResponse.Status responseStatus = ClientResponse.Status.OK;
        UUID fake = UUID.fromString( "AAAAAAAA-FFFF-FFFF-FFFF-AAAAAAAAAAAA" );
        try {
            node = resource().path( "/management/orgs/test-organization/apps/test-app/collection/users/export/" + fake )
                             .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE )
                             .get( JsonNode.class );
        }
        catch ( UniformInterfaceException uie ) {
            responseStatus = uie.getResponse().getClientResponseStatus();
        }
        assertEquals( ClientResponse.Status.UNAUTHORIZED, responseStatus );
    }


    //
    @Test
    public void exportGetApplicationUnauthorized() throws Exception {
        JsonNode node = null;
        ClientResponse.Status responseStatus = ClientResponse.Status.OK;
        UUID fake = UUID.fromString( "AAAAAAAA-FFFF-FFFF-FFFF-AAAAAAAAAAAA" );
        try {
            node = resource().path( "/management/orgs/test-organization/apps/test-app/export/" + fake )
                             .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE )
                             .get( JsonNode.class );
        }
        catch ( UniformInterfaceException uie ) {
            responseStatus = uie.getResponse().getClientResponseStatus();
        }
        assertEquals( ClientResponse.Status.UNAUTHORIZED, responseStatus );
    }


    @Test
    public void exportGetOrganizationUnauthorized() throws Exception {
        JsonNode node = null;
        ClientResponse.Status responseStatus = ClientResponse.Status.OK;
        UUID fake = UUID.fromString( "AAAAAAAA-FFFF-FFFF-FFFF-AAAAAAAAAAAA" );
        try {
            node = resource().path( "/management/orgs/test-organization/export/" + fake )
                             .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE )
                             .get( JsonNode.class );
        }
        catch ( UniformInterfaceException uie ) {
            responseStatus = uie.getResponse().getClientResponseStatus();
        }
        assertEquals( ClientResponse.Status.UNAUTHORIZED, responseStatus );
    }


    @Test
    public void exportPostOrganizationNullPointerStorageInfo() throws Exception {
        JsonNode node = null;
        ClientResponse.Status responseStatus = ClientResponse.Status.OK;

        HashMap<String, Object> payload = payloadBuilder();
        HashMap<String, Object> properties = ( HashMap<String, Object> ) payload.get( "properties" );
        //remove storage_info field
        properties.remove( "storage_info" );

        try {
            node = resource().path( "/management/orgs/test-organization/export" )
                             .queryParam( "access_token", superAdminToken() ).accept( MediaType.APPLICATION_JSON )
                             .type( MediaType.APPLICATION_JSON_TYPE ).post( JsonNode.class, payload );
        }
        catch ( UniformInterfaceException uie ) {
            responseStatus = uie.getResponse().getClientResponseStatus();
        }
        assertEquals( ClientResponse.Status.BAD_REQUEST, responseStatus );
    }


    @Test
    public void exportPostApplicationNullPointerStorageInfo() throws Exception {
        JsonNode node = null;
        ClientResponse.Status responseStatus = ClientResponse.Status.OK;

        HashMap<String, Object> payload = payloadBuilder();
        HashMap<String, Object> properties = ( HashMap<String, Object> ) payload.get( "properties" );
        //remove storage_info field
        properties.remove( "storage_info" );

        try {
            node = resource().path( "/management/orgs/test-organization/apps/test-app/export" )
                             .queryParam( "access_token", superAdminToken() ).accept( MediaType.APPLICATION_JSON )
                             .type( MediaType.APPLICATION_JSON_TYPE ).post( JsonNode.class, payload );
        }
        catch ( UniformInterfaceException uie ) {
            responseStatus = uie.getResponse().getClientResponseStatus();
        }
        assertEquals( ClientResponse.Status.BAD_REQUEST, responseStatus );
    }


    @Test
    public void exportPostCollectionNullPointerStorageInfo() throws Exception {
        JsonNode node = null;
        ClientResponse.Status responseStatus = ClientResponse.Status.OK;

        HashMap<String, Object> payload = payloadBuilder();
        HashMap<String, Object> properties = ( HashMap<String, Object> ) payload.get( "properties" );
        //remove storage_info field
        properties.remove( "storage_info" );

        try {
            node = resource().path( "/management/orgs/test-organization/apps/test-app/collection/users/export" )
                             .queryParam( "access_token", superAdminToken() ).accept( MediaType.APPLICATION_JSON )
                             .type( MediaType.APPLICATION_JSON_TYPE ).post( JsonNode.class, payload );
        }
        catch ( UniformInterfaceException uie ) {
            responseStatus = uie.getResponse().getClientResponseStatus();
        }
        assertEquals( ClientResponse.Status.BAD_REQUEST, responseStatus );
    }


    @Test
    public void exportPostApplicationNullPointerStorageProvider() throws Exception {
        JsonNode node = null;
        ClientResponse.Status responseStatus = ClientResponse.Status.OK;

        HashMap<String, Object> payload = payloadBuilder();
        HashMap<String, Object> properties = ( HashMap<String, Object> ) payload.get( "properties" );
        //remove storage_info field
        properties.remove( "storage_provider" );


        try {
            node = resource().path( "/management/orgs/test-organization/apps/test-app/export" )
                             .queryParam( "access_token", superAdminToken() ).accept( MediaType.APPLICATION_JSON )
                             .type( MediaType.APPLICATION_JSON_TYPE ).post( JsonNode.class, payload );
        }
        catch ( UniformInterfaceException uie ) {
            responseStatus = uie.getResponse().getClientResponseStatus();
        }
        assertEquals( ClientResponse.Status.BAD_REQUEST, responseStatus );
    }


    @Test
    public void exportPostCollectionNullPointerStorageProvider() throws Exception {
        JsonNode node = null;
        ClientResponse.Status responseStatus = ClientResponse.Status.OK;

        HashMap<String, Object> payload = payloadBuilder();
        HashMap<String, Object> properties = ( HashMap<String, Object> ) payload.get( "properties" );
        //remove storage_info field
        properties.remove( "storage_provider" );


        try {
            node = resource().path( "/management/orgs/test-organization/apps/test-app/collection/users/export" )
                             .queryParam( "access_token", superAdminToken() ).accept( MediaType.APPLICATION_JSON )
                             .type( MediaType.APPLICATION_JSON_TYPE ).post( JsonNode.class, payload );
        }
        catch ( UniformInterfaceException uie ) {
            responseStatus = uie.getResponse().getClientResponseStatus();
        }
        assertEquals( ClientResponse.Status.BAD_REQUEST, responseStatus );
    }


    @Test
    public void exportPostApplicationNullPointerStorageVerification() throws Exception {
        JsonNode node = null;
        ClientResponse.Status responseStatus = ClientResponse.Status.OK;

        HashMap<String, Object> payload = payloadBuilder();
        HashMap<String, Object> properties = ( HashMap<String, Object> ) payload.get( "properties" );
        HashMap<String, Object> storage_info = ( HashMap<String, Object> ) properties.get( "storage_info" );
        //remove storage_key field
        storage_info.remove( "s3_key" );

        try {
            node = resource().path( "/management/orgs/test-organization/apps/test-app/export" )
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
            node = resource().path( "/management/orgs/test-organization/apps/test-app/export" )
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
            node = resource().path( "/management/orgs/test-organization/apps/test-app/export" )
                             .queryParam( "access_token", superAdminToken() ).accept( MediaType.APPLICATION_JSON )
                             .type( MediaType.APPLICATION_JSON_TYPE ).post( JsonNode.class, payload );
        }
        catch ( UniformInterfaceException uie ) {
            responseStatus = uie.getResponse().getClientResponseStatus();
        }
        assertEquals( ClientResponse.Status.BAD_REQUEST, responseStatus );
    }


    @Test
    public void exportPostCollectionNullPointerStorageVerification() throws Exception {
        JsonNode node = null;
        ClientResponse.Status responseStatus = ClientResponse.Status.OK;

        HashMap<String, Object> payload = payloadBuilder();
        HashMap<String, Object> properties = ( HashMap<String, Object> ) payload.get( "properties" );
        HashMap<String, Object> storage_info = ( HashMap<String, Object> ) properties.get( "storage_info" );
        //remove storage_key field
        storage_info.remove( "s3_key" );

        try {
            node = resource().path( "/management/orgs/test-organization/apps/test-app/collection/users/export" )
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
            node = resource().path( "/management/orgs/test-organization/apps/test-app/collection/users/export" )
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
            node = resource().path( "/management/orgs/test-organization/apps/test-app/collection/users/export" )
                             .queryParam( "access_token", superAdminToken() ).accept( MediaType.APPLICATION_JSON )
                             .type( MediaType.APPLICATION_JSON_TYPE ).post( JsonNode.class, payload );
        }
        catch ( UniformInterfaceException uie ) {
            responseStatus = uie.getResponse().getClientResponseStatus();
        }
        assertEquals( ClientResponse.Status.BAD_REQUEST, responseStatus );
    }


    /*Creates fake payload for testing purposes.*/
    public HashMap<String, Object> payloadBuilder() {
        HashMap<String, Object> payload = new HashMap<String, Object>();
        Map<String, Object> properties = new HashMap<String, Object>();
        Map<String, Object> storage_info = new HashMap<String, Object>();
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
