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


import com.fasterxml.jackson.databind.JsonNode;
import net.jcip.annotations.NotThreadSafe;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.usergrid.persistence.index.utils.UUIDUtils;
import org.apache.usergrid.rest.management.organizations.OrganizationsResource;
import org.apache.usergrid.rest.test.resource.AbstractRestIT;
import org.apache.usergrid.rest.test.resource.model.*;
import org.apache.usergrid.rest.test.resource.model.Collection;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.*;

import static org.apache.usergrid.rest.management.ManagementResource.USERGRID_CENTRAL_URL;
import static org.apache.usergrid.utils.MapUtils.hashMap;
import static org.junit.Assert.*;

/**
 * @author tnine
 */
@NotThreadSafe // due to use of /testproperties end-point
public class ManagementResourceIT extends AbstractRestIT {

    private static final Logger logger = LoggerFactory.getLogger(ManagementResourceIT.class);
    private org.apache.usergrid.rest.test.resource.endpoints.mgmt.ManagementResource management;

    public ManagementResourceIT() throws Exception {

    }


    @Before
    public void setup() {
        management= clientSetup.getRestClient().management();
        Token token = management.token()
            .get( new QueryParameters()
                .addParam( "grant_type", "password" )
                .addParam( "username", clientSetup.getEmail() )
                .addParam( "password", clientSetup.getPassword() ) );
        management.token().setToken(token);
    }

    /**
     * Test if we can reset our password as an admin
     */
    @Test
    public void setSelfAdminPasswordAsAdmin() {
        UUID uuid =  UUIDUtils.newTimeUUID();
        management.token().setToken(clientSetup.getSuperuserToken());
        management.orgs()
            .org( clientSetup.getOrganizationName() )
            .users()
            .post( ApiResponse.class, new User( "test" + uuid, "test" + uuid, "test" + uuid + "@email.com", "test" ) );
        Map<String, Object> data = new HashMap<>();
        data.put( "newpassword", "foo" );
        data.put( "oldpassword", "test" );
        management.users()
            .user( "test" + uuid )
            .password()
            .post( Entity.class, data );
        Token token = management.token().post(Token.class, new Token( "test"+uuid, "foo" ) );
        management.token().setToken( token );
        data.clear();
        data.put( "oldpassword", "foo" );
        data.put( "newpassword", "test" );
        management.users().user("test"+uuid).password().post(Entity.class, data);
    }


