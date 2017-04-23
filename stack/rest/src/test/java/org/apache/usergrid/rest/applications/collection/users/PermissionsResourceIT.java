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
package org.apache.usergrid.rest.applications.collection.users;


import net.jcip.annotations.NotThreadSafe;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.usergrid.rest.test.resource.AbstractRestIT;
import org.apache.usergrid.rest.test.resource.model.*;
import org.apache.usergrid.utils.UUIDUtils;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.*;


/**
 * Tests permissions of adding and removing users from roles as well as groups.
 */
@NotThreadSafe
public class PermissionsResourceIT extends AbstractRestIT {
    private static final Logger logger = LoggerFactory.getLogger(PermissionsResourceIT.class);

    private static final String ROLE = "permtestrole";

    private static final String USER = "edanuff";
    private User user;


    public PermissionsResourceIT() throws Exception {

    }


    /**
     * Creates a user in the default org/app combo for use by all of the tests
     */
    @Before
    public void setup(){

        user = new User(USER,USER,USER+"@apigee.com","password");
        user = new User( this.app().collection("users").post(user));
        waitForQueueDrainAndRefreshIndex();
    }


    /**
     * Tests that we can delete a role that is in use by a user.
     * @throws IOException
     */
    @Test
    public void deleteUserFromRole() throws IOException {

        //Create a role and check the response
        Entity data = new Entity().chainPut("name", ROLE);

        Entity node = this.app().collection("roles").post(data);

        assertNull(node.get("error"));

        assertEquals( ROLE, node.get("name").toString() );

        waitForQueueDrainAndRefreshIndex();

        //Post the user with a specific role into the users collection
        node = this.app().collection("roles").entity(node).collection("users").entity(USER).post();
        assertNull( node.get( "error" ) );

        waitForQueueDrainAndRefreshIndex();

        // now check the user has the role
        node =  this.app().collection("users").entity(USER).collection("roles").entity(ROLE).get();


        // check if the role was assigned
        assertEquals( ROLE, node.get("name").toString() );

        // now delete the role
        this.app().collection("users").entity(USER).collection("roles").entity(ROLE).delete();

        waitForQueueDrainAndRefreshIndex();

        // check if the role was deleted

        int status = 0;
        try {
            node = this.app().collection("users").entity(USER).collection("roles").entity(ROLE).get();
        }catch (ClientErrorException e){
            status = e.getResponse().getStatus();
        }

        // check if the role was assigned
        assertEquals(404, status);
    }


    /**
     * Deletes a user from the group.
     * @throws IOException
     */
    @Test
    public void deleteUserGroup() throws IOException {

        String groupPath = "groupPath" ;

        //Creates and posts a group.
        Entity data = new Entity().chainPut("name",groupPath).chainPut("type", "group").chainPut( "path", groupPath );

        Entity node = this.app().collection("groups").post(data);

        assertNull( node.get( "error" ) );

        waitForQueueDrainAndRefreshIndex();

        //Create a user that is in the group.
        node = this.app().collection("groups").entity(groupPath).collection("users").entity(user).post();

        assertNull( node.get( "error" ) );

        waitForQueueDrainAndRefreshIndex();

        //Get the user and make sure that they are part of the group
        Collection groups = this.app().collection("users").entity(user).collection("groups").get();

        assertEquals( groups.next().get( "name" ), groupPath );

        // now delete the group

        ApiResponse response = this.app()
            .collection("groups").entity(groupPath).collection("users").entity(user).delete();

        assertNull( response.getError() );

        waitForQueueDrainAndRefreshIndex();

        //Check that the user no longer exists in the group
        int status = 0;
        try {
            this.app().collection("users").entity(user)
                .collection("groups").entity( groupPath ).collection( "users" ).entity( user ).get();
            fail("Should not have been able to retrieve the user as it was deleted");
        }catch (ClientErrorException e){
            status=e.getResponse().getStatus();
            assertEquals( 404, status );
        }

    }


