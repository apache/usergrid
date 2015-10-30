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


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.annotation.JSONP;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.entities.Asset;
import org.apache.usergrid.rest.AbstractContextResource;
import org.apache.usergrid.rest.ApiResponse;
import org.apache.usergrid.rest.applications.ServiceResource;
import org.apache.usergrid.rest.security.annotations.RequireApplicationAccess;
import org.apache.usergrid.services.assets.data.AssetUtils;
import org.apache.usergrid.services.assets.data.AwsSdkS3BinaryStore;
import org.apache.usergrid.services.assets.data.BinaryStore;
import org.apache.usergrid.services.assets.data.LocalFileBinaryStore;
import org.apache.usergrid.utils.StringUtils;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.io.InputStream;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_USERGRID_BINARY_UPLOADER;


/** @deprecated  */
@Component("org.apache.usergrid.rest.applications.assets.AssetsResource")
@Scope("prototype")
@Produces(MediaType.APPLICATION_JSON)
public class AssetsResource extends ServiceResource {

    private Logger logger = LoggerFactory.getLogger( AssetsResource.class );

    //@Autowired
    private BinaryStore binaryStore;

    @Autowired
    private LocalFileBinaryStore localFileBinaryStore;

    @Autowired
    private AwsSdkS3BinaryStore awsSdkS3BinaryStore;



    @Override
    @Path("{itemName}")
    public AbstractContextResource addNameParameter( @Context UriInfo ui, @PathParam("itemName") PathSegment itemName )
            throws Exception {
        logger.info( "in AssetsResource.addNameParameter" );
        super.addNameParameter( ui, itemName );
        logger.info( "serviceParamters now has: {}", getServiceParameters() );
        // HTF to work w/ the ../data endpoint when we are looking up by path?
        return getSubResource( AssetsResource.class );
    }


    @Override
    @RequireApplicationAccess
    @GET
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ApiResponse executeGet( @Context UriInfo ui,
                                       @QueryParam("callback") @DefaultValue("callback") String callback )
            throws Exception {
        logger.info( "In AssetsResource.executeGet with ui: {} and callback: {}", ui, callback );
        return super.executeGet( ui, callback );
    }


    @Override
    @PUT
    @RequireApplicationAccess
    @Consumes(MediaType.APPLICATION_JSON)
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ApiResponse executePut( @Context UriInfo ui, String body,
                                       @QueryParam("callback") @DefaultValue("callback") String callback )
            throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> json = mapper.readValue( body, mapTypeReference );

        return super.executePutWithMap( ui, json, callback );
    }


    @POST
    @RequireApplicationAccess
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("{entityId: [A-Fa-f0-9]{8}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{12}}/data")
    public Response uploadData( @FormDataParam("file") InputStream uploadedInputStream,
                                // @FormDataParam("file") FormDataContentDisposition fileDetail,
                                @PathParam("entityId") PathSegment entityId ) throws Exception {

        if(properties.getProperty( PROPERTIES_USERGRID_BINARY_UPLOADER ).equals( "local" )){
            this.binaryStore = localFileBinaryStore;
        }
        else{
            this.binaryStore = awsSdkS3BinaryStore;
        }

        if (uploadedInputStream != null ) {
    		UUID assetId = UUID.fromString( entityId.getPath() );
    		logger.info( "In AssetsResource.uploadData with id: {}", assetId );
    		EntityManager em = emf.getEntityManager( getApplicationId() );
    		Asset asset = em.get( assetId, Asset.class );

    		binaryStore.write( getApplicationId(), asset, uploadedInputStream );
    		em.update( asset );
    		return Response.status( 200 ).build();
    	} else {
    		return Response.status(Response.Status.BAD_REQUEST).entity("File Not Found").build();
    	}
    }


    @PUT
    @RequireApplicationAccess
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Path("{entityId: [A-Fa-f0-9]{8}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{12}}/data")
    public Response uploadDataStreamPut( @PathParam("entityId") PathSegment entityId, InputStream uploadedInputStream )
            throws Exception {
        return uploadDataStream( entityId, uploadedInputStream );
    }


    @POST
    @RequireApplicationAccess
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Path("{entityId: [A-Fa-f0-9]{8}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{12}}/data")
    public Response uploadDataStream( @PathParam("entityId") PathSegment entityId, InputStream uploadedInputStream )
            throws Exception {

        if(properties.getProperty( PROPERTIES_USERGRID_BINARY_UPLOADER ).equals( "local" )){
            this.binaryStore = localFileBinaryStore;
        }
        else{
            this.binaryStore = awsSdkS3BinaryStore;
        }

        UUID assetId = UUID.fromString( entityId.getPath() );
        logger.info( "In AssetsResource.uploadDataStream with id: {}", assetId );
        EntityManager em = emf.getEntityManager( getApplicationId() );
        Asset asset = em.get( assetId, Asset.class );

        binaryStore.write( getApplicationId(), asset, uploadedInputStream );
        logger.info( "uploadDataStream written, returning response" );
        em.update( asset );
        return Response.status( 200 ).build();
    }


    @GET
    @Path("{entityId: [A-Fa-f0-9]{8}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{12}}/data")
    public Response findAsset( @Context UriInfo ui, @QueryParam("callback") @DefaultValue("callback") String callback,
                               @PathParam("entityId") PathSegment entityId, @HeaderParam("range") String range,
                               @HeaderParam("if-modified-since") String modifiedSince ) throws Exception {
        if(properties.getProperty( PROPERTIES_USERGRID_BINARY_UPLOADER ).equals( "local" )){
            this.binaryStore = localFileBinaryStore;
        }
        else{
            this.binaryStore = awsSdkS3BinaryStore;
        }

        UUID assetId = UUID.fromString( entityId.getPath() );
        logger.info( "In AssetsResource.findAsset with id: {}, range: {}, modifiedSince: {}",
                new Object[] { assetId, range, modifiedSince } );
        EntityManager em = emf.getEntityManager( getApplicationId() );

        Asset asset = em.get( assetId, Asset.class );
        Map<String, Object> fileMetadata = AssetUtils.getFileMetadata( asset );

        // todo: use fileMetadata
        // return a 302 if not modified
        Date moded = AssetUtils.fromIfModifiedSince( modifiedSince );
        if ( moded != null ) {
            if ( asset.getModified() - moded.getTime() < 0 ) {
                return Response.status( Response.Status.NOT_MODIFIED ).build();
            }
        }

        InputStream is;
        if ( StringUtils.isBlank( range ) ) {
            is = binaryStore.read( getApplicationId(), asset );
        }
        else {
            // TODO range parser
            is = binaryStore.read( getApplicationId(), asset );
        }
        if ( is == null ) {
            return Response.status( Response.Status.NOT_FOUND ).build();
        }

        logger.info( "AssetResource.findAsset read inputStream, composing response" );
        Response.ResponseBuilder responseBuilder =
                Response.ok( is ).type( fileMetadata.get( "content-type" ).toString() )
                        .lastModified( new Date( asset.getModified() ) );
        if ( fileMetadata.get( AssetUtils.E_TAG ) != null ) {
            responseBuilder.tag( ( String ) fileMetadata.get( AssetUtils.E_TAG ) );
        }
        if ( StringUtils.isNotBlank( range ) ) {
            logger.info( "Range header was not blank, sending back Content-Range" );
            // TODO build content range header if needed
            //responseBuilder.header("Content-Range", );
        }
        return responseBuilder.build();
    }
}
