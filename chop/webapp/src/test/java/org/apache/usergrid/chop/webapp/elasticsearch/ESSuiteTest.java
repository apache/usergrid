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
package org.apache.usergrid.chop.webapp.elasticsearch;


import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.commons.lang.time.DateUtils;
import org.apache.usergrid.chop.api.Commit;
import org.apache.usergrid.chop.api.Module;
import org.apache.usergrid.chop.api.ProviderParams;
import org.apache.usergrid.chop.stack.User;
import org.apache.usergrid.chop.webapp.ChopUiModule;
import org.apache.usergrid.chop.webapp.dao.*;
import org.apache.usergrid.chop.webapp.dao.model.*;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;


@RunWith(Suite.class)
@Suite.SuiteClasses(
        {
                ModuleDaoTest.class, CommitDaoTest.class, NoteDaoTest.class, RunDaoTest.class,
                RunnerDaoTest.class, RunnerGroupTest.class, GroupedRunnersTest.class,
                RunResultDaoTest.class, UserDaoTest.class, ProviderParamsDaoTest.class
        })
public class ESSuiteTest {

    private static Logger LOG = LoggerFactory.getLogger(ESSuiteTest.class);

    public static final String MODULE_GROUPID = "org.apache.usergrid.chop";
    public static final String MODULE_ARTIFACT_1 = "chop-runner";
    public static final String MODULE_ARTIFACT_2 = "chop-client";
    public static final String MODULE_VERSION = "1.0-SNAPSHOT";
    public static final String COMMIT_ID_1 = "cc471b502aca2791c3a068f93d15b79ff6b7b827";
    public static final String COMMIT_ID_2 = "7072b85746a980bc5dd9923ccdc9e0ed8e4eb19e";
    public static final String COMMIT_ID_3 = "e29074efad5e0e1c7c2b63128ff9284f9b47ceb3";
    public static final String NOTE = "This is a note!";
    public static final String IMAGE_ID = "ami-213213214";
    public static final String RUNNER_ID_1 = "35231";
    public static final String RUNNER_ID_2 = "23412";
    public static final String TEST_NAME = "org.apache.usergrid.chop.example.DigitalWatchTest";
    public static final String USER_1 = "testuser";
    public static final String USER_2 = "user-2";
    public static final String RUNNER_IPV4_1 = "54.227.39.111";
    public static final String RUNNER_IPV4_2 = "23.20.162.112";
    public static final String RUNNER_HOSTNAME_3 = "ec2-84-197-213-113.compute-1.amazonaws.com";
    public static final String MODULE_ID_1 = BasicModule.createId( MODULE_GROUPID, MODULE_ARTIFACT_1, MODULE_VERSION );
    public static final String MODULE_ID_2 = BasicModule.createId( MODULE_GROUPID, MODULE_ARTIFACT_2, MODULE_VERSION );


    @ClassRule
    public static ElasticSearchResource esClient = new ElasticSearchResource();

    public static ModuleDao moduleDao;
    public static CommitDao commitDao;
    public static NoteDao noteDao;
    public static ProviderParamsDao ppDao;
    public static RunDao runDao;
    public static RunResultDao runResultDao;
    public static UserDao userDao;
    public static RunnerDao runnerDao;

    // Populate elastic search for all tests
    @BeforeClass
    public static void setUpData() throws Exception {
        LOG.info("Setting up sample data for elasticsearch Dao tests...");

        Injector injector = Guice.createInjector(new ChopUiModule());

        setupModules(injector);
        setupCommits(injector);
        setupNotes(injector);
        setupProviderParams(injector);
        String[] runIds = setupRuns(injector);
        setupRunResults(injector, runIds);
        setupUsers(injector);
        setupRunners(injector);

        LOG.info("Sample data for dao tests are saved into elasticsearch");
    }

    private static String[] setupRuns(Injector injector) throws Exception {

        String[] runIds = new String[3];

        runDao = injector.getInstance(RunDao.class);
        BasicRun run = new BasicRun(
                COMMIT_ID_2, // commitId
                RUNNER_ID_2, // runner
                1, // runNumber
                TEST_NAME // testName
        );
        runDao.save(run);
        runIds[0] = run.getId();

        run = new BasicRun(
                COMMIT_ID_2, // commitId
                RUNNER_ID_2, // runner
                2, // runNumber
                TEST_NAME // testName
        );
        runDao.save(run);
        runIds[1] = run.getId();

        run = new BasicRun(
                COMMIT_ID_3, // commitId
                RUNNER_ID_1, // runner
                1, // runNumber
                TEST_NAME // testName
        );
        runDao.save(run);
        runIds[2] = run.getId();

        return runIds;
    }

