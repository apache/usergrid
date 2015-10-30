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


import net.jcip.annotations.NotThreadSafe;
import org.apache.commons.io.IOUtils;
import org.apache.usergrid.rest.test.resource.AbstractRestIT;
import org.apache.usergrid.rest.test.resource.model.ApiResponse;
import org.apache.usergrid.rest.test.resource.model.Entity;
import org.apache.usergrid.services.assets.data.AssetUtils;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.glassfish.jersey.media.multipart.file.StreamDataBodyPart;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_USERGRID_BINARY_UPLOADER;
import static org.apache.usergrid.utils.MapUtils.hashMap;
import static org.junit.Assert.*;

@NotThreadSafe
public class AssetResourceIT extends AbstractRestIT {

    private String access_token;
    private Logger LOG = LoggerFactory.getLogger( AssetResourceIT.class );
    private Map<String, Object> originalProperties;



    @Before
    public void setup(){
        originalProperties = getRemoteTestProperties();
        setTestProperty(PROPERTIES_USERGRID_BINARY_UPLOADER, "local");


        access_token = this.getAdminToken().getAccessToken();

    }

    @After
    public void teardown(){
        setTestProperties(originalProperties);
    }


    @Test
    public void octetStreamOnDynamicEntity() throws Exception {

        this.refreshIndex();

        //  post an asset entity

        Map<String, String> payload = hashMap( "name", "assetname" );
        ApiResponse postResponse = pathResource( getOrgAppPath( "foos" )).post( payload );
        UUID assetId = postResponse.getEntities().get(0).getUuid();
        assertNotNull(assetId);

        // post a binary asset to that entity

        byte[] data = IOUtils.toByteArray( getClass().getResourceAsStream( "/cassandra_eye.jpg" ) );
        ApiResponse putResponse = pathResource( getOrgAppPath("foos/" + assetId) )
            .put( data, MediaType.APPLICATION_OCTET_STREAM_TYPE );

        // check that the asset entity has asset metadata

        ApiResponse getResponse = pathResource( getOrgAppPath( "foos/" + assetId) ).get( ApiResponse.class );
        Entity entity = getResponse.getEntities().get(0);
        Map<String, Object> fileMetadata = (Map<String, Object>)entity.get("file-metadata");
        Assert.assertEquals( "image/jpeg", fileMetadata.get( "content-type" ) );
        Assert.assertEquals( 7979,         fileMetadata.get( "content-length" ));
        assertEquals( assetId, entity.getUuid() );

        // get binary asset by UUID

        InputStream is = pathResource( getOrgAppPath("foos/" + assetId) ).getAssetAsStream();
        byte[] foundData = IOUtils.toByteArray( is );
        assertEquals( 7979, foundData.length );

        // get binary asset by name

        is = pathResource( getOrgAppPath("foos/assetname") ).getAssetAsStream();
        foundData = IOUtils.toByteArray( is );
        assertEquals( 7979, foundData.length );
    }


    @Test
    public void multipartPostFormOnDynamicEntity() throws Exception {

        this.refreshIndex();

        // post data larger than 5M

        byte[] data = IOUtils.toByteArray( this.getClass().getResourceAsStream( "/file-bigger-than-5M" ) );
        FormDataMultiPart form = new FormDataMultiPart().field( "file", data, MediaType.MULTIPART_FORM_DATA_TYPE );
        ApiResponse putResponse = pathResource(getOrgAppPath("foos")).post(form);
        this.refreshIndex();

        UUID assetId = putResponse.getEntities().get(0).getUuid();
        assertNotNull(assetId);

        // retry until upload complete and we can get the data

        int retries = 0;
        boolean done = false;
        byte[] foundData = new byte[0];
        while ( !done && retries < 30 ) {

            try {
                InputStream is = pathResource( getOrgAppPath( "foos/" + assetId ) ).getAssetAsStream();
                foundData = IOUtils.toByteArray( is );
                done = true;

            } catch ( Exception intentiallyIgnored ) {}

            Thread.sleep(1000);
            retries++;
        }

        //  did we get expected number of bytes of data?

        assertEquals( 5324800, foundData.length );

        pathResource( getOrgAppPath( "foos/" + assetId ) ).delete();
    }


