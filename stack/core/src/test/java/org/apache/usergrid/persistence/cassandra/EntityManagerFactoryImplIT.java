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
package org.apache.usergrid.persistence.cassandra;


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.usergrid.persistence.*;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.lang3.RandomStringUtils;

import org.apache.usergrid.AbstractCoreIT;
import org.apache.usergrid.persistence.cassandra.util.TraceTag;
import org.apache.usergrid.persistence.cassandra.util.TraceTagManager;
import org.apache.usergrid.persistence.cassandra.util.TraceTagReporter;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;
import org.apache.usergrid.setup.ConcurrentProcessSingleton;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


public class EntityManagerFactoryImplIT extends AbstractCoreIT {

    private static final Logger logger = LoggerFactory.getLogger( EntityManagerFactoryImplIT.class );



    public EntityManagerFactoryImplIT() {
        emf = ConcurrentProcessSingleton.getInstance().getSpringResource().getBean( EntityManagerFactory.class );
    }


    @BeforeClass
    public static void setup() throws Exception {
        logger.info( "setup" );
    }


    @AfterClass
    public static void teardown() throws Exception {
        logger.info( "teardown" );
    }


    EntityManagerFactory emf;
    TraceTagManager traceTagManager;
    TraceTagReporter traceTagReporter;


    public UUID createApplication( String organizationName, String applicationName ) throws Exception {
        return emf.createApplication( organizationName, applicationName );
    }


    @Before
    public void initTracing() {
        traceTagManager = ConcurrentProcessSingleton.getInstance().getSpringResource().getBean( "traceTagManager",
            TraceTagManager.class );
        traceTagReporter = ConcurrentProcessSingleton.getInstance().getSpringResource().getBean( "traceTagReporter",
            TraceTagReporter.class );
    }


    @Test
    public void testDeleteApplication() throws Exception {

        String rand = UUIDGenerator.newTimeUUID().toString();

        // create an application with a collection and an entity

        UUID applicationId = setup.createApplication( "test-org-" + rand, "test-app-" + rand );

        EntityManager em = setup.getEmf().getEntityManager( applicationId );

        Map<String, Object> properties1 = new LinkedHashMap<String, Object>();
        properties1.put( "Name", "12 Angry Men" );
        properties1.put( "Year", 1957 );
        Entity film1 = em.create("film", properties1);

        Map<String, Object> properties2 = new LinkedHashMap<String, Object>();
        properties2.put( "Name", "Reservoir Dogs" );
        properties2.put( "Year", 1992 );
        Entity film2 = em.create( "film", properties2 );

        em.refreshIndex();

        // TODO: this assertion should work!
        //assertNotNull( "cannot lookup app by name", setup.getEmf().lookupApplication("test-app-" + rand) );

        // delete the application

        setup.getEmf().deleteApplication( applicationId );

        em.refreshIndex();

        boolean found = false;
        Map<String, UUID> deletedApps = emf.getDeletedApplications();
        for ( String appName : deletedApps.keySet() ) {
            UUID appId = deletedApps.get( appName );
            if ( appId.equals( applicationId )) {
                found = true;
                break;
            }
        }

        assertTrue("Deleted app not found in deleted apps collection", found );

        // attempt to get entities in application's collections in various ways should all fail

        assertNull( setup.getEmf().lookupApplication("test-app-" + rand) );

        Map<String, UUID> appMap = setup.getEmf().getApplications();
        for ( String appName : appMap.keySet() ) {
            UUID appId = appMap.get( appName );
            assertNotEquals( appId, applicationId );
            assertNotEquals( appName, "test-app-" + rand );
        }

        // restore the app

        emf.restoreApplication( applicationId );

        emf.rebuildAllIndexes(new EntityManagerFactory.ProgressObserver() {
            @Override
            public void onProgress(EntityRef entity) {
                logger.debug("Reindexing {}:{}", entity.getType(), entity.getUuid() );
            }
        });

        // test to see that app now works and is happy

        // it should not be found in the deleted apps collection
        found = false;
        deletedApps = emf.getDeletedApplications();
        for ( String appName : deletedApps.keySet() ) {
            UUID appId = deletedApps.get( appName );
            if ( appId.equals( applicationId )) {
                found = true;
                break;
            }
        }
        assertFalse("Restored app found in deleted apps collection", found);

        found = false;
        appMap = setup.getEmf().getApplications();
        for ( String appName : appMap.keySet() ) {
            UUID appId = appMap.get( appName );
            if ( appId.equals( applicationId )) {
                found = true;
                break;
            }
        }
        assertTrue("Restored app not found in apps collection", found);

        // TODO: this assertion should work!
        //assertNotNull(setup.getEmf().lookupApplication("test-app-" + rand));
    }


