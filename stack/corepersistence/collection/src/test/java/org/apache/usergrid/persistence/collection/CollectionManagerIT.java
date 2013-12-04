package org.apache.usergrid.persistence.collection;


import org.junit.Rule;
import org.junit.Test;

import org.apache.usergrid.persistence.collection.guice.CassandraTestCollectionModule;
import org.apache.usergrid.persistence.collection.impl.CollectionContextImpl;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.field.IntegerField;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;
import org.apache.usergrid.persistence.test.CassandraRule;

import com.google.guiceberry.junit4.GuiceBerryRule;
import com.google.inject.Inject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


/** @author tnine */
public class CollectionManagerIT {
    @Rule
    public final GuiceBerryRule guiceBerry = new GuiceBerryRule( CassandraTestCollectionModule.class );


    @Rule
    public final CassandraRule rule = new CassandraRule();


    @Inject
    private CollectionManagerFactory factory;


    @Test
    public void create() {

        CollectionContext context =
                new CollectionContextImpl( UUIDGenerator.newTimeUUID(), UUIDGenerator.newTimeUUID(), "test" );
        Entity newEntity = new Entity( "test" );

        CollectionManager manager = factory.createCollectionManager( context );

        Entity returned = manager.create( newEntity );

        assertNotNull( "Returned has a uuid", returned.getUuid() );
        assertEquals( "Version matches uuid for create", returned.getUuid(), returned.getVersion() );

        assertTrue( "Created time was set", returned.getCreated() > 0 );
        assertEquals( "Created and updated time match on create", returned.getCreated(), returned.getUpdated() );
    }


    @Test
    public void createAndLoad() {

        CollectionContext context =
                new CollectionContextImpl( UUIDGenerator.newTimeUUID(), UUIDGenerator.newTimeUUID(), "test" );
        Entity newEntity = new Entity( "test" );

        CollectionManager manager = factory.createCollectionManager( context );

        Entity createReturned = manager.create( newEntity );


        assertNotNull( "Id was assigned", createReturned.getUuid() );

        Entity loadReturned = manager.load( createReturned.getUuid() );

        assertEquals( "Same value", createReturned, loadReturned );
    }


    @Test
    public void createLoadDelete() {

        CollectionContext context =
                new CollectionContextImpl( UUIDGenerator.newTimeUUID(), UUIDGenerator.newTimeUUID(), "test" );
        Entity newEntity = new Entity( "test" );

        CollectionManager manager = factory.createCollectionManager( context );

        Entity createReturned = manager.create( newEntity );


        assertNotNull( "Id was assigned", createReturned.getUuid() );

        Entity loadReturned = manager.load( createReturned.getUuid() );

        assertEquals( "Same value", createReturned, loadReturned );

        manager.delete( createReturned.getUuid() );

        loadReturned = manager.load( createReturned.getUuid() );

        assertNull( "Entity was deleted", loadReturned );
    }


    @Test
    public void createLoadUpdateLoad() {

        CollectionContext context =
                new CollectionContextImpl( UUIDGenerator.newTimeUUID(), UUIDGenerator.newTimeUUID(), "test" );

        Entity newEntity = new Entity( "test" );
        newEntity.setField( new IntegerField( "counter", 1 ) );

        CollectionManager manager = factory.createCollectionManager( context );

        Entity createReturned = manager.create( newEntity );


        assertNotNull( "Id was assigned", createReturned.getUuid() );

        Entity loadReturned = manager.load( createReturned.getUuid() );

        assertEquals( "Same value", createReturned, loadReturned );


        assertEquals("Field value correct", createReturned.getField( "counter" ), loadReturned.getField( "counter" ));


        //update the field to 2
        createReturned.setField( new IntegerField( "counter", 2 ) );

        manager.update( createReturned );

        loadReturned = manager.load( createReturned.getUuid() );

        assertEquals( "Same value", createReturned, loadReturned );


        assertEquals("Field value correct", createReturned.getField( "counter" ), loadReturned.getField( "counter" ));
    }
}
