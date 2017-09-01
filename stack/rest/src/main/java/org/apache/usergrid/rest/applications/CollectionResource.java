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


import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriInfo;

import com.google.common.base.Preconditions;
import org.apache.usergrid.corepersistence.index.CollectionDeleteRequestBuilder;
import org.apache.usergrid.corepersistence.index.CollectionDeleteRequestBuilderImpl;
import org.apache.usergrid.corepersistence.index.CollectionDeleteService;
import org.apache.usergrid.persistence.index.utils.ConversionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import org.apache.commons.lang.NullArgumentException;
import org.apache.commons.lang.StringUtils;

import org.apache.usergrid.persistence.Query;
import org.apache.usergrid.rest.ApiResponse;
import org.apache.usergrid.rest.security.annotations.RequireApplicationAccess;
import org.apache.usergrid.rest.security.annotations.RequireSystemAccess;
import org.apache.usergrid.rest.system.IndexResource;
import org.apache.usergrid.services.ServiceAction;
import org.apache.usergrid.services.ServiceParameter;
import org.apache.usergrid.services.ServicePayload;

import com.fasterxml.jackson.jaxrs.json.annotation.JSONP;

import java.util.HashMap;
import java.util.Map;


/**
 * A collection resource that stands before the Service Resource. If it cannot find
 * the specified method then we should route the call to the service resource proper.
 * Otherwise handle it in here.
 */
@Component
@Scope("prototype")
@Produces({ MediaType.APPLICATION_JSON, "application/javascript", "application/x-javascript", "text/ecmascript",
    "application/ecmascript", "text/jscript"
})
public class CollectionResource extends ServiceResource {

    private static final Logger logger = LoggerFactory.getLogger( CollectionResource.class );
    private static final String UPDATED_BEFORE_FIELD = "updatedBefore";

    public CollectionResource() {
    }


    /**
     * POST settings for a collection.
     *
     * Expects a JSON object which may include:
     * - fields: (array or string) either an array of field names to be indexed, or 'all' or 'none'
     * - region: (string) name of the authoritative region for this collection
     */
    @POST
    @Path( "{itemName}/_settings" )
    @Produces({ MediaType.APPLICATION_JSON,"application/javascript"})
    @RequireApplicationAccess
    @JSONP
    public ApiResponse executePostOnSettingsWithCollectionName(
        @Context UriInfo ui,
        @PathParam("itemName") PathSegment itemName,
        String body,
        @QueryParam("callback") @DefaultValue("callback") String callback ) throws Exception {

        if(logger.isTraceEnabled()){
            logger.trace( "CollectionResource.executePostOnSettingsWithCollectionName" );
        }

        addItemToServiceContext( ui, itemName );

        Object json;
        if ( StringUtils.isEmpty( body ) ) {
            throw new NullArgumentException( "No body posted" );
        } else {
            json = readJsonToObject( body );
        }

        ApiResponse response = createApiResponse();

        response.setAction( "post" );
        response.setApplication( services.getApplication() );
        response.setParams( ui.getQueryParameters() );

        ServicePayload payload = getPayload( json );

        executeServicePostRequestForSettings( ui,response, ServiceAction.POST, payload );

        return response;
    }


    private void addItemToServiceContext( UriInfo ui, PathSegment itemName ) throws Exception {

        // The below is duplicated because it could change in the future
        // and is probably not all needed but not determined yet.
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
    }


    /**
     * Delete settings for a collection.
     */
    @DELETE
    @Path( "{itemName}/_settings" )
    @Produces({ MediaType.APPLICATION_JSON,"application/javascript"})
    @RequireApplicationAccess
    @JSONP
    public ApiResponse executeDeleteOnSettingsWithCollectionName(
        @Context UriInfo ui,
        @PathParam("itemName") PathSegment itemName,
        String body,
        @QueryParam("callback") @DefaultValue("callback") String callback )
        throws Exception {

        if(logger.isTraceEnabled()){
            logger.trace( "CollectionResource.executeDeleteOnSettingsWithCollectionName" );
        }

        addItemToServiceContext( ui, itemName );

        ApiResponse response = createApiResponse();

        response.setAction( "delete" );
        response.setApplication( services.getApplication() );
        response.setParams( ui.getQueryParameters() );


        emf.getEntityManager( services.getApplicationId() ).deleteCollectionSettings( itemName.getPath().toLowerCase() );

        return response;
    }



    @GET
    @Path( "{itemName}/_settings")
    @Produces({MediaType.APPLICATION_JSON,"application/javascript"})
    @RequireApplicationAccess
    @JSONP
    public ApiResponse executeGetOnIndex(
        @Context UriInfo ui,
        @PathParam("itemName") PathSegment itemName,
        @QueryParam("callback") @DefaultValue("callback") String callback ) throws Exception {

        if(logger.isTraceEnabled()){
            logger.trace( "CollectionResource.executeGetOnSettings" );
        }

        addItemToServiceContext( ui, itemName );

        ApiResponse response = createApiResponse();
        response.setAction( "get" );
        response.setApplication( services.getApplication() );
        response.setParams( ui.getQueryParameters() );

        executeServiceGetRequestForSettings( ui,response,ServiceAction.GET,null );

        return response;
    }


