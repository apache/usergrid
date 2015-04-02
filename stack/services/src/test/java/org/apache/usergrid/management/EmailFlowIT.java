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
package org.apache.usergrid.management;


import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.jvnet.mock_javamail.Mailbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrSubstitutor;

import org.apache.usergrid.ServiceITSetup;
import org.apache.usergrid.ServiceITSetupImpl;
import org.apache.usergrid.cassandra.ClearShiroSubject;
import org.apache.usergrid.management.cassandra.ManagementServiceImpl;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.SimpleEntityRef;
import org.apache.usergrid.persistence.entities.Application;
import org.apache.usergrid.persistence.entities.User;

import net.jcip.annotations.NotThreadSafe;

import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.apache.usergrid.TestHelper.uniqueApp;
import static org.apache.usergrid.TestHelper.uniqueEmail;
import static org.apache.usergrid.TestHelper.uniqueOrg;
import static org.apache.usergrid.TestHelper.uniqueUsername;
import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_ADMIN_USERS_REQUIRE_CONFIRMATION;
import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_EMAIL_ADMIN_ACTIVATED;
import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_EMAIL_ADMIN_CONFIRMATION;
import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_EMAIL_ADMIN_PASSWORD_RESET;
import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_EMAIL_ADMIN_USER_ACTIVATION;
import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_EMAIL_ORGANIZATION_ACTIVATED;
import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_EMAIL_ORGANIZATION_CONFIRMATION;
import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_EMAIL_SYSADMIN_ADMIN_ACTIVATION;
import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_EMAIL_SYSADMIN_ORGANIZATION_ACTIVATION;
import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_EMAIL_USER_ACTIVATED;
import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_EMAIL_USER_CONFIRMATION;
import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_EMAIL_USER_PASSWORD_RESET;
import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_EMAIL_USER_PIN_REQUEST;
import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_NOTIFY_ADMIN_OF_ACTIVATION;
import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_ORGANIZATIONS_REQUIRE_CONFIRMATION;
import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_SYSADMIN_APPROVES_ADMIN_USERS;
import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_SYSADMIN_APPROVES_ORGANIZATIONS;
import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_SYSADMIN_EMAIL;
import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_USER_ACTIVATION_URL;
import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_USER_CONFIRMATION_URL;
import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_USER_RESETPW_URL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;


/**
 * This test cannot be run concurrently because it changes the management service properties that control how the
 * activation workflow is handled and it uses a shared global mock Mailbox to process emails.
 * <p/>
 * Hence there can be race conditions between test methods in this class.
 */
@NotThreadSafe
public class EmailFlowIT {
    private static final Logger LOG = LoggerFactory.getLogger( EmailFlowIT.class );


    @Rule
    public ClearShiroSubject clearShiroSubject = new ClearShiroSubject();

    @ClassRule
    public static ServiceITSetup setup = new ServiceITSetupImpl( );

    @Rule
    public TestName name = new TestName();


    @Test
    public void testCreateOrganizationAndAdminWithConfirmationOnly() throws Exception {
        setup.set( PROPERTIES_SYSADMIN_APPROVES_ADMIN_USERS, "false" );
        setup.set( PROPERTIES_SYSADMIN_APPROVES_ORGANIZATIONS, "false" );
        setup.set( PROPERTIES_ADMIN_USERS_REQUIRE_CONFIRMATION, "true" );
        setup.set( PROPERTIES_SYSADMIN_EMAIL, "sysadmin-1@mockserver.com" );
        setup.set( PROPERTIES_NOTIFY_ADMIN_OF_ACTIVATION, "true" );

        final String orgName = uniqueOrg();
        final String userName = uniqueUsername();
        final String email = uniqueEmail();

        OrganizationOwnerInfo org_owner =
                createOwnerAndOrganization( orgName, userName, "Test User", email, "testpassword", false, false );


        assertNotNull( org_owner );

        List<Message> inbox = Mailbox.get( email );

        assertFalse( inbox.isEmpty() );

        MockImapClient client = new MockImapClient( "mockserver.com", "test-user-1", "somepassword" );
        client.processMail();

        Message confirmation = inbox.get( 0 );
        assertEquals( "User Account Confirmation: " + email, confirmation.getSubject() );

        String token = getTokenFromMessage( confirmation );
        LOG.info( token );

        assertEquals( ActivationState.ACTIVATED,
                setup.getMgmtSvc().handleConfirmationTokenForAdminUser( org_owner.owner.getUuid(), token ) );

        Message activation = inbox.get( 1 );
        assertEquals( "User Account Activated", activation.getSubject() );

        client = new MockImapClient( "mockserver.com", "test-user-1", "somepassword" );
        client.processMail();
    }


