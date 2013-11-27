package org.apache.usergrid.persistence.collection.serialization;


import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Rule;
import org.junit.Test;

import org.apache.usergrid.persistence.collection.CollectionContext;
import org.apache.usergrid.persistence.collection.CollectionContextImpl;
import org.apache.usergrid.persistence.collection.guice.TestCollectionModule;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccLogEntry;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccLogEntryImpl;
import org.apache.usergrid.persistence.collection.mvcc.entity.Stage;
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


/**
 * @author tnine
 */
public class MvccLogEntrySerializationStrategyImplTest {


    /**
     * Set our timeout to 1 seconds.  If it works for 1 seconds, we'll be good a any value
     */
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
            MvccLogEntry saved = new MvccLogEntryImpl( context, uuid, version, stage );
            logEntryStrategy.write( saved ).execute();

            //Read it back

            MvccLogEntry returned = logEntryStrategy.load( context, uuid, version );

            assertNotNull( "Returned value should not be null", returned );

            assertEquals( "Returned should equal the saved", saved, returned );
        }
    }


    @Test
    public void transientTimeout() throws ConnectionException, InterruptedException {

        final UUID applicationId = UUIDGenerator.newTimeUUID();
        final String name = "test";


        CollectionContext context = new CollectionContextImpl( applicationId, applicationId, name );


        final UUID uuid = UUIDGenerator.newTimeUUID();
        final UUID version = UUIDGenerator.newTimeUUID();

        for ( Stage stage : Stage.values() ) {

            MvccLogEntry saved = new MvccLogEntryImpl( context, uuid, version, stage );
            logEntryStrategy.write( saved ).execute();

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


    /**
     * Mapper that will change which module we implement based on the test case
     */
    public static class TimeoutModMapper implements GuiceBerryEnvSelector {

        @Override
        public Class<? extends Module> guiceBerryEnvToUse( final TestDescription testDescription ) {

            //in this edge case, we want to truncate the timeout to 1 second for this test, override the env to use
            //this module setup
            if ( (MvccLogEntrySerializationStrategyImplTest.class.getName()+".transientTimeout").equals( testDescription.getName() ) ) {
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

