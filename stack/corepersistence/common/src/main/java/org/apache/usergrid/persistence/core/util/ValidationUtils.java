/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.usergrid.persistence.core.util;


import java.util.UUID;

import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.base.Preconditions;


/**
 * @author tnine
 */
public class ValidationUtils {

    private static final int UUID_VERSION = 1;


    /**
     * Verify the entity has an id and a version
     */
    public static void verifyEntityWrite( Entity entity ) {


        Preconditions.checkNotNull( entity, "Entity is required in the new stage of the mvcc write" );

        verifyIdentity( entity.getId() );

    }

    public static void verifyVersion( UUID version ){
        verifyTimeUuid( version, "version" );
    }





    /**
     * Verify the version is not null and is a type 1 version
     */
    public static void verifyTimeUuid( final UUID uuid, final String fieldName ) {

        Preconditions.checkNotNull( uuid, "%s is required to be set for an update operation", fieldName );


        Preconditions.checkArgument( uuid.version() == UUID_VERSION, "%s uuid must be version 1", fieldName );
    }


    /**
     * Verifies an identity is correct.  It must pass the following checks 1) Not null 2) A UUID is present 3) The UUID
     * is of type 1 (time uuid) 4) The type is present and has a length
     */
    public static void verifyIdentity( final Id entityId ) {

        Preconditions.checkNotNull( entityId, "The id is required to be set" );

        final UUID uuid = entityId.getUuid();

        Preconditions.checkNotNull( uuid, "The id uuid is required to be set" );

        //Preconditions.checkArgument( uuid.version() == UUID_VERSION, "The uuid must be version 1" );

        final String type = entityId.getType();
        Preconditions.checkNotNull( type, "The id type is required " );

        Preconditions.checkArgument( type.length() > 0, "The id type must have a length greater than 0" );
    }



    /**
     * Validate the organization scope
     */
    public static void validateApplicationScope( final ApplicationScope scope ) {
        Preconditions.checkNotNull( scope, "organization scope is required" );

        verifyIdentity( scope.getApplication() );
    }


    /**
     * Verify a string is not null nas has a length
     *
     * @param value The string to verify
     * @param fieldName The name to use in constructing error messages
     */
    public static void verifyString( final String value, String fieldName ) {
        Preconditions.checkNotNull( value, "%s is required", fieldName );

        Preconditions.checkArgument( value.length() > 0, "%s must have a length > than 0", fieldName);
    }
}
