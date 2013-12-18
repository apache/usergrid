package org.apache.usergrid.persistence.collection.impl;


import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.mvcc.entity.ValidationUtils;
import org.apache.usergrid.persistence.model.entity.Id;


/**
 * Simple impl of hte collection context
 *
 * @author tnine
 */
public class CollectionScopeImpl extends OrganizationScopeImpl implements CollectionScope {
    private final Id ownerId;
    private final String name;


    public CollectionScopeImpl( final Id organizationId, final Id ownerId, final String name ) {
        super( organizationId );
        this.ownerId = ownerId;
        this.name = name;

        ValidationUtils.validateCollectionScope( this );
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
        if ( !( o instanceof CollectionScopeImpl ) ) {
            return false;
        }
        if ( !super.equals( o ) ) {
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
        int result = super.hashCode();
        result = 31 * result + ownerId.hashCode();
        result = 31 * result + name.hashCode();
        return result;
    }


    @Override
    public String toString() {
        return "CollectionScopeImpl{" +
                "ownerId=" + ownerId +
                ", name='" + name + '\'' +
                '}';
    }
}
