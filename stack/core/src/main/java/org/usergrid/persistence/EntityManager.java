/*******************************************************************************
 * Copyright 2012 Apigee Corporation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.usergrid.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.usergrid.persistence.entities.Application;
import org.usergrid.persistence.entities.Role;

/**
 * The interface class for the data access object for Applications. Each
 * application contains a set of users as well as a hierarchy of groups. A
 * application also includes a set of message inboxes and a set of assets.
 */
public interface EntityManager {

    public EntityRef getApplicationRef();

    public Application getApplication() throws Exception;

    public void updateApplication(Application app) throws Exception;

    public void updateApplication(Map<String, Object> properties)
            throws Exception;

    public RelationManager getRelationManager(EntityRef entityRef);

    public Set<String> getApplicationCollections() throws Exception;

    public Map<String, Object> getApplicationCollectionMetadata()
            throws Exception;

    public long getApplicationCollectionSize(String collectionName)
            throws Exception;

    /**
     * Creates an entity of the specified type attached to the specified
     * application.
     * 
     * @param type
     *            the type of the entity to create.
     * @param properties
     *            property values to create in the new entity or null.
     * @return the newly created entity object.
     * 
     * @throws Exception
     */
    public Entity create(String entityType, Map<String, Object> properties)
            throws Exception;

    public <A extends Entity> A create(String entityType, Class<A> entityClass,
            Map<String, Object> properties) throws Exception;

    public <A extends TypedEntity> A create(A entity) throws Exception;

    /**
     * Creates an entity of the specified type attached to the specified
     * application.
     * 
     * @param importId
     *            the UUID to assign to the imported entity
     * @param type
     *            the type of the entity to create.
     * @param properties
     *            property values to create in the new entity or null.
     * @return the newly created entity object.
     * @throws Exception
     *             the exception
     */
    public Entity create(UUID importId, String entityType,
            Map<String, Object> properties) throws Exception;

    public void createApplicationCollection(String entityType) throws Exception;

    public UUID createAlias(UUID id, String aliasType, String alias)
            throws Exception;

    public UUID createAlias(EntityRef ref, String aliasType, String alias)
            throws Exception;

    public UUID createAlias(UUID ownerId, EntityRef ref, String aliasType,
            String alias) throws Exception;

    public void deleteAlias(String aliasType, String alias) throws Exception;

    public void deleteAlias(UUID ownerId, String aliasType, String alias)
            throws Exception;

    public EntityRef getAlias(String aliasType, String alias) throws Exception;

    public EntityRef getAlias(UUID ownerId, String aliasType, String alias)
            throws Exception;

    public Map<String, EntityRef> getAlias(String aliasType,
            List<String> aliases) throws Exception;

    public Map<String, EntityRef> getAlias(UUID ownerId, String aliasType,
            List<String> aliases) throws Exception;

    /**
     * Validates that the entity exists in the datastore meaning that it exists
     * and the type has been loaded if not already provided.
     * 
     * @param EntityRef
     * 
     * @return an validated EntityRef or null.
     * 
     * @throws Exception
     */
    public EntityRef validate(EntityRef entityRef) throws Exception;

    public String getType(UUID entityId) throws Exception;

    public EntityRef getRef(UUID entityId) throws Exception;

    public Entity get(UUID entityId) throws Exception;

    /**
     * Retrieves the entity for the specified entity reference.
     * 
     * @param entity
     *            an Entity reference
     * 
     * @return an Entity object for the specified entity reference.
     * 
     * @throws Exception
     */
    public Entity get(EntityRef entityRef) throws Exception;

    public <A extends Entity> A get(UUID entityId, Class<A> entityClass)
            throws Exception;

    /**
     * Retrieves a set of Entities. Will return an Entity object containing all
     * of the entity's name/value properties and properties. For large numbers
     * of entities, retrieving the properties can have additional overhead,
     * passing false for includeProperties can result in better performance.
     * <p>
     * This method will be deprecated in future releases in favor of a version
     * that supports paging.
     * 
     * @param entityIds
     *            a list of entity UUIDs.
     * @param includeProperties
     *            whether to retrieve properties for the specified entities.
     * @return a list of entity objects.
     * 
     * @throws Exception
     */
    public Results get(List<UUID> entityIds, Results.Level resultsLevel)
            throws Exception;

