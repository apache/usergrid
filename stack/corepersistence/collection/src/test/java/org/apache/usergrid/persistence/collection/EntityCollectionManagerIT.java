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


import java.util.UUID;

import org.jukito.UseModules;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.usergrid.persistence.collection.exception.CollectionRuntimeException;
import org.apache.usergrid.persistence.collection.exception.WriteUniqueVerifyException;
import org.apache.usergrid.persistence.collection.guice.MigrationManagerRule;
import org.apache.usergrid.persistence.collection.guice.TestCollectionModule;
import org.apache.usergrid.persistence.collection.impl.CollectionScopeImpl;
import org.apache.usergrid.persistence.core.cassandra.ITRunner;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.field.IntegerField;
import org.apache.usergrid.persistence.model.field.StringField;

import com.google.inject.Inject;

import rx.Observable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import static org.junit.Assert.*;


/** @author tnine */
@RunWith( ITRunner.class )
@UseModules( TestCollectionModule.class )
public class EntityCollectionManagerIT {
    @Inject
    private EntityCollectionManagerFactory factory;


    @Inject
    @Rule
    public MigrationManagerRule migrationManagerRule;


    @Test
    public void write() {

        CollectionScope context = new CollectionScopeImpl(
                new SimpleId( "organization" ),  new SimpleId( "test" ), "test" );


        Entity newEntity = new Entity( new SimpleId( "test" ) );

        EntityCollectionManager manager = factory.createCollectionManager( context );

        Observable<Entity> observable = manager.write( newEntity );


        Entity returned = observable.toBlockingObservable().lastOrDefault( null );

        assertNotNull( "Returned has a uuid", returned.getId() );
        assertNotNull( "Version exists" );
    }

    @Test
    public void writeWithUniqueValues() {

        CollectionScope context = new CollectionScopeImpl(
                new SimpleId( "organization" ),  new SimpleId( "test" ), "test" );

        EntityCollectionManager manager = factory.createCollectionManager( context );

        {
            Entity newEntity = new Entity( new SimpleId( "test" ) );
            newEntity.setField( new IntegerField("count", 5, true) );

            Observable<Entity> observable = manager.write( newEntity );
            Entity returned = observable.toBlockingObservable().lastOrDefault( null );
        }

        {
            try {
                Entity newEntity = new Entity( new SimpleId( "test" ) );
                newEntity.setField( new IntegerField("count", 5, true) );

                manager.write( newEntity ).toBlockingObservable().last();
                fail("Write should have thrown an exception");

            } catch ( Exception ex ) {
                WriteUniqueVerifyException e = (WriteUniqueVerifyException)ex;
                assertEquals( 1, e.getVioliations().size() );
            }
        }
    }

    @Test
    public void writeAndLoad() {

        CollectionScope context = new CollectionScopeImpl(
                new SimpleId( "organization" ),  new SimpleId( "test" ), "test" );

        Entity newEntity = new Entity( new SimpleId( "test" ) );

        EntityCollectionManager manager = factory.createCollectionManager( context );

        Observable<Entity> observable = manager.write( newEntity );

        Entity createReturned = observable.toBlockingObservable().lastOrDefault( null );


        assertNotNull( "Id was assigned", createReturned.getId() );
        assertNotNull( "Version was assigned", createReturned.getVersion() );


        Observable<Entity> loadObservable = manager.load( createReturned.getId() );

        Entity loadReturned = loadObservable.toBlockingObservable().lastOrDefault( null );

        assertEquals( "Same value", createReturned, loadReturned );
    }


    @Test
    public void writeLoadDelete() {

        CollectionScope context = new CollectionScopeImpl(
                new SimpleId( "organization" ),  new SimpleId( "test" ), "test" );
        Entity newEntity = new Entity( new SimpleId( "test" ) );

        EntityCollectionManager manager = factory.createCollectionManager( context );

        Observable<Entity> observable = manager.write( newEntity );

        Entity createReturned = observable.toBlockingObservable().lastOrDefault( null );


        assertNotNull( "Id was assigned", createReturned.getId() );

        UUID version = createReturned.getVersion();

        Observable<Entity> loadObservable = manager.load( createReturned.getId() );

        Entity loadReturned = loadObservable.toBlockingObservable().lastOrDefault( null );

        assertEquals( "Same value", createReturned, loadReturned );

        manager.delete( createReturned.getId() ).toBlockingObservable().last();

        loadObservable = manager.load( createReturned.getId() );

        //load may return null, use last or default
        loadReturned = loadObservable.toBlockingObservable().lastOrDefault( null );

        assertNull( "Entity was deleted", loadReturned );
    }


