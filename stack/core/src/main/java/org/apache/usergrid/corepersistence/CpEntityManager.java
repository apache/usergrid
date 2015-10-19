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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

import com.google.common.base.Optional;
import org.apache.usergrid.corepersistence.index.IndexLocationStrategyFactory;
import org.apache.usergrid.corepersistence.service.CollectionService;
import org.apache.usergrid.corepersistence.service.ConnectionService;
import org.apache.usergrid.persistence.index.EntityIndex;
import org.apache.usergrid.persistence.index.IndexLocationStrategy;
import org.apache.usergrid.persistence.index.utils.*;
import org.apache.usergrid.utils.*;
import org.apache.usergrid.utils.ClassUtils;
import org.apache.usergrid.utils.ConversionUtils;
import org.apache.usergrid.utils.UUIDUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import org.apache.usergrid.corepersistence.asyncevents.AsyncEventService;
import org.apache.usergrid.corepersistence.pipeline.builder.PipelineBuilderFactory;
import org.apache.usergrid.corepersistence.util.CpEntityMapUtils;
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.persistence.AggregateCounter;
import org.apache.usergrid.persistence.AggregateCounterSet;
import org.apache.usergrid.persistence.CollectionRef;
import org.apache.usergrid.persistence.ConnectedEntityRef;
import org.apache.usergrid.persistence.ConnectionRef;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityFactory;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.EntityRef;
import org.apache.usergrid.persistence.IndexBucketLocator;
import org.apache.usergrid.persistence.Query;
import org.apache.usergrid.persistence.Query.Level;
import org.apache.usergrid.persistence.RelationManager;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.persistence.Schema;
import org.apache.usergrid.persistence.SimpleEntityRef;
import org.apache.usergrid.persistence.SimpleRoleRef;
import org.apache.usergrid.persistence.TypedEntity;
import org.apache.usergrid.persistence.cassandra.ApplicationCF;
import org.apache.usergrid.persistence.cassandra.CassandraPersistenceUtils;
import org.apache.usergrid.persistence.cassandra.CassandraService;
import org.apache.usergrid.persistence.cassandra.ConnectionRefImpl;
import org.apache.usergrid.persistence.cassandra.CounterUtils;
import org.apache.usergrid.persistence.cassandra.util.TraceParticipant;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.FieldSet;
import org.apache.usergrid.persistence.collection.exception.WriteUniqueVerifyException;
import org.apache.usergrid.persistence.core.metrics.MetricsFactory;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.entities.Application;
import org.apache.usergrid.persistence.entities.Event;
import org.apache.usergrid.persistence.entities.Group;
import org.apache.usergrid.persistence.entities.Role;
import org.apache.usergrid.persistence.entities.User;
import org.apache.usergrid.persistence.exceptions.DuplicateUniquePropertyExistsException;
import org.apache.usergrid.persistence.exceptions.EntityNotFoundException;
import org.apache.usergrid.persistence.exceptions.RequiredPropertyNotFoundException;
import org.apache.usergrid.persistence.exceptions.UnexpectedEntityTypeException;
import org.apache.usergrid.persistence.graph.GraphManager;
import org.apache.usergrid.persistence.graph.GraphManagerFactory;
import org.apache.usergrid.persistence.graph.SearchByEdgeType;
import org.apache.usergrid.persistence.graph.SearchEdgeType;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchByEdgeType;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchEdgeType;
import org.apache.usergrid.persistence.index.query.CounterResolution;
import org.apache.usergrid.persistence.index.query.Identifier;
import org.apache.usergrid.persistence.map.MapManager;
import org.apache.usergrid.persistence.map.MapScope;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.field.Field;
import org.apache.usergrid.persistence.model.field.StringField;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import com.google.common.base.Preconditions;

import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.ColumnSlice;
import me.prettyprint.hector.api.beans.CounterRow;
import me.prettyprint.hector.api.beans.CounterRows;
import me.prettyprint.hector.api.beans.CounterSlice;
import me.prettyprint.hector.api.beans.DynamicComposite;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.beans.HCounterColumn;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.MultigetSliceCounterQuery;
import me.prettyprint.hector.api.query.QueryResult;
import me.prettyprint.hector.api.query.SliceCounterQuery;
import rx.Observable;

import static java.lang.String.CASE_INSENSITIVE_ORDER;
import static java.util.Arrays.asList;

import static me.prettyprint.hector.api.factory.HFactory.createCounterSliceQuery;
import static me.prettyprint.hector.api.factory.HFactory.createMutator;
import static org.apache.commons.lang.StringUtils.capitalize;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.usergrid.corepersistence.util.CpEntityMapUtils.entityToCpEntity;
import static org.apache.usergrid.corepersistence.util.CpNamingUtils.createConnectionTypeSearch;
import static org.apache.usergrid.corepersistence.util.CpNamingUtils.createGraphOperationTimestamp;
import static org.apache.usergrid.corepersistence.util.CpNamingUtils.getConnectionNameFromEdgeName;
import static org.apache.usergrid.persistence.Schema.COLLECTION_ROLES;
import static org.apache.usergrid.persistence.Schema.COLLECTION_USERS;
import static org.apache.usergrid.persistence.Schema.DICTIONARY_PERMISSIONS;
import static org.apache.usergrid.persistence.Schema.DICTIONARY_ROLENAMES;
import static org.apache.usergrid.persistence.Schema.DICTIONARY_ROLETIMES;
import static org.apache.usergrid.persistence.Schema.DICTIONARY_SETS;
import static org.apache.usergrid.persistence.Schema.PROPERTY_CREATED;
import static org.apache.usergrid.persistence.Schema.PROPERTY_INACTIVITY;
import static org.apache.usergrid.persistence.Schema.PROPERTY_MODIFIED;
import static org.apache.usergrid.persistence.Schema.PROPERTY_NAME;
import static org.apache.usergrid.persistence.Schema.PROPERTY_TIMESTAMP;
import static org.apache.usergrid.persistence.Schema.PROPERTY_TYPE;
import static org.apache.usergrid.persistence.Schema.PROPERTY_UUID;
import static org.apache.usergrid.persistence.Schema.TYPE_APPLICATION;
import static org.apache.usergrid.persistence.Schema.TYPE_ENTITY;
import static org.apache.usergrid.persistence.SimpleEntityRef.getUuid;
import static org.apache.usergrid.persistence.cassandra.ApplicationCF.APPLICATION_AGGREGATE_COUNTERS;
import static org.apache.usergrid.persistence.cassandra.ApplicationCF.ENTITY_COMPOSITE_DICTIONARIES;
import static org.apache.usergrid.persistence.cassandra.ApplicationCF.ENTITY_COUNTERS;
import static org.apache.usergrid.persistence.cassandra.ApplicationCF.ENTITY_DICTIONARIES;
import static org.apache.usergrid.persistence.cassandra.CassandraService.ALL_COUNT;
import static org.apache.usergrid.persistence.cassandra.Serializers.be;
import static org.apache.usergrid.persistence.cassandra.Serializers.le;
import static org.apache.usergrid.persistence.cassandra.Serializers.se;
import static org.apache.usergrid.persistence.cassandra.Serializers.ue;
import static org.apache.usergrid.utils.ClassUtils.cast;
import static org.apache.usergrid.utils.ConversionUtils.bytebuffer;
import static org.apache.usergrid.utils.ConversionUtils.getLong;
import static org.apache.usergrid.utils.ConversionUtils.object;
import static org.apache.usergrid.utils.ConversionUtils.string;
import static org.apache.usergrid.utils.InflectionUtils.singularize;


/**
 * Implement good-old Usergrid EntityManager with the new-fangled Core Persistence API.
 */
public class CpEntityManager implements EntityManager {
    private static final Logger logger = LoggerFactory.getLogger( CpEntityManager.class );

    public static final String APPLICATION_COLLECTION = "application.collection.";
    public static final String APPLICATION_ENTITIES = "application.entities";
    public static final long ONE_COUNT = 1L;

    private final UUID applicationId;
    private final EntityManagerFig entityManagerFig;
    private Application application;


    private final ManagerCache managerCache;

    private final ApplicationScope applicationScope;

    private final CassandraService cass;

    private final CounterUtils counterUtils;

    private final AsyncEventService indexService;

    private final CollectionService collectionService;
    private final ConnectionService connectionService;


    private final GraphManagerFactory graphManagerFactory;

    private boolean skipAggregateCounters;
    private MetricsFactory metricsFactory;
    private Timer aggCounterTimer;
    private Timer entCreateTimer;
    private Timer entCreateBatchTimer;
    private Timer esDeletePropertyTimer;
    private Timer entAddDictionaryTimer;
    private Timer entAddDictionarySetTimer;
    private Timer entAddDictionaryMapTimer;
    private Timer entRemoveDictionaryTimer;
    private Timer entCreateRoleTimer;
    private Timer entCreateRolePermissionsTimer;
    private Timer entGrantGroupPermissionTimer;
    private Timer entRevokeGroupPermissionTimer;
    private Timer entIncrementAggregateCountersTimer;
    private Timer entGetAggregateCountersQueryTimer;
    private Timer entGetEntityCountersTimer;
    private Timer esIndexEntityCollectionTimer;
    private Timer entRevokeRolePermissionsTimer;
    private Timer entGetRepairedEntityTimer;
    private Timer updateEntityTimer;
    private Meter updateEntityMeter;
//    private EntityIndex ei;

    private EntityCollectionManager ecm;

    //    /** Short-term cache to keep us from reloading same Entity during single request. */
//    private LoadingCache<EntityScope, org.apache.usergrid.persistence.model.entity.Entity> entityCache;


    /**
     * Fugly, make this part of DI
     * @param cass
     * @param counterUtils
     * @param managerCache
     * @param metricsFactory
     * @param applicationId
     */
    public CpEntityManager( final CassandraService cass, final CounterUtils counterUtils, final AsyncEventService indexService, final ManagerCache managerCache,
                            final MetricsFactory metricsFactory,
                            final EntityManagerFig entityManagerFig,
                            final GraphManagerFactory graphManagerFactory,
                            final CollectionService collectionService,
                            final ConnectionService connectionService,
                            final UUID applicationId ) {

        this.entityManagerFig = entityManagerFig;

        Preconditions.checkNotNull( cass, "cass must not be null" );
        Preconditions.checkNotNull( counterUtils, "counterUtils must not be null" );
        Preconditions.checkNotNull( managerCache, "managerCache must not be null" );
        Preconditions.checkNotNull( applicationId, "applicationId must not be null" );
        Preconditions.checkNotNull( indexService, "indexService must not be null" );

        Preconditions.checkNotNull( graphManagerFactory, "graphManagerFactory must not be null" );
        Preconditions.checkNotNull( connectionService, "connectionService must not be null" );
        Preconditions.checkNotNull( collectionService, "collectionService must not be null" );

        this.graphManagerFactory = graphManagerFactory;
        this.connectionService = connectionService;
        this.collectionService = collectionService;



        this.managerCache = managerCache;
        this.applicationId = applicationId;
        this.indexService = indexService;

        applicationScope = CpNamingUtils.getApplicationScope( applicationId );

        ecm =  managerCache.getEntityCollectionManager( applicationScope );

        this.cass = cass;
        this.counterUtils = counterUtils;

        //Timer Setup
        this.metricsFactory = metricsFactory;
        this.aggCounterTimer = this.metricsFactory.getTimer(CpEntityManager.class, "aggregate_counters.get");
        this.entIncrementAggregateCountersTimer = this.metricsFactory.getTimer(CpEntityManager.class, "aggregate_counters.increment");
        this.entGetAggregateCountersQueryTimer = this.metricsFactory.getTimer(CpEntityManager.class, "aggregate_counters_query.get");

        this.entCreateTimer = this.metricsFactory.getTimer(CpEntityManager.class, "entity.create");
        this.updateEntityMeter = this.metricsFactory.getMeter(CpEntityManager.class, "entity.update");
        this.updateEntityTimer = this.metricsFactory.getTimer(CpEntityManager.class, "entity.update");

        this.entCreateBatchTimer = this.metricsFactory.getTimer(CpEntityManager.class, "batch.create");

        this.esDeletePropertyTimer = this.metricsFactory.getTimer(CpEntityManager.class, "es_property.delete");
        this.entAddDictionaryTimer = this.metricsFactory.getTimer(CpEntityManager.class, "dictionary.add");
        this.entAddDictionarySetTimer = this.metricsFactory.getTimer(CpEntityManager.class, "dictionary_set.add");
        this.entAddDictionaryMapTimer = this.metricsFactory.getTimer(CpEntityManager.class, "dictionary_map.add");
        this.entRemoveDictionaryTimer = this.metricsFactory.getTimer(CpEntityManager.class, "dictionary.remove");

        this.entCreateRoleTimer = this.metricsFactory.getTimer(CpEntityManager.class, "role.create");
        this.entRevokeRolePermissionsTimer = this.metricsFactory.getTimer(CpEntityManager.class, "role.revoke_permissions");
        this.entCreateRolePermissionsTimer = this.metricsFactory.getTimer(CpEntityManager.class, "role.create_permissions");

        this.entGrantGroupPermissionTimer = this.metricsFactory.getTimer(CpEntityManager.class, "group.grant_permission");
        this.entRevokeGroupPermissionTimer = this.metricsFactory.getTimer(CpEntityManager.class, "group.revoke_permission");

        this.entGetEntityCountersTimer = this.metricsFactory.getTimer(CpEntityManager.class, "entity_counters.get");
        this.esIndexEntityCollectionTimer = this.metricsFactory.getTimer(CpEntityManager.class, "es.index_entity_to_collection");
        this.entGetRepairedEntityTimer = this.metricsFactory.getTimer(CpEntityManager.class, "repaired_entity.get");


        // set to false for now
        this.skipAggregateCounters = false;
    }


