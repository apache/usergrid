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
package org.apache.usergrid.management.cassandra;


import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.NewOrgAppAdminRule;
import org.apache.usergrid.ServiceITSetup;
import org.apache.usergrid.ServiceITSetupImpl;
import org.apache.usergrid.cassandra.SpringResource;
import org.apache.usergrid.cassandra.ClearShiroSubject;

import org.apache.usergrid.count.SimpleBatcher;
import org.apache.usergrid.management.OrganizationInfo;
import org.apache.usergrid.management.UserInfo;
import org.apache.usergrid.persistence.CredentialsInfo;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.entities.User;
import org.apache.usergrid.persistence.index.impl.ElasticSearchResource;
import org.apache.usergrid.security.AuthPrincipalType;
import org.apache.usergrid.security.crypto.command.Md5HashCommand;
import org.apache.usergrid.security.crypto.command.Sha1HashCommand;
import org.apache.usergrid.security.tokens.TokenCategory;
import org.apache.usergrid.security.tokens.exceptions.InvalidTokenException;
import org.apache.usergrid.utils.JsonUtils;
import org.apache.usergrid.utils.UUIDUtils;

import static org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString;
import static org.apache.usergrid.TestHelper.newUUIDString;
import static org.apache.usergrid.TestHelper.uniqueApp;
import static org.apache.usergrid.TestHelper.uniqueEmail;
import static org.apache.usergrid.TestHelper.uniqueOrg;
import static org.apache.usergrid.TestHelper.uniqueUsername;
import static org.apache.usergrid.persistence.Schema.DICTIONARY_CREDENTIALS;
import static org.apache.usergrid.persistence.Schema.PROPERTY_APPLICATION_ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author zznate
 */

public class ManagementServiceIT {
    private static final Logger LOG = LoggerFactory.getLogger( ManagementServiceIT.class );


     @ClassRule
    public static final ServiceITSetup setup = new ServiceITSetupImpl();


    @Rule
    public ClearShiroSubject clearShiroSubject = new ClearShiroSubject();

    @Rule
    public NewOrgAppAdminRule orgAppAdminRule = new NewOrgAppAdminRule( setup );


    // app-level data generated only once
    private UserInfo adminUser;
    private UUID applicationId;


    @Before
    public void setup() throws Exception {
        LOG.info( "in setup" );


        adminUser = orgAppAdminRule.getAdminInfo();
        applicationId = orgAppAdminRule.getApplicationInfo().getId();

        setup.getEntityIndex().refresh();
    }



    @Test
    public void testGetTokenForPrincipalAdmin() throws Exception {
        String token = ( ( ManagementServiceImpl ) setup.getMgmtSvc() )
                .getTokenForPrincipal( TokenCategory.ACCESS, null, setup.getEmf().getManagementAppId(),
                        AuthPrincipalType.ADMIN_USER, adminUser.getUuid(), 0 );
        // ^ same as:
        // managementService.getAccessTokenForAdminUser(user.getUuid());
        assertNotNull( token );
        token = ( ( ManagementServiceImpl ) setup.getMgmtSvc() )
                .getTokenForPrincipal( TokenCategory.ACCESS, null, setup.getEmf().getManagementAppId(),
                        AuthPrincipalType.APPLICATION_USER, adminUser.getUuid(), 0 );
        // This works because ManagementService#getSecret takes the same code
        // path
        // on an OR for APP._USER as for ADMIN_USER
        // is ok technically as ADMIN_USER is a APP_USER to the admin app, but
        // should still
        // be stricter checking
        assertNotNull( token );
        // managementService.getTokenForPrincipal(appUuid, authPrincipal, pUuid,
        // salt, true);
    }


    @Test
    public void testGetTokenForPrincipalUser() throws Exception {
        // create a user
        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put( "username", "edanuff" );
        properties.put( "email", "ed@anuff.com" );

        Entity user = setup.getEmf().getEntityManager( applicationId ).create( "user", properties );

        assertNotNull( user );
        String token = ( ( ManagementServiceImpl ) setup.getMgmtSvc() )
                .getTokenForPrincipal( TokenCategory.ACCESS, null, setup.getEmf().getManagementAppId(),
                        AuthPrincipalType.APPLICATION_USER, user.getUuid(), 0 );
        assertNotNull( token );
    }


