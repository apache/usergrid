package org.apache.usergrid.persistence.collection.guice;


import java.io.File;
import java.io.IOException;

import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.cassandra.io.util.FileUtils;

import org.apache.usergrid.persistence.collection.migration.MigrationManager;
import org.apache.usergrid.persistence.test.AvailablePortFinder;

import com.google.common.io.Files;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.netflix.astyanax.test.EmbeddedCassandra;


/**
 * @TODO - I wanted this in the test module but unfortunately that will create a circular dep
 *         due to the inclusion of the MigrationManager
 */
public class MigrationManagerRule extends ExternalResource {
    private static final Logger LOG = LoggerFactory.getLogger( MigrationManagerRule.class );

    public static final int THRIFT_PORT = AvailablePortFinder.getNextAvailable();
    public static final int GOSSIP_PORT = AvailablePortFinder.getNextAvailable();

    private final Object mutex = new Object();

    private EmbeddedCassandra cass;

    private boolean started = false;

    private MigrationManager migrationManager;


    @Override
    protected void before() throws Throwable {

        Injector injector = Guice.createInjector( new TestCollectionModule() );
        migrationManager = injector.getInstance( MigrationManager.class );

        if ( migrationManager == null ) {
            LOG.error( "The migrationManager is NULL: blowing chunks" );
            throw new NullPointerException( "The migration manager was not injected." );
        }

        if ( started ) {
            return;
        }

        synchronized ( mutex ) {

            //we're late to the party, bail
            if ( started ) {
                return;
            }


            File dataDir = Files.createTempDir();
            dataDir.deleteOnExit();



            //cleanup before we run, shouldn't be necessary, but had the directory exist during JVM kill
            if( dataDir.exists() ) {
                FileUtils.deleteRecursive( dataDir );
            }

            try {
                LOG.info( "Starting cassandra" );

                cass = new EmbeddedCassandra( dataDir, "Usergrid", THRIFT_PORT, GOSSIP_PORT );
                cass.start();

                LOG.info( "Cassandra started" );
                migrationManager.migrate();

                started = true;
            }
            catch ( IOException e ) {
                throw new RuntimeException( "Unable to start cassandra", e );
            }
        }
    }


    @Override
    protected void after() {

        //TODO TN.  this should only really happen when we shut down
//        cass.stop();
    }
}
