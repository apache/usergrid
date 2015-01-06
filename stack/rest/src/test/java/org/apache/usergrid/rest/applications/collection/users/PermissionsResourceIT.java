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


import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.usergrid.rest.test.resource2point0.AbstractRestIT;
import org.apache.usergrid.rest.test.resource2point0.model.*;
import org.elasticsearch.common.collect.HppcMaps;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.apache.usergrid.cassandra.Concurrent;
import org.apache.usergrid.java.client.entities.Group;
import org.apache.usergrid.management.ApplicationInfo;
import org.apache.usergrid.management.OrganizationOwnerInfo;
import org.apache.usergrid.utils.UUIDUtils;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.api.client.UniformInterfaceException;
import java.io.IOException;

import static org.apache.usergrid.utils.MapUtils.hashMap;
import static org.junit.Assert.*;


/**
 * Tests permissions of adding and removing users from roles as well as groups
 *
 * @author tnine
 */
@Concurrent()
public class PermissionsResourceIT extends AbstractRestIT {

    private static final String ROLE = "permtestrole";

    private static final String USER = "edanuff";
    private User user;


    public PermissionsResourceIT() throws Exception {

    }
    
    @Before
    public void setup(){

        user = new User(USER,USER,USER+"@apigee.com","password");
        user = new User( this.app().collection("users").post(user));
        refreshIndex();
    }


    @Test
    public void deleteUserFromRole() throws IOException {
        Entity data = new Entity().chainPut("name", ROLE);

        Entity node = this.app().collection("roles").post(data);

        assertNull(node.get("error"));

        assertEquals( ROLE, node.get("name").toString() );

        refreshIndex();
        node = this.app().collection("roles").entity(node).collection("users").entity(USER).post();
        assertNull( node.get( "error" ) );

        refreshIndex();

        // now check the user has the role
        node =  this.app().collection("users").entity(USER).collection("roles").entity(ROLE).get();


        // check if the role was assigned
        assertEquals( ROLE, node.get("name").toString() );

        // now delete the role
        this.app().collection("users").entity(USER).collection("roles").entity(ROLE).delete();

        refreshIndex();

        // check if the role was deleted

        int status = 0;
        try {
            node = this.app().collection("users").entity(USER).collection("roles").entity(ROLE).get();
        }catch (UniformInterfaceException e){
            status = e.getResponse().getStatus();
        }

        // check if the role was assigned
        assertEquals(status, 404);
    }


    @Test
    public void deleteUserGroup() throws IOException {

        // don't populate the user, it will use the currently authenticated
        // user.
        String groupPath = "groupPath" ;

        Entity data = new Entity().chainPut("name",groupPath).chainPut("type", "group").chainPut( "path", groupPath );

        Entity node = this.app().collection("groups").post(data);

        assertNull( node.get( "error" ) );

        refreshIndex();

        node = this.app().collection("groups").entity(groupPath).collection("users").entity(USER).post();

        assertNull( node.get( "error" ) );

        refreshIndex();

       Collection groups = this.app().collection("users").entity(USER).collection("groups").get();

        assertEquals(groups.next().get("name"), groupPath);

        // now delete the group

        ApiResponse response = this.app().collection("groups").entity(groupPath).collection("users").entity(USER).delete();

        assertNull( response.getError() );

        refreshIndex();

        int status = 0;
        try {
            groups = this.app().collection("users").entity(USER).collection("groups").get();
            assertFalse(groups.hasNext());
        }catch (UniformInterfaceException e){
            status=e.getResponse().getStatus();
            fail();
        }

    }


    /**
     * For the record, you should NEVER allow the guest role to add roles. This is a gaping security hole and a VERY BAD
     * IDEA! That being said, this should technically work, and needs testing.
     */
    @Test
    public void dictionaryPermissions() throws Exception {
        UUID id = UUIDUtils.newTimeUUID();

        String applicationName = "testapp";
        String orgname = "dictionaryPermissions";
        String username = "permissionadmin" + id;
        String password = "password";
        String email = String.format( "email%s@usergrid.com", id );


        // add the perms to the guest to allow users in the role to create roles
        // themselves
        addPermission(  "guest", "get,put,post:/roles/**" );

        Entity data = new Entity().chainPut("name", "usercreatedrole");

        // create a role as the user
        Entity entity  = this.app().collection("roles").post(data);

        assertNull( entity.getError() );

        refreshIndex();

        // now try to add permission as the user, this should work
        addPermission(  "usercreatedrole", "get,put,post:/foo/**" );
    }


