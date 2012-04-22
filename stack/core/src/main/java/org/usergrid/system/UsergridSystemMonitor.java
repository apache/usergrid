package org.usergrid.system;

import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.exceptions.HectorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provide a single spot for monitoring usergrid system health
 *
 * @author zznate
 */
public class UsergridSystemMonitor {
  private static Logger logger = LoggerFactory.getLogger(UsergridSystemMonitor.class);

  private final String buildNumber;
  private final Cluster cluster;

  /**
   * Must be instantiated with a build number and a cluster to be of any use
   *
   */
  public UsergridSystemMonitor(String buildNumber, Cluster cluster) {
    this.buildNumber = buildNumber;
    this.cluster = cluster;
  }

  /**
   * Wraps "describe_thrift_version API call as this hits a static string in Cassandra.
   * This is the most lightweight way to assure that Hector is alive and talking to the
   * cluster.
   * @return true if we have a lit connection to the cluster.
   */
  public boolean getIsCassandraAlive() {
    boolean isAlive = false;
    try {
      isAlive = cluster.describeThriftVersion() != null;
    } catch (HectorException he) {
      logger.error("Could not communicate with Cassandra cluster",he);
    }
    return isAlive;
  }

  /**
   *
   * @return a string representing the build number
   */
  public String getBuildNumber() {
    return buildNumber;
  }


}
