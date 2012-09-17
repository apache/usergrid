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
package org.usergrid.rest.applications;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.usergrid.utils.MapUtils.hashMap;

import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.codehaus.jackson.JsonNode;
import org.junit.Test;
import org.usergrid.management.ApplicationInfo;
import org.usergrid.management.OrganizationInfo;
import org.usergrid.rest.AbstractRestTest;

import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.api.client.UniformInterfaceException;

/**
 * Invokes methods on ApplicationResource
 * 
 * @author zznate
 */
public class ApplicationResourceTest extends AbstractRestTest {

    @Test
    public void applicationWithOrgCredentials() throws Exception {

        OrganizationInfo orgInfo = managementService
                .getOrganizationByName("test-organization");

        String clientId = managementService.getClientIdForOrganization(orgInfo
                .getUuid());
        String clientSecret = managementService
                .getClientSecretForOrganization(orgInfo.getUuid());

        JsonNode node = resource().path("/test-organization/test-app/users")
                .queryParam("client_id", clientId)
                .queryParam("client_secret", clientSecret)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).get(JsonNode.class);

        assertNotNull(node.get("entities"));

    }

    @Test
    public void applicationWithAppCredentials() throws Exception {

        ApplicationInfo appInfo = managementService
                .getApplicationInfo("test-organization/test-app");

        String clientId = managementService.getClientIdForApplication(appInfo
                .getId());
        String clientSecret = managementService
                .getClientSecretForApplication(appInfo.getId());

        JsonNode node = resource().path("/test-organization/test-app/users")
                .queryParam("client_id", clientId)
                .queryParam("client_secret", clientSecret)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).get(JsonNode.class);

        assertNotNull(node.get("entities"));

    }

    @Test
    public void applicationWithJsonCreds() throws Exception {

        ApplicationInfo appInfo = managementService
                .getApplicationInfo("test-organization/test-app");

        String clientId = managementService.getClientIdForApplication(appInfo
                .getId());
        String clientSecret = managementService
                .getClientSecretForApplication(appInfo.getId());

        Map<String, String> payload = hashMap("email",
                "applicationWithJsonCreds@usergrid.org")
                .map("username", "applicationWithJsonCreds")
                .map("name", "applicationWithJsonCreds")
                .map("password", "applicationWithJsonCreds").map("pin", "1234");

        JsonNode node = resource().path("/test-organization/test-app/users")
                .queryParam("client_id", clientId)
                .queryParam("client_secret", clientSecret)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .post(JsonNode.class, payload);

        assertNotNull(getEntity(node, 0));

        payload = hashMap("username", "applicationWithJsonCreds").map(
                "password", "applicationWithJsonCreds").map("grant_type",
                "password");

        node = resource().path("/test-organization/test-app/token")
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .post(JsonNode.class, payload);

        JsonNode token = node.get("access_token");

        assertNotNull(token);

    }

    @Test
    public void test_GET_credentials_ok() {
        String mgmtToken = adminToken();

        JsonNode node = resource()
                .path("/test-organization/test-app/credentials")
                .queryParam("access_token", mgmtToken)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE).get(JsonNode.class);
        assertEquals("ok", node.get("status").getTextValue());
        logNode(node);
    }
    
    @Test
    public void noAppDelete() {
        String mgmtToken = adminToken();

        Status status = null;
        JsonNode node = null;
        
        try {
             node = resource().path("/test-organization/test-app")
                    .queryParam("access_token", mgmtToken).accept(MediaType.APPLICATION_JSON)
                    .type(MediaType.APPLICATION_JSON_TYPE).delete(JsonNode.class);
        } catch (UniformInterfaceException uie) {
            status = uie.getResponse().getClientResponseStatus();
        }

        assertEquals(Status.BAD_REQUEST, status);
//        assertEquals("Application delete is not allowed yet", node.get("error_description").asText());
    }
}