    /**
     * For the record, you should NEVER allow the guest role to add roles.
     * This is a gaping security hole and a VERY BAD IDEA!
     * That being said, this should technically work, and needs testing.
     * Tests that you can allow a guest role to add additional roles.
     */
    @Test
    public void dictionaryPermissions() throws Exception {

        // add the perms to the guest to allow users in the role to create roles
        // themselves
        addPermission( "guest", "get,put,post:/roles/**" );

        Entity data = new Entity().chainPut("name", "usercreatedrole");

        // create a role as the user
        Entity entity  = this.app().collection( "roles" ).post( data );

        assertNull( entity.getError() );

        waitForQueueDrainAndRefreshIndex();

        // now try to add permission as the user, this should work
        addPermission( "usercreatedrole", "get,put,post:/foo/**" );
    }

    @Test
    public void getNonExistentEntityReturns404() throws Exception {

        // Call a get on a existing entity with no access token and check if we get a 401
        try {
            this.app().collection( "roles" ).entity( "guest" ).get( false );
        } catch(ClientErrorException uie){
            assertEquals( 401,uie.getResponse().getStatus() );
        }

        // add the perms such that anybody can do a get call
        addPermission(  "guest", "get:/**" );


        // Call a get on a non existing entity that doesn't need permissions and check it we get a 404.
        try {
            this.app().collection( "roles" ).entity( "banana" ).get( false );
        } catch(ClientErrorException uie){
            assertEquals( 404,uie.getResponse().getStatus() );
        }

        try {
            this.app().collection( "roles" ).entity( UUIDUtils.newTimeUUID() ).get( false );
        } catch(ClientErrorException uie){
            assertEquals( 404,uie.getResponse().getStatus() );
        }
    }



