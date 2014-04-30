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
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import org.apache.commons.lang.StringUtils;
import org.apache.usergrid.persistence.DynamicEntity;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.EntityManagerFactory;
import static org.apache.usergrid.persistence.Schema.PROPERTY_NAME;
import static org.apache.usergrid.persistence.Schema.PROPERTY_UUID;
import static org.apache.usergrid.persistence.Schema.TYPE_APPLICATION;
import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.collection.impl.CollectionScopeImpl;
import org.apache.usergrid.persistence.core.scope.OrganizationScope;
import org.apache.usergrid.persistence.core.scope.OrganizationScopeImpl;
import org.apache.usergrid.persistence.exceptions.ApplicationAlreadyExistsException;
import org.apache.usergrid.persistence.index.EntityIndex;
import org.apache.usergrid.persistence.index.EntityIndexFactory;
import org.apache.usergrid.persistence.index.query.Query;
import org.apache.usergrid.persistence.index.query.Results;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.field.Field;
import org.apache.usergrid.persistence.model.field.StringField;
import org.apache.usergrid.persistence.model.field.UUIDField;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;
import org.junit.After;
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

    private EntityCollectionManagerFactory ecmf;
    private EntityIndexFactory ecif;

    public static final Class<DynamicEntity> APPLICATION_ENTITY_CLASS = DynamicEntity.class;


    // organization scope in which to store system info
    public static final String SYSTEM_ORG_UUID = "b6768a08-b5d5-11e3-a495-10ddb1de66c1";
    public static final String SYSTEM_ORG_TYPE = "zzzsystemorgzzz";

    // collection scope in which to store Organization record entities
    public static final String SYSTEM_ORGS_UUID = "b6768a08-b5d5-11e3-a495-10ddb1de66c2";
    public static final String SYSTEM_ORGS_TYPE = "zzzorgszzz";

    // collection scope in which to store Application record entities
    public static final String SYSTEM_APPS_UUID = "b6768a08-b5d5-11e3-a495-10ddb1de66c3";
    public static final String SYSTEM_APPS_TYPE = "zzzappszzz";
    
    // collection scope in which to store the one and only Properties entity
    public static final String SYSTEM_PROPS_UUID = "b6768a08-b5d5-11e3-a495-10ddb1de66c5";
    public static final String SYSTEM_PROPS_TYPE = "zzzpropszzz"; 


    public static final OrganizationScope SYSTEM_ORG_SCOPE = 
        new OrganizationScopeImpl( 
            new SimpleId( UUID.fromString(SYSTEM_ORG_UUID), 
                    SYSTEM_ORG_TYPE ));

    public static final CollectionScope SYSTEM_APPS_SCOPE = 
        new CollectionScopeImpl( SYSTEM_ORG_SCOPE.getOrganization(), 
            new SimpleId( UUID.fromString(SYSTEM_ORGS_UUID), SYSTEM_ORGS_TYPE ), 
                SYSTEM_ORGS_TYPE);

    public static final CollectionScope SYSTEM_ORGS_SCOPE = 
        new CollectionScopeImpl( SYSTEM_ORG_SCOPE.getOrganization(), 
            new SimpleId( UUID.fromString(SYSTEM_APPS_UUID), SYSTEM_APPS_TYPE ), 
                SYSTEM_APPS_TYPE);

    public static final CollectionScope SYSTEM_PROPS_SCOPE = 
        new CollectionScopeImpl( SYSTEM_ORG_SCOPE.getOrganization(), 
            new SimpleId( UUID.fromString(SYSTEM_PROPS_UUID), SYSTEM_PROPS_TYPE ), 
                SYSTEM_PROPS_TYPE);


    // cache of already instantiated entity managers
    private LoadingCache<UUID, EntityManager> entityManagers
        = CacheBuilder.newBuilder().maximumSize(100).build(new CacheLoader<UUID, EntityManager>() {
            public EntityManager load(UUID appId) { // no checked exception
                return _getEntityManager(appId);
            }
        });


    public CpEntityManagerFactory() {

        // TODO: better solution for getting injector? 
        Injector injector = CpSetup.getInjector();

        ecmf = injector.getInstance( EntityCollectionManagerFactory.class );
        ecif = injector.getInstance( EntityIndexFactory.class );
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
        EntityManager em = applicationContext.getBean( "entityManager", EntityManager.class );
        em.setApplicationId( applicationId );
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
        logger.debug( "New application id " + applicationId.toString() );

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

        UUID orgUuid = lookupOrganization( organizationName );
        if ( orgUuid == null ) {
          
            // organization does not exist, create it.
            Entity orgInfoEntity = new Entity(
                new SimpleId(UUIDGenerator.newTimeUUID(), "organization" ));

            orgUuid = orgInfoEntity.getId().getUuid();

            orgInfoEntity.setField( new StringField( PROPERTY_NAME, name ));
            orgInfoEntity.setField( new UUIDField( PROPERTY_UUID, orgUuid ));

            EntityCollectionManager ecm = ecmf.createCollectionManager( SYSTEM_ORGS_SCOPE );
            EntityIndex eci = ecif.createEntityIndex( SYSTEM_ORG_SCOPE, SYSTEM_ORGS_SCOPE );

            orgInfoEntity = ecm.write( orgInfoEntity ).toBlockingObservable().last();
            eci.index( SYSTEM_ORGS_SCOPE, orgInfoEntity );
            eci.refresh();
        }

        if ( properties == null ) {
            properties = new TreeMap<String, Object>( CASE_INSENSITIVE_ORDER );
        }
        properties.put( PROPERTY_NAME, appName );

        Entity appInfoEntity = new Entity(
            new SimpleId(UUIDGenerator.newTimeUUID(), "application" ));

        appInfoEntity.setField( new StringField( PROPERTY_NAME, name ));
        appInfoEntity.setField( new UUIDField( PROPERTY_UUID, applicationId ));
        appInfoEntity.setField( new UUIDField( "organizationUuid", orgUuid ));

        // create app in system app scope
        {
            EntityCollectionManager ecm = ecmf.createCollectionManager( SYSTEM_APPS_SCOPE );
            EntityIndex eci = ecif.createEntityIndex( SYSTEM_ORG_SCOPE, SYSTEM_APPS_SCOPE );

            appInfoEntity = ecm.write( appInfoEntity ).toBlockingObservable().last();
            eci.index( SYSTEM_APPS_SCOPE, appInfoEntity );
            eci.refresh();
        }

        // create app in its own scope
        EntityManager em = getEntityManager( applicationId );
        em.create( TYPE_APPLICATION, APPLICATION_ENTITY_CLASS, properties );
        em.resetRoles();

        return applicationId;
    }


    public OrganizationScope getOrganizationScope( UUID applicationId ) {

        Query q = Query.fromQL( PROPERTY_UUID + " = '" + applicationId.toString() + "'");

        EntityIndex ei = ecif.createEntityIndex( SYSTEM_ORG_SCOPE, SYSTEM_APPS_SCOPE );
        org.apache.usergrid.persistence.index.query.Results results = 
            ei.search( SYSTEM_APPS_SCOPE, q );

        if ( results.isEmpty() ) {
            return null;
        }

        Entity appEntity = results.iterator().next(); 

        return new OrganizationScopeImpl( new SimpleId( 
            ((UUID)(appEntity.getField("organizationUuid")).getValue()), "organization"));
    }


    public CollectionScope getApplicationScope( UUID applicationId ) {

        Query q = Query.fromQL( PROPERTY_UUID + " = '" + applicationId.toString() + "'");

        EntityIndex ei = ecif.createEntityIndex( SYSTEM_ORG_SCOPE, SYSTEM_APPS_SCOPE );
        Results results = ei.search( SYSTEM_APPS_SCOPE, q );

        if ( results.isEmpty() ) {
            return null;
        }

        Entity appEntity = results.iterator().next(); 

        return new CollectionScopeImpl(
            new SimpleId( ((UUID)(appEntity.getField("organizationUuid")).getValue()), "organization"),
            new SimpleId( appEntity.getId().getUuid(), "application"),
            appEntity.getField(PROPERTY_NAME).getValue().toString()
        );
    }
    

    @Override
    public UUID importApplication(
            String organization, UUID applicationId,
            String name, Map<String, Object> properties) throws Exception {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    
    public UUID lookupOrganization( String name) throws Exception {

        Query q = Query.fromQL(PROPERTY_NAME + " = '" + name + "'");

        EntityIndex ei = ecif.createEntityIndex( SYSTEM_ORG_SCOPE, SYSTEM_ORGS_SCOPE );
        Results results = ei.search( SYSTEM_ORGS_SCOPE, q );

        if ( results.isEmpty() ) {
            return null; 
        } 

        return results.iterator().next().getId().getUuid();
    }


    @Override
    public UUID lookupApplication( String name) throws Exception {

        Query q = Query.fromQL( PROPERTY_NAME + " = '" + name + "'");

        EntityIndex ei = ecif.createEntityIndex( SYSTEM_ORG_SCOPE, SYSTEM_APPS_SCOPE );
        org.apache.usergrid.persistence.index.query.Results results = 
            ei.search( SYSTEM_APPS_SCOPE, q );

        if ( results.isEmpty() ) {
            return null; 
        } 

        return results.iterator().next().getId().getUuid();
    }


    @Override
    @Metered(group = "core", name = "EntityManagerFactory_getApplication")
    public Map<String, UUID> getApplications() throws Exception {

        Query q = Query.fromQL("select *");

        EntityIndex ei = ecif.createEntityIndex( SYSTEM_ORG_SCOPE, SYSTEM_APPS_SCOPE );
        Results results = ei.search( SYSTEM_APPS_SCOPE, q );

        Map<String, UUID> appMap = new HashMap<String, UUID>();
        for ( Entity e : results.getEntities() ) {
            appMap.put( 
                (String)e.getField(PROPERTY_NAME).getValue(), 
                (UUID)e.getField(PROPERTY_UUID).getValue() );
        }

        return appMap;
    }

    
    @Override
    public void setup() throws Exception {
        // no op?
    }

    
    @Override
    public Map<String, String> getServiceProperties() {

        Query q = Query.fromQL("select *");
        EntityIndex ei = ecif.createEntityIndex( SYSTEM_ORG_SCOPE, SYSTEM_PROPS_SCOPE );
        Results results = ei.search( SYSTEM_PROPS_SCOPE, q );
        if ( results.isEmpty() ) {
            return new HashMap<String,String>();
        }

        Entity propsEntity = results.iterator().next();
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

        EntityCollectionManager ecm = ecmf.createCollectionManager( SYSTEM_PROPS_SCOPE );
        EntityIndex ei = ecif.createEntityIndex( SYSTEM_ORG_SCOPE, SYSTEM_PROPS_SCOPE );

        Query q = Query.fromQL("select *");
        Results results = ei.search( SYSTEM_PROPS_SCOPE, q );
        Entity propsEntity;
        if ( !results.isEmpty() ) {
            propsEntity = results.iterator().next();
        } else {
            propsEntity = new Entity( new SimpleId( "properties" ));
        }

        // intentionally going only one-level deep into fields and treating all 
        // values as strings because that is all we need for service properties.'
        for ( String key : properties.keySet() ) {
            propsEntity.setField( new StringField(key, properties.get(key)) );
        }

        propsEntity = ecm.write( propsEntity ).toBlockingObservable().last();
        ei.index( SYSTEM_PROPS_SCOPE, propsEntity );    

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

        EntityCollectionManager ecm = ecmf.createCollectionManager( SYSTEM_PROPS_SCOPE );
        EntityIndex ei = ecif.createEntityIndex( SYSTEM_ORG_SCOPE, SYSTEM_PROPS_SCOPE );

        Query q = Query.fromQL("select *");
        Results results = ei.search( SYSTEM_PROPS_SCOPE, q );
        Entity propsEntity = results.iterator().next();
        if ( propsEntity == null ) {
            return false; // nothing to delete
        }

        if ( propsEntity.getField(name) == null ) {
            return false; // no such field
        }

        propsEntity.removeField( name );

        propsEntity = ecm.write( propsEntity ).toBlockingObservable().last();
        ei.index( SYSTEM_PROPS_SCOPE, propsEntity );    

        return true;
    }

    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    @Override
    public void setApplicationContext( ApplicationContext applicationContext ) throws BeansException {
        this.applicationContext = applicationContext;
    }

}
