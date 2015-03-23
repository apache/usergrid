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

import org.junit.Ignore;
import org.junit.Test;

import org.apache.usergrid.persistence.index.utils.UUIDUtils;
import org.apache.usergrid.rest.test.resource2point0.AbstractRestIT;
import org.apache.usergrid.rest.test.resource2point0.RestClient;
import org.apache.usergrid.rest.test.resource2point0.model.Entity;
import org.apache.usergrid.rest.test.resource2point0.model.Organization;
import org.apache.usergrid.rest.test.resource2point0.model.QueryParameters;
import org.apache.usergrid.rest.test.resource2point0.model.Token;
import org.apache.usergrid.rest.test.resource2point0.model.User;

import com.sun.jersey.api.client.ClientResponse;
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

    String duplicateUniquePropertyExistsErrorMessage = "duplicate_unique_property_exists";
    String invalidGrantErrorMessage = "invalid_grant";

    /**
     * Tests that a Organization and Owner can be created and that they persist properties and default permissions.
     */
    @Test
    public void createOrgAndOwner() throws Exception {

        //User property to see if owner properties exist when created.
        Map<String, Object> userProperties = new HashMap<String, Object>();
        userProperties.put( "company", "Apigee" );

        //Create organization
        Organization organization = createOrgPayload( "createOrgAndOwner", userProperties );

        //Get back organization response
        Organization organizationResponse = clientSetup.getRestClient().management().orgs().post( organization );

        assertNotNull( organizationResponse );

        //Creates token
        Token token =
                clientSetup.getRestClient().management().token().post(Token.class, new Token( "password",
                        organization.getUsername(), organization.getPassword() ) );

        assertNotNull( token );

        //Assert that the get returns the correct org and owner.
        Organization returnedOrg = clientSetup.getRestClient().management().orgs().organization( organization.getOrganization()).get();

        assertTrue( returnedOrg != null && returnedOrg.getName().equals( organization.getOrganization() ) );

        User returnedUser = returnedOrg.getOwner();

        //Assert that the property was retained in the owner of the organization.
        assertNotNull( returnedUser );
        assertEquals( "Apigee", returnedUser.getProperties().get( "company" ) );
    }


    /**
     * Creates a organization with an owner, then attempts to create an organization with the same name ( making sure it
     * fails) When it fails it verifies that the original is still intact.
     * @throws Exception
     */
    @Test
    public void testCreateDuplicateOrgName() throws Exception {

        //Create organization
        Organization organization = createOrgPayload( "testCreateDuplicateOrgName", null );

        Organization orgCreatedResponse = clientSetup.getRestClient().management().orgs().post( organization );
        this.refreshIndex();

        assertNotNull( orgCreatedResponse );

        //Ensure that the token from the newly created organization works.
        Token tokenPayload = new Token( "password", organization.getUsername(), organization.getPassword() );
        Token tokenReturned = clientSetup.getRestClient().management().token().post(Token.class, tokenPayload );

        assertNotNull( tokenReturned );

        //Try to create a organization with the same name as an organization that already exists, ensure that it fails
        Organization orgTestDuplicatePayload =
                new Organization( organization.getOrganization(), organization.getUsername() + "test",
                        organization.getEmail() + "test", organization.getName() + "test",
                        organization.getPassword(), null );
        try {
            clientSetup.getRestClient().management().orgs().post( orgTestDuplicatePayload );
            fail("Should not have been able to create duplicate organization");
        }
        catch ( UniformInterfaceException ex ) {
            errorParse( 400,duplicateUniquePropertyExistsErrorMessage, ex );
        }

        // Post to get token of what should be a non existent user due to the failure of creation above

        tokenPayload = new Token( "password", organization.getName() + "test", organization.getPassword() );
        Token tokenError = null;
        try {
            tokenError = clientSetup.getRestClient().management().token().post(Token.class, tokenPayload );
            fail( "Should not have created user" );
        }
        catch ( UniformInterfaceException ex ) {
            errorParse( 400,invalidGrantErrorMessage, ex );

        }

        assertNull( tokenError );
    }


    /**
     * Tests creation of an organization with a duplicate email. Then checks to make sure correct
     * error message is thrown. Also makes sure that the owner of the duplicate org isn't created
     * while the original is still intact.
     * @throws Exception
     */
    @Test
    public void testCreateDuplicateOrgEmail() throws Exception {

        Organization organization = createOrgPayload( "testCreateDuplicateOrgEmail", null );

        //create the org/owner
        Organization orgCreatedResponse = clientSetup.getRestClient().management().orgs().post( organization );

        this.refreshIndex();

        assertNotNull( orgCreatedResponse );

        //get token from organization that was created to verify it exists.
        Token tokenPayload = new Token( "password", organization.getUsername(), organization.getPassword() );
        Token tokenReturned = clientSetup.getRestClient().management().token().post(Token.class, tokenPayload );

        assertNotNull( tokenReturned );

        //recreate a new payload using a duplicate email
        Organization orgDuplicatePayload = new Organization( organization.getOrganization()+"test",
                organization.getUsername()+"test", organization.getEmail(),
                organization.getName()+"test", organization.getPassword()+"test", null );


        //verify that we cannot create an organization that shares a email with another preexisting organization.
        try {
            clientSetup.getRestClient().management().orgs().post( orgDuplicatePayload );
            fail( "Should not have created organization" );
        }
        catch ( UniformInterfaceException ex ) {
            errorParse( 400,duplicateUniquePropertyExistsErrorMessage,ex);
        }

        //try to get the token from the organization that failed to be created to verify it was not made.
        tokenPayload = new Token( "password", organization.getUsername()+"test", organization.getPassword()+"test" );
        Token tokenError = null;
        try {
            tokenError = clientSetup.getRestClient().management().token().post(Token.class, tokenPayload );
            fail( "Should not have created organization" );
        }
        catch ( UniformInterfaceException ex ) {
            errorParse( 400,invalidGrantErrorMessage,ex );
        }

        assertNull( tokenError );

    }


    /**
     * Creates a organization by setting the information as part of the queryParameters
     * @throws IOException
     */
    @Test
    public void testOrgPOSTParams() throws IOException {

        //Create organization defaults
        Organization organization =  createOrgPayload( "testOrgPOSTParams",null );

        //Append them to the end as query parameters
        QueryParameters queryParameters = new QueryParameters();
        queryParameters.setKeyValue( "organization",organization.getOrganization());
        queryParameters.setKeyValue( "username",organization.getUsername() );
        queryParameters.setKeyValue( "grant_type","password" );
        queryParameters.setKeyValue( "email",organization.getEmail() );
        queryParameters.setKeyValue( "name",organization.getName() );
        queryParameters.setKeyValue( "password",organization.getPassword() );

        //Post the organization and verify it worked
        Organization organizationReturned = clientSetup.getRestClient().management().orgs().post( queryParameters );

        assertNotNull( organizationReturned );
        assertEquals( organization.getOrganization(), organizationReturned.getName());

        //get token from organization that was created to verify it exists. also sets the current context.
        Token tokenPayload = new Token( "password", organization.getName(), organization.getPassword() );
        Token tokenReturned = clientSetup.getRestClient().management().token().post(Token.class, tokenPayload );

        assertNotNull( tokenReturned );

        //Assert that the get returns the correct org and owner.
        Organization returnedOrg = clientSetup.getRestClient().management().orgs().organization( organization.getOrganization() ).get();

        assertTrue( returnedOrg != null && returnedOrg.getName().equals(organization.getOrganization()) );

    }


    /**
     * Creates a organization by posting a form with the organization data.
     * @throws IOException
     */
    @Test
    public void testOrgPOSTForm() throws IOException {

        Organization organization =  createOrgPayload( "testOrgPOSTForm",null );

        //create the form to hold the organization
        Form form = new Form();
        form.add( "organization", organization.getOrganization() );
        form.add( "username", organization.getUsername() );
        form.add( "grant_type", "password" );
        form.add( "email", organization.getEmail() );
        form.add( "name", organization.getName() );
        form.add( "password", organization.getPassword() );

        //Post the organization and verify it worked.
        Organization organizationReturned = clientSetup.getRestClient().management().orgs().post( form );

        assertNotNull( organizationReturned );
        assertEquals( organization.getOrganization(),organizationReturned.getName() );

        //get token from organization that was created to verify it exists. also sets the current context.
        Token tokenPayload = new Token( "password", organization.getName(), organization.getPassword() );
        Token tokenReturned = clientSetup.getRestClient().management().token().post(Token.class, tokenPayload );

        assertNotNull( tokenReturned );

        //Assert that the get returns the correct org and owner.
        Organization returnedOrg = clientSetup.getRestClient().management().orgs().organization( organization.getOrganization() ).get();

        assertTrue( returnedOrg != null && returnedOrg.getName().equals(organization.getOrganization()) );

    }


    /**
     * Returns error from unimplemented delete method by trying to call the delete organization endpoint
     * @throws IOException
     */
    @Ignore("It should return a 501, so when this is fixed the test can be run")
    @Test
    public void noOrgDelete() throws IOException {

        try {
            //Delete default organization
            clientSetup.getRestClient().management().orgs().organization( clientSetup.getOrganizationName() ).delete();
            fail( "Delete is not implemented yet" );
        }catch(UniformInterfaceException uie){
            assertEquals( ClientResponse.Status.NOT_IMPLEMENTED ,uie.getResponse().getStatus());
        }
    }


    /**
     * Creates a regular organization user and then does a get to check the correct username is returned.
     * @throws Exception
     */
    @Test
    public void testCreateOrgUserAndReturnCorrectUsername() throws Exception {
        RestClient restClient = clientSetup.getRestClient();

        //Create adminUser values
        Entity adminUserPayload = new Entity();
        String username = "testCreateOrgUserAndReturnCorrectUsername"+UUIDUtils.newTimeUUID();
        adminUserPayload.put( "username", username );
        adminUserPayload.put( "name", username );
        adminUserPayload.put( "email", username+"@usergrid.com" );
        adminUserPayload.put( "password", username );

        //create adminUser
        Entity adminUserResponse = restClient.management().orgs().organization( clientSetup.getOrganizationName() ).users().post(Entity.class, adminUserPayload );

        //verify that the response contains the correct data
        assertNotNull( adminUserResponse );
        assertEquals( username, adminUserResponse.get( "username" ) );

        //fetch the stored response
        adminUserResponse = restClient.management().users().entity( username ).get(this.getAdminToken(username,username));

        //verify that values match stored response
        assertNotNull( adminUserResponse );
        assertEquals( username , adminUserResponse.get( "username" ) );
        assertEquals( username, adminUserResponse.get( "name" ) );
        assertEquals( username+"@usergrid.com", adminUserResponse.get( "email" ));

    }


    /**
     * Inserts a value into the default organization then update that value and see if the value persists.
     * @throws Exception
     */
    @Test
    public void testOrganizationUpdate() throws Exception {

        RestClient restClient = clientSetup.getRestClient();

        //Setup what will be interested into the organization
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put( "puppies", 5 );

        Organization orgPayload = clientSetup.getOrganization();
        orgPayload.put( "properties", properties );

        //update the organization.
        restClient.management().orgs().organization( clientSetup.getOrganizationName() ).put(orgPayload);

        this.refreshIndex();

        //retrieve the organization
        Organization orgResponse = restClient.management().orgs().organization( clientSetup.getOrganizationName() ).get();

        assertEquals( 5, orgResponse.getProperties().get( "puppies" ));

        //update the value added to the organization
        properties.put( "puppies", 6 );
        orgPayload.put( "properties", properties );

        //update the organization.
        restClient.management().orgs().organization( clientSetup.getOrganizationName() ).put(orgPayload);

        this.refreshIndex();

        orgResponse = restClient.management().orgs().organization( clientSetup.getOrganizationName() ).get();

        assertEquals( 6, orgResponse.getProperties().get( "puppies" ));

    }

    /**
     * Create an organization payload with almost the same value for everyfield.
     * @param baseName
     * @param properties
     * @return
     */
    public Organization createOrgPayload(String baseName,Map properties){
        String orgName = baseName + UUIDUtils.newTimeUUID();
        return new Organization( orgName+ UUIDUtils.newTimeUUID(),orgName,orgName+"@usergrid",orgName,orgName, properties);
    }
}
