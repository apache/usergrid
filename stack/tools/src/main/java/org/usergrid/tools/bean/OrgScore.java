package org.usergrid.tools.bean;

import java.util.UUID;

/**
 * Models an organization metrics. The UUID for the id parameter
 * is considered unique in the system, so equals and hashCode are
 * delegated to such.
 *
 * @author zznate
 */
public class OrgScore {
  private final UUID id;
  private final String name;
  private long adminLogins;
  private long userCount;
  private long adminCount;
  private long appCount;

  public OrgScore(UUID id, String name) {
    this.id = id;
    this.name = name;
  }

  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public long getUserCount() {
    return userCount;
  }

  public void setUserCount(long userCount) {
    this.userCount = userCount;
  }

  public long getAdminCount() {
    return adminCount;
  }

  public void setAdminCount(long adminCount) {
    this.adminCount = adminCount;
  }

  public long getAdminLogins() {
    return adminLogins;
  }

  public void setAdminLogins(long adminLogins) {
    this.adminLogins = adminLogins;
  }

  public long getAppCount() {
    return appCount;
  }

  public void setAppCount(long appCount) {
    this.appCount = appCount;
  }

  /**
   * Delegates to id UUID
   * @return
   */
  @Override
  public int hashCode() {
    return id.hashCode();
  }

  /**
   * Delegates to the id UUID
   * @param o
   * @return
   */
  @Override
  public boolean equals(Object o) {
    if ( o instanceof OrgScore ) {
      return ((OrgScore)o).getId().equals(id);
    }
    return false;
  }

}
