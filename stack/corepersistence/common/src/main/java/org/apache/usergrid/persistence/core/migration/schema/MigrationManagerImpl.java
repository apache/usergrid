/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.usergrid.persistence.core.migration.schema;


import java.util.Collection;
import java.util.Set;

import com.datastax.driver.core.Session;
import org.apache.usergrid.persistence.core.astyanax.CassandraFig;
import org.apache.usergrid.persistence.core.datastax.CQLUtils;
import org.apache.usergrid.persistence.core.datastax.DataStaxCluster;
import org.apache.usergrid.persistence.core.datastax.TableDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.core.astyanax.MultiTenantColumnFamilyDefinition;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.ddl.ColumnFamilyDefinition;
import com.netflix.astyanax.ddl.KeyspaceDefinition;


/**
 * Implementation of the migration manager to set up column families / tables
 *
 * @author tnine
 */
@Singleton
public class MigrationManagerImpl implements MigrationManager {


    private static final Logger logger = LoggerFactory.getLogger( MigrationManagerImpl.class );

    private final CassandraFig cassandraFig;
    private final Set<Migration> migrations;
    private final Keyspace keyspace;
    private final DataStaxCluster dataStaxCluster;


    @Inject
    public MigrationManagerImpl( final CassandraFig cassandraFig, final Keyspace keyspace,
                                 final Set<Migration> migrations, final DataStaxCluster dataStaxCluster) {

        this.cassandraFig = cassandraFig;
        this.keyspace = keyspace;
        this.migrations = migrations;
        this.dataStaxCluster = dataStaxCluster;
    }


    @Override
    public void migrate() throws MigrationException {

        try {

            dataStaxCluster.createOrUpdateKeyspace();

            for ( Migration migration : migrations ) {

                final Collection<MultiTenantColumnFamilyDefinition> columnFamilies = migration.getColumnFamilies();

                final Collection<TableDefinition> tables = migration.getTables();


                if ((columnFamilies == null || columnFamilies.size() == 0) &&
                    (tables == null || tables.size() == 0)) {
                    logger.warn(
                        "Class {} implements {} but returns null for getColumnFamilies and getTables for migration.  Either implement this method or remove the interface from the class",
                        migration.getClass().getSimpleName(), Migration.class.getSimpleName());
                    continue;
                }

                if (columnFamilies != null && !columnFamilies.isEmpty()) {
                    for (MultiTenantColumnFamilyDefinition cf : columnFamilies) {
                        testAndCreateColumnFamilyDef(cf);
                    }
                }


                if ( tables != null && !tables.isEmpty() ) {
                    for (TableDefinition tableDefinition : tables) {

                        createTable(tableDefinition);

                    }
                }


            }
        }
        catch ( Throwable t ) {
            logger.error( "Unable to perform migration", t );
            throw new MigrationException( "Unable to perform migration", t );
        }
    }


    /**
     * Check if the column family exists.  If it dosn't create it
     */
    private void testAndCreateColumnFamilyDef( MultiTenantColumnFamilyDefinition columnFamily )
            throws ConnectionException {
        final KeyspaceDefinition keyspaceDefinition = keyspace.describeKeyspace();

        final ColumnFamilyDefinition existing =
                keyspaceDefinition.getColumnFamily( columnFamily.getColumnFamily().getName() );

        if ( existing != null ) {
            return;
        }

        keyspace.createColumnFamily( columnFamily.getColumnFamily(), columnFamily.getOptions() );

        logger.info( "Created column family {}", columnFamily.getColumnFamily().getName() );

        dataStaxCluster.waitForSchemaAgreement();
    }

    private void createTable(TableDefinition tableDefinition ) throws Exception {

        String CQL = CQLUtils.getTableCQL( tableDefinition, CQLUtils.ACTION.CREATE );
        if (logger.isDebugEnabled()){
            logger.debug( CQL );
        }
        dataStaxCluster.getApplicationSession()
            .execute( CQL );

        logger.info("Created table: {}", tableDefinition.getTableName());

        dataStaxCluster.waitForSchemaAgreement();
    }



}
