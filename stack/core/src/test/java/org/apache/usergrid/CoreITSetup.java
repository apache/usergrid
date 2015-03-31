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


import java.util.UUID;

import com.google.inject.Injector;
import org.apache.usergrid.persistence.index.EntityIndex;
import org.junit.rules.TestRule;

import org.apache.usergrid.mq.QueueManagerFactory;
import org.apache.usergrid.persistence.EntityManagerFactory;
import org.apache.usergrid.persistence.IndexBucketLocator;
import org.apache.usergrid.persistence.cassandra.CassandraService;


public interface CoreITSetup extends TestRule {

    EntityManagerFactory getEmf();

    QueueManagerFactory getQmf();

    IndexBucketLocator getIbl();

    CassandraService getCassSvc();

    UUID createApplication( String organizationName, String applicationName ) throws Exception;

    void dump( String name, Object obj );

    Injector getInjector();

    TestEntityIndex getEntityIndex();
}
