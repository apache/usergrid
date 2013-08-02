package org.usergrid.cassandra;


import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;


/**
 * An example TestSuite to demonstrate how to use the new
 * CassandraResource fixture.
 */
@RunWith( ConcurrentSuite.class )
@Suite.SuiteClasses( {
        AnotherCassandraResourceIT.class,
        YetAnotherCassandraResourceIT.class,
        OkThisIsTheLastIT.class
} )
@Concurrent()
public class CassandraResourceITSuite
{
    @ClassRule
    public static CassandraResource cassandraResource = CassandraResource.newWithAvailablePorts();
}
