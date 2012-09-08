package org.usergrid.mongo;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.UnknownHostException;
import java.util.Set;
import java.util.UUID;

import org.junit.Ignore;
import org.junit.Test;
import org.usergrid.persistence.Entity;
import org.usergrid.persistence.EntityManager;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.MongoException;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;

public class BasicMongoTest extends AbstractMongoTest {

    @Test
    public void insertTest() throws Exception {

        DB db = getDb();

        BasicDBObject doc = new BasicDBObject();

        doc.put("name", "nico");
        doc.put("color", "tabby");

        WriteResult result = db.getCollection("inserttests").insert(doc);

        assertNull(result.getError());

        Set<String> colls = db.getCollectionNames();

        assertTrue(colls.contains("inserttests"));

        DBCollection coll = db.getCollection("inserttests");
        DBCursor cur = coll.find();

        int count = 0;

        DBObject object = null;


        assertTrue(cur.hasNext());

        object = cur.next();
        
        assertFalse(cur.hasNext());
        
        UUID id = UUID.fromString(object.get("uuid").toString());
        
        Object oid = object.get("_id");
        
        
        
        
        assertEquals("nico", object.get("name"));
        assertEquals("tabby", object.get("color"));
        assertNotNull(oid);
        assertNotNull(id);
        
        
        
        UUID appId = emf.lookupApplication("test-organization/test-app");
        EntityManager em = emf.getEntityManager(appId);

        Entity entity = em.get(id);
        
        assertNotNull(entity);
        assertEquals("nico", entity.getProperty("name"));
        assertEquals("tabby", entity.getProperty("color"));

    }

    @Test
    public void responseIdConsistent() throws UnknownHostException,
            MongoException {
        DB db = getDb();

        DBCollection coll = db.getCollection("inserttests");
        coll.getIndexInfo();

        Set<String> colNames = db.getCollectionNames();

    }
}
