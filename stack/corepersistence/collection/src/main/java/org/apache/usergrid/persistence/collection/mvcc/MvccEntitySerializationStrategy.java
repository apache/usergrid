package org.apache.usergrid.persistence.collection.mvcc;


import java.util.List;
import java.util.UUID;

import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.model.entity.Id;

import com.netflix.astyanax.MutationBatch;


/**
 * The interface that allows us to serialize an entity to disk
 */
public interface MvccEntitySerializationStrategy {

    /**
     * Serialize the entity to the data store with the given collection context
     *
     * @param entity The entity to persist
     *
     * @return The MutationBatch operations for this update
     */
    public MutationBatch write( CollectionScope context, MvccEntity entity );


    /**
     * Load and return the entity with the given id and a version that is <= the version provided
     *
     * @param context The context to persist the entity into
     * @param entityId The entity id to load
     * @param version The version to load.  This will return the version <= the given version
     *
     * @return The deserialized version of the entity.  Null if no version == to version exists. If the entity version
     *         has been cleared, the MvccEntity will be returned, but the optional entity will not be set
     */
    public MvccEntity load( CollectionScope context, Id entityId, UUID version );

    /**
     * Load a list, from highest to lowest of the entity with versions <= version up to maxSize elements
     *
     * @param context The context to persist the entity into
     * @param entityId The entity id to load
     * @param version The max version to seek from.  I.E a stored version <= this argument
     * @param maxSize The maximum size to return.  If you receive this size, there may be more versions to load.
     *
     * @return A list of entities up to max size ordered from max(UUID)=> min(UUID).  The return value should be null
     *         safe and return an empty list when there are no matches
     */
    public List<MvccEntity> load( CollectionScope context, Id entityId, UUID version, int maxSize );


    /**
     * DeleteCommit this version from the persistence store, but keep the version to mark that is has been cleared This
     * can be used in a mark+sweep system.  The entity with the given version will exist in the context, but no data
     * will be stored
     */
    public MutationBatch clear( CollectionScope context, Id entityId, UUID version );


    /**
     * DeleteCommit the entity from the context with the given entityId and version
     *
     * @param context The context that contains the entity
     * @param entityId The entity id to delete
     * @param version The version to delete
     */
    public MutationBatch delete( CollectionScope context, Id entityId, UUID version );
}
