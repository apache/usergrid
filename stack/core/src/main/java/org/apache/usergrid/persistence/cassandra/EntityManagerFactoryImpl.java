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


import java.nio.ByteBuffer;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.apache.usergrid.persistence.DynamicEntity;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.EntityManagerFactory;
import org.apache.usergrid.persistence.entities.Application;
import org.apache.usergrid.persistence.exceptions.ApplicationAlreadyExistsException;
import org.apache.usergrid.utils.UUIDUtils;

import org.apache.commons.lang.StringUtils;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.yammer.metrics.annotation.Metered;

import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.ColumnSlice;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.beans.OrderedRows;
import me.prettyprint.hector.api.beans.Row;
import me.prettyprint.hector.api.beans.Rows;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.QueryResult;
import me.prettyprint.hector.api.query.RangeSlicesQuery;

import static java.lang.String.CASE_INSENSITIVE_ORDER;

import static me.prettyprint.hector.api.factory.HFactory.createMutator;
import static me.prettyprint.hector.api.factory.HFactory.createRangeSlicesQuery;
import static org.apache.usergrid.persistence.Schema.PROPERTY_NAME;
import static org.apache.usergrid.persistence.Schema.PROPERTY_UUID;
import static org.apache.usergrid.persistence.Schema.TYPE_APPLICATION;
import static org.apache.usergrid.persistence.cassandra.CassandraPersistenceUtils.addInsertToMutator;
import static org.apache.usergrid.persistence.cassandra.CassandraPersistenceUtils.asMap;
import static org.apache.usergrid.persistence.cassandra.CassandraPersistenceUtils.batchExecute;
import static org.apache.usergrid.persistence.cassandra.CassandraService.APPLICATIONS_CF;
import static org.apache.usergrid.persistence.cassandra.CassandraService.PROPERTIES_CF;
import static org.apache.usergrid.persistence.cassandra.CassandraService.RETRY_COUNT;
import static org.apache.usergrid.utils.ConversionUtils.uuid;
import static org.apache.usergrid.persistence.cassandra.Serializers.*;


/**
 * Cassandra-specific implementation of Datastore
 *
 * @author edanuff
 */
