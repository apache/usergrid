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
package org.apache.usergrid.security;


import java.util.UUID;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.usergrid.management.UserInfo;
import org.apache.usergrid.persistence.entities.User;
import org.apache.usergrid.security.shiro.CustomPermission;
import org.apache.usergrid.security.shiro.principals.ApplicationUserPrincipal;
import org.apache.usergrid.utils.UUIDUtils;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.UnavailableSecurityManagerException;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.support.SubjectThreadState;
import org.apache.shiro.util.LifecycleUtils;
import org.apache.shiro.util.ThreadState;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class CustomResolverTest {

    public static final Logger logger = LoggerFactory.getLogger( CustomResolverTest.class );

    private static ThreadState subjectThreadState;


    @BeforeClass
    public static void setSecurityManager() {
        DefaultSecurityManager manager = new DefaultSecurityManager();
        SecurityUtils.setSecurityManager( manager );
    }


    @AfterClass
    public static void tearDownShiro() {
        doClearSubject();

        try {
            org.apache.shiro.mgt.SecurityManager securityManager = SecurityUtils.getSecurityManager();
            LifecycleUtils.destroy( securityManager );
        }
        catch ( UnavailableSecurityManagerException e ) {
            // we don't care about this when cleaning up the test environment
            // (for example, maybe the subclass is a unit test and it didn't
            // need a SecurityManager instance because it was using only
            // mock Subject instances)
        }
        SecurityUtils.setSecurityManager( null );
    }


    private static void doClearSubject() {
        if ( subjectThreadState != null ) {
            subjectThreadState.clear();
            subjectThreadState = null;
        }
    }


    /**
     * Allows subclasses to set the currently executing {@link Subject} instance.
     *
     * @param subject the Subject instance
     */
    protected void setSubject( Subject subject ) {
        doClearSubject();
        subjectThreadState = new SubjectThreadState( subject );
        subjectThreadState.bind();
    }


    @Test
    public void testResolver() throws Exception {

        testImplies( true, "applications:get:00000000-0000-0000-0000-000000000001:/foo",
                "applications:get:00000000-0000-0000-0000-000000000001:/foo" );

        testImplies( true, "applications:get:00000000-0000-0000-0000-000000000001:/foo/bar/*",
                "applications:get:00000000-0000-0000-0000-000000000001:/foo/bar" );

        testImplies( true, "applications:get:00000000-0000-0000-0000-000000000001:/foo/bar/*",
                "applications:get:00000000-0000-0000-0000-000000000001:/foo/bar/baz" );

        testImplies( false, "applications:get:00000000-0000-0000-0000-000000000001:/foo/bar/*",
                "applications:get:00000000-0000-0000-0000-000000000001:/foo/bar/baz/" );

        testImplies( false, "applications:get:00000000-0000-0000-0000-000000000001:/foo/bar/*",
                "applications:get:00000000-0000-0000-0000-000000000001:/foo/bar/baz/boz" );

        testImplies( true, "applications:get:00000000-0000-0000-0000-000000000001:/foo/bar/**",
                "applications:get:00000000-0000-0000-0000-000000000001:/foo/bar/baz/boz" );

        testImplies( true, "applications:get:00000000-0000-0000-0000-000000000001:/foo/bar/*/boz/*",
                "applications:get:00000000-0000-0000-0000-000000000001:/foo/bar/baz/boz" );

        testImplies( true, "applications:get:00000000-0000-0000-0000-000000000001:/foo/bar/*/boz/*",
                "applications:get:00000000-0000-0000-0000-000000000001:/foo/bar/baz/boz/biz" );

        testImplies( true, "applications:get:00000000-0000-0000-0000-000000000001:/bar/*/boz/*",
                "applications:get:00000000-0000-0000-0000-000000000001:/bar/3b270ee0-a2d7-11e2-b8ac-f14ec968db08/boz"
                        + "/b53761a-a2d7-11e2-abbb-11f6def11e98" );

        testImplies( false, "applications:get:00000000-0000-0000-0000-000000000001:/foo/bar/*/boz/*",
                "applications:get:00000000-0000-0000-0000-000000000001:/foo/bar/baz/boz/biz/box" );
    }


    @Test
    public void userMeSubstitution() {
        User fakeUser = new User();
        fakeUser.setUuid( UUIDUtils.newTimeUUID() );
        fakeUser.setUsername( "testusername" );

        UUID appId = UUIDUtils.newTimeUUID();

        UserInfo info = new UserInfo( appId, fakeUser.getProperties() );


        ApplicationUserPrincipal principal = new ApplicationUserPrincipal( appId, info );
        Subject subject = new Subject.Builder( SecurityUtils.getSecurityManager() )
                .principals( new SimplePrincipalCollection( principal, "usergrid" ) ).buildSubject();

        setSubject( subject );

        testImplies( true, "/users/mefake@usergrid.org/**", "/users/mefake@usergrid.org/permissions" );

        //test substitution
        testImplies( true, "/users/me/**", String.format( "/users/%s/permissions", fakeUser.getUsername() ) );

        testImplies( true, "/users/me/**", String.format( "/users/%s/permissions", fakeUser.getUuid() ) );
    }


    public void testImplies( boolean expected, String s1, String s2 ) {
        CustomPermission p1 = new CustomPermission( s1 );
        CustomPermission p2 = new CustomPermission( s2 );
        if ( expected ) {
            assertTrue( p1.implies( p2 ) );
        }
        else {
            assertFalse( p1.implies( p2 ) );
        }
    }
}
