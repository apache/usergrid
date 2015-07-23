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


import com.sun.org.apache.bcel.internal.generic.DUP;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.io.filefilter.PrefixFileFilter;
import org.apache.usergrid.management.OrganizationInfo;
import org.apache.usergrid.management.UserInfo;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.EntityRef;
import org.apache.usergrid.persistence.Identifier;
import org.apache.usergrid.persistence.SimpleEntityRef;
import org.apache.usergrid.persistence.entities.User;
import org.apache.usergrid.persistence.exceptions.DuplicateUniquePropertyExistsException;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.usergrid.persistence.Schema.PROPERTY_TYPE;
import static org.apache.usergrid.persistence.Schema.PROPERTY_UUID;
import static org.apache.usergrid.persistence.cassandra.CassandraService.MANAGEMENT_APPLICATION_ID;


/**
 * Import Admin Users and metadata including organizations and passwords.
 * 
 * Usage Example: 
 * 
 * java -Xmx8000m -Dlog4j.configuration=file:/home/me/log4j.properties -classpath . \
 *      -jar usergrid-tools-1.0.2.jar ImportAdmins -writeThreads 100 -auditThreads 100 \
 *      -host casshost -inputDir=/home/me/import-data 
 *      
 * If you want to provide any property overrides, put properties file named usergrid-custom-tools.properties
 * in the same directory where you run the above command. For example, you might want to set the Cassandra
 * client threads and import to a specific set of keyspaces:
 *
 *    cassandra.connections=110
 *    cassandra.system.keyspace=My_Other_Usergrid
 *    cassandra.application.keyspace=My_Other_Usergrid_Applications
 *    cassandra.lock.keyspace=My_Other_Usergrid_Locks
 */
public class ImportAdmins extends ToolBase {

    private static final Logger logger = LoggerFactory.getLogger(ImportAdmins.class);

    /**
     * Input directory where the .json export files are
     */
    static final String INPUT_DIR = "inputDir";
    static final String WRITE_THREAD_COUNT = "writeThreads";
    static final String AUDIT_THREAD_COUNT = "auditThreads";

    static File importDir;

    static final String DEFAULT_INPUT_DIR = "export";

    private Map<Stoppable, Thread> adminWriteThreads = new HashMap<Stoppable, Thread>();
    private Map<Stoppable, Thread> adminAuditThreads = new HashMap<Stoppable, Thread>();
    private Map<Stoppable, Thread> metadataWorkerThreadMap = new HashMap<Stoppable, Thread>();

    Map<UUID, DuplicateUser> dupsByDupUuid = new HashMap<UUID, DuplicateUser>(200);

    JsonFactory jsonFactory = new JsonFactory();

    AtomicInteger userCount = new AtomicInteger( 0 );
    AtomicInteger metadataCount = new AtomicInteger( 0 );

    AtomicInteger writeEmptyCount = new AtomicInteger( 0 );
    AtomicInteger auditEmptyCount = new AtomicInteger( 0 );
    AtomicInteger metadataEmptyCount = new AtomicInteger( 0 );
    
    
    static class DuplicateUser {
        String email;
        String username;
        public DuplicateUser( String propName, Map<String, Object> user ) {
            if ( "email".equals(propName)) {
                email = user.get("email").toString();
            } else {
                username = user.get("username").toString();
            }
        }
    }
    


    @Override
    @SuppressWarnings("static-access")
    public Options createOptions() {

        Option hostOption = OptionBuilder.withArgName("host")
                .hasArg()
                .withDescription("Cassandra host").create("host");

        Option inputDir = OptionBuilder
                .hasArg()
                .withDescription("input directory -inputDir").create(INPUT_DIR);

        Option writeThreads = OptionBuilder
                .hasArg()
                .withDescription("Write Threads -writeThreads").create(WRITE_THREAD_COUNT);

        Option auditThreads = OptionBuilder
                .hasArg()
                .withDescription("Audit Threads -auditThreads").create(AUDIT_THREAD_COUNT);

        Option verbose = OptionBuilder
                .withDescription("Print on the console an echo of the content written to the file")
                .create(VERBOSE);

        Options options = new Options();
        options.addOption(hostOption);
        options.addOption(writeThreads);
        options.addOption(auditThreads);
        options.addOption( inputDir );
        options.addOption( verbose );

        return options;
    }


