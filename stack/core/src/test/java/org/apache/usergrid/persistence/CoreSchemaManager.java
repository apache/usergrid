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


import org.junit.Ignore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.cassandra.SchemaManager;
import org.apache.usergrid.cassandra.SpringResource;
import org.apache.usergrid.persistence.cassandra.CassandraService;
import org.apache.usergrid.persistence.cassandra.Setup;
import org.apache.usergrid.persistence.index.impl.EsProvider;

import com.google.inject.Injector;

import me.prettyprint.hector.api.Cluster;


/** @author zznate */
@Ignore( "Not a test" )
public class CoreSchemaManager implements SchemaManager {
    private static final Logger LOG = LoggerFactory.getLogger( CoreSchemaManager.class );

    private final Setup setup;
    private final Cluster cluster;


    public CoreSchemaManager( final Setup setup, final Cluster cluster ) {
        this.setup = setup;
        this.cluster = cluster;
    }


    @Override
    public void create() {
        try {
            setup.initSubsystems();
        }
        catch ( Exception ex ) {
            LOG.error( "Could not setup usergrid core schema", ex );
            throw new RuntimeException( "Could not setup usergrid core schema", ex );
        }
    }


    @Override
    public boolean exists() {
        return setup.keyspacesExist();
    }


    @Override
    public void populateBaseData() {
        try {

            setup.setupStaticKeyspace();
            setup.setupSystemKeyspace();
            setup.createDefaultApplications();
        }

        catch ( Exception ex ) {
            LOG.error( "Could not create default applications", ex );
            throw new RuntimeException("Could not create default applications", ex );
        }
    }


    @Override
    public void destroy() {
        LOG.info( "dropping keyspaces" );
        try {
            cluster.dropKeyspace( CassandraService.getApplicationKeyspace() );
        }
        catch ( RuntimeException ire ) {
            //swallow if it just doesn't exist
        }


        try {
            cluster.dropKeyspace( CassandraService.getApplicationKeyspace() );
        }
        catch ( RuntimeException ire ) {
            //swallow if it just doesn't exist
        }

        LOG.info( "keyspaces dropped" );


        final EsProvider provider =
            SpringResource.getInstance().getBean( Injector.class ).getInstance( EsProvider.class );

        provider.getClient().admin().indices().prepareDelete( "_all" ).execute().actionGet();
    }
}
