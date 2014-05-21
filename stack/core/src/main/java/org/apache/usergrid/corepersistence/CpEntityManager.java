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

import com.yammer.metrics.annotation.Metered;
import java.io.Serializable;
import static java.lang.String.CASE_INSENSITIVE_ORDER;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import me.prettyprint.hector.api.mutation.Mutator;
import static org.apache.commons.lang.StringUtils.isBlank;
import org.apache.usergrid.persistence.CollectionRef;
import org.apache.usergrid.persistence.ConnectedEntityRef;
import org.apache.usergrid.persistence.ConnectionRef;
import org.apache.usergrid.persistence.DynamicEntity;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityFactory;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.EntityManagerFactory;
import org.apache.usergrid.persistence.EntityRef;
import org.apache.usergrid.persistence.IndexBucketLocator;
import org.apache.usergrid.persistence.index.query.Query;
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
import org.apache.usergrid.persistence.cassandra.ConnectionRefImpl;
import org.apache.usergrid.persistence.cassandra.GeoIndexManager;
import org.apache.usergrid.persistence.cassandra.util.TraceParticipant;
import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.exception.WriteUniqueVerifyException;
import org.apache.usergrid.persistence.collection.impl.CollectionScopeImpl;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.entities.Application;
import org.apache.usergrid.persistence.entities.Event;
import org.apache.usergrid.persistence.entities.Role;
import org.apache.usergrid.persistence.exceptions.DuplicateUniquePropertyExistsException;
import org.apache.usergrid.persistence.exceptions.RequiredPropertyNotFoundException;
import org.apache.usergrid.persistence.index.EntityIndex;
import org.apache.usergrid.persistence.index.IndexScope;
import org.apache.usergrid.persistence.index.impl.IndexScopeImpl;
import org.apache.usergrid.persistence.index.query.CounterResolution;
import org.apache.usergrid.persistence.index.query.Identifier;
import org.apache.usergrid.persistence.index.query.Query.Level;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.field.Field;
import org.apache.usergrid.persistence.schema.CollectionInfo;
import static org.apache.usergrid.utils.ConversionUtils.getLong;
import org.apache.usergrid.utils.UUIDUtils;
import static org.apache.usergrid.utils.UUIDUtils.getTimestampInMicros;
import static org.apache.usergrid.utils.UUIDUtils.getTimestampInMillis;
import static org.apache.usergrid.utils.UUIDUtils.isTimeBased;
import static org.apache.usergrid.utils.UUIDUtils.newTimeUUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;


/**
 * Implement good-old Usergrid EntityManager with the new-fangled Core Persistence API.
 */
public class CpEntityManager implements EntityManager {
    private static final Logger logger = LoggerFactory.getLogger( CpEntityManager.class );

    private UUID applicationId;
    private Application application;
   
    private CpEntityManagerFactory emf;

    private CpManagerCache managerCache;

    private ApplicationScope appScope;

    public CpEntityManager() {}


    @Override
    public void init( EntityManagerFactory emf, UUID applicationId ) {

        this.emf = (CpEntityManagerFactory)emf;
        this.managerCache = this.emf.getManagerCache();
        this.applicationId = applicationId;

        appScope = this.emf.getApplicationScope(applicationId);

        try {
            application = getApplication();
        }
        catch ( Exception ex ) {
            logger.error("Getting application", ex);
        }
    }


