package org.usergrid.rest.applications.users;

import com.sun.jersey.api.client.UniformInterfaceException;
import org.codehaus.jackson.JsonNode;
import org.junit.Rule;
import org.usergrid.rest.AbstractRestIT;

import javax.ws.rs.core.MediaType;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;
import com.sun.jersey.api.client.ClientResponse;
import org.usergrid.rest.TestContextSetup;
import org.usergrid.rest.test.resource.app.CustomEntity;

import static org.junit.Assert.assertEquals;
import static org.usergrid.utils.MapUtils.hashMap;

import org.usergrid.rest.test.resource.CustomCollection;

/**
 * // TODO: Document this
 *
 * @author ApigeeCorporation
 * @since 4.0
 */
public class ConnectionResourceTest extends AbstractRestIT
{
  @Rule
  public TestContextSetup context = new TestContextSetup( this );

  @Test
  public void connectionsQueryTest() {

    CustomEntity items = new CustomEntity("item", null);

    CustomCollection activities = context.collection("peeps");

    Map stuff = hashMap("type", "chicken");

    activities.create(stuff);


    Map<String, Object> payload = new LinkedHashMap<String, Object>();
    payload.put("username", "todd");

    Map<String, Object> objectOfDesire = new LinkedHashMap<String, Object>();
    objectOfDesire.put("codingmunchies", "doritoes");

    JsonNode node = resource().path("/test-organization/test-app/users")
        .queryParam("access_token", access_token)
        .accept(MediaType.APPLICATION_JSON)
        .type(MediaType.APPLICATION_JSON_TYPE)
        .post(JsonNode.class, payload);

    payload.put("username", "scott");


    node = resource().path("/test-organization/test-app/users")
        .queryParam("access_token", access_token)
        .accept(MediaType.APPLICATION_JSON)
        .type(MediaType.APPLICATION_JSON_TYPE)
        .post(JsonNode.class, payload);
    /*finish setting up the two users */


    ClientResponse toddWant = resource().path("/test-organization/test-app/users/todd/likes/peeps")
        .queryParam("access_token", access_token)
        .accept(MediaType.TEXT_HTML)
        .type(MediaType.APPLICATION_JSON_TYPE)
        .post(ClientResponse.class, objectOfDesire);

    assertEquals(200, toddWant.getStatus());

    node = resource().path("/test-organization/test-app/peeps")
        .queryParam("access_token", access_token)
        .accept(MediaType.APPLICATION_JSON)
        .type(MediaType.APPLICATION_JSON_TYPE)
        .get(JsonNode.class);

    String uuid = node.get("entities").get(0).get("uuid").getTextValue();




    try {
      node = resource().path("/test-organization/test-app/users/scott/likes/" + uuid)
          .queryParam("access_token", access_token)
          .accept(MediaType.APPLICATION_JSON)
          .type(MediaType.APPLICATION_JSON_TYPE)
          .get(JsonNode.class);
      assert (false);
    } catch (UniformInterfaceException uie) {
      assertEquals(404, uie.getResponse().getClientResponseStatus().getStatusCode());
      return;
    }

  }

}
