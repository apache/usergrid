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
package org.apache.usergrid.corepersistence;


import java.io.IOException;
import java.util.Properties;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.mq.cassandra.QueuesCF;
import org.apache.usergrid.persistence.EntityManagerFactory;
import org.apache.usergrid.persistence.cassandra.ApplicationCF;
import org.apache.usergrid.persistence.cassandra.CassandraService;
import org.apache.usergrid.persistence.cassandra.Setup;
import org.apache.usergrid.persistence.core.migration.MigrationException;
import org.apache.usergrid.persistence.core.migration.MigrationManager;
import org.apache.usergrid.persistence.entities.Application;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.netflix.config.ConfigurationManager;

import me.prettyprint.cassandra.service.CassandraHost;
import me.prettyprint.hector.api.ddl.ComparatorType;

import static me.prettyprint.hector.api.factory.HFactory.createColumnFamilyDefinition;
import org.apache.commons.lang.StringUtils;
import static org.apache.usergrid.persistence.cassandra.CassandraPersistenceUtils.getCfDefs;
import static org.apache.usergrid.persistence.cassandra.CassandraService.APPLICATIONS_CF;
import static org.apache.usergrid.persistence.cassandra.CassandraService.DEFAULT_APPLICATION;
import static org.apache.usergrid.persistence.cassandra.CassandraService.DEFAULT_ORGANIZATION;
import static org.apache.usergrid.persistence.cassandra.CassandraService.MANAGEMENT_APPLICATION;
import static org.apache.usergrid.persistence.cassandra.CassandraService.PRINCIPAL_TOKEN_CF;
import static org.apache.usergrid.persistence.cassandra.CassandraService.PROPERTIES_CF;
import static org.apache.usergrid.persistence.cassandra.CassandraService.STATIC_APPLICATION_KEYSPACE;
import static org.apache.usergrid.persistence.cassandra.CassandraService.SYSTEM_KEYSPACE;
import static org.apache.usergrid.persistence.cassandra.CassandraService.TOKENS_CF;
import static org.apache.usergrid.persistence.cassandra.CassandraService.USE_VIRTUAL_KEYSPACES;
import static org.apache.usergrid.persistence.cassandra.CassandraService.keyspaceForApplication;


/**
 * Cassandra-specific setup utilities.
 */
public class CpSetup implements Setup {

    private static final Logger logger = LoggerFactory.getLogger( CpSetup.class );

    private final org.apache.usergrid.persistence.EntityManagerFactory emf;
    private final CassandraService cass;

    private GuiceModule gm;


    /**
     * Instantiates a new setup object.
     *
     * @param emf the emf
     */
    public CpSetup( EntityManagerFactory emf, CassandraService cass ) {
        this.emf = emf;
        this.cass = cass;
    }


    private static Injector injector = null;

    public static Injector getInjector() {
        if ( injector == null ) {
            injector = Guice.createInjector( new GuiceModule() ); 
        }
        return injector;
    }


    @Override
    public void init() throws Exception {
        cass.init();

        try {
            logger.info("Loading Core Persistence properties");

            String hostsString = "";
            CassandraHost[] hosts = cass.getCassandraHostConfigurator().buildCassandraHosts();
            if ( hosts.length == 0 ) {
                throw new RuntimeException("Fatal error: no Cassandra hosts configured");
            }
            String sep = "";
            for ( CassandraHost host : hosts ) {
                if (StringUtils.isEmpty(host.getHost())) {
                    throw new RuntimeException("Fatal error: Cassandra hostname cannot be empty");
                }
                hostsString = hostsString + sep + host.getHost();
                sep = ",";
            }

            logger.info("hostsString: " + hostsString);

            Properties cpProps = new Properties();

            // Some Usergrid properties must be mapped to Core Persistence properties
            cpProps.put("cassandra.hosts", hostsString);
            cpProps.put("cassandra.port", hosts[0].getPort());
            cpProps.put("cassandra.cluster_name", cass.getProperties().get("cassandra.cluster"));

            cpProps.put("collections.keyspace.strategy.class", 
                    cass.getProperties().get("cassandra.keyspace.strategy"));

            cpProps.put("collections.keyspace.strategy.options", "replication_factor:" +  
                    cass.getProperties().get("cassandra.keyspace.replication"));

            cpProps.put("cassandra.keyspace.strategy.options.replication_factor",
                    cass.getProperties().get("cassandra.keyspace.replication"));

            logger.debug("Set Cassandra properties for Core Persistence: " + cpProps.toString() );

            // Make all Usergrid properties into Core Persistence config
            cpProps.putAll( cass.getProperties() );
            logger.debug("All properties fed to Core Persistence: " + cpProps.toString() );

            ConfigurationManager.loadProperties( cpProps );
        }
        catch ( Exception e ) {
            throw new RuntimeException( "Fatal error loading configuration.", e );
        }

        Injector injector = CpSetup.getInjector();
        MigrationManager m = injector.getInstance( MigrationManager.class );
        try {
            m.migrate();
        } catch (MigrationException ex) {
            throw new RuntimeException("Error migrating Core Persistence", ex);
        }

        setupSystemKeyspace();

        setupStaticKeyspace();

        createDefaultApplications();
    }


