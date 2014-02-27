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

package org.apache.usergrid.persistence;

import com.google.inject.Inject;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.collection.cassandra.CassandraRule;
import org.apache.usergrid.persistence.collection.guice.MigrationManagerRule;
import org.apache.usergrid.persistence.collection.impl.CollectionScopeImpl;
import org.apache.usergrid.persistence.index.EntityCollectionIndex;
import org.apache.usergrid.persistence.index.EntityCollectionIndexFactory;
import org.apache.usergrid.persistence.index.guice.IndexTestModule;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.query.Query;
import org.apache.usergrid.persistence.query.Results;
import org.apache.usergrid.persistence.utils.JsonUtils;
import org.apache.usergrid.test.CoreApplication;
import org.apache.usergrid.test.CoreITSetup;
import org.apache.usergrid.test.CoreITSetupImpl;
import org.apache.usergrid.test.EntityManagerFacade;
import org.jukito.JukitoRunner;
import org.jukito.UseModules;
import static org.junit.Assert.assertEquals;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@RunWith(JukitoRunner.class)
@UseModules({ IndexTestModule.class })
public class IndexIT {
    
    private static final Logger LOG = LoggerFactory.getLogger( IndexIT.class );

    @ClassRule
    public static CassandraRule cass = new CassandraRule();

    @Inject
    @Rule
    public MigrationManagerRule migrationManagerRule;
    
    @ClassRule
    public static CoreITSetup setup = new CoreITSetupImpl();

    @Rule
    public CoreApplication app = new CoreApplication( setup );

    @Inject
    public EntityCollectionManagerFactory collectionManagerFactory;
    
    @Inject
    public EntityCollectionIndexFactory collectionIndexFactory;

    @Inject 
    public EntityCollectionIndex index;


    public static final String[] alphabet = {
            "Alpha", "Bravo", "Charlie", "Delta", "Echo", "Foxtrot", "Golf", "Hotel", "India", "Juliet", "Kilo", "Lima",
            "Mike", "November", "Oscar", "Papa", "Quebec", "Romeo", "Sierra", "Tango", "Uniform", "Victor", "Whiskey",
            "X-ray", "Yankee", "Zulu"
    };

    @Ignore // TODO: enable when Cursor support implemented
    @Test
    public void testCollectionOrdering() throws Exception {
        LOG.info( "testCollectionOrdering" );

        Id appId = new SimpleId("application");
        Id orgId = new SimpleId("organization");
        CollectionScope scope = new CollectionScopeImpl( appId, orgId, "items" );

        EntityManagerFacade em = new EntityManagerFacade( 
            collectionManagerFactory, collectionIndexFactory, scope);

        for ( int i = alphabet.length - 1; i >= 0; i-- ) {
            String name = alphabet[i];
            Map<String, Object> properties = new LinkedHashMap<String, Object>();
            properties.put( "name", name );

            em.create( "items", properties );
        }

        int i = 0;
        
        Query query = Query.fromQL( "order by name" );
        Results r = em.searchCollection( em.getApplicationRef(), "items", query );
        for ( Entity entity : r.getEntities() ) {
            assertEquals( alphabet[i], entity.getField( "name" ).getValue() );
            i++;
        }

        query = Query.fromQL( "order by name" ).withCursor( r.getCursor() );
        r = em.searchCollection( em.getApplicationRef(), "items", query );
        for ( Entity entity : r.getEntities() ) {
            assertEquals( alphabet[i], entity.getField( "name" ).getValue() );
            i++;
        }

        query = Query.fromQL( "order by name" ).withCursor( r.getCursor() );
        r = em.searchCollection( em.getApplicationRef(), "items", query );
        for ( Entity entity : r.getEntities() ) {
            assertEquals( alphabet[i], entity.getField( "name" ).getValue() );
            i++;
        }

        assertEquals( alphabet.length, i );

        i = alphabet.length;

        query = Query.fromQL( "order by name desc" );
        r = em.searchCollection( em.getApplicationRef(), "items", query );
        for ( Entity entity : r.getEntities() ) {
            i--;
            assertEquals( alphabet[i], entity.getField( "name" ).getValue() );
        }

        query = Query.fromQL( "order by name desc" ).withCursor( r.getCursor() );
        r = em.searchCollection( em.getApplicationRef(), "items", query );
        // LOG.info(JsonUtils.mapToFormattedJsonString(r.getEntities()));
        for ( Entity entity : r.getEntities() ) {
            i--;
            assertEquals( alphabet[i], entity.getField( "name" ).getValue() );
        }

        query = Query.fromQL( "order by name desc" ).withCursor( r.getCursor() );
        r = em.searchCollection( em.getApplicationRef(), "items", query );
        for ( Entity entity : r.getEntities() ) {
            i--;
            assertEquals( alphabet[i], entity.getField( "name" ).getValue() );
        }

        assertEquals( 0, i );
    }


