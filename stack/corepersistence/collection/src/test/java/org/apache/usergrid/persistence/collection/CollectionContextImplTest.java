package org.apache.usergrid.persistence.collection;


import java.util.UUID;

import org.junit.Test;

import org.apache.usergrid.persistence.collection.impl.EntityCollectionImpl;
import org.apache.usergrid.persistence.model.entity.SimpleId;
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
        new EntityCollectionImpl(new SimpleId( "test" ), null );
    }


    @Test( expected = IllegalArgumentException.class )
    public void collectionRequiredLength() {
        new EntityCollectionImpl(new SimpleId( "test" ), "" );
    }


    @Test
    public void correctValues() {
        final SimpleId ownerId = new SimpleId( "test" );

        final String collection = "tests";

        EntityCollectionImpl context = new EntityCollectionImpl(ownerId, collection );

        assertEquals( ownerId, context.getOwner() );
        assertEquals( collection, context.getName() );
    }
}
