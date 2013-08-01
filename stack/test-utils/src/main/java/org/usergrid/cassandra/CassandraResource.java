package org.usergrid.cassandra;


import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

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
    public static final Logger logger = LoggerFactory.getLogger( CassandraResource.class );

    private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    ConfigurableApplicationContext applicationContext;
    CassandraDaemon cassandraDaemon;

    private final String schemaManagerName;
    private SchemaManager schemaManager;


    public CassandraResource()
    {
        this( null );
    }


    public CassandraResource(String schemaManagerName)
    {
        this.schemaManagerName = schemaManagerName;
    }


    public <T> T getBean( String name, Class<T> requiredType )
    {
        return applicationContext.getBean( name, requiredType );
    }


    public <T> T getBean( Class<T> requiredType )
    {
        return applicationContext.getBean( requiredType );
    }


    /**
     * Starts up Cassandra before TestSuite or test Class execution.
     *
     * @throws Throwable if we cannot start up Cassandra
     */
    @Override
    protected void before() throws Throwable
    {
        super.before();

        logger.info( "Initializing Cassandra..." );

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
    }


    /**
     * Stops Cassandra after a TestSuite or test Class executes.
     */
    @Override
    protected void after()
    {
        super.after();

        logger.info( "Shutting down Cassandra" );

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

            executor.shutdown();
            executor.shutdownNow();
        }
        catch ( Exception ex )
        {
            ex.printStackTrace();
        }
    }


    private void loadSchemaManager( String schemaManagerName )
    {
        if ( ! applicationContext.isActive() )
        {
            logger.info( "Restarting context..." );
            applicationContext.refresh();
        }

        if ( schemaManager != null )
        {
            // TODO check for classpath and go static?
            logger.info( "dataControl found - looking upma SchemaManager impl" );
            this.schemaManager = getBean( schemaManagerName, SchemaManager.class );
        }
        else
        {
            logger.info( "SchemaManager is null - using the default SchemaManager impl" );
            this.schemaManager = getBean( SchemaManager.class );
        }

        schemaManager.create();
        schemaManager.populateBaseData();
    }
}
