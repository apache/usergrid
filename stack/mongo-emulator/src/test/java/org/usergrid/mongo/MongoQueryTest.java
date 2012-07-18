package org.usergrid.mongo;

import static org.junit.Assert.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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
    public void basicTest() throws Exception {
        UUID appId = emf.lookupApplication("test-organization/test-query-app");
        EntityManager em = emf.getEntityManager(appId);

        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put("name", "Kings of Leon");
        properties.put("genre", "Southern Rock");
        em.create("band", properties);

        properties = new LinkedHashMap<String, Object>();
        properties.put("name", "Stone Temple Pilots");
        properties.put("genre", "Rock");
        em.create("band", properties);

        properties = new LinkedHashMap<String, Object>();
        properties.put("name", "Journey");
        properties.put("genre", "Classic Rock");
        em.create("band", properties);

        // See http://www.mongodb.org/display/DOCS/Java+Tutorial

        Mongo m = new Mongo("localhost", 27017);

        DB db = m.getDB("test-organization/test-query-app");
        db.authenticate("test@usergrid.com", "test".toCharArray());

        Set<String> colls = db.getCollectionNames();

        boolean found = false;

        for (String s : colls) {
            if ("bands".equals(s)) {
                found = true;
                break;
            }
        }

        assertTrue(found);

        DBCollection coll = db.getCollection("bands");
        DBCursor cur = coll.find();
        int count = 0;

        while (cur.hasNext()) {
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
}