    public CpManagerCache getManagerCache() {
        return managerCache;
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
    public Entity get( EntityRef entityRef ) throws Exception {

        Id id = new SimpleId(  entityRef.getUuid(), entityRef.getType() );
        String collectionName = Schema.defaultCollectionName( entityRef.getType() );

        CollectionScope collectionScope = new CollectionScopeImpl( 
            appScope.getApplication(), appScope.getApplication(), collectionName );

        EntityCollectionManager ecm = managerCache.getEntityCollectionManager(collectionScope);

        // logger.debug("Loading entity {} type {} to {}", 
        //      new String[] { entityId.toString(), type, collectionName });

        org.apache.usergrid.persistence.model.entity.Entity cpEntity = 
            ecm.load( id ).toBlockingObservable().last();

        if ( cpEntity == null ) {
            return null;
        }

        Entity entity = new DynamicEntity( entityRef.getType(), cpEntity.getId().getUuid() );
        entity.setUuid( cpEntity.getId().getUuid() );
        Map<String, Object> entityMap = CpEntityMapUtils.toMap( cpEntity );
        entity.addProperties( entityMap );

        return entity; 
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

        String type = getDefaultSchema().getEntityType( entityClass );

        Id id = new SimpleId( entityId, type );
        String collectionName = Schema.defaultCollectionName( type );

        CollectionScope collectionScope = new CollectionScopeImpl( 
            appScope.getApplication(), appScope.getApplication(), collectionName );

        EntityCollectionManager ecm = managerCache.getEntityCollectionManager(collectionScope);

//        logger.debug("Loading entity {} type {} to {}", 
//            new String[] { entityId.toString(), type, collectionName });

        org.apache.usergrid.persistence.model.entity.Entity cpEntity = 
            ecm.load( id ).toBlockingObservable().last();

        if ( cpEntity == null ) {
            return null;
        }

        A entity = EntityFactory.newEntity( entityId, type, entityClass );
        entity.setProperties( CpEntityMapUtils.toMap( cpEntity ) );

        return entity;
    }


    @Override
    public Results get(Collection<UUID> entityIds, Class<? extends Entity> entityClass, 
            Level resultsLevel) throws Exception {

        String type = getDefaultSchema().getEntityType( entityClass );

        ArrayList<Entity> entities = new ArrayList<Entity>();

        for ( UUID uuid : entityIds ) {
            EntityRef ref = new SimpleEntityRef( type, uuid );
            Entity entity = get( ref, entityClass );

            if ( entity != null) {
                entities.add( entity );
            }
        }

        return Results.fromEntities( entities );
    }


    @Override
    public Results get(Collection<UUID> entityIds, String entityType, 
            Class<? extends Entity> entityClass, Level resultsLevel) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }
    

    @Override
    public void update( Entity entity ) throws Exception {

        // first, update entity index in its own collection scope
        String collectionName = Schema.defaultCollectionName( entity.getType() );

        CollectionScope collectionScope = new CollectionScopeImpl( 
            appScope.getApplication(), appScope.getApplication(), collectionName );

        IndexScope indexScope = new IndexScopeImpl( 
            appScope.getApplication(), appScope.getApplication(), entity.getType());

        EntityCollectionManager ecm = managerCache.getEntityCollectionManager( collectionScope );
        EntityIndex ei = managerCache.getEntityIndex( indexScope );

        Id entityId = new SimpleId( entity.getUuid(), entity.getType() );

        org.apache.usergrid.persistence.model.entity.Entity cpEntity =
            ecm.load( entityId ).toBlockingObservable().last();
        
        cpEntity = CpEntityMapUtils.fromMap( 
                cpEntity, entity.getProperties(), entity.getType(), true );

        cpEntity = ecm.write( cpEntity ).toBlockingObservable().last();
        ei.index( cpEntity );

        // next, update entity in every collection and connection scope in which it is indexed 
        updateEntityIndexes(entity, cpEntity );
    }


    private void updateEntityIndexes( Entity entity, 
            org.apache.usergrid.persistence.model.entity.Entity cpEntity ) throws Exception {

        RelationManager rm = getRelationManager( entity );
        Map<String, Map<UUID, Set<String>>> owners = rm.getOwners();
        
        logger.debug("Updating indexes of all {} collections owning the entity", 
                owners.keySet().size());

        for ( String ownerType : owners.keySet() ) {
            Map<UUID, Set<String>> collectionsByUuid = owners.get( ownerType );

            for ( UUID uuid : collectionsByUuid.keySet() ) {
                Set<String> collections = collectionsByUuid.get( uuid );
                for ( String coll : collections ) {
                    
                    IndexScope indexScope = new IndexScopeImpl( 
                        appScope.getApplication(), 
                        new SimpleId(uuid, ownerType), 
                        coll + CpRelationManager.COLLECTION_SUFFIX);

                    EntityIndex ei = managerCache.getEntityIndex( indexScope );

                    ei.index( cpEntity );
                }
            }
        }
    }


