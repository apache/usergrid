package org.usergrid.rest.applications.assets;


import java.io.InputStream;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.usergrid.persistence.EntityManager;
import org.usergrid.persistence.entities.Asset;
import org.usergrid.rest.AbstractContextResource;
import org.usergrid.rest.applications.ServiceResource;
import org.usergrid.rest.security.annotations.RequireApplicationAccess;
import org.usergrid.services.assets.data.AssetUtils;
import org.usergrid.services.assets.data.BinaryStore;
import org.usergrid.utils.StringUtils;

import com.sun.jersey.api.json.JSONWithPadding;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;


/** @deprecated  */
@Component("org.usergrid.rest.applications.assets.AssetsResource")
@Scope("prototype")
@Produces(MediaType.APPLICATION_JSON)
public class AssetsResource extends ServiceResource {

    private Logger logger = LoggerFactory.getLogger( AssetsResource.class );

    @Autowired
    private BinaryStore binaryStore;


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
    public JSONWithPadding executeGet( @Context UriInfo ui,
                                       @QueryParam("callback") @DefaultValue("callback") String callback )
            throws Exception {
        logger.info( "In AssetsResource.executeGet with ui: {} and callback: {}", ui );
        return super.executeGet( ui, callback );
    }


    @Override
    @PUT
    @RequireApplicationAccess
    @Consumes(MediaType.APPLICATION_JSON)
    public JSONWithPadding executePut( @Context UriInfo ui, Map<String, Object> json,
                                       @QueryParam("callback") @DefaultValue("callback") String callback )
            throws Exception {

        return super.executePut( ui, json, callback );
    }


    @POST
    @RequireApplicationAccess
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("{entityId: [A-Fa-f0-9]{8}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{12}}/data")
    public Response uploadData( @FormDataParam("file") InputStream uploadedInputStream,
                                @FormDataParam("file") FormDataContentDisposition fileDetail,
                                @PathParam("entityId") PathSegment entityId ) throws Exception {

        UUID assetId = UUID.fromString( entityId.getPath() );
        logger.info( "In AssetsResource.uploadData with id: {}", assetId );
        EntityManager em = emf.getEntityManager( getApplicationId() );
        Asset asset = em.get( assetId, Asset.class );

        binaryStore.write( getApplicationId(), asset, uploadedInputStream );
        em.update( asset );
        return Response.status( 200 ).build();
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