    /**
     * Retrieves a set of Entitues cast to the specified class type.
     * 
     * @param <A>
     * @param entityIds
     * @param includeProperties
     * @param entityClass
     * @return a list of entity objects.
     * @throws Exception
     */
    public Results get(List<UUID> entityIds,
            Class<? extends Entity> entityClass, Results.Level resultsLevel)
            throws Exception;

    /**
     * Retrieves a set of Entities cast to the specified class type.
     * 
     * @param <A>
     * @param entityIds
     * @param includeProperties
     * @param entityType
     * @param entityClass
     * @return a list of entity objects.
     * @throws Exception
     */
    public Results get(List<UUID> entityIds, String entityType,
            Class<? extends Entity> entityClass, Results.Level resultsLevel)
            throws Exception;

    /**
     * Updates the entity with the properties and values in the Entity Object.
     * 
     * @param entity
     *            an Entity object.
     * 
     * @throws Exception
     */
    public void update(Entity entity) throws Exception;

    /**
     * Gets the value for a named entity property. Entity properties must be
     * defined in the schema
     * 
     * @param entity
     *            an entity reference
     * @param propertyName
     *            the property name to retrieve.
     * @return the value of the named property or null.
     * @throws Exception
     *             the exception
     */
    public Object getProperty(EntityRef entityRef, String propertyName)
            throws Exception;

    /**
     * Gets the properties for the specified entity property.
     * 
     * @param entity
     *            an entity reference
     * @return the property values.
     * @throws Exception
     *             the exception
     */
    public Map<String, Object> getProperties(EntityRef entityRef)
            throws Exception;

    /**
     * Sets the value for a named entity property. If the property is being
     * index, the index is updated to remove the old value and add the new
     * value.
     * 
     * @param entity
     *            an entity reference
     * @param propertyName
     *            the property to set.
     * @param propertyValue
     *            new value for property.
     * @throws Exception
     *             the exception
     */
    public void setProperty(EntityRef entityRef, String propertyName,
            Object propertyValue) throws Exception;

    /**
     * You should only use this method if you are absolutely sure what you're
     * doing. Use setProperty without the override param in most cases. With
     * great power comes great responsibility....
     * 
     * @param entityRef
     * @param propertyName
     * @param propertyValue
     * @param override
     *            set to true to force this value to persist. This will ignore
     *            all mutable attributes as well as validation. Use with care
     * @throws Exception
     */
    void setProperty(EntityRef entityRef, String propertyName,
            Object propertyValue, boolean override) throws Exception;

    /**
     * Updates the properties for the specified entity.
     * 
     * @param entity
     *            an entity reference
     * @param properties
     *            the properties
     * @throws Exception
     *             the exception
     */
    public void updateProperties(EntityRef entityRef,
            Map<String, Object> properties) throws Exception;

    public void deleteProperty(EntityRef entityRef, String propertyName)
            throws Exception;

    /**
     * Gets the values from an entity list property. Lists are a special type of
     * entity property that can contain an unordered set of non-duplicate
     * values.
     * 
     * @param entity
     *            an entity reference
     * @param dictionaryName
     *            the property list name to retrieve.
     * @return the value of the named property or null.
     * @throws Exception
     *             the exception
     */
    public Set<Object> getDictionaryAsSet(EntityRef entityRef,
            String dictionaryName) throws Exception;

    /**
     * Adds the specified value to the named entity list property. Lists are a
     * special type of entity property that can contain an unordered set of
     * non-duplicate values.
     * 
     * @param entity
     *            an entity reference
     * @param dictionaryName
     *            the property to set.
     * @param elementValue
     *            new value for property.
     * @throws Exception
     *             the exception
     */
    public void addToDictionary(EntityRef entityRef, String dictionaryName,
            Object elementValue) throws Exception;

