package org.apache.usergrid.persistence.collection.impl;


import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.base.Preconditions;


/**
 * Simple impl of hte collection context
 *
 * @author tnine
 */
public class CollectionScopeImpl implements CollectionScope {

    private final Id ownerId;
    private final String name;


    public CollectionScopeImpl( final Id ownerId, final String name ) {
        Preconditions.checkNotNull( ownerId, "ownerId is required" );
        Preconditions.checkNotNull( name, "name is required" );
        Preconditions.checkArgument( name.length() > 0, "name must have a length" );


        this.ownerId = ownerId;
        this.name = name;
    }


    @Override
    public Id getOwner() {
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

        final CollectionScopeImpl that = ( CollectionScopeImpl ) o;

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
        return "CollectionScopeImpl{" +
                " ownerId=" + ownerId +
                ", name='" + name + '\'' +
                '}';
    }
}
