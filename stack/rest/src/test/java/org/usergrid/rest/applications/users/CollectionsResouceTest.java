package org.usergrid.rest.applications.users;

import com.sun.jersey.api.client.UniformInterfaceException;
import org.codehaus.jackson.JsonNode;
import org.junit.Before;
import org.junit.Test;
import org.usergrid.rest.AbstractRestTest;

import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.usergrid.utils.MapUtils.hashMap;

/**
 * @author zznate
 */
public class CollectionsResouceTest extends AbstractRestTest {

    @Test
    public void postToBadPath() {
        Map<String, String> payload = hashMap("name", "Austin").map("state", "TX");
        JsonNode node = null;
        try {
            node = resource().path("/test-organization/test-organization/test-app/cities")
                    .queryParam("access_token", access_token).accept(MediaType.APPLICATION_JSON)
                    .type(MediaType.APPLICATION_JSON_TYPE).post(JsonNode.class, payload);
        } catch (UniformInterfaceException e) {
            assertEquals("Should receive a 400 Not Found", 400, e.getResponse().getStatus());
        }
    }

    /**
     * emails with "me" in them are causing errors. Test we can post to a
     * colleciton after creating a user with this email
     * 
     * USERGRID-689
     * 
     * @throws Exception
     */
    @Test
    public void emailWithMeInString() throws Exception {
        // user is created get a token
        createUser("sumeet.agarwal@usergrid.com", "sumeet.agarwal@usergrid.com", "secret", "Sumeet Agarwal");

        String token = userToken("sumeet.agarwal@usergrid.com", "secret");

   
        //create a permission with the path "me" in it
        Map<String, String> data = new HashMap<String, String>();
        
        data.put("permission", "get,post,put,delete:/users/sumeet.agarwal@usergrid.com/**");
    
        JsonNode posted = resource().path("/test-organization/test-app/users/sumeet.agarwal@usergrid.com/permissions").queryParam("access_token", token)
                .accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON_TYPE).post(JsonNode.class, data);

        
        //now post data
        data = new HashMap<String, String>();
                
        data.put("name", "profile-sumeet");
        data.put("firstname", "sumeet");
        data.put("lastname", "agarwal");
        data.put("mobile", "122");

        
        
        posted = resource().path("/test-organization/test-app/nestprofiles").queryParam("access_token", token)
                .accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON_TYPE).post(JsonNode.class, data);

        JsonNode response = resource().path("/test-organization/test-app/nestprofiles")
                .queryParam("access_token", token).accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).get(JsonNode.class);

        assertNotNull(getEntity(response, 0));

    }
}
