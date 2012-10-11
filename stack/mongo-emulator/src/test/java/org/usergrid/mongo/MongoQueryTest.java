package org.usergrid.mongo;

import static org.junit.Assert.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bson.types.BasicBSONList;
import org.junit.Test;
import org.usergrid.persistence.EntityManager;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;

public class MongoQueryTest extends AbstractMongoTest {

    @Test
    public void stringEqual() throws Exception {

        UUID appId = emf.lookupApplication("test-organization/test-app");
        EntityManager em = emf.getEntityManager(appId);

        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put("name", "Kings of Leon");
        properties.put("genre", "Southern Rock");
        properties.put("founded", 2000);
        em.create("stringequal", properties);

        properties = new LinkedHashMap<String, Object>();
        properties.put("name", "Stone Temple Pilots");
        properties.put("genre", "Rock");
        properties.put("founded", 1986);
        em.create("stringequal", properties);

        properties = new LinkedHashMap<String, Object>();
        properties.put("name", "Journey");
        properties.put("genre", "Classic Rock");
        properties.put("founded", 1973);
        em.create("stringequal", properties);

        // See http://www.mongodb.org/display/DOCS/Java+Tutorial

        Mongo m = new Mongo("localhost", 27017);

        DB db = m.getDB("test-organization/test-app");
        db.authenticate("test", "test".toCharArray());

        Set<String> colls = db.getCollectionNames();

        assertTrue(colls.contains("stringequals"));

        DBCollection coll = db.getCollection("stringequals");
        DBCursor cur = coll.find();
        int count = 0;

        while (cur.hasNext()) {
            cur.next();
            count++;
        }

        assertEquals(3, count);

        BasicDBObject query = new BasicDBObject();
        query.put("genre", "Southern Rock");
        cur = coll.find(query);

        assertTrue(cur.hasNext());

        DBObject result = cur.next();
        assertEquals("Kings of Leon", result.get("name"));
        assertEquals("Southern Rock", result.get("genre"));

        assertFalse(cur.hasNext());

    }

    @Test
    public void greaterThan() throws Exception {

        UUID appId = emf.lookupApplication("test-organization/test-app");
        EntityManager em = emf.getEntityManager(appId);

        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put("name", "Kings of Leon");
        properties.put("genre", "Southern Rock");
        properties.put("founded", 2000);
        em.create("greaterthan", properties);

        properties = new LinkedHashMap<String, Object>();
        properties.put("name", "Stone Temple Pilots");
        properties.put("genre", "Rock");
        properties.put("founded", 1986);
        em.create("greaterthan", properties);

        properties = new LinkedHashMap<String, Object>();
        properties.put("name", "Journey");
        properties.put("genre", "Classic Rock");
        properties.put("founded", 1973);
        em.create("greaterthan", properties);

        // See http://www.mongodb.org/display/DOCS/Java+Tutorial

        Mongo m = new Mongo("localhost", 27017);

        DB db = m.getDB("test-organization/test-app");
        db.authenticate("test", "test".toCharArray());

        BasicDBObject query = new BasicDBObject();
        query.put("founded", new BasicDBObject("$gt", 1973));

        DBCollection coll = db.getCollection("greaterthans");
        DBCursor cur = coll.find(query);

        assertTrue(cur.hasNext());

        DBObject result = cur.next();
        assertEquals("Stone Temple Pilots", result.get("name"));
        assertEquals("Rock", result.get("genre"));

        result = cur.next();
        assertEquals("Kings of Leon", result.get("name"));
        assertEquals("Southern Rock", result.get("genre"));

        assertFalse(cur.hasNext());

    }

    @Test
    public void greaterThanEqual() throws Exception {

        UUID appId = emf.lookupApplication("test-organization/test-app");
        EntityManager em = emf.getEntityManager(appId);

        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put("name", "Kings of Leon");
        properties.put("genre", "Southern Rock");
        properties.put("founded", 2000);
        em.create("greaterthanequal", properties);

        properties = new LinkedHashMap<String, Object>();
        properties.put("name", "Stone Temple Pilots");
        properties.put("genre", "Rock");
        properties.put("founded", 1986);
        em.create("greaterthanequal", properties);

        properties = new LinkedHashMap<String, Object>();
        properties.put("name", "Journey");
        properties.put("genre", "Classic Rock");
        properties.put("founded", 1973);
        em.create("greaterthanequal", properties);

        // See http://www.mongodb.org/display/DOCS/Java+Tutorial

        Mongo m = new Mongo("localhost", 27017);

        DB db = m.getDB("test-organization/test-app");
        db.authenticate("test", "test".toCharArray());

        BasicDBObject query = new BasicDBObject();
        query.put("founded", new BasicDBObject("$gte", 1973));

        DBCollection coll = db.getCollection("greaterthanequals");
        DBCursor cur = coll.find(query);

        assertTrue(cur.hasNext());

        DBObject result = cur.next();
        assertEquals("Journey", result.get("name"));
        assertEquals("Classic Rock", result.get("genre"));

        result = cur.next();
        assertEquals("Stone Temple Pilots", result.get("name"));
        assertEquals("Rock", result.get("genre"));

        result = cur.next();
        assertEquals("Kings of Leon", result.get("name"));
        assertEquals("Southern Rock", result.get("genre"));

        assertFalse(cur.hasNext());

    }

