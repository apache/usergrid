package org.apache.usergrid.persistence.collection.mvcc.entity;


import java.util.UUID;

import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.base.Optional;


/**
 * An entity with internal information for versioning
 */
public interface MvccEntity {


    /**
     * Get the entity for this context.
     *
     * @return This will return absent if no data is present.  Otherwise the entity will be contained within the
     *         optional
     */
    Optional<Entity> getEntity();

    /**
     * Return the version of this entityId we are attempting to write used in the current context
     */
    UUID getVersion();

    /**
     * Get the UUID of the entity
     */
    Id getId();
}
