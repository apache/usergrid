package org.usergrid.tools;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.BiMap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.usergrid.management.UserInfo;
import org.usergrid.persistence.*;
import org.usergrid.tools.bean.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Tools class which dumps metrics for tracking Usergrid developer adoption
 * and high-level application usage
 * @author zznate
 */
public class Metrics extends ExportingToolBase {



  @Override
  public void runTool(CommandLine line) throws Exception {
    startSpring();

    setVerbose(line);
    prepareBaseOutputFileName(line);

    outputDir = createOutputParentDir();

    logger.info("Export directory: {}",outputDir.getAbsolutePath());

    BiMap<UUID, String> organizations = managementService
    				.getOrganizations();


    UUID orgId;
    for (Map.Entry<UUID, String> organization : organizations.entrySet()) {
      logger.info("Org Name: {}",organization.getValue());

      orgId = organization.getKey();

      List<UserInfo> adminUsers = managementService.getAdminUsersForOrganization(orgId);

      BiMap<UUID, String> applications = managementService
      				.getApplicationsForOrganization(organization.getKey());

      for (UUID uuid : applications.keySet() ) {
        MetricQuery metricQuery = MetricQuery.getInstance(uuid,MetricSort.APP_REQ_COUNT);

        logger.info("Checking app: {}", applications.get(uuid));
        List<AggregateCounter> ac = metricQuery.resolution(CounterResolution.DAY).execute(emf.getEntityManager(uuid));
        // add(uuid, queryFilter, acs.getValues())
        for ( AggregateCounter a : ac ) {
          logger.info("col: {} val: {}",new Date(a.getTimestamp()), a.getValue());

        }

      }
      //TODO
      // for each organization, for each app
      // get request counter for specified range & granularity
      // line format: {reportQuery: application.requests, date: date, startDate : startDate, endDate: endDate, orgs : [
      // {orgId: guid, orgName: name, apps [{appId: guid, appName: name, dates: [{"[human date from ts]" : "[value]"},{...

      // NEED:
      //
      // - this.collect(appId,metricType,aggregateList,resolution)
    }

  }



}