    /**
     * Test that admins can't view organizations they're not authorized to view.
     */
    @Test
    public void crossOrgsNotViewable() throws Exception {

        String differentiator = UUIDUtils.newTimeUUID().toString();
        String username = "test" + differentiator;
        String name = "someguy2" + differentiator;
        String email = "someguy" + differentiator + "@usergrid.com";
        String password = "password";
        String orgName = "someneworg" + differentiator;

        Entity payload =
                new Entity().chainPut("company", "Apigee" );

        Organization organization = new Organization(orgName,username,email,name,password,payload);

        Organization node = management().orgs().post(  organization );
        management.token().get(clientSetup.getUsername(), clientSetup.getPassword());

        // check that the test admin cannot access the new org info

        //  management/organizations/{orgName}
        Response.Status status = null;
        try {
            this.management().orgs().org( orgName ).get(String.class);
        }
        catch ( ClientErrorException uie ) {
            status = Response.Status.fromStatusCode( uie.getResponse().getStatus() );
        }
        assertNotNull( status );
        assertEquals( Response.Status.UNAUTHORIZED, status );


        //  management/organizations/{orgName}/users
        status = null;
        try {
            this.management().orgs().org( orgName ).users().get( String.class );
        }
        catch ( ClientErrorException uie ) {
            status = Response.Status.fromStatusCode( uie.getResponse().getStatus() );
        }
        assertNotNull( status );
        assertEquals( Response.Status.UNAUTHORIZED, status );


        //  management/organizations/{orgName}/applications
        status = null;
        try {
            this.management().orgs().org( orgName ).applications().get( String.class );
        }
        catch ( ClientErrorException uie ) {
            status = Response.Status.fromStatusCode( uie.getResponse().getStatus() );
        }
        assertNotNull( status );
        assertEquals( Response.Status.UNAUTHORIZED, status );


        // this admin should have access to test org
        status = null;
        try {
            this.management().orgs().org( this.clientSetup.getOrganizationName() ).get( String.class );
        }
        catch ( ClientErrorException uie ) {
            status = Response.Status.fromStatusCode( uie.getResponse().getStatus() );
        }
        assertNull( status );


        // this admin should have access to test org - users
        status = null;
        try {
            this.management().orgs().org( this.clientSetup.getOrganizationName() ).users().get( String.class );
        }
        catch ( ClientErrorException uie ) {
            status = Response.Status.fromStatusCode( uie.getResponse().getStatus() );
        }
        assertNull(status);


        // this admin should have access to test org - apps
        status = null;
        try {
            this.management().orgs().org( this.clientSetup.getOrganizationName() ).applications().get( String.class );
        }
        catch ( ClientErrorException uie ) {
            status = Response.Status.fromStatusCode( uie.getResponse().getStatus() );
        }
        assertNull(status);


        // test getting the organization by org
        status = null;
        try {
            this.management().orgs().org( this.clientSetup.getOrganizationName() ).get( String.class );
        }
        catch ( ClientErrorException uie ) {
            status = Response.Status.fromStatusCode( uie.getResponse().getStatus() );
        }

        assertNull(status);
    }


    /**
     * Test that we can support over 10 items in feed.
     */
    @Test
    public void mgmtFollowsUserFeed() throws Exception {
        List<String> users1 = new ArrayList<String>();
        int i;
        //try with 10 users
        for ( i = 0; i < 10; i++ ) {
            users1.add( "follower" + Integer.toString( i ) );
        }

        refreshIndex(  );

        checkFeed( "leader1", users1 );
        //try with 11
        List<String> users2 = new ArrayList<String>();
        for ( i = 20; i < 31; i++ ) {
            users2.add( "follower" + Integer.toString( i ) );
        }
        checkFeed( "leader2", users2 );
    }


    private void checkFeed( String leader, List<String> followers ) throws IOException {
        List<Entity> userFeed;

        //create user
        createUser( leader );
        refreshIndex(   );

        String preFollowContent = leader + ": pre-something to look for " + UUID.randomUUID().toString();

        addActivity( leader, leader + " " + leader + "son", preFollowContent );
        refreshIndex(  );

        String lastUser = followers.get( followers.size() - 1 );
        int i = 0;
        for ( String user : followers ) {
            createUser( user );
            refreshIndex( );
            follow( user, leader );
            refreshIndex(  );
        }
        userFeed = getUserFeed( lastUser );
        assertTrue( userFeed.size() == 1 );

        //retrieve feed
        userFeed = getUserFeed( lastUser );
        assertTrue( userFeed.size() == 1 );
        String postFollowContent = leader + ": something to look for " + UUID.randomUUID().toString();
        addActivity( leader, leader + " " + leader + "son", postFollowContent );

        refreshIndex(  );

        //check feed
        userFeed = getUserFeed( lastUser );
        assertNotNull( userFeed );
        assertTrue( userFeed.size() > 1 );
        String serialized = ((Entity)userFeed.get(0))
            .get( "content" )
            .toString()+ ((Entity)userFeed.get(1))
            .get( "content" ).toString();
        assertTrue( serialized.indexOf( postFollowContent ) >= 0 );
        assertTrue( serialized.indexOf( preFollowContent ) >= 0 );
    }


