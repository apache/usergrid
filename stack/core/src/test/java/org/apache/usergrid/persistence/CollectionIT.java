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


import java.util.*;

import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.AbstractCoreIT;
import org.apache.usergrid.Application;
import org.apache.usergrid.CoreApplication;
import org.apache.usergrid.persistence.Query.Level;
import org.apache.usergrid.persistence.entities.User;
import org.apache.usergrid.persistence.exceptions.DuplicateUniquePropertyExistsException;
import org.apache.usergrid.persistence.index.query.Identifier;
import org.apache.usergrid.utils.JsonUtils;
import org.apache.usergrid.utils.UUIDUtils;
import rx.Observable;
import rx.schedulers.Schedulers;

import static org.apache.usergrid.utils.MapUtils.hashMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


//@RunWith(JukitoRunner.class)
//@UseModules({ GuiceModule.class })
public class CollectionIT extends AbstractCoreIT {

    private static final Logger LOG = LoggerFactory.getLogger( CollectionIT.class );

    @Rule
    public Application app = new CoreApplication( setup );


    @Test
    public void testSimpleCrud() throws Exception {

        LOG.debug( "testSimpleCrud" );

        app.put( "username", "edanuff" );
        app.put( "email", "ed@anuff.com" );
        Entity user = app.create( "user" );
        assertNotNull( user );

        user = app.get( user.getUuid(), "user" );
        assertNotNull( user );

        app.remove( user );
        user = app.get( user.getUuid(), "user" );
        assertNull( user );
    }


    @Test
    public void testCollection() throws Exception {
        LOG.debug( "testCollection" );

        app.put( "username", "edanuff" );
        app.put( "email", "ed@anuff.com" );

        Entity user = app.create( "user" );
        assertNotNull( user );

        app.put( "actor", new LinkedHashMap<String, Object>() {
            {
                put( "displayName", "Ed Anuff" );
                put( "objectType", "person" );
            }
        } );
        app.put( "verb", "tweet" );
        app.put( "content", "I ate a sammich" );
        app.put( "ordinal", 3 );

        Entity activity = app.create( "activity" );
        assertNotNull( activity );

        LOG.info( "" + activity.getClass() );
        LOG.info( JsonUtils.mapToFormattedJsonString( activity ) );

        activity = app.get( activity.getUuid(), activity.getType() );

        LOG.info( "Activity class = {}", activity.getClass() );
        LOG.info( JsonUtils.mapToFormattedJsonString( activity ) );

        app.addToCollection( user, "activities", activity );

        // test queries on the collection

        app.put( "actor", new LinkedHashMap<String, Object>() {
            {
                put( "displayName", "Ed Anuff" );
                put( "objectType", "person" );
            }
        } );
        app.put( "verb", "tweet2" );
        app.put( "content", "I ate a pickle" );
        app.put( "ordinal", 2 );
        Entity activity2 = app.create( "activity" );
        activity2 = app.get( activity2.getUuid(), activity2.getType() );
        app.addToCollection( user, "activities", activity2 );

        app.put( "actor", new LinkedHashMap<String, Object>() {
            {
                put( "displayName", "Ed Anuff" );
                put( "objectType", "person" );
            }
        } );
        app.put( "verb", "tweet2" );
        app.put( "content", "I ate an apple" );
        app.put( "ordinal", 1 );
        Entity activity3 = app.create( "activity" );
        activity3 = app.get( activity3.getUuid(), activity3.getType() );
        app.addToCollection( user, "activities", activity3 );

        app.refreshIndex();

        // empty query
        Query query = new Query();
        Results r = app.searchCollection( user, "activities", query );
        assertEquals( 3, r.size() ); // success

        // query verb
        query = Query.fromQL( "where verb = 'tweet2'" );
        r = app.searchCollection( user, "activities", query );
        assertEquals( 2, r.size() );

        // query verb, sort created
        query = Query.fromQL( "verb = 'tweet2' order by created" );
        r = app.searchCollection( user, "activities", query );
        assertEquals( 2, r.size() );
        List<Entity> entities = r.getEntities();
        assertEquals( entities.get( 0 ).getUuid(), activity2.getUuid() );
        assertEquals( entities.get( 1 ).getUuid(), activity3.getUuid() );

        // query verb, sort ordinal
        query = Query.fromQL( "verb = 'tweet2' order by ordinal" );
        r = app.searchCollection( user, "activities", query );
        assertEquals( 2, r.size() );
        entities = r.getEntities();
        assertEquals( entities.get( 0 ).getUuid(), activity3.getUuid() );
        assertEquals( entities.get( 1 ).getUuid(), activity2.getUuid() );

        // TODO: figure out why sort by content ascending is not working here
        // it works in the exact same test in the QueryIndex module/

        //        // empty query, sort content
        query = Query.fromQL( "order by content" );
        r = app.searchCollection( user, "activities", query );
        assertEquals( 3, r.size() );
        entities = r.getEntities();
        LOG.info( JsonUtils.mapToFormattedJsonString( entities ) );
        assertEquals( entities.get( 0 ).getUuid(), activity2.getUuid() );
        assertEquals( entities.get( 1 ).getUuid(), activity.getUuid() );
        assertEquals( entities.get( 2 ).getUuid(), activity3.getUuid() );

        // empty query, sort verb
        query = Query.fromQL( "order by verb" );
        r = app.searchCollection( user, "activities", query );
        assertEquals( 3, r.size() );

        // empty query, sort ordinal
        query = Query.fromQL( "order by ordinal" );
        r = app.searchCollection( user, "activities", query );
        assertEquals( 3, r.size() );
        entities = r.getEntities();
        assertEquals( entities.get( 0 ).getUuid(), activity3.getUuid() );
        assertEquals( entities.get( 1 ).getUuid(), activity2.getUuid() );
        assertEquals( entities.get( 2 ).getUuid(), activity.getUuid() );

        // query ordinal
        query = Query.fromQL( "where ordinal = 2" );
        r = app.searchCollection( user, "activities", query );
        assertEquals( 1, r.size() );

        // query ordinal and sort ordinal
        query = Query.fromQL( " where ordinal = 2 order by ordinal" );
        r = app.searchCollection( user, "activities", query );
        assertEquals( 1, r.size() );
    }

