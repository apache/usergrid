package org.apache.usergrid.persistence.collection.mvcc.entity;


import java.util.UUID;

import org.apache.usergrid.persistence.collection.CollectionContext;
import org.apache.usergrid.persistence.model.entity.Entity;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;


/**
 * @author tnine
 */
public class MvccEntityImpl implements MvccEntity {

    private final CollectionContext context;
    private final UUID entityId;
    private final UUID version;
    private final Optional<Entity> entity;


    public MvccEntityImpl( final CollectionContext context, final UUID entityId, final UUID version,
                           final Optional<Entity> entity ) {
        Preconditions.checkNotNull( context, "context is required" );
        Preconditions.checkNotNull( entityId, "entity id is required" );
        Preconditions.checkNotNull( version, "version id is required" );
        Preconditions.checkNotNull( entity, "entity  is required" );

        this.context = context;
        this.entityId = entityId;
        this.version = version;
        this.entity = entity;
    }


    @Override
    public Optional<Entity> getEntity() {
        return entity;
    }


    @Override
    public UUID getVersion() {
        return version;
    }


    @Override
    public UUID getUuid() {
        return entityId;
    }


    @Override
    public CollectionContext getContext() {
        return context;
    }


    @Override
    public boolean equals( final Object o ) {
        if ( this == o ) {
            return true;
        }
        if ( o == null || getClass() != o.getClass() ) {
            return false;
        }

        final MvccEntityImpl that = ( MvccEntityImpl ) o;

        if ( !context.equals( that.context ) ) {
            return false;
        }
        if ( !getUuid().equals( that.getUuid() ) ) {
            return false;
        }

        if ( !getVersion().equals( that.getVersion() ) ) {
            return false;
        }

        return true;
    }


    @Override
    public int hashCode() {
        int result = context.hashCode();
        result = 31 * result + getUuid().hashCode();
        result = 31 * result + getVersion().hashCode();
        return result;
    }


    @Override
    public String toString() {
        return "MvccEntityImpl{" +
                "context=" + context +
                ", entityId=" + entityId +
                ", version=" + version +
                ", entity=" + entity +
                '}';
    }
}
