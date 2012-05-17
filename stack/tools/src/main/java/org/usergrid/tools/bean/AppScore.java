package org.usergrid.tools.bean;

import java.util.UUID;

/**
 * Models the metrics associated with an application.
 * The UUID for the id parameter is considered unique
 * in the system, so equals and hashCode are delegated
 * to such.
 *
 * @author zznate
 */
public class AppScore {
  private final OrgScore orgScore;
  private final UUID appId;
  private final String appName;
  private long userCount;
  private long requestCount;

  public AppScore(OrgScore orgScore, UUID appId, String appName) {
    this.orgScore = orgScore;
    this.appId = appId;
    this.appName = appName;
  }

  public OrgScore getOrgScore() {
    return orgScore;
  }

  public UUID getAppId() {
    return appId;
  }

  public String getAppName() {
    return appName;
  }

  public long getUserCount() {
    return userCount;
  }

  public long getRequestCount() {
    return requestCount;
  }

  public void setUserCount(long userCount) {
    this.userCount = userCount;
  }

  public void setRequestCount(long requestCount) {
    this.requestCount = requestCount;
  }

  /**
   * Returns the hashCode of he appid parameter
   * @return
   */
  @Override
  public int hashCode() {
    return appId.hashCode();
  }

  /**
   * Checks the equality of the appId vs. o.getAppId()
   * @param o
   * @return true if the appId attributes are equal
   */
  @Override
  public boolean equals(Object o) {
    if ( o instanceof AppScore ) {
      return ((AppScore)o).getAppId().equals(appId);
    }
    return false;
  }

}
