package org.apache.usergrid.persistence.collection.event;


import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.model.entity.Id;


/**
 *
 * Invoked when an entity is deleted.  The delete log entry is not removed until all instances of this listener has completed.
 * If any listener fails with an exception, the entity will not be removed.
 *
 */
public interface EntityDeleted {


    /**
     * The event fired when an entity is deleted
     *
     * @param scope The scope of the entity
     * @param entityId The id of the entity
     */
    public void deleted( final CollectionScope scope, final Id entityId);

}
