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
package org.apache.usergrid.rest.applications;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jersey.api.json.JSONWithPadding;
import com.sun.jersey.multipart.BodyPart;
import com.sun.jersey.multipart.BodyPartEntity;
import com.sun.jersey.multipart.FormDataBodyPart;
import com.sun.jersey.multipart.FormDataMultiPart;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
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
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.apache.commons.lang.StringUtils;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.QueryUtils;
import org.apache.usergrid.persistence.index.query.Query;
import org.apache.usergrid.rest.AbstractContextResource;
import org.apache.usergrid.rest.ApiResponse;
import org.apache.usergrid.rest.RootResource;
import org.apache.usergrid.rest.applications.assets.AssetsResource;
import org.apache.usergrid.rest.security.annotations.RequireApplicationAccess;
import org.apache.usergrid.security.oauth.AccessInfo;
import org.apache.usergrid.services.ServiceAction;
import org.apache.usergrid.services.ServiceManager;
import org.apache.usergrid.services.ServiceParameter;
import org.apache.usergrid.services.ServicePayload;
import org.apache.usergrid.services.ServiceRequest;
import org.apache.usergrid.services.ServiceResults;
import org.apache.usergrid.services.assets.data.AssetUtils;
import org.apache.usergrid.services.assets.data.BinaryStore;
import org.apache.usergrid.utils.InflectionUtils;
import org.apache.usergrid.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;


@Component
@Scope("prototype")
@Produces({
        MediaType.APPLICATION_JSON, "application/javascript", "application/x-javascript", "text/ecmascript",
        "application/ecmascript", "text/jscript"
})
public class ServiceResource extends AbstractContextResource {

    protected static final Logger LOG = LoggerFactory.getLogger( ServiceResource.class );
    private static final String FILE_FIELD_NAME = "file";


    @Autowired
    private BinaryStore binaryStore;

    protected ServiceManager services;

    List<ServiceParameter> serviceParameters = null;


    public ServiceResource() {
    }


    @Override
    public void setParent( AbstractContextResource parent ) {
        super.setParent( parent );
        if ( parent instanceof ServiceResource ) {
            services = ( ( ServiceResource ) parent ).services;
        }
    }


    public ServiceResource getServiceResourceParent() {
        if ( parent instanceof ServiceResource ) {
            return ( ServiceResource ) parent;
        }
        return null;
    }


    public ServiceManager getServices() {
        return services;
    }


    public UUID getApplicationId() {
        return services.getApplicationId();
    }


    public List<ServiceParameter> getServiceParameters() {
        if ( serviceParameters != null ) {
            return serviceParameters;
        }
        if ( getServiceResourceParent() != null ) {
            return getServiceResourceParent().getServiceParameters();
        }
        serviceParameters = new ArrayList<ServiceParameter>();
        return serviceParameters;
    }


    public static List<ServiceParameter> addMatrixParams( List<ServiceParameter> parameters, UriInfo ui,
                                                          PathSegment ps ) throws Exception {

        MultivaluedMap<String, String> params = ps.getMatrixParameters();

        if ( params != null ) {
            Query query = Query.fromQueryParams( params );
            if ( query != null ) {
                parameters = ServiceParameter.addParameter( parameters, query );
            }
        }

        return parameters;
    }


    public static List<ServiceParameter> addQueryParams( List<ServiceParameter> parameters, UriInfo ui )
            throws Exception {

        MultivaluedMap<String, String> params = ui.getQueryParameters();
        if ( params != null ) {
            //TODO TN query parameters are not being correctly decoded here.  The URL encoded strings
            //aren't getting decoded properly
            Query query = Query.fromQueryParams( params );

            if(query == null && parameters.size() > 0 && parameters.get( 0 ).isId()){
                query = Query.fromUUID( parameters.get( 0 ).getId() );
            }

            if ( query != null ) {
                parameters = ServiceParameter.addParameter( parameters, query );
            }
        }

        return parameters;
    }


    @Path("file")
    public AbstractContextResource getFileResource( @Context UriInfo ui ) throws Exception {
        LOG.debug( "in assets in ServiceResource" );
        ServiceParameter.addParameter( getServiceParameters(), "assets" );

        PathSegment ps = getFirstPathSegment( "assets" );
        if ( ps != null ) {
            addMatrixParams( getServiceParameters(), ui, ps );
        }

        return getSubResource( AssetsResource.class );
    }


