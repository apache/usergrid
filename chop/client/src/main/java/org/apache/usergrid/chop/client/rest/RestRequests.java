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
package org.apache.usergrid.chop.client.rest;


import javax.net.ssl.SSLHandshakeException;

import org.apache.usergrid.chop.api.ChopUtils;
import org.apache.usergrid.chop.api.CoordinatorFig;
import org.apache.usergrid.chop.api.Project;
import org.apache.usergrid.chop.api.RestParams;
import org.apache.usergrid.chop.api.Result;
import org.apache.usergrid.chop.api.Runner;
import org.apache.usergrid.chop.api.StatsSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.WebResource;


/**
 * Client REST request functions.
 */
public class RestRequests {
    private static final Logger LOG = LoggerFactory.getLogger( RestRequests.class );


    private static void preparations( final Runner runner ) {
        Preconditions.checkNotNull( runner, "The runner parameter cannot be null." );
        Preconditions.checkNotNull( runner.getHostname(), "The runner parameter's hostname property cannot be null." );

        /**
         * This is because we are using self-signed uniform certificates for now,
         * it should be removed if we switch to a CA signed dynamic certificate scheme!
         * */
        javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier(
            new javax.net.ssl.HostnameVerifier() {
                public boolean verify( String hostname, javax.net.ssl.SSLSession sslSession) {
                    return hostname.equals( runner.getHostname() );
                }
            }
        );
        if ( ! ChopUtils.isTrusted( runner ) ) {
            try {
                ChopUtils.installRunnerKey( null, runner );
            }
            catch ( Exception e ) {
                LOG.error( "Failed to install certificate for runner: {}", runner.getHostname() );

                try {
                    ChopUtils.installCert( runner.getHostname(), runner.getServerPort(), null );
                }
                catch ( Exception e2 ) {
                    LOG.error( "Failed to get certificate from server {} on port {}: dumping stack trace!",
                            runner.getHostname(), runner.getServerPort() );
                    e.printStackTrace();
                }
            }
        }
    }


    public static AbstractRestOperation<Result> newRestOp( HttpOp op, WebResource resource ) {
        return new AbstractRestOperation<Result>( op, resource ) {};
    }


    public static AbstractRestOperation<Result> newRestOp( HttpOp op, String path, Runner runner ) {
        return new AbstractRestOperation<Result>( op, path, runner ) {};
    }


    public static AbstractRestOperation<Result> newResetOp( WebResource resource ) {
        resource.path( Runner.RESET_POST );
        return newRestOp( HttpOp.POST, resource );
    }


    public static AbstractRestOperation<Result> newResetOp( Runner runner ) {
        return newRestOp( HttpOp.POST, Runner.RESET_POST, runner );
    }


    public static AbstractRestOperation<Result> newStartOp( WebResource resource ) {
        resource.path( Runner.START_POST );
        return newRestOp( HttpOp.POST, resource );
    }


    public static AbstractRestOperation<Result> newStartOp( Runner runner ) {
        return newRestOp( HttpOp.POST, Runner.START_POST, runner );
    }


    public static AbstractRestOperation<Result> newStopOp( WebResource resource ) {
        resource.path( Runner.STOP_POST );
        return newRestOp( HttpOp.POST, resource );
    }


    public static AbstractRestOperation<Result> newStopOp( Runner runner ) {
        return newRestOp( HttpOp.POST, Runner.STOP_POST, runner );
    }


    public static AbstractRestOperation<Result> newStatusOp( WebResource resource ) {
        resource.path( Runner.STATUS_GET );
        return newRestOp( HttpOp.GET, resource );
    }


    public static AbstractRestOperation<Result> newStatusOp( Runner runner ) {
        return newRestOp( HttpOp.GET, Runner.STATUS_GET, runner );
    }


    public static AbstractRestOperation<StatsSnapshot> newStatsOp( WebResource resource ) {
        resource.path( Runner.STATS_GET );
        return new AbstractRestOperation<StatsSnapshot>( HttpOp.GET, resource ) {};
    }


