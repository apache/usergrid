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
package org.apache.usergrid.rest;


import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.apache.usergrid.cassandra.CassandraResource;
import org.apache.usergrid.cassandra.Concurrent;
import org.apache.usergrid.cassandra.ConcurrentSuite;
import org.apache.usergrid.rest.applications.ApplicationRequestCounterIT;
import org.apache.usergrid.rest.applications.ApplicationResourceIT;
import org.apache.usergrid.rest.applications.DevicesResourceIT;
import org.apache.usergrid.rest.applications.assets.AssetResourceIT;
import org.apache.usergrid.rest.applications.collection.PagingResourceIT;
import org.apache.usergrid.rest.applications.events.EventsResourceIT;
import org.apache.usergrid.rest.applications.users.ActivityResourceIT;
import org.apache.usergrid.rest.applications.users.CollectionsResourceIT;
import org.apache.usergrid.rest.applications.users.GroupResourceIT;
import org.apache.usergrid.rest.applications.users.OwnershipResourceIT;
import org.apache.usergrid.rest.applications.users.PermissionsResourceIT;
import org.apache.usergrid.rest.applications.users.UserResourceIT;
import org.apache.usergrid.rest.filters.ContentTypeResourceIT;
import org.apache.usergrid.rest.management.ManagementResourceIT;
import org.apache.usergrid.rest.management.RegistrationIT;
import org.apache.usergrid.rest.management.organizations.AdminEmailEncodingIT;
import org.apache.usergrid.rest.management.users.organizations.UsersOrganizationsResourceIT;


@RunWith(ConcurrentSuite.class)
@Suite.SuiteClasses(
        {
                ActivityResourceIT.class, AdminEmailEncodingIT.class, ApplicationRequestCounterIT.class,
                ApplicationResourceIT.class, AssetResourceIT.class, BasicIT.class, CollectionsResourceIT.class,
                ContentTypeResourceIT.class, DevicesResourceIT.class, EventsResourceIT.class, GroupResourceIT.class,
                MUUserResourceIT.class, ManagementResourceIT.class, OrganizationResourceIT.class,
                OrganizationsResourceIT.class, OwnershipResourceIT.class, PagingResourceIT.class,
                PermissionsResourceIT.class, RegistrationIT.class, UserResourceIT.class,
                UsersOrganizationsResourceIT.class
        })
@Concurrent()
public class ConcurrentRestITSuite {
    @ClassRule
    public static CassandraResource cassandraResource = CassandraResource.newWithAvailablePorts();
}
