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
package org.apache.usergrid;


import java.util.UUID;

import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.usergrid.cassandra.CassandraResource;
import org.apache.usergrid.mq.QueueManagerFactory;
import org.apache.usergrid.persistence.EntityManagerFactory;
import org.apache.usergrid.persistence.IndexBucketLocator;
import org.apache.usergrid.persistence.cassandra.CassandraService;
import org.apache.usergrid.utils.JsonUtils;


public class CoreITSetupImpl implements CoreITSetup {
    private static final Logger LOG = LoggerFactory.getLogger( CoreITSetupImpl.class );

    protected EntityManagerFactory emf;
    protected QueueManagerFactory qmf;
    protected IndexBucketLocator indexBucketLocator;
    protected CassandraService cassandraService;
    protected CassandraResource cassandraResource;
    protected ElasticSearchResource elasticSearchResource;
    protected boolean enabled = false;


    public CoreITSetupImpl( CassandraResource cassandraResource, ElasticSearchResource elasticSearchResource ) {
        this.cassandraResource = cassandraResource;
        this.elasticSearchResource = elasticSearchResource;
    }


    @Override
    public Statement apply( Statement base, Description description ) {
        return statement( base, description );
    }


    private Statement statement( final Statement base, final Description description ) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                before( description );

                try {
                    base.evaluate();
                }
                finally {
                    after( description );
                }
            }
        };
    }


    /**
     * Sets up the resources for the test here.
     *
     * @throws Throwable if setup fails (which will disable {@code after}
     */
    protected void before( Description description ) throws Throwable {
        LOG.info( "Setting up for {}", description.getDisplayName() );
        elasticSearchResource.before();
        initialize();
    }


    private void initialize() {
        if ( !enabled ) {
            cassandraService = cassandraResource.getBean( CassandraService.class );
            emf = cassandraResource.getBean( EntityManagerFactory.class );
            qmf = cassandraResource.getBean( QueueManagerFactory.class );
            indexBucketLocator = cassandraResource.getBean( IndexBucketLocator.class );
            enabled = true;
        }
    }


    /** Override to tear down your specific external resource. */
    protected void after( Description description ) {
        LOG.info( "Tearing down for {}", description.getDisplayName() );
        elasticSearchResource.after();
    }


    @Override
    public EntityManagerFactory getEmf() {
        if ( emf == null ) {
            initialize();
        }

        return emf;
    }


    @Override
    public QueueManagerFactory getQmf() {
        if ( qmf == null ) {
            initialize();
        }

        return qmf;
    }


    @Override
    public IndexBucketLocator getIbl() {
        if ( indexBucketLocator == null ) {
            initialize();
        }

        return indexBucketLocator;
    }


    @Override
    public CassandraService getCassSvc() {
        if ( cassandraService == null ) {
            initialize();
        }

        return cassandraService;
    }


    @Override
    public UUID createApplication( String organizationName, String applicationName ) throws Exception {
        if ( emf == null ) {
            emf = cassandraResource.getBean( EntityManagerFactory.class );
        }

        if ( USE_DEFAULT_APPLICATION ) {
            return emf.getDefaultAppId();
        }

        return emf.createApplication( organizationName, applicationName );
    }


    @Override
    public void dump( String name, Object obj ) {
        if ( obj != null && LOG.isInfoEnabled() ) {
            LOG.info( name + ":\n" + JsonUtils.mapToFormattedJsonString( obj ) );
        }
    }
}
