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

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.yammer.metrics.annotation.Metered;
import static java.lang.String.CASE_INSENSITIVE_ORDER;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import me.prettyprint.hector.api.mutation.Mutator;
import static org.apache.commons.lang.StringUtils.isBlank;
import org.apache.usergrid.persistence.ConnectedEntityRef;
import org.apache.usergrid.persistence.ConnectionRef;
import org.apache.usergrid.persistence.CounterResolution;
import org.apache.usergrid.persistence.DynamicEntity;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityFactory;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.EntityRef;
import org.apache.usergrid.persistence.Identifier;
import org.apache.usergrid.persistence.IndexBucketLocator;
import org.apache.usergrid.persistence.Query;
import org.apache.usergrid.persistence.RelationManager;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.persistence.RoleRef;
import org.apache.usergrid.persistence.Schema;
import static org.apache.usergrid.persistence.Schema.PROPERTY_CREATED;
import static org.apache.usergrid.persistence.Schema.PROPERTY_MODIFIED;
import static org.apache.usergrid.persistence.Schema.PROPERTY_TIMESTAMP;
import static org.apache.usergrid.persistence.Schema.PROPERTY_TYPE;
import static org.apache.usergrid.persistence.Schema.PROPERTY_UUID;
import static org.apache.usergrid.persistence.Schema.TYPE_APPLICATION;
import static org.apache.usergrid.persistence.Schema.TYPE_ENTITY;
import static org.apache.usergrid.persistence.Schema.getDefaultSchema;
import org.apache.usergrid.persistence.SimpleEntityRef;
import org.apache.usergrid.persistence.TypedEntity;
import org.apache.usergrid.persistence.cassandra.CassandraService;
import org.apache.usergrid.persistence.cassandra.CounterUtils;
import org.apache.usergrid.persistence.cassandra.GeoIndexManager;
import org.apache.usergrid.persistence.cassandra.util.TraceParticipant;
import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.collection.impl.CollectionScopeImpl;
import org.apache.usergrid.persistence.core.scope.OrganizationScope;
import org.apache.usergrid.persistence.entities.Application;
import org.apache.usergrid.persistence.entities.Event;
import org.apache.usergrid.persistence.entities.Role;
import org.apache.usergrid.persistence.exceptions.RequiredPropertyNotFoundException;
import org.apache.usergrid.persistence.index.EntityIndex;
import org.apache.usergrid.persistence.index.EntityIndexFactory;
import org.apache.usergrid.persistence.index.utils.EntityMapUtils;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.schema.CollectionInfo;
import static org.apache.usergrid.utils.ConversionUtils.getLong;
import org.apache.usergrid.utils.UUIDUtils;
import static org.apache.usergrid.utils.UUIDUtils.getTimestampInMicros;
import static org.apache.usergrid.utils.UUIDUtils.isTimeBased;
import static org.apache.usergrid.utils.UUIDUtils.newTimeUUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Implement good-old Usergrid EntityManager with the new-fangled Core Persistence API.
 */
public class CpEntityManager implements EntityManager {
    private static final Logger logger = LoggerFactory.getLogger( CpEntityManager.class );

    private UUID applicationId;
    private Application application;
    
    private CpEntityManagerFactory emf = new CpEntityManagerFactory();
    private EntityCollectionManagerFactory ecmf;
    private EntityIndexFactory eif;


    public CpEntityManager() {
        Injector injector = Guice.createInjector( new GuiceModule() );
        this.ecmf = injector.getInstance( EntityCollectionManagerFactory.class );
        this.eif = injector.getInstance( EntityIndexFactory.class );
    }

    public CpEntityManager init( 
            CpEntityManagerFactory emf, CassandraService cass, CounterUtils counterUtils,
            UUID applicationId, boolean skipAggregateCounters ) {

        this.emf = emf;
        this.applicationId = applicationId;

//        this.cass = cass;
//        this.counterUtils = counterUtils;
//        this.skipAggregateCounters = skipAggregateCounters;

        // prime the application entity for the EM
        try {
            application = getApplication();
        }
        catch ( Exception ex ) {
            ex.printStackTrace();
        }
        return this;
    }


    @Override
    public Entity create(String entityType, Map<String, Object> properties) throws Exception {
        return create( entityType, null, properties );
    }


