package org.usergrid.rest.applications.users;

import com.sun.jersey.api.client.UniformInterfaceException;
import org.codehaus.jackson.JsonNode;
import org.junit.Before;
import org.junit.Test;
import org.usergrid.rest.AbstractRestTest;

import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.usergrid.utils.MapUtils.hashMap;

/**
 * @author zznate
 */
public class CollectionsResouceTest extends AbstractRestTest {

  @Test
  public void postToBadPath() {
    Map<String,String> payload = hashMap("name", "Austin")
    				.map("state", "TX");
    JsonNode node = null;
    try {
      node = resource().path("/test-organization/test-organization/test-app/cities")
              .queryParam("access_token", access_token)
              .accept(MediaType.APPLICATION_JSON)
              .type(MediaType.APPLICATION_JSON_TYPE)
              .post(JsonNode.class, payload);
    } catch (UniformInterfaceException e) {
      assertEquals("Should receive a 400 Not Found", 400, e
              .getResponse().getStatus());


    }
    //logNode(node);
    //assertEquals("required_property_not_found",node.get("error").getTextValue()); //
    //assertTrue(node.get("exception").getTextValue().contains("RequiredPropertyNotFoundException")); // when should we leave this out
  }
}
