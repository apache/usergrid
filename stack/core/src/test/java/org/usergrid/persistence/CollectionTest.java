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
import static org.junit.Assert.assertTrue;
import static org.usergrid.utils.MapUtils.hashMap;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.utils.JsonUtils;
import org.usergrid.utils.UUIDUtils;

public class CollectionTest extends AbstractPersistenceTest {

    private static final Logger logger = LoggerFactory
            .getLogger(CollectionTest.class);

    @SuppressWarnings("serial")
    @Test
    public void testCollection() throws Exception {
        UUID applicationId = createApplication("testOrganization","testCollection");
        assertNotNull(applicationId);

        EntityManager em = emf.getEntityManager(applicationId);
        assertNotNull(em);

        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put("username", "edanuff");
        properties.put("email", "ed@anuff.com");

        Entity user = em.create("user", properties);
        assertNotNull(user);

        properties = new LinkedHashMap<String, Object>();
        properties.put("actor", new LinkedHashMap<String, Object>() {
            {
                put("displayName", "Ed Anuff");
                put("objectType", "person");
            }
        });
        properties.put("verb", "tweet");
        properties.put("content", "I ate a sammich");

        Entity activity = em.create("activity", properties);
        assertNotNull(activity);

        logger.info("" + activity.getClass());
        logger.info(JsonUtils.mapToFormattedJsonString(activity));

        activity = em.get(activity.getUuid());

        logger.info("" + activity.getClass());
        logger.info(JsonUtils.mapToFormattedJsonString(activity));

        em.addToCollection(user, "activities", activity);

    }

    @Test
    public void userFirstNameSearch() throws Exception {
        UUID applicationId = createApplication("testOrganization","testFirstName");
        assertNotNull(applicationId);

        EntityManager em = emf.getEntityManager(applicationId);
        assertNotNull(em);

        String firstName = "firstName" + UUIDUtils.newTimeUUID();

        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put("username", "edanuff");
        properties.put("email", "ed@anuff.com");
        properties.put("firstname", firstName);

        Entity user = em.create("user", properties);
        assertNotNull(user);

        // EntityRef
        Query query = new Query();
        query.addEqualityFilter("firstname", firstName);

        Results r = em.searchCollection(em.getApplicationRef(), "users", query);

        assertTrue(r.size() > 0);

        Entity returned = r.getEntities().get(0);

        assertEquals(user.getUuid(), returned.getUuid());
        
        //update the username
        String newFirstName = "firstName" + UUIDUtils.newTimeUUID();
        
        user.setProperty("firstname", newFirstName);
        
        em.update(user);
        
        //search with the old username, should be no results
        query = new Query();
        query.addEqualityFilter("firstname", firstName);

        r = em.searchCollection(em.getApplicationRef(), "users", query);

        assertEquals(0, r.size());

        
        //search with the new username, should be results.
        
        query = new Query();
        query.addEqualityFilter("firstname", newFirstName);

        r = em.searchCollection(em.getApplicationRef(), "users", query);

        assertTrue(r.size() > 0);

        returned = r.getEntities().get(0);

        assertEquals(user.getUuid(), returned.getUuid());

    }
    
    @Test
    public void userMiddleNameSearch() throws Exception {
        UUID applicationId = createApplication("testOrganization","testMiddleName");
        assertNotNull(applicationId);

        EntityManager em = emf.getEntityManager(applicationId);
        assertNotNull(em);

        String middleName = "middleName" + UUIDUtils.newTimeUUID();

        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put("username", "edanuff");
        properties.put("email", "ed@anuff.com");
        properties.put("middlename", middleName);

        Entity user = em.create("user", properties);
        assertNotNull(user);

        // EntityRef
        Query query = new Query();
        query.addEqualityFilter("middlename", middleName);

        Results r = em.searchCollection(em.getApplicationRef(), "users", query);

        assertTrue(r.size() > 0);

        Entity returned = r.getEntities().get(0);

        assertEquals(user.getUuid(), returned.getUuid());

    }
    
    @Test
    public void userLastNameSearch() throws Exception {
        UUID applicationId = createApplication("testOrganization","testLastName");
        assertNotNull(applicationId);

        EntityManager em = emf.getEntityManager(applicationId);
        assertNotNull(em);

        String lastName = "lastName" + UUIDUtils.newTimeUUID();

        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put("username", "edanuff");
        properties.put("email", "ed@anuff.com");
        properties.put("lastname", lastName);

        Entity user = em.create("user", properties);
        assertNotNull(user);

        // EntityRef
        Query query = new Query();
        query.addEqualityFilter("lastname", lastName);

        Results r = em.searchCollection(em.getApplicationRef(), "users", query);

        assertTrue(r.size() > 0);

        Entity returned = r.getEntities().get(0);

        assertEquals(user.getUuid(), returned.getUuid());

    }


