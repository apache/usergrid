package org.usergrid.rest.applications.assets;

import org.codehaus.jackson.JsonNode;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.rest.AbstractRestTest;
import org.usergrid.rest.applications.utils.UserRepo;

import javax.ws.rs.core.MediaType;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertNotNull;
import static org.usergrid.utils.MapUtils.hashMap;

/**
 * @author zznate
 */
public class AssetResourceTest extends AbstractRestTest {

  private Logger logger = LoggerFactory.getLogger(AssetResourceTest.class);

  @Test
  public void verifyBinaryCrud() throws Exception {
    UserRepo.INSTANCE.load(resource(), access_token);

    UUID userId = UserRepo.INSTANCE.getByUserName("user1");
    Map<String, String> payload = hashMap("path", "/path/to/asset")
            .map("name", "assetname")
            .map("owner",userId.toString())
            .map("someprop", "somevalue");

    JsonNode node = resource().path("/test-organization/test-app/assets")
            .queryParam("access_token", access_token)
            .accept(MediaType.APPLICATION_JSON)
            .type(MediaType.APPLICATION_JSON_TYPE)
            .post(JsonNode.class, payload);

    JsonNode idNode = node.get("entities").get(0).get("uuid");
    assertNotNull(idNode.getTextValue());

    logNode(node);
  }

}
