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
package org.apache.usergrid.persistence.core;


import org.safehaus.guicyfig.Default;
import org.safehaus.guicyfig.FigSingleton;
import org.safehaus.guicyfig.GuicyFig;
import org.safehaus.guicyfig.Key;


/**
 * Cassandra configuration interface.
 */
@FigSingleton
public interface CassandraFig extends GuicyFig {

    // cassndra properties used by datastax driver
    String READ_CL = "cassandra.readcl";
    String READ_CL_CONSISTENT = "cassandra.readcl.consistent";
    String WRITE_CL = "cassandra.writecl";

    String STRATEGY = "cassandra.strategy";
    String STRATEGY_OPTIONS = "cassandra.strategy.options";

    String STRATEGY_LOCAL = "cassandra.strategy.local";
    String STRATEGY_OPTIONS_LOCAL = "cassandra.strategy.options.local";

    // main application cassandra properties
    String ASTYANAX_READ_CONSISTENT_CL = "usergrid.consistent.read.cl";
    String ASTYANAX_READ_CL = "usergrid.read.cl";
    String ASTYANAX_WRITE_CL = "usergrid.write.cl";
    String SHARD_VALUES = "cassandra.shardvalues";
    String THRIFT_TRANSPORT_SIZE = "cassandra.thrift.transport.frame";
    String USERNAME = "cassandra.username";
    String PASSWORD = "cassandra.password";

    // locks cassandra properties
    String LOCKS_KEYSPACE_NAME = "cassandra.lock.keyspace";
    String LOCKS_KEYSPACE_REPLICATION = "cassandra.lock.keyspace.replication";
    String LOCKS_KEYSPACE_STRATEGY = "cassandra.lock.keyspace.strategy";
    String LOCKS_CL = "cassandra.lock.cl";
    String LOCKS_SHARED_POOL_FLAG = "cassandra.lock.use_shared_pool";
    String LOCKS_CONNECTIONS = "cassandra.lock.connections";
    String LOCKS_EXPIRATION = "cassandra.lock.expiration.milliseconds";

    String LOCK_MANAGER_INIT_RETRIES = "cassandra.lock.init.retries";
    String LOCK_MANAGER_INIT_INTERVAL = "cassandra.lock.init.interval";

    // re-usable default values
    String DEFAULT_CONNECTION_POOLSIZE = "15";
    String DEFAULT_LOCKS_EXPIRATION = "3600000";  // 1 hour
    String DEFAULT_LOCAL_DC = "";
    String DEFAULT_USERNAME = "";
    String DEFAULT_PASSWORD = "";


    @Key( "cassandra.hosts" )
    String getHosts();

    /**
     * Valid options are 1.2, 2.0, 2.1
     *
     * @return
     */
    @Key( "cassandra.version" )
    @Default( "2.1" )
    String getVersion();

    @Key( "cassandra.cluster_name" )
    @Default( "Usergrid" )
    String getClusterName();

    @Key( "cassandra.keyspace.application" )
    @Default( "Usergrid_Applications" )
    String getApplicationKeyspace();

    /** Prefix for local keyspace name. Name will be this prefix plus "_" plus local data center name. */
    @Key( "cassandra.keyspace.application.local" )
    @Default( "Usergrid_Applications_Local" )
    String getApplicationLocalKeyspace();

    @Key( "cassandra.port" )
    @Default( "9160" )
    int getThriftPort();

    @Key( USERNAME )
    @Default( DEFAULT_USERNAME )
    String getUsername();

    @Key( PASSWORD )
    @Default( DEFAULT_PASSWORD )
    String getPassword();

    @Key( "cassandra.datacenter.local" )
    @Default( DEFAULT_LOCAL_DC )
    String getLocalDataCenter();

    @Key( "cassandra.connections" )
    @Default( DEFAULT_CONNECTION_POOLSIZE )
    int getConnections();

    @Key( "cassandra.timeout" )
    @Default( "10000" )
    int getTimeout();

