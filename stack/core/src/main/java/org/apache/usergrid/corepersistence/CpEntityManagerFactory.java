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


import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.usergrid.corepersistence.index.ReIndexRequestBuilder;
import org.apache.usergrid.persistence.*;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchByEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import org.apache.commons.lang.StringUtils;

import org.apache.usergrid.corepersistence.asyncevents.AsyncEventService;
import org.apache.usergrid.corepersistence.index.ReIndexService;
import org.apache.usergrid.corepersistence.pipeline.builder.PipelineBuilderFactory;
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.exception.ConflictException;
import org.apache.usergrid.persistence.cassandra.CassandraService;
import org.apache.usergrid.persistence.cassandra.CounterUtils;
import org.apache.usergrid.persistence.cassandra.Setup;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.serialization.impl.migration.EntityIdScope;
import org.apache.usergrid.persistence.core.metrics.MetricsFactory;
import org.apache.usergrid.persistence.core.migration.data.MigrationDataProvider;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.core.util.Health;
import org.apache.usergrid.persistence.entities.Application;
import org.apache.usergrid.persistence.exceptions.ApplicationAlreadyExistsException;
import org.apache.usergrid.persistence.exceptions.DuplicateUniquePropertyExistsException;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.GraphManager;
import org.apache.usergrid.persistence.graph.GraphManagerFactory;
import org.apache.usergrid.persistence.graph.SearchByEdgeType;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchByEdgeType;
import org.apache.usergrid.persistence.index.ApplicationEntityIndex;
import org.apache.usergrid.persistence.index.EntityIndex;
import org.apache.usergrid.persistence.index.EntityIndexFactory;
import org.apache.usergrid.persistence.index.IndexRefreshCommand;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;
import org.apache.usergrid.utils.UUIDUtils;

import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;

import rx.Observable;

import static java.lang.String.CASE_INSENSITIVE_ORDER;

import static org.apache.usergrid.persistence.Schema.PROPERTY_APPLICATION_ID;
import static org.apache.usergrid.persistence.Schema.PROPERTY_NAME;
import static org.apache.usergrid.persistence.Schema.TYPE_APPLICATION;


/**
 * Implement good-old Usergrid EntityManagerFactory with the new-fangled Core Persistence API.
 * This is where we keep track of applications and system properties.
 */
