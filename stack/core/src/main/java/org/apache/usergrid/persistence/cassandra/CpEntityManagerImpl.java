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
package org.apache.usergrid.persistence.cassandra;

import com.google.inject.Guice;
import com.google.inject.Injector;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import me.prettyprint.hector.api.mutation.Mutator;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.EntityRef;
import org.apache.usergrid.persistence.Query;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.persistence.TypedEntity;
import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.collection.impl.CollectionScopeImpl;
import org.apache.usergrid.persistence.entities.Application;
import org.apache.usergrid.persistence.index.EntityCollectionIndex;
import org.apache.usergrid.persistence.index.EntityCollectionIndexFactory;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.field.StringField;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hybrid Core Persistence implementation of EntityManager.
 */
public class CpEntityManagerImpl extends EntityManagerImpl {
    private static final Logger logger = LoggerFactory.getLogger(CpEntityManagerImpl.class);

    private static EntityCollectionManagerFactory ecmf;
    private static EntityCollectionIndexFactory ecif ;
    static {
        Injector injector = Guice.createInjector( new CpModule() );
        ecmf = injector.getInstance( EntityCollectionManagerFactory.class );
        ecif = injector.getInstance( EntityCollectionIndexFactory.class );
    }
    private final Map<CollectionScope, EntityCollectionManager> managers = new HashMap<>();
    private final Map<CollectionScope, EntityCollectionIndex> indexes = new HashMap<>();

    private CollectionScope applicationScope = null;

    @Override
    public EntityManager init( 
            EntityManagerFactoryImpl emf, 
            CassandraService cass, 
            CounterUtils counterUtils,
            UUID applicationId, 
            boolean skipAggregateCounters ) {

        super.init( emf, cass, counterUtils, applicationId, skipAggregateCounters );
        setApplicationId(applicationId);
        return this;
    }

    @Override
    public void setApplicationId( UUID applicationId ) {
        super.setApplicationId( applicationId );
        try {
            Application app = getApplication();

            Id orgId = getOrganizationId( app );
            Id appId = new SimpleId( applicationId, "application");

            applicationScope = 
                new CollectionScopeImpl(orgId, appId, "applicationScope");

        } catch (Exception ex) {
            logger.error("Error getting applicationScope", ex);
        }
    }

    private Id getOrganizationId( Application app ) {

        Id defaultOrgId = new SimpleId("zzz_default_zzz");
        Id defaultAppId = new SimpleId("zzz_default_zzz");
        CollectionScope defaultScope = new CollectionScopeImpl(
                defaultOrgId, defaultAppId, "organizations" );

        EntityCollectionManager ecm = getManager( defaultScope );
        EntityCollectionIndex eci = getIndex( defaultScope );

        org.apache.usergrid.persistence.query.Query q = 
            org.apache.usergrid.persistence.query.Query.fromQL(
                "name = '" + app.getOrganizationName() + "'");

        org.apache.usergrid.persistence.query.Results execute = eci.execute(q);

        if ( execute.isEmpty() ) { // create if does not exist 

            org.apache.usergrid.persistence.model.entity.Entity entity =
                new org.apache.usergrid.persistence.model.entity.Entity(
                    new SimpleId(UUIDGenerator.newTimeUUID(), "organization" ));

            entity.setField(new StringField("name", app.getOrganizationName()));
            entity = ecm.write( entity ).toBlockingObservable().last();

            Id orgId = entity.getId();
            return orgId;
        } 

        org.apache.usergrid.persistence.model.entity.Entity entity =
            execute.getEntities().get(0);

        Id orgId = entity.getId();
        return orgId;
    }


    private EntityCollectionIndex getIndex( CollectionScope scope ) { 
        EntityCollectionIndex eci = indexes.get( scope );
        if ( eci == null ) {
            eci = ecif.createCollectionIndex( scope );
            indexes.put( scope, eci );
        }
        return eci;
    }

    private EntityCollectionManager getManager( CollectionScope scope ) { 
        EntityCollectionManager ecm = managers.get( scope );
        if ( ecm == null ) {
            ecm = ecmf.createCollectionManager( scope );
            managers.put( scope, ecm);
        }
        return ecm;
    }


    @Override
    public Set<String> getCollectionIndexes(
            EntityRef entity, String collectionName) throws Exception {
        return super.getCollectionIndexes(entity, collectionName); 
    }

