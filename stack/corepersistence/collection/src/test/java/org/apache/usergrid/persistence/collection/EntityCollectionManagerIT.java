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
package org.apache.usergrid.persistence.collection;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.usergrid.persistence.collection.exception.WriteUniqueVerifyException;
import org.apache.usergrid.persistence.collection.guice.TestCollectionModule;
import org.apache.usergrid.persistence.collection.mvcc.entity.Stage;
import org.apache.usergrid.persistence.collection.serialization.MvccEntitySerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.SerializationFig;
import org.apache.usergrid.persistence.collection.serialization.UniqueValueSerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.UniqueValueSet;
import org.apache.usergrid.persistence.collection.util.EntityHelper;
import org.apache.usergrid.persistence.core.guice.MigrationManagerRule;
import org.apache.usergrid.persistence.core.guicyfig.SetConfigTestBypass;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.core.test.ITRunner;
import org.apache.usergrid.persistence.core.test.UseModules;
import org.apache.usergrid.persistence.core.util.Health;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.field.BooleanField;
import org.apache.usergrid.persistence.model.field.Field;
import org.apache.usergrid.persistence.model.field.IntegerField;
import org.apache.usergrid.persistence.model.field.StringField;

import com.fasterxml.uuid.UUIDComparator;
import com.google.inject.Inject;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import rx.Observable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


/** @author tnine */
@RunWith( ITRunner.class )
@UseModules( TestCollectionModule.class )
public class EntityCollectionManagerIT {

    @Inject
    private EntityCollectionManagerFactory factory;

    @Inject
    @Rule
    public MigrationManagerRule migrationManagerRule;

    @Inject
    private SerializationFig serializationFig;


    @Inject
    private UniqueValueSerializationStrategy uniqueValueSerializationStrategy;

    @Inject
    private MvccEntitySerializationStrategy entitySerializationStrategy;


    @Test
    public void write() {


        ApplicationScope context =
                new ApplicationScopeImpl( new SimpleId( "organization" ) );


        Entity newEntity = new Entity( new SimpleId( "test" ) );

        EntityCollectionManager manager = factory.createCollectionManager( context );

        Observable<Entity> observable = manager.write( newEntity );


        Entity returned = observable.toBlocking().lastOrDefault( null );

        assertNotNull( "Returned has a uuid", returned.getId() );
        assertNotNull( "Version exists", returned.getVersion() );
    }


    @Test
    public void writeWithUniqueValues() {


        ApplicationScope context =
                new ApplicationScopeImpl( new SimpleId( "organization" ) );

        EntityCollectionManager manager = factory.createCollectionManager( context );

        {
            Entity newEntity = new Entity( new SimpleId( "test" ) );
            newEntity.setField( new IntegerField( "count", 5, true ) );

            Observable<Entity> observable = manager.write( newEntity );
            Entity returned = observable.toBlocking().lastOrDefault( null );
        }

        {
            try {
                Entity newEntity = new Entity( new SimpleId( "test" ) );
                newEntity.setField( new IntegerField( "count", 5, true ) );

                manager.write( newEntity ).toBlocking().last();
                fail( "Write should have thrown an exception" );
            }
            catch ( Exception ex ) {
                WriteUniqueVerifyException e = ( WriteUniqueVerifyException ) ex;
                assertEquals( 1, e.getVioliations().size() );
            }
        }
    }


    @Test
    public void writeAndLoad() {


        ApplicationScope context =
                new ApplicationScopeImpl( new SimpleId( "organization" ) );

        Entity newEntity = new Entity( new SimpleId( "test" ) );

        EntityCollectionManager manager = factory.createCollectionManager( context );

        Observable<Entity> observable = manager.write( newEntity );

        Entity createReturned = observable.toBlocking().lastOrDefault( null );


        assertNotNull( "Id was assigned", createReturned.getId() );
        assertNotNull( "Version was assigned", createReturned.getVersion() );


        Observable<Entity> loadObservable = manager.load( createReturned.getId() );

        Entity loadReturned = loadObservable.toBlocking().lastOrDefault( null );

        assertEquals( "Same value", createReturned, loadReturned );
    }


