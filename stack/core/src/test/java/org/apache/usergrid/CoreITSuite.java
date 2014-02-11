package org.apache.usergrid;


import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.apache.usergrid.cassandra.CassandraResource;
import org.apache.usergrid.cassandra.Concurrent;
import org.apache.usergrid.locking.cassandra.HectorLockManagerIT;
import org.apache.usergrid.mq.MessagesIT;
import org.apache.usergrid.persistence.CollectionIT;
import org.apache.usergrid.persistence.CounterIT;
import org.apache.usergrid.persistence.EntityConnectionsIT;
import org.apache.usergrid.persistence.EntityDictionaryIT;
import org.apache.usergrid.persistence.EntityManagerIT;
import org.apache.usergrid.persistence.GeoIT;
import org.apache.usergrid.persistence.IndexIT;
import org.apache.usergrid.persistence.PathQueryIT;
import org.apache.usergrid.persistence.PermissionsIT;
import org.apache.usergrid.persistence.cassandra.EntityManagerFactoryImplIT;
import org.apache.usergrid.system.UsergridSystemMonitorIT;


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
