package org.usergrid.rest.management;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.usergrid.management.AccountCreationProps.PROPERTIES_ADMIN_USERS_REQUIRE_CONFIRMATION;
import static org.usergrid.management.AccountCreationProps.PROPERTIES_SYSADMIN_APPROVES_ADMIN_USERS;
import static org.usergrid.management.AccountCreationProps.PROPERTIES_SYSADMIN_APPROVES_ORGANIZATIONS;
import static org.usergrid.management.AccountCreationProps.PROPERTIES_SYSADMIN_EMAIL;
import static org.usergrid.utils.MapUtils.hashMap;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

        String t = mgmtToken();
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

        String t = mgmtToken();
        postAddAdminToOrg("test-organization", "test-admin@mockserver.com",
                "password", t);

    }

}
