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


import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import org.apache.commons.lang.StringUtils;

import org.apache.usergrid.corepersistence.asyncevents.AsyncEventService;
import org.apache.usergrid.corepersistence.index.ReIndexRequestBuilder;
import org.apache.usergrid.corepersistence.index.ReIndexService;
import org.apache.usergrid.corepersistence.service.CollectionService;
import org.apache.usergrid.corepersistence.service.ConnectionService;
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.exception.ConflictException;
import org.apache.usergrid.persistence.AbstractEntity;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityFactory;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.EntityManagerFactory;
import org.apache.usergrid.persistence.EntityRef;
import org.apache.usergrid.persistence.Query;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.persistence.SimpleEntityRef;
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
import org.apache.usergrid.persistence.graph.GraphManagerFactory;
import org.apache.usergrid.persistence.graph.SearchByEdgeType;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchByEdgeType;
import org.apache.usergrid.persistence.index.EntityIndex;
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

import static org.apache.usergrid.persistence.Schema.PROPERTY_NAME;
import static org.apache.usergrid.persistence.Schema.TYPE_APPLICATION;


/**
 * Implement good-old Usergrid EntityManagerFactory with the new-fangled Core Persistence API.
 * This is where we keep track of applications and system properties.
 */
public class CpEntityManagerFactory implements EntityManagerFactory, ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger( CpEntityManagerFactory.class );
    private final EntityManagerFig entityManagerFig;

    private ApplicationContext applicationContext;

    private Setup setup = null;

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
    private final MetricsFactory metricsFactory;
    private final AsyncEventService indexService;
    private final CollectionService collectionService;
    private final ConnectionService connectionService;
    private final GraphManagerFactory graphManagerFactory;

    public CpEntityManagerFactory( final CassandraService cassandraService, final CounterUtils counterUtils,
                                   final Injector injector ) {

        this.cassandraService = cassandraService;
        this.counterUtils = counterUtils;
        this.injector = injector;
        this.reIndexService = injector.getInstance(ReIndexService.class);
        this.entityManagerFig = injector.getInstance(EntityManagerFig.class);
        this.managerCache = injector.getInstance( ManagerCache.class );
        this.metricsFactory = injector.getInstance( MetricsFactory.class );
        this.indexService = injector.getInstance( AsyncEventService.class );
        this.graphManagerFactory = injector.getInstance( GraphManagerFactory.class );
        this.collectionService = injector.getInstance( CollectionService.class );
        this.connectionService = injector.getInstance( ConnectionService.class );

        //this line always needs to be last due to the temporary cicular dependency until spring is removed
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
        indexService.queueInitializeApplicationIndex(CpNamingUtils.getApplicationScope(getManagementAppId()));

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
            metricsFactory, entityManagerFig, graphManagerFactory,  collectionService, connectionService, applicationId );

        return em;
    }

    @Override
    public Entity createApplicationV2(String organizationName, String name) throws Exception {
        return createApplicationV2( organizationName, name, null, null );
    }


    @Override
    public Entity createApplicationV2(
        String orgName, String name, UUID applicationId, Map<String, Object> properties) throws Exception {

        String appName = buildAppName( orgName, name );


        final Optional<UUID> appId = applicationIdCache.getApplicationId( appName );

        if ( appId.isPresent()) {
            throw new ApplicationAlreadyExistsException( name );
        }

        applicationId = applicationId==null ?  UUIDGenerator.newTimeUUID() : applicationId;

        logger.debug( "New application orgName {} orgAppName {} id {} ",
            new Object[] { orgName, name, applicationId.toString() } );

        return initializeApplicationV2( orgName, applicationId, appName, properties );
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
        EntityManager appEm = getEntityManager(applicationId);

        final String appName = buildAppName(organizationName, name);

        // check for pre-existing application

        if ( lookupApplication( appName ).isPresent()) {
            throw new ApplicationAlreadyExistsException( appName );
        }

        getSetup().setupApplicationKeyspace( applicationId, appName );
        appEm.initializeIndex();
        indexService.queueInitializeApplicationIndex(CpNamingUtils.getApplicationScope(applicationId));
        if ( properties == null ) {
            properties = new TreeMap<>( CASE_INSENSITIVE_ORDER);
        }
        properties.put( PROPERTY_NAME, appName );
        appEm.create(applicationId, TYPE_APPLICATION, properties);
        appEm.resetRoles();


        // create application info entity in the management app

        Map<String, Object> appInfoMap = new HashMap<String, Object>() {{
            put( PROPERTY_NAME, appName );
            put( "org", organizationName );
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

        migrateAppInfo(applicationId, CpNamingUtils.APPLICATION_INFO, CpNamingUtils.DELETED_APPLICATION_INFOS, CpNamingUtils.DELETED_APPLICATION_INFO).toBlocking()
            .lastOrDefault( null );
    }

    //TODO: return status for restore
    @Override
    public Entity restoreApplication(UUID applicationId) throws Exception {

        // get the deleted_application_info for the deleted app
        return (Entity) migrateAppInfo( applicationId, CpNamingUtils.DELETED_APPLICATION_INFO,
            CpNamingUtils.APPLICATION_INFOS , CpNamingUtils.APPLICATION_INFO ).lastOrDefault( null )
             .map( appInfo -> {

                 //start the index rebuild
                 final ReIndexRequestBuilder builder = reIndexService.getBuilder().withApplicationId( applicationId );
                 reIndexService.rebuildIndex( builder );

                 //load the entity
                 final EntityManager managementEm = getEntityManager( getManagementAppId() );
                 try {
                     return managementEm.get( new SimpleEntityRef( CpNamingUtils.APPLICATION_INFO, applicationId ) );
                 }
                 catch ( Exception e ) {
                     logger.error( "Failed to get entity", e );
                     throw new RuntimeException( e );
                 }
             } )
            .toBlocking().lastOrDefault(null);

    }

//    @Override


    /**
     * Migrate the application from one type to another.  Used in delete and restore
     * @param applicationUUID The applicationUUID
     * @param deleteTypeName The type to use on the delete
     * @param createCollectionName The name of the collection to write the entity into
     * @param createTypeName The type to use on the create
     * @return
     * @throws Exception
     */
    private Observable migrateAppInfo(final UUID applicationUUID,  final String deleteTypeName, final String createCollectionName, final String createTypeName ) throws Exception {

        final ApplicationScope managementAppScope = CpNamingUtils.getApplicationScope(CpNamingUtils.MANAGEMENT_APPLICATION_ID);
        final EntityManager managementEm = getEntityManager(CpNamingUtils.MANAGEMENT_APPLICATION_ID);

        //the application id we will be removing
        final Id deleteApplicationId = new SimpleId(applicationUUID, deleteTypeName );

        //the application id we'll be creating
        final Id createApplicationId = new SimpleId( applicationUUID, createTypeName );

        //the application scope of the deleted app to clean it's index
        final ApplicationScope deleteApplicationScope = new ApplicationScopeImpl(deleteApplicationId);

        Entity oldAppEntity = managementEm.get(new SimpleEntityRef( deleteTypeName, applicationUUID));

        if(oldAppEntity == null){
            throw new EntityNotFoundException( String.format("Could not find application with UUID '%s'", applicationUUID) );
        }


        // ensure that there is not already a deleted app with the same name

        final EntityRef alias = managementEm.getAlias( createCollectionName, oldAppEntity.getName() );
        if ( alias != null ) {
            throw new ConflictException( "Cannot delete app with same name as already deleted app" );
        }
        // make a copy of the app to delete application_info entity
        // and put it in a deleted_application_info collection

        final Entity newAppEntity =
            managementEm.create( new SimpleId( applicationUUID, createTypeName ), oldAppEntity.getProperties() );

        // copy its connections too

        final Set<String> connectionTypes = managementEm.getConnectionTypes( oldAppEntity );
        Observable copyConnections = Observable.from( connectionTypes ).doOnNext( connType -> {
            try {
                final Results connResults =
                    managementEm.getTargetEntities( oldAppEntity, connType, null, Query.Level.ALL_PROPERTIES );
                connResults.getEntities().forEach( entity -> {
                    try {
                        managementEm.createConnection( newAppEntity, connType, entity );
                    }
                    catch ( Exception e ) {
                        throw new RuntimeException( e );
                    }
                } );
            }
            catch ( Exception e ) {
                throw new RuntimeException( e );
            }
        } );

        final Id managementAppId = CpNamingUtils.getManagementApplicationId();
        final EntityIndex aei = getManagementIndex();
        final GraphManager managementGraphManager = managerCache.getGraphManager(managementAppScope);
        final Edge createEdge = CpNamingUtils.createCollectionEdge(managementAppId, createCollectionName, createApplicationId);

        final Observable createNodeGraph = managementGraphManager.writeEdge(createEdge);

        final Observable deleteAppFromIndex = aei.deleteApplication();

        return Observable
            .merge( copyConnections, createNodeGraph, deleteAppFromIndex )
            .doOnCompleted( () -> {
                try {
                    if ( oldAppEntity != null ) {
                        managementEm.delete( oldAppEntity );
                        applicationIdCache.evictAppId( oldAppEntity.getName() );
                    }
                    EntityIndex ei = getManagementIndex();
                    ei.refreshAsync().toBlocking().last();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

            } );
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
        return getApplications( CpNamingUtils.getEdgeTypeFromCollectionName( CpNamingUtils.APPLICATION_INFOS ) );
    }


    @Override
    public Map<String, UUID> getDeletedApplications() throws Exception {
        return getApplications( CpNamingUtils.getEdgeTypeFromCollectionName( CpNamingUtils.DELETED_APPLICATION_INFOS ) );
    }


    private Map<String, UUID> getApplications(final String edgeType) throws Exception {

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



        logger.debug("getApplications(): Loading edges of edgeType {} from {}:{}",
            new Object[]{edgeType, managementId.getType(), managementId.getUuid()});

        Observable<Edge> edges = gm.loadEdgesFromSource( new SimpleSearchByEdgeType(
                managementId, edgeType, Long.MAX_VALUE,
                SearchByEdgeType.Order.DESCENDING, Optional.<Edge>absent() ));

        final EntityCollectionManager ecm = managerCache.getEntityCollectionManager( appScope );

        //buffer our edges and batch fetch the app infos for faster I/O
        return edges.map( edge -> {
            return edge.getTargetNode();
        } ).buffer( 100 ).flatMap( entityIds -> {
            return ecm.load( entityIds );
        } )
                    .flatMap( entitySet -> Observable.from( entitySet.getEntities() ) )
            //collect all the app infos into a single map for return
                    .collect( () -> new HashMap<String, UUID>(), ( appMap, entity ) -> {

                            if ( !entity.getEntity().isPresent() ) {
                                return;
                            }

                            final org.apache.usergrid.persistence.model.entity.Entity entityData =
                                entity.getEntity().get();

                            final UUID applicationId = entity.getId().getUuid();
                            final String applicationName = ( String ) entityData.getField( PROPERTY_NAME ).getValue();

                            appMap.put( applicationName , applicationId );
                        } ).toBlocking().last();
    }


    @Override
    public void setup() throws Exception {
        getSetup().initSubsystems();
    }


    @Override
    public void boostrap() throws Exception {

        //we want to make sure our keyspaces exist
        setup();
        //create the defautl applications
        getSetup().createDefaultApplications();
        //init any other data we need
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
    public EntityIndex.IndexRefreshCommandInfo refreshIndex(UUID applicationId) {
        return getEntityManager(applicationId).refreshIndex();
    }



    private EntityIndex getManagementIndex() {

        return
            managerCache.getEntityIndex( // management app
                CpNamingUtils.getApplicationScope(getManagementAppId()));
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
    public Health getEntityStoreHealth() {

        // could use any collection scope here, does not matter
        EntityCollectionManager ecm = managerCache.getEntityCollectionManager(
            new ApplicationScopeImpl( new SimpleId( CpNamingUtils.MANAGEMENT_APPLICATION_ID, "application" ) ) );

        return ecm.getHealth();
    }



    @Override
    public Health getIndexHealth() {

       return getManagementIndex().getIndexHealth();
    }

    @Override
    public void initializeManagementIndex(){
        getManagementIndex().initialize();
    }
}
