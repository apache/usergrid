package org.usergrid;


import org.junit.ClassRule;
import org.junit.Rule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.cassandra.Concurrent;
import org.usergrid.mq.QueueManagerFactory;
import org.usergrid.persistence.EntityManagerFactory;
import org.usergrid.utils.JsonUtils;


public abstract class AbstractCoreIT
{
    private static final Logger LOG = LoggerFactory.getLogger( AbstractCoreIT.class );


    @ClassRule
    public static CoreITSetup setup = new CoreITSetupImpl( CoreITSuite.cassandraResource );

    @Rule
    public CoreApplication app = new CoreApplication( setup );


    public void dump( Object obj )
    {
        dump( "Object", obj );
    }


    public void dump( String name, Object obj )
    {
        if ( obj != null && LOG.isInfoEnabled() )
        {
            LOG.info(name + ":\n" + JsonUtils.mapToFormattedJsonString(obj));
        }
    }
}
