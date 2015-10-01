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


import org.apache.usergrid.rest.test.resource.AbstractRestIT;
import org.apache.usergrid.rest.test.resource.model.ApiResponse;
import org.junit.Ignore;
import org.junit.Test;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.*;


public class ExportResourceIT extends AbstractRestIT {



    public ExportResourceIT() throws Exception {

    }

    @Ignore
    @Test
    public void exportApplicationUUIDRetTest() throws Exception {

        try {
            management().orgs().org( clientSetup.getOrganizationName() )
                        .app().addToPath( clientSetup.getAppUuid() ).addToPath( "export" );
        }
        catch ( ClientErrorException uie ) {
            fail("We got back " + uie.getResponse().getStatus() + " instead of having a successful call" );
        }

    }

    @Ignore
    @Test
    public void exportCollectionUUIDRetTest() throws Exception {

        HashMap<String, Object> payload = payloadBuilder();
        ApiResponse exportEntity = null;

        try {


            exportEntity = management().orgs().org( clientSetup.getOrganizationName() )
                        .app().addToPath( clientSetup.getAppUuid()).addToPath( "collection" )
                        .addToPath( "users" ).addToPath( "export" ).post(ApiResponse.class,payloadBuilder() );
        }
        catch ( ClientErrorException uie ) {
            fail( "We got back "+uie.getResponse().getStatus()+" instead of having a successful call" );
        }

        assertNotNull( exportEntity );
        assertNotNull( exportEntity.getProperties().get( "Export Entity" ));
    }


    /**
     * Check that you can get the org export uuid returned and that you can check the status of the job using that uuid.
     * @throws Exception
     */
    @Ignore
    @Test
    public void exportGetOrganizationJobStatTest() throws Exception {

        ApiResponse exportEntity = null;


        try {
            exportEntity = management().orgs().org( clientSetup.getOrganizationName() )
                                       .addToPath( "export" ).post( ApiResponse.class, payloadBuilder() );
        }
        catch ( ClientErrorException uie ) {
            fail( "We got back "+uie.getResponse().getStatus()+" instead of having a successful call" );
        }

        assertNotNull( exportEntity );
        String uuid = ( String ) exportEntity.getProperties().get( "Export Entity" );
        assertNotNull( uuid );

        exportEntity = null;
        try {

            exportEntity = management().orgs().org( clientSetup.getOrganizationName() )
                                       .addToPath( "export" ).addToPath( uuid ).get( ApiResponse.class );
        }
        catch ( ClientErrorException uie ) {
            fail( "We got back "+uie.getResponse().getStatus()+" instead of having a successful call" );
        }

        assertNotNull( exportEntity );
        String state = (String) exportEntity.getProperties().get( "state" );
        assertEquals( "SCHEDULED", state);
    }
//
//


    /**
     * Check that you can get the app export uuid returned and that you can check the status of the job using that uuid.
     * @throws Exception
     */

    @Ignore
    @Test
    public void exportGetApplicationJobStatTest() throws Exception {

        ApiResponse exportEntity = null;


        try {
            exportEntity = management().orgs().org( clientSetup.getOrganizationName() )
                                       .app().addToPath( clientSetup.getAppUuid() )
                                       .addToPath( "export" ).post( ApiResponse.class, payloadBuilder() );
        }
        catch ( ClientErrorException uie ) {
            fail( "We got back "+uie.getResponse().getStatus()+" instead of having a successful call" );
        }

        assertNotNull( exportEntity );
        String uuid = ( String ) exportEntity.getProperties().get( "Export Entity" );
        assertNotNull( uuid );

        exportEntity = null;
        refreshIndex();
        try {

            exportEntity = management().orgs().org( clientSetup.getOrganizationName() )
                                       .addToPath( "export" ).addToPath( uuid ).get( ApiResponse.class );
        }
        catch ( ClientErrorException uie ) {
            fail( "We got back "+uie.getResponse().getStatus()+" instead of having a successful call" );
        }

        assertNotNull( exportEntity );
        String state = (String) exportEntity.getProperties().get( "state" );
        assertEquals( "SCHEDULED", state);
    }


