package org.apache.usergrid.persistence.collection.cassandra;


import org.apache.usergrid.persistence.collection.util.AvailablePortFinder;
import java.io.File;
import java.io.IOException;

import org.safehaus.guicyfig.Env;
import org.safehaus.guicyfig.EnvironResource;
import org.safehaus.guicyfig.GuicyFigModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.cassandra.io.util.FileUtils;

import org.apache.usergrid.persistence.collection.astyanax.CassandraFig;

import com.google.common.io.Files;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.netflix.astyanax.test.EmbeddedCassandra;


/**
 * @TODO - I wanted this in the test module but unfortunately that will create a circular dep
 *         due to the inclusion of the MigrationManager
 */
public class CassandraRule extends EnvironResource {
    private static final Logger LOG = LoggerFactory.getLogger( CassandraRule.class );

    private static final Object mutex = new Object();

    private static EmbeddedCassandra cass;

    private static boolean started = false;

    private final CassandraFig cassandraFig;


    public CassandraRule() {
        super( Env.UNIT );

        Injector injector = Guice.createInjector( new GuicyFigModule( CassandraFig.class ) );
        cassandraFig = injector.getInstance( CassandraFig.class );
    }


    public CassandraFig getCassandraFig() {
        return cassandraFig;
    }


    @Override
    protected void before() throws Throwable {

        if ( started ) {
            return;
        }

        synchronized ( mutex ) {

            File dataDir = Files.createTempDir();
            dataDir.deleteOnExit();

            //cleanup before we run, shouldn't be necessary, but had the directory exist during JVM kill
            if( dataDir.exists() ) {
                FileUtils.deleteRecursive( dataDir );
            }

            try {
                LOG.info( "Starting cassandra" );

                cass = new EmbeddedCassandra( dataDir, "Usergrid", cassandraFig.getThriftPort(),
                        AvailablePortFinder.getNextAvailable() );
                cass.start();

                LOG.info( "Cassandra boostrapped" );

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
