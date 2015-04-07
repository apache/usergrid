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

package org.apache.usergrid.rest.applications;


import org.apache.commons.lang3.RandomStringUtils;
import org.apache.usergrid.rest.test.resource2point0.AbstractRestIT;
import org.apache.usergrid.rest.test.resource2point0.endpoints.mgmt.ManagementResponse;
import org.apache.usergrid.rest.test.resource2point0.model.ApiResponse;
import org.apache.usergrid.rest.test.resource2point0.model.Application;
import org.apache.usergrid.rest.test.resource2point0.model.Entity;
import org.apache.usergrid.rest.test.resource2point0.model.Token;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


public class ApplicationCreateTest extends AbstractRestIT {
    private static final Logger logger = LoggerFactory.getLogger(ApplicationCreateTest.class);


    /**
     * Test that we can create and then immediately retrieve an app by name.
     * https://issues.apache.org/jira/browse/USERGRID-491
     */
    @Test
    public void testCreateAndImmediateGet() throws Exception {

        // create app

        String orgName = clientSetup.getOrganization().getName();
        String appName = clientSetup.getAppName() + "_new_app";
        Token orgAdminToken = getAdminToken(clientSetup.getUsername(), clientSetup.getUsername());

        ApiResponse appCreateResponse = clientSetup.getRestClient()
            .management().orgs().organization(orgName).app().getResource()
            .queryParam("access_token", orgAdminToken.getAccessToken())
            .type( MediaType.APPLICATION_JSON )
            .post(ApiResponse.class, new Application(appName));
        appCreateResponse.getEntities().get(0).getUuid();

        // should be able to immediately get the application's roles collection

        ApiResponse response = clientSetup.getRestClient().getResource()
            .path("/" + clientSetup.getOrganizationName() + "/" + appName + "/roles" )
            .queryParam( "access_token", orgAdminToken.getAccessToken() )
            .get(ApiResponse.class);
        assertTrue( !response.getEntities().isEmpty() );
    }


    /**
     * Test that we can create applications and the immediately retrieve them all.
     */
    @Test
    public void testCreateAndImmediateList() throws Exception {

        int appCount = 40;

        String random = RandomStringUtils.randomAlphabetic(10);
        String orgName = "org_" + random;
        String appName = "app_" + random;
        Token orgAdminToken = getAdminToken(clientSetup.getUsername(), clientSetup.getUsername());

        for ( int i=0; i<appCount; i++ ) {
           createAppWithCollection( orgName, appName + i, orgAdminToken, new ArrayList<>() );
        }

        // test that we get all applications back from the management end-point

        ManagementResponse orgAppResponse = clientSetup.getRestClient()
            .management().orgs().organization( orgName ).apps().getOrganizationApplications();

        int count = 0;
        for ( String name : orgAppResponse.getData().keySet() ) {
            count++;
        }
        assertEquals( appCount, count );
    }


    private UUID createAppWithCollection(
        String orgName, String appName, Token orgAdminToken, List<Entity> entities) {

        ApiResponse appCreateResponse = clientSetup.getRestClient()
            .management().orgs().organization( orgName ).app().getResource()
            .queryParam( "access_token", orgAdminToken.getAccessToken() )
            .type( MediaType.APPLICATION_JSON )
            .post( ApiResponse.class, new Application( appName ) );
        UUID appId = appCreateResponse.getEntities().get(0).getUuid();

        try { Thread.sleep(1000); } catch (InterruptedException ignored ) { }

        for ( int i=0; i<5; i++ ) {

            final String entityName = "entity" + i;
            Entity entity = new Entity();
            entity.setProperties(new HashMap<String, Object>() {{
                put("name", entityName );
            }});

            ApiResponse createResponse = clientSetup.getRestClient()
                .org(orgName).app( appName ).collection("things").getResource()
                .queryParam("access_token", orgAdminToken.getAccessToken())
                .type(MediaType.APPLICATION_JSON)
                .post( ApiResponse.class, entity );

            entities.add( createResponse.getEntities().get(0) );
        }
        return appId;
    }
}

