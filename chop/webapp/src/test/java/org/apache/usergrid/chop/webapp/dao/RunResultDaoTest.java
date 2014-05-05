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

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;


public class RunResultDaoTest {

    private static Logger LOG = LoggerFactory.getLogger(RunResultDaoTest.class);


    @Test
    public void getAll() {

        LOG.info("\n\n===RunResultDaoTest.getAll===");

        List<RunResult> list = ESSuiteTest.runResultDao.getAll();

        for (RunResult runResult : list) {
            LOG.info(runResult.toString());
        }

        assertEquals(3, list.size());
    }


    @Test
    public void getMap() {

        LOG.info("\n\n===RunResultDaoTest.getMap===");

        Map<String, Run> runs = ESSuiteTest.runDao.getMap(ESSuiteTest.COMMIT_ID_2, 2, ESSuiteTest.TEST_NAME);
        Map<Run, List<RunResult>> runResults = ESSuiteTest.runResultDao.getMap(runs);

        for (Run run : runResults.keySet()) {
            LOG.info(run.toString());

            for (RunResult runResult : runResults.get(run)) {
                LOG.info("   {}", runResult.toString());
            }
        }

        assertEquals(1, runResults.size());
    }


    @Test
    public void deleteAll() {

        LOG.info("\n\n=== RunResultDaoTest.deleteAll() ===");

        for (RunResult runResult : ESSuiteTest.runResultDao.getAll()) {
            ESSuiteTest.runResultDao.delete(runResult.getId());
        }

        List<RunResult> list = ESSuiteTest.runResultDao.getAll();

        assertEquals(0, list.size());
    }

}
