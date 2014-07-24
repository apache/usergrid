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
package org.apache.usergrid.rest.test.resource;


import java.util.UUID;

import org.apache.usergrid.rest.test.resource.app.Application;
import org.apache.usergrid.rest.test.resource.app.User;
import org.apache.usergrid.rest.test.resource.app.UsersCollection;
import org.apache.usergrid.rest.test.resource.mgmt.Management;
import org.apache.usergrid.rest.test.security.TestUser;

import com.sun.jersey.test.framework.JerseyTest;
import java.io.IOException;


public class TestContext {

    private JerseyTest test;
    private TestUser activeUser;
    private String orgName;
    private UUID orgUuid;
    private String appName;
    private UUID appUuid;


    /**
     *
     */
    protected TestContext( JerseyTest test ) {
        this.test = test;
    }


    /** Create a test context */
    public static TestContext create( JerseyTest test ) {
        return new TestContext( test );
    }


    public TestUser getActiveUser() {
        return activeUser;
    }


    public TestContext withUser( TestUser user ) {
        this.activeUser = user;
        return this;
    }


    public TestContext clearUser() {
        return withUser( null );
    }


    public TestContext withOrg( String orgName ) {
        this.orgName = orgName;
        return this;
    }


    public TestContext withApp( String appName ) {
        this.appName = appName;
        return this;
    }


    public String getOrgName() {
        return orgName;
    }


    public String getAppName() {
        return appName;
    }


    /** Creates the org specified */
    public TestContext createNewOrgAndUser() throws IOException {
        orgUuid = management().orgs().create( orgName, activeUser );

        return this;
    }


    /** Creates the org specified */
    public TestContext createAppForOrg() throws IOException {
        appUuid = management().orgs().organization( orgName ).apps().create( appName );

        return this;
    }


    /** Create the app if it doesn't exist with the given TestUser. If the app exists, the user is logged in */
    public TestContext loginUser() {
        // nothing to do
        if ( activeUser.isLoggedIn() ) {
            return this;
        }

        // try to log in the user first
        activeUser.login( this );

        return this;
    }


    /** Get the users resource for the application */
    public UsersCollection users() {
        return application().users();
    }


    /** Get the app user resource */
    public User user( String username ) {
        return application().users().user( username );
    }


    /** @return the orgUuid */
    public UUID getOrgUuid() {
        return orgUuid;
    }


    /** @return the appUuid */
    public UUID getAppUuid() {
        return appUuid;
    }


    /** Get the application resource */
    public Application application() {
        return new Application( orgName, appName, root() );
    }


    public CustomCollection collection( String str ) {
        return application().collection( str );
    }


    public Management management() {
        return new Management( root() );
    }


    protected RootResource root() {
        return new RootResource( test.resource(), activeUser == null ? null : activeUser.getToken() );
    }


    /** Calls createNewOrgAndUser, logs in the user, then creates the app. All in 1 call. */
    public TestContext initAll() throws IOException {
        return createNewOrgAndUser().loginUser().createAppForOrg();
    }
}
