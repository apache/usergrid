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

  @Override
  public String toString() {
    return new StringBuilder()
            .append(metricSort.name())
            .append(" for org/app: ")
            .append(orgName)
            .append("/")
            .append(appName)
            .append(" is: ")
            .append(count).toString();
  }

  /**
   * Compares all field values for equality
   * @param o
   * @return
   */
  @Override
  public boolean equals(Object o) {
    if ( o instanceof MetricLine ) {
      MetricLine oth = (MetricLine)o;
      return oth.getMetricSort().equals(metricSort) &&
              oth.getOrgName().equals(orgName) &&
              oth.getAppName().equals(appName) &&
              oth.getCount() == count;

    }
    return false;
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
