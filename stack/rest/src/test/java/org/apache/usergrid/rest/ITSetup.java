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
import static org.apache.usergrid.persistence.index.impl.EsProvider.LOCAL_ES_PORT_PROPNAME;
import org.apache.usergrid.security.providers.SignInProviderFactory;
import org.apache.usergrid.security.tokens.TokenService;
import org.apache.usergrid.services.ServiceManagerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;


/** A {@link org.junit.rules.TestRule} that sets up services. */
public class ITSetup extends ExternalResource {

    private static final Logger LOG = LoggerFactory.getLogger( ITSetup.class );
    private final ElasticSearchResource elasticSearchResource;
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
        
        try {
            String[] locations = { "usergrid-properties-context.xml" };
            ConfigurableApplicationContext appContext = 
                    new ClassPathXmlApplicationContext( locations );
            
            properties = (Properties)appContext.getBean("properties");

        } catch (Exception ex) {
            throw new RuntimeException("Error getting properties", ex);
        }

        this.cassandraResource = cassandraResource;

        tomcatResource = TomcatResource.instance;
        tomcatResource.setWebAppsPath( "src/main/webapp" );

        elasticSearchResource = ElasticSearchResource.instance;

    }


    public ITSetup( CassandraResource cassandraResource, String webAppsPath ) {
        this( cassandraResource );
        tomcatResource.setWebAppsPath(webAppsPath);
    }


    @Override
    protected void before() throws Throwable {
        synchronized ( cassandraResource ) {
            super.before();

            elasticSearchResource.before();

            emf =                cassandraResource.getBean( EntityManagerFactory.class );
            smf =                cassandraResource.getBean( ServiceManagerFactory.class );
            tokenService =       cassandraResource.getBean( TokenService.class );
            providerFactory =    cassandraResource.getBean( SignInProviderFactory.class );
            applicationCreator = cassandraResource.getBean( ApplicationCreator.class );
            managementService =  cassandraResource.getBean( ManagementService.class );

            if ( !setupCalled ) {
                managementService.setup();
                setupCalled = true;
            }

            String esStartup = properties.getProperty("elasticsearch.startup");
            if ( "embedded".equals(esStartup)) {
                tomcatResource.setCassandraPort( cassandraResource.getRpcPort() );
                tomcatResource.setElasticSearchPort( 
                    Integer.parseInt( System.getProperty(LOCAL_ES_PORT_PROPNAME)) );
                
            } else {
                tomcatResource.setCassandraPort( cassandraResource.getRpcPort() );
                tomcatResource.setElasticSearchPort(elasticSearchResource.getPort());
            }

            tomcatResource.before();

            // Initialize Jersey Client
            uri = UriBuilder.fromUri("http://localhost/").port( tomcatResource.getPort() ).build();

            ready = true;
            LOG.info( "Test setup complete..." );
        }
    }


    @Override
    protected void after() {
        emf.flushEntityManagerCaches();
        tomcatResource.after();
        elasticSearchResource.after();
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