    public static AbstractRestOperation<StatsSnapshot> newStatsOp( Runner runner ) {
        return new AbstractRestOperation<StatsSnapshot>( HttpOp.GET, Runner.STATS_GET, runner ) {};
    }


    /**
     * Performs a POST HTTP operation against the /start endpoint with a propagate query parameter.
     *
     * @param runner the runner which will perform the start operation
     * @return the result of the operation
     */
    public static Result start( Runner runner ) {
        preparations( runner );
        return newStartOp( runner ).execute( Result.class );
    }


    /**
     * Performs a POST HTTP operation against the /reset endpoint with a propagate query parameter.
     *
     * @param runner the runner to perform the reset operation on
     * @return the result of the operation
     */
    public static Result reset( Runner runner ) {
        preparations( runner );
        return newResetOp( runner ).execute( Result.class );
    }


    /**
     * Performs a POST HTTP operation against the /stop endpoint with a propagate query parameter.
     *
     * @param runner the runner which will perform the stop operation
     * @return the result of the operation
     */
    public static Result stop( Runner runner ) {
        preparations( runner );
        return newStopOp( runner ).execute( Result.class );
    }


    /**
     * Performs a GET HTTP operation against the /status endpoint.
     *
     * @param runner the runner to perform the status operation on
     *
     * @return the result of the operation
     */
    public static Result status( Runner runner ) {
        preparations( runner );

        try {
            return newStatusOp( runner ).execute( Result.class );
        }
        catch ( ClientHandlerException e ) {
            if ( e.getCause() instanceof SSLHandshakeException &&
                    e.getCause().toString().contains( "PKIX path building failed" ) ) {

                /*
                 * Oddly this fails the first time but works the second time. Until
                 * I get to the bottom of this and figure it out this is the work
                 * around we will use to make sure this does not fail. We retry once
                 * on the failure.
                 */

                return newStatusOp( runner ).execute( Result.class );
            }
        }

        throw new RuntimeException( "If we got here then the retry also failed." );
    }


    public static StatsSnapshot stats( Runner runner ) {
        preparations( runner );
        return newStatsOp( runner ).execute( StatsSnapshot.class );
    }


    public static WebResource addParams( WebResource resource, CoordinatorFig coordinator ) {
        Preconditions.checkNotNull( resource, "The 'resource' MUST NOT be null." );
        Preconditions.checkNotNull( coordinator, "The 'coordinator' MUST NOT be null." );

        resource = resource.queryParam( RestParams.USERNAME, coordinator.getUsername() );
        return resource.queryParam( RestParams.PASSWORD, coordinator.getPassword() );
    }


    public static WebResource addParams( WebResource resource, Runner runner ) {
        Preconditions.checkNotNull( resource, "The 'resource' MUST NOT be null." );
        Preconditions.checkNotNull( runner, "The 'runner' MUST NOT be null." );

        resource = resource.queryParam( RestParams.RUNNER_URL, runner.getUrl() );
        resource = resource.queryParam( RestParams.RUNNER_HOSTNAME, runner.getHostname() );
        resource = resource.queryParam( RestParams.RUNNER_IPV4_ADDRESS, runner.getIpv4Address() );
        return resource.queryParam( RestParams.RUNNER_PORT, String.valueOf( runner.getServerPort() ) );
    }


    public static WebResource addParams( WebResource resource, Project project ) {
        Preconditions.checkNotNull( resource, "The 'resource' MUST NOT be null." );
        Preconditions.checkNotNull( project, "The 'project' MUST NOT be null." );

        resource = resource.queryParam( RestParams.MODULE_ARTIFACTID, project.getArtifactId() );
        resource = resource.queryParam( RestParams.MODULE_GROUPID, project.getGroupId() );
        resource = resource.queryParam( RestParams.MODULE_VERSION, project.getVersion() );
        resource = resource.queryParam( RestParams.COMMIT_ID, project.getVcsVersion() );
        resource = resource.queryParam( RestParams.TEST_PACKAGE, project.getTestPackageBase() );
        resource = resource.queryParam( RestParams.MD5, project.getMd5() );
        return resource.queryParam( RestParams.VCS_REPO_URL, project.getVcsRepoUrl() );
    }
}


