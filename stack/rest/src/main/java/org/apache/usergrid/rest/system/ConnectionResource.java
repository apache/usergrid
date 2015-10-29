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

package org.apache.usergrid.rest.system;


import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import org.apache.usergrid.corepersistence.service.ConnectionService;
import org.apache.usergrid.corepersistence.service.ConnectionServiceImpl;
import org.apache.usergrid.corepersistence.service.StatusService;
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.index.query.Identifier;
import org.apache.usergrid.persistence.index.utils.UUIDUtils;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;
import org.apache.usergrid.rest.AbstractContextResource;
import org.apache.usergrid.rest.ApiResponse;
import org.apache.usergrid.rest.RootResource;
import org.apache.usergrid.rest.security.annotations.RequireSystemAccess;

import com.google.common.base.Preconditions;
import com.sun.jersey.api.json.JSONWithPadding;

import rx.Observable;
import rx.functions.Action1;
import rx.schedulers.Schedulers;


/**
 * system/index/otherstuff
 */
@Component
@Scope( "singleton" )
@Produces( {
    MediaType.APPLICATION_JSON, "application/javascript", "application/x-javascript", "text/ecmascript",
    "application/ecmascript", "text/jscript"
} )
public class ConnectionResource extends AbstractContextResource {

    private static final Logger logger = LoggerFactory.getLogger( ConnectionResource.class );

    public ConnectionResource() {
        super();
    }


    @RequireSystemAccess
    @POST
    @Path( "dedup/" + RootResource.APPLICATION_ID_PATH )
    public JSONWithPadding rebuildIndexesPost( @PathParam( "applicationId" ) String applicationIdStr,
                                               @QueryParam( "callback" ) @DefaultValue( "callback" ) String callback )
        throws Exception {


        logger.info( "Rebuilding all applications" );

        final UUID applicationId = UUIDUtils.tryGetUUID( applicationIdStr );

        Preconditions.checkNotNull( applicationId, "applicationId must be specified" );

        return executeAndCreateResponse( applicationId, callback );
    }


    @RequireSystemAccess
    @GET
    @Path( "dedup/{jobId: " + Identifier.UUID_REX + "}" )
    public JSONWithPadding rebuildIndexesGet( @PathParam( "jobId" ) String jobId,
                                              @QueryParam( "callback" ) @DefaultValue( "callback" ) String callback )
        throws Exception {
        logger.info( "Getting status for index jobs" );

        Preconditions.checkNotNull( jobId, "query param jobId must not be null" );


        final UUID jobUUID = UUIDUtils.tryGetUUID( jobId );

        final StatusService.JobStatus
            job = getStatusService().getStatus( CpNamingUtils.MANAGEMENT_APPLICATION_ID, jobUUID ).toBlocking().lastOrDefault(
            null );

        Preconditions.checkNotNull( job, "job with id '" + jobId + "' does not exist" );


        return createResult( job, callback );
    }


    private ConnectionService getConnectionService() {
        return injector.getInstance( ConnectionServiceImpl.class );
    }


    private StatusService getStatusService() {
        return injector.getInstance( StatusService.class );
    }



    /**
     * Execute the request and return the response.
     */
    private JSONWithPadding executeAndCreateResponse( final UUID applicationId, final String callback ) {

        final Observable<ApplicationScope> applicationScopeObservable =
            Observable.just( CpNamingUtils.getApplicationScope( applicationId ) );

        final UUID jobId = UUIDGenerator.newTimeUUID();

        final StatusService statusService = getStatusService();
        final ConnectionService connectionService = getConnectionService();

        final AtomicLong count = new AtomicLong( 0 );

        //start de duping and run in the background
        connectionService.deDupeConnections( applicationScopeObservable ).buffer( 10, TimeUnit.SECONDS, 1000 )
                         .doOnNext(buffer -> {


                             final long runningTotal = count.addAndGet(buffer.size());

                             final Map<String, Object> status = new HashMap<String, Object>() {{
                                 put("countProcessed", runningTotal);
                                 put("updatedTimestamp", System.currentTimeMillis());
                             }};

                             statusService.setStatus(CpNamingUtils.MANAGEMENT_APPLICATION_ID, jobId,
                                 StatusService.Status.INPROGRESS, status).toBlocking().lastOrDefault(null);
                         }).doOnSubscribe(() -> {

            statusService.setStatus(CpNamingUtils.MANAGEMENT_APPLICATION_ID,
                jobId, StatusService.Status.STARTED, new HashMap<>()).toBlocking().lastOrDefault(null);

        }).doOnCompleted(() -> {

            final long runningTotal = count.get();

            final Map<String, Object> status = new HashMap<String, Object>() {{
                put("countProcessed", runningTotal);
                put("updatedTimestamp", System.currentTimeMillis());
            }};

            statusService.setStatus(CpNamingUtils.MANAGEMENT_APPLICATION_ID,
                jobId, StatusService.Status.COMPLETE, status).toBlocking().lastOrDefault(null);

        }).doOnError( (throwable) -> {
            logger.error("Error deduping connections", throwable);

            final Map<String, Object> status = new HashMap<String, Object>() {{
                put("error", throwable.getMessage() );
            }};

            statusService.setStatus(CpNamingUtils.MANAGEMENT_APPLICATION_ID,
                jobId, StatusService.Status.FAILED, status).toBlocking().lastOrDefault(null);;

        } ).subscribeOn(Schedulers.newThread()).subscribe();


        final StatusService.JobStatus status =
            new StatusService.JobStatus( jobId, StatusService.Status.STARTED, new HashMap<>(  ) );

        return createResult( status, callback );
    }


    /**
     * Create a response with the specified data.
     * @param jobStatus
     * @param callback
     * @return
     */
    private JSONWithPadding createResult(final StatusService.JobStatus jobStatus, final String callback){

        final ApiResponse response = createApiResponse();

        response.setAction( "de-dup connections" );
        response.setProperty( "status", jobStatus );
        response.setSuccess();

        return new JSONWithPadding( response, callback );
    }
}



