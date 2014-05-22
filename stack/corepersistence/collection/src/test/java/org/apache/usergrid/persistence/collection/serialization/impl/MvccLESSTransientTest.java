package org.apache.usergrid.persistence.collection.serialization.impl;


import java.util.UUID;

import org.jukito.UseModules;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.safehaus.guicyfig.Bypass;
import org.safehaus.guicyfig.Env;
import org.safehaus.guicyfig.Option;

import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.guice.MigrationManagerRule;
import org.apache.usergrid.persistence.collection.guice.TestCollectionModule;
import org.apache.usergrid.persistence.collection.impl.CollectionScopeImpl;
import org.apache.usergrid.persistence.collection.mvcc.MvccLogEntrySerializationStrategy;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccLogEntry;
import org.apache.usergrid.persistence.collection.mvcc.entity.Stage;
import org.apache.usergrid.persistence.collection.mvcc.entity.impl.MvccLogEntryImpl;
import org.apache.usergrid.persistence.collection.serialization.SerializationFig;
import org.apache.usergrid.persistence.core.cassandra.ITRunner;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.google.inject.Inject;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


/** @author tnine */
@RunWith( ITRunner.class )
@UseModules( TestCollectionModule.class )
public class MvccLESSTransientTest {
    @Inject
    @Bypass( environments = { Env.ALL, Env.UNIT }, options = @Option( method = "getTimeout", override = "1" ) )
    public SerializationFig serializationFig;


    @Inject
    private MvccLogEntrySerializationStrategy logEntryStrategy;


    @Inject
    @Rule
    public MigrationManagerRule migrationManagerRule;


    @Test
    public void transientTimeout() throws ConnectionException, InterruptedException {
        final Id organizationId = new SimpleId( "organization" );
        final Id applicationId = new SimpleId( "application" );
        final String name = "test";


        CollectionScope context = new CollectionScopeImpl( organizationId, applicationId, name );


        final SimpleId id = new SimpleId( "test" );
        final UUID version = UUIDGenerator.newTimeUUID();

        for ( Stage stage : Stage.values() ) {
            MvccLogEntry saved = new MvccLogEntryImpl( id, version, stage, MvccLogEntry.State.COMPLETE );
            logEntryStrategy.write( context, saved ).execute();

            //Read it back after the timeout

            //noinspection PointlessArithmeticExpression
            Thread.sleep( 1000 );

            MvccLogEntry returned = logEntryStrategy.load( context, id, version );


            if ( stage.isTransient() ) {
                assertNull( "Active is transient and should time out", returned );
            }
            else {
                assertNotNull( "Committed is not transient and should be returned", returned );
                assertEquals( "Returned should equal the saved", saved, returned );
            }
        }

        // null it out
        serializationFig.bypass( "getTimeout", null );
    }
}

