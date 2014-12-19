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
package org.apache.usergrid.rest.management;


import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.index.utils.UUIDUtils;
import org.apache.usergrid.rest.test.resource2point0.AbstractRestIT;

import org.apache.usergrid.rest.test.resource2point0.model.Organization;
import org.apache.usergrid.rest.test.resource2point0.model.QueryParameters;
import org.apache.usergrid.rest.test.resource2point0.model.Token;
import org.apache.usergrid.rest.test.resource2point0.model.User;

import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.representation.Form;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


/**
 * Handles all management organization endpoint tests. Any tests that work with organizations specifically can be found here
 */
public class OrganizationsIT extends AbstractRestIT {
    private static final Logger LOG = LoggerFactory.getLogger( OrganizationsIT.class );

    String duplicateUniquePropertyExistsErrorMessage = "duplicate_unique_property_exists";
    String invalidGrantErrorMessage = "invalid_grant";

    /**
     * Tests that a Organization and Owner can be created and that they persist properties and default permissions.
     */
    @Test
    public void createOrgAndOwner() throws Exception {

        String username = "createOrgAndOwner" + UUIDUtils.newTimeUUID();
        String name = username;
        String password = "password";
        String orgName = username;
        String email = username + "@usergrid.com";

        //TODO:seperate entity properties from organization properties.
        Map<String, Object> userProperties = new HashMap<String, Object>();
        userProperties.put( "company", "Apigee" );

        //Create organization
        Organization organization = new Organization( orgName, username, email, name, password, userProperties );

        //Get back organization response
        Organization organizationResponse = clientSetup.getRestClient().management().orgs().post( organization );

        assertNotNull( organizationResponse );

        //Creates token
        Token token =
                clientSetup.getRestClient().management().token().post( new Token( "password", username, password ) );

        assertNotNull( token );

        assertNotNull( clientSetup.getRestClient().getContext().getToken() );

        Organization returnedOrg = clientSetup.getRestClient().management().orgs().organization( orgName ).get();

        //TODO: clean this up
        assertTrue( returnedOrg != null && returnedOrg.getName().equals( orgName ) );

        User returnedUser = returnedOrg.getOwner();

        assertEquals( "Apigee", returnedUser.getProperties().get( "company" ) );
    }


    /**
     * Creates a organization with an owner, then attempts to create an organization with the same name ( making sure it
     * fails) When it fails it verifies that the original is still intact.
     * @throws Exception
     */
    @Test
    public void testCreateDuplicateOrgName() throws Exception {

        String username = "testCreateDuplicateOrgName" + UUIDUtils.newTimeUUID();
        String name = username;
        String password = "password";
        String orgName = username;
        String email = username + "@usergrid.com";

        //Create organization
        Organization orgPayload = new Organization( orgName, username, email, name, password, null );

        Organization orgCreatedResponse = clientSetup.getRestClient().management().orgs().post( orgPayload );


        assertNotNull( orgCreatedResponse );

        //TODO: Check to see if we still need to refresh the index.

        Organization orgTestDuplicatePayload =
                new Organization( orgName, username + "test", email + "test", name + "test", password, null );
        Organization orgTestDuplicateResponse = null;
        try {
            orgTestDuplicateResponse = clientSetup.getRestClient().management().orgs().post( orgTestDuplicatePayload );
            fail("Should not have been able to create duplicate organization");
        }
        catch ( UniformInterfaceException ex ) {
            errorParse( 400,duplicateUniquePropertyExistsErrorMessage, ex );
        }

        //refreshIndex( "create-duplicate-orgname-org" + timeuuid, "dummy" );

        // Post to get token of what should be a non existent user.

        Token tokenPayload = new Token( "password", username + "test", password );
        Token tokenError = null;
        try {
            tokenError = clientSetup.getRestClient().management().token().post( tokenPayload );
            fail( "Should not have created user" );
        }
        catch ( UniformInterfaceException ex ) {
            errorParse( 400,invalidGrantErrorMessage, ex );

        }

        assertNull( tokenError );

        tokenPayload = new Token( "password", username, password );
        Token tokenReturned = clientSetup.getRestClient().management().token().post( tokenPayload );

        assertNotNull( tokenReturned );
    }


