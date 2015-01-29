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
import com.yammer.metrics.annotation.Metered;
import static java.lang.String.CASE_INSENSITIVE_ORDER;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang.StringUtils;

import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.persistence.AbstractEntity;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityFactory;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.EntityManagerFactory;
import org.apache.usergrid.persistence.EntityRef;
import org.apache.usergrid.persistence.Results;
import static org.apache.usergrid.persistence.Schema.PROPERTY_NAME;
import static org.apache.usergrid.persistence.Schema.TYPE_APPLICATION;
import org.apache.usergrid.persistence.cassandra.CassandraService;
import org.apache.usergrid.persistence.cassandra.CounterUtils;
import org.apache.usergrid.persistence.cassandra.Setup;
import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.collection.impl.CollectionScopeImpl;
import org.apache.usergrid.persistence.core.migration.data.DataMigrationManager;
import org.apache.usergrid.persistence.core.rx.AllEntitiesInSystemObservable;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.core.util.Health;
import org.apache.usergrid.persistence.entities.Application;
import org.apache.usergrid.persistence.exceptions.ApplicationAlreadyExistsException;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.GraphManager;
import org.apache.usergrid.persistence.graph.SearchByEdgeType;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchByEdgeType;
import org.apache.usergrid.persistence.index.EntityIndex;
import org.apache.usergrid.persistence.index.query.Query;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.utils.UUIDUtils;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import rx.Observable;


/**
 * Implement good-old Usergrid EntityManagerFactory with the new-fangled Core Persistence API.
 * This is where we keep track of applications and system properties.
 */
