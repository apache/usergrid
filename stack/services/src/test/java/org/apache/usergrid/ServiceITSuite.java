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
import org.apache.usergrid.management.EmailFlowIT;
import org.apache.usergrid.management.OrganizationIT;
import org.apache.usergrid.management.RoleIT;
import org.apache.usergrid.management.cassandra.ApplicationCreatorIT;
import org.apache.usergrid.management.cassandra.ManagementServiceIT;
import org.apache.usergrid.security.providers.FacebookProviderIT;
import org.apache.usergrid.security.providers.PingIdentityProviderIT;
import org.apache.usergrid.services.ActivitiesServiceIT;
import org.apache.usergrid.services.ApplicationsServiceIT;
import org.apache.usergrid.services.CollectionServiceIT;
import org.apache.usergrid.services.ConnectionsServiceIT;
import org.apache.usergrid.services.GroupServiceIT;
import org.apache.usergrid.services.RolesServiceIT;
import org.apache.usergrid.services.ServiceFactoryIT;
import org.apache.usergrid.services.ServiceInvocationIT;
import org.apache.usergrid.services.ServiceRequestIT;
import org.apache.usergrid.services.UsersServiceIT;


@RunWith(Suite.class)
@Suite.SuiteClasses(
        {
                ActivitiesServiceIT.class, ApplicationCreatorIT.class, ApplicationsServiceIT.class,
                CollectionServiceIT.class, ConnectionsServiceIT.class, ManagementServiceIT.class, EmailFlowIT.class,
                FacebookProviderIT.class, GroupServiceIT.class, OrganizationIT.class, PingIdentityProviderIT.class,
                RoleIT.class, RolesServiceIT.class, ServiceRequestIT.class, ServiceFactoryIT.class,
                ServiceInvocationIT.class, UsersServiceIT.class
        })
@Concurrent()
public class ServiceITSuite {
    @ClassRule
    public static CassandraResource cassandraResource = CassandraResource.newWithAvailablePorts();
}
