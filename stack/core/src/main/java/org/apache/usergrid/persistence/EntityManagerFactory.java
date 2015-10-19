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


import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.common.base.Optional;

import org.apache.usergrid.corepersistence.index.ReIndexService;
import org.apache.usergrid.persistence.core.util.Health;
import org.apache.usergrid.persistence.index.EntityIndex;
import rx.Observable;


/**
 * The interface that specifies the operations that can be performed on the Usergrid Datastore.
 * This interface is designed to be implemented by different backends. Although these
 * operations are meant to take advantage of the capabilities of Cassandra, they should be
 * implementable using other relational databases such as MySql or NoSQL databases such as GAE or
 * MongoDB.
 */
public interface EntityManagerFactory {

    /**
     * Gets the entity manager.
     *
     * @param applicationId the application id
     *
     * @return EntityDao for the specified parameters
     */
    EntityManager getEntityManager( UUID applicationId );

    /**
     * get management app em
     * @return
     */
    EntityManager getManagementEntityManager();


        /**
         * Creates a new application.
         *
         * @param name a unique application name.
         *
         * @return Entity of type application_info that represents the newly created Application
         *
         * @throws Exception the exception
         */
    Entity createApplicationV2( String organizationName, String name ) throws Exception;

    /**
     * Creates a Application entity. All entities except for applications must be attached to a
     * Application.
     *
     * @param name the name of the application to create.
     * @param properties property values to create in the new entity or null.
     *
     * @return Entity of type application_info that represents the newly created Application
     *
     * @throws Exception the exception
     */
    Entity createApplicationV2(
        String organizationName, String name, UUID applicationId, Map<String, Object> properties ) throws Exception;


    /**
     * Delete Application.
     *
     * @param applicationId UUID of Application to be deleted.
     */
    void deleteApplication( UUID applicationId ) throws Exception;

//    /**
//     *
//     * @param applicationUUID
//     * @param collectionFromName
//     * @param collectionToName
//     * @return
//     * @throws Exception
//     */
//    Observable migrateAppInfo( UUID applicationUUID, String collectionFromName, String collectionToName) throws Exception;

    /**
     * Restore deleted application.
     */
    Entity restoreApplication( UUID applicationId) throws Exception;

    /**
     *
     * @param organization
     * @param applicationId
     * @param name
     * @param properties
     * @return
     * @throws Exception
     */
    UUID importApplication( String organization, UUID applicationId, String name,
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
    Optional<UUID> lookupApplication( String name ) throws Exception;

    /**
     * Returns all the applications in the system.
     *
     * @return all the applications.
     *
     * @throws Exception the exception
     */
    Map<String, UUID> getApplications() throws Exception;

    public Map<String, UUID> getDeletedApplications() throws Exception;

    /**
     * Sets up core system resources
     * @throws Exception
     */
    void setup() throws Exception;

    /**
     * Boostraps system data so that we can operate usergrid
     * @throws Exception
     */
    void boostrap() throws Exception;

    Map<String, String> getServiceProperties();

    boolean updateServiceProperties( Map<String, String> properties );

    boolean setServiceProperty( String name, String value );

    boolean deleteServiceProperty( String name );

    /**
     * @return Entity of type application_info that represents the newly created application.
     */
    public Entity initializeApplicationV2(
        String orgName, UUID appId, String appName, Map<String, Object> props) throws Exception;



    public UUID getManagementAppId();

    public EntityIndex.IndexRefreshCommandInfo refreshIndex(UUID applicationId);

    /**
     * Perform a realtime count of every entity in the system.  This can be slow as it traverses the entire system graph
     */
    public long performEntityCount();

    /** For testing purposes */
    public void flushEntityManagerCaches();


    public Health getEntityStoreHealth();

    public Health getIndexHealth();

    void initializeManagementIndex();

    public interface ProgressObserver {

        public void onProgress(EntityRef entity);

    }
}
