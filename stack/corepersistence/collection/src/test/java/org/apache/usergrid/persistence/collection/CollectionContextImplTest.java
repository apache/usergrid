package org.apache.usergrid.persistence.collection;


import java.util.UUID;

import org.junit.Test;

import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import static junit.framework.TestCase.assertEquals;


/** @author tnine */
public class CollectionContextImplTest {

    @Test( expected = NullPointerException.class )
    public void appIdRequired() {
        new CollectionContextImpl( null, UUIDGenerator.newTimeUUID(), "test" );
    }


    @Test( expected = NullPointerException.class )
    public void ownerIdRequired() {
        new CollectionContextImpl( UUIDGenerator.newTimeUUID(), null, "test" );
    }


    @Test( expected = NullPointerException.class )
    public void collectionRequired() {
        new CollectionContextImpl( UUIDGenerator.newTimeUUID(), UUIDGenerator.newTimeUUID(), null );
    }


    @Test( expected = IllegalArgumentException.class )
    public void collectionRequiredLength() {
        new CollectionContextImpl( UUIDGenerator.newTimeUUID(), UUIDGenerator.newTimeUUID(), "" );
    }


    @Test
    public void correctValues() {
        final UUID appId = UUIDGenerator.newTimeUUID();
        final UUID ownerId = UUIDGenerator.newTimeUUID();

        final String collection = "tests";

        CollectionContextImpl context = new CollectionContextImpl( appId, ownerId, collection );

        assertEquals( appId, context.getApplication() );
        assertEquals( ownerId, context.getOwner() );
        assertEquals( collection, context.getName() );
    }
}
