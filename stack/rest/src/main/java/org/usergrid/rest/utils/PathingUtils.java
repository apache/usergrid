package org.usergrid.rest.utils;

import javax.ws.rs.core.MultivaluedMap;

/**
 * Utilities and constants for path noodling
 *
 * @author zznate
 */
public class PathingUtils {

  public static final String PARAM_APP_NAME = "applicationName";
  public static final String PARAM_ORG_NAME = "organizationName";
  public static final String SLASH = "/";

  /**
   * Combine the two parameters to return a new path which represents the appName.
   * Previously, application names had to be unique accross the system. This is part of
   * the refactoring to treat the application name internally as a combination of
   * organization and application names.
   *
   * @param organizationName
   * @param applicationName
   * @return a new string in the format "organizationName/applicationName"
   */
  public static String assembleAppName(String organizationName, String applicationName) {
    return new String(organizationName.toLowerCase() + SLASH + applicationName.toLowerCase());
  }

  /**
   * Same as above except we pull the parameters from the pathParams
   * @param pathParams
   * @return
   */
  public static String assembleAppName(MultivaluedMap<String, String> pathParams) {
    return assembleAppName(pathParams.getFirst(PARAM_ORG_NAME),pathParams.getFirst(PARAM_APP_NAME));
  }

}
