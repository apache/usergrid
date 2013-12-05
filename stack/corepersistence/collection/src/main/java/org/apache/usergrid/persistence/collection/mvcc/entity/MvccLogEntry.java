package org.apache.usergrid.persistence.collection.mvcc.entity;


import java.util.UUID;


/**
 * A Marker interface for an in flight update to allow context information to be passed between states
 */
public interface MvccLogEntry {


    /**
     * Get the stage for the current version
     */
    Stage getStage();

    /**
     * Get the entity to add info to the log
     * @return
     */
    UUID getEntityId();

    /**
     * Get the version of the entity
     * @return
     */
    UUID getVersion();


}
