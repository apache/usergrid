package org.usergrid;


import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.usergrid.batch.AppArgsTest;
import org.usergrid.batch.BulkJobExecutionUnitTest;
import org.usergrid.batch.UsergridJobFactoryTest;
import org.usergrid.cassandra.Concurrent;
import org.usergrid.cassandra.ConcurrentSuite;


@RunWith( ConcurrentSuite.class )
@Suite.SuiteClasses(
    {
            AppArgsTest.class,
            UsergridJobFactoryTest.class,
            BulkJobExecutionUnitTest.class,

    } )
@Concurrent()
public class ConcurrentSchedulerTestSuite
{
}
