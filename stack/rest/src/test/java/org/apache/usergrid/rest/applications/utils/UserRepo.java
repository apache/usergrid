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
package org.apache.usergrid.rest.applications.utils;


import org.apache.usergrid.rest.test.resource.ClientSetup;
import org.apache.usergrid.rest.test.resource.model.Entity;
import org.apache.usergrid.utils.UUIDUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


/**
 * Creates three users in current app
 */
public class UserRepo {
    private final ClientSetup clientSetup;

    public UserRepo(ClientSetup clientSetup){
        this.clientSetup =  clientSetup;
    }

    private final Map<String, UUID> loaded = new HashMap<String, UUID>();

    public void load(  )  {
        if ( loaded.size() > 0 ) {
            return;
        }
        createUser( "user1", "user1@apigee.com", "user1", "Jane Smith 1" );
        createUser( "user2", "user2@apigee.com", "user2", "John Smith 2" );
        createUser( "user3", "user3@apigee.com", "user3", "John Smith 3"  );
    }

    private void createUser( String username, String email, String password, String fullName) {
        Entity entity = new Entity();
        entity.put( "email", email );
        entity.put( "username", username );
        entity.put("name", fullName);
        entity.put( "password", password );
        entity.put("pin", "1234");
        UUID id = createUser( entity );
        loaded.put( username, id );
    }

    public UUID getByUserName( String name ) {
        return loaded.get( name );
    }

    /** Create a user via the REST API and post it. Return the response */
    private UUID createUser( Entity payload )  {
        Entity entity =  clientSetup.getRestClient().org(
            clientSetup.getOrganizationName()).app(clientSetup.getAppName()).collection("users").post(payload);
        String idString = entity.get("uuid").toString();
        return UUIDUtils.tryExtractUUID( idString );
    }
}
