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

package org.apache.usergrid.tools;


import com.google.common.collect.BiMap;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.usergrid.persistence.*;
import org.apache.usergrid.persistence.Results.Level;
import org.apache.usergrid.persistence.cassandra.CassandraService;
import org.apache.usergrid.utils.StringUtils;
import org.codehaus.jackson.JsonGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;


/**
 * Export Admin Users and metadata including organizations.
 *
 * java -jar usergrid-tools.jar ExportAdmins
 */
public class ExportAdmins extends ExportingToolBase {

    static final Logger logger = LoggerFactory.getLogger( ExportAdmins.class );
    public static final String ADMIN_USERS_PREFIX = "admin-users";
    public static final String ADMIN_USER_METADATA_PREFIX = "admin-user-metadata";
    private static final String READ_THREAD_COUNT = "readThreads";
    private int readThreadCount;


    /**
     * Represents an AdminUser that has been read and is ready for export.
     */
    class AdminUserWriteTask {
        Entity                           adminUser;
        Map<String, List<UUID>>          collectionsByName;
        Map<String, List<ConnectionRef>> connectionsByType;
        Map<String, Map<Object, Object>> dictionariesByName;
        BiMap<UUID, String>              orgNamesByUuid;
    }


    /**
     * Export admin users using multiple threads.
     * <p/>
     * How it works:
     * In main thread we query for IDs of all admin users, add each ID to read queue.
     * Read-queue workers read admin user data, add data to write queue.
     * One write-queue worker reads data writes to file.
     */
    @Override
    public void runTool(CommandLine line) throws Exception {
        startSpring();

        setVerbose( line );

        applyOrgId( line );
        prepareBaseOutputFileName( line );
        outputDir = createOutputParentDir();
        logger.info( "Export directory: " + outputDir.getAbsolutePath() );

        if (StringUtils.isNotEmpty( line.getOptionValue( READ_THREAD_COUNT ) )) {
            try {
                readThreadCount = Integer.parseInt( line.getOptionValue( READ_THREAD_COUNT ) );
            } catch (NumberFormatException nfe) {
                logger.error( "-" + READ_THREAD_COUNT + " must be specified as an integer. Aborting..." );
                return;
            }
        } else {
            readThreadCount = 20;
        }

        // start write queue worker

        BlockingQueue<AdminUserWriteTask> writeQueue = new LinkedBlockingQueue<AdminUserWriteTask>();
        AdminUserWriter adminUserWriter = new AdminUserWriter( writeQueue );
        Thread writeThread = new Thread( adminUserWriter );
        writeThread.start();
        logger.debug( "Write thread started" );

        // start read queue workers

        BlockingQueue<UUID> readQueue = new LinkedBlockingQueue<UUID>();
        List<AdminUserReader> readers = new ArrayList<AdminUserReader>();
        for (int i = 0; i < readThreadCount; i++) {
            AdminUserReader worker = new AdminUserReader( readQueue, writeQueue );
            Thread readerThread = new Thread( worker, "AdminUserReader-" + i );
            readerThread.start();
            readers.add( worker );
        }
        logger.debug( readThreadCount + " read worker threads started" );

        // query for IDs, add each to read queue

        Query query = new Query();
        query.setLimit( MAX_ENTITY_FETCH );
        query.setResultsLevel( Level.IDS );
        EntityManager em = emf.getEntityManager( CassandraService.MANAGEMENT_APPLICATION_ID );
        Results ids = em.searchCollection( em.getApplicationRef(), "users", query );

        while (ids.size() > 0) {
            for (UUID uuid : ids.getIds()) {
                readQueue.add( uuid );
                logger.debug( "Added uuid to readQueue: " + uuid );
            }
            if (ids.getCursor() == null) {
                break;
            }
            query.setCursor( ids.getCursor() );
            ids = em.searchCollection( em.getApplicationRef(), "users", query );
        }

        adminUserWriter.setDone( true );
        for (AdminUserReader aur : readers) {
            aur.setDone( true );
        }

        logger.debug( "Waiting for write thread to complete" );
        writeThread.join();
    }


    @Override
    @SuppressWarnings("static-access")
    public Options createOptions() {

        Options options = super.createOptions();

        Option readThreads = OptionBuilder
                .hasArg().withType(0).withDescription("Read Threads -" + READ_THREAD_COUNT).create(READ_THREAD_COUNT);

        options.addOption( readThreads );
        return options;
    }


    public class AdminUserReader implements Runnable {