    @Test
    public void testCreateOrganizationAndAdminWithConfirmationAndActivation() throws Exception {
        setup.set( PROPERTIES_SYSADMIN_APPROVES_ADMIN_USERS, "true" );
        setup.set( PROPERTIES_NOTIFY_ADMIN_OF_ACTIVATION, "true" );
        setup.set( PROPERTIES_SYSADMIN_APPROVES_ORGANIZATIONS, "false" );
        setup.set( PROPERTIES_ADMIN_USERS_REQUIRE_CONFIRMATION, "true" );
        setup.set( PROPERTIES_SYSADMIN_EMAIL, "sysadmin-2@mockserver.com" );

        final String orgName = uniqueOrg();
        final String userName = uniqueUsername();
        final String email = uniqueEmail();

        OrganizationOwnerInfo org_owner =
                        createOwnerAndOrganization( orgName, userName, "Test User", email,
                                "testpassword", false, false );

        assertNotNull( org_owner );


        List<Message> user_inbox = Mailbox.get( email );

        assertFalse( user_inbox.isEmpty() );

        Message confirmation = user_inbox.get( 0 );
        assertEquals( "User Account Confirmation: "+email, confirmation.getSubject() );

        String token = getTokenFromMessage( confirmation );
        LOG.info( token );

        ActivationState state =
                setup.getMgmtSvc().handleConfirmationTokenForAdminUser( org_owner.owner.getUuid(), token );
        assertEquals( ActivationState.CONFIRMED_AWAITING_ACTIVATION, state );

        confirmation = user_inbox.get( 1 );
        String body = ( ( MimeMultipart ) confirmation.getContent() ).getBodyPart( 0 ).getContent().toString();
        Boolean subbedEmailed = StringUtils.contains( body, "$" );

        assertFalse( subbedEmailed );

        assertEquals( "User Account Confirmed", confirmation.getSubject() );

        List<Message> sysadmin_inbox = Mailbox.get( "sysadmin-2@mockserver.com" );
        assertFalse( sysadmin_inbox.isEmpty() );

        Message activation = sysadmin_inbox.get( 0 );
        assertEquals( "Request For Admin User Account Activation "+email, activation.getSubject() );

        token = getTokenFromMessage( activation );
        LOG.info( token );

        state = setup.getMgmtSvc().handleActivationTokenForAdminUser( org_owner.owner.getUuid(), token );
        assertEquals( ActivationState.ACTIVATED, state );

        Message activated = user_inbox.get( 2 );
        assertEquals( "User Account Activated", activated.getSubject() );

        MockImapClient client = new MockImapClient( "mockserver.com", "test-user-2", "somepassword" );
        client.processMail();
    }


    @Test
    public void skipAllEmailConfiguration() throws Exception {
        setup.set( PROPERTIES_SYSADMIN_APPROVES_ORGANIZATIONS, "false" );
        setup.set( PROPERTIES_ORGANIZATIONS_REQUIRE_CONFIRMATION, "false" );
        setup.set( PROPERTIES_SYSADMIN_APPROVES_ADMIN_USERS, "false" );
        setup.set( PROPERTIES_ADMIN_USERS_REQUIRE_CONFIRMATION, "false" );


        final String orgName = uniqueOrg();
        final String userName = uniqueUsername();
        final String email = uniqueEmail();

        OrganizationOwnerInfo ooi = setup.getMgmtSvc()
                                         .createOwnerAndOrganization(orgName, userName, "Test User", email,
                                                                         "testpassword");

        EntityManager em = setup.getEmf().getEntityManager( setup.getEmf().getManagementAppId() );
        User user = em.get( ooi.getOwner().getUuid(), User.class );
        assertTrue( user.activated() );
        assertFalse( user.disabled() );
        assertTrue( user.confirmed() );
    }


    @Test
    public void testEmailStrings() {
        testProperty( PROPERTIES_EMAIL_ADMIN_ACTIVATED, false );
        testProperty( PROPERTIES_EMAIL_ADMIN_CONFIRMATION, true );
        testProperty( PROPERTIES_EMAIL_ADMIN_PASSWORD_RESET, true );
        testProperty( PROPERTIES_EMAIL_ADMIN_USER_ACTIVATION, true );
        testProperty( PROPERTIES_EMAIL_ORGANIZATION_ACTIVATED, true );
        testProperty( PROPERTIES_EMAIL_ORGANIZATION_CONFIRMATION, true );
        testProperty( PROPERTIES_EMAIL_SYSADMIN_ADMIN_ACTIVATION, true );
        testProperty( PROPERTIES_EMAIL_SYSADMIN_ORGANIZATION_ACTIVATION, true );
        testProperty( PROPERTIES_EMAIL_USER_ACTIVATED, false );
        testProperty( PROPERTIES_EMAIL_USER_CONFIRMATION, true );
        testProperty( PROPERTIES_EMAIL_USER_PASSWORD_RESET, true );
        testProperty( PROPERTIES_EMAIL_USER_PIN_REQUEST, true );
    }