    @Override
    public void delete( EntityRef entityRef ) throws Exception {

        String collectionName = Schema.defaultCollectionName( entityRef.getType() );

        CollectionScope collectionScope = new CollectionScopeImpl( 
            appScope.getApplication(), appScope.getApplication(), collectionName );

        IndexScope defaultIndexScope = new IndexScopeImpl(
            appScope.getApplication(), appScope.getApplication(), entityRef.getType());

        EntityCollectionManager ecm = managerCache.getEntityCollectionManager(collectionScope);
        EntityIndex entityIndex = managerCache.getEntityIndex(defaultIndexScope);

        Id entityId = new SimpleId( entityRef.getUuid(), entityRef.getType() );

        org.apache.usergrid.persistence.model.entity.Entity entity =
            ecm.load( entityId ).toBlockingObservable().last();

        if ( entity != null ) {

            // first, delete entity in every collection and connection scope in which it is indexed 

            RelationManager rm = getRelationManager( entityRef );
            Map<String, Map<UUID, Set<String>>> owners = rm.getOwners();

            logger.debug("Deleting indexes of all {} collections owning the entity", 
                    owners.keySet().size());

            for ( String ownerType : owners.keySet() ) {
                Map<UUID, Set<String>> collectionsByUuid = owners.get( ownerType );

                for ( UUID uuid : collectionsByUuid.keySet() ) {
                    Set<String> collections = collectionsByUuid.get( uuid );
                    for ( String coll : collections ) {

                        IndexScope indexScope = new IndexScopeImpl( 
                            appScope.getApplication(), 
                            new SimpleId(uuid, ownerType), 
                            coll + CpRelationManager.COLLECTION_SUFFIX);

                        EntityIndex ei = managerCache.getEntityIndex( indexScope );

                        ei.deindex( entity );
                    }
                }
            }
            
            // next, delete entity index in its own collection scope
            entityIndex.deindex( entity );
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

        throw new UnsupportedOperationException("GeoIndexManager no longer supported."); 
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
        this.updateProperties( new SimpleEntityRef( "application", applicationId ), properties );
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
        return getAlias( applicationId, aliasType, alias );
    }

    @Override
    public EntityRef getAlias(
            UUID ownerId, String collectionType, String aliasValue) throws Exception {

        Assert.notNull( ownerId, "ownerId is required" );
        Assert.notNull( collectionType, "collectionType is required" );
        Assert.notNull( aliasValue, "aliasValue is required" );

        Map<String, EntityRef> results = getAlias( 
             ownerId, collectionType, Collections.singletonList( aliasValue ) );

        if ( results == null || results.size() == 0 ) {
            return null;
        }

        // add a warn statement so we can see if we have data migration issues.
        // TODO When we get an event system, trigger a repair if this is detected
        if ( results.size() > 1 ) {
            logger.warn("More than 1 entity with Owner id '{}' of type '{}' and alias '{}' exists. "
                    + " This is a duplicate alias, and needs audited", 
                    new Object[] { ownerId, collectionType, aliasValue } );
        }

        return results.get( aliasValue );
    }

    @Override
    public Map<String, EntityRef> getAlias(
            String aliasType, List<String> aliases) throws Exception {

        return getAlias( applicationId, aliasType, aliases );
    }

    @Override
    public Map<String, EntityRef> getAlias(
            UUID ownerId, String collName, List<String> aliases) throws Exception {

        Assert.notNull( ownerId, "ownerId is required" );
        Assert.notNull( collName, "collectionName is required" );
        Assert.notEmpty( aliases, "aliases are required" );

        String propertyName = Schema.getDefaultSchema().aliasProperty( collName );

        Map<String, EntityRef> results = new HashMap<String, EntityRef>();

//        for ( String alias : aliases ) {
//            for ( UUID id : getUUIDsForUniqueProperty( ownerId, collName, propertyName, alias)) {
//                results.put( alias, new SimpleEntityRef( collName, id ) );
//            }
//        }

        return results;
    }

    @Override
    public EntityRef validate( EntityRef entityRef ) throws Exception {
        // all entity refs should have type in CP
        return entityRef; 
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

        Entity entity = get( entityRef );
        return entity.getProperties();
    }

    @Override
    public void setProperty(
            EntityRef entityRef, String propertyName, Object propertyValue) throws Exception {

        setProperty( entityRef, propertyName, propertyValue, false);
    }

    @Override
    public void setProperty(EntityRef entityRef, String propertyName, 
            Object propertyValue, boolean override) throws Exception {

        if ( ( propertyValue instanceof String ) && ( (String)propertyValue ).equals( "" ) ) { 
            propertyValue = null;
        }

        Entity entity = get( entityRef );

//        if ( entity.getProperty(propertyName) != null && !override ) {
//            return;
//        }

        propertyValue = getDefaultSchema().validateEntityPropertyValue( 
            entity.getType(), propertyName, propertyValue );

        entity.setProperty( propertyName, propertyValue );
        entity.setProperty( PROPERTY_MODIFIED, getTimestampInMillis( newTimeUUID() ) );

        update( entity );
    }

    @Override
    public void updateProperties(
            EntityRef ref, Map<String, Object> properties) throws Exception {

        ref = validate( ref );
        properties = getDefaultSchema().cleanUpdatedProperties( ref.getType(), properties, false );

        EntityRef entityRef = ref;
        if ( entityRef instanceof CollectionRef ) {
            CollectionRef cref = (CollectionRef)ref;
            entityRef = cref.getItemRef();
        }

        Entity entity = get( entityRef );

        properties.put( PROPERTY_MODIFIED, getTimestampInMillis( newTimeUUID() ) );

        for ( String propertyName : properties.keySet() ) {
            Object propertyValue = properties.get( propertyName );

            Schema defaultSchema = Schema.getDefaultSchema();

            boolean entitySchemaHasProperty = 
                defaultSchema.hasProperty( entity.getType(), propertyName );

            propertyValue = getDefaultSchema().validateEntityPropertyValue( 
                entity.getType(), propertyName, propertyValue );

            if ( entitySchemaHasProperty ) {

                if ( !defaultSchema.isPropertyMutable( entity.getType(), propertyName ) ) {
                    continue;
                }

                if ( ( propertyValue == null ) 
                        && defaultSchema.isRequiredProperty( entity.getType(), propertyName ) ) {
                    continue;
                }
            }

            entity.setProperty(propertyName, propertyValue);
        }

        update(entity);
    }

    @Override
    public void deleteProperty(EntityRef entityRef, String propertyName) throws Exception {

        String collectionName = Schema.defaultCollectionName( entityRef.getType() );

        CollectionScope collectionScope = new CollectionScopeImpl( 
            appScope.getApplication(), appScope.getApplication(), collectionName );

        IndexScope defaultIndexScope = new IndexScopeImpl(
            appScope.getApplication(), appScope.getApplication(), entityRef.getType());

        EntityCollectionManager ecm = managerCache.getEntityCollectionManager(collectionScope);
        EntityIndex ei = managerCache.getEntityIndex(defaultIndexScope);

        Id entityId = new SimpleId( entityRef.getUuid(), entityRef.getType() );

        org.apache.usergrid.persistence.model.entity.Entity cpEntity =
            ecm.load( entityId ).toBlockingObservable().last();

        cpEntity.removeField( propertyName );

        cpEntity = ecm.write( cpEntity ).toBlockingObservable().last();
        ei.index( cpEntity );

        // update entity in every collection and connection scope in which it is indexed
        updateEntityIndexes( get( entityRef ), cpEntity );
    }

    @Override
    public Set<Object> getDictionaryAsSet(
            EntityRef entityRef, String dictionaryName) throws Exception {

        return new LinkedHashSet<>( getDictionaryAsMap( entityRef, dictionaryName ).keySet() );
    }

    @Override
    public void addToDictionary(
            EntityRef entityRef, String dictionaryName, Object elementValue) throws Exception {
        
        addToDictionary(entityRef,dictionaryName,elementValue,null);
    }

    @Override
    public void addToDictionary(EntityRef entityRef, String dictionaryName,
            Object elementName, Object elementValue) throws Exception {

        if (elementName == null) {
            return;
        }
        if (elementValue == null) {
            // placeholder value
            elementValue = new byte[0];
        }

        Entity entity = get( entityRef );

        if (!(elementName instanceof String)) {
            throw new IllegalArgumentException("Element name must be a string");
        }

        if (!(elementValue instanceof Serializable)) {
            throw new IllegalArgumentException("Element Value must be serializable.");
        }

        Map<String, Object> dictionary = entity.getDynamicProperties();
        Map<String, Object> props = (TreeMap) dictionary.get(dictionaryName);
        if (props == null) {
            props = new TreeMap();
        }
        props.put((String) elementName, elementValue);
        dictionary.put(dictionaryName, props);

        entity.addProperties(dictionary);
        update(entity);
    }

    @Override
    public void addSetToDictionary(
            EntityRef entityRef, String dictionaryName, Set<?> elementValues) throws Exception {

        if (dictionaryName == null) {
            return;
        }
        for (Object elementValue : elementValues) {
            addToDictionary(entityRef, dictionaryName, elementValue);
        }
    }

    @Override
    public void addMapToDictionary(
            EntityRef entityRef, String dictionaryName, Map<?, ?> elementValues) throws Exception {

        if(dictionaryName == null) {
            return;
        }

        Entity entity = get(entityRef);

        entity.getDynamicProperties().put( dictionaryName,elementValues );
        update( entity );
    }

    @Override
    public Map<Object, Object> getDictionaryAsMap(
            EntityRef entityRef, String dictionaryName) throws Exception {

        Entity entity = get( entityRef );
        Map<Object,Object> dictionary = ( TreeMap) entity.getProperty( dictionaryName );

        return dictionary;
    }

    @Override
    public Object getDictionaryElementValue(
            EntityRef entityRef, String dictionaryName, String elementName) throws Exception {

        Entity entity = get(entityRef);
        Map<String,Object> dictionary = ( Map<String, Object> ) entity.getProperty( dictionaryName );

        return dictionary.get( elementName );
    }

    @Override
    public void removeFromDictionary(
            EntityRef entityRef, String dictionaryName, Object elementName) throws Exception {
        if(elementName == null) {
            return;
        }

        Entity entity = get(entityRef);

        if ( !(elementName instanceof String) ) {
            throw new IllegalArgumentException( "Element name must be a string" );
        }

        Map<String,Object> dictionary = entity.getDynamicProperties();
        Map<String,Object> properties = ( Map<String, Object> ) dictionary.get( dictionaryName );
        properties.remove( elementName );
        dictionary.put( dictionaryName,properties );

        entity.setProperties( dictionary );

        update( entity );
    }

    @Override
    public Set<String> getDictionaries(EntityRef entityRef) throws Exception {
        Entity entity = get(entityRef);

        return entity.getProperties().keySet();

    }

    @Override
    public Map<String, Map<UUID, Set<String>>> getOwners(EntityRef entityRef) throws Exception {

        return getRelationManager(entityRef).getOwners();
    }

    @Override
    public boolean isCollectionMember(
            EntityRef owner, String collectionName, EntityRef entity) throws Exception {

        return getRelationManager(owner).isCollectionMember(collectionName, entity);
    }

    @Override
    public boolean isConnectionMember(
            EntityRef owner, String connectionName, EntityRef entity) throws Exception {

        return getRelationManager(owner).isConnectionMember(connectionName, entity);
    }

    @Override
    public Set<String> getCollections(EntityRef entityRef) throws Exception {

        return getRelationManager(entityRef).getCollections();
    }

    @Override
    public Results getCollection(
            EntityRef entityRef, String collectionName, UUID startResult, int count, 
            Level resultsLevel, boolean reversed) throws Exception {

        return getRelationManager(entityRef)
                .getCollection(collectionName, startResult, count, resultsLevel, reversed);
    }

    @Override
    public Results getCollection(
            UUID entityId, String collectionName, Query query, Level resultsLevel) 
            throws Exception {

        throw new UnsupportedOperationException("Cannot get entity by UUID alone"); 
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
    public Entity createItemInCollection( EntityRef entityRef, 
            String collectionName, String itemType, Map<String, Object> props) throws Exception {

        return getRelationManager( entityRef ).createItemInCollection(collectionName, itemType, props);
    }

    @Override
    public void removeFromCollection(
            EntityRef entityRef, String collectionName, EntityRef itemRef) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Set<String> getCollectionIndexes(
            EntityRef entity, String collectionName) throws Exception {

        return getRelationManager(entity).getCollectionIndexes(collectionName);
    }

    @Override
    public void copyRelationships(
            EntityRef srcEntityRef, String srcRelationName, EntityRef dstEntityRef, 
            String dstRelationName) throws Exception {

        getRelationManager(srcEntityRef)
                .copyRelationships(srcRelationName, dstEntityRef, dstRelationName);
    }

    @Override
    public ConnectionRef createConnection(ConnectionRef connection) throws Exception {

        return createConnection( 
                connection.getConnectingEntity(), 
                connection.getConnectionType(), 
                connection.getConnectedEntity());
    }

    @Override
    public ConnectionRef createConnection(
            EntityRef connectingEntity, String connectionType, EntityRef connectedEntityRef) 
            throws Exception {
        
        return getRelationManager( connectingEntity )
                .createConnection(connectionType, connectedEntityRef);
    }

    @Override
    public ConnectionRef createConnection(
            EntityRef connectingEntity, String pairedConnectionType, EntityRef pairedEntity, 
            String connectionType, EntityRef connectedEntityRef) throws Exception {

        return getRelationManager(connectingEntity).createConnection(
                pairedConnectionType, pairedEntity, connectionType, connectedEntityRef);
    }

    @Override
    public ConnectionRef createConnection(
            EntityRef connectingEntity, ConnectedEntityRef... connections) throws Exception {

        return getRelationManager(connectingEntity).connectionRef(connections);
    }

    @Override
    public ConnectionRef connectionRef(
            EntityRef connectingEntity, String connectionType, EntityRef connectedEntityRef) 
            throws Exception {

        return new ConnectionRefImpl(
                connectingEntity.getType(),
                connectingEntity.getUuid(),
                connectionType,
                connectedEntityRef.getType(),
                connectedEntityRef.getUuid());
    }

    @Override
    public ConnectionRef connectionRef(
            EntityRef connectingEntity, String pairedConnectionType, EntityRef pairedEntity, 
            String connectionType, EntityRef connectedEntityRef) throws Exception {

        return getRelationManager(connectingEntity).connectionRef(
                pairedConnectionType, pairedEntity, connectionType, connectedEntityRef);
    }

    @Override
    public ConnectionRef connectionRef(
            EntityRef connectingEntity, ConnectedEntityRef... connections) {
        
        return getRelationManager(connectingEntity).connectionRef(connections);
    }

    @Override
    public void deleteConnection(ConnectionRef connectionRef) throws Exception {

        getRelationManager(connectionRef).deleteConnection(connectionRef);
    }

    @Override
    public Set<String> getConnectionTypes(EntityRef ref) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Results getConnectedEntities(
            UUID entityId, String connectionType, String connectedEntityType, 
            Level resultsLevel) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Results getConnectingEntities(
            UUID entityId, String connectionType, String connectedEntityType, 
            Level resultsLevel) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Results getConnectingEntities(UUID uuid, String connectionType, 
            String entityType, Level level, int count) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Results searchConnectedEntities(
            EntityRef connectingEntity, Query query) throws Exception {

        return getRelationManager( connectingEntity ).searchConnectedEntities( query );
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
            UUID groupId, String roleName, Level level) throws Exception {
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

        Entity entity = get( entityRef );

        if ( entity == null ) {
            logger.warn( "Entity {}/{} not found ", entityRef.getUuid(), entityRef.getType() );
            return null;
        }

        A ret = EntityFactory.newEntity( entityRef.getUuid(), entityRef.getType(), entityClass );
        ret.setProperties( entity.getProperties() );

        return ret;
    }

    
    @Override
    public Results getEntities(List<UUID> ids, String type) {

        ArrayList<Entity> entities = new ArrayList<Entity>();

        for ( UUID uuid : ids ) {
            EntityRef ref = new SimpleEntityRef( type, uuid );
            Entity entity = null; 
            try {
                entity = get( ref );
            } catch (Exception ex) {
                logger.warn("Entity {}/{} not found", uuid, type);
            }

            if ( entity != null) {
                entities.add( entity );
            }
        }

        return Results.fromEntities( entities );
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
            Mutator<ByteBuffer> ignored, 
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
                properties.put( PROPERTY_CREATED, (long)(timestamp / 1000) );
            }

            if ( properties.get( PROPERTY_MODIFIED ) == null ) {
                properties.put( PROPERTY_MODIFIED, (long)(timestamp / 1000) );
            }
        }
        else {
            properties.put( PROPERTY_CREATED, (long)(timestamp / 1000) );
            properties.put( PROPERTY_MODIFIED, (long)(timestamp / 1000) );
        }

        // special case timestamp and published properties
        // and dictionary their timestamp values if not set
        // this is sure to break something for someone someday

        if ( properties.containsKey( PROPERTY_TIMESTAMP ) ) {
            long ts = getLong( properties.get( PROPERTY_TIMESTAMP ) );
            if ( ts <= 0 ) {
                properties.put( PROPERTY_TIMESTAMP, (long)(timestamp / 1000) );
            }
        }

        A entity = EntityFactory.newEntity( itemId, eType, entityClass );
        entity.addProperties(properties);
        
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
//            entity.setUuid( message.getUuid() );
            return entity;
        }

