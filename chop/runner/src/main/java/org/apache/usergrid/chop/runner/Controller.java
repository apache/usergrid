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


import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.usergrid.chop.runner.drivers.Driver;
import org.apache.usergrid.chop.runner.drivers.TimeDriver;
import org.reflections.Reflections;
import org.apache.usergrid.chop.api.Project;
import org.apache.usergrid.chop.api.Runner;
import org.apache.usergrid.chop.api.Signal;
import org.apache.usergrid.chop.api.StatsSnapshot;
import org.apache.usergrid.chop.api.State;
import org.apache.usergrid.chop.api.IterationChop;
import org.apache.usergrid.chop.api.TimeChop;
import org.apache.usergrid.chop.spi.RunManager;
import org.apache.usergrid.chop.spi.RunnerRegistry;
import org.apache.usergrid.chop.runner.drivers.IterationDriver;
import org.apache.usergrid.chop.stack.ChopCluster;
import org.apache.usergrid.chop.stack.ICoordinatedCluster;
import org.apache.usergrid.chop.stack.Instance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;


/**
 * The Controller controls the process of executing chops on test classes.
 */
@Singleton
public class Controller implements IController, Runnable {
    private static final Logger LOG = LoggerFactory.getLogger( Controller.class );

    // @todo make this configurable and also put this into the project or runner fig
    private static final long DEFAULT_LAGER_WAIT_TIMEOUT_MILLIS = 120000;
    private final Object lock = new Object();

    private Set<Class<?>> timeChopClasses;
    private Set<Class<?>> iterationChopClasses;
    private State state = State.INACTIVE;
    private Driver<?> currentDriver;

    private Map<String, ICoordinatedCluster> clusterMap = new HashMap<String, ICoordinatedCluster>();
    private List<Runner> otherRunners;
    private RunManager runManager;
    private Project project;
    private int runNumber;


    @Inject
    Controller( Project project, RunnerRegistry registry, RunManager runManager, Runner me ) {
        setProject( project );
        setRunManager( runManager );

        if ( state != State.INACTIVE ) {
            runNumber = runManager.getNextRunNumber( project );
            otherRunners = registry.getRunners( me );

            List<ICoordinatedCluster> clusters = registry.getClusters();
            if ( clusters == null ) {
                LOG.debug( "Returned clusters list is null" );
            }
            else {
                LOG.info( "{} clusters total", clusters.size() );
                for ( ICoordinatedCluster cluster : clusters ) {
                    clusterMap.put( cluster.getName(), cluster );
                    LOG.info( "Cluster name {}, {} instances", cluster.getName(), cluster.getInstances().size() );
                    for ( Instance i : cluster.getInstances() ) {
                        LOG.debug( "Public: {}, Private: {}", i.getPublicIpAddress(), i.getPrivateIpAddress() );
                    }
                }
                injectClusters();
            }
        }
    }


    private void setProject( Project project ) {
        // if the project is null which should never really happen we just return
        // and stay in the INACTIVE state waiting for a load to activate this runner
        if ( project == null ) {
            return;
        }

        // setup the valid runner project
        this.project = project;
        LOG.info( "Controller injected with project properties: {}", project );

        // if the project test package base is null there's nothing we can do but
        // return and stay in the inactive state waiting for a load to occur
        if ( project.getTestPackageBase() == null ) {
            return;
        }

        // reflect into package base looking for annotated classes
        Reflections reflections = new Reflections( project.getTestPackageBase() );

        timeChopClasses = reflections.getTypesAnnotatedWith( TimeChop.class );
        LOG.info( "TimeChop classes = {}", timeChopClasses );

        iterationChopClasses = reflections.getTypesAnnotatedWith( IterationChop.class );
        LOG.info( "IterationChop classes = {}", iterationChopClasses );

        // if we don't have a valid project load key then this is bogus
        if ( project.getLoadKey() == null ) {
            state = State.INACTIVE;
            LOG.info( "Null loadKey: controller going into INACTIVE state." );
            return;
        }


        if ( timeChopClasses.isEmpty() && iterationChopClasses.isEmpty() ) {
            state = State.INACTIVE;
            LOG.info( "Nothing to scan: controller going into INACTIVE state." );
            return;
        }

        state = State.READY;
        LOG.info( "We have things to scan and a valid loadKey: controller going into READY state." );
    }


    private void setRunManager( RunManager runManager ) {
        Preconditions.checkNotNull( runManager, "The RunManager cannot be null." );
        this.runManager = runManager;
    }


