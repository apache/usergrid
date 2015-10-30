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


import com.google.inject.Injector;
import org.apache.usergrid.cassandra.SpringResource;
import org.apache.usergrid.corepersistence.service.ApplicationService;
import org.apache.usergrid.mq.QueueManagerFactory;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityManagerFactory;
import org.apache.usergrid.persistence.IndexBucketLocator;
import org.apache.usergrid.persistence.cassandra.CassandraService;
import org.apache.usergrid.setup.ConcurrentProcessSingleton;
import org.apache.usergrid.utils.JsonUtils;
import org.apache.usergrid.utils.UUIDUtils;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;


public class CoreITSetupImpl implements CoreITSetup, TestEntityIndex {
    private static final Logger LOG = LoggerFactory.getLogger( CoreITSetupImpl.class );
    private final Injector injector;

    protected EntityManagerFactory emf;
    protected QueueManagerFactory qmf;
    protected CassandraService cassandraService;

    protected SpringResource springResource;


    public CoreITSetupImpl( ) {
        springResource = ConcurrentProcessSingleton.getInstance().getSpringResource();

        cassandraService = springResource.getBean( CassandraService.class );
        emf = springResource.getBean( EntityManagerFactory.class );
        qmf = springResource.getBean( QueueManagerFactory.class );
        injector = springResource.getBean(Injector.class);


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




    }



    /** Override to tear down your specific external resource. */
    protected void after( Description description ) {
        LOG.info( "Tearing down for {}", description.getDisplayName() );
    }


    @Override
    public EntityManagerFactory getEmf() {
        return emf;
    }


    @Override
    public QueueManagerFactory getQmf() {
         return qmf;
    }

    @Override
    public ApplicationService getApplicationService(){
        return injector.getInstance(ApplicationService.class);
    }


    @Override
    public CassandraService getCassSvc() {
        return cassandraService;
    }


    @Override
    public UUID createApplication( String organizationName, String applicationName ) throws Exception {
        Entity appInfo = emf.createApplicationV2(organizationName, applicationName);
        UUID applicationId = appInfo.getUuid();

        return applicationId;
    }

    @Override
    public void dump( String name, Object obj ) {
        if ( obj != null && LOG.isInfoEnabled() ) {
            LOG.info( name + ":\n" + JsonUtils.mapToFormattedJsonString( obj ) );
        }
    }

    @Override
    public Injector getInjector() {
        return injector;
    }

    @Override
    public TestEntityIndex getEntityIndex(){
        return this;
    }

    @Override
    public void refresh(UUID appId){
        try {
            Thread.sleep(50);
        } catch (InterruptedException ie){

        }
        emf.refreshIndex(appId);

        try {
            Thread.sleep(50);
        } catch (InterruptedException ie){

        }
    }
}