    @Test
    public void containsTest() throws Exception {
        LOG.debug("testCollection");

        app.put("username", "edanuff");
        app.put("email", "ed@anuff.com");

        Entity user = app.create("user");
        assertNotNull(user);

        app.put("actor", new LinkedHashMap<String, Object>() {
            {
                put("displayName", "Ed Anuff");
                put("objectType", "person");
            }
        });
        app.put("verb", "tweet");
        app.put("content", "I ate a sammich");
        app.put("ordinal", 3);

        Entity activity = app.create("activity");
        assertNotNull(activity);

        LOG.info("" + activity.getClass());
        LOG.info(JsonUtils.mapToFormattedJsonString(activity));

        activity = app.get(activity.getUuid(), activity.getType());

        LOG.info("Activity class = {}", activity.getClass());
        LOG.info(JsonUtils.mapToFormattedJsonString(activity));

        app.addToCollection(user, "activities", activity);

        // test queries on the collection

        app.put("actor", new LinkedHashMap<String, Object>() {
            {
                put("displayName", "Ed Anuff");
                put("objectType", "person");
            }
        });
        app.put("verb", "tweet2");
        app.put("content", "I ate a pickle");
        app.put("ordinal", 2);
        Entity activity2 = app.create("activity");
        activity2 = app.get(activity2.getUuid(), activity2.getType());
        app.addToCollection(user, "activities", activity2);

        app.put("actor", new LinkedHashMap<String, Object>() {
            {
                put("displayName", "Ed Anuff");
                put("objectType", "person");
            }
        });
        app.put("verb", "tweet2");
        app.put("content", "I ate an apple");
        app.put("ordinal", 1);
        Entity activity3 = app.create("activity");
        activity3 = app.get(activity3.getUuid(), activity3.getType());
        app.addToCollection(user, "activities", activity3);

        app.refreshIndex();

        // empty query
        Query query = new Query();
        Results r = app.searchCollection(user, "activities", query);
        assertEquals(3, r.size()); // success

        // query verb
        query = Query.fromQL("where verb contains 'tweet2'");
        r = app.searchCollection(user, "activities", query);
        assertEquals(2, r.size());
        // query verb
        query = Query.fromQL("where verb contains 'tw*'");
        r = app.searchCollection(user, "activities", query);
        assertEquals(3, r.size());

    }

    @Test
    public void userFirstNameSearch() throws Exception {
        LOG.debug( "userFirstNameSearch" );


        EntityManager em = app.getEntityManager();
        assertNotNull( em );

        String firstName = "firstName" + UUIDUtils.newTimeUUID();

        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put( "username", "edanuff" );
        properties.put( "email", "ed@anuff.com" );
        properties.put( "firstname", firstName );

        Entity user = em.create( "user", properties );
        assertNotNull( user );

        app.refreshIndex();

        // EntityRef
        Query query = Query.fromQL( "firstname = '" + firstName + "'" );

        Results r = em.searchCollection( em.getApplicationRef(), "users", query );

        assertTrue( r.size() > 0 );

        Entity returned = r.getEntities().get( 0 );

        assertEquals( user.getUuid(), returned.getUuid() );

        // update the username
        String newFirstName = "firstName" + UUIDUtils.newTimeUUID() + "_new";

        user.setProperty( "firstname", newFirstName );

        em.update( user );

        app.refreshIndex();

        // search with the old username, should be no results
        query = Query.fromQL( "firstname = '" + firstName + "'" );
        r = em.searchCollection( em.getApplicationRef(), "users", query );

        assertEquals( 0, r.size() );

        // search with the new username, should be results.

        query = Query.fromQL( "firstname = '" + newFirstName + "'" );

        r = em.searchCollection( em.getApplicationRef(), "users", query );

        assertTrue( r.size() > 0 );

        returned = r.getEntities().get( 0 );

        assertEquals( user.getUuid(), returned.getUuid() );
    }


    @Test
    public void userMiddleNameSearch() throws Exception {
        LOG.debug( "userMiddleNameSearch" );

        EntityManager em = app.getEntityManager();
        assertNotNull( em );

        String middleName = "middleName" + UUIDUtils.newTimeUUID();

        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put( "username", "edanuff" );
        properties.put( "email", "ed@anuff.com" );
        properties.put( "middlename", middleName );

        Entity user = em.create( "user", properties );
        assertNotNull( user );

        app.refreshIndex();

        // EntityRef
        final Query query = Query.fromQL( "middlename = '" + middleName + "'" );

        Results r = em.searchCollection( em.getApplicationRef(), "users", query );

        assertTrue( r.size() > 0 );

        Entity returned = r.getEntities().get( 0 );

        assertEquals( user.getUuid(), returned.getUuid() );
    }


    @Test
    public void userLastNameSearch() throws Exception {
        LOG.debug( "userLastNameSearch" );

        EntityManager em = app.getEntityManager();
        assertNotNull( em );

        String lastName = "lastName" + UUIDUtils.newTimeUUID();

        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put( "username", "edanuff" );
        properties.put( "email", "ed@anuff.com" );
        properties.put( "lastname", lastName );

        Entity user = em.create( "user", properties );
        assertNotNull( user );

        app.refreshIndex();

        // EntityRef
        final Query query = Query.fromQL( "lastname = '" + lastName + "'" );

        Results r = em.searchCollection( em.getApplicationRef(), "users", query );

        assertTrue( r.size() > 0 );

        Entity returned = r.getEntities().get( 0 );

        assertEquals( user.getUuid(), returned.getUuid() );
    }


    @Test
    public void testGroups() throws Exception {
        LOG.debug("testGroups");

        EntityManager em = app.getEntityManager();
        assertNotNull(em);

        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put( "username", "edanuff" );
        properties.put("email", "ed@anuff.com");

        Entity user1 = em.create( "user", properties );
        assertNotNull( user1 );

        properties = new LinkedHashMap<String, Object>();
        properties.put("username", "djacobs");
        properties.put("email", "djacobs@gmail.com");

        Entity user2 = em.create( "user", properties );
        assertNotNull( user2 );

        properties = new LinkedHashMap<String, Object>();
        properties.put("path", "group1");
        Entity group = em.create( "group", properties );
        assertNotNull(group);

        em.addToCollection( group, "users", user1 );
        em.addToCollection( group, "users", user2 );

        properties = new LinkedHashMap<String, Object>();
        properties.put("nickname", "ed");
        em.updateProperties(user1, properties);

        app.refreshIndex();
        Thread.sleep(1000);

        final Query query = Query.fromQL( "nickname = 'ed'" );

        Results r = em.searchCollectionConsistent( group, "users", query.withResultsLevel( Level.LINKED_PROPERTIES ),1 );

        LOG.info(JsonUtils.mapToFormattedJsonString(r.getEntities()));
        assertEquals(1, r.size());
        assertTrue(r.getEntities().get(0).getUuid().equals(user1.getUuid()));

        em.removeFromCollection(user1, "groups", group);
        r = em.searchCollection(user1,"groups",Query.all());
        List<Entity> entities = r.getEntities();
        assertTrue(entities.size()==0);
    }


    @Test
    public void groupNameSearch() throws Exception {
        LOG.debug( "groupNameSearch" );

        EntityManager em = app.getEntityManager();
        assertNotNull( em );

        String groupName = "groupName" + UUIDUtils.newTimeUUID();

        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put( "title", "testTitle" );
        properties.put( "path", "testPath" );
        properties.put( "name", groupName );

        Entity group = em.create( "group", properties );
        assertNotNull( group );

        app.refreshIndex();

        // EntityRef
        final Query query = Query.fromQL( "name = '" + groupName + "'" );

        Results r = em.searchCollection( em.getApplicationRef(), "groups", query );

        assertTrue( r.size() > 0 );

        Entity returned = r.getEntities().get( 0 );

        assertEquals( group.getUuid(), returned.getUuid() );
    }


