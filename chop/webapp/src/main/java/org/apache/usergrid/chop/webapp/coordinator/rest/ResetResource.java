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


import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.usergrid.chop.api.BaseResult;
import org.apache.usergrid.chop.api.Project;
import org.safehaus.jettyjam.utils.TestMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;


/**
 * REST operation to setup the Stack under test.
 */
@Singleton
@Produces(MediaType.APPLICATION_JSON)
@Path(ResetResource.ENDPOINT)
public class ResetResource {
    public final static String ENDPOINT = "/reset";
    private static final Logger LOG = LoggerFactory.getLogger(ResetResource.class);


    public static final String TEST_PARAM = TestMode.TEST_MODE_PROPERTY;
    public static final String SUCCESS_MESSAGE = "Controller has been reset.";
    public static final String ALREADY_RUNNING_MESSAGE = "Cannot reset when running.";
    public static final String TEST_MESSAGE = "/reset resource called in test mode";
    private static final String RESET_NEEDED_MESSAGE = "A reset is need before starting.";


    private final Project project;


    @Inject
    public ResetResource(Project project) {
        this.project = project;
    }


    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response reset(@QueryParam(TEST_PARAM) String test) {
        BaseResult result = new BaseResult();
        result.setProject(project);
        result.setEndpoint(ENDPOINT);

        if (test != null && (test.equals(TestMode.INTEG.toString()) || test.equals(TestMode.UNIT.toString()))) {
            result.setStatus(true);
            result.setMessage(TEST_MESSAGE);
            LOG.info(TEST_MESSAGE);
            return Response.ok(result, MediaType.APPLICATION_JSON_TYPE).build();
        }
//
//        if ( controller.isRunning() ) {
//            result.setStatus( false );
//            result.setMessage( ALREADY_RUNNING_MESSAGE );
//            LOG.warn( ALREADY_RUNNING_MESSAGE );
//            return Response.status( Response.Status.CONFLICT ).entity( result ).build();
//        }
//
//        if ( controller.needsReset() ) {
//            result.setStatus( false );
//            result.setMessage( RESET_NEEDED_MESSAGE );
//            LOG.warn( RESET_NEEDED_MESSAGE );
//            return Response.status( Response.Status.CONFLICT ).entity( result ).build();
//        }
//
//        controller.start();
//        result.setStatus( true );
//        result.setMessage( SUCCESS_MESSAGE );
//        LOG.info( SUCCESS_MESSAGE );
        return Response.status(Response.Status.OK).entity(result).build();
    }
}