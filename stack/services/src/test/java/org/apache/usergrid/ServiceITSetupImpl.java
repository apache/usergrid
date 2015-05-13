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

import org.apache.usergrid.corepersistence.GuiceFactory;
import org.apache.usergrid.corepersistence.migration.AppInfoMigrationPlugin;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.shiro.SecurityUtils;

import org.apache.usergrid.management.ApplicationCreator;
import org.apache.usergrid.management.ManagementService;
import org.apache.usergrid.management.export.ExportService;
import org.apache.usergrid.management.importer.ImportService;
import org.apache.usergrid.persistence.cassandra.CassandraService;
import org.apache.usergrid.security.providers.SignInProviderFactory;
import org.apache.usergrid.security.tokens.TokenService;
import org.apache.usergrid.services.ServiceManagerFactory;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.PropertiesFactoryBean;

import java.util.Properties;


/** A {@link org.junit.rules.TestRule} that sets up services. */
public class ServiceITSetupImpl extends CoreITSetupImpl implements ServiceITSetup {
    private static final Logger logger = LoggerFactory.getLogger( ServiceITSetupImpl.class );

    private ServiceManagerFactory smf;
    private ManagementService managementService;
    private ApplicationCreator applicationCreator;
    private TokenService tokenService;
    private SignInProviderFactory providerFactory;
    private Properties properties;
    private ExportService exportService;
    private ImportService importService;
    private AppInfoMigrationPlugin appInfoMigrationPlugin;


    public ServiceITSetupImpl() {
        super();

        managementService =  springResource.getBean( ManagementService.class );
        applicationCreator = springResource.getBean( ApplicationCreator.class );
        tokenService =       springResource.getBean( TokenService.class );
        providerFactory =    springResource.getBean( SignInProviderFactory.class );
        properties =         springResource.getBean( "properties", Properties.class );
        smf =                springResource.getBean( ServiceManagerFactory.class );
        exportService =      springResource.getBean( ExportService.class );
        importService =      springResource.getBean( ImportService.class );

        try {
            appInfoMigrationPlugin = springResource.getBean(GuiceFactory.class)
                .getObject().getInstance(AppInfoMigrationPlugin.class);
        } catch ( Exception e ) {
            logger.error("Unable to instantiate AppInfoMigrationPlugin", e);
        }

        //set our security manager for shiro
        SecurityUtils.setSecurityManager(springResource.getBean( org.apache.shiro.mgt.SecurityManager.class ));
    }


    protected void after( Description description ) {
        super.after(description);
        logger.info("Test {}: finish with application", description.getDisplayName());
    }


    protected void before( Description description ) throws Throwable {

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
        return  springResource.getBean( CassandraService.class );
    }


    @Override
    public ManagementService getMgmtSvc() {
        return managementService;
    }

    @Override
    public ExportService getExportService() { return exportService; }

    @Override
    public ImportService getImportService() { return importService; }


    public ServiceManagerFactory getSmf() {
        if ( smf == null ) {
            smf =  springResource.getBean( ServiceManagerFactory.class );
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
        return properties.getProperty(key);
    }


    @Override
    public SignInProviderFactory getProviderFactory() {
        return providerFactory;
    }

    @Override
    public AppInfoMigrationPlugin getAppInfoMigrationPlugin() {
        return appInfoMigrationPlugin;
    }

    @Override
    public void refreshIndex(){
        this.getEntityIndex().refresh();
    }
}
