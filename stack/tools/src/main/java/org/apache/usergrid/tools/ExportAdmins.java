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
import com.google.common.collect.HashBiMap;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.management.UserInfo;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.persistence.index.query.Query;
import org.apache.usergrid.utils.StringUtils;
import org.codehaus.jackson.JsonGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Export Admin Users and metadata including organizations and passwords.
 *
 * Usage Example:
 *
 * java -Xmx8000m -Dlog4j.configuration=file:/home/me/log4j.properties -classpath . \
 *      -jar usergrid-tools-1.0.2.jar ImportAdmins -writeThreads 100 -auditThreads 100 \
 *      -host casshost -inputDir=/home/me/export-data
 *
 * If you want to provide any property overrides, put properties file named usergrid-custom-tools.properties
 * in the same directory where you run the above command. For example, you might want to set the Cassandra
 * client threads and export from a specific set of keyspaces:
 *
 *    cassandra.connections=110
 *    cassandra.system.keyspace=My_Usergrid
 *    cassandra.application.keyspace=My_Usergrid_Applications
 *    cassandra.lock.keyspace=My_Usergrid_Locks
 */
public class ExportAdmins extends ExportingToolBase {

    static final Logger logger = LoggerFactory.getLogger( ExportAdmins.class );

    public static final String ADMIN_USERS_PREFIX = "admin-users";
    public static final String ADMIN_USER_METADATA_PREFIX = "admin-user-metadata";

    // map admin user UUID to list of organizations to which user belongs
    private Map<UUID, List<Org>> userToOrgsMap = new HashMap<UUID, List<Org>>(50000);

    private Map<String, UUID> orgNameToUUID = new HashMap<String, UUID>(50000);

    private Set<UUID> orgsWritten = new HashSet<UUID>(50000);

    private Set<UUID> duplicateOrgs = new HashSet<UUID>();

    private static final String READ_THREAD_COUNT = "readThreads";
    private int readThreadCount;

    AtomicInteger userCount = new AtomicInteger( 0 );

    boolean ignoreInvalidUsers = false; // true to ignore users with no credentials or orgs


    /**
     * Represents an AdminUser that has been read and is ready for export.
     */
    class AdminUserWriteTask {
        Entity                           adminUser;
        Map<String, Map<Object, Object>> dictionariesByName;
        BiMap<UUID, String>              orgNamesByUuid;
    }


    /**
     * Represents an organization associated with a user.
     */
    private class Org {
        UUID orgId;
        String orgName;
        public Org( UUID orgId, String orgName ) {
            this.orgId = orgId;
            this.orgName = orgName;
        }
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

        buildOrgMap();

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
        query.setResultsLevel( Query.Level.IDS );
        EntityManager em = emf.getEntityManager( CpNamingUtils.MANAGEMENT_APPLICATION_ID );
        Results ids = em.searchCollection( em.getApplicationRef(), "users", query );

        while (ids.size() > 0) {
            for (UUID uuid : ids.getIds()) {
                readQueue.add( uuid );
                //logger.debug( "Added uuid to readQueue: " + uuid );
            }
            if (ids.getCursor() == null) {
                break;
            }
            query.setCursor( ids.getCursor() );
            ids = em.searchCollection( em.getApplicationRef(), "users", query );
        }

        logger.debug( "Waiting for write thread to complete" );

        boolean done = false;
        while ( !done ) {
            writeThread.join( 10000, 0 );
            done = !writeThread.isAlive();
            logger.info( "Wrote {} users", userCount.get() );
        }
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