    @Test
    public void groupTitleSearch() throws Exception {
        LOG.debug( "groupTitleSearch" );

        EntityManager em = app.getEntityManager();
        assertNotNull( em );

        String titleName = "groupName" + UUIDUtils.newTimeUUID();

        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put( "title", titleName );
        properties.put( "path", "testPath" );
        properties.put( "name", "testName" );

        Entity group = em.create( "group", properties );
        assertNotNull( group );

        app.refreshIndex();

        // EntityRef

        final Query query = Query.fromQL( "title = '" + titleName + "'" );

        Results r = em.searchCollection( em.getApplicationRef(), "groups", query );

        assertTrue( r.size() > 0 );

        Entity returned = r.getEntities().get( 0 );

        assertEquals( group.getUuid(), returned.getUuid() );
    }


    @Test
    public void testSubkeys() throws Exception {
        LOG.debug( "testSubkeys" );

        EntityManager em = app.getEntityManager();
        assertNotNull( em );

        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put( "username", "edanuff" );
        properties.put( "email", "ed@anuff.com" );

        Entity user = em.create( "user", properties );
        assertNotNull( user );

        properties = new LinkedHashMap<String, Object>();
        properties.put( "actor", hashMap( "displayName", "Ed Anuff" ).map( "objectType", "person" ) );
        properties.put( "verb", "tweet" );
        properties.put( "content", "I ate a sammich" );

        em.addToCollection( user, "activities", em.create( "activity", properties ) );

        properties = new LinkedHashMap<String, Object>();
        properties.put( "actor", hashMap( "displayName", "Ed Anuff" ).map( "objectType", "person" ) );
        properties.put( "verb", "post" );
        properties.put( "content", "I wrote a blog post" );

        em.addToCollection(user, "activities", em.create("activity", properties));

        properties = new LinkedHashMap<String, Object>();
        properties.put( "actor", hashMap( "displayName", "Ed Anuff" ).map( "objectType", "person" ) );
        properties.put("verb", "tweet");
        properties.put( "content", "I ate another sammich" );

        em.addToCollection(user, "activities", em.create("activity", properties));

        properties = new LinkedHashMap<String, Object>();
        properties.put("actor", hashMap("displayName", "Ed Anuff").map("objectType", "person"));
        properties.put("verb", "post");
        properties.put( "content", "I wrote another blog post" );

        em.addToCollection( user, "activities", em.create( "activity", properties ) );

        app.refreshIndex();

        final Query query = Query.fromQL( "verb = 'post'" );

        Results r = em.searchCollection(user, "activities", query);
        LOG.info( JsonUtils.mapToFormattedJsonString( r.getEntities() ) );
        assertEquals( 2, r.size() );
    }


    @Test
    public void emptyQuery() throws Exception {
        LOG.debug( "emptyQuery" );

        EntityManager em = app.getEntityManager();
        assertNotNull( em );

        String firstName = "firstName" + UUIDUtils.newTimeUUID();

        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put( "username", "edanuff" );
        properties.put( "email", "ed@anuff.com" );
        properties.put( "firstname", firstName );

        Entity user = em.create( "user", properties );
        assertNotNull( user );

        properties = new LinkedHashMap<String, Object>();
        properties.put( "username", "djacobs" );
        properties.put( "email", "djacobs@gmail.com" );

        Entity user2 = em.create( "user", properties );
        assertNotNull( user2 );

        app.refreshIndex();

        // EntityRef
        Query query = new Query();

        Results r = em.searchCollection( em.getApplicationRef(), "users", query );

        assertEquals( 2, r.size() );

        Entity returned = r.getEntities().get( 0 );

        assertEquals( user2.getUuid(), returned.getUuid() );

        returned = r.getEntities().get( 1 );

        assertEquals( user.getUuid(), returned.getUuid() );
    }


    @Test
    public void emptyQueryReverse() throws Exception {
        LOG.debug( "emptyQueryReverse" );

        EntityManager em = app.getEntityManager();
        assertNotNull( em );

        String firstName = "firstName" + UUIDUtils.newTimeUUID();

        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put( "username", "edanuff" );
        properties.put( "email", "ed@anuff.com" );
        properties.put( "firstname", firstName );

        Entity user = em.create( "user", properties );
        assertNotNull( user );

        properties = new LinkedHashMap<String, Object>();
        properties.put( "username", "djacobs" );
        properties.put( "email", "djacobs@gmail.com" );

        Entity user2 = em.create( "user", properties );
        assertNotNull( user2 );

        app.refreshIndex();

        // EntityRef
        Query query = new Query();
        query.setReversed( true );

        Results r = em.searchCollection( em.getApplicationRef(), "users", query );

        assertEquals( 2, r.size() );

        Entity returned = r.getEntities().get( 0 );

        assertEquals( user2.getUuid(), returned.getUuid() );

        returned = r.getEntities().get( 1 );

        assertEquals( user.getUuid(), returned.getUuid() );
    }


    @Test
    public void orQuery() throws Exception {
        LOG.debug( "orQuery" );

        EntityManager em = app.getEntityManager();
        assertNotNull( em );

        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put( "keywords", "blah,test,game" );
        properties.put( "title", "Solitaire" );

        Entity game1 = em.create( "orquerygame", properties );
        assertNotNull( game1 );

        properties = new LinkedHashMap<String, Object>();
        properties.put( "keywords", "random,test" );
        properties.put( "title", "Hearts" );

        Entity game2 = em.create( "orquerygame", properties );
        assertNotNull( game2 );

        app.refreshIndex();

        // EntityRef
        Query query = Query
            .fromQL("select * where keywords contains 'Random' " + "OR keywords contains 'Game' order by title desc");

        Results r = em.searchCollection( em.getApplicationRef(), "orquerygames", query );

        assertEquals( 2, r.size() );

        Entity returned = r.getEntities().get( 0 );

        assertEquals( game1.getUuid(), returned.getUuid() );

        returned = r.getEntities().get( 1 );

        assertEquals( game2.getUuid(), returned.getUuid() );

        query = Query.fromQL(
            "select * where ( keywords contains 'Random' " + "OR keywords contains 'Game') order by title desc" );

        r = em.searchCollection( em.getApplicationRef(), "orquerygames", query );

        assertEquals( 2, r.size() );

        returned = r.getEntities().get(0);

        assertEquals( game1.getUuid(), returned.getUuid() );

        returned = r.getEntities().get(1);

        assertEquals( game2.getUuid(), returned.getUuid() );

        // field order shouldn't matter USERGRID-375
        query = Query
            .fromQL( "select * where keywords contains 'blah' " + "OR title contains 'blah'  order by title desc" );

        r = em.searchCollection( em.getApplicationRef(), "orquerygames", query );

        assertEquals( 1, r.size() );

        returned = r.getEntities().get( 0 );

        assertEquals( game1.getUuid(), returned.getUuid() );

        query = Query
            .fromQL("select * where  title contains 'blah' " + "OR keywords contains 'blah' order by title desc");

        r = em.searchCollection( em.getApplicationRef(), "orquerygames", query );

        assertEquals( 1, r.size() );

        returned = r.getEntities().get(0);

        assertEquals( game1.getUuid(), returned.getUuid() );
    }