    @Ignore
    @Test
    public void exportGetCollectionJobStatTest() throws Exception {

        ApiResponse exportEntity = null;

        exportEntity = management().orgs().org( clientSetup.getOrganizationName() )
                                   .app().addToPath( clientSetup.getAppUuid()).addToPath( "collection" )
                                   .addToPath( "users" ).addToPath( "export" )
                                   .post( ApiResponse.class, payloadBuilder() );

        assertNotNull( exportEntity );
        String uuid = ( String ) exportEntity.getProperties().get( "Export Entity" );
        assertNotNull( uuid );

        exportEntity = null;
        try {
            exportEntity = management().orgs().org( clientSetup.getOrganizationName() )
                                       .addToPath( "export" ).addToPath( uuid ).get( ApiResponse.class );
        }
        catch ( ClientErrorException uie ) {
            fail( "We got back "+uie.getResponse().getStatus()+" instead of having a successful call" );
        }


        assertNotNull( exportEntity );
        String state = (String) exportEntity.getProperties().get( "state" );
        assertEquals( "SCHEDULED", state);
    }
//
//
//    //    //do an unauthorized test for both post and get
@Ignore
@Test
    public void exportGetWrongUUID() throws Exception {
        UUID fake = UUID.fromString( "AAAAAAAA-FFFF-FFFF-FFFF-AAAAAAAAAAAA" );
        try {
            management().orgs().org( clientSetup.getOrganizationName() )
                                       .addToPath( "export" ).addToPath( fake.toString() ).get( ApiResponse.class );
            fail( "Should not have been able to get fake uuid" );
        }
        catch ( ClientErrorException uie ) {
            assertEquals( Response.Status.BAD_REQUEST.getStatusCode(), uie.getResponse().getStatus() );

        }
    }


    //
    @Ignore
    @Test
    public void exportPostApplicationNullPointerProperties() throws Exception {
        try {
            management().orgs().org( clientSetup.getOrganizationName() )
                                       .app().addToPath( clientSetup.getAppUuid() )
                                       .addToPath( "export" ).post( ApiResponse.class,
                new HashMap<String, Object>() );
            fail( "Should not have passed, The payload is empty." );
        }
        catch ( ClientErrorException uie ) {
            assertEquals( Response.Status.BAD_REQUEST.getStatusCode(), uie.getResponse().getStatus() );
        }
    }


    @Ignore
    @Test
    public void exportPostOrganizationNullPointerProperties() throws Exception {
        try {
            management().orgs().org( clientSetup.getOrganizationName() )
                        .addToPath( "export" ).post( ApiResponse.class, new HashMap<String, Object>()  );
            fail( "Should not have passed, The payload is empty." );
        }
        catch ( ClientErrorException uie ) {
            assertEquals( Response.Status.BAD_REQUEST.getStatusCode(), uie.getResponse().getStatus() );
        }
    }

    //
    @Ignore
    @Test
    public void exportPostCollectionNullPointer() throws Exception {
        try {
            management().orgs().org( clientSetup.getOrganizationName() )
                        .app().addToPath( clientSetup.getAppUuid() )
                        .addToPath( "collection" ).addToPath( "users" )
                        .addToPath( "export" ).post( ApiResponse.class, new HashMap<String, Object>()  );

            fail( "Should not have passed, The payload is empty." );
        }
        catch ( ClientErrorException uie ) {
            assertEquals( Response.Status.BAD_REQUEST.getStatusCode(), uie.getResponse().getStatus() );
        }
    }

    @Ignore
    @Test
    public void exportGetCollectionUnauthorized() throws Exception {
        UUID fake = UUID.fromString( "AAAAAAAA-FFFF-FFFF-FFFF-AAAAAAAAAAAA" );

        try {
            management().orgs().org( clientSetup.getOrganizationName() )
                        .app().addToPath( clientSetup.getAppUuid() )
                        .addToPath( "collection" ).addToPath( "users" )
                        .addToPath( "export" ).addToPath( fake.toString() ).get(ApiResponse.class ,false);
            fail( "Should not have passed as we didn't have an access token." );
        }
        catch ( ClientErrorException uie ) {
            assertEquals( Response.Status.UNAUTHORIZED.getStatusCode(), uie.getResponse().getStatus() );
        }
    }

