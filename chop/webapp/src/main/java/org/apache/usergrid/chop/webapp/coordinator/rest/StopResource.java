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
import java.util.LinkedList;
import java.util.Map;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.usergrid.chop.api.RestParams;
import org.apache.usergrid.chop.api.Runner;
import org.apache.usergrid.chop.api.State;
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
 * REST operation to stop an ongoing test.
 */
@Singleton
@Produces( MediaType.APPLICATION_JSON )
@Path( StopResource.ENDPOINT )
public class StopResource extends TestableResource implements RestParams {
    public final static String ENDPOINT = "/stop";

    private static final Logger LOG = LoggerFactory.getLogger( StopResource.class );

    @Inject
    private RunnerCoordinator runnerCoordinator;

    @Inject
    private StackCoordinator stackCoordinator;


    @Inject
    public StopResource() {
        super( ENDPOINT );
    }


    @POST
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( MediaType.APPLICATION_JSON )
    public Response stop(
            @QueryParam( RestParams.COMMIT_ID ) String commitId,
            @QueryParam( RestParams.MODULE_ARTIFACTID ) String artifactId,
            @QueryParam( RestParams.MODULE_GROUPID ) String groupId,
            @QueryParam( RestParams.MODULE_VERSION ) String version,
            @QueryParam( RestParams.USERNAME ) String user,
            @Nullable @QueryParam( TestMode.TEST_MODE_PROPERTY ) String testMode
                        ) {

        if( inTestMode( testMode ) ) {
            LOG.info( "Calling /stop in test mode ..." );
        }
        else {
            LOG.info( "Calling /stop" );
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
                           .entity( "Stack is " + status.toString() + ", cannot stop tests." )
                           .type( MediaType.APPLICATION_JSON )
                           .build();
        }
        /** SetupStackState.SetUp */
        LOG.info( "Stack is set up, checking runner states..." );

        /** Check state of all runners */
        String moduleId = BasicModule.createId( groupId, artifactId, version );
        Collection<Runner> runners = runnerCoordinator.getRunners( user, commitId, moduleId );
        Map<Runner, State> states = runnerCoordinator.getStates( runners );

        Collection<Runner> runningRunners = new LinkedList<Runner>();
        StringBuilder sbToStop = new StringBuilder();
        for ( Runner runner: runners ) {
            State state = states.get( runner );
            if( state == State.RUNNING ) {
                runningRunners.add( runner );
                sbToStop.append( "Runner at " )
                        .append( runner.getUrl() )
                        .append( " is in " )
                        .append( state )
                        .append( " state.\n" );
            }
        }

        if( runningRunners.size() == 0 ) {
            return Response.status( Response.Status.OK )
                           .entity( "No runners found that are still RUNNING, will not stop." )
                           .type( MediaType.APPLICATION_JSON )
                           .build();
        }

        /** Sending stop signal to runners */
        LOG.info( sbToStop.toString() );
        LOG.info( "Sending stop signal to running instances..." );
        states = runnerCoordinator.stop( runningRunners );

        int notStopped = 0;
        StringBuilder sb = new StringBuilder();
        sb.append( "Could not stop all runners.\n" );
        for( Runner runner: runningRunners ) {
            State state = states.get( runner );
            if( state != State.STOPPED ) {
                notStopped++;
                sb.append( "Runner at " )
                  .append( runner.getUrl() )
                  .append( " is in " )
                  .append( state )
                  .append( " state.\n" );
            }
        }

        /*
         * We have to remove, if any Runs have been stored for the current runNumber.
         * Otherwise, we may have inconsistencies in Run and RunResults, since runNumber
         * for one test class for a particular commitId may become different from another test class,
         * when we stop the tests.
         */
        runnerCoordinator.trimIncompleteRuns( runners, commitId );

        // Not all running runners could be stopped
        if( notStopped > 0 ) {
            sb.append( notStopped )
              .append( " out of " )
              .append( runningRunners.size() )
              .append( " running runners could not be stopped." );

            return Response.status( Response.Status.OK )
                           .entity( sb.toString() )
                           .type( MediaType.APPLICATION_JSON )
                           .build();
        }

        return Response.status( Response.Status.CREATED )
                       .entity( "All running runners have been stopped." )
                       .type( MediaType.APPLICATION_JSON )
                       .build();
    }
}
