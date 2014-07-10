/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.usergrid.chop.plugin;


import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import org.apache.usergrid.chop.api.Project;
import org.apache.usergrid.chop.api.RestParams;
import org.apache.usergrid.chop.stack.SetupStackState;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.multipart.FormDataBodyPart;
import com.sun.jersey.multipart.FormDataMultiPart;


/** Deploys the jar created by runner goal to coordinator using supplied configuration parameters */
@Mojo(name = "deploy", requiresDependencyResolution = ResolutionScope.TEST,
        requiresDependencyCollection = ResolutionScope.TEST )
public class DeployMojo extends MainMojo {


    public DeployMojo() {

    }


    protected DeployMojo( MainMojo mojo ) {
        this.username = mojo.username;
        this.password = mojo.password;
        this.endpoint = mojo.endpoint;
        this.testPackageBase = mojo.testPackageBase;
        this.certStorePassphrase = mojo.certStorePassphrase;
        this.failIfCommitNecessary = mojo.failIfCommitNecessary;
        this.localRepository = mojo.localRepository;
        this.plugin = mojo.plugin;
        this.project = mojo.project;
        this.runnerCount = mojo.runnerCount;
        this.finalName = mojo.finalName;
    }


    @Override
    public void execute() throws MojoExecutionException {
        initCertStore();

        File source = getRunnerFile();
        if ( source.exists() ) {
            LOG.info( "{} exists!", source.getAbsolutePath() );
        }
        else {
            LOG.info( "{} does not exist.", source.getAbsolutePath() );
        }

        if ( ! isReadyToDeploy() ) {
            LOG.info( "{} is NOT present to upload, calling chop:runner goal now...", RUNNER_JAR );
            RunnerMojo runnerMojo = new RunnerMojo( this );
            runnerMojo.execute();
        }

        if ( ! isReadyToDeploy() ) {
            throw new MojoExecutionException( "Files to be deployed are not ready and chop:runner failed" );
        }

        /** Prepare the POST content for upload */
        Properties props = new Properties();
        try {
            File extractedConfigPropFile = new File( getExtractedRunnerPath(), PROJECT_FILE );
            FileInputStream inputStream = new FileInputStream( extractedConfigPropFile );
            props.load( inputStream );
            inputStream.close();
        }
        catch ( Exception e ) {
            LOG.error( "Error while reading project.properties in runner.jar", e );
            throw new MojoExecutionException( e.getMessage() );
        }


        DefaultClientConfig clientConfig = new DefaultClientConfig();
        Client client = Client.create( clientConfig );
        WebResource resource = client.resource( endpoint ).path( "/upload" );

        ClientResponse uploadResponse = resource.path( "/status" )
                .queryParam( RestParams.COMMIT_ID,
                        props.getProperty( Project.GIT_UUID_KEY ) )
                .queryParam( RestParams.MODULE_ARTIFACTID,
                        props.getProperty( Project.ARTIFACT_ID_KEY ) )
                .queryParam( RestParams.MODULE_GROUPID,
                        props.getProperty( Project.GROUP_ID_KEY ) )
                .queryParam( RestParams.MODULE_VERSION,
                        props.getProperty( Project.PROJECT_VERSION_KEY ) )
                .queryParam( RestParams.USERNAME, username )
                .queryParam( RestParams.VCS_REPO_URL, props.getProperty( Project.GIT_URL_KEY ) )
                .queryParam( RestParams.TEST_PACKAGE,
                        props.getProperty( Project.TEST_PACKAGE_BASE ) )
                .queryParam( RestParams.MD5, props.getProperty( Project.MD5_KEY ) )
                .queryParam( RestParams.RUNNER_COUNT, runnerCount.toString() )
                .type( MediaType.APPLICATION_JSON )
                .accept( MediaType.APPLICATION_JSON )
                .post( ClientResponse.class );

        String uploadResponseMessage = uploadResponse.getEntity( String.class );

        // Check if latest jar exists on coordinator
        if ( uploadResponseMessage.equals( SetupStackState.NotSetUp.getStackStateMessage() )
                || uploadResponseMessage.equals( SetupStackState.SetUp.getStackStateMessage() ) ) {
            LOG.info( uploadResponseMessage );
            return;
        }

        FormDataMultiPart multipart = new FormDataMultiPart();

        try {
            multipart.field( RestParams.COMMIT_ID, props.getProperty( Project.GIT_UUID_KEY ) );
            multipart.field( RestParams.MODULE_GROUPID, props.getProperty( Project.GROUP_ID_KEY ) );
            multipart.field( RestParams.MODULE_ARTIFACTID, props.getProperty( Project.ARTIFACT_ID_KEY ) );
            multipart.field( RestParams.MODULE_VERSION, props.getProperty( Project.PROJECT_VERSION_KEY ) );
            multipart.field( RestParams.USERNAME, username );
            multipart.field( RestParams.VCS_REPO_URL, props.getProperty( Project.GIT_URL_KEY ) );
            multipart.field( RestParams.TEST_PACKAGE, props.getProperty( Project.TEST_PACKAGE_BASE ) );
            multipart.field( RestParams.RUNNER_COUNT, runnerCount.toString() );
            multipart.field( RestParams.MD5, props.getProperty( Project.MD5_KEY ) );

            FileInputStream in = new FileInputStream( source );
            FormDataBodyPart body = new FormDataBodyPart( RestParams.CONTENT, in,
                    MediaType.APPLICATION_OCTET_STREAM_TYPE );
            multipart.bodyPart( body );
        }
        catch ( Exception e ) {
            LOG.error( "Error while preparing upload data", e );
            throw new MojoExecutionException( e.getMessage() );
        }

        /** Upload TODO use chop-client module to talk to the coordinator */
        clientConfig = new DefaultClientConfig();
        client = Client.create( clientConfig );
        resource = client.resource( endpoint ).path( "/upload" );

        ClientResponse resp = resource.path( "/runner" )
                .type( MediaType.MULTIPART_FORM_DATA )
                .accept( MediaType.TEXT_PLAIN )
                .post( ClientResponse.class, multipart );

        String responseMessage = resp.getEntity( String.class );

        if( resp.getStatus() == Response.Status.CREATED.getStatusCode() ) {
            LOG.info( "Runner Jar uploaded to coordinator successfully on path: {}", responseMessage );
        }
        else {
            LOG.error( "Could not upload successfully, HTTP status: ", resp.getStatus() );
            LOG.error( "Error Message: {}", responseMessage );

            throw new MojoExecutionException( "Upload failed" );
        }
    }

}