    @Ignore
    @Test
    public void exportGetApplicationUnauthorized() throws Exception {
        UUID fake = UUID.fromString( "AAAAAAAA-FFFF-FFFF-FFFF-AAAAAAAAAAAA" );

        try {
            management().orgs().org( clientSetup.getOrganizationName() )
                        .app().addToPath( clientSetup.getAppUuid() )
                        .addToPath( "export" ).addToPath( fake.toString() ).get(ApiResponse.class ,false);
            fail( "Should not have passed as we didn't have an access token." );
        }
        catch ( ClientErrorException uie ) {
            assertEquals( Response.Status.UNAUTHORIZED.getStatusCode(), uie.getResponse().getStatus() );
        }
    }

    @Ignore
    @Test
    public void exportGetOrganizationUnauthorized() throws Exception {
        UUID fake = UUID.fromString( "AAAAAAAA-FFFF-FFFF-FFFF-AAAAAAAAAAAA" );

        try {
            management().orgs().org( clientSetup.getOrganizationName() )
                        .addToPath( "export" ).addToPath( fake.toString() ).get(ApiResponse.class ,false);
            fail( "Should not have passed as we didn't have an access token." );
        }
        catch ( ClientErrorException uie ) {
            assertEquals( Response.Status.UNAUTHORIZED.getStatusCode(), uie.getResponse().getStatus() );
        }
    }

    @Ignore
    @Test
    public void exportPostOrganizationNullPointerStorageInfo() throws Exception {
        HashMap<String, Object> payload = payloadBuilder();
        HashMap<String, Object> properties = ( HashMap<String, Object> ) payload.get( "properties" );
        //remove storage_info field
        properties.remove( "storage_info" );

        try {
            management().orgs().org( clientSetup.getOrganizationName() )
                        .addToPath( "export" ).post( ApiResponse.class, payload );
            fail( "Should not have passed as we were missing an important part of the payload" );

        }
        catch ( ClientErrorException uie ) {
            assertEquals( Response.Status.BAD_REQUEST.getStatusCode(), uie.getResponse().getStatus() );
        }
    }


    @Ignore
    @Test
    public void exportPostApplicationNullPointerStorageInfo() throws Exception {
        HashMap<String, Object> payload = payloadBuilder();
        HashMap<String, Object> properties = ( HashMap<String, Object> ) payload.get( "properties" );
        //remove storage_info field
        properties.remove( "storage_info" );

        try {
            management().orgs().org( clientSetup.getOrganizationName() )
                        .app().addToPath( clientSetup.getAppUuid() )
                        .addToPath( "export" ).post( ApiResponse.class,
                payload );
            fail( "Should not have passed as we were missing an important part of the payload" );

        }
        catch ( ClientErrorException uie ) {
            assertEquals( Response.Status.BAD_REQUEST.getStatusCode(), uie.getResponse().getStatus() );
        }
    }

    @Ignore
    @Test
    public void exportPostCollectionNullPointerStorageInfo() throws Exception {
        HashMap<String, Object> payload = payloadBuilder();
        HashMap<String, Object> properties = ( HashMap<String, Object> ) payload.get( "properties" );
        //remove storage_info field
        properties.remove( "storage_info" );

        try {
            management().orgs().org( clientSetup.getOrganizationName() )
                        .app().addToPath( clientSetup.getAppUuid() )
                        .addToPath( "collection" ).addToPath( "users" )
                        .addToPath( "export" ).post( ApiResponse.class,
                payload );
            fail( "Should not have passed as we were missing an important part of the payload" );

        }
        catch ( ClientErrorException uie ) {
            assertEquals( Response.Status.BAD_REQUEST.getStatusCode(), uie.getResponse().getStatus() );
        }
    }

    @Ignore
    @Test
    public void exportPostOrganizationNullPointerStorageProvider() throws Exception {
        HashMap<String, Object> payload = payloadBuilder();
        HashMap<String, Object> properties = ( HashMap<String, Object> ) payload.get( "properties" );
        //remove storage_info field
        properties.remove( "storage_provider" );

        try {
            management().orgs().org( clientSetup.getOrganizationName() )
                        .addToPath( "export" ).post( ApiResponse.class, payload );
            fail( "Should not have passed as we were missing an important part of the payload" );

        }
        catch ( ClientErrorException uie ) {
            assertEquals( Response.Status.BAD_REQUEST.getStatusCode(), uie.getResponse().getStatus() );
        }
    }


