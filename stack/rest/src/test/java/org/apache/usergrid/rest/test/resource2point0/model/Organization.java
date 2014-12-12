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


import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.usergrid.utils.MapUtils;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;


/**
 * Holds the parsed out information of the Organization returned from the ApiResponse.
 * so when you get the organization object, we could then model it out to do the client calls
 */
public class Organization extends Entity {


    private String organization;
    private String username;
    private String email;
    private String name;
    private String password;
    private String passwordHistorySize;


    public Organization() {

    }

    public Organization( String orgName, String username, String email, String ownerName, String password ){

        this.dynamic_properties.put( "orgName",orgName );
    }

    @JsonSerialize( include = JsonSerialize.Inclusion.NON_NULL )
    public String getOrganization( ) {
        return ( String ) this.dynamic_properties.get( "orgName" );
    }
    public void setOrganization( final String orgName ) {
        this.organization = orgName;
    }

    @JsonSerialize( include = JsonSerialize.Inclusion.NON_NULL )
    public String getUsername() {
        return ( String ) this.dynamic_properties.get( "orgName" );
    }


    public void setUsername( final String username ) {
        this.username = username;
    }

    @JsonSerialize( include = JsonSerialize.Inclusion.NON_NULL )
    public String getEmail() {
        return ( String ) this.dynamic_properties.get( "orgName" );
    }


    public void setEmail( final String email ) {
        this.email = email;
    }

    @JsonSerialize( include = JsonSerialize.Inclusion.NON_NULL )
    public String getName() {
        return ( String ) this.dynamic_properties.get( "orgName" );
    }

    public void setName( final String name ) {
        this.name = name;
    }


    @JsonSerialize( include = JsonSerialize.Inclusion.NON_NULL )
    public String getPassword() {
        return ( String ) this.dynamic_properties.get( "orgName" );
    }




//    public String getOrganizationName(){
//        return (String) dynamic_properties.get( "orgName" );
//    }


    public void setPasswordHistorySize( final String passwordHistorySize ) {
        this.passwordHistorySize = passwordHistorySize;
    }

    //private String orgName;
    //Keep the raw api response in here, dole out
   // private ApiResponse apiResponse;

    //Organizations are always initialized by name, and the uuid will be set on creations
//    public Organization( final String orgName) {
//        this.orgName = orgName;
//    }

    //Add setter and getter methods for throwing in relevant objects.
//
//    public UUID getUuid() {
//        return (UUID)this.get( "uuid" );
//    }
//
//    public String getOrgName() {
//        return (String) this.get( "name" );
//        //return orgName;
//    }

}
