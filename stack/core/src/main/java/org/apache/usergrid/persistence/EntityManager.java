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


import org.apache.usergrid.persistence.index.EntityIndex;
import org.apache.usergrid.persistence.index.query.Query;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import me.prettyprint.hector.api.mutation.Mutator;

import org.apache.usergrid.persistence.cassandra.CassandraService;
import org.apache.usergrid.persistence.cassandra.GeoIndexManager;
import org.apache.usergrid.persistence.core.util.Health;
import org.apache.usergrid.persistence.entities.Application;
import org.apache.usergrid.persistence.entities.Role;
import org.apache.usergrid.persistence.index.query.CounterResolution;
import org.apache.usergrid.persistence.index.query.Identifier;
import org.apache.usergrid.persistence.index.query.Query.Level;


/**
 * The interface class for the data access object for Applications. Each application contains a set of users as well as
 * a hierarchy of groups. A application also includes a set of message inboxes and a set of assets.
 */
public interface EntityManager {

//    public void setApplicationId( UUID applicationId );

    public GeoIndexManager getGeoIndexManager();

    public EntityRef getApplicationRef();

    public Application getApplication() throws Exception;

    public void updateApplication( Application app ) throws Exception;

    public void updateApplication( Map<String, Object> properties ) throws Exception;

    public RelationManager getRelationManager( EntityRef entityRef );

    /** Get all collections for the application. Includes both user defined collections and schema collections */
    public Set<String> getApplicationCollections() throws Exception;

    public Map<String, Object> getApplicationCollectionMetadata() throws Exception;

    public long getApplicationCollectionSize( String collectionName ) throws Exception;

    /**
     * Creates an entity of the specified type attached to the specified application.
     *
     * @param entityType the type of the entity to create.
     * @param properties property values to create in the new entity or null.
     *
     * @return the newly created entity object.
     */
    public Entity create( String entityType, Map<String, Object> properties ) throws Exception;

    public <A extends Entity> A create( String entityType, Class<A> entityClass, Map<String, Object> properties )
            throws Exception;

    public <A extends TypedEntity> A create( A entity ) throws Exception;

    /**
     * Creates an entity of the specified type attached to the specified application.
     *
     * @param importId the UUID to assign to the imported entity
     * @param entityType the type of the entity to create.
     * @param properties property values to create in the new entity or null.
     *
     * @return the newly created entity object.
     *
     * @throws Exception the exception
     */
    public Entity create( UUID importId, String entityType, Map<String, Object> properties )
            throws Exception;

    public void createApplicationCollection( String entityType ) throws Exception;

    public EntityRef getAlias( String aliasType, String alias ) throws Exception;

    /**
     * Get the entity ref from the value
     *
     * @param ownerRef The owner Id of the collection
     * @param collectionName The name of the collection
     * @param aliasValue The value of the alias
     */
    public EntityRef getAlias( EntityRef ownerRef, String collectionName, String aliasValue )
            throws Exception;

    public Map<String, EntityRef> getAlias( String aliasType, List<String> aliases ) throws Exception;

    /**
     * Get aliases from the index with the given value
     *
     * @param ownerRef The id of the collection owner
     * @param collectionName The name of the collection
     * @param aliases The alias property
     */
    public Map<String, EntityRef> getAlias( EntityRef ownerRef, String collectionName,
            List<String> aliases ) throws Exception;

    /**
     * Validates that the entity exists in the datastore meaning that it exists and the type has
     * been loaded if not already provided.
     *
     * @return an validated EntityRef or null.
     */
    public EntityRef validate( EntityRef entityRef ) throws Exception;

    /**
     * Retrieves the entity for the specified entity reference.
     *
     * @param entityRef an Entity reference
     *
     * @return an Entity object for the specified entity reference.
     */
    public Entity get( EntityRef entityRef ) throws Exception;

    public <A extends Entity> A get( UUID entityId, Class<A> entityClass ) throws Exception;

    /**
     * Retrieves a set of Entitues cast to the specified class type.
     *
     * @return a list of entity objects.
     */
    public Results get( Collection<UUID> entityIds, Class<? extends Entity> entityClass,
            Level resultsLevel ) throws Exception;

    /**
     * Retrieves a set of Entities cast to the specified class type.
     *
     * @return a list of entity objects.
     */
    public Results get( Collection<UUID> entityIds, String entityType,
        Class<? extends Entity> entityClass, Level resultsLevel ) throws Exception;

    public Results getEntities(List<UUID> ids, String type);