    @Override
    public <A extends Entity> A create(
            String entityType, Class<A> entityClass, Map<String, Object> properties) 
            throws Exception {

        if ( ( entityType != null ) 
            && ( entityType.startsWith( TYPE_ENTITY ) || entityType.startsWith( "entities" ) ) ) {
            throw new IllegalArgumentException( "Invalid entity type" );
        }
        A e = null;
        try {
            e = ( A ) create( entityType, ( Class<Entity> ) entityClass, properties, null );
        }
        catch ( ClassCastException e1 ) {
            logger.error( "Unable to create typed entity", e1 );
        }
        return e;
    }


    @Override
    public <A extends TypedEntity> A create(A entity) throws Exception {
        return ( A ) create( entity.getType(), entity.getClass(), entity.getProperties() );
    }


    @Override
    public Entity create(
            UUID importId, String entityType, Map<String, Object> properties) throws Exception {
        return create( entityType, properties );
    }

   
    /**
     * Creates a new entity.
     *
     * @param entityType the entity type
     * @param entityClass the entity class
     * @param properties the properties
     * @param importId an existing external UUID to use as the id for the new entity
     *
     * @return new entity
     *
     * @throws Exception the exception
     */
    @Metered( group = "core", name = "EntityManager_create" )
    @TraceParticipant
    public <A extends Entity> A create( 
            String entityType, Class<A> entityClass, 
            Map<String, Object> properties,
            UUID importId ) throws Exception {

        UUID timestampUuid = importId != null ?  importId : newTimeUUID();

        Mutator<ByteBuffer> m = null; // ain't got one
        A entity = batchCreate( m, entityType, entityClass, properties, importId, timestampUuid );

        return entity;
    }


    @Override
    public Entity get( UUID entityId ) throws Exception {
        throw new UnsupportedOperationException("Cannot get entity by UUID alone"); 
    }


    @Override
    public Entity get( UUID entityId, String type ) throws Exception {

        Id id = new SimpleId( entityId, type );
        String collectionName = Schema.defaultCollectionName( type );

        CollectionScope applicationScope = emf.getApplicationScope(applicationId);
        CollectionScope collectionScope = new CollectionScopeImpl( 
            applicationScope.getOrganization(), 
            applicationScope.getOwner(), 
            collectionName );

        EntityCollectionManager ecm = ecmf.createCollectionManager(collectionScope);

//        logger.debug("Loading entity {} type {} to {}", 
//            new String[] { entityId.toString(), type, collectionName });

        org.apache.usergrid.persistence.model.entity.Entity cpEntity = 
            ecm.load( id ).toBlockingObservable().last();

        if ( cpEntity == null ) {
            return null;
        }

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
        A e = null;
        try {
            e = ( A ) getEntity( entityId, ( Class<Entity> ) entityClass );
        }
        catch ( ClassCastException e1 ) {
            logger.error( "Unable to get typed entity: {} of class {}", 
                new Object[] {entityId, entityClass.getCanonicalName(), e1} );
        }
        return e;
    }

        /**
     * Gets the specified entity.
     *
     * @param entityId the entity id
     * @param entityClass the entity class
     *
     * @return entity
     *
     * @throws Exception the exception
     */
    public <A extends Entity> A getEntity( UUID entityId, Class<A> entityClass ) throws Exception {

        String type = entityClass.getSimpleName().toLowerCase(); 

        Id id = new SimpleId( entityId, type );
        String collectionName = Schema.defaultCollectionName( type );

        CollectionScope applicationScope = emf.getApplicationScope(applicationId);
        CollectionScope collectionScope = new CollectionScopeImpl( 
            applicationScope.getOrganization(), 
            applicationScope.getOwner(), 
            collectionName );

        EntityCollectionManager ecm = ecmf.createCollectionManager(collectionScope);

//        logger.debug("Loading entity {} type {} to {}", 
//            new String[] { entityId.toString(), type, collectionName });

        org.apache.usergrid.persistence.model.entity.Entity cpEntity = 
            ecm.load( id ).toBlockingObservable().last();

        if ( cpEntity == null ) {
            return null;
        }

        A entity = EntityFactory.newEntity( entityId, type, entityClass );
        entity.setProperties( EntityMapUtils.toMap( cpEntity ) );

        return entity;
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

        String collectionName = Schema.defaultCollectionName( entity.getType() );

        OrganizationScope organizationScope = emf.getOrganizationScope(applicationId);
        CollectionScope applicationScope = emf.getApplicationScope(applicationId);

        CollectionScope collectionScope = new CollectionScopeImpl( 
            applicationScope.getOrganization(), 
            applicationScope.getOwner(), 
            collectionName );

        EntityCollectionManager ecm = ecmf.createCollectionManager(collectionScope);
        EntityIndex ei = eif.createEntityIndex(organizationScope, applicationScope);

        Id entityId = new SimpleId( entity.getUuid(), entity.getType() );

        org.apache.usergrid.persistence.model.entity.Entity cpEntity =
            ecm.load( entityId ).toBlockingObservable().last();
        
        cpEntity = EntityMapUtils.fromMap( cpEntity, entity.getProperties() );

        cpEntity = ecm.write( cpEntity ).toBlockingObservable().last();
        ei.index( collectionScope, cpEntity );
    }