    @Test
    public void writeLoadDelete() {


        ApplicationScope context =   new ApplicationScopeImpl( new SimpleId( "organization" ) );
        Entity newEntity = new Entity( new SimpleId( "test" ) );

        EntityCollectionManager manager = factory.createCollectionManager( context );

        Observable<Entity> observable = manager.write( newEntity );

        Entity createReturned = observable.toBlocking().lastOrDefault( null );


        assertNotNull( "Id was assigned", createReturned.getId() );

        UUID version = createReturned.getVersion();

        Observable<Entity> loadObservable = manager.load( createReturned.getId() );

        Entity loadReturned = loadObservable.toBlocking().lastOrDefault( null );

        assertEquals( "Same value", createReturned, loadReturned );

        manager.delete( createReturned.getId() ).toBlocking().last();

        loadObservable = manager.load( createReturned.getId() );

        //load may return null, use last or default
        loadReturned = loadObservable.toBlocking().lastOrDefault( null );

        assertNull("Entity was deleted", loadReturned);
    }


    @Test
    public void writeLoadUpdateLoad() {

        ApplicationScope context =  new ApplicationScopeImpl( new SimpleId( "organization" ) );

        Entity newEntity = new Entity( new SimpleId( "test" ) );
        newEntity.setField( new IntegerField( "counter", 1 ) );

        EntityCollectionManager manager = factory.createCollectionManager( context );

        Observable<Entity> observable = manager.write( newEntity );

        Entity createReturned = observable.toBlocking().lastOrDefault(null);


        assertNotNull( "Id was assigned", createReturned.getId() );

        Observable<Entity> loadObservable = manager.load(createReturned.getId());

        Entity loadReturned = loadObservable.toBlocking().lastOrDefault( null );

        assertEquals( "Same value", createReturned, loadReturned );


        assertEquals("Field value correct", createReturned.getField("counter"), loadReturned.getField("counter"));


        //update the field to 2
        createReturned.setField( new IntegerField( "counter", 2 ) );

        //wait for the write to complete
        manager.write( createReturned ).toBlocking().lastOrDefault(null);


        loadObservable = manager.load( createReturned.getId() );

        loadReturned = loadObservable.toBlocking().lastOrDefault(null);

        assertEquals( "Same value", createReturned, loadReturned );


        assertEquals("Field value correct", createReturned.getField("counter"), loadReturned.getField("counter"));
    }


    @Test
    public void writeAndLoadScopeClosure() {


        ApplicationScope collectionScope1 = new ApplicationScopeImpl(new SimpleId("organization"));


        Entity newEntity = new Entity( new SimpleId( "test" ) );

        EntityCollectionManager manager = factory.createCollectionManager(collectionScope1);

        Observable<Entity> observable = manager.write(newEntity);

        Entity createReturned = observable.toBlocking().lastOrDefault( null );


        assertNotNull("Id was assigned", createReturned.getId());
        assertNotNull("Version was assigned", createReturned.getVersion());


        Observable<Entity> loadObservable = manager.load( createReturned.getId() );

        Entity loadReturned = loadObservable.toBlocking().lastOrDefault( null );

        assertEquals("Same value", createReturned, loadReturned);


        ApplicationScope collectionScope2 = new ApplicationScopeImpl(new SimpleId("organization"));

        //now make sure we can't load it from another scope, using the same org


        EntityCollectionManager manager2 = factory.createCollectionManager( collectionScope2 );

        Entity loaded = manager2.load( createReturned.getId() ).toBlocking().lastOrDefault( null );

        assertNull("CollectionScope works correctly", loaded);


    }


