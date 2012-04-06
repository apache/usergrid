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

import static org.junit.Assert.assertEquals;
import static org.usergrid.utils.MapUtils.hashMap;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.core.MediaType;

import org.codehaus.jackson.JsonNode;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.persistence.entities.User;
import org.usergrid.persistence.exceptions.NoFullTextIndexException;
import org.usergrid.rest.AbstractRestTest;
import org.usergrid.utils.UUIDUtils;

import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;

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
    public void usernameQuery() {

        UserRepo.INSTANCE.load(resource());

        String ql = "username = 'user*'";

        JsonNode node = resource().path("/test-app/users").queryParam("ql", ql)
                .queryParam("access_token", access_token)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).get(JsonNode.class);

        assertEquals(UserRepo.INSTANCE.getByUserName("user1"),
                getIdFromSearchResults(node, 0));
        assertEquals(UserRepo.INSTANCE.getByUserName("user2"),
                getIdFromSearchResults(node, 1));
        assertEquals(UserRepo.INSTANCE.getByUserName("user3"),
                getIdFromSearchResults(node, 2));

    }
    
    @Test
    public void nameQuery() {

        UserRepo.INSTANCE.load(resource());

        String ql = "name = 'John*'";

        JsonNode node = resource().path("/test-app/users").queryParam("ql", ql)
                .queryParam("access_token", access_token)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).get(JsonNode.class);

        assertEquals(UserRepo.INSTANCE.getByUserName("user2"),
                getIdFromSearchResults(node, 0));
        assertEquals(UserRepo.INSTANCE.getByUserName("user3"),
                getIdFromSearchResults(node, 1));

    }
    
    
    @Test
    public void nameFullTextQuery() {

        UserRepo.INSTANCE.load(resource());

        String ql = "name contains 'Smith' order by name ";

        JsonNode node = resource().path("/test-app/users").queryParam("ql", ql)
                .queryParam("access_token", access_token)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).get(JsonNode.class);

        assertEquals(UserRepo.INSTANCE.getByUserName("user1"),
                getIdFromSearchResults(node, 0));
        assertEquals(UserRepo.INSTANCE.getByUserName("user2"),
                getIdFromSearchResults(node, 1));
        assertEquals(UserRepo.INSTANCE.getByUserName("user3"),
                getIdFromSearchResults(node, 2));

    }
    
    /**
     * Tests that when a full text index is run on a field that isn't full text indexed an error is thrown
     */
    @Test(expected=UniformInterfaceException.class)
    public void fullTextQueryNotFullTextIndexed() {

        UserRepo.INSTANCE.load(resource());

        String ql = "username contains 'user' ";

        JsonNode node = resource().path("/test-app/users").queryParam("ql", ql)
                .queryParam("access_token", access_token)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).get(JsonNode.class);

        

    }
    
    /**
     * Tests that when a full text index is run on a field that isn't full text indexed an error is thrown
     */
    @Test(expected=UniformInterfaceException.class)
    public void fullQueryNotIndexed() {

        UserRepo.INSTANCE.load(resource());

        String ql = "picture = 'foo' ";

        JsonNode node = resource().path("/test-app/users").queryParam("ql", ql)
                .queryParam("access_token", access_token)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).get(JsonNode.class);

        

    }


    private UUID getIdFromSearchResults(JsonNode rootNode, int index) {
        JsonNode entityArray = rootNode.get("entities");
        
        if(entityArray == null){
            return null;
        }
        
        JsonNode entity = entityArray.get(index);
        
        if(entity == null){
            return null;
        }
        
        return UUIDUtils.tryExtractUUID(entity
                .get("uuid").asText());

    }

    private enum UserRepo {
        INSTANCE;

        private Map<String, UUID> loaded = new HashMap<String, UUID>();

        public void load(WebResource resource) {
            if (loaded.size() > 0) {
                return;
            }

            createUser("user1", "user1@apigee.com", "user1", "Jane Smith 1", resource);
            createUser("user2", "user2@apigee.com", "user2", "John Smith 2", resource);
            createUser("user3", "user3@apigee.com", "user3", "John Smith 3", resource);

        }

        private void createUser(String username, String email,  String password,
                String fullName, WebResource resource) {

            Map<String, String> payload = hashMap("email", email)
                    .map("username", username).map("name", fullName)
                    .map("password", password).map("pin", "1234");

            UUID id = createUser(payload, resource);

           

            loaded.put(username, id);
        }

        public UUID getByUserName(String name) {
            return loaded.get(name);
        }

        /**
         * Create a user via the REST API and post it. Return the response
         * 
         * @param user
         */
        private UUID createUser(Map<String, String> payload,  WebResource resource) {

            JsonNode response = resource.path("/test-app/users")
                    .queryParam("access_token", access_token)
                    .accept(MediaType.APPLICATION_JSON)
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .post(JsonNode.class, payload);

            String idString = response.get("entities").get(0).get("uuid")
                    .asText();

            return UUIDUtils.tryExtractUUID(idString);

        }

    }
  

}