    @Override
    public Results searchCollection(
            EntityRef entityRef, String collectionName, Query query) throws Exception {
        return super.searchCollection(entityRef, collectionName, query); 
    }

    @Override
    public Entity createItemInCollection(
            EntityRef entityRef, 
            String collectionName, 
            String itemType, 
            Map<String, Object> properties) throws Exception {
        return super.createItemInCollection(entityRef, collectionName, itemType, properties); 
    }

    @Override
    public Results getCollection(
            UUID entityId, 
            String collectionName, 
            Query query, 
            Results.Level resultsLevel) throws Exception {
        return super.getCollection(entityId, collectionName, query, resultsLevel); 
    }

    @Override
    public Results getCollection(
            EntityRef entityRef, 
            String collectionName, 
            UUID startResult, 
            int count, 
            Results.Level resultsLevel, 
            boolean reversed) throws Exception {
        return super.getCollection(entityRef, 
                collectionName, 
                startResult, 
                count, 
                resultsLevel, 
                reversed); 
    }

    @Override
    public Set<String> getCollections(
            EntityRef entityRef) throws Exception {
        return super.getCollections(entityRef); 
    }

    @Override
    public boolean isPropertyValueUniqueForEntity(
            String entityType, 
            String propertyName, 
            Object propertyValue) throws Exception {
        return super.isPropertyValueUniqueForEntity(
                entityType, propertyName, propertyValue); 
    }

    @Override
    public void updateProperties(
            EntityRef entityRef, Map<String, Object> properties) throws Exception {
        super.updateProperties(entityRef, properties); 
    }

    @Override
    public void setProperty(
            EntityRef entityRef, 
            String propertyName, 
            Object propertyValue, 
            boolean override) throws Exception {
        super.setProperty(entityRef, 
                propertyName, propertyValue, override); 
    }

    @Override
    public void setProperty(
            EntityRef entityRef, String propertyName, Object propertyValue) throws Exception {
        super.setProperty(entityRef, propertyName, propertyValue); 
    }

    @Override
    public List<Entity> getPartialEntities(
            Collection<UUID> ids, Collection<String> fields) throws Exception {
        return super.getPartialEntities(ids, fields); 
    }

    @Override
    public Map<String, Object> getProperties(
            EntityRef entityRef) throws Exception {
        return super.getProperties(entityRef); 
    }

    @Override
    public Object getProperty(
            EntityRef entityRef, String propertyName) throws Exception {
        return super.getProperty(entityRef, propertyName); 
    }

    @Override
    public void update(
            Entity entity) throws Exception {
        super.update(entity); 
    }

    @Override
    public Results loadEntities(
            Results results, 
            Results.Level resultsLevel, 
            Map<UUID, UUID> associatedMap, 
            int count) throws Exception {
        return super.loadEntities(results, resultsLevel, associatedMap, count); 
    }

    @Override
    public Results loadEntities(
            Results results, Results.Level resultsLevel, int count) throws Exception {
        return super.loadEntities(results, resultsLevel, count); 
    }

    @Override
    public Results get(
            Collection<UUID> entityIds, 
            String entityType, 
            Class<? extends Entity> entityClass, 
            Results.Level resultsLevel) throws Exception {
        return super.get(entityIds, entityType, entityClass, resultsLevel); 
    }

    @Override
    public Results get(
            Collection<UUID> entityIds, 
            Class<? extends Entity> entityClass, 
            Results.Level resultsLevel) throws Exception {
        return super.get(entityIds, entityClass, resultsLevel); 
    }

    @Override
    public Results get(
            Collection<UUID> entityIds) throws Exception {
        return super.get(entityIds); 
    }

    @Override
    public Results get(
            Collection<UUID> entityIds, 
            Results.Level resultsLevel) throws Exception {
        return super.get(entityIds, resultsLevel); 
    }

    @Override
    public <A extends Entity> A get(
            UUID entityId, Class<A> entityClass) throws Exception {
        return super.get(entityId, entityClass); 
    }

    @Override
    public <A extends Entity> A get(
            EntityRef entityRef, Class<A> entityClass) throws Exception {
        return super.get(entityRef, entityClass); 
    }