    /**
     * Updates the entity with the properties and values in the Entity Object.
     *
     * @param entity an Entity object.
     */
    public void update( Entity entity ) throws Exception;

    /**
     * Gets the value for a named entity property. Entity properties must be defined in the schema
     *
     * @param entityRef an entity reference
     * @param propertyName the property name to retrieve.
     *
     * @return the value of the named property or null.
     *
     * @throws Exception the exception
     */
    public Object getProperty( EntityRef entityRef, String propertyName ) throws Exception;

    /**
     * Do a single load of all entities with the given properties.  Efficient if you have a subset of properties, and
     * know the ids of them.  The entity UUID is in the key, the runtime subtype of Entity is in the value.  Note that
     * if an entity cannot be loaded (id doesn't exist) it is simply ignored
     */
    public List<Entity> getPartialEntities( Collection<UUID> ids, Collection<String> properties ) throws Exception;

    /**
     * Gets the properties for the specified entity property.
     *
     * @param entityRef an entity reference
     *
     * @return the property values.
     *
     * @throws Exception the exception
     */
    public Map<String, Object> getProperties( EntityRef entityRef ) throws Exception;

    /**
     * Sets the value for a named entity property. If the property is being index, the index is updated to remove the
     * old value and add the new value.
     *
     * @param entityRef an entity reference
     * @param propertyName the property to set.
     * @param propertyValue new value for property.
     *
     * @throws Exception the exception
     */
    public void setProperty( EntityRef entityRef, String propertyName, Object propertyValue ) throws Exception;

    /**
     * You should only use this method if you are absolutely sure what you're doing. Use setProperty without the
     * override param in most cases. With great power comes great responsibility....
     *
     * @param override set to true to force this value to persist. This will ignore all mutable attributes as well as
     * validation. Use with care
     */
    void setProperty( EntityRef entityRef, String propertyName, Object propertyValue, boolean override )
            throws Exception;

    /**
     * Updates the properties for the specified entity.
     *
     * @param entityRef an entity reference
     * @param properties the properties
     *
     * @throws Exception the exception
     */
    public void updateProperties( EntityRef entityRef, Map<String, Object> properties )
            throws Exception;

    public void deleteProperty( EntityRef entityRef, String propertyName ) throws Exception;

    /**
     * Gets the values from an entity list property. Lists are a special type of entity property
     * that can contain an unordered set of non-duplicate values.
     *
     * @param entityRef an entity reference
     * @param dictionaryName the property list name to retrieve.
     *
     * @return the value of the named property or null.
     *
     * @throws Exception the exception
     */
    public Set<Object> getDictionaryAsSet( EntityRef entityRef, String dictionaryName )
            throws Exception;

    /**
     * Adds the specified value to the named entity list property. Lists are a special type of
     * entity property that can contain an unordered set of non-duplicate values.
     *
     * @param entityRef an entity reference
     * @param dictionaryName the property to set.
     * @param elementValue new value for property.
     *
     * @throws Exception the exception
     */
    public void addToDictionary( EntityRef entityRef, String dictionaryName, Object elementValue )
            throws Exception;

    public void addToDictionary( EntityRef entityRef, String dictionaryName, Object elementName,
            Object elementValue ) throws Exception;

    public void addSetToDictionary( EntityRef entityRef, String dictionaryName,
            Set<?> elementValues ) throws Exception;

    public void addMapToDictionary( EntityRef entityRef, String dictionaryName,
            Map<?, ?> elementValues ) throws Exception;

    public Map<Object, Object> getDictionaryAsMap( EntityRef entityRef, String dictionaryName )
            throws Exception;

    public Object getDictionaryElementValue( EntityRef entityRef, String dictionaryName,
            String elementName ) throws Exception;

    /**
     * Removes the specified value to the named entity list property. Lists are a special type of
     * entity property that can contain an unordered set of non-duplicate values.
     *
     * @param entityRef an entity reference
     * @param dictionaryName the property to set.
     * @param elementValue new value for property.
     *
     * @throws Exception the exception
     */
    public void removeFromDictionary( EntityRef entityRef, String dictionaryName, Object elementValue )
            throws Exception;

    public Set<String> getDictionaries( EntityRef entity ) throws Exception;

    /**
     * Deletes the specified entity.
     *
     * @param entityRef an entity reference
     *
     * @throws Exception the exception
     */
    public void delete( EntityRef entityRef ) throws Exception;

    /**
     * Gets the entities and collections that the specified entity is a member of.
     *
     * @param entityRef an entity reference
     *
     * @return a map of entity references to set of collection names for the entities and
     * collections that this entity is a member of.
     *
     * @throws Exception the exception
     */
    public Map<String, Map<UUID, Set<String>>> getOwners( EntityRef entityRef ) throws Exception;

