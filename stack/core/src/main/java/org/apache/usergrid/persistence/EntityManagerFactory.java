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
package org.apache.usergrid.persistence;


import java.util.Map;
import java.util.UUID;
import org.apache.usergrid.persistence.core.util.Health;
import org.springframework.context.ApplicationContext;


/**
 * The interface that specifies the operations that can be performed on the Usergrid Datastore. 
 * This interface is designed to be implemented by different backends. Although these 
 * operations are meant to take advantage of the capabilities of Cassandra, they should be 
 * implementable using other relational databases such as MySql or NoSQL databases such as GAE or 
 * MongoDB.
 */
public interface EntityManagerFactory {

    /**
     * A string description provided by the implementing class.
     *
     * @return description text
     *
     * @throws Exception the exception
     */
    public abstract String getImpementationDescription() throws Exception;

    /**
     * Gets the entity manager.
     *
     * @param applicationId the application id
     *
     * @return EntityDao for the specified parameters
     */
    public abstract EntityManager getEntityManager( UUID applicationId );

    /**
     * Creates a new application.
     *
     * @param name a unique application name.
     *
     * @return the newly created application id.
     *
     * @throws Exception the exception
     */
    public abstract UUID createApplication( String organizationName, String name ) throws Exception;

    /**
     * Creates a Application entity. All entities except for applications must be attached to a 
     * Application.
     *
     * @param name the name of the application to create.
     * @param properties property values to create in the new entity or null.
     *
     * @return the newly created application id.
     *
     * @throws Exception the exception
     */
    public abstract UUID createApplication( 
            String organizationName, String name, Map<String, Object> properties ) throws Exception;

    public abstract UUID importApplication( String organization, UUID applicationId, String name,
                                            Map<String, Object> properties ) throws Exception;

    /**
     * Returns the application id for the application name.
     *
     * @param name a unique application name.
     *
     * @return the Application id or null.
     *
     * @throws Exception the exception
     */
    public abstract UUID lookupApplication( String name ) throws Exception;

    /**
     * Returns all the applications in the system.
     *
     * @return all the applications.
     *
     * @throws Exception the exception
     */
    public abstract Map<String, UUID> getApplications() throws Exception;

    public abstract void setup() throws Exception;

    public abstract Map<String, String> getServiceProperties();

    public abstract boolean updateServiceProperties( Map<String, String> properties );

    public abstract boolean setServiceProperty( String name, String value );

    public abstract boolean deleteServiceProperty( String name );

    public UUID initializeApplication( 
        String orgName, UUID appId, String appName, Map<String, Object> props) throws Exception;
            
    public UUID getManagementAppId();

    public UUID getDefaultAppId();

    public void refreshIndex();

    public void rebuildAllIndexes( ProgressObserver po ) throws Exception;
    
    public void rebuildInternalIndexes( ProgressObserver po ) throws Exception;

    public void rebuildApplicationIndexes( UUID appId, ProgressObserver po ) throws Exception;

    /**
     * Perform any data migrations necessary in the system
     * @throws Exception
     */
    public void migrateData() throws Exception;

    /**
     * Return the migration status message
     * @return
     */
    public String getMigrateDataStatus();

    /**
     * Return the current migration version of the system
     * @return
     */
    public int getMigrateDataVersion();

    /**
     * Force the migration version to the specified version
     * @param version
     */
    public void setMigrationVersion(int version);

    public void setApplicationContext(ApplicationContext ac);

    /**
     * Perform a realtime count of every entity in the system.  This can be slow as it traverses the entire system graph
     * @return
     */
    public long performEntityCount();

    /** For testing purposes */
    public void flushEntityManagerCaches();

    public void rebuildCollectionIndex(UUID appId, String collection, ProgressObserver object);

    public Health getEntityStoreHealth();

    public interface ProgressObserver {

        public void onProgress( EntityRef entity);

        /**
         * Get the write delay time from the progress observer.  Used to throttle writes
         * @return
         */
        public long getWriteDelayTime();
    }
}
