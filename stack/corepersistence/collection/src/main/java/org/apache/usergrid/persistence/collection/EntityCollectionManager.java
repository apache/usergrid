package org.apache.usergrid.persistence.collection;


import java.util.UUID;

import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;


/**
 *
 *
 * @author: tnine
 *
 */
public interface EntityCollectionManager
{

    /**
     * Write the entity in the entity collection.
     *
     * @param entity The entity to update
     */
    public Entity write( Entity entity );


    /** Delete the entity and remove it's indexes with the given entity id */
    public void delete( Id entityId );

    /**
     * Load the entity with the given entity Id
     * @param entityId
     * @return
     */
    public Entity load(Id entityId);
}