    @Test
    public void writeAndGetField() {


        ApplicationScope collectionScope1 = new ApplicationScopeImpl(new SimpleId("organization"));

        Entity newEntity = new Entity( new SimpleId( "test" ) );
        Field field = new StringField( "testField", "unique", true );
        newEntity.setField(field);

        EntityCollectionManager manager = factory.createCollectionManager( collectionScope1 );

        Observable<Entity> observable = manager.write(newEntity);

        Entity createReturned = observable.toBlocking().lastOrDefault( null );


        assertNotNull( "Id was assigned", createReturned.getId() );
        assertNotNull("Version was assigned", createReturned.getVersion());

        Id id = manager.getIdField( newEntity.getId().getType(), field ).toBlocking().lastOrDefault( null );
        assertNotNull( id );
        assertEquals( newEntity.getId(), id );

        Field fieldNull = new StringField( "testFieldNotThere", "uniquely", true );
        id = manager.getIdField(  newEntity.getId().getType(), fieldNull ).toBlocking().lastOrDefault( null );
        assertNull( id );
    }




    @Test
    public void updateVersioning() {

        // create entity
        Entity origEntity = new Entity( new SimpleId( "testUpdate" ) );
        origEntity.setField( new StringField( "testField", "value" ) );

        ApplicationScope context = new ApplicationScopeImpl(new SimpleId("organization"));

        EntityCollectionManager manager = factory.createCollectionManager( context );
        Entity returned = manager.write( origEntity ).toBlocking().lastOrDefault(null);

        // note its version
        UUID oldVersion = returned.getVersion();

        // partial update entity but with new entity that has version = null
        assertNotNull( "A version must be assigned", oldVersion );

        // partial update entity but we don't have version number
        Entity updateEntity = new Entity( origEntity.getId() );
        updateEntity.setField( new StringField( "addedField", "other value" ) );
        manager.write( updateEntity ).toBlocking().lastOrDefault(null);

        // get entity now, it must have a new version
        returned = manager.load( origEntity.getId() ).toBlocking().lastOrDefault( null );
        UUID newVersion = returned.getVersion();

        assertNotNull( "A new version must be assigned", newVersion );

        // new Version should be > old version
        assertTrue(UUIDComparator.staticCompare(newVersion, oldVersion) > 0);
    }


    @Test
    public void writeMultiget() {

        final ApplicationScope context =  new ApplicationScopeImpl( new SimpleId( "organization" ) );
        final EntityCollectionManager manager = factory.createCollectionManager( context );

        final int multigetSize = serializationFig.getMaxLoadSize();

        final List<Entity> writtenEntities = new ArrayList<>( multigetSize );
        final List<Id> entityIds = new ArrayList<>( multigetSize );

        for ( int i = 0; i < multigetSize; i++ ) {
            final Entity entity = new Entity( new SimpleId( "test" ) );

            final Entity written = manager.write( entity ).toBlocking().last();

            writtenEntities.add( written );
            entityIds.add( written.getId() );
        }


        final EntitySet entitySet = manager.load( entityIds ).toBlocking().lastOrDefault( null );

        assertNotNull(entitySet);

        assertEquals( multigetSize, entitySet.size() );
        assertFalse(entitySet.isEmpty());

        /**
         * Validate every element exists
         */
        for ( int i = 0; i < multigetSize; i++ ) {
            final Entity expected = writtenEntities.get( i );

            final MvccEntity returned = entitySet.getEntity( expected.getId() );

            assertEquals( "Same entity returned", expected, returned.getEntity().get() );
        }
    }