public class EntityManagerFactoryImpl implements EntityManagerFactory, ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger( EntityManagerFactoryImpl.class );

    public static String IMPLEMENTATION_DESCRIPTION = "Cassandra Entity Manager Factory 1.0";

    public static final Class<DynamicEntity> APPLICATION_ENTITY_CLASS = DynamicEntity.class;


    ApplicationContext applicationContext;

    CassandraService cass;
    CounterUtils counterUtils;

    private boolean skipAggregateCounters;

    private LoadingCache<UUID, EntityManager> entityManagers =
            CacheBuilder.newBuilder().maximumSize( 100 ).build( new CacheLoader<UUID, EntityManager>() {
                public EntityManager load( UUID appId ) { // no checked exception
                    return _getEntityManager( appId );
                }
            } );


    /**
     * Must be constructed with a CassandraClientPool.
     *
     * @param cass the cassandraService instance
     */
    public EntityManagerFactoryImpl( CassandraService cass, CounterUtils counterUtils, boolean skipAggregateCounters ) {
        this.cass = cass;
        this.counterUtils = counterUtils;
        this.skipAggregateCounters = skipAggregateCounters;
        if ( skipAggregateCounters ) {
            logger.warn( "NOTE: Counters have been disabled by configuration..." );
        }
    }


    /*
     * (non-Javadoc)
     * 
     * @see org.apache.usergrid.core.Datastore#getImpementationDescription()
     */
    @Override
    public String getImpementationDescription() {
        return IMPLEMENTATION_DESCRIPTION;
    }


    /*
     * (non-Javadoc)
     * 
     * @see org.apache.usergrid.core.Datastore#getEntityDao(java.util.UUID,
     * java.util.UUID)
     */
    @Override
    public EntityManager getEntityManager( UUID applicationId ) {
        try {
            return entityManagers.get( applicationId );
        }
        catch ( Exception ex ) {
            ex.printStackTrace();
        }
        return _getEntityManager( applicationId );
    }


    private EntityManager _getEntityManager( UUID applicationId ) {
        //EntityManagerImpl em = new EntityManagerImpl();
        EntityManager em = applicationContext.getBean( "entityManager", EntityManager.class );
        //em.init(this,cass,counterUtils,applicationId, skipAggregateCounters);
        em.setApplicationId( applicationId );
        return em;
    }


    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }


    /**
     * Gets the setup.
     *
     * @return Setup helper
     */
    public Setup getSetup() {
        return new Setup( this, cass );
    }


    @Override
    public void setup() throws Exception {
        Setup setup = getSetup();

        setup.setup();


        if ( cass.getPropertiesMap() != null ) {
            updateServiceProperties( cass.getPropertiesMap() );
        }
    }


    /*
     * (non-Javadoc)
     * 
     * @see org.apache.usergrid.core.Datastore#createApplication(java.lang.String)
     */
    @Override
    public UUID createApplication( String organization, String name ) throws Exception {
        return createApplication( organization, name, null );
    }


    /*
     * (non-Javadoc)
     * 
     * @see org.apache.usergrid.core.Datastore#createApplication(java.lang.String,
     * java.util.Map)
     */
    @Override
    public UUID createApplication( String organizationName, String name, Map<String, Object> properties )
            throws Exception {

        String appName = buildAppName( organizationName, name );

        HColumn<String, ByteBuffer> column =
                cass.getColumn( cass.getSystemKeyspace(), APPLICATIONS_CF, appName, PROPERTY_UUID );
        if ( column != null ) {
            throw new ApplicationAlreadyExistsException( name );
            // UUID uuid = uuid(column.getValue());
            // return uuid;
        }

        UUID applicationId = UUIDUtils.newTimeUUID();
        logger.info( "New application id " + applicationId.toString() );

        initializeApplication( organizationName, applicationId, appName, properties );

        return applicationId;
    }


    private String buildAppName( String organizationName, String name ) {
        return StringUtils.lowerCase( name.contains( "/" ) ? name : organizationName + "/" + name );
    }


    public UUID initializeApplication( String organizationName, UUID applicationId, String name,
                                       Map<String, Object> properties ) throws Exception {

        String appName = buildAppName( organizationName, name );
        // check for pre-existing
        if ( lookupApplication( appName ) != null ) {
            throw new ApplicationAlreadyExistsException( appName );
        }
        if ( properties == null ) {
            properties = new TreeMap<String, Object>( CASE_INSENSITIVE_ORDER );
        }

        properties.put( PROPERTY_NAME, appName );

        getSetup().setupApplicationKeyspace( applicationId, appName );


        Keyspace ko = cass.getSystemKeyspace();
        Mutator<ByteBuffer> m = createMutator( ko, be );

        long timestamp = cass.createTimestamp();

        addInsertToMutator( m, APPLICATIONS_CF, appName, PROPERTY_UUID, applicationId, timestamp );
        addInsertToMutator( m, APPLICATIONS_CF, appName, PROPERTY_NAME, appName, timestamp );

        batchExecute( m, RETRY_COUNT );

        EntityManager em = getEntityManager( applicationId );
        em.create( TYPE_APPLICATION, APPLICATION_ENTITY_CLASS, properties );

        em.resetRoles();

        return applicationId;
    }


    @Override
    public UUID importApplication( String organizationName, UUID applicationId, String name,
                                   Map<String, Object> properties ) throws Exception {

        name = buildAppName( organizationName, name );

        HColumn<String, ByteBuffer> column =
                cass.getColumn( cass.getSystemKeyspace(), APPLICATIONS_CF, name, PROPERTY_UUID );
        if ( column != null ) {
            throw new ApplicationAlreadyExistsException( name );
            // UUID uuid = uuid(column.getValue());
            // return uuid;
        }

        return initializeApplication( organizationName, applicationId, name, properties );
    }


    @Override
    @Metered(group = "core", name = "EntityManagerFactory_lookupApplication_byName")
    public UUID lookupApplication( String name ) throws Exception {
        name = name.toLowerCase();
        HColumn<String, ByteBuffer> column =
                cass.getColumn( cass.getSystemKeyspace(), APPLICATIONS_CF, name, PROPERTY_UUID );
        if ( column != null ) {
            return uuid( column.getValue() );
        }
        return null;
    }


    /**
     * Gets the application.
     *
     * @param name the name
     *
     * @return application for name
     *
     * @throws Exception the exception
     */
    @Metered(group = "core", name = "EntityManagerFactory_getApplication")
    public Application getApplication( String name ) throws Exception {
        name = name.toLowerCase();
        HColumn<String, ByteBuffer> column =
                cass.getColumn( cass.getSystemKeyspace(), APPLICATIONS_CF, name, PROPERTY_UUID );
        if ( column == null ) {
            return null;
        }

        UUID applicationId = uuid( column.getValue() );

        EntityManager em = getEntityManager( applicationId );
        return ( ( EntityManagerImpl ) em ).getEntity( applicationId, Application.class );
    }


    @Override
    public Map<String, UUID> getApplications() throws Exception {
        Map<String, UUID> applications = new TreeMap<String, UUID>( CASE_INSENSITIVE_ORDER );
        Keyspace ko = cass.getSystemKeyspace();
        RangeSlicesQuery<String, String, UUID> q = createRangeSlicesQuery( ko, se, se, ue );
        q.setKeys( "", "\uFFFF" );
        q.setColumnFamily( APPLICATIONS_CF );
        q.setColumnNames( PROPERTY_UUID );
        q.setRowCount( 10000 );
        QueryResult<OrderedRows<String, String, UUID>> r = q.execute();
        Rows<String, String, UUID> rows = r.get();
        for ( Row<String, String, UUID> row : rows ) {
            ColumnSlice<String, UUID> slice = row.getColumnSlice();
            HColumn<String, UUID> column = slice.getColumnByName( PROPERTY_UUID );
            applications.put( row.getKey(), column.getValue() );
        }
        return applications;
    }


    @Override
    public boolean setServiceProperty( String name, String value ) {
        try {
            cass.setColumn( cass.getSystemKeyspace(), PROPERTIES_CF, PROPERTIES_CF, name, value );
            return true;
        }
        catch ( Exception e ) {
            logger.error( "Unable to set property " + name + ": " + e.getMessage() );
        }
        return false;
    }


    @Override
    public boolean deleteServiceProperty( String name ) {
        try {
            cass.deleteColumn( cass.getSystemKeyspace(), PROPERTIES_CF, PROPERTIES_CF, name );
            return true;
        }
        catch ( Exception e ) {
            logger.error( "Unable to delete property " + name + ": " + e.getMessage() );
        }
        return false;
    }


    @Override
    public boolean updateServiceProperties( Map<String, String> properties ) {
        try {
            cass.setColumns( cass.getSystemKeyspace(), PROPERTIES_CF, PROPERTIES_CF.getBytes(), properties );
            return true;
        }
        catch ( Exception e ) {
            logger.error( "Unable to update properties: " + e.getMessage() );
        }
        return false;
    }


    @Override
    public Map<String, String> getServiceProperties() {
        try {
            return asMap( cass.getAllColumns( cass.getSystemKeyspace(), PROPERTIES_CF, PROPERTIES_CF, se, se ) );
        }
        catch ( Exception e ) {
            logger.error( "Unable to load properties: " + e.getMessage() );
        }
        return null;
    }


    @Override
    public void setApplicationContext( ApplicationContext applicationContext ) throws BeansException {
        this.applicationContext = applicationContext;
    }


    public void setCounterUtils( CounterUtils counterUtils ) {
        this.counterUtils = counterUtils;
    }
}
