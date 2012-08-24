package org.usergrid.mongo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Set;
import java.util.UUID;

import org.junit.Ignore;
import org.junit.Test;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;

public class BasicMongoTest extends AbstractMongoTest {
//    public class BasicMongoTest {

    @Test
//    @Ignore
    public void insertTest() throws Exception {
        Mongo m = new Mongo("localhost", 27017);
        m.setWriteConcern(WriteConcern.SAFE);

        DB db = m.getDB("test-organization/test-app");
        db.authenticate("test", "test".toCharArray());

        
        
//        DB db = m.getDB("testapp");
//        db.authenticate("test", "test".toCharArray());

        BasicDBObject doc = new BasicDBObject();

        doc.put("name", "nico");
        doc.put("color", "tabby");

        WriteResult result = db.getCollection("inserttests").insert(doc);

        assertNull(result.getError());
        Object mongoId = result.getField("_id");

        assertNotNull(mongoId);
        
        assertEquals(doc.get("_id"), mongoId);
        
        Object uuid = result.getField("uuid");
        
        assertNotNull(uuid);
        
        UUID id = UUID.fromString(uuid.toString());
        
        assertNotNull(id);
        

        Set<String> colls = db.getCollectionNames();

        assertTrue(colls.contains("inserttests"));
        

        DBCollection coll = db.getCollection("inserttests");
        DBCursor cur = coll.find();
        
        int count = 0;
        
        DBObject object = null;
        
        while (cur.hasNext()) {
            count++;
            object = cur.next();
            assertEquals("nico", object.get("name"));
            assertEquals("tabby", object.get("color"));
            assertEquals(mongoId, object.get("_id"));
            assertEquals(id, object.get("uuid"));
        }
        
        assertEquals(1, count);
        

//        UUID appId = emf.lookupApplication("test-organization/test-app");
//        EntityManager em = emf.getEntityManager(appId);
//        
//        
//        Entity entity = em.get(id);
//        assertEquals("nico", entity.getProperty("name"));
//        assertEquals("tabby", entity.getProperty("color"));
        
        
       
    }
}
