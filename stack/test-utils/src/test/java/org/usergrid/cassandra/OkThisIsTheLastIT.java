package org.usergrid.cassandra;


import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Concurrent()
public class OkThisIsTheLastIT
{
    public static final Logger logger = LoggerFactory.getLogger(CassandraResource.class);
    private CassandraResource cassandraResource =
            CassandraResourceITSuite.cassandraResource;

    @Test
    public void testUsage() throws Exception
    {
        String testBean = cassandraResource.getBean( "testBean", String.class );
        logger.info( "Got the test bean: " + testBean );
    }


    @Test
    public void testItAgainAndAgain() throws Exception
    {
        String testBean = cassandraResource.getBean( "testBean", String.class );
        logger.info( "Got another testBean again: {}", testBean );
        Thread.sleep( 1000L );
    }


    @Test
    public void testItAgainAndAgain2() throws Exception
    {
        String testBean = cassandraResource.getBean( "testBean", String.class );
        logger.info( "Got another testBean again: {}", testBean );
        Thread.sleep( 1000L );
    }


    @Test
    public void testItAgainAndAgain3() throws Exception
    {
        String testBean = cassandraResource.getBean( "testBean", String.class );
        logger.info( "Got another testBean again: {}", testBean );
        Thread.sleep( 1000L );
    }


    @Test
    public void testItAgainAndAgain4() throws Exception
    {
        String testBean = cassandraResource.getBean( "testBean", String.class );
        logger.info( "Got another testBean again: {}", testBean );
        Thread.sleep( 1000L );
    }


    @Test
    public void testItAgainAndAgain5() throws Exception
    {
        String testBean = cassandraResource.getBean( "testBean", String.class );
        logger.info( "Got another testBean again: {}", testBean );
        Thread.sleep( 1000L );
    }


    @Test
    public void testItAgainAndAgain6() throws Exception
    {
        String testBean = cassandraResource.getBean( "testBean", String.class );
        logger.info( "Got another testBean again: {}", testBean );
        Thread.sleep( 1000L );
    }
}
