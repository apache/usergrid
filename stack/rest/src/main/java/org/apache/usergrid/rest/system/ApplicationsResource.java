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
package org.apache.usergrid.rest.system;

import com.fasterxml.jackson.jaxrs.json.annotation.JSONP;
import org.apache.usergrid.corepersistence.service.StatusService;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;
import org.apache.usergrid.rest.AbstractContextResource;
import org.apache.usergrid.rest.ApiResponse;
import org.apache.usergrid.rest.security.annotations.RequireSystemAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Classy class class.
 */
@Component
@Scope( "singleton" )
@Produces( {
    MediaType.APPLICATION_JSON, "application/javascript", "application/x-javascript", "text/ecmascript",
    "application/ecmascript", "text/jscript"
} )
public class ApplicationsResource extends AbstractContextResource {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationsResource.class);


    public ApplicationsResource() {
        logger.info( "ApplicationsResource initialized" );
    }

    @RequireSystemAccess
    @DELETE
    @JSONP
    @Path( "{applicationId}" )
    public ApiResponse clearApplication(
        @Context UriInfo ui,
        @PathParam("applicationId") UUID applicationId,
        @QueryParam( "confirmApplicationName" ) String confirmApplicationName,
        @QueryParam( "limit" ) int limit,
        @QueryParam( "callback" ) @DefaultValue( "callback" ) String callback )

        throws Exception {

        if(confirmApplicationName == null){
            throw new IllegalArgumentException("please make add a QueryString for confirmApplicationName");
        }

        final UUID jobId = UUIDGenerator.newTimeUUID();

        final EntityManager em =  emf.getEntityManager(applicationId);
        final String name =  em.getApplication().getApplicationName();
        if(!name.toLowerCase().equals(confirmApplicationName.toLowerCase())){
            throw new IllegalArgumentException(
                "confirmApplicationName: " + confirmApplicationName + " does not equal " + name);
        }
        final StatusService statusService = injector.getInstance(StatusService.class);

        final ApiResponse response = createApiResponse();

        response.setAction( "clear application" );

        logger.info("clearing up application");

        final Thread delete = new Thread() {

            @Override
            public void run() {
                final AtomicInteger itemsDeleted = new AtomicInteger(0);
                try {
                    management.deleteAllEntities(applicationId, limit)
                        .map(id -> itemsDeleted.incrementAndGet())
                        .doOnNext(count -> {
                            if( count % 100 == 0 ){
                                Map<String,Object> map = new LinkedHashMap<>();
                                map.put("count",itemsDeleted.intValue());
                                final StatusService statusService = injector.getInstance(StatusService.class);
                                statusService.setStatus(applicationId, jobId, StatusService.Status.INPROGRESS,map)
                                    .subscribe();//do not want to throw this exception
                            }
                        })
                        .doOnCompleted(() -> {
                            Map<String, Object> map = new LinkedHashMap<>();
                            map.put("count", itemsDeleted.intValue());
                            final StatusService statusService = injector.getInstance(StatusService.class);
                            statusService.setStatus(applicationId, jobId, StatusService.Status.COMPLETE, map)
                                .toBlocking().lastOrDefault(null);//want to rethrow this exception
                        })
                        .toBlocking().lastOrDefault(null);//expecting exception to be caught if job fails

                } catch ( Exception e ) {
                    Map<String,Object> map = new LinkedHashMap<>();
                    map.put("exception",e);
                    try {
                        statusService.setStatus(applicationId, jobId, StatusService.Status.FAILED, map).toBlocking().lastOrDefault(null);//leave as subscribe if fails retry
                    }catch (Exception subE){
                        logger.error("failed to update status "+jobId,subE);
                    }
                    logger.error( "Failed to delete appid:"+applicationId + " jobid:"+jobId+" count:"+itemsDeleted, e );
                }
            }
        };

        delete.setName("Delete for app : " + applicationId + " job: " + jobId);
        delete.setDaemon(true);
        delete.start();

        try {
            //should throw exception if can't start
            statusService.setStatus(applicationId, jobId, StatusService.Status.STARTED, new LinkedHashMap<>()).toBlocking().lastOrDefault(null);
        }catch (Exception e){
            logger.error("failed to set status for " + jobId, e);
        }
        Map<String,Object> data = new HashMap<>();
        data.put("jobId",jobId);
        data.put("status",StatusService.Status.STARTED);
        response.setData(data);
        response.setSuccess();
        return response;
    }

    @RequireSystemAccess
    @GET
    @Path( "{applicationId}/job/{jobId}" )
    public ApiResponse getStatus(
        @Context UriInfo ui,
        @PathParam("applicationId") UUID applicationId,
        @PathParam("jobId") UUID jobId,
        @QueryParam( "callback" ) @DefaultValue( "callback" ) String callback ) throws Exception{

        final StatusService statusService = injector.getInstance(StatusService.class);

        final ApiResponse response = createApiResponse();

        response.setAction( "clear application" );

        StatusService.JobStatus jobStatus = statusService.getStatus(applicationId, jobId).toBlocking().lastOrDefault(null);

        Map<String,Object> data = new HashMap<>();
        data.put("jobId",jobId);
        data.put( "status", jobStatus.getStatus().toString() );
        data.put( "metadata", jobStatus.getData() );
        response.setData(data);
        response.setSuccess();
        return response;
    }

}
