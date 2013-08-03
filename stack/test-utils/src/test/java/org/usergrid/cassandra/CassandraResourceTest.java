package org.usergrid.cassandra;


import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import static junit.framework.Assert.assertTrue;


/**
 * This tests the CassandraResource.
 */
@Concurrent()
public class CassandraResourceTest
{
    public static final Logger LOG = LoggerFactory.getLogger( CassandraResourceTest.class );
    public static final long WAIT = 200L;


    /**
     * Tests to make sure port overrides works properly.
     *
     * @throws Exception
     */
    @Test
    public void testPortOverride() throws Throwable
    {
        int rpcPort;
        int storagePort;
        int sslStoragePort;

        do
        {
            rpcPort = AvailablePortFinder.getNextAvailable( CassandraResource.DEFAULT_RPC_PORT + 1 );
        }
        while ( rpcPort == CassandraResource.DEFAULT_RPC_PORT );
        LOG.info( "Setting rpc_port to {}", rpcPort );

        do
        {
            storagePort = AvailablePortFinder.getNextAvailable( CassandraResource.DEFAULT_STORAGE_PORT + 1 );
        }
        while ( storagePort == CassandraResource.DEFAULT_STORAGE_PORT || storagePort == rpcPort );
        LOG.info( "Setting storage_port to {}", storagePort );

        do
        {
            sslStoragePort = AvailablePortFinder.getNextAvailable( CassandraResource.DEFAULT_SSL_STORAGE_PORT + 1 );
        }
        while ( sslStoragePort == CassandraResource.DEFAULT_SSL_STORAGE_PORT || storagePort == sslStoragePort );
        LOG.info( "Setting ssl_storage_port to {}", sslStoragePort );

        final CassandraResource cassandraResource = new CassandraResource( rpcPort, storagePort, sslStoragePort );

        cassandraResource.before();

        // test here to see if we can access cassandra's ports
        // TODO - add some test code here using Hector

        cassandraResource.after();
        LOG.info( "Got the test bean: " );
    }


    @Test
    public void testTmpDirectory() throws Exception
    {
        File tmpdir = CassandraResource.getTempDirectory();
        assertTrue( tmpdir.toString().contains( "target" ) );
        assertTrue( tmpdir.exists() );
    }


    /**
     * Fires up two Cassandra instances on the same machine.
     *
     * @throws Exception if this don't work
     */
    @Test
    public void testDoubleTrouble() throws Throwable
    {
        CassandraResource c1 = CassandraResource.newWithAvailablePorts();
        LOG.info( "Starting up first Cassandra instance: {}", c1 );
        c1.before();

        LOG.debug( "Waiting for the new instance to come online." );
        while( ! c1.isReady() )
        {
            Thread.sleep( WAIT );
        }

        CassandraResource c2 = CassandraResource.newWithAvailablePorts();
        LOG.debug( "Starting up second Cassandra instance: {}", c2 );
        c2.before();

        LOG.debug( "Waiting a few seconds for second instance to be ready before shutting down." );
        while( ! c2.isReady() )
        {
            Thread.sleep( WAIT );
        }

        LOG.debug( "Shutting Cassandra instances down." );
        c1.after();
        c2.after();
    }
}
