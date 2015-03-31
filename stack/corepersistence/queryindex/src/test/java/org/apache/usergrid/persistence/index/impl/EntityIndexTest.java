/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 */
package org.apache.usergrid.persistence.index.impl;


import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.usergrid.persistence.index.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.lang3.time.StopWatch;

import org.apache.usergrid.persistence.core.guice.MigrationManagerRule;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.core.test.UseModules;
import org.apache.usergrid.persistence.core.util.Health;
import org.apache.usergrid.persistence.index.guice.TestIndexModule;
import org.apache.usergrid.persistence.index.query.CandidateResults;
import org.apache.usergrid.persistence.index.query.Query;
import org.apache.usergrid.persistence.index.utils.UUIDUtils;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.field.ArrayField;
import org.apache.usergrid.persistence.model.field.EntityObjectField;
import org.apache.usergrid.persistence.model.field.StringField;
import org.apache.usergrid.persistence.model.field.UUIDField;
import org.apache.usergrid.persistence.model.field.value.EntityObject;
import org.apache.usergrid.persistence.model.util.EntityUtils;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


@RunWith(EsRunner.class)
@UseModules({ TestIndexModule.class })
public class EntityIndexTest extends BaseIT {

    private static final Logger log = LoggerFactory.getLogger( EntityIndexTest.class );

    @Inject
    public EntityIndexFactory eif;
    @Inject
    public EntityIndex ei;

    //TODO T.N. Remove this when we move the cursor mapping back to core
    @Inject
    @Rule
    public MigrationManagerRule migrationManagerRule;

    @Before
    public  void setup(){
        ei.initialize();
    }

    @Test
    public void testIndex() throws IOException, InterruptedException {
        Id appId = new SimpleId("application");

        ApplicationScope applicationScope = new ApplicationScopeImpl(appId);
        ApplicationEntityIndex entityIndex = eif.createApplicationEntityIndex(applicationScope);


        final String entityType = "thing";
        IndexScope indexScope = new IndexScopeImpl(appId, "things");
        final SearchTypes searchTypes = SearchTypes.fromTypes(entityType);

        insertJsonBlob(entityIndex, entityType, indexScope, "/sample-large.json", 101, 0);

        ei.refresh();

        testQueries(indexScope, searchTypes, entityIndex);
    }

    @Test
    @Ignore("this is a problem i will work on when i can breathe")
    public void testIndexVariations() throws IOException {
        Id appId = new SimpleId( "application" );

        ApplicationScope applicationScope = new ApplicationScopeImpl( appId );

        ApplicationEntityIndex entityIndex = eif.createApplicationEntityIndex(applicationScope);

        final String entityType = "thing";
        IndexScope indexScope = new IndexScopeImpl( appId, "things" );
        final SearchTypes searchTypes = SearchTypes.fromTypes(entityType);
        EntityIndexBatch batch = entityIndex.createBatch();
        Entity entity = new Entity( entityType );
        EntityUtils.setVersion(entity, UUIDGenerator.newTimeUUID());
        entity.setField(new UUIDField(IndexingUtils.ENTITYID_ID_FIELDNAME, UUID.randomUUID()));
        entity.setField(new StringField("testfield","test"));
        batch.index(indexScope, entity);
        batch.execute().get();

        EntityUtils.setVersion(entity, UUIDGenerator.newTimeUUID());
        List<String> list = new ArrayList<>();
        list.add("test");
        entity.setField(new ArrayField<String>("testfield", list));
        batch.index(indexScope, entity);
        batch.execute().get();

        EntityUtils.setVersion(entity, UUIDGenerator.newTimeUUID());
        EntityObject testObj = new EntityObject();
        testObj.setField(new StringField("test","testFiedl"));
        entity.setField(new EntityObjectField("testfield", testObj));
        batch.index(indexScope, entity);
        batch.execute().get();

        ei.refresh();

        testQueries(indexScope, searchTypes, entityIndex);
    }

