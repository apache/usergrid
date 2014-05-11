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
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.safehaus.jettyjam.utils.CertUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.chop.api.BaseResult;
import org.apache.usergrid.chop.api.Run;
import org.apache.usergrid.chop.api.Runner;
import org.apache.usergrid.chop.api.State;
import org.apache.usergrid.chop.webapp.dao.RunDao;
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

    @Inject
    private RunDao runDao;

    private Map<Runner, Integer> lastRunNumbers = new HashMap<Runner, Integer>();


    /**
     *
     * @param username
     * @param commitId
     * @param moduleId
     * @return          the registered runners belonging to given username, commitId and moduleId
     */
    public Collection<Runner> getRunners( String username, String commitId, String moduleId ) {
        return runnerDao.getRunners( username, commitId, moduleId );
    }


    /**
     *
     * @param username
     * @param commitId
     * @param moduleId
     * @return
     */
    public Map<Runner, State> getStates( String username, String commitId, String moduleId ) {
        return getStates( getRunners( username, commitId, moduleId ) );
    }


    /**
     * Gets the run State of all given runners as a map.
     * <p>
     * <ul>
     *     <li>Key of map is the runner</li>
     *     <li>Value field is the state of a runner, or null if state could not be retrieved</li>
     * </ul>
     *
     * @param runners    Runners to get states
     * @return           Map of all Runner, State pairs
     */
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
                states.put( runner, null );
            }
            else {
                states.put( runner, response.getState() );
            }
        }
        return states;
    }


    /**
     *
     * @param username
     * @param commitId
     * @param moduleId
     * @return
     */
    public Map<Runner, State> start( String username, String commitId, String moduleId ) {
        return start( getRunners( username, commitId, moduleId ), runDao.getNextRunNumber( commitId ) );
    }


    /**
     * Starts the tests on given runners and puts them into RUNNING state, if indeed they were READY.
     *
     * @param runners   Runners that are going to run the tests
     * @param runNumber Run number of upcoming tests, this should be get from Run storage
     * @return          Map of resulting states of <code>runners</code>, after the operation
     */
    public Map<Runner, State> start( Collection<Runner> runners, int runNumber ) {
        Map<Runner, State> states = new HashMap<Runner, State>( runners.size() );
        for( Runner runner: runners ) {
            lastRunNumbers.put( runner, runNumber );
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


    /**
     *
     * @param username
     * @param commitId
     * @param moduleId
     * @return
     */
    public Map<Runner, State> stop( String username, String commitId, String moduleId ) {
        return stop( getRunners( username, commitId, moduleId ) );
    }


    /**
     * Stop the tests on given runners and puts them into STOPPED state, if indeed they were RUNNING.
     *
     * @param runners    Runners that are running the tests
     * @return           Map of resulting states of <code>runners</code>, after the operation
     */
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


    /**
     *
     * @param username
     * @param commitId
     * @param moduleId
     * @return
     */
    public Map<Runner, State> reset( String username, String commitId, String moduleId ) {
        return reset( getRunners( username, commitId, moduleId ) );
    }


    /**
     * Resets the given runners and puts them into READY state, if indeed they were STOPPED.
     *
     * @param runners   Runners to reset
     * @return          Map of resulting states of <code>runners</code>, after the operation
     */
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


    /**
     * Removes incomplete set of Runs from storage, which are there due to Stopped tests.
     * <p>
     * This uses <code>lastRunNumbers</code> map to get the latest run number given runners were running.
     * Start method puts the run number of upcoming tests to <code>lastRunNumbers</code> map,
     * so if stopped, Runs with that run number in storage, if there are any, are deleted.
     *
     * @param runners    All runners that were running a particular chop test
     * @param commitId   Commit Id that defines related test
     */
    public void trimIncompleteRuns( Collection<Runner> runners, String commitId ) {
        for( Runner runner: runners ) {
            Integer lastRunNumber = lastRunNumbers.get( runner );
            List<Run> runs = runDao.getRuns( runner.getHostname(), commitId );
            for( Run run: runs ) {
                if( run.getRunNumber() == lastRunNumber ) {
                    LOG.info( "Removing incomplete Run {}", run );
                    runDao.delete( run );
                }
            }
        }
    }


    /**
     * Registers given runner in storage, so that it belongs to given username, commitId and moduleId.
     *
     * @param username
     * @param commitId
     * @param moduleId
     * @param runner
     * @return          Whether the operation succeeded
     */
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


    /**
     * Removes the runner object with given runnerUrl
     *
     * @param runnerUrl Runner's url to unregister
     * @return          Whether such a runner had existed in storage
     */
    public boolean unregister( String runnerUrl ) {
        LOG.info( "Unregistering runner at {}", runnerUrl );
        return runnerDao.delete( runnerUrl );
    }


    /**
     * This is to resolve self signed uniform certificates in runners.
     *
     * @param runnerUrl    Runner's url to trust in SSL communications
     */
    private void trustRunner( final String runnerUrl ) {
        final URI uri = URI.create( runnerUrl );

        /**
         * This is because we are using self-signed uniform certificates for now,
         * it should be removed if we switch to a CA signed dynamic certificate scheme!
         * */
        javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier(
            new javax.net.ssl.HostnameVerifier() {
                public boolean verify( String hostname, javax.net.ssl.SSLSession sslSession) {
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
