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


import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.jclouds.ContextBuilder;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.blobstore.domain.Blob;
import org.jclouds.http.config.JavaUrlHttpCommandExecutorServiceModule;
import org.jclouds.logging.log4j.config.Log4JLoggingModule;
import org.jclouds.netty.config.NettyPayloadModule;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.ServiceITSetup;
import org.apache.usergrid.ServiceITSetupImpl;
import org.apache.usergrid.ServiceITSuite;
import org.apache.usergrid.batch.JobExecution;
import org.apache.usergrid.cassandra.CassandraResource;
import org.apache.usergrid.cassandra.ClearShiroSubject;
import org.apache.usergrid.cassandra.Concurrent;
import org.apache.usergrid.count.SimpleBatcher;
import org.apache.usergrid.management.ApplicationInfo;
import org.apache.usergrid.management.OrganizationInfo;
import org.apache.usergrid.management.UserInfo;
import org.apache.usergrid.management.export.ExportJob;
import org.apache.usergrid.management.export.ExportService;
import org.apache.usergrid.management.export.S3Export;
import org.apache.usergrid.management.export.S3ExportImpl;
import org.apache.usergrid.persistence.CredentialsInfo;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.EntityManagerFactory;
import org.apache.usergrid.persistence.entities.Export;
import org.apache.usergrid.persistence.entities.JobData;
import org.apache.usergrid.persistence.entities.User;
import org.apache.usergrid.security.AuthPrincipalType;
import org.apache.usergrid.security.crypto.command.Md5HashCommand;
import org.apache.usergrid.security.crypto.command.Sha1HashCommand;
import org.apache.usergrid.security.tokens.TokenCategory;
import org.apache.usergrid.security.tokens.exceptions.InvalidTokenException;
import org.apache.usergrid.utils.JsonUtils;
import org.apache.usergrid.utils.UUIDUtils;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Module;

import static org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString;
import static org.apache.usergrid.persistence.Schema.DICTIONARY_CREDENTIALS;
import static org.apache.usergrid.persistence.cassandra.CassandraService.MANAGEMENT_APPLICATION_ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author zznate
 */
@Concurrent()
public class ManagementServiceIT {
    private static final Logger LOG = LoggerFactory.getLogger( ManagementServiceIT.class );

    private static CassandraResource cassandraResource = ServiceITSuite.cassandraResource;

    // app-level data generated only once
    private static UserInfo adminUser;
    private static OrganizationInfo organization;
    private static UUID applicationId;

    @Rule
    public ClearShiroSubject clearShiroSubject = new ClearShiroSubject();

    @ClassRule
    public static final ServiceITSetup setup = new ServiceITSetupImpl( cassandraResource );


    @BeforeClass
    public static void setup() throws Exception {
        LOG.info( "in setup" );
        adminUser = setup.getMgmtSvc().createAdminUser( "edanuff", "Ed Anuff", "ed@anuff.com", "test", false, false );
        organization = setup.getMgmtSvc().createOrganization( "ed-organization", adminUser, true );
        applicationId = setup.getMgmtSvc().createApplication( organization.getUuid(), "ed-application" ).getId();
    }


