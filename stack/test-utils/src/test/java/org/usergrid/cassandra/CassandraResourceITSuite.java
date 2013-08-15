package org.usergrid.cassandra;


import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;


/**
 * An example TestSuite to demonstrate how to use the new CassandraResource
 * ExternalResource. Note that this suite fires up a CassandraResource and
 * so does the first test class: in fact it fires up two together besides the
 * third one fired up by the suite. This demonstrates how the parallelism
 * works along with the instance isolation.
 */
@RunWith( ConcurrentSuite.class )
@Suite.SuiteClasses( {
        CassandraResourceTest.class,           // <== itself fires up instances
        AnotherCassandraResourceIT.class,      // <== uses the existing suite instance
        YetAnotherCassandraResourceIT.class,   // <== uses the existing suite instance
        OkThisIsTheLastIT.class                // <== uses the existing suite instance
} )
@Concurrent()
public class CassandraResourceITSuite
{
    @ClassRule
    public static CassandraResource cassandraResource = CassandraResource.newWithAvailablePorts();
}
