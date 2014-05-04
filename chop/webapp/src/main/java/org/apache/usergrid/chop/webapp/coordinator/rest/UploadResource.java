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


import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;
import javax.mail.internet.MimeMultipart;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.usergrid.chop.client.ssh.Utils;
import org.apache.usergrid.chop.webapp.ChopUiFig;
import org.apache.usergrid.chop.webapp.coordinator.CoordinatorUtils;
import org.apache.usergrid.chop.webapp.dao.model.BasicCommit;
import org.apache.usergrid.chop.webapp.dao.model.BasicRun;
import org.apache.usergrid.chop.webapp.dao.model.BasicRunResult;
import org.apache.usergrid.chop.webapp.elasticsearch.Util;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import org.apache.usergrid.chop.api.Commit;
import org.apache.usergrid.chop.api.Module;
import org.apache.usergrid.chop.api.RestParams;
import org.apache.usergrid.chop.webapp.dao.CommitDao;
import org.apache.usergrid.chop.webapp.dao.ModuleDao;
import org.apache.usergrid.chop.webapp.dao.RunDao;
import org.apache.usergrid.chop.webapp.dao.RunResultDao;
import org.apache.usergrid.chop.webapp.dao.model.BasicModule;
import org.safehaus.jettyjam.utils.TestMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sun.jersey.multipart.FormDataParam;


/**
 * REST operation to upload (a.k.a. deploy) a project war file.
 */
@Singleton
@Produces( MediaType.TEXT_PLAIN )
@Path( UploadResource.ENDPOINT )
public class UploadResource extends TestableResource implements RestParams {
    public final static String ENDPOINT = "/upload";
    public final static String SUCCESSFUL_TEST_MESSAGE = "Test parameters are OK";
    private final static Logger LOG = LoggerFactory.getLogger( UploadResource.class );


    @Inject
    private ChopUiFig chopUiFig;

    @Inject
    private ModuleDao moduleDao;

    @Inject
    private RunDao runDao;

    @Inject
    private RunResultDao runResultDao;

    @Inject
    private CommitDao commitDao;


    public UploadResource() {
        super( ENDPOINT );
    }


    /**
     * Uploads a file to the servlet context temp directory. More for testing proper uploads.
     */
    @POST
    @Consumes( MediaType.MULTIPART_FORM_DATA )
    @Produces( MediaType.APPLICATION_JSON )
    public Response upload( MimeMultipart multipart )
    {
        try {
            String filename = multipart.getBodyPart( 0 ).getContent().toString();
            LOG.warn( "FILENAME: " + filename );
            InputStream in = multipart.getBodyPart( 1 ).getInputStream();
            File tempDir = new File( chopUiFig.getContextTempDir() );
            String fileLocation = new File( tempDir, filename ).getAbsolutePath();
            CoordinatorUtils.writeToFile( in, fileLocation );
        }
        catch ( Exception ex )  {
            LOG.error( "upload", ex );
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).entity( ex.getMessage() ).build();
        }

