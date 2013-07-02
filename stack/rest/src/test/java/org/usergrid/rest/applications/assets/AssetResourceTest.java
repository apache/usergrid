package org.usergrid.rest.applications.assets;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.usergrid.utils.MapUtils.hashMap;

import java.io.InputStream;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.core.MediaType;

import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.JsonNode;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.rest.AbstractRestTest;
import org.usergrid.rest.applications.utils.UserRepo;

/**
 * @author zznate
 */
public class AssetResourceTest extends AbstractRestTest {

  private Logger logger = LoggerFactory.getLogger(AssetResourceTest.class);

  @Test
  public void verifyBinaryCrud() throws Exception {
    UserRepo.INSTANCE.load(resource(), access_token);

    UUID userId = UserRepo.INSTANCE.getByUserName("user1");
    Map<String, String> payload = hashMap("path", "my/clean/path")
            .map("name", "assetname")
            .map("owner",userId.toString())
            .map("someprop", "somevalue");

    JsonNode node = resource().path("/test-organization/test-app/assets")
            .queryParam("access_token", access_token)
            .accept(MediaType.APPLICATION_JSON)
            .type(MediaType.APPLICATION_JSON_TYPE)
            .post(JsonNode.class, payload);
    JsonNode idNode = node.get("entities").get(0).get("uuid");
    UUID id = UUID.fromString(idNode.getTextValue());
    assertNotNull(idNode.getTextValue());
    logNode(node);

    byte[] data = IOUtils.toByteArray(this.getClass().getResourceAsStream("/cassandra_eye.jpg"));
    resource().path("/test-organization/test-app/assets/" + id.toString() + "/data")
                .queryParam("access_token", access_token)
                .type(MediaType.APPLICATION_OCTET_STREAM_TYPE)
                .put(data);

    InputStream is = resource().path("/test-organization/test-app/assets/" + id.toString() + "/data")
                    .queryParam("access_token", access_token)
                    .get(InputStream.class);

    byte[] foundData = IOUtils.toByteArray(is);
    assertEquals(7979, foundData.length);

    node = resource().path("/test-organization/test-app/assets/my/clean/path")
                        .queryParam("access_token", access_token)
                        .accept(MediaType.APPLICATION_JSON_TYPE)
                        .get(JsonNode.class);

    idNode = node.get("entities").get(0).get("uuid");
    assertEquals(id.toString(), idNode.getTextValue());
  }


}