    @Test
    public void testCountAdminUserAction() throws Exception {
        SimpleBatcher batcher = SpringResource.getInstance().getBean( SimpleBatcher.class );

        batcher.setBlockingSubmit( true );
        batcher.setBatchSize( 1 );

        EntityManager em = setup.getEmf().getEntityManager( setup.getEmf().getManagementAppId() );

        Map<String, Long> counts = em.getApplicationCounters();
        LOG.info( JsonUtils.mapToJsonString( counts ) );
        LOG.info( JsonUtils.mapToJsonString( em.getCounterNames() ) );

        final Long existingCounts = counts.get( "admin_logins" );

        final long startCount = existingCounts == null ? 0 : existingCounts;


        setup.getMgmtSvc().countAdminUserAction( adminUser, "login" );


        counts = em.getApplicationCounters();
        LOG.info( JsonUtils.mapToJsonString( counts ) );
        LOG.info( JsonUtils.mapToJsonString( em.getCounterNames() ) );
        assertNotNull( counts.get( "admin_logins" ) );

        final long newCount = counts.get( "admin_logins" );

        assertEquals( 1l, newCount - startCount );
    }


    @Test
    public void deactivateUser() throws Exception {

        UUID uuid = UUIDUtils.newTimeUUID();
        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put( "username", "test" + uuid );
        properties.put( "email", String.format( "test%s@anuff.com", uuid ) );

        EntityManager em = setup.getEmf().getEntityManager( applicationId );

        Entity entity = em.create( "user", properties );

        assertNotNull( entity );

        User user = em.get( entity.getUuid(), User.class );

        assertFalse( user.activated() );
        assertNull( user.getDeactivated() );

        setup.getMgmtSvc().activateAppUser( applicationId, user.getUuid() );

        setup.getEntityIndex().refresh();

        user = em.get( entity.getUuid(), User.class );

        assertTrue( user.activated() );
        assertNull( user.getDeactivated() );

        // get a couple of tokens. These shouldn't work after we deactive the user
        String token1 = setup.getMgmtSvc().getAccessTokenForAppUser( applicationId, user.getUuid(), 0 );
        String token2 = setup.getMgmtSvc().getAccessTokenForAppUser( applicationId, user.getUuid(), 0 );

        assertNotNull( setup.getTokenSvc().getTokenInfo( token1 ) );
        assertNotNull( setup.getTokenSvc().getTokenInfo( token2 ) );

        long startTime = System.currentTimeMillis();

        setup.getMgmtSvc().deactivateUser( applicationId, user.getUuid() );

        long endTime = System.currentTimeMillis();

        user = em.get( entity.getUuid(), User.class );

        assertFalse( user.activated() );
        assertNotNull( user.getDeactivated() );

        assertTrue( startTime <= user.getDeactivated() && user.getDeactivated() <= endTime );

        boolean invalidTokenExcpetion = false;

        try {
            setup.getTokenSvc().getTokenInfo( token1 );
        }
        catch ( InvalidTokenException ite ) {
            invalidTokenExcpetion = true;
        }

        assertTrue( invalidTokenExcpetion );

        invalidTokenExcpetion = false;

        try {
            setup.getTokenSvc().getTokenInfo( token2 );
        }
        catch ( InvalidTokenException ite ) {
            invalidTokenExcpetion = true;
        }

        assertTrue( invalidTokenExcpetion );
    }


