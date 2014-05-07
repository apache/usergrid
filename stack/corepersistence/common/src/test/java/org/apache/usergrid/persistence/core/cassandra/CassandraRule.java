package org.apache.usergrid.persistence.core.cassandra;


import java.io.File;
import java.io.IOException;

import org.safehaus.guicyfig.Env;
import org.safehaus.guicyfig.EnvironResource;
import org.safehaus.guicyfig.GuicyFigModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.cassandra.io.util.FileUtils;
import org.apache.commons.configuration.AbstractConfiguration;

import org.apache.usergrid.persistence.core.util.AvailablePortFinder;
import org.apache.usergrid.persistence.core.astyanax.CassandraFig;

import com.google.common.io.Files;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.netflix.astyanax.test.EmbeddedCassandra;
import com.netflix.config.ConfigurationManager;


/**
 * @TODO - I wanted this in the test module but unfortunately that will create a circular dep
 *         due to the inclusion of the MigrationManager
 */
public class CassandraRule extends EnvironResource {
    private static final Logger LOG = LoggerFactory.getLogger( CassandraRule.class );

    private static final Object mutex = new Object();

    private static EmbeddedCassandra cass;

    private static boolean started = false;


    public CassandraRule() {
        super( Env.UNIT );

        startCassandra();

    }


    @Override
    protected void before() throws Throwable {

      startCassandra();
    }


    /**
     * Start cassandra
     */
    public static void startCassandra(){
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

                      int thriftPort = AvailablePortFinder.getNextAvailable();
                      int gossipPort = AvailablePortFinder.getNextAvailable();


                      LOG.info ("Using available thrift port {}", thriftPort);
                      LOG.info ("Using available gossip port {}", gossipPort);


                      LOG.info( "Starting cassandra" );

                      cass = new EmbeddedCassandra( dataDir, "Usergrid",  thriftPort,
                             gossipPort);
                      cass.start();

                      //set our Archaius properties
                      configure( thriftPort );



                      LOG.info( "Cassandra boostrapped" );

                      started = true;
                  }
                  catch ( IOException e ) {
                      throw new RuntimeException( "Unable to start cassandra", e );
                  }
              }
    }

    protected static void configure(int thriftPort){
        AbstractConfiguration config = ConfigurationManager.getConfigInstance();

        config.setProperty( CassandraFig.CASSANDRA_HOSTS, "localhost");
        config.setProperty( CassandraFig.CASSANDRA_PORT, thriftPort );
    }


    @Override
    protected void after() {

        //TODO TN.  this should only really happen when we shut down
//        cass.stop();
    }
}
