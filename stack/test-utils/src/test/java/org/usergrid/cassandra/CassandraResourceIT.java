package org.usergrid.cassandra;


import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CassandraResourceIT
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
}
