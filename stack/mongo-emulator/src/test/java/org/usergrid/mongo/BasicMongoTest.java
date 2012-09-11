package org.usergrid.mongo;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.UnknownHostException;
import java.util.Set;
import java.util.UUID;

import org.bson.types.ObjectId;
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

        
        ObjectId savedOid = doc.getObjectId("_id");
     
        
        assertNull(result.getError());
        
        //check we've created the collection

        Set<String> colls = db.getCollectionNames();

        assertTrue(colls.contains("inserttests"));

        //iterate the collection to ensure we can retreive the object
        DBCollection coll = db.getCollection("inserttests");
        DBCursor cur = coll.find();

        BasicDBObject returnedObject = null;

        assertTrue(cur.hasNext());

        returnedObject = (BasicDBObject)cur.next();

        assertFalse(cur.hasNext());

        UUID id = UUID.fromString(returnedObject.get("uuid").toString());
        
        ObjectId returnedOid = returnedObject.getObjectId("_id");

        
        assertEquals("nico", returnedObject.get("name"));
        assertEquals("tabby", returnedObject.get("color"));
        assertEquals(savedOid, returnedOid);
        assertNotNull(id);
        
        BasicDBObject query = new BasicDBObject();
        query.put("_id", savedOid);

        // now load by the mongo Id. Users will use this the most to read data.

        returnedObject =  new BasicDBObject(db.getCollection("inserttests").findOne(query).toMap());
        
        assertEquals("nico", returnedObject.get("name"));
        assertEquals("tabby", returnedObject.get("color"));
      
        assertEquals(savedOid, returnedObject.getObjectId("_id"));
        assertNotNull(id.toString(), returnedObject.get("uuid"));

        // check we can find it when using the native entity manager

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
