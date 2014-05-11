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
package org.apache.usergrid.chop.webapp;

import com.google.inject.servlet.GuiceFilter;
import com.sun.jersey.api.client.UniformInterfaceException;
import org.junit.ClassRule;
import org.junit.Test;
import org.safehaus.jettyjam.utils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An integration test for the chop UI.
 */
public class ChopUiIT {

    private static final Logger LOG = LoggerFactory.getLogger( ChopUiIT.class );

    @JettyContext(
            enableSession = true,
            contextListeners = { @ContextListener( listener = ChopUiConfig.class ) },
            filterMappings = { @FilterMapping( filter = GuiceFilter.class, spec = "/*" ) }
    )

    @JettyConnectors(
            defaultId = "https",
            httpsConnectors = { @HttpsConnector( id = "https", port = 8443 ) }
    )

    @ClassRule
    public static JettyResource jetty = new JettyIntegResource( ChopUiIT.class, new String[] { "-e" } );


    @Test
    public void testRunManagerNext() {
        ChopUiTestUtils.testRunManagerNext( jetty.newTestParams().setLogger( LOG ) );
    }


    @Test
    public void testRunnerRegistryList() {
        ChopUiTestUtils.testRunnerRegistryList( jetty.newTestParams().setLogger( LOG ) );
    }


    @Test
    public void testRunnerRegistryRegister() {
        ChopUiTestUtils.testRunnerRegistryRegister( jetty.newTestParams().setLogger( LOG ) );
    }


    @Test
    public void testUploadRunner() throws Exception {
        ChopUiTestUtils.testUpload( jetty.newTestParams().setLogger( LOG ) );
    }


    @Test
    public void testUploadResults() throws Exception {
        ChopUiTestUtils.testStoreResults( jetty.newTestParams().setLogger( LOG ) );
    }


    @Test
    public void testRunCompleted() {
        ChopUiTestUtils.testRunCompleted( jetty.newTestParams().setLogger( LOG ) );
    }


    @Test
    public void testSetupStack() {
        ChopUiTestUtils.testSetup( jetty.newTestParams().setLogger( LOG ) );
    }


    @Test
    public void testDestroyStack() {
        ChopUiTestUtils.testDestroy( jetty.newTestParams().setLogger( LOG ) );
    }


    @Test
    public void testRunnerRegistryUnregister() {
        ChopUiTestUtils.testRunnerRegistryUnregister( jetty.newTestParams().setLogger( LOG ) );
    }


    @Test
    public void testRunnerRegistrySequence() {
        ChopUiTestUtils.testRunnerRegistrySequence( jetty.newTestParams().setLogger( LOG ) );
    }


    @Test
    public void testGet() {
        ChopUiTestUtils.testGet( jetty.newTestParams().setLogger( LOG ) );
    }


    @Test
    public void testAuthGet() {
        ChopUiTestUtils.testAuthGet( jetty.newTestParams().setLogger( LOG ) );
    }


    @Test
    public void testAuthPost() {
        ChopUiTestUtils.testAuthPost( jetty.newTestParams().setLogger( LOG ) );
    }


    @Test
    public void testAuthPostWithAllowedRole() {
        ChopUiTestUtils.testAuthPostWithAllowedRole( jetty.newTestParams().setLogger( LOG ) );
    }


    @Test
    public void testAuthGetWithAllowedRole() {
        ChopUiTestUtils.testAuthGetWithAllowedRole( jetty.newTestParams().setLogger( LOG ) );
    }


    @Test( expected = UniformInterfaceException.class )
    public void testAuthPostWithWrongCredentials() {
        ChopUiTestUtils.testAuthPostWithWrongCredentials( jetty.newTestParams().setLogger( LOG ) );
    }


    @Test( expected = UniformInterfaceException.class )
    public void testAuthPostWithUnallowedRole() {
        ChopUiTestUtils.testAuthPostWithUnallowedRole( jetty.newTestParams().setLogger( LOG ) );
    }


    @Test( expected = UniformInterfaceException.class )
    public void testAuthGetWithUnallowedRole() {
        ChopUiTestUtils.testAuthGetWithUnallowedRole( jetty.newTestParams().setLogger( LOG ) );
    }


    @Test( expected = UniformInterfaceException.class )
    public void testAuthGetWithWrongCredentials() {
        ChopUiTestUtils.testAuthGetWithWrongCredentials( jetty.newTestParams().setLogger( LOG ) );
    }
}
