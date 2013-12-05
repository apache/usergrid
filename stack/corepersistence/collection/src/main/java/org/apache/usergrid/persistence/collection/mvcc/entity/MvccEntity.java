package org.apache.usergrid.persistence.collection.mvcc.entity;


import java.util.UUID;

import org.apache.usergrid.persistence.model.entity.Entity;

import com.google.common.base.Optional;


/**
 * A Marker interface for an in flight update to allow context information to be passed between states
 */
public interface MvccEntity {


    /**
     * Get the entity for this context.
     * @return This will return absent if no data is present.  Otherwise the entity will be contained within the optional
     */
    Optional<Entity> getEntity();

    /**
     * Return the version of this entityId we are attempting to write used in the current context
     */
    UUID getVersion();

    /**
     * Get the UUID of the entity
     */
    UUID getUuid();

}
