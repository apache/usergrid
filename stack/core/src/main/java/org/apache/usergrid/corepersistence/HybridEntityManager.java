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
package org.apache.usergrid.corepersistence;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.netflix.config.ConfigurationManager;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.EntityRef;
import org.apache.usergrid.persistence.Query;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.persistence.cassandra.CassandraService;
import org.apache.usergrid.persistence.cassandra.CounterUtils;
import org.apache.usergrid.persistence.cassandra.EntityManagerFactoryImpl;
import org.apache.usergrid.persistence.cassandra.EntityManagerImpl;
import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.collection.OrganizationScope;
import org.apache.usergrid.persistence.collection.impl.CollectionScopeImpl;
import org.apache.usergrid.persistence.collection.impl.OrganizationScopeImpl;
import org.apache.usergrid.persistence.collection.migration.MigrationException;
import org.apache.usergrid.persistence.collection.migration.MigrationManager;
import org.apache.usergrid.persistence.graph.GraphManager;
import org.apache.usergrid.persistence.graph.GraphManagerFactory;
import org.apache.usergrid.persistence.index.EntityCollectionIndex;
import org.apache.usergrid.persistence.index.EntityCollectionIndexFactory;
import org.apache.usergrid.persistence.index.exceptions.QueryParseException;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.field.StringField;
import org.apache.usergrid.persistence.model.field.UUIDField;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hybrid Core Persistence implementation of EntityManager.
 */
public class HybridEntityManager extends EntityManagerImpl {
    private static final Logger log = LoggerFactory.getLogger(HybridEntityManager.class);

    /** Each instance operates on a CollectionScope. */
    private CollectionScope scope = null; 


    private static EntityCollectionManagerFactory ecmf;
    private static EntityCollectionIndexFactory ecif;
    private static GraphManagerFactory gmf;
   
    private CpEntityManager cpEntityManager = null;

    private static final String SYSTEM_ORG_UUID = "b9b51240-b5d5-11e3-9ea8-11c207d6769a";
    private static final String SYSTEM_ORG_TYPE = "zzz_defaultapp_zzz";

    private static final String SYSTEM_APP_UUID = "b6768a08-b5d5-11e3-a495-10ddb1de66c4";
    private static final String SYSTEM_APP_TYPE = "zzz_defaultapp_zzz";

    private static final OrganizationScope SYSTEM_ORG_SCOPE = 
        new OrganizationScopeImpl( 
            new SimpleId( UUID.fromString(SYSTEM_ORG_UUID), SYSTEM_ORG_TYPE ));

    private static final CollectionScope SYSTEM_APP_SCOPE = 
        new CollectionScopeImpl( 
            SYSTEM_ORG_SCOPE.getOrganization(), 
            new SimpleId( UUID.fromString(SYSTEM_APP_UUID), SYSTEM_APP_TYPE ), 
            SYSTEM_APP_TYPE);

    private final Map<CollectionScope, EntityCollectionManager> managers = new HashMap<>();
    private final Map<CollectionScope, EntityCollectionIndex> indexes = new HashMap<>();
    private final Map<OrganizationScope, GraphManager> graphManagers = new HashMap<>();


    static {

        try {
            ConfigurationManager.loadCascadedPropertiesFromResources("core-persistence");

            // TODO: make CpEntityManager work in non-test environment
            Properties testProps = new Properties() {{
                put("cassandra.hosts", "localhost:" + System.getProperty("cassandra.rpc_port"));
            }};

            ConfigurationManager.loadProperties( testProps );

        } catch (IOException ex) {
            throw new RuntimeException("Error loading Core Persistence proprties", ex);
        }

        Injector injector = Guice.createInjector( new GuiceModule() );

        MigrationManager m = injector.getInstance( MigrationManager.class );
        try {
            m.migrate();
        } catch (MigrationException ex) {
            throw new RuntimeException("Error migrating Core Persistence", ex);
        }

        ecmf = injector.getInstance( EntityCollectionManagerFactory.class );
        ecif = injector.getInstance( EntityCollectionIndexFactory.class );
    }


    @Override
    public EntityManager init( 
            EntityManagerFactoryImpl emf, 
            CassandraService cass, 
            CounterUtils counterUtils,
            UUID applicationId, 
            boolean skipAggregateCounters ) {

        super.init( emf, cass, counterUtils, applicationId, skipAggregateCounters );


        return this;
    
    }


    private EntityManager getCpEntityManager() {
        if ( cpEntityManager == null ) {
            EntityCollectionManager ecm = getManager( SYSTEM_APP_SCOPE );
            EntityCollectionIndex eci = getIndex( SYSTEM_APP_SCOPE );
            try {
                cpEntityManager = new CpEntityManager( getCollectionScope(), ecm, eci );
            } catch (Exception ex) {
                throw new RuntimeException("Error initializing CpEntityManager", ex);
            }
        }
        return cpEntityManager;
    }


