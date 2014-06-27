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
package org.apache.usergrid.chop.webapp.coordinator.rest;


import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.safehaus.jettyjam.utils.TestMode;

import org.apache.usergrid.chop.api.Commit;
import org.apache.usergrid.chop.api.Module;
import org.apache.usergrid.chop.stack.CoordinatedStack;
import org.apache.usergrid.chop.stack.Stack;
import org.apache.usergrid.chop.stack.User;
import org.apache.usergrid.chop.webapp.coordinator.CoordinatorUtils;
import org.apache.usergrid.chop.webapp.coordinator.StackCoordinator;
import org.apache.usergrid.chop.webapp.dao.CommitDao;
import org.apache.usergrid.chop.webapp.dao.ModuleDao;
import org.apache.usergrid.chop.webapp.dao.UserDao;
import org.apache.usergrid.chop.webapp.dao.model.BasicCommit;
import org.apache.usergrid.chop.webapp.dao.model.BasicModule;


/**
 * A base class for all signal resources.
 */
public abstract class TestableResource {
    public static final String TEST_PARAM = TestMode.TEST_MODE_PROPERTY;

    public final static String SUCCESSFUL_TEST_MESSAGE = "Test parameters are OK";

    private final String endpoint;


    protected TestableResource(String endpoint) {
        this.endpoint = endpoint;
    }


    public String getTestMessage() {
        return endpoint + " resource called in test mode.";
    }


    public boolean inTestMode(String testMode) {
        return testMode != null &&
                (testMode.equals(TestMode.INTEG.toString()) || testMode.equals(TestMode.UNIT.toString()));
    }

    public CoordinatedStack getCoordinatedStack( CommitDao commitDao, ModuleDao moduleDao, UserDao userDao,
                                                  StackCoordinator stackCoordinator, String artifactId,
                                                  String commitId, String md5, String username,
                                                  String groupId, String version, String vcsRepoUrl,
                                                  String testPackage, File runnerJar )
            throws IOException {
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

        // Send DEPLOY signal to coordinatedStack
        Stack stack = CoordinatorUtils.getStackFromRunnerJar( runnerJar );
        User chopUser = userDao.get( username );
        CoordinatedStack coordinatedStack =  stackCoordinator.getCoordinatedStack( stack, chopUser, commit, module );
        return coordinatedStack;
    }
}
