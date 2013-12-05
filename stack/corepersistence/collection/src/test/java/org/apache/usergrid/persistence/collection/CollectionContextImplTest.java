package org.apache.usergrid.persistence.collection;


import java.util.UUID;

import org.junit.Test;

import org.apache.usergrid.persistence.collection.impl.EntityCollectionImpl;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import static junit.framework.TestCase.assertEquals;


/** @author tnine */
public class CollectionContextImplTest {


    @Test( expected = NullPointerException.class )
    public void ownerIdRequired() {
        new EntityCollectionImpl( null, "test" );
    }


    @Test( expected = NullPointerException.class )
    public void collectionRequired() {
        new EntityCollectionImpl( UUIDGenerator.newTimeUUID(), null );
    }


    @Test( expected = IllegalArgumentException.class )
    public void collectionRequiredLength() {
        new EntityCollectionImpl( UUIDGenerator.newTimeUUID(), "" );
    }


    @Test
    public void correctValues() {
        final UUID ownerId = UUIDGenerator.newTimeUUID();

        final String collection = "tests";

        EntityCollectionImpl context = new EntityCollectionImpl(ownerId, collection );

        assertEquals( ownerId, context.getOwner() );
        assertEquals( collection, context.getName() );
    }
}