        private boolean done = true;

        private final BlockingQueue<UUID> readQueue;
        private final BlockingQueue<AdminUserWriteTask> writeQueue;

        public AdminUserReader( BlockingQueue<UUID> readQueue, BlockingQueue<AdminUserWriteTask> writeQueue ) {
            this.readQueue = readQueue;
            this.writeQueue = writeQueue;
        }


        @Override
        public void run() {
            try {
                readAndQueueAdminUsers();
            } catch (Exception e) {
                logger.error("Error reading data for export", e);
            }
        }


        private void readAndQueueAdminUsers() throws Exception {

            EntityManager em = emf.getEntityManager( CassandraService.MANAGEMENT_APPLICATION_ID );

            while ( true ) {

                UUID uuid = null;
                try {
                    uuid = readQueue.poll( 30, TimeUnit.SECONDS );
                    logger.debug("Got item from entityId queue: " + uuid );

                    if ( uuid == null && done ) {
                        break;
                    }

                    Entity entity = em.get( uuid );

                    AdminUserWriteTask task = new AdminUserWriteTask();
                    task.adminUser = entity;

                    addCollectionsToTask(   task, entity );
                    addDictionariesToTask(  task, entity );
                    addConnectionsToTask(   task, entity );
                    addOrganizationsToTask( task, entity );

                    writeQueue.add( task );

                } catch ( Exception e ) {
                    logger.error("Error reading data for user " + uuid, e );
                }
            }
        }


        private void addCollectionsToTask(AdminUserWriteTask task, Entity entity) throws Exception {

            EntityManager em = emf.getEntityManager( CassandraService.MANAGEMENT_APPLICATION_ID );
            Set<String> collections = em.getCollections( entity );
            if ((collections == null) || collections.isEmpty()) {
                return;
            }

            task.collectionsByName = new HashMap<String, List<UUID>>();

            for (String collectionName : collections) {

                List<UUID> uuids = task.collectionsByName.get( collectionName );
                if ( uuids == null ) {
                    uuids = new ArrayList<UUID>();
                    task.collectionsByName.put( collectionName, uuids );
                }

                Results collectionMembers = em.getCollection( entity, collectionName, null, 100000, Level.IDS, false );

                List<UUID> entityIds = collectionMembers.getIds();

                if ((entityIds != null) && !entityIds.isEmpty()) {
                    for (UUID childEntityUUID : entityIds) {
                        uuids.add( childEntityUUID );
                    }
                }
            }
        }


        private void addDictionariesToTask(AdminUserWriteTask task, Entity entity) throws Exception {
            EntityManager em = emf.getEntityManager( CassandraService.MANAGEMENT_APPLICATION_ID );

            Set<String> dictionaries = em.getDictionaries( entity );

            task.dictionariesByName = new HashMap<String, Map<Object, Object>>();

            for (String dictionary : dictionaries) {
                Map<Object, Object> dict = em.getDictionaryAsMap( entity, dictionary );
                task.dictionariesByName.put( dictionary, dict );
            }
        }


        private void addConnectionsToTask(AdminUserWriteTask task, Entity entity) throws Exception {
            EntityManager em = emf.getEntityManager( CassandraService.MANAGEMENT_APPLICATION_ID );

            task.connectionsByType = new HashMap<String, List<ConnectionRef>>();

            Set<String> connectionTypes = em.getConnectionTypes( entity );
            for (String connectionType : connectionTypes) {

                List<ConnectionRef> connRefs = task.connectionsByType.get( connectionType );
                if ( connRefs == null ) {
                    connRefs = new ArrayList<ConnectionRef>();
                }

                Results results = em.getConnectedEntities( entity.getUuid(), connectionType, null, Level.IDS );
                List<ConnectionRef> connections = results.getConnections();

                for (ConnectionRef connectionRef : connections) {
                    connRefs.add( connectionRef );
                }
            }
        }


        private void addOrganizationsToTask(AdminUserWriteTask task, Entity entity) throws Exception {
            task.orgNamesByUuid = managementService.getOrganizationsForAdminUser( entity.getUuid() );
        }

        public void setDone(boolean done) {
            this.done = done;
        }
    }

    class AdminUserWriter implements Runnable {

        private boolean done = false;

        private final BlockingQueue<AdminUserWriteTask> taskQueue;

        public AdminUserWriter( BlockingQueue<AdminUserWriteTask> taskQueue ) {
            this.taskQueue = taskQueue;
        }


