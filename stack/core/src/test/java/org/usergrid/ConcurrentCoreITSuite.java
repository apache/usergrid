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


import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.usergrid.cassandra.CassandraResource;
import org.usergrid.cassandra.Concurrent;
import org.usergrid.cassandra.ConcurrentSuite;
import org.usergrid.mq.MessagesIT;
import org.usergrid.persistence.*;
import org.usergrid.persistence.cassandra.EntityManagerFactoryImplIT;
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
