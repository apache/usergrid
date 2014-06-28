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


import java.util.Collection;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.usergrid.chop.api.RestParams;
import org.apache.usergrid.chop.api.Runner;
import org.apache.usergrid.chop.stack.SetupStackSignal;
import org.apache.usergrid.chop.stack.SetupStackState;
import org.apache.usergrid.chop.webapp.coordinator.RunnerCoordinator;
import org.apache.usergrid.chop.webapp.coordinator.StackCoordinator;
import org.apache.usergrid.chop.webapp.dao.model.BasicModule;

import org.safehaus.jettyjam.utils.TestMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;


/**
 * REST operation to destroy queried stack's running instances
 */
@Singleton
@Produces( MediaType.APPLICATION_JSON )
@Path( DestroyResource.ENDPOINT)
public class DestroyResource extends TestableResource implements RestParams {

    public final static String ENDPOINT = "/destroy";

    private static final Logger LOG = LoggerFactory.getLogger( DestroyResource.class );

    @Inject
    private StackCoordinator stackCoordinator;

    @Inject
    private RunnerCoordinator runnerCoordinator;


    public DestroyResource() {
        super( ENDPOINT );
    }


    @POST
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( MediaType.APPLICATION_JSON )
    @Path( "/stack" )
    public Response stack(
            @QueryParam( RestParams.COMMIT_ID ) String commitId,
            @QueryParam( RestParams.MODULE_ARTIFACTID ) String artifactId,
            @QueryParam( RestParams.MODULE_GROUPID ) String groupId,
            @QueryParam( RestParams.MODULE_VERSION ) String version,
            @QueryParam( RestParams.USERNAME ) String user,
            @Nullable @QueryParam( TestMode.TEST_MODE_PROPERTY ) String testMode
                         ) {

        if( inTestMode( testMode ) ) {
            LOG.info( "Calling /destroy/stack in test mode ..." );
            LOG.info( "  Commit Id: {}", commitId );
            LOG.info( "  Group Id: {}", groupId );
            LOG.info( "  Artifact Id: {}", artifactId );
            LOG.info( "  Version: {}", version );
            LOG.info( "  User: {}", user );
        }
        else {
            LOG.info( "Calling /destroy/stack" );
        }

        SetupStackState status = stackCoordinator.stackStatus( commitId, artifactId, groupId, version, user );

        if( inTestMode( testMode ) ) {
            return Response.status( Response.Status.CREATED )
                           .entity( status )
                           .type( MediaType.APPLICATION_JSON )
                           .build();
        }

        if( ! status.accepts( SetupStackSignal.DESTROY ) ) {
            return Response.status( Response.Status.OK )
                           .entity( "Stack is " + status.toString() + ", will not destroy." )
                           .type( MediaType.APPLICATION_JSON )
                           .build();
        }

        LOG.debug( "Stack is {}, destroying... ", status.toString() );
        // Unregister runners first
        String moduleId = BasicModule.createId( groupId, artifactId, version );
        Collection<Runner> runners = runnerCoordinator.getRunners( user, commitId, moduleId );
        for( Runner runner: runners ) {
            if( ! runnerCoordinator.unregister( runner.getUrl() ) ) {
                LOG.warn( "Could not unregister runner at {}.", runner.getUrl() );
            }
        }

        // Destroy all stack instances and remove it from stack registry
        stackCoordinator.destroyStack( commitId, artifactId, groupId, version, user );

        return Response.status( Response.Status.CREATED )
                       .entity( "Destroyed the stack" )
                       .type( MediaType.APPLICATION_JSON )
                       .build();
    }
}