    /**
     * Tests that when a user is added to an app and activation on that app is required, the org admin is emailed
     * @throws Exception
     *
     * TODO, I'm not convinced this worked correctly.  IT can't find users collection in the orgs.  Therefore,
     * I think this is a legitimate bug that was just found from fixing the tests to be unique admins orgs and emails
     */
    @Test
    public void testAppUserActivationResetpwdMail() throws Exception {

        final String orgName = uniqueOrg();
        final String appName = uniqueApp();
        final String adminUserName = uniqueUsername();
        final String adminEmail = uniqueEmail();
        final String adminPasswd = "testpassword";

        OrganizationOwnerInfo orgOwner = createOwnerAndOrganization( orgName, appName, adminUserName, adminEmail, adminPasswd, false, false );
        assertNotNull( orgOwner );

        ApplicationInfo app = setup.getMgmtSvc().createApplication( orgOwner.getOrganization().getUuid(), appName );

        //turn on app admin approval for app users
        enableAdminApproval( app.getId() );

        final String appUserUsername = uniqueUsername();
        final String appUserEmail = uniqueEmail();

        User appUser = setupAppUser( app.getId(), appUserUsername, appUserEmail, false );

        String subject = "Request For User Account Activation " + appUserEmail;
        String activation_url = String.format( setup.get( PROPERTIES_USER_ACTIVATION_URL ), orgName, appName,
            appUser.getUuid().toString() );

        setup.refreshIndex();

        // Activation
        setup.getMgmtSvc().startAppUserActivationFlow( app.getId(), appUser );

        List<Message> inbox = Mailbox.get( adminEmail );
        assertFalse( inbox.isEmpty() );
        MockImapClient client = new MockImapClient( "usergrid.com", adminUserName, "somepassword" );
        client.processMail();

        // subject ok
        Message activation = inbox.get( 0 );
        assertEquals( subject, activation.getSubject() );

        // activation url ok
        String mailContent = ( String ) ( ( MimeMultipart ) activation.getContent() ).getBodyPart( 1 ).getContent();
        LOG.info( mailContent );
        assertTrue( StringUtils.contains( mailContent, activation_url ) );

        // token ok
        String token = getTokenFromMessage( activation );
        LOG.info( token );
        ActivationState activeState =
                setup.getMgmtSvc().handleActivationTokenForAppUser( app.getId(), appUser.getUuid(), token );
        assertEquals( ActivationState.ACTIVATED, activeState );

        subject = "Password Reset";
        String reset_url =
                String.format( setup.get( PROPERTIES_USER_RESETPW_URL ), orgName, appName, appUser.getUuid().toString() );

        // reset_pwd
        setup.getMgmtSvc().startAppUserPasswordResetFlow( app.getId(), appUser );

        inbox = Mailbox.get( appUserEmail );
        assertFalse( inbox.isEmpty() );
        client = new MockImapClient( "test.com", appUserUsername, "somepassword" );
        client.processMail();

        // subject ok
        Message reset = inbox.get( 1 );
        assertEquals( subject, reset.getSubject() );

        // resetpwd url ok
        mailContent = ( String ) ( ( MimeMultipart ) reset.getContent() ).getBodyPart( 1 ).getContent();
        LOG.info( mailContent );
        assertTrue( StringUtils.contains( mailContent, reset_url ) );

        // token ok
        token = getTokenFromMessage( reset );
        LOG.info( token );
        assertTrue( setup.getMgmtSvc().checkPasswordResetTokenForAppUser( app.getId(), appUser.getUuid(), token ) );

        // ensure revoke works
        setup.getMgmtSvc().revokeAccessTokenForAppUser( token );
        assertFalse( setup.getMgmtSvc().checkPasswordResetTokenForAppUser( app.getId(), appUser.getUuid(), token ) );
    }


