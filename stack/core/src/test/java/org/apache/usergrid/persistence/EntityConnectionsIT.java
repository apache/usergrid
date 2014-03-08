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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.usergrid.AbstractCoreIT;
import org.apache.usergrid.cassandra.Concurrent;
import org.apache.usergrid.persistence.Results.Level;
import org.apache.usergrid.persistence.entities.User;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


@Concurrent()
public class EntityConnectionsIT extends AbstractCoreIT {
    private static final Logger LOG = LoggerFactory.getLogger( EntityConnectionsIT.class );


    public EntityConnectionsIT() {
        super();
    }


    @Test
    public void testEntityConnectionsSimple() throws Exception {
        UUID applicationId = setup.createApplication( "EntityConnectionsIT", "testEntityConnectionsSimple" );
        assertNotNull( applicationId );

        EntityManager em = setup.getEmf().getEntityManager( applicationId );
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

        Results r = em.getConnectedEntities( firstUserEntity.getUuid(), "likes", null, Level.IDS );

        List<ConnectionRef> connections = r.getConnections();

        assertNotNull( connections );
        assertEquals( 1, connections.size() );
        assertEquals( secondUserEntity.getUuid(), connections.get( 0 ).getConnectedEntity().getUuid() );
        assertEquals( firstUserEntity.getUuid(), connections.get( 0 ).getConnectingEntity().getUuid() );
    }


    @Test
    public void testEntityConnections() throws Exception {
        LOG.info( "\n\nEntityConnectionsIT.testEntityConnections\n" );

        UUID applicationId = setup.createApplication( "EntityConnectionsIT", "testEntityConnections" );
        assertNotNull( applicationId );

        EntityManager em = setup.getEmf().getEntityManager( applicationId );
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

        // List forward connections for cat A

        // Thread.sleep(5000);

        LOG.info( "Find all connections for cat A: " + catA.getUuid() );

        testEntityConnections( applicationId, catA.getUuid(), 1 );

        // List forward connections for award A

        LOG.info( "Find all connections for award A: " + awardA.getUuid() );

        testEntityConnections( applicationId, awardA.getUuid(), 1 );

        // Establish connection from award A to cat A

        LOG.info( "\n\nConnecting " + awardA.getUuid() + " \"awarded\" " + catA.getUuid() + "\n" );
        em.createConnection( awardA, "awarded", catA );

        // List forward connections for cat A

        testEntityConnections( applicationId, catA.getUuid(), 1 );

        // List forward connections for award A

        testEntityConnections( applicationId, awardA.getUuid(), 2 );

        // List all cats in application's cats collection

        testApplicationCollections( applicationId, "cats", 2 );

        // List all groups in application's cats collection

        testApplicationCollections( applicationId, "awards", 1 );

        LOG.info( "\n\nSearching Award A for recipients with the name Dylan\n" );
    }


    public Map<String, Map<String, List<UUID>>> testEntityConnections( UUID applicationId, UUID entityId,
                                                                       int expectedCount ) throws Exception {
        LOG.info( "----------------------------------------------------" );
        LOG.info( "Checking connections for " + entityId.toString() );

        EntityManager em = setup.getEmf().getEntityManager( applicationId );
        Entity en = em.get( entityId );

        Results results = em.getConnectedEntities( en.getUuid(), null, null, Results.Level.REFS );


        LOG.info( "----------------------------------------------------" );
        assertEquals( "Expected " + expectedCount + " connections", expectedCount, results.getConnections().size() );
        // return connections;
        return null;
    }


    public List<UUID> testApplicationCollections( UUID applicationId, String collectionName, int expectedCount )
            throws Exception {
        return testEntityCollections( applicationId, applicationId, collectionName, expectedCount );
    }


    public List<UUID> testEntityCollections( UUID applicationId, UUID entityId, String collectionName,
                                             int expectedCount ) throws Exception {
        LOG.info( "----------------------------------------------------" );
        LOG.info( "Checking collection " + collectionName + " for " + entityId.toString() );

        EntityManager em = setup.getEmf().getEntityManager( applicationId );
        Entity en = em.get( entityId );

        int i = 0;
        Results entities = em.getCollection( en, collectionName, null, 100, Results.Level.IDS, false );
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
        UUID applicationId = setup.createApplication( "entityConnectionsTest", "testEntityConnectionsMembership" );
        assertNotNull( applicationId );

        EntityManager em = setup.getEmf().getEntityManager( applicationId );
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


        Results r = em.getConnectedEntities( firstUserEntity.getUuid(), "likes", "restaurant", Level.IDS );

        List<ConnectionRef> connections = r.getConnections();

        assertNotNull( connections );
        assertEquals( 1, connections.size() );
        assertEquals( fourpeaks.getUuid(), connections.get( 0 ).getConnectedEntity().getUuid() );
        assertEquals( firstUserEntity.getUuid(), connections.get( 0 ).getConnectingEntity().getUuid() );

        // now check membership
        assertTrue( em.isConnectionMember( firstUserEntity, "likes", fourpeaks ) );
        assertFalse( em.isConnectionMember( firstUserEntity, "likes", arrogantbutcher ) );

        // check we don't get the restaurant from the second user
        r = em.getConnectedEntities( secondUserEntity.getUuid(), "likes", "restaurant", Level.IDS );

        connections = r.getConnections();

        assertNotNull( connections );
        assertEquals( 1, connections.size() );
        assertEquals( arrogantbutcher.getUuid(), connections.get( 0 ).getConnectedEntity().getUuid() );
        assertEquals( secondUserEntity.getUuid(), connections.get( 0 ).getConnectingEntity().getUuid() );

        // now check membership
        assertTrue( em.isConnectionMember( secondUserEntity, "likes", arrogantbutcher ) );
        assertFalse( em.isConnectionMember( secondUserEntity, "likes", fourpeaks ) );
    }
}
