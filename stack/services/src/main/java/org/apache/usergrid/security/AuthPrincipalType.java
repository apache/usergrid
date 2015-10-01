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
package org.apache.usergrid.security;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.persistence.entities.Group;
import org.apache.usergrid.persistence.entities.User;

import static org.apache.usergrid.utils.CodecUtils.base64;


public enum AuthPrincipalType {
    ORGANIZATION( "ou", Group.ENTITY_TYPE ), ADMIN_USER( "ad", User.ENTITY_TYPE ),
    APPLICATION( "ap", CpNamingUtils.APPLICATION_INFO ), APPLICATION_USER( "au", User.ENTITY_TYPE );

    public static final int PREFIX_LENGTH = 3;
    public static final int BASE64_PREFIX_LENGTH = 4;

    private final String prefix;
    private final String base64Prefix;
    private final String entityType;

    private static Map<String, AuthPrincipalType> prefixes;
    private static Map<String, AuthPrincipalType> base64Prefixes;


    private synchronized static void register( AuthPrincipalType type ) {
        if ( prefixes == null ) {
            prefixes = new ConcurrentHashMap<String, AuthPrincipalType>();
        }
        if ( base64Prefixes == null ) {
            base64Prefixes = new ConcurrentHashMap<String, AuthPrincipalType>();
        }
        prefixes.put( type.getPrefix(), type );
        base64Prefixes.put( type.getBase64Prefix(), type );
    }


    AuthPrincipalType( String prefix, String entityType ) {
        this.prefix = prefix;
        base64Prefix = base64( prefix + ":" );
        this.entityType = entityType;
        register( this );
    }


    public String getPrefix() {
        return prefix;
    }


    public String getBase64Prefix() {
        return base64Prefix;
    }


    public String getEntityType() {
        return entityType;
    }


    public boolean prefixesBase64String( String key ) {
        if ( key == null ) {
            return false;
        }
        return key.startsWith( base64Prefix );
    }


    public static AuthPrincipalType getFromBase64String( String key ) {
        if ( key == null ) {
            return null;
        }
        if ( key.length() >= 4 ) {
            return base64Prefixes.get( key.substring( 0, 4 ) );
        }
        return null;
    }


    public boolean prefixesString( String key ) {
        if ( key == null ) {
            return false;
        }
        return key.startsWith( prefix + ":" );
    }


    public static AuthPrincipalType getFromString( String key ) {
        if ( key == null ) {
            return null;
        }
        if ( ( key.length() >= 3 ) && ( key.charAt( 2 ) == ':' ) ) {
            return prefixes.get( key.substring( 0, 2 ) );
        }
        return null;
    }

}
