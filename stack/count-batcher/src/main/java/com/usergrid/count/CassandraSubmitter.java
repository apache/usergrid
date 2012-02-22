package com.usergrid.count;

import com.usergrid.count.common.Count;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.core.TimerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

/**
 * Submits directly to Cassandra for insertion
 * @author zznate
 */
public class CassandraSubmitter implements BatchSubmitter {
  private Logger log = LoggerFactory.getLogger(CassandraSubmitter.class);

  private int threadCount = 3;
  private CassandraCounterStore cassandraCounterStore;

  private ExecutorService executor = Executors.newFixedThreadPool(threadCount);
  private final Timer addTimer =
          Metrics.newTimer(CassandraSubmitter.class, "submit_invocation", TimeUnit.MICROSECONDS, TimeUnit.SECONDS);

  public CassandraSubmitter(CassandraCounterStore cassandraCounterStore) {
      this.cassandraCounterStore = cassandraCounterStore;
  }

  @Override
  public Future submit(final AbstractBatcher.Batch batch) {
      // TODO reconcile this dupped code with the other submitters
      return executor.submit(new Callable<Object>() {
          final TimerContext timer = addTimer.time();
          @Override
          public Object call() throws Exception {
              cassandraCounterStore.save(batch.getCounts());
              timer.stop();
              return true;
          }
      });

  }

  public void shutdown() {
      log.warn("Shutting down CassandraSubmitter");
      executor.shutdown();
  }
}
