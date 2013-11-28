package org.apache.usergrid.persistence.collection.mvcc.entity;


import java.util.UUID;

import org.apache.usergrid.persistence.collection.CollectionContext;

import com.google.common.base.Preconditions;


/**
 * The simple implementation of a log entry
 *
 * @author tnine
 */
public class MvccLogEntryImpl implements MvccLogEntry {

    private final CollectionContext context;
    private final UUID entityId;
    private final UUID version;
    private final Stage stage;


    public MvccLogEntryImpl( final CollectionContext context, final UUID entityId, final UUID version,
                             final Stage stage ) {

        Preconditions.checkNotNull( context, "context is required" );
        Preconditions.checkNotNull( entityId, "entity id is required" );
        Preconditions.checkNotNull( version, "version id is required" );
        Preconditions.checkNotNull( stage, "entity  is required" );


        this.context = context;
        this.entityId = entityId;
        this.version = version;
        this.stage = stage;
    }


    @Override
    public Stage getStage() {
        return stage;
    }


    @Override
    public UUID getEntityId() {
        return entityId;
    }


    @Override
    public UUID getVersion() {
        return version;
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

        final MvccLogEntryImpl that = ( MvccLogEntryImpl ) o;

        if ( !context.equals( that.context ) ) {
            return false;
        }
        if ( !entityId.equals( that.entityId ) ) {
            return false;
        }
        if ( !version.equals( that.version ) ) {
            return false;
        }

        if ( stage != that.stage ) {
            return false;
        }


        return true;
    }


    @Override
    public int hashCode() {
        int result = context.hashCode();
        result = 31 * result + entityId.hashCode();
        result = 31 * result + version.hashCode();
        result = 31 * result + stage.hashCode();
        return result;
    }


    @Override
    public String toString() {
        return "MvccLogEntryImpl{" +
                "context=" + context +
                ", entityId=" + entityId +
                ", version=" + version +
                ", stage=" + stage +
                '}';
    }
}
