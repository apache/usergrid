package org.apache.usergrid.persistence.collection;


import org.junit.Test;

import org.apache.usergrid.persistence.collection.impl.ScopeImpl;
import org.apache.usergrid.persistence.model.entity.SimpleId;

import static junit.framework.TestCase.assertEquals;


/** @author tnine */
public class CollectionContextImplTest {


    @Test(expected = NullPointerException.class)
    public void ownerIdRequired() {
        new ScopeImpl( null, "test" );
    }


    @Test(expected = NullPointerException.class)
    public void collectionRequired() {
        new ScopeImpl( new SimpleId( "test" ), null );
    }


    @Test(expected = IllegalArgumentException.class)
    public void collectionRequiredLength() {
        new ScopeImpl( new SimpleId( "test" ), "" );
    }


    @Test
    public void correctValues() {
        final SimpleId ownerId = new SimpleId( "test" );

        final String collection = "tests";

        ScopeImpl context = new ScopeImpl( ownerId, collection );

        assertEquals( ownerId, context.getOwner() );
        assertEquals( collection, context.getName() );
    }
}
