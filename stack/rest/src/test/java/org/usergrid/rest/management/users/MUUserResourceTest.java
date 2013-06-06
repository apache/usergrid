package org.usergrid.rest.management.users;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.usergrid.utils.MapUtils.hashMap;

import java.util.List;
import java.util.Map;

import javax.mail.Message;
import javax.ws.rs.core.MediaType;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.representation.Form;
import org.codehaus.jackson.JsonNode;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.management.UserInfo;
import org.usergrid.rest.AbstractRestTest;

/**
 * @author zznate
 */
public class MUUserResourceTest extends AbstractRestTest {

    private Logger logger = LoggerFactory.getLogger(MUUserResourceTest.class);

    @Test
    public void updateManagementUser() throws Exception {
        Map<String, String> payload = hashMap("email",
                "uort-user-1@apigee.com").map("username", "uort-user-1")
                .map("name", "Test User").map("password", "password")
                .map("organization", "uort-org")
                .map("company","Apigee");

        JsonNode node = resource().path("/management/organizations")
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .post(JsonNode.class, payload);
        logNode(node);
        String userId = node.get("data").get("owner").get("uuid").asText();

        assertEquals("Apigee",node.get("data").get("owner").get("properties").get("company").asText());

        String token = mgmtToken("uort-user-1@apigee.com","password");

        node = resource().path(String.format("/management/users/%s",userId))
                        .queryParam("access_token",token)
                        .type(MediaType.APPLICATION_JSON_TYPE)
                        .get(JsonNode.class);

        logNode(node);

        payload = hashMap("company","Usergrid");
        logger.info("sending PUT for company update");
        node = resource().path(String.format("/management/users/%s",userId))
                                .queryParam("access_token",token)
                                .type(MediaType.APPLICATION_JSON_TYPE)
                                .put(JsonNode.class, payload);
        assertNotNull(node);
        node = resource().path(String.format("/management/users/%s",userId))
                        .queryParam("access_token",token)
                        .type(MediaType.APPLICATION_JSON_TYPE)
                        .get(JsonNode.class);
        assertEquals("Usergrid",node.get("data").get("properties").get("company").asText());


        logNode(node);
    }

    @Test
    public void reactivateMultipleSend() throws Exception {

        JsonNode node = resource().path("/management/organizations")
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .post(JsonNode.class, buildOrgUserPayload("reactivate"));

        logNode(node);
        String email = node.get("data").get("owner").get("email").asText();
        String uuid = node.get("data").get("owner").get("uuid").asText();
        assertNotNull(email);
        assertEquals("MUUserResourceTest-reactivate@apigee.com", email);

        // reactivate should send activation email

        node = resource().path(String.format("/management/users/%s/reactivate",uuid))
                .queryParam("access_token",adminAccessToken)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .get(JsonNode.class);

        List<Message> inbox = org.jvnet.mock_javamail.Mailbox.get(email);

        assertFalse(inbox.isEmpty());
        logNode(node);
    }

    private Map<String, String> buildOrgUserPayload(String caller) {
        String className = this.getClass().getSimpleName();
        Map<String, String> payload = hashMap("email",
                String.format("%s-%s@apigee.com",className, caller))
                .map("username", String.format("%s-%s-user", className, caller))
                .map("name", String.format("%s %s", className, caller))
                .map("password", "password")
                .map("organization", String.format("%s-%s-org",className, caller));
        return payload;
    }

  @Test
  @Ignore("Scott, doens't run in maven build env.  Need to resolve jstl classloading issue")
  public void checkPasswordReset() throws Exception {

    String email = "test@usergrid.com";
    UserInfo userInfo = managementService.getAdminUserByEmail(email);
    String resetToken = managementService.getPasswordResetTokenForAdminUser(userInfo.getUuid(), 15000);

    assertTrue(managementService.checkPasswordResetTokenForAdminUser(userInfo.getUuid(), resetToken));

    Form formData = new Form();
    formData.add("token", resetToken);
    formData.add("password1", "sesame");
    formData.add("password2", "sesame");

    String html = resource()
        .path("/management/users/" + userInfo.getUsername() + "/resetpw")
        .type(MediaType.APPLICATION_FORM_URLENCODED_TYPE)
        .post(String.class, formData);

    assertTrue(html.contains("password set"));

    assertFalse(managementService.checkPasswordResetTokenForAdminUser(userInfo.getUuid(), resetToken));

    html = resource()
        .path("/management/users/" + userInfo.getUsername() + "/resetpw")
        .type(MediaType.APPLICATION_FORM_URLENCODED_TYPE)
        .post(String.class, formData);

    assertTrue(html.contains("invalid token"));
  }

}
