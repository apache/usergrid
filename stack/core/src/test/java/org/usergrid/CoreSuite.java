package org.usergrid;


import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.usergrid.cassandra.CassandraResource;
import org.usergrid.cassandra.Concurrent;
import org.usergrid.locking.cassandra.HectorLockManagerIT;
import org.usergrid.mq.MessagesIT;
import org.usergrid.persistence.*;
import org.usergrid.persistence.query.IteratingQueryIT;
import org.usergrid.system.UsergridSystemMonitorIT;


@RunWith( Suite.class )
@Suite.SuiteClasses(
    {
        HectorLockManagerIT.class,
        UsergridSystemMonitorIT.class,
        CollectionIT.class,
        CounterIT.class,
        EntityConnectionsIT.class,
        EntityDictionaryIT.class,
        EntityManagerFactoryIT.class,
        EntityManagerIT.class,
        GeoIT.class,
        IndexIT.class,
        MessagesIT.class,
        PermissionsIT.class,
        IteratingQueryIT.class,      // This should be broken down but it will not
                                     // help us since we cannot use concurrency anyway
                                     // due to the issues we're having with CME's and
                                     // the lack of thread safety.
    } )
@Concurrent()
public class CoreSuite
{
    @ClassRule
    public static CassandraResource cassandraResource = CassandraResource.newWithAvailablePorts( "coreManager" );
}
