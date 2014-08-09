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

    @ClassRule
    public static ElasticSearchResource elasticSearchResource = ElasticSearchResource.instance;

}
