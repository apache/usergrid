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
package org.apache.usergrid.rest.applications.assets;


import com.fasterxml.jackson.databind.JsonNode;
import com.sun.jersey.multipart.FormDataMultiPart;
import org.apache.commons.io.IOUtils;
import org.apache.usergrid.rest.applications.utils.UserRepo;
import org.apache.usergrid.rest.test.resource2point0.AbstractRestIT;
import org.apache.usergrid.services.assets.data.AssetUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import static org.apache.usergrid.utils.JsonUtils.mapToFormattedJsonString;
import static org.apache.usergrid.utils.MapUtils.hashMap;
import static org.junit.Assert.*;


public class AssetResourceIT extends AbstractRestIT {

    private String access_token;
    private Logger LOG = LoggerFactory.getLogger( AssetResourceIT.class );
    UserRepo userRepo;

    @Before
    public void setup(){
        userRepo = new UserRepo(this.clientSetup);
        access_token = this.getAdminToken().getAccessToken();
    }


    /** @Deprecated Tests legacy API */
    @Test
    @Ignore
    public void verifyBinaryCrud() throws Exception {

        userRepo.load();

        this.refreshIndex();

        UUID userId = userRepo.getByUserName( "user1" );
        Map<String, String> payload =
                hashMap( "path", "my/clean/path" ).map( "owner", userId.toString() ).map( "someprop", "somevalue" );

        String orgAppPath = clientSetup.getOrganizationName() + "/" + clientSetup.getAppName();

        JsonNode node =
                mapper.readTree( resource().path( orgAppPath + "/assets" ).queryParam( "access_token", access_token )
                    .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE )
                    .post( String.class, payload ) );
        JsonNode idNode = node.get( "entities" ).get( 0 ).get( "uuid" );
        UUID id = UUID.fromString( idNode.textValue() );
        assertNotNull( idNode.textValue() );

        byte[] data = IOUtils.toByteArray( this.getClass().getResourceAsStream( "/cassandra_eye.jpg" ) );
        resource().path( orgAppPath + "/assets/" + id.toString() + "/data" )
                .queryParam( "access_token", access_token ).type( MediaType.APPLICATION_OCTET_STREAM_TYPE ).put( data );

        refreshIndex();
        InputStream is = resource().path( orgAppPath + "/assets/" + id.toString() + "/data" )
                .queryParam( "access_token", access_token ).get( InputStream.class );

        byte[] foundData = IOUtils.toByteArray( is );
        assertEquals( 7979, foundData.length );

        refreshIndex();

        node = mapper.readTree( resource().path( orgAppPath + "/assets/my/clean/path" )
            .queryParam( "access_token", access_token ).accept( MediaType.APPLICATION_JSON_TYPE )
            .get( String.class ) );