    @Test
    public void andQuery() throws Exception {
        LOG.debug( "andQuery" );

        EntityManager em = app.getEntityManager();
        assertNotNull( em );

        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put( "keywords", "blah,test,game" );
        properties.put( "title", "Solitaire" );

        Entity game1 = em.create( "game", properties );
        assertNotNull( game1 );

        properties = new LinkedHashMap<String, Object>();
        properties.put( "keywords", "random,test" );
        properties.put( "title", "Hearts" );

        Entity game2 = em.create( "game", properties );
        assertNotNull( game2 );

        app.refreshIndex();

        // overlap
        Query query = Query.fromQL(
            "select * where keywords contains 'test' " + "AND keywords contains 'random' order by title desc" );
        Results r = em.searchCollection( em.getApplicationRef(), "games", query );
        assertEquals( 1, r.size() );

        // disjoint
        query = Query.fromQL(
            "select * where keywords contains 'random' " + "AND keywords contains 'blah' order by title desc" );
        r = em.searchCollection( em.getApplicationRef(), "games", query );
        assertEquals( 0, r.size() );

        // same each side
        query = Query
            .fromQL( "select * where keywords contains 'test' " + "AND keywords contains 'test' order by title desc" );
        r = em.searchCollection( em.getApplicationRef(), "games", query );
        assertEquals( 2, r.size() );

        Entity returned = r.getEntities().get( 0 );
        assertEquals( game1.getUuid(), returned.getUuid() );

        returned = r.getEntities().get( 1 );
        assertEquals( game2.getUuid(), returned.getUuid() );

        // one side, left
        query = Query.fromQL(
            "select * where keywords contains 'test' " + "AND keywords contains 'foobar' order by title desc" );
        r = em.searchCollection( em.getApplicationRef(), "games", query );
        assertEquals( 0, r.size() );

        // one side, right
        query = Query.fromQL(
            "select * where keywords contains 'foobar' " + "AND keywords contains 'test' order by title desc" );
        r = em.searchCollection( em.getApplicationRef(), "games", query );
        assertEquals( 0, r.size() );
    }


    @Test
    public void notQuery() throws Exception {
        LOG.debug( "notQuery" );

        EntityManager em = app.getEntityManager();
        assertNotNull( em );

        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put( "keywords", "blah,test,game" );
        properties.put( "title", "Solitaire" );

        Entity game1 = em.create( "game", properties );
        assertNotNull( game1 );

        properties = new LinkedHashMap<String, Object>();
        properties.put( "keywords", "random,test" );
        properties.put( "title", "Hearts" );

        Entity game2 = em.create( "game", properties );
        assertNotNull( game2 );

        app.refreshIndex();

        // simple not
        Query query = Query.fromQL( "select * where NOT keywords contains 'game'" );
        Results r = em.searchCollection( em.getApplicationRef(), "games", query );
        assertEquals( 1, r.size() );

        // full negation in simple
        query = Query.fromQL( "select * where NOT keywords contains 'test'" );
        r = em.searchCollection( em.getApplicationRef(), "games", query );
        assertEquals( 0, r.size() );

        // simple subtraction
        query = Query.fromQL( "select * where keywords contains 'test' AND NOT keywords contains 'random'" );
        r = em.searchCollection( em.getApplicationRef(), "games", query );
        assertEquals( 1, r.size() );

        // disjoint or
        query = Query.fromQL( "select * where keywords contains 'random' OR NOT keywords contains 'blah'" );
        r = em.searchCollection( em.getApplicationRef(), "games", query );
        assertEquals( 1, r.size() );

        // disjoint and
        query = Query.fromQL( "select * where keywords contains 'random' AND NOT keywords contains 'blah'" );
        r = em.searchCollection( em.getApplicationRef(), "games", query );
        assertEquals( 1, r.size() );

        // self canceling or
        query = Query.fromQL( "select * where keywords contains 'test' AND NOT keywords contains 'test'" );
        r = em.searchCollection( em.getApplicationRef(), "games", query );
        assertEquals( 0, r.size() );

        // select all
        query = Query.fromQL( "select * where keywords contains 'test' OR NOT keywords contains 'test'" );
        r = em.searchCollection( em.getApplicationRef(), "games", query );
        assertEquals( 2, r.size() );

        // null right and
        query = Query.fromQL( "select * where keywords contains 'test' AND NOT keywords contains 'foobar'" );
        r = em.searchCollection( em.getApplicationRef(), "games", query );
        assertEquals( 2, r.size() );

        // null left and
        query = Query.fromQL( "select * where keywords contains 'foobar' AND NOT keywords contains 'test'" );
        r = em.searchCollection( em.getApplicationRef(), "games", query );
        assertEquals( 0, r.size() );

        //search where we don't have a value, should return no results and no cursor
        query = Query.fromQL( "select * where NOT title = 'FooBar'" );
        r = em.searchCollection( em.getApplicationRef(), "games", query );
        assertEquals( 2, r.size() );
        assertNull( r.getCursor() );
    }


    @Test
    public void notSubObjectQuery() throws Exception {
        EntityManager em = app.getEntityManager();
        assertNotNull( em );

        // create two game entities, each with an array of entities that have subField = 'Foo'

        Map<String, Object> properties = new LinkedHashMap<String, Object>() {{
            put( "subObjectArray", new ArrayList<Map<String, Object>>() {{
                add( new LinkedHashMap<String, Object>() {{
                    put( "subField", "Foo" );
                }} );
            }} );
        }};

        Entity entity1 = em.create( "game", properties );
        assertNotNull( entity1 );

        Entity entity2 = em.create( "game", properties );
        assertNotNull( entity2 );

        app.refreshIndex();


        // search for games without sub-field Foo should returned zero entities

        Query query = Query.fromQL( "select * where NOT subObjectArray.subField = 'Foo'" ).withLimit(1);
        Results r = em.searchCollection( em.getApplicationRef(), "games", query );
        assertEquals( 0, r.size() );
        assertNull(r.getCursor());


        // full negation in simple with lower limit
        query = Query.fromQL( "select * where NOT subObjectArray.subField = 'Bar'" ).withLimit( 1 );
        r = em.searchCollection( em.getApplicationRef(), "games", query );
        assertEquals( 1, r.size() );
        assertNotNull(r.getCursor());
        assertEquals(entity2, r.getEntities().get(0));


        query = Query.fromQL( "select * where NOT subObjectArray.subField = 'Bar'" ).withLimit( 1 )
                     .withCursor( r.getCursor() );
        r = em.searchCollection( em.getApplicationRef(), "games", query );
        assertEquals(1, r.size());
        assertNotNull(r.getCursor());
        assertEquals(entity1, r.getEntities().get(0));

        query = Query.fromQL( "select * where NOT subObjectArray.subField = 'Bar'" ).withLimit( 1 )
                     .withCursor( r.getCursor() );
        r = em.searchCollection( em.getApplicationRef(), "games", query );
        assertEquals(0, r.size());
        assertNull(r.getCursor());
    }


