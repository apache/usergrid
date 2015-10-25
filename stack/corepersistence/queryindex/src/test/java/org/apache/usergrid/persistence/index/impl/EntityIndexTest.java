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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.base.Optional;
import org.apache.usergrid.persistence.core.astyanax.CassandraFig;
import org.apache.usergrid.persistence.index.*;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.lang3.time.StopWatch;

import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.core.test.UseModules;
import org.apache.usergrid.persistence.core.util.Health;
import org.apache.usergrid.persistence.index.guice.TestIndexModule;
import org.apache.usergrid.persistence.index.utils.UUIDUtils;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.field.ArrayField;
import org.apache.usergrid.persistence.model.field.IntegerField;
import org.apache.usergrid.persistence.model.field.StringField;
import org.apache.usergrid.persistence.model.field.UUIDField;
import org.apache.usergrid.persistence.model.util.EntityUtils;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


@RunWith( EsRunner.class )
@UseModules( { TestIndexModule.class } )
public class EntityIndexTest extends BaseIT {

    private static final Logger log = LoggerFactory.getLogger(EntityIndexTest.class);

    @Inject
    public EntityIndexFactory eif;


    @Inject
    public IndexFig fig;

    @Inject
    public IndexProducer indexProducer;

    @Inject
    public CassandraFig cassandraFig;

    @Inject
    @Rule
    public ElasticSearchRule elasticSearchRule;
    private EntityIndex entityIndex;
    private SimpleId appId;

    @Before
    public void setup(){
        appId = new SimpleId(UUID.randomUUID(), "application" );

        IndexLocationStrategy strategy =  new TestIndexIdentifier(cassandraFig,fig,new ApplicationScopeImpl(appId));

        entityIndex = eif.createEntityIndex( strategy );
    }

    @Test
    public void testIndex() throws IOException, InterruptedException {



        final String entityType = "thing";
        IndexEdge searchEdge = new IndexEdgeImpl( appId, "things", SearchEdge.NodeType.SOURCE, 1 );
        final SearchTypes searchTypes = SearchTypes.fromTypes( entityType );

        insertJsonBlob( entityType, searchEdge, "/sample-large.json", 101, 0 );


        testQueries( searchEdge, searchTypes  );
    }


    /**
     * Tests that when types conflict, but should match queries they work
     */
    @Test
    public void testIndexVariations() throws IOException {
        Id appId = new SimpleId( "application" );
        final String entityType = "thing";
        IndexEdge indexEdge = new IndexEdgeImpl( appId, "things", SearchEdge.NodeType.SOURCE, 1 );
        final SearchTypes searchTypes = SearchTypes.fromTypes( entityType );
        EntityIndexBatch batch = entityIndex.createBatch();


        UUID uuid = UUID.randomUUID();
        Entity entity1 = new Entity( entityType );
        EntityUtils.setVersion(entity1, UUIDGenerator.newTimeUUID());
        entity1.setField(new UUIDField(IndexingUtils.ENTITY_ID_FIELDNAME, UUID.randomUUID()));
        entity1.setField( new StringField( "testfield", "test" ) );
        entity1.setField(new IntegerField("ordinal", 0));
        entity1.setField(new UUIDField("testuuid", uuid));


        batch.index( indexEdge, entity1 );
        indexProducer.put(batch.build()).subscribe();


        Entity entity2 = new Entity( entityType );
        EntityUtils.setVersion(entity2, UUIDGenerator.newTimeUUID());


        List<String> list = new ArrayList<>();
        list.add("test");
        entity2.setField(new ArrayField<>("testfield", list));
        entity2.setField(new IntegerField("ordinal", 1));


        batch.index( indexEdge, entity2 );
        indexProducer.put(batch.build()).subscribe();;

        entityIndex.refreshAsync().toBlocking().first();


        StopWatch timer = new StopWatch();
        timer.start();
        CandidateResults candidateResults =
            entityIndex.search( indexEdge, searchTypes, "select * where testfield = 'test' order by ordinal", 100, 0 );

        timer.stop();

        assertEquals(2, candidateResults.size());
        log.debug("Query time {}ms", timer.getTime());

        final CandidateResult candidate1 = candidateResults.get(0);

        //check the id and version
        assertEquals( entity1.getId(), candidate1.getId() );
        assertEquals(entity1.getVersion(), candidate1.getVersion());


        final CandidateResult candidate2 = candidateResults.get(1);

        //check the id and version
        assertEquals( entity2.getId(), candidate2.getId() );
        assertEquals( entity2.getVersion(), candidate2.getVersion() );

        //make sure we can query uuids out as strings and not wrapped
        candidateResults =
            entityIndex.search( indexEdge, searchTypes, "select * where testuuid = '"+uuid+"'", 100, 0 );
        assertEquals(entity1.getId(),candidateResults.get(0).getId());

        candidateResults =
            entityIndex.search( indexEdge, searchTypes, "select * where testuuid = "+uuid, 100, 0 );
        assertEquals(entity1.getId(),candidateResults.get(0).getId());
    }


