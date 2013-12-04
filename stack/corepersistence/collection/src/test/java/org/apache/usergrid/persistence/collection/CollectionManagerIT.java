package org.apache.usergrid.persistence.collection;


import org.junit.Rule;
import org.junit.Test;

import org.apache.usergrid.persistence.collection.guice.CassandraTestCollectionModule;
import org.apache.usergrid.persistence.collection.guice.TestCollectionModule;
import org.apache.usergrid.persistence.collection.impl.CollectionContextImpl;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;
import org.apache.usergrid.persistence.test.CassandraRule;

import com.google.guiceberry.junit4.GuiceBerryRule;
import com.google.inject.Inject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.*;


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

        CollectionContext context = new CollectionContextImpl( UUIDGenerator.newTimeUUID(), UUIDGenerator.newTimeUUID(), "test");
        Entity newEntity = new Entity("test");

        CollectionManager manager = factory.createCollectionManager(context);

        Entity returned = manager.create( newEntity );

        assertNotNull("Returned has a uuid", returned.getUuid());
        assertEquals("Version matches uuid for create", returned.getUuid(), returned.getVersion());

        assertTrue("Created time was set", returned.getCreated() > 0);
        assertEquals("Created and updated time match on create", returned.getCreated(), returned.getUpdated());

    }
}
