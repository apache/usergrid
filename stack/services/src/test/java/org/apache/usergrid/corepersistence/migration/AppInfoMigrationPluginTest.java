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
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.management.OrganizationOwnerInfo;
import org.apache.usergrid.persistence.*;
import org.apache.usergrid.persistence.cassandra.CassandraService;
import org.apache.usergrid.persistence.core.migration.data.ProgressObserver;
import org.apache.usergrid.persistence.entities.Application;
import org.apache.usergrid.persistence.index.query.Query;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static org.apache.usergrid.TestHelper.*;
import static org.junit.Assert.*;


/**
 * Really a Core module test but it needs to be here so that it can use
 * ManagementService's application and organization logic.
 */
public class AppInfoMigrationPluginTest {
    private static final Logger logger = LoggerFactory.getLogger(AppInfoMigrationPluginTest.class);

    @Rule
    public ClearShiroSubject clearShiroSubject = new ClearShiroSubject();

    @ClassRule
    public static ServiceITSetup setup = new ServiceITSetupImpl();

    @Rule
    public NewOrgAppAdminRule orgAppRule = new NewOrgAppAdminRule( setup );


    @Test
    public void testRun() throws Exception {

       // create 10 applications, each with 10 entities

        logger.debug("\n\nCreate 10 apps each with 10 entities");

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

        setup.refreshIndex();

        UUID mgmtAppId = setup.getEmf().getManagementAppId();
        EntityManager rootEm = setup.getEmf().getEntityManager( mgmtAppId );

        checkApplicationsOk( orgName );

        // create corresponding 10 appinfo entities in Management app
        // and delete the application_info entities from the Management app

        logger.debug("\n\nCreate old-style appinfo entities to be migrated\n");

        List<Entity> deletedApps = new ArrayList<>();

        setup.getEmf().initializeApplicationV2(
            CassandraService.DEFAULT_ORGANIZATION, CpNamingUtils.SYSTEM_APP_ID, "systemapp", null);

        EntityManager systemAppEm = setup.getEmf().getEntityManager( CpNamingUtils.SYSTEM_APP_ID );

        int count = 0;
        for ( UUID appId : appIds ) {

            final Entity applicationInfo = getApplicationInfo( appId );

            final String appName = applicationInfo.getName();
            final String finalOrgId = organization.getOrganization().getUuid().toString();
            final String finalAppId = applicationInfo.getProperty( Schema.PROPERTY_APPLICATION_ID ).toString();
            systemAppEm.create("appinfo", new HashMap<String, Object>() {{
                put("name", appName );
                put("organizationUuid", finalOrgId );
                put("applicationUuid", finalAppId );
            }});

            // delete some but not all of the application_info entities
            // so that we cover both create and update cases

            if ( count++ % 2 == 0 ) {
                rootEm.delete( applicationInfo );
                deletedApps.add( applicationInfo );
            }
        }

        setup.refreshIndex();

        setup.getEmf().flushEntityManagerCaches();

        Thread.sleep(1000);

        // test that applications are now broken

        checkApplicationsBroken(orgName, deletedApps);

        // run the migration, which should restore the application_info entities

        logger.debug("\n\nRun the migration\n");

        ProgressObserver po = Mockito.mock(ProgressObserver.class);
        setup.getAppInfoMigrationPlugin().run(po);

        logger.debug("\n\nVerify migration results\n");

        // test that expected calls were made the to progress observer (use mock library)

        Mockito.verify( po, Mockito.times(10) ).update( Mockito.anyInt(), Mockito.anyString() );
        setup.refreshIndex();

        final Results appInfoResults = rootEm.searchCollection(
            new SimpleEntityRef("application", mgmtAppId), "appinfos", Query.fromQL("select *"));
        assertEquals( 0, appInfoResults.size() );

        final Results applicationInfoResults = rootEm.searchCollection(
            new SimpleEntityRef("application", mgmtAppId), "application_infos", Query.fromQL("select *"));
        assertEquals( 10, applicationInfoResults.size() );

        // test that 10 applications are no longer broken

        checkApplicationsOk( orgName );
    }

    private void checkApplicationsBroken( String orgName, List<Entity> deletedApps ) throws Exception {

        logger.debug("\n\nChecking applications broken\n");


        for ( Entity applicationInfo : deletedApps ) {

            String appName = applicationInfo.getName();
            UUID uuid = setup.getEmf().lookupApplication( appName );

            // missing application_info does not completely break applications, but we...
            assertNull("Should not be able to lookup deleted application by name" + appName, uuid);
        }
    }

    private void checkApplicationsOk( String orgName) throws Exception {

        logger.debug("\n\nChecking applications OK\n");

        for (int i=0; i<10; i++) {

            String appName = orgName + "/application" + i;

            UUID uuid = setup.getEmf().lookupApplication( appName );
            assertNotNull("Should be able to get application", uuid );

            EntityManager em = setup.getEmf().getEntityManager( uuid );

            Application app = em.getApplication();
            assertEquals( appName, app.getName() );

            Results results = em.searchCollection(
                em.getApplicationRef(), "things", Query.fromQL("select *"));
            assertEquals( "Should have 10 entities", 10, results.size() );
        }
    }

    private Entity getApplicationInfo( UUID appId ) throws Exception {

        UUID mgmtAppId = setup.getEmf().getManagementAppId();
        EntityManager rootEm = setup.getEmf().getEntityManager( mgmtAppId );

        final Results applicationInfoResults = rootEm.searchCollection(
            new SimpleEntityRef("application", mgmtAppId), "application_infos",
            Query.fromQL("select * where applicationId=" + appId.toString()));

        return applicationInfoResults.getEntity();
    }
}
