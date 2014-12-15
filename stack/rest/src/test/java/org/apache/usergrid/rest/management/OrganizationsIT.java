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
//jnote to self, remaining test failures are due to duplicate org names in the cassandra external. Easily fixed.
package org.apache.usergrid.rest.management;


import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.ws.rs.core.MediaType;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.entities.User;
import org.apache.usergrid.persistence.index.utils.UUIDUtils;
import org.apache.usergrid.rest.TestContextSetup;
import org.apache.usergrid.rest.management.organizations.OrganizationsResource;
import org.apache.usergrid.rest.test.resource2point0.AbstractRestIT;
import org.apache.usergrid.rest.test.resource2point0.model.ApiResponse;
import org.apache.usergrid.rest.test.resource2point0.model.Organization;
import org.apache.usergrid.rest.test.resource2point0.model.Token;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.representation.Form;

import junit.framework.Assert;

import static junit.framework.Assert.fail;
import static org.apache.usergrid.utils.MapUtils.hashMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


/**
 * Created by ApigeeCorporation on 9/17/14.
 */
public class OrganizationsIT extends AbstractRestIT {
    private static final Logger LOG = LoggerFactory.getLogger( OrganizationsIT.class );

    @Rule
    public TestContextSetup context = new TestContextSetup( this );


    /**
     * Tests that a Organization and Owner can be created and that they persist properties and default permissions.
     * @throws Exception
     */

