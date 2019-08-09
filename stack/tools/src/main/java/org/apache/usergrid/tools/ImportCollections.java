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
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.exceptions.DuplicateUniquePropertyExistsException;
import org.apache.usergrid.utils.ConversionUtils;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.usergrid.persistence.Schema.PROPERTY_TYPE;
import static org.apache.usergrid.persistence.Schema.PROPERTY_UUID;

/**
 * Import collection Users and metadata including organizations and passwords.
 *
 * <p>Usage Example:
 *
 * <p>java -Xmx8000m -Dlog4j.configuration=file:/home/me/log4j.properties -classpath . \ -jar
 * usergrid-tools-1.0.2.jar Importcollections -writeThreads 100 -auditThreads 100 \ -host casshost
 * -inputDir=/home/me/import-data
 *
 * <p>If you want to provide any property overrides, put properties file named
 * usergrid-custom-tools.properties in the same directory where you run the above command. For
 * example, you might want to set the Cassandra client threads and import to a specific set of
 * keyspaces:
 *
 * <p>cassandra.connections=110 cassandra.system.keyspace=My_Other_Usergrid
 * cassandra.application.keyspace=My_Other_Usergrid_Applications
 * cassandra.lock.keyspace=My_Other_Usergrid_Locks
 */
public class ImportCollections extends ToolBase {

    private static final Logger logger = LoggerFactory.getLogger(ImportCollections.class);

    /** Input directory where the .json export files are */
    static final String INPUT_DIR = "inputDir";

    static final String WRITE_THREAD_COUNT = "writeThreads";
    static final String AUDIT_THREAD_COUNT = "auditThreads";
    protected static final String APP_ID = "appId";
    static File importDir;
    protected String orgName;
    protected UUID applicationId;
    static final String DEFAULT_INPUT_DIR = "export";

    private static Map<Stoppable, Thread> collectionWriteThreads =
        new ConcurrentHashMap<Stoppable, Thread>();
    private Map<Stoppable, Thread> collectionAuditThreads = new HashMap<Stoppable, Thread>();
    private Map<Stoppable, Thread> metadataWorkerThreadMap = new HashMap<Stoppable, Thread>();

    BlockingQueue<Map<String, Object>> workQueue = new LinkedBlockingQueue<Map<String, Object>>();

    JsonFactory jsonFactory = new JsonFactory();

    AtomicInteger collectionCount = new AtomicInteger(0);
    AtomicInteger metadataCount = new AtomicInteger(0);

    AtomicInteger writeEmptyCount = new AtomicInteger(0);
    AtomicInteger auditEmptyCount = new AtomicInteger(0);
    AtomicInteger metadataEmptyCount = new AtomicInteger(0);

    @Override
    @SuppressWarnings("static-access")
    public Options createOptions() {

        // inherit parent options
        Options options = super.createOptions();

        Option inputDir =
            OptionBuilder.hasArg().withDescription("input directory -inputDir").create(INPUT_DIR);

        Option appId =
            OptionBuilder.hasArg()
                .withDescription("Use a specific application -appId (Needs -orgId or -orgName)")
                .create(APP_ID);

        Option writeThreads =
            OptionBuilder.hasArg()
                .withDescription("Write Threads -writeThreads")
                .create(WRITE_THREAD_COUNT);

        Option auditThreads =
            OptionBuilder.hasArg()
                .withDescription("Audit Threads -auditThreads")
                .create(AUDIT_THREAD_COUNT);

        Option verbose =
            OptionBuilder.withDescription(
                "Print on the console an echo of the content written to the file")
                .create(VERBOSE);

        options.addOption(appId);

        options.addOption(writeThreads);
        options.addOption(auditThreads);
        options.addOption(inputDir);
        options.addOption(verbose);

        return options;
    }

    protected void applyExportParams(CommandLine line) {

        if (line.hasOption(APP_ID)) {
            this.applicationId = ConversionUtils.uuid(line.getOptionValue(APP_ID));
        }
    }

    @Override
    public void runTool(CommandLine line) throws Exception {

        startSpring();

        setVerbose(line);

        applyExportParams(line);

        openImportDirectory(line);

        int writeThreadCount = 1;

        if (line.hasOption(WRITE_THREAD_COUNT)) {
            writeThreadCount = Integer.parseInt(line.getOptionValue(WRITE_THREAD_COUNT));
        }

        importCollectionData(writeThreadCount);
    }

    /** Import collection users. */
    private void importCollectionData(int writeThreadCount) throws Exception {

        String[] fileNames = importDir.list(new PrefixFileFilter("collection" + "."));
        logger.info("Applications to read: " + fileNames.length);

        for (String fileName : fileNames) {
            try {
                importCollectionData(fileName, writeThreadCount);
            } catch (Exception e) {
                logger.warn("Unable to import application: " + fileName, e);
            }
        }
    }

    /**
     * Imports collection users.
     *
     * @param fileName Name of collection user data file.
     */
    private void importCollectionData(final String fileName, final int writeThreadCount)
        throws Exception {

        readCollectionFile(fileName);

        startcollectionWorkers(workQueue, writeThreadCount);
    }

