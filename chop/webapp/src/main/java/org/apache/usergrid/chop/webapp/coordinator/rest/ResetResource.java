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
 * REST operation to setup the Stack under test.
 */
@Singleton
@Produces( MediaType.APPLICATION_JSON )
@Path( ResetResource.ENDPOINT )
public class ResetResource extends TestableResource implements RestParams {
    public final static String ENDPOINT = "/reset";

    private static final Logger LOG = LoggerFactory.getLogger( ResetResource.class );

    @Inject
    private RunnerCoordinator runnerCoordinator;

    @Inject
    private StackCoordinator stackCoordinator;


    @Inject
    public ResetResource() {
        super( ENDPOINT );
    }


    @POST
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( MediaType.APPLICATION_JSON )
    public Response reset(
            @QueryParam( RestParams.COMMIT_ID ) String commitId,
            @QueryParam( RestParams.MODULE_ARTIFACTID ) String artifactId,
            @QueryParam( RestParams.MODULE_GROUPID ) String groupId,
            @QueryParam( RestParams.MODULE_VERSION ) String version,
            @QueryParam( RestParams.USERNAME ) String user,
            @Nullable @QueryParam( TestMode.TEST_MODE_PROPERTY ) String testMode
                         ) {

        if( inTestMode( testMode ) ) {
            LOG.info( "Calling /reset in test mode ..." );
        }
        else {
            LOG.info( "Calling /reset" );
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
                           .entity( "Stack is " + status.toString() + ", cannot reset tests." )
                           .type( MediaType.APPLICATION_JSON )
                           .build();
        }
        /** SetupStackState.SetUp */
        LOG.info( "Stack is set up, checking runner states..." );

        /** Check state of all runners */
        String moduleId = BasicModule.createId( groupId, artifactId, version );
        Collection<Runner> runners = runnerCoordinator.getRunners( user, commitId, moduleId );
        Map<Runner, State> states = runnerCoordinator.getStates( runners );

        Collection<Runner> stoppedRunners = new LinkedList<Runner>();
        StringBuilder sbToReset = new StringBuilder();
        StringBuilder sbFail = new StringBuilder();
        sbFail.append( "Cannot reset tests.\n" );
        int notAvailable = 0;
        for ( Runner runner: runners ) {
            State state = states.get( runner );
            if( state == State.STOPPED ) {
                stoppedRunners.add( runner );
                sbToReset.append( "Runner at " )
                         .append( runner.getUrl() )
                         .append( " is in " )
                         .append( state )
                         .append( " state.\n" );
            }
            else if( state != State.READY ) {
                notAvailable++;
                sbFail.append( "Runner at " )
                      .append( runner.getUrl() )
                      .append( " is in " )
                      .append( state )
                      .append( " state.\n" );
            }
        }
        // Some runners are still running or inactive
        if( notAvailable > 0 ) {
            sbFail.append( notAvailable )
                  .append( " out of " )
                  .append( runners.size() )
                  .append( " are still running or inactive." );

            return Response.status( Response.Status.OK )
                           .entity( sbFail.toString() )
                           .type( MediaType.APPLICATION_JSON )
                           .build();
        }
        if( stoppedRunners.size() == 0 ) {
            return Response.status( Response.Status.OK )
                           .entity( "No runners are in STOPPED state, will not reset." )
                           .type( MediaType.APPLICATION_JSON )
                           .build();
        }

        /** Sending reset signal to runners */
        LOG.info( sbToReset.toString() );
        LOG.info( "Sending reset signal to stopped instances..." );
        states = runnerCoordinator.reset( stoppedRunners );

        int notReset = 0;
        StringBuilder sb = new StringBuilder();
        sb.append( "Could not reset all runners.\n" );
        for( Runner runner: stoppedRunners ) {
            State state = states.get( runner );
            if( state != State.READY ) {
                notReset++;
                sb.append( "Runner at " )
                  .append( runner.getUrl() )
                  .append( " is in " )
                  .append( state )
                  .append( " state.\n" );
            }
        }
        // Not all stopped runners could be reset
        if( notReset > 0 ) {
            sb.append( notReset )
              .append( " out of " )
              .append( stoppedRunners.size() )
              .append( " stopped runners could not be reset." );

            return Response.status( Response.Status.OK )
                           .entity( sb.toString() )
                           .type( MediaType.APPLICATION_JSON )
                           .build();
        }

        return Response.status( Response.Status.CREATED )
                       .entity( "All stopped runners have been reset." )
                       .type( MediaType.APPLICATION_JSON )
                       .build();
    }
}