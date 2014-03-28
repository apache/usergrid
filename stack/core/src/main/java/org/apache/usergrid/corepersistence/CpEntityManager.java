/*
 * Copyright 2014 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.usergrid.corepersistence;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import me.prettyprint.hector.api.mutation.Mutator;
import org.apache.usergrid.persistence.ConnectedEntityRef;
import org.apache.usergrid.persistence.ConnectionRef;
import org.apache.usergrid.persistence.CounterResolution;
import org.apache.usergrid.persistence.DynamicEntity;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.EntityRef;
import org.apache.usergrid.persistence.Identifier;
import org.apache.usergrid.persistence.IndexBucketLocator;
import org.apache.usergrid.persistence.Query;
import org.apache.usergrid.persistence.RelationManager;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.persistence.RoleRef;
import org.apache.usergrid.persistence.TypedEntity;
import org.apache.usergrid.persistence.cassandra.CassandraService;
import org.apache.usergrid.persistence.cassandra.GeoIndexManager;
import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.entities.Application;
import org.apache.usergrid.persistence.entities.Role;
import org.apache.usergrid.persistence.index.EntityCollectionIndex;
import org.apache.usergrid.persistence.index.utils.EntityMapUtils;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.field.LongField;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

public class CpEntityManager implements EntityManager {

    private CollectionScope applicationScope;
    private EntityCollectionManager ecm;
    private EntityCollectionIndex eci;

    // TODO: eliminate need for a UUID to type map
    private final Map<UUID, String> typesByUuid = new HashMap<UUID, String>();
    private final Map<String, String> typesByCollectionNames = new HashMap<String, String>();


    CpEntityManager(CollectionScope collectionScope, EntityCollectionManager ecm, EntityCollectionIndex eci) {
        this.applicationScope = collectionScope;
        this.ecm = ecm;
        this.eci = eci;
    }


    @Override
    public Entity create(String entityType, Map<String, Object> properties) throws Exception {

        org.apache.usergrid.persistence.model.entity.Entity cpEntity = 
            new org.apache.usergrid.persistence.model.entity.Entity(
                new SimpleId(UUIDGenerator.newTimeUUID(), entityType ));

        cpEntity = EntityMapUtils.fromMap( cpEntity, properties );
        cpEntity.setField(new LongField("created", cpEntity.getId().getUuid().timestamp()) );
        cpEntity.setField(new LongField("modified", cpEntity.getId().getUuid().timestamp()) );

        cpEntity = ecm.write( cpEntity ).toBlockingObservable().last();
        eci.index( cpEntity );

        Entity entity = new DynamicEntity( entityType, cpEntity.getId().getUuid() );
        entity.setUuid( cpEntity.getId().getUuid() );
        Map<String, Object> entityMap = EntityMapUtils.toMap( cpEntity );
        entity.addProperties( entityMap );

        typesByUuid.put( entity.getUuid(), entityType );

        return entity;
    }


    @Override
    public <A extends Entity> A create(
            String entityType, Class<A> entityClass, Map<String, Object> properties) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }


    @Override
    public <A extends TypedEntity> A create(A entity) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }


    @Override
    public Entity create( UUID importId, String entityType, Map<String, Object> properties) throws Exception {
        return create( entityType, properties );
    }

    
    @Override
    public Entity get( UUID entityId ) throws Exception {
        String type = typesByUuid.get( entityId );
        return get( entityId, type );
    }


    public Entity get( UUID entityId, String type ) throws Exception {

        Id id = new SimpleId( entityId, type );

        org.apache.usergrid.persistence.model.entity.Entity cpEntity = 
            ecm.load( id ).toBlockingObservable().last();

        Entity entity = new DynamicEntity( type, cpEntity.getId().getUuid() );
        entity.setUuid( cpEntity.getId().getUuid() );
        Map<String, Object> entityMap = EntityMapUtils.toMap( cpEntity );
        entity.addProperties( entityMap );
        return entity; 
    }

    @Override
    public Entity get(EntityRef entityRef) throws Exception {
        return get( entityRef.getUuid(), entityRef.getType() );
    }


    @Override
    public <A extends Entity> A get(UUID entityId, Class<A> entityClass) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }


    @Override
    public Results get(Collection<UUID> entityIds, Results.Level resultsLevel) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }


    @Override
    public Results get(Collection<UUID> entityIds) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }


    @Override
    public Results get(Collection<UUID> entityIds, Class<? extends Entity> entityClass, 
            Results.Level resultsLevel) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }


    @Override
    public Results get(Collection<UUID> entityIds, String entityType, 
            Class<? extends Entity> entityClass, Results.Level resultsLevel) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }
    

    @Override
    public void update( Entity entity ) throws Exception {

        Id entityId = new SimpleId( entity.getUuid(), entity.getType() );

        org.apache.usergrid.persistence.model.entity.Entity cpEntity =
            ecm.load( entityId ).toBlockingObservable().last();
        
        cpEntity = EntityMapUtils.fromMap( cpEntity, entity.getProperties() );

        cpEntity = ecm.write( cpEntity ).toBlockingObservable().last();
        eci.index( cpEntity );
    }


    @Override
    public void delete(EntityRef entityRef) throws Exception {

        Id entityId = new SimpleId( entityRef.getUuid(), entityRef.getType() );

        org.apache.usergrid.persistence.model.entity.Entity entity =
            ecm.load( entityId ).toBlockingObservable().last();

        if ( entity != null ) {
            eci.deindex( entity );
            ecm.delete( entityId );
        }
    }


    @Override
    public Results searchCollection(EntityRef entityRef, String collectionName, Query query) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
//        String type = typesByCollectionNames.get( collectionName );
//		if ( type == null ) {
//			throw new RuntimeException( 
//					"No type found for collection name: " + collectionName);
//		}
//
//        org.apache.usergrid.persistence.index.query.Results results = eci.execute( query );
//        return results;
    }


    @Override
    public void setApplicationId(UUID applicationId) {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public GeoIndexManager getGeoIndexManager() {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public EntityRef getApplicationRef() {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Application getApplication() throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public void updateApplication(Application app) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public void updateApplication(Map<String, Object> properties) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public RelationManager getRelationManager(EntityRef entityRef) {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Set<String> getApplicationCollections() throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Map<String, Object> getApplicationCollectionMetadata() throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public long getApplicationCollectionSize(String collectionName) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }


    @Override
    public void createApplicationCollection(String entityType) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public EntityRef getAlias(String aliasType, String alias) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public EntityRef getAlias(UUID ownerId, String collectionName, String aliasValue) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Map<String, EntityRef> getAlias(String aliasType, List<String> aliases) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Map<String, EntityRef> getAlias(UUID ownerId, String collectionName, List<String> aliases) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public EntityRef validate(EntityRef entityRef) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public String getType(UUID entityId) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public EntityRef getRef(UUID entityId) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }


    @Override
    public Object getProperty(EntityRef entityRef, String propertyName) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public List<Entity> getPartialEntities(Collection<UUID> ids, Collection<String> properties) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Map<String, Object> getProperties(EntityRef entityRef) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public void setProperty(EntityRef entityRef, String propertyName, Object propertyValue) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public void setProperty(EntityRef entityRef, String propertyName, 
            Object propertyValue, boolean override) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public void updateProperties(EntityRef entityRef, Map<String, Object> properties) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public void deleteProperty(EntityRef entityRef, String propertyName) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Set<Object> getDictionaryAsSet(EntityRef entityRef, String dictionaryName) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public void addToDictionary(EntityRef entityRef, String dictionaryName, Object elementValue) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public void addToDictionary(EntityRef entityRef, String dictionaryName, 
            Object elementName, Object elementValue) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public void addSetToDictionary(EntityRef entityRef, String dictionaryName, Set<?> elementValues) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public void addMapToDictionary(EntityRef entityRef, String dictionaryName, Map<?, ?> elementValues) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Map<Object, Object> getDictionaryAsMap(EntityRef entityRef, String dictionaryName) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Object getDictionaryElementValue(EntityRef entityRef, String dictionaryName, String elementName) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public void removeFromDictionary(EntityRef entityRef, String dictionaryName, Object elementValue) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Set<String> getDictionaries(EntityRef entity) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Map<String, Map<UUID, Set<String>>> getOwners(EntityRef entityRef) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public boolean isCollectionMember(EntityRef owner, String collectionName, EntityRef entity) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public boolean isConnectionMember(EntityRef owner, String connectionName, EntityRef entity) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Set<String> getCollections(EntityRef entityRef) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Results getCollection(EntityRef entityRef, String collectionName, UUID startResult, int count, Results.Level resultsLevel, boolean reversed) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Results getCollection(UUID entityId, String collectionName, Query query, Results.Level resultsLevel) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Entity addToCollection(EntityRef entityRef, String collectionName, EntityRef itemRef) throws Exception {
        // TODO: eliminate need for typesByCollectionNames
        typesByCollectionNames.put( collectionName, entityRef.getType() );
        return get( entityRef );
    }

    @Override
    public Entity addToCollections(List<EntityRef> ownerEntities, String collectionName, EntityRef itemRef) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Entity createItemInCollection(EntityRef entityRef, String collectionName, String itemType, Map<String, Object> properties) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public void removeFromCollection(EntityRef entityRef, String collectionName, EntityRef itemRef) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Set<String> getCollectionIndexes(EntityRef entity, String collectionName) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public void copyRelationships(EntityRef srcEntityRef, String srcRelationName, EntityRef dstEntityRef, String dstRelationName) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public ConnectionRef createConnection(ConnectionRef connection) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public ConnectionRef createConnection(EntityRef connectingEntity, String connectionType, EntityRef connectedEntityRef) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public ConnectionRef createConnection(EntityRef connectingEntity, String pairedConnectionType, EntityRef pairedEntity, String connectionType, EntityRef connectedEntityRef) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public ConnectionRef createConnection(EntityRef connectingEntity, ConnectedEntityRef... connections) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public ConnectionRef connectionRef(EntityRef connectingEntity, String connectionType, EntityRef connectedEntityRef) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public ConnectionRef connectionRef(EntityRef connectingEntity, String pairedConnectionType, EntityRef pairedEntity, String connectionType, EntityRef connectedEntityRef) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public ConnectionRef connectionRef(EntityRef connectingEntity, ConnectedEntityRef... connections) {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public void deleteConnection(ConnectionRef connectionRef) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Set<String> getConnectionTypes(EntityRef ref) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Results getConnectedEntities(UUID entityId, String connectionType, String connectedEntityType, Results.Level resultsLevel) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Results getConnectingEntities(UUID entityId, String connectionType, String connectedEntityType, Results.Level resultsLevel) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Results getConnectingEntities(UUID uuid, String connectionType, String entityType, Results.Level level, int count) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Results searchConnectedEntities(EntityRef connectingEntity, Query query) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Set<String> getConnectionIndexes(EntityRef entity, String connectionType) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Map<String, String> getRoles() throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public void resetRoles() throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Entity createRole(String roleName, String roleTitle, long inactivity) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public void grantRolePermission(String roleName, String permission) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public void grantRolePermissions(String roleName, Collection<String> permissions) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public void revokeRolePermission(String roleName, String permission) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Set<String> getRolePermissions(String roleName) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public void deleteRole(String roleName) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Map<String, String> getGroupRoles(UUID groupId) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Entity createGroupRole(UUID groupId, String roleName, long inactivity) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public void grantGroupRolePermission(UUID groupId, String roleName, String permission) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public void revokeGroupRolePermission(UUID groupId, String roleName, String permission) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Set<String> getGroupRolePermissions(UUID groupId, String roleName) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public void deleteGroupRole(UUID groupId, String roleName) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Set<String> getUserRoles(UUID userId) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public void addUserToRole(UUID userId, String roleName) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public void removeUserFromRole(UUID userId, String roleName) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Set<String> getUserPermissions(UUID userId) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public void grantUserPermission(UUID userId, String permission) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public void revokeUserPermission(UUID userId, String permission) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Map<String, String> getUserGroupRoles(UUID userId, UUID groupId) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public void addUserToGroupRole(UUID userId, UUID groupId, String roleName) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public void removeUserFromGroupRole(UUID userId, UUID groupId, String roleName) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Results getUsersInGroupRole(UUID groupId, String roleName, Results.Level level) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public void incrementAggregateCounters(UUID userId, UUID groupId, String category, String counterName, long value) {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Results getAggregateCounters(UUID userId, UUID groupId, String category, String counterName, CounterResolution resolution, long start, long finish, boolean pad) {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Results getAggregateCounters(UUID userId, UUID groupId, UUID queueId, String category, String counterName, CounterResolution resolution, long start, long finish, boolean pad) {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Results getAggregateCounters(Query query) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public EntityRef getUserByIdentifier(Identifier identifier) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public EntityRef getGroupByIdentifier(Identifier identifier) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Set<String> getCounterNames() throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Map<String, Long> getEntityCounters(UUID entityId) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Map<String, Long> getApplicationCounters() throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public void incrementAggregateCounters(UUID userId, UUID groupId, String category, Map<String, Long> counters) {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public boolean isPropertyValueUniqueForEntity(String entityType, String propertyName, Object propertyValue) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public <A extends Entity> A get(EntityRef entityRef, Class<A> entityClass) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Map<String, Role> getRolesWithTitles(Set<String> roleNames) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public String getRoleTitle(String roleName) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Map<String, Role> getUserRolesWithTitles(UUID userId) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Map<String, Role> getGroupRolesWithTitles(UUID userId) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public void addGroupToRole(UUID userId, String roleName) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public void removeGroupFromRole(UUID userId, String roleName) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Set<String> getGroupPermissions(UUID groupId) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public void grantGroupPermission(UUID groupId, String permission) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public void revokeGroupPermission(UUID groupId, String permission) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public <A extends Entity> A batchCreate(Mutator<ByteBuffer> m, String entityType, Class<A> entityClass, Map<String, Object> properties, UUID importId, UUID timestampUuid) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public void batchCreateRole(Mutator<ByteBuffer> batch, UUID groupId, String roleName, String roleTitle, long inactivity, RoleRef roleRef, UUID timestampUuid) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Mutator<ByteBuffer> batchSetProperty(Mutator<ByteBuffer> batch, EntityRef entity, String propertyName, Object propertyValue, UUID timestampUuid) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Mutator<ByteBuffer> batchSetProperty(Mutator<ByteBuffer> batch, EntityRef entity, String propertyName, Object propertyValue, boolean force, boolean noRead, UUID timestampUuid) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Mutator<ByteBuffer> batchUpdateDictionary(Mutator<ByteBuffer> batch, EntityRef entity, String dictionaryName, Object elementValue, Object elementCoValue, boolean removeFromDictionary, UUID timestampUuid) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Mutator<ByteBuffer> batchUpdateDictionary(Mutator<ByteBuffer> batch, EntityRef entity, String dictionaryName, Object elementValue, boolean removeFromDictionary, UUID timestampUuid) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Mutator<ByteBuffer> batchUpdateProperties(Mutator<ByteBuffer> batch, EntityRef entity, Map<String, Object> properties, UUID timestampUuid) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Set<String> getDictionaryNames(EntityRef entity) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public void insertEntity(String type, UUID entityId) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public void deleteEntity(UUID entityId) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public UUID getApplicationId() {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public IndexBucketLocator getIndexBucketLocator() {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public CassandraService getCass() {
        throw new UnsupportedOperationException("Not supported yet."); 
    }
    
}
