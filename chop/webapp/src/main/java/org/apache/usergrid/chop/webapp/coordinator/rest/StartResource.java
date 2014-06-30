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
import java.util.Map;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.apache.usergrid.chop.api.RestParams;
import org.apache.usergrid.chop.api.Runner;
import org.apache.usergrid.chop.api.State;
import org.apache.usergrid.chop.stack.CoordinatedStack;
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
 * REST operation to start already set up tests.
 */
@Singleton
@Produces( MediaType.APPLICATION_JSON )
@Path( StartResource.ENDPOINT )
public class StartResource extends TestableResource implements RestParams {
    public final static String ENDPOINT = "/start";

    private static final Logger LOG = LoggerFactory.getLogger( StartResource.class );

    @Inject
    private RunnerCoordinator runnerCoordinator;

    @Inject
    private StackCoordinator stackCoordinator;


    public StartResource() {
        super( ENDPOINT );
    }


    @POST
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( MediaType.APPLICATION_JSON )
    public Response start(
            @QueryParam( RestParams.COMMIT_ID ) String commitId,
            @QueryParam( RestParams.MODULE_ARTIFACTID ) String artifactId,
            @QueryParam( RestParams.MODULE_GROUPID ) String groupId,
            @QueryParam( RestParams.MODULE_VERSION ) String version,
            @QueryParam( RestParams.USERNAME ) String user,
            @Nullable @QueryParam( TestMode.TEST_MODE_PROPERTY ) String testMode
                         ) {

        if( inTestMode( testMode ) ) {
            LOG.info( "Calling /start in test mode ..." );
        }
        else {
            LOG.info( "Calling /start" );
        }
        LOG.info( "  Commit Id: {}", commitId );
        LOG.info( "  Group Id: {}", groupId );
        LOG.info( "  Artifact Id: {}", artifactId );
        LOG.info( "  Version: {}", version );
        LOG.info( "  User: {}", user );

        // Check if the stack is set up first
        SetupStackState status = stackCoordinator.stackStatus( commitId, artifactId, groupId, version, user );

        if( inTestMode( testMode ) ) {
            return Response.status( Response.Status.CREATED )
                           .entity( status )
                           .type( MediaType.APPLICATION_JSON )
                           .build();
        }

        if( ! status.equals( SetupStackState.SetUp ) ) {
            return Response.status( Response.Status.OK )
                           .entity( status.getStackStateMessage() )
                           .type( MediaType.APPLICATION_JSON )
                           .build();
        }
        /** SetupStackState.SetUp */
        LOG.info( "Stack is set up, checking runner states..." );

        /** Check state of all runners */
        String moduleId = BasicModule.createId( groupId, artifactId, version );
        Collection<Runner> runners = runnerCoordinator.getRunners( user, commitId, moduleId );
        Map<Runner, State> states = runnerCoordinator.getStates( runners );

        CoordinatedStack stack = stackCoordinator.findCoordinatedStack( commitId, artifactId, groupId, version, user );

        int notReady = 0;
        StringBuilder sb = new StringBuilder();
        sb.append( "Cannot start tests.\n" );

        // Check also if all runners are registered to coordinator
        if( stack.getRunnerCount() != runners.size() ) {
            sb.append( "Not all runners are registered !!!\n" )
                .append( "Number of registered runners : " )
                .append( runners.size() );
            return Response.status( Response.Status.OK )
                    .entity( sb.toString() )
                    .type( MediaType.APPLICATION_JSON )
                    .build();
        }

        for ( Runner runner: runners ) {
            State state = states.get( runner );
            if( state != State.READY ) {
                notReady++;
                sb.append( "Runner at " )
                  .append( runner.getUrl() )
                  .append( " is in " )
                  .append( state )
                  .append( " state.\n" );
            }
        }

        // Not all runners are ready to start
        if( notReady > 0 ) {
            sb.append( notReady )
              .append( " out of " )
              .append( runners.size() )
              .append( " are not ready." );

            return Response.status( Response.Status.OK )
                           .entity( sb.toString() )
                           .type( MediaType.APPLICATION_JSON )
                           .build();
        }

        /** Sending start signal to runners */
        LOG.info( "Runners are all ready, sending start signal..." );
        states = runnerCoordinator.start( user, commitId, moduleId );

        int notStarted = 0;
        sb = new StringBuilder();
        sb.append( "Could not start all tests.\n" );
        for( Runner runner: runners ) {
            State state = states.get( runner );
            if( state != State.RUNNING ) {
                notStarted++;
                sb.append( "Runner at " )
                  .append( runner.getUrl() )
                  .append( " is in " )
                  .append( state )
                  .append( " state.\n" );
            }
        }
        // Not all runners could be started
        if( notStarted > 0 ) {
            sb.append( notStarted )
              .append( " out of " )
              .append( runners.size() )
              .append( " could not be started." );

            return Response.status( Response.Status.OK )
                           .entity( sb.toString() )
                           .type( MediaType.APPLICATION_JSON )
                           .build();
        }

        return Response.status( Response.Status.CREATED )
                       .entity( "Started chop tests" )
                       .type( MediaType.APPLICATION_JSON )
                       .build();
    }
}
