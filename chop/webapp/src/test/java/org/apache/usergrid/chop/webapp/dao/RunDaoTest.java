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
package org.apache.usergrid.chop.webapp.dao;

import org.apache.usergrid.chop.api.Commit;
import org.apache.usergrid.chop.api.Run;
import org.apache.usergrid.chop.api.Runner;
import org.apache.usergrid.chop.webapp.elasticsearch.ESSuiteTest;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class RunDaoTest {

    private static Logger LOG = LoggerFactory.getLogger( RunDaoTest.class );


    @Test
    public void get() {

        LOG.info( "\n===RunDaoTest.get===\n" );

        Run run = ESSuiteTest.runDao.get( ESSuiteTest.RUN_ID_3 );

        assertEquals( ESSuiteTest.COMMIT_ID_3, run.getCommitId() );
        assertEquals( ESSuiteTest.RUNNER_HOSTNAME_1, run.getRunner() );
        assertEquals( ESSuiteTest.RUN_DURATION, ( Long ) run.getActualTime() );
    }


    @Test
    public void getAll() {

        LOG.info( "\n===RunDaoTest.getAll===\n" );

        List<Run> list = ESSuiteTest.runDao.getAll();

        for ( Run run : list ) {
            LOG.info( run.toString() );
        }

        assertEquals( 8, list.size() );
    }


    @Test
    public void getListByCommitsAndTestName() {

        LOG.info( "\n===RunDaoTest.getListByCommitsAndTestName===\n" );

        // This should result in commits with { COMMIT_ID_2, COMMIT_ID_3 }
        List<Commit> commits = ESSuiteTest.commitDao.getByModule( ESSuiteTest.MODULE_ID_2 );

        // This should result in 2 Runs with { RUN_ID_1, RUN_ID_2 }
        List<Run> list = ESSuiteTest.runDao.getList( commits, ESSuiteTest.TEST_NAME_1 );

        assertEquals( 2, list.size() );

        for ( Run run : list ) {
            LOG.info( run.toString() );
            assertTrue( run.getId().equals( ESSuiteTest.RUN_ID_1 ) ||  run.getId().equals( ESSuiteTest.RUN_ID_2 ) );
        }
    }


    @Test
    public void getListByCommitAndTestName() {

        LOG.info( "\n===RunDaoTest.getListByCommitAndTestName===\n" );

        // This should result in 2 Runs with { RUN_ID_1, RUN_ID_2 }
        List<Run> list = ESSuiteTest.runDao.getList( ESSuiteTest.COMMIT_ID_2, ESSuiteTest.TEST_NAME_1 );

        assertEquals( 2, list.size() );

        for ( Run run : list ) {
            LOG.info( run.toString() );
            assertTrue( run.getId().equals( ESSuiteTest.RUN_ID_1 ) || run.getId().equals( ESSuiteTest.RUN_ID_2 ) );
        }
    }


    @Test
    public void getListByCommitAndRunNumber() {

        LOG.info( "\n===RunDaoTest.getListByCommitAndRunNumber===\n" );

        int runNumber = 1;

        // This should result in 2 Runs with { RUN_ID_1, RUN_ID_4 }
        List<Run> list = ESSuiteTest.runDao.getList( ESSuiteTest.COMMIT_ID_2, runNumber );

        assertEquals( 2, list.size() );

        for ( Run run : list ) {
            LOG.info( run.toString() );
            assertTrue( run.getId().equals( ESSuiteTest.RUN_ID_1 ) || run.getId().equals( ESSuiteTest.RUN_ID_4 ) );
        }
    }


    @Test
    public void getNextRunNumber() {

        LOG.info( "\n===RunDaoTest.getNextRunNumber===\n" );

        List<Runner> runners = ESSuiteTest.runnerDao.getRunners( ESSuiteTest.USER_1, ESSuiteTest.COMMIT_ID_2,
                ESSuiteTest.MODULE_ID_2 );

        int nextRunNumber = ESSuiteTest.runDao.getNextRunNumber( ESSuiteTest.COMMIT_ID_2 );
        assertEquals( 3, nextRunNumber );
    }


    @Test
    public void getMap() {

        LOG.info( "\n===RunDaoTest.getMap===\n" );

        // This should result in a Map with 1 item which is ( RUN_ID_1, AllRuns[0] )
        Map<String, Run> runs = ESSuiteTest.runDao.getMap( ESSuiteTest.COMMIT_ID_2, 1, ESSuiteTest.TEST_NAME_1 );

        assertEquals( 1, runs.size() );

        for ( String runId : runs.keySet() ) {
            LOG.info( "{}: {}", runId, runs.get( runId ) );
            assertEquals( ESSuiteTest.RUN_ID_1, runId );
            assertEquals( ESSuiteTest.RUN_AVG_TIME_1, ( Long ) runs.get( runId ).getAvgTime() );
        }
    }


    @Test
    public void getMapWithRunners() {
        LOG.info( "\n===RunDaoTest.getMapWithRunners===\n" );

        Collection<Runner> twoRunners = new ArrayList<Runner>();
        Collection<List<Runner>> runners = ESSuiteTest.runnerDao.getRunnersGrouped().values();
        for( List<Runner> runnerList: runners ) {
            for( Runner runner: runnerList ) {
                if( runner.getHostname().equals( ESSuiteTest.RUNNER_HOSTNAME_1 ) ||
                        runner.getHostname().equals( ESSuiteTest.RUNNER_HOSTNAME_2 ) ) {
                    twoRunners.add( runner );
                }
            }
        }

        assertEquals( 2, twoRunners.size() );

        // This should result in a Map with 2 items which are ( RUN_ID_6, AllRuns[5] ), ( RUN_ID_7, AllRuns[6] )
        Map<String, Run> runs = ESSuiteTest.runDao.getMap( ESSuiteTest.COMMIT_ID_1, 2, ESSuiteTest.TEST_NAME_2,
                twoRunners );

        assertEquals( 2, runs.size() );

        for( String runId : runs.keySet() ) {
            LOG.info( "{}: {}", runId, runs.get( runId ) );
            assertTrue( runId.equals( ESSuiteTest.RUN_ID_6 ) || runId.equals( ESSuiteTest.RUN_ID_7 ) );
            assertEquals( 122, runs.get( runId ).getTotalTestsRun() );
        }
    }
}
