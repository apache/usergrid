/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one or more
 *  *  contributor license agreements.  The ASF licenses this file to You
 *  * under the Apache License, Version 2.0 (the "License"); you may not
 *  * use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.  For additional information regarding
 *  * copyright in this work, please see the NOTICE file in the top level
 *  * directory of this distribution.
 *
 */

package org.apache.usergrid.rest;


import java.util.Map;
import java.util.UUID;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import org.apache.usergrid.corepersistence.index.ReIndexRequestBuilder;
import org.apache.usergrid.corepersistence.index.ReIndexService;
import org.apache.usergrid.persistence.index.utils.UUIDUtils;
import org.apache.usergrid.rest.security.annotations.RequireSystemAccess;

import com.google.common.base.Preconditions;
import com.sun.jersey.api.json.JSONWithPadding;


/**
 * Classy class class.
 */
@Component
@Scope( "singleton" )
@Produces( {
    MediaType.APPLICATION_JSON, "application/javascript", "application/x-javascript", "text/ecmascript",
    "application/ecmascript", "text/jscript"
} )
public class IndexResource extends AbstractContextResource {

    private static final Logger logger = LoggerFactory.getLogger( IndexResource.class );
    private static final String UPDATED_FIELD = "updated";


    public IndexResource() {
        super();
    }


    @RequireSystemAccess
    @POST
    @Path( "rebuild" )
    public JSONWithPadding rebuildIndexesPost( @QueryParam( "callback" ) @DefaultValue( "callback" ) String callback )
        throws Exception {


        logger.info( "Rebuilding all applications" );

        final ReIndexRequestBuilder request = createRequest();

        return executeAndCreateResponse( request, callback );
    }


    @RequireSystemAccess
    @PUT
    @Path( "rebuild" )
    public JSONWithPadding rebuildIndexesPut( final Map<String, Object> payload,
                                              @QueryParam( "callback" ) @DefaultValue( "callback" ) String callback )
        throws Exception {


        logger.info( "Resuming rebuilding all applications" );
        final ReIndexRequestBuilder request = createRequest();

        return executeResumeAndCreateResponse( payload, request, callback );
    }


    @RequireSystemAccess
    @POST
    @Path( "rebuild/" + RootResource.APPLICATION_ID_PATH )
    public JSONWithPadding rebuildIndexesPut( @PathParam( "applicationId" ) String applicationIdStr,
                                              @QueryParam( "callback" ) @DefaultValue( "callback" ) String callback,
                                              @QueryParam( "delay" ) @DefaultValue( "10" ) final long delay )

        throws Exception {


        logger.info( "Rebuilding application {}", applicationIdStr );


        final UUID appId = UUIDUtils.tryExtractUUID( applicationIdStr );

        final ReIndexRequestBuilder request = createRequest().withApplicationId( appId );

        return executeAndCreateResponse( request, callback );
    }


    @RequireSystemAccess
    @PUT
    @Path( "rebuild/" + RootResource.APPLICATION_ID_PATH )
    public JSONWithPadding rebuildIndexesPut( final Map<String, Object> payload,
                                              @PathParam( "applicationId" ) String applicationIdStr,
                                              @QueryParam( "callback" ) @DefaultValue( "callback" ) String callback,
                                              @QueryParam( "delay" ) @DefaultValue( "10" ) final long delay )

        throws Exception {

        logger.info( "Resuming rebuilding application {}", applicationIdStr );

        final UUID appId = UUIDUtils.tryExtractUUID( applicationIdStr );

        final ReIndexRequestBuilder request = createRequest().withApplicationId( appId );

        return executeResumeAndCreateResponse( payload, request, callback );
    }


    @RequireSystemAccess
    @POST
    @Path( "rebuild/" + RootResource.APPLICATION_ID_PATH + "/{collectionName}" )
    public JSONWithPadding rebuildIndexesPost( @PathParam( "applicationId" ) final String applicationIdStr,
                                               @PathParam( "collectionName" ) final String collectionName,
                                               @QueryParam( "reverse" ) @DefaultValue( "false" ) final Boolean reverse,
                                               @QueryParam( "callback" ) @DefaultValue( "callback" ) String callback )
        throws Exception {


        logger.info( "Rebuilding collection {} in  application {}", collectionName, applicationIdStr );

        final UUID appId = UUIDUtils.tryExtractUUID( applicationIdStr );

        final ReIndexRequestBuilder request =
            createRequest().withApplicationId( appId ).withCollection( collectionName );

        return executeAndCreateResponse( request, callback );
    }


