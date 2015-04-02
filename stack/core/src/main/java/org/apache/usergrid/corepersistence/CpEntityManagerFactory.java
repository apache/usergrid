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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import org.apache.commons.lang.StringUtils;
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.exception.ConflictException;
import org.apache.usergrid.persistence.*;
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
import org.apache.usergrid.persistence.exceptions.EntityNotFoundException;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.GraphManager;
import org.apache.usergrid.persistence.graph.SearchByEdgeType;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchByEdgeType;
import org.apache.usergrid.persistence.index.ApplicationEntityIndex;
import org.apache.usergrid.persistence.index.EntityIndex;
import org.apache.usergrid.persistence.index.EntityIndexFactory;
import org.apache.usergrid.persistence.index.query.Query;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;
import org.apache.usergrid.utils.UUIDUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import rx.Observable;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.String.CASE_INSENSITIVE_ORDER;
import static org.apache.usergrid.persistence.Schema.*;


/**
 * Implement good-old Usergrid EntityManagerFactory with the new-fangled Core Persistence API.
 * This is where we keep track of applications and system properties.
 */
public class CpEntityManagerFactory implements EntityManagerFactory, ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger( CpEntityManagerFactory.class );
    private final EntityIndexFactory entityIndexFactory;

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
    private final EntityIndex entityIndex;
    private final MetricsFactory metricsFactory;

    public CpEntityManagerFactory(
            final CassandraService cassandraService, final CounterUtils counterUtils, final Injector injector) {

        this.cassandraService = cassandraService;
        this.counterUtils = counterUtils;
        this.injector = injector;
        this.entityIndex = injector.getInstance(EntityIndex.class);
        this.entityIndexFactory = injector.getInstance(EntityIndexFactory.class);
        this.managerCache = injector.getInstance( ManagerCache.class );
        this.metricsFactory = injector.getInstance( MetricsFactory.class );
        this.applicationIdCache = new ApplicationIdCacheImpl( this );
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

            entityIndex.refresh();

        } catch (Exception ex) {
            throw new RuntimeException("Fatal error creating system application", ex);
        }
    }


    public ManagerCache getManagerCache() {

        if ( managerCache == null ) {
            managerCache = injector.getInstance( ManagerCache.class );
        }
        return managerCache;
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

        EntityManager em = new CpEntityManager();
        em.init( this ,applicationId );

        return em;
    }

    public MetricsFactory getMetricsFactory(){
        return metricsFactory;
    }

    @Override
    public Entity createApplicationV2(String organizationName, String name) throws Exception {
        return createApplicationV2( organizationName, name, null );
    }


    @Override
    public Entity createApplicationV2(
        String orgName, String name, Map<String, Object> properties) throws Exception {

        String appName = buildAppName( orgName, name );


        final UUID appId = applicationIdCache.getApplicationId( appName );

        if ( appId != null ) {
            throw new ApplicationAlreadyExistsException( name );
        }

        UUID applicationId = UUIDGenerator.newTimeUUID();

        logger.debug( "New application orgName {} orgAppName {} id {} ",
            new Object[] { orgName, name, applicationId.toString() } );

        return initializeApplicationV2(orgName, applicationId, appName, properties);
    }



    private String buildAppName( String organizationName, String name ) {
        return StringUtils.lowerCase( name.contains( "/" ) ? name : organizationName + "/" + name );
    }


    /**
     * @return UUID of newly created Entity of type application_info
     */
    @Override
    public Entity initializeApplicationV2( String organizationName, final UUID applicationId, String name,
                                       Map<String, Object> properties ) throws Exception {

        // Ensure our management system exists before creating our application
        init();

        EntityManager em = getEntityManager( CpNamingUtils.MANAGEMENT_APPLICATION_ID );

        final String appName = buildAppName( organizationName, name );

        // check for pre-existing application

        if ( lookupApplication( appName ) != null ) {
            throw new ApplicationAlreadyExistsException( appName );
        }

        getSetup().setupApplicationKeyspace( applicationId, appName );

        if ( properties == null ) {
            properties = new TreeMap<>( CASE_INSENSITIVE_ORDER );
        }
        properties.put( PROPERTY_NAME, appName );
        EntityManager appEm = getEntityManager( applicationId);
        appEm.create(applicationId, TYPE_APPLICATION, properties);
        appEm.resetRoles();
        entityIndex.refresh();

        // create application info entity in the management app

        Map<String, Object> appInfoMap = new HashMap<String, Object>() {{
            put( PROPERTY_NAME, appName );
            put( PROPERTY_APPLICATION_ID, applicationId );
        }};

        Entity appInfo;
        try {
            appInfo = em.create(CpNamingUtils.APPLICATION_INFO, appInfoMap);
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

        final EntityManager em = getEntityManager(getManagementAppId());

        final Results results = em.searchCollection(em.getApplicationRef(), CpNamingUtils.APPLICATION_INFOS,
            Query.fromQL("select * where " + PROPERTY_APPLICATION_ID + " = " + applicationId.toString()));
        Entity appInfoToDelete = results.getEntity();

        // ensure that there is not already a deleted app with the same name

        final EntityRef alias = em.getAlias(
            CpNamingUtils.DELETED_APPLICATION_INFO, appInfoToDelete.getName());
        if ( alias != null ) {
            throw new ConflictException("Cannot delete app with same name as already deleted app");
        }

        // make a copy of the app to delete application_info entity
        // and put it in a deleted_application_info collection

        Entity deletedApp = em.create(
            CpNamingUtils.DELETED_APPLICATION_INFO, appInfoToDelete.getProperties());

        // copy its connections too

        final Set<String> connectionTypes = em.getConnectionTypes(appInfoToDelete);
        for ( String connType : connectionTypes ) {
            final Results connResults =
                em.getConnectedEntities(appInfoToDelete, connType, null, Query.Level.ALL_PROPERTIES);
            for ( Entity entity : connResults.getEntities() ) {
                em.createConnection( deletedApp, connType, entity );
            }
        }

        // delete the app from the application_info collection and delete its index

        em.delete(appInfoToDelete);

        final ApplicationEntityIndex entityIndex = managerCache.getEntityIndex(
            new ApplicationScopeImpl(new SimpleId(applicationId, TYPE_APPLICATION)));
        entityIndex.deleteApplication();

        applicationIdCache.evictAppId(appInfoToDelete.getName());
    }


    @Override
    public Entity restoreApplication(UUID applicationId) throws Exception {

        // get the deleted_application_info for the deleted app

        EntityManager em = getEntityManager(getManagementAppId());

        final Results results = em.searchCollection(
            em.getApplicationRef(), CpNamingUtils.DELETED_APPLICATION_INFOS,
            Query.fromQL("select * where " + PROPERTY_APPLICATION_ID + " = '" + applicationId.toString() + "'"));
        Entity deletedAppInfo = results.getEntity();

        if ( deletedAppInfo == null ) {
            throw new EntityNotFoundException("Cannot restore. Deleted Application not found: " + applicationId );
        }

        // create application_info for restored app

        Entity restoredAppInfo = em.create(
            deletedAppInfo.getUuid(), CpNamingUtils.APPLICATION_INFO, deletedAppInfo.getProperties());

        // copy connections from deleted app entity

        final Set<String> connectionTypes = em.getConnectionTypes(deletedAppInfo);
        for ( String connType : connectionTypes ) {
            final Results connResults =
                em.getConnectedEntities(deletedAppInfo, connType, null, Query.Level.ALL_PROPERTIES);
            for ( Entity entity : connResults.getEntities() ) {
                em.createConnection( restoredAppInfo, connType, entity );
            }
        }

        // delete the deleted app entity rebuild the app index

        em.delete(deletedAppInfo);
        entityIndex.refresh();

        this.rebuildApplicationIndexes(applicationId, new ProgressObserver() {
            @Override
            public void onProgress(EntityRef entity) {
                logger.info("Restored entity {}:{}", entity.getType(), entity.getUuid());
            }
        });

        return restoredAppInfo;
    }


    @Override
    public UUID importApplication(
            String organization, UUID applicationId,
            String name, Map<String, Object> properties) throws Exception {

        throw new UnsupportedOperationException("Not supported yet.");
    }


    public UUID lookupApplication( String orgAppName ) throws Exception {
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

        ApplicationScope appScope = CpNamingUtils.getApplicationScope(CpNamingUtils.SYSTEM_APP_ID);
        GraphManager gm = managerCache.getGraphManager(appScope);

        EntityManager em = getEntityManager(CpNamingUtils.SYSTEM_APP_ID);
        Application app = em.getApplication();
        if(app == null)
            throw new RuntimeException("System App "+CpNamingUtils.SYSTEM_APP_ID+" should never be null");
        Id fromEntityId = new SimpleId( app.getUuid(), app.getType() );

        final String edgeType;

        if ( deleted ) {
            edgeType = CpNamingUtils.getEdgeTypeFromCollectionName( CpNamingUtils.DELETED_APPLICATION_INFO );
        } else {
            edgeType = CpNamingUtils.getEdgeTypeFromCollectionName( CpNamingUtils.APPLICATION_INFOS );
        }

        logger.debug("getApplications(): Loading edges of edgeType {} from {}:{}",
            new Object[] { edgeType, fromEntityId.getType(), fromEntityId.getUuid() } );

        Observable<Edge> edges = gm.loadEdgesFromSource( new SimpleSearchByEdgeType(
                fromEntityId, edgeType, Long.MAX_VALUE,
                SearchByEdgeType.Order.DESCENDING, null ));

        // TODO This is wrong, and will result in OOM if there are too many applications.
        // This needs to stream properly with a buffer

        Iterator<Edge> iter = edges.toBlocking().getIterator();
        while ( iter.hasNext() ) {

            Edge edge = iter.next();
            Id targetId = edge.getTargetNode();

            logger.debug("getApplications(): Processing edge from {}:{} to {}:{}", new Object[] {
                edge.getSourceNode().getType(), edge.getSourceNode().getUuid(),
                edge.getTargetNode().getType(), edge.getTargetNode().getUuid()
            });



            org.apache.usergrid.persistence.model.entity.Entity appInfo =
                    managerCache.getEntityCollectionManager(  appScope ).load( targetId )
                        .toBlocking().lastOrDefault(null);

            if ( appInfo == null ) {
                logger.warn("Application {} in index but not found in collections", targetId );
                continue;
            }

            UUID applicationId = UUIDUtils.tryExtractUUID(
                appInfo.getField( PROPERTY_APPLICATION_ID ).getValue().toString() );

            appMap.put( (String)appInfo.getField( PROPERTY_NAME ).getValue(), applicationId);
        }

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
        Query q = Query.fromQL( "select *");
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
            propsEntity.setProperty( key, properties.get(key).toString() );
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
        return updateServiceProperties( new HashMap<String, String>() {{
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
    public void refreshIndex() {

        // refresh special indexes without calling EntityManager refresh because stack overflow
        maybeCreateIndexes();

        entityIndex.refresh();
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
                new ApplicationScopeImpl(new SimpleId(getManagementAppId(), "application"))));
    }


    public void rebuildAllIndexes( ProgressObserver po ) throws Exception {

        logger.info("\n\nRebuilding all indexes\n");

        rebuildInternalIndexes( po );

        Map<String, UUID> appMap = getApplications();

        logger.info("About to rebuild indexes for {} applications", appMap.keySet().size());

        for ( UUID appUuid : appMap.values() ) {
            try {
                rebuildApplicationIndexes(appUuid, po);
            } catch ( Exception e) {
                logger.error("Error rebuilding index for app " + appUuid + " continuing...", e );
            }
        }
    }


    @Override
    public void rebuildInternalIndexes( ProgressObserver po ) throws Exception {
        rebuildApplicationIndexes( CpNamingUtils.SYSTEM_APP_ID, po);
        rebuildApplicationIndexes( CpNamingUtils.MANAGEMENT_APPLICATION_ID, po );
    }


    @Override
    public void rebuildApplicationIndexes( UUID appId, ProgressObserver po ) throws Exception {

        EntityManager em = getEntityManager( appId );
        em.reindex( po );

        logger.info("\n\nRebuilt index for applicationId {} \n", appId);
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
    public void rebuildCollectionIndex(
        UUID appId, String collectionName, boolean reverse, ProgressObserver po ) throws Exception  {

        EntityManager em = getEntityManager( appId );

        //explicitly invoke create index, we don't know if it exists or not in ES during a rebuild.
        Application app = em.getApplication();

        em.reindexCollection(po, collectionName, reverse);

        logger.info("\n\nRebuilt index for application {} id {} collection {}\n",
            new Object[]{app.getName(), appId, collectionName});
    }

    @Override
    public void addIndex(final UUID applicationId,final String indexSuffix,final int shards,final int replicas, final String writeConsistency){
        entityIndex.addIndex( indexSuffix, shards, replicas, writeConsistency);
    }

    @Override
    public Health getEntityStoreHealth() {

        // could use any collection scope here, does not matter
        EntityCollectionManager ecm = getManagerCache().getEntityCollectionManager(
            new ApplicationScopeImpl( new SimpleId( CpNamingUtils.SYSTEM_APP_ID, "application" ) ) );

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
