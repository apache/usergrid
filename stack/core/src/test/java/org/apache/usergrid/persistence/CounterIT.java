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


import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.lang3.RandomStringUtils;

import org.apache.usergrid.AbstractCoreIT;
import org.apache.usergrid.persistence.entities.Event;
import org.apache.usergrid.persistence.entities.Group;
import org.apache.usergrid.persistence.entities.User;
import org.apache.usergrid.persistence.index.query.CounterResolution;
import org.apache.usergrid.persistence.index.query.Query;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;
import org.apache.usergrid.utils.ImmediateCounterRule;
import org.apache.usergrid.utils.JsonUtils;
import org.apache.usergrid.utils.UUIDUtils;

import net.jcip.annotations.NotThreadSafe;

import static org.apache.usergrid.persistence.Schema.PROPERTY_APPLICATION_ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


@NotThreadSafe
public class CounterIT extends AbstractCoreIT {

    private static final Logger LOG = LoggerFactory.getLogger( CounterIT.class );

    @Rule
    public ImmediateCounterRule counterRule = new ImmediateCounterRule( );

    long ts = System.currentTimeMillis() - ( 24 * 60 * 60 * 1000 );


    public CounterIT() {
        super();
    }



    @Test
    public void testIncrementAndDecrement() throws Exception {

        LOG.info( "CounterIT.testIncrementAndDecrement" );


        EntityManager em = app.getEntityManager();


        assertNotNull( em );

        final UUID applicationId = em.getApplicationId();

        Map<String, Long> counters = em.getEntityCounters( applicationId );
        assertEquals( null, counters.get( "application.collection.users" ) );

        UUID uuid = UUIDUtils.newTimeUUID(); // UUID.();
        Map<String, Object> userProperties = new HashMap<String, Object>();
        userProperties.put( "name", "test-name" );
        userProperties.put( "username", "test-username" );
        userProperties.put( "email", "test-email" );
        User user = ( User ) em.create( uuid, "user", userProperties ).toTypedEntity();
        LOG.debug( "user={}", user );


        counters = em.getEntityCounters( applicationId );
        assertEquals( new Long( 1 ), counters.get( "application.collection.users" ) );

        em.delete( user );
        counters = em.getEntityCounters( applicationId );
        assertEquals( new Long( 0 ), counters.get( "application.collection.users" ) );
    }


    @Test
    public void testCounters() throws Exception {
        LOG.info( "CounterIT.testCounters" );

        EntityManager em = app.getEntityManager();

        assertNotNull( em );



        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();
        // UUID groupId = UUID.randomUUID();

        Event event = null;

        for ( int i = 0; i < 100; i++ ) {
            event = new Event();
            event.setTimestamp( ts + ( i * 60 * 1000 ) );
            event.addCounter( "visits", 1 );
            event.setUser( user1 );
            em.create( event );

            event = new Event();
            event.setTimestamp( ts + ( i * 60 * 1000 ) );
            event.addCounter( "visits", 1 );
            event.setUser( user2 );
            em.create( event );
        }

        Results r = em.getAggregateCounters( null, null, null, "visits", CounterResolution.SIX_HOUR, ts,
                System.currentTimeMillis(), false );
        LOG.info( JsonUtils.mapToJsonString( r.getCounters() ) );

        r = em.getAggregateCounters( user1, null, null, "visits", CounterResolution.SIX_HOUR, ts,
                System.currentTimeMillis(), false );
        LOG.info( JsonUtils.mapToJsonString( r.getCounters() ) );

        r = em.getAggregateCounters( user1, null, null, "visits", CounterResolution.SIX_HOUR, ts,
                System.currentTimeMillis(), true );
        LOG.info( JsonUtils.mapToJsonString( r.getCounters() ) );

        r = em.getAggregateCounters( user1, null, null, "visits", CounterResolution.ALL, ts,
                System.currentTimeMillis(),
                false );
        LOG.info( JsonUtils.mapToJsonString( r.getCounters() ) );

        for ( int i = 0; i < 10; i++ ) {
            event = new Event();
            event.setTimestamp( ts + ( i * 60 * 60 * 1000 ) );
            event.addCounter( "clicks", 1 );
            em.create( event );
        }

        r = em.getAggregateCounters( null, null, null, "clicks", CounterResolution.HALF_HOUR, ts,
                System.currentTimeMillis(), true );
        LOG.info( JsonUtils.mapToJsonString( r.getCounters() ) );

        Query query = new Query();
        query.addCounterFilter( "clicks:*:*:*" );
        query.addCounterFilter( "visits:*:*:*" );
        query.setStartTime( ts );
        query.setFinishTime( System.currentTimeMillis() );
        query.setResolution( CounterResolution.SIX_HOUR );
        query.setPad( true );
        r = em.getAggregateCounters( query );
        LOG.info( JsonUtils.mapToJsonString( r.getCounters() ) );

        LOG.info( JsonUtils.mapToJsonString( em.getCounterNames() ) );


        Map<String, Long> counts = em.getApplicationCounters();
        LOG.info( "counts map: " + JsonUtils.mapToJsonString( counts ) );

        assertEquals( new Long( 10 ), counts.get( "clicks" ) );
        assertEquals( new Long( 200 ), counts.get( "visits" ) );
        assertEquals( new Long( 210 ), counts.get( "application.collection.events" ) );
    }