public class CpEntityManagerFactory implements EntityManagerFactory, ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger( CpEntityManagerFactory.class );

    public static String IMPLEMENTATION_DESCRIPTION = "Core Persistence Entity Manager Factory 1.0";

    private ApplicationContext applicationContext;

    private Setup setup = null;

    /** Have we already initialized the index for the management app? */
    private AtomicBoolean indexInitialized = new AtomicBoolean(  );

    /** Keep track of applications that already have indexes to avoid redundant re-creation. */
    private static final Set<UUID> applicationIndexesCreated = new HashSet<UUID>();


    // cache of already instantiated entity managers
    private LoadingCache<UUID, EntityManager> entityManagers
        = CacheBuilder.newBuilder().maximumSize(100).build(new CacheLoader<UUID, EntityManager>() {
            public EntityManager load(UUID appId) { // no checked exception
                return _getEntityManager(appId);
            }
        });

    private ManagerCache managerCache;

    private AllEntitiesInSystemObservable allEntitiesInSystemObservable;

    private DataMigrationManager dataMigrationManager;

    CassandraService cass;
    CounterUtils counterUtils;


    public CpEntityManagerFactory(
            CassandraService cass, CounterUtils counterUtils) {

        this.cass = cass;
        this.counterUtils = counterUtils;
    }


    private void init() {

        EntityManager em = getEntityManager( CpNamingUtils.SYSTEM_APP_ID);

        try {
            if ( em.getApplication() == null ) {
                logger.info("Creating system application");
                Map sysAppProps = new HashMap<String, Object>();
                sysAppProps.put(PROPERTY_NAME, "systemapp");
                em.create(CpNamingUtils.SYSTEM_APP_ID, TYPE_APPLICATION, sysAppProps);
                em.getApplication();
                em.createIndex();
                em.refreshIndex();
            }

        } catch (Exception ex) {
            throw new RuntimeException("Fatal error creating system application", ex);
        }
    }


    public ManagerCache getManagerCache() {

        if ( managerCache == null ) {

            // TODO: better solution for getting injector?
            Injector injector = CpSetup.getInjector();

            managerCache = injector.getInstance( ManagerCache.class );

            dataMigrationManager = injector.getInstance( DataMigrationManager.class );
        }
        return managerCache;
    }

    private AllEntitiesInSystemObservable getAllEntitiesObservable(){
        if(allEntitiesInSystemObservable==null)
            allEntitiesInSystemObservable = CpSetup.getInjector().getInstance(AllEntitiesInSystemObservable.class);
        return allEntitiesInSystemObservable;
    }


    @Override
    public String getImplementationDescription() throws Exception {
        return IMPLEMENTATION_DESCRIPTION;
    }


    @Override
    public EntityManager getEntityManager(UUID applicationId) {
        try {
            return entityManagers.get( applicationId );
        }
        catch ( Exception ex ) {
            logger.error("Error getting entity manager", ex);
        }
        return _getEntityManager( applicationId );
    }


    private EntityManager _getEntityManager( UUID applicationId ) {

        EntityManager em = new CpEntityManager();
        em.init( this, applicationId );

        // only need to do this once
        if ( !applicationIndexesCreated.contains( applicationId ) ) {
            em.createIndex();
            applicationIndexesCreated.add( applicationId );
        }

        return em;
    }


    @Override
    public UUID createApplication(String organizationName, String name) throws Exception {
        return createApplication( organizationName, name, null );
    }


    @Override
    public UUID createApplication(
        String orgName, String name, Map<String, Object> properties) throws Exception {

        String appName = buildAppName( orgName, name );

        UUID applicationId = lookupApplication( appName );

        if ( applicationId != null ) {
            throw new ApplicationAlreadyExistsException( name );
        }

        applicationId = UUIDGenerator.newTimeUUID();

        logger.debug( "New application orgName {} name {} id {} ",
                new Object[] { orgName, name, applicationId.toString() } );

        initializeApplication( orgName, applicationId, appName, properties );
        return applicationId;
    }


    private String buildAppName( String organizationName, String name ) {
        return StringUtils.lowerCase( name.contains( "/" ) ? name : organizationName + "/" + name );
    }


    @Override
    public UUID initializeApplication( String organizationName, UUID applicationId, String name,
                                       Map<String, Object> properties ) throws Exception {


        EntityManager em = getEntityManager( CpNamingUtils.SYSTEM_APP_ID);

        final String appName = buildAppName( organizationName, name );

        // check for pre-existing application
        if ( lookupApplication( appName ) != null ) {
            throw new ApplicationAlreadyExistsException( appName );
        }

        getSetup().setupApplicationKeyspace( applicationId, appName );

        UUID orgUuid = lookupOrganization( organizationName );
        if ( orgUuid == null ) {

            // create new org because the specified one does not exist
            final String orgName = organizationName;
            Entity orgInfo = em.create("organization", new HashMap<String, Object>() {{
                put( PROPERTY_NAME, orgName );
            }});
            em.refreshIndex();
            orgUuid = orgInfo.getUuid();
        }

        // create appinfo entry in the system app
        final UUID appId = applicationId;
        final UUID orgId = orgUuid;
        Map<String, Object> appInfoMap = new HashMap<String, Object>() {{
            put( PROPERTY_NAME, appName );
            put( "applicationUuid", appId );
            put( "organizationUuid", orgId );
        }};
        Entity appInfo = em.create( "appinfo", appInfoMap );
        em.refreshIndex();

        // create application entity
        if ( properties == null ) {
            properties = new TreeMap<String, Object>( CASE_INSENSITIVE_ORDER );
        }
        properties.put( PROPERTY_NAME, appName );
        EntityManager appEm = getEntityManager( applicationId );

        appEm.create( applicationId, TYPE_APPLICATION, properties );
        appEm.resetRoles();
        appEm.refreshIndex();

        logger.info("Initialized application {}", appName );
        return applicationId;
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

        // remove old appinfo Entity, which is in the System App's appinfos collection
        EntityManager em = getEntityManager(CpNamingUtils.SYSTEM_APP_ID);
        Query q = Query.fromQL(String.format("select * where applicationUuid = '%s'", applicationId.toString()));
        Results results = em.searchCollection( em.getApplicationRef(), "appinfos", q);
        Entity appToDelete = results.getEntity();
        em.delete( appToDelete );

        // create new Entity in deleted_appinfos collection, with same UUID and properties as deleted appinfo
        em.create( "deleted_appinfo", appToDelete.getProperties() );

        em.refreshIndex();

        // delete the application's index
        EntityIndex ei = managerCache.getEntityIndex(new ApplicationScopeImpl(
            new SimpleId(applicationId, TYPE_APPLICATION)));
        ei.deleteIndex();
    }


    @Override
    public UUID importApplication(
            String organization, UUID applicationId,
            String name, Map<String, Object> properties) throws Exception {

        throw new UnsupportedOperationException("Not supported yet.");
    }


    public UUID lookupOrganization( String name ) throws Exception {
        init();


        //        Query q = Query.fromQL(PROPERTY_NAME + " = '" + name + "'");
        EntityManager em = getEntityManager( CpNamingUtils.SYSTEM_APP_ID );


        final EntityRef alias = em.getAlias( "organizations", name );

        if ( alias == null ) {
            return null;
        }

        final Entity entity = em.get( alias );

        if ( entity == null ) {
            return null;
        }

        return entity.getUuid();
        //        Results results = em.searchCollection( em.getApplicationRef(), "organizations", q );
        //
        //        if ( results.isEmpty() ) {
        //            return null;
        //        }
        //
        //        return results.iterator().next().getUuid();
    }


    @Override
    public UUID lookupApplication( String name ) throws Exception {
        init();

        EntityManager em = getEntityManager( CpNamingUtils.SYSTEM_APP_ID );


        final EntityRef alias = em.getAlias( CpNamingUtils.APPINFOS, name );

        if ( alias == null ) {
            return null;
        }

        final Entity entity = em.get( alias );

        if ( entity == null ) {
            return null;
        }


        final UUID property = ( UUID ) entity.getProperty( "applicationUuid" );

        return property;
    }


    @Override
    @Metered(group = "core", name = "EntityManagerFactory_getApplication")
    public Map<String, UUID> getApplications() throws Exception {

        Map<String, UUID> appMap = new HashMap<String, UUID>();

        ApplicationScope appScope = CpNamingUtils.getApplicationScope( CpNamingUtils.SYSTEM_APP_ID );
        GraphManager gm = managerCache.getGraphManager(appScope);

        EntityManager em = getEntityManager( CpNamingUtils.SYSTEM_APP_ID);
        Application app = em.getApplication();
        Id fromEntityId = new SimpleId( app.getUuid(), app.getType() );

        String edgeType = CpNamingUtils.getEdgeTypeFromCollectionName( CpNamingUtils.APPINFOS );

        logger.debug("getApplications(): Loading edges of edgeType {} from {}:{}",
            new Object[] { edgeType, fromEntityId.getType(), fromEntityId.getUuid() } );

        Observable<Edge> edges = gm.loadEdgesFromSource( new SimpleSearchByEdgeType(
                fromEntityId, edgeType, Long.MAX_VALUE,
                SearchByEdgeType.Order.DESCENDING, null ));

        Iterator<Edge> iter = edges.toBlockingObservable().getIterator();
        while ( iter.hasNext() ) {

            Edge edge = iter.next();
            Id targetId = edge.getTargetNode();

            logger.debug("getApplications(): Processing edge from {}:{} to {}:{}", new Object[] {
                edge.getSourceNode().getType(), edge.getSourceNode().getUuid(),
                edge.getTargetNode().getType(), edge.getTargetNode().getUuid()
            });

            CollectionScope collScope = new CollectionScopeImpl(
                    appScope.getApplication(),
                    appScope.getApplication(),
                    CpNamingUtils.getCollectionScopeNameFromCollectionName( CpNamingUtils.APPINFOS ));

            org.apache.usergrid.persistence.model.entity.Entity e =
                    managerCache.getEntityCollectionManager( collScope ).load( targetId )
                        .toBlockingObservable().lastOrDefault(null);

            if ( e == null ) {
                logger.warn("Applicaion {} in index but not found in collections", targetId );
                continue;
            }

            appMap.put(
                (String)e.getField( PROPERTY_NAME ).getValue(),
                (UUID)e.getField( "applicationUuid" ).getValue());
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

        EntityManager em = getEntityManager( CpNamingUtils.SYSTEM_APP_ID);
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

        EntityManager em = getEntityManager( CpNamingUtils.SYSTEM_APP_ID);
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

        EntityManager em = getEntityManager( CpNamingUtils.SYSTEM_APP_ID);


        Query q = Query.fromQL("select *");
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
            logger.error("Error deleting service property name: " + name, ex);
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
        try {
            setup();
        } catch (Exception ex) {
            logger.error("Error setting up EMF", ex);
        }
    }


    @Override
    public long performEntityCount() {
        //TODO, this really needs to be a task that writes this data somewhere since this will get
        //progressively slower as the system expands
        return getAllEntitiesObservable().getAllEntitiesInSystem(1000).longCount().toBlocking().last();
    }


    /**
     * @param managerCache the managerCache to set
     */
    public void setManagerCache(CpManagerCache managerCache) {
        this.managerCache = managerCache;
    }


    @Override
    public UUID getManagementAppId() {
        return CpNamingUtils.MANAGEMENT_APPLICATION_ID;
    }


    @Override
    public UUID getDefaultAppId() {
        return CpNamingUtils.DEFAULT_APPLICATION_ID;
    }




    /**
     * Gets the setup.
     * @return Setup helper
     */
    public Setup getSetup() {
        if ( setup == null ) {
            setup = new CpSetup( this, cass );
        }
        return setup;
    }


    /**
     * TODO, these 3 methods are super janky.  During refactoring we should clean this model up
     */
    public void refreshIndex() {

        // refresh special indexes without calling EntityManager refresh because stack overflow
        maybeCreateIndexes();
        // system app

        for ( EntityIndex index : getManagementIndexes() ) {
            index.refresh();
        }
    }


    private void maybeCreateIndexes() {
        // system app
        if ( indexInitialized.getAndSet( true ) ) {
            return;
        }

        for ( EntityIndex index : getManagementIndexes() ) {
            index.initializeIndex();
        }
    }


    private List<EntityIndex> getManagementIndexes() {

        return Arrays.asList(
            getManagerCache().getEntityIndex(
                new ApplicationScopeImpl( new SimpleId( CpNamingUtils.SYSTEM_APP_ID, "application" ))),

            // management app
            getManagerCache().getEntityIndex(
                new ApplicationScopeImpl( new SimpleId( getManagementAppId(), "application" ))),

            // default app TODO: do we need this in two-dot-o
            getManagerCache().getEntityIndex(
                new ApplicationScopeImpl( new SimpleId( getDefaultAppId(), "application" ))));
    }


    public void rebuildAllIndexes( ProgressObserver po ) throws Exception {

        logger.info("\n\nRebuilding all indexes\n");

        rebuildInternalIndexes( po );

        Map<String, UUID> appMap = getApplications();

        logger.info("About to rebuild indexes for {} applications", appMap.keySet().size());

        for ( UUID appUuid : appMap.values() ) {
            rebuildApplicationIndexes( appUuid, po );
        }
    }


    @Override
    public void rebuildInternalIndexes( ProgressObserver po ) throws Exception {
        rebuildApplicationIndexes( CpNamingUtils.SYSTEM_APP_ID, po);
        rebuildApplicationIndexes( CpNamingUtils.MANAGEMENT_APPLICATION_ID, po );
        rebuildApplicationIndexes( CpNamingUtils.DEFAULT_APPLICATION_ID, po );
    }


    @Override
    public void rebuildApplicationIndexes( UUID appId, ProgressObserver po ) throws Exception {

        EntityManager em = getEntityManager( appId );

        //explicitly invoke create index, we don't know if it exists or not in ES during a rebuild.
        em.createIndex();
        em.reindex(po);
        
        Application app = em.getApplication();
        logger.info("\n\nRebuilt index for application {} id {}\n", app.getName(), appId );
    }


    @Override
    public void migrateData() throws Exception {
         dataMigrationManager.migrate();
    }


    @Override
    public String getMigrateDataStatus() {
        return dataMigrationManager.getLastStatus();
    }


    @Override
    public int getMigrateDataVersion() {
        return dataMigrationManager.getCurrentVersion();
    }


    @Override
    public void setMigrationVersion( final int version ) {
        dataMigrationManager.resetToVersion( version );
        dataMigrationManager.invalidate();
    }


    @Override
    public void flushEntityManagerCaches() {
        Map<UUID, EntityManager>  entityManagersMap = entityManagers.asMap();
        for ( UUID appUuid : entityManagersMap.keySet() ) {
            EntityManager em = entityManagersMap.get(appUuid);
            em.flushManagerCaches();
        }
    }

    @Override
    public void rebuildCollectionIndex(UUID appId, String collection, ProgressObserver po ) {
        throw new UnsupportedOperationException( "Not supported yet." );
    }

    @Override
    public void addIndex(final UUID applicationId,final String indexSuffix,final int shards,final int replicas){
        EntityIndex entityIndex = getManagerCache().getEntityIndex(CpNamingUtils.getApplicationScope(applicationId));
        entityIndex.addIndex(indexSuffix, shards, replicas);
    }

    @Override
    public Health getEntityStoreHealth() {

        // could use any collection scope here, does not matter
        EntityCollectionManager ecm = getManagerCache().getEntityCollectionManager(
            new CollectionScopeImpl(
                new SimpleId( CpNamingUtils.SYSTEM_APP_ID, "application"),
                new SimpleId( CpNamingUtils.SYSTEM_APP_ID, "application"),
                "dummy"
        ));

        return ecm.getHealth();
    }
}