    public void createDefaultApplications() throws Exception {

        logger.info("Setting up default applications");

        emf.initializeApplication( DEFAULT_ORGANIZATION, 
                emf.getDefaultAppId(), DEFAULT_APPLICATION, null );

        emf.initializeApplication( DEFAULT_ORGANIZATION, 
                emf.getManagementAppId(), MANAGEMENT_APPLICATION, null );
    }


    /** @return staticly constructed reference to the management application */
    public static Application getManagementApp() {
        return SystemDefaults.managementApp;
    }


    /** @return statically constructed reference to the default application */
    public static Application getDefaultApp() {
        return SystemDefaults.defaultApp;
    }

    @Override
    public void setupSystemKeyspace() throws Exception {

        logger.info( "Initialize system keyspace" );

        cass.createColumnFamily( SYSTEM_KEYSPACE, createColumnFamilyDefinition( 
                SYSTEM_KEYSPACE, APPLICATIONS_CF, ComparatorType.BYTESTYPE ) );

        cass.createColumnFamily( SYSTEM_KEYSPACE, createColumnFamilyDefinition( 
                SYSTEM_KEYSPACE, PROPERTIES_CF, ComparatorType.BYTESTYPE ) );

        cass.createColumnFamily( SYSTEM_KEYSPACE, createColumnFamilyDefinition( 
                SYSTEM_KEYSPACE, TOKENS_CF, ComparatorType.BYTESTYPE ) );

        cass.createColumnFamily( SYSTEM_KEYSPACE, createColumnFamilyDefinition( 
                SYSTEM_KEYSPACE, PRINCIPAL_TOKEN_CF, ComparatorType.UUIDTYPE ) );

        logger.info( "System keyspace initialized" );
    }

    
    /**
     * Initialize application keyspace.
     *
     * @param applicationId the application id
     * @param applicationName the application name
     *
     * @throws Exception the exception
     */

    public void setupApplicationKeyspace( 
            final UUID applicationId, String applicationName ) throws Exception {

        // Need this legacy stuff for queues

        if ( !USE_VIRTUAL_KEYSPACES ) {

            String app_keyspace = keyspaceForApplication( applicationId );

            logger.info( "Creating application keyspace " + app_keyspace 
                    + " for " + applicationName + " application" );

            cass.createColumnFamily( app_keyspace, 
                createColumnFamilyDefinition( 
                    SYSTEM_KEYSPACE, APPLICATIONS_CF, ComparatorType.BYTESTYPE ) );

            cass.createColumnFamilies( app_keyspace, 
                getCfDefs( ApplicationCF.class, app_keyspace ) );

            cass.createColumnFamilies( app_keyspace, 
                getCfDefs( QueuesCF.class, app_keyspace ) );
        }
    }


    @Override
    public void setupStaticKeyspace() throws Exception {

        // Need this legacy stuff for queues

        if ( USE_VIRTUAL_KEYSPACES ) {

            logger.info( "Creating static application keyspace " + STATIC_APPLICATION_KEYSPACE );

            cass.createColumnFamily( STATIC_APPLICATION_KEYSPACE,
                    createColumnFamilyDefinition( STATIC_APPLICATION_KEYSPACE, APPLICATIONS_CF,
                            ComparatorType.BYTESTYPE ) );

            cass.createColumnFamilies( STATIC_APPLICATION_KEYSPACE,
                    getCfDefs( ApplicationCF.class, STATIC_APPLICATION_KEYSPACE ) );

            cass.createColumnFamilies( STATIC_APPLICATION_KEYSPACE,
                    getCfDefs( QueuesCF.class, STATIC_APPLICATION_KEYSPACE ) );
        }
    }

    @Override
    public boolean keyspacesExist() {
        return true;
    }

    static class SystemDefaults {

        private static final Application managementApp = 
                new Application( CpEntityManagerFactory.MANAGEMENT_APPLICATION_ID);

        private static final Application defaultApp = 
                new Application( CpEntityManagerFactory.DEFAULT_APPLICATION_ID );

        static {
            managementApp.setName( MANAGEMENT_APPLICATION );
            defaultApp.setName( DEFAULT_APPLICATION );
        }
    }
}
