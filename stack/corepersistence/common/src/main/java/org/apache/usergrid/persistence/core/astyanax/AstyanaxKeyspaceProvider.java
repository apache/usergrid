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


import java.util.HashSet;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.netflix.astyanax.AstyanaxConfiguration;
import com.netflix.astyanax.AstyanaxContext;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.connectionpool.ConnectionPoolConfiguration;
import com.netflix.astyanax.connectionpool.NodeDiscoveryType;
import com.netflix.astyanax.connectionpool.impl.ConnectionPoolConfigurationImpl;
import com.netflix.astyanax.connectionpool.impl.Slf4jConnectionPoolMonitorImpl;
import com.netflix.astyanax.impl.AstyanaxConfigurationImpl;
import com.netflix.astyanax.thrift.ThriftFamilyFactory;


/**
 * TODO.  Provide the ability to do a service hook for realtime tuning without the need of a JVM restart This could be
 * done with governator and service discovery
 *
 * @author tnine
 */
@Singleton
public class AstyanaxKeyspaceProvider implements Provider<Keyspace> {
    private final CassandraFig cassandraFig;
    private final CassandraConfig cassandraConfig;


    @Inject
    public AstyanaxKeyspaceProvider( final CassandraFig cassandraFig, final CassandraConfig cassandraConfig) {
        this.cassandraFig = cassandraFig;
        this.cassandraConfig = cassandraConfig;
    }


    @Override
    public Keyspace get() {

        AstyanaxConfiguration config = new AstyanaxConfigurationImpl()
                .setDiscoveryType( NodeDiscoveryType.valueOf( cassandraFig.getDiscoveryType() ) )
                .setTargetCassandraVersion( cassandraFig.getVersion() )
                .setDefaultReadConsistencyLevel( cassandraConfig.getReadCL() )
                .setDefaultWriteConsistencyLevel( cassandraConfig.getWriteCL() );

        ConnectionPoolConfiguration connectionPoolConfiguration =
                new ConnectionPoolConfigurationImpl( "UsergridConnectionPool" )
                        .setPort( cassandraFig.getThriftPort() )
                        .setLocalDatacenter( cassandraFig.getLocalDataCenter() )
                        .setMaxConnsPerHost( cassandraFig.getConnections() )
                        .setSeeds( cassandraFig.getHosts() )
                        .setSocketTimeout( cassandraFig.getTimeout() );

        AstyanaxContext<Keyspace> context =
                new AstyanaxContext.Builder().forCluster( cassandraFig.getClusterName() )
                        .forKeyspace( cassandraFig.getApplicationKeyspace())

                        /*
                         * TODO tnine Filter this by adding a host supplier.  We will get token discovery from cassandra
                         * but only connect
                         * to nodes that have been specified.  Good for real time updates of the cass system without
                         * adding
                         * load to them during runtime
                         */

                        .withAstyanaxConfiguration( config )
                        .withConnectionPoolConfiguration( connectionPoolConfiguration )
                        .withConnectionPoolMonitor( new Slf4jConnectionPoolMonitorImpl() )
                        .buildKeyspace( ThriftFamilyFactory.getInstance() );

        context.start();


        return context.getClient();
    }


}
