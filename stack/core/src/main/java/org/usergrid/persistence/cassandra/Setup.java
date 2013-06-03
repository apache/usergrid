/*******************************************************************************
 * Copyright 2012 Apigee Corporation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.usergrid.persistence.cassandra;

import static me.prettyprint.hector.api.factory.HFactory.createColumnFamilyDefinition;
import static org.usergrid.persistence.cassandra.CassandraPersistenceUtils.getCfDefs;
import static org.usergrid.persistence.cassandra.CassandraService.APPLICATIONS_CF;
import static org.usergrid.persistence.cassandra.CassandraService.DEFAULT_APPLICATION;
import static org.usergrid.persistence.cassandra.CassandraService.DEFAULT_APPLICATION_ID;
import static org.usergrid.persistence.cassandra.CassandraService.DEFAULT_ORGANIZATION;
import static org.usergrid.persistence.cassandra.CassandraService.MANAGEMENT_APPLICATION;
import static org.usergrid.persistence.cassandra.CassandraService.MANAGEMENT_APPLICATION_ID;
import static org.usergrid.persistence.cassandra.CassandraService.PRINCIPAL_TOKEN_CF;
import static org.usergrid.persistence.cassandra.CassandraService.PROPERTIES_CF;
import static org.usergrid.persistence.cassandra.CassandraService.STATIC_APPLICATION_KEYSPACE;
import static org.usergrid.persistence.cassandra.CassandraService.SYSTEM_KEYSPACE;
import static org.usergrid.persistence.cassandra.CassandraService.TOKENS_CF;
import static org.usergrid.persistence.cassandra.CassandraService.USE_VIRTUAL_KEYSPACES;
import static org.usergrid.persistence.cassandra.CassandraService.keyspaceForApplication;

import java.util.UUID;

import me.prettyprint.hector.api.ddl.ComparatorType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.mq.cassandra.QueuesCF;
import org.usergrid.persistence.entities.Application;

// TODO: Auto-generated Javadoc
/**
 * Cassandra-specific setup utilities.
 * 
 * @author edanuff
 */
public class Setup {

  private static final Logger logger = LoggerFactory.getLogger(Setup.class);

  private final org.usergrid.persistence.EntityManagerFactory emf;
  private final CassandraService cass;

  /**
   * Instantiates a new setup object.
   * 
   * @param emf
   *          the emf
   * @param cass
   */
  Setup(EntityManagerFactoryImpl emf, CassandraService cass) {
    this.emf = emf;
    this.cass = cass;
  }

  /**
   * Initialize.
   * 
   * @throws Exception
   *           the exception
   */
  public synchronized void setup() throws Exception {
    init();

    setupSystemKeyspace();

    setupStaticKeyspace();

    createDefaultApplications();
  }

  public void init() throws Exception {
    cass.init();
  }

  public void createDefaultApplications() throws Exception {
    // TODO unique check?
    ((EntityManagerFactoryImpl) emf).initializeApplication(DEFAULT_ORGANIZATION, DEFAULT_APPLICATION_ID,
        DEFAULT_APPLICATION, null);

    ((EntityManagerFactoryImpl) emf).initializeApplication(DEFAULT_ORGANIZATION, MANAGEMENT_APPLICATION_ID,
        MANAGEMENT_APPLICATION, null);
  }

  /**
   * Initialize system keyspace.
   * 
   * @throws Exception
   *           the exception
   */
  public void setupSystemKeyspace() throws Exception {

    logger.info("Initialize system keyspace");

    cass.createColumnFamily(SYSTEM_KEYSPACE,
        createColumnFamilyDefinition(SYSTEM_KEYSPACE, APPLICATIONS_CF, ComparatorType.BYTESTYPE));

    cass.createColumnFamily(SYSTEM_KEYSPACE,
        createColumnFamilyDefinition(SYSTEM_KEYSPACE, PROPERTIES_CF, ComparatorType.BYTESTYPE));

    cass.createColumnFamily(SYSTEM_KEYSPACE,
        createColumnFamilyDefinition(SYSTEM_KEYSPACE, TOKENS_CF, ComparatorType.BYTESTYPE));

    cass.createColumnFamily(SYSTEM_KEYSPACE,
        createColumnFamilyDefinition(SYSTEM_KEYSPACE, PRINCIPAL_TOKEN_CF, ComparatorType.UUIDTYPE));

    logger.info("System keyspace initialized");
  }

  /**
   * Initialize application keyspace.
   * 
   * @param applicationId
   *          the application id
   * @param applicationName
   *          the application name
   * @throws Exception
   *           the exception
   */
  public void setupApplicationKeyspace(final UUID applicationId, String applicationName) throws Exception {

    if (!USE_VIRTUAL_KEYSPACES) {
      String app_keyspace = keyspaceForApplication(applicationId);

      logger.info("Creating application keyspace " + app_keyspace + " for " + applicationName + " application");

      cass.createColumnFamily(app_keyspace,
          createColumnFamilyDefinition(SYSTEM_KEYSPACE, APPLICATIONS_CF, ComparatorType.BYTESTYPE));

      cass.createColumnFamilies(app_keyspace, getCfDefs(ApplicationCF.class, app_keyspace));
      cass.createColumnFamilies(app_keyspace, getCfDefs(QueuesCF.class, app_keyspace));
    }
  }

  public void setupStaticKeyspace() throws Exception {

    if (USE_VIRTUAL_KEYSPACES) {

      logger.info("Creating static application keyspace " + STATIC_APPLICATION_KEYSPACE);

      cass.createColumnFamily(STATIC_APPLICATION_KEYSPACE,
          createColumnFamilyDefinition(STATIC_APPLICATION_KEYSPACE, APPLICATIONS_CF, ComparatorType.BYTESTYPE));

      cass.createColumnFamilies(STATIC_APPLICATION_KEYSPACE,
          getCfDefs(ApplicationCF.class, STATIC_APPLICATION_KEYSPACE));
      cass.createColumnFamilies(STATIC_APPLICATION_KEYSPACE, getCfDefs(QueuesCF.class, STATIC_APPLICATION_KEYSPACE));

   
    }
  }

  public boolean keyspacesExist() {
    return cass.checkKeyspacesExist();
  }

  public static void logCFPermissions() {
    System.out.println(SYSTEM_KEYSPACE + "." + APPLICATIONS_CF + ".<rw>=usergrid");
    System.out.println(SYSTEM_KEYSPACE + "." + PROPERTIES_CF + ".<rw>=usergrid");
    for (CFEnum cf : ApplicationCF.values()) {
      System.out.println(STATIC_APPLICATION_KEYSPACE + "." + cf + ".<rw>=usergrid");
    }
    for (CFEnum cf : QueuesCF.values()) {
      System.out.println(STATIC_APPLICATION_KEYSPACE + "." + cf + ".<rw>=usergrid");
    }
  }

  /**
   * @return staticly constructed reference to the management application
   */
  public static Application getManagementApp() {
    return SystemDefaults.managementApp;
  }

  /**
   * @return statically constructed reference to the default application
   */
  public static Application getDefaultApp() {
    return SystemDefaults.defaultApp;
  }

  static class SystemDefaults {
    private static final Application managementApp = new Application(MANAGEMENT_APPLICATION_ID);
    private static final Application defaultApp = new Application(DEFAULT_APPLICATION_ID);

    static {
      managementApp.setName(MANAGEMENT_APPLICATION);
      defaultApp.setName(DEFAULT_APPLICATION);
    }

  }

}
