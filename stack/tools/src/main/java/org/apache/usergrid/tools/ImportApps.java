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
import org.apache.usergrid.management.ApplicationInfo;
import org.apache.usergrid.management.OrganizationInfo;
import org.apache.usergrid.management.UserInfo;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.EntityRef;
import org.apache.usergrid.persistence.SimpleEntityRef;
import org.apache.usergrid.persistence.entities.Application;
import org.apache.usergrid.persistence.entities.User;
import org.apache.usergrid.persistence.exceptions.DuplicateUniquePropertyExistsException;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.usergrid.persistence.Schema.PROPERTY_TYPE;
import static org.apache.usergrid.persistence.Schema.PROPERTY_UUID;
import static org.junit.Assert.assertNotNull;

import org.apache.usergrid.persistence.index.query.Identifier;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.utils.JsonUtils;

/**
 * Import Apps and metadata including organizations and passwords.
 *
 * <p>Usage Example:
 *
 * <p>java -Xmx8000m -Dlog4j.configuration=file:/home/me/log4j.properties -classpath . \ -jar
 * usergrid-tools-1.0.2.jar ImportApps -writeThreads 100 -auditThreads 100 \ -host casshost
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
public class ImportApps extends ToolBase {

  private static final Logger logger = LoggerFactory.getLogger(ImportApps.class);

  /** Input directory where the .json export files are */
  static final String INPUT_DIR = "inputDir";

  static final String WRITE_THREAD_COUNT = "writeThreads";
  static final String AUDIT_THREAD_COUNT = "auditThreads";

  static File importDir;

  static final String DEFAULT_INPUT_DIR = "export";

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

    importApps();
  }

  /** Import Apps. */
  private void importApps() throws Exception {

    String[] fileNames = importDir.list(new PrefixFileFilter("applications" + "."));

    logger.info("Applications to read: " + fileNames.length);

    for (String fileName : fileNames) {
      try {
        importApps(fileName);
      } catch (Exception e) {
        logger.warn("Unable to import application: " + fileName, e);
      }
    }
  }

  /**
   * Imports Apps .
   *
   * @param fileName Name of apps data file.
   */
  @SuppressWarnings("unchecked")
  private void importApps(final String fileName) throws Exception {

    int count = 0;

    File applicationFile = new File(importDir, fileName);

    logger.info("----- Loading file: " + applicationFile.getAbsolutePath());
    JsonParser jp = getJsonParserForFile(applicationFile);

    int loopCounter = 0;

    JsonToken token = jp.nextToken();
    validateStartArray(token);

    Map<String, Object> entityProps = null;
    while (jp.nextValue() != JsonToken.END_ARRAY) {
      loopCounter += 1;
      entityProps = jp.readValueAs(HashMap.class);
      if (loopCounter % 1000 == 0) {
        logger.debug("Publishing to queue... counter=" + loopCounter);
      }
    }

    EntityManager em = emf.getEntityManager(CpNamingUtils.MANAGEMENT_APPLICATION_ID);

    importEntityMetadata(em, entityProps);

    logger.info(
        "----- End: Imported {} Apps from file {}", count, applicationFile.getAbsolutePath());

    jp.close();
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
  private void importEntityMetadata(EntityManager em, Map<String, Object> appObj) throws Exception {

    // app does not exist yet, create it and add application

    UUID appUuid = UUID.fromString((String) appObj.get("uuid"));
    String appName = (String) appObj.get("applicationName");

    String orgName = (String) appObj.get("organizationName");
    OrganizationInfo orgInfo = managementService.getOrganizationByName(orgName);

    if (orgInfo != null) {
      UUID orgUuid = orgInfo.getUuid();

      try {

        ApplicationInfo appInfo =
            managementService.createApplication(orgUuid, appName, appUuid, null, false);

        // Import all dictionary values for app
        em = emf.getEntityManager(appUuid);

        Map<String, Object> metadata = (Map<String, Object>) appObj.get("metadata");
        Map<String, Object> dictionariesAppMap = (Map<String, Object>) metadata.get("dictionaries");
        EntityRef entityRefApp = em.getApplicationRef();
        if (dictionariesAppMap != null && !dictionariesAppMap.isEmpty()) {
          for (String name : dictionariesAppMap.keySet()) {
            try {
              Map<String, Object> dictionaryApp =
                  (Map<String, Object>) dictionariesAppMap.get(name);
              em.addMapToDictionary(entityRefApp, name, dictionaryApp);

              logger.debug("Creating dictionary for {} name {}", new Object[] {entityRefApp, name});

            } catch (Exception e) {
              if (logger.isDebugEnabled()) {
                logger.error(
                    "Error importing dictionary name "
                        + name
                        + " for App "
                        + entityRefApp.getUuid(),
                    e);
              } else {
                logger.error(
                    "Error importing dictionary name "
                        + name
                        + " for App "
                        + entityRefApp.getUuid());
              }
            }
          }

        } else {
          logger.warn("App {} has no dictionaries", entityRefApp.getUuid());
        }
      } catch (DuplicateUniquePropertyExistsException dpee) {
        logger.debug("App {} already exists", appName);
      }
    } else {
      logger.debug("Org {} Not Found ", orgName);
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
}
