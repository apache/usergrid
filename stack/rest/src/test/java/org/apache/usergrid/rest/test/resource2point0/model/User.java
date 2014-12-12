/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
