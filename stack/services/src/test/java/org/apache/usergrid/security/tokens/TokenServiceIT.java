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
package org.apache.usergrid.security.tokens;


import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.NewOrgAppAdminRule;
import org.apache.usergrid.ServiceITSetup;
import org.apache.usergrid.ServiceITSetupImpl;
import org.apache.usergrid.cassandra.ClearShiroSubject;

import org.apache.usergrid.management.ApplicationInfo;
import org.apache.usergrid.management.UserInfo;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.entities.Application;
import org.apache.usergrid.security.AuthPrincipalInfo;
import org.apache.usergrid.security.AuthPrincipalType;
import org.apache.usergrid.security.tokens.cassandra.TokenServiceImpl;
import org.apache.usergrid.security.tokens.exceptions.ExpiredTokenException;
import org.apache.usergrid.security.tokens.exceptions.InvalidTokenException;
import org.apache.usergrid.utils.UUIDUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;



public class TokenServiceIT {

    static Logger log = LoggerFactory.getLogger( TokenServiceIT.class );

    // app-level data generated only once per test
    private UserInfo adminUser;

    @Rule
    public ClearShiroSubject clearShiroSubject = new ClearShiroSubject();
    @ClassRule
    public static ServiceITSetup setup = new ServiceITSetupImpl(  );


    @Rule
    public NewOrgAppAdminRule newOrgAppAdminRule = new NewOrgAppAdminRule( setup );

    @Before
    public void setup() throws Exception {
        log.info( "in setup" );
        adminUser = newOrgAppAdminRule.getAdminInfo();
    }


    @SuppressWarnings("unchecked")
    @Test
    public void testEmailConfirmToken() throws Exception {

        Map<String, Object> data = new HashMap<String, Object>() {
            {
                put( "email", adminUser.getEmail());
                put( "username", adminUser.getUsername());
            }
        };


        String tokenStr = setup.getTokenSvc().createToken( TokenCategory.EMAIL, "email_confirm", null, data, 0 );

        log.info( "token: " + tokenStr );

        TokenInfo tokenInfo = setup.getTokenSvc().getTokenInfo( tokenStr );

        long last_access = tokenInfo.getAccessed();

        assertEquals( "email_confirm", tokenInfo.getType() );
        assertEquals( adminUser.getEmail(), tokenInfo.getState().get( "email" ) );
        assertEquals( adminUser.getUsername(), tokenInfo.getState().get( "username" ) );

        tokenInfo = setup.getTokenSvc().getTokenInfo( tokenStr );

        assertTrue( last_access < tokenInfo.getAccessed() );
    }


    @Test
    public void testAdminPrincipalToken() throws Exception {

        AuthPrincipalInfo adminPrincipal =
                new AuthPrincipalInfo( AuthPrincipalType.ADMIN_USER, adminUser.getUuid(), UUIDUtils.newTimeUUID() );

        String tokenStr = setup.getTokenSvc().createToken( TokenCategory.ACCESS, null, adminPrincipal, null, 0 );

        log.info( "token: " + tokenStr );

        TokenInfo tokenInfo = setup.getTokenSvc().getTokenInfo( tokenStr );

        long last_access = tokenInfo.getAccessed();

        assertEquals( "access", tokenInfo.getType() );
        assertEquals( adminUser.getUuid(), tokenInfo.getPrincipal().getUuid() );

        tokenInfo = setup.getTokenSvc().getTokenInfo( tokenStr );

        assertTrue( last_access < tokenInfo.getAccessed() );
    }