    @Test
    public void writeLoadUpdateLoad() {

        CollectionScope context = new CollectionScopeImpl(
                new SimpleId( "organization" ),  new SimpleId( "test" ), "test" );

        Entity newEntity = new Entity( new SimpleId( "test" ) );
        newEntity.setField( new IntegerField( "counter", 1 ) );

        EntityCollectionManager manager = factory.createCollectionManager( context );

        Observable<Entity> observable = manager.write( newEntity );

        Entity createReturned = observable.toBlockingObservable().lastOrDefault( null );


        assertNotNull( "Id was assigned", createReturned.getId() );

        Observable<Entity> loadObservable = manager.load( createReturned.getId() );

        Entity loadReturned = loadObservable.toBlockingObservable().lastOrDefault( null );

        assertEquals( "Same value", createReturned, loadReturned );


        assertEquals( "Field value correct", createReturned.getField( "counter" ), loadReturned.getField( "counter" ) );


        //update the field to 2
        createReturned.setField( new IntegerField( "counter", 2 ) );

        //wait for the write to complete
        manager.write( createReturned ).toBlockingObservable().lastOrDefault( null );


        loadObservable = manager.load( createReturned.getId() );

        loadReturned = loadObservable.toBlockingObservable().lastOrDefault( null );

        assertEquals( "Same value", createReturned, loadReturned );


        assertEquals( "Field value correct", createReturned.getField( "counter" ), loadReturned.getField( "counter" ) );
    }


    @Test
    public void writeAndLoadScopeClosure() {


        CollectionScope collectionScope1 = new CollectionScopeImpl(
                new SimpleId( "organization" ),  new SimpleId( "test1" ), "test1" );

        Entity newEntity = new Entity( new SimpleId( "test" ) );

        EntityCollectionManager manager = factory.createCollectionManager( collectionScope1 );

        Observable<Entity> observable = manager.write( newEntity );

        Entity createReturned = observable.toBlockingObservable().lastOrDefault( null );


        assertNotNull( "Id was assigned", createReturned.getId() );
        assertNotNull( "Version was assigned", createReturned.getVersion() );


        Observable<Entity> loadObservable = manager.load( createReturned.getId() );

        Entity loadReturned = loadObservable.toBlockingObservable().lastOrDefault( null );

        assertEquals( "Same value", createReturned, loadReturned );


        //now make sure we can't load it from another scope, using the same org
        CollectionScope collectionScope2 = new CollectionScopeImpl(
            collectionScope1.getApplication(),  new SimpleId("test2"), collectionScope1.getName());

        EntityCollectionManager manager2 = factory.createCollectionManager( collectionScope2 );

        Entity loaded = manager2.load( createReturned.getId() )
                .toBlockingObservable().lastOrDefault( null );

        assertNull( "CollectionScope works correctly", loaded );

        //now try to load it from another org, with the same scope

        CollectionScope collectionScope3 = new CollectionScopeImpl( 
            new SimpleId("organization2"), collectionScope1.getOwner(), collectionScope1.getName());
        assertNotNull( collectionScope3 );
    }

    @Test
    public void partialUpdate() {
        StringField testField1 = new StringField("testField","value");
        StringField addedField = new StringField( "testFud", "NEWPARTIALUPDATEZOMG" );

        CollectionScope context = new CollectionScopeImpl(
                new SimpleId( "organization" ),  new SimpleId( "testUpdate" ), "testUpdate" );

        Entity oldEntity = new Entity( new SimpleId( "testUpdate" ) );
        oldEntity.setField( new StringField( "testField", "value" ) );

        EntityCollectionManager manager = factory.createCollectionManager( context );

        Observable<Entity> observable = manager.write( oldEntity );

        Entity returned = observable.toBlockingObservable().lastOrDefault( null );

        assertNotNull( "Returned has a uuid", returned.getId() );

        /**
         * Modify the oldEntity
         */
        oldEntity.getFields().remove( testField1  );
        oldEntity.setField( addedField );

        observable = manager.update( oldEntity );

        Entity updateReturned = observable.toBlockingObservable().lastOrDefault( null );

        assertNotNull( "Returned has a uuid", returned.getId() );
        assertEquals( oldEntity.getField( "testFud" ), returned.getField( "testFud" ) );

        Observable<Entity> newEntityObs = manager.load( updateReturned.getId() );
        Entity newEntity = newEntityObs.toBlockingObservable().last();

        assertNotNull( "Returned has a uuid", returned.getId() );
        assertEquals( addedField, newEntity.getField( "testFud" ));


    }

    @Test
    public void partialUpdateDelete() {
        StringField testField1 = new StringField("testField","value");
        StringField addedField = new StringField( "testFud", "NEWPARTIALUPDATEZOMG" );

        CollectionScope context = new CollectionScopeImpl(
                new SimpleId( "organization" ),  new SimpleId( "testUpdate" ), "testUpdate" );

        Entity oldEntity = new Entity( new SimpleId( "testUpdate" ) );
        oldEntity.setField( new StringField( "testField", "value" ) );

        EntityCollectionManager manager = factory.createCollectionManager( context );

        Observable<Entity> observable = manager.write( oldEntity );

        Entity returned = observable.toBlockingObservable().lastOrDefault( null );

        assertNotNull( "Returned has a uuid", returned.getId() );

        oldEntity.getFields().remove( testField1  );
        oldEntity.setField( addedField );

        //Entity is deleted then updated right afterwards.
        manager.delete( oldEntity.getId() );

        observable = manager.update( oldEntity);

        returned = observable.toBlockingObservable().lastOrDefault( null );

        assertNotNull( "Returned has a uuid", returned.getId() );
        assertEquals( oldEntity.getField( "testFud" ), returned.getField( "testFud" ) );

        Observable<Entity> newEntityObs = manager.load( oldEntity.getId() );
        Entity newEntity = newEntityObs.toBlockingObservable().last();

        assertNotNull( "Returned has a uuid", returned.getId() );
        assertEquals( addedField, newEntity.getField( "testFud" ));


    }


}
