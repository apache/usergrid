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


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jersey.api.client.WebResource;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.ws.rs.core.MediaType;
import static org.apache.usergrid.utils.MapUtils.hashMap;
import org.apache.usergrid.utils.UUIDUtils;


public enum UserRepo {
        

    INSTANCE;

    private final Map<String, UUID> loaded = new HashMap<String, UUID>();


    public void load( WebResource resource, String accessToken ) throws IOException {
        if ( loaded.size() > 0 ) {
            return;
        }

        createUser( "user1", "user1@apigee.com", "user1", "Jane Smith 1", resource, accessToken );
        createUser( "user2", "user2@apigee.com", "user2", "John Smith 2", resource, accessToken );
        createUser( "user3", "user3@apigee.com", "user3", "John Smith 3", resource, accessToken );
    }


    private void createUser( String username, String email, String password, String fullName, WebResource resource,
                             String accessToken ) throws IOException {
    

        Map<String, String> payload = hashMap( "email", email ).map( "username", username ).map( "name", fullName )
                .map( "password", password ).map( "pin", "1234" );

        UUID id = createUser( payload, resource, accessToken );

        loaded.put( username, id );
    }


    public UUID getByUserName( String name ) {
        return loaded.get( name );
    }


    /** Create a user via the REST API and post it. Return the response */
    private UUID createUser( Map<String, String> payload, WebResource resource, String access_token ) throws IOException {

        ObjectMapper mapper = new ObjectMapper();
        JsonNode response = mapper.readTree( resource.path( "/test-organization/test-app/users" ).queryParam( "access_token", access_token )
                        .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE )
                        .post( String.class, payload ));

        String idString = response.get( "entities" ).get( 0 ).get( "uuid" ).asText();

        return UUIDUtils.tryExtractUUID( idString );
    }

}
