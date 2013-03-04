package org.usergrid.rest.management;

import static org.junit.Assert.*;
import static org.usergrid.management.AccountCreationProps.PROPERTIES_ADMIN_RESETPW_URL;
import static org.usergrid.management.AccountCreationProps.PROPERTIES_ADMIN_USERS_REQUIRE_CONFIRMATION;
import static org.usergrid.management.AccountCreationProps.PROPERTIES_SYSADMIN_APPROVES_ADMIN_USERS;
import static org.usergrid.management.AccountCreationProps.PROPERTIES_SYSADMIN_APPROVES_ORGANIZATIONS;
import static org.usergrid.management.AccountCreationProps.PROPERTIES_SYSADMIN_EMAIL;
import static org.usergrid.utils.MapUtils.hashMap;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.MimeMultipart;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import com.sun.jersey.api.client.ClientResponse;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.junit.Ignore;
import org.junit.Test;
import org.jvnet.mock_javamail.Mailbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.management.UserInfo;
import org.usergrid.persistence.cassandra.CassandraService;
import org.usergrid.persistence.entities.User;
import org.usergrid.rest.AbstractRestTest;

import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.core.util.MultivaluedMapImpl;

public class RegistrationTest extends AbstractRestTest {

    private static final Logger logger = LoggerFactory
            .getLogger(RegistrationTest.class);

    @Ignore
    @Test
    public void postCreateOrgAndAdmin() throws Exception {

        properties.setProperty(PROPERTIES_SYSADMIN_APPROVES_ADMIN_USERS,
                "false");
        properties.setProperty(PROPERTIES_SYSADMIN_APPROVES_ORGANIZATIONS,
                "false");
        properties.setProperty(PROPERTIES_ADMIN_USERS_REQUIRE_CONFIRMATION,
                "true");
        properties.setProperty(PROPERTIES_SYSADMIN_EMAIL,
                "sysadmin-1@mockserver.com");

        JsonNode node = postCreateOrgAndAdmin("test-org-1", "test-user-1",
                "Test User", "test-user-1@mockserver.com", "testpassword");

        UUID owner_uuid = UUID.fromString(node.findPath("data")
                .findPath("owner").findPath("uuid").getTextValue());

        List<Message> inbox = org.jvnet.mock_javamail.Mailbox
                .get("test-user-1@mockserver.com");

        assertFalse(inbox.isEmpty());

        Message account_confirmation_message = inbox.get(0);
        assertEquals("User Account Confirmation: test-user-1@mockserver.com",
                account_confirmation_message.getSubject());

        String token = getTokenFromMessage(account_confirmation_message);
        logger.info(token);


        managementService.disableAdminUser(owner_uuid);
        try{
            resource().path("/management/token")
                .queryParam("grant_type", "password")
                .queryParam("username", "test-user-1")
                .queryParam("password", "testpassword")
                .accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON_TYPE).get(JsonNode.class);
            fail("request for disabled user should fail");
        } catch(UniformInterfaceException uie) {
            ClientResponse.Status status = uie.getResponse().getClientResponseStatus();
            JsonNode body = uie.getResponse().getEntity(JsonNode.class);
            assertEquals("user disabled", body.findPath("error_description").getTextValue());
        }


