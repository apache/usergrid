package org.usergrid;


import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.usergrid.batch.job.*;
import org.usergrid.cassandra.CassandraResource;
import org.usergrid.cassandra.Concurrent;


@RunWith( Suite.class )
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
@Ignore( "TODO: Todd fix. Does not reliably pass on our build server." )
// TODO - this suite actually runs correctly now so we can
// remove this ignore if Todd is OK with it.
public class SchedulerITSuite
{
    @ClassRule
    public static CassandraResource cassandraResource = CassandraResource.newWithAvailablePorts();
}
