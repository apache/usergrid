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
package org.usergrid.tools;

import java.io.FileWriter;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.management.OrganizationInfo;
import org.usergrid.persistence.Entity;
import org.usergrid.persistence.EntityManager;
import org.usergrid.persistence.Query;
import org.usergrid.persistence.Results;
import org.usergrid.persistence.cassandra.CassandraService;
import org.usergrid.persistence.entities.Application;
import org.usergrid.utils.JsonUtils;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * This is a utility to load all entities in an application and re-save them,
 * this forces the secondary indexing to be updated.
 * 
 * @author tnine
 * 
 */
public class DupOrgRepair extends ExportingToolBase {

  /**
     * 
     */
  private static final int PAGE_SIZE = 100;

  private static final Logger logger = LoggerFactory.getLogger(DupOrgRepair.class);

  @Override
  @SuppressWarnings("static-access")
  public Options createOptions() {

    Option hostOption = OptionBuilder.withArgName("host").hasArg().isRequired(true).withDescription("Cassandra host")
        .create("host");

    Option outputOption = OptionBuilder.withArgName("output").hasArg().isRequired(true)
        .withDescription("Cassandra host").create("output");

    Options options = new Options();
    options.addOption(hostOption);
    options.addOption(outputOption);

    return options;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.usergrid.tools.ToolBase#runTool(org.apache.commons.cli.CommandLine)
   */
  @Override
  public void runTool(CommandLine line) throws Exception {
    String outputDir = line.getOptionValue("output");

    createDir(outputDir);

    startSpring();

    logger.info("Starting crawl of all admins");

    EntityManager em = emf.getEntityManager(CassandraService.MANAGEMENT_APPLICATION_ID);
    Application app = em.getApplication();

    // search for all orgs

    Query query = new Query();
    query.setLimit(PAGE_SIZE);
    Results r = null;

    Multimap<String, UUID> orgs = HashMultimap.create();

    do {

      r = em.searchCollection(app, "groups", query);

      for (Entity entity : r.getEntities()) {
        orgs.put(entity.getProperty("path").toString().toLowerCase(), entity.getUuid());
      }

      query.setCursor(r.getCursor());

      logger.info("Searching next page");

    } while (r != null && r.size() == PAGE_SIZE);

    // now go through and print out duplicate emails

    for (String name : orgs.keySet()) {
      Collection<UUID> ids = orgs.get(name);

      if (ids.size() > 1) {
        logger.warn("Found multiple orgs with the name {}", name);

        FileWriter file = new FileWriter(String.format("%s/%s", outputDir, name));

        for (UUID id : ids) {

          OrganizationInfo orgInfo = managementService.getOrganizationByUuid(id);

          Map<String, Object> orgData = managementService.getOrganizationData(orgInfo);

          file.write(JsonUtils.mapToFormattedJsonString(orgData));

          file.write("\n\n");
        }

        file.flush();
        file.close();
      }
    }

  }

  /**
   * Merge the source orgId into the targetId in the following way.
   * 
   * 1) link all admins from the source org to the target org
   * 2) link all apps from the source org to the target or
   * 3) delete the target org
   * 
   * @param sourceOrgId
   * @param targetOrgId
   */
  private void mergeOrganizations(UUID sourceOrgId, UUID targetOrgId) {

  }
}
