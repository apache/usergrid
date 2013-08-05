package org.usergrid;


import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.usergrid.cassandra.CassandraResource;
import org.usergrid.cassandra.Concurrent;
import org.usergrid.cassandra.ConcurrentSuite;
import org.usergrid.locking.cassandra.HectorLockManagerTest;
import org.usergrid.persistence.CollectionTest;
import org.usergrid.persistence.CounterTest;
import org.usergrid.system.UsergridSystemMonitorTest;


@RunWith( Suite.class )
@Suite.SuiteClasses( {
        HectorLockManagerTest.class,
        UsergridSystemMonitorTest.class,
        CollectionTest.class,
        CounterTest.class
} )
@Concurrent()
public class CoreSuite
{
    @ClassRule
    public static CassandraResource cassandraResource = CassandraResource.newWithAvailablePorts( "coreManager" );
}
