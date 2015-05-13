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
package org.apache.usergrid.mongo;


import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bson.types.ObjectId;
import org.junit.Ignore;
import org.junit.Test;
import org.apache.usergrid.mongo.protocol.OpDelete;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.index.query.Query;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.persistence.SimpleEntityRef;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoException;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


@Ignore
public class BasicMongoTest extends AbstractMongoTest {

    @Test
    public void insertTest() throws Exception {

        DB db = getDb();

        BasicDBObject doc = new BasicDBObject();

        doc.put( "name", "nico" );
        doc.put( "color", "tabby" );

        WriteResult result = db.getCollection( "inserttests" ).insert( doc );

        ObjectId savedOid = doc.getObjectId( "_id" );

        assertNull( result.getError() );

        // check we've created the collection

        Set<String> colls = db.getCollectionNames();

        assertTrue( colls.contains( "inserttests" ) );

        // iterate the collection to ensure we can retrieve the object
        DBCollection coll = db.getCollection( "inserttests" );
        DBCursor cur = coll.find();

        BasicDBObject returnedObject = null;

        assertTrue( cur.hasNext() );

        returnedObject = ( BasicDBObject ) cur.next();

        assertFalse( cur.hasNext() );

        UUID id = UUID.fromString( returnedObject.get( "uuid" ).toString() );

        //this should work.  Appears to be the type of ObjectId getting lost on column serialization
        ObjectId returnedOid = new ObjectId( returnedObject.getString( "_id" ) );

        assertEquals( "nico", returnedObject.get( "name" ) );
        assertEquals( "tabby", returnedObject.get( "color" ) );
        assertEquals( savedOid, returnedOid );
        assertNotNull( id );

        BasicDBObject query = new BasicDBObject();
        query.put( "_id", savedOid );

        // now load by the mongo Id. Users will use this the most to read data.

        returnedObject = new BasicDBObject( db.getCollection( "inserttests" ).findOne( query ).toMap() );

        assertEquals( "nico", returnedObject.get( "name" ) );
        assertEquals( "tabby", returnedObject.get( "color" ) );

        assertEquals( savedOid, new ObjectId( returnedObject.getString( "_id" ) ) );
        assertEquals( id.toString(), returnedObject.get( "uuid" ) );

        // check we can find it when using the native entity manager

        UUID appId = emf.lookupApplication( "test-organization/test-app" ).get();
        EntityManager em = emf.getEntityManager( appId );

        Entity entity = em.get( new SimpleEntityRef( (String)returnedObject.get("type"), id ));

        assertNotNull( entity );
        assertEquals( "nico", entity.getProperty( "name" ) );
        assertEquals( "tabby", entity.getProperty( "color" ) );
    }


    @Test
    public void insertDuplicateTest() throws Exception {

        DB db = getDb();

        BasicDBObject doc = new BasicDBObject();

        doc.put( "username", "insertduplicate" );

        WriteResult result = db.getCollection( "users" ).insert( doc );


        assertNull( result.getError() );

        // check we've created the collection

        Set<String> colls = db.getCollectionNames();

        assertTrue( colls.contains( "users" ) );

        // iterate the collection to ensure we can retrieve the object
        doc = new BasicDBObject();

        doc.put( "username", "insertduplicate" );


        String message = null;

        try {
            result = db.getCollection( "users" ).insert( doc );
        }
        catch ( MongoException me ) {
            message = me.getMessage();
        }

        assertNotNull( message );
        assertTrue( message.contains(
                "Entity users requires that property named username be unique, value of insertduplicate exists" ) );
    }