    /**
     * Shouldn't have to do this but getOrganizationsForAdminUser() is not 100% reliable in some Usergrid installations.
     */
    private void buildOrgMap() throws Exception {

        logger.info( "Building org map" );

        ExecutorService execService = Executors.newFixedThreadPool( this.readThreadCount );

        EntityManager em = emf.getEntityManager( CpNamingUtils.MANAGEMENT_APPLICATION_ID );
        String queryString = "select *";
        Query query = Query.fromQL( queryString );
        query.withLimit( 1000 );
        Results organizations = null;
        int count = 0;
        do {
            organizations = em.searchCollection( em.getApplicationRef(), "groups", query );
            for ( Entity organization : organizations.getEntities() ) {
                execService.submit( new OrgMapWorker( organization ) );
                count++;
            }

            if ( count % 1000 == 0 ) {
                logger.info("Queued {} org map workers", count);
            }
            query.setCursor( organizations.getCursor() );
        }
        while ( organizations != null && organizations.hasCursor() );

        execService.shutdown();
        while ( !execService.awaitTermination( 10, TimeUnit.SECONDS ) ) {
            logger.info( "Processed {} orgs for map", userToOrgsMap.size() );
        }

        logger.info("Org map complete, counted {} organizations", count);
    }


    public class OrgMapWorker implements Runnable {
        private final Entity orgEntity;

        public OrgMapWorker( Entity orgEntity ) {
            this.orgEntity = orgEntity;
        }

        @Override
        public void run() {
            try {
                final String orgName = orgEntity.getProperty( "path" ).toString();
                final UUID orgId = orgEntity.getUuid();

                for (UserInfo user : managementService.getAdminUsersForOrganization( orgEntity.getUuid() )) {
                    try {
                        Entity admin = managementService.getAdminUserEntityByUuid( user.getUuid() );
                        Org org = new Org( orgId, orgName );

                        synchronized (userToOrgsMap) {
                            List<Org> userOrgs = userToOrgsMap.get( admin.getUuid() );
                            if (userOrgs == null) {
                                userOrgs = new ArrayList<Org>();
                                userToOrgsMap.put( admin.getUuid(), userOrgs );
                            }
                            userOrgs.add( org );
                        }

                        synchronized (orgNameToUUID) {
                            UUID existingOrgId = orgNameToUUID.get( orgName );
                            ;
                            if (existingOrgId != null && !orgId.equals( existingOrgId )) {
                                if ( !duplicateOrgs.contains( orgId )) {
                                    logger.info( "Org {}:{} is a duplicate", orgId, orgName );
                                    duplicateOrgs.add(orgId);
                                }
                            } else {
                                orgNameToUUID.put( orgName, orgId );
                            }
                        }

                    } catch (Exception e) {
                        logger.warn( "Cannot get orgs for userId {}", user.getUuid() );
                    }
                }
            } catch ( Exception e ) {
                logger.error("Error getting users for org {}:{}", orgEntity.getName(), orgEntity.getUuid());
            }
        }
    }


    public class AdminUserReader implements Runnable {

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

            EntityManager em = emf.getEntityManager( CpNamingUtils.MANAGEMENT_APPLICATION_ID );

            while ( true ) {

                UUID uuid = null;
                try {
                    uuid = readQueue.poll( 30, TimeUnit.SECONDS );
                    if ( uuid == null ) {
                        break;
                    }

                    Entity entity = em.get( uuid );

                    AdminUserWriteTask task = new AdminUserWriteTask();
                    task.adminUser = entity;

                    addDictionariesToTask( task, entity );
                    addOrganizationsToTask( task );

                    String actionTaken = "Processed";

                    if (ignoreInvalidUsers && (task.orgNamesByUuid.isEmpty()
                            || task.dictionariesByName.isEmpty()
                            || task.dictionariesByName.get( "credentials" ).isEmpty())) {

                        actionTaken = "Ignored";

                    } else {
                        writeQueue.add( task );
                    }

                    Map<String, Object> creds = (Map<String, Object>) (task.dictionariesByName.isEmpty() ?
                                                0 : task.dictionariesByName.get( "credentials" ));

                    logger.error( "{} admin user {}:{}:{} has organizations={} dictionaries={} credentials={}",
                            new Object[]{
                                    actionTaken,
                                    task.adminUser.getProperty( "username" ),
                                    task.adminUser.getProperty( "email" ),
                                    task.adminUser.getUuid(),
                                    task.orgNamesByUuid.size(),
                                    task.dictionariesByName.size(),
                                    creds == null ? 0 : creds.size()
                            } );

                } catch ( Exception e ) {
                    logger.error("Error reading data for user " + uuid, e );
                }
            }
        }


