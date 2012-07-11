package org.usergrid.tools;

import com.google.common.collect.*;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.usergrid.management.ApplicationInfo;
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
  private ListMultimap<UUID, ApplicationInfo> orgApps = ArrayListMultimap.create();
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

    printReport(MetricSort.APP_REQ_COUNT);
  }

  private void printReport(MetricSort metricSort) throws Exception {
    JsonGenerator jg = getJsonGenerator(createOutputFile("metrics", metricSort.name().toLowerCase()));
    jg.writeStartObject();
    jg.writeStringField("report", metricSort.name());
    jg.writeStringField("date", new Date().toString());
    jg.writeArrayFieldStart("orgs");
    for ( UUID orgId : organizations.keySet() ) {
      jg.writeStartObject();
      jg.writeStringField("org_id", orgId.toString());
      jg.writeStringField("org_name",organizations.get(orgId));
      writeAppLines(jg, orgId);
      jg.writeEndObject();
    }
    jg.writeEndArray();
    jg.writeEndObject();
    jg.close();
  }

  private void writeAppLines(JsonGenerator jg, UUID orgId) throws Exception {
    jg.writeArrayFieldStart("apps");
    for (ApplicationInfo appInfo : orgApps.get(orgId) ) {

      jg.writeStartObject();
      jg.writeStringField("app_id", appInfo.getId().toString());
      jg.writeStringField("app_name",appInfo.getName());
      jg.writeArrayFieldStart("counts");
      MetricLine line = collector.get(appInfo.getId());
      if ( line != null ) {
        jg.writeStartObject();
        for ( AggregateCounter ag : line.getAggregateCounters() ) {
          jg.writeStringField(new Date(ag.getTimestamp()).toString(),Long.toString(ag.getValue()));
        }
        jg.writeEndObject();
      }
      jg.writeEndArray();
      jg.writeEndObject();

    }
    jg.writeEndArray();
  }

  private void applicationsFor(UUID orgId) throws Exception {
    BiMap<UUID, String> applications = managementService
            .getApplicationsForOrganization(orgId);

    for (UUID uuid : applications.keySet() ) {
      logger.info("Checking app: {}", applications.get(uuid));

      orgApps.put(orgId, new ApplicationInfo(uuid, applications.get(uuid)));

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

  }
  // line format: {reportQuery: application.requests, date: date, startDate : startDate, endDate: endDate, orgs : [
  // {orgId: guid, orgName: name, apps [{appId: guid, appName: name, dates: [{"[human date from ts]" : "[value]"},{...



}
