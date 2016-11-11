/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.usergrid.persistence.qakka.api;


import org.apache.usergrid.persistence.qakka.KeyspaceDropper;
import org.apache.usergrid.persistence.qakka.api.impl.StartupListener;
import org.apache.usergrid.persistence.qakka.api.impl.JerseyResourceConfig;
import org.glassfish.jersey.test.DeploymentContext;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.ServletDeploymentContext;
import org.junit.BeforeClass;

import javax.ws.rs.core.Application;


abstract public class AbstractRestTest extends JerseyTest {

    static Application app;

    static DeploymentContext context = null;

    static { new KeyspaceDropper(); }
    

    @BeforeClass
    public static void startCassandra() throws Exception {
        //EmbeddedCassandraServerHelper.startEmbeddedCassandra("/cassandra.yaml");
    }
    
    @Override
    protected Application configure() {
        if ( app == null ) {
            app = new JerseyResourceConfig();
        }
        return app;
    }

    @Override
    protected DeploymentContext configureDeployment() {
        if ( context == null ) {
            context = ServletDeploymentContext.builder( configure() ) .addListener( StartupListener.class ).build();
        }
        return context;
    }
}

