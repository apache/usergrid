package org.apache.usergrid.persistence.collection.event;


import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.model.entity.Entity;


/**
 *
 * Invoked after a new version of an entity has been created.  The entity should be a complete
 * view of the entity.
 *
 */
public interface EntityVersionCreated {


    /**
     * The new version of the entity.  Note that this should be a fully merged view of the entity.
     * In the case of partial updates, the passed entity should be fully merged with it's previous entries
     * @param scope The scope of the entity
     * @param entity The fully loaded and merged entity
     */
    public void versionCreated( final CollectionScope scope, final Entity entity );

}
