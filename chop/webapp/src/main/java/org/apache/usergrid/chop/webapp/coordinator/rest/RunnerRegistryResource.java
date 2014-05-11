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
import org.apache.usergrid.chop.api.Module;
import org.apache.usergrid.chop.api.RestParams;
import org.apache.usergrid.chop.api.Runner;
import org.apache.usergrid.chop.webapp.coordinator.RunnerCoordinator;
import org.apache.usergrid.chop.webapp.dao.ModuleDao;
import org.apache.usergrid.chop.webapp.dao.model.BasicModule;
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

import java.util.Collection;
import java.util.Collections;


/**
 * REST operation to setup the Stack under test.
 */
@Singleton
@Produces( MediaType.APPLICATION_JSON )
@Consumes( MediaType.APPLICATION_JSON )
@Path( RunnerRegistryResource.ENDPOINT )
public class RunnerRegistryResource extends TestableResource {
    public final static String ENDPOINT = "/runners";

    private static final Logger LOG = LoggerFactory.getLogger( RunnerRegistryResource.class );

    @Inject
    private ModuleDao moduleDao;

    @Inject
    private RunnerCoordinator runnerCoordinator;


    public RunnerRegistryResource() {
        super( ENDPOINT );
    }


    @GET
    @Path( "/list" )
    public Response list(

            @QueryParam( RestParams.USERNAME ) String user,
            @QueryParam( RestParams.MODULE_ARTIFACTID ) String artifactId,
            @QueryParam( RestParams.MODULE_GROUPID ) String groupId,
            @QueryParam( RestParams.MODULE_VERSION ) String version,
            @QueryParam( RestParams.COMMIT_ID ) String commitId,
            @Nullable @QueryParam( TestMode.TEST_MODE_PROPERTY ) String testMode

    ) throws Exception {
        Collection<Runner> runnerList = Collections.emptyList();

        if ( inTestMode( testMode ) ) {
            LOG.info( "Calling /runners/list in test mode ..." );
            return Response.ok( runnerList ).build();
        }
        LOG.info( "Calling /runners/list" );

        Preconditions.checkNotNull( user, "The 'user' request parameter MUST NOT be null." );
        Preconditions.checkNotNull( artifactId, "The 'artifactId' request parameter MUST NOT be null." );
        Preconditions.checkNotNull( groupId, "The 'groupId' request parameter MUST NOT be null." );
        Preconditions.checkNotNull( version, "The 'version' request parameter MUST NOT be null." );
        Preconditions.checkNotNull( commitId, "The 'commitId' request parameter MUST NOT be null." );

        String moduleId = BasicModule.createId( groupId, artifactId, version );
        Module inStore = moduleDao.get( moduleId );
        if ( inStore == null ) {
            LOG.warn( "Returning empty runner list for request associated with non-existent module: {}",
                    groupId + "." + artifactId + "-" + version );
            return Response.ok( runnerList ).build();
        }

        runnerList = runnerCoordinator.getRunners( user, commitId, moduleId );

        return Response.status( Response.Status.CREATED ).entity( runnerList ).build();
    }


    @POST
    @Path( "/register" )
    @Produces( MediaType.APPLICATION_JSON )
    @Consumes( MediaType.APPLICATION_JSON )
    public Response register(

            @QueryParam( RestParams.USERNAME ) String user,
            @QueryParam( RestParams.MODULE_ARTIFACTID ) String artifactId,
            @QueryParam( RestParams.MODULE_GROUPID ) String groupId,
            @QueryParam( RestParams.MODULE_VERSION ) String version,
            @QueryParam( RestParams.TEST_PACKAGE ) String testPackageBase,
            @QueryParam( RestParams.COMMIT_ID ) String commitId,
            @QueryParam( RestParams.VCS_REPO_URL ) String vcsRepoUrl,
            @Nullable @QueryParam( TestMode.TEST_MODE_PROPERTY ) String testMode,
            Runner runner

    ) throws Exception {
        if ( inTestMode( testMode ) ) {
            LOG.info( "Calling /runners/register in test mode ..." );
            return Response.ok( false ).build();
        }

        Preconditions.checkNotNull( user, "The 'user' request parameter MUST NOT be null." );
        Preconditions.checkNotNull( artifactId, "The 'artifactId' request parameter MUST NOT be null." );
        Preconditions.checkNotNull( groupId, "The 'groupId' request parameter MUST NOT be null." );
        Preconditions.checkNotNull( version, "The 'version' request parameter MUST NOT be null." );
        Preconditions.checkNotNull( version, "The 'testPackageBase' request parameter MUST NOT be null." );
        Preconditions.checkNotNull( commitId, "The 'commitId' request parameter MUST NOT be null." );
        Preconditions.checkNotNull( vcsRepoUrl, "The 'vcsRepoUrl' request parameter MUST NOT be null." );
        Preconditions.checkNotNull( runner, "The 'runner' request parameter MUST NOT be null." );

        String moduleId = BasicModule.createId( groupId, artifactId, version );
        Module module = moduleDao.get( moduleId );
        if ( module == null ) {
            LOG.warn( "Module {} does not exist for runner {}. Creating and storing it ...",
                    groupId + "." + artifactId + "-" + version, runner );
            module = new BasicModule( groupId, artifactId, version, vcsRepoUrl, testPackageBase );
            moduleDao.save( module );
        }

        boolean result = runnerCoordinator.register( user, commitId, moduleId, runner );
        if ( result ) {
            LOG.info( "Registered runner at {} for commit {}", runner.getUrl(), commitId );
        }
        else {
            LOG.warn( "Failed to register runner at {}", runner.getUrl() );
        }
        return Response.ok( result ).build();
    }


    @POST
    @Path( "/unregister" )
    public Response unregister(

            @QueryParam( RestParams.RUNNER_URL ) String runnerUrl,
            @Nullable @QueryParam( TestMode.TEST_MODE_PROPERTY ) String testMode

                              ) {

        if ( inTestMode( testMode ) ) {
            LOG.info( "Calling /runners/unregister ..." );
            return Response.ok( false ).build();
        }

        Preconditions.checkNotNull( runnerUrl, "The 'runnerUrl' MUST NOT be null." );

        LOG.info( "Calling /runners/unregister ..." );

        boolean result = runnerCoordinator.unregister( runnerUrl );
        if( result ) {
            LOG.info( "Unregistered runner at {}", runnerUrl );
        }
        else {
            LOG.warn( "Failed to unregister runner at {}", runnerUrl );
        }

        return Response.ok( result ).build();
    }
}