    @Test
    public void testIndexThreads() throws IOException {


        long now = System.currentTimeMillis();
        final int threads = 20;
        final int size = 30;

        final String entityType = "thing";

        final CountDownLatch latch = new CountDownLatch( threads );
        final AtomicLong failTime = new AtomicLong( 0 );
        InputStream is = this.getClass().getResourceAsStream("/sample-large.json");
        ObjectMapper mapper = new ObjectMapper();
        final List<Object> sampleJson = mapper.readValue( is, new TypeReference<List<Object>>() {} );
        for ( int i = 0; i < threads; i++ ) {

            final IndexEdge indexEdge = new IndexEdgeImpl( appId, "things", SearchEdge.NodeType.SOURCE, i );

            Thread thread = new Thread( () -> {
                try {


                    EntityIndexBatch batch = entityIndex.createBatch();
                    insertJsonBlob( sampleJson, batch, entityType, indexEdge, size, 0 );
                    indexProducer.put(batch.build()).subscribe();;
                }
                catch ( Exception e ) {
                    synchronized ( failTime ) {
                        if ( failTime.get() == 0 ) {
                            failTime.set( System.currentTimeMillis() );
                        }
                    }
                    System.out.println( e.toString() );
                    fail( "threw exception" );
                }
                finally {
                    latch.countDown();
                }
            } );
            thread.start();
        }
        try {
            latch.await();
        }
        catch ( InterruptedException ie ) {
            throw new RuntimeException( ie );
        }
        assertTrue( "system must have failed at " + ( failTime.get() - now ), failTime.get() == 0 );
    }




    @Test
    public void testAddMultipleIndexes() throws IOException {


        final String entityType = "thing";
        IndexEdge searchEdge = new IndexEdgeImpl( appId, "things", SearchEdge.NodeType.SOURCE, 10 );
        final SearchTypes searchTypes = SearchTypes.fromTypes(entityType);

        insertJsonBlob( entityType, searchEdge, "/sample-large.json", 101, 0);


        testQueries(searchEdge, searchTypes);

        entityIndex.addIndex(UUID.randomUUID()+"_v2", 1, 0, "one");

        insertJsonBlob( entityType, searchEdge, "/sample-large.json", 101, 100);

        //Hilda Youn
        testQuery( searchEdge, searchTypes,  "name = 'Hilda Young'", 1 );

        testQuery( searchEdge, searchTypes,  "name = 'Lowe Kelley'", 1 );

        log.info("hi");
    }


    @Test
    public void testDeleteWithAlias() throws IOException {


        final String entityType = "thing";
        IndexEdge searchEdge = new IndexEdgeImpl( appId, "things", SearchEdge.NodeType.SOURCE, 1 );
        final SearchTypes searchTypes = SearchTypes.fromTypes( entityType );

        insertJsonBlob(  entityType, searchEdge, "/sample-large.json", 1, 0 );


        entityIndex.addIndex(UUID.randomUUID() + "v2", 1, 0, "one");
        entityIndex.refreshAsync().toBlocking().first();

        insertJsonBlob(  entityType, searchEdge, "/sample-large.json", 1, 1 );

        CandidateResults crs = testQuery( searchEdge, searchTypes, "name = 'Bowers Oneil'", 1 );

        EntityIndexBatch entityIndexBatch = entityIndex.createBatch();
        entityIndexBatch.deindex(searchEdge, crs.get(0));
        indexProducer.put(entityIndexBatch.build()).subscribe();
        entityIndex.refreshAsync().toBlocking().first();

        //Hilda Youn
        testQuery(searchEdge, searchTypes, "name = 'Bowers Oneil'", 0);
    }


    private void insertJsonBlob( String entityType, IndexEdge indexEdge,
                                 String filePath, final int max, final int startIndex ) throws IOException {
        InputStream is = this.getClass().getResourceAsStream( filePath );
        ObjectMapper mapper = new ObjectMapper();
        List<Object> sampleJson = mapper.readValue(is, new TypeReference<List<Object>>() {
        });
        EntityIndexBatch batch = entityIndex.createBatch();
        insertJsonBlob(sampleJson, batch, entityType, indexEdge, max, startIndex);
        indexProducer.put(batch.build()).subscribe();;
        EntityIndex.IndexRefreshCommandInfo info =  entityIndex.refreshAsync().toBlocking().first();
        long time = info.getExecutionTime();
        log.info("refresh took ms:" + time);
    }


    private void insertJsonBlob( List<Object> sampleJson, EntityIndexBatch batch, String entityType,
                                 IndexEdge indexEdge, final int max, final int startIndex ) throws IOException {
        int count = 0;
        StopWatch timer = new StopWatch();
        timer.start();


        if ( startIndex > 0 ) {
            for ( int i = 0; i < startIndex; i++ ) {
                sampleJson.remove( 0 );
            }
        }

        for ( Object o : sampleJson ) {

            Map<String, Object> item = ( Map<String, Object> ) o;

            Entity entity = new Entity( entityType );
            entity = EntityIndexMapUtils.fromMap( entity, item );
            EntityUtils.setVersion( entity, UUIDGenerator.newTimeUUID() );
            entity.setField( new UUIDField( IndexingUtils.ENTITY_ID_FIELDNAME, UUID.randomUUID() ) );
            batch.index( indexEdge, entity );

            if ( ++count > max ) {
                break;
            }
        }

        timer.stop();
        log.info( "Total time to index {} entries {}ms, average {}ms/entry",
            new Object[] { count, timer.getTime(), timer.getTime() / count } );
    }