    @Override
    public void delete(EntityRef entityRef) throws Exception {

        String collectionName = Schema.defaultCollectionName( entityRef.getType() );

        OrganizationScope organizationScope = emf.getOrganizationScope(applicationId);
        CollectionScope applicationScope = emf.getApplicationScope(applicationId);

        CollectionScope collectionScope = new CollectionScopeImpl( 
            applicationScope.getOrganization(), 
            applicationScope.getOwner(), 
            collectionName );

        EntityCollectionManager ecm = ecmf.createCollectionManager(collectionScope);
        EntityIndex ei = eif.createEntityIndex(organizationScope, applicationScope);

        Id entityId = new SimpleId( entityRef.getUuid(), entityRef.getType() );

        org.apache.usergrid.persistence.model.entity.Entity entity =
            ecm.load( entityId ).toBlockingObservable().last();

        if ( entity != null ) {
            ei.deindex( collectionScope, entity );
            ecm.delete( entityId ).toBlockingObservable().last();
        }
    }


    @Override
    public Results searchCollection(
            EntityRef entityRef, String collectionName, Query query) throws Exception {

        return getRelationManager(entityRef).searchCollection(collectionName, query);
    }


    @Override
    public void setApplicationId(UUID applicationId) {
        this.applicationId = applicationId;
    }