    /**
     * Tests creation of an organization with a duplicate email. Then checks to make sure correct
     * error message is thrown. Also makes sure that the owner of the duplicate org isn't created
     * while the original is still intact.
     * @throws Exception
     */
    @Test
    public void testCreateDuplicateOrgEmail() throws Exception {

        String username = "testCreateDuplicateOrgEmail" + UUIDUtils.newTimeUUID();
        String name = username;
        String password = "password";
        String orgName = username;
        String email = username + "@usergrid.com";

        Organization orgPayload = new Organization( orgName, username, email, name, password, null );

        Organization orgCreatedResponse = clientSetup.getRestClient().management().orgs().post( orgPayload );

        assertNotNull( orgCreatedResponse );

        orgPayload = new Organization( orgName+"test", username+"test", email, name+"test", password+"test", null );

        try {
            clientSetup.getRestClient().management().orgs().post( orgPayload );
            fail( "Should not have created organization" );
        }
        catch ( UniformInterfaceException ex ) {
            errorParse( 400,duplicateUniquePropertyExistsErrorMessage,ex);
        }

        //refreshIndex( "test-organization", "test-app" );

        Token tokenPayload = new Token( "password", username + "test", password );
        Token tokenError = null;
        try {
            tokenError = clientSetup.getRestClient().management().token().post( tokenPayload );
            fail( "Should not have created organization" );
        }
        catch ( UniformInterfaceException ex ) {
            //TODO: Should throw a 404 not a 400.
            errorParse( 400,invalidGrantErrorMessage,ex );
        }

        assertNull( tokenError );

        tokenPayload = new Token( "password", username, password );
        Token tokenReturned = clientSetup.getRestClient().management().token().post( tokenPayload );

        assertNotNull( tokenReturned );
    }


    //TODO:
    @Test
    public void testOrgPOSTParams() throws IOException {

        String username = "testCreateDuplicateOrgEmail" + UUIDUtils.newTimeUUID();
        String name = username;
        String password = "password";
        String orgName = username;
        String email = username + "@usergrid.com";

        UUID timeUuid = UUIDUtils.newTimeUUID();

        QueryParameters queryParameters = new QueryParameters();
        queryParameters.setKeyValue( "organization",orgName );
        queryParameters.setKeyValue( "username",username );
        queryParameters.setKeyValue( "grant_type",password );
        queryParameters.setKeyValue( "email",email  );
        queryParameters.setKeyValue( "name",name );
        queryParameters.setKeyValue( "password",password );

        Organization organization = clientSetup.getRestClient().management().orgs().post( queryParameters );

        assertNotNull( organization );
        assertEquals( orgName,organization.getName() );
    }

    @Test
    public void testOrgPOSTForm() throws IOException {

        UUID timeUuid = UUIDUtils.newTimeUUID();
        Form form = new Form();
        form.add( "organization", "testOrgPOSTForm" + timeUuid );
        form.add( "username", "testOrgPOSTForm" + timeUuid );
        form.add( "grant_type", "password" );
        form.add( "email", "testOrgPOSTForm" + timeUuid + "@apigee.com" );
        form.add( "name", "testOrgPOSTForm" );
        form.add( "password", "password" );

        Organization organization = clientSetup.getRestClient().management().orgs().post( form );

        assertNotNull( organization );
        assertEquals( "testOrgPOSTForm" + timeUuid ,organization.getName() );
    }


    /**
     * Returns error from unimplemented delete method
     * @throws IOException
     */
    @Test
    public void noOrgDelete() throws IOException {

        try {
            clientSetup.getRestClient().management().orgs().organization( clientSetup.getOrganizationName() ).delete();
            fail( "Delete is not implemented yet" );
        }catch(UniformInterfaceException uie){
            assertEquals(500,uie.getResponse().getStatus());
           // assertEquals( ClientResponse.Status.NOT_IMPLEMENTED ,uie.getResponse().getStatus());
        }

    }


    @Test
    public void testCreateOrgUserAndReturnCorrectUsername() throws Exception {
/*
        String username = "testCreateOrgUserAndReturnCorrectUsername"+UUIDUtils.newTimeUUID();

        RestClient restClient = clientSetup.getRestClient();

        User adminUserPayload = new User(username,username,username+"@usergrid.com",username  );

        User adminUserResponse = restClient.management().orgs().organization( clientSetup.getOrganizationName() )
                                           .users().post( adminUserPayload );

        assertNotNull( adminUserResponse );
*/
//        Map<String, String> payload = hashMap( "username", "test-user-2" ).map( "name", "Test User 2" )
//                                                                          .map( "email", "test-user-2@mockserver.com" )
//                                                                          .map( "password", "password" );
//
//        JsonNode node = mapper.readTree(
//                resource().path( "/management/organizations/" + context.getOrgName() + "/users" )
//                          .queryParam( "access_token", mgmtToken ).accept( MediaType.APPLICATION_JSON )
//                          .type( MediaType.APPLICATION_JSON_TYPE ).post( String.class, payload ) );
//
//        clientSetup.getRestClient().management().orgs()
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
    }
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
