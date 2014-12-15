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


import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.usergrid.rest.test.resource.TestContext;

import com.fasterxml.jackson.databind.JsonNode;


/**
 * Models the user class for response from REST calls.
 */
public class User extends Entity {

    public User (ApiResponse response){
        setResponse( response,"owner" );
    }
    //TODO: create another constructor to take in the nessesary things to post to a user.

    public Boolean getActivated(){
        return (Boolean) this.get( "activated" );
    }

    public Boolean getAdminUser(){
        return (Boolean) this.get( "adminUser" );
    }

    public UUID getApplicationId(){
        return  UUID.fromString( (String) get("applicationId") );
    }

    public Boolean getConfirmed(){
        return (Boolean) this.get("confirmed");
    }

    public Boolean getDisabled(){
        return (Boolean) this.get("disabled");
    }

    public String getDisplayEmailAddress(){
        return (String) this.get("displayEmailAddress");
    }

    public String getEmail(){
        return (String) this.get("email");
    }

    public String getHtmlDisplayEmailAddress(){
        return (String) this.get("htmldisplayEmailAddress");
    }

    public String getName(){
        return (String) this.get("name");
    }

    public Map<String,Object> getProperties(){
        return (Map<String,Object>) this.get("properties");
    }

    public String getUsername(){
        return (String) this.get("username");
    }

    public UUID getUuid(){
        return UUID.fromString( (String) get("uuid") );
    }



}
