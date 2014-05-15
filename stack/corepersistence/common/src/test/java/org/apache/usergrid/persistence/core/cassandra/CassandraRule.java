package org.apache.usergrid.persistence.core.cassandra;


import com.google.common.io.Files;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.netflix.astyanax.test.EmbeddedCassandra;
import java.io.File;
import java.io.IOException;
import org.apache.cassandra.io.util.FileUtils;
import org.apache.usergrid.persistence.core.astyanax.CassandraFig;
import org.apache.usergrid.persistence.core.util.AvailablePortFinder;
import org.safehaus.guicyfig.Env;
import org.safehaus.guicyfig.EnvironResource;
import org.safehaus.guicyfig.GuicyFigModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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

    @Override
    protected void before() throws Throwable {

        if ( !cassandraFig.isEmbedded()) {
            LOG.info("Using external Cassandra"); 
        }

        if ( started ) {
            return;
        }

        synchronized ( mutex ) {

            //second into mutex
            if(started){
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

                cass = new EmbeddedCassandra( dataDir, "Usergrid", 9160,
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