public class CpEntityManagerFactory implements EntityManagerFactory, ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger( CpEntityManagerFactory.class );
    private final EntityIndexFactory entityIndexFactory;
    private final EntityManagerFig entityManagerFig;

    private ApplicationContext applicationContext;

    private Setup setup = null;

    /** Have we already initialized the index for the management app? */
    private AtomicBoolean indexInitialized = new AtomicBoolean(  );

    // cache of already instantiated entity managers
    private LoadingCache<UUID, EntityManager> entityManagers
        = CacheBuilder.newBuilder().maximumSize(100).build(new CacheLoader<UUID, EntityManager>() {
            public EntityManager load(UUID appId) { // no checked exception
                return _getEntityManager(appId);
            }
        });

    private final ApplicationIdCache applicationIdCache;

    private ManagerCache managerCache;

    private CassandraService cassandraService;
    private CounterUtils counterUtils;
    private Injector injector;
    private final ReIndexService reIndexService;
    private final EntityIndex entityIndex;
    private final MetricsFactory metricsFactory;
    private final AsyncEventService indexService;
    private final PipelineBuilderFactory pipelineBuilderFactory;
    private final GraphManagerFactory graphManagerFactory;

    public CpEntityManagerFactory( final CassandraService cassandraService, final CounterUtils counterUtils,
                                   final Injector injector ) {

        this.cassandraService = cassandraService;
        this.counterUtils = counterUtils;
        this.injector = injector;
        this.reIndexService = injector.getInstance(ReIndexService.class);
        this.entityManagerFig = injector.getInstance(EntityManagerFig.class);
        this.entityIndex = injector.getInstance(EntityIndex.class);
        this.entityIndexFactory = injector.getInstance(EntityIndexFactory.class);
        this.managerCache = injector.getInstance( ManagerCache.class );
        this.metricsFactory = injector.getInstance( MetricsFactory.class );
        this.indexService = injector.getInstance( AsyncEventService.class );
        this.pipelineBuilderFactory = injector.getInstance( PipelineBuilderFactory.class );
        this.graphManagerFactory = injector.getInstance( GraphManagerFactory.class );
        this.applicationIdCache = injector.getInstance(ApplicationIdCacheFactory.class).getInstance(
            getManagementEntityManager() );


    }


    public CounterUtils getCounterUtils() {
        return counterUtils;
    }


    public CassandraService getCassandraService() {
        return cassandraService;
    }



    private void init() {

        EntityManager em = getEntityManager(getManagementAppId());

        try {
            if ( em.getApplication() == null ) {
                logger.info("Creating management application");
                Map mgmtAppProps = new HashMap<String, Object>();
                mgmtAppProps.put(PROPERTY_NAME, "systemapp");
                em.create( getManagementAppId(), TYPE_APPLICATION, mgmtAppProps);
                em.getApplication();
            }

//            entityIndex.refreshAsync();

        } catch (Exception ex) {
            throw new RuntimeException("Fatal error creating management application", ex);
        }
    }


    private Observable<EntityIdScope> getAllEntitiesObservable(){
      return injector.getInstance( Key.get(new TypeLiteral< MigrationDataProvider<EntityIdScope>>(){})).getData();
    }



    @Override
    public EntityManager getEntityManager(UUID applicationId) {
        try {
            return entityManagers.get( applicationId );
        }
        catch ( Exception ex ) {
            logger.error("Error getting oldAppInfo manager", ex);
        }
        return _getEntityManager(applicationId);
    }


    private EntityManager _getEntityManager( UUID applicationId ) {
        EntityManager em = new CpEntityManager(cassandraService, counterUtils, indexService, managerCache,
            metricsFactory, entityManagerFig, pipelineBuilderFactory, graphManagerFactory, applicationId );

        return em;
    }

    @Override
    public Entity createApplicationV2(String organizationName, String name) throws Exception {
        return createApplicationV2(organizationName, name, null);
    }


    @Override
    public Entity createApplicationV2(
        String orgName, String name, Map<String, Object> properties) throws Exception {

        String appName = buildAppName( orgName, name );


        final Optional<UUID> appId = applicationIdCache.getApplicationId( appName );

        if ( appId.isPresent()) {
            throw new ApplicationAlreadyExistsException( name );
        }

        UUID applicationId = UUIDGenerator.newTimeUUID();

        logger.debug( "New application orgName {} orgAppName {} id {} ",
            new Object[] { orgName, name, applicationId.toString() } );

        return initializeApplicationV2(orgName, applicationId, appName, properties);
    }



    private String buildAppName( String organizationName, String name ) {
        return StringUtils.lowerCase(name.contains("/") ? name : organizationName + "/" + name);
    }


    /**
     * @return UUID of newly created Entity of type application_info
     */
    @Override
    public Entity initializeApplicationV2( String organizationName, final UUID applicationId, String name,
                                       Map<String, Object> properties ) throws Exception {

        // Ensure our management system exists before creating our application
        init();

        EntityManager managementEm = getEntityManager( getManagementAppId() );

        final String appName = buildAppName( organizationName, name );

        // check for pre-existing application

        if ( lookupApplication( appName ).isPresent()) {
            throw new ApplicationAlreadyExistsException( appName );
        }

        getSetup().setupApplicationKeyspace( applicationId, appName );

        if ( properties == null ) {
            properties = new TreeMap<>( CASE_INSENSITIVE_ORDER );
        }
        properties.put( PROPERTY_NAME, appName );
        EntityManager appEm = getEntityManager(applicationId);
        appEm.create(applicationId, TYPE_APPLICATION, properties);
        appEm.resetRoles();
     //   entityIndex.refreshAsync();//.toBlocking().last();


        // create application info entity in the management app

        Map<String, Object> appInfoMap = new HashMap<String, Object>() {{
            put( PROPERTY_NAME, appName );
            put( PROPERTY_APPLICATION_ID, applicationId );
        }};

        Entity appInfo;
        try {
            appInfo = managementEm.create(new SimpleId(applicationId,CpNamingUtils.APPLICATION_INFO), appInfoMap);
        } catch (DuplicateUniquePropertyExistsException e) {
            throw new ApplicationAlreadyExistsException(appName);
        }

        // evict app Id from cache
        applicationIdCache.evictAppId(appName);

        logger.info("Initialized application {}", appName);
        return appInfo;
    }



    /**
     * Delete Application.
     *
     * <p>The Application Entity is be moved to a Deleted_Applications collection and the
     * Application index will be removed.
     *
     * <p>TODO: add scheduled task that can completely delete all deleted application data.</p>
     *
     * @param applicationId UUID of Application to be deleted.
     */
    @Override
    public void deleteApplication(UUID applicationId) throws Exception {

        // find application_info for application to delete

        migrateAppInfo(applicationId, CpNamingUtils.APPLICATION_INFO, CpNamingUtils.DELETED_APPLICATION_INFO).toBlocking()
            .lastOrDefault(null);
    }

    //TODO: return status for restore
    @Override
    public Entity restoreApplication(UUID applicationId) throws Exception {

        // get the deleted_application_info for the deleted app
        return (Entity) migrateAppInfo(applicationId, CpNamingUtils.DELETED_APPLICATION_INFO, CpNamingUtils.APPLICATION_INFO)
            .map(o -> {
                final ReIndexRequestBuilder builder =
                    reIndexService.getBuilder().withApplicationId(applicationId);
                return reIndexService.rebuildIndex(builder);
            })
            .map(status -> {
                final EntityManager managementEm = getEntityManager(getManagementAppId());
                try {
                    return managementEm.get(new SimpleEntityRef(CpNamingUtils.APPLICATION_INFO, applicationId));
                } catch (Exception e) {
                    logger.error("Failed to get entity", e);
                    throw new RuntimeException(e);
                }
            })
            .toBlocking().lastOrDefault(null);

    }

    @Override
    public Observable migrateAppInfo(UUID applicationUUID, String collectionFromName, String collectionToName) throws Exception {

        final ApplicationScope managementAppScope = CpNamingUtils.getApplicationScope(CpNamingUtils.MANAGEMENT_APPLICATION_ID);
        final EntityManager managementEm = getEntityManager(CpNamingUtils.MANAGEMENT_APPLICATION_ID);

        final Id applicationId = new SimpleId(applicationUUID, collectionFromName);
        final ApplicationScope applicationScope = new ApplicationScopeImpl(applicationId);

        Entity oldAppEntity = managementEm.get(new SimpleEntityRef(collectionFromName, applicationUUID));
        Observable copyConnections = Observable.empty();
        if (oldAppEntity != null) {
            // ensure that there is not already a deleted app with the same name

            final EntityRef alias = managementEm.getAlias(collectionToName, oldAppEntity.getName());
            if (alias != null) {
                throw new ConflictException("Cannot delete app with same name as already deleted app");
            }
            // make a copy of the app to delete application_info entity
            // and put it in a deleted_application_info collection

            final Entity newAppEntity = managementEm.create(new SimpleId(applicationUUID,
                collectionToName), oldAppEntity.getProperties());

            // copy its connections too

            final Set<String> connectionTypes = managementEm.getConnectionTypes(oldAppEntity);
            copyConnections = Observable.from(connectionTypes).doOnNext(connType -> {
                try {
                    final Results connResults =
                        managementEm.getTargetEntities(oldAppEntity, connType, null, Query.Level.ALL_PROPERTIES);
                    connResults.getEntities().forEach(entity -> {
                        try {
                            managementEm.createConnection(newAppEntity, connType, entity);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

        }
        final Id managementAppId = CpNamingUtils.getManagementApplicationId();
        final ApplicationEntityIndex aei = entityIndexFactory.createApplicationEntityIndex(applicationScope);
        final GraphManager managementGraphManager = managerCache.getGraphManager(managementAppScope);
        final Edge createEdge = CpNamingUtils.createCollectionEdge(managementAppId, collectionToName, applicationId);

        //TODO T.N. Removing this causes a failure
//        final Observable compactObservable = managementGraphManager.compactNode(applicationId);

        final Observable deleteNodeGraph = managementGraphManager
            .markNode( applicationId, CpNamingUtils.createGraphOperationTimestamp() );
//            .flatMap(id -> compactObservable);

        final Observable createNodeGraph = managementGraphManager.writeEdge(createEdge);

        final Observable deleteAppFromIndex = aei.deleteApplication();

        return Observable
            .merge(copyConnections, createNodeGraph, deleteNodeGraph, deleteAppFromIndex)
            .doOnCompleted(() -> {
                try {
                    if (oldAppEntity != null) {
                        managementEm.delete(oldAppEntity);
                        applicationIdCache.evictAppId(oldAppEntity.getName());
                    }
                    entityIndex.refreshAsync().toBlocking().last();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
    }


    @Override
    public UUID importApplication(
            String organization, UUID applicationId,
            String name, Map<String, Object> properties) throws Exception {

        throw new UnsupportedOperationException("Not supported yet.");
    }


    public Optional<UUID> lookupApplication( String orgAppName ) throws Exception {
        return applicationIdCache.getApplicationId(orgAppName);
    }


    @Override
    public Map<String, UUID> getApplications() throws Exception {
        return getApplications(false);
    }


    @Override
    public Map<String, UUID> getDeletedApplications() throws Exception {
        return getApplications( true );
    }


    public Map<String, UUID> getApplications(boolean deleted) throws Exception {

        Map<String, UUID> appMap = new HashMap<>();

        ApplicationScope appScope =
            CpNamingUtils.getApplicationScope(CpNamingUtils.MANAGEMENT_APPLICATION_ID);
        GraphManager gm = managerCache.getGraphManager(appScope);

        EntityManager managementEM = getEntityManager(CpNamingUtils.MANAGEMENT_APPLICATION_ID);
        Application managementApp = managementEM.getApplication();
        if( managementApp == null ) {
            throw new RuntimeException("Management App "
                + CpNamingUtils.MANAGEMENT_APPLICATION_ID + " should never be null");
        }
        Id managementId = new SimpleId( managementApp.getUuid(), managementApp.getType() );

        final String edgeType;

        if ( deleted ) {
            edgeType = CpNamingUtils.getEdgeTypeFromCollectionName( CpNamingUtils.DELETED_APPLICATION_INFOS );
        } else {
            edgeType = CpNamingUtils.getEdgeTypeFromCollectionName( CpNamingUtils.APPLICATION_INFOS );
        }

        logger.debug("getApplications(): Loading edges of edgeType {} from {}:{}",
            new Object[]{edgeType, managementId.getType(), managementId.getUuid()});

        Observable<Edge> edges = gm.loadEdgesFromSource( new SimpleSearchByEdgeType(
                managementId, edgeType, Long.MAX_VALUE,
                SearchByEdgeType.Order.DESCENDING, Optional.<Edge>absent() ));

        // TODO This is wrong, and will result in OOM if there are too many applications.
        // This needs to stream properly with a buffer

        edges.doOnNext(edge -> {
            Id targetId = edge.getTargetNode();

            logger.debug("getApplications(): Processing edge from {}:{} to {}:{}", new Object[]{
                edge.getSourceNode().getType(), edge.getSourceNode().getUuid(),
                edge.getTargetNode().getType(), edge.getTargetNode().getUuid()
            });

            org.apache.usergrid.persistence.model.entity.Entity appInfo =
                managerCache.getEntityCollectionManager(appScope).load(targetId)
                    .toBlocking().lastOrDefault(null);

            if (appInfo == null) {
                logger.warn("Application {} has edge but not found in em", targetId);
                return;
            }

            UUID applicationId = UUIDUtils.tryExtractUUID(
                appInfo.getField(PROPERTY_APPLICATION_ID).getValue().toString());

            appMap.put((String) appInfo.getField(PROPERTY_NAME).getValue(), applicationId);
        }).toBlocking().lastOrDefault(null);

        return appMap;
    }


    @Override
    public void setup() throws Exception {
        getSetup().init();
        init();
    }


    @Override
    public Map<String, String> getServiceProperties() {

        Map<String, String> props = new HashMap<String,String>();

        EntityManager em = getEntityManager(getManagementAppId());
        Query q = Query.fromQL("select *");
        Results results = null;
        try {
            results = em.searchCollection( em.getApplicationRef(), "propertymaps", q);

        } catch (Exception ex) {
            logger.error("Error getting system properties", ex);
        }

        if ( results == null || results.isEmpty() ) {
            return props;
        }

        org.apache.usergrid.persistence.Entity e = results.getEntity();
        for ( String key : e.getProperties().keySet() ) {
            props.put( key, props.get(key).toString() );
        }
        return props;
    }


    @Override
    public boolean updateServiceProperties(Map<String, String> properties) {

        EntityManager em = getEntityManager(getManagementAppId());
        Query q = Query.fromQL("select *");
        Results results = null;
        try {
            results = em.searchCollection( em.getApplicationRef(), "propertymaps", q);

        } catch (Exception ex) {
            logger.error("Error getting system properties", ex);
            return false;
        }

        org.apache.usergrid.persistence.Entity propsEntity = null;

        if ( !results.isEmpty() ) {
            propsEntity = results.getEntity();

        } else {
            propsEntity = EntityFactory.newEntity( UUIDUtils.newTimeUUID(), "propertymap");
        }

        // intentionally going only one-level deep into fields and treating all
        // values as strings because that is all we need for service properties
        for ( String key : properties.keySet() ) {
            propsEntity.setProperty(key, properties.get(key).toString());
        }

        try {
            em.update( propsEntity );

        } catch (Exception ex) {
            logger.error("Error updating service properties", ex);
            return false;
        }

        return true;
    }


    @Override
    public boolean setServiceProperty(final String name, final String value) {
        return updateServiceProperties(new HashMap<String, String>() {{
            put(name, value);
        }});
    }


    @Override
    public boolean deleteServiceProperty(String name) {

        EntityManager em = getEntityManager(getManagementAppId());


        Query q = Query.fromQL( "select *");
        Results results = null;
        try {
            results = em.searchCollection( em.getApplicationRef(), "propertymaps", q);

        } catch (Exception ex) {
            logger.error("Error getting service property for delete of property: " + name, ex);
            return false;
        }

        org.apache.usergrid.persistence.Entity propsEntity = null;

        if ( !results.isEmpty() ) {
            propsEntity = results.getEntity();

        } else {
            propsEntity = EntityFactory.newEntity( UUIDUtils.newTimeUUID(), "propertymap");
        }

        try {
            ((AbstractEntity)propsEntity).clearDataset( name );
            em.update( propsEntity );

        } catch (Exception ex) {
            logger.error("Error deleting service property orgAppName: " + name, ex);
            return false;
        }

        return true;
    }

    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    @Override
    public void setApplicationContext( ApplicationContext applicationContext ) throws BeansException {
        this.applicationContext = applicationContext;
//        try {
//            setup();
//        } catch (Exception ex) {
//            logger.error("Error setting up EMF", ex);
//        }
    }


    @Override
    public long performEntityCount() {
        //TODO, this really needs to be a task that writes this data somewhere since this will get
        //progressively slower as the system expands
        return (Long) getAllEntitiesObservable().countLong().toBlocking().last();
    }



    @Override
    public UUID getManagementAppId() {
        return CpNamingUtils.MANAGEMENT_APPLICATION_ID;
    }

    @Override
    public EntityManager getManagementEntityManager() {
        return getEntityManager(CpNamingUtils.MANAGEMENT_APPLICATION_ID);
    }


    /**
     * Gets the setup.
     * @return Setup helper
     */
    public Setup getSetup() {
        if ( setup == null ) {
            setup = new CpSetup( this, cassandraService, injector );
        }
        return setup;
    }


    /**
     * TODO, these 3 methods are super janky.  During refactoring we should clean this model up
     */
    public IndexRefreshCommand.IndexRefreshCommandInfo refreshIndex() {

        // refresh special indexes without calling EntityManager refresh because stack overflow
        maybeCreateIndexes();

        return entityIndex.refreshAsync().toBlocking().first();
    }

    private void maybeCreateIndexes() {
        if ( indexInitialized.getAndSet( true ) ) {
            return;
        }

//        entityIndex.initializeIndex();
    }


    private List<ApplicationEntityIndex> getManagementIndexes() {

        return Arrays.asList(
            managerCache.getEntityIndex( // management app
                CpNamingUtils.getApplicationScope(getManagementAppId())));
    }




    @Override
    public void flushEntityManagerCaches() {

        managerCache.invalidate();

        applicationIdCache.evictAll();

        Map<UUID, EntityManager>  entityManagersMap = entityManagers.asMap();
        for ( UUID appUuid : entityManagersMap.keySet() ) {
            EntityManager em = entityManagersMap.get(appUuid);
            em.flushManagerCaches();
        }
    }


    @Override
    public void addIndex(final String indexSuffix,final int shards,final int replicas, final String writeConsistency){
        entityIndex.addIndex( indexSuffix, shards, replicas, writeConsistency);
    }

    @Override
    public Health getEntityStoreHealth() {

        // could use any collection scope here, does not matter
        EntityCollectionManager ecm = managerCache.getEntityCollectionManager(
            new ApplicationScopeImpl( new SimpleId( CpNamingUtils.MANAGEMENT_APPLICATION_ID, "application" ) ) );

        return ecm.getHealth();
    }


    @Override
    public UUID createApplication(String organizationName, String name) throws Exception {
        throw new UnsupportedOperationException("Not supported in v2");
    }


    @Override
    public UUID createApplication(
        String organizationName, String name, Map<String, Object> properties) throws Exception {
        throw new UnsupportedOperationException("Not supported in v2");
    }

    @Override
    public UUID initializeApplication(
        String orgName, UUID appId, String appName, Map<String, Object> props) throws Exception {
        throw new UnsupportedOperationException("Not supported in v2");
    }


    @Override
    public Health getIndexHealth() {
        return entityIndex.getIndexHealth();
    }
}