    @Test
    public void testIndexThreads() throws IOException {
        final Id appId = new SimpleId( "application" );

        final ApplicationScope applicationScope = new ApplicationScopeImpl( appId );

        long now = System.currentTimeMillis();
        final int threads = 20;
        final int size = 30;
        final ApplicationEntityIndex entityIndex = eif.createApplicationEntityIndex(applicationScope);
        final IndexScope indexScope = new IndexScopeImpl(appId, "things");
        final String entityType = "thing";

        final CountDownLatch latch = new CountDownLatch(threads);
        final AtomicLong failTime=new AtomicLong(0);
        InputStream is = this.getClass().getResourceAsStream(  "/sample-large.json" );
        ObjectMapper mapper = new ObjectMapper();
        final List<Object> sampleJson = mapper.readValue( is, new TypeReference<List<Object>>() {} );
        for(int i=0;i<threads;i++) {
            Thread thread = new Thread(new Runnable() {
                public void run() {
                    try {
                        EntityIndexBatch batch = entityIndex.createBatch();
                        insertJsonBlob(sampleJson,batch, entityType, indexScope, size, 0);
                        batch.execute().get();
                    } catch (Exception e) {
                        synchronized (failTime) {
                            if (failTime.get() == 0) {
                                failTime.set(System.currentTimeMillis());
                            }
                        }
                        System.out.println(e.toString());
                        fail("threw exception");
                    }finally {
                        latch.countDown();
                    }
                }
            });
            thread.start();
        }
        try {
            latch.await();
        }catch (InterruptedException ie){
            throw new RuntimeException(ie);
        }
        assertTrue("system must have failed at " + (failTime.get() - now), failTime.get() == 0);
    }

    @Test
    public void testMultipleIndexInitializations(){
        Id appId = new SimpleId( "application" );

        ApplicationScope applicationScope = new ApplicationScopeImpl( appId );

        ApplicationEntityIndex entityIndex = eif.createApplicationEntityIndex(applicationScope);

        for(int i=0;i<10;i++) {

        }

    }

    @Test
    public void testAddMultipleIndexes() throws IOException {
        Id appId = new SimpleId( "application" );

        ApplicationScope applicationScope = new ApplicationScopeImpl( appId );

        ApplicationEntityIndex entityIndex = eif.createApplicationEntityIndex(applicationScope);


        final String entityType = "thing";
        IndexScope indexScope = new IndexScopeImpl( appId, "things" );
        final SearchTypes searchTypes = SearchTypes.fromTypes(entityType);

        insertJsonBlob(entityIndex, entityType, indexScope, "/sample-large.json",101,0);

       ei.refresh();

        testQueries(indexScope, searchTypes, entityIndex);

        ei.addIndex("v2", 1, 0, "one");

        insertJsonBlob(entityIndex, entityType, indexScope, "/sample-large.json", 101, 100);

       ei.refresh();

        //Hilda Youn
        testQuery(indexScope, searchTypes, entityIndex, "name = 'Hilda Young'", 1 );

        testQuery(indexScope, searchTypes, entityIndex, "name = 'Lowe Kelley'", 1 );
    }

    @Test
    public void testDeleteWithAlias() throws IOException {
        Id appId = new SimpleId( "application" );

        ApplicationScope applicationScope = new ApplicationScopeImpl( appId );

        ApplicationEntityIndex entityIndex = eif.createApplicationEntityIndex( applicationScope );


        final String entityType = "thing";
        IndexScope indexScope = new IndexScopeImpl( appId, "things" );
        final SearchTypes searchTypes = SearchTypes.fromTypes( entityType );

        insertJsonBlob(entityIndex, entityType, indexScope, "/sample-large.json",1,0);

        ei.refresh();

        ei.addIndex("v2", 1, 0, "one");

        insertJsonBlob(entityIndex, entityType, indexScope, "/sample-large.json", 1, 0);

        ei.refresh();
        CandidateResults crs = testQuery(indexScope, searchTypes, entityIndex, "name = 'Bowers Oneil'", 2);

        EntityIndexBatch entityIndexBatch = entityIndex.createBatch();
        entityIndexBatch.deindex(indexScope, crs.get(0));
        entityIndexBatch.deindex(indexScope, crs.get(1));
        entityIndexBatch.execute().get();
        ei.refresh();

        //Hilda Youn
        testQuery(indexScope, searchTypes, entityIndex, "name = 'Bowers Oneil'", 0);

    }
    private void insertJsonBlob(ApplicationEntityIndex entityIndex, String entityType, IndexScope indexScope, String filePath,final int max,final int startIndex) throws IOException {
        InputStream is = this.getClass().getResourceAsStream( filePath );
        ObjectMapper mapper = new ObjectMapper();
        List<Object> sampleJson = mapper.readValue( is, new TypeReference<List<Object>>() {} );
        EntityIndexBatch batch = entityIndex.createBatch();
        insertJsonBlob(sampleJson,batch, entityType, indexScope, max, startIndex);
        batch.execute().get();
        ei.refresh();
    }

