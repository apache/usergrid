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
import org.apache.usergrid.rest.test.resource.AbstractRestIT;
import org.apache.usergrid.rest.test.resource.endpoints.mgmt.ManagementResponse;
import org.apache.usergrid.rest.test.resource.model.ApiResponse;
import org.apache.usergrid.rest.test.resource.model.Application;
import org.apache.usergrid.rest.test.resource.model.Entity;
import org.apache.usergrid.rest.test.resource.model.Token;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;



public class ApplicationCreateIT extends AbstractRestIT {
    private static final Logger logger = LoggerFactory.getLogger(ApplicationCreateIT.class);


    /**
     * Test that we can create and then immediately retrieve an app by name.
     * https://issues.apache.org/jira/browse/USERGRID-491
     */
    @Test
    public void testCreateAndImmediateGet() throws Exception {

        String orgName = clientSetup.getOrganizationName();
        String appName = clientSetup.getAppName() + "_new_app";
        Token orgAdminToken = getAdminToken(clientSetup.getUsername(), clientSetup.getPassword());

        Map applicationMap = new HashMap<String, Object>(  );
        applicationMap.put( "name", appName );

        this.management().token().setToken(orgAdminToken);
        this.management().orgs().org( orgName ).apps().post(applicationMap );

        Entity response = this.management().orgs().org( orgName ).addToPath( "apps" ).addToPath( appName ).get();

        assertNotNull( response );
    }


    /**
     * Test that we can create applications and the immediately retrieve them all.
     */
    @Test
    public void testCreateAndImmediateList() throws Exception {

        int appCount = 10;

        String random = RandomStringUtils.randomAlphabetic(10);
        String orgName = clientSetup.getOrganizationName();
        String appName = "testCreateAndImmediateList_app_" + random;
        Token orgAdminToken = getAdminToken( clientSetup.getUsername(), clientSetup.getPassword() );

        for ( int i=0; i<appCount; i++ ) {
           createAppWithCollection( orgName, appName + i, orgAdminToken, new ArrayList<>() );
        }

        // test that we get all applications back from the management end-point

        ManagementResponse orgAppResponse = clientSetup.getRestClient()
            .management().orgs().org( orgName ).apps().getOrganizationApplications();

        int count = 0;
        for ( String name : orgAppResponse.getData().keySet() ) {
            if(name.contains("testcreateandimmediatelist_app")) {
                count++;
            }
        }
        assertEquals( appCount, count );
    }


    private UUID createAppWithCollection(
        String orgName, String appName, Token orgAdminToken, List<Entity> entities) {

        ApiResponse appCreateResponse = clientSetup.getRestClient()
            .management().orgs().org( orgName ).app().post( new Application( appName ) );
        UUID appId = appCreateResponse.getEntities().get(0).getUuid();

        refreshIndex();
        for ( int i=0; i<5; i++ ) {

            final String entityName = "entity" + i;
            Entity entity = new Entity();
            entity.setProperties( new HashMap<String, Object>() {{
                put( "name", entityName );
            }} );

            ApiResponse createResponse = clientSetup.getRestClient()
                .org( orgName ).app( appName ).collection( "things" ).getTarget()
                .queryParam( "access_token", orgAdminToken.getAccessToken() )
                .request()
                .post( javax.ws.rs.client.Entity.entity(entity, MediaType.APPLICATION_JSON_TYPE ), ApiResponse.class);

            entities.add( createResponse.getEntities().get(0) );
        }
        return appId;
    }
}

