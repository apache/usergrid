package org.usergrid.cassandra;


import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class AnotherCassandraResourceIT
{
    private static final Logger logger = LoggerFactory.getLogger( AnotherCassandraResourceIT.class );
    private CassandraResource cassandraResource = CassandraResourceITSuite.cassandraResource;


    @Test
    public void testItAgain() throws Exception
    {
        String testBean = cassandraResource.getBean( "testBean", String.class );
        logger.info( "Got another testBean: {}", testBean );
    }


    @Test
    public void testItAgainAndAgain() throws Exception
    {
        String testBean = cassandraResource.getBean( "testBean", String.class );
        logger.info( "Got another testBean again: {}", testBean );
    }
}
