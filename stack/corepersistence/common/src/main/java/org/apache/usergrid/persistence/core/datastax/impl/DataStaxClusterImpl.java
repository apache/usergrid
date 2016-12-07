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
import com.datastax.driver.core.exceptions.NoHostAvailableException;
import com.datastax.driver.core.policies.DCAwareRoundRobinPolicy;
import com.datastax.driver.core.policies.LoadBalancingPolicy;
import com.datastax.driver.core.policies.Policies;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.usergrid.persistence.core.CassandraConfig;
import org.apache.usergrid.persistence.core.CassandraFig;
import org.apache.usergrid.persistence.core.datastax.CQLUtils;
import org.apache.usergrid.persistence.core.datastax.DataStaxCluster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.lang.StringUtils.isBlank;


@Singleton
public class DataStaxClusterImpl implements DataStaxCluster {

    private static final Logger logger = LoggerFactory.getLogger( DataStaxClusterImpl.class );


    private final CassandraConfig cassandraConfig;
    private Cluster cluster;
    private Session applicationSession;
    private Session queueMessageSession;
    private Session clusterSession;

    @Inject
    public DataStaxClusterImpl(final CassandraConfig cassandraFig ) throws Exception {
        this.cassandraConfig = cassandraFig;
        this.cluster = getCluster();

        logger.info("Initialized datastax cluster client. Hosts={}, Idle Timeout={}s,  Pool Timeout={}s",
            getCluster().getMetadata().getAllHosts().toString(),
            getCluster().getConfiguration().getPoolingOptions().getIdleTimeoutSeconds(),
            getCluster().getConfiguration().getPoolingOptions().getPoolTimeoutMillis() / 1000);

        // always initialize the keyspaces
        this.createApplicationKeyspace(false);
        this.createApplicationLocalKeyspace(false);
    }

    @Override
    public synchronized Cluster getCluster(){

        // ensure we can build the cluster if it was previously closed
        if ( cluster == null || cluster.isClosed() ){
            cluster = buildCluster();
        }

        return cluster;
    }

    @Override
    public synchronized Session getClusterSession(){

        // always grab cluster from getCluster() in case it was prematurely closed
        if ( clusterSession == null || clusterSession.isClosed() ){
            int retries = 3;
            int retryCount = 0;
            while ( retryCount < retries){
                try{
                    retryCount++;
                    clusterSession = getCluster().connect();
                    break;
                }catch(NoHostAvailableException e){
                    if(retryCount == retries){
                        throw e;
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        // swallow
                    }
                }
            }
        }

        return clusterSession;
    }

    @Override
    public synchronized Session getApplicationSession(){

        // always grab cluster from getCluster() in case it was prematurely closed
        if ( applicationSession == null || applicationSession.isClosed() ){
            int retries = 3;
            int retryCount = 0;
            while ( retryCount < retries){
                try{
                    retryCount++;
                    applicationSession = getCluster().connect( CQLUtils.quote( cassandraConfig.getApplicationKeyspace() ) );
                    break;
                }catch(NoHostAvailableException e){
                    if(retryCount == retries){
                        throw e;
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        // swallow
                    }
                }
            }
        }
        return applicationSession;
    }


    @Override
    public synchronized Session getApplicationLocalSession(){

        // always grab cluster from getCluster() in case it was prematurely closed
        if ( queueMessageSession == null || queueMessageSession.isClosed() ){
            int retries = 3;
            int retryCount = 0;
            while ( retryCount < retries){
                try{
                    retryCount++;
                    queueMessageSession = getCluster().connect( CQLUtils.quote( cassandraConfig.getApplicationLocalKeyspace() ) );
                    break;
                }catch(NoHostAvailableException e){
                    if(retryCount == retries){
                        throw e;
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        // swallow
                    }
                }
            }
        }
        return queueMessageSession;
    }


    /**
     * Execute CQL that will create the keyspace if it doesn't exist and alter it if it does.
     * @throws Exception
     * @param forceCheck
     */
    @Override
    public synchronized void createApplicationKeyspace(boolean forceCheck) throws Exception {

        boolean exists;
        if(!forceCheck) {
            // this gets info from client's metadata
            exists = getClusterSession().getCluster().getMetadata()
                .getKeyspace(CQLUtils.quote(cassandraConfig.getApplicationKeyspace())) != null;
        }else{
            exists = getClusterSession()
                .execute("select * from system.schema_keyspaces where keyspace_name = '"+cassandraConfig.getApplicationKeyspace()+"'")
                .one() != null;
        }

        if(exists){
            logger.info("Not creating keyspace {}, it already exists.", cassandraConfig.getApplicationKeyspace());
            return;
        }

        final String createApplicationKeyspace = String.format(
            "CREATE KEYSPACE IF NOT EXISTS %s WITH replication = %s",
            CQLUtils.quote( cassandraConfig.getApplicationKeyspace()),
            CQLUtils.getFormattedReplication( cassandraConfig.getStrategy(), cassandraConfig.getStrategyOptions())

        );

        getClusterSession().execute(createApplicationKeyspace);

        waitForSchemaAgreement();

        logger.info("Created keyspace: {}", cassandraConfig.getApplicationKeyspace());

    }



