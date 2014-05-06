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
package org.apache.usergrid.chop.runner;


import org.apache.usergrid.chop.api.Result;
import org.apache.usergrid.chop.api.Runner;
import org.apache.usergrid.chop.api.StatsSnapshot;
import org.apache.usergrid.chop.client.rest.RestRequests;
import org.safehaus.jettyjam.utils.TestParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertEquals;


/**
 * Common tests run in unit and test mode.
 */
public class RunnerTestUtils {

    public static void testStart( TestParams testParams ) {
        Result result = RestRequests.newStartOp(
                testParams.setEndpoint( Runner.START_POST ).newWebResource() ).execute( Result.class );

        assertEquals( result.getEndpoint(), Runner.START_POST );
    }


    public static void testReset( TestParams testParams ) {
        Result result = RestRequests.newResetOp(
                testParams.setEndpoint( Runner.RESET_POST ).newWebResource() ).execute( Result.class );

        assertEquals( result.getEndpoint(), Runner.RESET_POST );
    }


    public static void testStop( TestParams testParams ) {
        Result result = RestRequests.newStopOp(
                testParams.setEndpoint( Runner.STOP_POST ).newWebResource() ).execute( Result.class );

        assertEquals( result.getEndpoint(), Runner.STOP_POST );
    }


    public static void testStats( final TestParams testParams ) {
        StatsSnapshot snapshot = RestRequests.newStatsOp(
                testParams.setEndpoint( Runner.STATS_GET ).newWebResource() ).execute( StatsSnapshot.class );

        assertNotNull( snapshot );
    }


    public static void testStatus( final TestParams testParams ) {
        Result result = RestRequests.newStatusOp(
                testParams.setEndpoint( Runner.STATUS_GET ).newWebResource() ).execute( Result.class );

        assertEquals( result.getEndpoint(), Runner.STATUS_GET );
    }
}