    /**
     * Load entity from short-term cache. Package scope so that CpRelationManager can use it too.
     *
     * @param entityId Load the entity by entityId
     *
     * @return Entity or null if not found
     */
    org.apache.usergrid.persistence.model.entity.Entity load( Id entityId ) {

            return ecm .load( entityId ).toBlocking()
                                       .lastOrDefault(null);

    }


    public ManagerCache getManagerCache() {
        return managerCache;
    }


    public ApplicationScope getApplicationScope() {
        return applicationScope;
    }


    @Override
    public Entity create( String entityType, Map<String, Object> properties ) throws Exception {
        return create( entityType, null, properties );
    }


    @Override
    public <A extends Entity> A create( String entityType, Class<A> entityClass, Map<String, Object> properties )
            throws Exception {

        if ( ( entityType != null ) && ( entityType.startsWith( TYPE_ENTITY ) || entityType
                .startsWith( "entities" ) ) ) {
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
    public <A extends TypedEntity> A create( A entity ) throws Exception {
        return ( A ) create( entity.getType(), entity.getClass(), entity.getProperties() );
    }


    @Override
    public Entity create( UUID importId, String entityType, Map<String, Object> properties ) throws Exception {

        UUID timestampUuid = importId != null ? importId : UUIDUtils.newTimeUUID();
        Keyspace ko = cass.getApplicationKeyspace( applicationId );
        Mutator<ByteBuffer> m = createMutator( ko, be );

        Entity entity = batchCreate( m,entityType, null, properties, importId, timestampUuid );

        //Adding graphite metrics
        Timer.Context timeCassCreation = entCreateTimer.time();
        m.execute();
        timeCassCreation.stop();
        return entity;
    }

    @Override
    public Entity create( Id id, Map<String, Object> properties ) throws Exception {

        Keyspace ko = cass.getApplicationKeyspace( applicationId );
        Mutator<ByteBuffer> m = createMutator( ko, be );

        Entity entity = batchCreate( m, id.getType(), null, properties, id.getUuid(), UUIDUtils.newTimeUUID() );

        //Adding graphite metrics
        Timer.Context timeCassCreation = entCreateTimer.time();
        m.execute();
        timeCassCreation.stop();

        return entity;
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
    @TraceParticipant
    public <A extends Entity> A create( String entityType, Class<A> entityClass,
            Map<String, Object> properties, UUID importId ) throws Exception {

        UUID timestampUuid = importId != null ? importId : UUIDUtils.newTimeUUID();

        Keyspace ko = cass.getApplicationKeyspace( applicationId );
        Mutator<ByteBuffer> m = createMutator( ko, be );


        A entity = batchCreate( m, entityType, entityClass, properties, importId, timestampUuid );

        //Adding graphite metrics
        Timer.Context timeEntityCassCreation = entCreateBatchTimer.time();
        m.execute();
        timeEntityCassCreation.stop();

        return entity;
    }

    public Entity convertMvccEntityToEntity( org.apache.usergrid.persistence.model.entity.Entity entity){
        if(entity == null) {
            return null;
        }
        Class clazz = Schema.getDefaultSchema().getEntityClass(entity.getId().getType());

        Entity oldFormatEntity = EntityFactory.newEntity(entity.getId().getUuid(), entity.getId().getType(), clazz);
        oldFormatEntity.setProperties(CpEntityMapUtils.toMap(entity));

        return oldFormatEntity;
    }

    @Override
    public Entity get( EntityRef entityRef ) throws Exception {

        if ( entityRef == null ) {
            return null;
        }

        Id id = new SimpleId( entityRef.getUuid(), entityRef.getType() );

        org.apache.usergrid.persistence.model.entity.Entity cpEntity = load( id );

        if ( cpEntity == null ) {
            if ( logger.isDebugEnabled() ) {
                logger.debug( "FAILED to load entity {}:{} from app {}",
                    new Object[] {
                            id.getType(), id.getUuid(), applicationId
                    } );
            }
            return null;
        }

        Class clazz = Schema.getDefaultSchema().getEntityClass( entityRef.getType() );

        Entity entity = EntityFactory.newEntity( entityRef.getUuid(), entityRef.getType(), clazz );
        entity.setProperties(  cpEntity  );
        return entity;
    }


    @Override
    public <A extends Entity> A get( UUID entityId, Class<A> entityClass ) throws Exception {
        A e = null;
        try {
            e = ( A ) getEntity( entityId, ( Class<Entity> ) entityClass );
        }
        catch ( ClassCastException e1 ) {
            logger.error( "Unable to get typed entity: {} of class {}",
                    new Object[] { entityId, entityClass.getCanonicalName(), e1 } );
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

        String type = Schema.getDefaultSchema().getEntityType( entityClass );

        Id id = new SimpleId( entityId, type );



        //        if ( !UUIDUtils.isTimeBased( id.getUuid() ) ) {
        //            throw new IllegalArgumentException(
        //                "Entity Id " + id.getType() + ":"+ id.getUuid() +" uuid not time based");
        //        }

        org.apache.usergrid.persistence.model.entity.Entity cpEntity = load( id  );

        if ( cpEntity == null ) {
            if ( logger.isDebugEnabled() ) {
                logger.debug( "FAILED to load entity {}:{} from  app {}\n",
                        new Object[] {
                                id.getType(), id.getUuid(), applicationId
                        } );
            }
            return null;
        }

        A entity = EntityFactory.newEntity( entityId, type, entityClass );
        entity.setProperties(  cpEntity  );

        return entity;
    }


    @Override
    public Results get( Collection<UUID> entityIds, Class<? extends Entity> entityClass, Level resultsLevel )
            throws Exception {

        String type = Schema.getDefaultSchema().getEntityType( entityClass );

        ArrayList<Entity> entities = new ArrayList<Entity>();

        for ( UUID uuid : entityIds ) {
            EntityRef ref = new SimpleEntityRef( type, uuid );
            Entity entity = get( ref, entityClass );

            if ( entity != null ) {
                entities.add( entity );
            }
        }

        return Results.fromEntities( entities );
    }


    @Override
    public Results get( Collection<UUID> entityIds, String entityType, Class<? extends Entity> entityClass,
                        Level resultsLevel ) throws Exception {
        throw new UnsupportedOperationException( "Not supported yet." );
    }


    @Override
    public void update( Entity entity ) throws Exception {
        Preconditions.checkNotNull(entity,"entity should never be null");
        String type = entity.getType();
        Preconditions.checkNotNull(type,"entity type should never be null");
        Id appId  = getApplicationScope().getApplication();
        Preconditions.checkNotNull(appId,"app scope should never be null");
        // first, update entity index in its own collection scope

        updateEntityMeter.mark();


        Id entityId = new SimpleId( entity.getUuid(), entity.getType() );

        if ( logger.isDebugEnabled() ) {
            logger.debug( "Updating entity {}:{}  app {}\n",
                new Object[] {
                    entityId.getType(),
                    entityId.getUuid(),
                    appId
                } );
        }

        //        if ( !UUIDUtils.isTimeBased( entityId.getUuid() ) ) {
        //            throw new IllegalArgumentException(
        //                "Entity Id " + entityId.getType() + ":"+ entityId.getUuid() +" uuid not time based");
        //        }

        //        org.apache.usergrid.persistence.model.entity.Entity cpEntity =
        //                ecm.load( entityId ).toBlockingObservable().last();


        org.apache.usergrid.persistence.model.entity.Entity cpEntity =
                new org.apache.usergrid.persistence.model.entity.Entity( entityId );

        cpEntity = CpEntityMapUtils.fromMap( cpEntity, entity.getProperties(), entity.getType(), true );

        try {
            cpEntity = ecm.write( cpEntity ).toBlocking().last();
//            cpEntity = ecm.update( cpEntity ).toBlockingObservable().last();
//
//
//            // need to reload entity so bypass entity cache
//            cpEntity = ecm.load( entityId ).toBlockingObservable().last();

            logger.debug( "Wrote {}:{} version {}", new Object[] {
                    cpEntity.getId().getType(), cpEntity.getId().getUuid(), cpEntity.getVersion()
            } );
        }
        catch ( WriteUniqueVerifyException wuve ) {
            handleWriteUniqueVerifyException( entity, wuve );
        }

        // update in all containing collections and connection indexes

        indexService.queueEntityIndexUpdate( applicationScope, cpEntity );
    }


    /**
     * There are a series of steps that are kicked off by a delete
     * 1. Mark the entity in the entity collection manager as deleted
     * 2. Mark entity as deleted in the graph
     * 3. Kick off async process
     * 4. Delete all entity documents out of elasticsearch.
     * 5. Compact Graph so that it deletes the marked values.
     * 6. Delete entity from cassandra using the map manager.
     *
     * @param entityRef an entity reference
     *
     * @throws Exception
     */
    @Override
    public void delete( EntityRef entityRef ) throws Exception {
        //TODO: since we want the user to mark it and we sweep it later. It should be marked by the graph manager here.
        //Step 1 & 2 Currently to block so we ensure that marking is done immediately
        //If this returns null then nothing was marked null so the entity doesn't exist
        markEntity( entityRef ).toBlocking().lastOrDefault( null );

        //TODO: figure out how to return async call to service tier? Do I not need to?
        //Step 3
        deleteAsync( entityRef );

    }


    /**
     * Marks entity for deletion in entity collection manager and graph.
     * Convert this method to return a list of observables that we can crunch through on return.
     * Returns merged obversable that will mark the edges in the ecm and the graph manager.
     * @param entityRef
     * @return
     */
    private Observable markEntity(EntityRef entityRef){
        if(applicationScope == null || entityRef == null){
            return Observable.empty();
        }
        GraphManager gm =  graphManagerFactory.createEdgeManager( applicationScope );

        EntityCollectionManager ecm = managerCache.getEntityCollectionManager( applicationScope );

        Id entityId = new SimpleId( entityRef.getUuid(), entityRef.getType() );

        //Step 1 & 2 of delete
        return ecm.mark( entityId ).mergeWith( gm.markNode( entityId, createGraphOperationTimestamp() ) );

    }

    /**
    * 4. Delete all entity documents out of elasticsearch.
    * 5. Compact Graph so that it deletes the marked values.
    * 6. Delete entity from cassandra using the map manager.
     **/
    private void deleteAsync( EntityRef entityRef ) throws Exception {

        Id entityId = new SimpleId( entityRef.getUuid(), entityRef.getType() );

        //Step 4 && 5
        indexService.queueEntityDelete( applicationScope, entityId );

        //Step 6
        //delete from our UUID index
        MapManager mm = getMapManagerForTypes();
        mm.delete( entityRef.getUuid().toString() );

    }


    public void decrementEntityCollection( String collection_name ) {

        long cassandraTimestamp = cass.createTimestamp();
        decrementEntityCollection(collection_name, cassandraTimestamp);
    }


    public void decrementEntityCollection( String collection_name, long cassandraTimestamp ) {
        try {
            incrementAggregateCounters( null, null, null, APPLICATION_COLLECTION + collection_name, -ONE_COUNT,
                    cassandraTimestamp );
        }
        catch ( Exception e ) {
            logger.error( "Unable to decrement counter application.collection: {}.",
                    new Object[] { collection_name, e } );
        }
        try {
            incrementAggregateCounters( null, null, null, APPLICATION_ENTITIES, -ONE_COUNT, cassandraTimestamp );
        }
        catch ( Exception e ) {
            logger.error( "Unable to decrement counter application.entities for collection: {} " + "with timestamp: {}",
                    new Object[] { collection_name, cassandraTimestamp, e } );
        }
    }


    @Override
    public Results searchCollection( EntityRef entityRef, String collectionName, Query query ) throws Exception {

        return getRelationManager( entityRef ).searchCollection( collectionName, query );
    }

    @Override
    public Results searchCollectionConsistent( EntityRef entityRef, String collectionName, Query query, int expectedResults) throws Exception {

        return getRelationManager( entityRef ).searchCollectionConsistent(collectionName, query, expectedResults);
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
    public void updateApplication( Application app ) throws Exception {
        update( app );
        this.application = app;
    }


    @Override
    public void updateApplication( Map<String, Object> properties ) throws Exception {
        this.updateProperties(new SimpleEntityRef(Application.ENTITY_TYPE, applicationId), properties);
        this.application = get( applicationId, Application.class );
    }


    @Override
    public RelationManager getRelationManager( EntityRef entityRef ) {
        Preconditions.checkNotNull(entityRef, "entityRef cannot be null");

        CpRelationManager relationManager =
            new CpRelationManager(managerCache, indexService, collectionService, connectionService, this, entityManagerFig, applicationId, entityRef );
        return relationManager;
    }


    @Override
    public Set<String> getApplicationCollections() throws Exception {

        return getRelationManager( getApplication() ).getCollections();
    }


    @Override
    public Map<String, Object> getApplicationCollectionMetadata() throws Exception {
        Set<String> collections = getApplicationCollections();
        Map<String, Long> counts = getApplicationCounters();
        Map<String, Object> metadata = new HashMap<String, Object>();
        if ( collections != null ) {
            for ( String collectionCode : collections ) {

                String collectionName = collectionCode.split( "\\|" )[0];

                if ( !Schema.isAssociatedEntityType( collectionName ) ) {
                    Long count = counts.get( APPLICATION_COLLECTION + collectionName );
                    Map<String, Object> entry = new HashMap<String, Object>();
                    entry.put( "count", count != null ? count : 0 );
                    entry.put( "type", singularize( collectionName ) );
                    entry.put( "name", collectionName );
                    entry.put( "title", capitalize( collectionName ) );
                    metadata.put( collectionName, entry );
                }
            }
        }
        /*
		 * if ((counts != null) && !counts.isEmpty()) { metadata.put("counters",
		 * counts); }
		 */
        return metadata;
    }


    @Override
    public long getApplicationCollectionSize( String collectionName ) throws Exception {
        throw new UnsupportedOperationException( "Not supported yet." );
    }


    @Override
    public void createApplicationCollection( String entityType ) throws Exception {
        create( entityType, null );
    }

    @Override
    public Entity getUniqueEntityFromAlias( String collectionType, String aliasType ){
        String collName = Schema.defaultCollectionName( collectionType );


        String propertyName = Schema.getDefaultSchema().aliasProperty( collName );

        Timer.Context repairedEntityGet = entGetRepairedEntityTimer.time();

        //TODO: can't we just sub in the getEntityRepair method here so for every read of a uniqueEntityField we can verify it is correct?

        StringField uniqueLookupRepairField =  new StringField( propertyName, aliasType.toString());

        Observable<FieldSet> fieldSetObservable = ecm.getEntitiesFromFields(
            Inflector.getInstance().singularize( collectionType ), Arrays.<Field>asList( uniqueLookupRepairField ) );

        if(fieldSetObservable == null){
            logger.debug( "Couldn't return the observable based on unique entities." );
            return null;
        }
        FieldSet fieldSet = fieldSetObservable.toBlocking().last();

        repairedEntityGet.stop();
        if(fieldSet.isEmpty()) {
            return null;
        }

        return convertMvccEntityToEntity( fieldSet.getEntity( uniqueLookupRepairField ).getEntity().get() );
    }




    @Override
    public EntityRef getAlias( String aliasType, String alias ) throws Exception {

        return getAlias(new SimpleEntityRef(Application.ENTITY_TYPE, applicationId), aliasType, alias);
    }


    @Override
    public EntityRef getAlias( EntityRef ownerRef, String collectionType, String aliasValue ) throws Exception {

        Assert.notNull( ownerRef, "ownerRef is required" );
        Assert.notNull( collectionType, "collectionType is required" );
        Assert.notNull( aliasValue, "aliasValue is required" );

        logger.debug( "getAlias() for collection type {} alias {}", collectionType, aliasValue );

        String collName = Schema.defaultCollectionName( collectionType );

        Map<String, EntityRef> results = getAlias( ownerRef, collName, Collections.singletonList( aliasValue ) );

        if ( results == null || results.size() == 0 ) {
            return null;
        }

        // add a warn statement so we can see if we have data migration issues.
        // TODO When we get an event system, trigger a repair if this is detected
        if ( results.size() > 1 ) {
            logger.warn( "More than 1 entity with Owner id '{}' of type '{}' "
                            + "and alias '{}' exists. This is a duplicate alias, and needs audited",
                    new Object[] { ownerRef, collectionType, aliasValue } );
        }

        return results.get(aliasValue);
    }


    @Override
    public Map<String, EntityRef> getAlias( String aliasType, List<String> aliases ) throws Exception {

        String collName = Schema.defaultCollectionName( aliasType );

        return getAlias(new SimpleEntityRef(Application.ENTITY_TYPE, applicationId), collName, aliases);
    }


    @Override
    public Map<String, EntityRef> getAlias( EntityRef ownerRef, String collName, List<String> aliases )
            throws Exception {

        logger.debug( "getAliases() for collection {} aliases {}", collName, aliases );

        Assert.notNull( ownerRef, "ownerRef is required" );
        Assert.notNull( collName, "collectionName is required" );
        Assert.notEmpty( aliases, "aliases are required" );

        String propertyName = Schema.getDefaultSchema().aliasProperty( collName );

        Map<String, EntityRef> results = new HashMap<>();

        for ( String alias : aliases ) {

            Iterable<EntityRef> refs = getEntityRefsForUniqueProperty( collName, propertyName, alias );

            for ( EntityRef ref : refs ) {
                results.put( alias, ref );
            }
        }

        return results;
    }


    private Iterable<EntityRef> getEntityRefsForUniqueProperty(
            String collName, String propName, String alias ) throws Exception {

        final Id id = getIdForUniqueEntityField( collName, propName, alias );

        if ( id == null ) {
            return Collections.emptyList();
        }


        return Collections.<EntityRef>singleton( new SimpleEntityRef( id.getType(), id.getUuid() ) );
    }


    @Override
    public EntityRef validate( EntityRef entityRef ) throws Exception {
        return validate(entityRef, true);
    }


    public EntityRef validate( EntityRef entityRef, boolean verify ) throws Exception {

        if ( ( entityRef == null ) || ( entityRef.getUuid() == null ) ) {
            return null;
        }

        if ( ( entityRef.getType() == null ) || verify ) {
            UUID entityId = entityRef.getUuid();
            String entityType = entityRef.getType();
            try {
                get( entityRef ).getType();
            }
            catch ( Exception e ) {
                logger.error( "Unable to load entity " + entityRef.getType()
                        + ":" + entityRef.getUuid(), e );
            }
            if ( entityRef == null ) {
                throw new EntityNotFoundException(
                        "Entity " + entityId.toString() + " cannot be verified" );
            }
            if ( ( entityType != null ) && !entityType.equalsIgnoreCase( entityRef.getType() ) ) {
                throw new UnexpectedEntityTypeException(
                        "Entity " + entityId + " is not the expected type, expected "
                                + entityType + ", found " + entityRef.getType() );
            }
        }
        return entityRef;
    }


    @Override
    public Object getProperty( EntityRef entityRef, String propertyName ) throws Exception {

        Entity entity = get( entityRef );
        return entity.getProperty( propertyName );
    }


    @Override
    public List<Entity> getPartialEntities(
            Collection<UUID> ids, Collection<String> properties ) throws Exception {
        throw new UnsupportedOperationException( "Not supported yet." );
    }


    @Override
    public Map<String, Object> getProperties( EntityRef entityRef ) throws Exception {

        Entity entity = get( entityRef );
        return entity.getProperties();
    }


    @Override
    public void setProperty(
            EntityRef entityRef, String propertyName, Object propertyValue ) throws Exception {

        setProperty( entityRef, propertyName, propertyValue, false );
    }


    @Override
    public void setProperty( EntityRef entityRef, String propertyName, Object propertyValue,
            boolean override ) throws Exception {

        if ( ( propertyValue instanceof String ) && ( ( String ) propertyValue ).equals( "" ) ) {
            propertyValue = null;
        }

        Entity entity = get( entityRef );

        propertyValue = Schema.getDefaultSchema().validateEntityPropertyValue(
                entity.getType(), propertyName, propertyValue );

        entity.setProperty( propertyName, propertyValue );
        entity.setProperty( PROPERTY_MODIFIED, UUIDUtils.getTimestampInMillis( UUIDUtils.newTimeUUID() ) );

        update(entity);
    }


    @Override
    public void updateProperties( EntityRef ref, Map<String, Object> properties ) throws Exception {

        ref = validate( ref );
        properties = Schema.getDefaultSchema().cleanUpdatedProperties( ref.getType(), properties, false );

        EntityRef entityRef = ref;
        if ( entityRef instanceof CollectionRef ) {
            CollectionRef cref = ( CollectionRef ) ref;
            entityRef = cref.getItemRef();
        }

        Entity entity = get( entityRef );

        properties.put(PROPERTY_MODIFIED, UUIDUtils.getTimestampInMillis(UUIDUtils.newTimeUUID()));

        for ( String propertyName : properties.keySet() ) {
            Object propertyValue = properties.get( propertyName );

            Schema defaultSchema = Schema.getDefaultSchema();

            boolean entitySchemaHasProperty = defaultSchema.hasProperty( entity.getType(), propertyName );

            propertyValue = Schema.getDefaultSchema()
                                  .validateEntityPropertyValue( entity.getType(), propertyName, propertyValue );

            if ( entitySchemaHasProperty ) {

                if ( !defaultSchema.isPropertyMutable( entity.getType(), propertyName ) ) {
                    continue;
                }

                if ( ( propertyValue == null ) && defaultSchema.isRequiredProperty( entity.getType(), propertyName ) ) {
                    continue;
                }
            }

            entity.setProperty( propertyName, propertyValue );
        }

        update( entity );
    }


    @Override
    public void deleteProperty( EntityRef entityRef, String propertyName ) throws Exception {

        Id entityId = new SimpleId( entityRef.getUuid(), entityRef.getType() );

        //        if ( !UUIDUtils.isTimeBased( entityId.getUuid() ) ) {
        //            throw new IllegalArgumentException(
        //                "Entity Id " + entityId.getType() + ":"+entityId.getUuid() +" uuid not time based");
        //        }

        org.apache.usergrid.persistence.model.entity.Entity cpEntity =
                load( entityId );

        cpEntity.removeField( propertyName );

        logger.debug( "About to Write {}:{} version {}", new Object[] {
                cpEntity.getId().getType(), cpEntity.getId().getUuid(), cpEntity.getVersion()
        } );

        //TODO: does this call and others like it need a graphite reporter?
        cpEntity = ecm.write( cpEntity ).toBlocking().last();

        logger.debug("Wrote {}:{} version {}", new Object[]{
            cpEntity.getId().getType(), cpEntity.getId().getUuid(), cpEntity.getVersion()
        });

        //Adding graphite metrics

        indexService.queueEntityIndexUpdate(applicationScope, cpEntity);
    }


    @Override
    public Set<Object> getDictionaryAsSet( EntityRef entityRef, String dictionaryName ) throws Exception {

        return new LinkedHashSet<>( getDictionaryAsMap( entityRef, dictionaryName ).keySet() );
    }


    @Override
    public void addToDictionary( EntityRef entityRef, String dictionaryName,
            Object elementValue ) throws Exception {

        addToDictionary( entityRef, dictionaryName, elementValue, null );
    }


    @Override
    public void addToDictionary( EntityRef entityRef, String dictionaryName, Object elementName,
            Object elementValue ) throws Exception {

        if ( elementName == null ) {
            return;
        }

        EntityRef entity = get( entityRef );

        UUID timestampUuid = UUIDUtils.newTimeUUID();
        Mutator<ByteBuffer> batch = createMutator( cass.getApplicationKeyspace( applicationId ), be );

        batch = batchUpdateDictionary( batch, entity, dictionaryName, elementName, elementValue, false, timestampUuid );

        //Adding graphite metrics
        Timer.Context timeDictionaryCreation = entAddDictionaryTimer.time();
        CassandraPersistenceUtils.batchExecute( batch, CassandraService.RETRY_COUNT );
        timeDictionaryCreation.stop();
    }


    @Override
    public void addSetToDictionary( EntityRef entityRef, String dictionaryName, Set<?> elementValues )
            throws Exception {

        if ( ( elementValues == null ) || elementValues.isEmpty() ) {
            return;
        }

        EntityRef entity = get( entityRef );

        UUID timestampUuid = UUIDUtils.newTimeUUID();
        Mutator<ByteBuffer> batch = createMutator( cass.getApplicationKeyspace( applicationId ), be );

        for ( Object elementValue : elementValues ) {
            batch = batchUpdateDictionary( batch, entity, dictionaryName, elementValue, null, false, timestampUuid );
        }

        //Adding graphite metrics
        Timer.Context timeAddingSetDictionary = entAddDictionarySetTimer.time();
        CassandraPersistenceUtils.batchExecute( batch, CassandraService.RETRY_COUNT );
        timeAddingSetDictionary.stop();
    }


    @Override
    public void addMapToDictionary( EntityRef entityRef, String dictionaryName, Map<?, ?> elementValues )
            throws Exception {

        if ( ( elementValues == null ) || elementValues.isEmpty() || entityRef == null ) {
            return;
        }

        EntityRef entity = get( entityRef );

        UUID timestampUuid = UUIDUtils.newTimeUUID();
        Mutator<ByteBuffer> batch = createMutator( cass.getApplicationKeyspace( applicationId ), be );

        for ( Map.Entry<?, ?> elementValue : elementValues.entrySet() ) {
            batch = batchUpdateDictionary( batch, entity, dictionaryName, elementValue.getKey(),
                    elementValue.getValue(), false, timestampUuid );
        }

        //Adding graphite metrics
        Timer.Context timeMapDictionary = entAddDictionaryMapTimer.time();
        CassandraPersistenceUtils.batchExecute( batch, CassandraService.RETRY_COUNT );
        timeMapDictionary.stop();
    }


    @Override
    public Map<Object, Object> getDictionaryAsMap( EntityRef entity, String dictionaryName ) throws Exception {

        entity = validate( entity );

        Map<Object, Object> dictionary = new LinkedHashMap<Object, Object>();

        ApplicationCF dictionaryCf = null;

        boolean entityHasDictionary = Schema.getDefaultSchema().hasDictionary(entity.getType(), dictionaryName);

        if ( entityHasDictionary ) {
            dictionaryCf = ENTITY_DICTIONARIES;
        }
        else {
            dictionaryCf = ENTITY_COMPOSITE_DICTIONARIES;
        }

        Class<?> setType = Schema.getDefaultSchema().getDictionaryKeyType(entity.getType(), dictionaryName);
        Class<?> setCoType = Schema.getDefaultSchema().getDictionaryValueType(entity.getType(), dictionaryName);
        boolean coTypeIsBasic = ClassUtils.isBasicType( setCoType );

        List<HColumn<ByteBuffer, ByteBuffer>> results =
                cass.getAllColumns(cass.getApplicationKeyspace(applicationId), dictionaryCf,
                    CassandraPersistenceUtils.key(entity.getUuid(), dictionaryName), be, be);
        for ( HColumn<ByteBuffer, ByteBuffer> result : results ) {
            Object name = null;
            if ( entityHasDictionary ) {
                name = object( setType, result.getName() );
            }
            else {
                name = CompositeUtils.deserialize( result.getName() );
            }
            Object value = null;
            if ( entityHasDictionary && coTypeIsBasic ) {
                value = object( setCoType, result.getValue() );
            }
            else if ( result.getValue().remaining() > 0 ) {
                value = Schema.deserializePropertyValueFromJsonBinary( result.getValue().slice(), setCoType );
            }
            if ( name != null ) {
                dictionary.put( name, value );
            }
        }

        return dictionary;
    }


    @Override
    public Object getDictionaryElementValue( EntityRef entity, String dictionaryName, String elementName )
            throws Exception {

        if ( entity == null ) {
            throw new RuntimeException( "Entity is null" );
        }

        if ( dictionaryName == null ) {
            throw new RuntimeException( "dictionaryName is null" );
        }

        if ( elementName == null ) {
            throw new RuntimeException( "elementName is null" );
        }

        if ( Schema.getDefaultSchema() == null ) {
            throw new RuntimeException( "Schema.getDefaultSchema() is null" );
        }

        Object value = null;

        ApplicationCF dictionaryCf = null;

        boolean entityHasDictionary = Schema.getDefaultSchema().hasDictionary(entity.getType(), dictionaryName);

        if ( entityHasDictionary ) {
            dictionaryCf = ENTITY_DICTIONARIES;
        }
        else {
            dictionaryCf = ENTITY_COMPOSITE_DICTIONARIES;
        }

        Class<?> dictionaryCoType =
                Schema.getDefaultSchema().getDictionaryValueType( entity.getType(), dictionaryName );
        boolean coTypeIsBasic = ClassUtils.isBasicType( dictionaryCoType );

        HColumn<ByteBuffer, ByteBuffer> result =
                cass.getColumn(cass.getApplicationKeyspace(applicationId), dictionaryCf,
                    CassandraPersistenceUtils.key(entity.getUuid(), dictionaryName),
                    entityHasDictionary ? bytebuffer(elementName) : DynamicComposite.toByteBuffer(elementName), be,
                    be);

        if ( result != null ) {
            if ( entityHasDictionary && coTypeIsBasic ) {
                value = object( dictionaryCoType, result.getValue() );
            }
            else if ( result.getValue().remaining() > 0 ) {
                value = Schema.deserializePropertyValueFromJsonBinary( result.getValue().slice(), dictionaryCoType );
            }
        }
        else {
            logger.info( "Results of CpEntityManagerImpl.getDictionaryElementValue is null" );
        }

        return value;
    }


    public Map<String, Object> getDictionaryElementValues( EntityRef entity, String dictionaryName,
                                                           String... elementNames ) throws Exception {

        Map<String, Object> values = null;

        ApplicationCF dictionaryCf = null;

        boolean entityHasDictionary = Schema.getDefaultSchema().hasDictionary( entity.getType(), dictionaryName );

        if ( entityHasDictionary ) {
            dictionaryCf = ENTITY_DICTIONARIES;
        }
        else {
            dictionaryCf = ENTITY_COMPOSITE_DICTIONARIES;
        }

        Class<?> dictionaryCoType =
                Schema.getDefaultSchema().getDictionaryValueType( entity.getType(), dictionaryName );
        boolean coTypeIsBasic = ClassUtils.isBasicType(dictionaryCoType);

        ByteBuffer[] columnNames = new ByteBuffer[elementNames.length];
        for ( int i = 0; i < elementNames.length; i++ ) {
            columnNames[i] = entityHasDictionary ? bytebuffer( elementNames[i] ) :
                             DynamicComposite.toByteBuffer( elementNames[i] );
        }

        ColumnSlice<ByteBuffer, ByteBuffer> results =
                cass.getColumns(cass.getApplicationKeyspace(applicationId), dictionaryCf,
                    CassandraPersistenceUtils.key(entity.getUuid(), dictionaryName), columnNames, be, be);
        if ( results != null ) {
            values = new HashMap<String, Object>();
            for ( HColumn<ByteBuffer, ByteBuffer> result : results.getColumns() ) {
                String name = entityHasDictionary ? string( result.getName() ) :
                              DynamicComposite.fromByteBuffer( result.getName() ).get( 0, se );
                if ( entityHasDictionary && coTypeIsBasic ) {
                    values.put( name, object( dictionaryCoType, result.getValue() ) );
                }
                else if ( result.getValue().remaining() > 0 ) {
                    values.put( name, Schema.deserializePropertyValueFromJsonBinary( result.getValue().slice(),
                            dictionaryCoType ) );
                }
            }
        }
        else {
            logger.error("Results of CpEntityManagerImpl.getDictionaryElementValues is null");
        }

        return values;
    }


    @Override
    public void removeFromDictionary( EntityRef entityRef, String dictionaryName, Object elementName )
            throws Exception {
        if ( elementName == null ) {
            return;
        }

        EntityRef entity = get(entityRef);

        UUID timestampUuid = UUIDUtils.newTimeUUID();
        Mutator<ByteBuffer> batch = createMutator( cass.getApplicationKeyspace( applicationId ), be );

        batch = batchUpdateDictionary( batch, entity, dictionaryName, elementName, true, timestampUuid );
        //Adding graphite metrics
        Timer.Context timeRemoveDictionary = entRemoveDictionaryTimer.time();
        CassandraPersistenceUtils.batchExecute( batch, CassandraService.RETRY_COUNT );
        timeRemoveDictionary.stop();

    }


    @Override
    public Set<String> getDictionaries( EntityRef entity ) throws Exception {
        return getDictionaryNames( entity );
    }


    @Override
    public Map<String, Map<UUID, Set<String>>> getOwners( EntityRef entityRef ) throws Exception {

        return getRelationManager( entityRef ).getOwners();
    }


    @Override
    public boolean isCollectionMember( EntityRef owner, String collectionName, EntityRef entity ) throws Exception {

        return getRelationManager( owner ).isCollectionMember(collectionName, entity);
    }


    @Override
    public boolean isConnectionMember( EntityRef owner, String connectionName, EntityRef entity ) throws Exception {

        return getRelationManager( owner ).isConnectionMember(connectionName, entity);
    }


    @Override
    public Set<String> getCollections( EntityRef entityRef ) throws Exception {

        return getRelationManager( entityRef ).getCollections();
    }


    @Override
    public Results getCollection( EntityRef entityRef, String collectionName, UUID startResult, int count,
                                  Level resultsLevel, boolean reversed ) throws Exception {

        return getRelationManager( entityRef )
                .getCollection(collectionName, startResult, count, resultsLevel, reversed);
    }


    @Override
    public Results getCollection( UUID entityId, String collectionName, Query query, Level resultsLevel )
            throws Exception {

        return getRelationManager( get( entityId ))
                .getCollection( collectionName, query, resultsLevel );
    }


    @Override
    public Entity addToCollection( EntityRef entityRef, String collectionName, EntityRef itemRef ) throws Exception {

        return getRelationManager( entityRef ).addToCollection(collectionName, itemRef);
    }


    @Override
    public Entity addToCollections( List<EntityRef> ownerEntities, String collectionName, EntityRef itemRef )
            throws Exception {

        // don't fetch entity if we've already got one
        final Entity entity;
        if ( itemRef instanceof Entity ) {
            entity = ( Entity ) itemRef;
        }
        else {
            entity = get( itemRef );
        }

        for ( EntityRef eref : ownerEntities ) {
            addToCollection( eref, collectionName, entity );
        }

        return entity;
    }


    @Override
    public Entity createItemInCollection( EntityRef entityRef, String collectionName,
            String itemType, Map<String, Object> props ) throws Exception {

        return getRelationManager( entityRef ).createItemInCollection( collectionName, itemType, props );
    }


    @Override
    public void removeFromCollection( EntityRef entityRef, String collectionName, EntityRef itemRef ) throws Exception {

        getRelationManager( entityRef ).removeFromCollection( collectionName, itemRef );
    }


    @Override
    public Set<String> getCollectionIndexes( EntityRef entity, String collectionName ) throws Exception {

        return getRelationManager( entity ).getCollectionIndexes( collectionName );
    }


    @Override
    public void copyRelationships( EntityRef srcEntityRef, String srcRelationName, EntityRef dstEntityRef,
                                   String dstRelationName ) throws Exception {

        getRelationManager( srcEntityRef ).copyRelationships( srcRelationName, dstEntityRef, dstRelationName );
    }


    @Override
    public ConnectionRef createConnection( ConnectionRef connection ) throws Exception {

        return createConnection( connection.getSourceRefs(), connection.getConnectionType(),
            connection.getTargetRefs() );
    }


    @Override
    public ConnectionRef createConnection( EntityRef connectingEntity, String connectionType,
                                           EntityRef connectedEntityRef ) throws Exception {

        return getRelationManager( connectingEntity ).createConnection(connectionType, connectedEntityRef);
    }


    @Override
    public ConnectionRef createConnection( EntityRef connectingEntity, String pairedConnectionType,
            EntityRef pairedEntity, String connectionType, EntityRef connectedEntityRef )
            throws Exception {

        return getRelationManager( connectingEntity )
                .createConnection(pairedConnectionType, pairedEntity, connectionType, connectedEntityRef);
    }


    @Override
    public ConnectionRef createConnection( EntityRef connectingEntity, ConnectedEntityRef... connections )
            throws Exception {

        return getRelationManager( connectingEntity ).connectionRef(connections);
    }


    @Override
    public ConnectionRef connectionRef( EntityRef connectingEntity, String connectionType,
                                        EntityRef connectedEntityRef ) throws Exception {

        return new ConnectionRefImpl( connectingEntity.getType(), connectingEntity.getUuid(), connectionType,
                connectedEntityRef.getType(), connectedEntityRef.getUuid() );
    }


    @Override
    public ConnectionRef connectionRef( EntityRef connectingEntity, String pairedConnectionType,
            EntityRef pairedEntity, String connectionType, EntityRef connectedEntityRef ) throws Exception {

        return getRelationManager( connectingEntity )
                .connectionRef( pairedConnectionType, pairedEntity, connectionType, connectedEntityRef );
    }


    @Override
    public ConnectionRef connectionRef( EntityRef connectingEntity, ConnectedEntityRef... connections ) {

        return getRelationManager( connectingEntity ).connectionRef(connections);
    }


    @Override
    public void deleteConnection( ConnectionRef connectionRef ) throws Exception {

        EntityRef sourceEntity = connectionRef.getTargetRefs();

        getRelationManager( sourceEntity ).deleteConnection( connectionRef );
    }


    @Override
    public Set<String> getConnectionTypes( EntityRef ref ) throws Exception {

        return getRelationManager( ref ).getConnectionTypes();
    }


    @Override
    public Results getTargetEntities(EntityRef entityRef, String connectionType,
                                     String connectedEntityType, Level resultsLevel) throws Exception {

        return getRelationManager( entityRef )
                .getTargetEntities(connectionType, connectedEntityType, resultsLevel);
    }


    @Override
    public Results getSourceEntities(EntityRef entityRef, String connectionType,
                                     String connectedEntityType, Level resultsLevel) throws Exception {

        return getRelationManager( entityRef )
                .getSourceEntities(connectionType, connectedEntityType, resultsLevel);
    }


    @Override
    public Results getSourceEntities(EntityRef entityRef, String connectionType,
                                     String entityType, Level level, int count) throws Exception {

        return getRelationManager( entityRef ).getSourceEntities(connectionType, entityType, level, count);
    }


    @Override
    public Results searchTargetEntities(EntityRef connectingEntity, Query query) throws Exception {

        return getRelationManager( connectingEntity ).searchTargetEntities(query);
    }


    @Override
    public Set<String> getConnectionIndexes( EntityRef entity, String connectionType ) throws Exception {

        return getRelationManager( entity ).getConnectionIndexes(connectionType);
    }


    @Override
    public Map<String, String> getRoles() throws Exception {
        return cast(getDictionaryAsMap(getApplicationRef(), DICTIONARY_ROLENAMES));
    }


    @Override
    public void resetRoles() throws Exception {
        try {
            createRole( "admin", "Administrator", 0 );
        }
        catch ( DuplicateUniquePropertyExistsException dupe ) {
            logger.warn( "Role admin already exists " );
        }
        catch ( Exception e ) {
            logger.error( "Could not create admin role, may already exist", e );
        }

        try {
            createRole( "default", "Default", 0 );
        }
        catch ( DuplicateUniquePropertyExistsException dupe ) {
            logger.warn( "Role default already exists " );
        }
        catch ( Exception e ) {
            logger.error( "Could not create default role, may already exist", e );
        }

        try {
            createRole( "guest", "Guest", 0 );
        }
        catch ( DuplicateUniquePropertyExistsException dupe ) {
            logger.warn( "Role guest already exists " );
        }
        catch ( Exception e ) {
            logger.error( "Could not create guest role, may already exist", e );
        }

        try {
            grantRolePermissions( "default", Arrays.asList( "get,put,post,delete:/**" ) );
        }
        catch ( DuplicateUniquePropertyExistsException dupe ) {
            logger.warn( "Role default already has permission" );
        }
        catch ( Exception e ) {
            logger.error( "Could not populate default role", e );
        }

        try {
            grantRolePermissions( "guest", Arrays.asList( "post:/users", "post:/devices", "put:/devices/*" ) );
        }
        catch ( DuplicateUniquePropertyExistsException dupe ) {
            logger.warn( "Role guest already has permission" );
        }
        catch ( Exception e ) {
            logger.error( "Could not populate guest role", e );
        }
    }


    @Override
    public Entity createRole( String roleName, String roleTitle, long inactivity ) throws Exception {

        if ( roleName == null || roleName.isEmpty() ) {
            throw new RequiredPropertyNotFoundException( "role", roleTitle );
        }

        String propertyName = roleName;
        UUID ownerId = applicationId;
        String batchRoleName = StringUtils.stringOrSubstringAfterLast( roleName.toLowerCase(), ':' );
        return batchCreateRole( batchRoleName, roleTitle, inactivity, propertyName, ownerId, null );
    }


    private Entity batchCreateRole( String roleName, String roleTitle, long inactivity,
            String propertyName, UUID ownerId, Map<String, Object> additionalProperties ) throws Exception {

        UUID timestampUuid = UUIDUtils.newTimeUUID();
        long timestamp = UUIDUtils.getUUIDLong( timestampUuid );

        Map<String, Object> properties = new TreeMap<>( CASE_INSENSITIVE_ORDER );
        properties.put( PROPERTY_TYPE, Role.ENTITY_TYPE );
        properties.put( PROPERTY_NAME, propertyName );
        properties.put( "roleName", roleName );
        properties.put( "title", roleTitle );
        properties.put( PROPERTY_INACTIVITY, inactivity );
        if ( additionalProperties != null ) {
            for ( String key : additionalProperties.keySet() ) {
                properties.put( key, additionalProperties.get( key ) );
            }
        }

        UUID id = UUIDGenerator.newTimeUUID();
        batchCreate( null, Role.ENTITY_TYPE, null, properties, id, timestampUuid );

        Mutator<ByteBuffer> batch = createMutator( cass.getApplicationKeyspace( applicationId ), be );
        CassandraPersistenceUtils.addInsertToMutator( batch, ENTITY_DICTIONARIES,
                CassandraPersistenceUtils.key( ownerId,
                        Schema.DICTIONARY_ROLENAMES ), roleName, roleTitle, timestamp );
        CassandraPersistenceUtils.addInsertToMutator( batch, ENTITY_DICTIONARIES,
                CassandraPersistenceUtils.key( ownerId,
                        Schema.DICTIONARY_ROLETIMES ), roleName, inactivity,
                timestamp );
        CassandraPersistenceUtils.addInsertToMutator( batch, ENTITY_DICTIONARIES,
                CassandraPersistenceUtils.key( ownerId, DICTIONARY_SETS ), Schema.DICTIONARY_ROLENAMES, null,
                timestamp );
        //Adding graphite metrics
        Timer.Context timeCreateBatchRole= entCreateRoleTimer.time();
        CassandraPersistenceUtils.batchExecute( batch, CassandraService.RETRY_COUNT );
        timeCreateBatchRole.stop();

        return get( id, Role.class );
    }


    @Override
    public void grantRolePermission( String roleName, String permission ) throws Exception {
        roleName = roleName.toLowerCase();
        permission = permission.toLowerCase();
        long timestamp = cass.createTimestamp();
        Mutator<ByteBuffer> batch = createMutator( cass.getApplicationKeyspace( applicationId ), be );
        CassandraPersistenceUtils.addInsertToMutator( batch, ApplicationCF.ENTITY_DICTIONARIES,
            getRolePermissionsKey( roleName ), permission, ByteBuffer.allocate( 0 ), timestamp );
        //Adding graphite metrics
        Timer.Context timeGrantRolePermission = this.metricsFactory.getTimer(CpEntityManager.class,
            "role.create_permission").time();
        CassandraPersistenceUtils.batchExecute( batch, CassandraService.RETRY_COUNT );
        timeGrantRolePermission.stop();
    }


    @Override
    public void grantRolePermissions( String roleName, Collection<String> permissions ) throws Exception {

        roleName = roleName.toLowerCase();
        long timestamp = cass.createTimestamp();
        Mutator<ByteBuffer> batch = createMutator( cass.getApplicationKeyspace( applicationId ), be );
        for ( String permission : permissions ) {
            permission = permission.toLowerCase();
            CassandraPersistenceUtils.addInsertToMutator( batch, ApplicationCF.ENTITY_DICTIONARIES,
                getRolePermissionsKey( roleName ), permission, ByteBuffer.allocate( 0 ), timestamp);
        }
        //Adding graphite metrics
        Timer.Context timeGrantRolePermissions = entCreateRolePermissionsTimer.time();
        CassandraPersistenceUtils.batchExecute( batch, CassandraService.RETRY_COUNT );
        timeGrantRolePermissions.stop();

    }


    private Object getRolePermissionsKey( String roleName ) {
        return CassandraPersistenceUtils.key(
                SimpleRoleRef.getIdForRoleName( roleName ), DICTIONARY_PERMISSIONS );
    }


    private Object getRolePermissionsKey( UUID groupId, String roleName ) {
        try {
            return CassandraPersistenceUtils
                    .key( getGroupRoleRef( groupId, roleName ).getUuid(), DICTIONARY_PERMISSIONS );
        }
        catch ( Exception e ) {
            logger.error( "Error creating role key for uuid {} and role {}", groupId, roleName );
            return null;
        }
    }


    @Override
    public void revokeRolePermission( String roleName, String permission ) throws Exception {
        roleName = roleName.toLowerCase();
        permission = permission.toLowerCase();
        long timestamp = cass.createTimestamp();
        Mutator<ByteBuffer> batch = createMutator( cass.getApplicationKeyspace( applicationId ), be);
        CassandraPersistenceUtils.addDeleteToMutator( batch, ApplicationCF.ENTITY_DICTIONARIES,
            getRolePermissionsKey( roleName ), permission, timestamp );
        //Adding graphite metrics
        Timer.Context timeRevokeRolePermission = entRevokeRolePermissionsTimer.time();
        CassandraPersistenceUtils.batchExecute( batch, CassandraService.RETRY_COUNT );
        timeRevokeRolePermission.stop();
    }

//TODO: does this need graphite monitoring
    @Override
    public Set<String> getRolePermissions( String roleName ) throws Exception {
        roleName = roleName.toLowerCase();
        return cass.getAllColumnNames( cass.getApplicationKeyspace( applicationId ), ApplicationCF.ENTITY_DICTIONARIES,
            getRolePermissionsKey( roleName ) );
    }

//TODO: does this need graphite monitoring
    @Override
    public void deleteRole(final String roleName ) throws Exception {
       deleteRole(roleName, Optional.absent());
    }

    @Override
    public void deleteRole(final String roleName, final Optional<EntityRef> roleRef ) throws Exception {
        final String roleNameLowerCase = roleName.toLowerCase();
        Set<String> permissions = getRolePermissions( roleNameLowerCase );
        Iterator<String> itrPermissions = permissions.iterator();

        while ( itrPermissions.hasNext() ) {
            revokeRolePermission( roleNameLowerCase, itrPermissions.next() );
        }

        removeFromDictionary(getApplicationRef(), DICTIONARY_ROLENAMES, roleNameLowerCase);
        removeFromDictionary(getApplicationRef(), DICTIONARY_ROLETIMES, roleNameLowerCase);
        final EntityRef entity = roleRef.isPresent() ? roleRef.get() : getRoleRef( roleNameLowerCase );
        if ( entity != null ) {
            delete( entity );
        }
    }

    @Override
    public Map<String, String> getGroupRoles( UUID groupId ) throws Exception {
        return cast( getDictionaryAsMap( new SimpleEntityRef( Group.ENTITY_TYPE, groupId ), DICTIONARY_ROLENAMES ) );
    }


    @Override
    public Entity createGroupRole( UUID groupId, String roleName, long inactivity ) throws Exception {
        String batchRoleName = StringUtils.stringOrSubstringAfterLast( roleName.toLowerCase(), ':' );
        String roleTitle = batchRoleName;
        String propertyName = groupId + ":" + batchRoleName;
        Map<String, Object> properties = new TreeMap<String, Object>( CASE_INSENSITIVE_ORDER );
        properties.put( "group", groupId );

        Entity entity = batchCreateRole( roleName, roleTitle, inactivity, propertyName, groupId, properties );
        getRelationManager( new SimpleEntityRef( Group.ENTITY_TYPE, groupId ) )
                .addToCollection( COLLECTION_ROLES, entity );

        logger.info( "Created role {} with id {} in group {}",
            new String[] { roleName, entity.getUuid().toString(), groupId.toString() } );

        return entity;
    }


    @Override
    public void grantGroupRolePermission( UUID groupId, String roleName, String permission ) throws Exception {
        roleName = roleName.toLowerCase();
        permission = permission.toLowerCase();
        long timestamp = cass.createTimestamp();
        Mutator<ByteBuffer> batch = createMutator( cass.getApplicationKeyspace( applicationId ), be );
        CassandraPersistenceUtils.addInsertToMutator(batch, ApplicationCF.ENTITY_DICTIONARIES,
            getRolePermissionsKey(groupId, roleName), permission, ByteBuffer.allocate(0), timestamp);

        //Adding graphite metrics
        Timer.Context timeGroupRolePermission = entGrantGroupPermissionTimer.time();
        CassandraPersistenceUtils.batchExecute(batch, CassandraService.RETRY_COUNT);
        timeGroupRolePermission.stop();
    }


    @Override
    public void revokeGroupRolePermission( UUID groupId, String roleName, String permission ) throws Exception {
        roleName = roleName.toLowerCase();
        permission = permission.toLowerCase();
        long timestamp = cass.createTimestamp();
        Mutator<ByteBuffer> batch = createMutator( cass.getApplicationKeyspace( applicationId ), be );
        CassandraPersistenceUtils.addDeleteToMutator(batch, ApplicationCF.ENTITY_DICTIONARIES,
            getRolePermissionsKey(groupId, roleName), permission, timestamp);
        //Adding graphite metrics
        Timer.Context timeRevokeGroupRolePermission = entRevokeGroupPermissionTimer.time();
        CassandraPersistenceUtils.batchExecute( batch, CassandraService.RETRY_COUNT );
        timeRevokeGroupRolePermission.stop();
    }


    @Override
    public Set<String> getGroupRolePermissions( UUID groupId, String roleName ) throws Exception {
        roleName = roleName.toLowerCase();
        return cass.getAllColumnNames( cass.getApplicationKeyspace( applicationId ), ApplicationCF.ENTITY_DICTIONARIES,
            getRolePermissionsKey( groupId, roleName ) );
    }


    @Override
    public void deleteGroupRole( UUID groupId, String roleName ) throws Exception {
        roleName = roleName.toLowerCase();
        removeFromDictionary( new SimpleEntityRef( Group.ENTITY_TYPE, groupId ), DICTIONARY_ROLENAMES, roleName );
        cass.deleteRow( cass.getApplicationKeyspace( applicationId ), ApplicationCF.ENTITY_DICTIONARIES,
            SimpleRoleRef.getIdForGroupIdAndRoleName( groupId, roleName ) );
    }


    @Override
    public Set<String> getUserRoles( UUID userId ) throws Exception {
        return cast( getDictionaryAsSet( userRef( userId ), DICTIONARY_ROLENAMES ) );
    }


    @Override
    public void addUserToRole( UUID userId, String roleName ) throws Exception {
        roleName = roleName.toLowerCase();
        addToDictionary( userRef( userId ), DICTIONARY_ROLENAMES, roleName, roleName );
        addToCollection( userRef( userId ), COLLECTION_ROLES, getRoleRef( roleName ) );
    }


    private EntityRef userRef( UUID userId ) {
        return new SimpleEntityRef( User.ENTITY_TYPE, userId );
    }


    @Override
    public void removeUserFromRole( UUID userId, String roleName ) throws Exception {
        roleName = roleName.toLowerCase();
        removeFromDictionary( userRef( userId ), DICTIONARY_ROLENAMES, roleName );
        removeFromCollection( userRef( userId ), COLLECTION_ROLES, getRoleRef( roleName ) );
    }


    @Override
    public Set<String> getUserPermissions( UUID userId ) throws Exception {
        return cast(getDictionaryAsSet(
                new SimpleEntityRef( User.ENTITY_TYPE, userId ), Schema.DICTIONARY_PERMISSIONS ) );
    }


    @Override
    public void grantUserPermission( UUID userId, String permission ) throws Exception {
        permission = permission.toLowerCase();
        addToDictionary( userRef( userId ), DICTIONARY_PERMISSIONS, permission );
    }


    @Override
    public void revokeUserPermission( UUID userId, String permission ) throws Exception {
        permission = permission.toLowerCase();
        removeFromDictionary(userRef(userId), DICTIONARY_PERMISSIONS, permission);
    }


    @Override
    public Map<String, String> getUserGroupRoles( UUID userId, UUID groupId ) throws Exception {
        // TODO this never returns anything - write path not invoked
        EntityRef userRef = userRef( userId );
        return cast( getDictionaryAsMap( userRef, DICTIONARY_ROLENAMES ) );
    }


    @Override
    public void addUserToGroupRole( UUID userId, UUID groupId, String roleName ) throws Exception {
        roleName = roleName.toLowerCase();
        EntityRef userRef = userRef( userId );
        EntityRef roleRef = getRoleRef( roleName );
        addToDictionary( userRef, DICTIONARY_ROLENAMES, roleName, roleName );
        //adding will add reverse collection in addToCollection         for role-> users
        addToCollection( userRef, COLLECTION_ROLES, roleRef );
    }


    @Override
    public void removeUserFromGroupRole( UUID userId, UUID groupId, String roleName ) throws Exception {
        roleName = roleName.toLowerCase();
        EntityRef memberRef = userRef( userId );
        EntityRef roleRef = getRoleRef( roleName );
        removeFromDictionary( memberRef, DICTIONARY_ROLENAMES, roleName );
        removeFromCollection(memberRef, COLLECTION_ROLES, roleRef);
        removeFromCollection(roleRef, COLLECTION_USERS, userRef(userId));
    }


    @Override
    public Results getUsersInGroupRole( UUID groupId, String roleName, Level level ) throws Exception {
        return this.getCollection( getRoleRef( roleName ), COLLECTION_USERS, null, 10000, level, false );
    }


    @Override
    public EntityRef getGroupRoleRef( UUID groupId, String roleName ) throws Exception {
        Results results = this.searchCollection( new SimpleEntityRef( Group.ENTITY_TYPE, groupId ),
            Schema.defaultCollectionName( Role.ENTITY_TYPE ), Query.fromQL( "roleName = '" + roleName + "'" ) );
        Iterator<Entity> iterator = results.iterator();
        EntityRef roleRef = null;
        while ( iterator.hasNext() ) {
            roleRef = iterator.next();
        }
        return roleRef;
    }


    private EntityRef getRoleRef( String roleName ) throws Exception {
        Results results = this.searchCollection( new SimpleEntityRef( Application.ENTITY_TYPE, applicationId ),
            Schema.defaultCollectionName( Role.ENTITY_TYPE ), Query.fromQL( "roleName = '" + roleName + "'" ) );
        Iterator<Entity> iterator = results.iterator();
        EntityRef roleRef = null;
        while ( iterator.hasNext() ) {
            roleRef = iterator.next();
        }
        return roleRef;
    }


    @Override
    public void incrementAggregateCounters( UUID userId, UUID groupId, String category,
            String counterName, long value ) {

        long cassandraTimestamp = cass.createTimestamp();
        incrementAggregateCounters( userId, groupId, category, counterName, value, cassandraTimestamp );
    }


    private void incrementAggregateCounters( UUID userId, UUID groupId, String category,
            String counterName, long value, long cassandraTimestamp ) {

        // TODO short circuit
        if ( !skipAggregateCounters ) {
            Mutator<ByteBuffer> m = createMutator( cass.getApplicationKeyspace( applicationId ), be );

            counterUtils.batchIncrementAggregateCounters( m, applicationId, userId, groupId, null,
                    category, counterName, value, cassandraTimestamp / 1000, cassandraTimestamp );

            //Adding graphite metrics
            Timer.Context timeIncrementAggregateCounters = entIncrementAggregateCountersTimer.time();
            CassandraPersistenceUtils.batchExecute( m, CassandraService.RETRY_COUNT );
            timeIncrementAggregateCounters.stop();
        }
    }


    @Override
    public Results getAggregateCounters( UUID userId, UUID groupId, String category,
            String counterName, CounterResolution resolution, long start, long finish, boolean pad ) {
        return this.getAggregateCounters(
                userId, groupId, null, category, counterName, resolution, start, finish, pad );
    }


    @Override
    public Results getAggregateCounters( UUID userId, UUID groupId, UUID queueId, String category,
        String counterName, CounterResolution resolution, long start, long finish, boolean pad ) {

        start = resolution.round( start );
        finish = resolution.round( finish );
        long expected_time = start;
        Keyspace ko = cass.getApplicationKeyspace( applicationId );
        SliceCounterQuery<String, Long> q = createCounterSliceQuery( ko, se, le );
        q.setColumnFamily( APPLICATION_AGGREGATE_COUNTERS.toString() );
        q.setRange( start, finish, false, ALL_COUNT );

        //Adding graphite metrics
        Timer.Context timeGetAggregateCounters = aggCounterTimer.time();
        QueryResult<CounterSlice<Long>> r = q.setKey(
                counterUtils.getAggregateCounterRow( counterName, userId, groupId, queueId, category, resolution ) )
                                             .execute();
        timeGetAggregateCounters.stop();

        List<AggregateCounter> counters = new ArrayList<AggregateCounter>();
        for ( HCounterColumn<Long> column : r.get().getColumns() ) {
            AggregateCounter count = new AggregateCounter( column.getName(), column.getValue() );
            if ( pad && !( resolution == CounterResolution.ALL ) ) {
                while ( count.getTimestamp() != expected_time ) {
                    counters.add( new AggregateCounter( expected_time, 0 ) );
                    expected_time = resolution.next( expected_time );
                }
                expected_time = resolution.next( expected_time );
            }
            counters.add( count );
        }
        if ( pad && !( resolution == CounterResolution.ALL ) ) {
            while ( expected_time <= finish ) {
                counters.add( new AggregateCounter( expected_time, 0 ) );
                expected_time = resolution.next( expected_time );
            }
        }
        return Results.fromCounters( new AggregateCounterSet( counterName, userId, groupId, category, counters ) );
    }


    @Override
    public Results getAggregateCounters( Query query ) throws Exception {
        CounterResolution resolution = query.getResolution();
        if ( resolution == null ) {
            resolution = CounterResolution.ALL;
        }
        long start = query.getStartTime() != null ? query.getStartTime() : 0;
        long finish = query.getFinishTime() != null ? query.getFinishTime() : 0;
        boolean pad = query.isPad();
        if ( start <= 0 ) {
            start = 0;
        }
        if ( ( finish <= 0 ) || ( finish < start ) ) {
            finish = System.currentTimeMillis();
        }
        start = resolution.round( start );
        finish = resolution.round( finish );
        long expected_time = start;

        if ( pad && ( resolution != CounterResolution.ALL ) ) {
            long max_counters = ( finish - start ) / resolution.interval();
            if ( max_counters > 1000 ) {
                finish = resolution.round( start + ( resolution.interval() * 1000 ) );
            }
        }

        List<Query.CounterFilterPredicate> filters = query.getCounterFilters();
        if ( filters == null ) {
            return null;
        }
        Map<String, CounterUtils.AggregateCounterSelection> selections =
                new HashMap<String, CounterUtils.AggregateCounterSelection>();
        Keyspace ko = cass.getApplicationKeyspace( applicationId );

        for ( Query.CounterFilterPredicate filter : filters ) {
            CounterUtils.AggregateCounterSelection selection =
                new CounterUtils.AggregateCounterSelection( filter.getName(),
                    getUuid( getUserByIdentifier( filter.getUser() ) ),
                    getUuid( getGroupByIdentifier( filter.getGroup() ) ),
                    org.apache.usergrid.mq.Queue.getQueueId( filter.getQueue() ), filter.getCategory() );
            selections.put( selection.getRow( resolution ), selection );
        }

        MultigetSliceCounterQuery<String, Long> q = HFactory.createMultigetSliceCounterQuery( ko, se, le );
        q.setColumnFamily( APPLICATION_AGGREGATE_COUNTERS.toString() );
        q.setRange( start, finish, false, ALL_COUNT );
        //Adding graphite metrics
        Timer.Context timeGetAggregateCounters = entGetAggregateCountersQueryTimer.time();
        QueryResult<CounterRows<String, Long>> rows = q.setKeys( selections.keySet() ).execute();
        timeGetAggregateCounters.stop();


        List<AggregateCounterSet> countSets = new ArrayList<AggregateCounterSet>();
        for ( CounterRow<String, Long> r : rows.get() ) {
            expected_time = start;
            List<AggregateCounter> counters = new ArrayList<AggregateCounter>();
            for ( HCounterColumn<Long> column : r.getColumnSlice().getColumns() ) {
                AggregateCounter count = new AggregateCounter( column.getName(), column.getValue() );
                if ( pad && ( resolution != CounterResolution.ALL ) ) {
                    while ( count.getTimestamp() != expected_time ) {
                        counters.add( new AggregateCounter( expected_time, 0 ) );
                        expected_time = resolution.next( expected_time );
                    }
                    expected_time = resolution.next( expected_time );
                }
                counters.add( count );
            }
            if ( pad && ( resolution != CounterResolution.ALL ) ) {
                while ( expected_time <= finish ) {
                    counters.add( new AggregateCounter( expected_time, 0 ) );
                    expected_time = resolution.next( expected_time );
                }
            }
            CounterUtils.AggregateCounterSelection selection = selections.get( r.getKey() );
            countSets.add( new AggregateCounterSet( selection.getName(), selection.getUserId(),
                    selection.getGroupId(), selection.getCategory(), counters ) );
        }

        Collections.sort( countSets, new Comparator<AggregateCounterSet>() {
            @Override
            public int compare( AggregateCounterSet o1, AggregateCounterSet o2 ) {
                String s1 = o1.getName();
                String s2 = o2.getName();
                return s1.compareTo( s2 );
            }
        } );
        return Results.fromCounters( countSets );
    }


    @Override
    public EntityRef getUserByIdentifier( Identifier identifier ) throws Exception {

        if ( identifier == null ) {
            logger.debug( "getUserByIdentifier: returning null for null identifier" );
            return null;
        }
        logger.debug( "getUserByIdentifier {}:{}", identifier.getType(), identifier.toString() );

        if ( identifier.isUUID() ) {
            return new SimpleEntityRef( "user", identifier.getUUID() );
        }
        if ( identifier.isName() ) {
            return this.getAlias( new SimpleEntityRef(
                    Application.ENTITY_TYPE, applicationId ), "user", identifier.getName() );
        }
        if ( identifier.isEmail() ) {


            final Iterable<EntityRef> emailProperty =
                    getEntityRefsForUniqueProperty( Schema.defaultCollectionName( "user" ), "email",
                            identifier.getEmail() );

            for ( EntityRef firstRef : emailProperty ) {
                return firstRef;
            }

            //            Query query = new Query();
            //            query.setEntityType( "user" );
            //            query.addEqualityFilter( "email", identifier.getEmail() );
            //            query.setLimit( 1 );
            //            query.setResultsLevel( REFS );
            //
            //            Results r = getRelationManager(
            //                ref( Application.ENTITY_TYPE, applicationId ) ).searchCollection( "users", query );
            //
            //            if ( r != null && r.getRef() != null ) {
            //                logger.debug("Got entity ref!");
            //                return r.getRef();
            //            }
            //            else {
            // look-aside as it might be an email in the name field
            logger.debug( "return alias" );
            return this.getAlias( new SimpleEntityRef( Application.ENTITY_TYPE, applicationId ), "user",
                    identifier.getEmail() );
            //            }
        }
        return null;
    }


    @Override
    public EntityRef getGroupByIdentifier( Identifier identifier ) throws Exception {
        if ( identifier == null ) {
            return null;
        }
        if ( identifier.isUUID() ) {
            return new SimpleEntityRef( "group", identifier.getUUID() );
        }
        if ( identifier.isName() ) {
            return this.getAlias( new SimpleEntityRef( Application.ENTITY_TYPE, applicationId ), "group",
                    identifier.getName() );
        }
        return null;
    }


    @Override
    public Set<String> getCounterNames() throws Exception {
        Set<String> names = new TreeSet<String>( CASE_INSENSITIVE_ORDER );
        Set<String> nameSet = cast( getDictionaryAsSet( getApplicationRef(), Schema.DICTIONARY_COUNTERS ) );
        names.addAll( nameSet );
        return names;
    }


    @Override
    public Map<String, Long> getEntityCounters( UUID entityId ) throws Exception {
        Map<String, Long> counters = new HashMap<String, Long>();
        Keyspace ko = cass.getApplicationKeyspace( applicationId );
        SliceCounterQuery<UUID, String> q = createCounterSliceQuery( ko, ue, se );
        q.setColumnFamily( ENTITY_COUNTERS.toString() );
        q.setRange( null, null, false, ALL_COUNT );
        //Adding graphite metrics
        Timer.Context timeEntityCounters = entGetEntityCountersTimer.time();
        QueryResult<CounterSlice<String>> r = q.setKey( entityId ).execute();
        timeEntityCounters.stop();
        for ( HCounterColumn<String> column : r.get().getColumns() ) {
            counters.put( column.getName(), column.getValue() );
        }
        return counters;
    }


    @Override
    public Map<String, Long> getApplicationCounters() throws Exception {
        return getEntityCounters( applicationId );
    }


    @Override
    public void incrementAggregateCounters( UUID userId, UUID groupId, String category, Map<String, Long> counters ) {

        // TODO shortcircuit
        if ( !skipAggregateCounters ) {
            long timestamp = cass.createTimestamp();
            Mutator<ByteBuffer> m = createMutator( cass.getApplicationKeyspace( applicationId ), be );
            counterUtils.batchIncrementAggregateCounters( m, applicationId, userId, groupId, null, category, counters, timestamp );

            //Adding graphite metrics
            Timer.Context timeIncrementCounters =entIncrementAggregateCountersTimer.time();
            CassandraPersistenceUtils.batchExecute( m, CassandraService.RETRY_COUNT );
            timeIncrementCounters.stop();
        }
    }


    @Override
    public boolean isPropertyValueUniqueForEntity( String entityType, String propertyName, Object propertyValue )
            throws Exception {


        return getIdForUniqueEntityField( entityType, propertyName, propertyValue ) == null;
    }


    /**
     * Load the unique property for the field
     */
    private Id getIdForUniqueEntityField( final String collectionName, final String propertyName,
                                          final Object propertyValue ) {

        //convert to a string, that's what we store
        final Id results = ecm.getIdField( Inflector.getInstance().singularize( collectionName ), new StringField(
                propertyName, propertyValue.toString() ) ).toBlocking() .lastOrDefault( null );
        return results;
    }


    @Override
    public Entity get( UUID uuid ) throws Exception {

        MapManager mm = getMapManagerForTypes();
        String entityType = mm.getString( uuid.toString() );

        final Entity entity;

        //this is the fall back, why isn't this writt
        if ( entityType == null ) {
             return null;
//            throw new EntityNotFoundException( String.format( "Counld not find type for uuid {}", uuid ) );
        }

        entity = get( new SimpleEntityRef( entityType, uuid ) );

        return entity;
    }


    /**
     * Get the map manager for uuid mapping
     */
    private MapManager getMapManagerForTypes() {
        Id mapOwner = new SimpleId( applicationId, TYPE_APPLICATION );

        final MapScope ms = CpNamingUtils.getEntityTypeMapScope(mapOwner);

        MapManager mm = managerCache.getMapManager( ms );

        return mm;
    }


    @Override
    public <A extends Entity> A get( EntityRef entityRef, Class<A> entityClass ) throws Exception {

        if ( entityRef == null ) {
            return null;
        }

        Entity entity = get(entityRef);

        if ( entity == null ) {
            logger.warn( "Entity {}/{} not found ", entityRef.getUuid(), entityRef.getType() );
            return null;
        }

        A ret = EntityFactory.newEntity(entityRef.getUuid(), entityRef.getType(), entityClass);
        ret.setProperties( entity.getProperties() );

        return ret;
    }


    @Override
    public Results getEntities( List<UUID> ids, String type ) {

        ArrayList<Entity> entities = new ArrayList<Entity>();

        for ( UUID uuid : ids ) {
            EntityRef ref = new SimpleEntityRef( type, uuid );
            Entity entity = null;
            try {
                entity = get( ref );
            }
            catch ( Exception ex ) {
                logger.warn( "Entity {}/{} not found", uuid, type );
            }

            if ( entity != null ) {
                entities.add( entity );
            }
        }

        return Results.fromEntities( entities );
    }


    @Override
    public Map<String, Role> getRolesWithTitles( Set<String> roleNames ) throws Exception {
        Map<String, Role> rolesWithTitles = new HashMap<String, Role>();

        Map<String, Object> nameResults = null;

        if ( roleNames != null ) {
            nameResults = getDictionaryElementValues( getApplicationRef(), DICTIONARY_ROLENAMES,
                    roleNames.toArray( new String[roleNames.size()] ) );
        }
        else {
            nameResults = cast( getDictionaryAsMap( getApplicationRef(), DICTIONARY_ROLENAMES ) );
            roleNames = nameResults.keySet();
        }
        Map<String, Object> timeResults = getDictionaryElementValues( getApplicationRef(), DICTIONARY_ROLETIMES,
                roleNames.toArray( new String[roleNames.size()] ) );

        for ( String roleName : roleNames ) {

            String savedTitle = string( nameResults.get( roleName ) );

            // no title, skip the role
            if ( savedTitle == null ) {
                continue;
            }

            Role newRole = new Role();
            newRole.setName( roleName );
            newRole.setTitle( savedTitle );
            newRole.setInactivity( getLong( timeResults.get( roleName ) ) );

            rolesWithTitles.put( roleName, newRole );
        }

        return rolesWithTitles;
    }


    @Override
    public String getRoleTitle( String roleName ) throws Exception {
        String title = string( getDictionaryElementValue( getApplicationRef(), DICTIONARY_ROLENAMES, roleName ) );
        if ( title == null ) {
            title = roleName;
        }
        return title;
    }


    @SuppressWarnings( "unchecked" )
    @Override
    public Map<String, Role> getUserRolesWithTitles( UUID userId ) throws Exception {
        return getRolesWithTitles(
                ( Set<String> ) cast( getDictionaryAsSet( userRef( userId ), DICTIONARY_ROLENAMES ) ) );
    }


    @SuppressWarnings( "unchecked" )
    @Override
    public Map<String, Role> getGroupRolesWithTitles( UUID groupId ) throws Exception {
        return getRolesWithTitles(
                ( Set<String> ) cast( getDictionaryAsSet( groupRef(groupId), DICTIONARY_ROLENAMES ) ) );
    }


    @Override
    public void addGroupToRole( UUID groupId, String roleName ) throws Exception {
        roleName = roleName.toLowerCase();
        addToDictionary( groupRef( groupId ), DICTIONARY_ROLENAMES, roleName, roleName );
        addToCollection( groupRef( groupId ), COLLECTION_ROLES, getRoleRef( roleName ) );
    }


    @Override
    public void removeGroupFromRole( UUID groupId, String roleName ) throws Exception {
        roleName = roleName.toLowerCase();
        removeFromDictionary(groupRef(groupId), DICTIONARY_ROLENAMES, roleName);
        removeFromCollection(groupRef(groupId), COLLECTION_ROLES, getRoleRef(roleName));
    }


    @Override
    public Set<String> getGroupPermissions( UUID groupId ) throws Exception {
        return cast( getDictionaryAsSet( groupRef( groupId ), Schema.DICTIONARY_PERMISSIONS ) );
    }


    @Override
    public void grantGroupPermission( UUID groupId, String permission ) throws Exception {
        permission = permission.toLowerCase();
        addToDictionary( groupRef( groupId ), DICTIONARY_PERMISSIONS, permission );
    }


    @Override
    public void revokeGroupPermission( UUID groupId, String permission ) throws Exception {
        permission = permission.toLowerCase();
        removeFromDictionary(groupRef(groupId), DICTIONARY_PERMISSIONS, permission);
    }


    private EntityRef groupRef( UUID groupId ) {
        return new SimpleEntityRef( Group.ENTITY_TYPE, groupId );
    }


    @Override
    public <A extends Entity> A batchCreate( Mutator<ByteBuffer> ignored, String entityType,
            Class<A> entityClass, Map<String, Object> properties, UUID importId, UUID timestampUuid )
            throws Exception {

        String eType = Schema.normalizeEntityType( entityType );

        Schema schema = Schema.getDefaultSchema();

        boolean is_application = TYPE_APPLICATION.equals( eType );

        if ( ( ( applicationId == null ) || applicationId.equals( UUIDUtils.ZERO_UUID ) ) && !is_application ) {
            return null;
        }

        long timestamp = UUIDUtils.getUUIDLong( timestampUuid );

        UUID itemId = UUIDGenerator.newTimeUUID();

        if ( is_application ) {
            itemId = applicationId;
        }
        if ( importId != null ) {
            itemId = importId;
        }
        if ( properties == null ) {
            properties = new TreeMap<String, Object>( CASE_INSENSITIVE_ORDER );
        }

        if ( importId != null ) {
            if ( UUIDUtils.isTimeBased( importId ) ) {
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
                if ( !PROPERTY_UUID.equals( p ) && !PROPERTY_TYPE.equals( p ) && !PROPERTY_CREATED.equals( p )
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

        if ( properties.isEmpty() ) {
            return null;
        }

        properties.put( PROPERTY_UUID, itemId );
        properties.put( PROPERTY_TYPE, Schema.normalizeEntityType( entityType, false ) );

        if ( importId != null ) {
            if ( properties.get( PROPERTY_CREATED ) == null ) {
                properties.put( PROPERTY_CREATED, ( long ) ( timestamp / 1000 ) );
            }

            if ( properties.get( PROPERTY_MODIFIED ) == null ) {
                properties.put( PROPERTY_MODIFIED, ( long ) ( timestamp / 1000 ) );
            }
        }
        else {
            properties.put( PROPERTY_CREATED, ( long ) ( timestamp / 1000 ) );
            properties.put( PROPERTY_MODIFIED, ( long ) ( timestamp / 1000 ) );
        }

        // special case timestamp and published properties
        // and dictionary their timestamp values if not set
        // this is sure to break something for someone someday

        if ( properties.containsKey( PROPERTY_TIMESTAMP ) ) {
            long ts = getLong( properties.get( PROPERTY_TIMESTAMP ) );
            if ( ts <= 0 ) {
                properties.put( PROPERTY_TIMESTAMP, ( long ) ( timestamp / 1000 ) );
            }
        }

        A entity = EntityFactory.newEntity( itemId, eType, entityClass );
        entity.addProperties( properties );

        //        logger.info( "Entity created of type {}", entity.getClass().getName() );

        if ( Event.ENTITY_TYPE.equals( eType ) ) {
            Event event = ( Event ) entity.toTypedEntity();
            for ( String prop_name : properties.keySet() ) {
                Object propertyValue = properties.get( prop_name );
                if ( propertyValue != null ) {
                    event.setProperty( prop_name, propertyValue );
                }
            }

            //doesn't allow the mutator to be ignored.
            counterUtils.addEventCounterMutations( ignored, applicationId, event, timestamp );

            incrementEntityCollection( "events", timestamp );

            return entity;
        }

        org.apache.usergrid.persistence.model.entity.Entity cpEntity = entityToCpEntity( entity, importId );

        // prepare to write and index Core Persistence Entity into default scope

        if ( logger.isDebugEnabled() ) {
            logger.debug( "Writing entity {}:{} into app {}\n",
                new Object[] {
                    entity.getType(),
                    entity.getUuid(),
                    applicationId,
                    CpEntityMapUtils.toMap( cpEntity )
                } );

        }

        try {

            if(logger.isDebugEnabled()) {
                logger.debug( "About to Write {}:{} version {}", new Object[] {
                    cpEntity.getId().getType(), cpEntity.getId().getUuid(), cpEntity.getVersion()
                } );
            }

            cpEntity = ecm.write( cpEntity ).toBlocking().last();
            entity.setSize(cpEntity.getSize());

            if(logger.isDebugEnabled()) {
                logger.debug( "Wrote {}:{} version {}", new Object[] {
                    cpEntity.getId().getType(), cpEntity.getId().getUuid(), cpEntity.getVersion()
                } );
            }

        }
        catch ( WriteUniqueVerifyException wuve ) {
            handleWriteUniqueVerifyException( entity, wuve );
        }

        // reflect changes in the legacy Entity
        entity.setUuid( cpEntity.getId().getUuid() );
        entity.setProperties( cpEntity );

        // add to and index in collection of the application
        if ( !is_application ) {

            String collectionName = Schema.defaultCollectionName( eType );
            CpRelationManager cpr = ( CpRelationManager ) getRelationManager( getApplication() );
            cpr.addToCollection( collectionName, entity );

            // Invoke counters
            incrementEntityCollection( collectionName, timestamp );
        }

        //write to our types map
        MapManager mm = getMapManagerForTypes();
        mm.putString( itemId.toString(), entity.getType() );


        return entity;
    }


    private void incrementEntityCollection( String collection_name, long cassandraTimestamp ) {
        try {
            incrementAggregateCounters( null, null, null,
                    APPLICATION_COLLECTION + collection_name, ONE_COUNT, cassandraTimestamp );
        }
        catch ( Exception e ) {
            logger.error( "Unable to increment counter application.collection: {}.",
                    new Object[] { collection_name, e } );
        }
        try {
            incrementAggregateCounters( null, null, null,
                    APPLICATION_ENTITIES, ONE_COUNT, cassandraTimestamp );
        }
        catch ( Exception e ) {
            logger.error( "Unable to increment counter application.entities for collection: "
                    + "{} with timestamp: {}",
                    new Object[] { collection_name, cassandraTimestamp, e } );
        }
    }


    private void handleWriteUniqueVerifyException( Entity entity, WriteUniqueVerifyException wuve )
            throws DuplicateUniquePropertyExistsException {

        // we may have multiple conflicts, but caller expects only one
        Map<String, Field> violiations = wuve.getVioliations();

        if ( violiations != null ) {
            Field conflict = violiations.get( violiations.keySet().iterator().next() );

            throw new DuplicateUniquePropertyExistsException( entity.getType(), conflict.getName(),
                    conflict.getValue() );
        }
        else {
            throw new DuplicateUniquePropertyExistsException( entity.getType(), "Unknown property name",
                    "Unknown property value" );
        }
    }


    @Override
    public Mutator<ByteBuffer> batchSetProperty( Mutator<ByteBuffer> batch, EntityRef entity,
            String propertyName, Object propertyValue, UUID timestampUuid ) throws Exception {

        throw new UnsupportedOperationException( "Not supported yet." );
    }


    @Override
    public Mutator<ByteBuffer> batchSetProperty( Mutator<ByteBuffer> batch, EntityRef entity,
            String propertyName, Object propertyValue, boolean force, boolean noRead,
            UUID timestampUuid ) throws Exception {

        throw new UnsupportedOperationException( "Not supported yet." );
    }


    @Override
    public Mutator<ByteBuffer> batchUpdateDictionary( Mutator<ByteBuffer> batch, EntityRef entity,
            String dictionaryName, Object elementValue, Object elementCoValue,
            boolean removeFromDictionary, UUID timestampUuid )
            throws Exception {

        long timestamp = UUIDUtils.getUUIDLong( timestampUuid );

        // dictionaryName = dictionaryName.toLowerCase();
        if ( elementCoValue == null ) {
            elementCoValue = ByteBuffer.allocate( 0 );
        }

        boolean entityHasDictionary = Schema.getDefaultSchema()
                .hasDictionary( entity.getType(), dictionaryName );


        ApplicationCF dictionary_cf = entityHasDictionary
                ? ENTITY_DICTIONARIES : ENTITY_COMPOSITE_DICTIONARIES;

        if ( elementValue != null ) {
            if ( !removeFromDictionary ) {
                // Set the new value

                elementCoValue = CassandraPersistenceUtils.toStorableBinaryValue(
                        elementCoValue, !entityHasDictionary );

                CassandraPersistenceUtils.addInsertToMutator( batch, dictionary_cf,
                        CassandraPersistenceUtils.key( entity.getUuid(), dictionaryName ),
                        entityHasDictionary ? elementValue : asList( elementValue ),
                        elementCoValue, timestamp );

                if ( !entityHasDictionary ) {
                    CassandraPersistenceUtils.addInsertToMutator( batch, ENTITY_DICTIONARIES,
                            CassandraPersistenceUtils.key( entity.getUuid(), DICTIONARY_SETS ),
                            dictionaryName, null, timestamp );
                }
            }
            else {
                CassandraPersistenceUtils.addDeleteToMutator( batch, dictionary_cf,
                        CassandraPersistenceUtils.key( entity.getUuid(), dictionaryName ),
                        entityHasDictionary ? elementValue : asList( elementValue ), timestamp );
            }
        }

        return batch;
    }


    @Override
    public Mutator<ByteBuffer> batchUpdateDictionary( Mutator<ByteBuffer> batch, EntityRef entity,
            String dictionaryName, Object elementValue, boolean removeFromDictionary,
            UUID timestampUuid )
            throws Exception {

        return batchUpdateDictionary( batch, entity, dictionaryName, elementValue, null,
                removeFromDictionary, timestampUuid );
    }


    @Override
    public Mutator<ByteBuffer> batchUpdateProperties( Mutator<ByteBuffer> batch, EntityRef entity,
            Map<String, Object> properties, UUID timestampUuid ) throws Exception {

        throw new UnsupportedOperationException( "Not supported yet." );
    }


    //TODO: ask what the difference is.
    @Override
    public Set<String> getDictionaryNames( EntityRef entity ) throws Exception {

        Set<String> dictionaryNames = new TreeSet<String>( CASE_INSENSITIVE_ORDER );

        List<HColumn<String, ByteBuffer>> results =
                cass.getAllColumns( cass.getApplicationKeyspace( applicationId ), ENTITY_DICTIONARIES,
                        CassandraPersistenceUtils.key( entity.getUuid(), DICTIONARY_SETS ) );

        for ( HColumn<String, ByteBuffer> result : results ) {
            String str = string( result.getName() );
            if ( str != null ) {
                dictionaryNames.add( str );
            }
        }

        Set<String> schemaSets = Schema.getDefaultSchema().getDictionaryNames( entity.getType() );
        if ( ( schemaSets != null ) && !schemaSets.isEmpty() ) {
            dictionaryNames.addAll( schemaSets );
        }

        return dictionaryNames;
    }


    @Override
    public void insertEntity( EntityRef ref ) throws Exception {

        throw new UnsupportedOperationException( "Not supported yet." );
    }


    @Override
    public UUID getApplicationId() {

        return applicationId;
    }

    @Override
    public IndexBucketLocator getIndexBucketLocator() {

        throw new UnsupportedOperationException( "Not supported ever." );
    }


    @Override
    public CassandraService getCass() {
        return cass;
    }


    @Override
    public void flushManagerCaches() {
        managerCache.invalidate();
    }


    @Override
    public Set<String> getConnectionsAsSource( final EntityRef entityRef ) {

        Preconditions.checkNotNull(entityRef, "entityRef cannot be null");

        final GraphManager graphManager = graphManagerFactory.createEdgeManager( applicationScope );

        final SearchEdgeType searchByEdgeType = createConnectionTypeSearch( entityRef.asId() );

        return graphManager.getEdgeTypesFromSource( searchByEdgeType ).map( edgeName -> getConnectionNameFromEdgeName( edgeName ) ).collect(
            () -> new HashSet<String>(), ( r, s ) -> r.add( s ) ).toBlocking().last();
    }


    @Override
    public Set<String> getConnectionsAsTarget( final EntityRef entityRef ) {
        Preconditions.checkNotNull( entityRef, "entityRef cannot be null" );

        final GraphManager graphManager = graphManagerFactory.createEdgeManager( applicationScope );

        final SearchEdgeType searchByEdgeType = createConnectionTypeSearch( entityRef.asId() );

        return graphManager.getEdgeTypesToTarget(searchByEdgeType).map(
                    edgeName -> getConnectionNameFromEdgeName( edgeName ) ).collect( () -> new HashSet<String>(  ), ( r, s ) -> r.add(s) ).toBlocking().last();
    }


    @Override
    public void addIndex(final String newIndexName,final int shards,final int replicas, final String writeConsistency){
        managerCache.getEntityIndex(applicationScope).addIndex(newIndexName, shards, replicas, writeConsistency);
    }

    @Override
    public void initializeIndex(){
        managerCache.getEntityIndex(applicationScope).initialize();
    }
    /**
     * TODO, these 3 methods are super janky.  During refactoring we should clean this model up
     */
    public EntityIndex.IndexRefreshCommandInfo refreshIndex() {
        try {
            long start = System.currentTimeMillis();
            // refresh special indexes without calling EntityManager refresh because stack overflow
            Map<String, Object> map = new org.apache.usergrid.persistence.index.utils.MapUtils.HashMapBuilder<>();
            map.put("some prop", "test");
            boolean hasFinished = false;
            Entity refreshEntity = create("refresh", map);
            EntityIndex.IndexRefreshCommandInfo indexRefreshCommandInfo
                = managerCache.getEntityIndex(applicationScope).refreshAsync().toBlocking().first();
            try {
                for (int i = 0; i < 10; i++) {
                    if (searchCollection(
                        new SimpleEntityRef(org.apache.usergrid.persistence.entities.Application.ENTITY_TYPE, getApplicationId()),
                        InflectionUtils.pluralize("refresh"),
                        Query.fromQL("select * where uuid='" + refreshEntity.getUuid() + "'")
                    ).size() > 0
                        ) {
                        hasFinished = true;
                        break;
                    }
                    Thread.sleep(200);

                    indexRefreshCommandInfo
                        = managerCache.getEntityIndex(applicationScope).refreshAsync().toBlocking().first();
                }
                if(!hasFinished){
                    throw new RuntimeException("Did not find entity {} during refresh. uuid->"+refreshEntity.getUuid());
                }
            }finally {
                delete(refreshEntity);
            }
            Thread.sleep(200);

            return indexRefreshCommandInfo;
        } catch (Exception e) {
            throw new RuntimeException("refresh failed",e);
        }

    }
}


