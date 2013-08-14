package org.usergrid.system;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.usergrid.CoreITSuite;
import org.usergrid.cassandra.Concurrent;
import org.usergrid.utils.MapUtils;

import java.util.Date;


/**
 * @author zznate
 */
@Concurrent
public class UsergridSystemMonitorIT
{
    private UsergridSystemMonitor usergridSystemMonitor;


    @Before
    public void setupLocal()
    {
        usergridSystemMonitor = CoreITSuite.cassandraResource.getBean( UsergridSystemMonitor.class );
    }

    @Test
    public void testVersionNumber()
    {
        assertEquals( "0.1", usergridSystemMonitor.getBuildNumber() );
    }

    @Test
    public void testIsCassandraAlive()
    {
        assertTrue( usergridSystemMonitor.getIsCassandraAlive() );
    }


    @Test
    public void verifyLogDump()
    {
        String str = UsergridSystemMonitor.formatMessage( 1600L, MapUtils.hashMap( "message", "hello" ) );

        assertTrue( StringUtils.contains( str, "hello" ) );

        usergridSystemMonitor.maybeLogPayload( 16000L, "foo","bar","message","some text" );
        usergridSystemMonitor.maybeLogPayload( 16000L, new Date() );
    }
}
