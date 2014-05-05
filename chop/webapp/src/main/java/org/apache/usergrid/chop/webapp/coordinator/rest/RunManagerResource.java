/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.usergrid.chop.webapp.coordinator.rest;


import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.usergrid.chop.api.RestParams;
import org.apache.usergrid.chop.webapp.coordinator.StackCoordinator;
import org.apache.usergrid.chop.webapp.dao.RunDao;
import org.elasticsearch.indices.IndexMissingException;
import org.safehaus.jettyjam.utils.TestMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;


/**
 * REST operation to setup the Stack under test.
 */
@Singleton
@Path(RunManagerResource.ENDPOINT)
public class RunManagerResource extends TestableResource {
    public final static String ENDPOINT = "/run";
    private static final Logger LOG = LoggerFactory.getLogger(RunManagerResource.class);


    @Inject
    private StackCoordinator stackCoordinator;

    @Inject
    private RunDao runDao;


    protected RunManagerResource() {
        super(ENDPOINT);
    }


    @GET
    @Path("/next")
    @Produces(MediaType.APPLICATION_JSON)
    public Response next(

            @QueryParam(RestParams.COMMIT_ID) String commitId,
            @Nullable @QueryParam(TestMode.TEST_MODE_PROPERTY) String testMode

    ) throws Exception {
        int next;

        if (inTestMode(testMode)) {
            LOG.info("Calling /run/next in test mode ...");
            return Response.ok(0).build();
        }

        LOG.info("Calling /run/next ...");
        Preconditions.checkNotNull(commitId, "The commitId should not be null.");

        try {
            next = runDao.getNextRunNumber(commitId);
        } catch (IndexMissingException e) {
            LOG.warn("Got an index missing exception while looking up the next run number.");
            return Response.ok(0).build();
        } catch (Exception e) {
            LOG.error("Failed to get the next run number for commitId = " + commitId, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }

        LOG.info("Next run number to return is {}", next);
        return Response.ok(next).build();
    }


    @GET
    @Path("/completed")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response completed(

            @QueryParam(RestParams.RUNNER_HOSTNAME) String runnerHost,
            @QueryParam(RestParams.COMMIT_ID) String commitId,
            @QueryParam(RestParams.RUN_NUMBER) Integer runNumber,
            @QueryParam(RestParams.TEST_CLASS) String testClass

    ) throws Exception {
        LOG.warn("Calling completed ...");
        return Response.status(Response.Status.CREATED).entity("FALSE").build();
    }


    @POST // - this should perform an upload
    @Path("/store")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response store(@QueryParam(RestParams.RUNNER_HOSTNAME) String runnerHostname) {
        LOG.warn("Calling store ...");
        return Response.status(Response.Status.CREATED).entity("FALSE").build();
    }
}