    @Test
    public void createOrgAndOwner() throws Exception {

        String username = "createOrgAndOwner" + UUIDUtils.newTimeUUID();
        String name = username;
        String password = "password";
        String orgName = username;
        String email = username + "@usergrid.com";

        Map<String, Object> organizationProperties = new HashMap<String, Object>();
        organizationProperties.put( "securityLevel", 5 );
        organizationProperties.put( "company", "Apigee" );


        Organization organization = new Organization(orgName,username,email,name,password,organizationProperties);
        //TODO:seperate entity properties from organization properties.
        //organization.addProperty( "" ).addProperty(  )

        Organization orgOwner = clientSetup.getRestClient().management().orgs().post( organization );

        assertNotNull( orgOwner );

       // orgOwner = clientSetup.getRestClient().management().orgs().get();

        Token token = new Token( "password",username,password );
        Token tokenBack = clientSetup.getRestClient().management().token().post(token);

        assertNotNull( tokenBack );



//        Map payload =
//                hashMap( "email", email ).map( "username", username ).map( "name", name ).map( "password", password )
//                                         .map( "organization", orgName ).map( "company", "Apigee" );

//        Map payload2 = hashMap( "grant_type", "password" ).map( "username", username ).map( "password", password );
//
//        //TODO: make it easier to distinguish between owner/entity/response uuid.
//        UUID userUuid = UUID.fromString( node.get( "data" ).get( "owner" ).get( "uuid" ).asText() );
//
//        node = mapper.readTree( resource().path( "/management/token" ).accept( MediaType.APPLICATION_JSON )
//                                          .type( MediaType.APPLICATION_JSON_TYPE ).post( String.class, payload2 ) );
//
//        //assertNotNull( node );
//
//        node = mapper.readTree( resource().path( "/management/organizations/" + orgName + "/apps/sandbox" )
//                                          .queryParam( "access_token", node.get( "access_token" ).textValue() )
//                                          .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE )
//                                          .get( String.class ) );
//
//        assertNotNull( node );
//
//        Set<String> rolePerms = setup.getEmf().getEntityManager(
//                UUID.fromString( node.get( "entities" ).get( 0 ).get( "uuid" ).asText() ) )
//                                     .getRolePermissions( "guest" );
//        assertNotNull( rolePerms );
//        assertTrue( rolePerms.contains( "get,post,put,delete:/**" ) );
//        logNode( node );
//
//        EntityManager em = setup.getEmf().getEntityManager( setup.getEmf().getManagementAppId() );
//        User user = em.get( userUuid, User.class );
//        assertEquals( name, user.getName() );
//        assertEquals( "Apigee", user.getProperty( "company" ) );
    }

//
//    @Test
//    public void testCreateDuplicateOrgName() throws Exception {
//
//        // create organization with name
//        String timeuuid = UUIDUtils.newTimeUUID().toString();
//        Map<String, String> payload =
//                hashMap( "email", "create-duplicate-org" + timeuuid + "@mockserver.com" ).map( "password", "password" )
//                                                                                         .map( "organization",
//                                                                                                 "create-duplicate-orgname-org"
//                                                                                                         + timeuuid );
//        JsonNode node = mapper.readTree(
//                resource().path( "/management/organizations" ).accept( MediaType.APPLICATION_JSON )
//                          .type( MediaType.APPLICATION_JSON_TYPE ).post( String.class, payload ) );
//
//        logNode( node );
//        assertNotNull( node );
//
//        refreshIndex( "create-duplicate-orgname-org" + timeuuid, "dummy" );
//
//        // create another org with that same name, but a different user
//        payload = hashMap( "email", "create-duplicate-org2@mockserver.com" ).map( "username", "create-dupe-orgname2" )
//                                                                            .map( "password", "password" )
//                                                                            .map( "organization",
//                                                                                    "create-duplicate-orgname-org"
//                                                                                            + timeuuid );
//        try {
//            node = mapper.readTree( resource().path( "/management/organizations" ).accept( MediaType.APPLICATION_JSON )
//                                              .type( MediaType.APPLICATION_JSON_TYPE ).post( String.class, payload ) );
//        }
//        catch ( Exception ex ) {
//        }
//
//        refreshIndex( "create-duplicate-orgname-org" + timeuuid, "dummy" );
//
//        // now attempt to login as the user for the second organization
//        payload = hashMap( "grant_type", "password" ).map( "username", "create-dupe-orgname2" )
//                                                     .map( "password", "password" );
//        try {
//            node = mapper.readTree( resource().path( "/management/token" ).accept( MediaType.APPLICATION_JSON )
//                                              .type( MediaType.APPLICATION_JSON_TYPE ).post( String.class, payload ) );
//            fail( "Should not have created user" );
//        }
//        catch ( Exception ex ) {
//        }
//        logNode( node );
//
//        refreshIndex( "create-duplicate-orgname-org" + timeuuid, "dummy" );
//
//        payload = hashMap( "username", "create-duplicate-org@mockserver.com" ).map( "grant_type", "password" )
//                                                                              .map( "password", "password" );
//        node = mapper.readTree( resource().path( "/management/token" ).accept( MediaType.APPLICATION_JSON )
//                                          .type( MediaType.APPLICATION_JSON_TYPE ).post( String.class, payload ) );
//        logNode( node );
//    }
//
//
//    @Test
//    public void testCreateDuplicateOrgEmail() throws Exception {
//
//        String timeUuid = UUIDUtils.newTimeUUID().toString();
//        Map<String, String> payload =
//                hashMap( "email", "duplicate-email" + timeUuid + "@mockserver.com" ).map( "password", "password" )
//                                                                                    .map( "organization",
//                                                                                            "very-nice-org"
//                                                                                                    + timeUuid );
//
//        JsonNode node = mapper.readTree(
//                resource().path( "/management/organizations" ).accept( MediaType.APPLICATION_JSON )
//                          .type( MediaType.APPLICATION_JSON_TYPE ).post( String.class, payload ) );
//
//        logNode( node );
//        assertNotNull( node );
//
//        payload = hashMap( "email", "duplicate-email" + timeUuid + "@mockserver.com" ).map( "username", "anotheruser" )
//                                                                                      .map( "password", "password" )
//                                                                                      .map( "organization",
//                                                                                              "not-so-nice-org"
//                                                                                                      + timeUuid );
//
//        boolean failed = false;
//        try {
//            node = mapper.readTree( resource().path( "/management/organizations" ).accept( MediaType.APPLICATION_JSON )
//                                              .type( MediaType.APPLICATION_JSON_TYPE ).post( String.class, payload ) );
//        }
//        catch ( UniformInterfaceException ex ) {
//            Assert.assertEquals( 400, ex.getResponse().getStatus() );
//            JsonNode errorJson = ex.getResponse().getEntity( JsonNode.class );
//            Assert.assertEquals( "duplicate_unique_property_exists", errorJson.get( "error" ).asText() );
//            failed = true;
//        }
//        Assert.assertTrue( failed );
//
//        refreshIndex( "test-organization", "test-app" );
//
//        payload = hashMap( "grant_type", "password" ).map( "username", "create-dupe-orgname2" )
//                                                     .map( "password", "password" );
//        try {
//            node = mapper.readTree( resource().path( "/management/token" ).accept( MediaType.APPLICATION_JSON )
//                                              .type( MediaType.APPLICATION_JSON_TYPE ).post( String.class, payload ) );
//            fail( "Should not have created user" );
//        }
//        catch ( Exception ex ) {
//        }
//
//        logNode( node );
//
//        refreshIndex( "test-organization", "test-app" );
//
//        payload =
//                hashMap( "username", "duplicate-email" + timeUuid + "@mockserver.com" ).map( "grant_type", "password" )
//                                                                                       .map( "password", "password" );
//        node = mapper.readTree( resource().path( "/management/token" ).accept( MediaType.APPLICATION_JSON )
//                                          .type( MediaType.APPLICATION_JSON_TYPE ).post( String.class, payload ) );
//        logNode( node );
//    }
//
//
//    @Test
//    public void testOrgPOSTParams() throws IOException {
//        UUID timeUuid = UUIDUtils.newTimeUUID();
//        JsonNode node = mapper.readTree( resource().path( "/management/organizations" )
//                                                   .queryParam( "organization", "testOrgPOSTParams" + timeUuid )
//                                                   .queryParam( "username", "testOrgPOSTParams" + timeUuid )
//                                                   .queryParam( "grant_type", "password" ).queryParam( "email",
//                        "testOrgPOSTParams" + timeUuid + "@apigee.com" + timeUuid )
//                                                   .queryParam( "name", "testOrgPOSTParams" )
//                                                   .queryParam( "password", "password" )
//
//                                                   .accept( MediaType.APPLICATION_JSON )
//                                                   .type( MediaType.APPLICATION_FORM_URLENCODED )
//                                                   .post( String.class ) );
//
//        assertEquals( "ok", node.get( "status" ).asText() );
//    }
//
//
//    @Test
//    public void testOrgPOSTForm() throws IOException {
//
//        UUID timeUuid = UUIDUtils.newTimeUUID();
//        Form form = new Form();
//        form.add( "organization", "testOrgPOSTForm" + timeUuid );
//        form.add( "username", "testOrgPOSTForm" + timeUuid );
//        form.add( "grant_type", "password" );
//        form.add( "email", "testOrgPOSTForm" + timeUuid + "@apigee.com" );
//        form.add( "name", "testOrgPOSTForm" );
//        form.add( "password", "password" );
//
//        JsonNode node = mapper.readTree(
//                resource().path( "/management/organizations" ).accept( MediaType.APPLICATION_JSON )
//                          .type( MediaType.APPLICATION_FORM_URLENCODED ).post( String.class, form ) );
//
//        assertEquals( "ok", node.get( "status" ).asText() );
//    }
//
//
//    @Test
//    public void noOrgDelete() throws IOException {
//
//
//        String mgmtToken = context.getActiveUser().getToken();
//
//        ClientResponse.Status status = null;
//        JsonNode node = null;
//
//        try {
//            node = mapper.readTree( resource().path( context.getOrgName() ).queryParam( "access_token", mgmtToken )
//                                              .accept( MediaType.APPLICATION_JSON )
//                                              .type( MediaType.APPLICATION_JSON_TYPE ).delete( String.class ) );
//        }
//        catch ( UniformInterfaceException uie ) {
//            status = uie.getResponse().getClientResponseStatus();
//        }
//
//        assertEquals( ClientResponse.Status.NOT_IMPLEMENTED, status );
//    }
//
//
//    @Test
//    public void testCreateOrgUserAndReturnCorrectUsername() throws Exception {
//
//
//        String mgmtToken = context.getActiveUser().getToken();
//
//        Map<String, String> payload = hashMap( "username", "test-user-2" ).map( "name", "Test User 2" )
//                                                                          .map( "email", "test-user-2@mockserver.com" )
//                                                                          .map( "password", "password" );
//
//        JsonNode node = mapper.readTree(
//                resource().path( "/management/organizations/" + context.getOrgName() + "/users" )
//                          .queryParam( "access_token", mgmtToken ).accept( MediaType.APPLICATION_JSON )
//                          .type( MediaType.APPLICATION_JSON_TYPE ).post( String.class, payload ) );
//
//        logNode( node );
//        assertNotNull( node );
//
//        String username = node.get( "data" ).get( "user" ).get( "username" ).asText();
//        String name = node.get( "data" ).get( "user" ).get( "name" ).asText();
//        String email = node.get( "data" ).get( "user" ).get( "email" ).asText();
//
//        assertNotNull( username );
//        assertNotNull( name );
//        assertNotNull( email );
//
//        assertEquals( "test-user-2", username );
//        assertEquals( "Test User 2", name );
//        assertEquals( "test-user-2@mockserver.com", email );
//    }
//
//
//    @Test
//    public void testOrganizationUpdate() throws Exception {
//        String accessToken = context.getActiveUser().getToken();
//        Map<String, Object> properties = new HashMap<String, Object>();
//        properties.put( "securityLevel", 5 );
//        Map payload = new HashMap();
//        payload.put( OrganizationsResource.ORGANIZATION_PROPERTIES, properties );
//
//
//        //update the organizations
//        mapper.readTree( resource().path( "/management/organizations/" + context.getOrgName() )
//                                   .queryParam( "access_token", accessToken ).accept( MediaType.APPLICATION_JSON )
//                                   .type( MediaType.APPLICATION_JSON_TYPE ).put( String.class, payload ) );
//
//
//        refreshIndex( context.getOrgName(), context.getAppName() );
//
//        //get the organization
//        JsonNode node = mapper.readTree( resource().path( "/management/organizations/" + context.getOrgName() )
//                                                   .queryParam( "access_token", accessToken )
//                                                   .accept( MediaType.APPLICATION_JSON )
//                                                   .type( MediaType.APPLICATION_JSON_TYPE ).get( String.class ) );
//
//        assertEquals( 5L, node.get( "organization" ).get( "properties" ).get( "securityLevel" )
//                              .asLong() );//orgInfo.getProperties().get( "securityLevel" ) );
//
//        payload = new HashMap();
//        properties.put( "securityLevel", 6 );
//        payload.put( OrganizationsResource.ORGANIZATION_PROPERTIES, properties );
//
//        node = mapper.readTree( resource().path( "/management/organizations/" + context.getOrgName() )
//                                          .queryParam( "access_token", accessToken )
//                                          .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE )
//                                          .put( String.class, payload ) );
//        logNode( node );
//
//        refreshIndex( context.getOrgName(), context.getAppName() );
//
//        node = mapper.readTree( resource().path( "/management/organizations/" + context.getOrgName() )
//                                          .queryParam( "access_token", accessToken )
//                                          .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE )
//                                          .get( String.class ) );
//        logNode( node );
//        Assert.assertEquals( 6,
//                node.get( "organization" ).get( OrganizationsResource.ORGANIZATION_PROPERTIES ).get( "securityLevel" )
//                    .asInt() );
//    }
}
