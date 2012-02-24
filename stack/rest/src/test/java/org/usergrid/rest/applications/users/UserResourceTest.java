package org.usergrid.rest.applications.users;

import com.sun.jersey.api.client.UniformInterfaceException;
import org.codehaus.jackson.JsonNode;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.rest.AbstractRestTest;

import javax.ws.rs.core.MediaType;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.usergrid.utils.MapUtils.hashMap;

/**
 * @author zznate
 */
public class UserResourceTest extends AbstractRestTest {
  private static Logger log = LoggerFactory.getLogger(UserResourceTest.class);

  private static boolean userInited = false;

  public UserResourceTest() throws Exception {
    super();
  }

  @Test(expected = UniformInterfaceException.class)
  public void test_PUT_password_fail() {
    Map<String, String> payload = hashMap("password", "sesame1").map("oldpassword", "sesame");

    JsonNode node = resource().path("/test-app/users/ed@anuff.com/password")
            .queryParam("access_token", access_token)
            .accept(MediaType.APPLICATION_JSON)
            .type(MediaType.APPLICATION_JSON_TYPE)
            .put(JsonNode.class, payload);
  }

  @Test
  public void test_PUT_password_ok() {
    Map<String, String> payload = hashMap("newpassword", "sesame1").map("oldpassword", "sesame");

    JsonNode node = resource().path("/test-app/users/ed@anuff.com/password")
            .queryParam("access_token", access_token)
            .accept(MediaType.APPLICATION_JSON)
            .type(MediaType.APPLICATION_JSON_TYPE)
            .put(JsonNode.class, payload);
    logNode(node);
  }


}