    @Test
    public void disableAdminUser() throws Exception {

        UUID uuid = UUIDUtils.newTimeUUID();
        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put( "username", "test" + uuid );
        properties.put( "email", String.format( "test%s@anuff.com", uuid ) );

        EntityManager em = setup.getEmf().getEntityManager( setup.getEmf().getManagementAppId() );

        Entity entity = em.create( "user", properties );

        assertNotNull( entity );

        User user = em.get( entity.getUuid(), User.class );

        assertFalse( user.activated() );
        assertNull( user.getDeactivated() );

        setup.getMgmtSvc().activateAdminUser( user.getUuid() );

        user = em.get( entity.getUuid(), User.class );

        assertTrue( user.activated() );
        assertNull( user.getDeactivated() );

        // get a couple of tokens. These shouldn't work after we deactive the user
        String token1 = setup.getMgmtSvc().getAccessTokenForAdminUser( user.getUuid(), 0 );
        String token2 = setup.getMgmtSvc().getAccessTokenForAdminUser( user.getUuid(), 0 );

        assertNotNull( setup.getTokenSvc().getTokenInfo( token1 ) );
        assertNotNull( setup.getTokenSvc().getTokenInfo( token2 ) );

        setup.getMgmtSvc().disableAdminUser( user.getUuid() );

        user = em.get( entity.getUuid(), User.class );

        assertTrue( user.disabled() );

        boolean invalidTokenExcpetion = false;

        try {
            setup.getTokenSvc().getTokenInfo( token1 );
        }
        catch ( InvalidTokenException ite ) {
            invalidTokenExcpetion = true;
        }

        assertTrue( invalidTokenExcpetion );

        invalidTokenExcpetion = false;

        try {
            setup.getTokenSvc().getTokenInfo( token2 );
        }
        catch ( InvalidTokenException ite ) {
            invalidTokenExcpetion = true;
        }

        assertTrue( invalidTokenExcpetion );
    }


    @Test
    public void userTokensRevoke() throws Exception {
        UUID userId = UUIDUtils.newTimeUUID();

        String token1 = setup.getMgmtSvc().getAccessTokenForAppUser( applicationId, userId, 0 );
        String token2 = setup.getMgmtSvc().getAccessTokenForAppUser( applicationId, userId, 0 );

        assertNotNull( setup.getTokenSvc().getTokenInfo( token1 ) );
        assertNotNull( setup.getTokenSvc().getTokenInfo( token2 ) );

        setup.getMgmtSvc().revokeAccessTokensForAppUser( applicationId, userId );

        boolean invalidTokenExcpetion = false;

        try {
            setup.getTokenSvc().getTokenInfo( token1 );
        }
        catch ( InvalidTokenException ite ) {
            invalidTokenExcpetion = true;
        }

        assertTrue( invalidTokenExcpetion );

        invalidTokenExcpetion = false;

        try {
            setup.getTokenSvc().getTokenInfo( token2 );
        }
        catch ( InvalidTokenException ite ) {
            invalidTokenExcpetion = true;
        }

        assertTrue( invalidTokenExcpetion );
    }


    @Test
    public void userTokenRevoke() throws Exception {
        EntityManager em = setup.getEmf().getEntityManager( applicationId );

        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put( "username", "realbeast" );
        properties.put( "email", "sungju@softwaregeeks.org" );

        Entity user = em.create( "user", properties );
        assertNotNull( user );

        UUID userId = user.getUuid();

        String token1 = setup.getMgmtSvc().getAccessTokenForAppUser( applicationId, userId, 0 );
        String token2 = setup.getMgmtSvc().getAccessTokenForAppUser( applicationId, userId, 0 );

        assertNotNull( setup.getTokenSvc().getTokenInfo( token1 ) );
        assertNotNull( setup.getTokenSvc().getTokenInfo( token2 ) );

        setup.getMgmtSvc().revokeAccessTokenForAppUser( token1 );

        boolean invalidToken1Excpetion = false;

        try {
            setup.getTokenSvc().getTokenInfo( token1 );
        }
        catch ( InvalidTokenException ite ) {
            invalidToken1Excpetion = true;
        }

        assertTrue( invalidToken1Excpetion );

        boolean invalidToken2Excpetion = true;

        try {
            setup.getTokenSvc().getTokenInfo( token2 );
        }
        catch ( InvalidTokenException ite ) {
            invalidToken2Excpetion = false;
        }

        assertTrue( invalidToken2Excpetion );
    }


    @Test
    public void adminTokensRevoke() throws Exception {
        UUID userId = UUIDUtils.newTimeUUID();

        String token1 = setup.getMgmtSvc().getAccessTokenForAdminUser( userId, 0 );
        String token2 = setup.getMgmtSvc().getAccessTokenForAdminUser( userId, 0 );

        assertNotNull( setup.getTokenSvc().getTokenInfo( token1 ) );
        assertNotNull( setup.getTokenSvc().getTokenInfo( token2 ) );

        setup.getMgmtSvc().revokeAccessTokensForAdminUser( userId );

        boolean invalidTokenException = false;

        try {
            setup.getTokenSvc().getTokenInfo( token1 );
        }
        catch ( InvalidTokenException ite ) {
            invalidTokenException = true;
        }

        assertTrue( invalidTokenException );

        invalidTokenException = false;

        try {
            setup.getTokenSvc().getTokenInfo( token2 );
        }
        catch ( InvalidTokenException ite ) {
            invalidTokenException = true;
        }

        assertTrue( invalidTokenException );
    }


