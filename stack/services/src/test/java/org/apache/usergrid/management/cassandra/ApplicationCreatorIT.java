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
package org.apache.usergrid.management.cassandra;


import java.util.Set;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.ServiceITSetup;
import org.apache.usergrid.ServiceITSetupImpl;
import org.apache.usergrid.cassandra.CassandraResource;
import org.apache.usergrid.cassandra.ClearShiroSubject;
import org.apache.usergrid.cassandra.Concurrent;
import org.apache.usergrid.management.ApplicationInfo;
import org.apache.usergrid.management.OrganizationOwnerInfo;
import org.apache.usergrid.persistence.index.impl.ElasticSearchResource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


/** @author zznate */
@Concurrent()
public class ApplicationCreatorIT {
    private static final Logger LOG = LoggerFactory.getLogger( ApplicationCreatorIT.class );

    @ClassRule
    public static CassandraResource cassandraResource = CassandraResource.newWithAvailablePorts();

    @ClassRule
    public static ElasticSearchResource elasticSearchResource = new ElasticSearchResource();

    @Rule
    public ClearShiroSubject clearShiroSubject = new ClearShiroSubject();

    @Rule
    public ServiceITSetup setup = new ServiceITSetupImpl( cassandraResource, elasticSearchResource );


    @Test
    public void testCreateSampleApplication() throws Exception {
        OrganizationOwnerInfo orgOwner = setup.getMgmtSvc()
                                              .createOwnerAndOrganization( "appcreatortest", "nate-appcreatortest",
                                                      "Nate", "nate+appcreatortest@apigee.com", "password", true,
                                                      false );

        ApplicationInfo appInfo = setup.getAppCreator().createSampleFor( orgOwner.getOrganization() );
        assertNotNull( appInfo );
        assertEquals( "appcreatortest/sandbox", appInfo.getName() );

        Set<String> rolePerms = setup.getEmf().getEntityManager( appInfo.getId() ).getRolePermissions( "guest" );
        assertNotNull( rolePerms );
        assertTrue( rolePerms.contains( "get,post,put,delete:/**" ) );
    }


    @Test
    public void testCreateSampleApplicationAltName() throws Exception {
        OrganizationOwnerInfo orgOwner = setup.getMgmtSvc().createOwnerAndOrganization( "appcreatortestcustom",
                "nate-appcreatortestcustom", "Nate", "nate+appcreatortestcustom@apigee.com", "password", true, false );

        ApplicationCreatorImpl customCreator = new ApplicationCreatorImpl( setup.getEmf(), setup.getMgmtSvc() );
        customCreator.setSampleAppName( "messagee" );
        ApplicationInfo appInfo = customCreator.createSampleFor( orgOwner.getOrganization() );
        assertNotNull( appInfo );
        assertEquals( "appcreatortestcustom/messagee", appInfo.getName() );
    }
}
