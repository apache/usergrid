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


import org.junit.ClassRule;
import org.junit.Test;
import org.safehaus.jettyjam.utils.ContextListener;
import org.safehaus.jettyjam.utils.FilterMapping;
import org.safehaus.jettyjam.utils.HttpsConnector;
import org.safehaus.jettyjam.utils.JettyConnectors;
import org.safehaus.jettyjam.utils.JettyContext;
import org.safehaus.jettyjam.utils.JettyResource;
import org.safehaus.jettyjam.utils.JettyUnitResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.servlet.GuiceFilter;


/**
 * Tests the Runner.
 */
public class RunnerAppTest {
    private static final Logger LOG = LoggerFactory.getLogger( RunnerAppTest.class );

    @JettyContext(
        enableSession = true,
        contextListeners = { @ContextListener( listener = RunnerConfig.class ) },
        filterMappings = { @FilterMapping( filter = GuiceFilter.class, spec = "/*") }
    )
    @JettyConnectors(
        defaultId = "https",
        httpsConnectors = { @HttpsConnector( id = "https", port = 0 ) }
    )
    @ClassRule
    public static JettyResource jetty = new JettyUnitResource( RunnerAppTest.class );


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

