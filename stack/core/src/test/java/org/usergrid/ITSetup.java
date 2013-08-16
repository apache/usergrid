package org.usergrid;


import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.mq.QueueManagerFactory;
import org.usergrid.persistence.EntityManagerFactory;
import org.usergrid.persistence.IndexBucketLocator;
import org.usergrid.persistence.cassandra.CassandraService;
import org.usergrid.utils.JsonUtils;

import java.util.UUID;


public class ITSetup implements TestRule
{
    public static final boolean USE_DEFAULT_APPLICATION = false;
    private static final Logger LOG = LoggerFactory.getLogger( ITSetup.class );

    protected EntityManagerFactory emf;
    protected QueueManagerFactory qmf;
    protected IndexBucketLocator indexBucketLocator;
    protected CassandraService cassandraService;


    public Statement apply( Statement base, Description description )
    {
        return statement( base, description );
    }


    private Statement statement( final Statement base, final Description description )
    {
        return new Statement()
        {
            @Override
            public void evaluate() throws Throwable
            {
                before( description );

                try
                {
                    base.evaluate();
                }
                finally
                {
                    after( description );
                }
            }
        };
    }


    /**
     * Sets up the resources for the test here.
     *
     * @throws Throwable if setup fails (which will disable {@code after}
     */
    protected void before( Description description ) throws Throwable
    {
        LOG.info( "Setting up for {}", description.getDisplayName() );

        emf = CoreITSuite.cassandraResource.getBean( EntityManagerFactory.class );
        qmf = CoreITSuite.cassandraResource.getBean( QueueManagerFactory.class );
        indexBucketLocator = CoreITSuite.cassandraResource.getBean( IndexBucketLocator.class );
        cassandraService = CoreITSuite.cassandraResource.getBean( CassandraService.class );
    }


    /**
     * Override to tear down your specific external resource.
     */
    protected void after( Description description )
    {
        LOG.info( "Tearing down for {}", description.getDisplayName() );
    }


    public EntityManagerFactory getEmf()
    {
        return emf;
    }


    public QueueManagerFactory getQmf()
    {
        return qmf;
    }


    public IndexBucketLocator getIbl()
    {
        return indexBucketLocator;
    }


    public CassandraService getCassSvc()
    {
        return cassandraService;
    }


    public UUID createApplication( String organizationName, String applicationName ) throws Exception
    {
        if ( USE_DEFAULT_APPLICATION )
        {
            return CassandraService.DEFAULT_APPLICATION_ID;
        }

        return emf.createApplication( organizationName, applicationName );
    }


    public void dump( String name, Object obj )
    {
        if ( obj != null )
        {
            LOG.info(name + ":\n" + JsonUtils.mapToFormattedJsonString(obj));
        }
    }

}
