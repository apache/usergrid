package org.apache.usergrid.persistence.collection;


import org.jukito.UseModules;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.usergrid.persistence.collection.guice.MigrationManagerRule;
import org.apache.usergrid.persistence.collection.guice.TestCollectionModule;
import org.apache.usergrid.persistence.collection.impl.CollectionScopeImpl;
import org.apache.usergrid.persistence.core.cassandra.ITRunner;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.field.IntegerField;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import rx.Observable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;


/**
 * TODO: Refactor this and the org.apache.usergrid.persistence.core.consistency test into 1 common set of assertions with seperate invocations and result
 * returns once jukito is finished
 *
 */
@RunWith( ITRunner.class )
@UseModules( TestCollectionModule.class )
public class EntityCollectionManagerSyncIT {
    @Inject
    private EntityCollectionManagerFactory factory;

    @Inject
    private EventBus eventBus;


    @Inject
    @Rule
    public MigrationManagerRule migrationManagerRule;


    @Test
    @Assisted
    public void write() {

        CollectionScope context = new CollectionScopeImpl(new SimpleId( "organization" ),  new SimpleId( "test" ), "test" );


        Entity newEntity = new Entity( new SimpleId( "test" ) );

        EntityCollectionManagerSync manager = factory.createCollectionManagerSync( context );

        Entity returned = manager.write( newEntity );

        assertNotNull( "Returned has a uuid", returned.getId() );
        assertNotNull( "Version exists" );
    }


    @Test
    public void writeAndLoad() {

        CollectionScope context = new CollectionScopeImpl(new SimpleId( "organization" ),  new SimpleId( "test" ), "test" );
        Entity newEntity = new Entity( new SimpleId( "test" ) );

        EntityCollectionManagerSync manager = factory.createCollectionManagerSync( context );

        Entity createReturned  = manager.write( newEntity );


        assertNotNull( "Id was assigned", createReturned.getId() );
        assertNotNull( "Version was assigned", createReturned.getVersion() );


        Entity loadReturned  = manager.load( createReturned.getId() );

        assertEquals( "Same value", createReturned, loadReturned );
    }


    @Test
    public void writeLoadDelete() {

        CollectionScope context = new CollectionScopeImpl(new SimpleId( "organization" ),  new SimpleId( "test" ), "test" );
        Entity newEntity = new Entity( new SimpleId( "test" ) );

        EntityCollectionManagerSync manager = factory.createCollectionManagerSync( context );

        Entity createReturned  = manager.write( newEntity );



        assertNotNull( "Id was assigned", createReturned.getId() );

        Entity loadReturned = manager.load( createReturned.getId() );

        assertEquals( "Same value", createReturned, loadReturned );

        manager.delete( createReturned.getId() );

        loadReturned = manager.load( createReturned.getId() );


        assertNull( "Entity was deleted", loadReturned );
    }


    @Test
    public void writeLoadUpdateLoad() {

        CollectionScope context = new CollectionScopeImpl(new SimpleId( "organization" ),  new SimpleId( "test" ), "test" );

        Entity newEntity = new Entity( new SimpleId( "test" ) );
        newEntity.setField( new IntegerField( "counter", 1 ) );

        EntityCollectionManagerSync manager = factory.createCollectionManagerSync( context );

        Entity createReturned = manager.write( newEntity );

        assertNotNull( "Id was assigned", createReturned.getId() );

        Entity loadReturned  = manager.load( createReturned.getId() );


        assertEquals( "Same value", createReturned, loadReturned );


        assertEquals( "Field value correct", createReturned.getField( "counter" ), loadReturned.getField( "counter" ) );


        //update the field to 2
        createReturned.setField( new IntegerField( "counter", 2 ) );

        //wait for the write to complete
        manager.write( createReturned );


        loadReturned =  manager.load( createReturned.getId() );

        assertEquals( "Same value", createReturned, loadReturned );


        assertEquals( "Field value correct", createReturned.getField( "counter" ), loadReturned.getField( "counter" ) );
    }


    @Test
    public void writeAndLoadScopeClosure() {


        CollectionScope collectionScope1 = new CollectionScopeImpl(new SimpleId( "organization" ),  new SimpleId( "test1" ), "test1" );

        Entity newEntity = new Entity( new SimpleId( "test" ) );

        EntityCollectionManager manager = factory.createCollectionManager( collectionScope1 );

        Observable<Entity> observable = manager.write( newEntity );

        Entity createReturned = observable.toBlocking().lastOrDefault( null );


        assertNotNull( "Id was assigned", createReturned.getId() );
        assertNotNull( "Version was assigned", createReturned.getVersion() );


        Observable<Entity> loadObservable = manager.load( createReturned.getId() );

        Entity loadReturned = loadObservable.toBlocking().lastOrDefault( null );

        assertEquals( "Same value", createReturned, loadReturned );


        //now make sure we can't load it from another scope, using the same org
        CollectionScope collectionScope2 = new CollectionScopeImpl(collectionScope1.getApplication(),  new SimpleId("test2"), collectionScope1.getName());

        EntityCollectionManager manager2 = factory.createCollectionManager( collectionScope2 );

        Entity loaded = manager2.load( createReturned.getId() ).toBlocking().lastOrDefault( null );

        assertNull("CollectionScope works correctly", loaded);

        //now try to load it from another org, with the same scope

        new CollectionScopeImpl( new SimpleId("organization2"), collectionScope1.getOwner(), collectionScope1.getName() );
    }

}
