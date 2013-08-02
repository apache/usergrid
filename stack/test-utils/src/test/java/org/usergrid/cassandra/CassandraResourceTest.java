package org.usergrid.cassandra;


import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This tests the CassandraResource.
 */
public class CassandraResourceTest
{
    public static final Logger LOG = LoggerFactory.getLogger( CassandraResourceTest.class );


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
        while ( storagePort == CassandraResource.DEFAULT_STORAGE_PORT );
        LOG.info( "Setting storage_port to {}", storagePort );

        final CassandraResource cassandraResource = new CassandraResource( rpcPort, storagePort );

        cassandraResource.before();

        // test here to see if we can access cassandra's ports
        // TODO - add some test code here using Hector

        cassandraResource.after();
        LOG.info( "Got the test bean: " );
    }
}
