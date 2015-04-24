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
package org.apache.usergrid.persistence.collection;


import java.util.Collection;

import org.apache.usergrid.persistence.core.util.Health;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.field.Field;

import rx.Observable;


/**
 * The operations for performing changes on an entity
 */
public interface EntityCollectionManager {

    /**
     * Write the entity in the entity collection.  This is an entire entity, it's contents will
     * completely overwrite the previous values, if it exists.
     *
     * @param entity The entity to update
     *
     * @return the Observable with the updated entity in the body
     */
    Observable<Entity> write( Entity entity );


    /**
     * @param entityId MarkCommit the entity as deleted
     *
     * @return The observable of the id after the operation has completed
     */
    Observable<Id> delete( Id entityId );

    /**
     * @param entityId The entity id to load.
     *
     * @return The observable with the entity
     *
     * Load the entity with the given entity Id
     */
    Observable<Entity> load( Id entityId );

    /**
     * @param entityIds Returns a version set with the latest version for each of the entities
     * Return the latest versions of the specified entityIds
     *
     * @return A versionset that has all the latest versions for the specified Ids that could be found
     */
    Observable<VersionSet> getLatestVersion( Collection<Id> entityIds );


    /**
     * Get a fieldset of all fields from the entities
     * @param entityType The type of entity.  From the "type" field in the id.
     * @param fields The collection of fields to search
     * @return
     */
    Observable<FieldSet> getEntitiesFromFields( String entityType, Collection<Field> fields );

    /**
     * Gets the Id for a field
     * @param entityType the type field from the Id object
     * @param field The field to search for
     *
     * @return most likely a single Id, watch for onerror events
     */
    Observable<Id> getIdField( String entityType, Field field );


    /**
     * @param entityIds The entityIds for loading a collection
     * Load all the entityIds into the observable entity set
     *
     * @return An EntitySet with the latest data of every entity that could be located
     */
    Observable<EntitySet> load( Collection<Id> entityIds );


    /**
     * Returns health of entity data store.
     */
    Health getHealth();

}