    @Test
    public void testDeindex() {

       IndexEdge searchEdge = new IndexEdgeImpl( appId, "fastcars", SearchEdge.NodeType.SOURCE, 1 );

        Map entityMap = new HashMap() {{
            put( "name", "Ferrari 212 Inter" );
            put( "introduced", 1952 );
            put( "topspeed", 215 );
        }};


        Entity entity = EntityIndexMapUtils.fromMap( entityMap );
        EntityUtils.setId(entity, new SimpleId( "fastcar" ) );
        EntityUtils.setVersion(entity, UUIDGenerator.newTimeUUID() );
        entity.setField(new UUIDField(IndexingUtils.ENTITY_ID_FIELDNAME, UUID.randomUUID() ) );

        indexProducer.put(entityIndex.createBatch().index( searchEdge, entity ).build()).subscribe();
        entityIndex.refreshAsync().toBlocking().first();

        CandidateResults candidateResults = entityIndex
            .search( searchEdge, SearchTypes.fromTypes( entity.getId().getType() ), "name contains 'Ferrari*'", 10, 0 );
        assertEquals( 1, candidateResults.size() );

        EntityIndexBatch batch = entityIndex.createBatch();
        batch.deindex( searchEdge, entity );
        indexProducer.put(batch.build()).subscribe();;
        entityIndex.refreshAsync().toBlocking().first();

        candidateResults = entityIndex
            .search(searchEdge, SearchTypes.fromTypes( entity.getId().getType() ), "name contains 'Ferrari*'", 10, 0 );
        assertEquals(0, candidateResults.size());
    }


    /**
     * Tests that we aggregate results only before the halfway version point.
     */
    @Test
    public void testScollingDeindex() {

        int numberOfEntities = 1000;
        int versionToSearchFor = numberOfEntities / 2;

        IndexEdge searchEdge = new IndexEdgeImpl( appId, "mehCars", SearchEdge.NodeType.SOURCE, 1 );

        UUID entityUUID = UUID.randomUUID();
        Id entityId = new SimpleId( "mehCar" );

        Map entityMap = new HashMap() {{
            put( "name", "Toyota Corolla" );
            put( "introduced", 1966 );
            put( "topspeed", 111 );
        }};

        Entity[] entity = new Entity[numberOfEntities];
        for(int i = 0; i < numberOfEntities; i++) {
            entity[i] = EntityIndexMapUtils.fromMap( entityMap );
            EntityUtils.setId( entity[i], entityId );
            EntityUtils.setVersion( entity[i], UUIDGenerator.newTimeUUID() );
            entity[i].setField( new UUIDField( IndexingUtils.ENTITY_ID_FIELDNAME, entityUUID ) );

            //index the new entity. This is where the loop will be set to create like 100 entities.
            indexProducer.put(entityIndex.createBatch().index( searchEdge, entity[i]  ).build()).subscribe();

        }
        entityIndex.refreshAsync().toBlocking().first();

        CandidateResults candidateResults = entityIndex
            .getAllEntityVersionsBeforeMarkedVersion( entity[versionToSearchFor].getId(),
                entity[versionToSearchFor].getVersion() );
        assertEquals( 501, candidateResults.size() );
    }



    private CandidateResults testQuery( final SearchEdge scope, final SearchTypes searchTypes,
                                      final String queryString,
                                        final int num ) {

        StopWatch timer = new StopWatch();
        timer.start();
        CandidateResults candidateResults  = entityIndex.search( scope, searchTypes, queryString, num == 0 ?  1 : num  , 0 );

        timer.stop();

        assertEquals(num, candidateResults.size());
        log.debug("Query time {}ms", timer.getTime());
        return candidateResults;
    }


