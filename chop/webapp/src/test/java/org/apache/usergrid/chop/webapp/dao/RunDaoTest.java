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
import org.apache.usergrid.chop.webapp.elasticsearch.ESSuiteTest;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;


public class RunDaoTest {

    private static Logger LOG = LoggerFactory.getLogger(RunDaoTest.class);


    @Test
    public void getAll() {

        LOG.info("\n===RunDaoTest.getAll===\n");

        List<Run> list = ESSuiteTest.runDao.getAll();

        for (Run run : list) {
            LOG.info(run.toString());
        }

        assertEquals(3, list.size());
    }


    @Test
    public void getListByCommits() {

        LOG.info("\n===RunDaoTest.getListByCommits===\n");

        List<Commit> commits = ESSuiteTest.commitDao.getByModule(ESSuiteTest.MODULE_ID_2);

        List<Run> list = ESSuiteTest.runDao.getList(commits, ESSuiteTest.TEST_NAME);

        for (Run run : list) {
            LOG.info(run.toString());
        }

        assertEquals(3, list.size());
    }


    @Test
    public void getListByCommit() {

        LOG.info("\n===RunDaoTest.getListByCommit===\n");

        List<Run> list = ESSuiteTest.runDao.getList(ESSuiteTest.COMMIT_ID_2, ESSuiteTest.TEST_NAME);

        for (Run run : list) {
            LOG.info(run.toString());
        }

        assertEquals(2, list.size());
    }


    @Test
    public void getListByCommitAndRunNumber() {

        LOG.info("\n===RunDaoTest.getListByCommitAndRunNumber===\n");

        int runNumber = 2;

        List<Run> list = ESSuiteTest.runDao.getList(ESSuiteTest.COMMIT_ID_2, runNumber);

        for (Run run : list) {
            LOG.info(run.toString());
        }

        assertEquals(1, list.size());
    }


    @Test
    public void getNextRunNumber() {

        LOG.info("\n===RunDaoTest.getNextRunNumber===\n");

        int nextRunNumber = ESSuiteTest.runDao.getNextRunNumber(ESSuiteTest.COMMIT_ID_2);
        assertEquals(3, nextRunNumber);
    }


    @Test
    public void getMapByCommitAndRunNumber() {

        LOG.info("\n===RunDaoTest.getMapByCommitAndRunNumber===\n");

        Map<String, Run> runs = ESSuiteTest.runDao.getMap(ESSuiteTest.COMMIT_ID_2, 2, ESSuiteTest.TEST_NAME);

        for (String runId : runs.keySet()) {
            LOG.info("{}: {}", runId, runs.get(runId));
        }

        assertEquals(1, runs.size());
    }

}