    @Path(RootResource.ENTITY_ID_PATH)
    public AbstractContextResource addIdParameter( @Context UriInfo ui, @PathParam("entityId") PathSegment entityId )
            throws Exception {

        LOG.debug( "ServiceResource.addIdParameter" );

        UUID itemId = UUID.fromString( entityId.getPath() );

        ServiceParameter.addParameter( getServiceParameters(), itemId );

        addMatrixParams( getServiceParameters(), ui, entityId );

        return getSubResource( ServiceResource.class );
    }


    @Path("{itemName}")
    public AbstractContextResource addNameParameter( @Context UriInfo ui, @PathParam("itemName") PathSegment itemName )
            throws Exception {

        LOG.debug( "ServiceResource.addNameParameter" );

        LOG.debug( "Current segment is {}", itemName.getPath() );

        if ( itemName.getPath().startsWith( "{" ) ) {
            Query query = Query.fromJsonString( itemName.getPath() );
            if ( query != null ) {
                ServiceParameter.addParameter( getServiceParameters(), query );
            }
        }
        else {
            ServiceParameter.addParameter( getServiceParameters(), itemName.getPath() );
        }

        addMatrixParams( getServiceParameters(), ui, itemName );

        return getSubResource( ServiceResource.class );
    }


    public ServiceResults executeServiceRequest( UriInfo ui, ApiResponse response, ServiceAction action,
                                                 ServicePayload payload ) throws Exception {

        LOG.debug( "ServiceResource.executeServiceRequest" );

        boolean tree = "true".equalsIgnoreCase( ui.getQueryParameters().getFirst( "tree" ) );
        boolean collectionGet = false;
        if ( action == ServiceAction.GET ) {
            collectionGet = (getServiceParameters().size() == 1 && InflectionUtils
                    .isPlural(getServiceParameters().get(0)));
        }
        addQueryParams( getServiceParameters(), ui );
        ServiceRequest r = services.newRequest( action, tree, getServiceParameters(), payload );
        response.setServiceRequest( r );
        ServiceResults results = r.execute();
        if ( results != null ) {
            if ( results.hasData() ) {
                response.setData( results.getData() );
            }
            if ( results.getServiceMetadata() != null ) {
                response.setMetadata( results.getServiceMetadata() );
            }
            Query query = r.getLastQuery();
            if ( query != null ) {
                if ( query.hasSelectSubjects() ) {
                    response.setList( QueryUtils.getSelectionResults( query, results ) );
                    response.setCount( response.getList().size() );
                    response.setNext( results.getNextResult() );
                    response.setPath( results.getPath() );
                    return results;
                }
            }
            if ( collectionGet ) {
                response.setCount( results.size() );
            }

            response.setResults( results );
        }

        httpServletRequest.setAttribute( "applicationId", services.getApplicationId() );

        return results;
    }


    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_HTML})
    @RequireApplicationAccess
    public JSONWithPadding executeGet( @Context UriInfo ui,
                                       @QueryParam("callback") @DefaultValue("callback") String callback )
            throws Exception {

        LOG.debug( "ServiceResource.executeGet" );

        ApiResponse response = createApiResponse();

        response.setAction( "get" );
        response.setApplication( services.getApplication() );
        response.setParams( ui.getQueryParameters() );

        executeServiceRequest( ui, response, ServiceAction.GET, null );

        return new JSONWithPadding( response, callback );
    }


    @SuppressWarnings({ "unchecked" })
    public ServicePayload getPayload( Object json ) {
        ServicePayload payload = null;
        json = JsonUtils.normalizeJsonTree( json );
        if ( json instanceof Map ) {
            Map<String, Object> jsonMap = ( Map<String, Object> ) json;
            payload = ServicePayload.payload( jsonMap );
        }
        else if ( json instanceof List ) {
            List<?> jsonList = ( List<?> ) json;
            if ( jsonList.size() > 0 ) {
                if ( jsonList.get( 0 ) instanceof UUID ) {
                    payload = ServicePayload.idListPayload( ( List<UUID> ) json );
                }
                else if ( jsonList.get( 0 ) instanceof Map ) {
                    payload = ServicePayload.batchPayload( ( List<Map<String, Object>> ) jsonList );
                }
            }
        }
        if ( payload == null ) {
            payload = new ServicePayload();
        }
        return payload;
    }




    /**
     * Necessary to work around inexplicable problems with EntityHolder.
     * See above.
     */
    public JSONWithPadding executePostWithObject( @Context UriInfo ui, Object json,
            @QueryParam("callback") @DefaultValue("callback") String callback ) throws Exception {

        LOG.debug( "ServiceResource.executePostWithMap" );

        ApiResponse response = createApiResponse();


        response.setAction( "post" );
        response.setApplication( services.getApplication() );
        response.setParams( ui.getQueryParameters() );

        ServicePayload payload = getPayload( json );

        executeServiceRequest( ui, response, ServiceAction.POST, payload );

        return new JSONWithPadding( response, callback );
    }


    /**
     * Necessary to work around inexplicable problems with EntityHolder.
     * See above.
     */
    public JSONWithPadding executePutWithMap( @Context UriInfo ui, Map<String, Object> json,
            @QueryParam("callback") @DefaultValue("callback") String callback ) throws Exception {

        ApiResponse response = createApiResponse();
        response.setAction( "put" );

        services.getApplicationRef();
        response.setApplication( services.getApplication() );
        response.setParams( ui.getQueryParameters() );

        ServicePayload payload = getPayload( json );

        executeServiceRequest( ui, response, ServiceAction.PUT, payload );

        return new JSONWithPadding( response, callback );
    }


    @POST
    @RequireApplicationAccess
    @Consumes(MediaType.APPLICATION_JSON)
    public JSONWithPadding executePost( @Context UriInfo ui, String body,
            @QueryParam("callback") @DefaultValue("callback") String callback ) throws Exception {

        LOG.debug( "ServiceResource.executePost: body = " + body );

        Object json;
        if ( StringUtils.isEmpty( body ) ) {
            json = null;
        } else {
            json = readJsonToObject( body );
        }

        ApiResponse response = createApiResponse();


        response.setAction( "post" );
        response.setApplication( services.getApplication() );
        response.setParams( ui.getQueryParameters() );

        ServicePayload payload = getPayload( json );

        executeServiceRequest( ui, response, ServiceAction.POST, payload );

        return new JSONWithPadding( response, callback );
    }



    @PUT
    @RequireApplicationAccess
    @Consumes(MediaType.APPLICATION_JSON)
    public JSONWithPadding executePut( @Context UriInfo ui, String body,
                                       @QueryParam("callback") @DefaultValue("callback") String callback )
            throws Exception {

        LOG.debug( "ServiceResource.executePut" );

        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> json = mapper.readValue( body, mapTypeReference );

        return executePutWithMap(ui, json, callback);
    }


    @DELETE
    @RequireApplicationAccess
    public JSONWithPadding executeDelete(
        @Context UriInfo ui,
        @QueryParam("callback") @DefaultValue("callback") String callback,
        @QueryParam("app_delete_confirm") String confirmAppDelete )
        throws Exception {

        LOG.debug( "ServiceResource.executeDelete" );

        ApiResponse response = createApiResponse();
        response.setAction( "delete" );
        response.setApplication( services.getApplication() );
        response.setParams( ui.getQueryParameters() );

        ServiceResults sr = executeServiceRequest( ui, response, ServiceAction.DELETE, null );

        for ( Entity entity : sr.getEntities() ) {
            if ( entity.getProperty( AssetUtils.FILE_METADATA ) != null ) {
                binaryStore.delete( services.getApplicationId(), entity );
            }
        }

        return new JSONWithPadding( response, callback );
    }

    //    TODO Temporarily removed until we test further
    //    @Produces("text/csv")
    //    @GET
    //    @RequireApplicationAccess
    //    @Consumes("text/csv")
    //    public String executeGetCsv(@Context UriInfo ui,
    //            @QueryParam("callback") @DefaultValue("callback") String callback)
    //                    throws Exception {
    //        ui.getQueryParameters().putSingle("pad", "true");
    //        JSONWithPadding jsonp = executeGet(ui, callback);
    //
    //        StringBuilder builder = new StringBuilder();
    //        if ((jsonp != null) && (jsonp.getJsonSource() instanceof ApiResponse)) {
    //            ApiResponse apiResponse = (ApiResponse) jsonp.getJsonSource();
    //            if ((apiResponse.getCounters() != null)
    //                    && (apiResponse.getCounters().size() > 0)) {
    //                List<AggregateCounterSet> counters = apiResponse.getCounters();
    //                int size = counters.get(0).getValues().size();
    //                List<AggregateCounter> firstCounterList = counters.get(0)
    //                        .getValues();
    //                if (size > 0) {
    //                    builder.append("timestamp");
    //                    for (AggregateCounterSet counterSet : counters) {
    //                        builder.append(",");
    //                        builder.append(counterSet.getName());
    //                    }
    //                    builder.append("\n");
    //                    SimpleDateFormat formatter = new SimpleDateFormat(
    //                            "yyyy-MM-dd HH:mm:ss.SSS");
    //                    for (int i = 0; i < size; i++) {
    //                        // yyyy-mm-dd hh:mm:ss.000
    //                        builder.append(formatter.format(new Date(
    //                                firstCounterList.get(i).getTimestamp())));
    //                        for (AggregateCounterSet counterSet : counters) {
    //                            List<AggregateCounter> counterList = counterSet
    //                                    .getValues();
    //                            builder.append(",");
    //                            builder.append(counterList.get(i).getValue());
    //                        }
    //                        builder.append("\n");
    //                    }
    //                }
    //            } else if ((apiResponse.getEntities() != null)
    //                    && (apiResponse.getEntities().size() > 0)) {
    //                for (Entity entity : apiResponse.getEntities()) {
    //                    builder.append(entity.getUuid());
    //                    builder.append(",");
    //                    builder.append(entity.getType());
    //                    builder.append(",");
    //                    builder.append(mapToJsonString(entity));
    //                }
    //
    //            }
    //        }
    //        return builder.toString();
    //    }


    public static String wrapWithCallback( AccessInfo accessInfo, String callback ) {
        return wrapWithCallback( JsonUtils.mapToJsonString( accessInfo ), callback );
    }


    public static String wrapWithCallback( String json, String callback ) {
        if ( StringUtils.isNotBlank( callback ) ) {
            json = callback + "(" + json + ")";
        }
        return json;
    }


    public static MediaType jsonMediaType( String callback ) {
        return StringUtils.isNotBlank( callback ) ? new MediaType( "application", "javascript" ) : APPLICATION_JSON_TYPE;
    }


    /** ************** the following is file attachment (Asset) support ********************* */

    @POST
    @RequireApplicationAccess
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public JSONWithPadding executeMultiPartPost( @Context UriInfo ui,
                                                 @QueryParam("callback") @DefaultValue("callback") String callback,
                                                 FormDataMultiPart multiPart ) throws Exception {

        LOG.debug( "ServiceResource.executeMultiPartPost" );
        return executeMultiPart( ui, callback, multiPart, ServiceAction.POST );
    }


    @PUT
    @RequireApplicationAccess
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public JSONWithPadding executeMultiPartPut( @Context UriInfo ui,
                                                @QueryParam("callback") @DefaultValue("callback") String callback,
                                                FormDataMultiPart multiPart ) throws Exception {

        LOG.debug( "ServiceResource.executeMultiPartPut" );
        return executeMultiPart( ui, callback, multiPart, ServiceAction.PUT );
    }


    private JSONWithPadding executeMultiPart( UriInfo ui, String callback, FormDataMultiPart multiPart,
                                              ServiceAction serviceAction ) throws Exception {

        // collect form data values
        List<BodyPart> bodyParts = multiPart.getBodyParts();
        HashMap<String, Object> data = new HashMap<String, Object>();
        for ( BodyPart bp : bodyParts ) {
            FormDataBodyPart bodyPart = ( FormDataBodyPart ) bp;
            if ( bodyPart.getMediaType().equals( MediaType.TEXT_PLAIN_TYPE ) ) {
                data.put( bodyPart.getName(), bodyPart.getValue() );
            }
            else {
                LOG.info( "skipping bodyPart {} of media type {}", bodyPart.getName(), bodyPart.getMediaType() );
            }
        }

        FormDataBodyPart fileBodyPart = multiPart.getField( FILE_FIELD_NAME );

        if ( data.isEmpty() && fileBodyPart != null ) { // ensure entity is created even if there are no properties
            data.put( AssetUtils.FILE_METADATA, new HashMap() );
        }

        // process entity
        ApiResponse response = createApiResponse();
        response.setAction( serviceAction.name().toLowerCase() );
        response.setApplication( services.getApplication() );
        response.setParams( ui.getQueryParameters() );
        ServicePayload payload = getPayload( data );
        ServiceResults serviceResults = executeServiceRequest( ui, response, serviceAction, payload );

        // process file part
        if ( fileBodyPart != null ) {
            InputStream fileInput = ( ( BodyPartEntity ) fileBodyPart.getEntity() ).getInputStream();
            if ( fileInput != null ) {
                Entity entity = serviceResults.getEntity();
                EntityManager em = emf.getEntityManager( getApplicationId() );
                binaryStore.write( getApplicationId(), entity, fileInput );
                em.update( entity );
                serviceResults.setEntity( entity );
            }
        }

        return new JSONWithPadding( response, callback );
    }


    @PUT
    @RequireApplicationAccess
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    public Response uploadDataStreamPut( @Context UriInfo ui, InputStream uploadedInputStream ) throws Exception {
        return uploadDataStream( ui, uploadedInputStream );
    }


    @POST
    @RequireApplicationAccess
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    public Response uploadDataStream( @Context UriInfo ui, InputStream uploadedInputStream ) throws Exception {

        ApiResponse response = createApiResponse();
        response.setAction( "get" );
        response.setApplication( services.getApplication() );
        response.setParams( ui.getQueryParameters() );
        ServiceResults serviceResults = executeServiceRequest( ui, response, ServiceAction.GET, null );

        Entity entity = serviceResults.getEntity();
        binaryStore.write( getApplicationId(), entity, uploadedInputStream );

        EntityManager em = emf.getEntityManager( getApplicationId() );
        em.update( entity );
        return Response.status( 200 ).build();
    }


    @GET
    @RequireApplicationAccess
    @Produces(MediaType.WILDCARD)
    public Response executeStreamGet( @Context UriInfo ui, @PathParam("entityId") PathSegment entityId,
                                      @HeaderParam("range") String rangeHeader,
                                      @HeaderParam("if-modified-since") String modifiedSince ) throws Exception {

        LOG.debug( "ServiceResource.executeStreamGet" );

        ApiResponse response = createApiResponse();
        response.setAction( "get" );
        response.setApplication( services.getApplication() );
        response.setParams( ui.getQueryParameters() );
        ServiceResults serviceResults = executeServiceRequest( ui, response, ServiceAction.GET, null );
        Entity entity = serviceResults.getEntity();

        LOG.info( "In AssetsResource.findAsset with id: {}, range: {}, modifiedSince: {}",
                new Object[] { entityId, rangeHeader, modifiedSince } );

        Map<String, Object> fileMetadata = AssetUtils.getFileMetadata( entity );

        // return a 302 if not modified
        Date modified = AssetUtils.fromIfModifiedSince( modifiedSince );
        if ( modified != null ) {
            Long lastModified = ( Long ) fileMetadata.get( AssetUtils.LAST_MODIFIED );
            if ( lastModified - modified.getTime() < 0 ) {
                return Response.status( Response.Status.NOT_MODIFIED ).build();
            }
        }

        boolean range = StringUtils.isNotBlank( rangeHeader );
        long start = 0, end = 0, contentLength = 0;
        InputStream inputStream;

        if ( range ) { // honor range request, calculate start & end

            String rangeValue = rangeHeader.trim().substring( "bytes=".length() );
            contentLength = ( Long ) fileMetadata.get( AssetUtils.CONTENT_LENGTH );
            end = contentLength - 1;
            if ( rangeValue.startsWith( "-" ) ) {
                start = contentLength - 1 - Long.parseLong( rangeValue.substring( "-".length() ) );
            }
            else {
                String[] startEnd = rangeValue.split( "-" );
                long parsedStart = Long.parseLong( startEnd[0] );
                if ( parsedStart > start && parsedStart < end ) {
                    start = parsedStart;
                }
                if ( startEnd.length > 1 ) {
                    long parsedEnd = Long.parseLong( startEnd[1] );
                    if ( parsedEnd > start && parsedEnd < end ) {
                        end = parsedEnd;
                    }
                }
            }

            inputStream = binaryStore.read( getApplicationId(), entity, start, end - start );
        }
        else { // no range

            inputStream = binaryStore.read( getApplicationId(), entity );
        }

        // return 404 if not found
        if ( inputStream == null ) {
            return Response.status( Response.Status.NOT_FOUND ).build();
        }

        Long lastModified = ( Long ) fileMetadata.get( AssetUtils.LAST_MODIFIED );
        Response.ResponseBuilder responseBuilder =
                Response.ok( inputStream ).type( ( String ) fileMetadata.get( AssetUtils.CONTENT_TYPE ) )
                        .lastModified( new Date( lastModified ) );

        if ( fileMetadata.get( AssetUtils.E_TAG ) != null ) {
            responseBuilder.tag( ( String ) fileMetadata.get( AssetUtils.E_TAG ) );
        }

        if ( range ) {
            responseBuilder.header( "Content-Range", "bytes " + start + "-" + end + "/" + contentLength );
        }

        return responseBuilder.build();
    }
}