    private void testQueries( final SearchEdge scope, SearchTypes searchTypes) {

        testQuery( scope, searchTypes, "name = 'Morgan Pierce'", 1 );

        testQuery( scope, searchTypes, "name = 'morgan pierce'", 1 );

        testQuery( scope, searchTypes, "name = 'Morgan'", 0 );

        testQuery( scope, searchTypes, "name contains 'Morgan'", 1 );

        testQuery( scope, searchTypes, "company > 'GeoLogix'", 64 );

        testQuery( scope, searchTypes, "gender = 'female'", 45 );

        testQuery( scope, searchTypes, "name = 'Minerva Harrell' and age > 39", 1 );

        testQuery( scope, searchTypes, "name = 'Minerva Harrell' and age > 39 and age < 41", 1 );

        testQuery( scope, searchTypes, "name = 'Minerva Harrell' and age > 40", 0 );

        testQuery( scope, searchTypes, "name = 'Minerva Harrell' and age >= 40", 1 );

        testQuery( scope, searchTypes, "name = 'Minerva Harrell' and age <= 40", 1 );

        testQuery( scope, searchTypes, "name = 'Morgan* '", 1 );

        testQuery( scope, searchTypes, "name = 'Morgan*'", 1 );


        // test a couple of array sub-property queries

        int totalUsers = 102;

        // nobody has a friend named Jack the Ripper
        testQuery( scope, searchTypes, "friends.name = 'Jack the Ripper'", 0 );

        // everybody doesn't have a friend named Jack the Ripper
        testQuery( scope, searchTypes, "not (friends.name = 'Jack the Ripper')", totalUsers );

        // one person has a friend named Shari Hahn
        testQuery( scope, searchTypes, "friends.name = 'Wendy Moody'", 1 );

        // everybody but 1 doesn't have a friend named Shari Hahh
        testQuery( scope, searchTypes, "not (friends.name = 'Shari Hahn')", totalUsers - 1 );
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


        Id ownerId = new SimpleId( "owner" );



        IndexEdge indexSCope = new IndexEdgeImpl( ownerId, "user", SearchEdge.NodeType.SOURCE, 10 );




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

        batch.index( indexSCope, user );
        indexProducer.put(batch.build()).subscribe();;
        entityIndex.refreshAsync().toBlocking().first();

        final String query = "where username = 'edanuff'";

        CandidateResults r = entityIndex.search( indexSCope, SearchTypes.fromTypes( "edanuff" ), query, 10, 0);
        assertEquals( user.getId(), r.get( 0 ).getId());

        batch.deindex( indexSCope, user.getId(), user.getVersion() );
        indexProducer.put(batch.build()).subscribe();;
        entityIndex.refreshAsync().toBlocking().first();

        // EntityRef


        r = entityIndex.search( indexSCope, SearchTypes.fromTypes( "edanuff" ), query, 10, 0 );

        assertFalse( r.iterator().hasNext() );
    }


    @Test
    public void multiValuedTypes() {

        Id appId = new SimpleId( "entityindextest" );
        Id ownerId = new SimpleId( "multivaluedtype" );


        IndexEdge indexScope = new IndexEdgeImpl( ownerId, "user", SearchEdge.NodeType.SOURCE, 10 );



        entityIndex.createBatch();

        // Bill has favorites as string, age as string and retirement goal as number
        Map billMap = new HashMap() {{
            put( "username", "bill" );
            put( "email", "bill@example.com" );
            put( "age", "thirtysomething" );
            put( "favorites", "scallops, croquet, wine" );
            put( "retirementGoal", 100000 );
        }};
        Entity bill = EntityIndexMapUtils.fromMap( billMap );
        EntityUtils.setId( bill, new SimpleId( UUIDGenerator.newTimeUUID(), "user" ) );
        EntityUtils.setVersion( bill, UUIDGenerator.newTimeUUID() );

        EntityIndexBatch batch = entityIndex.createBatch();

        batch.index( indexScope, bill );

        // Fred has age as int, favorites as object and retirement goal as object
        Map fredMap = new HashMap() {{
            put( "username", "fred" );
            put( "email", "fred@example.com" );
            put( "age", 41 );
            put( "favorites", new HashMap<String, Object>() {{
                put( "food", "cheezewiz" );
                put( "sport", "nascar" );
                put( "beer", "budwizer" );
            }} );
            put( "retirementGoal", new HashMap<String, Object>() {{
                put( "car", "Firebird" );
                put( "home", "Mobile" );
            }} );
        }};
        Entity fred = EntityIndexMapUtils.fromMap( fredMap );
        EntityUtils.setId( fred, new SimpleId( UUIDGenerator.newTimeUUID(), "user" ) );
        EntityUtils.setVersion( fred, UUIDGenerator.newTimeUUID() );
        batch.index( indexScope, fred);

        indexProducer.put(batch.build()).subscribe();;
        entityIndex.refreshAsync().toBlocking().first();

        final SearchTypes searchTypes = SearchTypes.fromTypes( "user" );


        CandidateResults r = entityIndex.search( indexScope, searchTypes, "where username = 'bill'", 10, 0);
        assertEquals( bill.getId(), r.get( 0 ).getId() );

        r = entityIndex.search( indexScope, searchTypes, "where username = 'fred'", 10, 0);
        assertEquals(fred.getId(), r.get(0).getId());

        r = entityIndex.search( indexScope, searchTypes, "where age = 41", 10, 0);
        assertEquals(fred.getId(), r.get(0).getId());

        r = entityIndex.search( indexScope, searchTypes, "where age = 'thirtysomething'", 10, 0);
        assertEquals(bill.getId(), r.get(0).getId());
    }


    @Test
    public void healthTest() {
        Id appId = new SimpleId( "entityindextest" );
        assertNotEquals( "cluster should be ok", Health.RED, entityIndex.getClusterHealth() );
        assertEquals( "index should be ready", Health.GREEN, entityIndex.getIndexHealth() );
        entityIndex.refreshAsync().toBlocking().first();
        assertNotEquals( "cluster should be fine", Health.RED, entityIndex.getIndexHealth() );
        assertNotEquals( "cluster should be ready now", Health.RED, entityIndex.getClusterHealth() );
    }


