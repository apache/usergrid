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

import static org.apache.usergrid.persistence.cassandra.CassandraPersistenceUtils.getCfDefs;
import static org.apache.usergrid.persistence.cassandra.CassandraService.DEFAULT_ORGANIZATION;
import static org.apache.usergrid.persistence.cassandra.CassandraService.MANAGEMENT_APPLICATION;
import static org.apache.usergrid.persistence.cassandra.CassandraService.getApplicationKeyspace;


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
    public void initSchema(boolean forceCheckSchema) throws Exception {

        // Initialize the management app index in Elasticsearch
        this.emf.initializeManagementIndex();

        // Create the schema (including keyspace) in Cassandra
        setupSchema(forceCheckSchema);
        setupLegacySchema();

    }


    @Override
    public void initMgmtApp() throws Exception {


        try {
            emf.initializeApplicationV2( DEFAULT_ORGANIZATION, emf.getManagementAppId(),
                MANAGEMENT_APPLICATION, null, false);
        }
        catch ( ApplicationAlreadyExistsException ex ) {
            logger.warn( "Application {}/{} already exists", DEFAULT_ORGANIZATION, MANAGEMENT_APPLICATION );
        }
        catch ( OrganizationAlreadyExistsException oaee ) {
            logger.warn( "Organization {} already exists", DEFAULT_ORGANIZATION );
        }

    }


    @Override
    public void runDataMigration() throws Exception {

        injector.getInstance( DataMigrationManager.class ).migrate();

    }

    private void setupLegacySchema() throws Exception {

        logger.info( "Initialize keyspace and legacy column families" );

        cass.createColumnFamilies( getApplicationKeyspace(),
            getCfDefs( ApplicationCF.class, getApplicationKeyspace() ) );

        cass.createColumnFamilies( getApplicationKeyspace(),
            getCfDefs( QueuesCF.class, getApplicationKeyspace() ) );

        logger.info( "Keyspace and legacy column families initialized" );
    }


    /**
     * Initialize schema from the new 2.x Migration classes which contain schema individually
     *
     * @param forceCheckSchema
     */

    private void setupSchema(boolean forceCheckSchema) throws Exception {

        MigrationManager m = injector.getInstance( MigrationManager.class );
        try {
            m.migrate(forceCheckSchema);
        }
        catch ( MigrationException ex ) {
            throw new RuntimeException( "Error migrating Core Persistence", ex );
        }
    }

}
