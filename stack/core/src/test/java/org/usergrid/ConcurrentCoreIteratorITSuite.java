package org.usergrid;


import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.usergrid.cassandra.CassandraResource;
import org.usergrid.cassandra.Concurrent;
import org.usergrid.cassandra.ConcurrentSuite;
import org.usergrid.persistence.query.AllInCollectionIT;
import org.usergrid.persistence.query.AllInConnectionIT;
import org.usergrid.persistence.query.AllInConnectionNoTypeIT;
import org.usergrid.persistence.query.MultiOrderByCollectionIT;
import org.usergrid.persistence.query.MultiOrderByComplexUnionCollectionIT;
import org.usergrid.persistence.query.MultiOrderByComplexUnionConnectionIT;
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


@RunWith( ConcurrentSuite.class )
@Suite.SuiteClasses(
    {
        AllInCollectionIT.class,
        AllInConnectionIT.class,
        AllInConnectionNoTypeIT.class,
        MultiOrderByCollectionIT.class,
        MultiOrderByComplexUnionCollectionIT.class,
        MultiOrderByComplexUnionConnectionIT.class,
        SingleOrderByBoundRangeScanAscCollectionIT.class,
        SingleOrderByBoundRangeScanAscConnectionIT.class,
        SingleOrderByBoundRangeScanDescCollectionIT.class,
        SingleOrderByBoundRangeScanDescConnectionIT.class,
        SingleOrderByComplexIntersectionCollectionIT.class,
        SingleOrderByComplexIntersectionConnectionIT.class,
        SingleOrderByComplexUnionCollectionIT.class,
        SingleOrderByComplexUnionConnectionIT.class,
        SingleOrderByLessThanLimitCollectionIT.class,
        SingleOrderByLessThanLimitConnectionIT.class,
        SingleOrderByMaxLimitCollectionIT.class,
        SingleOrderByMaxLimitConnectionIT.class,
        SingleOrderByNoIntersectionCollectionIT.class,
        SingleOrderByNoIntersectionConnectionIT.class,
        SingleOrderByNotCollectionIT.class,
        SingleOrderByNotConnectionIT.class,
        SingleOrderBySameRangeScanGreaterCollectionIT.class,
        SingleOrderBySameRangeScanGreaterConnectionIT.class,
        SingleOrderBySameRangeScanGreaterThanEqualCollectionIT.class,
        SingleOrderBySameRangeScanLessCollectionIT.class,
        SingleOrderBySameRangeScanLessConnectionIT.class,
        SingleOrderBySameRangeScanLessThanEqualCollectionIT.class,
        SingleOrderBySameRangeScanLessThanEqualConnectionIT.class
    } )
@Concurrent( threads = 15 )
public class ConcurrentCoreIteratorITSuite
{
    @ClassRule
    public static CassandraResource cassandraResource = CassandraResource.newWithAvailablePorts( "coreManager" );
}