    /**
     * Perform a multiget where every entity will need repaired on load
     */
    @Test
    public void writeMultigetRepair() {

        final ApplicationScope context =  new ApplicationScopeImpl( new SimpleId( "organization" ) );
        final EntityCollectionManager manager = factory.createCollectionManager( context );

        final int multigetSize = serializationFig.getMaxLoadSize();

        final List<Entity> writtenEntities = new ArrayList<>( multigetSize );
        final List<Id> entityIds = new ArrayList<>( multigetSize );

        for ( int i = 0; i < multigetSize; i++ ) {
            final Entity entity = new Entity( new SimpleId( "test" ) );

            final Entity written = manager.write( entity ).toBlocking().last();

            written.setField( new BooleanField( "updated", true ) );

            final Entity updated = manager.write( written ).toBlocking().last();

            writtenEntities.add( updated );
            entityIds.add( updated.getId() );
        }


        final EntitySet entitySet = manager.load( entityIds ).toBlocking().lastOrDefault( null );

        assertNotNull( entitySet );

        assertEquals( multigetSize, entitySet.size() );
        assertFalse( entitySet.isEmpty() );

        /**
         * Validate every element exists
         */
        for ( int i = 0; i < multigetSize; i++ ) {
            final Entity expected = writtenEntities.get( i );

            final MvccEntity returned = entitySet.getEntity( expected.getId() );

            assertEquals( "Same entity returned", expected, returned.getEntity().get() );

            assertTrue((Boolean) returned.getEntity().get().getField("updated").getValue());
        }
    }


    @Test( expected = IllegalArgumentException.class )
    public void readTooLarge() {

        final ApplicationScope context =  new ApplicationScopeImpl( new SimpleId( "organization" ) );
        final EntityCollectionManager manager = factory.createCollectionManager( context );

        final int multigetSize = serializationFig.getMaxLoadSize() + 1;


        final List<Id> entityIds = new ArrayList<>( multigetSize );

        for ( int i = 0; i < multigetSize; i++ ) {

            entityIds.add( new SimpleId( "simple" ) );
        }


        //should throw an exception
        manager.load( entityIds ).toBlocking().lastOrDefault( null );
    }


    @Test
    public void testGetVersion() {

        ApplicationScope context =  new ApplicationScopeImpl( new SimpleId( "organization" ) );


        final EntityCollectionManager manager = factory.createCollectionManager( context );

        final Entity newEntity = new Entity( new SimpleId( "test" ) );

        Entity created1 = manager.write( newEntity ).toBlocking().lastOrDefault( null );

        assertNotNull( "Id was assigned", created1.getId() );
        assertNotNull( "Version was assigned", created1.getVersion() );

        Entity secondEntity = new Entity( new SimpleId( "test" ) );

        Entity created2 = manager.write( secondEntity ).toBlocking().lastOrDefault( null );

        assertNotNull( "Id was assigned", created2.getId() );
        assertNotNull( "Version was assigned", created2.getVersion() );


        VersionSet results =
                manager.getLatestVersion( Arrays.asList( created1.getId(), created2.getId() ) ).toBlocking().last();


        final MvccLogEntry version1Log = results.getMaxVersion( created1.getId() );
        assertEquals( created1.getId(), version1Log.getEntityId() );
        assertEquals( created1.getVersion(), version1Log.getVersion() );
        assertEquals( MvccLogEntry.State.COMPLETE, version1Log.getState() );
        assertEquals( Stage.COMMITTED, version1Log.getStage() );

        final MvccLogEntry version2Log = results.getMaxVersion( created2.getId() );
        assertEquals( created2.getId(), version2Log.getEntityId() );
        assertEquals( created2.getVersion(), version2Log.getVersion() );
        assertEquals( MvccLogEntry.State.COMPLETE, version2Log.getState() );
        assertEquals( Stage.COMMITTED, version2Log.getStage() );
    }


