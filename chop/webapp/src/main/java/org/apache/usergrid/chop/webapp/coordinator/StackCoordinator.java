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


import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.usergrid.chop.api.Commit;
import org.apache.usergrid.chop.api.Module;
import org.apache.usergrid.chop.stack.CoordinatedStack;
import org.apache.usergrid.chop.stack.SetupStackSignal;
import org.apache.usergrid.chop.stack.SetupStackState;
import org.apache.usergrid.chop.stack.Stack;
import org.apache.usergrid.chop.stack.User;
import org.apache.usergrid.chop.webapp.ChopUiFig;
import org.apache.usergrid.chop.webapp.dao.CommitDao;
import org.apache.usergrid.chop.webapp.dao.ModuleDao;
import org.apache.usergrid.chop.webapp.dao.UserDao;
import org.apache.usergrid.chop.webapp.dao.model.BasicModule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;


/**
 * Coordinates all chop runs in the server.
 */
@Singleton
public class StackCoordinator {

    private static final Logger LOG = LoggerFactory.getLogger( StackCoordinator.class );

    private final ExecutorService service = Executors.newCachedThreadPool();

    @Inject
    private ChopUiFig chopUiFig;

    @Inject
    private UserDao userDao;

    @Inject
    private CommitDao commitDao;

    @Inject
    private ModuleDao moduleDao;

    private Map<CoordinatedStack, SetupStackThread> setupStackThreads =
            new ConcurrentHashMap<CoordinatedStack, SetupStackThread>();

    private Map<Integer, CoordinatedStack> registeredStacks = new ConcurrentHashMap<Integer, CoordinatedStack>();


    /**
     * Sets up all clusters and runner instances defined by given parameters
     * <p>
     * Don't call this method without checking parameters first,
     * this method assumes that a runner with given parameters is already deployed
     * and its stack is ready to be set up
     *
     * @param commitId
     * @param artifactId
     * @param groupId
     * @param version
     * @param user
     * @param runnerCount
     * @return
     */
    public CoordinatedStack setupStack( String commitId, String artifactId, String groupId, String version,
                                        String user, int runnerCount ) {

        User chopUser = userDao.get( user );
        File runnerJar = CoordinatorUtils.getRunnerJar( chopUiFig.getContextPath(), user, groupId, artifactId, version,
                commitId );

        Stack stack = CoordinatorUtils.getStackFromRunnerJar( runnerJar );
        Module module = moduleDao.get( BasicModule.createId( groupId, artifactId, version ) );
        Commit commit = null;
        for( Commit c: commitDao.getByModule( module.getId() ) ) {
            if( commitId.equals( c.getId() ) ) {
                commit = c;
                break;
            }
        }

        return setupStack( stack, chopUser, commit, module, runnerCount );
    }

    /**
     * Sets up all clusters and runner instances defined by given parameters
     *
     * @param stack         Stack object to be set up
     * @param user          User who is doing the operation
     * @param commit        Commit to be chop tested
     * @param module        Module to be chop tested
     * @param runnerCount   Number of runner instances that will run the tests
     * @return              the CoordinatedStack object if setup succeeds
     * @throws Exception
     */
    public CoordinatedStack setupStack( Stack stack, User user, Commit commit, Module module, int runnerCount ) {

        CoordinatedStack coordinatedStack = getCoordinatedStack( stack, user, commit, module );
        if ( coordinatedStack != null && coordinatedStack.getRunnerCount() == runnerCount ) {
            LOG.info( "Stack {} is already registered", stack.getName() );
            if( coordinatedStack.getSetupState() == SetupStackState.SetUp ) {
                return coordinatedStack;
            }
        }
        else if ( coordinatedStack != null && coordinatedStack.getRunnerCount() != runnerCount  ) {
            LOG.info( "Stack {} is registered with different runner count, first removing the old stack", stack.getName() );
            registeredStacks.remove( coordinatedStack.hashCode() );
        }

        LOG.info( "Registering new stack {}...", stack.getName() );
        coordinatedStack = new CoordinatedStack( stack, user, commit, module, runnerCount );

        LOG.info( "Starting setup stack thread of {}...", stack.getName() );
        synchronized ( coordinatedStack ) {
            coordinatedStack.setSetupState( SetupStackSignal.SETUP );
            registeredStacks.put( coordinatedStack.hashCode(), coordinatedStack );

            SetupStackThread setupThread = new SetupStackThread( coordinatedStack );
            setupStackThreads.put( coordinatedStack, setupThread );

            // Not registering the results for now, since they are not being used
            service.submit( setupThread );
        }
        return coordinatedStack;
    }