        managementService.deactivateUser(CassandraService.MANAGEMENT_APPLICATION_ID, owner_uuid);
        try{
            resource().path("/management/token")
                .queryParam("grant_type", "password")
                .queryParam("username", "test-user-1")
                .queryParam("password", "testpassword")
                .accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON_TYPE).get(JsonNode.class);
            fail("request for deactivated user should fail");
        } catch(UniformInterfaceException uie) {
            ClientResponse.Status status = uie.getResponse().getClientResponseStatus();
            JsonNode body = uie.getResponse().getEntity(JsonNode.class);
            assertEquals("user not activated", body.findPath("error_description").getTextValue());
        }


        // assertEquals(ActivationState.ACTIVATED,
        // managementService.handleConfirmationTokenForAdminUser(
        // owner_uuid, token));

        String response = resource().path(
                "/management/users/" + owner_uuid + "/confirm").get(
                String.class);
        logger.info(response);

        Message account_activation_message = inbox.get(1);
        assertEquals("User Account Activated",
                account_activation_message.getSubject());

    }

    public String getTokenFromMessage(Message msg) throws IOException,
            MessagingException {
        String body = ((MimeMultipart) msg.getContent()).getBodyPart(0)
                .getContent().toString();
        String token = StringUtils.substringAfterLast(body, "token=");
        // TODO better token extraction
        // this is going to get the wrong string if the first part is not
        // text/plain and the url isn't the last character in the email
        return token;
    }

    public JsonNode postCreateOrgAndAdmin(String organizationName,
            String username, String name, String email, String password) {
        JsonNode node = null;
        Map<String, String> payload = hashMap("email", email)
                .map("username", username).map("name", name)
                .map("password", password)
                .map("organization", organizationName);

        node = resource().path("/management/organizations")
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .post(JsonNode.class, payload);

        assertNotNull(node);
        logNode(node);
        return node;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public JsonNode postAddAdminToOrg(String organizationName, String email,
            String password, String token) {
        JsonNode node = null;

        MultivaluedMap formData = new MultivaluedMapImpl();
        formData.add("email", email);
        formData.add("password", password);

        node = resource()
                .path("/management/organizations/" + organizationName
                        + "/users").queryParam("access_token", token)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_FORM_URLENCODED)
                .post(JsonNode.class, formData);

        assertNotNull(node);
        logNode(node);
        return node;
    }

    @Test
    public void putAddToOrganizationFail() throws Exception {

        properties.setProperty(PROPERTIES_SYSADMIN_APPROVES_ADMIN_USERS,
                "false");
        properties.setProperty(PROPERTIES_SYSADMIN_APPROVES_ORGANIZATIONS,
                "false");
        properties.setProperty(PROPERTIES_ADMIN_USERS_REQUIRE_CONFIRMATION,
                "false");
        properties.setProperty(PROPERTIES_SYSADMIN_EMAIL,
                "sysadmin-1@mockserver.com");

        String t = adminToken();
        MultivaluedMap formData = new MultivaluedMapImpl();
        formData.add("foo", "bar");
        try {
            resource()
                    .path("/management/organizations/test-organization/users/test-admin-null@mockserver.com")
                    .queryParam("access_token", t)
                    .accept(MediaType.APPLICATION_JSON)
                    .type(MediaType.APPLICATION_FORM_URLENCODED)
                    .put(JsonNode.class, formData);
        } catch (UniformInterfaceException e) {
            assertEquals("Should receive a 400 Not Found", 400, e.getResponse()
                    .getStatus());
        }
    }

    @Test
    public void postAddToOrganization() throws Exception {


        properties.setProperty(PROPERTIES_SYSADMIN_APPROVES_ADMIN_USERS,
                "false");
        properties.setProperty(PROPERTIES_SYSADMIN_APPROVES_ORGANIZATIONS,
                "false");
        properties.setProperty(PROPERTIES_ADMIN_USERS_REQUIRE_CONFIRMATION,
                "false");
        properties.setProperty(PROPERTIES_SYSADMIN_EMAIL,
                "sysadmin-1@mockserver.com");

        String t = adminToken();
        postAddAdminToOrg("test-organization", "test-admin@mockserver.com",
                "password", t);

    }

    @Test
    public void addNewAdminUserWithNoPwdToOrganization() throws Exception {

    	Mailbox.clearAll();
        properties.setProperty(PROPERTIES_SYSADMIN_APPROVES_ADMIN_USERS,
                "false");
        properties.setProperty(PROPERTIES_SYSADMIN_APPROVES_ORGANIZATIONS,
                "false");
        properties.setProperty(PROPERTIES_ADMIN_USERS_REQUIRE_CONFIRMATION,
                "false");
        properties.setProperty(PROPERTIES_SYSADMIN_EMAIL,
                "sysadmin-1@mockserver.com");

        // this should send resetpwd  link in email to newly added org admin user(that did not exist in usergrid)
        // and "User Invited To Organization" email
    	String adminToken = adminToken();
        JsonNode node = postAddAdminToOrg("test-organization", "test-admin-nopwd@mockserver.com","", adminToken);
        String uuid = node.get("data").get("user").get("uuid").getTextValue();
        UUID userId = UUID.fromString(uuid);

        String subject = "Password Reset";
        String reset_url = String.format(properties.getProperty(PROPERTIES_ADMIN_RESETPW_URL), uuid);
        String invited = "User Invited To Organization";

	    Message[] msgs = getMessages("mockserver.com","test-admin-nopwd",  "password");

	    // 1 Invite and 1 resetpwd
	    assertTrue(msgs.length == 2);

	    //email subject
	    assertEquals(subject, msgs[0].getSubject());
	    assertEquals(invited, msgs[1].getSubject());

	    // reseturl
	    String mailContent = (String)((MimeMultipart)msgs[0].getContent()).getBodyPart(1).getContent();
	    logger.info(mailContent);
	    assertTrue(StringUtils.contains(mailContent, reset_url));

	    //token
	    String token = getTokenFromMessage(msgs[0]);
	    assertTrue(managementService.checkPasswordResetTokenForAdminUser(userId, token));
    }

    @Test
    public void addExistingAdminUserToOrganization() throws Exception {

    	Mailbox.clearAll();
        properties.setProperty(PROPERTIES_SYSADMIN_APPROVES_ADMIN_USERS,
                "false");
        properties.setProperty(PROPERTIES_SYSADMIN_APPROVES_ORGANIZATIONS,
                "false");
        properties.setProperty(PROPERTIES_ADMIN_USERS_REQUIRE_CONFIRMATION,
                "false");
        properties.setProperty(PROPERTIES_SYSADMIN_EMAIL,
                "sysadmin-1@mockserver.com");

        // setup an admin user
        String adminUserEmail = "AdminUserFromOtherOrg@otherorg.com";
		UserInfo adminUser = managementService.createAdminUser(adminUserEmail, adminUserEmail, adminUserEmail, "password1",
				true, false);
		assertNotNull(adminUser);
        Message[] msgs = getMessages("otherorg.com","AdminUserFromOtherOrg",  "password1");
        assertEquals(1,msgs.length);
		// add existing admin user to org
        // this should NOT send resetpwd  link in email to newly added org admin user(that already exists in usergrid)
		// only "User Invited To Organization" email
    	String adminToken = adminToken();
        JsonNode node = postAddAdminToOrg("test-organization", "AdminUserFromOtherOrg@otherorg.com", "password1", adminToken);
        String uuid = node.get("data").get("user").get("uuid").getTextValue();
        UUID userId = UUID.fromString(uuid);

        assertEquals(adminUser.getUuid(), userId);

        String resetpwd = "Password Reset";
        String invited = "User Invited To Organization";

        msgs = getMessages("otherorg.com","AdminUserFromOtherOrg",  "password1");

	    // only 1 invited msg
	    assertEquals(2,msgs.length);

	    //email subject
	    assertNotSame(resetpwd, msgs[1].getSubject());
	    assertEquals(invited, msgs[1].getSubject());
    }

    private Message[] getMessages(String host, String user, String password) throws MessagingException, IOException {

    	Session session = Session.getDefaultInstance(new Properties());
	    Store store = session.getStore("imap");
	    store.connect(host,user,  password);

	    Folder folder = store.getFolder("inbox");
	    folder.open(Folder.READ_ONLY);
	    Message[] msgs = folder.getMessages();

		for (Message m : msgs) {
			logger.info("Subject: " + m.getSubject());
			logger.info("Body content 0 " +(String)((MimeMultipart)m.getContent()).getBodyPart(0).getContent());
			logger.info("Body content 1 " +(String)((MimeMultipart)m.getContent()).getBodyPart(1).getContent());
		}
		return msgs;

    }

}
