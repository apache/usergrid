/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.usergrid.corepersistence.migration;

import org.apache.usergrid.NewOrgAppAdminRule;
import org.apache.usergrid.ServiceITSetup;
import org.apache.usergrid.ServiceITSetupImpl;
import org.apache.usergrid.cassandra.ClearShiroSubject;
import org.apache.usergrid.management.OrganizationOwnerInfo;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.persistence.SimpleEntityRef;
import org.apache.usergrid.persistence.core.migration.data.ProgressObserver;
import org.apache.usergrid.persistence.index.query.Query;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.*;

import static org.apache.usergrid.TestHelper.uniqueEmail;
import static org.apache.usergrid.TestHelper.uniqueOrg;
import static org.apache.usergrid.TestHelper.uniqueUsername;
import static org.junit.Assert.*;


/**
 * Really a Core module test but it needs to be here so that it can use
 * ManagementService's application and organization logic.
 */
public class AppInfoMigrationPluginTest {

    @Rule
    public ClearShiroSubject clearShiroSubject = new ClearShiroSubject();

    @ClassRule
    public static ServiceITSetup setup = new ServiceITSetupImpl();

    @Rule
    public NewOrgAppAdminRule orgAppRule = new NewOrgAppAdminRule( setup );


    @Test
    public void testRun() throws Exception {

        // create 10 applications, each with 10 entities

        final String orgName =  uniqueOrg();
        OrganizationOwnerInfo organization =  orgAppRule.createOwnerAndOrganization(
            orgName, uniqueUsername(), uniqueEmail(),"Ed Anuff", "test" );

        List<UUID> appIds = new ArrayList<>();

        for ( int i=0; i<10; i++ ) {

            UUID appId = setup.getMgmtSvc().createApplication(
                organization.getOrganization().getUuid(), "application" + i ).getId();
            appIds.add( appId );

            EntityManager em = setup.getEmf().getEntityManager( appId );
            for ( int j=0; j<10; j++ ) {
                final String entityName = "thing" + j;
                em.create("thing", new HashMap<String, Object>() {{
                    put("name", entityName );
                }});
            }
        }

        assertNotNull("Should be able to get application",
            setup.getEmf().lookupApplication(orgName + "/application0"));

        // create corresponding 10 appinfo entities in Management app
        // and delete the application_info entities from the Management app

        UUID mgmtAppId = setup.getEmf().getManagementAppId();
        EntityManager rootEm = setup.getEmf().getEntityManager( mgmtAppId );

        for ( UUID appId : appIds ) {
            final String finalOrgId = organization.getOrganization().getUuid().toString();
            final String finalAppId = appId.toString();
            rootEm.create("appinfo", new HashMap<String, Object>() {{
                put("organizationUuid", finalOrgId );
                put("applicationUuid", finalAppId );
            }});
            rootEm.delete( new SimpleEntityRef("application_info", appId ));
        }

        setup.getEmf().flushEntityManagerCaches();

        // test that applications are now borked

        assertNull("Should not be able to get application",
            setup.getEmf().lookupApplication(orgName + "/application0"));

        // run the migration, which should restore the application_info entities

        ProgressObserver po = Mockito.mock(ProgressObserver.class);
        AppInfoMigrationPlugin plugin = new AppInfoMigrationPlugin();
        plugin.emf = setup.getEmf();
        plugin.run( po );

        // test that expected calls were made the to progress observer (use mock library)

        Mockito.verify( po, Mockito.times(10) ).update( Mockito.anyInt(), Mockito.anyString() );
        setup.getEmf().refreshIndex();

        // test that 10 appinfo entities have been removed

        final Results appInfoResults = rootEm.searchCollection(
            new SimpleEntityRef("application", mgmtAppId), "appinfos", Query.fromQL("select *"));
        assertEquals( 0, appInfoResults.size() );

        final Results applicationInfoResults = rootEm.searchCollection(
            new SimpleEntityRef("application", mgmtAppId), "application_infos", Query.fromQL("select *"));
        assertEquals( 10, applicationInfoResults.size() );

        // test that 10 applications are no longer borked

        assertNotNull("Should be able to get application",
            setup.getEmf().lookupApplication(orgName + "/application0"));
    }
}