    @Test
    public void testCollectionFilters() throws Exception {
        LOG.info( "testCollectionFilters" );

        Id appId = new SimpleId("application");
        Id orgId = new SimpleId("organization");
        CollectionScope scope = new CollectionScopeImpl( appId, orgId, "items" );
        
        EntityManagerFacade em = new EntityManagerFacade( 
            collectionManagerFactory, collectionIndexFactory, scope);

        for ( int i = alphabet.length - 1; i >= 0; i-- ) {
            String name = alphabet[i];
            Map<String, Object> properties = new LinkedHashMap<String, Object>();
            properties.put( "name", name );
            em.create( "items", properties );
        }

        Query query = Query.fromQL( "name < 'Delta' order by name" );
        Results r = em.searchCollection( em.getApplicationRef(), "items", query );
        LOG.info( JsonUtils.mapToFormattedJsonString( r.getEntities() ) );
        int i = 0;
        for ( Entity entity : r.getEntities() ) {
            assertEquals( alphabet[i], entity.getField( "name" ).getValue() );
            i++;
        }
        assertEquals( 3, i );

        query = Query.fromQL( "name <= 'delta' order by name" );
        r = em.searchCollection( em.getApplicationRef(), "items", query );
        LOG.info( JsonUtils.mapToFormattedJsonString( r.getEntities() ) );
        i = 0;
        for ( Entity entity : r.getEntities() ) {
            assertEquals( alphabet[i], entity.getField( "name" ).getValue() );
            i++;
        }
        assertEquals( 4, i );

        query = Query.fromQL( "name <= 'foxtrot' and name > 'bravo' order by name" );
        r = em.searchCollection( em.getApplicationRef(), "items", query );
        LOG.info( JsonUtils.mapToFormattedJsonString( r.getEntities() ) );
        i = 2;
        for ( Entity entity : r.getEntities() ) {
            assertEquals( alphabet[i], entity.getField( "name" ).getValue() );
            i++;
        }
        assertEquals( 6, i );

        query = Query.fromQL( "name < 'foxtrot' and name > 'bravo' order by name" );
        r = em.searchCollection( em.getApplicationRef(), "items", query );
        LOG.info( JsonUtils.mapToFormattedJsonString( r.getEntities() ) );
        i = 2;
        for ( Entity entity : r.getEntities() ) {
            assertEquals( alphabet[i], entity.getField( "name" ).getValue() );
            i++;
        }
        assertEquals( 5, i );

        query = Query.fromQL( "name < 'foxtrot' and name >= 'bravo' order by name" );
        r = em.searchCollection( em.getApplicationRef(), "items", query );
        LOG.info( JsonUtils.mapToFormattedJsonString( r.getEntities() ) );
        i = 1;
        for ( Entity entity : r.getEntities() ) {
            assertEquals( alphabet[i], entity.getField( "name" ).getValue() );
            i++;
        }
        assertEquals( 5, i );

        query = Query.fromQL( "name <= 'foxtrot' and name >= 'bravo' order by name" );
        r = em.searchCollection( em.getApplicationRef(), "items", query );
        LOG.info( JsonUtils.mapToFormattedJsonString( r.getEntities() ) );
        i = 1;
        for ( Entity entity : r.getEntities() ) {
            assertEquals( alphabet[i], entity.getField( "name" ).getValue() );
            i++;
        }
        assertEquals( 6, i );

        query = Query.fromQL( "name <= 'foxtrot' and name >= 'bravo' order by name desc" );
        r = em.searchCollection( em.getApplicationRef(), "items", query );
        LOG.info( JsonUtils.mapToFormattedJsonString( r.getEntities() ) );
        i = 6;
        for ( Entity entity : r.getEntities() ) {
            i--;
            assertEquals( alphabet[i], entity.getField( "name" ).getValue() );
        }
        assertEquals( 1, i );

        query = Query.fromQL( "name < 'foxtrot' and name > 'bravo' order by name desc" );
        r = em.searchCollection( em.getApplicationRef(), "items", query );
        LOG.info( JsonUtils.mapToFormattedJsonString( r.getEntities() ) );
        i = 5;
        for ( Entity entity : r.getEntities() ) {
            i--;
            assertEquals( alphabet[i], entity.getField( "name" ).getValue() );
        }
        assertEquals( 2, i );

        query = Query.fromQL( "name < 'foxtrot' and name >= 'bravo' order by name desc" );
        r = em.searchCollection( em.getApplicationRef(), "items", query );
        LOG.info( JsonUtils.mapToFormattedJsonString( r.getEntities() ) );
        i = 5;
        for ( Entity entity : r.getEntities() ) {
            i--;
            assertEquals( alphabet[i], entity.getField( "name" ).getValue() );
        }
        assertEquals( 1, i );

        query = Query.fromQL( "name = 'foxtrot'" );
        r = em.searchCollection( em.getApplicationRef(), "items", query );
        LOG.info( JsonUtils.mapToFormattedJsonString( r.getEntities() ) );
        assertEquals( 1, r.size() );

        long created = r.getEntity().getVersion().timestamp();
        Id entityId = r.getEntity().getId();

        query = Query.fromQL( "created = " + created );
        r = em.searchCollection( em.getApplicationRef(), "items", query );
        LOG.info( JsonUtils.mapToFormattedJsonString( r.getEntities() ) );
        assertEquals( 1, r.size() );
        assertEquals( entityId, r.getEntity().getId() );
    }