    @Key( "cassandra.timeout.pool" )
    @Default( "5000" )
    int getPoolTimeout();

    @Key("cassandra.discovery")
    @Default( "RING_DESCRIBE" )
    String getDiscoveryType();


    @Default("CL_LOCAL_QUORUM")
    @Key(ASTYANAX_READ_CL)
    String getAstyanaxReadCL();

    @Default("CL_QUORUM")
    @Key(ASTYANAX_READ_CONSISTENT_CL)
    String getAstyanaxConsistentReadCL();

    @Default("CL_LOCAL_QUORUM")
    @Key(ASTYANAX_WRITE_CL)
    String getAstyanaxWriteCL();


    @Default("LOCAL_QUORUM")
    @Key(READ_CL)
    String getReadCl();

    @Default("QUORUM")
    @Key(READ_CL_CONSISTENT)
    String getReadClConsistent();

    @Default("LOCAL_QUORUM")
    @Key(WRITE_CL)
    String getWriteCl();

    @Default("SimpleStrategy")
    @Key( STRATEGY )
    String getStrategy();

    @Default("replication_factor:1")
    @Key( STRATEGY_OPTIONS )
    String getStrategyOptions();

    @Default("SimpleStrategy")
    @Key( STRATEGY_LOCAL )
    String getStrategyLocal();

    @Default("replication_factor:1")
    @Key( STRATEGY_OPTIONS_LOCAL )
    String getStrategyOptionsLocal();

    /**
     * Return the history of all shard values which are immutable.  For instance, if shard values
     * are initially set to 20 (the default) then increased to 40, the property should contain the string of
     * "20, 40" so that we can read historic data.
     *
     * @return
     */
    @Default("20")
    @Key(SHARD_VALUES)
    String getShardValues();

    /**
     * Get the thrift transport size.  Should be set to what is on the cassandra servers.  As we move to CQL, this will become obsolete
     * @return
     */
    @Key( THRIFT_TRANSPORT_SIZE)
    @Default( "15728640" )
    int getThriftBufferSize();


    /**
     * Returns the name of the keyspace that should be used for Locking
     */
    @Key( LOCKS_KEYSPACE_NAME )
    @Default("Locks")
    String getLocksKeyspace();

    /**
     * Returns the Astyanax consistency level for writing a Lock
     */
    @Key(LOCKS_CL)
    @Default("CL_LOCAL_QUORUM")
    String getLocksCl();

    /**
     * Returns a flag on whether or not to share the connection pool with other keyspaces
     */
    @Key( LOCKS_SHARED_POOL_FLAG )
    @Default("true")
    boolean useSharedPoolForLocks();

    /**
     * Returns a flag on whether or not to share the connection pool with other keyspaces
     */
    @Key( LOCKS_CONNECTIONS )
    @Default( DEFAULT_CONNECTION_POOLSIZE )
    int getConnectionsLocks();

    /**
     * Returns a flag on whether or not to share the connection pool with other keyspaces
     */
    @Key( LOCKS_KEYSPACE_REPLICATION )
    @Default("replication_factor:1")
    String getLocksKeyspaceReplication();

    /**
     * Returns a flag on whether or not to share the connection pool with other keyspaces
     */
    @Key( LOCKS_KEYSPACE_STRATEGY )
    @Default( "org.apache.cassandra.locator.SimpleStrategy" )
    String getLocksKeyspaceStrategy();

    /**
     * Return the expiration that should be used for expiring a lock if it's not released
     */
    @Key( LOCKS_EXPIRATION )
    @Default(DEFAULT_LOCKS_EXPIRATION)
    int getLocksExpiration();

    /**
     * How many times to attempt lock keyspace and column family creation
     */
    @Key( LOCK_MANAGER_INIT_RETRIES )
    @Default( "100" )
    int getLockManagerInitRetries();

    /**
     * Return the expiration that should be used for expiring a lock if it's not released
     */
    @Key( LOCK_MANAGER_INIT_INTERVAL )
    @Default( "1000" )
    int getLockManagerInitInterval();

}
