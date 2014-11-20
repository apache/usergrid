/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.usergrid.persistence.collection.mvcc;


import java.util.Collection;
import java.util.Iterator;
import java.util.UUID;

import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.EntitySet;
import org.apache.usergrid.persistence.collection.MvccEntity;
import org.apache.usergrid.persistence.core.migration.schema.Migration;
import org.apache.usergrid.persistence.model.entity.Id;

import com.netflix.astyanax.MutationBatch;


/**
 * The interface that allows us to serialize an entity to disk
 */
public interface MvccEntitySerializationStrategy extends Migration {

    /**
     * Serialize the entity to the data store with the given collection context
     *
     * @param entity The entity to persist
     *
     * @return The MutationBatch operations for this update
     */
    public MutationBatch write( CollectionScope context, MvccEntity entity );



    /**
     * Load the entities into the entitySet from the specified Ids.  Loads versions <= the maxVersion
     * @param scope
     * @param entityIds
     * @return
     */
    public EntitySet load( CollectionScope scope, Collection<Id> entityIds, UUID maxVersion);

    /**
     * Load a list, from highest to lowest of the entity with versions <= version up to maxSize elements
     *
     * @param context The context to persist the entity into
     * @param entityId The entity id to load
     * @param version The max version to seek from.  I.E a stored version <= this argument
     * @param fetchSize The maximum size to return.  If you receive this size, there may be more versions to load.
     *
     * @return A list of entities up to max size ordered from max(UUID)=> min(UUID).  The return value should be null
     *         safe and return an empty list when there are no matches
     */
    public Iterator<MvccEntity> load( CollectionScope context, Id entityId, UUID version, int fetchSize );

    /**
     * Load a historical list of entities, from highest to lowest of the entity with versions < version up to maxSize elements
     *
     * @param context The context to persist the entity into
     * @param entityId The entity id to load
     * @param version The max version to seek from.  I.E a stored version < this argument
     * @param fetchSize The maximum size to return.  If you receive this size, there may be more versions to load.
     * @return A list of entities up to max size ordered from max(UUID)=> min(UUID).  The return value should be null
     *         safe and return an empty list when there are no matches
     */
    public Iterator<MvccEntity> loadHistory( CollectionScope context, Id entityId, UUID version, int fetchSize );

    /**
     * Mark this  this version as deleted from the persistence store, but keep the version to mark that is has been cleared This
     * can be used in a mark+sweep system.  The entity with the given version will exist in the context, but no data
     * will be stored
     */
    public MutationBatch mark( CollectionScope context, Id entityId, UUID version );


    /**
     * Delete the entity from the context with the given entityId and version
     *
     * @param context The context that contains the entity
     * @param entityId The entity id to delete
     * @param version The version to delete
     */
    public MutationBatch delete( CollectionScope context, Id entityId, UUID version );
}
