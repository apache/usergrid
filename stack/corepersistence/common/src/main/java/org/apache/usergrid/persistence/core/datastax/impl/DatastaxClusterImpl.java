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
package org.apache.usergrid.persistence.core.datastax.impl;

import com.datastax.driver.core.*;
import com.datastax.driver.core.policies.DCAwareRoundRobinPolicy;
import com.datastax.driver.core.policies.LoadBalancingPolicy;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.usergrid.persistence.core.astyanax.CassandraFig;
import org.apache.usergrid.persistence.core.datastax.CQLUtils;
import org.apache.usergrid.persistence.core.datastax.DataStaxCluster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Singleton
public class DataStaxClusterImpl implements DataStaxCluster {

    private static final Logger logger = LoggerFactory.getLogger( DataStaxClusterImpl.class );


    private final CassandraFig cassandraFig;
    private final Cluster cluster;
    private Session applicationSession;
    private Session clusterSession;

    @Inject
    public DataStaxClusterImpl(final CassandraFig cassandraFig ) throws Exception {
        this.cassandraFig = cassandraFig;

        ConsistencyLevel defaultConsistencyLevel;
        try {
            defaultConsistencyLevel = ConsistencyLevel.valueOf(cassandraFig.getReadCl());
        } catch (IllegalArgumentException e){

            logger.error("Unable to parse provided consistency level in property: {}, defaulting to: {}",
                CassandraFig.READ_CL,
                ConsistencyLevel.LOCAL_QUORUM);

            defaultConsistencyLevel = ConsistencyLevel.LOCAL_QUORUM;
        }


        LoadBalancingPolicy loadBalancingPolicy;
        if( !cassandraFig.getLocalDataCenter().isEmpty() ){

            loadBalancingPolicy = new DCAwareRoundRobinPolicy.Builder()
                .withLocalDc( cassandraFig.getLocalDataCenter() ).build();
        }else{
            loadBalancingPolicy = new DCAwareRoundRobinPolicy.Builder().build();
        }

        final PoolingOptions poolingOptions = new PoolingOptions()
            .setCoreConnectionsPerHost(HostDistance.LOCAL, cassandraFig.getConnections() / 2)
            .setMaxConnectionsPerHost(HostDistance.LOCAL, cassandraFig.getConnections())
            .setIdleTimeoutSeconds(cassandraFig.getTimeout() / 1000)
            .setPoolTimeoutMillis(cassandraFig.getPoolTimeout());

        final QueryOptions queryOptions = new QueryOptions()
            .setConsistencyLevel(defaultConsistencyLevel);

        final Cluster.Builder datastaxCluster = Cluster.builder()
            .withClusterName(cassandraFig.getClusterName())
            .addContactPoints(cassandraFig.getHosts().split(","))
            .withCompression(ProtocolOptions.Compression.LZ4)
            .withLoadBalancingPolicy(loadBalancingPolicy)
            .withPoolingOptions(poolingOptions)
            .withQueryOptions(queryOptions)
            .withProtocolVersion(ProtocolVersion.NEWEST_SUPPORTED);

        // only add auth credentials if they were provided
        if ( !cassandraFig.getUsername().isEmpty() && !cassandraFig.getPassword().isEmpty() ){
            datastaxCluster.withCredentials(
                cassandraFig.getUsername(),
                cassandraFig.getPassword()
            );
        }

        this.cluster = datastaxCluster.build();
        logger.info("Initialized datastax cluster client. Hosts={}, Idle Timeout={}s,  Request Timeout={}s",
            cluster.getMetadata().getAllHosts().toString(),
            cluster.getConfiguration().getPoolingOptions().getIdleTimeoutSeconds(),
            cluster.getConfiguration().getPoolingOptions().getPoolTimeoutMillis() / 1000);

        createOrUpdateKeyspace();

    }

    @Override
    public Cluster getCluster(){

        return cluster;
    }

    @Override
    public Session getClusterSession(){

        if ( clusterSession == null || clusterSession.isClosed() ){
            clusterSession = cluster.connect();
        }

        return clusterSession;
    }

    @Override
    public Session getApplicationSession(){

        if ( applicationSession == null || applicationSession.isClosed() ){
            applicationSession = cluster.connect( CQLUtils.quote(cassandraFig.getApplicationKeyspace() ) );
        }
        return applicationSession;
    }

    private void createOrUpdateKeyspace() throws Exception {

        clusterSession = getClusterSession();

        final String createApplicationKeyspace = String.format(
            "CREATE KEYSPACE IF NOT EXISTS \"%s\" WITH replication = %s",
            cassandraFig.getApplicationKeyspace(),
            CQLUtils.getFormattedReplication( cassandraFig.getStrategy(), cassandraFig.getStrategyOptions() )

        );

        final String updateApplicationKeyspace = String.format(
            "ALTER KEYSPACE \"%s\" WITH replication = %s",
            cassandraFig.getApplicationKeyspace(),
            CQLUtils.getFormattedReplication( cassandraFig.getStrategy(), cassandraFig.getStrategyOptions() )
        );

        logger.info("Creating application keyspace with the following CQL: {}", createApplicationKeyspace);
        clusterSession.execute(createApplicationKeyspace);
        logger.info("Updating application keyspace with the following CQL: {}", updateApplicationKeyspace);
        clusterSession.executeAsync(updateApplicationKeyspace);

        // this session pool is only used when running database setup so close it when finished to clear resources
        clusterSession.close();

        waitForSchemaAgreement();
    }

    /**
     * Wait until all Cassandra nodes agree on the schema.  Sleeps 100ms between checks.
     *
     */
    private void waitForSchemaAgreement() {

        while ( true ) {

            if( cluster.getMetadata().checkSchemaAgreement() ){
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