    /**
     * Tests a real world example with the following steps. Creates an application.
     * <p/>
     * Creates a new role "reviewer"
     * <p/>
     * Grants a permission to GET, POST, and PUT the reviews url for the reviewer role
     * <p/>
     * Grants a permission GET on the reviewer for the
     * <p/>
     * Create a user reviewer1 and add them to the reviewer role
     * <p/>
     * Test access with reviewer1
     * <p/>
     * Create a group reviewergroup and add the "reviewer" group to it
     * <p/>
     * Create a user reviewer 2 and add them to the "reveiwergroup"
     */
    @Test
    public void applicationPermissions() throws Exception {
        // now create the new role
        UUID secondUserId = createRoleUser(  "reviewer2", "reviewer2@usergrid.com" );
        UUID userId = createRoleUser( "reviewer1",  "reviewer1@usergrid.com" );

        Entity  data = new Entity().chainPut("name", "reviewer");

        Entity node = this.app().collection("roles").post(data);

        assertNull( node.getError() );

        // delete the default role to test permissions later
        refreshIndex();

        ApiResponse response = this.app().collection("roles").entity("default").delete();

        assertNull( response.getError() );
        refreshIndex();

        // grant the perms to reviewer
        addPermission( "reviewer", "get,put,post:/reviews/**" );

        // grant get to guest
        addPermission(  "guest", "get:/reviews/**" );

        Entity group = new Entity().chainPut( "path", "reviewergroup" ).chainPut("name","reviewergroup");

        // now create the group
        this.app().collection("groups").post(group);

        refreshIndex();

        this.app().collection("groups").entity("reviewergroup").collection("roles").entity("reviewer").post();

        refreshIndex();

        // add the user to the group
        this.app().collection("users").entity("reviewer2").collection("groups").entity("reviewergroup").post();

        refreshIndex();

        Entity userRole = this.app().collection("users").entity("reviewer1").collection("roles").entity("reviewer").post();
        // grant this user the "reviewer" role

        refreshIndex();

        this.app().token().post(new Token("reviewer1","password"));

        Entity review =
                new Entity().chainPut("rating", "4").chainPut("name", "noca").chainPut("review", "Excellent service and food");

        this.app().collection("reviews").post(review);
        refreshIndex();

        // post a review as the reviewer1 user
        review = new Entity().chainPut ("rating", "4").chainPut( "name", "4peaks").chainPut("review", "Huge beer selection" );
        this.app().collection("reviews").post(review);

        refreshIndex();

        // get the reviews
        Collection reviews = this.app().collection("reviews").get();

        assertEquals( "noca",reviews.next().get("name") );
        assertEquals("4peaks", reviews.next().get("name").toString());

        // can't delete, not in the grants

        int status = 0;
        try {
            this.app().collection("reviews").entity("noca").delete();
        }
        catch ( UniformInterfaceException uie ) {
            status = uie.getResponse().getStatus();
        }

        assertEquals( Status.UNAUTHORIZED.getStatusCode(), status );

        refreshIndex();

        status = 0;

        try {
            this.app().collection("reviews").entity("4peaks").delete();
        }
        catch ( UniformInterfaceException uie ) {
            status = uie.getResponse().getStatus();
        }

        assertEquals( Status.UNAUTHORIZED.getStatusCode(), status );

        refreshIndex();

        // now test some groups

        // post 2 reviews. Should get permissions from the group

        Token secondUserToken = this.app().token().post(new Token("reviewer2", "password"));

        review = new Entity().chainPut("rating", "4").chainPut("name", "cowboyciao").chainPut("review", "Great atmosphoere");

        // post a review as the reviewer2 user
        this.app().collection("reviews").post(review);

        refreshIndex();
        review = new Entity().chainPut( "rating", "4" ).chainPut("name", "currycorner").chainPut( "review", "Authentic" );

        // post a review as the reviewer2 user
        this.app().collection("reviews").post(review);

        refreshIndex();

        reviews =  this.app().collection("reviews").get();

        // get all reviews as a user


        assertEquals("noca", reviews.next().get("name").toString());
        assertEquals("4peaks", reviews.next().get("name").toString());
        assertEquals("cowboyciao", reviews.next().get("name").toString());
        assertEquals("currycorner", reviews.next().get("name").toString());

        // issue a delete, it shouldn't work, no permissions

        status = 0;

        try {
            this.app().collection("reviews").entity("cowboyciao").delete();
      
        }
        catch ( UniformInterfaceException uie ) {
            status = uie.getResponse().getStatus();
        }

        assertEquals( Status.UNAUTHORIZED.getStatusCode(), status );

        refreshIndex();

        status = 0;

        try {
            this.app().collection("reviews").entity("currycorner").delete();

        }
        catch ( UniformInterfaceException uie ) {
            status = uie.getResponse().getStatus();
        }

        assertEquals( Status.UNAUTHORIZED.getStatusCode(), status );
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

        this.app().collection("roles").entity("default").delete();
        Entity data = new Entity().chainPut("name", "reviewer");

        this.app().collection("roles").post(data);

        refreshIndex();

        // allow access to reviews
        addPermission( "reviewer",
                "get,put,post:/reviews/**" );
        // allow access to all user's connections
        addPermission( "reviewer",
                "get,put,post:/users/${user}/**" );
        // allow access to the review relationship
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

        refreshIndex();
        
        this.app().collection("users").entity(userOneId).collection("roles").entity("reviewer").post();

       
        refreshIndex();

        Entity book = new Entity().chainPut( "title", "Ready Player One" ).chainPut("author", "Earnest Cline");

        book = this.app().collection("books").post(book);
        // create a book as admin
       

        assertEquals( "Ready Player One", book.get("title").toString() );
        String bookId = book.get("uuid").toString();

        refreshIndex();

        this.app().token().post(new Token("wildcardpermuserone","password"));
        // post a review of the book as user1
        // POST https://api.usergrid.com/my-org/my-app/users/$user1/reviewed/books/$uuid
        Entity review =
                new Entity().chainPut( "heading", "Loved It" ).chainPut( "body", "80s Awesomeness set in the future" );
        review = this.app().collection("reviews").post(review);
        String reviewId = review.get("uuid").toString();

        refreshIndex();

        // POST https://api.usergrid.com/my-org/my-app/users/me/wrote/review/${reviewId}
        this.app().collection("users").entity("me").connection("wrote").collection("review").entity(reviewId).post();


        refreshIndex();
        this.app().collection("users").entity("me").connection("reviewed").collection("books").entity(bookId).post();


        refreshIndex();



        // POST https://api.usergrid.com/my-org/my-app/books/${bookId}/review/${reviewId}
        this.app().collection("books").entity(bookId).collection("review").entity(reviewId).post();


        refreshIndex();

        // now try to post the same thing to books to verify as userOne the failure
        int status = 0;
        try {
            this.app().collection("books").post();

        }
        catch ( UniformInterfaceException uie ) {
            status = uie.getResponse().getStatus();
        }
        assertEquals( Status.UNAUTHORIZED.getStatusCode(), status );

        refreshIndex();
        this.app().collection("users").entity("me").connection("reviewed").collection("books").get();

        this.app().collection("reviews").entity(reviewId).get();

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
    @Test
    public void wildcardFollowingPermission() throws Exception {
        app().collection("roles").entity("default").delete();
        Entity data = new Entity().chainPut( "name", "patient" );
        app().collection("roles").post(data);

        //allow patients to add doctors as their followers
        addPermission(  "patient", "delete,post:/users/*/following/users/${user}" );
        refreshIndex();
        // create examplepatient
        UUID patientId =  createRoleUser( "examplepatient",  "examplepatient@apigee.com" );
        assertNotNull( patientId );

        // create exampledoctor
        UUID doctorId = createRoleUser( "exampledoctor",  "exampledoctor@apigee.com" );
        assertNotNull( doctorId );
        refreshIndex();
        // assign examplepatient the patient role
        this.app().collection("users").entity(patientId).collection("roles").entity("patient").post();
        refreshIndex();
        this.app().token().post(new Token("examplepatient","password"));
        refreshIndex();
        //not working yet, used to be ignored
//        this.app().collection("users").entity("exampledoctor").connection("following").collection("users").entity("examplepatient").post();
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

        return entity.getUuid();
    }


    /** Test adding the permission to the role */
    private void addPermission(  String rolename, String grant ) throws IOException {
        Entity props = new Entity().chainPut("permission", grant);

        this.app().collection("roles").entity(rolename).collection("permissions").post(props);

        Collection node = this.app().collection("roles").entity(rolename).collection("permissions").get();

        List<Object> data =(List) node.getResponse().getData();

        for(Object o : data){
            if(grant.equals(o.toString())){
                return;
            }
        }

        fail( String.format( "didn't find grant %s in the results", grant ) );
    }


}
