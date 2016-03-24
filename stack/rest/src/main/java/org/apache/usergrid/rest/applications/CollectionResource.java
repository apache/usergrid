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
    @Path("_indexes")
    @Produces({ MediaType.APPLICATION_JSON,"application/javascript"})
    @RequireApplicationAccess
    @JSONP
    public ApiResponse executePostOnIndexes( @Context UriInfo ui, String body,
                                             @QueryParam("callback") @DefaultValue("callback") String callback )
        throws Exception {

        if(logger.isTraceEnabled()){
            logger.trace( "ServiceResource.executePostOnIndexes" );
        }

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

    @GET
    @Path("_index")
    @Produces({MediaType.APPLICATION_JSON,"application/javascript"})
    @RequireApplicationAccess
    @JSONP
    public ApiResponse executeGetOnIndex( @Context UriInfo ui,
                                          @QueryParam("callback") @DefaultValue("callback") String callback )
        throws Exception {

        if(logger.isTraceEnabled()){
            logger.trace( "CollectionResource.executeGetOnIndex" );
        }

        ApiResponse response = createApiResponse();
        response.setAction( "get" );
        response.setApplication( services.getApplication() );
        response.setParams( ui.getQueryParameters() );

        executeServiceGetRequestForSchema( ui,response,ServiceAction.GET,null );

        return response;
    }

    @POST
    @Path("_reindex")
    @Produces({ MediaType.APPLICATION_JSON,"application/javascript"})
    @RequireApplicationAccess
    @JSONP
    public ApiResponse executePostForReindexing( @Context UriInfo ui, String body,
                                             @QueryParam("callback") @DefaultValue("callback") String callback )
        throws Exception {

        final ReIndexRequestBuilder request =
            createRequest().withApplicationId( services.getApplicationId() ).withCollection(
                String.valueOf( getServiceParameters().get( 0 ) ) );
//
        return executeAndCreateResponse( request, callback );
    }

    @Override
    @Path( RootResource.ENTITY_ID_PATH)
    public AbstractContextResource addIdParameter( @Context UriInfo ui, @PathParam("entityId") PathSegment entityId )
        throws Exception {

        if(logger.isTraceEnabled()){
            logger.trace( "ServiceResource.addIdParameter" );
        }

        UUID itemId = UUID.fromString( entityId.getPath() );

        ServiceParameter.addParameter( getServiceParameters(), itemId );

        addMatrixParams( getServiceParameters(), ui, entityId );

        return getSubResource( CollectionResource.class );
    }


    @Override
    @Path("{itemName}")
    public AbstractContextResource addNameParameter( @Context UriInfo ui, @PathParam("itemName") PathSegment itemName )
        throws Exception {
        if(logger.isTraceEnabled()){
            logger.trace( "ServiceResource.addNameParameter" );
            logger.trace( "Current segment is {}", itemName.getPath() );
        }


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

        return getSubResource( CollectionResource.class );
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
