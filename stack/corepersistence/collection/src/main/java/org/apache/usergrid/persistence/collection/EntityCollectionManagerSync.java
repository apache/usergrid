package org.apache.usergrid.persistence.collection;


import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;

import rx.Observable;


/**
 *
 * A synchronous implementation that will block until the call is returned.
 * @author: tnine
 *
 */
public interface EntityCollectionManagerSync {

    /**
     * Write the entity in the entity collection.
     *
     * @param entity The entity to update
     */
    public Entity write( Entity entity );


    /**
     * DeleteCommit the entity and remove it's indexes with the given entity id
     */
    public void delete( Id entityId );

    /**
     * Load the entity with the given entity Id
     */
    public Entity load( Id entityId );
}
