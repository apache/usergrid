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

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import me.prettyprint.cassandra.serializers.ByteBufferSerializer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.management.UserInfo;
import org.usergrid.persistence.Entity;
import org.usergrid.persistence.EntityManager;
import org.usergrid.persistence.EntityRef;
import org.usergrid.persistence.Query;
import org.usergrid.persistence.Results;
import org.usergrid.persistence.SimpleEntityRef;
import org.usergrid.persistence.cassandra.CassandraService;
import org.usergrid.persistence.entities.Application;
import org.usergrid.persistence.exceptions.DuplicateUniquePropertyExistsException;
import org.usergrid.utils.JsonUtils;
import org.usergrid.utils.UUIDUtils;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

/**
 * This is a utility to load all entities in an application and re-save them,
 * this forces the secondary indexing to be updated.
 * 
 * @author tnine
 * 
 */
public class DupAppUserCheck extends ExportingToolBase {

  /**
     * 
     */
  private static final int PAGE_SIZE = 100;

  private static final Logger logger = LoggerFactory.getLogger(DupAppUserCheck.class);

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

    String emailsDir = String.format("%s/emails", outputDir);
    String usernamesDir = String.format("%s/usernames", outputDir);
    createDir(emailsDir);
    createDir(usernamesDir);
    
    
    startSpring();

    logger.info("Starting crawl of all admins");

    

    EntityManager em = emf.getEntityManager(CassandraService.MANAGEMENT_APPLICATION_ID);
    Application app = em.getApplication();

    // search for all orgs

    Query query = new Query();
    query.setLimit(PAGE_SIZE);
    Results r = null;

    Multimap<String, UUID> emails = HashMultimap.create();
    Multimap<String, UUID> usernames = HashMultimap.create();

    do {

      r = em.searchCollection(app, "users", query);

      for (Entity entity : r.getEntities()) {
        emails.put(entity.getProperty("email").toString().toLowerCase(), entity.getUuid());
        usernames.put(entity.getProperty("username").toString().toLowerCase(), entity.getUuid());
      }

      query.setCursor(r.getCursor());
      
      logger.info("Searching next page");

    } while (r != null && r.size() == PAGE_SIZE);

    // now go through and print out duplicate emails

    for (String email : emails.keySet()) {
      Collection<UUID> ids = emails.get(email);

      if (ids.size() > 1) {
        logger.warn("Found multiple admins with the email {}", email);

        FileWriter file = new FileWriter(String.format("%s/%s", emailsDir, email));

        for (UUID id : ids) {

         Map<String, Object> userOrganizationData = managementService
              .getAdminUserOrganizationData(id);
              
          file.write(JsonUtils.mapToFormattedJsonString(userOrganizationData));
          
          file.write("\n\n");
        }

        file.flush();
        file.close();
      }
    }

    for (String username : usernames.keySet()) {
      Collection<UUID> ids = emails.get(username);

      if (ids.size() > 1) {
        logger.warn("Found multiple users with the username {}", username);

        FileWriter file = new FileWriter(String.format("%s/%s", usernamesDir, username));

        for (UUID id : ids) {
          Map<String, Object> userOrganizationData = managementService
              .getAdminUserOrganizationData(id);
          
          file.write(JsonUtils.mapToFormattedJsonString(userOrganizationData));
          file.write("\n\n");
        }

        file.flush();
        file.close();
      }
    }

  }
}