    @Ignore
    @Test
    public void exportPostApplicationNullPointerStorageProvider() throws Exception {
        HashMap<String, Object> payload = payloadBuilder();
        HashMap<String, Object> properties = ( HashMap<String, Object> ) payload.get( "properties" );
        //remove storage_info field
        properties.remove( "storage_provider" );

        try {
            management().orgs().org( clientSetup.getOrganizationName() )
                        .app().addToPath( clientSetup.getAppUuid() )
                        .addToPath( "export" ).post( ApiResponse.class,
                payload );
            fail( "Should not have passed as we were missing an important part of the payload" );

        }
        catch ( ClientErrorException uie ) {
            assertEquals( Response.Status.BAD_REQUEST.getStatusCode(), uie.getResponse().getStatus() );
        }
    }

    @Ignore
    @Test
    public void exportPostCollectionNullPointerStorageProvider() throws Exception {
        HashMap<String, Object> payload = payloadBuilder();
        HashMap<String, Object> properties = ( HashMap<String, Object> ) payload.get( "properties" );
        //remove storage_info field
        properties.remove( "storage_provider" );

        try {
            management().orgs().org( clientSetup.getOrganizationName() )
                        .app().addToPath( clientSetup.getAppUuid() )
                        .addToPath( "collection" ).addToPath( "users" )
                        .addToPath( "export" ).post( ApiResponse.class,
                payload );
            fail( "Should not have passed as we were missing an important part of the payload" );

        }
        catch ( ClientErrorException uie ) {
            assertEquals( Response.Status.BAD_REQUEST.getStatusCode(), uie.getResponse().getStatus() );
        }
    }


    @Ignore
    @Test
    public void exportPostOrganizationNullPointerStorageVerification() throws Exception {
        HashMap<String, Object> payload = payloadBuilder();
        HashMap<String, Object> properties = ( HashMap<String, Object> ) payload.get( "properties" );
        HashMap<String, Object> storage_info = ( HashMap<String, Object> ) properties.get( "storage_info" );
        //remove storage_key field
        storage_info.remove( "s3_key" );

        try {
            management().orgs().org( clientSetup.getOrganizationName() )
                        .addToPath( "export" ).post( ApiResponse.class, payload );
            fail( "Should not have passed as we were missing an important part of the payload" );

        }
        catch ( ClientErrorException uie ) {
            assertEquals( Response.Status.BAD_REQUEST.getStatusCode(), uie.getResponse().getStatus() );
        }

        payload = payloadBuilder();
        properties = ( HashMap<String, Object> ) payload.get( "properties" );
        storage_info = ( HashMap<String, Object> ) properties.get( "storage_info" );
        //remove storage_key field
        storage_info.remove( "s3_access_id");

        try {
            management().orgs().org( clientSetup.getOrganizationName() )
                        .addToPath( "export" ).post( ApiResponse.class, payload );
            fail( "Should not have passed as we were missing an important part of the payload" );

        }
        catch ( ClientErrorException uie ) {
            assertEquals( Response.Status.BAD_REQUEST.getStatusCode(), uie.getResponse().getStatus() );
        }

        payload = payloadBuilder();
        properties = ( HashMap<String, Object> ) payload.get( "properties" );
        storage_info = ( HashMap<String, Object> ) properties.get( "storage_info" );
        //remove storage_key field
        storage_info.remove( "bucket_location" );

        try {
            management().orgs().org( clientSetup.getOrganizationName() )
                        .addToPath( "export" ).post( ApiResponse.class, payload );
            fail( "Should not have passed as we were missing an important part of the payload" );

        }
        catch ( ClientErrorException uie ) {
            assertEquals( Response.Status.BAD_REQUEST.getStatusCode(), uie.getResponse().getStatus() );
        }
    }