    @Override
    public GeoIndexManager getGeoIndexManager() {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public EntityRef getApplicationRef() {
        return new SimpleEntityRef( TYPE_APPLICATION, applicationId );
    }

    @Override
    public Application getApplication() throws Exception {
        if ( application == null ) {
            application = get( applicationId, Application.class );
        }
        return application;
    }

    @Override
    public void updateApplication(Application app) throws Exception {
        update( app );
        this.application = app;
    }

    @Override
    public void updateApplication(Map<String, Object> properties) throws Exception {
        //this.updateProperties( applicationId, properties );
        this.application = get( applicationId, Application.class );
    }

    @Override
    public RelationManager getRelationManager(EntityRef entityRef) {
        CpRelationManager rmi = new CpRelationManager();
        rmi.init( this, emf, applicationId, entityRef, null);
        return rmi;
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
    public EntityRef getAlias(
            UUID ownerId, String collectionName, String aliasValue) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Map<String, EntityRef> getAlias(
            String aliasType, List<String> aliases) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Map<String, EntityRef> getAlias(
            UUID ownerId, String collectionName, List<String> aliases) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public EntityRef validate( EntityRef entityRef ) throws Exception {
        // all entity refs should have type in CP
        return entityRef; 
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
    public Object getProperty(
            EntityRef entityRef, String propertyName) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public List<Entity> getPartialEntities(
            Collection<UUID> ids, Collection<String> properties) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Map<String, Object> getProperties(EntityRef entityRef) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public void setProperty(
            EntityRef entityRef, String propertyName, Object propertyValue) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public void setProperty(EntityRef entityRef, String propertyName, 
            Object propertyValue, boolean override) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public void updateProperties(
            EntityRef entityRef, Map<String, Object> properties) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public void deleteProperty(EntityRef entityRef, String propertyName) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Set<Object> getDictionaryAsSet(
            EntityRef entityRef, String dictionaryName) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public void addToDictionary(
            EntityRef entityRef, String dictionaryName, Object elementValue) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public void addToDictionary(EntityRef entityRef, String dictionaryName, 
            Object elementName, Object elementValue) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public void addSetToDictionary(
            EntityRef entityRef, String dictionaryName, Set<?> elementValues) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public void addMapToDictionary(
            EntityRef entityRef, String dictionaryName, Map<?, ?> elementValues) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Map<Object, Object> getDictionaryAsMap(
            EntityRef entityRef, String dictionaryName) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Object getDictionaryElementValue(
            EntityRef entityRef, String dictionaryName, String elementName) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public void removeFromDictionary(
            EntityRef entityRef, String dictionaryName, Object elementValue) throws Exception {
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
    public boolean isCollectionMember(
            EntityRef owner, String collectionName, EntityRef entity) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public boolean isConnectionMember(
            EntityRef owner, String connectionName, EntityRef entity) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Set<String> getCollections(EntityRef entityRef) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Results getCollection(
            EntityRef entityRef, String collectionName, UUID startResult, int count, 
            Results.Level resultsLevel, boolean reversed) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Results getCollection(
            UUID entityId, String collectionName, Query query, Results.Level resultsLevel) 
            throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Entity addToCollection(
            EntityRef entityRef, String collectionName, EntityRef itemRef) throws Exception {

        return getRelationManager( entityRef ).addToCollection(collectionName, itemRef);
    }

    @Override
    public Entity addToCollections(
            List<EntityRef> ownerEntities, String collectionName, EntityRef itemRef) 
            throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Entity createItemInCollection(
            EntityRef entityRef, String collectionName, String itemType, 
            Map<String, Object> properties) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public void removeFromCollection(
            EntityRef entityRef, String collectionName, EntityRef itemRef) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Set<String> getCollectionIndexes(
            EntityRef entity, String collectionName) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public void copyRelationships(
            EntityRef srcEntityRef, String srcRelationName, EntityRef dstEntityRef, 
            String dstRelationName) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public ConnectionRef createConnection(ConnectionRef connection) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public ConnectionRef createConnection(
            EntityRef connectingEntity, String connectionType, EntityRef connectedEntityRef) 
            throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public ConnectionRef createConnection(
            EntityRef connectingEntity, String pairedConnectionType, EntityRef pairedEntity, 
            String connectionType, EntityRef connectedEntityRef) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public ConnectionRef createConnection(
            EntityRef connectingEntity, ConnectedEntityRef... connections) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public ConnectionRef connectionRef(
            EntityRef connectingEntity, String connectionType, EntityRef connectedEntityRef) 
            throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public ConnectionRef connectionRef(
            EntityRef connectingEntity, String pairedConnectionType, EntityRef pairedEntity, 
            String connectionType, EntityRef connectedEntityRef) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public ConnectionRef connectionRef(
            EntityRef connectingEntity, ConnectedEntityRef... connections) {
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
    public Results getConnectedEntities(
            UUID entityId, String connectionType, String connectedEntityType, 
            Results.Level resultsLevel) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Results getConnectingEntities(
            UUID entityId, String connectionType, String connectedEntityType, 
            Results.Level resultsLevel) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Results getConnectingEntities(UUID uuid, String connectionType, 
            String entityType, Results.Level level, int count) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Results searchConnectedEntities(
            EntityRef connectingEntity, Query query) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Set<String> getConnectionIndexes(
            EntityRef entity, String connectionType) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Map<String, String> getRoles() throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public void resetRoles() throws Exception {
        // TODO
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
    public void grantRolePermissions(
            String roleName, Collection<String> permissions) throws Exception {
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
    public void grantGroupRolePermission(
            UUID groupId, String roleName, String permission) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public void revokeGroupRolePermission(
            UUID groupId, String roleName, String permission) throws Exception {
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
    public void removeUserFromGroupRole(UUID userId, UUID groupId, String roleName) 
            throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Results getUsersInGroupRole(
            UUID groupId, String roleName, Results.Level level) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public void incrementAggregateCounters(
            UUID userId, UUID groupId, String category, String counterName, long value) {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Results getAggregateCounters(
            UUID userId, UUID groupId, String category, String counterName, 
            CounterResolution resolution, long start, long finish, boolean pad) {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Results getAggregateCounters(
            UUID userId, UUID groupId, UUID queueId, String category, String counterName, 
            CounterResolution resolution, long start, long finish, boolean pad) {
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
    public void incrementAggregateCounters(
            UUID userId, UUID groupId, String category, Map<String, Long> counters) {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public boolean isPropertyValueUniqueForEntity(
            String entityType, String propertyName, Object propertyValue) throws Exception {
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
    public <A extends Entity> A batchCreate(
            Mutator<ByteBuffer> m, 
            String entityType, 
            Class<A> entityClass, 
            Map<String, Object> properties, 
            UUID importId, 
            UUID timestampUuid) throws Exception {

        String eType = Schema.normalizeEntityType( entityType );

        Schema schema = getDefaultSchema();

        boolean is_application = TYPE_APPLICATION.equals( eType );

        if ( ( ( applicationId == null ) 
                || applicationId.equals( UUIDUtils.ZERO_UUID ) ) && !is_application ) {
            return null;
        }

        long timestamp = getTimestampInMicros( timestampUuid );

        UUID itemId = UUIDUtils.newTimeUUID();

        if ( is_application ) {
            itemId = applicationId;
        }
        if ( importId != null ) {
            itemId = importId;
        }
        boolean emptyPropertyMap = false;
        if ( properties == null ) {
            properties = new TreeMap<String, Object>( CASE_INSENSITIVE_ORDER );
        }
        if ( properties.isEmpty() ) {
            emptyPropertyMap = true;
        }

        if ( importId != null ) {
            if ( isTimeBased( importId ) ) {
                timestamp = UUIDUtils.getTimestampInMicros( importId );
            }
            else if ( properties.get( PROPERTY_CREATED ) != null ) {
                timestamp = getLong( properties.get( PROPERTY_CREATED ) ) * 1000;
            }
        }

        if ( entityClass == null ) {
            entityClass = ( Class<A> ) Schema.getDefaultSchema().getEntityClass( entityType );
        }

        Set<String> required = schema.getRequiredProperties( entityType );

        if ( required != null ) {
            for ( String p : required ) {
                if ( !PROPERTY_UUID.equals( p ) 
                        && !PROPERTY_TYPE.equals( p ) && !PROPERTY_CREATED.equals( p )
                        && !PROPERTY_MODIFIED.equals( p ) ) {
                    Object v = properties.get( p );
                    if ( schema.isPropertyTimestamp( entityType, p ) ) {
                        if ( v == null ) {
                            properties.put( p, timestamp / 1000 );
                        }
                        else {
                            long ts = getLong( v );
                            if ( ts <= 0 ) {
                                properties.put( p, timestamp / 1000 );
                            }
                        }
                        continue;
                    }
                    if ( v == null ) {
                        throw new RequiredPropertyNotFoundException( entityType, p );
                    }
                    else if ( ( v instanceof String ) && isBlank( ( String ) v ) ) {
                        throw new RequiredPropertyNotFoundException( entityType, p );
                    }
                }
            }
        }

        // Create collection name based on entity: i.e. "users"
        String collectionName = Schema.defaultCollectionName( eType );

//        // Create collection key based collection name
//        String bucketId = indexBucketLocator.getBucket( 
//            applicationId, IndexBucketLocator.IndexType.COLLECTION, itemId, collection_name );
//
//        Object collection_key = key( applicationId, 
//            Schema.DICTIONARY_COLLECTIONS, collection_name, bucketId );

        CollectionInfo collection = null;

        if ( emptyPropertyMap ) {
            return null;
        }
        properties.put( PROPERTY_UUID, itemId );
        properties.put( PROPERTY_TYPE, Schema.normalizeEntityType( entityType, false ) );

        if ( importId != null ) {
            if ( properties.get( PROPERTY_CREATED ) == null ) {
                properties.put( PROPERTY_CREATED, timestamp / 1000 );
            }

            if ( properties.get( PROPERTY_MODIFIED ) == null ) {
                properties.put( PROPERTY_MODIFIED, timestamp / 1000 );
            }
        }
        else {
            properties.put( PROPERTY_CREATED, timestamp / 1000 );
            properties.put( PROPERTY_MODIFIED, timestamp / 1000 );
        }

        // special case timestamp and published properties
        // and dictionary their timestamp values if not set
        // this is sure to break something for someone someday

        if ( properties.containsKey( PROPERTY_TIMESTAMP ) ) {
            long ts = getLong( properties.get( PROPERTY_TIMESTAMP ) );
            if ( ts <= 0 ) {
                properties.put( PROPERTY_TIMESTAMP, timestamp / 1000 );
            }
        }

        A entity = EntityFactory.newEntity( itemId, eType, entityClass );
//        logger.info( "Entity created of type {}", entity.getClass().getName() );

        if ( Event.ENTITY_TYPE.equals( eType ) ) {
            Event event = ( Event ) entity.toTypedEntity();
            for ( String prop_name : properties.keySet() ) {
                Object propertyValue = properties.get( prop_name );
                if ( propertyValue != null ) {
                    event.setProperty( prop_name, propertyValue );
                }
            }
//            Message message = storeEventAsMessage( m, event, timestamp );
//            incrementEntityCollection( "events", timestamp );
//
//            entity.setUuid( message.getUuid() );
            return entity;
        }

        // create Core Persistence Entity from those properties
        org.apache.usergrid.persistence.model.entity.Entity cpEntity = 
            new org.apache.usergrid.persistence.model.entity.Entity(
                new SimpleId(itemId, entityType ));

        cpEntity = EntityMapUtils.fromMap( cpEntity, properties );

        // prepare to write and index Core Persistence Entity into correct scope
        OrganizationScope organizationScope = emf.getOrganizationScope(applicationId);
        CollectionScope applicationScope = emf.getApplicationScope(applicationId);
        CollectionScope collectionScope = new CollectionScopeImpl( 
            applicationScope.getOrganization(), 
            applicationScope.getOwner(), 
            collectionName );

        EntityCollectionManager ecm = ecmf.createCollectionManager(collectionScope);
        EntityIndex ei = eif.createEntityIndex(organizationScope, applicationScope);

        cpEntity = ecm.write( cpEntity ).toBlockingObservable().last();
        ei.index( collectionScope, cpEntity );

        // reflect changes in the legacy Entity
        entity.setUuid( cpEntity.getId().getUuid() );
        Map<String, Object> entityMap = EntityMapUtils.toMap( cpEntity );
        entity.addProperties( entityMap );

        if ( !is_application ) {
            getRelationManager( getApplication() ).addToCollection( collectionName, entity );
        }

        return entity;
    }

    @Override
    public void batchCreateRole(
            Mutator<ByteBuffer> batch, UUID groupId, String roleName, String roleTitle, 
            long inactivity, RoleRef roleRef, UUID timestampUuid) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Mutator<ByteBuffer> batchSetProperty(
            Mutator<ByteBuffer> batch, EntityRef entity, String propertyName, Object propertyValue, 
            UUID timestampUuid) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Mutator<ByteBuffer> batchSetProperty(
            Mutator<ByteBuffer> batch, EntityRef entity, String propertyName, Object propertyValue, 
            boolean force, boolean noRead, UUID timestampUuid) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Mutator<ByteBuffer> batchUpdateDictionary(
            Mutator<ByteBuffer> batch, EntityRef entity, String dictionaryName, Object elementValue, 
            Object elementCoValue, boolean removeFromDictionary, UUID timestampUuid) 
            throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Mutator<ByteBuffer> batchUpdateDictionary(
            Mutator<ByteBuffer> batch, EntityRef entity, String dictionaryName, Object elementValue, 
            boolean removeFromDictionary, UUID timestampUuid) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Mutator<ByteBuffer> batchUpdateProperties(
            Mutator<ByteBuffer> batch, EntityRef entity, Map<String, Object> properties, 
            UUID timestampUuid) throws Exception {
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
   
    @Override
    public void refreshIndex() {

        // refresh system entity index
        EntityIndex sei = eif.createEntityIndex(
            CpEntityManagerFactory.SYSTEM_ORG_SCOPE, CpEntityManagerFactory.SYSTEM_APPS_SCOPE);
        sei.refresh();

        // refresh application entity index
        EntityIndex aei = eif.createEntityIndex(
            emf.getOrganizationScope(applicationId), emf.getApplicationScope(applicationId));
        aei.refresh();

        logger.debug("Refreshed index for system and application: " + applicationId);
    }
}
