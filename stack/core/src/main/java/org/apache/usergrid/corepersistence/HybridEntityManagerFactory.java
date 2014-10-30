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

import java.util.Map;
import java.util.UUID;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.EntityManagerFactory;
import org.apache.usergrid.persistence.cassandra.CassandraService;
import org.apache.usergrid.persistence.cassandra.CounterUtils;
import org.apache.usergrid.persistence.cassandra.EntityManagerFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;


/**
 * Can read from either old EntityManagerImpl or new CpEntityManager, can write to either or both.
 */
public class HybridEntityManagerFactory implements EntityManagerFactory, ApplicationContextAware {
    private static final Logger logger = LoggerFactory.getLogger( CpEntityManagerFactory.class );
    private final EntityManagerFactory factory;


    public HybridEntityManagerFactory( 
            CassandraService cass, CounterUtils counterUtils, boolean skipAggCounters ) {

        boolean useCP = cass.getPropertiesMap().get("usergrid.persistence").equals("CP");
        if ( useCP ) {
            logger.info("HybridEntityManagerFactory: configured for New Core Persistence engine");
            factory = new CpEntityManagerFactory(cass, counterUtils, skipAggCounters );
        } else {
            logger.info("HybridEntityManagerFactory: configured for Classic Usergrid persistence");
            factory = new EntityManagerFactoryImpl( cass, counterUtils, skipAggCounters );
        }
    }

    @Override
    public String getImpementationDescription() throws Exception {
        return factory.getImpementationDescription();
    }

    @Override
    public EntityManager getEntityManager(UUID applicationId) {
        return factory.getEntityManager(applicationId);
    }

    @Override
    public UUID createApplication(String organizationName, String name) throws Exception {
        return factory.createApplication(organizationName, name);
    }

    @Override
    public UUID createApplication(String organizationName, String name, 
            Map<String, Object> properties) throws Exception {
        return factory.createApplication(organizationName, name, properties);
    }

    @Override
    public UUID importApplication(String organization, UUID applicationId, String name, 
            Map<String, Object> properties) throws Exception {
        return factory.importApplication(organization, applicationId, name, properties);
    }

    @Override
    public UUID lookupApplication(String name) throws Exception {
        return factory.lookupApplication(name);
    }

    @Override
    public Map<String, UUID> getApplications() throws Exception {
        return factory.getApplications();
    }

    @Override
    public void setup() throws Exception {
        factory.setup();
    }

    @Override
    public Map<String, String> getServiceProperties() {
        return factory.getServiceProperties();
    }

    @Override
    public boolean updateServiceProperties(Map<String, String> properties) {
        return factory.updateServiceProperties(properties);
    }

    @Override
    public boolean setServiceProperty(String name, String value) {
        return factory.setServiceProperty(name, value);
    }

    @Override
    public boolean deleteServiceProperty(String name) {
        return factory.deleteServiceProperty(name);
    }

    @Override
    public UUID initializeApplication(String orgName, UUID appId, String appName, 
            Map<String, Object> props) throws Exception {
        return factory.initializeApplication(orgName, appId, appName, props);
    }

    @Override
    public UUID getManagementAppId() {
        return factory.getManagementAppId();
    }

    @Override
    public UUID getDefaultAppId() {
        return factory.getDefaultAppId();
    }

    @Override
    public void refreshIndex() {
        factory.refreshIndex();
    }

    @Override
    public void setApplicationContext(ApplicationContext ac) throws BeansException {
        factory.setApplicationContext(ac);
    }

    @Override
    public void flushEntityManagerCaches() {
        factory.flushEntityManagerCaches();
    }

    @Override
    public void rebuildInternalIndexes(ProgressObserver po) throws Exception {
        factory.rebuildInternalIndexes(po);
    }

    @Override
    public void rebuildAllIndexes(ProgressObserver po) throws Exception {
        factory.rebuildAllIndexes(po);
    }

    @Override
    public void rebuildApplicationIndexes(UUID appId, ProgressObserver po) throws Exception {
        factory.rebuildApplicationIndexes(appId, po);
    }


    @Override
    public void migrateData() throws Exception {
        factory.migrateData();
    }


    @Override
    public String getMigrateDataStatus() {
        return factory.getMigrateDataStatus();
    }


    @Override
    public int getMigrateDataVersion() {
        return factory.getMigrateDataVersion();
    }


    @Override
    public void rebuildCollectionIndex(UUID appId, String collection, ProgressObserver po) {
        factory.rebuildCollectionIndex(appId, collection, po);
    }
}
