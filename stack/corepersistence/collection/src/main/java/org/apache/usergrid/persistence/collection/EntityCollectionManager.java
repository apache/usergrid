package org.apache.usergrid.persistence.collection;


import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;

import rx.Observable;


/**
 *
 *
 * @author: tnine
 *
 */
public interface EntityCollectionManager {

    /**
     * Write the entity in the entity collection.
     *
     * @param entity The entity to update
     */
    public Observable<Entity> write( Entity entity );


    /**
     * DeleteCommit the entity and remove it's indexes with the given entity id
     */
    public Observable<Void> delete( Id entityId );

    /**
     * Load the entity with the given entity Id
     */
    public Observable<Entity> load( Id entityId );
}