    @Test
    public void adminPrincipalTokenRevoke() throws Exception {


        AuthPrincipalInfo adminPrincipal =
                new AuthPrincipalInfo( AuthPrincipalType.ADMIN_USER, UUIDUtils.newTimeUUID(), UUIDUtils.newTimeUUID() );

        String firstToken = setup.getTokenSvc().createToken( TokenCategory.ACCESS, null, adminPrincipal, null, 0 );
        String secondToken = setup.getTokenSvc().createToken( TokenCategory.ACCESS, null, adminPrincipal, null, 0 );

        assertNotNull( firstToken );
        assertNotNull( secondToken );

        TokenInfo firstInfo = setup.getTokenSvc().getTokenInfo( firstToken );
        assertNotNull( firstInfo );

        TokenInfo secondInfo = setup.getTokenSvc().getTokenInfo( secondToken );
        assertNotNull( secondInfo );

        setup.getTokenSvc().removeTokens( adminPrincipal );

        // tokens shouldn't be there anymore
        boolean invalidTokenException = false;

        try {
            setup.getTokenSvc().getTokenInfo( firstToken );
        }
        catch ( InvalidTokenException ite ) {
            invalidTokenException = true;
        }

        assertTrue( invalidTokenException );

        invalidTokenException = false;

        try {
            setup.getTokenSvc().getTokenInfo( secondToken );
        }
        catch ( InvalidTokenException ite ) {
            invalidTokenException = true;
        }

        assertTrue( invalidTokenException );
    }


    @Test
    public void userPrincipalTokenRevoke() throws Exception {
        AuthPrincipalInfo adminPrincipal =
                new AuthPrincipalInfo( AuthPrincipalType.APPLICATION_USER, UUIDUtils.newTimeUUID(),
                        UUIDUtils.newTimeUUID() );

        String firstToken = setup.getTokenSvc().createToken( TokenCategory.ACCESS, null, adminPrincipal, null, 0 );
        String secondToken = setup.getTokenSvc().createToken( TokenCategory.ACCESS, null, adminPrincipal, null, 0 );

        assertNotNull( firstToken );
        assertNotNull( secondToken );

        TokenInfo firstInfo = setup.getTokenSvc().getTokenInfo( firstToken );
        assertNotNull( firstInfo );

        TokenInfo secondInfo = setup.getTokenSvc().getTokenInfo( secondToken );
        assertNotNull( secondInfo );

        setup.getTokenSvc().removeTokens( adminPrincipal );

        // tokens shouldn't be there anymore
        boolean invalidTokenException = false;

        try {
            setup.getTokenSvc().getTokenInfo( firstToken );
        }
        catch ( InvalidTokenException ite ) {
            invalidTokenException = true;
        }

        assertTrue( invalidTokenException );

        invalidTokenException = false;

        try {
            setup.getTokenSvc().getTokenInfo( secondToken );
        }
        catch ( InvalidTokenException ite ) {
            invalidTokenException = true;
        }

        assertTrue( invalidTokenException );
    }


    @Test
    public void tokenDurationExpiration() throws Exception {
        AuthPrincipalInfo adminPrincipal =
                new AuthPrincipalInfo( AuthPrincipalType.APPLICATION_USER, UUIDUtils.newTimeUUID(),
                        UUIDUtils.newTimeUUID() );

        // 2 second token
        long expirationTime = 2000;

        String token =
                setup.getTokenSvc().createToken( TokenCategory.ACCESS, null, adminPrincipal, null, expirationTime );

        long start = System.currentTimeMillis();

        assertNotNull( token );

        TokenInfo tokenInfo = setup.getTokenSvc().getTokenInfo( token );
        assertNotNull( tokenInfo );
        assertEquals( expirationTime, tokenInfo.getDuration() );
        long maxTokenAge = setup.getTokenSvc().getMaxTokenAge( token );
        assertEquals( expirationTime, maxTokenAge );


        tokenInfo = setup.getTokenSvc().getTokenInfo( token );
        assertNotNull( tokenInfo );
        assertEquals( expirationTime, tokenInfo.getDuration() );

        maxTokenAge = setup.getTokenSvc().getMaxTokenAge( token );
        assertEquals( expirationTime, maxTokenAge );

        /**
         * Sleep at least expirationTime millis to allow token to expire
         */
        Thread.sleep( expirationTime - ( System.currentTimeMillis() - start ) + 1000 );

        boolean invalidTokenException = false;

        try {
            setup.getTokenSvc().getTokenInfo( token );
        }
        catch ( ExpiredTokenException ite ) {
            invalidTokenException = true;
        }

        assertTrue( invalidTokenException );
    }


