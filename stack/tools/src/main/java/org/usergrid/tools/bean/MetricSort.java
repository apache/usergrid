package org.usergrid.tools.bean;

/**
 * @author zznate
 */
public enum MetricSort {

  APP_REQ_COUNT("application.requests:*:*:*"),
  APP_USER_COUNT("application.users:*:*:*"),
  ORG_ADMIN_COUNT("admin.users:*:*:*"),
  ORG_USER_COUNT("organization.users:*:*:*"),
  ORG_APP_COUNT("applications:*:*:*"),
  ORG_ADMIN_LOGIN_COUNT("admin.logincount:*:*:*");

  private final String queryString;

  MetricSort(String queryString) {
    this.queryString = queryString;
  }

  public String queryFilter() {
    return queryString;
  }

}