    public void destroyStack( String commitId, String artifactId, String groupId, String version, String user ) {
        User chopUser = userDao.get( user );
        File runnerJar = CoordinatorUtils.getRunnerJar( chopUiFig.getContextPath(), user, groupId, artifactId, version,
                commitId );

        Stack stack = CoordinatorUtils.getStackFromRunnerJar( runnerJar );
        Module module = moduleDao.get( BasicModule.createId( groupId, artifactId, version ) );
        Commit commit = null;
        for( Commit c: commitDao.getByModule( module.getId() ) ) {
            if( commitId.equals( c.getId() ) ) {
                commit = c;
                break;
            }
        }
        destroyStack( stack, chopUser, commit, module );
    }


    public void destroyStack( Stack stack, User user, Commit commit, Module module ) {
        CoordinatedStack coordinatedStack = getCoordinatedStack( stack, user, commit, module );
        if ( coordinatedStack == null || coordinatedStack.getSetupState() == SetupStackState.JarNotFound ) {
            LOG.info( "No such stack was found." );
            return;
        }

        synchronized ( coordinatedStack ) {
            if ( ! coordinatedStack.getSetupState().accepts( SetupStackSignal.DESTROY ) ) {
                LOG.info( "Stack is in {} state, will not destroy.", coordinatedStack.getSetupState().toString() );
                return;
            }

            // TODO should we also check run state of stack?
            LOG.info( "Starting to destroy stack instances of {}...", stack.getName() );
            coordinatedStack.setSetupState( SetupStackSignal.DESTROY );
            StackDestroyer destroyer = new StackDestroyer( coordinatedStack );
            destroyer.destroy();
            registeredStacks.remove( coordinatedStack.hashCode() );
            setupStackThreads.remove( coordinatedStack );
            coordinatedStack.setSetupState( SetupStackSignal.COMPLETE );
            coordinatedStack.notifyAll();
        }
    }


    public SetupStackThread getSetupStackThread( CoordinatedStack stack ) {
        return setupStackThreads.get( stack );
    }


    public CoordinatedStack getMatching( User user, Commit commit, Module module ) {
        for( CoordinatedStack stack: registeredStacks.values() ) {
            if( stack.getUser().equals( user ) && stack.getCommit().equals( commit ) &&
                    stack.getModule().equals( module ) ) {
                return stack;
            }
        }
        return null;
    }


    /**
     * Looks for a registered <code>CoordinatedStack</code> matching given parameters
     *
     * @param stack
     * @param user
     * @param commit
     * @param module
     * @return
     */
    public CoordinatedStack getCoordinatedStack( Stack stack, User user, Commit commit, Module module ) {

        return registeredStacks.get( CoordinatedStack.calcHashCode( stack, user, commit, module ) );
    }


