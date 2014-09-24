package org.apache.usergrid.persistence.collection.event;


import java.util.UUID;

import org.apache.usergrid.persistence.collection.CollectionScope;


/**
 *
 * Invoked when an entity version is removed.  Note that this is not a deletion of the entity itself,
 * only the version itself.
 *
 */
public interface EntityVersionRemoved {


    /**
     * The version specified was removed.
     * @param scope
     * @param entityId
     * @param entityVersion
     */
    public void versionRemoved(final CollectionScope scope, final UUID entityId, final UUID entityVersion);

}
