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

import org.apache.usergrid.corepersistence.ApplicationIdCacheImpl;
import org.junit.Rule;
import org.junit.Test;

import org.apache.usergrid.ServiceITSetup;
import org.apache.usergrid.ServiceITSetupImpl;
import org.apache.usergrid.cassandra.ClearShiroSubject;

import org.apache.usergrid.management.ApplicationInfo;
import org.apache.usergrid.management.OrganizationOwnerInfo;

import static org.apache.usergrid.TestHelper.uniqueApp;
import static org.apache.usergrid.TestHelper.uniqueEmail;
import static org.apache.usergrid.TestHelper.uniqueOrg;
import static org.apache.usergrid.TestHelper.uniqueUsername;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


/** @author zznate */

public class ApplicationCreatorIT {
    @Rule
    public ClearShiroSubject clearShiroSubject = new ClearShiroSubject();

    @Rule
    public ServiceITSetup setup = new ServiceITSetupImpl();


    @Test
    public void testCreateSampleApplication() throws Exception {

        final String orgName = uniqueOrg();
        final String appName = uniqueApp();
        final String expecteAppname = "sandbox";
        final String expectedName = orgName + "/" + expecteAppname;

        OrganizationOwnerInfo orgOwner = setup.getMgmtSvc().createOwnerAndOrganization( orgName, appName, uniqueUsername(),
                uniqueEmail(), "password", true, false );

        ApplicationInfo appInfo = setup.getAppCreator().createSampleFor( orgOwner.getOrganization() );
        if(appInfo == null){
            appInfo = setup.getMgmtSvc().getApplicationInfo("sandbox");
        }
        assertNotNull( appInfo );
        assertEquals(expectedName, appInfo.getName());

        Set<String> rolePerms = setup.getEmf().getEntityManager( appInfo.getId() ).getRolePermissions( "guest" );
        assertNotNull( rolePerms );
        assertTrue( rolePerms.contains( "get,post,put,delete:/**") );
    }


    @Test
    public void testCreateSampleApplicationAltName() throws Exception {

        final String orgName = uniqueOrg();
        final String appName = uniqueApp();
        final String sampleAppName =  "messagee" ;
        final String expectedName = orgName + "/" + sampleAppName;

        OrganizationOwnerInfo orgOwner = setup.getMgmtSvc().createOwnerAndOrganization( orgName, appName, uniqueUsername(),
                uniqueEmail(), "password", true, false );

        ApplicationCreatorImpl customCreator = new ApplicationCreatorImpl( setup.getEmf(), setup.getMgmtSvc() );
        customCreator.setSampleAppName(sampleAppName);
        ApplicationInfo appInfo = customCreator.createSampleFor( orgOwner.getOrganization() );
        if(appInfo == null){
            setup.refreshIndex(setup.getEmf().getManagementAppId());
        }
        assertNotNull( appInfo );
        assertEquals( expectedName, appInfo.getName() );
    }
}