        org.apache.usergrid.persistence.model.entity.Entity cpEntity = entityToCpEntity( entity ); 

        // prepare to write and index Core Persistence Entity into default scope
        CollectionScope collectionScope = new CollectionScopeImpl( 
            appScope.getApplication(), appScope.getApplication(), collectionName );

        IndexScope defaultIndexScope = new IndexScopeImpl(
            appScope.getApplication(), appScope.getApplication(), entity.getType());

        EntityCollectionManager ecm = managerCache.getEntityCollectionManager(collectionScope);
        EntityIndex ei = managerCache.getEntityIndex(defaultIndexScope);

        try {
            cpEntity = ecm.write( cpEntity ).toBlockingObservable().last();

        } catch (WriteUniqueVerifyException wuve) {

            // we may have multiple conflicts, but caller expects only one 
            Map<String, Field> violiations = wuve.getVioliations();
            Field conflict = violiations.get( violiations.keySet().iterator().next() );

            throw new DuplicateUniquePropertyExistsException( 
                entity.getType(), conflict.getName(), conflict.getValue());
        }
        ei.index( cpEntity );

        // reflect changes in the legacy Entity
        entity.setUuid( cpEntity.getId().getUuid() );
        Map<String, Object> entityMap = CpEntityMapUtils.toMap( cpEntity );
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

