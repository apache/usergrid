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


    public static final String CASSANDRA_HOSTS = "cassandra.hosts";
    public static final String CASSANDRA_VERSION = "cassandra.version";
    public static final String CASSANDRA_CLUSTER_NAME = "cassandra.cluster_name";
    public static final String COLLECTIONS_KEYSPACE = "collections.keyspace";
    public static final String CASSANDRA_PORT = "cassandra.port";
    public static final String CASSANDRA_CONNECTIONS = "cassandra.connections";
    public static final String CASSANDRA_TIMEOUT = "cassandra.timeout";
    public static final String CASSANDRA_DISCOVERY = "cassandra.discovery";

    @Key( CASSANDRA_HOSTS )
    String getHosts();

    @Key(CASSANDRA_VERSION)
    @Default("1.2")
    String getVersion();

    @Key(CASSANDRA_CLUSTER_NAME)
    @Default("Usergrid")
    String getClusterName();

    @Key(COLLECTIONS_KEYSPACE)
    @Default("Usergrid_Collections")
    String getKeyspaceName();

    @Key(CASSANDRA_PORT)
    @Default("9160")
    int getThriftPort();

    @Key(CASSANDRA_CONNECTIONS)
    @Default("100")
    int getConnections();

    @Key(CASSANDRA_TIMEOUT)
    @Default("5000")
    int getTimeout();

    @Key(CASSANDRA_DISCOVERY)
    @Default("RING_DESCRIBE")
    String getDiscoveryType();
}
