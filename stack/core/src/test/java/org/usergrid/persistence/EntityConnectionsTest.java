/*******************************************************************************
 * Copyright 2012 Apigee Corporation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.usergrid.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import me.prettyprint.cassandra.utils.TimeUUIDUtils;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.cassandra.CassandraRunner;
import org.usergrid.persistence.Results.Level;
import org.usergrid.persistence.cassandra.CassandraService;
import org.usergrid.persistence.entities.User;
import org.usergrid.utils.UUIDUtils;

public class EntityConnectionsTest extends AbstractPersistenceTest {

    private static final Logger logger = LoggerFactory
            .getLogger(EntityConnectionsTest.class);

    public EntityConnectionsTest() {
        super();
    }

    @Test
    public void testEntityConnectionsSimple() throws Exception {
        UUID applicationId = createApplication(
                "testOrganization" + UUIDUtils.newTimeUUID(),
                "testEntityConnections");
        assertNotNull(applicationId);

        EntityManager em = emf.getEntityManager(applicationId);
        assertNotNull(em);

        User first = new User();
        first.setUsername("first");
        first.setEmail("first@usergrid.com");

        Entity firstUserEntity = em.create(first);

        assertNotNull(firstUserEntity);

        User second = new User();
        second.setUsername("second");
        second.setEmail("second@usergrid.com");

        Entity secondUserEntity = em.create(second);

        assertNotNull(secondUserEntity);

        em.createConnection(firstUserEntity, "likes", secondUserEntity);

        Results r = em.getConnectedEntities(first.getUuid(), "likes", null,
                Level.IDS);

        List<ConnectionRef> connections = r.getConnections();
        
        assertNotNull(connections);
        assertEquals(1, connections.size());
        assertEquals(secondUserEntity.getUuid(), connections.get(0).getConnectedEntity().getUuid());
        assertEquals(firstUserEntity.getUuid(), connections.get(0).getConnectingEntity().getUuid());

    }

    @Test
    public void testEntityConnections() throws Exception {
        logger.info("\n\nEntityConnectionsTest.testEntityConnections\n");

        UUID applicationId = createApplication("testOrganization",
                "testEntityConnections");
        assertNotNull(applicationId);

        EntityManager em = emf.getEntityManager(applicationId);
        assertNotNull(em);

        logger.info("\n\nCreating Cat entity A with name of Dylan\n");
        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put("name", "Dylan");
        Entity catA = em.create("cat", properties);
        assertNotNull(catA);
        logger.info("\n\nEntity A created with id " + catA.getUuid() + "\n");

        // Do entity get by id for id of cat entity A

        logger.info("\n\nLooking up cat with id " + catA.getUuid() + "\n");

        Entity cat = em.get(catA);
        assertNotNull(cat);

        logger.info("\n\nFound entity " + cat.getUuid() + " of type "
                + cat.getType() + " with name " + cat.getProperty("name")
                + "\n");

        // Create cat entity B

        logger.info("\n\nCreating cat entity B with name of Nico\n");
        properties = new LinkedHashMap<String, Object>();
        properties.put("name", "Nico");
        Entity catB = em.create("cat", properties);
        assertNotNull(catB);
        logger.info("\n\nEntity B created with id " + catB.getUuid() + "\n");

        // Create award entity A

        logger.info("\n\nCreating award entity with name of 'best cat'\n");
        properties = new LinkedHashMap<String, Object>();
        properties.put("name", "Best Cat Ever");
        Entity awardA = em.create("award", properties);
        assertNotNull(awardA);
        logger.info("\n\nEntity created with id " + awardA.getUuid() + "\n");

        // Establish connection from cat A to cat B

        logger.info("\n\nConnecting " + catA.getUuid() + " \"likes\" "
                + catB.getUuid() + "\n");
        em.createConnection(catA, "likes", catB);

        // Establish connection from award A to cat B

        logger.info("\n\nConnecting " + awardA.getUuid() + " \"awarded\" "
                + catB.getUuid() + "\n");
        em.createConnection(awardA, "awarded", catB);

        // List forward connections for cat A

        // Thread.sleep(5000);

//        CassandraRunner.getBean(CassandraService.class).logKeyspaces();

        logger.info("Find all connections for cat A: " + catA.getUuid());

        testEntityConnections(applicationId, catA.getUuid(), 1);

        // List forward connections for award A

        logger.info("Find all connections for award A: " + awardA.getUuid());

        testEntityConnections(applicationId, awardA.getUuid(), 1);

        // Establish connection from award A to cat A

        logger.info("\n\nConnecting " + awardA.getUuid() + " \"awarded\" "
                + catA.getUuid() + "\n");
        em.createConnection(awardA, "awarded", catA);

        // List forward connections for cat A

        testEntityConnections(applicationId, catA.getUuid(), 1);

        // List forward connections for award A

        testEntityConnections(applicationId, awardA.getUuid(), 2);

        // List all cats in application's cats collection

        testApplicationCollections(applicationId, "cats", 2);

        // List all groups in application's cats collection

        testApplicationCollections(applicationId, "awards", 1);

        logger.info("\n\nSearching Award A for recipients with the name Dylan\n");

        // T.N. This isn't used anywhere. Removing for this release
        // Results found_entities =
        // em.searchConnectedEntitiesForProperty(awardA,
        // "awarded", "cat", "name", "Dylan", null, null, 1, false,
        // Results.Level.IDS);
        // assertNotNull(found_entities);
        // assertEquals("Wrong number of results found", 1,
        // found_entities.size());
        //
        // UUID found_cat_id = found_entities.getId();
        // assertNotNull(found_cat_id);
        //
        // Entity found_cat = em.get(found_cat_id);
        // logger.info("Found cat id: " + found_cat.getUuid());
        // logger.info("Found cat name: " + found_cat.getProperty("name"));
        //
        // logger.info("\n\nSetting 'foo'='bar' for found recipient (Dylan)\n");
        //
        // em.setProperty(found_cat, "foo", "bar");
        //
        // logger.info("\n\nSearching Group A for members with 'foo'='bar'\n");
        //
        // found_entities = em.searchConnectedEntitiesForProperty(awardA,
        // "awarded", "cat", "foo", "bar", null, null, 1, false,
        // Results.Level.IDS);
        // assertNotNull(found_entities);
        // assertEquals("Wrong number of results found", 1,
        // found_entities.size());
        //
        // logger.info("\n\nSetting 'foo'='baz' for found member (Dylan)\n");
        //
        // em.setProperty(found_cat, "foo", "baz");
        //
        // logger.info("\n\nSearching award A for members with 'foo'='bar', expecting to get no results\n");
        //
        // found_entities = em.searchConnectedEntitiesForProperty(awardA,
        // "awarded", "cat", "foo", "bar", null, null, 1, false,
        // Results.Level.IDS);
        // assertNotNull(found_entities);
        // assertEquals("Wrong number of results found", 0,
        // found_entities.size());
        //
        // logger.info("\n\nSearching Group A for members with 'foo'='baz'\n");
        //
        // found_entities = em.searchConnectedEntitiesForProperty(awardA,
        // "awarded", "cat", "foo", "baz", null, null, 1, false,
        // Results.Level.IDS);
        // assertNotNull(found_entities);
        // assertEquals("Wrong number of results found", 1,
        // found_entities.size());
        //
        // em.deleteConnection(em.connectionRef(awardA, "awarded", catA));
        //
        // found_entities = em.searchConnectedEntitiesForProperty(awardA,
        // "awarded", "cat", "foo", "baz", null, null, 1, false,
        // Results.Level.IDS);
        // assertNotNull(found_entities);
        // assertEquals("Wrong number of results found", 0,
        // found_entities.size());

    }

    public Map<String, Map<String, List<UUID>>> testEntityConnections(
            UUID applicationId, UUID entityId, int expectedCount)
            throws Exception {
        logger.info("----------------------------------------------------");
        logger.info("Checking connections for " + entityId.toString());

        EntityManager em = emf.getEntityManager(applicationId);
        Entity en = em.get(entityId);

        Results results = em.getConnectedEntities(en.getUuid(), null, null,
                Results.Level.REFS);
        /*
         * Map<String, Map<String, List<UUID>>> connections = results
         * .getConnectionTypeAndEntityTypeToEntityIdMap(); for (String
         * connectionType : connections.keySet()) { Map<String, List<UUID>>
         * entityTypeToEntityIds = connections .get(connectionType); for (String
         * entityType : entityTypeToEntityIds.keySet()) { List<UUID> entityIds =
         * entityTypeToEntityIds.get(entityType); logger.info(connectionType +
         * " " + entityType + ":"); for (UUID id : entityIds) {
         * logger.info(entityType + " " + id.toString()); } } }
         */
        logger.info("----------------------------------------------------");
        assertEquals("Expected " + expectedCount + " connections",
                expectedCount, results.getConnections() != null ? results
                        .getConnections().size() : 0);
        // return connections;
        return null;
    }

    public List<UUID> testApplicationCollections(UUID applicationId,
            String collectionName, int expectedCount) throws Exception {
        return testEntityCollections(applicationId, applicationId,
                collectionName, expectedCount);
    }

    public List<UUID> testEntityCollections(UUID applicationId, UUID entityId,
            String collectionName, int expectedCount) throws Exception {
        logger.info("----------------------------------------------------");
        logger.info("Checking collection " + collectionName + " for "
                + entityId.toString());

        EntityManager em = emf.getEntityManager(applicationId);
        Entity en = em.get(entityId);

        int i = 0;
        Results entities = em.getCollection(en, collectionName, null, 100,
                Results.Level.IDS, false);
        for (UUID id : entities.getIds()) {
            logger.info((i++) + " " + id.toString());
        }
        logger.info("----------------------------------------------------");
        assertEquals("Expected " + expectedCount + " connections",
                expectedCount, entities.getIds() != null ? entities.getIds()
                        .size() : 0);
        // return connections;
        return entities.getIds();
    }

}