    @Override
    public Entity get(
            EntityRef entityRef) throws Exception {
        return super.get(entityRef); 
    }

    @Override
    public Entity get(
            UUID entityid) throws Exception {
        return super.get(entityid); 
    }

    @Override
    public EntityRef validate(
            EntityRef entityRef, boolean verify) throws Exception {
        return super.validate(entityRef, verify); 
    }

    @Override
    public EntityRef validate(
            EntityRef entityRef) throws Exception {
        return super.validate(entityRef); 
    }

    @Override
    public void delete(
            EntityRef entityRef) throws Exception {
        super.delete(entityRef); 
    }

    @Override
    public void deleteEntity(
            UUID entityId) throws Exception {
        super.deleteEntity(entityId); 
    }

    @Override
    public void updateProperties(
            UUID entityId, Map<String, Object> properties) throws Exception {
        super.updateProperties(entityId, properties); 
    }

    @Override
    public Set<String> getPropertyNames(
            EntityRef entity) throws Exception {
        return super.getPropertyNames(entity); 
    }

    @Override
    public <A extends Entity> List<A> getEntities(
            Collection<UUID> entityIds, Class<A> entityClass) throws Exception {
        return super.getEntities(entityIds, entityClass); 
    }

    @Override
    public <A extends Entity> A getEntity(
            UUID entityId, Class<A> entityClass) throws Exception {
        return super.getEntity(entityId, entityClass); 
    }

    @Override
    public String getEntityType(
            UUID entityId) throws Exception {
        return super.getEntityType(entityId); 
    }

    @Override
    public <A extends Entity> A batchCreate(
            Mutator<ByteBuffer> m, 
            String entityType, 
            Class<A> entityClass, 
            Map<String, Object> properties, 
            UUID importId, 
            UUID timestampUuid) throws Exception {
        return super.batchCreate(m, 
                entityType, 
                entityClass, 
                properties, 
                importId, 
                timestampUuid); 
    }

    @Override
    public <A extends Entity> A create(
            String entityType, 
            Class<A> entityClass, 
            Map<String, Object> properties, 
            UUID importId) throws Exception {
        return super.create(entityType, entityClass, properties, importId); 
    }

    @Override
    public Entity create(
            String entityType, Map<String, Object> properties) throws Exception {
        return super.create(entityType, properties); 
    }

    @Override
    public Entity create(
            UUID importId, 
            String entityType, 
            Map<String, Object> properties) throws Exception {
        return super.create(importId, entityType, properties); 
    }

    @Override
    public <A extends Entity> A create(
            String entityType, 
            Class<A> entityClass, 
            Map<String, Object> properties) throws Exception {
        return super.create(entityType, entityClass, properties); 
    }

    @Override
    public boolean isPropertyValueUniqueForEntity(
            UUID ownerEntityId, 
            String entityType, 
            String propertyName, 
            Object propertyValue) throws Exception {
        return super.isPropertyValueUniqueForEntity(ownerEntityId, 
                entityType, 
                propertyName, 
                propertyValue); 
    }

    @Override
    public Mutator<ByteBuffer> batchUpdateProperties(
            Mutator<ByteBuffer> batch, 
            EntityRef entity, 
            Map<String, Object> properties, 
            UUID timestampUuid) throws Exception {
        return super.batchUpdateProperties(batch, entity, properties, timestampUuid); 
    }

    @Override
    public Mutator<ByteBuffer> batchSetProperty(
            Mutator<ByteBuffer> batch, 
            EntityRef entity, 
            String propertyName, 
            Object propertyValue, 
            boolean force, 
            boolean noRead, 
            UUID timestampUuid) throws Exception {
        return super.batchSetProperty(batch, 
                entity, 
                propertyName, 
                propertyValue, 
                force, 
                noRead, 
                timestampUuid); 
    }

    @Override
    public Mutator<ByteBuffer> batchSetProperty(
            Mutator<ByteBuffer> batch, 
            EntityRef entity, 
            String propertyName, 
            Object propertyValue, 
            UUID timestampUuid) throws Exception {
        return super.batchSetProperty(batch, 
                entity, 
                propertyName, 
                propertyValue, 
                timestampUuid); 
    }

    @Override
    public <A extends TypedEntity> A create(
            A entity) throws Exception {
        return super.create(entity);
    }

}