    /**
     * Execute CQL that will create the keyspace if it doesn't exist and alter it if it does.
     * @throws Exception
     * @param forceCheck
     */
    @Override
    public synchronized void createApplicationLocalKeyspace(boolean forceCheck) throws Exception {

        boolean exists;
        if(!forceCheck) {
            // this gets info from client's metadata
            exists = getClusterSession().getCluster().getMetadata()
                .getKeyspace(CQLUtils.quote(cassandraConfig.getApplicationLocalKeyspace())) != null;
        }else{
            exists = getClusterSession()
                .execute("select * from system.schema_keyspaces where keyspace_name = '"+cassandraConfig.getApplicationLocalKeyspace()+"'")
                .one() != null;
        }

        if (exists) {
            logger.info("Not creating keyspace {}, it already exists.", cassandraConfig.getApplicationLocalKeyspace());
            return;
        }

        final String createQueueMessageKeyspace = String.format(
            "CREATE KEYSPACE IF NOT EXISTS %s WITH replication = %s",
            CQLUtils.quote( cassandraConfig.getApplicationLocalKeyspace()),
            CQLUtils.getFormattedReplication(
                cassandraConfig.getStrategyLocal(), cassandraConfig.getStrategyOptionsLocal())

        );

        getClusterSession().execute(createQueueMessageKeyspace);

        waitForSchemaAgreement();

        logger.info("Created keyspace: {}", cassandraConfig.getApplicationLocalKeyspace());

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

    public synchronized Cluster buildCluster(){

        ConsistencyLevel defaultConsistencyLevel;
        try {
            defaultConsistencyLevel = cassandraConfig.getDataStaxReadCl();
        } catch (IllegalArgumentException e){

            logger.error("Unable to parse provided consistency level in property: {}, defaulting to: {}",
                CassandraFig.READ_CL,
                ConsistencyLevel.LOCAL_QUORUM);

            defaultConsistencyLevel = ConsistencyLevel.LOCAL_QUORUM;
        }


        LoadBalancingPolicy loadBalancingPolicy;
        if( !cassandraConfig.getLocalDataCenter().isEmpty() ){

            loadBalancingPolicy = new DCAwareRoundRobinPolicy.Builder()
                .withLocalDc( cassandraConfig.getLocalDataCenter() ).build();
        }else{
            loadBalancingPolicy = new DCAwareRoundRobinPolicy.Builder().build();
        }

        final PoolingOptions poolingOptions = new PoolingOptions()
            .setCoreConnectionsPerHost(HostDistance.LOCAL, cassandraConfig.getConnections())
            .setMaxConnectionsPerHost(HostDistance.LOCAL, cassandraConfig.getConnections())
            .setIdleTimeoutSeconds( cassandraConfig.getPoolTimeout() / 1000 )
            .setPoolTimeoutMillis( cassandraConfig.getPoolTimeout());

        // purposely add a couple seconds to the driver's lower level socket timeouts vs. cassandra timeouts
        final SocketOptions socketOptions = new SocketOptions()
            .setConnectTimeoutMillis( cassandraConfig.getTimeout())
            .setReadTimeoutMillis( cassandraConfig.getTimeout())
            .setKeepAlive(true);

        final QueryOptions queryOptions = new QueryOptions()
            .setConsistencyLevel(defaultConsistencyLevel)
            .setMetadataEnabled(true); // choose whether to have the driver store metadata such as schema info

        Cluster.Builder datastaxCluster = Cluster.builder()
            .withClusterName(cassandraConfig.getClusterName())
            .addContactPoints(cassandraConfig.getHosts().split(","))
            .withMaxSchemaAgreementWaitSeconds(45)
            .withCompression(ProtocolOptions.Compression.LZ4)
            .withLoadBalancingPolicy(loadBalancingPolicy)
            .withPoolingOptions(poolingOptions)
            .withQueryOptions(queryOptions)
            .withSocketOptions(socketOptions)
            .withReconnectionPolicy(Policies.defaultReconnectionPolicy())
            .withProtocolVersion(getProtocolVersion(cassandraConfig.getVersion()));

        // only add auth credentials if they were provided
        if ( !cassandraConfig.getUsername().isEmpty() && !cassandraConfig.getPassword().isEmpty() ){
            datastaxCluster.withCredentials(
                cassandraConfig.getUsername(),
                cassandraConfig.getPassword()
            );
        }


        return datastaxCluster.build();

    }

    @Override
    public void shutdown(){

        logger.info("Received shutdown request, shutting down cluster and keyspace sessions NOW!");

        getApplicationSession().close();
        getApplicationLocalSession().close();
        getCluster().close();

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
