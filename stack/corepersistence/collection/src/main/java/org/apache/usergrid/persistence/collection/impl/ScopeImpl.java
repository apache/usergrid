package org.apache.usergrid.persistence.collection.impl;


import org.apache.usergrid.persistence.collection.Scope;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.base.Preconditions;


/**
 * Simple impl of hte collection context
 *
 * @author tnine
 */
public class ScopeImpl implements Scope {

    private final Id ownerId;
    private final String name;


    public ScopeImpl( final Id ownerId, final String name ) {
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

        final ScopeImpl that = ( ScopeImpl ) o;

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
        return "ScopeImpl{" +
                " ownerId=" + ownerId +
                ", name='" + name + '\'' +
                '}';
    }
}