    private void createUser( String username ) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put( "username", username );
       this.app().collection("users").post(String.class, payload);
    }


    private List<Entity> getUserFeed( String username ) throws IOException {
        Collection collection = this.app().collection("users").entity(username).collection("feed").get();
        return collection.getResponse().getEntities();
    }


    private void follow( String user, String followUser ) {
        //post follow
        Entity entity = this.app()
            .collection( "users" )
            .entity(user)
            .collection("following")
            .collection("users")
            .entity(followUser)
            .post();
    }


    private void addActivity( String user, String name, String content ) {
        Map<String, Object> activityPayload = new HashMap<String, Object>();
        activityPayload.put( "content", content );
        activityPayload.put( "verb", "post" );
        Map<String, String> actorMap = new HashMap<String, String>();
        actorMap.put( "displayName", name );
        actorMap.put( "username", user );
        activityPayload.put("actor", actorMap);
        Entity entity = this.app()
            .collection( "users" )
            .entity(user)
            .collection("activities")
            .post( new Entity( activityPayload ) );
    }


    @Test
    public void mgmtCreateAndGetApplication() throws Exception {

        // POST /applications
        ApiResponse apiResponse = management()
            .orgs()
            .org( clientSetup.getOrganizationName() )
            .app()
            .post( new Application( "mgmt-org-app" ) );


        refreshIndex();

        Entity appdata = apiResponse.getEntities().get(0);
        assertEquals((clientSetup.getOrganizationName() + "/mgmt-org-app")
            .toLowerCase(), appdata.get("name") .toString() .toLowerCase());
        assertNotNull(appdata.get("metadata"));
        Map metadata =(Map) appdata.get( "metadata" );
        assertNotNull(metadata.get("collections"));
        Map collections =  ((Map)metadata.get("collections"));
        assertNotNull(collections.get("roles"));
        Map roles =(Map) collections.get("roles");
        assertNotNull(roles.get("title"));
        assertEquals("Roles", roles.get("title").toString());
        assertEquals(4, roles.size());

        refreshIndex(   );

        // GET /applications/mgmt-org-app


        Entity app = management().orgs().org( clientSetup.getOrganizationName() ).app().addToPath("mgmt-org-app").get();


        assertEquals(this.clientSetup.getOrganizationName().toLowerCase(), app.get("organizationName").toString());
        assertEquals( "mgmt-org-app", app.get( "applicationName" ).toString() );

        assertEquals( clientSetup.getOrganizationName().toLowerCase() + "/mgmt-org-app", app.get( "name" ).toString() );
        metadata =(Map) appdata.get( "metadata" );
        collections =  ((Map)metadata.get("collections"));
        roles =(Map) collections.get("roles");

        assertEquals( "Roles", roles.get("title").toString() );
        assertEquals(4, roles.size());

    }

    @Test
    public void checkSizes() throws Exception {
        final String appname = clientSetup.getAppName();
        this.app().collection("testCollection").post(new Entity().chainPut("name","test"));
        refreshIndex();
        Entity size = management().orgs().org( clientSetup.getOrganizationName() ).app().addToPath(appname).addToPath("_size").get();
        Entity rolesSize = management().orgs().org(clientSetup.getOrganizationName()).app().addToPath(appname).addToPath("roles/_size").get();
        Entity collectionsSize = management().orgs().org(clientSetup.getOrganizationName()).app().addToPath(appname).addToPath("collections/_size").get();

        assertTrue(size != null);
        assertTrue(rolesSize != null);
        int sum =  (int)((LinkedHashMap)((LinkedHashMap)size.metadata().get("aggregation")).get("size")).get("application");
        int sumRoles = (int)((LinkedHashMap)((LinkedHashMap)rolesSize.metadata().get("aggregation")).get("size")).get("roles");
        int sumRoles2 = (int)((LinkedHashMap)((LinkedHashMap)collectionsSize.metadata().get("aggregation")).get("size")).get("roles");

        assertTrue(size != null);
        assertTrue(rolesSize != null);

        assertNotEquals(sum, sumRoles);
        assertTrue(sum > sumRoles);
        assertTrue(sumRoles == sumRoles2);
    }

    @Test
    public void tokenTtl() throws Exception {

        long ttl = 2000;

        Token token = management.token().get( new QueryParameters()
            .addParam( "grant_type", "password" )
            .addParam( "username", clientSetup.getEmail() )
            .addParam( "password", clientSetup.getPassword() )
            .addParam( "ttl", String.valueOf( ttl ) ) );


        long startTime = System.currentTimeMillis();


        assertNotNull( token );

        Entity userdata = management.users().entity(clientSetup.getEmail()).get(token);

        assertNotNull(userdata.get("email").toString());

        // wait for the token to expire
        Thread.sleep(  (System.currentTimeMillis() - startTime) + ttl );

        Response.Status responseStatus = null;
        try {
            userdata = management.users().user(clientSetup.getEmail()).get();
        }
        catch ( ClientErrorException uie ) {
            responseStatus = Response.Status.fromStatusCode( uie.getResponse().getStatus() );
        }

        assertEquals( Response.Status.UNAUTHORIZED, responseStatus );
    }


    @Test
    public void token() throws Exception {
        Token myToken = management.token()
            .get( new QueryParameters()
                .addParam( "grant_type", "password" )
                .addParam( "username", clientSetup.getEmail() )
                .addParam( "password", clientSetup.getPassword() ) );

        String token = myToken.getAccessToken();
        assertNotNull( token );

        // set an organization property
        Organization payload = new Organization();
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put( "securityLevel", 5 );
        payload.put( OrganizationsResource.ORGANIZATION_PROPERTIES, properties );
        management.orgs().org( clientSetup.getOrganizationName() ).put( payload );


        // ensure the organization property is included
        String obj = management.token().get(String.class,new QueryParameters().addParam("access_token", token));
        assertTrue(obj.indexOf("securityLevel")>0);
    }


    @Test
    public void meToken() throws Exception {
        QueryParameters queryParameters = new QueryParameters()
            .addParam( "grant_type", "password" )
            .addParam( "username", clientSetup.getUsername() )
            .addParam( "password", clientSetup.getPassword() );
        Token myToken = management.me().get( Token.class, queryParameters );


        String token = myToken.getAccessToken();
        assertNotNull( token );

        Entity entity = management.me().get( Entity.class );

        assertNotNull( entity.get( "passwordChanged" ) );
        assertNotNull( entity.get( "access_token" ) );
        assertNotNull( entity.get( "expires_in" ) );
        Map<String,Object> userNode =(Map<String,Object>) entity.get( "user" );
        assertNotNull( userNode );
        assertNotNull( userNode.get( "uuid" ) );
        assertNotNull( userNode.get( "username" ) );
        assertNotNull( userNode.get( "email" ) );
        assertNotNull( userNode.get( "name" ) );
        assertNotNull( userNode.get( "properties" ) );
        Map<String,Object> orgsNode = (Map<String,Object>) userNode.get( "organizations" );
        assertNotNull( orgsNode );
        Map<String,Object> orgNode =(Map<String,Object>) orgsNode.entrySet().iterator().next().getValue();
        assertNotNull( orgNode );
        assertNotNull( orgNode.get( "name" ) );
        assertNotNull( orgNode.get( "properties" ) );
    }


    @Test
    public void meTokenPost() throws Exception {
        Map<String, String> payload = hashMap( "grant_type", "password" )
            .map( "username", clientSetup.getUsername() )
            .map( "password", clientSetup.getPassword() );

        JsonNode node = management.me().post( JsonNode.class, payload );

        logger.info("node:", node);
        String token = node.get( "access_token" ).textValue();

        assertNotNull( token );

        node = management.me().get( JsonNode.class );
    }


    @Test
    public void meTokenPostForm() {

        Form form = new Form();
        form.param( "grant_type", "password" );
        form.param( "username", clientSetup.getUsername() );
        form.param( "password", clientSetup.getPassword() );

        JsonNode node = management.me().post( JsonNode.class, form );
        logger.info( "node:", node);

        String token = node.get( "access_token" ).textValue();

        assertNotNull( token );

        node = target().path( "/management/me" )
            .queryParam( "access_token", token ).request()
            .accept( MediaType.APPLICATION_JSON )
            .get( JsonNode.class );
        logger.info("node:", node );

    }


    @Test
    public void ttlNan() throws Exception {

        Map<String, String> payload = hashMap( "grant_type", "password" )
            .map( "username", clientSetup.getUsername() )
            .map( "password", clientSetup.getPassword() )
            .map( "ttl", "derp" );

        Response.Status responseStatus = null;
        try {
           management.token().post( JsonNode.class, payload );
        }
        catch ( ClientErrorException uie ) {
            responseStatus = Response.Status.fromStatusCode( uie.getResponse().getStatus());
        }

        assertEquals( Response.Status.BAD_REQUEST, responseStatus );
    }


    @Test
    public void ttlOverMax() throws Exception {

        Map<String, String> payload = hashMap( "grant_type", "password" )
            .map( "username", clientSetup.getUsername() )
            .map( "password", clientSetup.getPassword() )
            .map( "ttl", Long.MAX_VALUE + "" );

        Response.Status responseStatus = null;

        try {
            management.token().post( JsonNode.class, payload );
        }
        catch ( ClientErrorException uie ) {
            responseStatus = Response.Status.fromStatusCode( uie.getResponse().getStatus() );
        }

        assertEquals( Response.Status.BAD_REQUEST, responseStatus );
    }


    @Test
    public void revokeToken() throws Exception {
        Token token1 = management.token().get(clientSetup.getUsername(),clientSetup.getPassword());

        Entity response = management.users().user(clientSetup.getUsername()).get();

        assertNotNull(response.get("email").toString());

        response =   management.users().user(clientSetup.getUsername()).get();

        assertNotNull(response.get("email").toString());

        // now revoke the tokens
        response = management.users().user(
            clientSetup.getUsername()).revokeTokens().post(true,Entity.class,null, null,false);

        // the tokens shouldn't work

        Response.Status status = null;

        try {
            response = management.users().user(clientSetup.getUsername()).get();
        }
        catch ( ClientErrorException uie ) {
            status = Response.Status.fromStatusCode( uie.getResponse().getStatus() );
        }

        assertEquals( Response.Status.UNAUTHORIZED, status );

        Token token3 = management.token().get(clientSetup.getUsername(), clientSetup.getPassword());

        response = management.users().user(clientSetup.getUsername()).get();
        assertNotNull(response.get("email").toString());

        // now revoke the token3
        QueryParameters queryParameters = new QueryParameters();
        queryParameters.addParam( "token", token3.getAccessToken() );
        management.users().user(
            clientSetup.getUsername()).revokeToken().post( false, Entity.class,null,queryParameters );

        // the token3 shouldn't work
        status = null;

        try {
            management.users().user(clientSetup.getUsername()).get();
        }
        catch ( ClientErrorException uie ) {
            status = Response.Status.fromStatusCode( uie.getResponse().getStatus() );
        }

        assertEquals( Response.Status.UNAUTHORIZED, status );

    }


    @Test
    public void testValidateExternalToken() throws Exception {

        // create a new admin user, get access token

        String rand = RandomStringUtils.randomAlphanumeric(10);
        final String username = "user_" + rand;
        management().orgs().post(
            new Organization( username, username, username+"@example.com", username, "password", null ) );

        refreshIndex();

        refreshIndex();
        QueryParameters queryParams = new QueryParameters()
            .addParam( "username", username )
            .addParam( "password", "password" )
            .addParam( "grant_type", "password" );
        Token accessInfoNode = management.token().get(queryParams);
        String accessToken = accessInfoNode.getAccessToken();

        // set the Usergrid Central SSO URL because Tomcat port is dynamically assigned

        String suToken = clientSetup.getSuperuserToken().getAccessToken();
        Map<String, String> props = new HashMap<String, String>();
        props.put( USERGRID_CENTRAL_URL, getBaseURI().toURL().toExternalForm() );
        pathResource( "testproperties" ).post( props );

        try {

            // attempt to validate the token, must be valid
            queryParams = new QueryParameters()
                .addParam( "ext_access_token", accessToken )
                .addParam( "ttl", "1000" );

            Entity validatedNode = management.externaltoken().get( Entity.class, queryParams );
            String validatedAccessToken = validatedNode.get( "access_token" ).toString();
            assertEquals( accessToken, validatedAccessToken );

            // attempt to validate an invalid token, must fail

            try {
                queryParams = new QueryParameters()
                    .addParam( "access_token", suToken )
                    .addParam( "ext_access_token", "rubbish_token" )
                    .addParam( "ttl", "1000" );

                validatedNode = management.externaltoken().get( Entity.class, queryParams );

                fail( "Validation should have failed" );

            } catch (ClientErrorException actual) {
                assertEquals( 404, actual.getResponse().getStatus() );
                String errorMsg = actual.getResponse().readEntity( JsonNode.class )
                    .get( "error_description" ).toString();
                logger.error( "ERROR: " + errorMsg );
                assertTrue( errorMsg.contains( "Cannot find Admin User" ) );
            }

            // TODO: how do we test the create new user and organization case?

        } finally {

            // unset the Usergrid Central SSO URL so it does not interfere with other tests

            props.put( USERGRID_CENTRAL_URL, "" );
            pathResource( "testproperties" ).post( props );
        }

    }


    @Test
    public void testSuperuserOnlyWhenValidateExternalTokensEnabled() throws Exception {

        // create an org and an admin user

        String rand = RandomStringUtils.randomAlphanumeric( 10 );
        final String username = "user_" + rand;
        management().orgs().post(
            new Organization( username, username, username+"@example.com", username, "password", null ) );

        // turn on validate external tokens by setting the usergrid.central.url

        String suToken = clientSetup.getSuperuserToken().getAccessToken();
        Map<String, String> props = new HashMap<String, String>();
        props.put( USERGRID_CENTRAL_URL, getBaseURI().toURL().toExternalForm() );
        pathResource( "testproperties" ).post( props );

        try {
            // calls to login as an Admin User must now fail

            try {

                Map<String, Object> loginInfo = new HashMap<String, Object>() {{
                    put( "username", username );
                    put( "password", "password" );
                    put( "grant_type", "password" );
                }};
                ApiResponse postResponse = pathResource( "management/token" ).post( false, ApiResponse.class, loginInfo );
                fail( "Login as Admin User must fail when validate external tokens is enabled" );

            } catch (ClientErrorException actual) {
                assertEquals( 400, actual.getResponse().getStatus() );
                String errorMsg = actual.getResponse().readEntity( JsonNode.class )
                    .get( "error_description" ).toString();
                logger.error( "ERROR: " + errorMsg );
                assertTrue( errorMsg.contains( "Admin Users must login via" ) );

            } catch (Exception e) {
                fail( "We expected a ClientErrorException" );
            }

            // login as superuser must succeed

            Map<String, Object> loginInfo = new HashMap<String, Object>() {{
                put( "username", "superuser" );
                put( "password", "superpassword" );
                put( "grant_type", "password" );
            }};
            ApiResponse postResponse2 = pathResource( "management/token" ).post( loginInfo );
            String accessToken = postResponse2.getAccessToken();
            assertNotNull( accessToken );

        } finally {

            // turn off validate external tokens by un-setting the usergrid.central.url

            props.put( USERGRID_CENTRAL_URL, "" );
            pathResource( "testproperties" ).post( props );
        }
    }

}
