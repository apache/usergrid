package org.apache.usergrid.persistence.model.entity;


import java.util.UUID;

import org.apache.usergrid.persistence.model.value.EntityObject;

import com.google.common.base.Preconditions;


/**
 * Simple entity that is used for persistence.  It has 4 required properties. <p/> uuid: The uuid of the entity type:
 * The entity name (user, car, restaurant etc) created: The time the entity was created in millis since epoch updated:
 * The time the entity was updated in millis since epoch;
 */
public class Entity extends EntityObject {


    /**
     * The entity type. This must be set
     */
    private String type;

    /**
     * The generated uuid.  This should never be set by a user
     */
    private UUID uuid;

    /**
     * The version of this entity.  Options, since it can be used for optimistic locking
     */
    private UUID version;

    /**
     * The time in milliseconds since epoch the entity was created
     */
    private long created;

    /**
     * The time in milliseconds since epoch the entity was updated
     */
    private long updated;


    /**
     * Create an entity with no uuid.  This should be used for creating new entities
     */
    public Entity( String type ) {
        Preconditions.checkNotNull( type, "Type must not be null" );
        Preconditions.checkArgument( type.length() > 0, "Type must have a length" );
        this.type = type;
    }


    /**
     * Create an entity with the given type and uuid.  Should be used for all update operations to an existing entity
     */
    public Entity( UUID uuid, String type ) {
        this( type );

        Preconditions.checkNotNull( uuid, "uuid must not be null" );

        this.uuid = uuid;
    }


    /**
     * Do not use!  This is only for serialization.
     */
    public Entity() {

    }


    public UUID getUuid() {
        return uuid;
    }


    public String getType() {
        return type;
    }


    public UUID getVersion() {
        return version;
    }


    public void setVersion( final UUID version ) {
        this.version = version;
    }


    /**
     * Should only be invoked by the persistence framework
     */
    public void setCreated( long created ) {
        this.created = created;
    }


    /**
     * Should only be invoked by the persistence framework
     */
    public void setUpdated( long updated ) {
        this.updated = updated;
    }


    public long getCreated() {
        return created;
    }


    public long getUpdated() {
        return updated;
    }


    @Override
    public boolean equals( Object o ) {
        if ( this == o ) {
            return true;
        }
        if ( o == null || getClass() != o.getClass() ) {
            return false;
        }

        Entity entity = ( Entity ) o;

        if ( type != null ? !type.equals( entity.type ) : entity.type != null ) {
            return false;
        }
        if ( uuid != null ? !uuid.equals( entity.uuid ) : entity.uuid != null ) {
            return false;
        }

        return true;
    }


    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + ( uuid != null ? uuid.hashCode() : 0 );
        return result;
    }
}
