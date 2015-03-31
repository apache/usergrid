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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;

import org.apache.usergrid.AbstractCoreIT;
import org.apache.usergrid.persistence.index.query.Query;
import org.apache.usergrid.persistence.index.query.Query.Level;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import static org.junit.Assert.assertEquals;


public class PathQueryIT extends AbstractCoreIT {

    @Test
    public void testUserDevicePathQuery() throws Exception {
        UUID applicationId = setup.createApplication(
                "testOrganization"+ UUIDGenerator.newTimeUUID(), "testUserDevicePathQuery" + UUIDGenerator.newTimeUUID()  );
        EntityManager em = setup.getEmf().getEntityManager( applicationId );

        List<Entity> users = new ArrayList<Entity>();
        for ( int i = 0; i < 15; i++ ) {
            Map<String, Object> properties = new LinkedHashMap<String, Object>();
            properties.put( "index", i );
            properties.put( "username", "user " + i );
            Entity created = em.create( "user", properties );
            users.add( created );
        }

        List<EntityRef> deviceRefs = new ArrayList<EntityRef>();
        for ( Entity user : users ) {
            for ( int i = 0; i < 5; i++ ) {
                Map<String, Object> properties = new LinkedHashMap<String, Object>();
                properties.put( "index", i );
                Entity created = em.create( "device", properties );
                deviceRefs.add( created );
                em.addToCollection( user, "devices", created );
            }
        }

        app.refreshIndex();

        // pick an arbitrary user, ensure it has 5 devices
        Results devices = em.getCollection( users.get( 10 ), "devices", null, 20, Level.IDS, false );
        assertEquals( 5, devices.size() );

        int pageSize = 10; // shouldn't affect these tests

        Query userQuery = new Query();
        userQuery.setCollection( "users" );
        userQuery.setLimit( pageSize );
        userQuery.addFilter( "index >= 2" );
        userQuery.addFilter( "index <= 13" );
        int expectedUserQuerySize = 12;

        // query the users, ignoring page boundaries
        Results results = em.searchCollection( em.getApplicationRef(), "users", userQuery );
        PagingResultsIterator pri = new PagingResultsIterator( results );
        int count = 2;
        while ( pri.hasNext() ) {
            Entity e = ( Entity ) pri.next();
            assertEquals( count++, e.getProperty( "index" ) );
        }
        assertEquals( count, expectedUserQuerySize + 2 );

        // query devices as a sub-query of the users, ignoring page boundaries
        Query deviceQuery = new Query();
        deviceQuery.setCollection( "devices" );
        deviceQuery.setLimit( pageSize );
        deviceQuery.addFilter( "index >= 2" );
        int expectedDeviceQuerySize = 3;

        PathQuery<EntityRef> usersPQ = new PathQuery<EntityRef>(
                new SimpleEntityRef( em.getApplicationRef()), userQuery );
        PathQuery<Entity> devicesPQ = usersPQ.chain( deviceQuery );
        HashSet set = new HashSet( expectedUserQuerySize * expectedDeviceQuerySize );
        Iterator<Entity> i = devicesPQ.iterator( em );
        while ( i.hasNext() ) {
            set.add( i.next() );
        }
        assertEquals( expectedUserQuerySize * expectedDeviceQuerySize, set.size() );
    }


