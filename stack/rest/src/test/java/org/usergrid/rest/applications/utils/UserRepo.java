package org.usergrid.rest.applications.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.core.MediaType;

import org.codehaus.jackson.JsonNode;
import org.usergrid.utils.UUIDUtils;

import com.sun.jersey.api.client.WebResource;
import static org.usergrid.utils.MapUtils.hashMap;

public enum UserRepo {
    INSTANCE;

    private Map<String, UUID> loaded = new HashMap<String, UUID>();

    public void load(WebResource resource, String accessToken) {
        if (loaded.size() > 0) {
            return;
        }

        createUser("user1", "user1@apigee.com", "user1", "Jane Smith 1",
                resource, accessToken);
        createUser("user2", "user2@apigee.com", "user2", "John Smith 2",
                resource, accessToken);
        createUser("user3", "user3@apigee.com", "user3", "John Smith 3",
                resource, accessToken);

    }

    private void createUser(String username, String email, String password,
            String fullName, WebResource resource, String accessToken) {

        Map<String, String> payload = hashMap("email", email)
                .map("username", username).map("name", fullName)
                .map("password", password).map("pin", "1234");

        UUID id = createUser(payload, resource, accessToken);

        loaded.put(username, id);
    }

    public UUID getByUserName(String name) {
        return loaded.get(name);
    }

    /**
     * Create a user via the REST API and post it. Return the response
     * 
     * @param user
     */
    private UUID createUser(Map<String, String> payload, WebResource resource, String access_token) {

        JsonNode response = resource.path("/test-app/users")
                .queryParam("access_token", access_token)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .post(JsonNode.class, payload);

        String idString = response.get("entities").get(0).get("uuid").asText();

        return UUIDUtils.tryExtractUUID(idString);

    }

}