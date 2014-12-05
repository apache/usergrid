package org.apache.usergrid.rest.test.resource2point0.state;


/**
 * Context to hold client stateful information
 */
public class ClientContext {
    private String token;


    public String getToken() {
        return token;
    }


    public void setToken( final String token ) {
        this.token = token;
    }
}