    private void insertJsonBlob(List<Object> sampleJson, EntityIndexBatch batch, String entityType, IndexScope indexScope,final int max,final int startIndex) throws IOException {
        int count = 0;
        StopWatch timer = new StopWatch();
        timer.start();


        if(startIndex > 0){
            for(int i =0; i<startIndex;i++){
                sampleJson.remove(0);
            }
        }

        for ( Object o : sampleJson ) {

            Map<String, Object> item = ( Map<String, Object> ) o;

            Entity entity = new Entity( entityType );
            entity = EntityIndexMapUtils.fromMap(entity, item);
            EntityUtils.setVersion(entity, UUIDGenerator.newTimeUUID());
            entity.setField(new UUIDField(IndexingUtils.ENTITYID_ID_FIELDNAME, UUID.randomUUID()));
            batch.index(indexScope, entity);
            batch.execute().get();


            if ( ++count > max ) {
                break;
            }
        }

        timer.stop();
        log.info("Total time to index {} entries {}ms, average {}ms/entry",
                new Object[]{count, timer.getTime(), timer.getTime() / count } );
    }


    @Test
    public void testDeindex() {

        Id appId = new SimpleId( "application" );

        ApplicationScope applicationScope = new ApplicationScopeImpl( appId );

        IndexScope indexScope = new IndexScopeImpl( appId, "fastcars" );

        ApplicationEntityIndex entityIndex = eif.createApplicationEntityIndex(applicationScope);


        Map entityMap = new HashMap() {{
            put( "name", "Ferrari 212 Inter" );
            put( "introduced", 1952 );
            put( "topspeed", 215 );
        }};


        Entity entity = EntityIndexMapUtils.fromMap( entityMap );
        EntityUtils.setId( entity, new SimpleId( "fastcar" ) );
        EntityUtils.setVersion( entity, UUIDGenerator.newTimeUUID() );
        entity.setField(new UUIDField(IndexingUtils.ENTITYID_ID_FIELDNAME, UUID.randomUUID()));

        entityIndex.createBatch().index(indexScope , entity ).execute().get();
        ei.refresh();

        CandidateResults candidateResults = entityIndex.search( indexScope,
            SearchTypes.fromTypes( entity.getId().getType() ), Query.fromQL( "name contains 'Ferrari*'" ) );
        assertEquals( 1, candidateResults.size() );

        EntityIndexBatch batch = entityIndex.createBatch();
        batch.deindex(indexScope, entity).execute().get();
        batch.execute().get();
        ei.refresh();

        candidateResults = entityIndex.search( indexScope, SearchTypes.fromTypes(entity.getId().getType()), Query.fromQL( "name contains 'Ferrari*'" ) );
        assertEquals( 0, candidateResults.size() );
    }


    private CandidateResults testQuery(final IndexScope scope, final SearchTypes searchTypes, final ApplicationEntityIndex entityIndex, final String queryString, final int num ) {

        StopWatch timer = new StopWatch();
        timer.start();
        Query query = Query.fromQL( queryString );
        CandidateResults candidateResults = null;
        candidateResults = entityIndex.search( scope, searchTypes, query,num+1 );

        timer.stop();

        assertEquals( num,candidateResults.size() );
        log.debug( "Query time {}ms", timer.getTime() );
        return candidateResults;
    }


    private void testQueries(final IndexScope scope, SearchTypes searchTypes, final ApplicationEntityIndex entityIndex ) {

        testQuery(scope, searchTypes, entityIndex, "name = 'Morgan Pierce'", 1 );

        testQuery(scope, searchTypes, entityIndex, "name = 'morgan pierce'", 1 );

        testQuery(scope, searchTypes, entityIndex, "name = 'Morgan'", 0 );

        testQuery(scope, searchTypes, entityIndex, "name contains 'Morgan'", 1 );

        testQuery(scope, searchTypes, entityIndex, "company > 'GeoLogix'", 64 );

        testQuery(scope, searchTypes, entityIndex, "gender = 'female'", 45 );

        testQuery(scope, searchTypes, entityIndex, "name = 'Minerva Harrell' and age > 39", 1 );

        testQuery(scope, searchTypes, entityIndex, "name = 'Minerva Harrell' and age > 39 and age < 41", 1 );

        testQuery(scope, searchTypes, entityIndex, "name = 'Minerva Harrell' and age > 40", 0 );

        testQuery(scope, searchTypes, entityIndex, "name = 'Minerva Harrell' and age >= 40", 1 );

        testQuery(scope, searchTypes, entityIndex, "name = 'Minerva Harrell' and age <= 40", 1 );

        testQuery(scope, searchTypes, entityIndex, "name = 'Morgan* '", 1 );

        testQuery(scope, searchTypes, entityIndex, "name = 'Morgan*'", 1 );


        // test a couple of array sub-property queries

        int totalUsers = 102;

        // nobody has a friend named Jack the Ripper
        testQuery(scope, searchTypes, entityIndex, "friends.name = 'Jack the Ripper'", 0 );

        // everybody doesn't have a friend named Jack the Ripper
        testQuery(scope,  searchTypes,entityIndex, "not (friends.name = 'Jack the Ripper')", totalUsers );

        // one person has a friend named Shari Hahn
        testQuery(scope, searchTypes, entityIndex, "friends.name = 'Wendy Moody'", 1 );

        // everybody but 1 doesn't have a friend named Shari Hahh
        testQuery(scope, searchTypes, entityIndex, "not (friends.name = 'Shari Hahn')", totalUsers - 1);

    }


