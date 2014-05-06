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

import org.apache.usergrid.chop.api.Runner;
import org.apache.usergrid.chop.webapp.dao.model.BasicRunner;
import org.apache.usergrid.chop.webapp.dao.model.RunnerGroup;
import org.apache.usergrid.chop.webapp.elasticsearch.ESSuiteTest;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;

public class GroupedRunnersTest {

    private static Logger LOG = LoggerFactory.getLogger(GroupedRunnersTest.class);

    private static final RunnerGroup RUNNER_GROUP = new RunnerGroup("user", "commit", "module");

    private static final BasicRunner RUNNER1 = new BasicRunner(
            "172.168.0.1",
            "runner.chop.com",
            1001,
            "https://runner.chop.com:1001",
            "/tmp"
    );

    private static final BasicRunner RUNNER2 = new BasicRunner(
            "172.168.0.1",
            "runner.chop.com",
            1002,
            "https://runner.chop.com:1002",
            "/tmp"
    );

    @Test
    public void test() throws Exception {
        LOG.info("\n=== GroupedRunnersTest.test() ===\n");

        ESSuiteTest.runnerDao.save( RUNNER1, RUNNER_GROUP.getUser(), RUNNER_GROUP.getCommitId(), RUNNER_GROUP.getModuleId() );
        ESSuiteTest.runnerDao.save( RUNNER2, RUNNER_GROUP.getUser(), RUNNER_GROUP.getCommitId(), RUNNER_GROUP.getModuleId() );

        Map<RunnerGroup, List<Runner>> runnerGroups = ESSuiteTest.runnerDao.getRunnersGrouped();
        List<Runner> runners = runnerGroups.get(RUNNER_GROUP);

        assertTrue(runners.size() == 2);
    }

}
