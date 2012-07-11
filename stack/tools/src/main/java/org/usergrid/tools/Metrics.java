package org.usergrid.tools;

import com.google.common.collect.*;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.usergrid.management.OrganizationInfo;
import org.usergrid.management.UserInfo;
import org.usergrid.persistence.*;
import org.usergrid.tools.bean.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Tools class which dumps metrics for tracking Usergrid developer adoption
 * and high-level application usage.
 *
 * Can be called thusly:
 * mvn exec:java -Dexec.mainClass="org.usergrid.tools.Command" -Dexec.args="Metrics -host localhost -outputDir ./output"
 *
 * @author zznate
 */
public class Metrics extends ExportingToolBase {

  private BiMap<UUID, String> organizations;
  private ListMultimap<UUID, UUID> orgApps = ArrayListMultimap.create();
  private Map<UUID,MetricLine> collector = new HashMap<UUID, MetricLine>();

  @Override
  public void runTool(CommandLine line) throws Exception {
    startSpring();

    setVerbose(line);

    prepareBaseOutputFileName(line);

    applyOrgId(line);

    outputDir = createOutputParentDir();

    logger.info("Export directory: {}",outputDir.getAbsolutePath());

    if ( orgId == null ) {
      organizations = managementService.getOrganizations();
      for (Map.Entry<UUID, String> organization : organizations.entrySet()) {
        logger.info("Org Name: {} key: {}",organization.getValue(), organization.getKey());
        orgId = organization.getKey();
        //List<UserInfo> adminUsers = managementService.getAdminUsersForOrganization(orgId);
        applicationsFor(orgId);

      }
    } else {
      OrganizationInfo orgInfo = managementService.getOrganizationByUuid(orgId);
      applicationsFor(orgInfo.getUuid());
      organizations = HashBiMap.create(1);
      organizations.put(orgInfo.getUuid(), orgInfo.getName());
    }

  }

  private void applicationsFor(UUID orgId) throws Exception {
    BiMap<UUID, String> applications = managementService
            .getApplicationsForOrganization(orgId);

    for (UUID uuid : applications.keySet() ) {
      logger.info("Checking app: {}", applications.get(uuid));

      orgApps.put(orgId, uuid);

      collect(MetricQuery.getInstance(uuid,MetricSort.APP_REQ_COUNT)
              .resolution(CounterResolution.DAY)
              .execute(emf.getEntityManager(uuid)));

    }
  }

  private void collect(MetricLine metricLine) {
    for ( AggregateCounter a : metricLine.getAggregateCounters() ) {
      logger.info("col: {} val: {}",new Date(a.getTimestamp()), a.getValue());
    }
    collector.put(metricLine.getAppId(), metricLine);
    // guava Table?
    // store by app
    // store by metricType
  }

  //TODO
  // for each organization, for each app
  // get request counter for specified range & granularity
  // line format: {reportQuery: application.requests, date: date, startDate : startDate, endDate: endDate, orgs : [
  // {orgId: guid, orgName: name, apps [{appId: guid, appName: name, dates: [{"[human date from ts]" : "[value]"},{...

  // NEED:
  // - add query and sortType to MetricScore
  // - this.collect(appId,metricType,aggregateList,resolution)
  // - output formatter


}
