package org.usergrid.cassandra;


import org.junit.Test;


public class CassandraResourceTest
{
    private CassandraResource cassandraResource =
            CassandraResourceTestSuite.cassandraResource;

    @Test
    public void testUsage() throws Exception
    {
        String testBean = cassandraResource.getBean( "testBean", String.class );
        CassandraResource.logger.info( "Got the test bean: " + testBean );
    }
}
