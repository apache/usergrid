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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import org.apache.commons.lang.StringUtils;
import org.apache.usergrid.persistence.DynamicEntity;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.EntityManagerFactory;
import org.apache.usergrid.persistence.Results;
import static org.apache.usergrid.persistence.Schema.PROPERTY_CREATED;
import static org.apache.usergrid.persistence.Schema.PROPERTY_NAME;
import static org.apache.usergrid.persistence.Schema.PROPERTY_UUID;
import static org.apache.usergrid.persistence.Schema.TYPE_APPLICATION;
import org.apache.usergrid.persistence.cassandra.CassandraService;
import org.apache.usergrid.persistence.cassandra.CounterUtils;
import org.apache.usergrid.persistence.cassandra.Setup;
import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.collection.impl.CollectionScopeImpl;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.entities.Application;
import org.apache.usergrid.persistence.exceptions.ApplicationAlreadyExistsException;
import org.apache.usergrid.persistence.exceptions.DuplicateUniquePropertyExistsException;
import org.apache.usergrid.persistence.graph.GraphManager;
import org.apache.usergrid.persistence.graph.GraphManagerFactory;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchEdgeType;
import org.apache.usergrid.persistence.index.EntityIndex;
import org.apache.usergrid.persistence.index.EntityIndexFactory;
import org.apache.usergrid.persistence.index.IndexScope;
import org.apache.usergrid.persistence.index.query.CandidateResult;
import org.apache.usergrid.persistence.index.impl.IndexScopeImpl;
import org.apache.usergrid.persistence.index.query.CandidateResults;
import org.apache.usergrid.persistence.index.query.Query;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.field.Field;
import org.apache.usergrid.persistence.model.field.LongField;
import org.apache.usergrid.persistence.model.field.StringField;
import org.apache.usergrid.persistence.model.field.UUIDField;
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

    public static final Class<DynamicEntity> APPLICATION_ENTITY_CLASS = DynamicEntity.class;

    // The System Application where we store app and org metadata
    public static final UUID SYSTEM_APP_ID = 
            UUID.fromString("b6768a08-b5d5-11e3-a495-10ddb1de66c3");
    
    public static final  UUID MANAGEMENT_APPLICATION_ID = 
            UUID.fromString("b6768a08-b5d5-11e3-a495-11ddb1de66c8");

    public static final  UUID DEFAULT_APPLICATION_ID = 
            UUID.fromString("b6768a08-b5d5-11e3-a495-11ddb1de66c9");


    @Deprecated // use system app for these in future
    public static final String SYSTEM_APPS_TYPE = "zzzappszzz";

    @Deprecated 
    public static final String SYSTEM_ORGS_TYPE = "zzzorgszzz";
    
    @Deprecated 
    public static final String SYSTEM_PROPS_TYPE = "zzzpropszzz"; 

    @Deprecated // use system app for these in future
    private static final Id systemAppId = 
         new SimpleId( SYSTEM_APP_ID, SYSTEM_APPS_TYPE );
    
    @Deprecated 
    public static final CollectionScope SYSTEM_APPS_SCOPE = 
        new CollectionScopeImpl( systemAppId, systemAppId, SYSTEM_APPS_TYPE );

    @Deprecated 
    public static final IndexScope SYSTEM_APPS_INDEX_SCOPE = 
        new IndexScopeImpl( systemAppId, systemAppId,  SYSTEM_APPS_TYPE);

    @Deprecated 
    public static final CollectionScope SYSTEM_ORGS_SCOPE = 
        new CollectionScopeImpl( systemAppId, systemAppId,  SYSTEM_ORGS_TYPE);

    @Deprecated
    public static final IndexScope SYSTEM_ORGS_INDEX_SCOPE = 
        new IndexScopeImpl( systemAppId, systemAppId, SYSTEM_ORGS_TYPE);

    @Deprecated
    public static final CollectionScope SYSTEM_PROPS_SCOPE = 
        new CollectionScopeImpl( systemAppId, systemAppId, SYSTEM_PROPS_TYPE);

    @Deprecated
    public static final IndexScope SYSTEM_PROPS_INDEX_SCOPE = 
        new IndexScopeImpl( systemAppId, systemAppId, SYSTEM_PROPS_TYPE);


    // cache of already instantiated entity managers
    private LoadingCache<UUID, EntityManager> entityManagers
        = CacheBuilder.newBuilder().maximumSize(100).build(new CacheLoader<UUID, EntityManager>() {
            public EntityManager load(UUID appId) { // no checked exception
                return _getEntityManager(appId);
            }
        });


    private CpManagerCache managerCache;

    CassandraService cass;
    CounterUtils counterUtils;

    private boolean skipAggregateCounters;

    private static final int REBUILD_PAGE_SIZE = 100;


    public CpEntityManagerFactory(
            CassandraService cass, CounterUtils counterUtils, boolean skipAggregateCounters) {

        this.cass = cass;
        this.counterUtils = counterUtils;
        this.skipAggregateCounters = skipAggregateCounters;
        if (skipAggregateCounters) {
            logger.warn("NOTE: Counters have been disabled by configuration...");
        }

        // if system app does have apps, orgs and props then populate it
        try {
            EntityManager em = getEntityManager(SYSTEM_APP_ID);
            Results orgs = em.searchCollection(em.getApplicationRef(), "organizations", null);
            if (orgs.isEmpty()) {
                populateSystemAppsFromEs();
                populateSystemOrgsFromEs();
                populateSystemPropsFromEs();
            }

        } catch (Exception ex) {
            throw new RuntimeException("Fatal error migrating data", ex);
        }
    }
    

    public CpManagerCache getManagerCache() {

        if ( managerCache == null ) {

            // TODO: better solution for getting injector? 
            Injector injector = CpSetup.getInjector();

            EntityCollectionManagerFactory ecmf;
            EntityIndexFactory eif;
            GraphManagerFactory gmf;

            ecmf = injector.getInstance( EntityCollectionManagerFactory.class );
            eif = injector.getInstance( EntityIndexFactory.class );
            gmf = injector.getInstance( GraphManagerFactory.class );

            managerCache = new CpManagerCache( ecmf, eif, gmf );
        }
        return managerCache;
    }


    @Override
    public String getImpementationDescription() throws Exception {
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

        String appName = buildAppName( organizationName, name );

        // check for pre-existing application
        if ( lookupApplication( appName ) != null ) {
            throw new ApplicationAlreadyExistsException( appName );
        }

        getSetup().setupApplicationKeyspace( applicationId, appName );

        UUID orgUuid = lookupOrganization( organizationName );
        if ( orgUuid == null ) {
          
            // organization does not exist, create it.
            Entity orgInfoEntity = new Entity(generateOrgId( UUIDGenerator.newTimeUUID() ));

            orgUuid = orgInfoEntity.getId().getUuid();

            long timestamp = System.currentTimeMillis();
            orgInfoEntity.setField( new LongField( PROPERTY_CREATED, (long)(timestamp / 1000)));
            orgInfoEntity.setField( new StringField( PROPERTY_NAME, name ));
            orgInfoEntity.setField( new UUIDField( PROPERTY_UUID, orgUuid ));

            EntityCollectionManager ecm = getManagerCache()
                    .getEntityCollectionManager( SYSTEM_ORGS_SCOPE );
            EntityIndex eci = getManagerCache()
                    .getEntityIndex( SYSTEM_ORGS_INDEX_SCOPE );

            orgInfoEntity = ecm.write( orgInfoEntity ).toBlockingObservable().last();
            eci.index( orgInfoEntity );
            eci.refresh();
        }

        if ( properties == null ) {
            properties = new TreeMap<String, Object>( CASE_INSENSITIVE_ORDER );
        }
        properties.put( PROPERTY_NAME, appName );

        Entity appInfoEntity = new Entity( generateApplicationId( applicationId ));

        long timestamp = System.currentTimeMillis();
        appInfoEntity.setField( new LongField( PROPERTY_CREATED, (long)(timestamp / 1000)));
        appInfoEntity.setField( new StringField( PROPERTY_NAME, name ));
        appInfoEntity.setField( new UUIDField( "applicationUuid", applicationId ));
        appInfoEntity.setField( new UUIDField( "organizationUuid", orgUuid ));

        // create app in system app scope
        {
            EntityCollectionManager ecm = getManagerCache()
                    .getEntityCollectionManager(SYSTEM_APPS_SCOPE );
            EntityIndex eci = getManagerCache()
                    .getEntityIndex( SYSTEM_APPS_INDEX_SCOPE );

            appInfoEntity = ecm.write( appInfoEntity ).toBlockingObservable().last();
            eci.index( appInfoEntity );
            eci.refresh();
        }

        // create app in its own scope
        EntityManager em = getEntityManager( applicationId );
        em.create( applicationId, TYPE_APPLICATION, properties );
        em.resetRoles();
        em.refreshIndex();

        return applicationId;
    }


    public ApplicationScope getApplicationScope( UUID applicationId ) {

        // We can always generate a scope, it doesn't matter if  the application exists yet or not.

        final ApplicationScopeImpl scope = new ApplicationScopeImpl( generateApplicationId( applicationId ) );

        return scope;
    }
    

    @Override
    public UUID importApplication(
            String organization, UUID applicationId,
            String name, Map<String, Object> properties) throws Exception {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    
    public UUID lookupOrganization( String name) throws Exception {

        Query q = Query.fromQL(PROPERTY_NAME + " = '" + name + "'");

        EntityIndex ei = getManagerCache().getEntityIndex( SYSTEM_ORGS_INDEX_SCOPE );
        CandidateResults results = ei.search( q );

        if ( results.isEmpty() ) {
            return null; 
        } 

        return results.iterator().next().getId().getUuid();
    }


    @Override
    public UUID lookupApplication( String name) throws Exception {

        Query q = Query.fromQL( PROPERTY_NAME + " = '" + name + "'");

        EntityIndex ei = getManagerCache().getEntityIndex( SYSTEM_APPS_INDEX_SCOPE );
        
        CandidateResults results = ei.search( q );

        if ( results.isEmpty() ) {
            return null; 
        } 

        return results.iterator().next().getId().getUuid();
    }


    @Override
    @Metered(group = "core", name = "EntityManagerFactory_getApplication")
    public Map<String, UUID> getApplications() throws Exception {

        EntityCollectionManager em = getManagerCache()
                .getEntityCollectionManager(SYSTEM_APPS_SCOPE );
        EntityIndex ei = getManagerCache()
                .getEntityIndex( SYSTEM_APPS_INDEX_SCOPE );

        Map<String, UUID> appMap = new HashMap<String, UUID>();

        String cursor = null;
        boolean done = false;

        while ( !done ) {

            Query q = Query.fromQL("select *");
            q.setCursor( cursor );

            CandidateResults results = ei.search( q );
            cursor = results.getCursor();

            Iterator<CandidateResult> iter = results.iterator();
            while ( iter.hasNext() ) {

                CandidateResult cr = iter.next();
                Entity e = em.load( cr.getId() ).toBlockingObservable().last();

                if ( cr.getVersion().compareTo( e.getVersion()) < 0 )  {
                    logger.debug("Stale version of Entity uuid:{} type:{}, stale v:{}, latest v:{}", 
                        new Object[] { cr.getId().getUuid(), cr.getId().getType(), 
                            cr.getVersion(), e.getVersion()});
                    continue;
                }
                
                appMap.put( 
                    (String)e.getField(PROPERTY_NAME).getValue(), 
                    (UUID)e.getField("applicationUuid").getValue() );
            }

            if ( cursor == null ) {
                done = true;
            }
        }

        return appMap;
    }

    
    @Override
    public void setup() throws Exception {
        getSetup().init();
    }

    
    @Override
    public Map<String, String> getServiceProperties() {

        EntityIndex ei = getManagerCache()
                .getEntityIndex( SYSTEM_PROPS_INDEX_SCOPE );
        EntityCollectionManager em = getManagerCache()
                .getEntityCollectionManager( SYSTEM_PROPS_SCOPE );

        Query q = Query.fromQL("select *");

        CandidateResults results = ei.search( q );

        if ( results.isEmpty() ) {
            return new HashMap<String,String>();
        }

        CandidateResult cr = results.iterator().next();
        Entity propsEntity = em.load( cr.getId() ).toBlockingObservable().last();

        Map<String, String> props = new HashMap<String, String>();

        // intentionally going only one-level deep into fields and treating all 
        // values as strings because that is all we need for service properties.
        for ( Field f : propsEntity.getFields() ) {
            props.put( f.getName(), f.getValue().toString() ); 
        }

        return props;
    }

    
    @Override
    public boolean updateServiceProperties(Map<String, String> properties) {

        EntityCollectionManager em = getManagerCache()
            .getEntityCollectionManager( SYSTEM_PROPS_SCOPE );
        EntityIndex ei = getManagerCache()
            .getEntityIndex( SYSTEM_PROPS_INDEX_SCOPE );

        Query q = Query.fromQL("select *");
        CandidateResults results = ei.search( q );
        Entity propsEntity;
        if ( !results.isEmpty() ) {
            propsEntity = em.load( results.iterator().next().getId()).toBlockingObservable().last();
        } else {
            propsEntity = new Entity( new SimpleId( "properties" ));
            long timestamp = System.currentTimeMillis();
            propsEntity.setField( new LongField( PROPERTY_CREATED, (long)(timestamp / 1000)));
        }

        // intentionally going only one-level deep into fields and treating all 
        // values as strings because that is all we need for service properties
        for ( String key : properties.keySet() ) {
            propsEntity.setField( new StringField(key, properties.get(key)) );
        }

        propsEntity = em.write( propsEntity ).toBlockingObservable().last();
        ei.index( propsEntity );    

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

        EntityCollectionManager em = getManagerCache().getEntityCollectionManager( SYSTEM_PROPS_SCOPE );
        EntityIndex ei = getManagerCache().getEntityIndex( SYSTEM_PROPS_INDEX_SCOPE );

        Query q = Query.fromQL("select *");
        CandidateResults results = ei.search( q );

        Entity propsEntity = em.load( 
                results.iterator().next().getId() ).toBlockingObservable().last();

        if ( propsEntity == null ) {
            return false; // nothing to delete
        }

        if ( propsEntity.getField(name) == null ) {
            return false; // no such field
        }

        propsEntity.removeField( name );

        propsEntity = em.write( propsEntity ).toBlockingObservable().last();
        ei.index( propsEntity );    

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

    /**
     * @param managerCache the managerCache to set
     */
    public void setManagerCache(CpManagerCache managerCache) {
        this.managerCache = managerCache;
    }


    @Override
    public UUID getManagementAppId() {
        return MANAGEMENT_APPLICATION_ID;
    }

    @Override
    public UUID getDefaultAppId() {
        return DEFAULT_APPLICATION_ID; 
    }

    private Id generateOrgId(UUID id){
        return new SimpleId( id, "organization" );
    }


    private Id generateApplicationId(UUID id){
        return new SimpleId( id, Application.ENTITY_TYPE );
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


    public void refreshIndex() {

        // refresh factory's indexes, will refresh all three index scopes
        managerCache.getEntityIndex( CpEntityManagerFactory.SYSTEM_APPS_INDEX_SCOPE ).refresh();

        // these are unecessary because of above call
        //managerCache.getEntityIndex( CpEntityManagerFactory.SYSTEM_ORGS_INDEX_SCOPE ).refresh();
        //managerCache.getEntityIndex( CpEntityManagerFactory.SYSTEM_PROPS_INDEX_SCOPE ).refresh();

        // refresh special indexes without calling EntityManager refresh because stack overflow 
        IndexScope mscope = new IndexScopeImpl( 
            new SimpleId( getManagementAppId(), "application"), 
            new SimpleId( getManagementAppId(), "application"), "dummy");
        managerCache.getEntityIndex( mscope ).refresh();

        IndexScope dscope = new IndexScopeImpl( 
            new SimpleId( getDefaultAppId(), "application"), 
            new SimpleId( getDefaultAppId(), "application"), "dummy");
        managerCache.getEntityIndex( dscope ).refresh();
    }


    public void rebuildInternalIndexes( ProgressObserver po ) throws Exception {

        // get all connections from systems app
//        GraphManager gm = managerCache.getGraphManager( CpEntityManagerFactory.SYSTEM_APPS_SCOPE );
//
//        Observable<String> edgeTypes = gm.getEdgeTypesFromSource( 
//            new SimpleSearchEdgeType( systemAppId, null , null ));

        logger.info("Rebuilding system apps index");
        rebuildIndexScope(
                CpEntityManagerFactory.SYSTEM_APPS_SCOPE, 
                CpEntityManagerFactory.SYSTEM_APPS_INDEX_SCOPE, po );

        logger.info("Rebuilding system orgs index");
        rebuildIndexScope(
                CpEntityManagerFactory.SYSTEM_ORGS_SCOPE,
                CpEntityManagerFactory.SYSTEM_ORGS_INDEX_SCOPE, po );

        logger.info("Rebuilding system props index");
        rebuildIndexScope(
                CpEntityManagerFactory.SYSTEM_PROPS_SCOPE,
                CpEntityManagerFactory.SYSTEM_PROPS_INDEX_SCOPE, po );

        logger.info("Rebuilding management application index");
        rebuildApplicationIndex( MANAGEMENT_APPLICATION_ID, po );

        logger.info("Rebuilding default application index");
        rebuildApplicationIndex( DEFAULT_APPLICATION_ID, po );
    }


    private void rebuildIndexScope( CollectionScope cs, IndexScope is, ProgressObserver po ) {

        logger.info("Rebuild index scope for {}:{}:{}", new Object[] {
            cs.getOwner(), cs.getApplication(), cs.getName()
        });

        EntityCollectionManager ecm = managerCache.getEntityCollectionManager( cs );
        EntityIndex ei = managerCache.getEntityIndex( is );

        Query q = Query.fromQL("select *");
        CandidateResults results = ei.search( q );

        Iterator<CandidateResult> iter = results.iterator();
        while (iter.hasNext()) {
            CandidateResult cr = iter.next();

            Entity entity = ecm.load(cr.getId()).toBlockingObservable().last();

            if ( cr.getVersion().compareTo( entity.getVersion()) < 0 ) {
                logger.warn("   Ignoring stale version uuid:{} type:{} state v:{} latest v:{}",
                    new Object[] { 
                        cr.getId().getUuid(), cr.getId().getType(), 
                        cr.getVersion(), entity.getVersion()
                    });

            } else {

                logger.info("   Updating entity type {} with id {} for app {}/{}", new Object[] { 
                    cr.getId().getType(), cr.getId().getUuid(), cs.getApplication().getUuid()
                });

                ei.index(entity);

                if ( po != null ) {
                    po.onProgress();
                }

            }
        }
    }


    public void rebuildApplicationIndex( UUID appId, ProgressObserver po ) throws Exception {

        EntityManager em = getEntityManager( appId );

        Set<String> collections = em.getApplicationCollections();

        logger.debug("For app {} found {} collections: {}", new Object[] {
            appId, collections.size(), collections });

        for ( String collection : collections ) {
            rebuildCollectionIndex( appId, collection, po );
        }
    }


    public void rebuildCollectionIndex( UUID appId, String collectionName, ProgressObserver po ) 
            throws Exception {

        logger.info( "Reindexing collection: {} for app id: {}", collectionName, appId );

        EntityManager em = getEntityManager( appId );
        Application app = em.getApplication();

        // search for all orgs

        Query query = new Query();
        query.setLimit(REBUILD_PAGE_SIZE );
        Results r = null;

        do {

            r = em.searchCollection( app, collectionName, query );

            for ( org.apache.usergrid.persistence.Entity entity : r.getEntities() ) {

                logger.info( "   Updating Entity name {}, type: {}, id: {} in app id: {}", new Object[] {
                        entity.getName(), entity.getType(), entity.getUuid(), appId
                } );

                try {
                    em.update( entity );

                    if ( po != null ) {
                        po.onProgress();
                    }
                }
                catch ( DuplicateUniquePropertyExistsException dupee ) {
                    logger.error( "   Duplicate property for type: {} with id: {} for app id: {}.  "
                            + "Property name: {} , value: {}", new Object[] {
                            entity.getType(), entity.getUuid(), appId, dupee.getPropertyName(), 
                            dupee.getPropertyValue()
                    } );
                }
            }

            query.setCursor( r.getCursor() );
        }
        while ( r != null && r.size() == REBUILD_PAGE_SIZE );
    }


    @Override
    public void flushEntityManagerCaches() {
        Map<UUID, EntityManager>  entityManagersMap = entityManagers.asMap();
        for ( UUID appUuid : entityManagersMap.keySet() ) {
            EntityManager em = entityManagersMap.get(appUuid);
            em.flushManagerCaches();
        }
    }


    private void populateSystemOrgsFromEs() throws Exception {

        logger.info("Migrating system orgs");

        EntityCollectionManager ecm = getManagerCache()
                .getEntityCollectionManager(SYSTEM_ORGS_SCOPE);
        EntityIndex ei = getManagerCache()
                .getEntityIndex( SYSTEM_ORGS_INDEX_SCOPE );

        EntityManager systemAppEm = getEntityManager(SYSTEM_APP_ID);

        String cursor = null;
        boolean done = false;

        while ( !done ) {

            Query q = Query.fromQL("select *");
            q.setCursor( cursor );

            CandidateResults results = ei.search( q );
            cursor = results.getCursor();

            Iterator<CandidateResult> iter = results.iterator();
            while ( iter.hasNext() ) {

                CandidateResult cr = iter.next();
                Entity e = ecm.load( cr.getId() ).toBlockingObservable().last();

                if ( cr.getVersion().compareTo( e.getVersion()) < 0 )  {
                    logger.debug("Stale version of Entity uuid:{} type:{}, stale v:{}, latest v:{}", 
                        new Object[] { cr.getId().getUuid(), cr.getId().getType(), 
                            cr.getVersion(), e.getVersion()});
                    continue;
                }

                Map<String, Object> entityMap = CpEntityMapUtils.toMap( e );
                systemAppEm.create("organization", entityMap );
            }

            if ( cursor == null ) {
                done = true;
            }
        }
    }


    private void populateSystemAppsFromEs() throws Exception {

        logger.info("Migrating system apps");

        EntityCollectionManager ecm = getManagerCache()
                .getEntityCollectionManager(SYSTEM_APPS_SCOPE );
        EntityIndex ei = getManagerCache()
                .getEntityIndex( SYSTEM_APPS_INDEX_SCOPE );

        EntityManager systemAppEm = getEntityManager(SYSTEM_APP_ID);

        String cursor = null;
        boolean done = false;

        while ( !done ) {

            Query q = Query.fromQL("select *");
            q.setCursor( cursor );

            CandidateResults results = ei.search( q );
            cursor = results.getCursor();

            Iterator<CandidateResult> iter = results.iterator();
            while ( iter.hasNext() ) {

                CandidateResult cr = iter.next();
                Entity e = ecm.load( cr.getId() ).toBlockingObservable().last();

                if ( cr.getVersion().compareTo( e.getVersion()) < 0 )  {
                    logger.debug("Stale version of Entity uuid:{} type:{}, stale v:{}, latest v:{}", 
                        new Object[] { cr.getId().getUuid(), cr.getId().getType(), 
                            cr.getVersion(), e.getVersion()});
                    continue;
                }

                Map<String, Object> entityMap = CpEntityMapUtils.toMap( e );
                systemAppEm.create("application", entityMap );
            }

            if ( cursor == null ) {
                done = true;
            }
        }
    }


    private void populateSystemPropsFromEs() throws Exception {

        logger.info("Migrating system props");

        EntityCollectionManager ecm = getManagerCache()
                .getEntityCollectionManager(SYSTEM_PROPS_SCOPE );
        EntityIndex ei = getManagerCache()
                .getEntityIndex( SYSTEM_PROPS_INDEX_SCOPE );

        EntityManager systemAppEm = getEntityManager(SYSTEM_APP_ID);

        String cursor = null;
        boolean done = false;

        while ( !done ) {

            Query q = Query.fromQL("select *");
            q.setCursor( cursor );

            CandidateResults results = ei.search( q );
            cursor = results.getCursor();

            Iterator<CandidateResult> iter = results.iterator();
            while ( iter.hasNext() ) {

                CandidateResult cr = iter.next();
                Entity e = ecm.load( cr.getId() ).toBlockingObservable().last();

                if ( cr.getVersion().compareTo( e.getVersion()) < 0 )  {
                    logger.debug("Stale version of Entity uuid:{} type:{}, stale v:{}, latest v:{}", 
                        new Object[] { cr.getId().getUuid(), cr.getId().getType(), 
                            cr.getVersion(), e.getVersion()});
                    continue;
                }

                Map<String, Object> entityMap = CpEntityMapUtils.toMap( e );
                systemAppEm.create("property", entityMap );
            }

            if ( cursor == null ) {
                done = true;
            }
        }
    }

}
