package org.usergrid.rest.applications.collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.usergrid.utils.JsonUtils.loadJsonFromResourceFile;
import static org.usergrid.utils.MapUtils.hashMap;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.core.MediaType;

import org.junit.Assert;
import org.junit.Test;
import org.usergrid.rest.AbstractRestTest;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.jersey.api.client.UniformInterfaceException;

/**
 * @author zznate
 * @author tnine
 */
public class CollectionsResourceTest extends AbstractRestTest {

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

    @Test
    public void postToEmptyCollection() {
        Map<String, String> payload = new HashMap<String, String>();

        JsonNode node = resource().path("/test-organization/test-app/cities")
                    .queryParam("access_token", access_token).accept(MediaType.APPLICATION_JSON)
                    .type(MediaType.APPLICATION_JSON_TYPE).post(JsonNode.class, payload);
        assertNull(getEntity(node, 0));
        assertNull(node.get("count"));
    }


    @Test
    public void stringWithSpaces() {
        Map<String, String> payload = hashMap("summaryOverview", "My Summary").map("caltype", "personal");

        JsonNode node = resource().path("/test-organization/test-app/calendarlists")
                    .queryParam("access_token", access_token).accept(MediaType.APPLICATION_JSON)
                    .type(MediaType.APPLICATION_JSON_TYPE).post(JsonNode.class, payload);


        UUID id = getEntityId(node, 0);

        //post a second entity


        payload = hashMap("summaryOverview", "Your Summary").map("caltype", "personal");

        node = resource().path("/test-organization/test-app/calendarlists")
                .queryParam("access_token", access_token).accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).post(JsonNode.class, payload);


        //query for the first entity

        String query = "summaryOverview = 'My Summary'";


        JsonNode queryResponse =
                resource().path("/test-organization/test-app/calendarlists")
                .queryParam("access_token", access_token).queryParam("ql", query).accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).get(JsonNode.class);


        UUID returnedId = getEntityId(queryResponse, 0);

        assertEquals(id, returnedId);

        assertEquals(1, queryResponse.get("entities").size());
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testCollectionSchema() throws Exception {
      Map<String, Object> payload = loadJsonFromResourceFile(CollectionsResourceTest.class, Map.class, "cat-schema.json");

      JsonNode node = resource().path("/test-organization/test-app/cats/schema").queryParam("access_token", access_token)
          .accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON_TYPE).put(JsonNode.class, payload);

      try {
        payload = loadJsonFromResourceFile(CollectionsResourceTest.class, Map.class, "bad-schema.json");
        
        node = resource().path("/test-organization/test-app/cats/schema").queryParam("access_token", access_token)
            .accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON_TYPE).put(JsonNode.class, payload);

        Assert.fail();
      } catch (UniformInterfaceException iae) {
        // ok
      }

      try {
        payload = new LinkedHashMap<String, Object>();
        properties.put("name", "Tom");

        node = resource().path("/test-organization/test-app/cats").queryParam("access_token", access_token)
            .accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON_TYPE).put(JsonNode.class, payload);
        Assert.fail();
      } catch (UniformInterfaceException iae) {
        // ok
      }

      payload = new LinkedHashMap<String, Object>();
      payload.put("name", "Tom");
      payload.put("color", "tabby");

      node = resource().path("/test-organization/test-app/cats").queryParam("access_token", access_token)
          .accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON_TYPE).put(JsonNode.class, payload);
      assertNotNull(node);

    }
    
    
    
    
}
