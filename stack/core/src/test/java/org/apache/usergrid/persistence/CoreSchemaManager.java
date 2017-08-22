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
package org.apache.usergrid.persistence;


import org.apache.usergrid.locking.LockManager;
import org.apache.usergrid.persistence.core.CassandraConfig;
import org.apache.usergrid.persistence.core.CassandraFig;
import org.apache.usergrid.persistence.core.datastax.CQLUtils;
import org.apache.usergrid.persistence.core.datastax.DataStaxCluster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.cassandra.SchemaManager;
import org.apache.usergrid.cassandra.SpringResource;
import org.apache.usergrid.persistence.cassandra.Setup;
import org.apache.usergrid.persistence.index.impl.EsProvider;

import com.google.inject.Injector;


/** @author zznate */
public class CoreSchemaManager implements SchemaManager {
    private static final Logger logger = LoggerFactory.getLogger( CoreSchemaManager.class );

    private final Setup setup;
    private final CassandraConfig cassandraConfig;
    private final LockManager lockManager;
    private final DataStaxCluster dataStaxCluster;


    public CoreSchemaManager( final Setup setup, Injector injector ) {
        this.setup = setup;
        this.cassandraConfig = injector.getInstance( CassandraConfig.class );
        this.lockManager = injector.getInstance( LockManager.class );
        this.dataStaxCluster = injector.getInstance( DataStaxCluster.class );
    }


    @Override
    public void create() {
        try {
            setup.initSchema(true);
            lockManager.setup();
        }
        catch ( Exception ex ) {
            logger.error( "Could not setup usergrid core schema", ex );
            throw new RuntimeException( "Could not setup usergrid core schema", ex );
        }
    }


    @Override
    public void populateBaseData() {
        try {

            setup.runDataMigration();
            setup.initMgmtApp();
        }

        catch ( Exception ex ) {
            logger.error( "Could not create default applications", ex );
            throw new RuntimeException("Could not create default applications", ex );
        }
    }


    @Override
    public void destroy() {
        try {
            logger.info( "dropping application keyspace" );
            dataStaxCluster.getClusterSession()
                .execute("DROP KEYSPACE "+ CQLUtils.quote(cassandraConfig.getApplicationKeyspace()));
            dataStaxCluster.waitForSchemaAgreement();

            logger.info( "dropping application local keyspace" );
            dataStaxCluster.getClusterSession()
                .execute("DROP KEYSPACE "+ CQLUtils.quote(cassandraConfig.getApplicationLocalKeyspace()));
            dataStaxCluster.waitForSchemaAgreement();

            dataStaxCluster.getClusterSession().close(); // close session so it's meta will get refreshed
        }
        catch ( Exception e ) {
            logger.error("Error dropping application keyspaces: {} error: {}", cassandraConfig.getApplicationKeyspace(), e);
        }
        logger.info( "keyspaces dropped" );
        logger.info( "dropping indices" );
        final EsProvider provider =
            SpringResource.getInstance().getBean( Injector.class ).getInstance( EsProvider.class );

        provider.getClient().admin().indices().prepareDelete( "_all" ).execute().actionGet();
    }
}