        private void addDictionariesToTask(AdminUserWriteTask task, Entity entity) throws Exception {
            EntityManager em = emf.getEntityManager( CpNamingUtils.MANAGEMENT_APPLICATION_ID );

            task.dictionariesByName = new HashMap<String, Map<Object, Object>>();

            Set<String> dictionaries = em.getDictionaries( entity );

            if ( dictionaries.isEmpty() ) {
                logger.error("User {}:{} has no dictionaries", task.adminUser.getName(), task.adminUser.getUuid() );
                return;
            }

            Map<Object, Object> credentialsDictionary = em.getDictionaryAsMap( entity, "credentials" );

            if ( credentialsDictionary != null ) {
                task.dictionariesByName.put( "credentials", credentialsDictionary );
            }
        }

        private void addOrganizationsToTask(AdminUserWriteTask task) throws Exception {

            task.orgNamesByUuid = managementService.getOrganizationsForAdminUser( task.adminUser.getUuid() );

            List<Org> orgs = userToOrgsMap.get( task.adminUser.getProperty( "username" ).toString().toLowerCase() );

            if ( orgs != null && task.orgNamesByUuid.size() < orgs.size() ) {

                // list of orgs from getOrganizationsForAdminUser() is less than expected, use userToOrgsMap
                BiMap<UUID, String> bimap = HashBiMap.create();
                for (Org org : orgs) {
                    bimap.put( org.orgId, org.orgName );
                }
                task.orgNamesByUuid = bimap;
            }
        }
    }

    class AdminUserWriter implements Runnable {

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
            EntityManager em = emf.getEntityManager( CpNamingUtils.MANAGEMENT_APPLICATION_ID );

            // write one JSON file for management application users
            JsonGenerator usersFile =
                    getJsonGenerator( createOutputFile( ADMIN_USERS_PREFIX, em.getApplication().getName() ) );
            usersFile.writeStartArray();

            // write one JSON file for metadata: collections, connections and dictionaries of those users
            JsonGenerator metadataFile =
                    getJsonGenerator( createOutputFile( ADMIN_USER_METADATA_PREFIX, em.getApplication().getName() ) );
            metadataFile.writeStartObject();

            while ( true ) {

                try {
                    AdminUserWriteTask task = taskQueue.poll( 30, TimeUnit.SECONDS );
                    if ( task == null ) {
                        break;
                    }

                    // write user to application file
                    usersFile.writeObject( task.adminUser );
                    echo( task.adminUser );

                    // write metadata to metadata file
                    metadataFile.writeFieldName( task.adminUser.getUuid().toString() );
                    metadataFile.writeStartObject();

                    saveOrganizations( metadataFile, task );
                    saveDictionaries( metadataFile, task );

                    metadataFile.writeEndObject();

                    logger.debug( "Exported user {}:{}:{}", new Object[] {
                        task.adminUser.getProperty("username"),
                        task.adminUser.getProperty("email"),
                        task.adminUser.getUuid() } );

                    userCount.addAndGet( 1 );

                } catch (InterruptedException e) {
                    throw new Exception("Interrupted", e);
                }
            }

            metadataFile.writeEndObject();
            metadataFile.close();

            usersFile.writeEndArray();
            usersFile.close();

            logger.info( "Exported TOTAL {} admin users and {} organizations", userCount.get(), orgsWritten.size() );
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

                synchronized (orgsWritten) {
                    orgsWritten.add( uuid );
                }
            }

            jg.writeEndArray();
        }
    }
}