    /**
     * Scans all @IterationChop and @TimeChop annotated test classes in code base
     * and sets @ChopCluster annotated fields in these classes to their runtime values.
     * <p>
     * For this to work properly, fields in test classes should be declared as follows:
     * <p>
     *     <code>@ChopCluster( name = "ClusterName" )</code>
     *     <code>public static ICoordinatedCluster clusterToBeInjected;</code>
     * </p>
     * In this case, <code>clusterToBeInjected</code> field will be set to the cluster object
     * taken from the coordinator, if indeed a cluster with a name of "ClusterName" exists.
     */
    private void injectClusters() {
        Collection<Class<?>> testClasses = new LinkedList<Class<?>>();
        testClasses.addAll( iterationChopClasses );
        testClasses.addAll( timeChopClasses );
        for( Class<?> iterationTest : testClasses ) {
            LOG.info( "Scanning test class {} for annotations", iterationTest.getName() );
            for( Field f : iterationTest.getDeclaredFields() ) {
                if( f.getType().isAssignableFrom( ICoordinatedCluster.class ) ) {
                    for( Annotation annotation : f.getDeclaredAnnotations() ) {
                        if( annotation.annotationType().equals( ChopCluster.class )  ) {
                            String clusterName = ( ( ChopCluster ) annotation).name();
                            ICoordinatedCluster cluster;
                            if ( ! clusterMap.containsKey( clusterName ) ||
                                    ( cluster = clusterMap.get( clusterName ) ) == null ) {
                                LOG.warn( "No clusters found with name: {}", clusterName );
                                continue;
                            }
                            try {
                                LOG.info( "Setting cluster {} on {} field", clusterName, f.getName() );
                                f.set( null, cluster );
                            }
                            catch ( IllegalAccessException e ) {
                                LOG.error( "Cannot access field {}", f.getName(), e );
                            }
                        }
                    }
                }
            }
        }
    }


    @Override
    public StatsSnapshot getCurrentChopStats() {
        return currentDriver != null ? currentDriver.getChopStats() : null;
    }


    @Override
    public State getState() {
        return state;
    }


    @Override
    public boolean isRunning() {
        return state == State.RUNNING;
    }


    @Override
    public boolean needsReset() {
        return state == State.STOPPED;
    }


    @Override
    public Project getProject() {
        return project;
    }


    @Override
    public void reset() {
        synchronized ( lock ) {
            Preconditions.checkState( state.accepts( Signal.RESET, State.READY ),
                    "Cannot reset the controller in state: " + state );
            state = state.next( Signal.RESET );
            currentDriver = null;
        }
    }


    @Override
    public void start() {
        synchronized ( lock ) {
            Preconditions.checkState( state.accepts( Signal.START ), "Cannot start the controller in state: " + state );
            runNumber = runManager.getNextRunNumber( project );
            state = state.next( Signal.START );
            new Thread( this ).start();
            lock.notifyAll();
        }
    }


    @Override
    public void stop() {
        synchronized ( lock ) {
            Preconditions.checkState( state.accepts( Signal.STOP ), "Cannot stop a controller in state: " + state );
            state = state.next( Signal.STOP );
            lock.notifyAll();
        }
    }


    @Override
    public void send( final Signal signal ) {
        Preconditions.checkState( state.accepts( signal ), state.getMessage( signal ) );

        switch ( signal ) {
            case STOP: stop(); break;
            case START: start(); break;
            case RESET: reset(); break;
            default:
                throw new IllegalStateException( "Just accepting start, stop, and reset." );
        }
    }


    /**
     * Gets the collection of runners that are still executing a chop on a test class.
     *
     * @param runNumber the current run number
     * @param testClass the current chop test
     * @return the runners still executing a test class
     */
    private Collection<Runner> getLagers( int runNumber, Class<?> testClass ) {
        Collection<Runner> lagers = new ArrayList<Runner>( otherRunners.size() );

        for ( Runner runner : otherRunners ) {
            if ( runManager.hasCompleted( runner, project, runNumber, testClass ) ) {
                LOG.info( "Runner {} has completed test {}", runner.getHostname(), testClass.getName() );
            }
            else {
                LOG.warn( "Waiting on runner {} to complete test {}", runner.getHostname(), testClass.getName() );
                lagers.add( runner );
            }
        }

        return lagers;
    }


