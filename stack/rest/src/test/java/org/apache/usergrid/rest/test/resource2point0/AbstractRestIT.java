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
package org.apache.usergrid.rest.test.resource2point0;


import java.net.URI;
import java.net.URLClassLoader;
import java.util.Arrays;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.usergrid.rest.test.resource2point0.endpoints.ApplicationsResource;
import org.apache.usergrid.rest.test.resource2point0.endpoints.OrganizationResource;
import org.junit.ClassRule;
import org.junit.Rule;

import org.apache.usergrid.java.client.Client;
import org.apache.usergrid.rest.ITSetup;
import org.apache.usergrid.rest.RestITSuite;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.test.framework.AppDescriptor;
import com.sun.jersey.test.framework.JerseyTest;
import com.sun.jersey.test.framework.WebAppDescriptor;
import com.sun.jersey.test.framework.spi.container.TestContainerFactory;

import static org.junit.Assert.assertEquals;



/**
 * How would we get the client from here
 */
public class AbstractRestIT extends JerseyTest {

    private static ClientConfig clientConfig = new DefaultClientConfig();

    protected static Client client;

    @ClassRule
    public static ITSetup setup = new ITSetup( RestITSuite.cassandraResource );
//
//    TODO: Allow the client to be setup seperately
    @Rule
    public ClientSetup clientSetup = new ClientSetup(setup.getBaseURI().toString());

    protected static final AppDescriptor descriptor;

    public AbstractRestIT() {
        super( descriptor );
    }


    protected ObjectMapper mapper = new ObjectMapper();

    static {
        clientConfig.getFeatures().put( JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE );
        descriptor = new WebAppDescriptor.Builder( "org.apache.usergrid.rest" )
                .clientConfig( clientConfig ).build();
        dumpClasspath( AbstractRestIT.class.getClassLoader() );
    }

    public static void dumpClasspath( ClassLoader loader ) {
        System.out.println( "Classloader " + loader + ":" );

        if ( loader instanceof URLClassLoader ) {
            URLClassLoader ucl = ( URLClassLoader ) loader;
            System.out.println( "\t" + Arrays.toString( ucl.getURLs() ) );
        }
        else {
            System.out.println( "\t(cannot display components as not a URLClassLoader)" );
        }

        if ( loader.getParent() != null ) {
            dumpClasspath( loader.getParent() );
        }
    }

    @Override
    protected URI getBaseURI() {
        return setup.getBaseURI();
    }

    @Override
    protected TestContainerFactory getTestContainerFactory() {
        return new com.sun.jersey.test.framework.spi.container.external.ExternalTestContainerFactory();
    }

    protected OrganizationResource getOrganizationResource(){
        return clientSetup.restClient.org(clientSetup.getOrganization().getName());
    }

    protected ApplicationsResource getApplicationResource(){
        return clientSetup.restClient.org(clientSetup.getOrganization().getName()).app(clientSetup.getAppName());
    }

    protected ApplicationsResource getAppResource(){
        return getApplicationResource();
    }


    public void errorParse(Integer expectedStatus, String expectedErrorMessage, UniformInterfaceException uie){
        assertEquals((Integer)400,expectedStatus);
        JsonNode errorJson = uie.getResponse().getEntity( JsonNode.class );
        assertEquals( expectedErrorMessage, errorJson.get( "error" ).asText() );

    }
}