    @Ignore
    @Test
    public void exportPostApplicationNullPointerStorageVerification() throws Exception {
        HashMap<String, Object> payload = payloadBuilder();
        HashMap<String, Object> properties = ( HashMap<String, Object> ) payload.get( "properties" );
        HashMap<String, Object> storage_info = ( HashMap<String, Object> ) properties.get( "storage_info" );
        //remove storage_key field
        storage_info.remove( "s3_key" );

        try {
            management().orgs().org( clientSetup.getOrganizationName() )
                        .app().addToPath( clientSetup.getAppUuid() )
                        .addToPath( "export" ).post( ApiResponse.class, payload );
            fail( "Should not have passed as we were missing an important part of the payload" );

        }
        catch ( ClientErrorException uie ) {
            assertEquals( Response.Status.BAD_REQUEST.getStatusCode(), uie.getResponse().getStatus() );
        }

        payload = payloadBuilder();
        properties = ( HashMap<String, Object> ) payload.get( "properties" );
        storage_info = ( HashMap<String, Object> ) properties.get( "storage_info" );
        //remove storage_key field
        storage_info.remove( "s3_access_id" );

        try {
            management().orgs().org( clientSetup.getOrganizationName() )
                        .app().addToPath( clientSetup.getAppUuid() )
                        .addToPath( "export" ).post( ApiResponse.class, payload );
            fail( "Should not have passed as we were missing an important part of the payload" );

        }
        catch ( ClientErrorException uie ) {
            assertEquals( Response.Status.BAD_REQUEST.getStatusCode(), uie.getResponse().getStatus() );
        }

        payload = payloadBuilder();
        properties = ( HashMap<String, Object> ) payload.get( "properties" );
        storage_info = ( HashMap<String, Object> ) properties.get( "storage_info" );
        //remove storage_key field
        storage_info.remove( "bucket_location" );

        try {
            management().orgs().org( clientSetup.getOrganizationName() )
                        .app().addToPath( clientSetup.getAppUuid() )
                        .addToPath( "export" ).post( ApiResponse.class, payload );
            fail( "Should not have passed as we were missing an important part of the payload" );

        }
        catch ( ClientErrorException uie ) {
            assertEquals( Response.Status.BAD_REQUEST.getStatusCode(), uie.getResponse().getStatus() );
        }
    }

    @Ignore
    @Test
    public void exportPostCollectionNullPointerStorageVerification() throws Exception {
        HashMap<String, Object> payload = payloadBuilder();
        HashMap<String, Object> properties = ( HashMap<String, Object> ) payload.get( "properties" );
        HashMap<String, Object> storage_info = ( HashMap<String, Object> ) properties.get( "storage_info" );
        //remove storage_key field
        storage_info.remove( "s3_key" );

        try {
            management().orgs().org( clientSetup.getOrganizationName() )
                        .app().addToPath( clientSetup.getAppUuid() )
                        .addToPath( "collection" ).addToPath( "users" )
                        .addToPath( "export" ).post( ApiResponse.class, payload );
            fail( "Should not have passed as we were missing an important part of the payload" );

        }
        catch ( ClientErrorException uie ) {
            assertEquals( Response.Status.BAD_REQUEST.getStatusCode(), uie.getResponse().getStatus() );
        }

        payload = payloadBuilder();
        properties = ( HashMap<String, Object> ) payload.get( "properties" );
        storage_info = ( HashMap<String, Object> ) properties.get( "storage_info" );
        //remove storage_key field
        storage_info.remove( "s3_access_id" );

        try {
            management().orgs().org( clientSetup.getOrganizationName() )
                        .app().addToPath( clientSetup.getAppUuid() )
                        .addToPath( "collection" ).addToPath( "users" )
                        .addToPath( "export" ).post( ApiResponse.class, payload );
            fail( "Should not have passed as we were missing an important part of the payload" );

        }
        catch ( ClientErrorException uie ) {
            assertEquals( Response.Status.BAD_REQUEST.getStatusCode(), uie.getResponse().getStatus() );
        }

        payload = payloadBuilder();
        properties = ( HashMap<String, Object> ) payload.get( "properties" );
        storage_info = ( HashMap<String, Object> ) properties.get( "storage_info" );
        //remove storage_key field
        storage_info.remove( "bucket_location" );

        try {
            management().orgs().org( clientSetup.getOrganizationName() )
                        .app().addToPath( clientSetup.getAppUuid() )
                        .addToPath( "collection" ).addToPath( "users" )
                        .addToPath( "export" ).post( ApiResponse.class, payload );
            fail( "Should not have passed as we were missing an important part of the payload" );

        }
        catch ( ClientErrorException uie ) {
            assertEquals( Response.Status.BAD_REQUEST.getStatusCode(), uie.getResponse().getStatus() );
        }
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
