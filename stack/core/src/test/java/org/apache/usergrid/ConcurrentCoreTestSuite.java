package org.apache.usergrid;


import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.apache.usergrid.cassandra.Concurrent;
import org.apache.usergrid.cassandra.ConcurrentSuite;
import org.apache.usergrid.locking.zookeeper.ZookeeperLockManagerTest;
import org.apache.usergrid.mq.QueuePathsTest;
import org.apache.usergrid.persistence.EntityTest;
import org.apache.usergrid.persistence.QueryTest;
import org.apache.usergrid.persistence.QueryUtilsTest;
import org.apache.usergrid.persistence.SchemaTest;
import org.apache.usergrid.persistence.UtilsTest;
import org.apache.usergrid.persistence.cassandra.QueryProcessorTest;
import org.apache.usergrid.persistence.cassandra.SimpleIndexBucketLocatorImplTest;
import org.apache.usergrid.persistence.query.ir.result.IntersectionIteratorTest;
import org.apache.usergrid.persistence.query.ir.result.SubtractionIteratorTest;
import org.apache.usergrid.persistence.query.ir.result.UnionIteratorTest;
import org.apache.usergrid.persistence.query.tree.GrammarTreeTest;
import org.apache.usergrid.persistence.query.tree.LongLiteralTest;
import org.apache.usergrid.persistence.query.tree.StringLiteralTest;


@RunWith(ConcurrentSuite.class)
@Suite.SuiteClasses({
        ZookeeperLockManagerTest.class, QueuePathsTest.class, QueryProcessorTest.class,
        SimpleIndexBucketLocatorImplTest.class, EntityTest.class, QueryTest.class, QueryUtilsTest.class,
        SchemaTest.class, UtilsTest.class, IntersectionIteratorTest.class, SubtractionIteratorTest.class,
        UnionIteratorTest.class, GrammarTreeTest.class, LongLiteralTest.class, StringLiteralTest.class
})
@Concurrent()
public class ConcurrentCoreTestSuite {}
