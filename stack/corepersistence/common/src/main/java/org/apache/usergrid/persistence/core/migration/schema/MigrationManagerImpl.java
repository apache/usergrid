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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.core.astyanax.MultiTennantColumnFamilyDefinition;
import org.apache.usergrid.persistence.core.migration.util.AstayanxUtils;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.connectionpool.exceptions.BadRequestException;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.connectionpool.exceptions.NotFoundException;
import com.netflix.astyanax.ddl.ColumnFamilyDefinition;
import com.netflix.astyanax.ddl.KeyspaceDefinition;


/**
 * Implementation of the migration manager to set up keyspace
 *
 * @author tnine
 */
@Singleton
public class MigrationManagerImpl implements MigrationManager {


    private static final Logger logger = LoggerFactory.getLogger( MigrationManagerImpl.class );

    private final Set<Migration> migrations;
    private final Keyspace keyspace;

    private final MigrationManagerFig fig;


    @Inject
    public MigrationManagerImpl( final Keyspace keyspace, final Set<Migration> migrations,
                                 MigrationManagerFig fig ) {
        this.keyspace = keyspace;
        this.migrations = migrations;
        this.fig = fig;
    }


    @Override
    public void migrate() throws MigrationException {


        try {

            testAndCreateKeyspace();

            for ( Migration migration : migrations ) {

                final Collection<MultiTennantColumnFamilyDefinition> columnFamilies = migration.getColumnFamilies();


                if ( columnFamilies == null || columnFamilies.size() == 0 ) {
                    logger.warn(
                            "Class {} implements {} but returns null column families for migration.  Either implement"
                                    + " this method or remove the interface from the class", migration.getClass(),
                            Migration.class );
                    continue;
                }

                for ( MultiTennantColumnFamilyDefinition cf : columnFamilies ) {
                    testAndCreateColumnFamilyDef( cf );
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
    private void testAndCreateColumnFamilyDef( MultiTennantColumnFamilyDefinition columnFamily )
            throws ConnectionException {
        final KeyspaceDefinition keyspaceDefinition = keyspace.describeKeyspace();

        final ColumnFamilyDefinition existing =
                keyspaceDefinition.getColumnFamily( columnFamily.getColumnFamily().getName() );

        if ( existing != null ) {
            return;
        }

        keyspace.createColumnFamily( columnFamily.getColumnFamily(), columnFamily.getOptions() );

        logger.info( "Created column family {}", columnFamily.getColumnFamily().getName() );

        waitForMigration();
    }


    /**
     * Check if they keyspace exists.  If it doesn't create it
     */
    private void testAndCreateKeyspace() throws ConnectionException {


        KeyspaceDefinition keyspaceDefinition = null;

        try {
            keyspaceDefinition = keyspace.describeKeyspace();

        }catch( NotFoundException nfe){
            //if we execute this immediately after a drop keyspace in 1.2.x, Cassandra is returning the NFE instead of a BadRequestException
            //swallow and log, then continue to create the keyspaces.
            logger.info( "Received a NotFoundException when attempting to describe keyspace.  It does not exist" );
        }
        catch(Exception e){
            AstayanxUtils.isKeyspaceMissing("Unable to connect to cassandra", e);
        }


        if ( keyspaceDefinition != null ) {
            return;
        }


        ImmutableMap.Builder<String, Object> strategyOptions = getKeySpaceProps();


        ImmutableMap<String, Object> options =
                ImmutableMap.<String, Object>builder().put( "strategy_class", fig.getStrategyClass() )
                            .put( "strategy_options", strategyOptions.build() ).build();


        keyspace.createKeyspace( options );

        strategyOptions.toString();

        logger.info( "Created keyspace {} with options {}", keyspace.getKeyspaceName(), options.toString() );

        waitForMigration();
    }


    /**
     * Get keyspace properties
     */
    private ImmutableMap.Builder<String, Object> getKeySpaceProps() {
        ImmutableMap.Builder<String, Object> keyspaceProps = ImmutableMap.<String, Object>builder();

        String optionString = fig.getStrategyOptions();

        if(optionString == null){
            return keyspaceProps;
        }



        for ( String key : optionString.split( "," ) ) {

            final String[] options = key.split( ":" );

            keyspaceProps.put( options[0], options[1] );
        }

        return keyspaceProps;
    }


    private void waitForMigration() throws ConnectionException {

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