    private void readCollectionFile(final String fileName)
        throws Exception, IOException, JsonParseException, InterruptedException {
        File collectionFile = new File(importDir, fileName);

        logger.info("----- Loading file: " + collectionFile.getAbsolutePath());
        JsonParser jp = getJsonParserForFile(collectionFile);

        int loopCounter = 0;

        JsonToken jsonToken = jp.nextToken();
        while (jsonToken != null) {
            if (jsonToken.equals(JsonToken.START_OBJECT)) {
                try {
                    loopCounter += 1;

                    @SuppressWarnings("unchecked")
                    Map<String, Object> entityProps = jp.readValueAs(HashMap.class);

                    Map<String, Object> dynamic_properties =
                        (Map<String, Object>) entityProps.get("dynamic_properties");
                    if (dynamic_properties != null) {
                        Map<String, Object> data = (Map<String, Object>) dynamic_properties.get("data");
                        entityProps.remove("dynamic_properties");
                        if (data != null) entityProps.put("data", data);
                    }
                    if (loopCounter % 1000 == 0) {
                        logger.debug("Publishing to queue... counter=" + loopCounter);
                    }

                    workQueue.add(entityProps);
                    jsonToken = jp.nextToken();
                } catch (Exception e) {
                    logger.debug("Error with user {}, not putting in metadata queue");
                }
            }
        }

        logger.info(
            "----- End: Imported {} collection users from file {}",
            loopCounter,
            collectionFile.getAbsolutePath());

        jp.close();
    }

    private static void waitForQueueAndMeasure(
        final BlockingQueue workQueue,
        final AtomicInteger emptyCounter,
        final Map<Stoppable, Thread> threadMap,
        final String identifier)
        throws InterruptedException {
        double rateAverageSum = 0;
        int iterations = 0;

        while (emptyCounter.get() < threadMap.size()) {
            iterations += 1;

            logger.info(
                "emptyCounter is {} , threadMap size={}, waiting...", emptyCounter, threadMap.size());

            int sizeLast = workQueue.size();
            long lastTime = System.currentTimeMillis();
            logger.info("Queue {} is not empty, remaining size={}, waiting...", identifier, sizeLast);
            Thread.sleep(1000);

            long timeNow = System.currentTimeMillis();
            int sizeNow = workQueue.size();

            int processed = sizeLast - sizeNow;

            long timeDelta = timeNow - lastTime;

            double rateLast = (double) processed / (timeDelta / 1000);
            rateAverageSum += rateLast;

            long timeRemaining = (long) (sizeLast / (rateAverageSum / iterations));

            logger.info(
                "++PROGRESS ({}): sizeLast={} nowSize={} processed={} rateLast={}/s rateAvg={}/s timeRemaining={}s",
                new Object[] {
                    identifier,
                    sizeLast,
                    sizeNow,
                    processed,
                    rateLast,
                    (rateAverageSum / iterations),
                    timeRemaining
                });
        }

        for (Stoppable worker : threadMap.keySet()) {
            worker.setDone(true);
            collectionWriteThreads.remove(worker);
        }
    }

    private void startcollectionWorkers(BlockingQueue<Map<String, Object>> workQueue, int workerCount)
        throws InterruptedException {

        for (int x = 0; x < workerCount; x++) {
            ImportcollectionWorker worker = new ImportcollectionWorker(workQueue);
            Thread workerThread = new Thread(worker, "collectionWriter-" + x);
            workerThread.start();
            collectionWriteThreads.put(worker, workerThread);
        }

        waitForQueueAndMeasure(workQueue, writeEmptyCount, collectionWriteThreads, "collection Write");

        logger.info("Started {} collection workers", workerCount);
    }

    private String getType(Map<String, Object> entityProps) {
        return (String) entityProps.get(PROPERTY_TYPE);
    }

    private UUID getId(Map<String, Object> entityProps) {
        return UUID.fromString((String) entityProps.get(PROPERTY_UUID));
    }

    private JsonParser getJsonParserForFile(File organizationFile) throws Exception {
        JsonParser jp = jsonFactory.createJsonParser(organizationFile);
        jp.setCodec(new ObjectMapper());
        return jp;
    }

    /** Open up the import directory based on <code>importDir</code> */
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

    class ImportcollectionWorker implements Runnable, Stoppable {

        private BlockingQueue<Map<String, Object>> workQueue1;
        private boolean done = false;

        public ImportcollectionWorker(final BlockingQueue<Map<String, Object>> workQueue) {
            this.workQueue1 = workQueue;
        }

        @Override
        public void setDone(boolean done) {
            this.done = done;
        }

        @Override
        public void run() {
            int count = 0;

            EntityManager em = emf.getEntityManager(applicationId);

            long durationSum = 0;

            while (!done) {

                try {

                    Map<String, Object> entityProps = this.workQueue1.poll(30, TimeUnit.SECONDS);

                    if (entityProps == null) {
                        logger.warn("Reading from collection import queue was null!");
                        writeEmptyCount.getAndIncrement();
                        Thread.sleep(10);
                        continue;
                    }
                    writeEmptyCount.set(0);

                    // Import/create the entity
                    UUID uuid = getId(entityProps);
                    String type = getType(entityProps);

                    try {
                        long startTime = System.currentTimeMillis();

                        if (em != null) {
                            em.create(uuid, type, entityProps);
                        } else {
                            continue;
                        }

                        logger.debug(
                            "Imported collection  {}:{}:{}",
                            new Object[] {entityProps.get("uuid"), entityProps.get("uuid"), uuid});

                        collectionCount.getAndIncrement();
                        long stopTime = System.currentTimeMillis();
                        long duration = stopTime - startTime;
                        durationSum += duration;

                        count++;
                        if (count % 30 == 0) {
                            logger.info(
                                "This worked has imported {} users of total {} imported so far. "
                                    + "Average Creation Rate: {}ms",
                                new Object[] {count, collectionCount.get(), durationSum / count});
                        }

                    } catch (DuplicateUniquePropertyExistsException de) {
                        continue;

                    } catch (Exception e) {
                        logger.error("Error", e);
                    }
                } catch (InterruptedException e) {
                    logger.error("Error", e);
                }
            }
        }
    }
}
