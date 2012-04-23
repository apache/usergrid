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
import org.usergrid.persistence.Entity;
import org.usergrid.persistence.EntityManager;
import org.usergrid.tools.bean.AppScore;
import org.usergrid.tools.bean.MetricLine;
import org.usergrid.tools.bean.MetricSort;
import org.usergrid.tools.bean.OrgScore;

import java.io.File;
import java.io.IOException;
import java.util.*;

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

    ListMultimap<OrgScore,AppScore> appScores = ArrayListMultimap.create();

    for (Map.Entry<UUID, String> organization : organizations.entrySet()) {
      System.out.println("Org Name: " + organization.getValue());
      OrgScore orgScore = new OrgScore(organization.getKey(), organization.getValue());

      /*
      if (organization.equals(properties
              .getProperty("usergrid.test-account.organization"))) {
        // Skip test data from being exported.
        // orgScore.setIsTestAccount(true);
        continue;
      }
      */

      // for each organization
      // get # of admin users
      // get applications
      BiMap<UUID, String> applications = managementService
      				.getApplicationsForOrganization(organization.getKey());
      orgScore.setAppCount(applications.size());
      for (UUID uuid : applications.keySet() ) {
        AppScore appScore = new AppScore(orgScore, uuid, applications.get(uuid));
        EntityManager em = emf.getEntityManager(uuid);
        Map<String,Long> counters = em.getApplicationCounters();
        //application.collection.users
        appScore.setUserCount(counters.get("application.collection.users"));
        appScore.setRequestCount(counters.get("application.requests"));
        System.out.println(applications.get(uuid) + " has counters: " + em.getApplicationCounters());

        appScores.put(orgScore,appScore);

      }
      // get users count for application :em.getApplicationCounters(); getEntityCounters(uuid entityId)
      // - keep user count for organization
      // - keep user count for all apps
      // - keep req. aggregate for all apps

    }
    //System.out.println("AppScores multimap: " + appScores);
    JsonGenerator jg = getJsonGenerator(outputDir);
    // begin output of various sorts
    // TODO convert to ouput printing types
    jsonLineWriter(jg, MetricSort.APP_REQ_COUNT, appScores);
    //System.out.println("Apps by request: \n" + sortDelegator(appScores, MetricSort.APP_REQ_COUNT));

    jsonLineWriter(jg, MetricSort.APP_USER_COUNT, appScores);
    //System.out.println("Apps by user count: \n" + sortDelegator(appScores, MetricSort.APP_USER_COUNT));

    jsonLineWriter(jg, MetricSort.ORG_ADMIN_COUNT, appScores);
    //System.out.println("Orgs by Admin Count: \n" + sortDelegator(appScores, MetricSort.ORG_ADMIN_COUNT));

    jsonLineWriter(jg, MetricSort.ORG_USER_COUNT, appScores);
    //System.out.println("Orgs by Total User Count: \n" + sortDelegator(appScores, MetricSort.ORG_USER_COUNT));

    jsonLineWriter(jg, MetricSort.ORG_APP_COUNT, appScores);
    //System.out.println("Orgs by Application Count: \n" + sortDelegator(appScores, MetricSort.ORG_APP_COUNT));

    jsonLineWriter(jg, MetricSort.ORG_ADMIN_LOGIN_COUNT, appScores);
    //System.out.println("Orgs by Admin Login Count: \n" + sortDelegator(appScores, MetricSort.ORG_ADMIN_LOGIN_COUNT));
    jg.close();
  }

  private List<MetricLine> sortDelegator(ListMultimap<OrgScore,AppScore> scoreMaps, MetricSort sortType) {
    List<MetricLine> metrics = new ArrayList<MetricLine>(scoreMaps.size()*2);
    List<AppScore> appScores;
    List<OrgScore> orgScores;
    switch (sortType) {
      case APP_REQ_COUNT:
        appScores = new ArrayList<AppScore>(scoreMaps.values());
        Collections.sort(appScores, new Comparator<AppScore>() {
          public int compare(AppScore a1, AppScore a2) {
            return new Long(a1.getRequestCount()).compareTo(a2.getRequestCount());
          }
        });
        for (AppScore as : appScores) {
          metrics.add(new MetricLine(MetricSort.APP_REQ_COUNT, as.getRequestCount(), as.getOrgScore(), as));
        }
        break;
      case APP_USER_COUNT:
        appScores = new ArrayList<AppScore>(scoreMaps.values());
        Collections.sort(appScores, new Comparator<AppScore>() {
          public int compare(AppScore a1, AppScore a2) {
            return new Long(a1.getUserCount()).compareTo(a2.getUserCount());
          }
        });
        for (AppScore as : appScores) {
          metrics.add(new MetricLine(MetricSort.APP_USER_COUNT, as.getRequestCount(), as.getOrgScore(), as));
        }
        break;
      case ORG_ADMIN_COUNT:
        orgScores = new ArrayList<OrgScore>(scoreMaps.keys());
        Collections.sort(orgScores, new Comparator<OrgScore>() {
          public int compare(OrgScore a1, OrgScore a2) {
            return new Long(a1.getAdminCount()).compareTo(a2.getAdminCount());
          }
        });
        for (OrgScore orgScore : orgScores) {
          metrics.add(new MetricLine(MetricSort.ORG_ADMIN_COUNT, orgScore.getAdminCount(), orgScore, null));
        }
        break;
      case ORG_USER_COUNT:
        orgScores = new ArrayList<OrgScore>(scoreMaps.keys());
        Collections.sort(orgScores, new Comparator<OrgScore>() {
          public int compare(OrgScore a1, OrgScore a2) {
            return new Long(a1.getUserCount()).compareTo(a2.getUserCount());
          }
        });
        for (OrgScore orgScore : orgScores) {
          metrics.add(new MetricLine(MetricSort.ORG_USER_COUNT, orgScore.getUserCount(), orgScore, null));
        }
        break;
      case ORG_APP_COUNT:
        orgScores = new ArrayList<OrgScore>(scoreMaps.keys());
        Collections.sort(orgScores, new Comparator<OrgScore>() {
          public int compare(OrgScore a1, OrgScore a2) {
            return new Long(a1.getAppCount()).compareTo(a2.getAppCount());
          }
        });
        for (OrgScore orgScore : orgScores) {
          metrics.add(new MetricLine(MetricSort.ORG_APP_COUNT, orgScore.getAppCount(), orgScore, null));
        }
        break;
      case ORG_ADMIN_LOGIN_COUNT:
        orgScores = new ArrayList<OrgScore>(scoreMaps.keys());
        Collections.sort(orgScores, new Comparator<OrgScore>() {
          public int compare(OrgScore a1, OrgScore a2) {
            return new Long(a1.getAdminLogins()).compareTo(a2.getAdminLogins());
          }
        });
        for (OrgScore orgScore : orgScores) {
          metrics.add(new MetricLine(MetricSort.ORG_ADMIN_LOGIN_COUNT, orgScore.getAdminLogins(), orgScore, null));
        }
        break;
    }
    return metrics;
  }

  private void jsonLineWriter(JsonGenerator jg, MetricSort metricSort, ListMultimap<OrgScore,AppScore> scoreMaps) {
    try {
      jg.writeStartObject();
      jg.writeString(MetricSort.APP_REQ_COUNT.toString());
      jg.writeStartArray();
      for (MetricLine ml : sortDelegator(scoreMaps, MetricSort.APP_REQ_COUNT)) {
        jg.writeObject(ml);
      }
      jg.writeEndArray();
      jg.writeEndObject();
    } catch (IOException e) {
      e.printStackTrace();
    }

  }


}
