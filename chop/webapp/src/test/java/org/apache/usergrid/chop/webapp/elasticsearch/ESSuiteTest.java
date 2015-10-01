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
import java.util.UUID;


/**
 * This Suite populates an empty embedded elastic search with Dao related data,
 * for all Dao unit tests that are going to be conducted.
 * <p>
 * Almost all data prepared here is related to each other to a degree, according to the Dao relations,
 * by references to one another and common fields they have. So, if you want to change anything here,
 * or decide to add more data, first be sure you understand how relevant Daos relate to one another
 * and what existing data here relate to each other.
 * <p>
 * Or, if you were to change or add new functionality in Dao model, make sure you also make appropriate
 * modifications here and in DaoTest classes too, without reducing test coverage or losing sight of
 * the practical usage of Dao classes.
 */
@RunWith( Suite.class )
@Suite.SuiteClasses(
        {
                ModuleDaoTest.class, CommitDaoTest.class, NoteDaoTest.class, RunDaoTest.class,
                RunnerDaoTest.class, RunnerGroupTest.class, GroupedRunnersTest.class,
                RunResultDaoTest.class, UserDaoTest.class, ProviderParamsDaoTest.class
        } )
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
    public static final String TEST_NAME_1 = "org.apache.usergrid.chop.example.DigitalWatchTest";
    public static final String TEST_NAME_2 = "org.apache.usergrid.chop.example.MechanicalWatchTest";
    public static final String USER_1 = "testuser";
    public static final String USER_2 = "user-2";
    public static final String RUNNER_IPV4_1 = "54.227.39.111";
    public static final String RUNNER_IPV4_2 = "23.20.162.112";
    public static final String RUNNER_HOSTNAME_1 = "ec2-54-227-39-111.compute-1.amazonaws.com";
    public static final String RUNNER_HOSTNAME_2 = "ec2-23-20-162-112.compute-1.amazonaws.com";
    public static final String RUNNER_HOSTNAME_3 = "ec2-84-197-213-113.compute-1.amazonaws.com";
    public static final String MODULE_ID_1 = BasicModule.createId( MODULE_GROUPID, MODULE_ARTIFACT_1, MODULE_VERSION );
    public static final String MODULE_ID_2 = BasicModule.createId( MODULE_GROUPID, MODULE_ARTIFACT_2, MODULE_VERSION );
    public static final String RUN_ID_1 = UUID.randomUUID().toString();
    public static final String RUN_ID_2 = UUID.randomUUID().toString();
    public static final String RUN_ID_3 = UUID.randomUUID().toString();
    public static final String RUN_ID_4 = UUID.randomUUID().toString();
    public static final String RUN_ID_5 = UUID.randomUUID().toString();
    public static final String RUN_ID_6 = UUID.randomUUID().toString();
    public static final String RUN_ID_7 = UUID.randomUUID().toString();
    public static final String RUN_ID_8 = UUID.randomUUID().toString();
    public static final RunnerGroup RUNNER_GROUP = new RunnerGroup( USER_1, COMMIT_ID_2, MODULE_ID_2 );
    public static final Long RUN_DURATION = 100000L;
    public static final Long RUN_AVG_TIME_1 = 1505L;
    public static final int RESULT_RUN_COUNT = 18;

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
        LOG.info( "Setting up sample data for elasticsearch Dao tests..." );

        Injector injector = Guice.createInjector( new ChopUiModule() );
        IElasticSearchClient elasticSearchClient = injector.getInstance(IElasticSearchClient.class);
        elasticSearchClient.start();

        setupUsers( injector );
        setupModules( injector );
        setupCommits( injector );
        setupNotes( injector );
        setupProviderParams( injector );
        setupRunners( injector );
        setupRuns( injector );
        setupRunResults( injector );

        LOG.info( "Sample data for dao tests are saved into elasticsearch" );
    }


    private static void setupRuns( Injector injector ) throws Exception {

        Long startTime = new Date().getTime();
        runDao = injector.getInstance( RunDao.class );
        BasicRun run = new BasicRun(
                RUN_ID_1,
                COMMIT_ID_2, // commitId
                RUNNER_HOSTNAME_1, // runner
                1, // runNumber
                TEST_NAME_1 // testName
        );
        run.setActualTime( RUN_DURATION );
        run.setStartTime( startTime );
        run.setStopTime( startTime + RUN_DURATION );
        run.setAvgTime( RUN_AVG_TIME_1 );
        run.setChopType( "IterationChop" );
        run.setIterations( 10 );
        runDao.save( run );

        startTime = new Date().getTime();
        run = new BasicRun(
                RUN_ID_2,
                COMMIT_ID_2, // commitId
                RUNNER_HOSTNAME_1, // runner
                2, // runNumber
                TEST_NAME_1 // testName
        );
        run.setActualTime( RUN_DURATION );
        run.setStartTime( startTime );
        run.setStopTime( startTime + RUN_DURATION );
        run.setAvgTime( 1284L );
        run.setChopType( "IterationChop" );
        run.setIterations( 20 );
        runDao.save( run );

        startTime = new Date().getTime();
        run = new BasicRun(
                RUN_ID_3,
                COMMIT_ID_3, // commitId
                RUNNER_HOSTNAME_1, // runner
                1, // runNumber
                TEST_NAME_2 // testName
        );
        run.setActualTime( RUN_DURATION );
        run.setStartTime( startTime );
        run.setStopTime( startTime + RUN_DURATION );
        run.setTotalTestsRun( 84 );
        run.setChopType( "TimeChop" );
        runDao.save( run );

        startTime = new Date().getTime();
        run = new BasicRun(
                RUN_ID_4,
                COMMIT_ID_2, // commitId
                RUNNER_HOSTNAME_3, // runner
                1, // runNumber
                TEST_NAME_2 // testName
        );
        run.setActualTime( RUN_DURATION );
        run.setStartTime( startTime );
        run.setStopTime( startTime + RUN_DURATION );
        run.setTotalTestsRun( 60 );
        run.setChopType( "TimeChop" );
        run.setSaturate( true );
        runDao.save( run );

        startTime = new Date().getTime();
        run = new BasicRun(
                RUN_ID_5,
                COMMIT_ID_1, // commitId
                RUNNER_HOSTNAME_3, // runner
                2, // runNumber
                TEST_NAME_2 // testName
        );
        run.setActualTime( RUN_DURATION );
        run.setStartTime( startTime );
        run.setStopTime( startTime + RUN_DURATION );
        run.setTotalTestsRun( 72 );
        run.setChopType( "TimeChop" );
        run.setSaturate( true );
        runDao.save( run );

        startTime = new Date().getTime();
        run = new BasicRun(
                RUN_ID_6,
                COMMIT_ID_1, // commitId
                RUNNER_HOSTNAME_1, // runner
                2, // runNumber
                TEST_NAME_2 // testName
        );
        run.setActualTime( RUN_DURATION );
        run.setStartTime( startTime );
        run.setStopTime( startTime + RUN_DURATION );
        run.setTotalTestsRun( 122 );
        run.setChopType( "TimeChop" );
        run.setSaturate( false );
        runDao.save( run );

        startTime = new Date().getTime();
        run = new BasicRun(
                RUN_ID_7,
                COMMIT_ID_1, // commitId
                RUNNER_HOSTNAME_2, // runner
                2, // runNumber
                TEST_NAME_2 // testName
        );
        run.setActualTime( RUN_DURATION );
        run.setStartTime( startTime );
        run.setStopTime( startTime + RUN_DURATION );
        run.setTotalTestsRun( 122 );
        run.setChopType( "TimeChop" );
        run.setSaturate( false );
        runDao.save( run );

        startTime = new Date().getTime();
        run = new BasicRun(
                RUN_ID_8,
                COMMIT_ID_1, // commitId
                RUNNER_HOSTNAME_3, // runner
                1, // runNumber
                TEST_NAME_2 // testName
        );
        run.setActualTime( RUN_DURATION );
        run.setStartTime( startTime );
        run.setStopTime( startTime + RUN_DURATION );
        run.setTotalTestsRun( 60 );
        run.setChopType( "TimeChop" );
        run.setSaturate( true );
        runDao.save( run );
    }


    private static void setupProviderParams( Injector injector ) throws Exception {

        ppDao = injector.getInstance( ProviderParamsDao.class );
        ProviderParams pp = new BasicProviderParams(
                USER_1,
                "m1.large",
                "1230d4353459da23ec21a259a",
                "ad911213ab21ef23ab4e0e",
                IMAGE_ID,
                "testKey1"
        );
        ppDao.save( pp );

        pp = new BasicProviderParams(
                "testuser2",
                "t1.micro",
                "1230d4353459da23ec21a259a",
                "ad911213ab21ef23ab4e0e",
                "ami-2143224",
                "testKey2"
        );
        ppDao.save( pp );
    }


    private static void setupNotes( Injector injector ) throws Exception {
        noteDao = injector.getInstance( NoteDao.class );
        Note note = new Note( COMMIT_ID_1, 1, NOTE );
        noteDao.save( note );
    }


    private static void setupModules( Injector injector ) throws Exception {

        moduleDao = injector.getInstance( ModuleDao.class );

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


    private static void setupCommits( Injector injector ) throws Exception {

        // Commits shouldn't have the same createDate b/c of issues with sorting them
        Date now = new Date();

        commitDao = injector.getInstance( CommitDao.class );
        Commit commit = new BasicCommit(
                COMMIT_ID_1, // commitId
                MODULE_ID_1, // moduleId
                "742e2a76a6ba161f9efb87ce58a9187e", // warMD5
                now, // createDate
                "/some/dummy/path"
        );
        commitDao.save( commit );

        commit = new BasicCommit(
                COMMIT_ID_2, // commitId
                MODULE_ID_2, // moduleId
                "395cfdfc3b77242a6f957d6d92da8958", // warMD5
                DateUtils.addMinutes( now, 1 ), // createDate
                "/some/dummy/path"
        );
        commitDao.save( commit );

        commit = new BasicCommit(
                COMMIT_ID_3, // commitId
                MODULE_ID_2, // moduleId
                "b9860ffa5e39b6f7123ed8c72c4b7046", // warMD5
                DateUtils.addMinutes( now, 2 ), // createDate
                "/some/dummy/path"
        );
        commitDao.save( commit );
    }


    private static void setupRunResults( Injector injector ) throws Exception {

        runResultDao = injector.getInstance( RunResultDao.class );

        BasicRunResult runResult = new BasicRunResult( RUN_ID_1, 5, 1000, 0, 1 );
        runResultDao.save( runResult );

        runResult = new BasicRunResult( RUN_ID_1, 5, 1103, 0, 0 );
        runResultDao.save( runResult );

        runResult = new BasicRunResult( RUN_ID_2, 5, 1200, 1, 0 );
        runResultDao.save( runResult );

        runResult = new BasicRunResult( RUN_ID_3, 17, 15789, 2, 2 );
        runResultDao.save( runResult );

        runResult = new BasicRunResult( RUN_ID_4, 17, 15789, 2, 2 );
        runResultDao.save( runResult );

        runResult = new BasicRunResult( RUN_ID_4, 17, 15789, 2, 2 );
        runResultDao.save( runResult );

        runResult = new BasicRunResult( RUN_ID_4, 17, 15789, 2, 2 );
        runResultDao.save( runResult );

        runResult = new BasicRunResult( RUN_ID_5, RESULT_RUN_COUNT, 15729, 2, 2 );
        runResultDao.save( runResult );

        runResult = new BasicRunResult( RUN_ID_5, RESULT_RUN_COUNT, 13429, 0, 0 );
        runResultDao.save( runResult );

        runResult = new BasicRunResult( RUN_ID_5, RESULT_RUN_COUNT, 16421, 1, 0 );
        runResultDao.save( runResult );
    }


    private static void setupUsers( Injector injector ) throws Exception {
        userDao = injector.getInstance( UserDao.class );
        User user = new User( USER_1, "password" );
        userDao.save( user );

        user = new User( USER_2, "sosecretsuchcryptowow" );
        userDao.save( user );
    }


    private static void setupRunners( Injector injector ) throws Exception {

        StringBuilder url = new StringBuilder();
        runnerDao = injector.getInstance( RunnerDao.class );
        BasicRunner runner = new BasicRunner(
                RUNNER_IPV4_1, // ipv4Address
                RUNNER_HOSTNAME_1, // hostname
                24981, // serverPort
                url.append( "https://" ).append( RUNNER_HOSTNAME_1 ).append( ":" ).append( 24981 ).toString(), // url
                "/tmp" // tempDir
        );
        runnerDao.save( runner, USER_1, COMMIT_ID_2, MODULE_ID_2 );

        url = new StringBuilder();
        runner = new BasicRunner(
                RUNNER_IPV4_2, // ipv4Address
                RUNNER_HOSTNAME_2, // hostname
                8443, // serverPort
                url.append( "https://" ).append( RUNNER_HOSTNAME_2 ).append( ":" ).append( 8443 ).toString(), // url
                "/tmp" // tempDir
        );
        runnerDao.save( runner, USER_1, COMMIT_ID_3, MODULE_ID_2 );

        runner = new BasicRunner(
                "84.197.213.113", // ipv4Address
                RUNNER_HOSTNAME_3, // hostname
                24981,// serverPort
                "https://ec2-84-197-213-113.compute-1.amazonaws.com:24981", // url
                "/tmp" // tempDir
        );
        runnerDao.save( runner, USER_2, COMMIT_ID_2, MODULE_ID_2 );
    }


    @AfterClass
    public static void tearDownData() {
        LOG.info( "ESSuiteTest teardown called" );
    }


}
