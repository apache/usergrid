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
package org.apache.usergrid.security.oauth;


import java.util.UUID;


import org.apache.usergrid.security.AuthPrincipalType;
import org.apache.usergrid.utils.UUIDUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;


public class ClientCredentialsInfo {

    private String id;
    private String secret;

    /** Needed for Jackson since this class is serialized to the Shiro Cache */
    public ClientCredentialsInfo() {}

    public ClientCredentialsInfo( String id, String secret ) {
        this.id = id;
        this.secret = secret;
    }


    @JsonProperty("client_id")
    public String getId() {
        return id;
    }


    @JsonProperty("client_secret")
    public String getSecret() {
        return secret;
    }


    public static ClientCredentialsInfo forUuidAndSecret( AuthPrincipalType type, UUID uuid, String secret ) {
        return new ClientCredentialsInfo( getClientIdForTypeAndUuid( type, uuid ), secret );
    }


    public static String getClientIdForTypeAndUuid( AuthPrincipalType type, UUID uuid ) {
        return type.getBase64Prefix() + UUIDUtils.toBase64( uuid );
    }


    @JsonIgnore
    public UUID getUUIDFromConsumerKey() {
        return getUUIDFromClientId( id );
    }


    public static UUID getUUIDFromClientId( String key ) {
        if ( key == null ) {
            return null;
        }
        if ( key.length() != 26 ) {
            return null;
        }
        return UUIDUtils.fromBase64( key.substring( 4 ) );
    }


    @JsonIgnore
    public AuthPrincipalType getTypeFromClientId() {
        return getTypeFromClientId( id );
    }


    public static AuthPrincipalType getTypeFromClientId( String key ) {
        if ( key == null ) {
            return null;
        }
        if ( key.length() != 26 ) {
            return null;
        }
        return AuthPrincipalType.getFromBase64String( key );
    }
}
