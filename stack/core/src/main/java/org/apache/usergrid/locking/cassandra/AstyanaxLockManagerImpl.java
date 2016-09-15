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
package org.apache.usergrid.locking.cassandra;


import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.connectionpool.exceptions.NoAvailableHostsException;
import com.netflix.astyanax.connectionpool.exceptions.PoolTimeoutException;
import com.netflix.astyanax.ddl.ColumnFamilyDefinition;
import com.netflix.astyanax.ddl.KeyspaceDefinition;
import com.netflix.astyanax.model.ColumnFamily;
import com.netflix.astyanax.model.ConsistencyLevel;
import com.netflix.astyanax.recipes.locks.ColumnPrefixDistributedRowLock;
import com.netflix.astyanax.serializers.StringSerializer;
import org.apache.cassandra.db.marshal.BytesType;
import org.apache.cassandra.db.marshal.UTF8Type;
import org.apache.usergrid.locking.Lock;
import org.apache.usergrid.locking.LockManager;
import org.apache.usergrid.locking.LockPathBuilder;
import org.apache.usergrid.persistence.core.CassandraFig;
import org.apache.usergrid.persistence.core.astyanax.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Singleton
public class AstyanaxLockManagerImpl implements LockManager {

    private static final Logger logger = LoggerFactory.getLogger( AstyanaxLockManagerImpl.class );
    private static final String CF_NAME = "AstyanaxLocks";


    private final CassandraFig cassandraFig;
    private final CassandraCluster cassandraCluster;
    private Keyspace keyspace;
    private ColumnFamily columnFamily;
    private static final int MINIMUM_LOCK_EXPIRATION = 60000; // 1 minute


    @Inject
    public AstyanaxLockManagerImpl(CassandraFig cassandraFig,
                                   CassandraCluster cassandraCluster ) throws ConnectionException {

        this.cassandraFig = cassandraFig;
        this.cassandraCluster = cassandraCluster;
        this.keyspace = cassandraCluster.getLocksKeyspace();
    }


    @Override
    public void setup() {
        try {
            createLocksKeyspace();
            columnFamily = createLocksColumnFamily();
        } catch (ConnectionException e) {
            throw new RuntimeException( "Error setting up locks keyspace and column family", e );
        }
    }


    @Override
    public Lock createLock(final UUID applicationId, final String... path ) {

        String lockPath = LockPathBuilder.buildPath( applicationId, path );

        ConsistencyLevel consistencyLevel;
        try{
            consistencyLevel = ConsistencyLevel.valueOf(cassandraFig.getLocksCl());
        }catch(IllegalArgumentException e){

            logger.warn( "Property {} value provided: {} is not valid", CassandraFig.LOCKS_CL,
                cassandraFig.getLocksCl() );

            // just default it to local quorum if we can't parse
            consistencyLevel = ConsistencyLevel.CL_LOCAL_QUORUM;
        }


        int lockExpiration;
        int lockConfigExpiration = cassandraFig.getLocksExpiration();
        if( lockConfigExpiration >= MINIMUM_LOCK_EXPIRATION ){

            lockExpiration = Math.min(cassandraFig.getLocksExpiration(), Integer.MAX_VALUE);

        }else{

            logger.warn("Property {} is not valid.  Choose a value greater than or equal to {}",
                CassandraFig.LOCKS_EXPIRATION,
                MINIMUM_LOCK_EXPIRATION);

            // use the default if seomthing below the minimum is provided
            lockExpiration = Integer.valueOf(CassandraFig.DEFAULT_LOCKS_EXPIRATION);
        }



        ColumnPrefixDistributedRowLock<String> lock =
            new ColumnPrefixDistributedRowLock<>(keyspace, getLocksColumnFamily(), lockPath)
                .expireLockAfter( lockExpiration, TimeUnit.MILLISECONDS)
                .withConsistencyLevel(consistencyLevel);


        return new AstyanaxLockImpl( lock );

    }


    private ColumnFamily getLocksColumnFamily() {

        if ( columnFamily == null ) {

            columnFamily = ColumnFamily.newColumnFamily(
                CF_NAME, StringSerializer.get(), StringSerializer.get() );

            if ( logger.isDebugEnabled() ) {

                try {
                    final KeyspaceDefinition kd = keyspace.describeKeyspace();
                    final ColumnFamilyDefinition cfd = kd.getColumnFamily( columnFamily.getName() );
                    Map<String, Object> options = new HashMap<>( 1 );
                    options.put( "gc_grace_seconds", cfd.getGcGraceSeconds() );
                    options.put( "caching", cfd.getCaching() );
                    options.put( "compaction_strategy", cfd.getCompactionStrategy() );
                    options.put( "compaction_strategy_options", cfd.getCompactionStrategyOptions() );
                    logger.debug( "Locks column family {} exists with options: {}", cfd.getName(), options);

                } catch ( ConnectionException ce ) {
                    logger.warn("Error connecting to Cassandra for debug column family info", ce);
                }
            }
        }

        return columnFamily;
    }


    private ColumnFamily createLocksColumnFamily() throws ConnectionException {

        ColumnFamily<String, String> cflocks = ColumnFamily.newColumnFamily(
            CF_NAME, StringSerializer.get(), StringSerializer.get() );

        final KeyspaceDefinition kd = keyspace.describeKeyspace();
        final ColumnFamilyDefinition cfdef = kd.getColumnFamily( cflocks.getName() );

        if ( cfdef == null ) {

            // create only if does not already exist

            MultiTenantColumnFamilyDefinition mtcfd = new MultiTenantColumnFamilyDefinition(
                cflocks,
                BytesType.class.getSimpleName(),
                UTF8Type.class.getSimpleName(),
                BytesType.class.getSimpleName(),
                MultiTenantColumnFamilyDefinition.CacheOption.ALL
            );

            Map<String, Object> cfOptions = mtcfd.getOptions();

            // Additionally set the gc grace low
            cfOptions.put( "gc_grace_seconds", 60 );

            keyspace.createColumnFamily( mtcfd.getColumnFamily(), cfOptions );

            logger.info( "Created column family {}", mtcfd.getOptions() );

            cflocks = mtcfd.getColumnFamily();

        } else {
            return getLocksColumnFamily();
        }

        return cflocks;
    }


    private void createLocksKeyspace() throws ConnectionException {

        ImmutableMap.Builder<String, Object> strategyOptions = getKeySpaceProps();

        ImmutableMap<String, Object> options =
            ImmutableMap.<String, Object>builder().put( "strategy_class", cassandraFig.getLocksKeyspaceStrategy() )
                .put( "strategy_options", strategyOptions.build() ).build();


        keyspace.createKeyspaceIfNotExists( options );

        strategyOptions.toString();

        logger.info( "Keyspace {} created or already exists with options {}",
            keyspace.getKeyspaceName(),
            options.toString() );
    }

    /**
     * Get keyspace properties
     */
    private ImmutableMap.Builder<String, Object> getKeySpaceProps() {
        ImmutableMap.Builder<String, Object> keyspaceProps = ImmutableMap.<String, Object>builder();

        String optionString = cassandraFig.getLocksKeyspaceReplication();

        if(optionString == null){
            return keyspaceProps;
        }


        for ( String key : optionString.split( "," ) ) {

            final String[] options = key.split( ":" );
            if (options.length > 0) {
                keyspaceProps.put(options[0], options[1]);
            }
        }

        return keyspaceProps;
    }
}
