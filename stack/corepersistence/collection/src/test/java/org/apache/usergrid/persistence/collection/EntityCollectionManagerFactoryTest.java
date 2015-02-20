package org.apache.usergrid.persistence.collection;


import org.apache.usergrid.persistence.core.test.UseModules;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.usergrid.persistence.core.guice.MigrationManagerRule;
import org.apache.usergrid.persistence.collection.guice.TestCollectionModule;
import org.apache.usergrid.persistence.collection.impl.CollectionScopeImpl;
import org.apache.usergrid.persistence.core.test.ITRunner;
import org.apache.usergrid.persistence.model.entity.SimpleId;

import com.google.inject.Inject;
import com.google.inject.ProvisionException;

import static org.junit.Assert.assertNotNull;


/**
 * Basic tests
 *
 * @author tnine
 */
@RunWith( ITRunner.class )
@UseModules( TestCollectionModule.class )
public class EntityCollectionManagerFactoryTest {
    @Inject
    private EntityCollectionManagerFactory entityCollectionManagerFactory;


    @Inject
    @Rule
    public MigrationManagerRule migrationManagerRule;


    @Test
    public void validInput() {

        CollectionScopeImpl context = new CollectionScopeImpl(new SimpleId( "organization" ), new SimpleId( "test" ), "test" );

        EntityCollectionManager entityCollectionManager =
                entityCollectionManagerFactory.createCollectionManager( context );

        assertNotNull( "A collection manager must be returned", entityCollectionManager );
    }


    @Test(expected = NullPointerException.class)
    public void nullInput() {
        entityCollectionManagerFactory.createCollectionManager( null );
    }

}