        return Response.status( Response.Status.CREATED ).entity( "ok" ).build();
    }


    /**
     * Uploads an executable runner jar into a special path in the temp directory for the application.
     */
    @POST
    @Path( "/runner" )
    @Consumes( MediaType.MULTIPART_FORM_DATA )
    @Produces( MediaType.TEXT_PLAIN )
    public Response uploadRunner(

            @FormDataParam( COMMIT_ID ) String commitId,
            @FormDataParam( MODULE_GROUPID ) String groupId,
            @FormDataParam( MODULE_ARTIFACTID ) String artifactId,
            @FormDataParam( MODULE_VERSION ) String version,
            @FormDataParam( USERNAME ) String username,
            @FormDataParam( VCS_REPO_URL ) String vcsRepoUrl,
            @FormDataParam( TEST_PACKAGE ) String testPackage,
            @FormDataParam( MD5 ) String md5,
            @FormDataParam( CONTENT ) InputStream runnerJarStream,
            @Nullable @QueryParam( TestMode.TEST_MODE_PROPERTY ) String testMode

                                ) throws Exception {

        if( inTestMode( testMode ) ) {
            LOG.info( "Calling /upload/runner in test mode ..." );
        }
        else {
            LOG.info( "/upload/runner called ..." );
        }

        LOG.debug( "extracted {} = {}", RestParams.COMMIT_ID, commitId );
        LOG.debug( "extracted {} = {}", RestParams.MODULE_GROUPID, groupId );
        LOG.debug( "extracted {} = {}", RestParams.MODULE_ARTIFACTID, artifactId );
        LOG.debug( "extracted {} = {}", RestParams.MODULE_VERSION, version );
        LOG.debug( "extracted {} = {}", RestParams.USERNAME, username );
        LOG.debug( "extracted {} = {}", RestParams.VCS_REPO_URL, vcsRepoUrl );
        LOG.debug( "extracted {} = {}", RestParams.TEST_PACKAGE, testPackage );
        LOG.debug( "extracted {} = {}", RestParams.MD5, md5 );

        if( inTestMode( testMode ) ) {
            return Response.status( Response.Status.CREATED )
                           .entity( SUCCESSFUL_TEST_MESSAGE )
                           .type( MediaType.TEXT_PLAIN )
                           .build();
        }

        File runnerJar = CoordinatorUtils.getRunnerJar( chopUiFig.getContextPath(), username, groupId, artifactId,
                version, commitId );

        if ( ! runnerJar.getParentFile().exists() ) {
            if ( runnerJar.getParentFile().mkdirs() ) {
                LOG.info( "Created parent directory {} for uploaded runner file", runnerJar.getAbsolutePath() );
            }
            else {
                String errorMessage = "Failed to create parent directory " + runnerJar.getAbsolutePath()
                        + " for uploaded runner file.";
                LOG.error( errorMessage );
                return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).entity( errorMessage ).build();
            }
        }

        // Download and write the file to the proper position on disk & reference
        CoordinatorUtils.writeToFile( runnerJarStream, runnerJar.getAbsolutePath() );

        // - this is bad news because we will get commits of other users :(
        // - we also need to qualify the commit with username, groupId,
        //   and the version of module as well

        Commit commit = null;
        Module module = null;

        List<Commit> commits = commitDao.getByModule( artifactId );
        for ( Commit returnedCommit : commits ) {
            Module commitModule = moduleDao.get( returnedCommit.getModuleId() );
            if ( commitModule.getArtifactId().equals( artifactId ) &&
                 commitModule.getGroupId().equals( groupId ) &&
                 commitModule.getVersion().equals( version ) )
            {
                commit = returnedCommit;
                module = commitModule;
            }
        }

        if ( module == null ) {
            module = new BasicModule( groupId, artifactId, version, vcsRepoUrl, testPackage );
            moduleDao.save( module );
        }

        if ( commit == null ) {
            commit = new BasicCommit( commitId, module.getId(), md5, new Date(), runnerJar.getAbsolutePath() );
            commitDao.save( commit );
        }

        return Response.status( Response.Status.CREATED ).entity( runnerJar.getAbsolutePath() ).build();
    }


    @SuppressWarnings( "unchecked" )
    @POST
    @Path( "/results" )
    @Consumes( MediaType.MULTIPART_FORM_DATA )
    @Produces( MediaType.APPLICATION_JSON )
    public Response uploadResults(
            @QueryParam( RUNNER_HOSTNAME ) String runnerHostName,
            @QueryParam( COMMIT_ID ) String commitId,
            @QueryParam( RUN_ID ) String runId,
            @QueryParam( RUN_NUMBER ) int runNumber,
            @FormDataParam( CONTENT ) InputStream resultsFileInputStream,
            @Nullable @QueryParam( TestMode.TEST_MODE_PROPERTY ) String testMode
                                 ) throws Exception {

        if( inTestMode( testMode ) ) {
            LOG.info( "Calling /upload/results in test mode ..." );
            LOG.info( "{} is {}", RUNNER_HOSTNAME, runnerHostName );
            LOG.info( "{} is {}", COMMIT_ID, commitId );
            LOG.info( "{} is {}", RUN_ID, runId );
            LOG.info( "{} is {}", RUN_NUMBER, runNumber );

            return Response.status( Response.Status.CREATED ).entity( SUCCESSFUL_TEST_MESSAGE ).build();
        }
        else {
            LOG.info( "/upload/results called ..." );
        }

        String message;
        JSONObject object = ( JSONObject ) new JSONParser().parse( new InputStreamReader( resultsFileInputStream ) );
        String testClass = Util.getString( object, "testClass" );

        // First save the summary info
        BasicRun run = new BasicRun( commitId, runnerHostName, runNumber, testClass );
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
                runResult.setFailures( Util.getString( jsonResult, "failures" ) );
            }

            if ( runResultDao.save( runResult ) ) {
                LOG.info( "Saved run result: {}", runResult );
            }
        }

        return Response.status( Response.Status.CREATED ).entity( "TRUE" ).build();
    }
}