    @Test
    public void adminTokenRevoke() throws Exception {
        UUID userId = adminUser.getUuid();

        String token1 = setup.getMgmtSvc().getAccessTokenForAdminUser( userId, 0 );
        String token2 = setup.getMgmtSvc().getAccessTokenForAdminUser( userId, 0 );

        assertNotNull( setup.getTokenSvc().getTokenInfo( token1 ) );
        assertNotNull( setup.getTokenSvc().getTokenInfo( token2 ) );

        setup.getMgmtSvc().revokeAccessTokenForAdminUser( userId, token1 );

        boolean invalidToken1Excpetion = false;

        try {
            setup.getTokenSvc().getTokenInfo( token1 );
        }
        catch ( InvalidTokenException ite ) {
            invalidToken1Excpetion = true;
        }

        assertTrue( invalidToken1Excpetion );

        boolean invalidToken2Excpetion = true;

        try {
            setup.getTokenSvc().getTokenInfo( token2 );
        }
        catch ( InvalidTokenException ite ) {
            invalidToken2Excpetion = false;
        }

        assertTrue( invalidToken2Excpetion );
    }


    @Ignore("Why is this ignored?")
    public void superUserGetOrganizationsPage() throws Exception {
        int beforeSize = setup.getMgmtSvc().getOrganizations().size() - 1;
        // create 15 orgs
        for ( int x = 0; x < 15; x++ ) {
            setup.getMgmtSvc().createOrganization( "super-user-org-" + x, adminUser, true );
        }
        // should be 17 total
        assertEquals( 16 + beforeSize, setup.getMgmtSvc().getOrganizations().size() );
        List<OrganizationInfo> orgs = setup.getMgmtSvc().getOrganizations( null, 10 );
        assertEquals( 10, orgs.size() );
        UUID val = orgs.get( 9 ).getUuid();
        orgs = setup.getMgmtSvc().getOrganizations( val, 10 );
        assertEquals( 7 + beforeSize, orgs.size() );
        assertEquals( val, orgs.get( 0 ).getUuid() );
    }


    @Test
    public void authenticateAdmin() throws Exception {

        String username = uniqueUsername();
        String password = "test";

        UserInfo adminUser = setup.getMgmtSvc()
                                  .createAdminUser( username, "Todd Nine",uniqueEmail(), password,
                                          false, false );

        EntityManager em = setup.getEmf().getEntityManager( setup.getSmf().getManagementAppId() );
        setup.getEntityIndex().refresh();

        UserInfo authedUser = setup.getMgmtSvc().verifyAdminUserPasswordCredentials( username, password );

        assertEquals( adminUser.getUuid(), authedUser.getUuid() );

        authedUser = setup.getMgmtSvc().verifyAdminUserPasswordCredentials( adminUser.getEmail(), password );

        assertEquals( adminUser.getUuid(), authedUser.getUuid() );

        authedUser = setup.getMgmtSvc().verifyAdminUserPasswordCredentials( adminUser.getUuid().toString(), password );

        assertEquals( adminUser.getUuid(), authedUser.getUuid() );
    }