    @Override
    public void setApplicationId( UUID applicationId ) {
        super.setApplicationId( applicationId );
    }


    private CollectionScope getCollectionScope() throws Exception {
        
        if ( scope == null && getApplication() != null ) {
            Id appId = getAppId();
            Id orgId = getOrgId();
            scope = new CollectionScopeImpl( orgId, appId, "applications" );
        }
        return scope;
    }

    private Id getOrgId() throws Exception, QueryParseException {

        Id orgId;
        EntityCollectionManager ecm = getManager( SYSTEM_APP_SCOPE );
        EntityCollectionIndex eci = getIndex( SYSTEM_APP_SCOPE );

        String orgName = getApplication().getOrganizationName();

        org.apache.usergrid.persistence.index.query.Query q =
                org.apache.usergrid.persistence.index.query.Query.fromQL(
                        "name = '" + orgName + "'");

        org.apache.usergrid.persistence.index.query.Results results = eci.execute(q);

        if ( results.isEmpty() ) { // create if does not exist
            
            org.apache.usergrid.persistence.model.entity.Entity entity =
                    new org.apache.usergrid.persistence.model.entity.Entity(
                            new SimpleId(UUIDGenerator.newTimeUUID(), "organization" ));
            
            entity.setField( new StringField( "name", orgName ));
            entity = ecm.write( entity ).toBlockingObservable().last();
            log.debug("Added record for org name {}", orgName );
            
            orgId = entity.getId();
            
        } else {
            org.apache.usergrid.persistence.model.entity.Entity entity =
                    results.getEntities().get(0);
            orgId = entity.getId();
        }

        return orgId;
    }


    private Id getAppId() throws QueryParseException, Exception {

        Id appId;
        EntityCollectionManager ecm = getManager( SYSTEM_APP_SCOPE );
        EntityCollectionIndex eci = getIndex( SYSTEM_APP_SCOPE );

        UUID appUuid = getApplication().getUuid();

        org.apache.usergrid.persistence.index.query.Query q =
                org.apache.usergrid.persistence.index.query.Query.fromQL(
                        "uuid = '" + appUuid.toString() + "'");
        org.apache.usergrid.persistence.index.query.Results results = eci.execute(q);
        if ( results.isEmpty() ) { // create if does not exist
            
            org.apache.usergrid.persistence.model.entity.Entity entity =
                    new org.apache.usergrid.persistence.model.entity.Entity(
                            new SimpleId(UUIDGenerator.newTimeUUID(), "application" ));
            
            entity.setField( new UUIDField( "uuid", appUuid ));
            entity = ecm.write( entity ).toBlockingObservable().last();
            log.debug("Added record for app uuid {}", appUuid.toString() );
            
            appId = entity.getId();
            
        } else {
            org.apache.usergrid.persistence.model.entity.Entity entity =
                    results.getEntities().get(0);
            appId = entity.getId();
        }
        return appId;
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

    //-------------------------------------------------------------------------
    //---------------------------------------------------------- Implementation 
    //-------------------------------------------------------------------------

    @Override
    public Results searchCollection(EntityRef entityRef, String collectionName, Query query) throws Exception {
        return getCpEntityManager().searchCollection(entityRef, collectionName, query); 
    }

    @Override
    public Entity addToCollection(EntityRef entityRef, String collectionName, EntityRef itemRef) throws Exception {
        return getCpEntityManager().addToCollection(entityRef, collectionName, itemRef); 
    }

    @Override
    public Entity get(UUID entityid) throws Exception {
        return getCpEntityManager().get(entityid); 
    }

    @Override
    public Entity create(UUID importId, String entityType, Map<String, Object> properties) throws Exception {
        return getCpEntityManager().create(importId, entityType, properties); 
    }
    
    @Override
    public Entity create(String entityType, Map<String, Object> properties) throws Exception {
        return getCpEntityManager().create( entityType, properties); 
    }

    @Override
    public void update(Entity entity) throws Exception {
        getCpEntityManager().update(entity); 
    }

    @Override
    public EntityRef getApplicationRef() {
        return getCpEntityManager().getApplicationRef(); 
    }

    @Override
    public void updateProperties(EntityRef entityRef, Map<String, Object> properties) throws Exception {
        getCpEntityManager().updateProperties(entityRef, properties); 
    }

    @Override
    public void delete(EntityRef entityRef) throws Exception {
        getCpEntityManager().delete( entityRef );
    }
}
