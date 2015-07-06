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


import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import javax.ws.rs.core.MediaType;

import org.codehaus.jackson.JsonNode;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.usergrid.cassandra.Concurrent;
import org.apache.usergrid.rest.AbstractRestIT;
import org.apache.usergrid.rest.applications.utils.UserRepo;
import org.apache.usergrid.services.assets.data.AssetUtils;

import org.apache.commons.io.IOUtils;

import com.sun.jersey.multipart.FormDataMultiPart;

import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_ADMIN_USERS_REQUIRE_CONFIRMATION;
import static org.apache.usergrid.utils.MapUtils.hashMap;
import static org.junit.Assert.*;


@Concurrent()
public class AssetResourceIT extends AbstractRestIT {

    private Logger LOG = LoggerFactory.getLogger( AssetResourceIT.class );


    /** @Deprecated Tests legacy API */
    @Test
    public void verifyBinaryCrud() throws Exception {
        UserRepo.INSTANCE.load( resource(), access_token );

        UUID userId = UserRepo.INSTANCE.getByUserName( "user1" );
        Map<String, String> payload =
                hashMap( "path", "my/clean/path" ).map( "owner", userId.toString() ).map( "someprop", "somevalue" );

        JsonNode node =
                resource().path( "/test-organization/test-app/assets" ).queryParam( "access_token", access_token )
                        .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE )
                        .post( JsonNode.class, payload );
        JsonNode idNode = node.get( "entities" ).get( 0 ).get( "uuid" );
        UUID id = UUID.fromString( idNode.getTextValue() );
        assertNotNull( idNode.getTextValue() );
        logNode( node );

        byte[] data = IOUtils.toByteArray( this.getClass().getResourceAsStream( "/cassandra_eye.jpg" ) );
        resource().path( "/test-organization/test-app/assets/" + id.toString() + "/data" )
                .queryParam( "access_token", access_token ).type( MediaType.APPLICATION_OCTET_STREAM_TYPE ).put( data );

        InputStream is = resource().path( "/test-organization/test-app/assets/" + id.toString() + "/data" )
                .queryParam( "access_token", access_token ).get( InputStream.class );

        byte[] foundData = IOUtils.toByteArray( is );
        assertEquals( 7979, foundData.length );

        node = resource().path( "/test-organization/test-app/assets/my/clean/path" )
                .queryParam( "access_token", access_token ).accept( MediaType.APPLICATION_JSON_TYPE )
                .get( JsonNode.class );

