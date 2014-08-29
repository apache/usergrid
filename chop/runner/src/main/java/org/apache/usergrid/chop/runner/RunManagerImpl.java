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
package org.apache.usergrid.chop.runner;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.usergrid.chop.api.*;
import org.apache.usergrid.chop.spi.RunManager;
import org.safehaus.jettyjam.utils.CertUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.multipart.FormDataBodyPart;
import com.sun.jersey.multipart.FormDataMultiPart;


/**
 * An implementation of the RunManager that works with the Coordinator service on the web ui.
 */
public class RunManagerImpl implements RunManager, RestParams {
    private static final Logger LOG = LoggerFactory.getLogger( RunManagerImpl.class );

    private CoordinatorFig coordinatorFig;
    private URL endpoint;

    @Inject
    private Runner me;


    @Inject
    private void setCoordinatorConfig( CoordinatorFig coordinatorFig ) {
        this.coordinatorFig = coordinatorFig;

        try {
            endpoint = new URL( coordinatorFig.getEndpoint() );
        }
        catch ( MalformedURLException e ) {
            LOG.error( "Failed to parse URL for coordinator", e );
        }

        // Need to get the configuration information for the coordinator
        if ( ! CertUtils.isTrusted( endpoint.getHost() ) ) {
            CertUtils.preparations( endpoint.getHost(), endpoint.getPort() );
        }
        Preconditions.checkState( CertUtils.isTrusted( endpoint.getHost() ), "coordinator must be trusted" );
    }


    private WebResource addQueryParameters( WebResource resource, Project project, Runner runner ) {
        return resource.queryParam( RUNNER_HOSTNAME, runner.getHostname() )
                .queryParam( RUNNER_PORT, String.valueOf( runner.getServerPort() ) )
                .queryParam( RUNNER_IPV4_ADDRESS, runner.getIpv4Address() )
                .queryParam( MODULE_GROUPID, project.getGroupId() )
                .queryParam( MODULE_ARTIFACTID, project.getArtifactId() )
                .queryParam( MODULE_VERSION, project.getVersion() )
                .queryParam( COMMIT_ID, project.getVcsVersion() )
                .queryParam( USERNAME, coordinatorFig.getUsername() )
                .queryParam( PASSWORD, coordinatorFig.getPassword() );
    }


    @Override
    public void store( final Project project, final Summary summary, final File resultsFile,
                       final Class<?> testClass ) throws FileNotFoundException, MalformedURLException {
        Preconditions.checkNotNull( summary, "The summary argument cannot be null." );

        // upload the results file
        InputStream in = new FileInputStream( resultsFile );
        FormDataMultiPart part = new FormDataMultiPart();
        part.field( FILENAME, resultsFile.getName() );

        FormDataBodyPart body = new FormDataBodyPart( CONTENT, in, MediaType.APPLICATION_OCTET_STREAM_TYPE );
        part.bodyPart( body );

        WebResource resource = Client.create().resource( coordinatorFig.getEndpoint() );
        resource = addQueryParameters( resource, project, me );
        String result = resource.path( coordinatorFig.getStoreResultsPath() )
                         .queryParam( RUN_ID, summary.getRunId() )
                         .queryParam( RUN_NUMBER, "" + summary.getRunNumber() )
                         .type( MediaType.MULTIPART_FORM_DATA_TYPE )
                         .post( String.class, part );

        LOG.debug( "Got back result from results file store = {}", result );
    }


    @Override
    public boolean hasCompleted( final Runner runner, final Project project, final int runNumber,
                                 final Class<?> testClass ) {
        // get run status information
        WebResource resource = Client.create().resource( coordinatorFig.getEndpoint() );
        resource = addQueryParameters( resource, project, runner );
        ClientResponse result = resource.path( coordinatorFig.getRunCompletedPath() )
                                .queryParam( RUNNER_HOSTNAME, runner.getHostname() )
                                .queryParam( COMMIT_ID, project.getVcsVersion() )
                                .queryParam( RUN_NUMBER, String.valueOf( runNumber ) )
                                .queryParam( TEST_CLASS, testClass.getName() )
                                .type( MediaType.APPLICATION_JSON )
                                .get( ClientResponse.class );

        if( result.getStatus() != Response.Status.CREATED.getStatusCode() ) {
            LOG.error( "Could not get if run has completed status from coordinator, HTTP status: {}",
                    result.getStatus() );
            return false;
        }

        return result.getEntity( Boolean.class );
    }


    @Override
    public int getNextRunNumber( final Project project ) {
        WebResource resource = Client.create().resource( coordinatorFig.getEndpoint() );
        resource = addQueryParameters( resource, project, me );
        Integer result = resource.path( coordinatorFig.getRunNextPath() )
                                 .type( MediaType.APPLICATION_JSON_TYPE )
                                 .get( Integer.class );

        LOG.debug( "Got back result from next run number get = {}", result );

        return result;
    }
}
