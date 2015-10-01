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

import org.apache.usergrid.chop.api.Run;
import org.apache.usergrid.chop.api.RunResult;
import org.apache.usergrid.chop.webapp.elasticsearch.ESSuiteTest;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


public class RunResultDaoTest {

    private static Logger LOG = LoggerFactory.getLogger( RunResultDaoTest.class );


    @Test
    public void getAll() {

        LOG.info( "\n===RunResultDaoTest.getAll===\n" );

        List<RunResult> list = ESSuiteTest.runResultDao.getAll();

        for ( RunResult runResult : list ) {
            LOG.info( runResult.toString() );
        }

        assertEquals( 10, list.size() );
    }


    @Test
    public void getMap() {

        LOG.info( "\n===RunResultDaoTest.getMap===\n" );

        // This should result in a Map with 1 item which is ( RUN_ID_5, AllRuns[4] )
        Map<String, Run> runs = ESSuiteTest.runDao.getMap( ESSuiteTest.COMMIT_ID_1, 2, ESSuiteTest.TEST_NAME_2 );
        Run run = runs.get( ESSuiteTest.RUN_ID_5 );
        assertNotNull( run );
        assertEquals( ESSuiteTest.RUNNER_HOSTNAME_3, run.getRunner() );

        // This should result in 3 RunResults
        Map<Run, List<RunResult>> runResults = ESSuiteTest.runResultDao.getMap( runs );
        List<RunResult> results = runResults.get( run );

        assertEquals( 3, results.size() );

        for ( RunResult result : results ) {
            LOG.info( result.toString() );

            assertEquals( ESSuiteTest.RESULT_RUN_COUNT, result.getRunCount() );
        }
    }


    @Test
    public void deleteAll() throws IOException {

        LOG.info("\n=== RunResultDaoTest.deleteAll() ===\n");

        List<RunResult> allRunResults = ESSuiteTest.runResultDao.getAll();

        for ( RunResult runResult: allRunResults ) {
            ESSuiteTest.runResultDao.delete( runResult.getId() );
        }

        List<RunResult> list = ESSuiteTest.runResultDao.getAll();

        assertEquals( 0, list.size() );

        // Have to save them back for other tests
        for( RunResult runResult: allRunResults ) {
            ESSuiteTest.runResultDao.save( runResult );
        }
    }
}