    /**
     * Test we can change the password if it's hashed with sha1
     */
    @Test
    public void testAdminPasswordChangeShaType() throws Exception {
        String username = uniqueUsername();
        String password = "test";


        User user = new User();
        user.setActivated( true );
        user.setUsername( username );

        EntityManager em = setup.getEmf().getEntityManager( setup.getEmf().getManagementAppId() );

        User storedUser = em.create( user );


        UUID userId = storedUser.getUuid();

        //set the password in the sha1 format
        CredentialsInfo info = new CredentialsInfo();
        info.setRecoverable( false );
        info.setEncrypted( true );


        Sha1HashCommand command = new Sha1HashCommand();
        byte[] hashed = command.hash( password.getBytes( "UTF-8" ), info, userId, setup.getEmf().getManagementAppId() );

        info.setSecret( encodeBase64URLSafeString( hashed ) );
        info.setCipher( command.getName() );


        em.addToDictionary( storedUser, DICTIONARY_CREDENTIALS, "password", info );

        setup.getEntityIndex().refresh();


        //verify authorization works
        User authedUser =
                setup.getMgmtSvc().verifyAppUserPasswordCredentials( setup.getEmf().getManagementAppId(), username, password );

        assertEquals( userId, authedUser.getUuid() );

        //test we can change the password
        String newPassword = "test2";

        setup.getMgmtSvc().setAppUserPassword( setup.getEmf().getManagementAppId(), userId, password, newPassword );

        //verify authorization works
        authedUser =
                setup.getMgmtSvc().verifyAppUserPasswordCredentials( setup.getEmf().getManagementAppId(), username, newPassword );

        assertEquals( userId, authedUser.getUuid() );
    }


    /**
     * Test we can change the password if it's hashed with md5 then sha1
     */
    @Test
    public void testAdminPasswordChangeMd5ShaType() throws Exception {
        String username = uniqueUsername();
        String password = "test";


        User user = new User();
        user.setActivated( true );
        user.setUsername( username );

        EntityManager em = setup.getEmf().getEntityManager( setup.getEmf().getManagementAppId() );

        User storedUser = em.create( user );
        setup.getEntityIndex().refresh();


        UUID userId = storedUser.getUuid();

        //set the password in the sha1 format

        //set the password in the sha1 format
        CredentialsInfo info = new CredentialsInfo();
        info.setRecoverable( false );
        info.setEncrypted( true );


        Md5HashCommand md5 = new Md5HashCommand();

        Sha1HashCommand sha1 = new Sha1HashCommand();

        byte[] hashed = md5.hash( password.getBytes( "UTF-8" ), info, userId, setup.getEmf().getManagementAppId() );
        hashed = sha1.hash( hashed, info, userId, setup.getEmf().getManagementAppId() );

        info.setSecret( encodeBase64URLSafeString( hashed ) );
        //set the final cipher to sha1
        info.setCipher( sha1.getName() );
        //set the next hash type to md5
        info.setHashType( md5.getName() );


        em.addToDictionary( storedUser, DICTIONARY_CREDENTIALS, "password", info );


        //verify authorization works
        User authedUser =
                setup.getMgmtSvc().verifyAppUserPasswordCredentials( setup.getEmf().getManagementAppId(), username, password );

        assertEquals( userId, authedUser.getUuid() );

        //test we can change the password
        String newPassword = "test2";

        setup.getMgmtSvc().setAppUserPassword( setup.getEmf().getManagementAppId(), userId, password, newPassword );

        //verify authorization works
        authedUser =
                setup.getMgmtSvc().verifyAppUserPasswordCredentials( setup.getEmf().getManagementAppId(), username, newPassword );

        assertEquals( userId, authedUser.getUuid() );
    }


    @Test
    public void authenticateUser() throws Exception {

        String username = uniqueUsername();
        String password = "test";
        String orgName = uniqueOrg();
        String appName = uniqueApp();

        Entity appInfo = setup.getEmf().createApplicationV2( orgName, appName );
        UUID appId = UUIDUtils.tryExtractUUID(
            appInfo.getProperty(PROPERTY_APPLICATION_ID).toString());

        User user = new User();
        user.setActivated( true );
        user.setUsername( username );

        EntityManager em = setup.getEmf().getEntityManager( appId );

        User storedUser = em.create( user );

        setup.getEntityIndex().refresh();

        UUID userId = storedUser.getUuid();

        //set the password
        setup.getMgmtSvc().setAppUserPassword( appId, userId, password );

        //verify authorization works
        User authedUser = setup.getMgmtSvc().verifyAppUserPasswordCredentials( appId, username, password );

        assertEquals( userId, authedUser.getUuid() );

        //test we can change the password
        String newPassword = "test2";

        setup.getMgmtSvc().setAppUserPassword( appId, userId, password, newPassword );

        setup.getEntityIndex().refresh();

        //verify authorization works
        authedUser = setup.getMgmtSvc().verifyAppUserPasswordCredentials( appId, username, newPassword );
    }


