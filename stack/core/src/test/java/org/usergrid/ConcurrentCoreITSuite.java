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
import org.usergrid.persistence.query.AllInCollectionIT;
import org.usergrid.persistence.query.AllInConnectionIT;
import org.usergrid.persistence.query.AllInConnectionNoTypeIT;
import org.usergrid.persistence.query.MultiOrderByCollectionIT;
import org.usergrid.persistence.query.MultiOrderByComplexUnionCollectionIT;
import org.usergrid.persistence.query.MultiOrderByComplexUnionConnectionIT;
import org.usergrid.persistence.query.MultiOrderByConnectionIT;
import org.usergrid.persistence.query.SingleOrderByBoundRangeScanAscCollectionIT;
import org.usergrid.persistence.query.SingleOrderByBoundRangeScanAscConnectionIT;
import org.usergrid.persistence.query.SingleOrderByBoundRangeScanDescCollectionIT;
import org.usergrid.persistence.query.SingleOrderByBoundRangeScanDescConnectionIT;
import org.usergrid.persistence.query.SingleOrderByComplexIntersectionCollectionIT;
import org.usergrid.persistence.query.SingleOrderByComplexIntersectionConnectionIT;
import org.usergrid.persistence.query.SingleOrderByComplexUnionCollectionIT;
import org.usergrid.persistence.query.SingleOrderByComplexUnionConnectionIT;
import org.usergrid.persistence.query.SingleOrderByLessThanLimitCollectionIT;
import org.usergrid.persistence.query.SingleOrderByLessThanLimitConnectionIT;
import org.usergrid.persistence.query.SingleOrderByMaxLimitCollectionIT;
import org.usergrid.persistence.query.SingleOrderByMaxLimitConnectionIT;
import org.usergrid.persistence.query.SingleOrderByNoIntersectionCollectionIT;
import org.usergrid.persistence.query.SingleOrderByNoIntersectionConnectionIT;
import org.usergrid.persistence.query.SingleOrderByNotCollectionIT;
import org.usergrid.persistence.query.SingleOrderByNotConnectionIT;
import org.usergrid.persistence.query.SingleOrderBySameRangeScanGreaterCollectionIT;
import org.usergrid.persistence.query.SingleOrderBySameRangeScanGreaterConnectionIT;
import org.usergrid.persistence.query.SingleOrderBySameRangeScanGreaterThanEqualCollectionIT;
import org.usergrid.persistence.query.SingleOrderBySameRangeScanLessCollectionIT;
import org.usergrid.persistence.query.SingleOrderBySameRangeScanLessConnectionIT;
import org.usergrid.persistence.query.SingleOrderBySameRangeScanLessThanEqualCollectionIT;
import org.usergrid.persistence.query.SingleOrderBySameRangeScanLessThanEqualConnectionIT;
import org.usergrid.system.UsergridSystemMonitorIT;


@RunWith( ConcurrentSuite.class )
@Suite.SuiteClasses(
    {
//        HectorLockManagerIT.class,
        UsergridSystemMonitorIT.class,
        CollectionIT.class,
        CounterIT.class,
        EntityConnectionsIT.class,
        EntityDictionaryIT.class,
        EntityManagerIT.class,
        GeoIT.class,
        IndexIT.class,
        MessagesIT.class,
        PermissionsIT.class,
        PathQueryIT.class,
        EntityManagerFactoryImplIT.class
    } )
@Concurrent()
public class ConcurrentCoreITSuite
{
    @ClassRule
    public static CassandraResource cassandraResource = CassandraResource.newWithAvailablePorts( "coreManager" );
}
