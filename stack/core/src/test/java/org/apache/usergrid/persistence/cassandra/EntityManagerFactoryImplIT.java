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

import org.apache.usergrid.Application;
import org.apache.usergrid.CoreApplication;
import org.apache.usergrid.corepersistence.index.ReIndexRequestBuilder;
import org.apache.usergrid.corepersistence.index.ReIndexRequestBuilderImpl;
import org.apache.usergrid.corepersistence.index.ReIndexService;
import org.apache.usergrid.corepersistence.index.ReIndexServiceImpl;
import org.apache.usergrid.persistence.*;
import org.apache.usergrid.utils.UUIDUtils;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.lang3.RandomStringUtils;

import org.apache.usergrid.AbstractCoreIT;
import org.apache.usergrid.persistence.cassandra.util.TraceTag;
import org.apache.usergrid.persistence.cassandra.util.TraceTagManager;
import org.apache.usergrid.persistence.cassandra.util.TraceTagReporter;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;
import org.apache.usergrid.setup.ConcurrentProcessSingleton;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.functions.Func2;

import javax.annotation.concurrent.NotThreadSafe;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


@NotThreadSafe
public class EntityManagerFactoryImplIT extends AbstractCoreIT {

    private static final Logger logger = LoggerFactory.getLogger( EntityManagerFactoryImplIT.class );



    public EntityManagerFactoryImplIT() {
        emf = ConcurrentProcessSingleton.getInstance().getSpringResource().getBean( EntityManagerFactory.class );
    }

    @Rule
    public Application app = new CoreApplication( setup );



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
        Entity appInfo = emf.createApplicationV2(organizationName, applicationName);
        UUID appId = appInfo.getUuid();
        return appId;
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
        ReIndexService reIndexService = setup.getInjector().getInstance( ReIndexService.class );

        int maxRetries = 10;

        String rand = UUIDGenerator.newTimeUUID().toString();

        // create an application with a collection and an entity

        String appName = "test-app-" + rand;
        String orgName = "test-org-" + rand;
        final UUID deletedAppId = setup.createApplication( orgName, appName );

        EntityManager em = setup.getEmf().getEntityManager(deletedAppId);

        Map<String, Object> properties1 = new LinkedHashMap<String, Object>();
        properties1.put( "Name", "12 Angry Men" );
        properties1.put( "Year", 1957 );
        Entity film1 = em.create("film", properties1);

        Map<String, Object> properties2 = new LinkedHashMap<String, Object>();
        properties2.put( "Name", "Reservoir Dogs" );
        properties2.put( "Year", 1992 );
        Entity film2 = em.create( "film", properties2 );

        for ( int j=0; j<maxRetries; j++ ) {
            if ( setup.getEmf().lookupApplication( orgName + "/" + appName ).isPresent()) {
                break;
            }
            Thread.sleep( 500 );
        }

        this.app.refreshIndex();


        // wait for it to appear in delete apps list

        Func2<UUID, Map<String, UUID> ,Boolean> findApps = (applicationId, apps) -> {
            boolean found = false;
            for (String app : apps.keySet()) {
                UUID appId = apps.get(app);
                if (appId.equals(applicationId)) {
                    found = true;
                    break;
                }
            }
            return found;
        };

        Map<String,UUID> apps = setup.getEmf().getApplications();
        boolean found = findApps.call(deletedAppId, apps);
        assertTrue("Restored app not found in apps collection", found);

        // delete the application
        setup.getEmf().deleteApplication(deletedAppId);

        this.app.refreshIndex();

        found = findApps.call( deletedAppId, emf.getDeletedApplications() );

        assertTrue("Deleted app must be found in in deleted apps collection", found);

        // attempt to get entities in application's collections in various ways should all fail
        found =  setup.getEmf().lookupApplication( orgName + "/" + appName ).isPresent() ;

        assertFalse("Lookup of deleted app must fail", found);

        // app must not be found in apps collection
        found = findApps.call( deletedAppId, emf.getApplications());
        assertFalse("Deleted app must not be found in apps collection", found);

        // restore the app
        emf.restoreApplication(deletedAppId);
        final ReIndexRequestBuilder builder = reIndexService.getBuilder().withApplicationId( deletedAppId );

        ReIndexService.ReIndexStatus status = reIndexService.rebuildIndex(builder);
        int count = 0;
        do{
            status = reIndexService.getStatus(status.getJobId());
            count++;
            if(count>0){
                if(count>10){
                    break;
                }
                Thread.sleep(1000);
            }
        }while (status.getStatus()!= ReIndexService.Status.COMPLETE);

        this.app.refreshIndex();

        // test to see that app now works and is happy

        // it should not be found in the deleted apps collection
        found = findApps.call( deletedAppId, emf.getDeletedApplications());
        assertFalse("Restored app found in deleted apps collection", found);
        this.app.refreshIndex();

        apps = setup.getEmf().getApplications();
        found = findApps.call(deletedAppId, apps);

        assertTrue("Restored app not found in apps collection", found);

        // TODO: this assertion should work!
        assertTrue(setup.getEmf().lookupApplication( orgName + "/" + appName ).isPresent());
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
            assertEquals( "number of properties wrong", 6, properties.size() );
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


    @Test
    public void testCreateAndImmediateGet() throws Exception {

        String random = RandomStringUtils.randomAlphabetic(10);
        String orgName = "org_" + random;
        String appName = "app_" + random;
        String orgAppName = orgName + "/" + appName;

        UUID appId = setup.createApplication(orgName, appName);

        UUID lookedUpId = setup.getEmf().lookupApplication( orgAppName ).get();

        Assert.assertEquals(appId, lookedUpId);
    }

}
