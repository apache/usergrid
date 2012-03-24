/*******************************************************************************
 * Copyright 2012 Apigee Corporation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.usergrid.rest.applications.users;

import com.sun.jersey.api.client.UniformInterfaceException;
import org.codehaus.jackson.JsonNode;
import org.junit.Before;
import org.junit.Ignore;
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

  @Test
  public void test_GET_user_ok() {
    // TODO figure out what is being overridden? why 400?
    JsonNode node = resource().path("/test-app/users")
            .queryParam("access_token", access_token)
            .accept(MediaType.APPLICATION_JSON)
            .type(MediaType.APPLICATION_JSON_TYPE)
            .get(JsonNode.class);
    String uuid = node.get("entities").get(0).get("uuid").getTextValue();

    node = resource().path("/test-app/users/"+ uuid)
                .queryParam("access_token", access_token)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .get(JsonNode.class);
    logNode(node);
    assertEquals("ed@anuff.com",node.get("entities").get(0).get("email").getTextValue());
  }

}