    /**
     * Test we can change the password if it's hashed with sha1
     */
    @Test
    public void testAppUserPasswordChangeShaType() throws Exception {
        String username = "tnine"+newUUIDString();
        String password = "test";
        String orgName = "testAppUserPasswordChangeShaType"+newUUIDString();
        String appName = "testAppUserPasswordChangeShaType"+newUUIDString();

        Entity appInfo = setup.getEmf().createApplicationV2(orgName, appName);
        UUID appId = UUIDUtils.tryExtractUUID(
            appInfo.getProperty(PROPERTY_APPLICATION_ID).toString());

        User user = new User();
        user.setActivated( true );
        user.setUsername( username );

        EntityManager em = setup.getEmf().getEntityManager( appId );

        User storedUser = em.create( user );

        setup.getEntityIndex().refresh();

        UUID userId = storedUser.getUuid();

        //set the password in the sha1 format
        CredentialsInfo info = new CredentialsInfo();
        info.setRecoverable( false );
        info.setEncrypted( true );


        Sha1HashCommand command = new Sha1HashCommand();
        byte[] hashed = command.hash( password.getBytes( "UTF-8" ), info, userId, appId );

        info.setSecret( encodeBase64URLSafeString( hashed ) );
        info.setCipher( command.getName() );


        em.addToDictionary( storedUser, DICTIONARY_CREDENTIALS, "password", info );


        //verify authorization works
        User authedUser = setup.getMgmtSvc().verifyAppUserPasswordCredentials( appId, username, password );

        assertEquals( userId, authedUser.getUuid() );

        //test we can change the password
        String newPassword = "test2";

        setup.getMgmtSvc().setAppUserPassword( appId, userId, password, newPassword );

        setup.getEntityIndex().refresh();

        //verify authorization works
        authedUser = setup.getMgmtSvc().verifyAppUserPasswordCredentials( appId, username, newPassword );

        assertEquals( userId, authedUser.getUuid() );
    }


    /**
     * Test we can change the password if it's hashed with md5 then sha1
     */
    @Test
    public void testAppUserPasswordChangeMd5ShaType() throws Exception {
        String username = uniqueUsername();
        String password = "test";
        String orgName = uniqueOrg();
        String appName = uniqueApp();

        Entity appInfo = setup.getEmf().createApplicationV2( orgName, appName );
        UUID appId = UUIDUtils.tryExtractUUID(
            appInfo.getProperty(PROPERTY_APPLICATION_ID).toString());

        User user = new User();
        user.setActivated( true );
        user.setUsername( username );

        EntityManager em = setup.getEmf().getEntityManager( appId );

        User storedUser = em.create( user );

        setup.getEntityIndex().refresh();

        UUID userId = storedUser.getUuid();

        //set the password in the sha1 format
        CredentialsInfo info = new CredentialsInfo();
        info.setRecoverable( false );
        info.setEncrypted( true );


        Md5HashCommand md5 = new Md5HashCommand();

        Sha1HashCommand sha1 = new Sha1HashCommand();

        byte[] hashed = md5.hash( password.getBytes( "UTF-8" ), info, userId, appId );
        hashed = sha1.hash( hashed, info, userId, appId );

        info.setSecret( encodeBase64URLSafeString( hashed ) );
        //set the final cipher to sha1
        info.setCipher( sha1.getName() );
        //set the next hash type to md5
        info.setHashType( md5.getName() );


        em.addToDictionary( storedUser, DICTIONARY_CREDENTIALS, "password", info );


        //verify authorization works
        User authedUser = setup.getMgmtSvc().verifyAppUserPasswordCredentials( appId, username, password );

        assertEquals( userId, authedUser.getUuid() );

        //test we can change the password
        String newPassword = "test2";

        setup.getMgmtSvc().setAppUserPassword( appId, userId, password, newPassword );

        setup.getEntityIndex().refresh();

        //verify authorization works
        authedUser = setup.getMgmtSvc().verifyAppUserPasswordCredentials( appId, username, newPassword );

        assertEquals( userId, authedUser.getUuid() );
    }

}