    private static void setupProviderParams(Injector injector) throws Exception {

        ppDao = injector.getInstance(ProviderParamsDao.class);
        ProviderParams pp = new BasicProviderParams(
                USER_1,
                "m1.large",
                "1230d4353459da23ec21a259a",
                "ad911213ab21ef23ab4e0e",
                IMAGE_ID,
                "testKey1"
        );
        ppDao.save(pp);

        pp = new BasicProviderParams(
                "testuser2",
                "t1.micro",
                "1230d4353459da23ec21a259a",
                "ad911213ab21ef23ab4e0e",
                "ami-2143224",
                "testKey2"
        );
        ppDao.save(pp);
    }

    private static void setupNotes(Injector injector) throws Exception {
        noteDao = injector.getInstance(NoteDao.class);
        Note note = new Note(COMMIT_ID_1, 1, NOTE);
        noteDao.save(note);
    }

    private static void setupModules(Injector injector) throws Exception {

        moduleDao = injector.getInstance(ModuleDao.class);

        Module module = new BasicModule(
                MODULE_GROUPID, // groupId
                MODULE_ARTIFACT_1, // artifactId
                MODULE_VERSION, // version
                "https://stash.safehaus.org/scm/chop/main.git", // vcsRepoUrl
                MODULE_GROUPID // testPackageBase
        );
        moduleDao.save( module );

        module = new BasicModule(
                MODULE_GROUPID, // groupId
                MODULE_ARTIFACT_2, // artifactId
                MODULE_VERSION, // version
                "https://stash.safehaus.org/scm/chop/main.git", // vcsRepoUrl
                MODULE_GROUPID // testPackageBase
        );
        moduleDao.save( module );
    }

    private static void setupCommits(Injector injector) throws Exception {

        // Commits shouldn't have the same createDate b/c of issues with sorting them
        Date now = new Date();

        commitDao = injector.getInstance(CommitDao.class);
        Commit commit = new BasicCommit(
                COMMIT_ID_1, // commitId
                MODULE_ID_1, // moduleId
                "742e2a76a6ba161f9efb87ce58a9187e", // warMD5
                now, // createDate
                "/some/dummy/path"
        );
        commitDao.save(commit);

        commit = new BasicCommit(
                COMMIT_ID_2, // commitId
                MODULE_ID_2, // moduleId
                "395cfdfc3b77242a6f957d6d92da8958", // warMD5
                DateUtils.addMinutes(now, 1), // createDate
                "/some/dummy/path"
        );
        commitDao.save(commit);

        commit = new BasicCommit(
                COMMIT_ID_3, // commitId
                MODULE_ID_2, // moduleId
                "b9860ffa5e39b6f7123ed8c72c4b7046", // warMD5
                DateUtils.addMinutes(now, 2), // createDate
                "/some/dummy/path"
        );
        commitDao.save(commit);
    }

    private static void setupRunResults(Injector injector, String[] runIds) throws Exception {

        runResultDao = injector.getInstance(RunResultDao.class);

        BasicRunResult runResult = new BasicRunResult(runIds[0], 5, 1000, 0, 1);
        runResultDao.save(runResult);

        runResult = new BasicRunResult(runIds[1], 5, 1200, 1, 0);
        runResultDao.save(runResult);

        runResult = new BasicRunResult(runIds[2], 17, 15789, 2, 2);
        runResultDao.save(runResult);
    }

    private static void setupUsers(Injector injector) throws Exception {
        userDao = injector.getInstance(UserDao.class);
        User user = new User(USER_1, "password");
        userDao.save(user);

        user = new User(USER_2, "sosecretsuchcryptowow");
        userDao.save(user);
    }

    private static void setupRunners(Injector injector) throws Exception {

        runnerDao = injector.getInstance(RunnerDao.class);
        BasicRunner runner = new BasicRunner(
                RUNNER_IPV4_1, // ipv4Address
                "ec2-54-227-39-111.compute-1.amazonaws.com", // hostname
                24981,// serverPort
                "https://ec2-54-227-39-111.compute-1.amazonaws.com:24981", // url
                "/tmp" // tempDir
        );
        runnerDao.save(runner, USER_1, COMMIT_ID_1, MODULE_ID_1);

        runner = new BasicRunner(
                RUNNER_IPV4_2, // ipv4Address
                "ec2-23-20-162-112.compute-1.amazonaws.com", // hostname
                8443, // serverPort
                "https://ec2-23-20-162-112.compute-1.amazonaws.com:8443", // url
                "/tmp" // tempDir
        );
        runnerDao.save(runner, USER_1, COMMIT_ID_1, MODULE_ID_1);

        runner = new BasicRunner(
                "84.197.213.113", // ipv4Address
                RUNNER_HOSTNAME_3, // hostname
                24981,// serverPort
                "https://ec2-84-197-213-113.compute-1.amazonaws.com:24981", // url
                "/tmp" // tempDir
        );
        runnerDao.save(runner, USER_2, COMMIT_ID_1, MODULE_ID_1);
    }

    @AfterClass
    public static void tearDownData() {
        LOG.info("ESSuiteTest teardown called");
    }


}
