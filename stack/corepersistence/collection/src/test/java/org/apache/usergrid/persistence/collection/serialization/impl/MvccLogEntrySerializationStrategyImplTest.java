package org.apache.usergrid.persistence.collection.serialization.impl;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jukito.JukitoRunner;
import org.jukito.UseModules;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.guice.TestCollectionModule;
import org.apache.usergrid.persistence.collection.impl.CollectionScopeImpl;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccLogEntry;
import org.apache.usergrid.persistence.collection.mvcc.entity.Stage;
import org.apache.usergrid.persistence.collection.mvcc.entity.impl.MvccLogEntryImpl;
import org.apache.usergrid.persistence.collection.mvcc.MvccLogEntrySerializationStrategy;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;
import org.apache.usergrid.persistence.collection.guice.MigrationManagerRule;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;


/** @author tnine */
@RunWith( JukitoRunner.class )
@UseModules( { TestCollectionModule.class } )
public class MvccLogEntrySerializationStrategyImplTest {


    /** Set our timeout to 1 seconds.  If it works for 1 seconds, we'll be good a any value */
    private static final int TIMEOUT = 1;


    @Rule
    public final MigrationManagerRule rule = new MigrationManagerRule();

    @Inject
    private MvccLogEntrySerializationStrategy logEntryStrategy;


    @Test
    public void createAndDelete() throws ConnectionException {

        final Id organizationId = new SimpleId( "organization" );
        final Id applicationId = new SimpleId( "application" );
        final String name = "test";


        CollectionScope context = new CollectionScopeImpl(organizationId, applicationId, name );


        final SimpleId id = new SimpleId( "test" );
        final UUID version = UUIDGenerator.newTimeUUID();

        for ( Stage stage : Stage.values() ) {
            MvccLogEntry saved = new MvccLogEntryImpl( id, version, stage );
            logEntryStrategy.write( context, saved ).execute();

            //Read it back

            MvccLogEntry returned = logEntryStrategy.load( context, id, version );

            assertNotNull( "Returned value should not be null", returned );

            assertEquals( "Returned should equal the saved", saved, returned );
        }
    }


    @Test
    public void loadNoData() throws ConnectionException {

        final Id organizationId = new SimpleId( "organization" );
        final Id applicationId = new SimpleId( "application" );
        final String name = "test";


        CollectionScope context = new CollectionScopeImpl(organizationId, applicationId, name );


        final SimpleId id = new SimpleId( "test" );
        final UUID version = UUIDGenerator.newTimeUUID();


        MvccLogEntry returned = logEntryStrategy.load( context, id, version );

        assertNull( "Returned value should not exist", returned );
    }


    @Test
    public void getMultipleEntries() throws ConnectionException {

        final Id organizationId = new SimpleId( "organization" );
        final Id applicationId = new SimpleId( "application" );
        final String name = "test";


        CollectionScope context = new CollectionScopeImpl(organizationId, applicationId, name );


        final SimpleId id = new SimpleId( "test" );

        int count = 10;

        final UUID[] versions = new UUID[count];
        final Stage COMPLETE = Stage.COMPLETE;
        final MvccLogEntry[] entries = new MvccLogEntry[count];


        for ( int i = 0; i < count; i++ ) {
            versions[i] = UUIDGenerator.newTimeUUID();

            entries[i] = new MvccLogEntryImpl( id, versions[i], COMPLETE );
            logEntryStrategy.write( context, entries[i] ).execute();

            //Read it back

            MvccLogEntry returned = logEntryStrategy.load( context, id, versions[i] );

            assertNotNull( "Returned value should not be null", returned );

            assertEquals( "Returned should equal the saved", entries[i], returned );
        }

        //now do a range scan from the end

        List<MvccLogEntry> results = logEntryStrategy.load( context, id, versions[count - 1], count );

        assertEquals( count, results.size() );

        for ( int i = 0; i < count; i++ ) {
            final MvccLogEntry saved = entries[count - i - 1];
            final MvccLogEntry returned = results.get( i );

            assertEquals( "Entry was not equal to the saved value", saved, returned );
        }

        //now delete them all and ensure we get no results back
        for ( int i = 0; i < count; i++ ) {
            logEntryStrategy.delete( context, id, versions[i] ).execute();
        }

        results = logEntryStrategy.load( context, id, versions[versions.length - 1], versions.length );

        assertEquals( "All log entries were deleted", 0, results.size() );
    }


