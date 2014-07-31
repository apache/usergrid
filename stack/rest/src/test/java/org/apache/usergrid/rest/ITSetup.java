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

import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.usergrid.cassandra.CassandraResource;
import org.apache.usergrid.management.ApplicationCreator;
import org.apache.usergrid.management.ManagementService;
import org.apache.usergrid.persistence.EntityManagerFactory;
import org.apache.usergrid.security.providers.SignInProviderFactory;
import org.apache.usergrid.security.tokens.TokenService;
import org.apache.usergrid.services.ServiceManagerFactory;


/** A {@link org.junit.rules.TestRule} that sets up services. */
public class ITSetup extends ExternalResource {

    private static final Logger LOG = LoggerFactory.getLogger( ITSetup.class );
    private final CassandraResource cassandraResource;
    private final TomcatResource tomcatResource;

    private ServiceManagerFactory smf;
    private ManagementService managementService;
    private EntityManagerFactory emf;
    private ApplicationCreator applicationCreator;
    private TokenService tokenService;
    private SignInProviderFactory providerFactory;
    private Properties properties;

    private boolean setupCalled = false;
    private boolean ready = false;
    private URI uri;


    public ITSetup( CassandraResource cassandraResource) {
        this.cassandraResource = cassandraResource;
        managementService = cassandraResource.getBean( ManagementService.class );
        tomcatResource = TomcatResource.instance;
        tomcatResource.setWebAppsPath( "src/main/webapp" );
        tomcatResource.setProperties( managementService.getProperties() );
    }


    public ITSetup( CassandraResource cassandraResource, String webAppsPath ) {
        this.cassandraResource = cassandraResource;
        managementService = cassandraResource.getBean( ManagementService.class );
        tomcatResource = TomcatResource.instance;
        tomcatResource.setWebAppsPath(webAppsPath);
        tomcatResource.setProperties( managementService.getProperties() );
    }


    @Override
    protected void before() throws Throwable {
        synchronized ( cassandraResource ) {
            super.before();

            if ( !setupCalled ) {
                managementService.setup();
                setupCalled = true;
            }

            emf =                cassandraResource.getBean( EntityManagerFactory.class );
            smf =                cassandraResource.getBean( ServiceManagerFactory.class );
            properties =         cassandraResource.getBean( "properties", Properties.class );
            tokenService =       cassandraResource.getBean( TokenService.class );
            providerFactory =    cassandraResource.getBean( SignInProviderFactory.class );
            applicationCreator = cassandraResource.getBean( ApplicationCreator.class );

            tomcatResource.setCassandraPort( cassandraResource.getRpcPort() );
            tomcatResource.setElasticSearchPort( 
                    Integer.parseInt( System.getProperty("EMBEDDED_ES_PORT")) );

            tomcatResource.before();

            // Initialize Jersey Client
            uri = UriBuilder.fromUri( "http://localhost/" ).port( tomcatResource.getPort() ).build();

            ready = true;
            LOG.info( "Test setup complete..." );
        }
    }


    @Override
    protected void after() {
        tomcatResource.after();
    }


    public void protect() {
        if ( ready ) {
            return;
        }

        try {
            LOG.warn( "Calls made to access members without being ready ... initializing..." );
            before();
        }
        catch ( Throwable t ) {
            throw new RuntimeException( "Failed on before()", t );
        }
    }


    public int getTomcatPort() {
        protect();
        return tomcatResource.getPort();
    }


    public ManagementService getMgmtSvc() {
        protect();
        return managementService;
    }


    public EntityManagerFactory getEmf() {
        protect();
        return emf;
    }


    public ServiceManagerFactory getSmf() {
        protect();
        return smf;
    }


    public ApplicationCreator getAppCreator() {
        protect();
        return applicationCreator;
    }


    public TokenService getTokenSvc() {
        protect();
        return tokenService;
    }


    public Properties getProps() {
        protect();
        return properties;
    }


    public SignInProviderFactory getProviderFactory() {
        protect();
        return providerFactory;
    }


    public URI getBaseURI() {
        protect();
        return uri;
    }
}