    @Test
    public void testCursorFormat() throws Exception {

        String myType = UUID.randomUUID().toString();
        Id ownerId = new SimpleId( UUID.randomUUID(),"owner" );



        IndexEdge indexEdge = new IndexEdgeImpl( ownerId, "users", SearchEdge.NodeType.SOURCE, 10 );


        final EntityIndexBatch batch = entityIndex.createBatch();


        final int size = 100;

        final List<Id> entityIds = new ArrayList<>( size );


        for ( int i = 0; i < size; i++ ) {

            final String middleName = "middleName" + UUIDUtils.newTimeUUID();

            final int ordinal = i;

            Map entityMap = new HashMap() {{
                put( "username", "edanuff" );
                put( "email", "ed@anuff.com" );
                put( "middlename", middleName );
                put( "ordinal", ordinal );
                put( "mytype", myType);
            }};

            final Id userId = new SimpleId( "user" );

            Entity user = EntityIndexMapUtils.fromMap( entityMap );
            EntityUtils.setId( user, userId );
            EntityUtils.setVersion( user, UUIDGenerator.newTimeUUID() );

            entityIds.add(userId );


            batch.index( indexEdge, user );
        }


        indexProducer.put(batch.build()).subscribe();;

        entityIndex.refreshAsync().toBlocking().first();


        final int limit = 5;


        final int expectedPages = size / limit;


        Optional<Integer> offset = Optional.absent();
        UUID lastId = null;
        final String query = "select * where mytype='"+myType+"' order by ordinal asc";
        int i ;
        for ( i=0; i < expectedPages; i++ ) {
            final CandidateResults results = !offset.isPresent()
                ? entityIndex.search( indexEdge, SearchTypes.allTypes(), query, limit, 0 )
                : entityIndex.search(indexEdge, SearchTypes.allTypes(), query, limit, i * limit);

            assertEquals(limit, results.size());

            int ordinal = 0;//i == 0 ? 0 : 1;
            assertNotEquals("Scroll matches last item from previous page",lastId, results.get(ordinal).getId().getUuid());
            lastId = results.get(limit -1).getId().getUuid();
            offset = results.getOffset();
            assertEquals("Failed on page "+i, results.get( ordinal ).getId(), entityIds.get( i*limit ) );
        }

        //get our next page, we shouldn't get a cursor
        final CandidateResults results = entityIndex.search(indexEdge, SearchTypes.allTypes(), query, limit, i * limit);

        assertEquals( 0, results.size() );
        assertFalse(results.hasOffset());
    }


    @Test
    public void queryByUUID() throws Throwable {

        Id appId = new SimpleId( "application" );
        Id ownerId = new SimpleId( "owner" );



        IndexEdge indexSCope = new IndexEdgeImpl( ownerId, "user", SearchEdge.NodeType.SOURCE, 10 );




        final UUID searchUUID = UUIDGenerator.newTimeUUID();

        Map entityMap = new HashMap() {{
            put( "searchUUID", searchUUID );
        }};

        Entity user = EntityIndexMapUtils.fromMap( entityMap );

        final Id entityId = new SimpleId( "entitytype" );

        EntityUtils.setId( user, entityId );
        EntityUtils.setVersion( user, UUIDGenerator.newTimeUUID() );


        EntityIndexBatch batch = entityIndex.createBatch();

        batch.index( indexSCope, user );
        indexProducer.put(batch.build()).subscribe();;
        entityIndex.refreshAsync().toBlocking().first();

        final String query = "where searchUUID = " + searchUUID;

        final CandidateResults r =
            entityIndex.search( indexSCope, SearchTypes.fromTypes(entityId.getType()), query, 10, 0);
        assertEquals(user.getId(), r.get(0).getId());
    }


    @Test
    public void queryByStringWildCardSpaces() throws Throwable {

        Id appId = new SimpleId( "application" );
        Id ownerId = new SimpleId( "owner" );



        IndexEdge indexSCope = new IndexEdgeImpl( ownerId, "user", SearchEdge.NodeType.SOURCE, 10 );




        Map entityMap = new HashMap() {{
            put( "string", "I am a search string" );
        }};

        Entity user = EntityIndexMapUtils.fromMap( entityMap );

        final Id entityId = new SimpleId( "entitytype" );

        EntityUtils.setId( user, entityId );
        EntityUtils.setVersion( user, UUIDGenerator.newTimeUUID() );


        EntityIndexBatch batch = entityIndex.createBatch();

        batch.index(indexSCope, user);
        indexProducer.put(batch.build()).subscribe();;
        entityIndex.refreshAsync().toBlocking().first();

        final String query = "where string = 'I am*'";

        final CandidateResults r =
            entityIndex.search( indexSCope, SearchTypes.fromTypes( entityId.getType() ), query, 10, 0);

        assertEquals(user.getId(), r.get(0).getId());

        //shouldn't match
        final String queryNoWildCard = "where string = 'I am'";

        final CandidateResults noWildCardResults =
            entityIndex.search( indexSCope, SearchTypes.fromTypes( entityId.getType() ), queryNoWildCard, 10, 0 );

        assertEquals( 0, noWildCardResults.size() );
    }


