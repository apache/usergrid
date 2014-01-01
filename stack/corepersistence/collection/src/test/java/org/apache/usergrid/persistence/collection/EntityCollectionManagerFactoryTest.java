package org.apache.usergrid.persistence.collection;


import org.jukito.JukitoModule;
import org.jukito.JukitoRunner;
import org.jukito.UseModules;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.usergrid.persistence.collection.cassandra.CassandraRule;
import org.apache.usergrid.persistence.collection.guice.CollectionModule;
import org.apache.usergrid.persistence.collection.guice.MigrationManagerRule;
import org.apache.usergrid.persistence.collection.impl.CollectionScopeImpl;
import org.apache.usergrid.persistence.model.entity.SimpleId;

import com.google.inject.Inject;
import com.google.inject.ProvisionException;

import static org.junit.Assert.assertNotNull;


/**
 * Basic tests
 *
 * @author tnine
 */
@RunWith( JukitoRunner.class )
public class EntityCollectionManagerFactoryTest {
    @Inject
    private EntityCollectionManagerFactory entityCollectionManagerFactory;


    @ClassRule
    public static CassandraRule rule = new CassandraRule();


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


    @Test(expected = ProvisionException.class)
    public void nullInput() {
        EntityCollectionManager entityCollectionManager =
                entityCollectionManagerFactory.createCollectionManager( null );
    }


    public static class TestModule extends JukitoModule {

        @Override
        protected void configureTest() {
            install( new CollectionModule() );
        }
    }
}
