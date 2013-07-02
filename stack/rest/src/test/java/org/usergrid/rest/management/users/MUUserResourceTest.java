package org.usergrid.rest.management.users;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.usergrid.utils.MapUtils.hashMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.mail.Message;
import javax.ws.rs.core.MediaType;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.representation.Form;
import org.codehaus.jackson.JsonNode;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.management.OrganizationInfo;
import org.usergrid.management.UserInfo;
import org.usergrid.rest.AbstractRestTest;
import org.usergrid.rest.management.organizations.OrganizationsResource;

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
  public void getUser() throws Exception {

    // set an organization property
    HashMap<String,Object> payload = new HashMap<String,Object>();
    Map<String, Object> properties = new HashMap<String,Object>();
    properties.put("securityLevel", 5);
    payload.put(OrganizationsResource.ORGANIZATION_PROPERTIES, properties);
    JsonNode node = resource().path("/management/organizations/test-organization")
        .queryParam("access_token", superAdminToken())
        .accept(MediaType.APPLICATION_JSON)
        .type(MediaType.APPLICATION_JSON_TYPE)
        .put(JsonNode.class, payload);

    // ensure the organization property is included
    node = resource().path("/management/users/test@usergrid.com")
        .queryParam("access_token",adminAccessToken)
        .accept(MediaType.APPLICATION_JSON)
        .type(MediaType.APPLICATION_JSON_TYPE)
        .get(JsonNode.class);
    logNode(node);

    JsonNode securityLevel = node.findValue("securityLevel");
    assertNotNull(securityLevel);
    assertEquals(5L, securityLevel.asLong());
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
  @Ignore("Doesn't run in maven build env.  Need to resolve jstl classloading issue")
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

  @Test
  public void checkPasswordHistoryConflict() throws Exception {

    String[] passwords = new String[] {"password1", "password2", "password3", "password4"};

    UserInfo user = managementService.createAdminUser("edanuff", "Ed Anuff", "ed@anuff.com", passwords[0], true, false);
    assertNotNull(user);

    OrganizationInfo organization = managementService.createOrganization("ed-organization", user, true);
    assertNotNull(organization);

    // set history to 1
    Map<String,Object> props = new HashMap<String,Object>();
    props.put(OrganizationInfo.PASSWORD_HISTORY_SIZE_KEY, 1);
    organization.setProperties(props);
    managementService.updateOrganization(organization);

    UserInfo userInfo = managementService.getAdminUserByEmail("ed@anuff.com");

    Map<String, String> payload =
        hashMap("oldpassword", passwords[0])
           .map("newpassword", passwords[0]); // fail

    try {
      JsonNode node = resource()
          .path("/management/users/edanuff/password")
          .accept(MediaType.APPLICATION_JSON)
          .type(MediaType.APPLICATION_JSON_TYPE)
          .post(JsonNode.class, payload);
      fail("should fail with conflict");
    } catch (UniformInterfaceException e) {
      assertEquals(409, e.getResponse().getStatus());
    }

    payload.put("newpassword", passwords[1]); // ok
    JsonNode node = resource()
        .path("/management/users/edanuff/password")
        .accept(MediaType.APPLICATION_JSON)
        .type(MediaType.APPLICATION_JSON_TYPE)
        .post(JsonNode.class, payload);
    payload.put("oldpassword", passwords[1]);

    payload.put("newpassword", passwords[0]); // fail
    try {
      node = resource()
          .path("/management/users/edanuff/password")
          .accept(MediaType.APPLICATION_JSON)
          .type(MediaType.APPLICATION_JSON_TYPE)
          .post(JsonNode.class, payload);
      fail("should fail with conflict");
    } catch (UniformInterfaceException e) {
      assertEquals(409, e.getResponse().getStatus());
    }
  }

  @Test
  @Ignore("because of that jstl classloader error thing")
  public void checkPasswordChangeTime() throws Exception {

    String email = "test@usergrid.com";
    UserInfo userInfo = managementService.getAdminUserByEmail(email);
    String resetToken = managementService.getPasswordResetTokenForAdminUser(userInfo.getUuid(), 15000);

    Form formData = new Form();
    formData.add("token", resetToken);
    formData.add("password1", "sesame");
    formData.add("password2", "sesame");

    String html = resource()
        .path("/management/users/" + userInfo.getUsername() + "/resetpw")
        .type(MediaType.APPLICATION_FORM_URLENCODED_TYPE)
        .post(String.class, formData);
    assertTrue(html.contains("password set"));

    JsonNode node = resource().path("/management/token")
        .queryParam("grant_type", "password")
        .queryParam("username", email)
        .queryParam("password", "sesame")
        .accept(MediaType.APPLICATION_JSON)
        .get(JsonNode.class);

    Long changeTime = node.get("passwordChanged").getLongValue();
    assertTrue(System.currentTimeMillis() - changeTime < 2000);

    Map<String, String> payload =
        hashMap("oldpassword", "sesame")
            .map("newpassword", "test");
    node = resource()
        .path("/management/users/" + userInfo.getUsername() + "/password")
        .accept(MediaType.APPLICATION_JSON)
        .type(MediaType.APPLICATION_JSON_TYPE)
        .post(JsonNode.class, payload);

    node = resource().path("/management/token")
        .queryParam("grant_type", "password")
        .queryParam("username", email)
        .queryParam("password", "test")
        .accept(MediaType.APPLICATION_JSON)
        .get(JsonNode.class);

    Long changeTime2 = node.get("passwordChanged").getLongValue();
    assertTrue(changeTime < changeTime2);
    assertTrue(System.currentTimeMillis() - changeTime2 < 2000);

    node = resource().path("/management/me")
        .queryParam("grant_type", "password")
        .queryParam("username", email)
        .queryParam("password", "test")
        .accept(MediaType.APPLICATION_JSON)
        .get(JsonNode.class);

    Long changeTime3 = node.get("passwordChanged").getLongValue();
    assertEquals(changeTime2, changeTime3);
  }
}
