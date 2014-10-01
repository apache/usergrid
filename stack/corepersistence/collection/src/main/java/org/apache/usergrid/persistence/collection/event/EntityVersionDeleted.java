package org.apache.usergrid.persistence.collection.event;


import java.util.List;
import java.util.UUID;

import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.model.entity.Id;


/**
 *
 * Invoked when an entity version is removed.  Note that this is not a deletion of the entity itself,
 * only the version itself.
 *
 */
public interface EntityVersionDeleted {


    /**
     * The version specified was removed.
     *
     * @param scope The scope of the entity
     * @param entityId The entity Id that was removed
     * @param entityVersions The versions that are to be removed
     */
    public void versionDeleted(final CollectionScope scope, final Id entityId, final List<UUID> entityVersions);

}
