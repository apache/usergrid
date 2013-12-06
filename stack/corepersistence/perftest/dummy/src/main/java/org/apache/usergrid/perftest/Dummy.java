package org.apache.usergrid.perftest;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Created with IntelliJ IDEA. User: akarasulu Date: 12/6/13 Time: 1:08 AM To change this template use File | Settings |
 * File Templates.
 */

public class Dummy {
    private static final Logger LOG = LoggerFactory.getLogger( Dummy.class );

    @PerftestParams(
        callCount = 1000,
        delayBetweenCalls = 0,
        threadCount = 10,
        modules = DummyModule.class
    )
    public void foobar()
    {
        LOG.debug( "foobar" );
    }
}
