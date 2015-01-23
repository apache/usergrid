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
package org.apache.usergrid;


import java.util.Properties;

import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.apache.usergrid.cassandra.SpringResource;
import org.apache.usergrid.management.ApplicationCreator;
import org.apache.usergrid.management.ManagementService;
import org.apache.usergrid.management.export.ExportService;
import org.apache.usergrid.persistence.cassandra.CassandraService;
import org.apache.usergrid.persistence.index.impl.ElasticSearchResource;
import org.apache.usergrid.security.providers.SignInProviderFactory;
import org.apache.usergrid.security.tokens.TokenService;
import org.apache.usergrid.services.ServiceManagerFactory;


/** A {@link org.junit.rules.TestRule} that sets up services. */
public class ServiceITSetupImpl extends CoreITSetupImpl implements ServiceITSetup {
    private static final Logger LOG = LoggerFactory.getLogger( ServiceITSetupImpl.class );

    private ServiceManagerFactory smf;
    private ManagementService managementService;
    private ApplicationCreator applicationCreator;
    private TokenService tokenService;
    private SignInProviderFactory providerFactory;
    private Properties properties;
    private ExportService exportService;


    public ServiceITSetupImpl(  ) {
    }


    protected void after( Description description ) {
        super.after( description );
        LOG.info( "Test {}: finish with application", description.getDisplayName() );
    }


    protected void before( Description description ) throws Throwable {
        super.before( description );
        managementService =  SpringResource.getInstance().getBean( ManagementService.class );
        applicationCreator =  SpringResource.getInstance().getBean( ApplicationCreator.class );
        tokenService =  SpringResource.getInstance().getBean( TokenService.class );
        providerFactory =  SpringResource.getInstance().getBean( SignInProviderFactory.class );
        properties =  SpringResource.getInstance().getBean( PropertiesFactoryBean.class ).getObject();
        smf =  SpringResource.getInstance().getBean( ServiceManagerFactory.class );
        exportService =  SpringResource.getInstance().getBean( ExportService.class );

        LOG.info( "Test setup complete..." );
    }


    @Override
    public Statement apply( final Statement base, final Description description ) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                before( description );

                try {
                    base.evaluate();
                }
                finally {
                    after( description );
                }
            }
        };
    }


    @Override
    public CassandraService getCassSvc() {
        return  SpringResource.getInstance().getBean( CassandraService.class );
    }


    @Override
    public ManagementService getMgmtSvc() {
        return managementService;
    }

    @Override
    public ExportService getExportService() { return exportService; }


    public ServiceManagerFactory getSmf() {
        if ( smf == null ) {
            smf =  SpringResource.getInstance().getBean( ServiceManagerFactory.class );
        }

        return smf;
    }


    @Override
    public ApplicationCreator getAppCreator() {
        return applicationCreator;
    }


    @Override
    public TokenService getTokenSvc() {
        return tokenService;
    }


    @Override
    public Properties getProps() {
        return properties;
    }


    @Override
    public Object set( String key, String value ) {
        return properties.setProperty( key, value );
    }


    @Override
    public String get( String key ) {
        return properties.getProperty( key );
    }


    @Override
    public SignInProviderFactory getProviderFactory() {
        return providerFactory;
    }
}