    /**
     * Test application permissions by posting entities different users and making sure we work within
     * the permissions given.
     */
    @Test
    public void applicationPermissions() throws Exception {

        // Creates two new roles: reviewer1 and reviewer2
        createRoleUser( "reviewer1",  "reviewer1@usergrid.com" );
        createRoleUser( "reviewer2", "reviewer2@usergrid.com" );

        Entity  data = new Entity().chainPut("name", "reviewer");

        // Creates a new role "reviewer"
        Entity node = this.app().collection("roles").post(data);

        assertNull( node.getError() );

        waitForQueueDrainAndRefreshIndex();

        // delete the default role to test permissions later
        ApiResponse response = this.app().collection("roles").entity("default").delete();

        assertNull( response.getError() );
        waitForQueueDrainAndRefreshIndex();

        // Grants a permission to GET, POST, and PUT the reviews url for the reviewer role
        addPermission( "reviewer", "get,put,post:/reviews/**" );

        // Grants a permission GET on the guests for the reviews url
        addPermission(  "guest", "get:/reviews/**" );

        // Creates a reviewer group
        Entity group = new Entity().chainPut( "path", "reviewergroup" ).chainPut("name","reviewergroup");

        this.app().collection("groups").post(group);

        waitForQueueDrainAndRefreshIndex();

        // Adds the reviewer to the reviewerGroup
        this.app().collection("groups").entity("reviewergroup").collection("roles").entity("reviewer").post();

        waitForQueueDrainAndRefreshIndex();

        // Adds reviewer2 user to the reviewergroup
        this.app().collection("users").entity("reviewer2").collection("groups").entity("reviewergroup").post();

        waitForQueueDrainAndRefreshIndex();

        // Adds reviewer1 to the reviewer role
        this.app().collection("users").entity("reviewer1").collection("roles").entity("reviewer").post();

        waitForQueueDrainAndRefreshIndex();

        // Set the current context to reviewer1
        this.app().token().post(new Token("reviewer1","password"));

        // Post reviews to the reviews collection as reviewer1
        Entity review = new Entity()
            .chainPut("rating", "4").chainPut("name", "noca").chainPut("review", "Excellent service and food");
        this.app().collection("reviews").post( review );

        review = new Entity()
            .chainPut ("rating", "4").chainPut( "name", "4peaks").chainPut("review", "Huge beer selection" );
        this.app().collection("reviews").post(review);

        waitForQueueDrainAndRefreshIndex();

        // get the reviews and assert they were created
        QueryParameters queryParameters = new QueryParameters();
        queryParameters.setQuery( "select * ORDER BY created" );
        Collection reviews = this.app().collection("reviews").get(queryParameters);

        assertEquals( "noca",reviews.next().get("name") );
        assertEquals("4peaks", reviews.next().get( "name" ));

        // Try to delete the reviews, but it should fail due to have having delete permission in the grants.
        int status = 0;
        try {
            this.app().collection("reviews").entity("noca").delete();
            fail( "this should have failed due to having insufficient permissions" );
        }
        catch ( ClientErrorException uie ) {
            status = uie.getResponse().getStatus();
        }

        assertEquals( Response.Status.UNAUTHORIZED.getStatusCode(), status );

        status = 0;

        // Try to delete the reviews, but it should fail due to have having delete permission in the grants.
        try {
            this.app().collection("reviews").entity("4peaks").delete();
            fail( "this should have failed due to having insufficient permissions" );
        }
        catch ( ClientErrorException uie ) {
            status = uie.getResponse().getStatus();
        }

        assertEquals( Response.Status.UNAUTHORIZED.getStatusCode(), status );

        waitForQueueDrainAndRefreshIndex();

        //TODO: maybe make this into two different tests?

        //Change context to reviewer2
        this.app().token().post(new Token("reviewer2", "password"));

        // post 2 reviews as reviewer2
        review = new Entity()
            .chainPut("rating", "4").chainPut("name", "cowboyciao").chainPut("review", "Great atmosphoere");
        this.app().collection("reviews").post(review);

        review = new Entity()
            .chainPut( "rating", "4" ).chainPut("name", "currycorner").chainPut( "review", "Authentic" );
        this.app().collection("reviews").post(review);

        waitForQueueDrainAndRefreshIndex();

        // get all reviews as reviewer2
        queryParameters = new QueryParameters();
        queryParameters.setQuery( "select * ORDER BY created" );
        reviews =  this.app().collection("reviews").get(queryParameters);

        assertEquals("noca", reviews.next().get("name").toString());
        assertEquals("4peaks", reviews.next().get("name").toString());
        assertEquals("cowboyciao", reviews.next().get("name").toString());
        assertEquals("currycorner", reviews.next().get("name").toString());

        // issue a delete, it shouldn't work, no permissions

        status = 0;

        try {
            this.app().collection("reviews").entity("cowboyciao").delete();
            fail( "this should have failed due to having insufficient permissions" );
        }
        catch ( ClientErrorException uie ) {
            status = uie.getResponse().getStatus();
        }

        assertEquals( Response.Status.UNAUTHORIZED.getStatusCode(), status );

        waitForQueueDrainAndRefreshIndex();

        status = 0;

        try {
            this.app().collection("reviews").entity("currycorner").delete();
            fail( "this should have failed due to having insufficient permissions" );
        }
        catch ( ClientErrorException uie ) {
            status = uie.getResponse().getStatus();
        }

        assertEquals( Response.Status.UNAUTHORIZED.getStatusCode(), status );
    }