    @Test
    public void sortyByString() throws Throwable {

        Id appId = new SimpleId( "application" );
        Id ownerId = new SimpleId( "owner" );



        IndexEdge indexSCope = new IndexEdgeImpl( ownerId, "user", SearchEdge.NodeType.SOURCE, 10 );




        /**
         * Ensures sort ordering is correct when more than 1 token is present.  Should order by the unanalyzed field,
         * not the analyzed field
         */

        final Entity first = new Entity( "search" );

        first.setField( new StringField( "string", "alpha long string" ) );


        EntityUtils.setVersion( first, UUIDGenerator.newTimeUUID() );


        final Entity second = new Entity( "search" );

        second.setField(new StringField("string", "bravo long string"));


        EntityUtils.setVersion( second, UUIDGenerator.newTimeUUID() );


        EntityIndexBatch batch = entityIndex.createBatch();
        batch.index(indexSCope, first );
        batch.index( indexSCope, second );
        indexProducer.put(batch.build()).subscribe();;
        entityIndex.refreshAsync().toBlocking().first();


        final String ascQuery = "order by string";

        final CandidateResults ascResults =
            entityIndex.search(indexSCope, SearchTypes.fromTypes( first.getId().getType() ), ascQuery, 10 , 0);


        assertEquals( first.getId(), ascResults.get( 0).getId() );
        assertEquals( second.getId(), ascResults.get( 1 ).getId() );


        //search in reversed
        final String descQuery = "order by string desc";

        final CandidateResults descResults =
            entityIndex.search(indexSCope, SearchTypes.fromTypes( first.getId().getType() ), descQuery, 10 , 0);


        assertEquals( second.getId(), descResults.get( 0).getId() );
        assertEquals( first.getId(), descResults.get( 1 ).getId() );
    }


    @Test
    public void unionString() throws Throwable {

        Id appId = new SimpleId( "application" );
        Id ownerId = new SimpleId( "owner" );






        final Entity first = new Entity( "search" );

        first.setField( new StringField( "string", "alpha long string" ) );


        EntityUtils.setVersion( first, UUIDGenerator.newTimeUUID() );


        final Entity second = new Entity( "search" );

        second.setField( new StringField( "string", "bravo long string" ) );


        EntityUtils.setVersion( second, UUIDGenerator.newTimeUUID() );


        EntityIndexBatch batch = entityIndex.createBatch();


        //get ordering, so 2 is before 1 when both match
        IndexEdge indexScope1 = new IndexEdgeImpl( ownerId, "searches", SearchEdge.NodeType.SOURCE, 10 );
        batch.index(indexScope1, first);


        IndexEdge indexScope2 = new IndexEdgeImpl( ownerId, "searches", SearchEdge.NodeType.SOURCE, 11 );
        batch.index(indexScope2, second);


        indexProducer.put(batch.build()).subscribe();;
        entityIndex.refreshAsync().toBlocking().first();


        final String singleMatchQuery = "string contains 'alpha' OR string contains 'foo'";

        final CandidateResults singleResults =
            entityIndex.search(indexScope1, SearchTypes.fromTypes( first.getId().getType() ), singleMatchQuery, 10, 0 );


        assertEquals(1, singleResults.size());
        assertEquals(first.getId(), singleResults.get(0).getId());


        //search in reversed
        final String bothKeywordsMatch = "string contains 'alpha' OR string contains 'bravo'";

        final CandidateResults singleKeywordUnion =
            entityIndex.search(indexScope1, SearchTypes.fromTypes( first.getId().getType() ), bothKeywordsMatch, 10 , 0);


        assertEquals( 2, singleKeywordUnion.size() );
        assertEquals( second.getId(), singleKeywordUnion.get( 0).getId() );
        assertEquals( first.getId(), singleKeywordUnion.get( 1 ).getId() );


        final String twoKeywordMatches = "string contains 'alpha' OR string contains 'long'";

        final CandidateResults towMatchResults =
            entityIndex.search(indexScope1, SearchTypes.fromTypes( first.getId().getType() ), twoKeywordMatches, 10, 0 );


        assertEquals( 2, towMatchResults.size() );
        assertEquals(second.getId(), towMatchResults.get( 0).getId() );
        assertEquals(first.getId(), towMatchResults.get( 1 ).getId() );
    }