    @Override
    public void run() {
        for ( Class<?> iterationTest : iterationChopClasses ) {
            synchronized ( lock ) {
                currentDriver = new IterationDriver( iterationTest );
                currentDriver.setTimeout( project.getTestStopTimeout() );
                currentDriver.start();
                lock.notifyAll();
            }

            LOG.info( "Started new IterationDriver driver: controller state = {}", state );
            while ( currentDriver.blockTilDone( project.getTestStopTimeout() ) ) {
                if ( state == State.STOPPED ) {
                    LOG.info( "Got the signal to stop running." );
                    synchronized ( lock ) {
                        currentDriver.stop();
                        currentDriver = null;
                        lock.notifyAll();
                    }
                    return;
                }
            }

            LOG.info( "Out of while loop. controller state = {}, currentDriver is running = {}",
                    state, currentDriver.isRunning());

            if ( currentDriver.isComplete() ) {
                BasicSummary summary = new BasicSummary( runNumber );
                summary.setIterationTracker( ( ( IterationDriver ) currentDriver ).getTracker() );
                try {
                    runManager.store( project, summary, currentDriver.getResultsFile(),
                            currentDriver.getTracker().getTestClass() );
                }
                catch ( Exception e ) {
                    LOG.error( "Failed to store project results file " + currentDriver.getResultsFile() +
                            " with runManager", e );
                }

                long startWaitingForLagers = System.currentTimeMillis();
                while ( state == State.RUNNING ) {
                    Collection<Runner> lagers = getLagers( runNumber, iterationTest );
                    if ( lagers.size() > 0 ) {
                        LOG.info( "IterationChop test {} completed but waiting on lagging runners:\n{}",
                                iterationTest.getName(), lagers );
                    }
                    else {
                        LOG.info( "IterationChop test {} completed and there are NO lagging runners.",
                                iterationTest.getName() );
                        break;
                    }

                    synchronized ( lock ) {
                        try {
                            lock.wait( project.getTestStopTimeout() );
                        }
                        catch ( InterruptedException e ) {
                            LOG.error( "Awe snap! Someone woke me up before it was time!" );
                        }
                    }

                    boolean waitTimeoutReached = ( System.currentTimeMillis() - startWaitingForLagers )
                            > DEFAULT_LAGER_WAIT_TIMEOUT_MILLIS;

                    if ( waitTimeoutReached && ( lagers.size() > 0 ) ) {
                        LOG.warn( "Timeout reached. Not waiting anymore for lagers: {}", lagers );
                        break;
                    }
                }
            }
        }

        for ( Class<?> timeTest : timeChopClasses ) {
            synchronized ( lock ) {
                currentDriver = new TimeDriver( timeTest );
                currentDriver.setTimeout( project.getTestStopTimeout() );
                currentDriver.start();
                lock.notifyAll();
            }

            LOG.info( "Started new TimeDriver driver: controller state = {}", state );
            while ( currentDriver.blockTilDone( project.getTestStopTimeout() ) ) {
                if ( state == State.STOPPED ) {
                    LOG.info( "Got the signal to stop running." );
                    synchronized ( lock ) {
                        currentDriver.stop();
                        currentDriver = null;
                        lock.notifyAll();
                    }
                    return;
                }
            }

            LOG.info( "Out of while loop. controller state = {}, currentDriver is running = {}",
                    state, currentDriver.isRunning());

            if ( currentDriver.isComplete() ) {
                BasicSummary summary = new BasicSummary( runNumber );
                summary.setTimeTracker( ( ( TimeDriver ) currentDriver ).getTracker() );
                try {
                    runManager.store( project, summary, currentDriver.getResultsFile(),
                            currentDriver.getTracker().getTestClass() );
                }
                catch ( Exception e ) {
                    LOG.error( "Failed to store project results file " + currentDriver.getResultsFile() +
                            " with runManager", e );
                }

                long startWaitingForLagers = System.currentTimeMillis();
                while ( state == State.RUNNING ) {
                    Collection<Runner> lagers = getLagers( runNumber, timeTest );
                    if ( lagers.size() > 0 ) {
                        LOG.warn( "TimeChop test {} completed but waiting on lagging runners:\n{}",
                                timeTest.getName(), lagers );
                    }
                    else {
                        LOG.info( "TimeChop test {} completed and there are NO lagging runners.",
                                timeTest.getName() );
                        break;
                    }

                    synchronized ( lock ) {
                        try {
                            lock.wait( project.getTestStopTimeout() );
                        }
                        catch ( InterruptedException e ) {
                            LOG.error( "Awe snap! Someone woke me up before it was time!" );
                        }
                    }

                    boolean waitTimeoutReached = ( System.currentTimeMillis() - startWaitingForLagers )
                            > DEFAULT_LAGER_WAIT_TIMEOUT_MILLIS;

                    if ( waitTimeoutReached && ( lagers.size() > 0 ) ) {
                        LOG.warn( "Timeout reached. Not waiting anymore for lagers: {}", lagers );
                        break;
                    }
                }
            }
        }

        LOG.info( "The controller has completed." );
        currentDriver = null;
        state = state.next( Signal.COMPLETED );
    }
}
