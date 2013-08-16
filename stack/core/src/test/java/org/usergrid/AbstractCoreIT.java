package org.usergrid;


import org.junit.ClassRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.cassandra.Concurrent;
import org.usergrid.mq.QueueManagerFactory;
import org.usergrid.persistence.EntityManagerFactory;
import org.usergrid.utils.JsonUtils;


@Concurrent()
public abstract class AbstractCoreIT
{
    private static final Logger LOG = LoggerFactory.getLogger( AbstractCoreIT.class );


    @ClassRule
    public static ITSetup setup = new ITSetup();


    public AbstractCoreIT()
    {
        LOG.info("Initializing test ...");
    }


    public EntityManagerFactory getEntityManagerFactory()
    {
        return setup.getEmf();
    }


    public QueueManagerFactory getQueueManagerFactory()
    {
        return setup.getQmf();
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