    @Test
    public void testGroupUserDevicePathQuery() throws Exception {

        UUID applicationId = setup.createApplication(
                "testOrganization"+ UUIDGenerator.newTimeUUID(), "testGroupUserDevicePathQuery" + UUIDGenerator.newTimeUUID()  );
        EntityManager em = setup.getEmf().getEntityManager( applicationId );

        List<Entity> groups = new ArrayList<Entity>();
        for ( int i = 0; i < 4; i++ ) {
            Map<String, Object> properties = new LinkedHashMap<String, Object>();
            properties.put( "index", i );
            properties.put( "path", "group_" + i );
            Entity created = em.create( "group", properties );
            groups.add( created );
        }

        List<Entity> users = new ArrayList<Entity>();
        for ( Entity group : groups ) {
            for ( int i = 0; i < 7; i++ ) {
                Map<String, Object> properties = new LinkedHashMap<String, Object>();
                properties.put( "index", i );
                properties.put( "username", group.getProperty( "path" ) + " user " + i );
                Entity created = em.create( "user", properties );
                em.addToCollection( group, "users", created );
                users.add( created );
            }
        }

        app.refreshIndex();

        // pick an arbitrary group, ensure it has 7 users
        Results ru = em.getCollection( groups.get( 2 ), "users", null, 20, Level.IDS, false );
        assertEquals( 7, ru.size() );

        List<EntityRef> devices = new ArrayList<EntityRef>();
        for ( Entity user : users ) {
            for ( int i = 0; i < 7; i++ ) {
                Map<String, Object> properties = new LinkedHashMap<String, Object>();
                properties.put( "index", i );
                Entity created = em.create( "device", properties );
                devices.add( created );
                em.addToCollection( user, "devices", created );
            }
        }

        app.refreshIndex();

        // pick an arbitrary user, ensure it has 7 devices
        Results rd = em.getCollection( users.get( 6 ), "devices", null, 20, Level.IDS, false );
        assertEquals( 7, rd.size() );

        int pageSize = 3; // ensure we're crossing page boundaries

        Query groupQuery = new Query();
        groupQuery.setCollection( "groups" );
        groupQuery.setLimit( pageSize );
        groupQuery.addFilter( "index <= 7" );
        int expectedGroupQuerySize = 4;

        Query userQuery = new Query();
        userQuery.setCollection( "users" );
        userQuery.setLimit( pageSize );
        userQuery.addFilter( "index >= 2" );
        userQuery.addFilter( "index <= 6" );
        int expectedUserQuerySize = 5;

        Query deviceQuery = new Query();
        deviceQuery.setCollection( "devices" );
        deviceQuery.setLimit( pageSize );
        deviceQuery.addFilter( "index >= 4" );
        int expectedDeviceQuerySize = 3;


        final PathQuery groupsPQ = new PathQuery(new SimpleEntityRef( em.getApplicationRef() ), groupQuery );


        //test 1 level deep
        HashSet groupSet = new HashSet( expectedGroupQuerySize );
        Iterator<Entity> groupsIterator = groupsPQ.iterator( em );

        while ( groupsIterator.hasNext() ) {
            groupSet.add( groupsIterator.next() );
        }

        assertEquals( expectedGroupQuerySize, groupSet.size() );

        //test 2 levels

        final PathQuery groupsPQ1 = new PathQuery(new SimpleEntityRef( em.getApplicationRef() ), groupQuery );
        PathQuery usersPQ1 = groupsPQ1.chain( userQuery );
        final Iterator<Entity> userIterator  = usersPQ1.iterator( em );

        final HashSet userSet = new HashSet( expectedGroupQuerySize * expectedUserQuerySize );

        while ( userIterator.hasNext() ) {
            userSet.add( userIterator.next() );
        }

        assertEquals( expectedGroupQuerySize * expectedUserQuerySize, userSet.size() );


// ORIGINAL TEST, restore
        PathQuery groupsPQ2 = new PathQuery(new SimpleEntityRef( em.getApplicationRef() ), groupQuery );
        PathQuery usersPQ2 = groupsPQ2.chain( userQuery );
        PathQuery<Entity> devicesPQ2 = usersPQ2.chain( deviceQuery );

        final HashSet deviceSet = new HashSet( expectedGroupQuerySize * expectedUserQuerySize * expectedDeviceQuerySize );
        Iterator<Entity> i = devicesPQ2.iterator( em );
        while ( i.hasNext() ) {
            deviceSet.add( i.next() );
        }
        assertEquals( expectedGroupQuerySize * expectedUserQuerySize * expectedDeviceQuerySize, deviceSet.size() );
    }
}
