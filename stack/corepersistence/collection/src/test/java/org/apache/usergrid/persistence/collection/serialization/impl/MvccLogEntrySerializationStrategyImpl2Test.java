package org.apache.usergrid.persistence.collection.serialization.impl;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jukito.JukitoRunner;
import org.jukito.TestModule;
import org.jukito.UseModules;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.guice.CassandraRule;
import org.apache.usergrid.persistence.collection.guice.MigrationManagerRule;
import org.apache.usergrid.persistence.collection.guice.TestCollectionModule;
import org.apache.usergrid.persistence.collection.impl.CollectionScopeImpl;
import org.apache.usergrid.persistence.collection.mvcc.MvccLogEntrySerializationStrategy;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccLogEntry;
import org.apache.usergrid.persistence.collection.mvcc.entity.Stage;
import org.apache.usergrid.persistence.collection.mvcc.entity.impl.MvccLogEntryImpl;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;


/** @author tnine */
@RunWith( JukitoRunner.class )
public class MvccLogEntrySerializationStrategyImpl2Test {


    /** Set our timeout to 1 second.  If it works for 1 seconds, we'll be good a any value */
    private static final int TIMEOUT = 1;


    @Inject
    private MvccLogEntrySerializationStrategy logEntryStrategy;


    @ClassRule
    public static CassandraRule rule = new CassandraRule();


    @Inject
    @Rule
    public MigrationManagerRule migrationManagerRule;


    @Test
    public void transientTimeout() throws ConnectionException, InterruptedException {
        final Id organizationId = new SimpleId( "organization" );
        final Id applicationId = new SimpleId( "application" );
        final String name = "test";


        CollectionScope context = new CollectionScopeImpl(organizationId, applicationId, name );


        final SimpleId id = new SimpleId( "test" );
        final UUID version = UUIDGenerator.newTimeUUID();

        for ( Stage stage : Stage.values() ) {
            MvccLogEntry saved = new MvccLogEntryImpl( id, version, stage );
            logEntryStrategy.write( context, saved ).execute();

            //Read it back after the timeout

            //noinspection PointlessArithmeticExpression
            Thread.sleep( TIMEOUT * 1000 );

            MvccLogEntry returned = logEntryStrategy.load( context, id, version );


            if ( stage.isTransient() ) {
                assertNull( "Active is transient and should time out", returned );
            }
            else {
                assertNotNull( "Committed is not transient and should be returned", returned );
                assertEquals( "Returned should equal the saved", saved, returned );
            }
        }
    }


    public static class TimeoutEnv extends TestModule {
        @Override
        protected void configureTest() {

            //override the timeout property
            Map<String, String> overrides = new HashMap<String, String>();
            overrides.put( MvccLogEntrySerializationStrategyImpl.TIMEOUT_PROP, String.valueOf( TIMEOUT ) );

            //use the default module with cass
            install( new TestCollectionModule( overrides ) );
        }
    }
}

