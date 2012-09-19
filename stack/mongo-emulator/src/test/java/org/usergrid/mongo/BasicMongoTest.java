package org.usergrid.mongo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bson.types.ObjectId;
import org.junit.Ignore;
import org.junit.Test;
import org.usergrid.mongo.protocol.OpDelete;
import org.usergrid.persistence.Entity;
import org.usergrid.persistence.EntityManager;
import org.usergrid.persistence.Query;
import org.usergrid.persistence.Results;
import org.usergrid.persistence.SimpleEntityRef;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
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

        // check we've created the collection

        Set<String> colls = db.getCollectionNames();

        assertTrue(colls.contains("inserttests"));

        // iterate the collection to ensure we can retrieve the object
        DBCollection coll = db.getCollection("inserttests");
        DBCursor cur = coll.find();

        BasicDBObject returnedObject = null;

        assertTrue(cur.hasNext());

        returnedObject = (BasicDBObject) cur.next();

        assertFalse(cur.hasNext());

        UUID id = UUID.fromString(returnedObject.get("uuid").toString());

        //this should work.  Appears to be the type of ObjectId getting lost on column serialization
        ObjectId returnedOid = new ObjectId(returnedObject.getString("_id"));

        assertEquals("nico", returnedObject.get("name"));
        assertEquals("tabby", returnedObject.get("color"));
        assertEquals(savedOid, returnedOid);
        assertNotNull(id);

        BasicDBObject query = new BasicDBObject();
        query.put("_id", savedOid);

        // now load by the mongo Id. Users will use this the most to read data.

        returnedObject = new BasicDBObject(db.getCollection("inserttests")
                .findOne(query).toMap());

        assertEquals("nico", returnedObject.get("name"));
        assertEquals("tabby", returnedObject.get("color"));

        assertEquals(savedOid, new ObjectId(returnedObject.getString("_id")));
        assertEquals(id.toString(), returnedObject.get("uuid"));

        // check we can find it when using the native entity manager

        UUID appId = emf.lookupApplication("test-organization/test-app");
        EntityManager em = emf.getEntityManager(appId);

        Entity entity = em.get(id);

        assertNotNull(entity);
        assertEquals("nico", entity.getProperty("name"));
        assertEquals("tabby", entity.getProperty("color"));

    }
    
    @Test
    public void insertDuplicateTest() throws Exception {

        DB db = getDb();

        BasicDBObject doc = new BasicDBObject();

        doc.put("username", "insertduplicate");

        WriteResult result = db.getCollection("users").insert(doc);

   
        assertNull(result.getError());

        // check we've created the collection

        Set<String> colls = db.getCollectionNames();

        assertTrue(colls.contains("users"));

        // iterate the collection to ensure we can retrieve the object
        doc = new BasicDBObject();

        doc.put("username", "insertduplicate");

        
        String message = null;
        
        try{
            result = db.getCollection("users").insert(doc);
        }catch (MongoException me){
            message = me.getMessage();
        }

        assertNotNull(message);
        assertEquals("command failed [getlasterror]: { \"serverUsed\" : \"localhost/127.0.0.1:27017\" , \"n\" : 0 , \"connectionId\" : 20 , \"wtime\" : 0 , \"err\" : \"Entity users requires that property named username be unique, value of insertduplicate exists\" , \"ok\" : 0.0}", message);

    }

    @Test
    public void deleteTest() throws Exception {

        DB db = getDb();

        BasicDBObject doc = new BasicDBObject();

        doc.put("name", "nico");
        doc.put("color", "tabby");

        WriteResult result = db.getCollection("deletetests").insert(doc);

        ObjectId savedOid = doc.getObjectId("_id");

        assertNull(result.getError());

        BasicDBObject query = new BasicDBObject();
        query.put("_id", savedOid);

        // now load by the mongo Id. Users will use this the most to read data.

        BasicDBObject returnedObject = new BasicDBObject(db
                .getCollection("deletetests").findOne(query).toMap());

        assertEquals("nico", returnedObject.get("name"));
        assertEquals("tabby", returnedObject.get("color"));

        // TODO uncomment me assertEquals(savedOid,
        // returnedObject.getObjectId("_id"));

        UUID id = UUID.fromString(returnedObject.get("uuid").toString());

        // now delete the object
        db.getCollection("deletetests").remove(returnedObject,
                WriteConcern.SAFE);

        DBObject searched = db.getCollection("deletetests").findOne(query);

        assertNull(searched);

        // check it has been deleted

        UUID appId = emf.lookupApplication("test-organization/test-app");
        EntityManager em = emf.getEntityManager(appId);

        Entity entity = em.get(id);

        assertNull(entity);

    }

    @Test
    @Ignore("Really slow on the delete, not a good unit tests atm")
    public void deleteBatchTest() throws Exception {

        DB db = getDb();

        int count = (int) (OpDelete.BATCH_SIZE * 1.5);

        List<DBObject> docs = new ArrayList<DBObject>(count);

        for (int i = 0; i < count; i++) {
            BasicDBObject doc = new BasicDBObject();

            doc.put("index", i);

            docs.add(doc);
        }

        
        WriteResult result = db.getCollection("deletebatchtests").insert(docs);
        
        assertNull(result.getLastError().getErrorMessage());

        //iterate over all the data to make sure it's been inserted

        DBCursor cursor = db.getCollection("deletebatchtests").find();
        
        for(int i = 0; i < count && cursor.hasNext(); i ++){
            int index = new BasicDBObject(cursor.next().toMap()).getInt("index");
            
            assertEquals(i, index);
        }

   
        BasicDBObject query = new BasicDBObject();
        query.put("index", new BasicDBObject("$lte", count));
      
        // now delete the objects
        db.getCollection("deletebatchtests").remove(query, WriteConcern.SAFE);

        //now  try and iterate, there should be no results
        cursor = db.getCollection("deletebatchtests").find();
        
        assertFalse(cursor.hasNext());
        
        // check it has been deleted
        UUID appId = emf.lookupApplication("test-organization/test-app");
        EntityManager em = emf.getEntityManager(appId);

        Results results = em.searchCollection(new SimpleEntityRef("application", appId), "deletebatchtests", new Query());

        assertEquals(0, results.size());

    }
}
