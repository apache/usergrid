package org.apache.usergrid.persistence.collection;


import org.junit.Rule;
import org.junit.Test;

import org.apache.usergrid.persistence.collection.guice.CassandraTestCollectionModule;
import org.apache.usergrid.persistence.collection.impl.CollectionScopeImpl;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.field.IntegerField;
import org.apache.usergrid.persistence.test.CassandraRule;

import com.google.common.eventbus.EventBus;
import com.google.guiceberry.junit4.GuiceBerryRule;
import com.google.inject.Inject;

import rx.Observable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;


/** @author tnine */
public class EntityCollectionManagerIT {
    @Rule
    public final GuiceBerryRule guiceBerry = new GuiceBerryRule( CassandraTestCollectionModule.class );


    @Rule
    public final CassandraRule rule = new CassandraRule();


    @Inject
    private EntityCollectionManagerFactory factory;

    @Inject
    private EventBus eventBus;


    @Test
    public void write() {

        CollectionScope context = new CollectionScopeImpl( new SimpleId( "test" ), "test" );


        Entity newEntity = new Entity( new SimpleId( "test" ) );

        EntityCollectionManager manager = factory.createCollectionManager( context );

        Observable<Entity> observable = manager.write( newEntity );


        Entity returned = observable.toBlockingObservable().lastOrDefault( null );

        assertNotNull( "Returned has a uuid", returned.getId() );
        assertNotNull( "Version exists" );
    }


    @Test
    public void writeAndLoad() {

        CollectionScope context = new CollectionScopeImpl( new SimpleId( "test" ), "test" );
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

        CollectionScope context = new CollectionScopeImpl( new SimpleId( "test" ), "test" );
        Entity newEntity = new Entity( new SimpleId( "test" ) );

        EntityCollectionManager manager = factory.createCollectionManager( context );

        Observable<Entity> observable = manager.write( newEntity );

        Entity createReturned = observable.toBlockingObservable().lastOrDefault( null );


        assertNotNull( "Id was assigned", createReturned.getId() );

        Observable<Entity> loadObservable = manager.load( createReturned.getId() );

        Entity loadReturned = loadObservable.toBlockingObservable().lastOrDefault( null );

        assertEquals( "Same value", createReturned, loadReturned );

        manager.delete( createReturned.getId() ).toBlockingObservable().next();

        loadObservable = manager.load( createReturned.getId() );

        //load may return null, use last or default
        loadReturned = loadObservable.toBlockingObservable().lastOrDefault( null );

        assertNull( "Entity was deleted", loadReturned );
    }


    @Test
    public void writeLoadUpdateLoad() {

        CollectionScope context = new CollectionScopeImpl( new SimpleId( "test" ), "test" );

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

        CollectionScope collectionScope1 = new CollectionScopeImpl( new SimpleId( "test1" ), "test1" );

        Entity newEntity = new Entity( new SimpleId( "test" ) );

        EntityCollectionManager manager = factory.createCollectionManager( collectionScope1 );

        Observable<Entity> observable = manager.write( newEntity );

        Entity createReturned = observable.toBlockingObservable().lastOrDefault( null );


        assertNotNull( "Id was assigned", createReturned.getId() );
        assertNotNull( "Version was assigned", createReturned.getVersion() );


        Observable<Entity> loadObservable = manager.load( createReturned.getId() );

        Entity loadReturned = loadObservable.toBlockingObservable().lastOrDefault( null );

        assertEquals( "Same value", createReturned, loadReturned );


        //now make sure we can't load it from another scope
        CollectionScope collectionScope2 = new CollectionScopeImpl( new SimpleId("test2"), "test2" );

        EntityCollectionManager manager2 = factory.createCollectionManager( collectionScope2 );

        Entity loaded = manager2.load( createReturned.getId() ).toBlockingObservable().lastOrDefault( null );

        assertNull("CollectionScope works correctly", loaded);
    }

}
