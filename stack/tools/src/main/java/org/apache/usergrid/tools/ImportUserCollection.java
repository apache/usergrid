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
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.EntityRef;
import org.apache.usergrid.persistence.exceptions.DuplicateUniquePropertyExistsException;
import org.apache.usergrid.persistence.index.query.Identifier;
import org.apache.usergrid.utils.ConversionUtils;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.usergrid.persistence.Schema.PROPERTY_TYPE;
import static org.apache.usergrid.persistence.Schema.PROPERTY_UUID;

/**
 * Import Users and metadata including organizations and passwords.
 *
 * <p>Usage Example:
 *
 * <p>java -Xmx8000m -Dlog4j.configuration=file:/home/me/log4j.properties -classpath . \ -jar
 * usergrid-tools-1.0.2.jar ImportUserCollection -writeThreads 100 -auditThreads 100 \ -host
 * casshost -inputDir=/home/me/import-data
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
public class ImportUserCollection extends ToolBase {

  private static final Logger logger = LoggerFactory.getLogger(ImportUserCollection.class);

  /** Input directory where the .json export files are */
  static final String INPUT_DIR = "inputDir";

  static final String WRITE_THREAD_COUNT = "writeThreads";
  static final String AUDIT_THREAD_COUNT = "auditThreads";
  protected static final String APP_ID = "appId";

  static File importDir;

  static final String DEFAULT_INPUT_DIR = "export";
  protected UUID applicationId;

  JsonFactory jsonFactory = new JsonFactory();

  AtomicInteger applicationCount = new AtomicInteger(0);
  AtomicInteger metadataCount = new AtomicInteger(0);

  AtomicInteger writeEmptyCount = new AtomicInteger(0);
  AtomicInteger auditEmptyCount = new AtomicInteger(0);
  AtomicInteger metadataEmptyCount = new AtomicInteger(0);

  static class DuplicateUser {
    String email;
    String username;

    public DuplicateUser(String propName, Map<String, Object> user) {
      if ("email".equals(propName)) {
        email = user.get("email").toString();
      } else {
        username = user.get("username").toString();
      }
    }
  }

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

  @Override
  public void runTool(CommandLine line) throws Exception {

    startSpring();

    setVerbose(line);

    applyExportParams(line);

    openImportDirectory(line);

    importUsersCol();
  }

  protected void applyExportParams(CommandLine line) {

    if (line.hasOption(APP_ID)) {
      this.applicationId = ConversionUtils.uuid(line.getOptionValue(APP_ID));
    }
  }

  /** Import users. */
  private void importUsersCol() throws Exception {

    String[] fileNames = importDir.list(new PrefixFileFilter("users" + "."));

    logger.info("Applications to read: " + fileNames.length);

    for (String fileName : fileNames) {
      try {
        importUsersCol(fileName);
      } catch (Exception e) {
        logger.warn("Unable to import application: " + fileName, e);
      }
    }
  }

  /**
   * Imports users.
   *
   * @param fileName Name of user data file.
   */
  @SuppressWarnings("unchecked")
  private void importUsersCol(final String fileName) throws Exception {

    int count = 0;

    File usersFile = new File(importDir, fileName);

    logger.info("----- Loading file: " + usersFile.getAbsolutePath());
    JsonParser jp = getJsonParserForFile(usersFile);

    int loopCounter = 0;

    JsonToken token = jp.nextToken();
    validateStartArray(token);

    List<Map<String, Object>> listEntityProps = new ArrayList<Map<String, Object>>();

    while (jp.nextValue() != JsonToken.END_ARRAY) {
      loopCounter += 1;
      Map<String, Object> entityProps = null;
      entityProps = jp.readValueAs(HashMap.class);
      if (loopCounter % 1000 == 0) {
        logger.debug("Publishing to queue... counter=" + loopCounter);
      }
      listEntityProps.add(entityProps);
    }

    UUID appUuid = UUID.fromString("28868961-940a-11e9-bc62-26c8b3a04fa8");

    EntityManager em = emf.getEntityManager(appUuid);

    for (Map<String, Object> entityProps : listEntityProps) {
      try {

        if (entityProps == null) {
          logger.warn("Reading from app import queue was null!");
        }
        writeEmptyCount.set(0);

        // Import/create the entity
        UUID uuid = getId(entityProps);
        String type = "user";

        long startTime = System.currentTimeMillis();

        em.create(uuid, type, entityProps);

        logger.debug(
            "Imported  user {}:{}:{}",
            new Object[] {entityProps.get("username"), entityProps.get("email"), uuid});

        applicationCount.getAndIncrement();
        long stopTime = System.currentTimeMillis();
        long duration = stopTime - startTime;

        count++;
        if (count % 30 == 0) {
          logger.info(
              "This worked has imported {} application of total {} imported so far. "
                  + "Average Creation Rate: {}ms with time duration {}",
              new Object[] {count, applicationCount.get(), count, duration});
        }

        importEntityMetadata(em, entityProps, uuid);

      } catch (Exception e) {
        logger.error("Error", e);
      }

      logger.info("----- End: Imported {}  users from file {}", count, usersFile.getAbsolutePath());
    }

    jp.close();
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

  /** Imports the entity's connecting references (collections and connections) */
  @SuppressWarnings("unchecked")
  private void importEntityMetadata(
      EntityManager em, Map<String, Object> entityProps, UUID entityUuid) throws Exception {

    // app does not exist yet, create it and add application

    try {

      // Import all dictionary values for app
      em = emf.getEntityManager(this.applicationId);

      Map<String, Object> dynamic_properties =
          (Map<String, Object>) entityProps.get("dynamic_properties");
      Map<String, Object> metadata = (Map<String, Object>) dynamic_properties.get("metadata");
      EntityRef entityRefApp = em.getUserByIdentifier(Identifier.fromUUID(entityUuid));
      if (metadata != null && !metadata.isEmpty()) {
        for (String name : metadata.keySet()) {
          try {

            Map<String, Object> dictionaryApp = (Map<String, Object>) metadata.get(name);
            em.addMapToDictionary(entityRefApp, name, dictionaryApp);

            logger.debug("Creating dictionary for {} name {}", new Object[] {entityRefApp, name});

          } catch (Exception e) {
            if (logger.isDebugEnabled()) {
              logger.error(
                  "Error importing dictionary name " + name + " for App " + entityRefApp.getUuid(),
                  e);
            } else {
              logger.error(
                  "Error importing dictionary name " + name + " for App " + entityRefApp.getUuid());
            }
          }
        }

      } else {
        logger.warn("App {} has no dictionaries", entityRefApp.getUuid());
      }
    } catch (DuplicateUniquePropertyExistsException dpee) {
      logger.debug("User {} already exists");
    }
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

  class ImportUserWorker implements Runnable, Stoppable {

    private BlockingQueue<Map<String, Object>> workQueue;
    private BlockingQueue<Map<String, Object>> auditQueue;
    private boolean done = false;

    public ImportUserWorker(
        final BlockingQueue<Map<String, Object>> workQueue,
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

      EntityManager em = emf.getEntityManager(CpNamingUtils.MANAGEMENT_APPLICATION_ID);

      long durationSum = 0;

      while (!done) {

        try {

          Map<String, Object> entityProps = this.workQueue.poll(30, TimeUnit.SECONDS);

          if (entityProps == null) {
            logger.warn("Reading from app import queue was null!");
            writeEmptyCount.getAndIncrement();
            Thread.sleep(1000);
            continue;
          }
          writeEmptyCount.set(0);

          // Import/create the entity
          UUID uuid = getId(entityProps);
          String type = getType(entityProps);

          try {
            long startTime = System.currentTimeMillis();

            em.create(uuid, type, entityProps);

            logger.debug(
                "Imported  user {}:{}:{}",
                new Object[] {entityProps.get("username"), entityProps.get("email"), uuid});

            applicationCount.getAndIncrement();
            auditQueue.put(entityProps);
            long stopTime = System.currentTimeMillis();
            long duration = stopTime - startTime;
            durationSum += duration;

            count++;
            if (count % 30 == 0) {
              logger.info(
                  "This worked has imported {} application of total {} imported so far. "
                      + "Average Creation Rate: {}ms",
                  new Object[] {count, applicationCount.get(), durationSum / count});
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
