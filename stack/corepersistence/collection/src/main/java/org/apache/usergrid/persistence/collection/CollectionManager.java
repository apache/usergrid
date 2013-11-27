package org.apache.usergrid.persistence.collection;


import java.util.UUID;

import org.apache.usergrid.persistence.model.entity.Entity;


/**
 *
 *
 * @author: tnine
 *
 */
public interface CollectionManager
{

    /**
     * Create the entity in the collection.  Only use for entities your are sure are new.
     *
     * @param entity The entity to update
     */
    public void create( Entity entity );

    /**
     * Update the entity with the given fields.
     *
     * @param entity The entity properties to update
     */
    public void update( Entity entity );

    /** Delete the entity and remove it's indexes with the given entity id */
    public void delete( UUID entityId );

    /**
     * Load the entity with the given entity Id
     * @param entityId
     * @return
     */
    public Entity load(UUID entityId);
}
