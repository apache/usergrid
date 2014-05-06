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


import com.google.inject.Guice;
import com.google.inject.Injector;
import com.netflix.config.ConfigurationManager;
import java.io.IOException;
import java.util.Properties;
import java.util.UUID;
import org.apache.usergrid.persistence.cassandra.Setup;
import org.apache.usergrid.persistence.cassandra.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.usergrid.persistence.entities.Application;

import org.apache.usergrid.persistence.EntityManagerFactory;
import static org.apache.usergrid.persistence.cassandra.CassandraService.DEFAULT_APPLICATION;
import static org.apache.usergrid.persistence.cassandra.CassandraService.DEFAULT_APPLICATION_ID;
import static org.apache.usergrid.persistence.cassandra.CassandraService.DEFAULT_ORGANIZATION;
import static org.apache.usergrid.persistence.cassandra.CassandraService.MANAGEMENT_APPLICATION;
import static org.apache.usergrid.persistence.cassandra.CassandraService.MANAGEMENT_APPLICATION_ID;
import org.apache.usergrid.persistence.core.migration.MigrationException;
import org.apache.usergrid.persistence.core.migration.MigrationManager;


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


    /**
     * Initialize.
     *
     * @throws Exception the exception
     */
    @Override
    public synchronized void setup() throws Exception {
        createDefaultApplications();
    }


    @Override
    public void init() throws Exception {
        cass.init();

        try {
            logger.info("Loading Core Persistence properties");

            ConfigurationManager.loadCascadedPropertiesFromResources( "corepersistence" );
            Properties testProps = new Properties() {{
                put("cassandra.hosts", "localhost");
                put("cassandra.port", System.getProperty("cassandra.rpc_port"));
            }};
            ConfigurationManager.loadProperties( testProps );
        }
        catch ( IOException e ) {
            throw new RuntimeException( "Cannot do much without properly loading our configuration.", e );
        }

        Injector injector = CpSetup.getInjector();
        MigrationManager m = injector.getInstance( MigrationManager.class );
        try {
            m.migrate();
        } catch (MigrationException ex) {
            throw new RuntimeException("Error migrating Core Persistence", ex);
        }
    }


    public void createDefaultApplications() throws Exception {

        logger.info("Setting up default applications");

        UUID DEFAULT_APP_ID    = UUID.fromString("b6768a08-b5d5-11e3-a495-11ddb1de66c8");
        UUID MANAGEMENT_APP_ID = UUID.fromString("b6768a08-b5d5-11e3-a495-11ddb1de66c9");

        emf.initializeApplication( DEFAULT_ORGANIZATION, DEFAULT_APP_ID, DEFAULT_APPLICATION, null );
        emf.initializeApplication( DEFAULT_ORGANIZATION, MANAGEMENT_APP_ID, MANAGEMENT_APPLICATION, null );
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
    }

    @Override
    public void setupStaticKeyspace() throws Exception {
    }

    @Override
    public boolean keyspacesExist() {
        return true;
    }

    static class SystemDefaults {
        private static final Application managementApp = new Application( MANAGEMENT_APPLICATION_ID );
        private static final Application defaultApp = new Application( DEFAULT_APPLICATION_ID );
        static {
            managementApp.setName( MANAGEMENT_APPLICATION );
            defaultApp.setName( DEFAULT_APPLICATION );
        }
    }
}
