package org.usergrid.cassandra;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.net.URL;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.cassandra.thrift.CassandraDaemon;
import org.junit.rules.ExternalResource;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.yaml.snakeyaml.Yaml;


/**
 * A JUnit {@link org.junit.rules.ExternalResource} designed to start up
 * Cassandra once in a TestSuite or test Class as a shared external resource
 * across test cases and shut it down after the TestSuite has completed.
 */
public class CassandraResource extends ExternalResource
{
    public static final Logger LOG = LoggerFactory.getLogger( CassandraResource.class );

    /** TODO - make this be a file in the target directory */
    public static final String TMP = "tmp";

    public static final int DEFAULT_RPC_PORT= 9160;
    public static final int DEFAULT_STORAGE_PORT = 7000;
    public static final String RPC_PORT_KEY = "rpc_port";
    public static final String STORAGE_PORT_KEY = "storage_port";

    private final String schemaManagerName;
    private final Object lock = new Object();

    private boolean initialized = false;
    public int rpcPort = DEFAULT_RPC_PORT;
    public int storagePort = DEFAULT_STORAGE_PORT;
    private ConfigurableApplicationContext applicationContext;
    private CassandraDaemon cassandraDaemon;
    private SchemaManager schemaManager;


    /**
     * Creates a Cassandra starting ExternalResource for JUnit test cases
     * which uses the default SchemaManager for Cassandra.
     */
    public CassandraResource()
    {
        this( null, DEFAULT_RPC_PORT, DEFAULT_STORAGE_PORT );
    }


    /**
     * Creates a Cassandra starting ExternalResource for JUnit test cases
     * which uses the specified SchemaManager for Cassandra.
     */
    public CassandraResource( String schemaManagerName, int rpcPort, int storagePort )
    {
        this.schemaManagerName = schemaManagerName;
        this.rpcPort = rpcPort;
        this.storagePort = storagePort;
    }


    /**
     * Creates a Cassandra starting ExternalResource for JUnit test cases
     * which uses the specified SchemaManager for Cassandra.
     */
    public CassandraResource( int rpcPort, int storagePort )
    {
        this( null, rpcPort, storagePort );
    }


    /**
     * Gets the rpcPort for Cassandra.
     *
     * @return the rpc_port (in yaml file) used by Cassandra
     */
    public int getRpcPort()
    {
        return rpcPort;
    }


    /**
     * Gets the storagePort for Cassandra.
     *
     * @return the storage_port (in yaml file) used by Cassandra
     */
    public int getStoragePort()
    {
        return storagePort;
    }


    /**
     * Gets a bean from the application context.
     *
     * @param requiredType the type of the bean
     * @param <T> the type of the bean
     * @return the bean
     */
    public <T> T getBean( String name, Class<T> requiredType )
    {
        protect();
        return applicationContext.getBean( name, requiredType );
    }


    /**
     * Gets a bean from the application context.
     *
     * @param requiredType the type of the bean
     * @param <T> the type of the bean
     * @return the bean
     */
    public <T> T getBean( Class<T> requiredType )
    {
        protect();
        return applicationContext.getBean( requiredType );
    }


    /**
     * Gets whether this resource is ready to use.
     *
     * @return true if ready to use, false otherwise
     */
    public boolean isReady()
    {
        return initialized;
    }


    /**
     * Protects against IDE or command line runs of a unit test which when
     * starting the test outside of the Suite will not start the resource.
     * This makes sure the resource is automatically started on a usage
     * attempt.
     */
    private void protect()
    {
        if ( ! isReady() )
        {
            try
            {
                before();
            }
            catch ( Throwable t )
            {
                LOG.error("Failed to start up Cassandra.", t);
                throw new RuntimeException( t );
            }
        }
    }


    /**
     * Starts up Cassandra before TestSuite or test Class execution.
     *
     * @throws Throwable if we cannot start up Cassandra
     */
    @Override
    protected void before() throws Throwable
    {
        synchronized ( lock )
        {
            super.before();

            if ( isReady() )
            {
                return;
            }

            LOG.info("Initializing Cassandra...");

            System.setProperty( "cassandra-foreground", "true" );
            System.setProperty( "log4j.defaultInitOverride", "true" );
            System.setProperty( "log4j.configuration", "log4j.properties" );
            System.setProperty( "cassandra.ring_delay_ms", "100" );

            if ( getRpcPort() != DEFAULT_RPC_PORT || getStoragePort() != DEFAULT_STORAGE_PORT )
            {
                // See if we can modify the YAML file for the available port
                Yaml yaml = new Yaml();
                URL url = ClassLoader.getSystemResource( "cassandra.yaml" );
                Map<String, Object> map = ( Map <String, Object> ) yaml.load(
                        new FileInputStream( url.getFile() ) );
                map.put( RPC_PORT_KEY, getRpcPort() );
                map.put( STORAGE_PORT_KEY, getStoragePort() );
                yaml.dump( map, new FileWriter( url.getFile() ) );
            }

            FileUtils.deleteQuietly( new File( TMP ) );

            cassandraDaemon = new CassandraDaemon();
            cassandraDaemon.activate();
            String[] locations = { "usergrid-test-context.xml" };
            applicationContext = new ClassPathXmlApplicationContext( locations );

            loadSchemaManager( schemaManagerName );
            initialized = true;

            lock.notifyAll();
        }
    }


    /**
     * Stops Cassandra after a TestSuite or test Class executes.
     */
    @Override
    protected synchronized void after()
    {
        super.after();

        LOG.info("Shutting down Cassandra");

        if ( schemaManager != null )
        {
            schemaManager.destroy();
        }

        applicationContext.stop();

        try
        {
            if ( cassandraDaemon != null )
            {
                cassandraDaemon.deactivate();
            }
        }
        catch ( Exception ex )
        {
            ex.printStackTrace();
        }
    }


    /**
     * Loads the specified {@link SchemaManager} or the default manager if
     * the manager name that is provided is null.
     *
     * @param schemaManagerName the name of the SchemaManager to load, or null
     */
    private void loadSchemaManager( String schemaManagerName )
    {
        if ( ! applicationContext.isActive() )
        {
            LOG.info( "Restarting context..." );
            applicationContext.refresh();
        }

        if ( schemaManager != null )
        {
            LOG.info( "Looking up SchemaManager impl: {}", schemaManagerName );
            this.schemaManager = applicationContext.getBean( schemaManagerName, SchemaManager.class );
        }
        else
        {
            LOG.info( "The SchemaManager is not specified - using the default SchemaManager impl" );
            this.schemaManager = applicationContext.getBean( SchemaManager.class );
        }

        schemaManager.create();
        schemaManager.populateBaseData();
    }


    /**
     * Creates a new instance of the CassandraResource with rpc and storage
     * ports that may or may not be the default ports. If either port is
     * taken, an alternative free port is found.
     *
     * @return a new CassandaResource with possibly non-default ports
     */
    public static CassandraResource newWithAvailablePorts()
    {
        int rpcPort;
        int storagePort;

        do
        {
            rpcPort = AvailablePortFinder.getNextAvailable( CassandraResource.DEFAULT_RPC_PORT );
        }
        while ( rpcPort == CassandraResource.DEFAULT_RPC_PORT );

        do
        {
            storagePort = AvailablePortFinder.getNextAvailable(CassandraResource.DEFAULT_RPC_PORT);
        }
        while ( storagePort == CassandraResource.DEFAULT_RPC_PORT );

        return new CassandraResource( rpcPort, storagePort );
    }
}
