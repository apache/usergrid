package org.apache.usergrid.persistence.collection;


import org.junit.Test;

import org.apache.usergrid.persistence.collection.impl.CollectionScopeImpl;
import org.apache.usergrid.persistence.model.entity.SimpleId;

import static junit.framework.TestCase.assertEquals;


/** @author tnine */
public class CollectionContextImplTest {


    @Test( expected = NullPointerException.class )
    public void orgIdrequired() {
        new CollectionScopeImpl( null, new SimpleId( "test" ), "test" );
    }


    @Test( expected = NullPointerException.class )
    public void ownerIdRequired() {
        new CollectionScopeImpl( new SimpleId( "organization" ), null, "test" );
    }


    @Test( expected = NullPointerException.class )
    public void collectionRequired() {
        new CollectionScopeImpl( new SimpleId( "organization" ), new SimpleId( "test" ), null );
    }


    @Test( expected = IllegalArgumentException.class )
    public void collectionRequiredLength() {
        new CollectionScopeImpl( new SimpleId( "organization" ), new SimpleId( "test" ), "" );
    }


    @Test
    public void correctValues() {
        final SimpleId ownerId = new SimpleId( "test" );

        final String collection = "tests";

        CollectionScopeImpl context = new CollectionScopeImpl( new SimpleId( "organization" ), ownerId, collection );

        assertEquals( ownerId, context.getOwner() );
        assertEquals( collection, context.getName() );
    }


}
