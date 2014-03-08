/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.usergrid;


import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.apache.usergrid.cassandra.Concurrent;
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


@RunWith(Suite.class)
@Suite.SuiteClasses({
        ZookeeperLockManagerTest.class, QueuePathsTest.class, QueryProcessorTest.class,
        SimpleIndexBucketLocatorImplTest.class, EntityTest.class, QueryTest.class, QueryUtilsTest.class,
        SchemaTest.class, UtilsTest.class, IntersectionIteratorTest.class, SubtractionIteratorTest.class,
        UnionIteratorTest.class, GrammarTreeTest.class, LongLiteralTest.class, StringLiteralTest.class
})
@Concurrent()
public class CoreTestSuite {}
