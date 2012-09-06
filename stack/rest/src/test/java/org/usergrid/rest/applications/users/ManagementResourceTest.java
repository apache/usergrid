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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.codehaus.jackson.JsonNode;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.java.client.response.ApiResponse;
import org.usergrid.management.UserInfo;
import org.usergrid.rest.AbstractRestTest;

import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.api.client.UniformInterfaceException;

/**
 * @author tnine
 */
public class ManagementResourceTest extends AbstractRestTest {
    private static Logger log = LoggerFactory
            .getLogger(ManagementResourceTest.class);

    public ManagementResourceTest() throws Exception {

    }

    /**
     * Test if we can reset our password as an admin
     */
    @Test
    public void setSelfAdminPasswordAsAdmin() {

        String newPassword = "foo";

        Map<String, String> data = new HashMap<String, String>();
        data.put("newpassword", newPassword);
        data.put("oldpassword", "test");

        // change the password as admin. The old password isn't required
        JsonNode node = resource().path("/management/users/test/password")
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .post(JsonNode.class, data);

        assertNull(getError(node));

        adminAccessToken = mgmtToken("test", newPassword);

        data.put("oldpassword", newPassword);
        data.put("newpassword", "test");

        node = resource().path("/management/users/test/password")
                .queryParam("access_token", adminAccessToken)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .post(JsonNode.class, data);

        assertNull(getError(node));

    }

    @Test
    public void passwordMismatchErrorAdmin() {
        String origPassword = "foo";
        String newPassword = "bar";

        Map<String, String> data = new HashMap<String, String>();
        data.put("newpassword", origPassword);

        // now change the password, with an incorrect old password

        data.put("oldpassword", origPassword);
        data.put("newpassword", newPassword);

        Status responseStatus = null;

        try {
            resource().path("/management/users/test/password")
                    .accept(MediaType.APPLICATION_JSON)
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .post(JsonNode.class, data);
        } catch (UniformInterfaceException uie) {
            responseStatus = uie.getResponse().getClientResponseStatus();
        }

        assertNotNull(responseStatus);

        assertEquals(Status.BAD_REQUEST, responseStatus);

    }
    

    @Test
    public void setAdminPasswordAsSysAdmin() {

        
        String superToken = superAdminToken();
        
        String newPassword = "foo";

        Map<String, String> data = new HashMap<String, String>();
        data.put("newpassword", newPassword);

        // change the password as admin. The old password isn't required
        JsonNode node = resource()
                .path("/management/users/test/password")
                .queryParam("access_token", superToken)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .post(JsonNode.class, data);

        assertNull(getError(node));

        //log in with the new password
        String token = mgmtToken("test", newPassword);
        
        assertNotNull(token);
        
        data.put("newpassword", "test");
        
        //now change the password back
        node = resource()
                .path("/management/users/test/password")
                .queryParam("access_token", superToken)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .post(JsonNode.class, data);

        assertNull(getError(node));

        

    }
    

}
