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
import org.apache.usergrid.chop.webapp.elasticsearch.ESSuiteTest;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.Assert.assertEquals;


public class RunnerDaoTest {

    private static Logger LOG = LoggerFactory.getLogger( RunnerDaoTest.class );


    @Test
    public void delete() throws Exception {
        LOG.info( "\n===RunnerDaoTest.delete===\n" );

        LOG.info( "Runners before delete: " );

        List<Runner> runners = ESSuiteTest.runnerDao.getRunners(
                ESSuiteTest.USER_2, ESSuiteTest.COMMIT_ID_2, ESSuiteTest.MODULE_ID_2
                                                               );
        for( Runner runner : runners ) {
            LOG.info( runner.toString() );
            ESSuiteTest.runnerDao.delete( runner.getUrl() );
        }

        List<Runner> runnersAfter = ESSuiteTest.runnerDao.getRunners(
                ESSuiteTest.USER_2, ESSuiteTest.COMMIT_ID_2, ESSuiteTest.MODULE_ID_2
                                                                    );
        assertEquals( 0, runnersAfter.size() );

        /** We have to save them back for other tests */
        for ( Runner runner: runners ) {
            ESSuiteTest.runnerDao.save( runner, ESSuiteTest.USER_2, ESSuiteTest.COMMIT_ID_2, ESSuiteTest.MODULE_ID_2 );
        }
    }


    @Test
    public void getRunners() {

        LOG.info( "\n===RunnerDaoTest.getRunners===\n" );

        List<Runner> userRunners = ESSuiteTest.runnerDao.getRunners(
                ESSuiteTest.USER_1, ESSuiteTest.COMMIT_ID_2, ESSuiteTest.MODULE_ID_2
                                                                   );

        assertEquals( 1, userRunners.size() );
    }
}
