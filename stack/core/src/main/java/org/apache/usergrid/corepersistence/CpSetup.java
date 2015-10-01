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


import java.util.UUID;

import com.google.inject.Binding;
import org.apache.usergrid.corepersistence.index.IndexLocationStrategyFactory;
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.index.EntityIndex;
import org.apache.usergrid.persistence.index.EntityIndexFactory;
import org.apache.usergrid.persistence.index.IndexLocationStrategy;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.mq.cassandra.QueuesCF;
import org.apache.usergrid.persistence.EntityManagerFactory;
import org.apache.usergrid.persistence.cassandra.ApplicationCF;
import org.apache.usergrid.persistence.cassandra.CassandraService;
import org.apache.usergrid.persistence.cassandra.Setup;
import org.apache.usergrid.persistence.core.migration.data.DataMigrationManager;
import org.apache.usergrid.persistence.core.migration.schema.MigrationException;
import org.apache.usergrid.persistence.core.migration.schema.MigrationManager;
import org.apache.usergrid.persistence.exceptions.ApplicationAlreadyExistsException;
import org.apache.usergrid.persistence.exceptions.OrganizationAlreadyExistsException;

import com.google.inject.Injector;

import me.prettyprint.hector.api.ddl.ComparatorType;

import static me.prettyprint.hector.api.factory.HFactory.createColumnFamilyDefinition;
import static org.apache.usergrid.persistence.cassandra.CassandraPersistenceUtils.getCfDefs;
import static org.apache.usergrid.persistence.cassandra.CassandraService.APPLICATIONS_CF;
import static org.apache.usergrid.persistence.cassandra.CassandraService.DEFAULT_APPLICATION;
import static org.apache.usergrid.persistence.cassandra.CassandraService.DEFAULT_ORGANIZATION;
import static org.apache.usergrid.persistence.cassandra.CassandraService.MANAGEMENT_APPLICATION;
import static org.apache.usergrid.persistence.cassandra.CassandraService.PRINCIPAL_TOKEN_CF;
import static org.apache.usergrid.persistence.cassandra.CassandraService.PROPERTIES_CF;
import static org.apache.usergrid.persistence.cassandra.CassandraService.TOKENS_CF;
import static org.apache.usergrid.persistence.cassandra.CassandraService.getApplicationKeyspace;
import static org.apache.usergrid.persistence.cassandra.CassandraService.keyspaceForApplication;


/**
 * Cassandra-specific setup utilities.
 */
public class CpSetup implements Setup {

    private static final Logger logger = LoggerFactory.getLogger( CpSetup.class );


    private final Injector injector;


    private final CassandraService cass;

    private final EntityManagerFactory emf;


    /**
     * Instantiates a new setup object.
     *
     * @param emf the emf
     */
    public CpSetup( final EntityManagerFactory emf,
                    final CassandraService cassandraService, final Injector injector ) {
        this.emf = emf;
        this.cass = cassandraService;
        this.injector = injector;

    }


    @Override
    public void initSubsystems() throws Exception {
        //a no op, creating the injector creates the connections
        //init our index if required
        this.emf.initializeManagementIndex();
        setupStaticKeyspace();
        setupSystemKeyspace();

    }


    public void createDefaultApplications() throws Exception {

        setupSystemKeyspace();

        setupStaticKeyspace();

        injector.getInstance( DataMigrationManager.class ).migrate();

        try {
            emf.initializeApplicationV2( DEFAULT_ORGANIZATION, emf.getManagementAppId(), MANAGEMENT_APPLICATION, null );
        }
        catch ( ApplicationAlreadyExistsException ex ) {
            logger.warn( "Application {}/{} already exists", DEFAULT_ORGANIZATION, MANAGEMENT_APPLICATION );
        }
        catch ( OrganizationAlreadyExistsException oaee ) {
            logger.warn( "Organization {} already exists", DEFAULT_ORGANIZATION );
        }

        injector.getInstance( DataMigrationManager.class ).migrate();
    }


    /**
     * Perform migration of the 2.0 code
     */
    private void migrate() {
        MigrationManager m = injector.getInstance( MigrationManager.class );
        try {
            m.migrate();
        }
        catch ( MigrationException ex ) {
            throw new RuntimeException( "Error migrating Core Persistence", ex );
        }
    }


    @Override
    public void setupSystemKeyspace() throws Exception {

        logger.info( "Initialize system keyspace" );

        migrate();

        cass.createColumnFamily( getApplicationKeyspace(),
            createColumnFamilyDefinition( getApplicationKeyspace(), APPLICATIONS_CF, ComparatorType.BYTESTYPE ) );

        cass.createColumnFamily( getApplicationKeyspace(),
            createColumnFamilyDefinition( getApplicationKeyspace(), PROPERTIES_CF, ComparatorType.BYTESTYPE ) );

        cass.createColumnFamily( getApplicationKeyspace(),
            createColumnFamilyDefinition( getApplicationKeyspace(), TOKENS_CF, ComparatorType.BYTESTYPE ) );

        cass.createColumnFamily( getApplicationKeyspace(),
            createColumnFamilyDefinition( getApplicationKeyspace(), PRINCIPAL_TOKEN_CF, ComparatorType.UUIDTYPE ) );

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

    public void setupApplicationKeyspace( final UUID applicationId, String applicationName ) throws Exception {

        migrate();
    }


    @Override
    public void setupStaticKeyspace() throws Exception {

        migrate();

        // Need this legacy stuff for queues

        logger.info( "Creating static application keyspace " + getApplicationKeyspace() );

        cass.createColumnFamily( getApplicationKeyspace(),
            createColumnFamilyDefinition( getApplicationKeyspace(), APPLICATIONS_CF,
                ComparatorType.BYTESTYPE ) );

        cass.createColumnFamilies( getApplicationKeyspace(),
            getCfDefs( ApplicationCF.class, getApplicationKeyspace() ) );

        cass.createColumnFamilies( getApplicationKeyspace(),
            getCfDefs( QueuesCF.class, getApplicationKeyspace() ) );

    }


    @Override
    public boolean keyspacesExist() {
        return true;
    }
}