    public void addToDictionary(EntityRef entityRef, String dictionaryName,
            Object elementName, Object elementValue) throws Exception;

    public void addSetToDictionary(EntityRef entityRef, String dictionaryName,
            Set<?> elementValues) throws Exception;

    public void addMapToDictionary(EntityRef entityRef, String dictionaryName,
            Map<?, ?> elementValues) throws Exception;

    public Map<Object, Object> getDictionaryAsMap(EntityRef entityRef,
            String dictionaryName) throws Exception;

    public Object getDictionaryElementValue(EntityRef entityRef,
            String dictionaryName, String elementName) throws Exception;

    /**
     * Removes the specified value to the named entity list property. Lists are
     * a special type of entity property that can contain an unordered set of
     * non-duplicate values.
     * 
     * @param entity
     *            an entity reference
     * @param dictionaryName
     *            the property to set.
     * @param elementValue
     *            new value for property.
     * @throws Exception
     *             the exception
     */
    public void removeFromDictionary(EntityRef entityRef,
            String dictionaryName, Object elementValue) throws Exception;

    public Set<String> getDictionaries(EntityRef entity) throws Exception;

    /**
     * Deletes the specified entity.
     * 
     * @param entity
     *            an entity reference
     * @throws Exception
     *             the exception
     */
    public void delete(EntityRef entityRef) throws Exception;

    /**
     * Gets the entities and collections that the specified entity is a member
     * of.
     * 
     * @param entity
     *            an entity reference
     * @return a map of entity references to set of collection names for the
     *         entities and collections that this entity is a member of.
     * @throws Exception
     *             the exception
     */
    public Map<String, Map<UUID, Set<String>>> getOwners(EntityRef entityRef)
            throws Exception;

    /**
     * Gets the collections for the specified entity. Collection for a given
     * type are encoded in the schema, this method loads the entity type and
     * returns the collections from the schema.
     * 
     * @param entity
     *            an entity reference
     * @return the collections for the entity type of the given entity.
     * @throws Exception
     *             the exception
     */
    public Set<String> getCollections(EntityRef entityRef) throws Exception;

    /**
     * Gets a list of entities in the specified collection belonging to the
     * specified entity.
     * 
     * @param entity
     *            an entity reference
     * @param collectionName
     *            the collection name.
     * @param startResult
     *            the start result
     * @param count
     *            the count
     * @return a list of entities in the specified collection.
     * @throws Exception
     *             the exception
     */
    public Results getCollection(EntityRef entityRef, String collectionName,
            UUID startResult, int count, Results.Level resultsLevel,
            boolean reversed) throws Exception;

    // T.N. Unused. Removing for this release
    // /**
    // * Gets a list of entities in the specified collection belonging to the
    // * specified entity.
    // *
    // * @param entity
    // * an entity reference
    // * @param collectionName
    // * the collection name.
    // * @param subkeyProperties
    // * the subkey properties
    // * @param startResult
    // * the start result
    // * @param count
    // * the count
    // * @return a list of entities in the specified collection.
    // * @throws Exception
    // * the exception
    // */
    // public Results getCollection(EntityRef entityRef, String collectionName,
    // Map<String, Object> subkeyProperties, UUID startResult, int count,
    // Results.Level resultsLevel, boolean reversed) throws Exception;

    public Results getCollection(UUID entityId, String collectionName,
            Query query, Results.Level resultsLevel) throws Exception;

    /**
     * Adds an entity to the specified collection belonging to the specified
     * entity entity.
     * 
     * @param entity
     *            an entity reference
     * @param collectionName
     *            the collection name.
     * @param item
     *            an entity to be added to the collection.
     * @throws Exception
     *             the exception
     */
    public Entity addToCollection(EntityRef entityRef, String collectionName,
            EntityRef itemRef) throws Exception;

    public Entity addToCollections(List<EntityRef> ownerEntities,
            String collectionName, EntityRef itemRef) throws Exception;

