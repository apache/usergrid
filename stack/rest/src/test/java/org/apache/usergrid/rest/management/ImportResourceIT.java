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
import org.apache.usergrid.rest.AbstractRestIT;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by ApigeeCorporation on 7/8/14.
 */
public class ImportResourceIT extends AbstractRestIT {

    public ImportResourceIT() throws Exception {

    }

    @Test
    public void importCallSuccessful() throws Exception {
        ClientResponse.Status responseStatus = ClientResponse.Status.OK;
        JsonNode node = null;

        HashMap<String, Object> payload = payloadBuilder();

        try {
            node = resource().path( "/management/orgs/test-organization/apps/test-app/collection/users/import" )
                    .queryParam( "access_token", superAdminToken() ).accept( MediaType.APPLICATION_JSON )
                    .type( MediaType.APPLICATION_JSON_TYPE ).post( JsonNode.class, payload );
        }
        catch ( UniformInterfaceException uie ) {
            responseStatus = uie.getResponse().getClientResponseStatus();
        }

        assertEquals( ClientResponse.Status.OK, responseStatus );
    }

    @Test
    public void importCollectionUUIDRetTest() throws Exception {
        ClientResponse.Status responseStatus = ClientResponse.Status.ACCEPTED;
        JsonNode node = null;

        HashMap<String, Object> payload = payloadBuilder();

        try {
            node = resource().path( "/management/orgs/test-organization/apps/test-app/collection/users/import" )
                    .queryParam( "access_token", superAdminToken() ).accept( MediaType.APPLICATION_JSON )
                    .type( MediaType.APPLICATION_JSON_TYPE ).post( JsonNode.class, payload );
        }
        catch ( UniformInterfaceException uie ) {
            responseStatus = uie.getResponse().getClientResponseStatus();
        }

        assertEquals( ClientResponse.Status.ACCEPTED, responseStatus );
        assertNotNull( node.get( "Import Entity" ) );
    }

    @Test
    public void importApplicationUUIDRetTest() throws Exception {
        ClientResponse.Status responseStatus = ClientResponse.Status.ACCEPTED;
        JsonNode node = null;


        HashMap<String, Object> payload = payloadBuilder();

        try {
            node = mapper.readTree(resource().path( "/management/orgs/test-organization/apps/test-app/import" )
                    .queryParam( "access_token", superAdminToken() ).accept( MediaType.APPLICATION_JSON )
                    .type( MediaType.APPLICATION_JSON_TYPE ).post( String.class, payload ));
        }
        catch ( UniformInterfaceException uie ) {
            responseStatus = uie.getResponse().getClientResponseStatus();
        }

        assertEquals( ClientResponse.Status.ACCEPTED, responseStatus );
        assertNotNull( node.get( "Import Entity" ) );
    }

    @Test
    public void importOrganizationUUIDRetTest() throws Exception {
        ClientResponse.Status responseStatus = ClientResponse.Status.ACCEPTED;
        JsonNode node = null;


        HashMap<String, Object> payload = payloadBuilder();

        try {
            node = resource().path( "/management/orgs/test-organization/import" )
                    .queryParam( "access_token", superAdminToken() ).accept( MediaType.APPLICATION_JSON )
                    .type( MediaType.APPLICATION_JSON_TYPE ).post( JsonNode.class, payload );
        }
        catch ( UniformInterfaceException uie ) {
            responseStatus = uie.getResponse().getClientResponseStatus();
        }

        assertEquals( ClientResponse.Status.ACCEPTED, responseStatus );
        assertNotNull( node.get( "Import Entity" ) );
    }

