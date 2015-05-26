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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.AbstractCoreIT;
import org.apache.usergrid.persistence.entities.User;
import org.apache.usergrid.persistence.Query.Level;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class EntityConnectionsIT extends AbstractCoreIT {
    private static final Logger LOG = LoggerFactory.getLogger( EntityConnectionsIT.class );


    public EntityConnectionsIT() {
        super();
    }


    @Test
    public void testEntityConnectionsSimple() throws Exception {
        EntityManager em = app.getEntityManager();
        assertNotNull( em );

        User first = new User();
        first.setUsername( "first" );
        first.setEmail( "first@usergrid.com" );

        Entity firstUserEntity = em.create( first );

        assertNotNull( firstUserEntity );

        User second = new User();
        second.setUsername( "second" );
        second.setEmail( "second@usergrid.com" );

        Entity secondUserEntity = em.create( second );

        assertNotNull( secondUserEntity );

        em.createConnection( firstUserEntity, "likes", secondUserEntity );

        app.refreshIndex();

        Results r = em.getTargetEntities(firstUserEntity, "likes", null, Level.IDS);

        List<ConnectionRef> connections = r.getConnections();

        assertNotNull( connections );
        assertEquals(1, connections.size());
        assertEquals( secondUserEntity.getUuid(), connections.get( 0 ).getTargetRefs().getUuid() );
        assertEquals( firstUserEntity.getUuid(), connections.get( 0 ).getSourceRefs().getUuid() );
    }


    @Test
    public void testEntityConnections() throws Exception {
        EntityManager em = app.getEntityManager();
        final UUID applicationId = app.getId();
        assertNotNull( em );

        LOG.info( "\n\nCreating Cat entity A with name of Dylan\n" );
        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put( "name", "Dylan" );
        Entity catA = em.create( "cat", properties );
        assertNotNull( catA );
        LOG.info( "\n\nEntity A created with id " + catA.getUuid() + "\n" );

        // Do entity get by id for id of cat entity A

        LOG.info( "\n\nLooking up cat with id " + catA.getUuid() + "\n" );

        Entity cat = em.get( catA );
        assertNotNull( cat );

        LOG.info( "\n\nFound entity " + cat.getUuid() + " of type " + cat.getType() + " with name " + cat
                .getProperty( "name" ) + "\n" );

        // Create cat entity B

        LOG.info( "\n\nCreating cat entity B with name of Nico\n" );
        properties = new LinkedHashMap<String, Object>();
        properties.put( "name", "Nico" );
        Entity catB = em.create( "cat", properties );
        assertNotNull( catB );
        LOG.info( "\n\nEntity B created with id " + catB.getUuid() + "\n" );

        // Create award entity A

        LOG.info( "\n\nCreating award entity with name of 'best cat'\n" );
        properties = new LinkedHashMap<String, Object>();
        properties.put( "name", "Best Cat Ever" );
        Entity awardA = em.create( "award", properties );
        assertNotNull( awardA );
        LOG.info( "\n\nEntity created with id " + awardA.getUuid() + "\n" );

        // Establish connection from cat A to cat B

        LOG.info( "\n\nConnecting " + catA.getUuid() + " \"likes\" " + catB.getUuid() + "\n" );
        em.createConnection( catA, "likes", catB );

        // Establish connection from award A to cat B

        LOG.info( "\n\nConnecting " + awardA.getUuid() + " \"awarded\" " + catB.getUuid() + "\n" );
        em.createConnection( awardA, "awarded", catB );

        app.refreshIndex();

        // List forward connections for cat A

        // Thread.sleep(5000);

        LOG.info( "Find all connections for cat A: " + catA.getUuid() );

        testEntityConnections( applicationId, catA.getUuid(), "likes", "cat", 1 );

        // List forward connections for award A

        LOG.info( "Find all connections for award A: " + awardA.getUuid() );

        testEntityConnections( applicationId, awardA.getUuid(),"awarded", "award", 1 );

        // Establish connection from award A to cat A

        LOG.info( "\n\nConnecting " + awardA.getUuid() + " \"awarded\" " + catA.getUuid() + "\n" );
        em.createConnection( awardA, "awarded", catA );

        app.refreshIndex();

        // List forward connections for cat A
// Not valid with current usages
//        testEntityConnections( applicationId, catA.getUuid(), "cat", 1 );
//
//        // List forward connections for award A
//
//        testEntityConnections( applicationId, awardA.getUuid(), "award", 2 );

        // List all cats in application's cats collection

        testApplicationCollections( applicationId, "cats", 2 );

        // List all groups in application's cats collection

        testApplicationCollections( applicationId, "awards", 1 );

        LOG.info( "\n\nSearching Award A for recipients with the name Dylan\n" );
    }


    public Map<String, Map<String, List<UUID>>> testEntityConnections(
        UUID applicationId, UUID entityId, String connectionType,  String entityType, int expectedCount ) throws Exception {

        LOG.info( "----------------------------------------------------" );
        LOG.info( "Checking connections for " + entityId.toString() );

        EntityManager em = setup.getEmf().getEntityManager( applicationId );
        Entity en = em.get( new SimpleEntityRef( entityType, entityId));

        Results results = em.getTargetEntities(en, connectionType, null, Level.REFS);

        LOG.info( "----------------------------------------------------" );
        assertEquals( "Expected " + expectedCount + " connections",
                expectedCount, results.getConnections().size() );
        // return connections;
        return null;
    }


    public List<UUID> testApplicationCollections(
            UUID applicationId, String collectionName, int expectedCount ) throws Exception {

        return testEntityCollections(
            applicationId, applicationId, "application", collectionName, expectedCount );
    }


    public List<UUID> testEntityCollections( UUID applicationId, UUID entityId, String entityType,
            String collectionName, int expectedCount ) throws Exception {

        LOG.info( "----------------------------------------------------" );
        LOG.info( "Checking collection " + collectionName + " for " + entityId.toString() );

        EntityManager em = setup.getEmf().getEntityManager( applicationId );
        Entity en = em.get( new SimpleEntityRef( entityType, entityId ));

        int i = 0;
        Results entities = em.getCollection( en, collectionName, null, 100, Level.IDS, false );
        for ( UUID id : entities.getIds() ) {
            LOG.info( ( i++ ) + " " + id.toString() );
        }
        LOG.info( "----------------------------------------------------" );
        assertEquals( "Expected " + expectedCount + " connections", expectedCount,
                entities.getIds() != null ? entities.getIds().size() : 0 );
        // return connections;
        return entities.getIds();
    }


    @Test
    public void testEntityConnectionsMembership() throws Exception {
        EntityManager em = app.getEntityManager();
        assertNotNull( em );

        User first = new User();
        first.setUsername( "first" );
        first.setEmail( "first@usergrid.com" );

        Entity firstUserEntity = em.create( first );

        assertNotNull( firstUserEntity );

        User second = new User();
        second.setUsername( "second" );
        second.setEmail( "second@usergrid.com" );

        Entity secondUserEntity = em.create( second );

        assertNotNull( secondUserEntity );

        Map<String, Object> data = new HashMap<String, Object>();
        data.put( "name", "4peaks" );

        Entity fourpeaks = em.create( "restaurant", data );

        em.createConnection( firstUserEntity, "likes", fourpeaks );


        data = new HashMap<String, Object>();
        data.put( "name", "arrogantbutcher" );

        Entity arrogantbutcher = em.create( "restaurant", data );

        em.createConnection( secondUserEntity, "likes", arrogantbutcher );

        app.refreshIndex();

        Results r = em.getTargetEntities(firstUserEntity, "likes", "restaurant", Level.IDS);

        List<ConnectionRef> connections = r.getConnections();

        assertNotNull(connections);
        assertEquals(1, connections.size());
        assertEquals(fourpeaks.getUuid(), connections.get(0).getTargetRefs().getUuid());
        assertEquals(firstUserEntity.getUuid(), connections.get(0).getSourceRefs().getUuid());

        // now check membership
        assertTrue( em.isConnectionMember( firstUserEntity, "likes", fourpeaks ) );
        assertFalse( em.isConnectionMember( firstUserEntity, "likes", arrogantbutcher ) );

        // check we don't get the restaurant from the second user
        r = em.getTargetEntities(secondUserEntity, "likes", "restaurant", Level.IDS);

        connections = r.getConnections();

        assertNotNull( connections );
        assertEquals(1, connections.size());
        assertEquals( arrogantbutcher.getUuid(), connections.get( 0 ).getTargetRefs().getUuid() );
        assertEquals( secondUserEntity.getUuid(), connections.get( 0 ).getSourceRefs().getUuid() );

        // now check membership
        assertTrue( em.isConnectionMember( secondUserEntity, "likes", arrogantbutcher ) );
        assertFalse( em.isConnectionMember( secondUserEntity, "likes", fourpeaks ) );
    }


    @Test
    public void testGetConnectingEntities() throws Exception {

        UUID applicationId = app.getId( );
        assertNotNull( applicationId );

        EntityManager em = app.getEntityManager();
        assertNotNull( em );

        User fred = new User();
        fred.setUsername( "fred" );
        fred.setEmail( "fred@flintstones.com" );
        Entity fredEntity = em.create( fred );
        assertNotNull( fredEntity );

        User wilma = new User();
        wilma.setUsername( "wilma" );
        wilma.setEmail( "wilma@flintstones.com" );
        Entity wilmaEntity = em.create( wilma );
        assertNotNull( wilmaEntity );

        em.createConnection( fredEntity, "likes", wilmaEntity );

        app.refreshIndex();

//        // search for "likes" edges from fred
//        assertEquals( 1,
//            em.getTargetEntities( fredEntity, "likes", null, Level.IDS ).size());
//
//        // search for any type of edges from fred
//        assertEquals( 1,
//            em.getTargetEntities( fredEntity, null, null, Level.IDS ).size());

        // search for "likes" edges to wilman from any type of object
        Results res = em.getSourceEntities(wilmaEntity, "likes", null, Level.ALL_PROPERTIES);
        assertEquals( 1, res.size() );
        assertEquals( "user", res.getEntity().getType() ); // fred is a user

        // search for "likes" edges to wilman from user type object
        res = em.getSourceEntities(wilmaEntity, "likes", "user", Level.ALL_PROPERTIES);
        assertEquals( 1, res.size() );
        assertEquals( "user", res.getEntity().getType() );
    }




    @Test
    @Ignore("This is broken, and needs fixed after the refactor")
    public void testConnectionsIterable() throws Exception {
        EntityManager em = app.getEntityManager();
        assertNotNull( em );

        User first = new User();
        first.setUsername( "first" );
        first.setEmail( "first@usergrid.com" );

        Entity firstUserEntity = em.create( first );

        assertNotNull( firstUserEntity );


        final int connectionCount = 100;
        final List<Entity> things = new ArrayList<>( connectionCount );

        for(int i = 0; i < connectionCount; i ++){
            Map<String, Object> data = new HashMap<String, Object>();
            data.put( "ordinal",  i );

            Entity entity = em.create( "thing", data );

            em.createConnection( firstUserEntity, "likes", entity );

            things.add( entity );
        }


        app.refreshIndex();

        Results r = em.getTargetEntities(firstUserEntity, "likes", null, Level.ALL_PROPERTIES) ;

        PagingResultsIterator itr = new PagingResultsIterator( r );


        int checkedIndex = 0;
        for(; checkedIndex < connectionCount && itr.hasNext(); checkedIndex ++){
            final Entity returned = ( Entity ) itr.next();
            final Entity expected = things.get( checkedIndex );

            assertEquals("Entity expected", expected, returned);
        }

        assertEquals("Checked all entities", connectionCount, checkedIndex  );


    }
//
//
//    @Test
//    public void testGetConnectingEntities() throws Exception {
//
//        UUID applicationId = app.getId( );
//        assertNotNull( applicationId );
//
//        EntityManager em = app.getEntityManager();
//        assertNotNull( em );
//
//        User fred = new User();
//        fred.setUsername( "fred" );
//        fred.setEmail( "fred@flintstones.com" );
//        Entity fredEntity = em.create( fred );
//        assertNotNull( fredEntity );
//
//        User wilma = new User();
//        wilma.setUsername( "wilma" );
//        wilma.setEmail( "wilma@flintstones.com" );
//        Entity wilmaEntity = em.create( wilma );
//        assertNotNull( wilmaEntity );
//
//        em.createConnection( fredEntity, "likes", wilmaEntity );
//
//        app.refreshIndex();
//
////        // search for "likes" edges from fred
////        assertEquals( 1,
////            em.getTargetEntities( fredEntity, "likes", null, Level.IDS ).size());
////
////        // search for any type of edges from fred
////        assertEquals( 1,
////            em.getTargetEntities( fredEntity, null, null, Level.IDS ).size());
//
//        // search for "likes" edges to wilman from any type of object
//        Results res = em.getSourceEntities( wilmaEntity, "likes", null, Level.ALL_PROPERTIES);
//        assertEquals( 1, res.size() );
//        assertEquals( "user", res.getEntity().getType() ); // fred is a user
//
//        // search for "likes" edges to wilman from user type object
//        res = em.getSourceEntities( wilmaEntity, "likes", "user", Level.ALL_PROPERTIES);
//        assertEquals( 1, res.size() );
//        assertEquals( "user", res.getEntity().getType() );
//    }

}