    /**
     * Return true if the owner entity ref is an owner of the entity;
     *
     * @param owner The owner of the collection
     * @param collectionName The collection name
     * @param entity The entity in the collection
     */
    public boolean isCollectionMember( EntityRef owner, String collectionName, EntityRef entity )
            throws Exception;

    /**
     * Return true if the owner entity ref is an owner of the entity;
     *
     * @param owner The owner of the collection
     * @param connectionName The collection name
     * @param entity The entity in the collection
     */
    public boolean isConnectionMember( EntityRef owner, String connectionName, EntityRef entity )
            throws Exception;


    /**
     * Gets the collections for the specified entity. Collection for a given type are encoded
     * in the schema, this method loads the entity type and returns the collections from the schema.
     *
     * @param entityRef an entity reference
     *
     * @return the collections for the entity type of the given entity.
     *
     * @throws Exception the exception
     */
    public Set<String> getCollections( EntityRef entityRef ) throws Exception;

    /**
     * Gets a list of entities in the specified collection belonging to the specified entity.
     *
     * @param entityRef an entity reference
     * @param collectionName the collection name.
     * @param startResult the start result
     * @param count the count
     *
     * @return a list of entities in the specified collection.
     *
     * @throws Exception the exception
     */
    public Results getCollection( EntityRef entityRef, String collectionName, UUID startResult, int count,
                                  Level resultsLevel, boolean reversed ) throws Exception;


    public Results getCollection( UUID entityId, String collectionName, Query query, Level resultsLevel )
            throws Exception;

    /**
     * Adds an entity to the specified collection belonging to the specified entity entity.
     *
     * @param entityRef an entity reference
     * @param collectionName the collection name.
     * @param itemRef an entity to be added to the collection.
     *
     * @throws Exception the exception
     */
    public Entity addToCollection( EntityRef entityRef, String collectionName, EntityRef itemRef ) throws Exception;

    public Entity addToCollections( List<EntityRef> ownerEntities, String collectionName, EntityRef itemRef )
            throws Exception;

    /**
     * Create the item in a sub collection
     *
     * @param entityRef The owning entity
     * @param collectionName The name of the collection
     * @param itemType The type of the item
     * @param properties The properties for the item
     */
    public Entity createItemInCollection( EntityRef entityRef, String collectionName, String itemType,
                                          Map<String, Object> properties ) throws Exception;

    /**
     * Removes an entity to the specified collection belonging to the specified entity.
     *
     * @param entityRef an entity reference
     * @param collectionName the collection name.
     * @param itemRef a entity to be removed from the collection.
     *
     * @throws Exception the exception
     */
    public void removeFromCollection( EntityRef entityRef, String collectionName, EntityRef itemRef)
            throws Exception;

    public Results searchCollection( EntityRef entityRef, String collectionName, Query query )
            throws Exception;

    public Set<String> getCollectionIndexes( EntityRef entity, String collectionName )
            throws Exception;

    public void copyRelationships( EntityRef srcEntityRef, String srcRelationName,
            EntityRef dstEntityRef, String dstRelationName ) throws Exception;

    /**
     * Connect the specified entity to another entity with the specified connection type.
     * Connections are directional relationships that can be traversed in either direction.
     *
     * @throws Exception the exception
     */
    public ConnectionRef createConnection( ConnectionRef connection ) throws Exception;

    public ConnectionRef createConnection( EntityRef connectingEntity, String connectionType,
                                           EntityRef connectedEntityRef ) throws Exception;

    public ConnectionRef createConnection( EntityRef connectingEntity, String pairedConnectionType,
                                           EntityRef pairedEntity, String connectionType,
                                           EntityRef connectedEntityRef ) throws Exception;

    public ConnectionRef createConnection(
            EntityRef connectingEntity, ConnectedEntityRef... connections )
            throws Exception;

    public ConnectionRef connectionRef( EntityRef connectingEntity, String connectionType,
                                        EntityRef connectedEntityRef ) throws Exception;

    public ConnectionRef connectionRef( EntityRef connectingEntity, String pairedConnectionType,
            EntityRef pairedEntity, String connectionType, EntityRef connectedEntityRef )
            throws Exception;

    public ConnectionRef connectionRef( EntityRef connectingEntity, ConnectedEntityRef... connections );

    /**
     * Disconnects two connected entities with the specified connection type. Connections are
     * directional relationships that can be traversed in either direction.
     *
     * @throws Exception the exception
     */