    @Test
    public void testKeywordsOrQuery() throws Exception {
        LOG.debug( "testKeywordsOrQuery" );

        EntityManager em = app.getEntityManager();
        assertNotNull( em );

        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put( "title", "Galactians 2" );
        properties.put( "keywords", "Hot, Space Invaders, Classic" );
        em.create( "game", properties );

        properties = new LinkedHashMap<String, Object>();
        properties.put( "title", "Bunnies Extreme" );
        properties.put( "keywords", "Hot, New" );
        em.create( "game", properties );

        properties = new LinkedHashMap<String, Object>();
        properties.put( "title", "Hot Shots" );
        properties.put("keywords", "Action, New");
        em.create( "game", properties );

        app.refreshIndex();

        Query query = Query.fromQL( "select * where keywords contains 'hot' or title contains 'hot'" );
        Results r = em.searchCollection( em.getApplicationRef(), "games", query );
        LOG.info(JsonUtils.mapToFormattedJsonString(r.getEntities()));
        assertEquals( 3, r.size() );
    }


    @Test
    public void testKeywordsAndQuery() throws Exception {
        LOG.debug( "testKeywordsOrQuery" );

        EntityManager em = app.getEntityManager();
        assertNotNull( em );

        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put( "title", "Galactians 2" );
        properties.put( "keywords", "Hot, Space Invaders, Classic" );
        Entity firstGame = em.create( "game", properties );

        properties = new LinkedHashMap<String, Object>();
        properties.put( "title", "Bunnies Extreme" );
        properties.put("keywords", "Hot, New");
        Entity secondGame = em.create( "game", properties );

        properties = new LinkedHashMap<String, Object>();
        properties.put( "title", "Hot Shots Extreme" );
        properties.put( "keywords", "Action, New" );
        Entity thirdGame = em.create( "game", properties );

        app.refreshIndex();//need to track all batches then resolve promises

        Query query = Query.fromQL( "select * where keywords contains 'new' and title contains 'extreme'" );
        Results r = em.searchCollection( em.getApplicationRef(), "games", query );
        LOG.info( JsonUtils.mapToFormattedJsonString( r.getEntities() ) );
        assertEquals(2, r.size());

        assertEquals( thirdGame.getUuid(), r.getEntities().get( 0 ).getUuid() );
        assertEquals( secondGame.getUuid(), r.getEntities().get( 1 ).getUuid() );
    }


    @Test
    public void pagingAfterDelete() throws Exception {
        LOG.debug( "pagingAfterDelete" );


        EntityManager em = app.getEntityManager();
        assertNotNull( em );

        int initialSize = 20;
        List<UUID> entityIds = new ArrayList<UUID>();

        for ( int i = 0; i < initialSize; i++ ) {
            Map<String, Object> properties = new LinkedHashMap<String, Object>();
            properties.put( "name", "object" + i );
            Entity created = em.create( "objects", properties );

            entityIds.add( created.getUuid() );
        }

        app.refreshIndex();

        Query query = new Query();
        query.setLimit( 50 );

        Results r = em.searchCollection( em.getApplicationRef(), "objects", query );

        LOG.info( JsonUtils.mapToFormattedJsonString( r.getEntities() ) );

        assertEquals( initialSize, r.size() );

        // check they're all the same before deletion
        for ( int i = 0; i < initialSize; i++ ) {
            final int index = initialSize - i - 1;
            final UUID entityId = entityIds.get( index );

            assertEquals( entityId, r.getEntities().get( i ).getUuid() );
        }

        // now delete 5 items that will span the 10 pages
        int numDeleted = 0;
        for ( int i = 5; i < 10; i++ ) {
            Entity entity = r.getEntities().get( i );
            em.delete( entity );
            entityIds.remove( entity.getUuid() );
            numDeleted++;
        }

        app.refreshIndex();

        // wait for indexes to be cleared
        Thread.sleep(1000); //TODO find why we have to wait.  This is a bug

        // now query with paging
        query = new Query();

        r = em.searchCollection( em.getApplicationRef(), "objects", query );

        assertEquals( query.getLimit(), r.size() );

        for ( int i = 0; i < 10; i++ ) {
            final int index = initialSize - i - numDeleted - 1;
            final UUID entityId = entityIds.get( index );


            assertEquals( entityId, r.getEntities().get( i ).getUuid() );
        }

        // try the next page, set our cursor, it should be the last 5 entities
        query = new Query();
        query.setCursor( r.getCursor() );

        r = em.searchCollection( em.getApplicationRef(), "objects", query );

        assertEquals( 5, r.size() );
        for ( int i = 10; i < 15; i++ ) {
            final int index = initialSize - i - numDeleted - 1;
            final UUID entityId = entityIds.get( index );


            assertEquals( entityId, r.getEntities().get( i - 10 ).getUuid() );
        }
    }


    @Test
    public void pagingLessThanWithCriteria() throws Exception {
        LOG.debug( "pagingLessThanWithCriteria" );

        EntityManager em = app.getEntityManager();
        assertNotNull( em );

        int size = 40;
        List<UUID> entityIds = new ArrayList<UUID>();

        for ( int i = 0; i < size; i++ ) {
            Map<String, Object> properties = new LinkedHashMap<String, Object>();
            properties.put( "index", i );
            Entity created = em.create( "page", properties );

            entityIds.add( created.getUuid() );
        }

        int pageSize = 10;

        app.refreshIndex();
        final Query query = Query.fromQL( "index < " + size * 2 + " order by index asc" );

        Results r = null;

        // check they're all the same before deletion
        for ( int i = 0; i < size / pageSize; i++ ) {

            r = em.searchCollection( em.getApplicationRef(), "pages", query );

            LOG.info( JsonUtils.mapToFormattedJsonString( r.getEntities() ) );

            assertEquals( pageSize, r.size() );

            for ( int j = 0; j < pageSize; j++ ) {
                assertEquals( entityIds.get( i * pageSize + j ), r.getEntities().get( j ).getUuid() );
            }

            query.setCursor( r.getCursor() );
        }

        //check our last search
        r = em.searchCollection( em.getApplicationRef(), "pages", query );

        assertEquals( 0, r.size() );

        assertNull( r.getCursor() );
    }