    @Test
    public void importGetOrganizationJobStatTest() throws Exception {
        JsonNode node = null;
        ClientResponse.Status responseStatus = ClientResponse.Status.OK;

        HashMap<String, Object> payload = payloadBuilder();

        node = resource().path( "/management/orgs/test-organization/import" )
                .queryParam( "access_token", superAdminToken() ).accept( MediaType.APPLICATION_JSON )
                .type( MediaType.APPLICATION_JSON_TYPE ).post( JsonNode.class, payload );

        String uuid = String.valueOf( node.get( "Import Entity" ) );
        uuid = uuid.replaceAll( "\"", "" );

        try {
            node = mapper.readTree(resource().path( "/management/orgs/test-organization/import/" + uuid )
                    .queryParam( "access_token", superAdminToken() ).accept( MediaType.APPLICATION_JSON )
                    .type( MediaType.APPLICATION_JSON_TYPE ).get( String.class ));
        }
        catch ( UniformInterfaceException uie ) {
            responseStatus = uie.getResponse().getClientResponseStatus();
        }

        assertEquals( ClientResponse.Status.OK, responseStatus );
        assertEquals( "SCHEDULED", node.get( "state" ).textValue() );//TODO: do tests for other states in service tier

    }

    @Test
    public void importGetApplicationJobStatTest() throws Exception {
        JsonNode node = null;
        ClientResponse.Status responseStatus = ClientResponse.Status.OK;

        HashMap<String, Object> payload = payloadBuilder();

        node = resource().path( "/management/orgs/test-organization/apps/test-app/import" )
                .queryParam( "access_token", superAdminToken() ).accept( MediaType.APPLICATION_JSON )
                .type( MediaType.APPLICATION_JSON_TYPE ).post( JsonNode.class, payload );
        String uuid = String.valueOf( node.get( "Import Entity" ) );
        uuid = uuid.replaceAll( "\"", "" );

        try {
            node = resource().path( "/management/orgs/test-organization/import/" + uuid )
                    .queryParam( "access_token", superAdminToken() ).accept( MediaType.APPLICATION_JSON )
                    .type( MediaType.APPLICATION_JSON_TYPE ).get( JsonNode.class );
        }
        catch ( UniformInterfaceException uie ) {
            responseStatus = uie.getResponse().getClientResponseStatus();
        }

        assertEquals( ClientResponse.Status.OK, responseStatus );
        assertEquals( "SCHEDULED", node.get( "state" ).textValue() );//TODO: do tests for other states in service tier
    }

    @Test
    public void importGetCollectionJobStatTest() throws Exception {
        JsonNode node = null;
        ClientResponse.Status responseStatus = ClientResponse.Status.OK;

        HashMap<String, Object> payload = payloadBuilder();

        node = resource().path( "/management/orgs/test-organization/apps/test-app/collection/users/import" )
                .queryParam( "access_token", superAdminToken() ).accept( MediaType.APPLICATION_JSON )
                .type( MediaType.APPLICATION_JSON_TYPE ).post( JsonNode.class, payload );
        String uuid = String.valueOf( node.get( "Import Entity" ) );
        uuid = uuid.replaceAll( "\"", "" );

        try {
            node = resource().path( "/management/orgs/test-organization/import/" + uuid )
                    .queryParam( "access_token", superAdminToken() ).accept( MediaType.APPLICATION_JSON )
                    .type( MediaType.APPLICATION_JSON_TYPE ).get( JsonNode.class );
        }
        catch ( UniformInterfaceException uie ) {
            responseStatus = uie.getResponse().getClientResponseStatus();
        }

        assertEquals( ClientResponse.Status.OK, responseStatus );
        assertEquals( "SCHEDULED", node.get( "state" ).textValue() );//TODO: do tests for other states in service tier
    }

   //do an unauthorized test for both post and get
    @Test
    public void importGetWrongUUID() throws Exception {
        JsonNode node = null;
        ClientResponse.Status responseStatus = ClientResponse.Status.OK;
        UUID fake = UUID.fromString( "AAAAAAAA-FFFF-FFFF-FFFF-AAAAAAAAAAAA" );
        try {
            node = resource().path( "/management/orgs/test-organization/import/" + fake )
                    .queryParam( "access_token", superAdminToken() ).accept( MediaType.APPLICATION_JSON )
                    .type( MediaType.APPLICATION_JSON_TYPE ).get( JsonNode.class );
        }
        catch ( UniformInterfaceException uie ) {
            responseStatus = uie.getResponse().getClientResponseStatus();
        }
        assertEquals( ClientResponse.Status.BAD_REQUEST, responseStatus );
    }

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