        idNode = node.get( "entities" ).get( 0 ).get( "uuid" );
        assertEquals( id.toString(), idNode.getTextValue() );
    }


    @Test
    public void octetStreamOnDynamicEntity() throws Exception {
        UserRepo.INSTANCE.load( resource(), access_token );

        Map<String, String> payload = hashMap( "name", "assetname" );

        JsonNode node = resource().path( "/test-organization/test-app/foos" ).queryParam( "access_token", access_token )
                .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE )
                .post( JsonNode.class, payload );

        JsonNode idNode = node.get( "entities" ).get( 0 ).get( "uuid" );
        String uuid = idNode.getTextValue();
        assertNotNull( uuid );
        logNode( node );

        byte[] data = IOUtils.toByteArray( this.getClass().getResourceAsStream( "/cassandra_eye.jpg" ) );
        resource().path( "/test-organization/test-app/foos/" + uuid ).queryParam( "access_token", access_token )
                .type( MediaType.APPLICATION_OCTET_STREAM_TYPE ).put( data );

        // get entity
        node = resource().path( "/test-organization/test-app/foos/" + uuid ).queryParam( "access_token", access_token )
                .accept( MediaType.APPLICATION_JSON_TYPE ).get( JsonNode.class );
        logNode( node );
        Assert.assertEquals( "image/jpeg", node.findValue( AssetUtils.CONTENT_TYPE ).getTextValue() );
        Assert.assertEquals( 7979, node.findValue( "content-length" ).getIntValue() );
        idNode = node.get( "entities" ).get( 0 ).get( "uuid" );
        assertEquals( uuid, idNode.getTextValue() );

        // get data by UUID
        InputStream is =
                resource().path( "/test-organization/test-app/foos/" + uuid ).queryParam( "access_token", access_token )
                        .accept( MediaType.APPLICATION_OCTET_STREAM_TYPE ).get( InputStream.class );

        byte[] foundData = IOUtils.toByteArray( is );
        assertEquals( 7979, foundData.length );

        // get data by name
        is = resource().path( "/test-organization/test-app/foos/assetname" ).queryParam( "access_token", access_token )
                .accept( MediaType.APPLICATION_OCTET_STREAM_TYPE ).get( InputStream.class );

        foundData = IOUtils.toByteArray( is );
        assertEquals( 7979, foundData.length );
    }


    @Test
    public void multipartPostFormOnDynamicEntity() throws Exception {
        UserRepo.INSTANCE.load( resource(), access_token );

        byte[] data = IOUtils.toByteArray( this.getClass().getResourceAsStream( "/file-bigger-than-5M" ) );

        FormDataMultiPart form = new FormDataMultiPart()
                .field( "file", data, MediaType.MULTIPART_FORM_DATA_TYPE );

        JsonNode node = resource().path( "/test-organization/test-app/foos" )
                .queryParam( "access_token", access_token )
                .accept( MediaType.APPLICATION_JSON )
                .type( MediaType.MULTIPART_FORM_DATA )
                .post( JsonNode.class, form );

        JsonNode idNode = node.get( "entities" ).get( 0 ).get( "uuid" );
        String uuid = idNode.getTextValue();
        assertNotNull( uuid );
        logNode( node );

        // get entity
        node = resource().path( "/test-organization/test-app/foos/" + uuid )
                .queryParam( "access_token", access_token )
                .accept( MediaType.APPLICATION_JSON_TYPE )
                .get( JsonNode.class );

        logNode( node );
        assertEquals( "application/octet-stream", node.findValue( AssetUtils.CONTENT_TYPE ).getTextValue() );
        assertEquals( 5324800, node.findValue( AssetUtils.CONTENT_LENGTH ).getIntValue() );
        idNode = node.get( "entities" ).get( 0 ).get( "uuid" );
        assertEquals( uuid, idNode.getTextValue() );

        int retries = 0;
        boolean done = false;
        byte[] foundData = new byte[0];

        // retry until upload complete
        while ( !done && retries < 30 ) {

            // get data
            try {
                InputStream is = resource().path( "/test-organization/test-app/foos/" + uuid )
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
        node = resource().path( "/test-organization/test-app/foos/" + uuid )
                .queryParam( "access_token", access_token )
                .accept( MediaType.APPLICATION_JSON_TYPE )
                .delete( JsonNode.class );
    }


    @Test
    public void multipartPutFormOnDynamicEntity() throws Exception {
        UserRepo.INSTANCE.load( resource(), access_token );

        Map<String, String> payload = hashMap( "foo", "bar" );

        JsonNode node = resource().path( "/test-organization/test-app/foos" ).queryParam( "access_token", access_token )
                .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE )
                .post( JsonNode.class, payload );

        JsonNode idNode = node.get( "entities" ).get( 0 ).get( "uuid" );
        String uuid = idNode.getTextValue();
        assertNotNull( uuid );
        logNode( node );

        // set file & assetname
        byte[] data = IOUtils.toByteArray( this.getClass().getResourceAsStream( "/cassandra_eye.jpg" ) );
        FormDataMultiPart form = new FormDataMultiPart().field( "foo", "bar2" )
                                                        .field( "file", data, MediaType.MULTIPART_FORM_DATA_TYPE );

        long created = System.currentTimeMillis();
        node = resource().path( "/test-organization/test-app/foos/" + uuid ).queryParam( "access_token", access_token )
                .accept( MediaType.APPLICATION_JSON ).type( MediaType.MULTIPART_FORM_DATA ).put( JsonNode.class, form );
        logNode( node );

        // get entity
        node = resource().path( "/test-organization/test-app/foos/" + uuid ).queryParam( "access_token", access_token )
                .accept( MediaType.APPLICATION_JSON_TYPE ).get( JsonNode.class );
        logNode( node );
        assertEquals( "image/jpeg", node.findValue( AssetUtils.CONTENT_TYPE ).getTextValue() );
        assertEquals( 7979, node.findValue( AssetUtils.CONTENT_LENGTH ).getIntValue() );
        idNode = node.get( "entities" ).get( 0 ).get( "uuid" );
        assertEquals( uuid, idNode.getTextValue() );
        JsonNode nameNode = node.get( "entities" ).get( 0 ).get( "foo" );
        assertEquals( "bar2", nameNode.getTextValue() );
        long lastModified = node.findValue( AssetUtils.LAST_MODIFIED ).getLongValue();
        Assert.assertEquals( created, lastModified, 500 );

        // get data
        InputStream is =
                resource().path( "/test-organization/test-app/foos/" + uuid ).queryParam( "access_token", access_token )
                        .accept( "image/jpeg" ).get( InputStream.class );

        byte[] foundData = IOUtils.toByteArray( is );
        assertEquals( 7979, foundData.length );

        // post new data
        node = resource().path( "/test-organization/test-app/foos/" + uuid ).queryParam( "access_token", access_token )
                .accept( MediaType.APPLICATION_JSON ).type( MediaType.MULTIPART_FORM_DATA ).put( JsonNode.class, form );
        logNode( node );
        assertTrue( lastModified != node.findValue( AssetUtils.LAST_MODIFIED ).getLongValue() );
    }


    @Test
    public void largeFileInS3() throws Exception {
        UserRepo.INSTANCE.load( resource(), access_token );

        byte[] data = IOUtils.toByteArray( this.getClass().getResourceAsStream( "/file-bigger-than-5M" ) );
        FormDataMultiPart form = new FormDataMultiPart().field( "file", data, MediaType.MULTIPART_FORM_DATA_TYPE );

        // send data
        JsonNode node = resource().path( "/test-organization/test-app/foos" ).queryParam( "access_token", access_token )
                .accept( MediaType.APPLICATION_JSON ).type( MediaType.MULTIPART_FORM_DATA )
                .post( JsonNode.class, form );
        logNode( node );
        JsonNode idNode = node.get( "entities" ).get( 0 ).get( "uuid" );
        String uuid = idNode.getTextValue();

        // get entity
        long timeout = System.currentTimeMillis() + 60000;
        while ( true ) {
            LOG.info( "Waiting for upload to finish..." );
            Thread.sleep( 2000 );
            node = resource().path( "/test-organization/test-app/foos/" + uuid )
                    .queryParam( "access_token", access_token ).accept( MediaType.APPLICATION_JSON_TYPE )
                    .get( JsonNode.class );
            logNode( node );

            // poll for the upload to complete
            if ( node.findValue( AssetUtils.E_TAG ) != null ) {
                break;
            }
            if ( System.currentTimeMillis() > timeout ) {
                throw new TimeoutException();
            }
        }
        LOG.info( "Upload complete!" );

        // get data
        InputStream is =
                resource().path( "/test-organization/test-app/foos/" + uuid ).queryParam( "access_token", access_token )
                        .accept( MediaType.APPLICATION_OCTET_STREAM_TYPE ).get( InputStream.class );

        byte[] foundData = IOUtils.toByteArray( is );
        assertEquals( 5324800, foundData.length );

        // delete
        node = resource().path( "/test-organization/test-app/foos/" + uuid ).queryParam( "access_token", access_token )
                .accept( MediaType.APPLICATION_JSON_TYPE ).delete( JsonNode.class );
    }

    @Test
    public void fileTooLargeShouldResultInError() throws Exception {

        Map<String, String> props = new HashMap<String, String>();
        props.put( "usergrid.binary.max-size-mb", "6" );
        resource().path( "/testproperties" )
                .queryParam( "access_token", access_token )
                .accept( MediaType.APPLICATION_JSON )
                .type( MediaType.APPLICATION_JSON_TYPE ).post( props );

        try {

            UserRepo.INSTANCE.load( resource(), access_token );

            byte[] data = IOUtils.toByteArray( this.getClass().getResourceAsStream( "/ship-larger-than-6mb.gif" ) );
            FormDataMultiPart form = new FormDataMultiPart().field( "file", data, MediaType.MULTIPART_FORM_DATA_TYPE );

            // send data
            JsonNode node = resource().path( "/test-organization/test-app/bars" ).queryParam( "access_token", access_token )
                    .accept( MediaType.APPLICATION_JSON ).type( MediaType.MULTIPART_FORM_DATA )
                    .post( JsonNode.class, form );
            //logNode( node );
            JsonNode idNode = node.get( "entities" ).get( 0 ).get( "uuid" );
            String uuid = idNode.getTextValue();

            // get entity
            String errorMessage = null;
            long timeout = System.currentTimeMillis() + 60000;
            while (true) {
                LOG.info( "Waiting for upload to finish..." );
                Thread.sleep( 2000 );
                node = resource().path( "/test-organization/test-app/bars/" + uuid )
                        .queryParam( "access_token", access_token ).accept( MediaType.APPLICATION_JSON_TYPE )
                        .get( JsonNode.class );
                //logNode( node );

                // poll for the error to happen
                if (node.findValue( "error" ) != null) {
                    errorMessage = node.findValue("error").asText();
                    break;
                }
                if (System.currentTimeMillis() > timeout) {
                    throw new TimeoutException();
                }
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
    public void deleteConnectionToAsset() throws IOException {

        UserRepo.INSTANCE.load( resource(), access_token );

        final String uuid;

        // create the entity that will be the asset, an image

        Map<String, String> payload = hashMap("name", "cassandra_eye.jpg");

        JsonNode node = resource().path("/test-organization/test-app/foos")
                .queryParam("access_token", access_token)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .post(JsonNode.class, payload);
        JsonNode idNode = node.get("entities").get(0).get("uuid");
        uuid = idNode.getTextValue();

        // post image data to the asset entity

        byte[] data = IOUtils.toByteArray(this.getClass().getResourceAsStream("/cassandra_eye.jpg"));
        resource().path("/test-organization/test-app/foos/" + uuid)
                .queryParam("access_token", access_token)
                .type(MediaType.APPLICATION_OCTET_STREAM_TYPE)
                .put(data);

        // create an imagegallery entity

        Map<String, String> imageGalleryPayload = hashMap("name", "my image gallery");

        JsonNode imageGalleryNode = resource().path("/test-organization/test-app/imagegalleries")
                .queryParam("access_token", access_token)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .post(JsonNode.class, imageGalleryPayload);

        JsonNode imageGalleryIdNode = imageGalleryNode.get("entities").get(0).get("uuid");
        String imageGalleryId = imageGalleryIdNode.getTextValue();

        // connect imagegallery to asset

        JsonNode connectNode = resource()
                .path("/test-organization/test-app/imagegalleries/" + imageGalleryId + "/contains/" + uuid)
                .queryParam("access_token", access_token)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .post(JsonNode.class);

        // verify connection from imagegallery to asset

        JsonNode listConnectionsNode = resource()
                .path("/test-organization/test-app/imagegalleries/" + imageGalleryId + "/contains/")
                .queryParam("access_token", access_token)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .get(JsonNode.class);
        assertEquals(uuid, listConnectionsNode.get("entities").get(0).get("uuid").getTextValue());

        // delete the connection

        resource().path("/test-organization/test-app/imagegalleries/" + imageGalleryId + "/contains/" + uuid)
                .queryParam("access_token", access_token)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .delete();

        // verify that connection is gone

        listConnectionsNode = resource()
                .path("/test-organization/test-app/imagegalleries/" + imageGalleryId + "/contains/")
                .queryParam("access_token", access_token)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .get(JsonNode.class);
        assertFalse(listConnectionsNode.get("entities").getElements().hasNext());

        // asset should still be there

        JsonNode assetNode = resource().path("/test-organization/test-app/foos/" + uuid)
                .queryParam("access_token", access_token)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .get(JsonNode.class);

        logNode(assetNode);
        Assert.assertEquals("image/jpeg", assetNode.findValue(AssetUtils.CONTENT_TYPE).getTextValue());
        Assert.assertEquals(7979, assetNode.findValue("content-length").getIntValue());
        JsonNode assetIdNode = assetNode.get("entities").get(0).get("uuid");
        assertEquals(uuid, assetIdNode.getTextValue());

        // asset data should still be there

        InputStream assetIs = resource().path("/test-organization/test-app/foos/" + uuid)
                .queryParam("access_token", access_token)
                .accept(MediaType.APPLICATION_OCTET_STREAM_TYPE)
                .get(InputStream.class);

        byte[] foundData = IOUtils.toByteArray(assetIs);
        assertEquals(7979, foundData.length);
    }
}