    @Test
    public void testGroups() throws Exception {
        UUID applicationId = createApplication("testOrganization","testGroups");
        assertNotNull(applicationId);

        EntityManager em = emf.getEntityManager(applicationId);
        assertNotNull(em);

        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put("username", "edanuff");
        properties.put("email", "ed@anuff.com");

        Entity user1 = em.create("user", properties);
        assertNotNull(user1);

        properties = new LinkedHashMap<String, Object>();
        properties.put("username", "djacobs");
        properties.put("email", "djacobs@gmail.com");

        Entity user2 = em.create("user", properties);
        assertNotNull(user2);

        properties = new LinkedHashMap<String, Object>();
        properties.put("path", "group1");
        Entity group = em.create("group", properties);
        assertNotNull(group);

        em.addToCollection(group, "users", user1);
        em.addToCollection(group, "users", user2);

        properties = new LinkedHashMap<String, Object>();
        properties.put("nickname", "ed");
        em.updateProperties(new SimpleCollectionRef(group, "users", user1),
                properties);

        Results r = em.searchCollection(group, "users",
                new Query().addEqualityFilter("member.nickname", "ed")
                        .withResultsLevel(Results.Level.LINKED_PROPERTIES));
        logger.info(JsonUtils.mapToFormattedJsonString(r.getEntities()));
        assertEquals(1, r.size());

        em.removeFromCollection(user1, "groups", group);

    }
    
    @Test
    public void groupNameSearch() throws Exception {
        UUID applicationId = createApplication("testOrganization","groupNameSearch");
        assertNotNull(applicationId);

        EntityManager em = emf.getEntityManager(applicationId);
        assertNotNull(em);

        String groupName = "groupName" + UUIDUtils.newTimeUUID();

        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put("title", "testTitle");
        properties.put("path", "testPath");
        properties.put("name", groupName);

        Entity group = em.create("group", properties);
        assertNotNull(group);

        // EntityRef
        Query query = new Query();
        query.addEqualityFilter("name", groupName);

        Results r = em.searchCollection(em.getApplicationRef(), "groups", query);

        assertTrue(r.size() > 0);

        Entity returned = r.getEntities().get(0);

        assertEquals(group.getUuid(), returned.getUuid());

    }
    
    @Test
    public void groupTitleSearch() throws Exception {
        UUID applicationId = createApplication("testOrganization","groupTitleSearch");
        assertNotNull(applicationId);

        EntityManager em = emf.getEntityManager(applicationId);
        assertNotNull(em);

        String titleName = "groupName" + UUIDUtils.newTimeUUID();

        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put("title",  titleName);
        properties.put("path", "testPath");
        properties.put("name", "testName");

        Entity group = em.create("group", properties);
        assertNotNull(group);

        // EntityRef
        Query query = new Query();
        query.addEqualityFilter("title", titleName);

        Results r = em.searchCollection(em.getApplicationRef(), "groups", query);

        assertTrue(r.size() > 0);

        Entity returned = r.getEntities().get(0);

        assertEquals(group.getUuid(), returned.getUuid());

    }

    @Test
    public void testSubkeys() throws Exception {

        UUID applicationId = createApplication("testOrganization","testSubkeys");
        assertNotNull(applicationId);

        EntityManager em = emf.getEntityManager(applicationId);
        assertNotNull(em);

        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put("username", "edanuff");
        properties.put("email", "ed@anuff.com");

        Entity user = em.create("user", properties);
        assertNotNull(user);

        properties = new LinkedHashMap<String, Object>();
        properties.put("actor",
                hashMap("displayName", "Ed Anuff").map("objectType", "person"));
        properties.put("verb", "tweet");
        properties.put("content", "I ate a sammich");

        em.addToCollection(user, "activities",
                em.create("activity", properties));

        properties = new LinkedHashMap<String, Object>();
        properties.put("actor",
                hashMap("displayName", "Ed Anuff").map("objectType", "person"));
        properties.put("verb", "post");
        properties.put("content", "I wrote a blog post");

        em.addToCollection(user, "activities",
                em.create("activity", properties));

        properties = new LinkedHashMap<String, Object>();
        properties.put("actor",
                hashMap("displayName", "Ed Anuff").map("objectType", "person"));
        properties.put("verb", "tweet");
        properties.put("content", "I ate another sammich");

        em.addToCollection(user, "activities",
                em.create("activity", properties));

        properties = new LinkedHashMap<String, Object>();
        properties.put("actor",
                hashMap("displayName", "Ed Anuff").map("objectType", "person"));
        properties.put("verb", "post");
        properties.put("content", "I wrote another blog post");

        em.addToCollection(user, "activities",
                em.create("activity", properties));

        Results r = em.searchCollection(user, "activities",
                Query.searchForProperty("verb", "post"));
        logger.info(JsonUtils.mapToFormattedJsonString(r.getEntities()));
        assertEquals(2, r.size());

    }
}