    /**
     * Tests that Entity-to-map and Map-to-entity round trip works.
     */
    @Test
    public void testEntityIndexMapUtils() throws IOException {

        InputStream is = this.getClass().getResourceAsStream( "/sample-small.json" );
        ObjectMapper mapper = new ObjectMapper();
        List<Object> contacts = mapper.readValue( is, new TypeReference<List<Object>>() {} );

        for ( Object o : contacts ) {

            Map<String, Object> map1 = ( Map<String, Object> ) o;

            // convert map to entity
            Entity entity1 = EntityIndexMapUtils.fromMap( map1 );

            // convert entity back to map
            Map map2 = EntityIndexMapUtils.toMap( entity1 );

            // the two maps should be the same
            Map diff = Maps.difference( map1, map2 ).entriesDiffering();
            assertEquals( 0, diff.size() );
        }
    }

    @Test
    public void deleteVerification() throws Throwable {

        Id appId = new SimpleId( "application" );
        Id ownerId = new SimpleId( "owner" );

        ApplicationScope applicationScope = new ApplicationScopeImpl( appId );

        IndexScope appScope = new IndexScopeImpl( ownerId, "user" );

        ApplicationEntityIndex entityIndex = eif.createApplicationEntityIndex(applicationScope);


        final String middleName = "middleName" + UUIDUtils.newTimeUUID();

        Map entityMap = new HashMap() {{
            put( "username", "edanuff" );
            put( "email", "ed@anuff.com" );
            put( "middlename", middleName );
        }};

        Entity user = EntityIndexMapUtils.fromMap( entityMap );
        EntityUtils.setId( user, new SimpleId( "edanuff" ) );
        EntityUtils.setVersion( user, UUIDGenerator.newTimeUUID() );


        EntityIndexBatch batch = entityIndex.createBatch();

        batch.index( appScope, user);
        batch.execute().get();
        ei.refresh();

        Query query = new Query();
        query.addEqualityFilter( "username", "edanuff" );
        CandidateResults r = entityIndex.search( appScope, SearchTypes.fromTypes( "edanuff" ), query );
        assertEquals( user.getId(), r.get( 0 ).getId() );

        batch.deindex(appScope, user.getId(), user.getVersion() );
        batch.execute().get();
        ei.refresh();

        // EntityRef
        query = new Query();
        query.addEqualityFilter( "username", "edanuff" );

        r = entityIndex.search(appScope,SearchTypes.fromTypes( "edanuff" ),  query );

        assertFalse( r.iterator().hasNext() );
    }

