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
package org.apache.usergrid.chop.integ;


import java.util.Properties;

import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.safehaus.jettyjam.utils.JettyIntegResource;
import org.safehaus.jettyjam.utils.JettyResource;
import org.safehaus.jettyjam.utils.StartResources;
import org.safehaus.jettyjam.utils.TestMode;

import static junit.framework.TestCase.assertTrue;


/**
 * Integration tests the various interactions between runners and the coordinator.
 *
 * This integration test starts up the chop web UI as a jetty jam integ resource
 * and then proceeds to start up two runners generated from the example project
 * using chop:runner.
 */
public class RunnerCoordinatorIT {
    private static final String[] webappArgs = new String[] { "-e" };

    private final static Properties systemProperties = new Properties();

    static {
        systemProperties.setProperty( TestMode.TEST_MODE_PROPERTY, TestMode.UNIT.toString() );
        systemProperties.setProperty( "archaius.deployment.environment", "INTEG" );
    }

    private static JettyResource webapp = new JettyIntegResource( RunnerCoordinatorIT.class, "jettyjam-webapp.properties", webappArgs );
    private static JettyResource runner1 = new JettyIntegResource( RunnerCoordinatorIT.class, systemProperties );
    private static JettyResource runner2 = new JettyIntegResource( RunnerCoordinatorIT.class, systemProperties );

    @ClassRule
    public static StartResources resources = new StartResources( 1000, webapp, runner1, runner2 );

    @Test
    public void testBasic() {
        assertTrue( webapp.isStarted() );
        assertTrue( runner1.isStarted() );
        assertTrue( runner2.isStarted() );
    }
}
