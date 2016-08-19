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
import org.apache.usergrid.persistence.core.CassandraFig;
import org.apache.usergrid.persistence.core.datastax.CQLUtils;
import org.apache.usergrid.persistence.core.datastax.DataStaxCluster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Singleton
public class DataStaxClusterImpl implements DataStaxCluster {

    private static final Logger logger = LoggerFactory.getLogger( DataStaxClusterImpl.class );


    private final CassandraFig cassandraFig;
    private Cluster cluster;
    private Session applicationSession;
    private Session clusterSession;

    @Inject
    public DataStaxClusterImpl(final CassandraFig cassandraFig ) throws Exception {
        this.cassandraFig = cassandraFig;
        this.cluster = buildCluster();

        // always initialize the keyspaces
        this.createApplicationKeyspace();

        logger.info("Initialized datastax cluster client. Hosts={}, Idle Timeout={}s,  Pool Timeout={}s",
            cluster.getMetadata().getAllHosts().toString(),
            cluster.getConfiguration().getPoolingOptions().getIdleTimeoutSeconds(),
            cluster.getConfiguration().getPoolingOptions().getPoolTimeoutMillis() / 1000);


    }

    @Override
    public Cluster getCluster(){

        // ensure we can build the cluster if it was previously closed
        if ( cluster.isClosed() ){
            cluster = buildCluster();
        }

        return cluster;
    }

    @Override
    public Session getClusterSession(){

        // always grab cluster from getCluster() in case it was prematurely closed
        if ( clusterSession == null || clusterSession.isClosed() ){
            clusterSession = getCluster().connect();
        }

        return clusterSession;
    }

    @Override
    public Session getApplicationSession(){

        // always grab cluster from getCluster() in case it was prematurely closed
        if ( applicationSession == null || applicationSession.isClosed() ){
            applicationSession = getCluster().connect( CQLUtils.quote(cassandraFig.getApplicationKeyspace() ) );
        }
        return applicationSession;
    }


    /**
     * Execute CQL that will create the keyspace if it doesn't exist and alter it if it does.
     * @throws Exception
     */
    @Override
    public void createApplicationKeyspace() throws Exception {

        boolean exists = getClusterSession().getCluster().getMetadata()
            .getKeyspace(CQLUtils.quote(cassandraFig.getApplicationKeyspace())) != null;

        if(exists){
            return;
        }

        final String createApplicationKeyspace = String.format(
            "CREATE KEYSPACE IF NOT EXISTS %s WITH replication = %s",
            CQLUtils.quote(cassandraFig.getApplicationKeyspace()),
            CQLUtils.getFormattedReplication(cassandraFig.getStrategy(), cassandraFig.getStrategyOptions())

        );

        getClusterSession().execute(createApplicationKeyspace);
        waitForSchemaAgreement();

        logger.info("Created keyspace: {}", cassandraFig.getApplicationKeyspace());

    }

    /**
     * Wait until all Cassandra nodes agree on the schema.  Sleeps 100ms between checks.
     *
     */
    public void waitForSchemaAgreement() {

        while ( true ) {
            if( getClusterSession().getCluster().getMetadata().checkSchemaAgreement() ){
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

    public Cluster buildCluster(){

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

        Cluster.Builder datastaxCluster = Cluster.builder()
            .withClusterName(cassandraFig.getClusterName())
            .addContactPoints(cassandraFig.getHosts().split(","))
            .withMaxSchemaAgreementWaitSeconds(30)
            .withCompression(ProtocolOptions.Compression.LZ4)
            .withLoadBalancingPolicy(loadBalancingPolicy)
            .withPoolingOptions(poolingOptions)
            .withQueryOptions(queryOptions)
            .withProtocolVersion(getProtocolVersion(cassandraFig.getVersion()));

        // only add auth credentials if they were provided
        if ( !cassandraFig.getUsername().isEmpty() && !cassandraFig.getPassword().isEmpty() ){
            datastaxCluster.withCredentials(
                cassandraFig.getUsername(),
                cassandraFig.getPassword()
            );
        }


        return datastaxCluster.build();

    }

    private ProtocolVersion getProtocolVersion(String versionNumber){

        ProtocolVersion protocolVersion;
        switch (versionNumber) {

            case "2.1":
                protocolVersion = ProtocolVersion.V3;
                break;
            case "2.0":
                protocolVersion = ProtocolVersion.V2;
                break;
            case "1.2":
                protocolVersion = ProtocolVersion.V1;
                break;
            default:
                protocolVersion = ProtocolVersion.NEWEST_SUPPORTED;
                break;

        }

        return protocolVersion;


    }

}
