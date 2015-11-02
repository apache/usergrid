/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.usergrid.testapp;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.test.DeploymentContext;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.spi.TestContainer;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Application;
import java.net.URI;
import java.net.URISyntaxException;

import static junit.framework.Assert.assertEquals;


public class OrganizationTest extends JerseyTest {
    private static final Logger logger = LoggerFactory.getLogger( TomcatMain.class );
    
    // TomcatRuntime is copied from Usergrid
    public static TomcatRuntime tomcatRuntime = TomcatRuntime.getInstance();

    private static ClientConfig clientConfig = new ClientConfig();
    static {
        clientConfig.register( new JacksonFeature() );
    }

    @Override
    protected Application configure() {
        return new Application();
    }

    @Test
    public void testGetManagementStatus() {
        
        String path = "/management/status";
        logger.info( "***** Testing against URI: {}{}", getBaseUri(), path );
        String responseString = getClient().target( getBaseUri() + path ).request().get( String.class );
        
        assertEquals( "OK", responseString );
    }

    @Test
    public void testGetOrganizations() {
        
        String path = "/management/organizations";
        logger.info( "***** Testing against URI: {}{}", getBaseUri(), path );
        ApiResponse response = getClient().target( getBaseUri() + path ).request().get( ApiResponse.class );
        
        assertEquals( "All Organizations", response.getContent() );
        assertEquals( 2, response.getEntities().size() );
    }
    
    @Test
    public void testGetOrganization() {
        
        String path = "/management/organizations/1";
        logger.info( "***** Testing against URI: {}{}", getBaseUri(), path );
        ApiResponse response = getClient().target( getBaseUri() + path ).request().get( ApiResponse.class );
        
        assertEquals( "organization:1", response.getContent() );
        assertEquals( 1, response.getEntities().size() );
    }
    
    // Returns a do-nothing test container, we're using TomcatRuntime instead.
    @Override
    protected TestContainerFactory getTestContainerFactory() {
        final URI baseURI = getBaseUri();
        
        return new TestContainerFactory() {
            @Override
            public TestContainer create(URI uri, DeploymentContext deploymentContext) {
                return new TestContainer() {

                    @Override
                    public ClientConfig getClientConfig() {
                        return clientConfig;
                    }

                    @Override
                    public URI getBaseUri() {
                        return baseURI;
                    }

                    @Override
                    public void start() {
                        // noop
                    }

                    @Override
                    public void stop() {
                        // noop
                    }
                };
            }
        };

    }

    protected URI getBaseUri() {
        try {
            return new URI("http://localhost:" + tomcatRuntime.getPort());
        } catch (URISyntaxException e) {
            throw new RuntimeException("Error determining baseURI", e);
        }
    }
    
}
