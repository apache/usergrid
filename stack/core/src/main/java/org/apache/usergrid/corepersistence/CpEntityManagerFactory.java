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
import java.util.TreeMap;
import java.util.UUID;
import org.apache.commons.lang.StringUtils;
import org.apache.usergrid.persistence.DynamicEntity;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.EntityManagerFactory;
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
import org.apache.usergrid.persistence.graph.GraphManagerFactory;
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
    public static final String SYSTEM_APPS_UUID = "b6768a08-b5d5-11e3-a495-10ddb1de66c3";
    
    public static final  UUID MANAGEMENT_APPLICATION_ID = 
            UUID.fromString("b6768a08-b5d5-11e3-a495-11ddb1de66c8");

    public static final  UUID DEFAULT_APPLICATION_ID = 
            UUID.fromString("b6768a08-b5d5-11e3-a495-11ddb1de66c9");


    // Three types of things we store in System Application
    public static final String SYSTEM_APPS_TYPE = "zzzappszzz";
    public static final String SYSTEM_ORGS_TYPE = "zzzorgszzz";
    public static final String SYSTEM_PROPS_TYPE = "zzzpropszzz"; 

    private static final Id systemAppId = 
         new SimpleId( UUID.fromString(SYSTEM_APPS_UUID), SYSTEM_APPS_TYPE );

    // Scopes for those three types of things

    public static final CollectionScope SYSTEM_APP_SCOPE = 
        new CollectionScopeImpl( systemAppId, systemAppId, SYSTEM_APPS_TYPE );
    public static final IndexScope SYSTEM_APPS_INDEX_SCOPE = 
        new IndexScopeImpl( systemAppId, systemAppId,  SYSTEM_APPS_TYPE);

    public static final CollectionScope SYSTEM_ORGS_SCOPE = 
        new CollectionScopeImpl( systemAppId, systemAppId,  SYSTEM_ORGS_TYPE);
    public static final IndexScope SYSTEM_ORGS_INDEX_SCOPE = 
        new IndexScopeImpl( systemAppId, systemAppId, SYSTEM_ORGS_TYPE);

    public static final CollectionScope SYSTEM_PROPS_SCOPE = 
        new CollectionScopeImpl( systemAppId, systemAppId, SYSTEM_PROPS_TYPE);
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


    public CpEntityManagerFactory( 
            CassandraService cass, CounterUtils counterUtils, boolean skipAggregateCounters ) {

        this.cass = cass;
        this.counterUtils = counterUtils;
        this.skipAggregateCounters = skipAggregateCounters;
        if ( skipAggregateCounters ) {
            logger.warn( "NOTE: Counters have been disabled by configuration..." );
        }
        logger.debug("Created a new CpEntityManagerFactory");
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
                    .getEntityCollectionManager( SYSTEM_APP_SCOPE );
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

        Query q = Query.fromQL("select *");

        EntityCollectionManager em = getManagerCache()
                .getEntityCollectionManager( SYSTEM_APP_SCOPE );
        EntityIndex ei = getManagerCache()
                .getEntityIndex( SYSTEM_APPS_INDEX_SCOPE );

        CandidateResults results = ei.search( q );

        Map<String, UUID> appMap = new HashMap<String, UUID>();

        Iterator<CandidateResult> iter = results.iterator();
        while ( iter.hasNext() ) {
            CandidateResult cr = iter.next();
            Entity e = em.load( cr.getId() ).toBlockingObservable().last();
            appMap.put( 
                (String)e.getField(PROPERTY_NAME).getValue(), 
                (UUID)e.getField(PROPERTY_UUID).getValue() );
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
        // values as strings because that is all we need for service properties.'
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
        managerCache.getEntityIndex( CpEntityManagerFactory.SYSTEM_APPS_INDEX_SCOPE ).refresh();
    }


}