    /**
     * Tests that when NOT is the only query term, it functions correctly
     */
    @Test
    public void notRootOperandFilter() throws Throwable {

        Id appId = new SimpleId( "application" );
        Id ownerId = new SimpleId( "owner" );







        final Entity first = new Entity( "search" );

        first.setField( new IntegerField( "int", 1 ) );


        EntityUtils.setVersion( first, UUIDGenerator.newTimeUUID() );


        final Entity second = new Entity( "search" );

        second.setField( new IntegerField( "int", 2 ) );


        EntityUtils.setVersion( second, UUIDGenerator.newTimeUUID() );


        EntityIndexBatch batch = entityIndex.createBatch();


        //get ordering, so 2 is before 1 when both match
        IndexEdge indexScope1 = new IndexEdgeImpl( ownerId, "searches", SearchEdge.NodeType.SOURCE, 10 );
        batch.index( indexScope1, first );


        IndexEdge indexScope2 = new IndexEdgeImpl( ownerId, "searches", SearchEdge.NodeType.SOURCE, 11 );
        batch.index( indexScope2, second);


        indexProducer.put(batch.build()).subscribe();;
        entityIndex.refreshAsync().toBlocking().first();


        final String notFirst = "NOT int = 1";

        final CandidateResults notFirstResults =
            entityIndex.search(indexScope1, SearchTypes.fromTypes( first.getId().getType() ), notFirst, 10, 0 );


        assertEquals( 1, notFirstResults.size() );
        assertEquals(second.getId(), notFirstResults.get( 0 ).getId() );


        //search in reversed
        final String notSecond = "NOT int = 2";

        final CandidateResults notSecondUnion =
            entityIndex.search(indexScope1, SearchTypes.fromTypes( first.getId().getType() ), notSecond, 10, 0 );


        assertEquals( 1, notSecondUnion.size() );
        assertEquals( first.getId(), notSecondUnion.get( 0 ).getId() );


        final String notBothReturn = "NOT int = 3";

        final CandidateResults notBothReturnResults =
            entityIndex.search(indexScope1, SearchTypes.fromTypes( first.getId().getType() ), notBothReturn, 10, 0 );


        assertEquals( 2, notBothReturnResults.size() );
        assertEquals( second.getId(), notBothReturnResults.get( 0).getId() );
        assertEquals( first.getId(), notBothReturnResults.get( 1 ).getId() );


        final String notFilterBoth = "(NOT int = 1) AND (NOT int = 2) ";

        final CandidateResults filterBoth =
            entityIndex.search(indexScope1, SearchTypes.fromTypes( first.getId().getType() ), notFilterBoth, 10, 0 );


        assertEquals( 0, filterBoth.size() );

        final String noMatchesAnd = "(NOT int = 3) AND (NOT int = 4)";

        final CandidateResults noMatchesAndResults =
            entityIndex.search(indexScope1, SearchTypes.fromTypes( first.getId().getType() ), noMatchesAnd, 10, 0 );


        assertEquals( 2, noMatchesAndResults.size() );
        assertEquals( second.getId(), noMatchesAndResults.get( 0).getId() );
        assertEquals( first.getId(), noMatchesAndResults.get( 1 ).getId() );


        final String noMatchesOr = "(NOT int = 3) AND (NOT int = 4)";

        final CandidateResults noMatchesOrResults =
            entityIndex.search(indexScope1, SearchTypes.fromTypes( first.getId().getType() ), noMatchesOr, 10, 0 );


        assertEquals( 2, noMatchesOrResults.size() );
        assertEquals( second.getId(), noMatchesOrResults.get( 0).getId() );
        assertEquals( first.getId(), noMatchesOrResults.get( 1 ).getId() );
    }


