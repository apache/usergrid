package org.apache.usergrid.persistence.collection.util;


import java.lang.reflect.Field;
import java.util.UUID;

import org.apache.commons.lang3.reflect.FieldUtils;

import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.base.Preconditions;


/** @author tnine */
public class EntityUtils {

    private static final int UUID_VERSION = 1;

    private static final Field VERSION = FieldUtils.getField( Entity.class, "version", true );

    private static final Field ID = FieldUtils.getField( Entity.class, "id", true );


    /** Set the version into the entity */
    public static void setVersion( Entity entity, UUID version ) {

        try {
            FieldUtils.writeField( VERSION, entity, version, true );
        }
        catch ( IllegalAccessException e ) {
            throw new RuntimeException( "Unable to set the field " + VERSION + " into the entity", e );
        }
    }


    /** Set the id into the entity */
    public static void setId( Entity entity, Id id ) {
        try {
            FieldUtils.writeField( ID, entity, id, true );
        }
        catch ( IllegalAccessException e ) {
            throw new RuntimeException( "Unable to set the field " + ID + " into the entity", e );
        }
    }


    /** Verify the entity has an id and a version */
    public static void verifyEntityWrite( Entity entity ) {


        Preconditions.checkNotNull( entity, "Entity is required in the new stage of the mvcc write" );

        verifyIdentity( entity.getId() );

        verifyVersion( entity.getVersion() );
    }


    /** Verify the version is correct.  Also verifies the contained entity */
    public static void verifyMvccEntityWithEntity( MvccEntity entity ) {


        Preconditions.checkNotNull( entity.getEntity().isPresent(), "Entity is required" );
        verifyMvccEntityOptionalEntity( entity );


    }


    /** Verify the version is correct.  Does not verify the contained entity of it is null */
    public static void verifyMvccEntityOptionalEntity( MvccEntity entity ) {


        verifyIdentity( entity.getId() );

        verifyVersion( entity.getVersion() );

        if(entity.getEntity().isPresent()){
            verifyEntityWrite( entity.getEntity().orNull() );
        }
    }


    /** Verify the version is not null and is a type 1 version */
    public static void verifyVersion( final UUID entityVersion ) {

        Preconditions.checkNotNull( entityVersion, "The entity version is required to be set for an update operation" );


        Preconditions.checkArgument( entityVersion.version() == UUID_VERSION, "The uuid must be version 1" );
    }


    /**
     * Verifies an identity is correct.  It must pass the following checks 1) Not null 2) A UUID is present 3) The UUID
     * is of type 1 (time uuid) 4) The type is present and has a length
     */
    public static void verifyIdentity( final Id entityId ) {

        Preconditions.checkNotNull( entityId, "The entity id is required to be set for an update operation" );

        final UUID uuid = entityId.getUuid();

        Preconditions.checkNotNull( uuid, "The entity id uuid is required to be set for an update operation" );


        Preconditions.checkArgument( uuid.version() == UUID_VERSION, "The uuid must be version 1" );

        final String type = entityId.getType();
        Preconditions.checkNotNull( type, "The entity id type required to be set for an update operation" );

        Preconditions.checkArgument( type.length() > 0, "Type must have a length greater than 0" );
    }
}