    public void deleteConnection( ConnectionRef connectionRef ) throws Exception;

    public Set<String> getConnectionTypes( EntityRef ref ) throws Exception;


    /**
     * Gets the entities of the specified type connected to the specified entity, optionally
     * matching the specified connection types and/or entity types. Returns a list of entity ids.
     *
     * @param entityRef an entity reference
     * @param connectionType type of connection or null.
     * @param connectedEntityType type of entity or null.
     *
     * @return a list of connected entity ids.
     *
     * @throws Exception the exception
     */
    public Results getConnectedEntities( EntityRef entityRef, String connectionType,
            String connectedEntityType, Level resultsLevel ) throws Exception;

    /**
     * Gets the entities connecting to this entity, optionally with the specified connection
     * type and/or entity type.
     * <p/>
     * e.g. "get users who have favorited this place"
     *
     * @param entityRef an entity reference
     * @param connectionType type of connection or null.
     * @param connectedEntityType type of entity or null.
     *
     * @return a list of entities connecting to this one.
     *
     * @throws Exception the exception
     */
    public Results getConnectingEntities( EntityRef entityRef, String connectionType,
            String connectedEntityType, Level resultsLevel ) throws Exception;

    public Results getConnectingEntities( EntityRef entityRef, String connectionType,
    		String entityType, Level level, int count) throws Exception;

	public Results searchConnectedEntities( EntityRef connectingEntity, Query query ) throws Exception;


    // Application roles

    public Set<String> getConnectionIndexes( EntityRef entity, String connectionType ) throws Exception;

    public Map<String, String> getRoles() throws Exception;

    public void resetRoles() throws Exception;

    /**
     * Create the role with the title and inactivity
     *
     * @param roleName The name of the role
     * @param roleTitle The human readable title
     * @param inactivity The amount of inactivity time to have the role expire. 0 is infinity, I.E no expiration
     */
    public Entity createRole( String roleName, String roleTitle, long inactivity ) throws Exception;

    public void grantRolePermission( String roleName, String permission ) throws Exception;

    public void grantRolePermissions( String roleName, Collection<String> permissions ) throws Exception;

    public void revokeRolePermission( String roleName, String permission ) throws Exception;

    public Set<String> getRolePermissions( String roleName ) throws Exception;

    public void deleteRole( String roleName ) throws Exception;

    public EntityRef getGroupRoleRef( UUID ownerId, String roleName ) throws Exception;

    // Group roles

    public Map<String, String> getGroupRoles( UUID groupId ) throws Exception;

    /** Create a group role with the group id, roleName, and inactivity */
    public Entity createGroupRole( UUID groupId, String roleName, long inactivity ) throws Exception;

    public void grantGroupRolePermission( UUID groupId, String roleName, String permission ) throws Exception;

    public void revokeGroupRolePermission( UUID groupId, String roleName, String permission ) throws Exception;

    public Set<String> getGroupRolePermissions( UUID groupId, String roleName ) throws Exception;

    public void deleteGroupRole( UUID groupId, String roleName ) throws Exception;

    // User role membership

    public Set<String> getUserRoles( UUID userId ) throws Exception;

    public void addUserToRole( UUID userId, String roleName ) throws Exception;

    public void removeUserFromRole( UUID userId, String roleName ) throws Exception;

    // User permissions

    public Set<String> getUserPermissions( UUID userId ) throws Exception;

    public void grantUserPermission( UUID userId, String permission ) throws Exception;

    public void revokeUserPermission( UUID userId, String permission ) throws Exception;

    // User role membership

    public Map<String, String> getUserGroupRoles( UUID userId, UUID groupId ) throws Exception;

    public void addUserToGroupRole( UUID userId, UUID groupId, String roleName ) throws Exception;

    public void removeUserFromGroupRole( UUID userId, UUID groupId, String roleName ) throws Exception;

    public Results getUsersInGroupRole( UUID groupId, String roleName, Level level ) throws Exception;

    public void incrementAggregateCounters( UUID userId, UUID groupId, String category,
            String counterName, long value );

    public Results getAggregateCounters( UUID userId, UUID groupId, String category,
            String counterName, CounterResolution resolution, long start, long finish, boolean pad );

    public Results getAggregateCounters( UUID userId, UUID groupId, UUID queueId, String category,
            String counterName, CounterResolution resolution, long start, long finish, boolean pad );

    public Results getAggregateCounters( Query query ) throws Exception;

    public EntityRef getUserByIdentifier( Identifier identifier ) throws Exception;

    public EntityRef getGroupByIdentifier( Identifier identifier ) throws Exception;

