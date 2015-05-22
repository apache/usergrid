/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.usergrid.persistence;


import java.nio.ByteBuffer;
import org.apache.usergrid.persistence.Query;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.usergrid.persistence.Query.Level;

import me.prettyprint.hector.api.mutation.Mutator;


public interface RelationManager {

    public Set<String> getCollectionIndexes( String collectionName ) throws Exception;

    public Map<String, Map<UUID, Set<String>>> getOwners() throws Exception;


    /**
     * Returns true if the entity ref if a member of the owner ref for the current relation manager
     *
     * @param collectionName The name of the collection
     */
    public boolean isCollectionMember( String collectionName, EntityRef entity ) throws Exception;

    /** Returns true if the target entity is currently connected to the owner ref of this relation manager */
    public boolean isConnectionMember( String connectionName, EntityRef entity ) throws Exception;

    public Set<String> getCollections() throws Exception;

    public Results getCollection( String collectionName, UUID startResult, int count, Level resultsLevel,
                                  boolean reversed ) throws Exception;

    public Results getCollection( String collectionName, Query query, Level resultsLevel ) throws Exception;

    public Entity addToCollection( String collectionName, EntityRef itemRef ) throws Exception;

    public Entity createItemInCollection( String collectionName, String itemType, Map<String, Object> properties )
            throws Exception;

    public void removeFromCollection( String collectionName, EntityRef itemRef ) throws Exception;

    public void copyRelationships( String srcRelationName, EntityRef dstEntityRef, String dstRelationName )
            throws Exception;

    public Results searchCollection( String collectionName, Query query ) throws Exception;

    /**
     * this loops for consistentcy and is dangerous to run often
     * @param collectionName
     * @param query
     * @param expectedResults
     * @return
     * @throws Exception
     */
    public Results searchCollectionConsistent( String collectionName, Query query, int expectedResults ) throws Exception;

    public ConnectionRef createConnection( ConnectionRef connection ) throws Exception;

    public ConnectionRef createConnection( String connectionType, EntityRef connectedEntityRef ) throws Exception;

    public ConnectionRef createConnection( String pairedConnectionType, EntityRef pairedEntity, String connectionType,
                                           EntityRef connectedEntityRef ) throws Exception;

    public ConnectionRef createConnection( ConnectedEntityRef... connections ) throws Exception;

    public ConnectionRef connectionRef( String connectionType, EntityRef connectedEntityRef ) throws Exception;

    public ConnectionRef connectionRef( String pairedConnectionType, EntityRef pairedEntity, String connectionType,
                                        EntityRef connectedEntityRef ) throws Exception;

    public ConnectionRef connectionRef( ConnectedEntityRef... connections );

    public void deleteConnection( ConnectionRef connectionRef ) throws Exception;

    public Set<String> getConnectionTypes( UUID connectedEntityId ) throws Exception;

    public Set<String> getConnectionTypes() throws Exception;

    public Set<String> getConnectionTypes( boolean filterConnection ) throws Exception;

    /**
     * Get all entities connected to this entity.  Also get all
     *
     * @param connectionType The type/name of the connection
     * @param connectedEntityType The type of
     */
    public Results getTargetEntities(String connectionType, String connectedEntityType, Level resultsLevel)
            throws Exception;

    public Results getSourceEntities(String connectionType, String connectedEntityType,
                                     Level resultsLevel) throws Exception;

    // public Results searchConnectedEntitiesForProperty(String connectionType,
    // String connectedEntityType, String propertyName,
    // Object searchStartValue, Object searchFinishValue,
    // UUID startResult, int count, boolean reversed, Level resultsLevel)
    // throws Exception;

    public Results getSourceEntities(
        String connectionType, String entityType, Level level, int count) throws Exception;

	public Results searchTargetEntities(Query query) throws Exception;


    public Set<String> getConnectionIndexes( String connectionType ) throws Exception;


}