    @Test
    public void pagingGreaterThanWithCriteria() throws Exception {
        LOG.debug( "pagingGreaterThanWithCriteria" );


        EntityManager em = app.getEntityManager();
        assertNotNull( em );

        int size = 40;
        List<UUID> entityIds = new ArrayList<UUID>();

        for ( int i = 0; i < size; i++ ) {
            Map<String, Object> properties = new LinkedHashMap<String, Object>();
            properties.put( "index", i );
            Entity created = em.create( "page", properties );

            entityIds.add( created.getUuid() );
        }

        int pageSize = 10;

        app.refreshIndex();

        Query query = Query.fromQL( "select * where index >= " + size / 2 + " sort by index asc" );
        query.setLimit( pageSize );

        Results r = null;


        //2 rounds of iterations since we should get 20->40
        for ( int i = 0; i < 2; i++ ) {

            r = em.searchCollection( em.getApplicationRef(), "pages", query );

            LOG.info( JsonUtils.mapToFormattedJsonString( r.getEntities() ) );

            assertEquals( pageSize, r.size() );

            for ( int j = 0; j < pageSize; j++ ) {

                final int indexToCheck = size - ( i * pageSize + j ) - 1;
                final UUID entityId = entityIds.get( indexToCheck );

                assertEquals( entityId, r.getEntities().get( j ).getUuid() );
            }

            query.setCursor( r.getCursor() );
        }

        r = em.searchCollection( em.getApplicationRef(), "pages", query );

        assertEquals( 0, r.size() );

        assertNull( r.getCursor() );
    }


    @Test
    public void pagingWithBoundsCriteria() throws Exception {
        LOG.debug( "pagingWithBoundsCriteria" );

        EntityManager em = app.getEntityManager();
        assertNotNull( em );

        int size = 40;
        List<UUID> entityIds = new ArrayList<UUID>();

        for ( int i = 0; i < size; i++ ) {
            Map<String, Object> properties = new LinkedHashMap<String, Object>();
            properties.put( "index", i );
            Entity created = em.create( "page", properties );

            entityIds.add( created.getUuid() );
        }

        app.refreshIndex();

        int pageSize = 10;

        Query query = Query.fromQL( "select * where index >= 10 and index <= 29 sort by index asc" );
        query.setLimit( pageSize );

        Results r = null;

        // check they're all the same before deletion
        for ( int i = 1; i < 3; i++ ) {

            r = em.searchCollection( em.getApplicationRef(), "pages", query );

            LOG.info( JsonUtils.mapToFormattedJsonString( r.getEntities() ) );

            assertEquals( pageSize, r.size() );

            for ( int j = 0; j < pageSize; j++ ) {
                final int index = size - 1 - ( i * pageSize + j );
                final UUID expectedId = entityIds.get(index);
                assertEquals( expectedId, r.getEntities().get( j ).getUuid() );
            }

            query.setCursor( r.getCursor() );
        }

        r = em.searchCollection( em.getApplicationRef(), "pages", query );

        assertEquals( 0, r.size() );

        assertNull( r.getCursor() );
    }


    @Test
    public void testPagingWithGetNextResults() throws Exception {
        LOG.debug( "testPagingWithGetNextResults" );

        EntityManager em = app.getEntityManager();
        assertNotNull( em );

        int size = 60;
        List<UUID> entityIds = new ArrayList<UUID>();

        for ( int i = 0; i < size; i++ ) {
            Map<String, Object> properties = new LinkedHashMap<String, Object>();
            properties.put( "index", i );
            Entity created = em.create( "page", properties );

            entityIds.add( created.getUuid() );
        }

        app.refreshIndex();

        int pageSize = 5;

        Query query = Query.fromQL("select * where index >= 5 and index <= 49 order by index asc");
        query.setLimit( pageSize );

        Results r = em.searchCollection(em.getApplicationRef(), "pages", query);

        // check they're all the same before deletion
        for ( int i = 1; i < 10; i++ ) {

            LOG.info( JsonUtils.mapToFormattedJsonString( r.getEntities() ) );

            assertEquals( pageSize, r.size() );

            for ( int j = 0; j < pageSize; j++ ) {
                final int expectedIndex = i * pageSize + j;
                final UUID entityId = entityIds.get( expectedIndex );

                final UUID returnedId = r.getEntities().get( j ).getUuid();

                assertEquals( entityId, returnedId );
            }
            LOG.info( "collection loop "+i );

            r = r.getNextPageResults();
        }

        assertEquals( 0, r.size() );
        assertNull(r.getCursor());
    }


    @Test
    public void subpropertyQuerying() throws Exception {

        EntityManager em = app.getEntityManager();
        assertNotNull( em );

        LOG.debug( "subpropertyQuerying" );

        Map<String, Object> root = new HashMap<String, Object>();

        Map<String, Object> subEntity = new HashMap<String, Object>();

        root.put( "rootprop1", "simpleprop" );

        subEntity.put( "intprop", 10 );
        subEntity.put( "substring", "I'm a tokenized string that should be indexed" );

        root.put( "subentity", subEntity );


        Entity saved = em.create( "test", root );

        app.refreshIndex();

        Query query = Query.fromQL( "rootprop1 = 'simpleprop'" );
        Entity entity;
        Results results;
        results = em.searchCollection( em.getApplicationRef(), "tests", query );
        entity = results.getEntitiesMap().get( saved.getUuid() );

        assertNotNull( entity );

        // query on the nested int value
        query = Query.fromQL( "subentity.intprop = 10" );

        results = em.searchCollection( em.getApplicationRef(), "tests", query );

        entity = results.getEntitiesMap().get( saved.getUuid() );

        assertNotNull( entity );

        // query on the nexted tokenized value
        query = Query.fromQL( "subentity.substring contains 'tokenized' and subentity.substring contains 'indexed'" );

        results = em.searchCollection( em.getApplicationRef(), "tests", query );

        entity = results.getEntitiesMap().get( saved.getUuid() );

        assertNotNull( entity );
    }


    @Test
    public void arrayQuerying() throws Exception {
        EntityManager em = app.getEntityManager();
        assertNotNull( em );
        LOG.debug( "arrayQuerying" );


        Map<String, Object> root = new HashMap<String, Object>();

        root.put( "intprop", 10 );
        root.put( "array", new String[] { "val1", "val2", "val3 with spaces" } );

        Map<String, Object> jsonData = ( Map<String, Object> ) JsonUtils.parse( JsonUtils.mapToJsonString( root ) );


        Entity saved = em.create( "test", jsonData );

        app.refreshIndex();

        Query query = Query.fromQL( "intprop = 10" );

        Results results = em.searchCollection( em.getApplicationRef(), "tests", query );

        Entity entity = results.getEntitiesMap().get( saved.getUuid() );

        assertNotNull( entity );

        // query on the nested int value
        query = Query.fromQL( "array = 'val1'" );

        results = em.searchCollection( em.getApplicationRef(), "tests", query );

        entity = results.getEntitiesMap().get( saved.getUuid() );

        assertNotNull( entity );

        // query on the nexted tokenized value
        query = Query.fromQL( "array = 'val2'" );

        results = em.searchCollection( em.getApplicationRef(), "tests", query );

        entity = results.getEntitiesMap().get( saved.getUuid() );

        assertNotNull( entity );

        query = Query.fromQL( "array = 'val3'" );

        results = em.searchCollection( em.getApplicationRef(), "tests", query );

        entity = results.getEntitiesMap().get( saved.getUuid() );

        assertNull( entity );

        query = Query.fromQL( "array contains 'spaces'" );
        results = em.searchCollection( em.getApplicationRef(), "tests", query );

        entity = results.getEntitiesMap().get( saved.getUuid() );

        assertNotNull( entity );
    }


