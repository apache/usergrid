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
package org.apache.usergrid.security.providers;


import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.usergrid.management.ManagementService;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.index.query.Identifier;
import org.apache.usergrid.persistence.entities.User;
import org.apache.usergrid.security.tokens.exceptions.BadTokenException;

import com.fasterxml.jackson.databind.JsonNode;


/**
 * Provider implementation for accessing Ping Identity
 *
 * @author zznate
 */
public class PingIdentityProvider extends AbstractProvider {

    private Logger logger = LoggerFactory.getLogger( PingIdentityProvider.class );

    private String apiUrl;
    private String clientId;
    private String clientSecret;


    PingIdentityProvider( EntityManager entityManager, ManagementService managementService ) {
        super( entityManager, managementService );
    }


    @Override
    public User createOrAuthenticate( String externalToken ) throws BadTokenException {
        Map<String, Object> pingUser = userFromResource( externalToken );

        User user = null;
        try {
            user = managementService.getAppUserByIdentifier( entityManager.getApplication().getUuid(),
                    Identifier.fromEmail( pingUser.get( "username" ).toString() ) );
        }
        catch ( Exception ex ) {
            ex.printStackTrace();
            // TODO what to do here?
        }

        if ( user == null ) {
            Map<String, Object> properties = new LinkedHashMap<String, Object>();
            properties.putAll( pingUser );
            properties.put( "activated", true );
            properties.put( "confirmed", true );
            try {
                user = entityManager.create( "user", User.class, properties );
            }
            catch ( Exception ex ) {
                throw new BadTokenException( "Could not create user for that token", ex );
            }
        }
        else {
            user.setProperty( "expiration", pingUser.get( "expiration" ) );
            try {
                entityManager.update( user );
            }
            catch ( Exception ex ) {
                ex.printStackTrace();
            }
        }
        return user;
    }


    @Override
    void configure() {
        try {
            Map config = loadConfigurationFor();
            if ( config != null ) {
                apiUrl = ( String ) config.get( "api_url" );
                clientId = ( String ) config.get( "client_id" );
                clientSecret = ( String ) config.get( "client_secret" );
            }
        }
        catch ( Exception ex ) {
            ex.printStackTrace();
        }
    }


    @Override
    public Map<Object, Object> loadConfigurationFor() {
        return loadConfigurationFor( "pingIdentProvider" );
    }


    @Override
    public void saveToConfiguration( Map<String, Object> config ) {
        saveToConfiguration( "pingIdentProvider", config );
    }


    @Override
    Map<String, Object> userFromResource( String externalToken ) {

        JsonNode node = client.resource( apiUrl )
                              .queryParam( "grant_type", "urn:pingidentity.com:oauth2:grant_type:validate_bearer" )
                              .queryParam( "client_secret", clientSecret ).queryParam( "client_id", clientId )
                              .queryParam( "token", externalToken ).type( MediaType.APPLICATION_FORM_URLENCODED_TYPE )
                              .header("Content-Length", "0")
                              .post( JsonNode.class );

        String rawEmail = node.get( "access_token" ).get( "subject" ).asText();

        Map<String, Object> userMap = new HashMap<String, Object>();
        userMap.put( "expiration", node.get( "expires_in" ).asLong() );
        userMap.put( "username", pingUsernameFrom( rawEmail ) );
        userMap.put( "name", "pinguser" );
        userMap.put( "email", rawEmail );

        return userMap;
    }


    public static String pingUsernameFrom( String rawEmail ) {
        return String.format( "pinguser_%s", rawEmail );
    }


    public static long extractExpiration( User user ) {
        Long expiration = ( Long ) user.getProperty( "expiration" );
        if ( expiration == null ) {
            expiration = (long) 7200;
        }
        return expiration * 1000;
    }
}
