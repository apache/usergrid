package org.apache.usergrid.persistence.collection.mvcc.stage;


import java.util.UUID;

import org.apache.usergrid.persistence.collection.CollectionContext;
import org.apache.usergrid.persistence.model.entity.Entity;


/**
 * Interface to define mvcc operations
 *
 * TODO: Not sure we need this any more
 */
public interface MvccStrategy {

    /**
     * Start progress through states for this entity
     *
     * @param context The context this entity belongs in
     * @param entityId The entity id to assign to this entity
     * @param entity The entity values to write
     */
    public WriteStage beingWrite( CollectionContext context, UUID entityId, Entity entity );


    /**
     * Get the current stage of the entity in the context at the current version.  Should be used for write verification
     * on resume
     *
     * @param context The context this entity belongs in
     * @param entityId The entity Id to search for in the context
     * @param version The version of the entityId to review
     */
    public WriteStage getCurrentState( CollectionContext context, UUID entityId, UUID version );


    /**
     * Get the write stage of the entity in the context with a version <= the current version and a stage of Comitted
     */
    public WriteStage getCurrentStateOfEntity( CollectionContext context, UUID entityId, UUID maxVersion );
}