    @Test
    public void multiValuedTypes() {

        Id appId = new SimpleId( "entityindextest" );
        Id ownerId = new SimpleId( "multivaluedtype" );
        ApplicationScope applicationScope = new ApplicationScopeImpl( appId );

        IndexScope appScope = new IndexScopeImpl( ownerId, "user" );

        ApplicationEntityIndex entityIndex = eif.createApplicationEntityIndex(applicationScope);

        entityIndex.createBatch();

        // Bill has favorites as string, age as string and retirement goal as number
        Map billMap = new HashMap() {{
            put( "username", "bill" );
            put( "email", "bill@example.com" );
            put( "age", "thirtysomething");
            put( "favorites", "scallops, croquet, wine");
            put( "retirementGoal", 100000);
        }};
        Entity bill = EntityIndexMapUtils.fromMap( billMap );
        EntityUtils.setId( bill, new SimpleId( UUIDGenerator.newTimeUUID(), "user"  ) );
        EntityUtils.setVersion( bill, UUIDGenerator.newTimeUUID() );

        EntityIndexBatch batch = entityIndex.createBatch();

        batch.index( appScope,  bill );

        // Fred has age as int, favorites as object and retirement goal as object
        Map fredMap = new HashMap() {{
            put( "username", "fred" );
            put( "email", "fred@example.com" );
            put( "age", 41 );
            put( "favorites", new HashMap<String, Object>() {{
                put("food", "cheezewiz");
                put("sport", "nascar");
                put("beer", "budwizer");
            }});
            put( "retirementGoal", new HashMap<String, Object>() {{
                put("car", "Firebird");
                put("home", "Mobile");
            }});
        }};
        Entity fred = EntityIndexMapUtils.fromMap( fredMap );
        EntityUtils.setId( fred, new SimpleId( UUIDGenerator.newTimeUUID(), "user"  ) );
        EntityUtils.setVersion( fred, UUIDGenerator.newTimeUUID() );
        batch.index( appScope, fred);

        batch.execute().get();
        ei.refresh();

        final SearchTypes searchTypes = SearchTypes.fromTypes( "user" );

        Query query = new Query();
        query.addEqualityFilter( "username", "bill" );
        CandidateResults r = entityIndex.search( appScope, searchTypes,  query );
        assertEquals( bill.getId(), r.get( 0 ).getId() );

        query = new Query();
        query.addEqualityFilter( "username", "fred" );
        r = entityIndex.search( appScope, searchTypes,  query );
        assertEquals( fred.getId(), r.get( 0 ).getId() );

        query = new Query();
        query.addEqualityFilter( "age", 41 );
        r = entityIndex.search( appScope, searchTypes,  query );
        assertEquals( fred.getId(), r.get( 0 ).getId() );

        query = new Query();
        query.addEqualityFilter( "age", "thirtysomething" );
        r = entityIndex.search(  appScope, searchTypes, query );
        assertEquals( bill.getId(), r.get( 0 ).getId() );
    }


    @Test
    public void healthTest() {
        Id appId = new SimpleId( "entityindextest" );
        Id ownerId = new SimpleId( "multivaluedtype" );
        ApplicationScope applicationScope = new ApplicationScopeImpl( appId );
        assertNotEquals( "cluster should be ok", Health.RED, ei.getClusterHealth() );
        assertEquals("index should be ready", Health.GREEN, ei.getIndexHealth());
        ApplicationEntityIndex entityIndex = eif.createApplicationEntityIndex(applicationScope);

        ei.refresh();

        assertNotEquals( "cluster should be fine", Health.RED, ei.getIndexHealth() );
        assertNotEquals( "cluster should be ready now", Health.RED, ei.getClusterHealth() );
    }


    @Test
    public void testCursorFormat() throws Exception {

        Id appId = new SimpleId( "application" );
        Id ownerId = new SimpleId( "owner" );

        ApplicationScope applicationScope = new ApplicationScopeImpl( appId );

        IndexScope indexScope = new IndexScopeImpl( ownerId, "users" );


        ApplicationEntityIndex entityIndex = eif.createApplicationEntityIndex(applicationScope);


        final EntityIndexBatch batch = entityIndex.createBatch();


        final int size = 10;

        final List<Id> entities = new ArrayList<>( size );


        for ( int i = 0; i < size; i++ ) {
            final String middleName = "middleName" + UUIDUtils.newTimeUUID();

            Map entityMap = new HashMap() {{
                put( "username", "edanuff" );
                put( "email", "ed@anuff.com" );
                put( "middlename", middleName );
                put( "created", System.nanoTime() );
            }};

            final Id userId = new SimpleId( "user" );

            Entity user = EntityIndexMapUtils.fromMap( entityMap );
            EntityUtils.setId( user, userId );
            EntityUtils.setVersion( user, UUIDGenerator.newTimeUUID() );

            user.setField( new UUIDField( IndexingUtils.ENTITYID_ID_FIELDNAME, UUIDGenerator.newTimeUUID() ) );

            entities.add( userId );


            batch.index( indexScope, user );
        }


        batch.execute().get();
       ei.refresh();


        final int limit = 1;


        final int expectedPages = size / limit;


        String cursor = null;

        for ( int i = 0; i < expectedPages; i++ ) {
            //**
            Query query = Query.fromQL( "select * order by created" );

            final CandidateResults results = cursor == null ?  entityIndex.search( indexScope, SearchTypes.allTypes(), query , limit) : entityIndex.getNextPage(cursor, limit);

            assertTrue( results.hasCursor() );

            cursor = results.getCursor();

            assertEquals("Should be 16 bytes as hex", 32, cursor.length());


            assertEquals( 1, results.size() );


            assertEquals( results.get( 0 ).getId(), entities.get( i ) );
        }
    }


}



