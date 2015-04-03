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


import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import com.google.inject.Injector;
import org.apache.usergrid.persistence.index.EntityIndex;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.mq.QueueManager;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.EntityRef;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.persistence.SimpleEntityRef;
import org.apache.usergrid.persistence.index.query.Query;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import static junit.framework.Assert.assertNotNull;


public class CoreApplication implements Application, TestRule {

    private final static Logger LOG = LoggerFactory.getLogger( CoreApplication.class );
    protected UUID id;
    protected String appName;
    protected String orgName;
    protected CoreITSetup setup;
    protected EntityManager em;
    protected Map<String, Object> properties = new LinkedHashMap<String, Object>();
    private EntityIndex entityIndex;


    public CoreApplication( CoreITSetup setup ) {
        this.setup = setup;
    }


    @Override
    public void putAll( Map<String, Object> properties ) {
        this.properties.putAll( properties );
    }


    @Override
    public Object get( String key ) {
        return properties.get( key );
    }


    @Override
    public Map<String, Object> getProperties() {
        return properties;
    }


    @Override
    public UUID getId() {
        return id;
    }


    @Override
    public String getOrgName() {
        return orgName;
    }


    @Override
    public String getAppName() {
        return appName;
    }


    @Override
    public Entity create( String type ) throws Exception {
        Entity entity = em.create( type, properties );
        clear();
        return entity;
    }


    @Override
    public Object put( String property, Object value ) {
        return properties.put( property, value );
    }


    @Override
    public void clear() {
        properties.clear();
    }


    @Override
    public void addToCollection( Entity user, String collection, Entity item ) throws Exception {
        em.addToCollection( user, collection, item );
    }


    @Override
    public Results searchCollection( Entity user, String collection, Query query ) throws Exception {
        return em.searchCollection( user, collection, query );
    }


    @Override
    public Statement apply( final Statement base, final Description description ) {
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


    protected void after( Description description ) {
        LOG.info("Test {}: finish with application", description.getDisplayName());

//        try {
//            setup.getEmf().getEntityManager(id).().get();
//        }catch (Exception ee){
//            throw new RuntimeException(ee);
//        }
    }


    /**
     * Create an application with the given app name and org name
     * @param orgName
     * @param appName
     */
    public void createApplication(final String orgName, final String appName) throws Exception {
        this.orgName = orgName;
        this.appName = appName;
        id = setup.createApplication( orgName, appName );
        assertNotNull( id );

        em = setup.getEmf().getEntityManager( id );
        Injector injector = setup.getInjector();
        entityIndex = injector.getInstance(EntityIndex.class);
        assertNotNull(em);

        LOG.info( "Created new application {} in organization {}", appName, orgName );

    }

    protected void before( Description description ) throws Exception {
        final String orgName = description.getClassName()+ UUIDGenerator.newTimeUUID();
        final String appName = description.getMethodName();

        createApplication( orgName, appName  );
    }



    public QueueManager getQm() {
        return setup.getQmf().getQueueManager( getId() );
    }


    @Override
    public void remove(Entity entity) throws Exception {
        em.delete( entity );
    }


    @Override
    public void remove(EntityRef entityRef) throws Exception {
        em.delete( entityRef );
    }


    @Override
    public Entity get( EntityRef entityRef ) throws Exception {
        return em.get( entityRef );
    }


    @Override
    public Entity get( UUID id, String type ) throws Exception {
        return em.get( new SimpleEntityRef( type, id ) );
    }


    @Override
    public synchronized void refreshIndex() {
        try{
            Thread.sleep(50);
        }catch (InterruptedException ie){}

        entityIndex.refresh();

        try{
            Thread.sleep(100);
        }catch (InterruptedException ie){}
    }


    @Override
    public EntityManager getEntityManager() {
        return em;
    }
}
