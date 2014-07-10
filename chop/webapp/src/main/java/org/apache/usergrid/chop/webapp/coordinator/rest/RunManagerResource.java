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


import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Iterator;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sun.jersey.multipart.FormDataParam;

import org.apache.usergrid.chop.api.RestParams;
import org.apache.usergrid.chop.api.Run;
import org.apache.usergrid.chop.api.Runner;
import org.apache.usergrid.chop.webapp.dao.RunDao;
import org.apache.usergrid.chop.webapp.dao.RunResultDao;
import org.apache.usergrid.chop.webapp.dao.RunnerDao;
import org.apache.usergrid.chop.webapp.dao.model.BasicModule;
import org.apache.usergrid.chop.webapp.dao.model.BasicRun;
import org.apache.usergrid.chop.webapp.dao.model.BasicRunResult;
import org.apache.usergrid.chop.webapp.elasticsearch.Util;

import org.elasticsearch.indices.IndexMissingException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.safehaus.jettyjam.utils.TestMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
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
@Path( RunManagerResource.ENDPOINT )
public class RunManagerResource extends TestableResource implements RestParams {
    public final static String ENDPOINT = "/run";
    private static final Logger LOG = LoggerFactory.getLogger( RunManagerResource.class );

    @Inject
    private RunDao runDao;

    @Inject
    private RunResultDao runResultDao;

    @Inject
    private RunnerDao runnerDao;


    protected RunManagerResource() {
        super( ENDPOINT );
    }


    @GET
    @Path( "/next" )
    @Produces( MediaType.APPLICATION_JSON )
    public Response next(

            @QueryParam( USERNAME ) String username,
            @QueryParam( COMMIT_ID ) String commitId,
            @QueryParam( MODULE_GROUPID ) String groupId,
            @QueryParam( MODULE_ARTIFACTID ) String artifactId,
            @QueryParam( MODULE_VERSION ) String version,
            @Nullable @QueryParam( TestMode.TEST_MODE_PROPERTY ) String testMode

    ) throws Exception {
        int next;

        if ( inTestMode( testMode ) ) {
            LOG.info( "Calling /run/next in test mode ..." );
            return Response.ok( 1 ).build();
        }

        LOG.info( "Calling /run/next ..." );
        Preconditions.checkNotNull( commitId, "The commitId should not be null." );

        try {
            next = runDao.getNextRunNumber( commitId );
        }
        catch ( IndexMissingException e ) {
            LOG.warn( "Got an index missing exception while looking up the next run number." );
            return Response.ok( 0 ).build();
        }
        catch ( Exception e ) {
            LOG.error( "Failed to get the next run number for commitId = " + commitId, e );
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).entity( e.getMessage() ).build();
        }