    @Test
    public void testVersionLogWrite() {

        ApplicationScope context =  new ApplicationScopeImpl( new SimpleId( "organization" ) );


        final EntityCollectionManager manager = factory.createCollectionManager( context );

        final Entity newEntity = new Entity( new SimpleId( "test" ) );

        final Entity v1Created = manager.write( newEntity ).toBlocking().lastOrDefault( null );

        assertNotNull( "Id was assigned", v1Created.getId() );
        assertNotNull( "Version was assigned", v1Created.getVersion() );

        final UUID v1Version = v1Created.getVersion();

        final VersionSet resultsV1 = manager.getLatestVersion(Arrays.asList(v1Created.getId())).toBlocking().last();


        final MvccLogEntry version1Log = resultsV1.getMaxVersion(v1Created.getId());
        assertEquals( v1Created.getId(), version1Log.getEntityId() );
        assertEquals( v1Version, version1Log.getVersion() );
        assertEquals( MvccLogEntry.State.COMPLETE, version1Log.getState() );
        assertEquals( Stage.COMMITTED, version1Log.getStage() );

        final Entity v2Created = manager.write( v1Created ).toBlocking().last();

        final UUID v2Version = v2Created.getVersion();


        assertTrue("Newer version in v2", UUIDComparator.staticCompare(v2Version, v1Version) > 0);


        final VersionSet resultsV2 = manager.getLatestVersion( Arrays.asList( v1Created.getId() ) ).toBlocking().last();


        final MvccLogEntry version2Log = resultsV2.getMaxVersion( v1Created.getId() );
        assertEquals( v1Created.getId(), version2Log.getEntityId() );
        assertEquals( v2Version, version2Log.getVersion() );
        assertEquals( MvccLogEntry.State.COMPLETE, version2Log.getState() );
        assertEquals( Stage.COMMITTED, version2Log.getStage() );
    }


    @Test
    public void testVersionLogUpdate() {

        ApplicationScope context =  new ApplicationScopeImpl( new SimpleId( "organization" ) );


        final EntityCollectionManager manager = factory.createCollectionManager( context );

        final Entity newEntity = new Entity( new SimpleId( "test" ) );

        final Entity v1Created = manager.write( newEntity ).toBlocking().lastOrDefault( null );

        assertNotNull( "Id was assigned", v1Created.getId() );
        assertNotNull( "Version was assigned", v1Created.getVersion() );

        final UUID v1Version = v1Created.getVersion();


        final VersionSet resultsV1 = manager.getLatestVersion( Arrays.asList( v1Created.getId() ) ).toBlocking().last();


        final MvccLogEntry version1Log = resultsV1.getMaxVersion( v1Created.getId() );
        assertEquals( v1Created.getId(), version1Log.getEntityId() );
        assertEquals( v1Version, version1Log.getVersion() );
        assertEquals( MvccLogEntry.State.COMPLETE, version1Log.getState() );
        assertEquals( Stage.COMMITTED, version1Log.getStage() );

        final Entity v2Created = manager.write( v1Created ).toBlocking().last();

        final UUID v2Version = v2Created.getVersion();


        assertEquals( "Same entityId", v1Created.getId(), v2Created.getId() );

        assertTrue( "Newer version in v2", UUIDComparator.staticCompare( v2Version, v1Version ) > 0 );


        final VersionSet resultsV2 = manager.getLatestVersion( Arrays.asList( v1Created.getId() ) ).toBlocking().last();


        final MvccLogEntry version2Log = resultsV2.getMaxVersion( v1Created.getId() );
        assertEquals( v2Created.getId(), version2Log.getEntityId() );
        assertEquals( v2Version, version2Log.getVersion() );
        assertEquals( MvccLogEntry.State.COMPLETE, version2Log.getState() );
        assertEquals( Stage.COMMITTED, version2Log.getStage() );
    }


    @Test
    public void healthTest() {

        ApplicationScope context =  new ApplicationScopeImpl( new SimpleId( "organization" ) );

        final EntityCollectionManager manager = factory.createCollectionManager( context );

        assertEquals( Health.GREEN, manager.getHealth() );
    }


