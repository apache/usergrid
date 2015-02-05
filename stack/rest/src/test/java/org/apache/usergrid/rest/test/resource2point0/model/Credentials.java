package org.apache.usergrid.rest.test.resource2point0.model;

import java.util.Map;

/**
 */
public class Credentials extends Entity {
    private String clientId;
    private String clientSecret;

    public Credentials(ApiResponse response) {
        super(response);
        Map<String, Object> properties = response.getProperties();
        if (properties.containsKey("credentials")) {
            Map<String, String> credentials = (Map<String, String>) properties.get("credentials");
            this.setClientId(credentials.get("client_id"));
            this.setClientSecret(credentials.get("client_secret"));
        }
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }


}