    public Entity createItemInCollection(EntityRef entityRef,
            String collectionName, String itemType,
            Map<String, Object> properties) throws Exception;

    /**
     * Removes an entity to the specified collection belonging to the specified
     * entity.
     * 
     * @param entity
     *            an entity reference
     * @param collectionName
     *            the collection name.
     * @param item
     *            a entity to be removed from the collection.
     * @throws Exception
     *             the exception
     */
    public void removeFromCollection(EntityRef entityRef,
            String collectionName, EntityRef itemRef) throws Exception;

    public Results searchCollection(EntityRef entityRef, String collectionName,
            Query query) throws Exception;

    public Set<String> getCollectionIndexes(EntityRef entity,
            String collectionName) throws Exception;

	public void copyRelationships(EntityRef srcEntityRef,
			String srcRelationName, EntityRef dstEntityRef,
			String dstRelationName) throws Exception;

    /**
     * Connect the specified entity to another entity with the specified
     * connection type. Connections are directional relationships that can be
     * traversed in either direction.
     * 
     * @param entity
     *            an entity reference
     * @param connectionType
     *            type of connection to make.
     * @param connectedEntity
     *            the entity to connect.
     * @throws Exception
     *             the exception
     */
    public ConnectionRef createConnection(ConnectionRef connection)
            throws Exception;

    public ConnectionRef createConnection(EntityRef connectingEntity,
            String connectionType, EntityRef connectedEntityRef)
            throws Exception;

    public ConnectionRef createConnection(EntityRef connectingEntity,
            String pairedConnectionType, EntityRef pairedEntity,
            String connectionType, EntityRef connectedEntityRef)
            throws Exception;

    public ConnectionRef createConnection(EntityRef connectingEntity,
            ConnectedEntityRef... connections) throws Exception;

    public ConnectionRef connectionRef(EntityRef connectingEntity,
            String connectionType, EntityRef connectedEntityRef)
            throws Exception;

    public ConnectionRef connectionRef(EntityRef connectingEntity,
            String pairedConnectionType, EntityRef pairedEntity,
            String connectionType, EntityRef connectedEntityRef)
            throws Exception;

    public ConnectionRef connectionRef(EntityRef connectingEntity,
            ConnectedEntityRef... connections);

    /**
     * Disconnects two connected entities with the specified connection type.
     * Connections are directional relationships that can be traversed in either
     * direction.
     * 
     * @param entity
     *            an entity reference
     * @param connectionType
     *            type of connection to make.
     * @param connectedEntity
     *            the entity to connect
     * @throws Exception
     *             the exception
     */

    public void deleteConnection(ConnectionRef connectionRef) throws Exception;

    /**
     * Returns true if there is a connection between these entities, optionally
     * matching the specified connection type and/or entity type.
     * 
     * @param entity
     *            an entity reference
     * @param connectionType
     *            type of connection or null.
     * @param connectedEntity
     *            an entity reference for the connected entit.
     * @return a list of entities connecting to this one.
     * @throws Exception
     *             the exception
     */
    public boolean connectionExists(ConnectionRef connectionRef)
            throws Exception;

    /**
     * Gets the types of connections between two entities.
     * 
     * @param entity
     *            an entity reference
     * @param connectedEntity
     *            a connected.
     * @return a set of the connection types between the two entities.
     * @throws Exception
     *             the exception
     */
    public Set<String> getConnectionTypes(UUID entityId, UUID connectedEntityId)
            throws Exception;

    public Set<String> getConnectionTypes(EntityRef ref) throws Exception;

    public Set<String> getConnectionTypes(EntityRef ref,
            boolean filterConnection) throws Exception;

    /**
     * Gets the entities of the specified type connected to the specified
     * entity, optionally matching the specified connection types and/or entity
     * types. Returns a list of entity ids.
     * 
     * @param entity
     *            an entity reference
     * @param connectionType
     *            type of connection or null.
     * @param connectedEntityType
     *            type of entity or null.
     * @return a list of connected entity ids.
     * @throws Exception
     *             the exception
     */
    public Results getConnectedEntities(UUID entityId, String connectionType,
            String connectedEntityType, Results.Level resultsLevel)
            throws Exception;

