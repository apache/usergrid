package org.usergrid;


import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.usergrid.cassandra.CassandraResource;
import org.usergrid.cassandra.Concurrent;
import org.usergrid.locking.cassandra.HectorLockManagerIT;
import org.usergrid.mq.MessagesIT;
import org.usergrid.persistence.CollectionIT;
import org.usergrid.persistence.CounterIT;
import org.usergrid.persistence.EntityConnectionsIT;
import org.usergrid.persistence.EntityDictionaryIT;
import org.usergrid.persistence.EntityManagerIT;
import org.usergrid.persistence.GeoIT;
import org.usergrid.persistence.IndexIT;
import org.usergrid.persistence.PathQueryIT;
import org.usergrid.persistence.PermissionsIT;
import org.usergrid.persistence.cassandra.EntityManagerFactoryImplIT;
import org.usergrid.system.UsergridSystemMonitorIT;


@RunWith(Suite.class)
@Suite.SuiteClasses({
        HectorLockManagerIT.class, UsergridSystemMonitorIT.class, CollectionIT.class, CounterIT.class,
        EntityConnectionsIT.class, EntityDictionaryIT.class, EntityManagerIT.class, GeoIT.class, IndexIT.class,
        MessagesIT.class, PermissionsIT.class, PathQueryIT.class, EntityManagerFactoryImplIT.class
})
@Concurrent()
public class CoreITSuite {
    @ClassRule
    public static CassandraResource cassandraResource = CassandraResource.newWithAvailablePorts( "coreManager" );
}
