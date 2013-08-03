package org.usergrid.cassandra;


import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class AnotherCassandraResourceIT
{
    private static final Logger logger = LoggerFactory.getLogger( AnotherCassandraResourceIT.class );
    private CassandraResource cassandraResource = CassandraResourceITSuite.cassandraResource;
    private static final long WAIT = 200L;


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
        Thread.sleep( WAIT );
    }


    @Test
    public void testItAgainAndAgain2() throws Exception
    {
        String testBean = cassandraResource.getBean( "testBean", String.class );
        logger.info( "Got another testBean again: {}", testBean );
        Thread.sleep( WAIT );
    }


    @Test
    public void testItAgainAndAgain3() throws Exception
    {
        String testBean = cassandraResource.getBean( "testBean", String.class );
        logger.info( "Got another testBean again: {}", testBean );
        Thread.sleep( WAIT );
    }


    @Test
    public void testItAgainAndAgain4() throws Exception
    {
        String testBean = cassandraResource.getBean( "testBean", String.class );
        logger.info( "Got another testBean again: {}", testBean );
        Thread.sleep( WAIT );
    }


    @Test
    public void testItAgainAndAgain5() throws Exception
    {
        String testBean = cassandraResource.getBean( "testBean", String.class );
        logger.info( "Got another testBean again: {}", testBean );
        Thread.sleep( WAIT );
    }


    @Test
    public void testItAgainAndAgain6() throws Exception
    {
        String testBean = cassandraResource.getBean( "testBean", String.class );
        logger.info( "Got another testBean again: {}", testBean );
        Thread.sleep( WAIT );
    }
}
