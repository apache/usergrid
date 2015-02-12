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
package org.apache.usergrid.rest;


import java.net.URI;
import java.util.Properties;

import javax.ws.rs.core.UriBuilder;

import org.apache.usergrid.cassandra.SpringResource;
import org.apache.usergrid.management.ApplicationCreator;
import org.apache.usergrid.management.ManagementService;
import org.apache.usergrid.persistence.EntityManagerFactory;
import org.apache.usergrid.security.providers.SignInProviderFactory;
import org.apache.usergrid.security.tokens.TokenService;
import org.apache.usergrid.services.ServiceManagerFactory;
import org.apache.usergrid.setup.ConcurrentProcessSingleton;


/** A {@link org.junit.rules.TestRule} that sets up services. */
public class ITSetup  {

    private ServiceManagerFactory smf;
    private EntityManagerFactory emf;
    private ApplicationCreator applicationCreator;
    private TokenService tokenService;
    private SignInProviderFactory providerFactory;
    private Properties properties;
    private ManagementService managementService;

    private URI uri;

    private SpringResource springResource;


    public ITSetup( ) {

        this.springResource = ConcurrentProcessSingleton.getInstance().getSpringResource();

        //start tomcat



        emf =                springResource.getBean( EntityManagerFactory.class );
        smf =                springResource.getBean( ServiceManagerFactory.class );
        tokenService =       springResource.getBean( TokenService.class );
        providerFactory =    springResource.getBean( SignInProviderFactory.class );
        applicationCreator = springResource.getBean( ApplicationCreator.class );
        managementService =  springResource.getBean( ManagementService.class );


        // Initialize Jersey Client
        //TODO, make this port a resource that's filtered by maven build for the port number
        uri = UriBuilder.fromUri("http://localhost/").port( 8080 ).build();


    }



    public int getTomcatPort() {
        return 8080;
    }


    public ManagementService getMgmtSvc() {
        return managementService;
    }


    public EntityManagerFactory getEmf() {
        return emf;
    }


    public ServiceManagerFactory getSmf() {
        return smf;
    }


    public ApplicationCreator getAppCreator() {
        return applicationCreator;
    }


    public TokenService getTokenSvc() {
        return tokenService;
    }


    public Properties getProps() {
        return properties;
    }


    public SignInProviderFactory getProviderFactory() {
        return providerFactory;
    }


    public URI getBaseURI() {
        return uri;
    }
}
