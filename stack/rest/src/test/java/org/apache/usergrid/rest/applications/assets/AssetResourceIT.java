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


import java.io.InputStream;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.usergrid.rest.test.resource2point0.AbstractRestIT;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.rest.applications.utils.UserRepo;
import org.apache.usergrid.services.assets.data.AssetUtils;

import org.apache.commons.io.IOUtils;

import com.sun.jersey.multipart.FormDataMultiPart;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.apache.usergrid.utils.MapUtils.hashMap;



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
    public void verifyBinaryCrud() throws Exception {

        UUID userId = userRepo.getByUserName( "user1" );
        Map<String, String> payload =
                hashMap( "path", "my/clean/path" ).map( "owner", userId.toString() ).map( "someprop", "somevalue" );

        JsonNode node =
                mapper.readTree( resource().path( "/test-organization/test-app/assets" ).queryParam( "access_token", access_token )
                        .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE )
                        .post( String.class, payload ));
        JsonNode idNode = node.get( "entities" ).get( 0 ).get( "uuid" );
        UUID id = UUID.fromString( idNode.textValue() );
        assertNotNull(idNode.textValue());

        byte[] data = IOUtils.toByteArray( this.getClass().getResourceAsStream( "/cassandra_eye.jpg" ) );
        resource().path( "/test-organization/test-app/assets/" + id.toString() + "/data" )
                .queryParam( "access_token", access_token ).type( MediaType.APPLICATION_OCTET_STREAM_TYPE ).put( data );

        InputStream is = resource().path( "/test-organization/test-app/assets/" + id.toString() + "/data" )
                .queryParam( "access_token", access_token ).get( InputStream.class );

        byte[] foundData = IOUtils.toByteArray( is );
        assertEquals( 7979, foundData.length );

        refreshIndex();

        node = mapper.readTree( resource().path( "/test-organization/test-app/assets/my/clean/path" )
                .queryParam( "access_token", access_token ).accept( MediaType.APPLICATION_JSON_TYPE )
                .get( String.class ));

        idNode = node.get( "entities" ).get( 0 ).get( "uuid" );
        assertEquals( id.toString(), idNode.textValue() );
    }


    @Test
    public void octetStreamOnDynamicEntity() throws Exception {

        Map<String, String> payload = hashMap( "name", "assetname" );

        JsonNode node = mapper.readTree( resource().path( "/test-organization/test-app/foos" ).queryParam( "access_token", access_token )
                .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE )
                .post( String.class, payload ));

        JsonNode idNode = node.get( "entities" ).get( 0 ).get( "uuid" );
        String uuid = idNode.textValue();
        assertNotNull(uuid);

        byte[] data = IOUtils.toByteArray( this.getClass().getResourceAsStream( "/cassandra_eye.jpg" ) );
        resource().path( "/test-organization/test-app/foos/" + uuid ).queryParam( "access_token", access_token )
                .type( MediaType.APPLICATION_OCTET_STREAM_TYPE ).put( data );

        // get entity
        node = mapper.readTree( resource().path( "/test-organization/test-app/foos/" + uuid ).queryParam( "access_token", access_token )
                .accept( MediaType.APPLICATION_JSON_TYPE ).get( String.class ));
        Assert.assertEquals( "image/jpeg", node.findValue( AssetUtils.CONTENT_TYPE ).textValue() );
        Assert.assertEquals( 7979, node.findValue( "content-length" ).intValue() );
        idNode = node.get( "entities" ).get( 0 ).get( "uuid" );
        assertEquals( uuid, idNode.textValue() );

        // get data by UUID
        InputStream is =
                resource().path( "/test-organization/test-app/foos/" + uuid ).queryParam( "access_token", access_token )
                        .accept( MediaType.APPLICATION_OCTET_STREAM_TYPE ).get( InputStream.class );

        byte[] foundData = IOUtils.toByteArray( is );
        assertEquals( 7979, foundData.length );

        refreshIndex();

        // get data by name
        is = resource().path( "/test-organization/test-app/foos/assetname" ).queryParam( "access_token", access_token )
                .accept( MediaType.APPLICATION_OCTET_STREAM_TYPE ).get( InputStream.class );

        foundData = IOUtils.toByteArray( is );
        assertEquals( 7979, foundData.length );
    }


    @Test
    public void multipartPostFormOnDynamicEntity() throws Exception {

        byte[] data = IOUtils.toByteArray( this.getClass().getResourceAsStream( "/file-bigger-than-5M" ) );
        FormDataMultiPart form = new FormDataMultiPart().field( "file", data, MediaType.MULTIPART_FORM_DATA_TYPE );

        JsonNode node = mapper.readTree( resource().path( "/test-organization/test-app/foos" ).queryParam( "access_token", access_token )
                .accept( MediaType.APPLICATION_JSON ).type( MediaType.MULTIPART_FORM_DATA )
                .post( String.class, form ));

        JsonNode idNode = node.get( "entities" ).get( 0 ).get( "uuid" );
        String uuid = idNode.textValue();
        assertNotNull(uuid);

        // get entity
        node = mapper.readTree( resource().path( "/test-organization/test-app/foos/" + uuid ).queryParam( "access_token", access_token )
                .accept( MediaType.APPLICATION_JSON_TYPE ).get( String.class ));
        assertEquals( "application/octet-stream", node.findValue( AssetUtils.CONTENT_TYPE ).textValue() );
        assertEquals( 5324800, node.findValue( AssetUtils.CONTENT_LENGTH ).intValue() );
        idNode = node.get( "entities" ).get( 0 ).get( "uuid" );
        assertEquals( uuid, idNode.textValue() );

        // get data
        InputStream is =
                resource().path( "/test-organization/test-app/foos/" + uuid ).queryParam( "access_token", access_token )
                        .accept( MediaType.APPLICATION_OCTET_STREAM_TYPE ).get( InputStream.class );

        byte[] foundData = IOUtils.toByteArray( is );
        assertEquals( 5324800, foundData.length );

        // delete
        node = mapper.readTree( resource().path( "/test-organization/test-app/foos/" + uuid ).queryParam( "access_token", access_token )
                .accept( MediaType.APPLICATION_JSON_TYPE ).delete( String.class ));
    }


    @Test
    public void multipartPutFormOnDynamicEntity() throws Exception {

        Map<String, String> payload = hashMap( "foo", "bar" );

        JsonNode node = mapper.readTree( resource().path( "/test-organization/test-app/foos" ).queryParam( "access_token", access_token )
                .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE )
                .post( String.class, payload ));

        JsonNode idNode = node.get( "entities" ).get( 0 ).get( "uuid" );
        String uuid = idNode.textValue();
        assertNotNull(uuid);

        // set file & assetname
        byte[] data = IOUtils.toByteArray( this.getClass().getResourceAsStream( "/cassandra_eye.jpg" ) );
        FormDataMultiPart form = new FormDataMultiPart().field( "foo", "bar2" )
                                                        .field( "file", data, MediaType.MULTIPART_FORM_DATA_TYPE );

        long created = System.currentTimeMillis();
        node = mapper.readTree( resource().path( "/test-organization/test-app/foos/" + uuid ).queryParam( "access_token", access_token )
                .accept( MediaType.APPLICATION_JSON ).type( MediaType.MULTIPART_FORM_DATA ).put( String.class, form ));

        // get entity
        node = mapper.readTree( resource().path( "/test-organization/test-app/foos/" + uuid ).queryParam( "access_token", access_token )
                .accept( MediaType.APPLICATION_JSON_TYPE ).get( String.class ));
        assertEquals( "image/jpeg", node.findValue( AssetUtils.CONTENT_TYPE ).textValue() );
        assertEquals( 7979, node.findValue( AssetUtils.CONTENT_LENGTH ).intValue() );
        idNode = node.get( "entities" ).get( 0 ).get( "uuid" );
        assertEquals( uuid, idNode.textValue() );
        JsonNode nameNode = node.get( "entities" ).get( 0 ).get( "foo" );
        assertEquals( "bar2", nameNode.textValue() );
        long lastModified = node.findValue( AssetUtils.LAST_MODIFIED ).longValue();
        Assert.assertEquals( created, lastModified, 500 );

        // get data
        InputStream is =
                resource().path( "/test-organization/test-app/foos/" + uuid ).queryParam( "access_token", access_token )
                        .accept( "image/jpeg" ).get( InputStream.class );

        byte[] foundData = IOUtils.toByteArray( is );
        assertEquals( 7979, foundData.length );

        // post new data
        node = mapper.readTree( resource().path( "/test-organization/test-app/foos/" + uuid ).queryParam( "access_token", access_token )
                .accept( MediaType.APPLICATION_JSON ).type( MediaType.MULTIPART_FORM_DATA ).put( String.class, form ));
        Assert.assertTrue( lastModified != node.findValue( AssetUtils.LAST_MODIFIED ).longValue() );
    }


    @Test
    @Ignore("Just enable and run when testing S3 large file upload specifically")
    public void largeFileInS3() throws Exception {

        byte[] data = IOUtils.toByteArray( this.getClass().getResourceAsStream( "/file-bigger-than-5M" ) );
        FormDataMultiPart form = new FormDataMultiPart().field( "file", data, MediaType.MULTIPART_FORM_DATA_TYPE );

        // send data
        JsonNode node = mapper.readTree( resource().path( "/test-organization/test-app/foos" ).queryParam( "access_token", access_token )
                .accept( MediaType.APPLICATION_JSON ).type( MediaType.MULTIPART_FORM_DATA )
                .post( String.class, form ));
        JsonNode idNode = node.get( "entities" ).get( 0 ).get( "uuid" );
        String uuid = idNode.textValue();

        // get entity
        long timeout = System.currentTimeMillis() + 60000;
        while ( true ) {
            LOG.info("Waiting for upload to finish...");
            Thread.sleep( 2000 );
            node = mapper.readTree( resource().path( "/test-organization/test-app/foos/" + uuid )
                    .queryParam( "access_token", access_token ).accept( MediaType.APPLICATION_JSON_TYPE )
                    .get( String.class ));

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
        node = mapper.readTree( resource().path( "/test-organization/test-app/foos/" + uuid ).queryParam( "access_token", access_token )
                .accept( MediaType.APPLICATION_JSON_TYPE ).delete( String.class ));
    }
}
