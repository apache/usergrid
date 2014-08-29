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
     * MarkCommit the entity and remove it's indexes with the given entity id
     */
    public Observable<Void> delete( Id entityId );

    /**
     * Load the entity with the given entity Id
     */
    public Observable<Entity> load( Id entityId );


    //TODO add partial update

    /**
     * Takes the change and reloads an entity with all changes applied.
     * @param entity
     * @return
     */
    public Observable<Entity> update ( Entity entity );
}
