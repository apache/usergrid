package org.apache.usergrid.persistence.collection;


import org.junit.Rule;
import org.junit.Test;

import org.apache.usergrid.persistence.collection.guice.CassandraTestCollectionModule;
import org.apache.usergrid.persistence.collection.impl.EntityCollectionImpl;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.field.IntegerField;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;
import org.apache.usergrid.persistence.test.CassandraRule;

import com.google.common.eventbus.EventBus;
import com.google.guiceberry.junit4.GuiceBerryRule;
import com.google.inject.Inject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;


/** @author tnine */
public class CollectionManagerIT {
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

        EntityCollection context =
                new EntityCollectionImpl( new SimpleId( "test" ), "test" );


        Entity newEntity = new Entity( new SimpleId( "test" ) );

        EntityCollectionManager manager = factory.createCollectionManager( context );

        Entity returned = manager.write( newEntity );

        assertNotNull( "Returned has a uuid", returned.getId() );
        assertEquals( "Version matches uuid for create", returned.getVersion(), returned.getVersion() );


    }


    @Test
    public void writeAndLoad() {

        EntityCollection context =
                new EntityCollectionImpl( new SimpleId( "test" ), "test" );
        Entity newEntity = new Entity( new SimpleId( "test") );

        EntityCollectionManager manager = factory.createCollectionManager( context );

        Entity createReturned = manager.write( newEntity );


        assertNotNull( "Id was assigned", createReturned.getId() );

        Entity loadReturned = manager.load( createReturned.getId() );

        assertEquals( "Same value", createReturned, loadReturned );
    }


    @Test
    public void writeLoadDelete() {

        EntityCollection context =
                new EntityCollectionImpl(  new SimpleId( "test" ), "test" );
        Entity newEntity = new Entity( new SimpleId("test") );

        EntityCollectionManager manager = factory.createCollectionManager( context );

        Entity createReturned = manager.write( newEntity );


        assertNotNull( "Id was assigned", createReturned.getId() );

        Entity loadReturned = manager.load( createReturned.getId() );

        assertEquals( "Same value", createReturned, loadReturned );

        manager.delete( createReturned.getId() );

        loadReturned = manager.load( createReturned.getId() );

        assertNull( "Entity was deleted", loadReturned );
    }


    @Test
    public void writeLoadUpdateLoad() {

        EntityCollection context =
                new EntityCollectionImpl(new SimpleId( "test" ), "test" );

        Entity newEntity = new Entity( new SimpleId( "test") );
        newEntity.setField( new IntegerField( "counter", 1 ) );

        EntityCollectionManager manager = factory.createCollectionManager( context );

        Entity createReturned = manager.write( newEntity );


        assertNotNull( "Id was assigned", createReturned.getId() );

        Entity loadReturned = manager.load( createReturned.getId() );

        assertEquals( "Same value", createReturned, loadReturned );


        assertEquals("Field value correct", createReturned.getField( "counter" ), loadReturned.getField( "counter" ));


        //update the field to 2
        createReturned.setField( new IntegerField( "counter", 2 ) );

        manager.write( createReturned );

        loadReturned = manager.load( createReturned.getId() );

        assertEquals( "Same value", createReturned, loadReturned );


        assertEquals("Field value correct", createReturned.getField( "counter" ), loadReturned.getField( "counter" ));
    }


}