        idNode = node.get( "entities" ).get( 0 ).get( "uuid" );
        assertEquals( id.toString(), idNode.textValue() );
    }


    @Test
    @Ignore
    public void octetStreamOnDynamicEntity() throws Exception {

        this.refreshIndex();

        Map<String, String> payload = hashMap( "name", "assetname" );

        String orgAppPath = clientSetup.getOrganizationName() + "/" + clientSetup.getAppName();

        JsonNode node = mapper.readTree( resource().path( orgAppPath + "/foos" ).queryParam( "access_token", access_token )
            .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE )
            .post( String.class, payload ) );

        JsonNode idNode = node.get( "entities" ).get( 0 ).get( "uuid" );
        String uuid = idNode.textValue();
        assertNotNull( uuid );

        byte[] data = IOUtils.toByteArray( this.getClass().getResourceAsStream( "/cassandra_eye.jpg" ) );
        resource().path( orgAppPath + "/foos/" + uuid ).queryParam( "access_token", access_token )
                .type( MediaType.APPLICATION_OCTET_STREAM_TYPE ).put( data );

        // get entity
        node = mapper.readTree( resource().path( orgAppPath + "/foos/" + uuid ).queryParam( "access_token", access_token )
                .accept( MediaType.APPLICATION_JSON_TYPE ).get( String.class ));
        Assert.assertEquals( "image/jpeg", node.findValue( AssetUtils.CONTENT_TYPE ).textValue() );
        Assert.assertEquals( 7979, node.findValue( "content-length" ).intValue() );
        idNode = node.get( "entities" ).get( 0 ).get( "uuid" );
        assertEquals( uuid, idNode.textValue() );

        // get data by UUID
        InputStream is =
                resource().path( orgAppPath + "/foos/" + uuid ).queryParam( "access_token", access_token )
                        .accept( MediaType.APPLICATION_OCTET_STREAM_TYPE ).get( InputStream.class );

        byte[] foundData = IOUtils.toByteArray( is );
        assertEquals( 7979, foundData.length );

        refreshIndex();

        // get data by name
        is = resource().path( orgAppPath + "/foos/assetname" ).queryParam( "access_token", access_token )
                .accept( MediaType.APPLICATION_OCTET_STREAM_TYPE ).get( InputStream.class );

        foundData = IOUtils.toByteArray( is );
        assertEquals( 7979, foundData.length );
    }


    @Test
    @Ignore
    public void multipartPostFormOnDynamicEntity() throws Exception {

        this.refreshIndex();

        byte[] data = IOUtils.toByteArray( this.getClass().getResourceAsStream( "/file-bigger-than-5M" ) );
        FormDataMultiPart form = new FormDataMultiPart().field( "file", data, MediaType.MULTIPART_FORM_DATA_TYPE );

        String orgAppPath = clientSetup.getOrganizationName() + "/" + clientSetup.getAppName();

        JsonNode node = mapper.readTree( resource().path( orgAppPath + "/foos" )
            .queryParam( "access_token", access_token )
            .accept( MediaType.APPLICATION_JSON )
            .type( MediaType.MULTIPART_FORM_DATA )
            .post( String.class, form ));

        JsonNode idNode = node.get( "entities" ).get( 0 ).get( "uuid" );
        String uuid = idNode.textValue();
        assertNotNull(uuid);

        this.refreshIndex();

        int retries = 0;
        boolean done = false;
        byte[] foundData = new byte[0];

        // retry until upload complete
        while ( !done && retries < 30 ) {

            // get data
            try {
                InputStream is = resource().path( orgAppPath + "/foos/" + uuid )
                        .queryParam( "access_token", access_token )
                        .accept( MediaType.APPLICATION_OCTET_STREAM_TYPE )
                        .get( InputStream.class );

                foundData = IOUtils.toByteArray( is );
                done = true;

            } catch ( Exception intentiallyIgnored ) {}

            Thread.sleep(1000);
            retries++;
        }

        assertEquals( 5324800, foundData.length );

        // delete
        node = mapper.readTree( resource().path( orgAppPath + "/foos/" + uuid ).queryParam( "access_token", access_token )
                .accept( MediaType.APPLICATION_JSON_TYPE ).delete( String.class ));
    }


    @Test
    @Ignore
    public void multipartPutFormOnDynamicEntity() throws Exception {

        this.refreshIndex();

        Map<String, String> payload = hashMap( "foo", "bar" );

        String orgAppPath = clientSetup.getOrganizationName() + "/" + clientSetup.getAppName();

        JsonNode node = mapper.readTree( resource().path( orgAppPath + "/foos" ).queryParam( "access_token", access_token )
                .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE )
                .post( String.class, payload ));

        JsonNode idNode = node.get( "entities" ).get( 0 ).get( "uuid" );
        String uuid = idNode.textValue();
        assertNotNull( uuid );

        // set file & assetname
        byte[] data = IOUtils.toByteArray( this.getClass().getResourceAsStream( "/cassandra_eye.jpg" ) );
        FormDataMultiPart form = new FormDataMultiPart().field( "foo", "bar2" )
                                                        .field( "file", data, MediaType.MULTIPART_FORM_DATA_TYPE );

        long created = System.currentTimeMillis();
        node = mapper.readTree( resource().path( orgAppPath + "/foos/" + uuid )
            .queryParam( "access_token", access_token )
            .accept( MediaType.APPLICATION_JSON )
            .type( MediaType.MULTIPART_FORM_DATA )
            .put( String.class, form ));

        this.refreshIndex();

        // get entity
        node = mapper.readTree( resource().path( orgAppPath + "/foos/" + uuid )
            .queryParam( "access_token", access_token )
            .accept( MediaType.APPLICATION_JSON_TYPE )
            .get( String.class ));
        LOG.debug( mapToFormattedJsonString(node) );

        assertEquals( "image/jpeg", node.findValue( AssetUtils.CONTENT_TYPE ).textValue() );
        assertEquals( 7979, node.findValue( AssetUtils.CONTENT_LENGTH ).intValue() );
        idNode = node.get( "entities" ).get( 0 ).get( "uuid" );
        assertEquals( uuid, idNode.textValue() );
        JsonNode nameNode = node.get( "entities" ).get( 0 ).get( "foo" );
        assertEquals( "bar2", nameNode.textValue() );
        long lastModified = node.findValue( AssetUtils.LAST_MODIFIED ).longValue();
        Assert.assertEquals( created, lastModified, 500 );

        // get data
        InputStream is = resource().path( orgAppPath + "/foos/" + uuid )
            .queryParam( "access_token", access_token )
            .accept( "image/jpeg" )
            .get( InputStream.class );

        byte[] foundData = IOUtils.toByteArray( is );
        assertEquals( 7979, foundData.length );

        // post new data
        node = mapper.readTree( resource().path( orgAppPath + "/foos/" + uuid )
            .queryParam( "access_token", access_token )
            .accept( MediaType.APPLICATION_JSON )
            .type( MediaType.MULTIPART_FORM_DATA )
            .put( String.class, form ) );
        Assert.assertTrue( lastModified != node.findValue( AssetUtils.LAST_MODIFIED ).longValue() );
    }


    @Test
    @Ignore
    public void largeFileInS3() throws Exception {

        this.refreshIndex();

        byte[] data = IOUtils.toByteArray( this.getClass().getResourceAsStream( "/file-bigger-than-5M" ) );
        FormDataMultiPart form = new FormDataMultiPart().field( "file", data, MediaType.MULTIPART_FORM_DATA_TYPE );

        String orgAppPath = clientSetup.getOrganizationName() + "/" + clientSetup.getAppName();

        // send data
        JsonNode node = mapper.readTree( resource().path( orgAppPath + "/foos" )
            .queryParam( "access_token", access_token )
            .accept( MediaType.APPLICATION_JSON )
            .type( MediaType.MULTIPART_FORM_DATA )
            .post( String.class, form ) );
        JsonNode idNode = node.get( "entities" ).get( 0 ).get( "uuid" );
        String uuid = idNode.textValue();

        // get entity
        //TODO: seperate tests for s3 and local system property tests.
            LOG.info( "Waiting for upload to finish..." );
            Thread.sleep( 2000 );
            node = mapper.readTree( resource().path( orgAppPath + "/foos/" + uuid )
                .queryParam( "access_token", access_token )
                .accept( MediaType.APPLICATION_JSON_TYPE )
                .get( String.class ) );

        LOG.info( "Upload complete!" );

        // get data
        InputStream is = resource().path( orgAppPath + "/foos/" + uuid )
            .queryParam( "access_token", access_token )
            .accept( MediaType.APPLICATION_OCTET_STREAM_TYPE )
            .get( InputStream.class );

        byte[] foundData = IOUtils.toByteArray( is );
        assertEquals( data.length, foundData.length );

        // delete
        node = mapper.readTree( resource().path( orgAppPath + "/foos/" + uuid )
            .queryParam( "access_token", access_token )
            .accept( MediaType.APPLICATION_JSON_TYPE )
            .delete( String.class ) );
    }

    @Test
    @Ignore
    public void fileTooLargeShouldResultInError() throws Exception {

        this.refreshIndex();

        Map<String, String> props = new HashMap<String, String>();
        props.put( "usergrid.binary.max-size-mb", "6" );
        resource().path( "/testproperties" )
                .queryParam( "access_token", access_token )
                .accept( MediaType.APPLICATION_JSON )
                .type( MediaType.APPLICATION_JSON_TYPE ).post( props );

        try {

            //UserRepo.INSTANCE.load( resource(), access_token );

            byte[] data = IOUtils.toByteArray( this.getClass().getResourceAsStream( "/cat-larger-than-6mb.jpg" ) );
            FormDataMultiPart form = new FormDataMultiPart().field( "file", data, MediaType.MULTIPART_FORM_DATA_TYPE );

            String orgAppPath = clientSetup.getOrganizationName() + "/" + clientSetup.getAppName();

            // send data
            JsonNode node = resource().path( orgAppPath + "/bars" ).queryParam( "access_token", access_token )
                    .accept( MediaType.APPLICATION_JSON ).type( MediaType.MULTIPART_FORM_DATA )
                    .post( JsonNode.class, form );
            //logNode( node );
            JsonNode idNode = node.get( "entities" ).get( 0 ).get( "uuid" );
            String uuid = idNode.textValue();

            // get entity
            String errorMessage = null;
            //TODO: seperate tests for s3 and local system property tests.
                LOG.info( "Waiting for upload to finish..." );
                Thread.sleep( 2000 );
                node = resource().path( orgAppPath + "/bars/" + uuid )
                        .queryParam( "access_token", access_token ).accept( MediaType.APPLICATION_JSON_TYPE )
                        .get( JsonNode.class );

                // check for the error
                if (node.findValue( "error" ) != null) {
                    errorMessage = node.findValue("error").asText();
                }

            assertTrue( errorMessage.startsWith("Asset size "));

        } finally {
            props = new HashMap<String, String>();
            props.put( "usergrid.binary.max-size-mb", "25" );
            resource().path( "/testproperties" )
                    .queryParam( "access_token", access_token )
                    .accept( MediaType.APPLICATION_JSON )
                    .type( MediaType.APPLICATION_JSON_TYPE ).post( props );
        }
    }

    /**
     * Deleting a connection to an asset should not delete the asset or the asset's data
     */
    @Test
    @Ignore
    public void deleteConnectionToAsset() throws IOException {

        this.refreshIndex();

        final String uuid;

        access_token = this.getAdminToken().getAccessToken();

        String orgAppPath = clientSetup.getOrganizationName() + "/" + clientSetup.getAppName();

        // create the entity that will be the asset, an image

        Map<String, String> payload = hashMap("name", "cassandra_eye.jpg");

        JsonNode node = resource().path("/test-organization/test-app/foos")
                .queryParam("access_token", access_token)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .post(JsonNode.class, payload);

        JsonNode idNode = node.get("entities").get(0).get("uuid");
        uuid = idNode.textValue();

        // post image data to the asset entity

        byte[] data = IOUtils.toByteArray(this.getClass().getResourceAsStream("/cassandra_eye.jpg"));
        resource().path(orgAppPath + "/foos/" + uuid)
                .queryParam("access_token", access_token)
                .type(MediaType.APPLICATION_OCTET_STREAM_TYPE)
                .put(data);

        // create an imagegallery entity

        Map<String, String> imageGalleryPayload = hashMap("name", "my image gallery");

        JsonNode imageGalleryNode = resource().path(orgAppPath + "/imagegalleries")
                .queryParam("access_token", access_token)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .post(JsonNode.class, imageGalleryPayload);

        JsonNode imageGalleryIdNode = imageGalleryNode.get("entities").get(0).get("uuid");
        String imageGalleryId = imageGalleryIdNode.textValue();

        // connect imagegallery to asset

        JsonNode connectNode = resource()
                .path(orgAppPath + "/imagegalleries/" + imageGalleryId + "/contains/" + uuid)
                .queryParam("access_token", access_token)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .post(JsonNode.class);
        LOG.debug( mapToFormattedJsonString(connectNode) );

        this.refreshIndex();

        // verify connection from imagegallery to asset

        JsonNode listConnectionsNode = resource()
                .path(orgAppPath + "/imagegalleries/" + imageGalleryId + "/contains/")
                .queryParam("access_token", access_token)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .get(JsonNode.class);
        LOG.debug( mapToFormattedJsonString(listConnectionsNode) );
        assertEquals(uuid, listConnectionsNode.get("entities").get(0).get("uuid").textValue());

        // delete the connection

        resource().path(orgAppPath + "/imagegalleries/" + imageGalleryId + "/contains/" + uuid)
                .queryParam("access_token", access_token)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .delete();

        this.refreshIndex();

        // verify that connection is gone

        listConnectionsNode = resource()
                .path(orgAppPath + "/imagegalleries/" + imageGalleryId + "/contains/")
                .queryParam("access_token", access_token)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .get(JsonNode.class);
        assertFalse(listConnectionsNode.get("entities").elements().hasNext());

        // asset should still be there

        JsonNode assetNode = resource().path(orgAppPath + "/foos/" + uuid)
                .queryParam("access_token", access_token)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .get(JsonNode.class);

        Assert.assertEquals("image/jpeg", assetNode.findValue(AssetUtils.CONTENT_TYPE).textValue());
        Assert.assertEquals(7979, assetNode.findValue("content-length").intValue());
        JsonNode assetIdNode = assetNode.get("entities").get(0).get("uuid");
        assertEquals(uuid, assetIdNode.textValue());

        // asset data should still be there

        InputStream assetIs = resource().path(orgAppPath + "/foos/" + uuid)
                .queryParam("access_token", access_token)
                .accept(MediaType.APPLICATION_OCTET_STREAM_TYPE)
                .get(InputStream.class);

        byte[] foundData = IOUtils.toByteArray(assetIs);
        assertEquals(7979, foundData.length);
    }
}
