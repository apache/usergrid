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


import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.io.filefilter.PrefixFileFilter;
import org.apache.usergrid.management.OrganizationInfo;
import org.apache.usergrid.management.UserInfo;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.EntityRef;
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

import static org.apache.usergrid.persistence.Schema.PROPERTY_TYPE;
import static org.apache.usergrid.persistence.Schema.PROPERTY_UUID;
import static org.apache.usergrid.persistence.cassandra.CassandraService.MANAGEMENT_APPLICATION_ID;


/**
 * TODO: REFACTOR EVERYTHING TO USE JSON NODES
 * Example on how to run:
 * java -jar usergrid-tools.jar ImportAdmins -host cassandraHost -v -inputDir exportFilesDirectory
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


    JsonFactory jsonFactory = new JsonFactory();


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
        options.addOption(inputDir);
        options.addOption(verbose);

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
            writeThreadCount = Integer.parseInt(line.getOptionValue(WRITE_THREAD_COUNT));
        }

        importAdminUsers(writeThreadCount, auditThreadCount);

        importMetadata(writeThreadCount);

        // forces the counters to flush
//        logger.info( "Sleeping 35 seconds for batcher" );
//        Thread.sleep( 35000 );
    }


    /**
     * Import admin users.
     */
    private void importAdminUsers(int writeThreadCount, int auditThreadCount) throws Exception {

        String[] fileNames = importDir.list(new PrefixFileFilter(ExportAdmins.ADMIN_USERS_PREFIX + "."));

        logger.info("Applications to read: " + fileNames.length);

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
            if (loopCounter % 100 == 1)
                logger.info("Publishing to queue... counter=" + loopCounter);

            workQueue.add(entityProps);
        }

        waitForQueueAndMeasure(workQueue, adminWriteThreads, "Admin Write");
        waitForQueueAndMeasure(auditQueue, adminAuditThreads, "Admin Audit");

        logger.info("----- End: Imported {} admin users from file {}",
                count, adminUsersFile.getAbsolutePath());

        jp.close();
    }

    private static void waitForQueueAndMeasure(final BlockingQueue workQueue,
                                               final Map<Stoppable, Thread> threadMap,
                                               final String identifier) throws InterruptedException {
        double rateAverageSum = 0;
        int iterationCounter = 0;

        while (!workQueue.isEmpty()) {
            iterationCounter += 1;

            int sizeLast = workQueue.size();
            long lastTime = System.currentTimeMillis();
            logger.info("Queue {} is not empty, remaining size={}, waiting...", identifier, sizeLast);
            Thread.sleep(5000);

            long timeNow = System.currentTimeMillis();
            int sizeNow = workQueue.size();

            int processed = sizeLast - sizeNow;

            long timeDelta = timeNow - lastTime;

            double rateLast = (double) processed / (timeDelta / 1000);
            rateAverageSum += rateLast;

            long timeRemaining = (long) ( sizeLast / (rateAverageSum / iterationCounter) );

            logger.info(
                    String.format("++PROGRESS (%s): sizeLast=%s nowSize=%s processed=%s rateLast=%s/s rateAvg=%s/s timeRemaining=%s(s)",
                            identifier, sizeLast, sizeNow, processed, rateLast, (rateAverageSum / iterationCounter), timeRemaining)
            );
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
        JsonParser jp = jsonFactory.createJsonParser(organizationFile);
        jp.setCodec(new ObjectMapper());
        return jp;
    }


    /**
     * Import collections. Collections files are named: collections.<application_name>.Timestamp.json
     */
    private void importMetadata(int writeThreadCount) throws Exception {

        String[] fileNames = importDir.list(
                new PrefixFileFilter(ExportAdmins.ADMIN_USER_METADATA_PREFIX + "."));
        logger.info("Metadata files to read: " + fileNames.length);

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
            Thread workerThread = new Thread(worker, "ImportMetadataTask-" + x);
            workerThread.start();
            metadataWorkerThreadMap.put(worker, workerThread);
        }
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
                EntityRef entityRef = em.getRef(UUID.fromString(entityOwnerId));

                Map<String, Object> metadata = (Map<String, Object>) jp.readValueAs(Map.class);

                workQueue.put(new ImportMetadataTask(entityRef, metadata));
//                importEntityMetadata(em, entityRef, metadata);
            }
        }

        waitForQueueAndMeasure(workQueue, metadataWorkerThreadMap, "Metadata Load");

        logger.info("----- End of metadata -----");
        jp.close();
    }


    /**
     * Imports the entity's connecting references (collections and connections)
     */
    @SuppressWarnings("unchecked")
    private void importEntityMetadata(
            EntityManager em, EntityRef entityRef, Map<String, Object> metadata) throws Exception {

        Map<String, Object> connectionsMap = (Map<String, Object>) metadata.get("connections");

        if (connectionsMap != null && !connectionsMap.isEmpty()) {
            for (String type : connectionsMap.keySet()) {
                try {
                    UUID uuid = UUID.fromString((String) connectionsMap.get(type));
                    EntityRef connectedEntityRef = em.getRef(uuid);
                    em.createConnection(entityRef, type, connectedEntityRef);

                    logger.debug("Creating connection from {} type {} target {}",
                            new Object[]{entityRef, type, connectedEntityRef});

                } catch (Exception e) {
                    if (logger.isDebugEnabled()) {
                        logger.error("Error importing connection of type "
                                + type + " for user " + entityRef.getUuid(), e);
                    } else {
                        logger.error("Error importing connection of type "
                                + type + " for user " + entityRef.getUuid());
                    }
                }
            }
        }

        Map<String, Object> dictionariesMap = (Map<String, Object>) metadata.get("dictionaries");

        if (dictionariesMap != null && !dictionariesMap.isEmpty()) {
            for (String name : dictionariesMap.keySet()) {
                try {
                    Map<String, Object> dictionary = (Map<String, Object>) dictionariesMap.get(name);
                    em.addMapToDictionary(entityRef, name, dictionary);

                    logger.debug("Creating dictionary for {} name {} map {}",
                            new Object[]{entityRef, name, dictionary});

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
        }

        List<String> collectionsList = (List<String>) metadata.get("collections");
        if (collectionsList != null && !collectionsList.isEmpty()) {
            for (String name : collectionsList) {
                try {
                    UUID uuid = UUID.fromString((String) connectionsMap.get(name));
                    EntityRef collectedEntityRef = em.getRef(uuid);
                    em.addToCollection(entityRef, name, collectedEntityRef);

                    logger.debug("Add to collection of {} name {} entity {}",
                            new Object[]{entityRef, name, collectedEntityRef});

                } catch (Exception e) {
                    if (logger.isDebugEnabled()) {
                        logger.error("Error adding to collection "
                                + name + " for user " + entityRef.getUuid(), e);
                    } else {
                        logger.error("Error adding to collection "
                                + name + " for user " + entityRef.getUuid());
                    }
                }
            }
        }


        List<Object> organizationsList = (List<Object>) metadata.get("organizations");
        if (organizationsList != null && !organizationsList.isEmpty()) {

            User user = em.get(entityRef, User.class);

            if (user == null) {
                logger.error("User with uuid={} not found, not adding to organizations");

            } else {

                final UserInfo userInfo = managementService.getAdminUserByEmail(user.getEmail());

                for (Object orgObject : organizationsList) {

                    Map<String, Object> orgMap = (Map<String, Object>) orgObject;
                    UUID orgUuid = UUID.fromString((String) orgMap.get("uuid"));
                    String orgName = (String) orgMap.get("name");

                    // create org only if it does not exist
                    OrganizationInfo orgInfo = managementService.getOrganizationByUuid(orgUuid);
                    if (orgInfo == null) {
                        try {
                            managementService.createOrganization(orgUuid, orgName, userInfo, false);
                            orgInfo = managementService.getOrganizationByUuid(orgUuid);

                            logger.debug("Created new org {} for user {}",
                                    new Object[]{orgInfo.getName(), user.getEmail()});

                        } catch (DuplicateUniquePropertyExistsException dpee) {
                            logger.error("Org {} already exists", orgName);
                        }
                    } else {
                        managementService.addAdminUserToOrganization(userInfo, orgInfo, false);
                        logger.debug("Added user {} to org {}", new Object[]{user.getEmail(), orgName});
                    }
                }
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
                        Thread.sleep(1000);
                        continue;
                    }

                    count++;
                    long startTime = System.currentTimeMillis();

                    UUID uuid = (UUID) entityProps.get(PROPERTY_UUID);
                    String type = getType(entityProps);

                    if (em.get(uuid) == null) {
                        logger.error("Holy hell, we wrote an entity and it's missing.  " +
                                "Entity Id was {} and type is {}", uuid, type);
                        System.exit(1);
                    }

                    echo(entityProps);

                    long stopTime = System.currentTimeMillis();

                    long duration = stopTime - startTime;
                    durationSum += duration;
                    logger.debug(String.format("Audited [%s]th admin", count));
                    logger.info(String.format("Average Audit Rate: %s(ms)", durationSum / count));

                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            logger.warn("Done!");
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
                    ImportMetadataTask task = this.workQueue.poll(30, TimeUnit.SECONDS);

                    if (task == null) {
                        logger.warn("Reading from metadata queue was null!");
                        Thread.sleep(1000);
                        continue;
                    }

                    count++;
                    long startTime = System.currentTimeMillis();
                    importEntityMetadata(em, task.entityRef, task.metadata);
                    long stopTime = System.currentTimeMillis();

                    long duration = stopTime - startTime;
                    durationSum += duration;
                    logger.debug(String.format("Imported [%s]th metadata", count));
                    logger.info(String.format("Average metadata Imported Rate: %s(ms)", durationSum / count));

                } catch (Exception e) {
                    e.printStackTrace();
                    logger.debug("EXCEPTION", e);
                }
            }

            logger.warn("Done!");
        }
    }


    class ImportAdminWorker implements Runnable, Stoppable {

        private BlockingQueue<Map<String, Object>> workQueue;
        private BlockingQueue<Map<String, Object>> auditQueue;
        private boolean done = false;


        public ImportAdminWorker(final BlockingQueue<Map<String, Object>> workQueue,
                                 final BlockingQueue<Map<String, Object>> auditQueue) {
            logger.info("New Worker!");
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
                        Thread.sleep(1000);
                        continue;
                    }

                    // Import/create the entity
                    UUID uuid = getId(entityProps);
                    String type = getType(entityProps);


                    try {
                        long startTime = System.currentTimeMillis();
                        em.create(uuid, type, entityProps);
                        auditQueue.put(entityProps);
                        long stopTime = System.currentTimeMillis();

                        long duration = stopTime - startTime;
                        durationSum += duration;

                        count++;
                        logger.debug(String.format("Imported [%s]th admin user %s  / %s", count, uuid, entityProps.get("username")));
                        logger.info(String.format("Average Creation Rate: %s(ms)", durationSum / count));

                        if (count % 100 == 0) {
                            logger.info("Imported {} admin users", count);
                        }
                    } catch (DuplicateUniquePropertyExistsException de) {
                        logger.warn("Unable to create entity. It appears to be a duplicate: " +
                                        "id={}, type={}, name={}, username={}",
                                new Object[]{uuid, type, entityProps.get("name"), entityProps.get("username")});
                        if (logger.isDebugEnabled()) {
                            logger.debug("Exception", de);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }

            logger.warn("Done!");
        }
    }
}