    @Test
    @UseModules( TimeoutEnv.class )
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


    @Test(expected = NullPointerException.class)
    public void writeParamsNoContext() throws ConnectionException {
        logEntryStrategy.write( null, mock( MvccLogEntry.class ) );
    }


    @Test(expected = NullPointerException.class)
    public void writeParams() throws ConnectionException {
        logEntryStrategy.write( mock( CollectionScope.class ), null );
    }


    @Test(expected = NullPointerException.class)
    public void deleteParamContext() throws ConnectionException {
        logEntryStrategy.delete( null, new SimpleId( "test" ), UUIDGenerator.newTimeUUID() );
    }


    @Test(expected = NullPointerException.class)
    public void deleteParamEntityId() throws ConnectionException {

        logEntryStrategy.delete( new CollectionScopeImpl(new SimpleId( "organization" ), new SimpleId( "test" ), "test" ), null,
                UUIDGenerator.newTimeUUID() );
    }


    @Test(expected = NullPointerException.class)
    public void deleteParamVersion() throws ConnectionException {

        logEntryStrategy
                .delete( new CollectionScopeImpl(new SimpleId( "organization" ), new SimpleId( "test" ), "test" ), new SimpleId( "test" ), null );
    }


    @Test(expected = NullPointerException.class)
    public void loadParamContext() throws ConnectionException {
        logEntryStrategy.load( null, new SimpleId( "test" ), UUIDGenerator.newTimeUUID() );
    }


    @Test(expected = NullPointerException.class)
    public void loadParamEntityId() throws ConnectionException {

        logEntryStrategy
                .load( new CollectionScopeImpl(new SimpleId( "organization" ), new SimpleId( "test" ), "test" ), null, UUIDGenerator.newTimeUUID() );
    }


    @Test(expected = NullPointerException.class)
    public void loadParamVersion() throws ConnectionException {

        logEntryStrategy
                .load( new CollectionScopeImpl(new SimpleId( "organization" ), new SimpleId( "test" ), "test" ), new SimpleId( "test" ), null );
    }


    @Test(expected = NullPointerException.class)
    public void loadListParamContext() throws ConnectionException {
        logEntryStrategy.load( null, new SimpleId( "test" ), UUIDGenerator.newTimeUUID(), 1 );
    }


    @Test(expected = NullPointerException.class)
    public void loadListParamEntityId() throws ConnectionException {

        logEntryStrategy
                .load( new CollectionScopeImpl(new SimpleId( "organization" ), new SimpleId( "test" ), "test" ), null, UUIDGenerator.newTimeUUID(),
                        1 );
    }


    @Test(expected = NullPointerException.class)
    public void loadListParamVersion() throws ConnectionException {

        logEntryStrategy
                .load( new CollectionScopeImpl(new SimpleId( "organization" ), new SimpleId( "test" ), "test" ), new SimpleId( "test" ), null, 1 );
    }


    @Test(expected = IllegalArgumentException.class)
    public void loadListParamSize() throws ConnectionException {

        logEntryStrategy.load( new CollectionScopeImpl(new SimpleId( "organization" ), new SimpleId( "test" ), "test" ), new SimpleId( "test" ),
                UUIDGenerator.newTimeUUID(), 0 );
    }


    public static class TimeoutEnv extends AbstractModule {

        @Override
        protected void configure() {

            //override the timeout property
            Map<String, String> timeout = new HashMap<String, String>();
            timeout.put( MvccLogEntrySerializationStrategyImpl.TIMEOUT_PROP, String.valueOf( TIMEOUT ) );

            //use the default module with cass
            install( new TestCollectionModule( timeout ) );
        }
    }
}

