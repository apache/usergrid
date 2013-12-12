package org.apache.usergrid.persistence.model.entity;


import java.util.UUID;

import org.apache.usergrid.persistence.model.field.value.EntityObject;

import com.google.common.base.Preconditions;


/**
 * Simple entity that is used for persistence.  It has 1 required property, the Id.
 * Equality is based both on id an on version.
 */
public class Entity extends EntityObject {

    /**
     * The id.  We should never serialize this
     */
    private transient Id id;

    /**
     * The version of this entity.
     *
     * Do not remove this, set by the collection manager
     */
    private UUID version;



    /**
     * Create an entity with the given type and id.  Should be used for all update operations to an existing entity
     */
    public Entity( Id id ) {
       Preconditions.checkNotNull( id, "id must not be null" );

        this.id = id;
    }


    /**
     * Generate a new entity with the given type and a new id
     * @param type
     */
    public Entity(String type){
        this(new SimpleId( type ));
    }



    /**
     * Do not use!  This is only for serialization.
     */
    public Entity() {

    }


    public Id getId() {
        return id;
    }


    public UUID getVersion() {
        return version;
    }


    /**
     * Equality is based both on id and version.  If an entity
     * has the same id but different versions, they are not equals
     * @param o
     * @return
     */
    @Override
    public boolean equals( final Object o ) {
        if ( this == o ) {
            return true;
        }
        if ( !( o instanceof Entity ) ) {
            return false;
        }

        final Entity entity = ( Entity ) o;

        if ( id != null ? !id.equals( entity.id ) : entity.id != null ) {
            return false;
        }
        if ( version != null ? !version.equals( entity.version ) : entity.version != null ) {
            return false;
        }

        return true;
    }


    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + ( version != null ? version.hashCode() : 0 );
        return result;
    }


    @Override
    public String toString() {
        return "Entity{" +
                "id=" + id +
                ", version=" + version +
                '}';
    }
}
