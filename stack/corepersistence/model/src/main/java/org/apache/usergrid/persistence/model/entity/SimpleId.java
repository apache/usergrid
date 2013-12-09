package org.apache.usergrid.persistence.model.entity;


import java.io.Serializable;
import java.util.UUID;

import org.apache.usergrid.persistence.model.util.UUIDGenerator;
import org.apache.usergrid.persistence.model.util.Verify;

import com.google.common.base.Preconditions;


/** @author tnine */
public class SimpleId implements Id, Serializable {


    private final UUID uuid;
    private final String type;


    public SimpleId( final UUID uuid, final String type ) {
        Preconditions.checkNotNull( uuid, "uuid is required" );
        Verify.stringExists( type, "type is required" );

        this.uuid = uuid;
        this.type = type;
    }


    /**
     * Create a new ID.  Should only be used for new entities
     * @param type
     */
    public SimpleId( final String type ){
       this(UUIDGenerator.newTimeUUID(), type);
    }



    @Override
    public UUID getUuid() {
        return uuid;
    }


    @Override
    public String getType() {
        return type;
    }


    @Override
    public boolean equals( final Object o ) {
        if ( this == o ) {
            return true;
        }
        if ( !( o instanceof SimpleId ) ) {
            return false;
        }

        final SimpleId simpleId = ( SimpleId ) o;

        if ( !type.equals( simpleId.type ) ) {
            return false;
        }
        if ( !uuid.equals( simpleId.uuid ) ) {
            return false;
        }

        return true;
    }


    @Override
    public int hashCode() {
        int result = uuid.hashCode();
        result = 31 * result + type.hashCode();
        return result;
    }


    @Override
    public String toString() {
        return "SimpleId{" +
                "uuid=" + uuid +
                ", type='" + type + '\'' +
                '}';
    }
}
