package org.apache.usergrid;


import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.apache.usergrid.cassandra.CassandraResource;
import org.apache.usergrid.cassandra.Concurrent;
import org.apache.usergrid.cassandra.ConcurrentSuite;
import org.apache.usergrid.persistence.query.AllInCollectionIT;
import org.apache.usergrid.persistence.query.AllInConnectionIT;
import org.apache.usergrid.persistence.query.AllInConnectionNoTypeIT;
import org.apache.usergrid.persistence.query.MultiOrderByCollectionIT;
import org.apache.usergrid.persistence.query.MultiOrderByComplexUnionCollectionIT;
import org.apache.usergrid.persistence.query.MultiOrderByComplexUnionConnectionIT;
import org.apache.usergrid.persistence.query.SingleOrderByBoundRangeScanAscCollectionIT;
import org.apache.usergrid.persistence.query.SingleOrderByBoundRangeScanAscConnectionIT;
import org.apache.usergrid.persistence.query.SingleOrderByBoundRangeScanDescCollectionIT;
import org.apache.usergrid.persistence.query.SingleOrderByBoundRangeScanDescConnectionIT;
import org.apache.usergrid.persistence.query.SingleOrderByComplexIntersectionCollectionIT;
import org.apache.usergrid.persistence.query.SingleOrderByComplexIntersectionConnectionIT;
import org.apache.usergrid.persistence.query.SingleOrderByComplexUnionCollectionIT;
import org.apache.usergrid.persistence.query.SingleOrderByComplexUnionConnectionIT;
import org.apache.usergrid.persistence.query.SingleOrderByLessThanLimitCollectionIT;
import org.apache.usergrid.persistence.query.SingleOrderByLessThanLimitConnectionIT;
import org.apache.usergrid.persistence.query.SingleOrderByMaxLimitCollectionIT;
import org.apache.usergrid.persistence.query.SingleOrderByMaxLimitConnectionIT;
import org.apache.usergrid.persistence.query.SingleOrderByNoIntersectionCollectionIT;
import org.apache.usergrid.persistence.query.SingleOrderByNoIntersectionConnectionIT;
import org.apache.usergrid.persistence.query.SingleOrderByNotCollectionIT;
import org.apache.usergrid.persistence.query.SingleOrderByNotConnectionIT;
import org.apache.usergrid.persistence.query.SingleOrderBySameRangeScanGreaterCollectionIT;
import org.apache.usergrid.persistence.query.SingleOrderBySameRangeScanGreaterConnectionIT;
import org.apache.usergrid.persistence.query.SingleOrderBySameRangeScanGreaterThanEqualCollectionIT;
import org.apache.usergrid.persistence.query.SingleOrderBySameRangeScanLessCollectionIT;
import org.apache.usergrid.persistence.query.SingleOrderBySameRangeScanLessConnectionIT;
import org.apache.usergrid.persistence.query.SingleOrderBySameRangeScanLessThanEqualCollectionIT;
import org.apache.usergrid.persistence.query.SingleOrderBySameRangeScanLessThanEqualConnectionIT;


@RunWith(ConcurrentSuite.class)
@Suite.SuiteClasses({
        AllInCollectionIT.class, AllInConnectionIT.class, AllInConnectionNoTypeIT.class, MultiOrderByCollectionIT.class,
        MultiOrderByComplexUnionCollectionIT.class, MultiOrderByComplexUnionConnectionIT.class,
        SingleOrderByBoundRangeScanAscCollectionIT.class, SingleOrderByBoundRangeScanAscConnectionIT.class,
        SingleOrderByBoundRangeScanDescCollectionIT.class, SingleOrderByBoundRangeScanDescConnectionIT.class,
        SingleOrderByComplexIntersectionCollectionIT.class, SingleOrderByComplexIntersectionConnectionIT.class,
        SingleOrderByComplexUnionCollectionIT.class, SingleOrderByComplexUnionConnectionIT.class,
        SingleOrderByLessThanLimitCollectionIT.class, SingleOrderByLessThanLimitConnectionIT.class,
        SingleOrderByMaxLimitCollectionIT.class, SingleOrderByMaxLimitConnectionIT.class,
        SingleOrderByNoIntersectionCollectionIT.class, SingleOrderByNoIntersectionConnectionIT.class,
        SingleOrderByNotCollectionIT.class, SingleOrderByNotConnectionIT.class,
        SingleOrderBySameRangeScanGreaterCollectionIT.class, SingleOrderBySameRangeScanGreaterConnectionIT.class,
        SingleOrderBySameRangeScanGreaterThanEqualCollectionIT.class, SingleOrderBySameRangeScanLessCollectionIT.class,
        SingleOrderBySameRangeScanLessConnectionIT.class, SingleOrderBySameRangeScanLessThanEqualCollectionIT.class,
        SingleOrderBySameRangeScanLessThanEqualConnectionIT.class
})
@Concurrent(threads = 15)
public class ConcurrentCoreIteratorITSuite {
    @ClassRule
    public static CassandraResource cassandraResource = CassandraResource.newWithAvailablePorts( "coreManager" );
}