        @Override
        public void run() {
            try {
                writeEntities();
            } catch (Exception e) {
                logger.error("Error writing export data", e);
            }
        }


        private void writeEntities() throws Exception {
            EntityManager em = emf.getEntityManager( CassandraService.MANAGEMENT_APPLICATION_ID );

            // write one JSON file for management application users
            JsonGenerator usersFile =
                    getJsonGenerator( createOutputFile( ADMIN_USERS_PREFIX, em.getApplication().getName() ) );
            usersFile.writeStartArray();

            // write one JSON file for metadata: collections, connections and dictionaries of those users
            JsonGenerator metadataFile =
                    getJsonGenerator( createOutputFile( ADMIN_USER_METADATA_PREFIX, em.getApplication().getName() ) );
            metadataFile.writeStartObject();

            int count = 0;

            while ( true ) {

                try {
                    AdminUserWriteTask task = taskQueue.poll( 30, TimeUnit.SECONDS );
                    if ( task == null && done ) {
                        break;
                    }

                    // write user to application file
                    usersFile.writeObject( task.adminUser );
                    //usersFile.writeEndObject();
                    echo( task.adminUser );

                    // write metadata to metadata file
                    saveCollections(   metadataFile, task );
                    saveConnections(   metadataFile, task );
                    saveOrganizations( metadataFile, task );
                    saveDictionaries(  metadataFile, task );

                    logger.debug("Exported user {}", task.adminUser.getProperty( "email" ));

                    count++;
                    if ( count % 1000 == 0 ) {
                        logger.info("Exported {} admin users", count);
                    }


                } catch (InterruptedException e) {
                    throw new Exception("Interrupted", e);
                }
            }

            metadataFile.writeEndObject();
            metadataFile.close();

            usersFile.writeEndArray();
            usersFile.close();
        }


        private void saveCollections( JsonGenerator jg, AdminUserWriteTask task ) throws Exception {

            jg.writeFieldName( task.adminUser.getUuid().toString() );
            jg.writeStartObject();

            for (String collectionName : task.collectionsByName.keySet() ) {

                jg.writeFieldName( collectionName );
                jg.writeStartArray();

                List<UUID> entityIds = task.collectionsByName.get( collectionName );

                if ((entityIds != null) && !entityIds.isEmpty()) {
                    for (UUID childEntityUUID : entityIds) {
                        jg.writeObject( childEntityUUID.toString() );
                    }
                }

                jg.writeEndArray();
            }
        }


        private void saveDictionaries( JsonGenerator jg, AdminUserWriteTask task ) throws Exception {

            jg.writeFieldName( "dictionaries" );
            jg.writeStartObject();

            for (String dictionary : task.dictionariesByName.keySet() ) {

                Map<Object, Object> dict = task.dictionariesByName.get( dictionary );

                if (dict.isEmpty()) {
                    continue;
                }

                jg.writeFieldName( dictionary );

                jg.writeStartObject();

                for (Map.Entry<Object, Object> entry : dict.entrySet()) {
                    jg.writeFieldName( entry.getKey().toString() );
                    jg.writeObject( entry.getValue() );
                }

                jg.writeEndObject();
            }
            jg.writeEndObject();
        }


        private void saveConnections( JsonGenerator jg, AdminUserWriteTask task ) throws Exception {

            jg.writeFieldName( "connections" );
            jg.writeStartObject();

            for (String connectionType : task.connectionsByType.keySet() ) {

                jg.writeFieldName( connectionType );
                jg.writeStartArray();

                List<ConnectionRef> connections = task.connectionsByType.get( connectionType );
                for (ConnectionRef connectionRef : connections) {
                    jg.writeObject( connectionRef.getConnectedEntity().getUuid() );
                }

                jg.writeEndArray();
            }
            jg.writeEndObject();
        }


        private void saveOrganizations( JsonGenerator jg, AdminUserWriteTask task ) throws Exception {

            final BiMap<UUID, String> orgs = task.orgNamesByUuid;

            jg.writeFieldName( "organizations" );

            jg.writeStartArray();

            for (UUID uuid : orgs.keySet()) {

                jg.writeStartObject();

                jg.writeFieldName( "uuid" );
                jg.writeObject( uuid );

                jg.writeFieldName( "name" );
                jg.writeObject( orgs.get( uuid ) );

                jg.writeEndObject();
            }

            jg.writeEndArray();
        }

        public void setDone(boolean done) {
            this.done = done;
        }
    }
}