    @Test
    @Ignore()
    public void testCommunityCounters() throws Exception {

        EntityManager em = setup.getEmf().getEntityManager( setup.getEmf().getManagementAppId() );

        // get counts at start of test
        Query query = new Query();
        query.addCounterFilter( "admin.logins:*:*:*" );
        query.setStartTime( ts );
        query.setFinishTime( System.currentTimeMillis() );
        query.setResolution( CounterResolution.SIX_HOUR );

        Results or = em.getAggregateCounters( query );
        final long originalCount;
        if ( or.getCounters().get( 0 ).getValues().isEmpty() ) {
            originalCount = 0;
        } else {
            originalCount = or.getCounters().get( 0 ).getValues().get( 0 ).getValue();
        }

        Map<String, Long> counts = em.getApplicationCounters();
        final long originalAdminLoginsCount;
        if ( counts.get( "admin.logins" ) == null ) {
            originalAdminLoginsCount = 0;
        } else {
            originalAdminLoginsCount = counts.get( "admin.logins" );
        }

        String randomSuffix = UUIDGenerator.newTimeUUID().toString();
        String orgName = "testCounter" + randomSuffix;
        String appName = "testEntityCounters" + randomSuffix;

        Group organizationEntity = new Group();
        organizationEntity.setPath( "tst-counter" + randomSuffix );
        organizationEntity.setProperty( "name", orgName );
        organizationEntity = em.create( organizationEntity );

        Entity appInfo = setup.getEmf().createApplicationV2( orgName, appName  );
        UUID applicationId = UUIDUtils.tryExtractUUID(
            appInfo.getProperty(PROPERTY_APPLICATION_ID).toString());

        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put( "name", orgName + "/" + appName );
        Entity applicationEntity = em.create(
            applicationId, CpNamingUtils.APPLICATION_INFO, properties );

//Creating connections like below doesn't work.
//        em.createConnection( new SimpleEntityRef( "group", organizationEntity.getUuid() ), "owns",
//                new SimpleEntityRef( "application_info", applicationId ) );

        em.createConnection( organizationEntity.toTypedEntity(), "owns", applicationEntity );

        Event event = new Event();
        event.setTimestamp( System.currentTimeMillis() );
        event.addCounter( "admin.logins", 1 );
        event.setGroup( organizationEntity.getUuid() );

        // TODO look at row syntax of event counters being sent
        em.create( event );

        // event = new Event();
        // event.setTimestamp(System.currentTimeMillis());
        // event.addCounter("admin.logins", 1);
        // em.create(event);

        counts = em.getApplicationCounters();
        LOG.info( JsonUtils.mapToJsonString( counts ) );
        assertNotNull( counts.get( "admin.logins" ) );
        assertEquals( 1, counts.get( "admin.logins" ).longValue() - originalAdminLoginsCount );

        // Q's:
        // how to "count" a login to a specific application?
        // when org is provided, why is it returning 8? Is it 4 with one 'event'?

        Results r = em.getAggregateCounters( null, null, null, "admin.logins",
                CounterResolution.ALL, ts, System.currentTimeMillis(), false );

        LOG.info( JsonUtils.mapToJsonString( r.getCounters() ) );
        assertEquals( 1,
            r.getCounters().get( 0 ).getValues().get( 0 ).getValue()  - originalAdminLoginsCount );

        r = em.getAggregateCounters( query );
        LOG.info( JsonUtils.mapToJsonString( r.getCounters() ) );
        assertEquals( 1,
            r.getCounters().get( 0 ).getValues().get( 0 ).getValue() - originalCount );
    }
}
