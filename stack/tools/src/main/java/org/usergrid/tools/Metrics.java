package org.usergrid.tools;

import com.google.common.collect.*;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.lang.time.DateUtils;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.usergrid.management.ApplicationInfo;
import org.usergrid.management.OrganizationInfo;
import org.usergrid.management.UserInfo;
import org.usergrid.persistence.*;
import org.usergrid.tools.bean.*;
import org.usergrid.utils.TimeUtils;

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

  private List<OrganizationInfo> organizations;
  private ListMultimap<UUID, ApplicationInfo> orgApps = ArrayListMultimap.create();
  private ListMultimap<Long, UUID> totalScore = ArrayListMultimap.create();
  private Map<UUID,MetricLine> collector = new HashMap<UUID, MetricLine>();
  private int reportThreshold = 100;
  private long startDate;
  private long endDate;

  @Override
  public void runTool(CommandLine line) throws Exception {
    startSpring();

    setVerbose(line);

    prepareBaseOutputFileName(line);

    parseDuration(line);

    applyOrgId(line);

    parseDateRange(line);

    outputDir = createOutputParentDir();

    logger.info("Export directory: {}",outputDir.getAbsolutePath());

    if ( orgId == null ) {
      organizations = managementService.getOrganizations(null, 20000);
      for (OrganizationInfo organization : organizations) {
        logger.info("Org Name: {} key: {}",organization.getName(), organization.getUuid());
        //List<UserInfo> adminUsers = managementService.getAdminUsersForOrganization(orgId);
        applicationsFor(organization.getUuid());

      }
    } else {
      OrganizationInfo orgInfo = managementService.getOrganizationByUuid(orgId);
      applicationsFor(orgInfo.getUuid());
      organizations = new ArrayList<OrganizationInfo>();
      organizations.add(orgInfo);
    }

    Iterable<OrganizationInfo> workingOrgs = applyThreshold();

    printReport(MetricSort.APP_REQ_COUNT, workingOrgs);
  }



  @Override
  public Options createOptions() {
    Options options = super.createOptions();
    Option duration = OptionBuilder.hasArg()
            .withDescription("A duration signifying the previous time until now. " +
                    "Supported forms: h,m,d eg. '30d' would be 30 days")
            .create("duration");
    Option startDate = OptionBuilder.hasArg().withDescription("The start date of the report")
            .create("startDate");
    Option endDate = OptionBuilder.hasArg().withDescription("The end date of the report")
                .create("endDate");

    options.addOption(duration).addOption(endDate).addOption(startDate);

    return options;
  }

  /**
   * 30 days in milliseconds by default
   * @param line
   * @return
   */
  private void parseDuration(CommandLine line) {
    String duration = line.getOptionValue("duration");
    if ( duration != null ) {
      startDate = TimeUtils.millisFromDuration(duration);
      endDate = System.currentTimeMillis();
    }
  }

  private void parseDateRange(CommandLine line) throws Exception {
    if ( line.hasOption("startDate")) {
      startDate = DateUtils.parseDate(line.getOptionValue("startDate"),new String[]{"yyyyMMdd-HHmm"}).getTime();
    }
    if ( line.hasOption("endDate")) {
      endDate = DateUtils.parseDate(line.getOptionValue("endDate"),new String[]{"yyyyMMdd-HHmm"}).getTime();
    }
  }

  private Iterable<OrganizationInfo> applyThreshold() throws Exception {
    Set<OrganizationInfo> orgs = new HashSet<OrganizationInfo>(reportThreshold);
    for ( Long l : Ordering.natural().greatestOf(totalScore.keys(), reportThreshold) ) {
      List<UUID> apps = totalScore.get(l);
      for ( UUID appId : apps ) {
        orgs.add(managementService.getOrganizationForApplication(appId));
      }
    }
    return orgs;
  }

  private void printReport(MetricSort metricSort, Iterable<OrganizationInfo> workingOrgs) throws Exception {
    JsonGenerator jg = getJsonGenerator(createOutputFile("metrics", metricSort.name().toLowerCase()));
    jg.writeStartObject();
    jg.writeStringField("report", metricSort.name());
    jg.writeStringField("date", new Date().toString());
    jg.writeArrayFieldStart("orgs");
    for ( OrganizationInfo org : workingOrgs ) {
      jg.writeStartObject();
      jg.writeStringField("org_id", org.getUuid().toString());
      jg.writeStringField("org_name",org.getName());
      jg.writeArrayFieldStart("admins");
      for (UserInfo userInfo : managementService.getAdminUsersForOrganization(org.getUuid()) ) {
        jg.writeString(userInfo.getEmail());
      }
      jg.writeEndArray();
      writeAppLines(jg, org.getUuid());
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

      collect(MetricQuery.getInstance(uuid, MetricSort.APP_REQ_COUNT)
              .resolution(CounterResolution.DAY)
              .startDate(startDate)
              .endDate(endDate)
              .execute(emf.getEntityManager(uuid)));

    }
  }

  private void collect(MetricLine metricLine) {
    for ( AggregateCounter a : metricLine.getAggregateCounters() ) {
      logger.info("col: {} val: {}",new Date(a.getTimestamp()), a.getValue());
    }
    totalScore.put(metricLine.getCount(), metricLine.getAppId());
    collector.put(metricLine.getAppId(), metricLine);

  }
  // line format: {reportQuery: application.requests, date: date, startDate : startDate, endDate: endDate, orgs : [
  // {orgId: guid, orgName: name, apps [{appId: guid, appName: name, dates: [{"[human date from ts]" : "[value]"},{...



}
