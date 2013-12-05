package org.apache.usergrid.persistence.collection.impl;


import java.util.UUID;

import org.apache.usergrid.persistence.collection.EntityCollection;

import com.google.common.base.Preconditions;


/**
 * Simple impl of hte collection context
 * @author tnine
 */
public class EntityCollectionImpl implements EntityCollection {

    private final UUID ownerId;
    private final String name;


    public EntityCollectionImpl( final UUID ownerId, final String name ) {
        Preconditions.checkNotNull( ownerId , "ownerId is required");
        Preconditions.checkNotNull( name , "name is required");
        Preconditions.checkArgument( name.length() > 0, "name must have a length" );


        this.ownerId = ownerId;
        this.name = name;
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

        final EntityCollectionImpl that = ( EntityCollectionImpl ) o;

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
        int result = 31 * ownerId.hashCode();
        result = 31 * result + name.hashCode();
        return result;
    }


    @Override
    public String toString() {
        return "EntityCollectionImpl{" +
                " ownerId=" + ownerId +
                ", name='" + name + '\'' +
                '}';
    }
}
