package org.usergrid;


import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.usergrid.cassandra.CassandraResource;
import org.usergrid.cassandra.Concurrent;
import org.usergrid.cassandra.ConcurrentSuite;
import org.usergrid.locking.cassandra.HectorLockManagerIT;
import org.usergrid.mq.MessagesIT;
import org.usergrid.persistence.*;
import org.usergrid.persistence.cassandra.EntityManagerFactoryImplIT;
import org.usergrid.system.UsergridSystemMonitorIT;


@RunWith( ConcurrentSuite.class )
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
        EntityManagerFactoryImplIT.class,
        AnnotationDrivenIntIT.class,

    } )
@Concurrent()
public class ConcurrentCoreITSuite
{
    @ClassRule
    public static CassandraResource cassandraResource = CassandraResource.newWithAvailablePorts( "coreManager" );
}
