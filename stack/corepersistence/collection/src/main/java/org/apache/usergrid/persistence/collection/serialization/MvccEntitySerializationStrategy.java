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
package org.apache.usergrid.persistence.collection.serialization;


import java.util.Collection;
import java.util.Iterator;
import java.util.UUID;

import org.apache.usergrid.persistence.collection.EntitySet;
import org.apache.usergrid.persistence.collection.MvccEntity;
import org.apache.usergrid.persistence.core.migration.data.VersionedData;
import org.apache.usergrid.persistence.core.migration.schema.Migration;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.base.Optional;
import com.netflix.astyanax.MutationBatch;


/**
 * The interface that allows us to serialize an entity to disk
 */
public interface MvccEntitySerializationStrategy extends Migration, VersionedData {

    /**
     * Serialize the entity to the data store with the given collection context
     *
     * @param entity The entity to persist
     * @return The MutationBatch operations for this update
     */
    MutationBatch write( ApplicationScope context, MvccEntity entity );


    /**
     * Load the entities into the entitySet from the specified Ids.  Loads versions <= the maxVersion
     *
     * @param scope
     * @param entityIds
     * @return
     */
    EntitySet load( ApplicationScope scope, Collection<Id> entityIds, UUID maxVersion );

    /**
     * Load a list, from highest to lowest of the entity with versions <= version up to maxSize elements
     *
     * @param context   The context to persist the entity into
     * @param entityId  The entity id to load
     * @param version   The max version to seek from.  I.E a stored version <= this argument
     * @param fetchSize The fetch size to return for each trip to cassandra.
     * @return An iterator of entities ordered from max(UUID)=> min(UUID).  The return value should be null
     * safe and return an empty list when there are no matches
     */
    @Deprecated
    //this has been made obsolete in the latest version, only use the load methods
    Iterator<MvccEntity> loadDescendingHistory( ApplicationScope context, Id entityId, UUID version, int fetchSize );

    /**
     * Load a historical list of entities, from lowest to highest entity with versions < version up to maxSize elements
     *
     * @param context The context to persist the entity into
     * @param entityId The entity id to load
     * @param version The max version to seek to.  I.E a stored version < this argument
     * @param fetchSize The fetch size to return for each trip to cassandra.
     *
     * @return An iterator of entities ordered from min(UUID)=> max(UUID).  The return value should be null safe and
     * return an empty list when there are no matches
     */
    @Deprecated
    //this has been made obsolete in the latest version, only use the load methods
    Iterator<MvccEntity> loadAscendingHistory( ApplicationScope context, Id entityId, UUID version, int fetchSize );


    /**
     * Load a single entity.  A convenience method.  when multiple entiites are to be loaded, DO NOT use this method
     * it will be horribly inefficient on network I/o.
     * @param scope
     * @param entityId
     * @return The MvccEntity if it exists.  Null otherwise
     */
    Optional<MvccEntity> load( ApplicationScope scope, Id entityId );


    /**
     * Mark this  this version as deleted from the persistence store, but keep the version to mark that is has been cleared This
     * can be used in a mark+sweep system.  The entity with the given version will exist in the context, but no data
     * will be stored
     */
    MutationBatch mark( ApplicationScope context, Id entityId, UUID version );


    /**
     * Delete the entity from the context with the given entityId and version
     *
     * @param context  The context that contains the entity
     * @param entityId The entity id to delete
     * @param version  The version to delete
     */
    MutationBatch delete( ApplicationScope context, Id entityId, UUID version );

}