    @Test
    public void tokenDefaults() throws Exception {
        AuthPrincipalInfo adminPrincipal =
                new AuthPrincipalInfo( AuthPrincipalType.APPLICATION_USER, UUIDUtils.newTimeUUID(),
                        UUIDUtils.newTimeUUID() );

        long maxAge = ( ( TokenServiceImpl ) setup.getTokenSvc() ).getMaxPersistenceTokenAge();

        String token = setup.getTokenSvc().createToken( TokenCategory.ACCESS, null, adminPrincipal, null, 0 );

        assertNotNull( token );

        TokenInfo tokenInfo = setup.getTokenSvc().getTokenInfo( token );
        assertNotNull( tokenInfo );
        assertEquals( maxAge, tokenInfo.getDuration() );
    }


    @Test(expected = IllegalArgumentException.class)
    public void invalidDurationValue() throws Exception {

        long maxAge = ( ( TokenServiceImpl ) setup.getTokenSvc() ).getMaxPersistenceTokenAge();

        AuthPrincipalInfo adminPrincipal =
                new AuthPrincipalInfo( AuthPrincipalType.APPLICATION_USER, UUIDUtils.newTimeUUID(),
                        UUIDUtils.newTimeUUID() );

        setup.getTokenSvc().createToken( TokenCategory.ACCESS, null, adminPrincipal, null, maxAge + 1 );
    }


    @Test
    public void appDefaultExpiration() throws Exception {

        ApplicationInfo appInfo = newOrgAppAdminRule.getApplicationInfo();
        EntityManager em = setup.getEmf().getEntityManager( appInfo.getId() );
        Application app = em.getApplication();
        AuthPrincipalInfo userPrincipal =
                new AuthPrincipalInfo( AuthPrincipalType.APPLICATION_USER, UUIDUtils.newTimeUUID(), app.getUuid() );
        String token = setup.getTokenSvc().createToken( TokenCategory.ACCESS, null, userPrincipal, null, 0 );
        assertNotNull( token );
        TokenInfo tokenInfo = setup.getTokenSvc().getTokenInfo( token );
        assertNotNull( tokenInfo );
        assertEquals( TokenServiceImpl.LONG_TOKEN_AGE, tokenInfo.getDuration() );
    }


    @Test
    public void appExpiration() throws Exception {
        ApplicationInfo appInfo = newOrgAppAdminRule.getApplicationInfo();

        EntityManager em = setup.getEmf().getEntityManager( appInfo.getId() );

        Application app = em.getApplication();

        long appTokenAge = 1000;

        app.setAccesstokenttl( appTokenAge );

        em.updateApplication( app );

        AuthPrincipalInfo userPrincipal =
                new AuthPrincipalInfo( AuthPrincipalType.APPLICATION_USER, UUIDUtils.newTimeUUID(), app.getUuid() );

        String token = setup.getTokenSvc().createToken( TokenCategory.ACCESS, null, userPrincipal, null, 0 );

        long start = System.currentTimeMillis();

        assertNotNull( token );

        TokenInfo tokenInfo = setup.getTokenSvc().getTokenInfo( token );
        assertNotNull( tokenInfo );
        assertEquals( appTokenAge, tokenInfo.getDuration() );

        /**
         * Sleep at least expirationTime millis to allow token to expire
         */
        Thread.sleep( appTokenAge - ( System.currentTimeMillis() - start ) + 1000 );

        boolean invalidTokenException = false;

        try {
            setup.getTokenSvc().getTokenInfo( token );
        }
        catch ( ExpiredTokenException ite ) {
            invalidTokenException = true;
        }

        assertTrue( invalidTokenException );
    }