    @Test
    public void testSecondarySorts() throws Exception {
        LOG.info( "testSecondarySorts" );

        Id appId = new SimpleId("application");
        Id orgId = new SimpleId("organization");
        CollectionScope scope = new CollectionScopeImpl( appId, orgId, "items" );
        EntityManagerFacade em = new EntityManagerFacade( 
            collectionManagerFactory, collectionIndexFactory, scope);

        for ( int i = alphabet.length - 1; i >= 0; i-- ) {
            String name = alphabet[i];
            Map<String, Object> properties = new LinkedHashMap<String, Object>();
            properties.put( "name", name );
            properties.put( "group", i / 3 );
            properties.put( "reverse_name", alphabet[alphabet.length - 1 - i] );

            em.create( "items", properties );
        }

        Query query = Query.fromQL( "group = 1 order by name desc" );
        Results r = em.searchCollection( em.getApplicationRef(), "items", query );
        LOG.info( JsonUtils.mapToFormattedJsonString( r.getEntities() ) );
        int i = 6;
        for ( Entity entity : r.getEntities() ) {
            i--;
            assertEquals( 1, entity.getField( "group" ).getValue() );
            assertEquals( alphabet[i], entity.getField( "name" ).getValue() );
        }
        assertEquals( 3, i );
    }


//    @Test
//    public void testPropertyUpdateWithConnection() throws Exception {
//
//        Id appId = new SimpleId("application");
//        Id orgId = new SimpleId("organization");
//        CollectionScope scope = new CollectionScopeImpl( appId, orgId, "items" );
//        EntityManagerFacade em = new EntityManagerFacade( factory, null);
//
//
//        Map<String, Object> entity1 = new LinkedHashMap<String, Object>();
//        entity1.put( "name", "name_1" );
//        entity1.put( "status", "pickled" );
//
//
//        Map<String, Object> entity2 = new LinkedHashMap<String, Object>();
//        entity2.put( "name", "name_2" );
//        entity2.put( "status", "foo" );
//
//
//        Entity entity1Ref = em.create( "names", entity1 );
//        Entity entity2Ref = em.create( "names", entity2 );
//
//
//        em.createConnection( entity2Ref, "connecting", entity1Ref );
//
//        //should return valid values
//        Query query = Query.fromQL( "select * where status = 'pickled'" );
//
//        Results r = em.searchCollection( em.getApplicationRef(), "names", query );
//        assertEquals( 1, r.size() );
//        assertEquals( entity1Ref.getUuid(), r.getEntity().getUuid() );
//
//
//        r = em.searchCollection( em.getApplicationRef(), "names", query );
//        assertEquals( 1, r.size() );
//        assertEquals( entity1Ref.getUuid(), r.getEntity().getUuid() );
//
//        //now update the first entity, this causes the failure after connections
//        entity1Ref.setProperty( "status", "herring" );
//
//        em.update( entity1Ref );
//
//        //query and check the status has been updated, shouldn't return results
//        query = Query.fromQL( "select * where status = 'pickled'" );
//
//        r = em.searchCollection( em.getApplicationRef(), "names", query );
//        assertEquals( 0, r.size() );
//
//        //search connections
//        r = em.searchCollection( em.getApplicationRef(), "names", query );
//        assertEquals( 0, r.size() );
//
//
//        //should return results
//        query = Query.fromQL( "select * where status = 'herring'" );
//
//        r = em.searchCollection( em.getApplicationRef(), "names", query );
//        assertEquals( 1, r.size() );
//
//        assertEquals( entity1Ref.getUuid(), r.getEntity().getUuid() );
//
//
//        //search connections
//        r = em.searchCollection( em.getApplicationRef(), "names", query );
//        assertEquals( 1, r.size() );
//        assertEquals( entity1Ref.getUuid(), r.getEntity().getUuid() );
//    }
//
//
//    /** Same as above, but verifies the data in our entity_index_entry CF after the operations have completed */
//
//    @Test
//    public void testPropertyUpdateWithConnectionEntityIndexEntryAudit() throws Exception {
//
//        Id appId = new SimpleId("application");
//        Id orgId = new SimpleId("organization");
//        CollectionScope scope = new CollectionScopeImpl( appId, orgId, "items" );
//        EntityManagerFacade em = new EntityManagerFacade( factory, null);
//
//
//        Map<String, Object> entity1 = new LinkedHashMap<String, Object>();
//        entity1.put( "name", "name_1" );
//        entity1.put( "status", "pickled" );
//
//
//        Map<String, Object> entity2 = new LinkedHashMap<String, Object>();
//        entity2.put( "name", "name_2" );
//        entity2.put( "status", "foo" );
//
//
//        Entity entity1Ref = em.create( "names", entity1 );
//        Entity entity2Ref = em.create( "names", entity2 );
//
//
//        em.createConnection( entity2Ref, "connecting", entity1Ref );
//
//        //should return valid values
//        Query query = Query.fromQL( "select * where status = 'pickled'" );
//
//        Results r = em.searchCollection( em.getApplicationRef(), "names", query );
//        assertEquals( 1, r.size() );
//        assertEquals( entity1Ref.getUuid(), r.getEntity().getUuid() );
//
//
//        r = em.searchCollection( em.getApplicationRef(), "names", query );
//        assertEquals( 1, r.size() );
//        assertEquals( entity1Ref.getUuid(), r.getEntity().getUuid() );
//
//        //now update the first entity, this causes the failure after connections
//        entity1Ref.setProperty( "status", "herring" );
//
//        em.update( entity1Ref );
//
//        //query and check the status has been updated, shouldn't return results
//        query = Query.fromQL( "select * where status = 'pickled'" );
//
//        r = em.searchCollection( em.getApplicationRef(), "names", query );
//        assertEquals( 0, r.size() );
//
//        //search connections
//        r = em.searchCollection( em.getApplicationRef(), "names", query );
//        assertEquals( 0, r.size() );
//
//
//        //should return results
//        query = Query.fromQL( "select * where status = 'herring'" );
//
//        r = em.searchCollection( em.getApplicationRef(), "names", query );
//        assertEquals( 1, r.size() );
//
//        assertEquals( entity1Ref.getUuid(), r.getEntity().getUuid() );
//
//
//        //search connections
//        r = em.searchCollection( em.getApplicationRef(), "names", query );
//        assertEquals( 1, r.size() );
//        assertEquals( entity1Ref.getUuid(), r.getEntity().getUuid() );
//
//
//        RelationManagerImpl impl = ( RelationManagerImpl ) em.getRelationManager( entity2Ref );
//
//        //now read the index and see what properties are there
//
//
//        CassandraService cass = CoreITSuite.cassandraResource.getBean( CassandraService.class );
//
//        ByteBufferSerializer buf = ByteBufferSerializer.get();
//
//        Keyspace ko = cass.getApplicationKeyspace( applicationId );
//        Mutator<ByteBuffer> m = createMutator( ko, buf );
//
//
//        IndexUpdate update =
//                impl.batchStartIndexUpdate( m, entity1Ref, "status", "ignore", UUIDUtils.newTimeUUID(), false, false,
//                        true, false );
//
//        int count = 0;
//
//        IndexEntry lastMatch = null;
//
//        for ( IndexEntry entry : update.getPrevEntries() ) {
//            if ( "status".equals( entry.getPath() ) ) {
//                count++;
//                lastMatch = entry;
//            }
//        }
//
//
//        assertEquals( 1, count );
//
//        if ( lastMatch != null ) {
//            assertEquals( "herring", lastMatch.getValue() );
//        }
//        else {
//            fail( "The last match was null but should have been herring!" );
//        }
//    }
}
