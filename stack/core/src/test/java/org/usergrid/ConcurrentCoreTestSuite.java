package org.usergrid;


import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.usergrid.cassandra.Concurrent;
import org.usergrid.cassandra.ConcurrentSuite;
import org.usergrid.locking.zookeeper.ZookeeperLockManagerTest;
import org.usergrid.mq.QueuePathsTest;
import org.usergrid.persistence.*;
import org.usergrid.persistence.cassandra.QueryProcessorTest;
import org.usergrid.persistence.cassandra.SimpleIndexBucketLocatorImplTest;
import org.usergrid.persistence.query.ir.result.IntersectionIteratorTest;
import org.usergrid.persistence.query.ir.result.SubtractionIteratorTest;
import org.usergrid.persistence.query.ir.result.UnionIteratorTest;
import org.usergrid.persistence.query.tree.GrammarTreeTest;
import org.usergrid.persistence.query.tree.LongLiteralTest;
import org.usergrid.persistence.query.tree.StringLiteralTest;


@RunWith( ConcurrentSuite.class )
@Suite.SuiteClasses(
    {
            ZookeeperLockManagerTest.class,
            QueuePathsTest.class,
            QueryProcessorTest.class,
            SimpleIndexBucketLocatorImplTest.class,
            EntityTest.class,
            QueryTest.class,
            QueryUtilsTest.class,
            SchemaTest.class,
            UtilsTest.class,
            IntersectionIteratorTest.class,
            SubtractionIteratorTest.class,
            UnionIteratorTest.class,
            GrammarTreeTest.class,
            LongLiteralTest.class,
            StringLiteralTest.class
    } )
@Concurrent()
public class ConcurrentCoreTestSuite
{
}
