package org.apache.usergrid.persistence.collection;


import java.util.UUID;

import com.google.common.base.Preconditions;


/**
 * Simple impl of hte collection context
 * @author tnine
 */
public class CollectionContextImpl implements CollectionContext {

    private final UUID applicationId;
    private final UUID ownerId;
    private final String name;


    public CollectionContextImpl( final UUID applicationId, final UUID ownerId, final String name ) {
        Preconditions.checkNotNull( applicationId , "applicationId is required");
        Preconditions.checkNotNull( ownerId , "ownerId is required");
        Preconditions.checkNotNull( name , "name is required");


        this.applicationId = applicationId;
        this.ownerId = ownerId;
        this.name = name;
    }


    @Override
    public UUID getApplication() {
        return applicationId;
    }


    @Override
    public UUID getOwner() {
        return ownerId;
    }


    @Override
    public String getName() {
        return name;
    }


    @Override
    public boolean equals( final Object o ) {
        if ( this == o ) {
            return true;
        }
        if ( o == null || getClass() != o.getClass() ) {
            return false;
        }

        final CollectionContextImpl that = ( CollectionContextImpl ) o;

        if ( !applicationId.equals( that.applicationId ) ) {
            return false;
        }
        if ( !name.equals( that.name ) ) {
            return false;
        }
        if ( !ownerId.equals( that.ownerId ) ) {
            return false;
        }

        return true;
    }


    @Override
    public int hashCode() {
        int result = applicationId.hashCode();
        result = 31 * result + ownerId.hashCode();
        result = 31 * result + name.hashCode();
        return result;
    }


    @Override
    public String toString() {
        return "CollectionContextImpl{" +
                "applicationId=" + applicationId +
                ", ownerId=" + ownerId +
                ", name='" + name + '\'' +
                '}';
    }
}
