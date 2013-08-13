package org.usergrid;


import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.usergrid.batch.job.*;
import org.usergrid.cassandra.CassandraResource;
import org.usergrid.cassandra.Concurrent;
import org.usergrid.cassandra.ConcurrentSuite;


@RunWith( ConcurrentSuite.class )
@Suite.SuiteClasses(
    {
            SchedulerRuntime1IT.class,
            SchedulerRuntime2IT.class,
            SchedulerRuntime3IT.class,
            SchedulerRuntime4IT.class,
            SchedulerRuntime5IT.class,
            SchedulerRuntime6IT.class,
            SchedulerRuntime7IT.class,
            SchedulerRuntime8IT.class
    } )
@Concurrent()
public class ConcurrentSchedulerITSuite
{
    @ClassRule
    public static CassandraResource cassandraResource = CassandraResource.newWithAvailablePorts();
}