    @Test
    public void testGetTokenForPrincipalAdmin() throws Exception {
        String token = ( ( ManagementServiceImpl ) setup.getMgmtSvc() )
                .getTokenForPrincipal( TokenCategory.ACCESS, null, MANAGEMENT_APPLICATION_ID,
                        AuthPrincipalType.ADMIN_USER, adminUser.getUuid(), 0 );
        // ^ same as:
        // managementService.getAccessTokenForAdminUser(user.getUuid());
        assertNotNull( token );
        token = ( ( ManagementServiceImpl ) setup.getMgmtSvc() )
                .getTokenForPrincipal( TokenCategory.ACCESS, null, MANAGEMENT_APPLICATION_ID,
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
                .getTokenForPrincipal( TokenCategory.ACCESS, null, MANAGEMENT_APPLICATION_ID,
                        AuthPrincipalType.APPLICATION_USER, user.getUuid(), 0 );
        assertNotNull( token );
    }


    @Test
    public void testCountAdminUserAction() throws Exception {
        SimpleBatcher batcher = cassandraResource.getBean( SimpleBatcher.class );

        batcher.setBlockingSubmit( true );
        batcher.setBatchSize( 1 );

        setup.getMgmtSvc().countAdminUserAction( adminUser, "login" );

        EntityManager em = setup.getEmf().getEntityManager( MANAGEMENT_APPLICATION_ID );

        Map<String, Long> counts = em.getApplicationCounters();
        LOG.info( JsonUtils.mapToJsonString( counts ) );
        LOG.info( JsonUtils.mapToJsonString( em.getCounterNames() ) );
        assertNotNull( counts.get( "admin_logins" ) );
        assertEquals( 1, counts.get( "admin_logins" ).intValue() );
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

        EntityManager em = setup.getEmf().getEntityManager( MANAGEMENT_APPLICATION_ID );

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


    @Ignore
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

        String username = "tnine";
        String password = "test";

        UserInfo adminUser = setup.getMgmtSvc()
                                  .createAdminUser( username, "Todd Nine", UUID.randomUUID() + "@apigee.com", password,
                                          false, false );

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
        String username = "testAdminPasswordChangeShaType";
        String password = "test";


        User user = new User();
        user.setActivated( true );
        user.setUsername( username );

        EntityManager em = setup.getEmf().getEntityManager( MANAGEMENT_APPLICATION_ID );

        User storedUser = em.create( user );


        UUID userId = storedUser.getUuid();

        //set the password in the sha1 format
        CredentialsInfo info = new CredentialsInfo();
        info.setRecoverable( false );
        info.setEncrypted( true );


        Sha1HashCommand command = new Sha1HashCommand();
        byte[] hashed = command.hash( password.getBytes( "UTF-8" ), info, userId, MANAGEMENT_APPLICATION_ID );

        info.setSecret( encodeBase64URLSafeString( hashed ) );
        info.setCipher( command.getName() );


        em.addToDictionary( storedUser, DICTIONARY_CREDENTIALS, "password", info );


        //verify authorization works
        User authedUser =
                setup.getMgmtSvc().verifyAppUserPasswordCredentials( MANAGEMENT_APPLICATION_ID, username, password );

        assertEquals( userId, authedUser.getUuid() );

        //test we can change the password
        String newPassword = "test2";

        setup.getMgmtSvc().setAppUserPassword( MANAGEMENT_APPLICATION_ID, userId, password, newPassword );

        //verify authorization works
        authedUser =
                setup.getMgmtSvc().verifyAppUserPasswordCredentials( MANAGEMENT_APPLICATION_ID, username, newPassword );

        assertEquals( userId, authedUser.getUuid() );
    }


    /**
     * Test we can change the password if it's hashed with md5 then sha1
     */
    @Test
    public void testAdminPasswordChangeMd5ShaType() throws Exception {
        String username = "testAdminPasswordChangeMd5ShaType";
        String password = "test";


        User user = new User();
        user.setActivated( true );
        user.setUsername( username );

        EntityManager em = setup.getEmf().getEntityManager( MANAGEMENT_APPLICATION_ID );

        User storedUser = em.create( user );


        UUID userId = storedUser.getUuid();

        //set the password in the sha1 format

        //set the password in the sha1 format
        CredentialsInfo info = new CredentialsInfo();
        info.setRecoverable( false );
        info.setEncrypted( true );


        Md5HashCommand md5 = new Md5HashCommand();

        Sha1HashCommand sha1 = new Sha1HashCommand();

        byte[] hashed = md5.hash( password.getBytes( "UTF-8" ), info, userId, MANAGEMENT_APPLICATION_ID );
        hashed = sha1.hash( hashed, info, userId, MANAGEMENT_APPLICATION_ID );

        info.setSecret( encodeBase64URLSafeString( hashed ) );
        //set the final cipher to sha1
        info.setCipher( sha1.getName() );
        //set the next hash type to md5
        info.setHashType( md5.getName() );


        em.addToDictionary( storedUser, DICTIONARY_CREDENTIALS, "password", info );


        //verify authorization works
        User authedUser =
                setup.getMgmtSvc().verifyAppUserPasswordCredentials( MANAGEMENT_APPLICATION_ID, username, password );

        assertEquals( userId, authedUser.getUuid() );

        //test we can change the password
        String newPassword = "test2";

        setup.getMgmtSvc().setAppUserPassword( MANAGEMENT_APPLICATION_ID, userId, password, newPassword );

        //verify authorization works
        authedUser =
                setup.getMgmtSvc().verifyAppUserPasswordCredentials( MANAGEMENT_APPLICATION_ID, username, newPassword );

        assertEquals( userId, authedUser.getUuid() );
    }


    @Test
    public void authenticateUser() throws Exception {

        String username = "tnine";
        String password = "test";
        String orgName = "autneticateUser";
        String appName = "authenticateUser";

        UUID appId = setup.getEmf().createApplication( orgName, appName );

        User user = new User();
        user.setActivated( true );
        user.setUsername( username );

        EntityManager em = setup.getEmf().getEntityManager( appId );

        User storedUser = em.create( user );


        UUID userId = storedUser.getUuid();

        //set the password
        setup.getMgmtSvc().setAppUserPassword( appId, userId, password );

        //verify authorization works
        User authedUser = setup.getMgmtSvc().verifyAppUserPasswordCredentials( appId, username, password );

        assertEquals( userId, authedUser.getUuid() );

        //test we can change the password
        String newPassword = "test2";

        setup.getMgmtSvc().setAppUserPassword( appId, userId, password, newPassword );

        //verify authorization works
        authedUser = setup.getMgmtSvc().verifyAppUserPasswordCredentials( appId, username, newPassword );
    }


    /**
     * Test we can change the password if it's hashed with sha1
     */
    @Test
    public void testAppUserPasswordChangeShaType() throws Exception {
        String username = "tnine";
        String password = "test";
        String orgName = "testAppUserPasswordChangeShaType";
        String appName = "testAppUserPasswordChangeShaType";

        UUID appId = setup.getEmf().createApplication( orgName, appName );

        User user = new User();
        user.setActivated( true );
        user.setUsername( username );

        EntityManager em = setup.getEmf().getEntityManager( appId );

        User storedUser = em.create( user );


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

        //verify authorization works
        authedUser = setup.getMgmtSvc().verifyAppUserPasswordCredentials( appId, username, newPassword );

        assertEquals( userId, authedUser.getUuid() );
    }


    /**
     * Test we can change the password if it's hashed with md5 then sha1
     */
    @Test
    public void testAppUserPasswordChangeMd5ShaType() throws Exception {
        String username = "tnine";
        String password = "test";
        String orgName = "testAppUserPasswordChangeMd5ShaType";
        String appName = "testAppUserPasswordChangeMd5ShaType";

        UUID appId = setup.getEmf().createApplication( orgName, appName );

        User user = new User();
        user.setActivated( true );
        user.setUsername( username );

        EntityManager em = setup.getEmf().getEntityManager( appId );

        User storedUser = em.create( user );


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

        //verify authorization works
        authedUser = setup.getMgmtSvc().verifyAppUserPasswordCredentials( appId, username, newPassword );

        assertEquals( userId, authedUser.getUuid() );
    }


    //
    //
    //Tests to make sure we can call the job with mock data and it runs.
    @Ignore //Connections won't save when run with maven, but on local builds it will.
    public void testConnectionsOnCollectionExport() throws Exception {

        File f = null;
        int indexCon = 0;


        try {
            f = new File( "testFileConnections.json" );
        }
        catch ( Exception e ) {
            //consumed because this checks to see if the file exists. If it doesn't then don't do anything and carry on.
        }


        S3Export s3Export = new MockS3ExportImpl();
        s3Export.setFilename( "testFileConnections.json" );

        ExportService exportService = setup.getExportService();
        HashMap<String, Object> payload = payloadBuilder();

        payload.put( "organizationId", organization.getUuid() );
        payload.put( "applicationId", applicationId );
        payload.put( "collectionName", "users" );

        EntityManager em = setup.getEmf().getEntityManager( applicationId );
        //intialize user object to be posted
        Map<String, Object> userProperties = null;
        Entity[] entity;
        entity = new Entity[2];
        //creates entities
        for ( int i = 0; i < 2; i++ ) {
            userProperties = new LinkedHashMap<String, Object>();
            userProperties.put( "username", "meatIsGreat" + i );
            userProperties.put( "email", "grey" + i + "@anuff.com" );//String.format( "test%i@anuff.com", i ) );

            entity[i] = em.create( "users", userProperties );
        }
        //creates connections
        em.createConnection( em.getRef( entity[0].getUuid() ), "Vibrations", em.getRef( entity[1].getUuid() ) );
        em.createConnection( em.getRef( entity[1].getUuid() ), "Vibrations", em.getRef( entity[0].getUuid() ) );

        UUID exportUUID = exportService.schedule( payload );
        exportService.setS3Export( s3Export );

        //create and initialize jobData returned in JobExecution.
        JobData jobData = new JobData();
        jobData.setProperty( "jobName", "exportJob" );
        jobData.setProperty( "exportInfo", payload );
        jobData.setProperty( "exportId", exportUUID );

        JobExecution jobExecution = mock( JobExecution.class );
        when( jobExecution.getJobData() ).thenReturn( jobData );

        exportService.doExport( jobExecution );

        JSONParser parser = new JSONParser();

        org.json.simple.JSONArray a = ( org.json.simple.JSONArray ) parser.parse( new FileReader( f ) );
        //assertEquals(2, a.size() );

        for ( indexCon = 0; indexCon < a.size(); indexCon++ ) {
            JSONObject jObj = ( JSONObject ) a.get( indexCon );
            JSONObject data = ( JSONObject ) jObj.get( "Metadata" );
            String uuid = ( String ) data.get( "uuid" );
            if ( entity[0].getUuid().toString().equals( uuid ) ) {
                break;
            }
        }

        org.json.simple.JSONObject objEnt = ( org.json.simple.JSONObject ) a.get( indexCon );
        org.json.simple.JSONObject objConnections = ( org.json.simple.JSONObject ) objEnt.get( "connections" );

        assertNotNull( objConnections );

        org.json.simple.JSONArray objVibrations = ( org.json.simple.JSONArray ) objConnections.get( "Vibrations" );

        assertNotNull( objVibrations );

        f.deleteOnExit();
    }


    @Test //Connections won't save when run with maven, but on local builds it will.
    public void testConnectionsOnApplicationEndpoint() throws Exception {

        File f = null;

        try {
            f = new File( "testConnectionsOnApplicationEndpoint.json" );
        }
        catch ( Exception e ) {
            //consumed because this checks to see if the file exists. If it doesn't then don't do anything and carry on.
        }


        S3Export s3Export = new MockS3ExportImpl();
        s3Export.setFilename( "testConnectionsOnApplicationEndpoint.json" );

        ExportService exportService = setup.getExportService();
        HashMap<String, Object> payload = payloadBuilder();

        payload.put( "organizationId", organization.getUuid() );
        payload.put( "applicationId", applicationId );

        EntityManager em = setup.getEmf().getEntityManager( applicationId );
        //intialize user object to be posted
        Map<String, Object> userProperties = null;
        Entity[] entity;
        entity = new Entity[2];
        //creates entities
        for ( int i = 0; i < 2; i++ ) {
            userProperties = new LinkedHashMap<String, Object>();
            userProperties.put( "username", "billybob" + i );
            userProperties.put( "email", "test" + i + "@anuff.com" );//String.format( "test%i@anuff.com", i ) );

            entity[i] = em.create( "users", userProperties );
        }
        //creates connections
        em.createConnection( em.getRef( entity[0].getUuid() ), "Vibrations", em.getRef( entity[1].getUuid() ) );
        em.createConnection( em.getRef( entity[1].getUuid() ), "Vibrations", em.getRef( entity[0].getUuid() ) );

        UUID exportUUID = exportService.schedule( payload );
        exportService.setS3Export( s3Export );

        //create and initialize jobData returned in JobExecution.
        JobData jobData = new JobData();
        jobData.setProperty( "jobName", "exportJob" );
        jobData.setProperty( "exportInfo", payload );
        jobData.setProperty( "exportId", exportUUID );

        JobExecution jobExecution = mock( JobExecution.class );
        when( jobExecution.getJobData() ).thenReturn( jobData );

        exportService.doExport( jobExecution );

        JSONParser parser = new JSONParser();

        org.json.simple.JSONArray a = ( org.json.simple.JSONArray ) parser.parse( new FileReader( f ) );
        int indexApp = 0;

        for ( indexApp = 0; indexApp < a.size(); indexApp++ ) {
            JSONObject jObj = ( JSONObject ) a.get( indexApp );
            JSONObject data = ( JSONObject ) jObj.get( "Metadata" );
            String uuid = ( String ) data.get( "uuid" );
            if ( entity[0].getUuid().toString().equals( uuid ) ) {
                break;
            }
        }
        if ( indexApp >= a.size() ) {
            //what? How does this condition even get reached due to the above forloop
            assert ( false );
        }

        org.json.simple.JSONObject objEnt = ( org.json.simple.JSONObject ) a.get( indexApp );
        org.json.simple.JSONObject objConnections = ( org.json.simple.JSONObject ) objEnt.get( "connections" );

        assertNotNull( objConnections );

        org.json.simple.JSONArray objVibrations = ( org.json.simple.JSONArray ) objConnections.get( "Vibrations" );

        assertNotNull( objVibrations );

        f.deleteOnExit();
    }


    //
    ////need to add tests for the other endpoint as well.
    @Test
    public void testValidityOfCollectionExport() throws Exception {

        File f = null;

        try {
            f = new File( "fileValidity.json" );
        }
        catch ( Exception e ) {
            //consumed because this checks to see if the file exists. If it doesn't then don't do anything and carry on.
        }

        S3Export s3Export = new MockS3ExportImpl();
        s3Export.setFilename( "fileValidity.json" );
        ExportService exportService = setup.getExportService();
        HashMap<String, Object> payload = payloadBuilder();

        payload.put( "organizationId", organization.getUuid() );
        payload.put( "applicationId", applicationId );
        payload.put( "collectionName", "users" );

        UUID exportUUID = exportService.schedule( payload );
        exportService.setS3Export( s3Export );

        JobData jobData = new JobData();
        jobData.setProperty( "jobName", "exportJob" );
        jobData.setProperty( "exportInfo", payload );
        jobData.setProperty( "exportId", exportUUID );

        JobExecution jobExecution = mock( JobExecution.class );
        when( jobExecution.getJobData() ).thenReturn( jobData );

        exportService.doExport( jobExecution );

        JSONParser parser = new JSONParser();

        org.json.simple.JSONArray a = ( org.json.simple.JSONArray ) parser.parse( new FileReader( f ) );

        for ( int i = 0; i < a.size(); i++ ) {
            org.json.simple.JSONObject entity = ( org.json.simple.JSONObject ) a.get( i );
            org.json.simple.JSONObject entityData = ( JSONObject ) entity.get( "Metadata" );
            assertNotNull( entityData );
        }
        f.deleteOnExit();
    }


    //
    @Test
    public void testValidityOfApplicationExport() throws Exception {

        File f = null;

        try {
            f = new File( "testValidityOfApplicationExport.json" );
        }
        catch ( Exception e ) {
            //consumed because this checks to see if the file exists. If it doesn't then don't do anything and carry on.
        }

        S3Export s3Export = new MockS3ExportImpl();
        s3Export.setFilename( "testValidityOfApplicationExport.json" );
        ExportService exportService = setup.getExportService();
        HashMap<String, Object> payload = payloadBuilder();

        payload.put( "organizationId", organization.getUuid() );
        payload.put( "applicationId", applicationId );

        UUID exportUUID = exportService.schedule( payload );
        exportService.setS3Export( s3Export );

        JobData jobData = new JobData();
        jobData.setProperty( "jobName", "exportJob" );
        jobData.setProperty( "exportInfo", payload );
        jobData.setProperty( "exportId", exportUUID );

        JobExecution jobExecution = mock( JobExecution.class );
        when( jobExecution.getJobData() ).thenReturn( jobData );

        exportService.doExport( jobExecution );

        JSONParser parser = new JSONParser();

        org.json.simple.JSONArray a = ( org.json.simple.JSONArray ) parser.parse( new FileReader( f ) );

        for ( int i = 0; i < a.size(); i++ ) {
            org.json.simple.JSONObject entity = ( org.json.simple.JSONObject ) a.get( i );
            org.json.simple.JSONObject entityData = ( JSONObject ) entity.get( "Metadata" );
            assertNotNull( entityData );
        }
        f.deleteOnExit();
    }


    //
    @Test
    public void testExportOneOrgCollectionEndpoint() throws Exception {

        File f = null;


        try {
            f = new File( "exportOneOrg.json" );
        }
        catch ( Exception e ) {
            //consumed because this checks to see if the file exists. If it doesn't then don't do anything and carry on.
        }
        setup.getMgmtSvc()
             .createOwnerAndOrganization( "noExport", "junkUserName", "junkRealName", "ugExport@usergrid.com",
                     "123456789" );

        S3Export s3Export = new MockS3ExportImpl();
        s3Export.setFilename( "exportOneOrg.json" );
        ExportService exportService = setup.getExportService();
        HashMap<String, Object> payload = payloadBuilder();

        payload.put( "organizationId", organization.getUuid() );
        payload.put( "applicationId", applicationId );
        payload.put( "collectionName", "roles" );

        UUID exportUUID = exportService.schedule( payload );
        exportService.setS3Export( s3Export );

        JobData jobData = new JobData();
        jobData.setProperty( "jobName", "exportJob" );
        jobData.setProperty( "exportInfo", payload );
        jobData.setProperty( "exportId", exportUUID );

        JobExecution jobExecution = mock( JobExecution.class );
        when( jobExecution.getJobData() ).thenReturn( jobData );

        exportService.doExport( jobExecution );

        JSONParser parser = new JSONParser();

        org.json.simple.JSONArray a = ( org.json.simple.JSONArray ) parser.parse( new FileReader( f ) );

        assertEquals( 3, a.size() );
        for ( int i = 0; i < a.size(); i++ ) {
            org.json.simple.JSONObject entity = ( org.json.simple.JSONObject ) a.get( i );
            org.json.simple.JSONObject entityData = ( JSONObject ) entity.get( "Metadata" );
            String entityName = ( String ) entityData.get( "name" );
            // assertNotEquals( "NotEqual","junkRealName",entityName );
            assertFalse( "junkRealName".equals( entityName ) );
        }
        f.deleteOnExit();
    }


    //
    //creation of files doesn't always delete itself
    @Test
    public void testExportOneAppOnCollectionEndpoint() throws Exception {

        File f = null;
        String orgName = "ed-organization";
        String appName = "testAppCollectionTestNotExported";

        try {
            f = new File( "exportOneApp.json" );
        }
        catch ( Exception e ) {
            //consumed because this checks to see if the file exists. If it doesn't, don't do anything and carry on.
        }
        f.deleteOnExit();


        UUID appId = setup.getEmf().createApplication( orgName, appName );


        EntityManager em = setup.getEmf().getEntityManager( appId );
        //intialize user object to be posted
        Map<String, Object> userProperties = null;
        Entity[] entity;
        entity = new Entity[1];
        //creates entities
        for ( int i = 0; i < 1; i++ ) {
            userProperties = new LinkedHashMap<String, Object>();
            userProperties.put( "username", "junkRealName" );
            userProperties.put( "email", "test" + i + "@anuff.com" );//String.format( "test%i@anuff.com", i ) );
            entity[i] = em.create( "user", userProperties );
        }

        S3Export s3Export = new MockS3ExportImpl();
        s3Export.setFilename( "exportOneApp.json" );
        ExportService exportService = setup.getExportService();
        HashMap<String, Object> payload = payloadBuilder();

        payload.put( "organizationId", organization.getUuid() );
        payload.put( "applicationId", applicationId );

        UUID exportUUID = exportService.schedule( payload );
        exportService.setS3Export( s3Export );

        JobData jobData = new JobData();
        jobData.setProperty( "jobName", "exportJob" );
        jobData.setProperty( "exportInfo", payload );
        jobData.setProperty( "exportId", exportUUID );

        JobExecution jobExecution = mock( JobExecution.class );
        when( jobExecution.getJobData() ).thenReturn( jobData );

        exportService.doExport( jobExecution );

        JSONParser parser = new JSONParser();

        org.json.simple.JSONArray a = ( org.json.simple.JSONArray ) parser.parse( new FileReader( f ) );

        //assertEquals( 3 , a.size() );
        for ( int i = 0; i < a.size(); i++ ) {
            org.json.simple.JSONObject data = ( org.json.simple.JSONObject ) a.get( i );
            org.json.simple.JSONObject entityData = ( JSONObject ) data.get( "Metadata" );
            String entityName = ( String ) entityData.get( "name" );
            assertFalse( "junkRealName".equals( entityName ) );
        }
    }


    @Test
    public void testExportOneAppOnApplicationEndpointWQuery() throws Exception {

        File f = null;

        try {
            f = new File( "exportOneAppWQuery.json" );
        }
        catch ( Exception e ) {
            //consumed because this checks to see if the file exists. If it doesn't, don't do anything and carry on.
        }

        f.deleteOnExit();


        EntityManager em = setup.getEmf().getEntityManager( applicationId );
        //intialize user object to be posted
        Map<String, Object> userProperties = null;
        Entity[] entity;
        entity = new Entity[1];
        //creates entities
        for ( int i = 0; i < 1; i++ ) {
            userProperties = new LinkedHashMap<String, Object>();
            userProperties.put( "name", "me" );
            userProperties.put( "username", "junkRealName" );
            userProperties.put( "email", "burp" + i + "@anuff.com" );//String.format( "test%i@anuff.com", i ) );
            entity[i] = em.create( "users", userProperties );
        }

        S3Export s3Export = new MockS3ExportImpl();
        s3Export.setFilename( "exportOneAppWQuery.json" );
        ExportService exportService = setup.getExportService();
        HashMap<String, Object> payload = payloadBuilder();

        payload.put( "query", "select * where username = 'junkRealName'" );

        payload.put( "organizationId", organization.getUuid() );
        payload.put( "applicationId", applicationId );

        UUID exportUUID = exportService.schedule( payload );
        exportService.setS3Export( s3Export );

        JobData jobData = new JobData();
        jobData.setProperty( "jobName", "exportJob" );
        jobData.setProperty( "exportInfo", payload );
        jobData.setProperty( "exportId", exportUUID );

        JobExecution jobExecution = mock( JobExecution.class );
        when( jobExecution.getJobData() ).thenReturn( jobData );

        exportService.doExport( jobExecution );

        JSONParser parser = new JSONParser();

        org.json.simple.JSONArray a = ( org.json.simple.JSONArray ) parser.parse( new FileReader( f ) );

        assertEquals( 1, a.size() );
        for ( int i = 0; i < a.size(); i++ ) {
            org.json.simple.JSONObject data = ( org.json.simple.JSONObject ) a.get( i );
            org.json.simple.JSONObject entityData = ( JSONObject ) data.get( "Metadata" );
            String entityName = ( String ) entityData.get( "name" );
            assertFalse( "junkRealName".equals( entityName ) );
        }
    }


    //
    @Test
    public void testExportOneCollection() throws Exception {

        File f = null;
        int entitiesToCreate = 5;

        try {
            f = new File( "exportOneCollection.json" );
        }
        catch ( Exception e ) {
            //consumed because this checks to see if the file exists. If it doesn't, don't do anything and carry on.
        }

        f.deleteOnExit();

        EntityManager em = setup.getEmf().getEntityManager( applicationId );
        em.createApplicationCollection( "qt" );
        //intialize user object to be posted
        Map<String, Object> userProperties = null;
        Entity[] entity;
        entity = new Entity[entitiesToCreate];
        //creates entities
        for ( int i = 0; i < entitiesToCreate; i++ ) {
            userProperties = new LinkedHashMap<String, Object>();
            userProperties.put( "username", "billybob" + i );
            userProperties.put( "email", "test" + i + "@anuff.com" );//String.format( "test%i@anuff.com", i ) );
            entity[i] = em.create( "qts", userProperties );
        }

        S3Export s3Export = new MockS3ExportImpl();
        s3Export.setFilename( "exportOneCollection.json" );
        ExportService exportService = setup.getExportService();
        HashMap<String, Object> payload = payloadBuilder();

        payload.put( "organizationId", organization.getUuid() );
        payload.put( "applicationId", applicationId );
        payload.put( "collectionName", "qts" );

        UUID exportUUID = exportService.schedule( payload );
        exportService.setS3Export( s3Export );

        JobData jobData = new JobData();
        jobData.setProperty( "jobName", "exportJob" );
        jobData.setProperty( "exportInfo", payload );
        jobData.setProperty( "exportId", exportUUID );

        JobExecution jobExecution = mock( JobExecution.class );
        when( jobExecution.getJobData() ).thenReturn( jobData );

        exportService.doExport( jobExecution );

        JSONParser parser = new JSONParser();

        org.json.simple.JSONArray a = ( org.json.simple.JSONArray ) parser.parse( new FileReader( f ) );

        assertEquals( entitiesToCreate, a.size() );
    }


    @Test
    public void testExportOneCollectionWQuery() throws Exception {

        File f = null;
        int entitiesToCreate = 5;

        try {
            f = new File( "exportOneCollectionWQuery.json" );
        }
        catch ( Exception e ) {
            //consumed because this checks to see if the file exists. If it doesn't, don't do anything and carry on.
        }
        f.deleteOnExit();

        EntityManager em = setup.getEmf().getEntityManager( applicationId );
        em.createApplicationCollection( "baconators" );
        //intialize user object to be posted
        Map<String, Object> userProperties = null;
        Entity[] entity;
        entity = new Entity[entitiesToCreate];
        //creates entities
        for ( int i = 0; i < entitiesToCreate; i++ ) {
            userProperties = new LinkedHashMap<String, Object>();
            userProperties.put( "username", "billybob" + i );
            userProperties.put( "email", "test" + i + "@anuff.com" );//String.format( "test%i@anuff.com", i ) );
            entity[i] = em.create( "baconators", userProperties );
        }

        S3Export s3Export = new MockS3ExportImpl();
        s3Export.setFilename( "exportOneCollectionWQuery.json" );
        ExportService exportService = setup.getExportService();
        HashMap<String, Object> payload = payloadBuilder();
        payload.put( "query", "select * where username contains 'billybob0'" );

        payload.put( "organizationId", organization.getUuid() );
        payload.put( "applicationId", applicationId );
        payload.put( "collectionName", "baconators" );

        UUID exportUUID = exportService.schedule( payload );
        exportService.setS3Export( s3Export );

        JobData jobData = new JobData();
        jobData.setProperty( "jobName", "exportJob" );
        jobData.setProperty( "exportInfo", payload );
        jobData.setProperty( "exportId", exportUUID );

        JobExecution jobExecution = mock( JobExecution.class );
        when( jobExecution.getJobData() ).thenReturn( jobData );

        exportService.doExport( jobExecution );

        JSONParser parser = new JSONParser();

        org.json.simple.JSONArray a = ( org.json.simple.JSONArray ) parser.parse( new FileReader( f ) );

        //only one entity should match the query.
        assertEquals( 1, a.size() );
    }


    //@Ignore("file created won't be deleted when running tests")
    @Test
    public void testExportOneOrganization() throws Exception {

        //File f = new File( "exportOneOrganization.json" );
        int entitiesToCreate = 123;
        File f = null;


        try {
            f = new File( "exportOneOrganization.json" );
        }
        catch ( Exception e ) {
            //consumed because this checks to see if the file exists. If it doesn't, don't do anything and carry on.
        }

        EntityManager em = setup.getEmf().getEntityManager( applicationId );
        em.createApplicationCollection( "newOrg" );
        //intialize user object to be posted
        Map<String, Object> userProperties = null;
        Entity[] entity;
        entity = new Entity[entitiesToCreate];
        //creates entities
        for ( int i = 0; i < entitiesToCreate; i++ ) {
            userProperties = new LinkedHashMap<String, Object>();
            userProperties.put( "username", "billybob" + i );
            userProperties.put( "email", "test" + i + "@anuff.com" );//String.format( "test%i@anuff.com", i ) );
            entity[i] = em.create( "newOrg", userProperties );
        }

        S3Export s3Export = new MockS3ExportImpl();
        s3Export.setFilename( "exportOneOrganization.json" );
        ExportService exportService = setup.getExportService();
        HashMap<String, Object> payload = payloadBuilder();

        //        payload.put( "organizationId",organization.getUuid() );
        //        payload.put( "applicationId",applicationId);

        //creates 100s of organizations with some entities in each one to make sure we don't actually apply it
        OrganizationInfo orgMade = null;
        ApplicationInfo appMade = null;
        for ( int i = 0; i < 100; i++ ) {
            orgMade = setup.getMgmtSvc().createOrganization( "superboss" + i, adminUser, true );
            appMade = setup.getMgmtSvc().createApplication( orgMade.getUuid(), "superapp" + i );

            EntityManager customMaker = setup.getEmf().getEntityManager( appMade.getId() );
            customMaker.createApplicationCollection( "superappCol" + i );
            //intialize user object to be posted
            Map<String, Object> entityLevelProperties = null;
            Entity[] entNotCopied;
            entNotCopied = new Entity[entitiesToCreate];
            //creates entities
            for ( int index = 0; index < 20; index++ ) {
                entityLevelProperties = new LinkedHashMap<String, Object>();
                entityLevelProperties.put( "username", "bobso" + index );
                entityLevelProperties.put( "email", "derp" + index + "@anuff.com" );
                entNotCopied[index] = customMaker.create( "superappCol", entityLevelProperties );
            }
        }
        payload.put( "organizationId", orgMade.getUuid() );
        payload.put( "applicationId", appMade.getId() );

        UUID exportUUID = exportService.schedule( payload );
        exportService.setS3Export( s3Export );

        JobData jobData = new JobData();
        jobData.setProperty( "jobName", "exportJob" );
        jobData.setProperty( "exportInfo", payload );
        jobData.setProperty( "exportId", exportUUID );

        JobExecution jobExecution = mock( JobExecution.class );
        when( jobExecution.getJobData() ).thenReturn( jobData );

        exportService.doExport( jobExecution );

        JSONParser parser = new JSONParser();

        org.json.simple.JSONArray a = ( org.json.simple.JSONArray ) parser.parse( new FileReader( f ) );

        /*plus 3 for the default roles*/
        assertEquals( 23, a.size() );
        f.deleteOnExit();
    }


    @Test
    public void testExportDoJob() throws Exception {

        HashMap<String, Object> payload = payloadBuilder();
        payload.put( "organizationId", organization.getUuid() );
        payload.put( "applicationId", applicationId );


        JobData jobData = new JobData();
        jobData.setProperty( "jobName", "exportJob" );
        jobData.setProperty( "exportInfo", payload ); //this needs to be populated with properties of export info

        JobExecution jobExecution = mock( JobExecution.class );

        when( jobExecution.getJobData() ).thenReturn( jobData );

        ExportJob job = new ExportJob();
        ExportService eS = mock( ExportService.class );
        job.setExportService( eS );
        try {
            job.doJob( jobExecution );
        }
        catch ( Exception e ) {
            assert ( false );
        }
        assert ( true );
    }


    @Test
    public void testExportDoExportOnApplicationEndpoint() throws Exception {

        EntityManagerFactory emf = setup.getEmf();
        EntityManager em = emf.getEntityManager( applicationId );
        HashMap<String, Object> payload = payloadBuilder();
        ExportService eS = setup.getExportService();

        JobExecution jobExecution = mock( JobExecution.class );

        payload.put( "organizationId", organization.getUuid() );
        payload.put( "applicationId", applicationId );

        UUID entityExportUUID = eS.schedule( payload );


        JobData jobData = new JobData();
        jobData.setProperty( "jobName", "exportJob" );
        jobData.setProperty( "exportInfo", payload );
        jobData.setProperty( "exportId", entityExportUUID );

        when( jobExecution.getJobData() ).thenReturn( jobData );

        //Exportem.get(entityExport);
        Export exportEntity = ( Export ) em.get( entityExportUUID );
        assertNotNull( exportEntity );
        String derp = exportEntity.getState().name();
        assertEquals( "SCHEDULED", exportEntity.getState().name() );
        try {
            eS.doExport( jobExecution );
        }
        catch ( Exception e ) {
            //throw e;
            assert ( false );
        }
        exportEntity = ( Export ) em.get( entityExportUUID );
        assertNotNull( exportEntity );
        assertEquals( "FINISHED", exportEntity.getState().name() );
    }


    //tests that with empty job data, the export still runs.
    @Test
    public void testExportEmptyJobData() throws Exception {

        JobData jobData = new JobData();

        JobExecution jobExecution = mock( JobExecution.class );

        when( jobExecution.getJobData() ).thenReturn( jobData );

        ExportJob job = new ExportJob();
        S3Export s3Export = mock( S3Export.class );
        setup.getExportService().setS3Export( s3Export );
        job.setExportService( setup.getExportService() );
        try {
            job.doJob( jobExecution );
        }
        catch ( Exception e ) {
            assert ( false );
        }
        assert ( true );
    }


    @Test
    public void testNullJobExecution() {

        JobData jobData = new JobData();

        JobExecution jobExecution = mock( JobExecution.class );

        when( jobExecution.getJobData() ).thenReturn( jobData );

        ExportJob job = new ExportJob();
        S3Export s3Export = mock( S3Export.class );
        setup.getExportService().setS3Export( s3Export );
        job.setExportService( setup.getExportService() );
        try {
            job.doJob( jobExecution );
        }
        catch ( Exception e ) {
            assert ( false );
        }
        assert ( true );
    }


    //   @Ignore //For this test please input your s3 credentials into settings.xml or Attach a -D with relevant fields.
    @Test
    public void testIntegration100EntitiesOn() throws Exception {

        S3Export s3Export = new S3ExportImpl();
        ExportService exportService = setup.getExportService();
        HashMap<String, Object> payload = payloadBuilder();

        payload.put( "organizationId", organization.getUuid() );
        payload.put( "applicationId", applicationId );

        EntityManager em = setup.getEmf().getEntityManager( applicationId );
        //intialize user object to be posted
        Map<String, Object> userProperties = null;
        Entity[] entity;
        entity = new Entity[5];
        //creates entities

        ApplicationInfo appMade = null;
        for ( int i = 0; i < 5; i++ ) {
            appMade = setup.getMgmtSvc().createApplication( organization.getUuid(), "superapp" + i );

            EntityManager customMaker = setup.getEmf().getEntityManager( appMade.getId() );
            customMaker.createApplicationCollection( "superappCol" + i );
            //intialize user object to be posted
            Map<String, Object> entityLevelProperties = null;
            Entity[] entNotCopied;
            entNotCopied = new Entity[5];
            //creates entities
            for ( int index = 0; index < 5; index++ ) {
                entityLevelProperties = new LinkedHashMap<String, Object>();
                entityLevelProperties.put( "username", "bobso" + index );
                entityLevelProperties.put( "email", "derp" + index + "@anuff.com" );
                entNotCopied[index] = customMaker.create( "superappCol", entityLevelProperties );
            }
        }

        UUID exportUUID = exportService.schedule( payload );
        exportService.setS3Export( s3Export );

        //create and initialize jobData returned in JobExecution.
        JobData jobData = new JobData();
        jobData.setProperty( "jobName", "exportJob" );
        jobData.setProperty( "exportInfo", payload );
        jobData.setProperty( "exportId", exportUUID );

        JobExecution jobExecution = mock( JobExecution.class );
        when( jobExecution.getJobData() ).thenReturn( jobData );

        exportService.doExport( jobExecution );
        while ( !exportService.getState( applicationId, exportUUID ).equals( "FINISHED" ) ) {
            ;
        }

        String bucketName = System.getProperty( "bucketName" );
        String accessId = System.getProperty( "accessKey" );
        String secretKey = System.getProperty( "secretKey" );

        Properties overrides = new Properties();
        overrides.setProperty( "s3" + ".identity", accessId );
        overrides.setProperty( "s3" + ".credential", secretKey );

        Blob bo = null;
        BlobStore blobStore = null;

        try {
            final Iterable<? extends Module> MODULES = ImmutableSet
                    .of( new JavaUrlHttpCommandExecutorServiceModule(), new Log4JLoggingModule(),
                            new NettyPayloadModule() );

            BlobStoreContext context =
                    ContextBuilder.newBuilder( "s3" ).credentials( accessId, secretKey ).modules( MODULES )
                                  .overrides( overrides ).buildView( BlobStoreContext.class );


            blobStore = context.getBlobStore();
            if ( !blobStore.blobExists( bucketName, s3Export.getFilename() ) ) {
                blobStore.deleteContainer( bucketName );
                assert ( false );
            }
            Long numOfFiles = blobStore.countBlobs( bucketName );
            Long numWeWant = Long.valueOf( 1 );
            assertEquals( numOfFiles, numWeWant );

            bo = blobStore.getBlob( bucketName, s3Export.getFilename() );
        }
        catch ( Exception e ) {
            assert ( false );
        }

        assertNotNull( bo );
        blobStore.deleteContainer( bucketName );
    }


    @Test
    public void testIntegration100EntitiesForAllApps() throws Exception {

        S3Export s3Export = new S3ExportImpl();
        ExportService exportService = setup.getExportService();
        HashMap<String, Object> payload = payloadBuilder();

        OrganizationInfo orgMade = null;
        ApplicationInfo appMade = null;
        for ( int i = 0; i < 5; i++ ) {
            orgMade = setup.getMgmtSvc().createOrganization( "minorboss" + i, adminUser, true );
            for ( int j = 0; j < 5; j++ ) {
                appMade = setup.getMgmtSvc().createApplication( orgMade.getUuid(), "superapp" + j );

                EntityManager customMaker = setup.getEmf().getEntityManager( appMade.getId() );
                customMaker.createApplicationCollection( "superappCol" + j );
                //intialize user object to be posted
                Map<String, Object> entityLevelProperties = null;
                Entity[] entNotCopied;
                entNotCopied = new Entity[1];
                //creates entities
                for ( int index = 0; index < 1; index++ ) {
                    entityLevelProperties = new LinkedHashMap<String, Object>();
                    entityLevelProperties.put( "derp", "bacon" );
                    //                    entityLevelProperties.put( "username", "bobso" + index );
                    //                    entityLevelProperties.put( "email", "derp" + index + "@anuff.com" );
                    entNotCopied[index] = customMaker.create( "superappCol" + j, entityLevelProperties );
                }
            }
        }

        payload.put( "organizationId", orgMade.getUuid() );

        //EntityManager em = setup.getEmf().getEntityManager( appMade.getId() );
        //intialize user object to be posted
        //        Map<String, Object> userProperties = null;
        //        Entity[] entity;
        //        entity = new Entity[100];
        //        //creates entities
        //        for ( int i = 0; i < 100; i++ ) {
        //            userProperties = new LinkedHashMap<String, Object>();
        //            userProperties.put( "username", "bido" + i );
        //            userProperties.put( "email", "bido" + i + "@anuff.com" );
        //
        //            entity[i] = em.create( "user", userProperties );
        //        }

        UUID exportUUID = exportService.schedule( payload );
        assertNotNull( exportUUID );
        exportService.setS3Export( s3Export );

        //create and initialize jobData returned in JobExecution.
        JobData jobData = new JobData();
        jobData.setProperty( "jobName", "exportJob" );
        jobData.setProperty( "exportInfo", payload );
        jobData.setProperty( "exportId", exportUUID );

        JobExecution jobExecution = mock( JobExecution.class );
        when( jobExecution.getJobData() ).thenReturn( jobData );

        //the exporters app is never returned, and I'm not even sure if that is the way we want to go
        // I feel like there should be a better way than having to store data for the user.
        exportService.doExport( jobExecution );
        //BiMap<UUID,String> mapper = setup.getMgmtSvc().getApplicationsForOrganization(orgMade.getUuid());
        //mapper.get( "" )
        //        while (!exportService.getState( ,exportUUID )
        //                             .equals("FINISHED"));

        Thread.sleep( 3000 );

        String bucketName = System.getProperty( "bucketName" );
        String accessId = System.getProperty( "accessKey" );
        String secretKey = System.getProperty( "secretKey" );

        Properties overrides = new Properties();
        overrides.setProperty( "s3" + ".identity", accessId );
        overrides.setProperty( "s3" + ".credential", secretKey );

        Blob bo = null;
        BlobStore blobStore = null;

        try {
            final Iterable<? extends Module> MODULES = ImmutableSet
                    .of( new JavaUrlHttpCommandExecutorServiceModule(), new Log4JLoggingModule(),
                            new NettyPayloadModule() );

            BlobStoreContext context =
                    ContextBuilder.newBuilder( "s3" ).credentials( accessId, secretKey ).modules( MODULES )
                                  .overrides( overrides ).buildView( BlobStoreContext.class );


            blobStore = context.getBlobStore();
            //            if(!blobStore.blobExists( bucketName,s3Export.getFilename()  )){
            //                blobStore.deleteContainer( bucketName );
            //                assert(false);
            //            }
            //Grab Number of files
            Long numOfFiles = blobStore.countBlobs( bucketName );
            //delete container containing said files
            bo = blobStore.getBlob( bucketName, s3Export.getFilename() );
            blobStore.deleteContainer( bucketName );
            Long numWeWant = Long.valueOf( 5 );
            //asserts that the correct number of files was transferred over
            assertEquals( numWeWant, numOfFiles );
        }
        catch ( Exception e ) {
            blobStore.deleteContainer( bucketName );
            e.printStackTrace();
            assert ( false );
        }

        assertNotNull( bo );
        blobStore.deleteContainer( bucketName );
    }


    @Test
    public void testIntegration100EntitiesOnOneOrg() throws Exception {

        S3Export s3Export = new S3ExportImpl();
        ExportService exportService = setup.getExportService();
        HashMap<String, Object> payload = payloadBuilder();

        payload.put( "organizationId", organization.getUuid() );
        payload.put( "applicationId", applicationId );

        OrganizationInfo orgMade = null;
        ApplicationInfo appMade = null;
        for ( int i = 0; i < 100; i++ ) {
            orgMade = setup.getMgmtSvc().createOrganization( "largerboss" + i, adminUser, true );
            appMade = setup.getMgmtSvc().createApplication( orgMade.getUuid(), "superapp" + i );

            EntityManager customMaker = setup.getEmf().getEntityManager( appMade.getId() );
            customMaker.createApplicationCollection( "superappCol" + i );
            //intialize user object to be posted
            Map<String, Object> entityLevelProperties = null;
            Entity[] entNotCopied;
            entNotCopied = new Entity[20];
            //creates entities
            for ( int index = 0; index < 20; index++ ) {
                entityLevelProperties = new LinkedHashMap<String, Object>();
                entityLevelProperties.put( "username", "bobso" + index );
                entityLevelProperties.put( "email", "derp" + index + "@anuff.com" );
                entNotCopied[index] = customMaker.create( "superappCol", entityLevelProperties );
            }
        }

        EntityManager em = setup.getEmf().getEntityManager( applicationId );
        //intialize user object to be posted
        Map<String, Object> userProperties = null;
        Entity[] entity;
        entity = new Entity[100];
        //creates entities
        for ( int i = 0; i < 100; i++ ) {
            userProperties = new LinkedHashMap<String, Object>();
            userProperties.put( "username", "bido" + i );
            userProperties.put( "email", "bido" + i + "@anuff.com" );

            entity[i] = em.create( "user", userProperties );
        }

        UUID exportUUID = exportService.schedule( payload );
        exportService.setS3Export( s3Export );

        //create and initialize jobData returned in JobExecution.
        JobData jobData = new JobData();
        jobData.setProperty( "jobName", "exportJob" );
        jobData.setProperty( "exportInfo", payload );
        jobData.setProperty( "exportId", exportUUID );

        JobExecution jobExecution = mock( JobExecution.class );
        when( jobExecution.getJobData() ).thenReturn( jobData );

        exportService.doExport( jobExecution );
        while ( !exportService.getState( applicationId, exportUUID ).equals( "FINISHED" ) ) {
            ;
        }

        String bucketName = System.getProperty( "bucketName" );
        String accessId = System.getProperty( "accessKey" );
        String secretKey = System.getProperty( "secretKey" );

        Properties overrides = new Properties();
        overrides.setProperty( "s3" + ".identity", accessId );
        overrides.setProperty( "s3" + ".credential", secretKey );

        Blob bo = null;
        BlobStore blobStore = null;

        try {
            final Iterable<? extends Module> MODULES = ImmutableSet
                    .of( new JavaUrlHttpCommandExecutorServiceModule(), new Log4JLoggingModule(),
                            new NettyPayloadModule() );

            BlobStoreContext context =
                    ContextBuilder.newBuilder( "s3" ).credentials( accessId, secretKey ).modules( MODULES )
                                  .overrides( overrides ).buildView( BlobStoreContext.class );


            blobStore = context.getBlobStore();
            if ( !blobStore.blobExists( bucketName, s3Export.getFilename() ) ) {
                assert ( false );
            }
            Long numOfFiles = blobStore.countBlobs( bucketName );
            Long numWeWant = Long.valueOf( 1 );
            assertEquals( numOfFiles, numWeWant );

            bo = blobStore.getBlob( bucketName, s3Export.getFilename() );
        }
        catch ( Exception e ) {
            assert ( false );
        }

        assertNotNull( bo );
        blobStore.deleteContainer( bucketName );
    }


    /*Creates fake payload for testing purposes.*/
    public HashMap<String, Object> payloadBuilder() {
        HashMap<String, Object> payload = new HashMap<String, Object>();
        Map<String, Object> properties = new HashMap<String, Object>();
        Map<String, Object> storage_info = new HashMap<String, Object>();
        storage_info.put( "s3_key", System.getProperty( "secretKey" ) );
        storage_info.put( "s3_access_id", System.getProperty( "accessKey" ) );
        storage_info.put( "bucket_location", System.getProperty( "bucketName" ) );

        properties.put( "storage_provider", "s3" );
        properties.put( "storage_info", storage_info );

        payload.put( "path", "test-organization/test-app" );
        payload.put( "properties", properties );
        return payload;
    }
}