    @Test
    public void lessThan() throws Exception {

        UUID appId = emf.lookupApplication("test-organization/test-app");
        EntityManager em = emf.getEntityManager(appId);

        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put("name", "Kings of Leon");
        properties.put("genre", "Southern Rock");
        properties.put("founded", 2000);
        em.create("lessthan", properties);

        properties = new LinkedHashMap<String, Object>();
        properties.put("name", "Stone Temple Pilots");
        properties.put("genre", "Rock");
        properties.put("founded", 1986);
        em.create("lessthan", properties);

        properties = new LinkedHashMap<String, Object>();
        properties.put("name", "Journey");
        properties.put("genre", "Classic Rock");
        properties.put("founded", 1973);
        em.create("lessthan", properties);

        // See http://www.mongodb.org/display/DOCS/Java+Tutorial

        Mongo m = new Mongo("localhost", 27017);

        DB db = m.getDB("test-organization/test-app");
        db.authenticate("test", "test".toCharArray());

        BasicDBObject query = new BasicDBObject();
        query.put("founded", new BasicDBObject("$lt", 2000));

        DBCollection coll = db.getCollection("lessthans");
        DBCursor cur = coll.find(query);

        assertTrue(cur.hasNext());

        DBObject result = cur.next();
        assertEquals("Journey", result.get("name"));
        assertEquals("Classic Rock", result.get("genre"));

        result = cur.next();
        assertEquals("Stone Temple Pilots", result.get("name"));
        assertEquals("Rock", result.get("genre"));

        assertFalse(cur.hasNext());

    }

    @Test
    public void lessThanEqual() throws Exception {

        UUID appId = emf.lookupApplication("test-organization/test-app");
        EntityManager em = emf.getEntityManager(appId);

        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put("name", "Kings of Leon");
        properties.put("genre", "Southern Rock");
        properties.put("founded", 2000);
        em.create("lessthanequal", properties);

        properties = new LinkedHashMap<String, Object>();
        properties.put("name", "Stone Temple Pilots");
        properties.put("genre", "Rock");
        properties.put("founded", 1986);
        em.create("lessthanequal", properties);

        properties = new LinkedHashMap<String, Object>();
        properties.put("name", "Journey");
        properties.put("genre", "Classic Rock");
        properties.put("founded", 1973);
        em.create("lessthanequal", properties);

        // See http://www.mongodb.org/display/DOCS/Java+Tutorial

        Mongo m = new Mongo("localhost", 27017);

        DB db = m.getDB("test-organization/test-app");
        db.authenticate("test", "test".toCharArray());

        BasicDBObject query = new BasicDBObject();
        query.put("founded", new BasicDBObject("$lte", 2000));

        DBCollection coll = db.getCollection("lessthanequals");
        DBCursor cur = coll.find(query);

        assertTrue(cur.hasNext());

        DBObject result = cur.next();
        assertEquals("Journey", result.get("name"));
        assertEquals("Classic Rock", result.get("genre"));

        result = cur.next();
        assertEquals("Stone Temple Pilots", result.get("name"));
        assertEquals("Rock", result.get("genre"));

        result = cur.next();
        assertEquals("Kings of Leon", result.get("name"));
        assertEquals("Southern Rock", result.get("genre"));

        assertFalse(cur.hasNext());

    }

    @Test
    public void in() throws Exception {

        UUID appId = emf.lookupApplication("test-organization/test-app");
        EntityManager em = emf.getEntityManager(appId);

        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put("name", "Kings of Leon");
        properties.put("genre", "Southern Rock");
        properties.put("founded", 2000);
        em.create("testin", properties);

        properties = new LinkedHashMap<String, Object>();
        properties.put("name", "Stone Temple Pilots");
        properties.put("genre", "Rock");
        properties.put("founded", 1986);
        em.create("testin", properties);

        properties = new LinkedHashMap<String, Object>();
        properties.put("name", "Journey");
        properties.put("genre", "Classic Rock");
        properties.put("founded", 1973);
        em.create("testin", properties);

        // See http://www.mongodb.org/display/DOCS/Java+Tutorial

        Mongo m = new Mongo("localhost", 27017);

        DB db = m.getDB("test-organization/test-app");
        db.authenticate("test", "test".toCharArray());

        BasicBSONList list = new BasicBSONList();
        list.add("Stone Temple Pilots");
        list.add("Journey");

        BasicDBObject query = new BasicDBObject();
        query.put("name", new BasicDBObject("$in", list));

        DBCollection coll = db.getCollection("testins");
        DBCursor cur = coll.find(query);

        assertTrue(cur.hasNext());

        DBObject result = cur.next();
        assertEquals("Journey", result.get("name"));
        assertEquals("Classic Rock", result.get("genre"));

        result = cur.next();
        assertEquals("Stone Temple Pilots", result.get("name"));
        assertEquals("Rock", result.get("genre"));

        assertFalse(cur.hasNext());

    }