    @Override
    public void runTool(CommandLine line) throws Exception {

        startSpring();

        setVerbose(line);

        openImportDirectory(line);

        int auditThreadCount = 1;
        int writeThreadCount = 1;

        if (line.hasOption(AUDIT_THREAD_COUNT)) {
            auditThreadCount = Integer.parseInt(line.getOptionValue(AUDIT_THREAD_COUNT));
        }

        if (line.hasOption(WRITE_THREAD_COUNT)) {
            writeThreadCount = Integer.parseInt( line.getOptionValue(WRITE_THREAD_COUNT));
        }

        importAdminUsers( writeThreadCount, auditThreadCount );

        importMetadata( writeThreadCount );
    }


    /**
     * Import admin users.
     */
    private void importAdminUsers(int writeThreadCount, int auditThreadCount) throws Exception {

        String[] fileNames = importDir.list(new PrefixFileFilter(ExportAdmins.ADMIN_USERS_PREFIX + "."));

        logger.info( "Applications to read: " + fileNames.length );

        for (String fileName : fileNames) {
            try {
                importAdminUsers(fileName, writeThreadCount, auditThreadCount);
            } catch (Exception e) {
                logger.warn("Unable to import application: " + fileName, e);
            }
        }
    }


    /**
     * Imports admin users.
     *
     * @param fileName Name of admin user data file.
     */
    private void importAdminUsers(final String fileName,
                                  final int writeThreadCount,
                                  final int auditThreadCount) throws Exception {

        int count = 0;

        File adminUsersFile = new File(importDir, fileName);

        logger.info("----- Loading file: " + adminUsersFile.getAbsolutePath());
        JsonParser jp = getJsonParserForFile(adminUsersFile);

        int loopCounter = 0;

        BlockingQueue<Map<String, Object>> workQueue = new LinkedBlockingQueue<Map<String, Object>>();
        BlockingQueue<Map<String, Object>> auditQueue = new LinkedBlockingQueue<Map<String, Object>>();

        startAdminWorkers(workQueue, auditQueue, writeThreadCount);
        startAdminAuditors(auditQueue, auditThreadCount);

        JsonToken token = jp.nextToken();
        validateStartArray(token);

        while (jp.nextValue() != JsonToken.END_ARRAY) {
            loopCounter += 1;

            @SuppressWarnings("unchecked")
            Map<String, Object> entityProps = jp.readValueAs(HashMap.class);
            if (loopCounter % 1000 == 0) {
                logger.debug( "Publishing to queue... counter=" + loopCounter );
            }

            workQueue.add( entityProps );
        }

        waitForQueueAndMeasure(workQueue, writeEmptyCount, adminWriteThreads, "Admin Write");
        waitForQueueAndMeasure(auditQueue, auditEmptyCount, adminAuditThreads, "Admin Audit");

        logger.info("----- End: Imported {} admin users from file {}",
                count, adminUsersFile.getAbsolutePath());

        jp.close();
    }

    private static void waitForQueueAndMeasure(final BlockingQueue workQueue,
                                               final AtomicInteger emptyCounter,
                                               final Map<Stoppable, Thread> threadMap,
                                               final String identifier) throws InterruptedException {
        double rateAverageSum = 0;
        int iterations = 0;

        while ( emptyCounter.get() < threadMap.size() ) {
            iterations += 1;

            int sizeLast = workQueue.size();
            long lastTime = System.currentTimeMillis();
            logger.info("Queue {} is not empty, remaining size={}, waiting...", identifier, sizeLast);
            Thread.sleep(10000);

            long timeNow = System.currentTimeMillis();
            int sizeNow = workQueue.size();

            int processed = sizeLast - sizeNow;

            long timeDelta = timeNow - lastTime;

            double rateLast = (double) processed / (timeDelta / 1000);
            rateAverageSum += rateLast;

            long timeRemaining = (long) ( sizeLast / (rateAverageSum / iterations) );

            logger.info("++PROGRESS ({}): sizeLast={} nowSize={} processed={} rateLast={}/s rateAvg={}/s timeRemaining={}s",
                new Object[] { 
                    identifier, sizeLast, sizeNow, processed, rateLast, (rateAverageSum / iterations), timeRemaining } );
        }

        for (Stoppable worker : threadMap.keySet()) {
            worker.setDone(true);
        }
    }