    @Test
    public void stringWithSpaces() throws Exception {
        EntityManager em = app.getEntityManager();
        assertNotNull( em );

        LOG.debug( "stringWithSpaces" );

        Map<String, Object> props = new HashMap<String, Object>();

        props.put( "myString", "My simple string" );


        Entity saved = em.create( "test", props );

        app.refreshIndex();

        Query query = Query.fromQL( "myString = 'My simple string'" );

        Results results = em.searchCollection( em.getApplicationRef(), "tests", query );

        Entity entity = results.getEntitiesMap().get( saved.getUuid() );

        assertNotNull( entity );
    }


    @Test
    public void testSelectTerms() throws Exception {
        LOG.debug( "testSelectTerms" );
        EntityManager em = app.getEntityManager();
        assertNotNull( em );


        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put( "username", "edanuff" );
        properties.put( "email", "ed@anuff.com" );

        em.create( "user", properties );

        app.refreshIndex();

        String s = "select username, email where username = 'edanuff'";
        Query query = Query.fromQL( s );

        Results r = em.searchCollection( em.getApplicationRef(), "users", query );
        assertTrue( r.size() == 1 );

        // selection results should be a list of lists
        //        List<Object> sr = query.getSelectionResults( r );
        //        assertTrue( sr.size() == 1 );
        //        List firstResult = ( List ) sr.get( 0 );
        //        assertTrue( "edanuff".equals( firstResult.get( 0 ) ) );
        //        assertTrue( "ed@anuff.com".equals( firstResult.get( 1 ) ) );
    }


    @Test
    public void testRedefineTerms() throws Exception {
        LOG.debug( "testRedefineTerms" );

        EntityManager em = app.getEntityManager();
        assertNotNull( em );

        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put( "username", "edanuff" );
        properties.put( "email", "ed@anuff.com" );

        em.create( "user", properties );

        app.refreshIndex();

        String s = "select {name: username, email: email} where username = 'edanuff'";
        Query query = Query.fromQL( s );

        Results r = em.searchCollection( em.getApplicationRef(), "users", query );
        assertTrue( r.size() == 1 );

        // TODO: do we need selection results?

        // selection results should be a list of lists
        //        List<Object> sr = query.getSelectionResults( r );
        //        assertTrue( sr.size() == 1 );
        //        Map firstResult = ( Map ) sr.get( 0 );
        //        assertTrue( "edanuff".equals( firstResult.get( "name" ) ) );
        //        assertTrue( "ed@anuff.com".equals( firstResult.get( "email" ) ) );
    }


    @Test
    public void testSelectEmailViaConnection() throws Exception {
        LOG.debug( "testSelectEmailViaConnection" );

        EntityManager em = app.getEntityManager();
        assertNotNull( em );

        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put( "username", "ed@anuff.com" );
        properties.put( "email", "ed@anuff.com" );

        final Entity entity = em.create( "user", properties );

        app.refreshIndex();

        String s = "select * where username = 'ed@anuff.com'";
        Query query = Query.fromQL( s );

        Results r = em.searchCollection( em.getApplicationRef(), "users", query );
        assertTrue( r.size() == 1 );
        assertEquals( entity.getUuid(), r.getId() );

        // selection results should be a list of lists

        assertTrue( "ed@anuff.com".equals( entity.getProperty( "username" ) ) );
        assertTrue( "ed@anuff.com".equals( entity.getProperty( "email" ) ) );

        // now create a role and connect it
        properties = new LinkedHashMap<String, Object>();
        properties.put( "name", "test" );

        Entity foo = em.create( "foo", properties );

        em.createConnection( foo, "testconnection", entity );

        app.refreshIndex();

        // now query via the testConnection, this should work

        query = Query.fromQL( s );
        query.setConnectionType( "testconnection" );
        query.setEntityType( "user" );

        r = em.searchTargetEntities(foo, query);

        assertEquals( "connection must match", 1, r.size() );
        assertEquals( entity.getUuid(), r.getId() );

        // selection results should be a list of lists
        final Entity entityResults = r.getEntity();
        assertEquals( "ed@anuff.com", entityResults.getProperty( "username" ) );
        assertEquals( "ed@anuff.com", entityResults.getProperty( "email" ) );
    }


    @Test
    public void testNotQueryAnd() throws Exception {
        LOG.debug( "testNotQueryAnd" );


        EntityManager em = app.getEntityManager();
        assertNotNull( em );

        Map<String, Object> location = new LinkedHashMap<String, Object>();
        location.put( "Place", "24 Westminster Avenue, Venice, CA 90291, USA" );
        location.put( "Longitude", -118.47425979999998 );
        location.put( "Latitude", 33.9887663 );

        Map<String, Object> recipient = new LinkedHashMap<String, Object>();
        recipient.put( "TimeRequested", 1359077878l );
        recipient.put( "Username", "fb_536692245" );
        recipient.put( "Location", location );

        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put( "Flag", "requested" );
        properties.put( "Recipient", recipient );

        em.create( "loveobject", properties );

        app.refreshIndex();

        location = new LinkedHashMap<String, Object>();
        location.put( "Place", "Via Pietro Maroncelli, 48, 62012 Santa Maria Apparente Province of Macerata, Italy" );
        location.put( "Longitude", 13.693080199999999 );
        location.put( "Latitude", 43.2985019 );

        recipient = new LinkedHashMap<String, Object>();
        recipient.put( "TimeRequested", 1359077878l );
        recipient.put( "Username", "fb_100000787138041" );
        recipient.put( "Location", location );

        properties = new LinkedHashMap<String, Object>();
        properties.put( "Flag", "requested" );
        properties.put( "Recipient", recipient );

        em.create( "loveobject", properties );

        app.refreshIndex();

        // String s = "select * where Flag = 'requested'";
        // String s = "select * where Flag = 'requested' and NOT Recipient.Username =
        // 'fb_536692245' order by created asc";

        String s =
            "select * where Flag = 'requested' and NOT Recipient.Username " + "= 'fb_536692245' order by created asc";
        Query query = Query.fromQL( s );

        Results r = em.searchCollection( em.getApplicationRef(), "loveobjects", query );
        assertTrue( r.size() == 1 );

        String username = ( String ) ( ( Map ) r.getEntities().get( 0 ).getProperty( "Recipient" ) ).get( "Username" );

        // selection results should be a list of lists
        //        List<Object> sr = query.getSelectionResults( r );(
        //        assertTrue( sr.size() == 1 );

        assertEquals( "fb_100000787138041", username );
    }