    /** Tests to make sure a normal user must be activated by the admin after confirmation. */
    @Test
    public void testAppUserConfirmationMail() throws Exception {
        final String orgName = uniqueOrg();
        final String appName = uniqueApp();
        final String userName = uniqueUsername();
        final String email = uniqueEmail();
        final String passwd = "testpassword";


        OrganizationOwnerInfo orgOwner;

        orgOwner = createOwnerAndOrganization( orgName, appName, userName, email, passwd, false, false );
        assertNotNull( orgOwner );

        setup.getEntityIndex().refresh();

        ApplicationInfo app = setup.getMgmtSvc().createApplication( orgOwner.getOrganization().getUuid(), appName );
        setup.refreshIndex();
        assertNotNull( app );
        enableEmailConfirmation( app.getId() );
        enableAdminApproval( app.getId() );


        final String appUserEmail = uniqueEmail();
        final String appUserUsername = uniqueUsername();

        User user = setupAppUser( app.getId(), appUserUsername, appUserEmail, true );

        String subject = "User Account Confirmation: "+appUserEmail;
        String urlProp = setup.get( PROPERTIES_USER_CONFIRMATION_URL );
        String confirmation_url = String.format( urlProp, orgName, appName, user.getUuid().toString() );

        // request confirmation
        setup.getMgmtSvc().startAppUserActivationFlow( app.getId(), user );

        List<Message> inbox = Mailbox.get( appUserEmail );
        assertFalse( inbox.isEmpty() );
        MockImapClient client = new MockImapClient( "test.com", appUserUsername, "somepassword" );
        client.processMail();

        // subject ok
        Message confirmation = inbox.get( 0 );
        assertEquals( subject, confirmation.getSubject() );

        // confirmation url ok
        String mailContent = ( String ) ( ( MimeMultipart ) confirmation.getContent() ).getBodyPart( 1 ).getContent();
        LOG.info( mailContent );
        assertTrue( StringUtils.contains( mailContent, confirmation_url ) );

        // token ok
        String token = getTokenFromMessage( confirmation );
        LOG.info( token );
        ActivationState activeState =
                setup.getMgmtSvc().handleConfirmationTokenForAppUser( app.getId(), user.getUuid(), token );
        assertEquals( ActivationState.CONFIRMED_AWAITING_ACTIVATION, activeState );
    }


    /////////////////////////////
    // Private Utility Methods //
    /////////////////////////////


    private OrganizationOwnerInfo createOwnerAndOrganization( String orgName, String userName, String display,
                                                              String email, String password, boolean disabled,
                                                              boolean active ) throws Exception {
        return setup.getMgmtSvc()
                    .createOwnerAndOrganization( orgName, userName, display, email, password, disabled, active );
    }


    private String getTokenFromMessage( Message msg ) throws IOException, MessagingException {
        String body = ( ( MimeMultipart ) msg.getContent() ).getBodyPart( 0 ).getContent().toString();
        // TODO better token extraction
        // this is going to get the wrong string if the first part is not
        // text/plain and the url isn't the last character in the email
        return StringUtils.substringAfterLast( body, "token=" );
    }


    private void testProperty( String propertyName, boolean containsSubstitution ) {
        String propertyValue = setup.get( propertyName );
        assertTrue( propertyName + " was not found", isNotBlank( propertyValue ) );
        LOG.info( propertyName + "=" + propertyValue );

        if ( containsSubstitution ) {
            Map<String, String> valuesMap = new HashMap<String, String>();
            valuesMap.put( "reset_url", "test-url" );
            valuesMap.put( "organization_name", "test-org" );
            valuesMap.put( "activation_url", "test-url" );
            valuesMap.put( "confirmation_url", "test-url" );
            valuesMap.put( "user_email", "test-email" );
            valuesMap.put( "pin", "test-pin" );
            StrSubstitutor sub = new StrSubstitutor( valuesMap );
            String resolvedString = sub.replace( propertyValue );
            assertNotSame( propertyValue, resolvedString );
        }
    }


    private void enableEmailConfirmation( UUID appId ) throws Exception {
        EntityManager em = setup.getEmf().getEntityManager( appId );
        SimpleEntityRef ref = new SimpleEntityRef( Application.ENTITY_TYPE, appId );
        em.setProperty( ref, ManagementServiceImpl.REGISTRATION_REQUIRES_EMAIL_CONFIRMATION, true );
    }


    private void enableAdminApproval( UUID appId ) throws Exception {
        EntityManager em = setup.getEmf().getEntityManager( appId );
        SimpleEntityRef ref = new SimpleEntityRef( Application.ENTITY_TYPE, appId );
        em.setProperty( ref, ManagementServiceImpl.REGISTRATION_REQUIRES_ADMIN_APPROVAL, true );
    }


    private User setupAppUser( UUID appId, String username, String email, boolean activated ) throws Exception {
        Mailbox.clearAll();

        EntityManager em = setup.getEmf().getEntityManager( appId );

        Map<String, Object> userProps = new LinkedHashMap<String, Object>();
        userProps.put( "username", username );
        userProps.put( "email", email );
        userProps.put( "activated", activated );

        User user = em.create( User.ENTITY_TYPE, User.class, userProps );
        setup.getEntityIndex().refresh();

        return user;
    }
}
