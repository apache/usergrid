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
import org.apache.usergrid.chop.webapp.dao.model.RunnerGroup;
import org.apache.usergrid.chop.webapp.elasticsearch.ESSuiteTest;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class GroupedRunnersTest {

    private static Logger LOG = LoggerFactory.getLogger( GroupedRunnersTest.class );


    @Test
    public void testRunnersGrouped() throws Exception {
        LOG.info( "\n=== GroupedRunnersTest.testRunnersGrouped() ===\n" );

        Map<RunnerGroup, List<Runner>> runnerGroups = ESSuiteTest.runnerDao.getRunnersGrouped();
        List<Runner> runners = runnerGroups.get( ESSuiteTest.RUNNER_GROUP );

        assertEquals( 1, runners.size() );

        assertTrue( runners.get( 0 ).getIpv4Address().equals( ESSuiteTest.RUNNER_IPV4_1 ) );

    }

}
