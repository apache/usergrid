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
package org.apache.usergrid.persistence.core.astyanax;


import org.safehaus.guicyfig.Default;
import org.safehaus.guicyfig.FigSingleton;
import org.safehaus.guicyfig.GuicyFig;
import org.safehaus.guicyfig.Key;



/**
 * Cassandra configuration interface.
 */
@FigSingleton
public interface CassandraFig extends GuicyFig {


    public static final String READ_CONSISTENT_CL = "usergrid.consistent.read.cl";

    public static final String READ_CL = "usergrid.read.cl";

    public static final String WRITE_CL = "usergrid.write.cl";

    public static final String SHARD_VALUES = "cassandra.shardvalues";

    public static final String THRIFT_TRANSPORT_SIZE = "cassandra.thrift.transport.frame";


    @Key( "cassandra.hosts" )
    String getHosts();

    @Key( "cassandra.version" )
    @Default( "2.1" )
    String getVersion();

    @Key( "cassandra.cluster_name" )
    @Default( "Usergrid" )
    String getClusterName();

    @Key( "cassandra.keyspace.application" )
    @Default( "Usergrid_Applications" )
    String getApplicationKeyspace();

    @Key( "cassandra.port" )
    @Default( "9160" )
    int getThriftPort();

    @Key( "cassandra.datacenter.local" )
    String getLocalDataCenter();

    @Key( "cassandra.connections" )
    @Default( "15" )
    int getConnections();

    @Key( "cassandra.timeout" )
    @Default( "5000" )
    int getTimeout();

    @Key("cassandra.discovery")
    @Default( "RING_DESCRIBE" )
    String getDiscoveryType();


    @Default("CL_LOCAL_QUORUM")
    @Key(READ_CL)
    String getReadCL();

    @Default("CL_QUORUM")
    @Key(READ_CONSISTENT_CL)
    String getConsistentReadCL();

    @Default("CL_LOCAL_QUORUM")
    @Key(WRITE_CL)
    String getWriteCL();

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



}
