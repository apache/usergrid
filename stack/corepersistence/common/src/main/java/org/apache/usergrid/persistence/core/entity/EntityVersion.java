package org.apache.usergrid.persistence.core.entity;

import org.apache.usergrid.persistence.model.entity.Id;

import java.util.UUID;

/**
 * Created by ApigeeCorporation on 5/19/14.
 */
public interface EntityVersion {
    /**
     * Return the version of this entityId we are attempting to write used in the current context
     */
    UUID getVersion();

    /**
     * Get the UUID of the entity
     */
    Id getId();
}