    /**
     * Gets the entities connecting to this entity, optionally with the
     * specified connection type and/or entity type.
     * <p>
     * e.g. "get users who have favorited this place"
     * 
     * @param entity
     *            an entity reference
     * @param connectionType
     *            type of connection or null.
     * @param connectingEntityType
     *            type of entity or null.
     * @return a list of entities connecting to this one.
     * @throws Exception
     *             the exception
     */
    public Results getConnectingEntities(UUID entityId, String connectionType,
            String connectedEntityType, Results.Level resultsLevel)
            throws Exception;

    public List<ConnectedEntityRef> getConnections(UUID entityId, Query query)
            throws Exception;

    // T.N. This isn't used anywhere. Removing for this release
    // /**
    // * Gets the entities connected to the specified entity that match the
    // * requested property values and optionally the connection type and/or
    // * entity type.
    // *
    // * @param entity
    // * an entity reference
    // * @param connectionType
    // * type of connection or null.
    // * @param connectedEntityType
    // * type of entity or null.
    // * @param propertyName
    // * a property name in the connected entity.
    // * @param searchStartValue
    // * starting value in a range to match.
    // * @param searchFinishValue
    // * ending value in a range to match or null for exact match of
    // * searchStartValue.
    // * @param startResult
    // * the start result
    // * @param count
    // * the count
    // * @return a list of connected entities.
    // * @throws Exception
    // * the exception
    // */
    // public Results searchConnectedEntitiesForProperty(
    // EntityRef connectingEntity, String connectionType,
    // String connectedEntityType, String propertyName,
    // Object searchStartValue, Object searchFinishValue,
    // UUID startResult, int count, boolean reversed,
    // Results.Level resultsLevel) throws Exception;

    public Results searchConnectedEntities(EntityRef connectingEntity,
            Query query) throws Exception;

    /**
     * Gets the value for a named connection property. Connection properties are
     * properties associated with a connection.
     * 
     * @param connectionType
     *            the connection type.
     * @param connectedEntityId
     *            a unique entity UUID.
     * @param propertyName
     *            the property name to retrieve.
     * @return the value of the named property or null.
     * @throws Exception
     *             the exception
     */
    public Object getAssociatedProperty(
            AssociatedEntityRef associatedEntityRef, String propertyName)
            throws Exception;

    /**
     * Gets the connection properties for this entity.
     * 
     * @param connectionType
     *            the connection type.
     * @param connectedEntityId
     *            a unique entity UUID.
     * @return the property values.
     * @throws Exception
     *             the exception
     */
    public Map<String, Object> getAssociatedProperties(
            AssociatedEntityRef associatedEntityRef) throws Exception;

    /**
     * Sets the value for a named connection property. If the property is being
     * index, the index is updated to remove the old value and add the new
     * value.
     * 
     * @param connectionType
     *            the connection type.
     * @param connectedEntityId
     *            a unique entity UUID.
     * @param propertyName
     *            the property to set.
     * @param propertyValue
     *            new value for property.
     * @throws Exception
     *             the exception
     */
    public void setAssociatedProperty(AssociatedEntityRef associatedEntityRef,
            String propertyName, Object propertyValue) throws Exception;

    public List<ConnectionRef> searchConnections(EntityRef connectingEntity,
            Query query) throws Exception;

    // Application roles

    public Set<String> getConnectionIndexes(EntityRef entity,
            String connectionType) throws Exception;

    public Map<String, String> getRoles() throws Exception;

    public void resetRoles() throws Exception;

    /**
     * Create the role with the title and inactivity
     * 
     * @param roleName
     *            The name of the role
     * @param roleTitle
     *            The human readable title
     * @param inactivity
     *            The amount of inactivity time to have the role expire. 0 is
     *            infinity, I.E no expiration
     * @return
     * @throws Exception
     */
    public Entity createRole(String roleName, String roleTitle, long inactivity)
            throws Exception;

