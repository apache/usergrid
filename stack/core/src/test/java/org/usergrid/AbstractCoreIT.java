package org.usergrid;


import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.cassandra.Concurrent;
import org.usergrid.mq.QueueManagerFactory;
import org.usergrid.persistence.EntityManagerFactory;
import org.usergrid.persistence.IndexBucketLocator;
import org.usergrid.persistence.cassandra.CassandraService;
import org.usergrid.utils.JsonUtils;


import java.util.UUID;


@Concurrent()
public abstract class AbstractCoreIT
{
    public static final boolean USE_DEFAULT_APPLICATION = false;

    private static final Logger LOG = LoggerFactory.getLogger( AbstractCoreIT.class );

    protected EntityManagerFactory emf;
    protected QueueManagerFactory qmf;
    protected IndexBucketLocator indexBucketLocator;
    protected CassandraService cassandraService;



    public AbstractCoreIT()
    {
        LOG.info("Initializing test ...");
        emf = CoreITSuite.cassandraResource.getBean( EntityManagerFactory.class );
        qmf = CoreITSuite.cassandraResource.getBean( QueueManagerFactory.class );
        indexBucketLocator = CoreITSuite.cassandraResource.getBean( IndexBucketLocator.class );
        cassandraService = CoreITSuite.cassandraResource.getBean( CassandraService.class );
    }


    @BeforeClass
    public static void setup() throws Exception
    {
        LOG.info("setup");
    }


    @AfterClass
    public static void teardown() throws Exception
    {
        LOG.info("teardown");
    }


    public EntityManagerFactory getEntityManagerFactory()
    {
        return emf;
    }


    public QueueManagerFactory geQueueManagerFactory()
    {
        return qmf;
    }


    public UUID createApplication( String organizationName, String applicationName ) throws Exception
    {
        if ( USE_DEFAULT_APPLICATION )
        {
            return CassandraService.DEFAULT_APPLICATION_ID;
        }

        return emf.createApplication( organizationName, applicationName );
    }


    public void dump( Object obj )
    {
        dump( "Object", obj );
    }


    public void dump( String name, Object obj )
    {
        if ( obj != null )
        {
            LOG.info(name + ":\n" + JsonUtils.mapToFormattedJsonString(obj));
        }
    }
}
