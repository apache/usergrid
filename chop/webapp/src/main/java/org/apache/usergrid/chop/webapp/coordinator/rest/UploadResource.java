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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.annotation.Nullable;
import javax.mail.internet.MimeMultipart;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.usergrid.chop.stack.SetupStackState;
import org.apache.usergrid.chop.webapp.coordinator.StackCoordinator;
import org.safehaus.jettyjam.utils.TestMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.chop.api.Commit;
import org.apache.usergrid.chop.api.Constants;
import org.apache.usergrid.chop.api.Module;
import org.apache.usergrid.chop.api.Project;
import org.apache.usergrid.chop.api.RestParams;
import org.apache.usergrid.chop.webapp.ChopUiFig;
import org.apache.usergrid.chop.webapp.coordinator.CoordinatorUtils;
import org.apache.usergrid.chop.webapp.dao.CommitDao;
import org.apache.usergrid.chop.webapp.dao.ModuleDao;
import org.apache.usergrid.chop.webapp.dao.model.BasicCommit;
import org.apache.usergrid.chop.webapp.dao.model.BasicModule;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sun.jersey.multipart.FormDataParam;


/**
 * REST operation to upload (a.k.a. deploy) a project war file.
 */
@Singleton
@Produces( MediaType.TEXT_PLAIN )
@Path( UploadResource.ENDPOINT )
public class UploadResource extends TestableResource implements RestParams, Constants {
    public final static String ENDPOINT = "/upload";
    private final static Logger LOG = LoggerFactory.getLogger( UploadResource.class );


    @Inject
    private ChopUiFig chopUiFig;

    @Inject
    private ModuleDao moduleDao;

    @Inject
    private CommitDao commitDao;

    @Inject
    private StackCoordinator stackCoordinator;


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

    @POST
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( MediaType.APPLICATION_JSON )
    @Path( "/status" )
    public Response runnerStatus(
            @QueryParam( RestParams.COMMIT_ID ) String commitId,
            @QueryParam( RestParams.MODULE_ARTIFACTID ) String artifactId,
            @QueryParam( RestParams.MODULE_GROUPID ) String groupId,
            @QueryParam( RestParams.MODULE_VERSION ) String version,
            @QueryParam( RestParams.USERNAME ) String username,
            @QueryParam( VCS_REPO_URL ) String vcsRepoUrl,
            @QueryParam( TEST_PACKAGE ) String testPackage,
            @QueryParam( MD5 ) String md5,
            @QueryParam( RestParams.RUNNER_COUNT ) int runnerCount,
            @Nullable @QueryParam( TestMode.TEST_MODE_PROPERTY ) String testMode
                                ) throws IOException {

        if( inTestMode( testMode ) ) {
            LOG.info( "Calling /upload/status in test mode ..." );
        }
        else {
            LOG.info( "Calling /upload/status" );
        }
        File runnerJar = CoordinatorUtils.getRunnerJar( chopUiFig.getContextPath(), username, groupId, artifactId,
                version, commitId );
        SetupStackState status = stackCoordinator.stackStatus( commitId, artifactId, groupId, version, username );


        if( runnerJar.exists() ) {
            String coordinatorRunnerJarMd5 = getCoordinatorJarMd5( runnerJar.getAbsolutePath() );
            if ( isMD5SumsEqual( coordinatorRunnerJarMd5, md5 ) ) {
                return Response.status( Response.Status.OK )
                               .entity( SetupStackState.NotSetUp.getStackStateMessage() )
                               .build();
            }
        }

        return Response.status( Response.Status.OK )
                       .entity( SetupStackState.JarNotFound.getStackStateMessage() )
                       .build();
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
            @FormDataParam( RestParams.RUNNER_COUNT ) int runnerCount,
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
        LOG.debug( "extracted {} = {}", RestParams.RUNNER_COUNT, runnerCount );
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
        if( runnerJar.exists() ) {
            if( runnerJar.delete() ) {
                LOG.info( "Deleted old runner.jar" );
            }
            else {
                LOG.info( "Could not delete old runner.jar" );
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

        stackCoordinator.registerStack( commitId, artifactId, groupId, version, username, runnerCount );

        return Response.status( Response.Status.CREATED ).entity( runnerJar.getAbsolutePath() ).build();
    }


    private boolean isMD5SumsEqual( final String coordinatorRunnerJarMd5Sum, final String localRunnerJarMd5Sum ) {
        return coordinatorRunnerJarMd5Sum.equals( localRunnerJarMd5Sum );
    }


    public String getCoordinatorJarMd5( String coordinatorRunnerJarPath ) {
        InputStream stream;
        URL inputURL;
        Properties props = new Properties();
        String runnerJarProjectPropertiesFile = "jar:file:" + coordinatorRunnerJarPath + "!/" + PROJECT_FILE;

        if ( runnerJarProjectPropertiesFile.startsWith( "jar:" ) ) {
            try {
                inputURL = new URL( runnerJarProjectPropertiesFile );
                JarURLConnection conn = ( JarURLConnection ) inputURL.openConnection();
                stream = conn.getInputStream();
                InputStreamReader reader = new InputStreamReader( stream );
                props.load( reader );
                stream.close();
            } catch ( MalformedURLException e ) {
                LOG.error( "Malformed URL provided:", e );
            } catch ( IOException e ) {
                LOG.error( "Error while reading the file:", e );
            }
        }

        String coordinatorJarMd5 = props.getProperty( Project.MD5_KEY );

        return coordinatorJarMd5;
    }
}
