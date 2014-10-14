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


import java.util.Properties;

import org.junit.ClassRule;
import org.junit.Test;

import org.safehaus.jettyjam.utils.JettyIntegResource;
import org.safehaus.jettyjam.utils.JettyResource;

import org.safehaus.jettyjam.utils.TestMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An integration test for the chop UI.
 */
public class RunnerAppIT {
    private final static Logger LOG = LoggerFactory.getLogger( RunnerAppIT.class );
    private final static Properties systemProperties = new Properties();

    static {
        systemProperties.setProperty( TestMode.TEST_MODE_PROPERTY, TestMode.INTEG.toString() );
    }

    @ClassRule
    public static JettyResource jetty = new JettyIntegResource( RunnerAppIT.class, systemProperties );


    @Test
    public void testStart() {
        RunnerTestUtils.testStart( jetty.newTestParams().setLogger( LOG ) );
    }


    @Test
    public void testReset() {
        RunnerTestUtils.testReset( jetty.newTestParams().setLogger( LOG ) );
    }


    @Test
    public void testStop() {
        RunnerTestUtils.testStop( jetty.newTestParams().setLogger( LOG ) );
    }


    @Test
    public void testStats() {
        RunnerTestUtils.testStats( jetty.newTestParams().setLogger( LOG ) );
    }


    @Test
    public void testStatus() {
        RunnerTestUtils.testStatus( jetty.newTestParams().setLogger( LOG ) );
    }
}
