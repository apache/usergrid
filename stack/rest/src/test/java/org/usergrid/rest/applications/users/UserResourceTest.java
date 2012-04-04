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
import org.usergrid.persistence.entities.User;
import org.usergrid.rest.AbstractRestTest;
import org.usergrid.utils.JsonUtils;
import org.usergrid.utils.UUIDUtils;

import javax.ws.rs.core.MediaType;

import java.util.Map;
import java.util.UUID;

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
  
  @Test
  public void testUserQuery() {
    UUID user1 = createUser("user1", "user1", "Jane Smith 1");
    UUID user2 = createUser("user2", "user2", "John Smith 2");
    UUID user3 = createUser("user3", "user3", "John Smith 3");
    
    String ql = "username = 'user*'";
    
    JsonNode node = resource().path("/test-app/users")
            .queryParam("ql", ql)
            .queryParam("access_token", access_token)
            .accept(MediaType.APPLICATION_JSON)
            .type(MediaType.APPLICATION_JSON_TYPE)
            .get(JsonNode.class);
    
    JsonNode entities = node.get("entities");
    
    
    assertEquals(user1, UUIDUtils.tryExtractUUID(entities.get(0).get("uuid").asText()));
    assertEquals(user2, UUIDUtils.tryExtractUUID(entities.get(1).get("uuid").asText()));
    assertEquals(user3, UUIDUtils.tryExtractUUID(entities.get(2).get("uuid").asText()));
    
    
  }
  
  private UUID createUser(String username, String password, String fullName){
      User user = new User();
      user.setUsername(username);
      user.setProperty("password", password);
      user.setName(fullName);
      
      return createUser(user);
  }
  
  /**
   * Create a user via the REST API and post it.  Return the response
   * @param user
   */
  private UUID createUser(User user){
      
   
      JsonNode response = resource().path("/test-app/users")
              .queryParam("access_token", access_token)
              .accept(MediaType.APPLICATION_JSON)
              .type(MediaType.APPLICATION_JSON_TYPE)
              .post(JsonNode.class, user);
     
      
      
      
      
      String idString= response.get("entities").get(0).get("uuid").asText();
      
      return UUIDUtils.tryExtractUUID(idString);
              
              
      
      
  }


}