    public Set<String> getCounterNames() throws Exception;

    public Map<String, Long> getEntityCounters( UUID entityId ) throws Exception;

    public Map<String, Long> getApplicationCounters() throws Exception;

    public void incrementAggregateCounters(
            UUID userId, UUID groupId, String category, Map<String, Long> counters );

    public boolean isPropertyValueUniqueForEntity(
            String entityType, String propertyName, Object propertyValue ) throws Exception;

    @Deprecated
    /**
     * Get an entity by UUID.  This will return null if the entity is not found
     */
    public Entity get( UUID id ) throws Exception;

    public <A extends Entity> A get( EntityRef entityRef, Class<A> entityClass ) throws Exception;

    public Map<String, Role> getRolesWithTitles( Set<String> roleNames ) throws Exception;

    public String getRoleTitle( String roleName ) throws Exception;

    public Map<String, Role> getUserRolesWithTitles( UUID userId ) throws Exception;


    // Group role membership

    public Map<String, Role> getGroupRolesWithTitles( UUID userId ) throws Exception;

    public void addGroupToRole( UUID userId, String roleName ) throws Exception;

    public void removeGroupFromRole( UUID userId, String roleName ) throws Exception;

    // Group permissions

    public Set<String> getGroupPermissions( UUID groupId ) throws Exception;

    public void grantGroupPermission( UUID groupId, String permission ) throws Exception;

    public void revokeGroupPermission( UUID groupId, String permission ) throws Exception;


    <A extends Entity> A batchCreate(Mutator<ByteBuffer> m, String entityType,
            Class<A> entityClass, Map<String, Object> properties,
            UUID importId, UUID timestampUuid) throws Exception;
    /**
     * Batch dictionary property.
     *
     * @param batch The batch to set the property into
     * @param entity The entity that owns the property
     * @param propertyName the property name
     * @param propertyValue the property value
     * @param timestampUuid The update timestamp as a uuid
     *
     * @return batch
     *
     * @throws Exception the exception
     */
    Mutator<ByteBuffer> batchSetProperty(Mutator<ByteBuffer> batch, EntityRef entity,
            String propertyName, Object propertyValue, UUID timestampUuid) throws Exception;

    Mutator<ByteBuffer> batchSetProperty(Mutator<ByteBuffer> batch, EntityRef entity,
            String propertyName, Object propertyValue, boolean force, boolean noRead,
            UUID timestampUuid) throws Exception;

    Mutator<ByteBuffer> batchUpdateDictionary(Mutator<ByteBuffer> batch, EntityRef entity,
            String dictionaryName, Object elementValue, Object elementCoValue,
            boolean removeFromDictionary, UUID timestampUuid) throws Exception;

    /**
     * Batch update set.
     *
     * @param batch the batch
     * @param entity The owning entity
     * @param dictionaryName the dictionary name
     * @param elementValue the dictionary value
     * @param removeFromDictionary True to delete from the dictionary
     * @param timestampUuid the timestamp
     *
     * @return batch
     *
     * @throws Exception the exception
     */
    Mutator<ByteBuffer> batchUpdateDictionary(Mutator<ByteBuffer> batch, EntityRef entity,
            String dictionaryName, Object elementValue,
            boolean removeFromDictionary, UUID timestampUuid) throws Exception;

    /**
     * Batch update properties.
     *
     * @param batch the batch
     * @param entity The owning entity reference
     * @param properties the properties to set
     * @param timestampUuid the timestamp of the update operation as a time uuid
     *
     * @return batch
     *
     * @throws Exception the exception
     */
    Mutator<ByteBuffer> batchUpdateProperties(Mutator<ByteBuffer> batch,
            EntityRef entity, Map<String, Object> properties, UUID timestampUuid) throws Exception;

    Set<String> getDictionaryNames(EntityRef entity) throws Exception;

    void insertEntity( EntityRef ref ) throws Exception;

    /** @return the applicationId */
    UUID getApplicationId();

    /** @return the indexBucketLocator */
    IndexBucketLocator getIndexBucketLocator();

    /** @return the cass */
    CassandraService getCass();

    public void init( EntityManagerFactory emf,  UUID applicationId);

    /** For testing purposes */
    public void flushManagerCaches();

    void reindexCollection(
        EntityManagerFactory.ProgressObserver po, String collectionName, boolean reverse) throws Exception;

    public void reindex( final EntityManagerFactory.ProgressObserver po ) throws Exception;


    public Entity getUniqueEntityFromAlias( String aliasType, String aliasValue );
}
