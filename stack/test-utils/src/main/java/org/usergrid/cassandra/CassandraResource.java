package org.usergrid.cassandra;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.math.RandomUtils;
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
 *
 * This external resource is completely isolated in terms of the files used
 * and the ports selected if the {@link AvailablePortFinder} is used with it.
 *
 * Note that for this resource to work properly, a project.properties file
 * must be placed in the src/test/resources directory with the following
 * stanza in the project pom's build section:
 *
 * <testResources>
 *   <testResource>
 *     <directory>src/test/resources</directory>
 *     <filtering>true</filtering>
 *     <includes>
 *       <include>**\/*.properties</include>
 *       <include>**\/*.xml</include>
 *     </includes>
 *   </testResource>
 * </testResources>
 *
 * The following property expansion macro should be placed in this
 * project.properties file:
 *
 * target.directory=${pom.build.directory}
 */
public class CassandraResource extends ExternalResource
{
    public static final Logger LOG = LoggerFactory.getLogger( CassandraResource.class );
    public static final int DEFAULT_RPC_PORT= 9160;
    public static final int DEFAULT_STORAGE_PORT = 7000;
    public static final int DEFAULT_SSL_STORAGE_PORT = 7001;

    public static final String PROPERTIES_FILE = "project.properties";
    public static final String TARGET_DIRECTORY_KEY = "target.directory";
    public static final String DATA_FILE_DIR_KEY = "data_file_directories";
    public static final String COMMIT_FILE_DIR_KEY = "commitlog_directory";
    public static final String SAVED_CACHES_DIR_KEY = "saved_caches_directory";

    public static final String RPC_PORT_KEY = "rpc_port";
    public static final String STORAGE_PORT_KEY = "storage_port";
    public static final String SSL_STORAGE_PORT_KEY = "ssl_storage_port";

    private static final Object lock = new Object();

    private final File tempDir;
    private final String schemaManagerName;

    private boolean initialized = false;
    private int rpcPort = DEFAULT_RPC_PORT;
    private int storagePort = DEFAULT_STORAGE_PORT;
    private int sslStoragePort = DEFAULT_SSL_STORAGE_PORT;
    private ConfigurableApplicationContext applicationContext;
    private CassandraDaemon cassandraDaemon;
    private SchemaManager schemaManager;


    /**
     * Creates a Cassandra starting ExternalResource for JUnit test cases
     * which uses the default SchemaManager for Cassandra.
     */
    @SuppressWarnings( "UnusedDeclaration" )
    public CassandraResource() throws IOException
    {
        this( null, DEFAULT_RPC_PORT, DEFAULT_STORAGE_PORT, DEFAULT_SSL_STORAGE_PORT );
    }


    /**
     * Creates a Cassandra starting ExternalResource for JUnit test cases
     * which uses the specified SchemaManager for Cassandra.
     */
    public CassandraResource( String schemaManagerName, int rpcPort, int storagePort, int sslStoragePort )
    {
        this.schemaManagerName = schemaManagerName;
        this.rpcPort = rpcPort;
        this.storagePort = storagePort;
        this.sslStoragePort = sslStoragePort;

        try
        {
            this.tempDir = getTempDirectory();
        }
        catch ( IOException e )
        {
            LOG.error( "Failed to create temporary directory for Cassandra instance.", e );
            throw new RuntimeException( e );
        }
    }