    @Test
    public void multipartPutFormOnDynamicEntity() throws Exception {

        this.refreshIndex();

        // post an entity

        Map<String, String> payload = hashMap( "foo", "bar" );
        ApiResponse postResponse = pathResource( getOrgAppPath( "foos" ) ).post( payload );
        UUID assetId = postResponse.getEntities().get(0).getUuid();
        assertNotNull( assetId );

        // post asset to that entity

        byte[] data = IOUtils.toByteArray( this.getClass().getResourceAsStream( "/cassandra_eye.jpg" ) );
        FormDataMultiPart form = new FormDataMultiPart()
            .field( "foo", "bar2" )
            .field( "file", data, MediaType.MULTIPART_FORM_DATA_TYPE );
        ApiResponse putResponse = pathResource( getOrgAppPath( "foos/" + assetId ) ).put( form );
        this.refreshIndex();

        // get entity and check asset metadata

        ApiResponse getResponse = pathResource( getOrgAppPath( "foos/" + assetId ) ).get( ApiResponse.class );
        Entity entity = getResponse.getEntities().get( 0 );
        Map<String, Object> fileMetadata = (Map<String, Object>)entity.get("file-metadata");
        long lastModified = Long.parseLong( fileMetadata.get( AssetUtils.LAST_MODIFIED ).toString() );

        assertEquals( assetId,      entity.getUuid() );
        assertEquals( "bar2",       entity.get("foo") );
        assertEquals( "image/jpeg", fileMetadata.get( AssetUtils.CONTENT_TYPE ) );
        assertEquals( 7979,         fileMetadata.get( AssetUtils.CONTENT_LENGTH ));

        // get asset and check size

        InputStream is = pathResource( getOrgAppPath( "foos/" + assetId ) ).getAssetAsStream();
        byte[] foundData = IOUtils.toByteArray( is );
        assertEquals( 7979, foundData.length );

        // upload new asset to entity, then check that it was updated

        ApiResponse putResponse2 = pathResource( getOrgAppPath( "foos/" + assetId ) ).put( form );
        entity = putResponse2.getEntities().get( 0 );
        fileMetadata = (Map<String, Object>)entity.get("file-metadata");
        long justModified = Long.parseLong( fileMetadata.get( AssetUtils.LAST_MODIFIED ).toString() );
        assertNotEquals( lastModified, justModified );
    }


    @Test
    public void largeFileInS3() throws Exception {

        this.refreshIndex();

        // upload file larger than 5MB

        byte[] data = IOUtils.toByteArray( this.getClass().getResourceAsStream( "/file-bigger-than-5M" ) );
        FormDataMultiPart form = new FormDataMultiPart().field( "file", data, MediaType.MULTIPART_FORM_DATA_TYPE );
        ApiResponse postResponse = pathResource( getOrgAppPath( "foos" ) ).post( form );
        UUID assetId = postResponse.getEntities().get(0).getUuid();
        LOG.info( "Waiting for upload to finish..." );
        Thread.sleep( 2000 );

        // check that entire file was uploaded

        ApiResponse getResponse = pathResource( getOrgAppPath( "foos/" +assetId ) ).get( ApiResponse.class );
        LOG.info( "Upload complete!" );
        InputStream is = pathResource( getOrgAppPath( "foos/" + assetId ) ).getAssetAsStream();
        byte[] foundData = IOUtils.toByteArray( is );
        assertEquals( data.length, foundData.length );

        // delete file

        pathResource( getOrgAppPath( "foos/" + assetId ) ).delete();
    }

