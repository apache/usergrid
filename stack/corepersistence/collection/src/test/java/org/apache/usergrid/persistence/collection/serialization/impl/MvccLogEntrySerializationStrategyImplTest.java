package org.apache.usergrid.persistence.collection.serialization.impl;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Rule;
import org.junit.Test;

import org.apache.usergrid.persistence.collection.CollectionContext;
import org.apache.usergrid.persistence.collection.impl.CollectionContextImpl;
import org.apache.usergrid.persistence.collection.guice.TestCollectionModule;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccLogEntry;
import org.apache.usergrid.persistence.collection.mvcc.entity.impl.MvccLogEntryImpl;
import org.apache.usergrid.persistence.collection.mvcc.entity.Stage;
import org.apache.usergrid.persistence.collection.serialization.MvccLogEntrySerializationStrategy;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;
import org.apache.usergrid.persistence.test.CassandraRule;

import com.google.guiceberry.GuiceBerryEnvSelector;
import com.google.guiceberry.TestDescription;
import com.google.guiceberry.junit4.GuiceBerryRule;
import com.google.inject.Inject;
import com.google.inject.Module;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;


/** @author tnine */
public class MvccLogEntrySerializationStrategyImplTest {


    /** Set our timeout to 1 seconds.  If it works for 1 seconds, we'll be good a any value */
    private static final int TIMEOUT = 1;


    @Rule
    public final GuiceBerryRule guiceBerry = new GuiceBerryRule( new TimeoutModMapper() );

    @Rule
    public final CassandraRule rule = new CassandraRule();

    @Inject
    private MvccLogEntrySerializationStrategy logEntryStrategy;


    @Test
    public void createAndDelete() throws ConnectionException {

        final UUID applicationId = UUIDGenerator.newTimeUUID();
        final String name = "test";


        CollectionContext context = new CollectionContextImpl( applicationId, applicationId, name );


        final UUID uuid = UUIDGenerator.newTimeUUID();
        final UUID version = UUIDGenerator.newTimeUUID();

        for ( Stage stage : Stage.values() ) {
            MvccLogEntry saved = new MvccLogEntryImpl(  uuid, version, stage );
            logEntryStrategy.write( context, saved ).execute();

            //Read it back

            MvccLogEntry returned = logEntryStrategy.load( context, uuid, version );

            assertNotNull( "Returned value should not be null", returned );

            assertEquals( "Returned should equal the saved", saved, returned );
        }
    }


    @Test
    public void loadNoData() throws ConnectionException {

        final UUID applicationId = UUIDGenerator.newTimeUUID();
        final String name = "test";


        CollectionContext context = new CollectionContextImpl( applicationId, applicationId, name );


        final UUID uuid = UUIDGenerator.newTimeUUID();
        final UUID version = UUIDGenerator.newTimeUUID();


        MvccLogEntry returned = logEntryStrategy.load( context, uuid, version );

        assertNull( "Returned value should not exist", returned );
    }


    @Test
    public void getMultipleEntries() throws ConnectionException {

        final UUID applicationId = UUIDGenerator.newTimeUUID();
        final String name = "test";


        CollectionContext context = new CollectionContextImpl( applicationId, applicationId, name );


        final UUID uuid = UUIDGenerator.newTimeUUID();

        int count = 10;

        final UUID[] versions = new UUID[count];
        final Stage COMPLETE = Stage.COMPLETE;
        final MvccLogEntry[] entries = new MvccLogEntry[count];


        for ( int i = 0; i < count; i++ ) {
            versions[i] = UUIDGenerator.newTimeUUID();

            entries[i] = new MvccLogEntryImpl( uuid, versions[i], COMPLETE );
            logEntryStrategy.write( context,  entries[i] ).execute();

            //Read it back

            MvccLogEntry returned = logEntryStrategy.load( context, uuid, versions[i] );

            assertNotNull( "Returned value should not be null", returned );

            assertEquals( "Returned should equal the saved", entries[i], returned );
        }

        //now do a range scan from the end

        List<MvccLogEntry> results = logEntryStrategy.load( context, uuid, versions[count - 1], count );

        assertEquals( count, results.size() );

        for ( int i = 0; i < count; i++ ) {
            final MvccLogEntry saved = entries[count - i - 1];
            final MvccLogEntry returned = results.get( i );

            assertEquals( "Entry was not equal to the saved value", saved, returned );
        }

        //now delete them all and ensure we get no results back
        for ( int i = 0; i < count; i++ ) {
            logEntryStrategy.delete( context, uuid, versions[i] ).execute();
        }

        results = logEntryStrategy.load( context, uuid, versions[versions.length - 1], versions.length );

        assertEquals( "All log entries were deleted", 0, results.size() );
    }