    @Test
    public void tokenDeletion() throws Exception {
        AuthPrincipalInfo adminPrincipal =
                new AuthPrincipalInfo( AuthPrincipalType.APPLICATION_USER, UUIDUtils.newTimeUUID(),
                        UUIDUtils.newTimeUUID() );

        String realToken = setup.getTokenSvc().createToken( TokenCategory.ACCESS, null, adminPrincipal, null, 0 );

        assertNotNull( realToken );

        TokenInfo tokenInfo = setup.getTokenSvc().getTokenInfo( realToken );
        assertNotNull( tokenInfo );

        setup.getTokenSvc().revokeToken( realToken );

        boolean invalidTokenException = false;

        try {
            setup.getTokenSvc().getTokenInfo( realToken );
        }
        catch ( InvalidTokenException ite ) {
            invalidTokenException = true;
        }

        assertTrue( invalidTokenException );

        String fakeToken = "notarealtoken";

        setup.getTokenSvc().revokeToken( fakeToken );
    }


    @Test
    public void appExpirationInfinite() throws Exception {
        ApplicationInfo appInfo = newOrgAppAdminRule.getApplicationInfo();

        EntityManager em = setup.getEmf().getEntityManager( appInfo.getId() );

        Application app = em.getApplication();

        long appTokenAge = 0;

        app.setAccesstokenttl( appTokenAge );

        em.updateApplication( app );

        AuthPrincipalInfo userPrincipal =
                new AuthPrincipalInfo( AuthPrincipalType.APPLICATION_USER, UUIDUtils.newTimeUUID(), app.getUuid() );

        String token = setup.getTokenSvc().createToken( TokenCategory.ACCESS, null, userPrincipal, null, 0 );


        assertNotNull( token );

        TokenInfo tokenInfo = setup.getTokenSvc().getTokenInfo( token );
        assertNotNull( tokenInfo );
        assertEquals( Long.MAX_VALUE, tokenInfo.getDuration() );

        boolean invalidTokenException = false;

        try {
            setup.getTokenSvc().getTokenInfo( token );
        }
        catch ( InvalidTokenException ite ) {
            invalidTokenException = true;
        }

        assertFalse( invalidTokenException );

        setup.getTokenSvc().revokeToken( token );

        invalidTokenException = false;

        try {
            setup.getTokenSvc().getTokenInfo( token );
        }
        catch ( InvalidTokenException ite ) {
            invalidTokenException = true;
        }

        assertTrue( invalidTokenException );
    }

    @Test
    public void testImportToken() throws Exception {

        // create admin user token and make sure it is working

        AuthPrincipalInfo adminPrincipal = new AuthPrincipalInfo(
                AuthPrincipalType.ADMIN_USER, adminUser.getUuid(), UUIDUtils.newTimeUUID() );

        String tokenStr = setup.getTokenSvc().createToken(
                TokenCategory.ACCESS, null, adminPrincipal, null, 0 );

        log.info("token: " + tokenStr);

        // revoke token and check to make sure it is no longer valid

        setup.getTokenSvc().revokeToken( tokenStr );

        boolean invalidTokenException = false;
        try {
            setup.getTokenSvc().getTokenInfo( tokenStr );
        }
        catch ( InvalidTokenException ite ) {
            invalidTokenException = true;
        }
        assertTrue(invalidTokenException);

        // import same token and make sure it works again

        setup.getTokenSvc().importToken( tokenStr, TokenCategory.ACCESS, null, adminPrincipal, null, 0 );

        TokenInfo tokenInfo = setup.getTokenSvc().getTokenInfo( tokenStr );

        long last_access = tokenInfo.getAccessed();

        assertEquals( "access", tokenInfo.getType() );
        assertEquals( adminUser.getUuid(), tokenInfo.getPrincipal().getUuid() );

        tokenInfo = setup.getTokenSvc().getTokenInfo( tokenStr );

        assertTrue(last_access < tokenInfo.getAccessed());
    }
}
