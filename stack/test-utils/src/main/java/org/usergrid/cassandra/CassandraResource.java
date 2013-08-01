package org.usergrid.cassandra;


import java.io.File;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.cassandra.thrift.CassandraDaemon;
import org.junit.rules.ExternalResource;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;


/**
 * A JUnit {@link org.junit.rules.ExternalResource} designed to start up
 * Cassandra once in a TestSuite or test Class as a shared external resource
 * across test cases and shut it down after the TestSuite has completed.
 */
public class CassandraResource extends ExternalResource
{
    private static final String TMP = "tmp";
    public static final Logger LOG = LoggerFactory.getLogger( CassandraResource.class );

    private final String schemaManagerName;
    private final Object lock = new Object();
    private boolean initialized = false;
    private ConfigurableApplicationContext applicationContext;
    private CassandraDaemon cassandraDaemon;
    private SchemaManager schemaManager;


    /**
     * Creates a Cassandra starting ExternalResource for JUnit test cases
     * which uses the default SchemaManager for Cassandra.
     */
    public CassandraResource()
    {
        this( null );
    }


    /**
     * Creates a Cassandra starting ExternalResource for JUnit test cases
     * which uses the specified SchemaManager for Cassandra.
     */
    public CassandraResource( String schemaManagerName )
    {
        this.schemaManagerName = schemaManagerName;
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
}
