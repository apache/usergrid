package org.apache.usergrid.persistence.collection.mvcc.entity.impl;

import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.core.scope.OrganizationScope;

import java.io.Serializable;
import java.util.UUID;

/**
 * Created by ApigeeCorporation on 4/28/14.
 */
public class MvccEntityEvent<T> implements Serializable {
    private final CollectionScope collectionScope;
    private final T data;
    private final UUID version;


    public MvccEntityEvent( final CollectionScope collectionScope, final UUID version, final T data ) {
        this.collectionScope = collectionScope;
        this.data = data;
        this.version = version;
    }


    public CollectionScope getCollectionScope() {
        return collectionScope;
    }


    public UUID getVersion() {
        return version;
    }


    public T getData() {
        return data;
    }
}
