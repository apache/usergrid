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
package org.apache.usergrid.chop.webapp.coordinator;


import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.safehaus.jettyjam.utils.CertUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.chop.api.BaseResult;
import org.apache.usergrid.chop.api.Runner;
import org.apache.usergrid.chop.api.State;
import org.apache.usergrid.chop.webapp.dao.RunnerDao;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.DefaultClientConfig;


/**
 * Coordinates all runners in the server
 */
@Singleton
public class RunnerCoordinator {

    private static final Logger LOG = LoggerFactory.getLogger( RunnerCoordinator.class );

    @Inject
    private RunnerDao runnerDao;


    public Collection<Runner> getRunners( String username, String commitId, String moduleId ) {
        return runnerDao.getRunners( username, commitId, moduleId );
    }


    public Map<Runner, State> getStates( String username, String commitId, String moduleId ) {
        return getStates( getRunners( username, commitId, moduleId ) );
    }


    public Map<Runner, State> getStates( Collection<Runner> runners ) {
        Map<Runner, State> states = new HashMap<Runner, State>( runners.size() );
        for( Runner runner: runners ) {
            trustRunner( runner.getUrl() );
            DefaultClientConfig clientConfig = new DefaultClientConfig();
            Client client = Client.create( clientConfig );
            LOG.info( "Runner to get state: {}", runner.getUrl() );
            WebResource resource = client.resource( runner.getUrl() ).path( Runner.STATUS_GET );
            BaseResult response = resource.type( MediaType.APPLICATION_JSON ).get( BaseResult.class );
            if( ! response.getStatus() ) {
                LOG.warn( "Could not get the state of Runner at {}", runner.getUrl() );
                LOG.warn( response.getMessage() );
                // TODO should we throw exception, return null?
                states.put( runner, null );
            }
            else {
                states.put( runner, response.getState() );
            }
        }
        return states;
    }


    public Map<Runner, State> start( String username, String commitId, String moduleId ) {
        return start( getRunners( username, commitId, moduleId ) );
    }


    public Map<Runner, State> start( Collection<Runner> runners ) {
        Map<Runner, State> states = new HashMap<Runner, State>( runners.size() );
        for( Runner runner: runners ) {
            trustRunner( runner.getUrl() );
            DefaultClientConfig clientConfig = new DefaultClientConfig();
            Client client = Client.create( clientConfig );
            LOG.info( "Runner to start: {}", runner.getUrl() );
            WebResource resource = client.resource( runner.getUrl() ).path( Runner.START_POST );
            BaseResult response = resource.type( MediaType.APPLICATION_JSON ).post( BaseResult.class );
            if( ! response.getStatus() ) {
                LOG.warn( "Tests at runner {} could not be started.", runner.getUrl() );
                LOG.warn( response.getMessage() );
                states.put( runner, null );
            }
            else {
                states.put( runner, response.getState() );
            }
        }
        return states;
    }


    public Map<Runner, State> stop( String username, String commitId, String moduleId ) {
        return stop( getRunners( username, commitId, moduleId ) );
    }


    public Map<Runner, State> stop( Collection<Runner> runners ) {
        Map<Runner, State> states = new HashMap<Runner, State>( runners.size() );
        for( Runner runner: runners ) {
            trustRunner( runner.getUrl() );
            DefaultClientConfig clientConfig = new DefaultClientConfig();
            Client client = Client.create( clientConfig );
            WebResource resource = client.resource( runner.getUrl() ).path( Runner.STOP_POST );
            BaseResult response = resource.type( MediaType.APPLICATION_JSON ).post( BaseResult.class );
            if( ! response.getStatus() ) {
                LOG.warn( "Tests at runner {} could not be stopped.", runner.getUrl() );
                LOG.warn( response.getMessage() );
                states.put( runner, null );
            }
            else {
                states.put( runner, response.getState() );
            }
        }
        return states;
    }


    public Map<Runner, State> reset( String username, String commitId, String moduleId ) {
        return reset( getRunners( username, commitId, moduleId ) );
    }


    public Map<Runner, State> reset( Collection<Runner> runners ) {
        Map<Runner, State> states = new HashMap<Runner, State>( runners.size() );
        for( Runner runner: runners ) {
            trustRunner( runner.getUrl() );
            DefaultClientConfig clientConfig = new DefaultClientConfig();
            Client client = Client.create( clientConfig );
            WebResource resource = client.resource( runner.getUrl() ).path( Runner.RESET_POST );
            BaseResult response = resource.type( MediaType.APPLICATION_JSON ).post( BaseResult.class );
            if( ! response.getStatus() ) {
                LOG.warn( "Tests at runner {} could not be reset.", runner.getUrl() );
                LOG.warn( response.getMessage() );
                states.put( runner, null );
            }
            else {
                states.put( runner, response.getState() );
            }
        }
        return states;
    }


    public boolean register( String username, String commitId, String moduleId, Runner runner ) {
        try {
            LOG.info( "Registering runner: {}", runner.getUrl() );
            LOG.info( "  User: {}", username );
            LOG.info( "  Commit ID: {}", commitId );
            LOG.info( "  Module ID: {}", moduleId );

            return runnerDao.save( runner, username, commitId, moduleId );
        }
        catch ( IOException e ) {
            LOG.warn( "Error while trying to register runner. {}", e );
            return false;
        }
    }


    public boolean unregister( String runnerUrl ) {
        LOG.info( "Unregistering runner at {}", runnerUrl );
        return runnerDao.delete( runnerUrl );
    }


    private void trustRunner( final String runnerUrl ) {
        final URI uri = URI.create( runnerUrl );

        /**
         * This is because we are using self-signed uniform certificates for now,
         * it should be removed if we switch to a CA signed dynamic certificate scheme!
         * */
        javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier(
            new javax.net.ssl.HostnameVerifier() {
                public boolean verify( String hostname, javax.net.ssl.SSLSession sslSession) {
                    LOG.info( "Verify called for hostname: {} and url: {}", hostname, runnerUrl );
                    return hostname.equals( uri.getHost() );
                }
            }
        );
        // Need to get the configuration information for the coordinator
        if ( ! CertUtils.isTrusted( uri.getHost() ) ) {
            CertUtils.preparations( uri.getHost(), uri.getPort() );
        }
        Preconditions.checkState( CertUtils.isTrusted( uri.getHost() ), "coordinator must be trusted" );
    }
}
