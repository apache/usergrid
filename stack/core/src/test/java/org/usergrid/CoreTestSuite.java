/*
 * Copyright 2012 Apigee Corporation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.usergrid;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.usergrid.cassandra.Concurrent;
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

@RunWith(Suite.class)
@Suite.SuiteClasses({
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
})
@Concurrent()
public class CoreTestSuite {
}
