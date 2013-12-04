package org.apache.usergrid.persistence.collection;


import org.junit.Rule;
import org.junit.Test;

import org.apache.usergrid.persistence.collection.guice.TestCollectionModule;
import org.apache.usergrid.persistence.collection.impl.CollectionContextImpl;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.google.guiceberry.junit4.GuiceBerryRule;
import com.google.inject.Inject;
import com.google.inject.ProvisionException;

import static org.junit.Assert.assertNotNull;


/**
 * Basic tests
 *
 * @author tnine
 */
public class CollectionManagerFactoryTest {


    @Rule
    public final GuiceBerryRule guiceBerry = new GuiceBerryRule( TestCollectionModule.class );


    @Inject
    private CollectionManagerFactory collectionManagerFactory;




    @Test
    public void validInput() {

        CollectionContextImpl context =
                new CollectionContextImpl( UUIDGenerator.newTimeUUID(), UUIDGenerator.newTimeUUID(), "test" );

        CollectionManager collectionManager = collectionManagerFactory.createCollectionManager( context );

        assertNotNull( "A collection manager must be returned", collectionManager );
    }


    @Test( expected = ProvisionException.class )
    public void nullInput() {
           CollectionManager collectionManager = collectionManagerFactory.createCollectionManager( null );
    }
}
