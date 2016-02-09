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






    }

    public Cluster getCluster(){

        return cluster;
    }

    public Session getClusterSession(){

        if ( clusterSession == null || clusterSession.isClosed() ){
            clusterSession = cluster.connect();
        }

        return clusterSession;
    }

    public Session getApplicationSession(){

        if ( applicationSession == null || applicationSession.isClosed() ){
            applicationSession = cluster.connect( "\""+cassandraFig.getApplicationKeyspace()+"\"" );
        }
        return applicationSession;
    }


}