        LOG.info( "Next run number to return is {}", next );
        return Response.ok( next ).build();
    }


    @GET
    @Path( "/completed" )
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( MediaType.APPLICATION_JSON )
    public Response completed(

            @QueryParam( USERNAME ) String username,
            @QueryParam( RUNNER_HOSTNAME ) String runnerHost,
            @QueryParam( COMMIT_ID ) String commitId,
            @QueryParam( MODULE_GROUPID ) String groupId,
            @QueryParam( MODULE_ARTIFACTID ) String artifactId,
            @QueryParam( MODULE_VERSION ) String version,
            @QueryParam( RUN_NUMBER ) Integer runNumber,
            @QueryParam( TEST_CLASS ) String testClass,
            @Nullable @QueryParam( TestMode.TEST_MODE_PROPERTY ) String testMode

    ) throws Exception {
        LOG.warn( "Calling completed ..." );

        if( inTestMode( testMode ) ) {
            LOG.info( "Calling /run/completed in test mode ..." );
            LOG.info( "{} is {}", RUNNER_HOSTNAME, runnerHost );
            LOG.info( "{} is {}", COMMIT_ID, commitId );
            LOG.info( "{} is {}", MODULE_GROUPID, groupId );
            LOG.info( "{} is {}", MODULE_ARTIFACTID, artifactId );
            LOG.info( "{} is {}", MODULE_VERSION, version );
            LOG.info( "{} is {}", RUN_NUMBER, runNumber );
            LOG.info( "{} is {}", TEST_CLASS, testClass );

            return Response.status( Response.Status.CREATED ).entity( true ).build();
        }
        LOG.info( "/run/completed called ..." );

        String moduleId = BasicModule.createId( groupId, artifactId, version );
        Collection<Runner> runners = runnerDao.getRunners( username, commitId, moduleId );
        Collection<Run> runs = runDao.getMap( commitId, runNumber, testClass, runners ).values() ;

        Boolean allFinished = runs.size() == runners.size();

        return Response.status( Response.Status.CREATED ).entity( allFinished ).build();
    }


    @SuppressWarnings( "unchecked" )
    @POST
    @Path( "/store" )
    @Consumes( MediaType.MULTIPART_FORM_DATA )
    @Produces( MediaType.APPLICATION_JSON )
    public Response store(
            @QueryParam( RUNNER_HOSTNAME ) String runnerHostName,
            @QueryParam( COMMIT_ID ) String commitId,
            @QueryParam( RUN_ID ) String runId,
            @QueryParam( RUN_NUMBER ) int runNumber,
            @FormDataParam( CONTENT ) InputStream resultsFileInputStream,
            @Nullable @QueryParam( TestMode.TEST_MODE_PROPERTY ) String testMode
                                 ) throws Exception {

        if( inTestMode( testMode ) ) {
            LOG.info( "Calling /run/store in test mode ..." );
            LOG.info( "{} is {}", RUNNER_HOSTNAME, runnerHostName );
            LOG.info( "{} is {}", COMMIT_ID, commitId );
            LOG.info( "{} is {}", RUN_ID, runId );
            LOG.info( "{} is {}", RUN_NUMBER, runNumber );

            return Response.status( Response.Status.CREATED ).entity( SUCCESSFUL_TEST_MESSAGE ).build();
        }
        LOG.info( "/run/store called ..." );

        String message;
        JSONObject object = ( JSONObject ) new JSONParser().parse( new InputStreamReader( resultsFileInputStream ) );
        String testClass = Util.getString( object, "testClass" );

        // First save the summary info
        BasicRun run = new BasicRun( runId, commitId, runnerHostName, runNumber, testClass );
        run.copyJson( object );
        if ( runDao.save( run ) ) {
            LOG.info( "Created new Run {} ", run );
        }
        else {
            message = "Failed to create new Run";
            LOG.warn( message );
            throw new IllegalStateException( message );
        }

        // Here is the list of BasicRunResults
        JSONArray runResults = ( JSONArray ) object.get( "runResults" );
        Iterator<JSONObject> iterator = runResults.iterator();
        while( iterator.hasNext() ) {
            JSONObject jsonResult = iterator.next();

            int failureCount = Util.getInt( jsonResult, "failureCount" );
            BasicRunResult runResult = new BasicRunResult(
                    runId,
                    Util.getInt( jsonResult, "runCount"),
                    Util.getInt( jsonResult, "runTime" ),
                    Util.getInt( jsonResult, "ignoreCount" ),
                    failureCount
            );
            if( failureCount != 0 ) {
                try {
                    for( Object result : runResults ) {
                        JSONObject failures = ( JSONObject ) result;
                        JSONArray obj = ( JSONArray ) failures.get( "failures" );
                        runResult.setFailures( obj.toJSONString() );
                        LOG.info( "Saving run results into Elastic Search." );
                    }
                }catch ( Exception e ){
                    LOG.warn( "Could not serialize runResults JSON object", e );
                }
            }

            if ( runResultDao.save( runResult ) ) {
                LOG.info( "Saved run result.");
            }
        }

        return Response.status( Response.Status.CREATED ).entity( "TRUE" ).build();
    }
}
