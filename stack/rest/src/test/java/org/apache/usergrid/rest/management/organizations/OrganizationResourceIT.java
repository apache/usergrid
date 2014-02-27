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
package org.apache.usergrid.rest.management.organizations;


import org.apache.usergrid.rest.management.organizations.OrganizationsResource;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.codehaus.jackson.JsonNode;
import org.junit.Rule;
import org.junit.Test;
import org.apache.usergrid.cassandra.Concurrent;
import org.apache.usergrid.management.OrganizationInfo;
import org.apache.usergrid.rest.AbstractRestIT;
import org.apache.usergrid.rest.TestContextSetup;

import junit.framework.Assert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.apache.usergrid.utils.MapUtils.hashMap;


@Concurrent()
public class OrganizationResourceIT extends AbstractRestIT {

    @Rule
    public TestContextSetup context = new TestContextSetup( this );


    @Test
    public void testOrganizationUpdate() throws Exception {
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put( "securityLevel", 5 );

        Map payload = hashMap( "email", "test-user-1@organizationresourceit.testorganizationupdate.com" )
                .map( "username", "organizationresourceit.testorganizationupdate.test-user-1" )
                .map( "name", "organizationresourceit.testorganizationupdate" ).map( "password", "password" )
                .map( "organization", "organizationresourceit.testorganizationupdate.test-org-1" )
                .map( "company", "Apigee" );


        payload.put( OrganizationsResource.ORGANIZATION_PROPERTIES, properties );

        JsonNode node = resource().path( "/management/organizations" ).accept( MediaType.APPLICATION_JSON )
                .type( MediaType.APPLICATION_JSON_TYPE ).post( JsonNode.class, payload );
        assertNotNull( node );

        OrganizationInfo orgInfo =
                setup.getMgmtSvc().getOrganizationByName( "organizationresourceit.testorganizationupdate.test-org-1" );
        assertEquals( 5L, orgInfo.getProperties().get( "securityLevel" ) );

        payload = new HashMap();
        properties.put( "securityLevel", 6 );
        payload.put( OrganizationsResource.ORGANIZATION_PROPERTIES, properties );

        node = resource().path( "/management/organizations/organizationresourceit.testorganizationupdate.test-org-1" )
                .queryParam( "access_token", superAdminToken() ).accept( MediaType.APPLICATION_JSON )
                .type( MediaType.APPLICATION_JSON_TYPE ).put( JsonNode.class, payload );
        logNode( node );

        node = resource().path( "/management/organizations/organizationresourceit.testorganizationupdate.test-org-1" )
                .queryParam( "access_token", superAdminToken() ).accept( MediaType.APPLICATION_JSON )
                .type( MediaType.APPLICATION_JSON_TYPE ).get( JsonNode.class );
        logNode( node );
        Assert.assertEquals( 6,
                node.get( "organization" ).get( OrganizationsResource.ORGANIZATION_PROPERTIES ).get( "securityLevel" )
                    .asInt() );
    }
}