    private void startAdminAuditors(BlockingQueue<Map<String, Object>> auditQueue, int workerCount) {
        for (int x = 0; x < workerCount; x++) {
            AuditWorker worker = new AuditWorker(auditQueue);
            Thread workerThread = new Thread(worker, "AdminAuditor-" + x);
            workerThread.start();
            adminAuditThreads.put(worker, workerThread);
        }
        logger.info("Started {} admin auditors", workerCount);

    }


    private void startAdminWorkers(BlockingQueue<Map<String, Object>> workQueue,
                                   BlockingQueue<Map<String, Object>> auditQueue,
                                   int workerCount) {

        for (int x = 0; x < workerCount; x++) {
            ImportAdminWorker worker = new ImportAdminWorker(workQueue, auditQueue);
            Thread workerThread = new Thread(worker, "AdminWriter-" + x);
            workerThread.start();
            adminWriteThreads.put(worker, workerThread);
        }

        logger.info("Started {} admin workers", workerCount);
    }


    private String getType(Map<String, Object> entityProps) {
        return (String) entityProps.get(PROPERTY_TYPE);
    }


    private UUID getId(Map<String, Object> entityProps) {
        return UUID.fromString((String) entityProps.get(PROPERTY_UUID));
    }


    private void validateStartArray(JsonToken token) {
        if (token != JsonToken.START_ARRAY) {
            throw new RuntimeException("Token should be START ARRAY but it is:" + token.asString());
        }
    }


    private JsonParser getJsonParserForFile(File organizationFile) throws Exception {
        JsonParser jp = jsonFactory.createJsonParser( organizationFile );
        jp.setCodec( new ObjectMapper() );
        return jp;
    }


    /**
     * Import collections. Collections files are named: collections.<application_name>.Timestamp.json
     */
    private void importMetadata(int writeThreadCount) throws Exception {

        String[] fileNames = importDir.list(
                new PrefixFileFilter( ExportAdmins.ADMIN_USER_METADATA_PREFIX + "." ) );
        logger.info( "Metadata files to read: " + fileNames.length );

        for (String fileName : fileNames) {
            try {
                importMetadata(fileName, writeThreadCount);
            } catch (Exception e) {
                logger.warn("Unable to import metadata file: " + fileName, e);
            }
        }
    }

    private void startMetadataWorkers(BlockingQueue<ImportMetadataTask> workQueue, int writeThreadCount) {

        for (int x = 0; x < writeThreadCount; x++) {
            ImportMetadataWorker worker = new ImportMetadataWorker(workQueue);
            Thread workerThread = new Thread(worker, "ImportMetadataTask-" + x );
            workerThread.start();
            metadataWorkerThreadMap.put(worker, workerThread);
        }
        
        logger.info( "Started {} metadata workers", writeThreadCount );
    }


    @SuppressWarnings("unchecked")
    private void importMetadata(String fileName, int writeThreads) throws Exception {

        EntityManager em = emf.getEntityManager(MANAGEMENT_APPLICATION_ID);

        File metadataFile = new File(importDir, fileName);

        logger.info("----- Loading metadata file: " + metadataFile.getAbsolutePath());

        JsonParser jp = getJsonParserForFile(metadataFile);

        JsonToken jsonToken = null; // jp.nextToken();// START_OBJECT this is the outer hashmap

        int depth = 1;

        BlockingQueue<ImportMetadataTask> workQueue = new LinkedBlockingQueue<ImportMetadataTask>();
        startMetadataWorkers(workQueue, writeThreads);

        while (depth > 0) {

            jsonToken = jp.nextToken();

            if (jsonToken == null) {
                logger.info("token is null, breaking");
                break;
            }

            if (jsonToken.equals(JsonToken.START_OBJECT)) {
                depth++;
            } else if (jsonToken.equals(JsonToken.END_OBJECT)) {
                depth--;
            }

            if (jsonToken.equals(JsonToken.FIELD_NAME) && depth == 2) {

                jp.nextToken();
                String entityOwnerId = jp.getCurrentName();

                try {
                    EntityRef entityRef = new SimpleEntityRef( "user", UUID.fromString( entityOwnerId ) );
                    Map<String, Object> metadata = (Map<String, Object>) jp.readValueAs( Map.class );
                    
                    workQueue.put( new ImportMetadataTask( entityRef, metadata ) );
                    logger.debug( "Put user {} in metadata queue", entityRef.getUuid() );
                    
                } catch ( Exception e ) {
                    logger.debug( "Error with user {}, not putting in metadata queue", entityOwnerId );
                }
            }
        }

        waitForQueueAndMeasure(workQueue, metadataEmptyCount, metadataWorkerThreadMap, "Metadata Load");

        logger.info("----- End of metadata -----");
        jp.close();
    }