    @Test
    public void testCreateAndGet() throws Exception {
        TraceTag traceTag = traceTagManager.create( "testCreateAndGet" );
        traceTagManager.attach( traceTag );
        logger.info( "EntityDaoTest.testCreateAndGet" );

        UUID applicationId = createApplication( "EntityManagerFactoryImplIT", "testCreateAndGet"
                + UUIDGenerator.newTimeUUID()  );
        logger.info( "Application id " + applicationId );

        EntityManager em = emf.getEntityManager( applicationId );

        int i;
        List<Entity> things = new ArrayList<Entity>();
        for ( i = 0; i < 10; i++ ) {
            Map<String, Object> properties = new LinkedHashMap<String, Object>();
            properties.put( "name", "thing" + i );

            Entity thing = em.create( "thing", properties );
            assertNotNull( "thing should not be null", thing );
            assertFalse( "thing id not valid", thing.getUuid().equals( new UUID( 0, 0 ) ) );
            assertEquals( "name not expected value", "thing" + i, thing.getProperty( "name" ) );

            things.add( thing );
        }
        assertEquals( "should be ten entities", 10, things.size() );

        i = 0;
        for ( Entity entity : things ) {

            Entity thing = em.get( new SimpleEntityRef("thing", entity.getUuid()));
            assertNotNull( "thing should not be null", thing );
            assertFalse( "thing id not valid", thing.getUuid().equals( new UUID( 0, 0 ) ) );
            assertEquals( "name not expected value", "thing" + i, thing.getProperty( "name" ) );

            i++;
        }

        List<UUID> ids = new ArrayList<UUID>();
        for ( Entity entity : things ) {
            ids.add( entity.getUuid() );

            Entity en = em.get( new SimpleEntityRef("thing", entity.getUuid()));
            String type = en.getType();
            assertEquals( "type not expected value", "thing", type );

            Object property = en.getProperty( "name" );
            assertNotNull( "thing name property should not be null", property );
            assertTrue( "thing name should start with \"thing\"", property.toString().startsWith( "thing" ) );

            Map<String, Object> properties = en.getProperties();
            assertEquals( "number of properties wrong", 5, properties.size() );
        }

        i = 0;
        Results results = em.getEntities( ids, "thing" );
        for ( Entity thing : results ) {
            assertNotNull( "thing should not be null", thing );

            assertFalse( "thing id not valid", thing.getUuid().equals( new UUID( 0, 0 ) ) );

            assertEquals( "wrong type", "thing", thing.getType() );

            assertNotNull( "thing name should not be null", thing.getProperty( "name" ) );
            String name = thing.getProperty( "name" ).toString();
            assertEquals( "unexpected name", "thing" + i, name );

            i++;
        }

        assertEquals( "entities unfound entity name count incorrect", 10, i );

		/*
         * List<UUID> entities = emf.findEntityIds(applicationId, "thing", null,
		 * null, 100); assertNotNull("entities list should not be null",
		 * entities); assertEquals("entities count incorrect", 10,
		 * entities.size());
		 */
        traceTagReporter.report( traceTagManager.detach() );
    }
}
