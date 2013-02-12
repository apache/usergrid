package org.usergrid.rest.management.users.organizations;

import org.codehaus.jackson.JsonNode;
import org.junit.Test;
import org.usergrid.rest.AbstractRestTest;

import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.usergrid.utils.MapUtils.hashMap;

/**
 * @author zznate
 */
public class UsersOrganizationsResourceTest extends AbstractRestTest {



    @Test
    public void createOrgFromUserConnectionFail() throws Exception {


        Map<String, String> payload = hashMap("email",
                        "orgfromuserconn@apigee.com").map("password", "password")
                        .map("organization", "orgfromuserconn");

        JsonNode node = resource().path("/management/organizations")
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .post(JsonNode.class, payload);

        String userId = node.get("data").get("owner").get("uuid").asText();

        assertNotNull(node);

        String token = mgmtToken("orgfromuserconn@apigee.com","password");

        node = resource().path(String.format("/management/users/%s/",userId))
                .queryParam("access_token",token)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .get(JsonNode.class);

        logNode(node);

        payload = hashMap("organization", "Orgfromuserconn");

        // try to create the same org again off the connection
        try {
            node = resource().path(String.format("/management/users/%s/organizations",userId))
                    .queryParam("access_token",token)
                    .accept(MediaType.APPLICATION_JSON)
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .post(JsonNode.class, payload);
            fail("Should have thrown unique exception on org name");

        } catch (Exception ex) {}
    }
}