    @RequireSystemAccess
    @PUT
    @Path( "rebuild/" + RootResource.APPLICATION_ID_PATH + "/{collectionName}" )
    public JSONWithPadding rebuildIndexesPut( final Map<String, Object> payload,
                                              @PathParam( "applicationId" ) final String applicationIdStr,
                                              @PathParam( "collectionName" ) final String collectionName,
                                              @QueryParam( "reverse" ) @DefaultValue( "false" ) final Boolean reverse,
                                              @QueryParam( "callback" ) @DefaultValue( "callback" ) String callback )
        throws Exception {

        logger.info( "Resuming rebuilding collection {} in  application {}", collectionName, applicationIdStr );

        final UUID appId = UUIDUtils.tryExtractUUID( applicationIdStr );

        final ReIndexRequestBuilder request =
            createRequest().withApplicationId( appId ).withCollection( collectionName );

        return executeResumeAndCreateResponse( payload, request, callback );
    }


    @RequireSystemAccess
    @POST
    @Path( "rebuild/management" )
    public JSONWithPadding rebuildInternalIndexesPost(
        @QueryParam( "callback" ) @DefaultValue( "callback" ) String callback ) throws Exception {


        final UUID managementAppId = emf.getManagementAppId();

        logger.info( "Rebuilding management application with id {} ", managementAppId );
        final ReIndexRequestBuilder request = createRequest().withApplicationId( managementAppId );

        return executeAndCreateResponse( request, callback );
    }


    @RequireSystemAccess
    @POST
    @Path( "rebuild/management" )
    public JSONWithPadding rebuildInternalIndexesPut( final Map<String, Object> payload,
                                                      @QueryParam( "callback" ) @DefaultValue( "callback" )
                                                      String callback ) throws Exception {


        final UUID managementAppId = emf.getManagementAppId();

        logger.info( "Resuming rebuilding management application with id {} ", managementAppId );
        final ReIndexRequestBuilder request = createRequest().withApplicationId( managementAppId );

        return executeResumeAndCreateResponse( payload, request, callback );
    }


    @RequireSystemAccess
    @POST
    public JSONWithPadding addIndex( @Context UriInfo ui, Map<String, Object> config,
                                     @QueryParam( "callback" ) @DefaultValue( "callback" ) String callback )
        throws Exception {

        Preconditions
            .checkNotNull( config, "Payload for config is null, please pass {replicas:int, shards:int} in body" );

        ApiResponse response = createApiResponse();

        if ( !config.containsKey( "replicas" ) || !config.containsKey( "shards" ) ||
            !( config.get( "replicas" ) instanceof Integer ) || !( config.get( "shards" ) instanceof Integer ) ) {
            throw new IllegalArgumentException( "body must contains 'replicas' of type int and 'shards' of type int" );
        }

        if ( !config.containsKey( "indexSuffix" ) ) {
            throw new IllegalArgumentException( "Please add an indexSuffix to your post" );
        }


        emf.addIndex( config.get( "indexSuffix" ).toString(), ( int ) config.get( "shards" ),
            ( int ) config.get( "replicas" ), ( String ) config.get( "writeConsistency" ) );
        response.setAction( "Add index to alias" );

        return new JSONWithPadding( response, callback );
    }


    private ReIndexService getReIndexService() {
        return injector.getInstance( ReIndexService.class );
    }


    private ReIndexRequestBuilder createRequest() {
        return createRequest();
    }


    private JSONWithPadding executeResumeAndCreateResponse( final Map<String, Object> payload,
                                                            final ReIndexRequestBuilder request,
                                                            final String callback ) {

        Preconditions.checkArgument( payload.containsKey( UPDATED_FIELD ),
            "You must specified the field \"updated\" in the payload" );

        //add our updated timestamp to the request
        if ( !payload.containsKey( UPDATED_FIELD ) ) {
            final long timestamp = ( long ) payload.get( UPDATED_FIELD );
            request.withStartTimestamp( timestamp );
        }

        return executeAndCreateResponse( request, callback );
    }


    /**
     * Execute the request and return the response.
     */
    private JSONWithPadding executeAndCreateResponse( final ReIndexRequestBuilder request, final String callback ) {


        final ReIndexService.ReIndexStatus status = getReIndexService().rebuildIndex( request );

        final ApiResponse response = createApiResponse();

        response.setAction( "rebuild indexes" );
        response.setProperty( "jobId", status.getJobId() );
        response.setProperty( "status", status.getStatus() );
        response.setProperty( "lastUpdatedEpoch", status.getLastUpdated() );
        response.setProperty( "numberQueued", status.getNumberProcessed() );
        response.setSuccess();

        return new JSONWithPadding( response, callback );
    }
}