    /**
     * Creates a Cassandra starting ExternalResource for JUnit test cases
     * which uses the specified SchemaManager for Cassandra.
     */
    public CassandraResource( int rpcPort, int storagePort, int sslStoragePort ) throws IOException
    {
        this( null, rpcPort, storagePort, sslStoragePort );
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
     * Gets the sslStoragePort for Cassandra.
     *
     * @return the sslStoragePort
     */
    public int getSslStoragePort()
    {
        return sslStoragePort;
    }


    /**
     * Gets the temporary directory created for this CassandraResource.
     *
     * @return the temporary directory
     */
    @SuppressWarnings( "UnusedDeclaration" )
    public File getTemporaryDirectory()
    {
        return tempDir;
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


    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append( "\n" );
        sb.append( "cassandra.yaml = " ).append( new File( tempDir, "cassandra.yaml" ) );
        sb.append( "\n" );
        sb.append( RPC_PORT_KEY ).append(" = ").append( rpcPort );
        sb.append( "\n" );
        sb.append( STORAGE_PORT_KEY ).append( " = " ).append( storagePort );
        sb.append( "\n" );
        sb.append( SSL_STORAGE_PORT_KEY ).append( " = " ).append( sslStoragePort );
        sb.append( "\n" );
        sb.append( DATA_FILE_DIR_KEY ).append( " = " ).append( new File( tempDir, "data" ).toString() );
        sb.append( "\n" );
        sb.append( COMMIT_FILE_DIR_KEY ).append( " = " ).append( new File( tempDir, "commitlog" ).toString() );
        sb.append( "\n" );
        sb.append( SAVED_CACHES_DIR_KEY ).append( " = " ).append( new File( tempDir, "saved_caches" ).toString() );
        sb.append( "\n" );

        return sb.toString();
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
                LOG.error( "Failed to start up Cassandra.", t );
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
        /*
         * Note that the lock is static so it is JVM wide which prevents other
         * instances of this class from making changes to the Cassandra system
         * properties while we are initializing Cassandra with unique settings.
         */

        synchronized ( lock )
        {
            super.before();

            if ( isReady() )
            {
                return;
            }

            LOG.info( "Initializing Cassandra at {} ...", tempDir.toString() );

            // Create temp directory, setup to create new File configuration there
            File newYamlFile = new File( tempDir, "cassandra.yaml" );
            URL newYamlUrl = FileUtils.toURLs( new File [] { newYamlFile } )[0];

            // Read the original yaml file, make changes, and dump to new position in tmpdir
            Yaml yaml = new Yaml();
            @SuppressWarnings("unchecked") Map<String, Object> map =
                    ( Map <String, Object> ) yaml.load( ClassLoader.getSystemResourceAsStream( "cassandra.yaml" ) );
            map.put( RPC_PORT_KEY, getRpcPort() );
            map.put( STORAGE_PORT_KEY, getStoragePort() );
            map.put( SSL_STORAGE_PORT_KEY, getSslStoragePort() );
            map.put( COMMIT_FILE_DIR_KEY, new File( tempDir, "commitlog" ).toString() );
            map.put( DATA_FILE_DIR_KEY, new String [] { new File( tempDir, "data" ).toString() } );
            map.put( SAVED_CACHES_DIR_KEY, new File(tempDir, "saved_caches").toString());
            FileWriter writer = new FileWriter( newYamlFile );
            yaml.dump(map, writer);
            writer.flush();
            writer.close();

            // Fire up Cassandra by setting configuration to point to new yaml file
            System.setProperty( "cassandra.url", "localhost:" + Integer.toString( rpcPort ) );
            System.setProperty( "cassandra-foreground", "true" );
            System.setProperty( "log4j.defaultInitOverride", "true" );
            System.setProperty( "log4j.configuration", "log4j.properties" );
            System.setProperty( "cassandra.ring_delay_ms", "100" );
            System.setProperty( "cassandra.config", newYamlUrl.toString() );
            System.setProperty( "cassandra." + RPC_PORT_KEY, Integer.toString( rpcPort ) );
            System.setProperty( "cassandra." + STORAGE_PORT_KEY, Integer.toString( storagePort ) );
            System.setProperty( "cassandra." + SSL_STORAGE_PORT_KEY, Integer.toString( sslStoragePort ) );

            if ( ! newYamlFile.exists() )
            {
                throw new RuntimeException( "Cannot find new Yaml file: " + newYamlFile );
            }

            cassandraDaemon = new CassandraDaemon();
            cassandraDaemon.activate();

            Runtime.getRuntime().addShutdownHook( new Thread()
            {
                @Override
                public void run()
                {
                    after();
                }
            });

            String[] locations = { "usergrid-test-context.xml" };
            applicationContext = new ClassPathXmlApplicationContext( locations );

            loadSchemaManager( schemaManagerName );
            initialized = true;
            LOG.info( "External Cassandra resource at {} is ready!", tempDir.toString() );
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

        LOG.info( "Shutting down Cassandra instance in {}", tempDir.toString() );

        if ( schemaManager != null )
        {
            LOG.info( "Destroying schemaManager..." );
            try
            {
                schemaManager.destroy();
            }
            catch ( Exception e )
            {
                LOG.error( "Ignoring failures while dropping keyspaces: {}", e.getMessage() );
            }

            LOG.info( "SchemaManager destroyed..." );
        }

        applicationContext.stop();
        LOG.info( "ApplicationContext stopped..." );

        try
        {
            if ( cassandraDaemon != null )
            {
                LOG.info( "Deactivating CassandraDaemon..." );
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

        if ( schemaManagerName != null )
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
     * @param schemaManagerName the name of the schemaManager to use
     * @return a new CassandraResource with possibly non-default ports
     */
    public static CassandraResource newWithAvailablePorts( String schemaManagerName )
    {
        int rpcPort = AvailablePortFinder.getNextAvailable( CassandraResource.DEFAULT_RPC_PORT
                + RandomUtils.nextInt( 1000 ) );
        int storagePort = AvailablePortFinder.getNextAvailable( CassandraResource.DEFAULT_STORAGE_PORT
                + RandomUtils.nextInt( 1000 ) );
        int sslStoragePort = AvailablePortFinder.getNextAvailable( CassandraResource.DEFAULT_SSL_STORAGE_PORT
                + RandomUtils.nextInt( 1000 ) );

        if ( rpcPort == storagePort )
        {
            storagePort++;
            storagePort = AvailablePortFinder.getNextAvailable( storagePort );
        }

        if ( sslStoragePort == storagePort )
        {
            sslStoragePort++;
            sslStoragePort = AvailablePortFinder.getNextAvailable( sslStoragePort );
        }

        return new CassandraResource( schemaManagerName, rpcPort, storagePort, sslStoragePort );
    }


    /**
     * Creates a new instance of the CassandraResource with rpc and storage
     * ports that may or may not be the default ports. If either port is
     * taken, an alternative free port is found.
     *
     * @return a new CassandraResource with possibly non-default ports
     */
    public static CassandraResource newWithAvailablePorts()
    {
        return  newWithAvailablePorts( null );
    }


    /**
     * Uses a project.properties file that Maven does substitution on to
     * to replace the value of a property with the path to the Maven
     * build directory (a.k.a. target). It then uses this path to generate
     * a random String which it uses to append a path component to so a
     * unique directory is selected. If already present it's deleted,
     * then the directory is created.
     *
     * @return a unique temporary directory
     * @throws IOException if we cannot access the properties file
     */
    public static File getTempDirectory() throws IOException
    {
        File tmpdir;
        Properties props = new Properties();
        props.load( ClassLoader.getSystemResourceAsStream( PROPERTIES_FILE ) );
        File basedir = new File( ( String ) props.get( TARGET_DIRECTORY_KEY ) );
        String comp = RandomStringUtils.randomAlphanumeric(7);
        tmpdir = new File( basedir, comp );

        LOG.info( "Creating temporary directory: {}", tmpdir );
        if ( tmpdir.exists() )
        {
            FileUtils.forceDelete( tmpdir );
        }
        else
        {
            FileUtils.forceMkdir( tmpdir );
        }

        return tmpdir;
    }
}
