package org.usergrid.tools.bean;

import com.google.common.base.Preconditions;
import org.usergrid.persistence.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * @author zznate
 */
public class MetricQuery {

  private final UUID appId;
  private final MetricSort metricSort;
  private CounterResolution counterResolution = CounterResolution.DAY;
  private long startDate = 0;
  private long endDate = 0;
  private boolean padding = false;


  private MetricQuery(UUID appId, MetricSort metricSort) {
    this.appId = appId;
    this.metricSort = metricSort;
  }

  public static MetricQuery getInstance(UUID appId, MetricSort metricSort) {
    return new MetricQuery(appId, metricSort);
  }

  public MetricQuery resolution(CounterResolution counterResolution) {
    this.counterResolution = counterResolution;
    return this;
  }

  public MetricQuery startDate(long startDate) {
    this.startDate = startDate;
    return this;
  }

  public MetricQuery endDate(long endDate) {
    this.endDate = endDate;
    return this;
  }

  public MetricQuery pad() {
    this.padding = true;
    return this;
  }

  /**
   *
   * @param entityManager
   * @return A List (potentially empty) of the AggregateCounter values found.
   * @throws Exception
   */
  public MetricLine execute(EntityManager entityManager) throws Exception {
    Query query = new Query();
    query.addCounterFilter(metricSort.queryFilter()); // TODO MetricSort.queryFilter
    if ( startDate > 0)
      query.setStartTime(startDate);
    if ( endDate > 0) {
      Preconditions.checkArgument(endDate > startDate,
              "The endDate (%s) must be greater than the startDate (%s)",
              endDate, startDate);
    } else {
      endDate = System.currentTimeMillis();
    }
    query.setFinishTime(endDate);
    query.setResolution(counterResolution);
    query.setPad(padding);
    Results r = entityManager.getAggregateCounters(query);

    List<AggregateCounterSet> qc = r.getCounters();
    List<AggregateCounter> counters = new ArrayList();
    if ( qc != null && qc.size() > 0 ) {
      if ( qc.get(0) != null && qc.get(0).getValues() != null ) {
        counters.addAll(qc.get(0).getValues());
      }
    }
    return new MetricLine(appId, metricSort, counters);
  }


}
