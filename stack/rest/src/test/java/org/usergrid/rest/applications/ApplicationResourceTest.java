package org.usergrid.rest.applications;

import org.codehaus.jackson.JsonNode;
import org.junit.Test;
import org.usergrid.rest.AbstractRestTest;

import javax.ws.rs.core.MediaType;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.usergrid.utils.MapUtils.hashMap;

/**
 * Invokes methods on ApplicationResource
 * @author zznate
 */
public class ApplicationResourceTest extends AbstractRestTest {

  @Test
  public void test_GET_credentials_ok() {
    String mgmtToken = mgmtToken();

    JsonNode node = resource().path("/test-app/credentials")
            .queryParam("access_token", mgmtToken)
            .accept(MediaType.APPLICATION_JSON)
            .type(MediaType.APPLICATION_JSON_TYPE)
            .get(JsonNode.class);
    assertEquals("ok", node.get("status").getTextValue());
    logNode(node);
  }

}
