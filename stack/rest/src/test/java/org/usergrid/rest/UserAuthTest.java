package org.usergrid.rest;

import com.sun.jersey.api.client.UniformInterfaceException;
import org.codehaus.jackson.JsonNode;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.usergrid.utils.MapUtils.hashMap;

/**
 * @author zznate
 */
public class UserAuthTest extends AbstractRestTest {
  private static Logger log = LoggerFactory.getLogger(UserAuthTest.class);

  private static boolean userInited = false;
  private static String access_token;

  public UserAuthTest() throws Exception {
    super();
  }

  @Test(expected = UniformInterfaceException.class)
  public void testPasswordChangeFail() {
    Map<String, String> payload = hashMap("password", "sesame1").map("oldpassword", "sesame");

    JsonNode node = resource().path("/test-app/users/ed@anuff.com/password")
            .queryParam("access_token", access_token)
            .accept(MediaType.APPLICATION_JSON)
            .type(MediaType.APPLICATION_JSON_TYPE)
            .put(JsonNode.class, payload);

  }

  @Test
  public void testPasswordChangeOk() {
    Map<String, String> payload = hashMap("newpassword", "sesame1").map("oldpassword", "sesame");

    JsonNode node = resource().path("/test-app/users/ed@anuff.com/password")
            .queryParam("access_token", access_token)
            .accept(MediaType.APPLICATION_JSON)
            .type(MediaType.APPLICATION_JSON_TYPE)
            .put(JsonNode.class, payload);
    logNode(node);

  }

  @Before
  public void setupLocal() {
    if ( userInited )
      return;
    JsonNode node = resource().path("/management/token")
    				.queryParam("grant_type", "password")
    				.queryParam("username", "test@usergrid.com")
    				.queryParam("password", "test")
    				.accept(MediaType.APPLICATION_JSON).get(JsonNode.class);
    String mgmToken = node.get("access_token").getTextValue();

    Map<String, String> payload = hashMap("email", "ed@anuff.com")
            .map("username", "edanuff").map("name", "Ed Anuff")
            .map("password", "sesame").map("pin", "1234");

    node = resource().path("/test-app/users")
            .queryParam("access_token", mgmToken)
            .accept(MediaType.APPLICATION_JSON)
            .type(MediaType.APPLICATION_JSON_TYPE)
            .post(JsonNode.class, payload);
    userInited = true;
    access_token = acquireToken();

  }

  private String acquireToken() {
    JsonNode node = resource().path("/test-app/token")
                .queryParam("grant_type", "password")
                .queryParam("username", "ed@anuff.com")
                .queryParam("password", "sesame")
                .accept(MediaType.APPLICATION_JSON).get(JsonNode.class);

     return node.get("access_token").getTextValue();
  }
}