    /**
     * Imports the entity's connecting references (collections and connections)
     */
    @SuppressWarnings("unchecked")
    private void importEntityMetadata(
            EntityManager em, EntityRef entityRef, Map<String, Object> metadata) throws Exception {

        DuplicateUser dup = dupsByDupUuid.get( entityRef.getUuid() );
        
        if ( dup == null ) { // not a duplicate

            User user = em.get( entityRef, User.class );
            final UserInfo userInfo = managementService.getAdminUserByEmail( user.getEmail() );

            if (user == null || userInfo == null) {
                logger.error( "User {} does not exist, not processing metadata", entityRef.getUuid() );
                return;
            }

            List<Object> organizationsList = (List<Object>) metadata.get("organizations");
            if (organizationsList != null && !organizationsList.isEmpty()) {

                for (Object orgObject : organizationsList) {

                    Map<String, Object> orgMap = (Map<String, Object>) orgObject;
                    UUID orgUuid = UUID.fromString( (String) orgMap.get( "uuid" ) );
                    String orgName = (String) orgMap.get( "name" );

                    OrganizationInfo orgInfo = managementService.getOrganizationByUuid( orgUuid );

                    if (orgInfo == null) { // org does not exist yet, create it and add user
                        try {
                            managementService.createOrganization( orgUuid, orgName, userInfo, false );
                            orgInfo = managementService.getOrganizationByUuid( orgUuid );

                            logger.debug( "Created new org {} for user {}",
                                    new Object[]{orgInfo.getName(), user.getEmail()} );

                        } catch (DuplicateUniquePropertyExistsException dpee) {
                            logger.debug( "Org {} already exists", orgName );
                        }
                    } else { // org exists, add original user to it
                        try {
                            managementService.addAdminUserToOrganization( userInfo, orgInfo, false );
                            logger.debug( "Added to org user {}:{}:{}",
                                    new Object[]{
                                            orgInfo.getName(),
                                            user.getUsername(),
                                            user.getEmail(),
                                            user.getUuid()
                                    });

                        } catch (Exception e) {
                            logger.error( "Error Adding user {} to org {}", new Object[]{user.getEmail(), orgName} );
                        }
                    }
                }
            }
            
            Map<String, Object> dictionariesMap = (Map<String, Object>) metadata.get("dictionaries");
            if (dictionariesMap != null && !dictionariesMap.isEmpty()) {
                for (String name : dictionariesMap.keySet()) {
                    try {
                        Map<String, Object> dictionary = (Map<String, Object>) dictionariesMap.get(name);
                        em.addMapToDictionary( entityRef, name, dictionary);

                        logger.debug( "Creating dictionary for {} name {}",
                                new Object[]{entityRef, name} );

                    } catch (Exception e) {
                        if (logger.isDebugEnabled()) {
                            logger.error("Error importing dictionary name "
                                    + name + " for user " + entityRef.getUuid(), e);
                        } else {
                            logger.error("Error importing dictionary name "
                                    + name + " for user " + entityRef.getUuid());
                        }
                    }
                }

            } else {
                logger.warn("User {} has no dictionaries", entityRef.getUuid() );
            }
            
        } else { // this is a duplicate user, so merge orgs

            logger.info("Processing duplicate username={} email={}", dup.email, dup.username );
           
            Identifier identifier = dup.email != null ? 
                Identifier.fromEmail( dup.email ) : Identifier.from( dup.username );
            User originalUser = em.get( em.getUserByIdentifier(identifier), User.class );

            // get map of original user's orgs
            
            UserInfo originalUserInfo = managementService.getAdminUserByEmail( originalUser.getEmail() );
            Map<String, Object> originalUserOrgData =
                    managementService.getAdminUserOrganizationData( originalUser.getUuid() );
            Map<String, Map<String, Object>> originalUserOrgs =
                    (Map<String, Map<String, Object>>) originalUserOrgData.get( "organizations" );

            // loop through duplicate user's orgs and give orgs to original user

            List<Object> organizationsList = (List<Object>) metadata.get("organizations");
            for (Object orgObject : organizationsList) {

                Map<String, Object> orgMap = (Map<String, Object>) orgObject;
                UUID orgUuid = UUID.fromString( (String) orgMap.get( "uuid" ) );
                String orgName = (String) orgMap.get( "name" );

                if (originalUserOrgs.get( orgName ) == null) { // original user does not have this org

                    OrganizationInfo orgInfo = managementService.getOrganizationByUuid( orgUuid );
                    
                    if (orgInfo == null) { // org does not exist yet, create it and add original user to it
                        try {
                            managementService.createOrganization( orgUuid, orgName, originalUserInfo, false );
                            orgInfo = managementService.getOrganizationByUuid( orgUuid );

                            logger.debug( "Created new org {} for user {}:{}:{} from duplicate user {}:{}",
                                new Object[]{
                                        orgInfo.getName(),
                                        originalUser.getUsername(), 
                                        originalUser.getEmail(),
                                        originalUser.getUuid(), 
                                        dup.username, dup.email
                                });

                        } catch (DuplicateUniquePropertyExistsException dpee) {
                            logger.debug( "Org {} already exists", orgName );
                        }
                    } else { // org exists so add original user to it
                        try {
                            managementService.addAdminUserToOrganization( originalUserInfo, orgInfo, false );
                            logger.debug( "Added to org user {}:{}:{} from duplicate user {}:{}",
                                    new Object[]{
                                            orgInfo.getName(),
                                            originalUser.getUsername(), 
                                            originalUser.getEmail(),
                                            originalUser.getUuid(), 
                                            dup.username, dup.email
                                    });

                        } catch (Exception e) {
                            logger.error( "Error Adding user {} to org {}", 
                                    new Object[]{originalUserInfo.getEmail(), orgName} );
                        }
                    }
                    
                } // else original user already has this org
                
            }
        }
    }


