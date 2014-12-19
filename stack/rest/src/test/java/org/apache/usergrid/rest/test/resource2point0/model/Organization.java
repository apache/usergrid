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


import java.util.Map;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;


/**
 * Holds the parsed out information of the Organization returned from the ApiResponse.
 * so when you get the organization object, we could then model it out to do the client calls
 */
public class Organization extends Entity {

    private User user;

    public Organization() {

    }

    public Organization( String orgName, String username, String email, String ownerName, String password, Map<String,Object> properties ){

        this.put( "organization", orgName );
        this.put( "username", username);
        this.put( "email", email);
        //TODO: create clearer distinction between ownerName and username in the backend.
        this.put( "name", ownerName);
        this.put( "password", password);

        if(properties != null)
            setProperties( properties );
    }

    public Organization(ApiResponse response){
        setResponse(response,"organization");
    }

    @JsonSerialize( include = JsonSerialize.Inclusion.NON_NULL )
    public String getOrganization( ) {
        return ( String ) this.get( "organization" );
    }

    @JsonSerialize( include = JsonSerialize.Inclusion.NON_NULL )
    public String getUsername() {
        return ( String ) this.get( "username" );
    }

    @JsonSerialize( include = JsonSerialize.Inclusion.NON_NULL )
    public String getEmail() {
        return ( String ) this.get( "email" );
    }

    @JsonSerialize( include = JsonSerialize.Inclusion.NON_NULL )
    public String getName() {
        return ( String ) this.get( "name" );
    }

    @JsonSerialize( include = JsonSerialize.Inclusion.NON_NULL )
    public String getPassword() {
        return ( String ) this.get( "password" );
    }

    public Object getPasswordHistorySize() {
        return  (Integer) this.get("passwordHistorySize");
    }

    public User getOwner(){
        return user;
    }
    public void setOwner(User user){
        this.user = user;
    }

    public Organization mapOrgResponse(Map<String,Object> map){
        putAll((Map<String, Object>) map.get("organization"));
        return this;
    }
}
