package org.usergrid.tools.bean;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.ObjectMapper;
import org.usergrid.persistence.AggregateCounter;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author zznate
 */
public class MetricLine {
  private final MetricSort metricSort;
  private final UUID appId;
  private final List<AggregateCounter> aggregateCounters;
  private long count = 0;

  /**
   * Package level access - intented to be used by {@link MetricQuery} only.
   * Sets the value of count by iterating over the {@link AggregateCounter}
   * collection.
   *
   * @param appId
   * @param metricSort
   * @param counters
   */
  MetricLine(UUID appId, MetricSort metricSort, List<AggregateCounter> counters) {
    Preconditions.checkArgument(appId != null, "appId was null");
    Preconditions.checkArgument(counters != null, "Counters list cannot be null");
    this.metricSort = metricSort;
    this.appId = appId;
    this.aggregateCounters = counters;
    if ( aggregateCounters.size() > 0 ) {
      for ( AggregateCounter ac : aggregateCounters) {
        count += ac.getValue();
      }
    }
  }


  @Override
  public String toString() {
    return Objects.toStringHelper(this)
            .add("appId", appId)
            .add("metricSort",metricSort)
            .toString();
  }

  /**
   * Compares metricSort and appId for equality
   * @param o
   * @return
   */
  @Override
  public boolean equals(Object o) {
    if ( o instanceof MetricLine ) {
      MetricLine oth = (MetricLine)o;
      return oth.getMetricSort().equals(metricSort) &&
              oth.getAppId().equals(appId);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(metricSort, appId);
  }

  public MetricSort getMetricSort() {
    return metricSort;
  }

  public long getCount() {
    return count;
  }

  public UUID getAppId() {
    return appId;
  }

  /**
   *
   * @return an Immutable list of our counters
   */
  public List<AggregateCounter> getAggregateCounters() {
    return ImmutableList.copyOf(aggregateCounters);
  }

}