        throw new UnsupportedOperationException("This method is not supported.");
    }

    @Override
    public Mutator<ByteBuffer> batchUpdateDictionary(
            Mutator<ByteBuffer> batch, EntityRef entity, String dictionaryName, Object elementValue, 
            boolean removeFromDictionary, UUID timestampUuid) throws Exception {

        return batchUpdateDictionary( batch, entity, dictionaryName, elementValue, null, 
                removeFromDictionary, timestampUuid );
    }

    @Override
    public Mutator<ByteBuffer> batchUpdateProperties(
            Mutator<ByteBuffer> batch, EntityRef entity, Map<String, Object> properties, 
            UUID timestampUuid) throws Exception {
        
        throw new UnsupportedOperationException("Not supported yet.");
    }

    //TODO: ask what the difference is.
    @Override
    public Set<String> getDictionaryNames(EntityRef entity) throws Exception {

        return getDictionaries( entity );
    }

    @Override
    public void insertEntity( EntityRef ref ) throws Exception {

        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public UUID getApplicationId() {

        return applicationId;
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

        // refresh system indexes 
        emf.refreshIndex();

        // refresh application entity index
        IndexScope indexScope = new IndexScopeImpl(
            appScope.getApplication(), new SimpleId("dummy"), "dummy");
        EntityIndex ei = managerCache.getEntityIndex( indexScope );
        ei.refresh();

        logger.debug("Refreshed index for system and application: " + applicationId);
    }


    private org.apache.usergrid.persistence.model.entity.Entity entityToCpEntity( Entity entity) {

        org.apache.usergrid.persistence.model.entity.Entity cpEntity = 
            new org.apache.usergrid.persistence.model.entity.Entity(
                new SimpleId( entity.getUuid(), entity.getType() ));

        cpEntity  = CpEntityMapUtils.fromMap( 
            cpEntity, entity.getProperties(), entity.getType(), true );

        cpEntity  = CpEntityMapUtils.fromMap( 
            cpEntity, entity.getDynamicProperties(), entity.getType(), true );

        return cpEntity;
    }


}


