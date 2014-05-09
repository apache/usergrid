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

package org.apache.usergrid.persistence.index;

import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.index.query.Query;
import org.apache.usergrid.persistence.index.query.Results;
import org.apache.usergrid.persistence.model.entity.Id;

import java.util.UUID;


/**
 * Provides indexing of Entities within a scope.
 */
public interface EntityIndex {

    /** 
     * Create index for Entity
     * @param entity Entity to be indexed.
     */
    public void index( CollectionScope scope, Entity entity );
    

    /**
     * Remove index of entity.
     * @param entity Entity to be removed from index. 
     */
    public void deindex( CollectionScope collScope, Entity entity );
   

    /**
     * Execute query in Usergrid syntax.
     */
    public Results search( CollectionScope scope, Query query );


    /** 
     * Search within Connections of a specific Entity and Connection Type. 
     * @param query Represents Query created from Usergrid syntax query string.
     */
    Results searchConnections( Entity source, String type, Query query  );


    /** 
     * Index a named and one-way Connection from a Source to a Target entity. 
     * @param target Entity that is the target of the Connection (e.g. beer).
     * @param targetScope CollectionScope of the target Entity (e.g. beverages)
     */
    void indexConnection( Entity source, String type, Entity target, CollectionScope targetScope );   


    /** 
     * Delete single Connection from index. 
     * @param target Entity that is the target of the Connection (e.g. beer).
     */
    void deindexConnection( Id source, String type, Entity target );

    /**
     * Find versions prior to this version
     * @param id entity id
     * @param version find version <= this version
     * @param collScope CollectionScope of the entity
     */
    Results getEntityVersions (Id id, UUID version,CollectionScope collScope);

    /**
     * Force refresh of index (should be used for testing purposes only).
     */
    public void refresh();
}