    @Test
    public void updateTest() throws Exception {

        DB db = getDb();

        BasicDBObject doc = new BasicDBObject();

        doc.put( "name", "nico" );
        doc.put( "color", "tabby" );

        WriteResult result = db.getCollection( "updatetests" ).insert( doc );

        ObjectId savedOid = doc.getObjectId( "_id" );

        assertNull( result.getError() );

        // check we've created the collection
        Set<String> colls = db.getCollectionNames();

        assertTrue( colls.contains( "updatetests" ) );

        // iterate the collection to ensure we can retrieve the object
        DBCollection coll = db.getCollection( "updatetests" );
        DBCursor cur = coll.find();

        BasicDBObject returnedObject = null;

        assertTrue( cur.hasNext() );

        returnedObject = ( BasicDBObject ) cur.next();

        assertFalse( cur.hasNext() );

        UUID id = UUID.fromString( returnedObject.get( "uuid" ).toString() );

        //this should work.  Appears to be the type of ObjectId getting lost on column serialization
        ObjectId returnedOid = new ObjectId( returnedObject.getString( "_id" ) );

        assertEquals( "nico", returnedObject.get( "name" ) );
        assertEquals( "tabby", returnedObject.get( "color" ) );
        assertEquals( savedOid, returnedOid );
        assertNotNull( id );

        BasicDBObject query = new BasicDBObject();
        query.put( "_id", savedOid );

        // now load by the mongo Id. Users will use this the most to read data.

        returnedObject = new BasicDBObject( db.getCollection( "updatetests" ).findOne( query ).toMap() );

        assertEquals( "nico", returnedObject.get( "name" ) );
        assertEquals( "tabby", returnedObject.get( "color" ) );
        assertEquals( savedOid, new ObjectId( returnedObject.getString( "_id" ) ) );
        assertEquals( id.toString(), returnedObject.get( "uuid" ) );

        //now update the object and save it
        BasicDBObject object = new BasicDBObject();
        object.put( "newprop", "newvalue" );
        object.put( "color", "black" );

        db.getCollection( "updatetests" ).update( query, object );

        // check we can find it when using the native entity manager

        Thread.sleep( 5000 );

        UUID appId = emf.lookupApplication( "test-organization/test-app" ).get();
        EntityManager em = emf.getEntityManager( appId );

        Entity entity = em.get( new SimpleEntityRef( (String)returnedObject.get("type"), id ) );

        assertNotNull( entity );
        assertEquals( "nico", entity.getProperty( "name" ) );
        assertEquals( "black", entity.getProperty( "color" ) );
        assertEquals( "newvalue", entity.getProperty( "newprop" ) );


        //now check it in the client
        returnedObject = new BasicDBObject( db.getCollection( "updatetests" ).findOne( query ).toMap() );

        assertEquals( "nico", returnedObject.get( "name" ) );
        assertEquals( "black", returnedObject.get( "color" ) );
        assertEquals( "newvalue", returnedObject.get( "newprop" ) );
        assertEquals( savedOid, new ObjectId( returnedObject.getString( "_id" ) ) );
        assertEquals( id.toString(), returnedObject.get( "uuid" ) );
    }


    @Test
    public void deleteTest() throws Exception {

        DB db = getDb();

        BasicDBObject doc = new BasicDBObject();

        doc.put( "name", "nico" );
        doc.put( "color", "tabby" );

        WriteResult result = db.getCollection( "deletetests" ).insert( doc );

        ObjectId savedOid = doc.getObjectId( "_id" );

        assertNull( result.getError() );

        BasicDBObject query = new BasicDBObject();
        query.put( "_id", savedOid );

        // now load by the mongo Id. Users will use this the most to read data.

        BasicDBObject returnedObject = new BasicDBObject( db.getCollection( "deletetests" ).findOne( query ).toMap() );

        assertEquals( "nico", returnedObject.get( "name" ) );
        assertEquals( "tabby", returnedObject.get( "color" ) );

        // TODO uncomment me assertEquals(savedOid,
        // returnedObject.getObjectId("_id"));

        UUID id = UUID.fromString( returnedObject.get( "uuid" ).toString() );

        // now delete the object
        db.getCollection( "deletetests" ).remove( returnedObject, WriteConcern.SAFE );

        DBObject searched = db.getCollection( "deletetests" ).findOne( query );

        assertNull( searched );

        // check it has been deleted

        UUID appId = emf.lookupApplication( "test-organization/test-app" ).get();
        EntityManager em = emf.getEntityManager( appId );

        Entity entity = em.get( new SimpleEntityRef( (String)returnedObject.get("type"), id ) );

        assertNull( entity );
    }


    @Test
    @Ignore("Really slow on the delete, not a good unit tests atm")
    public void deleteBatchTest() throws Exception {

        DB db = getDb();

        int count = ( int ) ( OpDelete.BATCH_SIZE * 1.5 );

        List<DBObject> docs = new ArrayList<DBObject>( count );

        for ( int i = 0; i < count; i++ ) {
            BasicDBObject doc = new BasicDBObject();

            doc.put( "index", i );

            docs.add( doc );
        }


        WriteResult result = db.getCollection( "deletebatchtests" ).insert( docs );

        assertNull( result.getLastError().getErrorMessage() );

        //iterate over all the data to make sure it's been inserted

        DBCursor cursor = db.getCollection( "deletebatchtests" ).find();

        for ( int i = 0; i < count && cursor.hasNext(); i++ ) {
            int index = new BasicDBObject( cursor.next().toMap() ).getInt( "index" );

            assertEquals( i, index );
        }


        BasicDBObject query = new BasicDBObject();
        query.put( "index", new BasicDBObject( "$lte", count ) );

        // now delete the objects
        db.getCollection( "deletebatchtests" ).remove( query, WriteConcern.SAFE );

        //now  try and iterate, there should be no results
        cursor = db.getCollection( "deletebatchtests" ).find();

        assertFalse( cursor.hasNext() );

        // check it has been deleted
        UUID appId = emf.lookupApplication( "test-organization/test-app" ).get();
        EntityManager em = emf.getEntityManager( appId );

        Results results =
                em.searchCollection( new SimpleEntityRef( "application", appId ), "deletebatchtests", new Query() );

        assertEquals( 0, results.size() );
    }
}