    /**
     * Tests the scenario where we have roles declarations such as: <ul> <li>GET /users/[star]/reviews "any user can
     * read any others book review"</li> <li>POST /users/[user1]/reviews "cannot post as user2 to user1's reviews"</li>
     * <ii>POST /users/[star]/reviews/feedback/* "can post as user2 to user1's feedback/good or /bad</ii> </ul>
     * <p/>
     * Scenario is as follows: Create an application
     * <p/>
     * Add two application users - user1 - user2
     * <p/>
     * Create a book collection for user1
     */
    @Test
    public void wildcardMiddlePermission() throws Exception {

        //Deletes the default role
        this.app().collection("roles").entity("default").delete();

        //Creates a reviewer role
        Entity data = new Entity().chainPut("name", "reviewer");
        this.app().collection("roles").post(data);

        waitForQueueDrainAndRefreshIndex();

        // allow access to reviews excluding delete
        addPermission( "reviewer",
                "get,put,post:/reviews/**" );
        // allow access to all user's connections excluding delete
        addPermission( "reviewer",
                "get,put,post:/users/me/**" );
        // allow access to the review relationship excluding delete
        addPermission( "reviewer",
                "get,put,post:/books/*/review/*" );

        // create userOne
        UUID userOneId =
                createRoleUser( "wildcardpermuserone",
                        "wildcardpermuserone@apigee.com" );
        assertNotNull( userOneId );

        // create userTwo
        UUID userTwoId =
                createRoleUser( "wildcardpermusertwo",
                        "wildcardpermusertwo@apigee.com" );
        assertNotNull( userTwoId );

        waitForQueueDrainAndRefreshIndex();

        //Add user1 to the reviewer role
        this.app().collection("users").entity(userOneId).collection("roles").entity("reviewer").post();


        waitForQueueDrainAndRefreshIndex();

        //Add a book to the books collection
        Entity book = new Entity().chainPut( "title", "Ready Player One" ).chainPut("author", "Earnest Cline");

        book = this.app().collection("books").post(book);

        assertEquals( "Ready Player One", book.get("title").toString() );
        String bookId = book.get("uuid").toString();

        waitForQueueDrainAndRefreshIndex();

        //Switch the contex to be that of user1
        this.app().token().post(new Token("wildcardpermuserone","password"));

        // post a review of the book as user1
        // POST https://api.usergrid.com/my-org/my-app/users/$user1/reviewed/books/$uuid
        Entity review =
                new Entity().chainPut( "heading", "Loved It" ).chainPut( "body", "80s Awesomeness set in the future" );
        review = this.app().collection("reviews").post(review);
        String reviewId = review.get("uuid").toString();

        waitForQueueDrainAndRefreshIndex();

        // POST https://api.usergrid.com/my-org/my-app/users/me/wrote/review/${reviewId}
        this.app().collection("users").entity("me").connection("wrote").collection("review").entity(reviewId).post();

        // POST https://api.usergrid.com/my-org/my-app/users/me/reviewed/review/${reviewId}
        this.app().collection("users").entity("me").connection("reviewed").collection("books").entity(bookId).post();

        waitForQueueDrainAndRefreshIndex();

        // POST https://api.usergrid.com/my-org/my-app/books/${bookId}/review/${reviewId}
        this.app().collection("books").entity(bookId).collection("review").entity(reviewId).post();


        waitForQueueDrainAndRefreshIndex();

        // now try to post the same thing to books to verify as userOne does not have correct permissions
        int status = 0;
        try {
            this.app().collection("books").post(book);

        }
        catch ( ClientErrorException uie ) {
            status = uie.getResponse().getStatus();
        }
        assertEquals( Response.Status.UNAUTHORIZED.getStatusCode(), status );

        //Gets all books that user1 reviewed\
        this.app().collection("users").entity("me").connection("reviewed").collection("books").get();

        //Gets a specific review
        this.app().collection("reviews").entity(reviewId).get();

        //Gets all the reviews that user1 wrote
        this.app().collection("users").entity("me").connection("wrote").get();

    }


    /**
     * Tests the scenario where we have role declaration such as: <ul> <li>POST /users/[star]/following/users/${user}" a
     * user can add himself to any other user following list"</li> </ul>
     * <p/>
     * Scenario is as follows: Create an application
     * <p/>
     * Add two application users - examplepatient - exampledoctor
     * <p/>
     * examplepatient add himself to exampledoctor following list
     */
    //TODO: get this test working.
    @Test
    public void wildcardFollowingPermission() throws Exception {
        //Delete default role
        app().collection("roles").entity("default").delete();

        //Create new role named patient
        Entity data = new Entity().chainPut( "name", "patient" );
        app().collection("roles").post(data);

        //allow patients to add doctors as their followers
        addPermission(  "patient", "delete,post:/users/*/following/users/${user}" );
        waitForQueueDrainAndRefreshIndex();

        // create examplepatient
        UUID patientId =  createRoleUser( "examplepatient",  "examplepatient@apigee.com" );
        assertNotNull( patientId );

        // create exampledoctor
        UUID doctorId = createRoleUser( "exampledoctor",  "exampledoctor@apigee.com" );
        assertNotNull( doctorId );
        waitForQueueDrainAndRefreshIndex();
        // assign examplepatient the patient role
        this.app().collection("users").entity(patientId).collection("roles").entity("patient").post();
        waitForQueueDrainAndRefreshIndex();
        this.app().token().post(new Token("examplepatient","password"));
        waitForQueueDrainAndRefreshIndex();
        //not working yet, used to be ignored
        //        this.app().collection("users").entity("exampledoctor").connection("following")
        // .collection("users").entity("examplepatient").post();
    }

