package org.apache.usergrid.persistence.collection.mvcc.entity;


import java.util.UUID;

import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.OrganizationScope;
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

        verifyTimeUuid( entity.getVersion(), "version" );
    }


    /**
     * Verify the version is correct.  Also verifies the contained entity
     */
    public static void verifyMvccEntityWithEntity( MvccEntity entity ) {


        Preconditions.checkNotNull( entity.getEntity().isPresent(), "Entity is required" );
        verifyMvccEntityOptionalEntity( entity );
    }


    /**
     * Verify the version is correct.  Does not verify the contained entity of it is null
     */
    public static void verifyMvccEntityOptionalEntity( MvccEntity entity ) {


        verifyIdentity( entity.getId() );

        verifyTimeUuid( entity.getVersion(), "version" );

        if ( entity.getEntity().isPresent() ) {
            verifyEntityWrite( entity.getEntity().orNull() );
        }
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


        Preconditions.checkArgument( uuid.version() == UUID_VERSION, "The uuid must be version 1" );

        final String type = entityId.getType();
        Preconditions.checkNotNull( type, "The id type is required " );

        Preconditions.checkArgument( type.length() > 0, "The id type must have a length greater than 0" );
    }



    /**
     * Validate the organization scope
     */
    public static void validateOrganizationScope( final OrganizationScope scope ) {
        Preconditions.checkNotNull( scope, "organization scope is required" );

        verifyIdentity( scope.getOrganization() );
    }


    /**
     * Validate the collection scope
     */
    public static void validateCollectionScope( final CollectionScope scope ) {

        Preconditions.checkNotNull( scope, "collection scope is required" );

        verifyIdentity( scope.getOwner() );

        verifyString( scope.getName(), "name" );

        validateOrganizationScope( scope );
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
