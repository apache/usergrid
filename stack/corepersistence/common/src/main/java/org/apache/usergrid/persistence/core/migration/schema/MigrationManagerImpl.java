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


import com.datastax.driver.core.KeyspaceMetadata;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.ddl.ColumnFamilyDefinition;
import com.netflix.astyanax.ddl.KeyspaceDefinition;
import org.apache.usergrid.persistence.core.CassandraFig;
import org.apache.usergrid.persistence.core.astyanax.MultiTenantColumnFamilyDefinition;
import org.apache.usergrid.persistence.core.datastax.CQLUtils;
import org.apache.usergrid.persistence.core.datastax.DataStaxCluster;
import org.apache.usergrid.persistence.core.datastax.TableDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;


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
    public void migrate(boolean forceCheckKeyspaces) throws MigrationException {

        try {

            dataStaxCluster.createApplicationKeyspace(forceCheckKeyspaces);

            dataStaxCluster.createApplicationLocalKeyspace(forceCheckKeyspaces);

            for ( Migration migration : migrations ) {

                final Collection<MultiTenantColumnFamilyDefinition> columnFamilies = migration.getColumnFamilies();

                final Collection<TableDefinition> tables = migration.getTables();


                if ((columnFamilies == null || columnFamilies.size() == 0) &&
                    (tables == null || tables.size() == 0)) {
                    logger.warn(
                        "Class {} implements {} but returns null for getColumnFamilies and " +
                            "getTables for migration.  Either implement this method or remove " +
                            "the interface from the class",
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
            logger.info("Not creating columnfamily {}, it already exists.", columnFamily.getColumnFamily().getName());
            return;
        }

        keyspace.createColumnFamily( columnFamily.getColumnFamily(), columnFamily.getOptions() );

        // the CF def creation uses Asytanax, so manually check the schema agreement
        astyanaxWaitForSchemaAgreement();

        logger.info( "Created column family {}", columnFamily.getColumnFamily().getName() );

    }

    private void createTable(TableDefinition tableDefinition ) throws Exception {

        // this snippet will use the drivers metadata info, but that doesn't play nice with tests
        // use the system table to verify instead

        //KeyspaceMetadata keyspaceMetadata = dataStaxCluster.getClusterSession().getCluster().getMetadata()
        //    .getKeyspace(CQLUtils.quote( tableDefinition.getKeyspace() ) );
        //boolean exists =  keyspaceMetadata != null
        //    && keyspaceMetadata.getTable( tableDefinition.getTableName() ) != null;

        boolean exists = dataStaxCluster.getClusterSession()
            .execute("select * from system.schema_columnfamilies where keyspace_name='"+tableDefinition.getKeyspace()
                +"' and columnfamily_name='"+CQLUtils.unquote(tableDefinition.getTableName())+"'").one() != null;

        if( exists ){
            logger.info("Not creating table {}, it already exists.", tableDefinition.getTableName());
            return;
        }

        String CQL = tableDefinition.getTableCQL(cassandraFig, TableDefinition.ACTION.CREATE);
        if (logger.isDebugEnabled()) {
            logger.debug(CQL);
        }

        if ( tableDefinition.getKeyspace().equals( cassandraFig.getApplicationKeyspace() )) {
            dataStaxCluster.getApplicationSession().execute( CQL );
        } else {
            dataStaxCluster.getApplicationLocalSession().execute( CQL );
        }

        logger.info("Created table: {} in keyspace {}",
            tableDefinition.getTableName(), tableDefinition.getKeyspace());

    }

    private void astyanaxWaitForSchemaAgreement() throws ConnectionException {

        while ( true ) {

            final Map<String, List<String>> versions = keyspace.describeSchemaVersions();

            if ( versions != null && versions.size() == 1 ) {
                return;
            }

            //sleep and try it again
            try {
                Thread.sleep( 100 );
            }
            catch ( InterruptedException e ) {
                //swallow
            }
        }
    }



}