    /**
     * Create the user, check there are no errors
     *
     * @return the userid
     */
    private UUID createRoleUser(String username, String email)
            throws Exception {

        User props = new User(username, username, email, "password");

        Entity entity = this.app().collection("users").post(props);

        assertNotNull( entity );

        return entity.getUuid();
    }


    /**
     * Adds the permission in grant to the rolename role, and tests that they were added correctly
     * @param rolename
     * @param grant
     * @throws IOException
     */
    private void addPermission(  String rolename, String grant ) throws IOException {

        // Create and post the permissions
        Entity props = new Entity().chainPut("permission", grant);

        this.app().collection("roles").entity(rolename).collection("permissions").post(props);

        // Checks that the permissions were added correctly
        Collection node = this.app().collection("roles").entity(rolename).collection("permissions").get();

        List<Object> data =(List) node.getResponse().getData();

        for ( Object o : data ) {
            if ( grant.equals(o.toString()) ) {
                return;
            }
        }

        fail( String.format( "didn't find grant %s in the results", grant ) );
    }


    @Test
    public void testUsersMeAlwaysAvailable() {

        // delete default roles/permissions from app

        app().collection("roles").entity("default").delete();

        // create an app user, get token (and switch context to that of user)

        Token token = null;
        try {
            String password = "s3cr3t";
            Entity newUser = app().collection( "users" ).post(
                new User( "dave", "Dave Johnson", "dave@example.com", password ) );
            token = app().token().post(
                new Token( "password", (String)newUser.get("username") , password ));

        } catch ( Exception e ) {
            logger.error( "Error creating user and logging in: {}", e);
        }
        assertNotNull( token );

        // user cannot post to a collection

        try {
            Map<String, Object> catMap = new HashMap<String, Object>() {{
                put("name", "enzo");
                put("color", "orange");
            }};
            app().collection( "cats" ).post( true, token, ApiResponse.class, catMap, null, false );
            fail("Post should have failed");
        } catch ( Exception expected ) {}

        // but the /users/me end-point should work

        Entity me = app().collection( "users" ).entity( "me" ).get();
        assertNotNull( me );

        try {
            app().collection( "users" ).entity( "me" ).delete();
            fail("Delete /users/me must fail");
        } catch ( Exception expected ) {}
    }


    @Test
    public void testAppUserNamedMeNotAllowed() {

        // cannot create app user named me
        try {
            app().collection( "users" ).post( new User( "me", "it's me", "me@example.com", "me!me!" ) );
            fail("Must not be able to create app user named me");
        } catch ( BadRequestException expected ) {}

        // cannot use update to rename app user to me
        Entity user = app().collection( "users" ).post( new User( "dave", "Sneaky Me", "me@example.com", "me!me!" ) );
        try {
            app().collection( "users" ).entity( user ).put( new Entity().chainPut( "username", "me" ));
            fail("Must not be able to update app user to name me");

        } catch ( BadRequestException expected ) {}

    }


    @Test
    public void testAdminUserNamedMeNotAllowed() {

        // cannot create admin user named me
        try {
            Form form = new Form();
            form.param( "username", "me" );
            form.param( "email", "me@example.com");
            form.param( "name", "me Me ME!");
            form.param( "password", "me me 123" );
            management().users().post( ApiResponse.class, form );

            fail("Must not be able to create admin user named me");

        } catch ( BadRequestException expected ) {}

        // cannot use update to rename admin user to me
        String randomString = RandomStringUtils.randomAlphanumeric( 10 );
        String username = "user_" + randomString;
        String password = "me me 123";
        Form form = new Form();
        form.param( "username", username );
        form.param( "email", username + "@example.com");
        form.param( "name", "Despicable me");
        form.param( "password", password );
        management().users().post( ApiResponse.class, form );
        management().token().get( username, password );

        try {
            management().users().user( username ).put( true, new Entity().chainPut( "username", "me" ) );
            fail("Must not be able to create admin user named me");

        } catch ( BadRequestException e ) {}

    }

}
