package org.usergrid.system;

import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.exceptions.HectorException;
import org.codehaus.jackson.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.utils.JsonUtils;
import org.usergrid.utils.MapUtils;
import org.usergrid.utils.TimeUtils;

import java.util.Properties;

/**
 * Provide a single spot for monitoring usergrid system health
 *
 * @author zznate
 */
public class UsergridSystemMonitor {
  private static final String TIMER_THRESHOLD_TRIGGERED_MSG =
          "TimerThreshold triggered on duration: %d \n%s\n----------------";
  private static Logger logger = LoggerFactory.getLogger(UsergridSystemMonitor.class);

  private final String buildNumber;
  private final Cluster cluster;
  /** The trigger point for printing debugging information. {@see #maybeLogPayload}*/
  private long timerLogThreshold = 15*1000;
  public static final String LOG_THRESHOLD_PROPERTY = "metering.request.timer.log.threshold";

  /**
   * Must be instantiated with a build number and a cluster to be of any use. Properties can be null.
   * Threshold property must be a form compatible with {@link TimeUtils#millisFromDuration(String)}
   */
  public UsergridSystemMonitor(String buildNumber, Cluster cluster, Properties properties) {
    this.buildNumber = buildNumber;
    this.cluster = cluster;
    if ( properties != null ) {
      timerLogThreshold = TimeUtils.millisFromDuration(
              properties.getProperty(LOG_THRESHOLD_PROPERTY,"15s"));
    }
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

  /**
   * Uses {@link JsonUtils#mapToFormattedJsonString(Object)} against the object if the duration is
   * greater than {@link #timerLogThreshold}. When using the varargs form, the number of elements
   * must be even such that key,value,key,value mapping via {@link MapUtils#map(Object...)} can
   * collect all the elements.
   *
   * Conversion to a map this way let's us lazy create the map if and only if the triggering threshold is true
   * or we are in debug mode.
   *
   * @param duration
   * @param objects
   */
  public void maybeLogPayload(long duration, Object... objects) {
    if ( duration > timerLogThreshold || logger.isDebugEnabled() ) {
      String message;
      if ( objects.length > 1) {
        message = formatMessage(duration, MapUtils.map(objects));
      }else {
        message = formatMessage(duration, objects);
      }
      logger.info(message);
    }
  }

  static String formatMessage(long duration, Object object) {
    return String.format(TIMER_THRESHOLD_TRIGGERED_MSG,
                  duration,
                  JsonUtils.mapToFormattedJsonString(object));
  }


}
