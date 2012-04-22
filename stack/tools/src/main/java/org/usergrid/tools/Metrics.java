package org.usergrid.tools;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.BiMap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.usergrid.persistence.Entity;
import org.usergrid.persistence.EntityManager;

import java.util.Map;
import java.util.UUID;

/**
 * @author zznate
 */
public class Metrics extends ExportingToolBase {

  @Override
  public void runTool(CommandLine line) throws Exception {
    startSpring();

    setVerbose(line);
    prepareBaseOutputFileName(line);

    outputDir = createOutputParentDir();

    logger.info("Export directory: " + outputDir.getAbsolutePath());

    BiMap<UUID, String> organizations = managementService
    				.getOrganizations();

    ListMultimap<Integer,Entity> usersForApp = ArrayListMultimap.create();
    ListMultimap<Integer,Entity> requestsForApp = ArrayListMultimap.create();

    for (Map.Entry<UUID, String> organization : organizations.entrySet()) {
      if (organization.equals(properties
              .getProperty("usergrid.test-account.organization"))) {
        // Skip test data from being exported.
        continue;
      }
      // for each organization
      // get # of admin users
      // get applications
      BiMap<UUID, String> applications = managementService
      				.getApplicationsForOrganization(organization.getKey());
      for (UUID uuid : applications.keySet() ) {
        EntityManager em = emf.getEntityManager(uuid);
        System.out.println(em.getApplicationCounters());
      }
      // get users count for application :em.getApplicationCounters(); getEntityCounters(uuid entityId)
      // - keep user count for organization
      // - keep user count for all apps
      // - keep req. aggregate for all apps

    }

  }




}