    @Test
    public void fileTooLargeShouldResultInError() throws Exception {

        this.refreshIndex();

        // set max file size down to 6mb

        Map<String, String> props = new HashMap<String, String>();
        props.put( "usergrid.binary.max-size-mb", "6" );
        pathResource( "testproperties" ).post( props );

        try {

            // upload a file larger than 6mb

            final StreamDataBodyPart part = new StreamDataBodyPart(
                "file", getClass().getResourceAsStream( "/ship-larger-than-6mb.gif" ), "ship");
            final MultiPart multipart = new FormDataMultiPart().bodyPart( part );

            ApiResponse postResponse = pathResource( getOrgAppPath( "bars" ) ).post( multipart );
            UUID assetId = postResponse.getEntities().get(0).getUuid();

            String errorMessage = null;
            LOG.info( "Waiting for upload to finish..." );
            Thread.sleep( 2000 );

            // attempt to get asset entity, it should contain error

            ApiResponse getResponse = pathResource( getOrgAppPath( "bars/" +assetId ) ).get( ApiResponse.class );
            Map<String, Object> fileMetadata = (Map<String, Object>)getResponse.getEntities().get(0).get("file-metadata");
            assertTrue( fileMetadata.get( "error" ).toString().startsWith( "Asset size " ) );

        } finally {

            // set max upload size back to default 25mb

            props.put( "usergrid.binary.max-size-mb", "25" );
            pathResource( "testproperties" ).post( props );
        }
    }

    /**
     * Deleting a connection to an asset should not delete the asset or the asset's data
     */
    @Test
    public void deleteConnectionToAsset() throws IOException {

        this.refreshIndex();

        // create the entity that will be the asset, an image

        Map<String, String> payload = hashMap("name", "cassandra_eye.jpg");
        ApiResponse postReponse = pathResource( getOrgAppPath( "foos" ) ).post( payload );
        final UUID uuid = postReponse.getEntities().get(0).getUuid();

        // post image data to the asset entity

        byte[] data = IOUtils.toByteArray(this.getClass().getResourceAsStream("/cassandra_eye.jpg"));
        pathResource( getOrgAppPath( "foos/" + uuid ) ).put( data, MediaType.APPLICATION_OCTET_STREAM_TYPE );

        // create an imagegallery entity

        Map<String, String> imageGalleryPayload = hashMap("name", "my image gallery");

        ApiResponse postResponse2 = pathResource( getOrgAppPath( "imagegalleries" ) ).post( imageGalleryPayload );
        UUID imageGalleryId = postResponse2.getEntities().get(0).getUuid();

        // connect imagegallery to asset

        ApiResponse connectResponse = pathResource(
            getOrgAppPath( "imagegalleries/" + imageGalleryId + "/contains/" + uuid ) ).post( ApiResponse.class );
        this.refreshIndex();

        // verify connection from imagegallery to asset

        ApiResponse containsResponse = pathResource(
            getOrgAppPath( "imagegalleries/" + imageGalleryId + "/contains/" ) ).get( ApiResponse.class );
        assertEquals( uuid, containsResponse.getEntities().get(0).getUuid() );

        // delete the connection

        pathResource( getOrgAppPath( "imagegalleries/" + imageGalleryId + "/contains/" + uuid ) ).delete();
        this.refreshIndex();

        // verify that connection is gone

        ApiResponse listResponse = pathResource(
            getOrgAppPath( "imagegalleries/" + imageGalleryId + "/contains/" )).get( ApiResponse.class );
        assertEquals( 0, listResponse.getEntityCount() );

        // asset should still be there

        ApiResponse getResponse2 = pathResource( getOrgAppPath( "foos/" + uuid ) ).get( ApiResponse.class );
        Entity entity = getResponse2.getEntities().get(0);
        Map<String, Object> fileMetadata = (Map<String, Object>)entity.get("file-metadata");

        Assert.assertEquals("image/jpeg", fileMetadata.get( AssetUtils.CONTENT_TYPE ));
        Assert.assertEquals(7979, fileMetadata.get( AssetUtils.CONTENT_LENGTH ));
        assertEquals(uuid, entity.getUuid());

        // asset data should still be there

        InputStream assetIs = pathResource( getOrgAppPath( "foos/" + uuid ) ).getAssetAsStream();
        byte[] foundData = IOUtils.toByteArray(assetIs);
        assertEquals(7979, foundData.length);
    }
}