    /**
     * Open up the import directory based on <code>importDir</code>
     */
    private void openImportDirectory(CommandLine line) {

        boolean hasInputDir = line.hasOption(INPUT_DIR);

        if (hasInputDir) {
            importDir = new File(line.getOptionValue(INPUT_DIR));
        } else {
            importDir = new File(DEFAULT_INPUT_DIR);
        }

        logger.info("Importing from:" + importDir.getAbsolutePath());
        logger.info("Status. Exists: " + importDir.exists() + " - Readable: " + importDir.canRead());
    }


    interface Stoppable {
        void setDone(boolean done);
    }

    class AuditWorker implements Runnable, Stoppable {
        private BlockingQueue<Map<String, Object>> workQueue;
        private boolean done;

        public AuditWorker(BlockingQueue<Map<String, Object>> workQueue) {
            this.workQueue = workQueue;
        }

        @Override
        public void setDone(boolean done) {
            this.done = done;
        }

        @Override
        public void run() {
            int count = 0;

            EntityManager em = emf.getEntityManager(MANAGEMENT_APPLICATION_ID);

            long durationSum = 0;

            while (!done) {
                try {
                    Map<String, Object> entityProps = this.workQueue.poll(30, TimeUnit.SECONDS);

                    if (entityProps == null) {
                        logger.warn("Reading from AUDIT queue was null!");
                        auditEmptyCount.getAndIncrement();
                        Thread.sleep(1000);
                        continue;
                    }
                    auditEmptyCount.set(0);

                    count++;
                    long startTime = System.currentTimeMillis();

                    UUID uuid = (UUID) entityProps.get(PROPERTY_UUID);
                    String type = getType(entityProps);

                    if (em.get(uuid) == null) {
                        logger.error( "FATAL ERROR: wrote an entity {}:{} and it's missing", uuid, type );
                        System.exit(1);
                    }

                    echo(entityProps);

                    long stopTime = System.currentTimeMillis();

                    long duration = stopTime - startTime;
                    durationSum += duration;

                    //logger.debug( "Audited {}th admin", userCount );
                    
                    if ( count % 100 == 0 ) {
                        logger.info( "Audited {}. Average Audit Rate: {}(ms)", count, durationSum / count );
                    }

                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }


    class ImportMetadataTask {
        public EntityRef entityRef;
        public Map<String, Object> metadata;

        public ImportMetadataTask(EntityRef entityRef, Map<String, Object> metadata) {
            this.entityRef = entityRef;
            this.metadata = metadata;
        }
    }

    class ImportMetadataWorker implements Runnable, Stoppable {
        private BlockingQueue<ImportMetadataTask> workQueue;
        private boolean done = false;

        public ImportMetadataWorker(final BlockingQueue<ImportMetadataTask> workQueue) {
            this.workQueue = workQueue;

        }

        @Override
        public void setDone(boolean done) {
            this.done = done;
        }

        @Override
        public void run() {
            int count = 0;

            EntityManager em = emf.getEntityManager(MANAGEMENT_APPLICATION_ID);

            long durationSum = 0;

            while (!done) {
                try {
                    ImportMetadataTask task = this.workQueue.poll( 30, TimeUnit.SECONDS );

                    if (task == null) {
                        logger.warn("Reading from metadata queue was null!");
                        metadataEmptyCount.getAndIncrement();
                        Thread.sleep(1000);
                        continue;
                    }
                    metadataEmptyCount.set( 0 );
                    
                    long startTime = System.currentTimeMillis();
                    
                    importEntityMetadata( em, task.entityRef, task.metadata );
                    
                    long stopTime = System.currentTimeMillis();
                    long duration = stopTime - startTime;
                    durationSum += duration;
                    metadataCount.getAndIncrement();
                    count++;
                    
                    if ( count % 30 == 0 ) {
                        logger.info( "Imported {} metadata of total {} expected. " +
                                        "Average metadata Imported Rate: {}(ms)", 
                           new Object[] { metadataCount.get(), userCount.get(), durationSum / count });
                    }

                } catch (Exception e) {
                    logger.debug("Error reading writing metadata", e);
                }
            }
        }
    }


    class ImportAdminWorker implements Runnable, Stoppable {

        private BlockingQueue<Map<String, Object>> workQueue;
        private BlockingQueue<Map<String, Object>> auditQueue;
        private boolean done = false;


        public ImportAdminWorker(final BlockingQueue<Map<String, Object>> workQueue,
                                 final BlockingQueue<Map<String, Object>> auditQueue) {
            this.workQueue = workQueue;
            this.auditQueue = auditQueue;
        }

        @Override
        public void setDone(boolean done) {
            this.done = done;
        }

        @Override
        public void run() {
            int count = 0;

            EntityManager em = emf.getEntityManager(MANAGEMENT_APPLICATION_ID);

            long durationSum = 0;

            while (!done) {

                try {

                    Map<String, Object> entityProps = this.workQueue.poll(30, TimeUnit.SECONDS);

                    if (entityProps == null) {
                        logger.warn("Reading from admin import queue was null!");
                        writeEmptyCount.getAndIncrement();
                        Thread.sleep( 1000 );
                        continue;
                    }
                    writeEmptyCount.set(0);

                    // Import/create the entity
                    UUID uuid = getId(entityProps);
                    String type = getType( entityProps );

                    try {
                        long startTime = System.currentTimeMillis();
                        
                        em.create(uuid, type, entityProps);

                        logger.debug( "Imported admin user {}:{}:{}",
                            new Object[] { entityProps.get( "username" ), entityProps.get("email"), uuid } );

                        userCount.getAndIncrement();
                        auditQueue.put(entityProps);
                        long stopTime = System.currentTimeMillis();
                        long duration = stopTime - startTime;
                        durationSum += duration;
                        
                        count++;
                        if (count % 30 == 0) {
                            logger.info( "This worked has imported {} users of total {} imported so far. " +
                                            "Average Creation Rate: {}ms", 
                                new Object[] { count, userCount.get(), durationSum / count });
                        }
                        
                    } catch (DuplicateUniquePropertyExistsException de) {
                        String dupProperty = de.getPropertyName();
                        handleDuplicateAccount( em, dupProperty, entityProps );
                        continue;

                        
                    } catch (Exception e) {
                        logger.error("Error", e);
                    }
                } catch (InterruptedException e) {
                    logger.error( "Error", e );
                }

            }
        }

        
        private void handleDuplicateAccount(EntityManager em, String dupProperty, Map<String, Object> entityProps ) {

            logger.info( "Processing duplicate user {}:{}:{} with duplicate {}", new Object[]{
                    entityProps.get( "username" ), 
                    entityProps.get( "email" ), 
                    entityProps.get( "uuid" ), 
                    dupProperty} );
           
            UUID dupUuid = UUID.fromString( entityProps.get("uuid").toString() );
            try {
                dupsByDupUuid.put( dupUuid, new DuplicateUser( dupProperty, entityProps ) );
                
            } catch (Exception e) {
                logger.error("Error processing dup user {}:{}:{}",
                        new Object[] {entityProps.get( "username" ), entityProps.get("email"), dupUuid});
                return;
            }

        }
    }
}