    /**
     * Tests an entity with more than  65535 bytes worth of data
     */
    @Test
    public void largeEntityWriteRead() {
        final int setSize = 65535 * 2;

        final int currentMaxSize = serializationFig.getMaxEntitySize();

        //override our default
        SetConfigTestBypass.setValueByPass( serializationFig, "getMaxEntitySize", 65535 * 10 + "" );


        final Entity entity = EntityHelper.generateEntity( setSize );

        //now we have one massive, entity, save it and retrieve it.
        ApplicationScope context =  new ApplicationScopeImpl( new SimpleId( "organization" ) );

        final EntityCollectionManager manager = factory.createCollectionManager( context );

        final Entity saved = manager.write( entity ).toBlocking().last();


        assertEquals( entity, saved );

        //now load it
        final Entity loaded = manager.load( entity.getId() ).toBlocking().last();


        EntityHelper.verifyDeepEquals( entity, loaded );


        //override our default
        SetConfigTestBypass.setValueByPass( serializationFig, "getMaxEntitySize", currentMaxSize + "" );
    }

    @Test
    public void invalidNameRepair() throws ConnectionException {

        //write an entity with a unique field
        ApplicationScope context =  new ApplicationScopeImpl( new SimpleId( "organization" ) );

        Entity newEntity = new Entity( new SimpleId( "test" ) );

        //if we add a second field we get a second entity that is the exact same. Is this expected?
        final IntegerField expectedInteger =  new IntegerField( "count", 5, true );
       // final StringField expectedString = new StringField( "yes", "fred", true );

        newEntity.setField( expectedInteger );
       // newEntity.setField( expectedString );

        EntityCollectionManager manager = factory.createCollectionManager( context );

        Observable<Entity> observable = manager.write( newEntity );

        Entity createReturned = observable.toBlocking().lastOrDefault( null );

        assertNotNull( "Need entity to be created before proceeding", createReturned );
        assertNotNull( "Id was assigned", createReturned.getId() );
        assertNotNull( "Version was assigned", createReturned.getVersion() );

        FieldSet
            fieldResults = manager.getEntitiesFromFields( newEntity.getId().getType(), Arrays.<Field>asList( expectedInteger ) ).toBlocking().last();

        assertEquals(1,fieldResults.size());


        //verify the entity is correct.
        assertEquals( "Same value", createReturned, fieldResults.getEntity( expectedInteger ).getEntity().get()); //loadReturned );

        //use the entity serializationStrategy to remove the entity data.

        //do a mark as one test, and a delete as another
        entitySerializationStrategy.delete( context,createReturned.getId(),createReturned.getVersion() ).execute();

        //try to load via the unique field, should have triggered repair
        final FieldSet
            results = manager.getEntitiesFromFields( newEntity.getId().getType(), Arrays.<Field>asList( expectedInteger ) ).toBlocking().last();


        //verify no entity returned
        assertTrue( results.isEmpty() );

        //user the unique serialization to verify it's been deleted from cassandra

        UniqueValueSet uniqueValues = uniqueValueSerializationStrategy.load( context,  newEntity.getId().getType(), createReturned.getFields() );
        assertFalse( uniqueValues.iterator().hasNext() );

    }


    @Test
    public void testGetIdField() throws Exception {

        ApplicationScope context =  new ApplicationScopeImpl( new SimpleId( "organization" ) );
        EntityCollectionManager manager = factory.createCollectionManager( context );

        // create an entity of type "item" with a unique_id field value = 1

        Entity entity1 = new Entity( new SimpleId( "item" ) );
        entity1.setField( new StringField( "unique_id", "1", true ));
        manager.write( entity1 ).toBlocking().last();

        final Observable<Id> idObs = manager.getIdField("item", new StringField("unique_id", "1"));
        Id id = idObs.toBlocking().lastOrDefault(null);
        assertEquals(entity1.getId(), id);

        // create an entity of type "deleted_item" with a unique_id field value = 1

        Entity entity2 = new Entity( new SimpleId( "deleted_item" ) );
        entity2.setField( new StringField( "unique_id", "1", true ));
        manager = factory.createCollectionManager( context );
        manager.write( entity2 ).toBlocking().last();

        final Observable<Id> id2Obs = manager.getIdField("deleted_item", new StringField("unique_id", "1"));
        Id id2 = id2Obs.toBlocking().lastOrDefault(null);
        assertEquals(entity2.getId(), id2);
    }
}