    public void grantRolePermission(String roleName, String permission)
            throws Exception;

    public void grantRolePermissions(String roleName,
            Collection<String> permissions) throws Exception;

    public void revokeRolePermission(String roleName, String permission)
            throws Exception;

    public Set<String> getRolePermissions(String roleName) throws Exception;

    public void deleteRole(String roleName) throws Exception;

    // Group roles

    public Map<String, String> getGroupRoles(UUID groupId) throws Exception;

    /**
     * Create a group role with the group id, roleName, and inactivity
     * 
     * @param groupId
     * @param roleName
     * @param inactivity
     * @return
     * @throws Exception
     */
    public Entity createGroupRole(UUID groupId, String roleName, long inactivity)
            throws Exception;

    public void grantGroupRolePermission(UUID groupId, String roleName,
            String permission) throws Exception;

    public void revokeGroupRolePermission(UUID groupId, String roleName,
            String permission) throws Exception;

    public Set<String> getGroupRolePermissions(UUID groupId, String roleName)
            throws Exception;

    public void deleteGroupRole(UUID groupId, String roleName) throws Exception;

    // User role membership

    public Set<String> getUserRoles(UUID userId) throws Exception;

    public void addUserToRole(UUID userId, String roleName) throws Exception;

    public void removeUserFromRole(UUID userId, String roleName)
            throws Exception;

    // User permissions

    public Set<String> getUserPermissions(UUID userId) throws Exception;

    public void grantUserPermission(UUID userId, String permission)
            throws Exception;

    public void revokeUserPermission(UUID userId, String permission)
            throws Exception;

    // User role membership

    public Map<String, String> getUserGroupRoles(UUID userId, UUID groupId)
            throws Exception;

    public void addUserToGroupRole(UUID userId, UUID groupId, String roleName)
            throws Exception;

    public void removeUserFromGroupRole(UUID userId, UUID groupId,
            String roleName) throws Exception;

    public Results getUsersInGroupRole(UUID groupId, String roleName,
            Results.Level level) throws Exception;

    public void incrementAggregateCounters(UUID userId, UUID groupId,
            String category, String counterName, long value);

    public Results getAggregateCounters(UUID userId, UUID groupId,
            String category, String counterName, CounterResolution resolution,
            long start, long finish, boolean pad);

    public Results getAggregateCounters(UUID userId, UUID groupId,
            UUID queueId, String category, String counterName,
            CounterResolution resolution, long start, long finish, boolean pad);

    public Results getAggregateCounters(Query query) throws Exception;

    public EntityRef getUserByIdentifier(Identifier identifier)
            throws Exception;

    public EntityRef getGroupByIdentifier(Identifier identifier)
            throws Exception;

    public Set<String> getCounterNames() throws Exception;

    public void incrementApplicationCounters(Map<String, Long> counts);

    public void incrementApplicationCounter(String name, long value);

    public void incrementEntitiesCounters(Map<UUID, Map<String, Long>> counts);

    public void incrementEntityCounters(UUID entityId, Map<String, Long> counts);

    public void incrementEntityCounter(UUID entityId, String name, long value);

    public Map<String, Long> getEntityCounters(UUID entityId) throws Exception;

    public Map<String, Long> getApplicationCounters() throws Exception;

    public void incrementAggregateCounters(UUID userId, UUID groupId,
            String category, Map<String, Long> counters);

    public boolean isPropertyValueUniqueForEntity(String entityType,
            String propertyName, Object propertyValue) throws Exception;

    public <A extends Entity> A get(EntityRef entityRef, Class<A> entityClass)
            throws Exception;

    public Map<String, Role> getRolesWithTitles(Set<String> roleNames)
            throws Exception;

    public String getRoleTitle(String roleName) throws Exception;

    public Map<String, Role> getUserRolesWithTitles(UUID userId)
            throws Exception;

}
