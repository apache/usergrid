package org.usergrid.tools.bean;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.io.StringWriter;

/**
 * @author zznate
 */
public class MetricLine {
  private final MetricSort metricSort;
  private final long count;
  private final String orgName;
  private final String appName;

  public MetricLine(MetricSort metricSort, long count, OrgScore orgScore, AppScore appScore) {
    this.metricSort = metricSort;
    this.count = count;
    this.orgName = orgScore.getName();
    this.appName = appScore != null ? appScore.getAppName() : "N/A";
  }

  public String printJson() {
    ObjectMapper om = new ObjectMapper();
    StringWriter sw = new StringWriter();
    try {
      om.writeValue(sw, this);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return sw.toString();
  }

  public String printCsvLine() {
    // TODO impl
    return "";
  }

  public String toString() {
    return printJson();
  }

  public MetricSort getMetricSort() {
    return metricSort;
  }

  public long getCount() {
    return count;
  }

  public String getOrgName() {
    return orgName;
  }

  public String getAppName() {
    return appName;
  }
}