    @POST
    @Path("{itemName}/_clear")
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @RequireApplicationAccess
    @JSONP
    public ApiResponse clearCollectionPut(
        final Map<String, Object> payload,
        @PathParam("itemName") final String collectionName,
        @QueryParam("callback") @DefaultValue("callback") String callback
    ) throws Exception {

        logger.info("Clearing collection {} for application {}", collectionName, getApplicationId().toString());

        final CollectionDeleteRequestBuilder request = createRequest()
            .withApplicationId(getApplicationId())
            .withCollection(collectionName);

        return executeResumeAndCreateResponse(payload, request, callback);

    }


    @GET
    @Path( "{itemName}/_clear/{jobId}")
    @Produces({MediaType.APPLICATION_JSON,"application/javascript"})
    @RequireApplicationAccess
    @JSONP
    public ApiResponse clearCollectionJobGet(
        @Context UriInfo ui,
        @PathParam("itemName") PathSegment itemName,
        @PathParam("jobId") String jobId,
        @QueryParam("callback") @DefaultValue("callback") String callback ) throws Exception {

        if(logger.isTraceEnabled()){
            logger.trace( "CollectionResource.clearCollectionJobGet" );
        }

        Preconditions
            .checkNotNull(jobId, "path param jobId must not be null" );

        CollectionDeleteService.CollectionDeleteStatus status = getCollectionDeleteService().getStatus(jobId);

        final ApiResponse response = createApiResponse();

        response.setAction( "clear collection" );
        response.setProperty( "jobId", status.getJobId() );
        response.setProperty( "status", status.getStatus() );
        response.setProperty( "lastUpdatedEpoch", status.getLastUpdated() );
        response.setProperty( "numberCheckedForDeletion", status.getNumberProcessed() );
        response.setSuccess();

        return response;
    }


    // TODO: this can't be controlled and until it can be controlled we shouldn' allow muggles to do this.
    // So system access only.
    // TODO: use scheduler here to get around people sending a reindex call 30 times.
    @POST
    @Path("{itemName}/_reindex")
    @Produces({ MediaType.APPLICATION_JSON,"application/javascript"})
    @RequireSystemAccess
    @JSONP
    public ApiResponse executePostForReindexing(
        @Context UriInfo ui, String body,
        @PathParam("itemName") PathSegment itemName,
        @QueryParam("callback") @DefaultValue("callback") String callback ) throws Exception {

        addItemToServiceContext( ui, itemName );

        IndexResource indexResource = new IndexResource(injector);
        return indexResource.rebuildIndexesPost(
            services.getApplicationId().toString(),itemName.getPath(),false,callback );
    }


    private CollectionDeleteService getCollectionDeleteService() {
        return injector.getInstance( CollectionDeleteService.class );
    }


    private CollectionDeleteRequestBuilder createRequest() {
        return new CollectionDeleteRequestBuilderImpl();
    }


    private ApiResponse executeResumeAndCreateResponse( final Map<String, Object> payload,
                                                        final CollectionDeleteRequestBuilder request,
                                                        final String callback ) {

        Map<String,Object> newPayload = payload;
        if(newPayload == null ||  !payload.containsKey( UPDATED_BEFORE_FIELD )){
            newPayload = new HashMap<>(1);
            newPayload.put(UPDATED_BEFORE_FIELD,Long.MAX_VALUE);
        }

        Preconditions.checkArgument(newPayload.get(UPDATED_BEFORE_FIELD) instanceof Number,
            "The field \"updatedBefore\" in the payload must be a timestamp" );

        //add our updated timestamp to the request
        if ( newPayload.containsKey( UPDATED_BEFORE_FIELD ) ) {
            final long timestamp = ConversionUtils.getLong(newPayload.get(UPDATED_BEFORE_FIELD));
            request.withEndTimestamp( timestamp );
        }

        return executeAndCreateResponse( request, callback );
    }

    /**
     * Execute the request and return the response.
     */
    private ApiResponse executeAndCreateResponse(final CollectionDeleteRequestBuilder request, final String callback ) {


        final CollectionDeleteService.CollectionDeleteStatus status = getCollectionDeleteService().deleteCollection( request );

        final ApiResponse response = createApiResponse();

        response.setAction( "clear collection" );
        response.setProperty( "jobId", status.getJobId() );
        response.setProperty( "status", status.getStatus() );
        response.setProperty( "lastUpdatedEpoch", status.getLastUpdated() );
        response.setProperty( "numberQueued", status.getNumberProcessed() );
        response.setSuccess();

        return response;
    }

}