    @Test
    public void transientTimeout() throws ConnectionException, InterruptedException {

        final UUID applicationId = UUIDGenerator.newTimeUUID();
        final String name = "test";


        CollectionContext context = new CollectionContextImpl( applicationId, applicationId, name );


        final UUID uuid = UUIDGenerator.newTimeUUID();
        final UUID version = UUIDGenerator.newTimeUUID();

        for ( Stage stage : Stage.values() ) {

            MvccLogEntry saved = new MvccLogEntryImpl( uuid, version, stage );
            logEntryStrategy.write(context,  saved ).execute();

            //Read it back after the timeout

            Thread.sleep( TIMEOUT * 1000 );

            MvccLogEntry returned = logEntryStrategy.load( context, uuid, version );


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
        logEntryStrategy.write( null, mock(MvccLogEntry.class) );
    }


    @Test(expected = NullPointerException.class)
    public void writeParams() throws ConnectionException {
        logEntryStrategy.write( mock(CollectionContext.class), null );
    }


    @Test(expected = NullPointerException.class)
    public void deleteParamContext() throws ConnectionException {
        logEntryStrategy.delete( null, UUIDGenerator.newTimeUUID(), UUIDGenerator.newTimeUUID() );
    }


    @Test(expected = NullPointerException.class)
    public void deleteParamEntityId() throws ConnectionException {

        logEntryStrategy
                .delete( new CollectionContextImpl( UUIDGenerator.newTimeUUID(), UUIDGenerator.newTimeUUID(), "test" ),
                        null, UUIDGenerator.newTimeUUID() );
    }


    @Test(expected = NullPointerException.class)
    public void deleteParamVersion() throws ConnectionException {

        logEntryStrategy
                .delete( new CollectionContextImpl( UUIDGenerator.newTimeUUID(), UUIDGenerator.newTimeUUID(), "test" ),
                        UUIDGenerator.newTimeUUID(), null );
    }


    @Test(expected = NullPointerException.class)
    public void loadParamContext() throws ConnectionException {
        logEntryStrategy.load( null, UUIDGenerator.newTimeUUID(), UUIDGenerator.newTimeUUID() );
    }


    @Test(expected = NullPointerException.class)
    public void loadParamEntityId() throws ConnectionException {

        logEntryStrategy
                .load( new CollectionContextImpl( UUIDGenerator.newTimeUUID(), UUIDGenerator.newTimeUUID(), "test" ),
                        null, UUIDGenerator.newTimeUUID() );
    }


    @Test(expected = NullPointerException.class)
    public void loadParamVersion() throws ConnectionException {

        logEntryStrategy
                .load( new CollectionContextImpl( UUIDGenerator.newTimeUUID(), UUIDGenerator.newTimeUUID(), "test" ),
                        UUIDGenerator.newTimeUUID(), null );
    }


    @Test(expected = NullPointerException.class)
    public void loadListParamContext() throws ConnectionException {
        logEntryStrategy.load( null, UUIDGenerator.newTimeUUID(), UUIDGenerator.newTimeUUID(), 1 );
    }


    @Test(expected = NullPointerException.class)
    public void loadListParamEntityId() throws ConnectionException {

        logEntryStrategy
                .load( new CollectionContextImpl( UUIDGenerator.newTimeUUID(), UUIDGenerator.newTimeUUID(), "test" ),
                        null, UUIDGenerator.newTimeUUID(), 1 );
    }


    @Test(expected = NullPointerException.class)
    public void loadListParamVersion() throws ConnectionException {

        logEntryStrategy
                .load( new CollectionContextImpl( UUIDGenerator.newTimeUUID(), UUIDGenerator.newTimeUUID(), "test" ),
                        UUIDGenerator.newTimeUUID(), null, 1 );
    }


    @Test(expected = IllegalArgumentException.class)
    public void loadListParamSize() throws ConnectionException {

        logEntryStrategy
                .load( new CollectionContextImpl( UUIDGenerator.newTimeUUID(), UUIDGenerator.newTimeUUID(), "test" ),
                        UUIDGenerator.newTimeUUID(), UUIDGenerator.newTimeUUID(), 0 );
    }


    /** Mapper that will change which module we implement based on the test case */
    public static class TimeoutModMapper implements GuiceBerryEnvSelector {

        @Override
        public Class<? extends Module> guiceBerryEnvToUse( final TestDescription testDescription ) {

            //in this edge case, we want to truncate the timeout to 1 second for this test, override the env to use
            //this module setup
            if ( ( MvccLogEntrySerializationStrategyImplTest.class.getName() + ".transientTimeout" )
                    .equals( testDescription.getName() ) ) {
                return TimeoutEnv.class;
            }

            //by default, we wnat to run the TestCollectionModule
            return TestCollectionModule.class;
        }
    }


    public static class TimeoutEnv extends TestCollectionModule {

        @Override
        public Map<String, String> getOverrides() {
            Map<String, String> timeout = new HashMap<String, String>();
            timeout.put( MvccLogEntrySerializationStrategyImpl.TIMEOUT_PROP, TIMEOUT + "" );
            return timeout;
        }
    }
}

