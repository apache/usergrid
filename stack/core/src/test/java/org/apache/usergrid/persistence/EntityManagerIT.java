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


import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.usergrid.AbstractCoreIT;
import org.apache.usergrid.cassandra.Concurrent;
import org.apache.usergrid.persistence.Results.Level;
import org.apache.usergrid.persistence.entities.Group;
import org.apache.usergrid.persistence.entities.User;
import org.apache.usergrid.utils.UUIDUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.apache.usergrid.persistence.cassandra.CassandraService.MANAGEMENT_APPLICATION_ID;


@Concurrent()
public class EntityManagerIT extends AbstractCoreIT {
    private static final Logger LOG = LoggerFactory.getLogger( EntityManagerIT.class );


    public EntityManagerIT() {
        super();
    }


    @Test
    public void testEntityManager() throws Exception {
        LOG.info( "EntityManagerIT.testEntityManagerTest" );

        UUID applicationId = setup.createApplication( "testOrganization", "testEntityManagerTest" );
        assertNotNull( applicationId );

        EntityManager em = setup.getEmf().getEntityManager( applicationId );
        assertNotNull( em );

        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put( "username", "edanuff" );
        properties.put( "email", "ed@anuff.com" );

        Entity user = em.create( "user", properties );
        assertNotNull( user );

        user = em.get( user );
        assertNotNull( user );
        assertEquals( "user.username not expected value", "edanuff", user.getProperty( "username" ) );
        assertEquals( "user.email not expected value", "ed@anuff.com", user.getProperty( "email" ) );

        EntityRef userRef = em.getAlias( applicationId, "user", "edanuff" );
        assertNotNull( userRef );
        assertEquals( "userRef.id not expected value", user.getUuid(), userRef.getUuid() );
        assertEquals( "userRef.type not expected value", "user", userRef.getType() );

        LOG.info( "user.username: " + user.getProperty( "username" ) );
        LOG.info( "user.email: " + user.getProperty( "email" ) );

        Results results = em.searchCollection( em.getApplicationRef(), "users",
                new Query().addEqualityFilter( "username", "edanuff" ) );
        assertNotNull( results );
        assertEquals( 1, results.size() );
        user = results.getEntity();
        assertNotNull( user );
        assertEquals( "user.username not expected value", "edanuff", user.getProperty( "username" ) );
        assertEquals( "user.email not expected value", "ed@anuff.com", user.getProperty( "email" ) );

        LOG.info( "user.username: " + user.getProperty( "username" ) );
        LOG.info( "user.email: " + user.getProperty( "email" ) );

        results = em.searchCollection( em.getApplicationRef(), "users",
                new Query().addEqualityFilter( "email", "ed@anuff.com" ) );
        assertNotNull( results );
        assertEquals( 1, results.size() );
        user = results.getEntity();
        assertNotNull( user );
        assertEquals( "user.username not expected value", "edanuff", user.getProperty( "username" ) );
        assertEquals( "user.email not expected value", "ed@anuff.com", user.getProperty( "email" ) );

        LOG.info( "user.username: " + user.getProperty( "username" ) );
        LOG.info( "user.email: " + user.getProperty( "email" ) );
    }


    @Test
    public void testCreateAndGet() throws Exception {
        LOG.info( "EntityDaoTest.testCreateAndGet" );

        UUID applicationId = setup.createApplication( "testOrganization", "testCreateAndGet" );

        EntityManager em = setup.getEmf().getEntityManager( applicationId );

        int i = 0;
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

            Entity thing = em.get( entity.getUuid() );
            assertNotNull( "thing should not be null", thing );
            assertFalse( "thing id not valid", thing.getUuid().equals( new UUID( 0, 0 ) ) );
            assertEquals( "name not expected value", "thing" + i, thing.getProperty( "name" ) );

            i++;
        }

        List<UUID> ids = new ArrayList<UUID>();
        for ( Entity entity : things ) {
            ids.add( entity.getUuid() );

            Entity en = em.get( entity.getUuid() );
            String type = en.getType();
            assertEquals( "type not expected value", "thing", type );

            Object property = en.getProperty( "name" );
            assertNotNull( "thing name property should not be null", property );
            assertTrue( "thing name should start with \"thing\"", property.toString().startsWith( "thing" ) );

            Map<String, Object> properties = en.getProperties();
            assertEquals( "number of properties wrong", 5, properties.size() );
        }