    /**
     * Tries to find a registered <code>CoordinatedStack</code> object matching given parameters
     * <p>
     * Returns null if;
     * <ul>
     *     <li>no users, modules or commits exist by supplied parameters</li>
     *     <li>or no runner jars have been deployed matching these parameters</li>
     *     <li>or no matching stack has been registered to be set up yet</li>
     * </ul>
     * Returns the matching <code>CoordinatedStack<code/> object otherwise
     *
     * @param commitId
     * @param artifactId
     * @param groupId
     * @param version
     * @param user
     * @return  matching coordinated stack, or null
     */
    public CoordinatedStack findCoordinatedStack( String commitId, String artifactId, String groupId, String version,
                                                  String user ) {

        User chopUser = userDao.get( user );
        if( chopUser == null ) {
            LOG.warn( "No such user: {}", user );
            return null;
        }

        File runnerJar = CoordinatorUtils.getRunnerJar( chopUiFig.getContextPath(), user, groupId, artifactId, version,
                commitId );

        if( ! runnerJar.exists() ) {
            LOG.warn( "No runner jars have been found by these parameters, deploy first" );
            return null;
        }

        Stack stack = CoordinatorUtils.getStackFromRunnerJar( runnerJar );
        if( stack == null ) {
            LOG.warn( "Could not read stack from runner.jar's resources" );
            return null;
        }

        Module module = moduleDao.get( BasicModule.createId( groupId, artifactId, version ) );
        if( module == null ) {
            LOG.warn( "No registered modules found by {}" + groupId + ":" + artifactId + ":" + version );
            return null;
        }

        Commit commit = null;
        for( Commit c: commitDao.getByModule( module.getId() ) ) {
            if( commitId.equals( c.getId() ) ) {
                commit = c;
                break;
            }
        }
        if( commit == null ) {
            LOG.warn( "Commit with id {} is not found", commitId );
            return null;
        }

        return getCoordinatedStack( stack, chopUser, commit, module );
    }


    /**
     * @param commitId
     * @param artifactId
     * @param groupId
     * @param version
     * @param user
     * @return Setup state of given parameters' stack
     */
    public SetupStackState stackStatus( String commitId, String artifactId, String groupId,
                                        String version, String user ) {

        CoordinatedStack stack = findCoordinatedStack( commitId, artifactId, groupId, version, user );

        /** Stack is not registered in StackCoordinator */
        if( stack == null ) {
            File runnerJar = CoordinatorUtils.getRunnerJar( chopUiFig.getContextPath(), user, groupId, artifactId,
                    version, commitId );

            if( ! runnerJar.exists() ) {
                return SetupStackState.JarNotFound;
            }

            return SetupStackState.NotSetUp;
        }

        return stack.getSetupState();
    }


    /**
     * Removes the given failed coordinated stack object from <code>setupStackThreads</code> and
     * <code>registeredStacks</code> if indeed such a stack really exists and its setup failed
     *
     * @param stack CoordinatedStack object whose set up operation has failed
     */
    public void removeFailedStack( CoordinatedStack stack ) {
        if( stack == null ) {
            return;
        }
        synchronized ( stack ) {
            if ( stack.getSetupState() != SetupStackState.SetupFailed ) {
                LOG.debug( "Setup didn't fail for given stack, so not removed" );
                return;
            }
            registeredStacks.remove( stack.hashCode() );
            setupStackThreads.remove( stack );
            stack.notifyAll();
        }
    }


    public CoordinatedStack registerStack( final String commitId, final String artifactId, final String groupId,
                                           final String version, final String user, final int runnerCount ) {

        User chopUser = userDao.get( user );
        File runnerJar = CoordinatorUtils.getRunnerJar( chopUiFig.getContextPath(), user, groupId, artifactId,
                version, commitId );

        Stack stack = CoordinatorUtils.getStackFromRunnerJar( runnerJar );
        Module module = moduleDao.get( BasicModule.createId( groupId, artifactId, version ) );
        Commit commit = null;
        for( Commit c: commitDao.getByModule( module.getId() ) ) {
            if( commitId.equals( c.getId() ) ) {
                commit = c;
                break;
            }
        }

        return registerStack( stack, chopUser, commit, module, runnerCount );
    }


    public CoordinatedStack registerStack( final Stack stack, final User user, final Commit commit, final Module
            module, final int runnerCount ) {
        CoordinatedStack coordinatedStack = getCoordinatedStack( stack, user, commit, module );
        if ( coordinatedStack != null ) {
            LOG.info( "Stack {} is already registered", stack.getName() );
            return coordinatedStack;
        }
        else {
            coordinatedStack = new CoordinatedStack( stack, user, commit, module, runnerCount );
        }

        LOG.info( "Registering stack...", stack.getName() );
        synchronized ( coordinatedStack ) {
            coordinatedStack.setSetupState( SetupStackSignal.DEPLOY );
            registeredStacks.put( coordinatedStack.hashCode(), coordinatedStack );
        }

        return coordinatedStack;
    }
}