    /**
     * Tests that when NOT is the only query term, it functions correctly
     */
    @Test
    public void notRootOperandQuery() throws Throwable {

        Id appId = new SimpleId( "application" );
        Id ownerId = new SimpleId( "owner" );







        final Entity first = new Entity( "search" );

        first.setField( new StringField( "string", "I ate a sammich" ) );


        EntityUtils.setVersion( first, UUIDGenerator.newTimeUUID() );


        final Entity second = new Entity( "search" );

        second.setField( new StringField( "string", "I drank a beer" ) );


        EntityUtils.setVersion( second, UUIDGenerator.newTimeUUID() );


        EntityIndexBatch batch = entityIndex.createBatch();


        //get ordering, so 2 is before 1 when both match
        IndexEdge indexScope1 = new IndexEdgeImpl( ownerId, "searches", SearchEdge.NodeType.SOURCE, 10 );
        batch.index( indexScope1, first );


        IndexEdge indexScope2 = new IndexEdgeImpl( ownerId, "searches", SearchEdge.NodeType.SOURCE, 11 );
        batch.index( indexScope2, second);


        indexProducer.put(batch.build()).subscribe();;
        entityIndex.refreshAsync().toBlocking().first();


        final String notFirst = "NOT string = 'I ate a sammich'";

        final CandidateResults notFirstResults =
            entityIndex.search(indexScope1, SearchTypes.fromTypes( first.getId().getType() ), notFirst, 10, 0 );


        assertEquals( 1, notFirstResults.size() );
        assertEquals(second.getId(), notFirstResults.get( 0 ).getId() );


        final String notFirstWildCard = "NOT string = 'I ate*'";

        final CandidateResults notFirstWildCardResults =
            entityIndex.search(indexScope1, SearchTypes.fromTypes( first.getId().getType() ), notFirstWildCard, 10, 0 );


        assertEquals( 1, notFirstWildCardResults.size() );
        assertEquals(second.getId(), notFirstWildCardResults.get( 0 ).getId() );


        final String notFirstContains = "NOT string contains 'sammich'";

        final CandidateResults notFirstContainsResults =
            entityIndex.search(indexScope1, SearchTypes.fromTypes( first.getId().getType() ), notFirstContains, 10 , 0);


        assertEquals( 1, notFirstContainsResults.size() );
        assertEquals(second.getId(), notFirstContainsResults.get( 0 ).getId() );


        //search in reversed
        final String notSecond = "NOT string = 'I drank a beer'";

        final CandidateResults notSecondUnion =
            entityIndex.search(indexScope1, SearchTypes.fromTypes( first.getId().getType() ), notSecond, 10, 0 );


        assertEquals( 1, notSecondUnion.size() );
        assertEquals( first.getId(), notSecondUnion.get( 0 ).getId() );


        final String notSecondWildcard = "NOT string = 'I drank*'";

        final CandidateResults notSecondWildcardUnion =
            entityIndex.search(indexScope1, SearchTypes.fromTypes( first.getId().getType() ), notSecondWildcard, 10, 0 );


        assertEquals( 1, notSecondWildcardUnion.size() );
        assertEquals( first.getId(), notSecondWildcardUnion.get( 0 ).getId() );


        final String notSecondContains = "NOT string contains 'beer'";

        final CandidateResults notSecondContainsUnion =
            entityIndex.search(indexScope1, SearchTypes.fromTypes( first.getId().getType() ), notSecondContains, 10, 0 );


        assertEquals( 1, notSecondContainsUnion.size() );
        assertEquals( first.getId(), notSecondContainsUnion.get( 0 ).getId() );


        final String notBothReturn = "NOT string = 'I'm a foodie'";

        final CandidateResults notBothReturnResults =
            entityIndex.search(indexScope1, SearchTypes.fromTypes( first.getId().getType() ), notBothReturn, 10, 0 );


        assertEquals( 2, notBothReturnResults.size() );
        assertEquals( second.getId(), notBothReturnResults.get( 0).getId() );
        assertEquals( first.getId(), notBothReturnResults.get( 1 ).getId() );


        final String notFilterBoth = "(NOT string = 'I ate a sammich') AND (NOT string = 'I drank a beer') ";

        final CandidateResults filterBoth =
            entityIndex.search(indexScope1, SearchTypes.fromTypes( first.getId().getType() ), notFilterBoth, 10, 0 );


        assertEquals( 0, filterBoth.size() );

        final String noMatchesAnd = "(NOT string = 'I ate*') AND (NOT string = 'I drank*')";

        final CandidateResults noMatchesAndResults =
            entityIndex.search(indexScope1, SearchTypes.fromTypes( first.getId().getType() ), noMatchesAnd, 10, 0 );


        assertEquals( 0, noMatchesAndResults.size() );

        final String noMatchesContainsAnd = "(NOT string contains 'ate') AND (NOT string contains 'drank')";

        final CandidateResults noMatchesContainsAndResults = entityIndex
            .search(indexScope1, SearchTypes.fromTypes( first.getId().getType() ), noMatchesContainsAnd, 10, 0 );


        assertEquals( 0, noMatchesContainsAndResults.size() );


        final String noMatchesOr = "(NOT string = 'I ate*') AND (NOT string = 'I drank*')";

        final CandidateResults noMatchesOrResults =
            entityIndex.search(indexScope1, SearchTypes.fromTypes( first.getId().getType() ), noMatchesOr, 10, 0 );


        assertEquals( 0, noMatchesOrResults.size() );

        final String noMatchesContainsOr = "(NOT string contains 'ate') AND (NOT string contains 'drank')";

        final CandidateResults noMatchesContainsOrResults = entityIndex
            .search(indexScope1, SearchTypes.fromTypes( first.getId().getType() ), noMatchesContainsOr, 10, 0 );


        assertEquals( 0, noMatchesContainsOrResults.size() );
    }


    @Test
    public void testSizeByEdge(){
        final String type = UUID.randomUUID().toString();

        Id ownerId = new SimpleId( "owner" );


        final Entity first = new Entity( type );

        first.setField( new StringField( "string", "I ate a sammich" ) );
        first.setSize(100);

        EntityUtils.setVersion( first, UUIDGenerator.newTimeUUID() );


        final Entity second = new Entity( type );
        second.setSize(100);

        second.setField( new StringField( "string", "I drank a beer" ) );


        EntityUtils.setVersion( second, UUIDGenerator.newTimeUUID() );


        EntityIndexBatch batch = entityIndex.createBatch();


        //get ordering, so 2 is before 1 when both match
        IndexEdge indexScope1 = new IndexEdgeImpl( ownerId,type , SearchEdge.NodeType.SOURCE, 10 );
        batch.index( indexScope1, first );


        IndexEdge indexScope2 = new IndexEdgeImpl( ownerId, type+"er", SearchEdge.NodeType.SOURCE, 11 );
        batch.index( indexScope2, second);


        indexProducer.put(batch.build()).subscribe();;
        entityIndex.refreshAsync().toBlocking().first();
        long size = entityIndex.getEntitySize(new SearchEdgeImpl(ownerId,type, SearchEdge.NodeType.SOURCE));
        assertTrue( size == 100 );

    }


}



