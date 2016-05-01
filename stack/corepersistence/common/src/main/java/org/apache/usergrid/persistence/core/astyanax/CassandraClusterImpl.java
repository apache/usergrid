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


import java.util.HashMap;
import java.util.Map;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.astyanax.AstyanaxConfiguration;
import com.netflix.astyanax.AstyanaxContext;
import com.netflix.astyanax.Cluster;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.connectionpool.ConnectionPoolConfiguration;
import com.netflix.astyanax.connectionpool.NodeDiscoveryType;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.connectionpool.impl.ConnectionPoolConfigurationImpl;
import com.netflix.astyanax.connectionpool.impl.SimpleAuthenticationCredentials;
import com.netflix.astyanax.connectionpool.impl.Slf4jConnectionPoolMonitorImpl;
import com.netflix.astyanax.impl.AstyanaxConfigurationImpl;
import com.netflix.astyanax.thrift.ThriftFamilyFactory;
import org.apache.usergrid.persistence.core.CassandraFig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * TODO.  Provide the ability to do a service hook for realtime tuning without the need of a JVM restart This could be
 * done with governator and service discovery
 *
 * @author tnine
 */
@Singleton
public class CassandraClusterImpl implements CassandraCluster {

    private static final Logger logger = LoggerFactory.getLogger( CassandraClusterImpl.class );


    private final CassandraFig cassandraFig;
    private final CassandraConfig cassandraConfig;
    private final Map<String, Keyspace> keyspaces = new HashMap<>(2);


    @Inject
    public CassandraClusterImpl(final CassandraFig cassandraFig, final CassandraConfig cassandraConfig) {
        this.cassandraFig = cassandraFig;
        this.cassandraConfig = cassandraConfig;



        AstyanaxConfiguration config = new AstyanaxConfigurationImpl()
            .setDiscoveryType( NodeDiscoveryType.valueOf( cassandraFig.getDiscoveryType() ) )
            .setTargetCassandraVersion( cassandraFig.getVersion() )
            .setDefaultReadConsistencyLevel( cassandraConfig.getReadCL() )
            .setDefaultWriteConsistencyLevel( cassandraConfig.getWriteCL() )
            .setMaxThriftSize( cassandraFig.getThriftBufferSize() );


        if(cassandraFig.useSharedPoolForLocks()){

            ConnectionPoolConfiguration sharedPoolConfig =
                getConnectionPoolConfig( "ConnectionPool-Shared", cassandraFig.getConnections());

            AstyanaxContext<Cluster> sharedClusterContext =
                getCluster( cassandraFig.getClusterName(), config, sharedPoolConfig );

            sharedClusterContext.start();
            Cluster sharedCluster = sharedClusterContext.getClient();


            try {
                addKeyspace( sharedCluster, cassandraFig.getApplicationKeyspace());
                addKeyspace( sharedCluster, cassandraFig.getLocksKeyspace());
            } catch (Exception e) {
                throw new RuntimeException( "Unable to create keyspace clients");
            }



        }else{

            ConnectionPoolConfiguration applicationPoolConfig =
                getConnectionPoolConfig( "ConnectionPool-Application", cassandraFig.getConnections());

            ConnectionPoolConfiguration locksPoolConfig =
                getConnectionPoolConfig( "ConnectionPool-Locks", cassandraFig.getConnectionsLocks());

            AstyanaxContext<Cluster> applicationClusterContext =
                getCluster( cassandraFig.getClusterName(), config, applicationPoolConfig );

            AstyanaxContext<Cluster> locksClusterContext =
                getCluster( cassandraFig.getClusterName() + "-Locks", config, locksPoolConfig );


            applicationClusterContext.start();
            locksClusterContext.start();

            Cluster applicationCluster = applicationClusterContext.getClient();
            Cluster locksCluster = locksClusterContext.getClient();


            try {
                addKeyspace( applicationCluster, cassandraFig.getApplicationKeyspace());
                addKeyspace( locksCluster, cassandraFig.getLocksKeyspace());
            } catch (Exception e) {
                throw new RuntimeException( "Unable to create keyspace clients");
            }


        }



    }


    @Override
    public Map<String, Keyspace> getKeyspaces() {

        return keyspaces;

    }


    @Override
    public Keyspace getApplicationKeyspace() {

        return keyspaces.get( cassandraFig.getApplicationKeyspace() );

    }

    @Override
    public Keyspace getLocksKeyspace() {

        return keyspaces.get( cassandraFig.getLocksKeyspace() );

    }






    private ConnectionPoolConfiguration getConnectionPoolConfig ( final String poolName, final int poolSize ){

        ConnectionPoolConfiguration config;
        final String username = cassandraFig.getUsername();
        final String password = cassandraFig.getPassword();

        if ( username != null && !username.isEmpty() && password != null && !password.isEmpty() ){

            config = new ConnectionPoolConfigurationImpl( poolName )
                .setPort( cassandraFig.getThriftPort() )
                .setLocalDatacenter( cassandraFig.getLocalDataCenter() )
                .setMaxConnsPerHost( poolSize )
                .setSeeds( cassandraFig.getHosts() )
                .setConnectTimeout( cassandraFig.getTimeout() )
                .setSocketTimeout( cassandraFig.getTimeout() )
                .setAuthenticationCredentials(new SimpleAuthenticationCredentials( username, password));

        } else {

            // create instance of the connection pool without credential if they are not set
            config = new ConnectionPoolConfigurationImpl( poolName )
                .setPort( cassandraFig.getThriftPort() )
                .setLocalDatacenter( cassandraFig.getLocalDataCenter() )
                .setMaxConnsPerHost( poolSize )
                .setSeeds( cassandraFig.getHosts() )
                .setSocketTimeout( cassandraFig.getTimeout() )
                .setConnectTimeout( cassandraFig.getTimeout() );
        }


        return config;

    }

    private AstyanaxContext<Cluster> getCluster ( final String clusterName,
                                                  final AstyanaxConfiguration astyanaxConfiguration,
                                                  final ConnectionPoolConfiguration poolConfig ) {

        return new AstyanaxContext.Builder().forCluster( clusterName )
            .withAstyanaxConfiguration( astyanaxConfiguration )
            .withConnectionPoolConfiguration( poolConfig )
            .withConnectionPoolMonitor( new Slf4jConnectionPoolMonitorImpl())
            .buildCluster( ThriftFamilyFactory.getInstance() );

    }

    private void addKeyspace (Cluster cluster, String keyspaceName) throws Exception {

        try {
            keyspaces.put( keyspaceName, cluster.getKeyspace( keyspaceName ) );
        } catch (ConnectionException e) {

            logger.error("Unable to get keyspace client for keyspace: {}, detail: {}",
                keyspaceName,
                e.getMessage());

            throw e;
        }


    }


}