        i = 0;
        Results results = em.get( ids, Results.Level.CORE_PROPERTIES );
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
    }


    @Test
    public void testDictionaries() throws Exception {
        LOG.info( "EntityDaoTest.testDictionaries" );

        UUID applicationId = setup.createApplication( "testOrganization", "testDictionaries" );

        EntityManager em = setup.getEmf().getEntityManager( applicationId );

        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put( "name", "thing" );
        Entity thing = em.create( "thing", properties );
        assertNotNull( thing );
        em.addToDictionary( thing, "stuff", "alpha" );
        em.addToDictionary( thing, "stuff", "beta" );
        em.addToDictionary( thing, "stuff", "gamma" );

        Set<Object> set = em.getDictionaryAsSet( thing, "stuff" );
        assertNotNull( "list should not be null", set );
        assertEquals( "Wrong number of items in list", 3, set.size() );

        Iterator<Object> i = set.iterator();
        LOG.info( "first item is " + i.next() );
        LOG.info( "second item is " + i.next() );
        LOG.info( "third item is " + i.next() );

        i = set.iterator();
        assertEquals( "first item should be alpha", "alpha", i.next() );
        assertEquals( "second item should be beta", "beta", i.next() );
        assertEquals( "third item should be gamma", "gamma", i.next() );

        em.addToDictionary( thing, "test", "foo", "bar" );
        String val = ( String ) em.getDictionaryElementValue( thing, "test", "foo" );
        assertEquals( "val should be bar", "bar", val );

        /*
         * Results r = em.searchCollection(em.getApplicationRef(), "things",
         * Query.findForProperty("stuff", "beta"));
         * assertNotNull("results should not be null", r);
         * assertEquals("Wrong number of items in list", 1, r.size());
         */
    }


    @Test
    public void testProperties() throws Exception {
        LOG.info( "EntityDaoTest.testProperties" );

        UUID applicationId = setup.createApplication( "testOrganization", "testProperties" );

        EntityManager em = setup.getEmf().getEntityManager( applicationId );

        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put( "name", "testprop" );
        Entity thing = em.create( "thing", properties );

        Entity entity = em.get( thing.getUuid() );
        assertNotNull( "entity should not be null", entity );
        em.setProperty( entity, "alpha", 1 );
        em.setProperty( entity, "beta", 2 );
        em.setProperty( entity, "gamma", 3 );

        Map<String, Object> props = em.getProperties( entity );
        assertNotNull( "properties should not be null", props );
        assertEquals( "wrong number of properties", 8, props.size() );

        assertEquals( "wrong value for property alpha", new Long( 1 ), props.get( "alpha" ) );
        assertEquals( "wrong value for property beta", new Long( 2 ), props.get( "beta" ) );
        assertEquals( "wrong value for property gamma", new Long( 3 ), props.get( "gamma" ) );

        for ( Entry<String, Object> entry : props.entrySet() ) {
            LOG.info( entry.getKey() + " : " + entry.getValue() );
        }

        em.deleteProperty( entity, "alpha" );

        props = em.getProperties( entity );
        assertNotNull( "properties should not be null", props );
        assertEquals( "wrong number of properties", 7, props.size() );
    }


    @Test
    public void testCreateAndDelete() throws Exception {
        LOG.info( "EntityDaoTest.testCreateAndDelete" );

        UUID applicationId = setup.createApplication( "testOrganization", "testCreateAndDelete" );

        EntityManager em = setup.getEmf().getEntityManager( applicationId );

        String name = "test.thing" + UUIDUtils.newTimeUUID();

        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put( "name", name );
        properties.put( "foo", "bar" );

        LOG.info( "Starting entity create" );
        Entity thing = em.create( "thing", properties );
        LOG.info( "Entity created" );

        LOG.info( "Starting entity delete" );
        em.delete( thing );
        LOG.info( "Entity deleted" );

        // now search by username, no results should be returned

        Results r =
                em.searchCollection( em.getApplicationRef(), "thing", new Query().addEqualityFilter( "name", name ) );

        assertEquals( 0, r.size() );
    }


    @Test
    public void testCreateAndDeleteUser() throws Exception {
        LOG.info( "EntityDaoTest.testCreateAndDeleteUser" );

        UUID applicationId = setup.createApplication( "testOrganization", "testCreateAndDeleteUser" );

        EntityManager em = setup.getEmf().getEntityManager( applicationId );

        String name = "test.thing" + UUIDUtils.newTimeUUID();

        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put( "username", name );
        properties.put( "foo", "bar" );

        LOG.info( "Starting entity create" );
        Entity user = em.create( "user", properties );
        LOG.info( "Entity created" );

        LOG.info( "Starting entity delete" );
        em.delete( user );
        LOG.info( "Entity deleted" );

        // now search by username, no results should be returned

        Results r = em.searchCollection( em.getApplicationRef(), "users",
                new Query().addEqualityFilter( "username", name ) );

        assertEquals( 0, r.size() );

        // now re-create a new user with the same username

        properties = new LinkedHashMap<String, Object>();
        properties.put( "username", name );
        properties.put( "foo", "bar" );

        LOG.info( "Starting entity create" );
        user = em.create( "user", properties );
        LOG.info( "Entity created" );

        r = em.searchCollection( em.getApplicationRef(), "users", new Query().addEqualityFilter( "username", name ) );

        assertEquals( 1, r.size() );

        assertEquals( user.getUuid(), r.getEntity().getUuid() );
    }


    @SuppressWarnings("unchecked")
    @Test
    public void testJson() throws Exception {
        LOG.info( "EntityDaoTest.testProperties" );

        UUID applicationId = setup.createApplication( "testOrganization", "testJson" );

        EntityManager em = setup.getEmf().getEntityManager( applicationId );

        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put( "name", "testprop" );
        Entity thing = em.create( "thing", properties );

        Entity entity = em.get( thing.getUuid() );
        assertNotNull( "entity should not be null", entity );

        Map<String, Object> json = new LinkedHashMap<String, Object>();
        json.put( "a", "alpha" );
        json.put( "b", "beta" );
        json.put( "c", "gamma" );

        em.setProperty( entity, "json", json );

        Map<String, Object> props = em.getProperties( entity );
        assertNotNull( "properties should not be null", props );
        assertEquals( "wrong number of properties", 6, props.size() );

        json = ( Map<String, Object> ) props.get( "json" );
        assertEquals( "wrong size for property alpha", 3, json.size() );
        assertEquals( "wrong value for property beta", "alpha", json.get( "a" ) );

        em.deleteProperty( entity, "json" );
    }


    @Test
    @Ignore("There is a concurrency issue due to counters not being thread safe: see USERGRID-1753")
    public void testEntityCounters() throws Exception {
        LOG.info( "EntityManagerIT#testEntityCounters" );
        EntityManager em = setup.getEmf().getEntityManager( MANAGEMENT_APPLICATION_ID );

        Group organizationEntity = new Group();
        organizationEntity.setPath( "testCounterOrg" );
        organizationEntity.setProperty( "name", "testCounterOrg" );
        organizationEntity = em.create( organizationEntity );

        UUID applicationId = setup.getEmf().createApplication( "testCounterOrg", "testEntityCounters" );

        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put( "name", "testEntityCounters" );
        Entity applicationEntity = em.create( applicationId, "application_info", properties );

        em.createConnection( new SimpleEntityRef( "group", organizationEntity.getUuid() ), "owns",
                new SimpleEntityRef( "application_info", applicationId ) );

        em = setup.getEmf().getEntityManager( applicationId );
        properties = new LinkedHashMap<String, Object>();
        properties.put( "username", "edanuff" );
        properties.put( "email", "ed@anuff.com" );
        Entity user = em.create( "user", properties );

        em = setup.getEmf().getEntityManager( MANAGEMENT_APPLICATION_ID );
        Map<String, Long> counts = em.getEntityCounters( MANAGEMENT_APPLICATION_ID );
        LOG.info( "Entity counters: {}", counts );
        assertNotNull( counts );
        assertEquals( 4, counts.size() );

        Entity entity = em.get( new SimpleEntityRef( Group.ENTITY_TYPE, organizationEntity.getUuid() ) );
        assertEquals( "testCounterOrg", entity.getName() );

        em = setup.getEmf().getEntityManager( applicationId );
        counts = em.getEntityCounters( applicationId );
        LOG.info( "Entity counters: {}", counts );
        assertNotNull( counts );
        assertEquals( 3, counts.size() );
    }


    @Test
    public void testCreateAndList() throws Exception {
        LOG.info( "EntityDaoTest.testCreateAndDelete" );

        UUID applicationId = setup.createApplication( "testOrganization", "testCreateAndList" );

        EntityManager em = setup.getEmf().getEntityManager( applicationId );

        String name = "test.thing" + UUIDUtils.newTimeUUID() + 1;

        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put( "name", name );
        properties.put( "foo", "bar" );

        LOG.info( "Starting entity create" );
        Entity thing1 = em.create( "thing", properties );
        LOG.info( "Entity created" );

        String name2 = "test.thing" + UUIDUtils.newTimeUUID() + 2;

        properties = new LinkedHashMap<String, Object>();
        properties.put( "name", name2 );
        properties.put( "foo", "bar" );

        LOG.info( "Starting entity create" );
        Entity thing2 = em.create( "thing", properties );
        LOG.info( "Entity created" );

        // now search by username, no results should be returned

        EntityRef appRef = em.getRef( applicationId );

        Results r = em.getCollection( appRef, "things", null, 10, Level.ALL_PROPERTIES, false );

        assertEquals( 2, r.size() );

        assertEquals( thing1.getUuid(), r.getEntities().get( 0 ).getUuid() );
        assertEquals( thing2.getUuid(), r.getEntities().get( 1 ).getUuid() );
    }


    @Test
    public void testCorrectType() throws Exception {

        UUID applicationId = setup.createApplication( "testOrganization", "testCorrectType" );

        EntityManager em = setup.getEmf().getEntityManager( applicationId );

        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put( "name", "testuser" );
        properties.put( "username", "testuser" );
        properties.put( "email", "test@foo.bar" );
        Entity created = em.create( "user", properties );

        Entity returned = em.get( created.getUuid() );

        assertNotNull( created );
        assertNotNull( returned );


        assertTrue( created instanceof User );
        assertTrue( returned instanceof User );

        assertEquals( created, returned );
    }


    @Test
    public void testImmutableForcedPropChange() throws Exception {
        LOG.info( "EntityDaoTest.testProperties" );

        UUID applicationId = setup.createApplication( "testOrganization", "testNamePropChanges" );

        EntityManager em = setup.getEmf().getEntityManager( applicationId );

        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put( "name", "one" );
        Entity saved = em.create( "thing", properties );

        Entity thingOne = em.get( saved.getUuid() );
        assertNotNull( "entity should not be null", thingOne );
        assertEquals( "one", thingOne.getProperty( "name" ).toString() );

        em.setProperty( thingOne, "name", "two", true );

        Entity thingTwo = em.get( saved.getUuid() );

        assertEquals( "two", thingTwo.getProperty( "name" ) );
    }


    @Test
    public void ownershipScopeCorrect() throws Exception {

        UUID applicationId = setup.createApplication( "testOrganization", "ownershipScopeCorrect" );

        EntityManager em = setup.getEmf().getEntityManager( applicationId );

        //first user
        Map<String, Object> userProps = new LinkedHashMap<String, Object>();
        userProps.put( "name", "testuser" );
        userProps.put( "username", "testuser" );
        userProps.put( "email", "test@foo.bar" );
        Entity createdUser = em.create( "user", userProps );

        Entity returnedUser = em.get( createdUser.getUuid() );

        assertNotNull( createdUser );
        assertNotNull( returnedUser );

        //second user
        Map<String, Object> userProps2 = new LinkedHashMap<String, Object>();
        userProps2.put( "name", "testuser2" );
        userProps2.put( "username", "testuser2" );
        userProps2.put( "email", "test2@foo.bar" );
        Entity createdUser2 = em.create( "user", userProps2 );

        Entity returnedUser2 = em.get( createdUser2.getUuid() );

        assertNotNull( createdUser2 );
        assertNotNull( returnedUser2 );

        //now create the device, in the scope of the user

        Map<String, Object> device = new LinkedHashMap<String, Object>();
        device.put( "name", "device1" );

        Entity createdDevice = em.createItemInCollection( createdUser, "devices", "device", device );

        Entity returnedDevice = em.get( createdDevice.getUuid() );

        assertNotNull( createdDevice );
        assertNotNull( returnedDevice );

        assertEquals( "device1", returnedDevice.getName() );

        //now load it within the context of the user, it should load.

        //first user is an owner
        assertTrue( em.isCollectionMember( createdUser, "devices", createdDevice ) );

        //Not an owner
        assertFalse( em.isCollectionMember( createdUser2, "devices", createdDevice ) );
    }
}