    @Test
    public void or() throws Exception {

        UUID appId = emf.lookupApplication("test-organization/test-app");
        EntityManager em = emf.getEntityManager(appId);

        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put("name", "Kings of Leon");
        properties.put("genre", "Southern Rock");
        properties.put("founded", 2000);
        em.create("testor", properties);

        properties = new LinkedHashMap<String, Object>();
        properties.put("name", "Stone Temple Pilots");
        properties.put("genre", "Rock");
        properties.put("founded", 1986);
        em.create("testor", properties);

        properties = new LinkedHashMap<String, Object>();
        properties.put("name", "Journey");
        properties.put("genre", "Classic Rock");
        properties.put("founded", 1973);
        em.create("testor", properties);

        // See http://www.mongodb.org/display/DOCS/Java+Tutorial

        Mongo m = new Mongo("localhost", 27017);

        DB db = m.getDB("test-organization/test-app");
        db.authenticate("test", "test".toCharArray());

        BasicBSONList list = new BasicBSONList();
        list.add(new BasicDBObject("founded", new BasicDBObject("$gte", 2000)));
        list.add(new BasicDBObject("founded", new BasicDBObject("$lte", 1973)));

        BasicDBObject query = new BasicDBObject();
        query.put("$or", list);

        DBCollection coll = db.getCollection("testors");
        DBCursor cur = coll.find(query);

        assertTrue(cur.hasNext());

        DBObject result = cur.next();
        assertEquals("Journey", result.get("name"));
        assertEquals("Classic Rock", result.get("genre"));

        result = cur.next();
        assertEquals("Kings of Leon", result.get("name"));
        assertEquals("Southern Rock", result.get("genre"));

        assertFalse(cur.hasNext());

    }

    @Test
    public void and() throws Exception {

        UUID appId = emf.lookupApplication("test-organization/test-app");
        EntityManager em = emf.getEntityManager(appId);

        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put("name", "Kings of Leon");
        properties.put("genre", "Southern Rock");
        properties.put("founded", 2000);
        em.create("testand", properties);

        properties = new LinkedHashMap<String, Object>();
        properties.put("name", "Stone Temple Pilots");
        properties.put("genre", "Rock");
        properties.put("founded", 1986);
        em.create("testand", properties);

        properties = new LinkedHashMap<String, Object>();
        properties.put("name", "Journey");
        properties.put("genre", "Classic Rock");
        properties.put("founded", 1973);
        em.create("testand", properties);

        // See http://www.mongodb.org/display/DOCS/Java+Tutorial

        Mongo m = new Mongo("localhost", 27017);

        DB db = m.getDB("test-organization/test-app");
        db.authenticate("test", "test".toCharArray());

        BasicBSONList list = new BasicBSONList();
        list.add(new BasicDBObject("founded", new BasicDBObject("$gte", 2000)));
        list.add(new BasicDBObject("founded", new BasicDBObject("$lte", 2005)));

        BasicDBObject query = new BasicDBObject();
        query.put("$and", list);

        DBCollection coll = db.getCollection("testands");
        DBCursor cur = coll.find(query);

        assertTrue(cur.hasNext());

        DBObject result = cur.next();
        assertEquals("Kings of Leon", result.get("name"));
        assertEquals("Southern Rock", result.get("genre"));
        assertFalse(cur.hasNext());

    }

  @Test
  public void withFieldSelector() throws Exception {
    UUID appId = emf.lookupApplication("test-organization/test-app");
    EntityManager em = emf.getEntityManager(appId);

    Map<String, Object> properties = new LinkedHashMap<String, Object>();
    properties.put("name", "Kings of Leon");
    properties.put("genre", "Southern Rock");
    properties.put("founded", 2000);
    em.create("withfieldselector", properties);

    properties = new LinkedHashMap<String, Object>();
    properties.put("name", "Stone Temple Pilots");
    properties.put("genre", "Rock");
    properties.put("founded", 1986);
    em.create("withfieldselector", properties);


    properties = new LinkedHashMap<String, Object>();
    properties.put("name", "Journey");
    properties.put("genre", "Classic Rock");
    properties.put("founded", 1973);
    em.create("withfieldselector", properties);

    Mongo m = new Mongo("localhost", 27017);

    DB db = m.getDB("test-organization/test-app");
    db.authenticate("test", "test".toCharArray());

    BasicDBObject queryName = new BasicDBObject();
    queryName.put("name", "Journey");

    BasicDBObject limitName = new BasicDBObject();
    limitName.put("name",1);

    //query.put();
    DBCollection coll = db.getCollection("withfieldselectors");

    DBCursor cur = coll.find(queryName, limitName);

    assertTrue(cur.hasNext());



  }
}