    @Test
    public void runtimeTypeCorrect() throws Exception {
        LOG.debug( "runtimeTypeCorrect" );


        EntityManager em = app.getEntityManager();
        assertNotNull( em );

        int size = 20;
        List<User> createdEntities = new ArrayList<User>();

        for ( int i = 0; i < size; i++ ) {
            User user = new User();
            user.setEmail( String.format( "test%d@usergrid.com", i ) );
            user.setUsername( String.format( "test%d", i ) );
            user.setName( String.format( "test%d", i ) );

            User created = em.create( user );

            createdEntities.add( created );
        }

        app.refreshIndex();

        Results r = em.getCollection( em.getApplicationRef(), "users", null, 50, Level.ALL_PROPERTIES, false );

        LOG.info( JsonUtils.mapToFormattedJsonString( r.getEntities() ) );

        assertEquals( size, r.size() );

        // check they're all the same before deletion
        for ( int i = 0; i < size; i++ ) {
            final Entity entity = r.getEntities().get( size - i - 1 );

            assertEquals( createdEntities.get( i ).getUuid(), entity.getUuid() );
            assertTrue( entity instanceof User );
        }
    }


    @Test
    public void badOrderByBadGrammarAsc() throws Exception {
        LOG.debug( "badOrderByBadGrammarAsc" );

        EntityManager em = app.getEntityManager();
        assertNotNull( em );

        String s = "select * where name = 'bob' order by";

        String error = null;
        String entityType = null;
        String propertyName = null;

        try {
            em.searchCollection( em.getApplicationRef(), "users", Query.fromQL( s ) );
            fail( "I should throw an exception" );
        }
        catch ( Exception nie ) {
            error = nie.getMessage();
            //            entityType = nie.getEntityType();
            //            propertyName = nie.getPropertyName();
        }

        //        assertEquals( "Entity 'user' with property named '' is not indexed.  "
        //                + "You cannot use the this field in queries.", error );
        //        assertEquals( "user", entityType );
        //        assertEquals( "", propertyName );
    }


    @Test
    public void badOrderByBadGrammarDesc() throws Exception {
        LOG.debug( "badOrderByBadGrammarDesc" );

        EntityManager em = app.getEntityManager();
        assertNotNull( em );

        String s = "select * where name = 'bob' order by";

        String error = null;
        String entityType = null;
        String propertyName = null;


        try {
            em.searchCollection( em.getApplicationRef(), "users", Query.fromQL( s ) );
            fail( "I should throw an exception" );
        }
        catch ( Exception nie ) {
            error = nie.getMessage();
            //            entityType = nie.getEntityType();
            //            propertyName = nie.getPropertyName();
        }

        //        assertEquals( "Entity 'user' with property named '' is not indexed.  "
        //                + "You cannot use the this field in queries.", error );
        //        assertEquals( "user", entityType );
        //        assertEquals( "", propertyName );
    }


    @Test
    public void uuidIdentifierTest() throws Exception {
        LOG.debug( "uuidIdentifierTest" );

        EntityManager em = app.getEntityManager();
        assertNotNull( em );

        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put( "keywords", "blah,test,game" );
        properties.put( "title", "Solitaire" );

        Entity game1 = em.create( "game", properties );
        assertNotNull( game1 );

        //we create 2 entities, otherwise this test will pass when it shouldn't
        Entity game2 = em.create( "game", properties );
        assertNotNull( game2 );

        app.refreshIndex();

        // overlap
        Query query = new Query();
        query.addIdentifier( Identifier.fromUUID( game1.getUuid() ) );
        Results r = em.searchCollection( em.getApplicationRef(), "games", query );
        assertEquals( "We should only get 1 result", 1, r.size() );
        assertNull( "No cursor should be present", r.getCursor() );

        assertEquals( "Saved entity returned", game1, r.getEntity() );
    }


    @Test
    public void nameIdentifierTest() throws Exception {
        LOG.debug( "nameIdentifierTest" );

        EntityManager em = app.getEntityManager();
        assertNotNull( em );


        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put( "keywords", "blah,test,game" );
        properties.put( "title", "Solitaire" );
        properties.put( "name", "test" );

        Entity game1 = em.create( "games", properties );
        assertNotNull( game1 );

        //we create 2 entities, otherwise this test will pass when it shouldn't
        properties.put( "name", "test2" );
        Entity game2 = em.create( "game", properties );
        assertNotNull( game2 );

        app.refreshIndex();

        // overlap
        Query query = new Query();
        query.addIdentifier( Identifier.fromName( "test" ) );
        Results r = em.searchCollection( em.getApplicationRef(), "games", query );
        assertEquals( "We should only get 1 result", 1, r.size() );
        assertNull( "No cursor should be present", r.getCursor() );

        assertEquals( "Saved entity returned", game1, r.getEntity() );
    }


    @Test
    public void emailIdentifierTest() throws Exception {
        LOG.debug( "emailIdentifierTest" );

        EntityManager em = app.getEntityManager();
        assertNotNull( em );

        User user = new User();
        user.setUsername( "foobar" );
        user.setEmail( "foobar@usergrid.org" );

        Entity createUser = em.create( user );
        assertNotNull( createUser );

        //we create 2 entities, otherwise this test will pass when it shouldn't
        User user2 = new User();
        user2.setUsername( "foobar2" );
        user2.setEmail( "foobar2@usergrid.org" );
        Entity createUser2 = em.create( user2 );
        assertNotNull( createUser2 );

        app.refreshIndex();

        // overlap
        Query query = new Query();
        query.addIdentifier( Identifier.fromEmail( "foobar@usergrid.org" ) );
        Results r = em.searchCollection( em.getApplicationRef(), "users", query );
        assertEquals( "We should only get 1 result", 1, r.size() );
        assertNull( "No cursor should be present", r.getCursor() );

        assertEquals( "Saved entity returned", createUser, r.getEntity() );
    }


    @Test( expected = DuplicateUniquePropertyExistsException.class )
    public void duplicateIdentifierTest() throws Exception {
        LOG.debug( "duplicateIdentifierTest" );

        EntityManager em = app.getEntityManager();
        assertNotNull( em );

        User user = new User();
        user.setUsername( "foobar" );
        user.setEmail( "foobar@usergrid.org" );

        Entity createUser = em.create( user );
        assertNotNull( createUser );

        //we create 2 entities, otherwise this test will pass when it shouldn't
        User user2 = new User();
        user2.setUsername( "foobar" );
        user2.setEmail( "foobar@usergrid.org" );
        em.create( user2 );
    }


    @Test( expected = DuplicateUniquePropertyExistsException.class )
    public void duplicateNameTest() throws Exception {
        LOG.debug( "duplicateNameTest" );

        EntityManager em = app.getEntityManager();
        assertNotNull( em );

        DynamicEntity restaurant = new DynamicEntity();
        restaurant.setName( "4peaks" );

        Entity createdRestaurant = em.create( "restaurant", restaurant.getProperties() );
        assertNotNull( createdRestaurant );

        //we create 2 entities, otherwise this test will pass when it shouldn't
        DynamicEntity restaurant2 = new DynamicEntity();
        restaurant2.setName( "4peaks" );

        em.create( "restaurant", restaurant2.getProperties() );
    }
}
