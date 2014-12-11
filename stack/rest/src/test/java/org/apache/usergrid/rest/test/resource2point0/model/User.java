package org.apache.usergrid.rest.test.resource2point0.model;


import java.util.UUID;

import org.apache.usergrid.rest.test.resource.TestContext;

import com.fasterxml.jackson.databind.JsonNode;


/**
 * Created by ApigeeCorporation on 12/10/14.
 */
public class User {

    protected String user;
    protected String password;
    protected String email;
    protected String token;
    protected UUID uuid;


    /**
     * @param user
     * @param password
     * @param email
     */
    public User( String user, String password, String email ) {
        this.user = user;
        this.password = password;
        this.email = email;
    }

    /**
     * Manually set our token for this user.
     * @param token
     */
    public void setToken(String token){
        this.token = token;
    }

    /** @return the user */
    public String getUser() {
        return user;
    }


    /** @return the password */
    public String getPassword() {
        return password;
    }


    /** @return the email */
    public String getEmail() {
        return email;
    }


    public String getToken() {
        return this.token;
    }


    /** @return the uuid */
    public UUID getUuid() {
        return uuid;
    }

    public void setUUID(UUID uuid) {
        this.uuid = uuid;
    }

    public boolean isLoggedIn() {
        return this.token != null;
    }

    /** Log out */
    public void logout() {
        token = null;
    }

}
