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


import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriInfo;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import org.apache.commons.lang.StringUtils;

import org.apache.usergrid.corepersistence.index.ReIndexRequestBuilder;
import org.apache.usergrid.corepersistence.index.ReIndexRequestBuilderImpl;
import org.apache.usergrid.corepersistence.index.ReIndexService;
import org.apache.usergrid.persistence.Query;
import org.apache.usergrid.persistence.index.utils.UUIDUtils;
import org.apache.usergrid.rest.AbstractContextResource;
import org.apache.usergrid.rest.ApiResponse;
import org.apache.usergrid.rest.RootResource;
import org.apache.usergrid.rest.security.annotations.RequireApplicationAccess;
import org.apache.usergrid.rest.security.annotations.RequireSystemAccess;
import org.apache.usergrid.services.AbstractCollectionService;
import org.apache.usergrid.services.ServiceAction;
import org.apache.usergrid.services.ServiceParameter;
import org.apache.usergrid.services.ServicePayload;

import com.fasterxml.jackson.jaxrs.json.annotation.JSONP;


/**
 * A collection resource that stands before the Service Resource. If it cannot find
 * the specified method then we should route the call to the service resource proper.
 * Otherwise handle it in here.
 */
@Component
@Scope("prototype")
@Produces({
    MediaType.APPLICATION_JSON, "application/javascript", "application/x-javascript", "text/ecmascript",
    "application/ecmascript", "text/jscript"
})
public class CollectionResource extends ServiceResource {

    public CollectionResource() {
    }

    /**
     * THE BEGINNINGS OF AN ENDPOINT THAT WILL ALLOW TO DEFINE WHAT TO
     * STORE IN ELASTICSEARCH.
     * @param ui
     * @param callback
     * @return
     * @throws Exception
     */
    @POST
    @Path( "{itemName}/_indexes" )
    @Produces({ MediaType.APPLICATION_JSON,"application/javascript"})
    @RequireApplicationAccess
    @JSONP
    public ApiResponse executePostOnIndexesWithCollectionName( @Context UriInfo ui, @PathParam("itemName") PathSegment itemName,
                                                               String body,
                                                               @QueryParam("callback") @DefaultValue("callback") String callback )
        throws Exception {

        if(logger.isTraceEnabled()){
            logger.trace( "ServiceResource.executePostOnIndexes" );
        }

        addItemToServiceContext( ui, itemName );

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

        executeServicePostRequestForSchema( ui,response, ServiceAction.POST,payload );

        return response;
    }

    private void addItemToServiceContext( final @Context UriInfo ui,
                                          final @PathParam( "itemName" ) PathSegment itemName ) throws Exception {
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

    @DELETE
    @Path( "{itemName}/_indexes" )
    @Produces({ MediaType.APPLICATION_JSON,"application/javascript"})
    @RequireApplicationAccess
    @JSONP
    public ApiResponse executeDeleteOnIndexesWithCollectionName( @Context UriInfo ui, @PathParam("itemName") PathSegment itemName,
                                                               String body,
                                                               @QueryParam("callback") @DefaultValue("callback") String callback )
        throws Exception {

        if(logger.isTraceEnabled()){
            logger.trace( "CollectionResource.executeDeleteOnIndexesWithCollectionName" );
        }

        addItemToServiceContext( ui, itemName );

        ApiResponse response = createApiResponse();

        response.setAction( "delete" );
        response.setApplication( services.getApplication() );
        response.setParams( ui.getQueryParameters() );


        emf.getEntityManager( services.getApplicationId() ).deleteCollectionSchema( itemName.getPath().toLowerCase() );

        return response;
    }



    @GET
    @Path( "{itemName}/_index")
    @Produces({MediaType.APPLICATION_JSON,"application/javascript"})
    @RequireApplicationAccess
    @JSONP
    public ApiResponse executeGetOnIndex( @Context UriInfo ui,@PathParam("itemName") PathSegment itemName,
                                          @QueryParam("callback") @DefaultValue("callback") String callback )
        throws Exception {

        if(logger.isTraceEnabled()){
            logger.trace( "CollectionResource.executeGetOnIndex" );
        }

        addItemToServiceContext( ui, itemName );

        ApiResponse response = createApiResponse();
        response.setAction( "get" );
        response.setApplication( services.getApplication() );
        response.setParams( ui.getQueryParameters() );

        executeServiceGetRequestForSchema( ui,response,ServiceAction.GET,null );

        return response;
    }


    //TODO: this can't be controlled and until it can be controlled we shouldn' allow muggles to do this. So system access only.
    //TODO: use scheduler here to get around people sending a reindex call 30 times.
    @POST
    @Path("{itemName}/_reindex")
    @Produces({ MediaType.APPLICATION_JSON,"application/javascript"})
    @RequireSystemAccess
    @JSONP
    public ApiResponse executePostForReindexing( @Context UriInfo ui, String body,
                                                 @PathParam("itemName") PathSegment itemName,
                                             @QueryParam("callback") @DefaultValue("callback") String callback )
        throws Exception {

        addItemToServiceContext( ui, itemName );

        final ReIndexRequestBuilder request =
            createRequest().withApplicationId( services.getApplicationId() ).withCollection(
                String.valueOf( getServiceParameters().get( 0 ) ) ).withDelay( 50, TimeUnit.MILLISECONDS );

        return executeAndCreateResponse( request, callback );
    }

    private ReIndexService getReIndexService() {
        return injector.getInstance( ReIndexService.class );
    }

    private ReIndexRequestBuilder createRequest() {
        //TODO: wire this up through spring, and in the future guice.
        return new ReIndexRequestBuilderImpl();
    }

    /**
     * Execute the request and return the response.
     */
    private ApiResponse executeAndCreateResponse( final ReIndexRequestBuilder request, final String callback ) {


        final ReIndexService.ReIndexStatus status = getReIndexService().rebuildIndex( request );

        final ApiResponse response = createApiResponse();

        response.setAction( "rebuild indexes" );
        response.setProperty( "jobId", status.getJobId() );
        response.setProperty( "status", status.getStatus() );
        response.setProperty( "lastUpdatedEpoch", status.getLastUpdated() );
        response.setProperty( "numberQueued", status.getNumberProcessed() );
        response.setSuccess();

        return response;
    }

}
