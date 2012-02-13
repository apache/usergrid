package com.usergrid.count;

import com.usergrid.count.common.Count;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.annotation.Timed;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.core.TimerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

/**
 * A BatchSubmitter that prints contents to the configured slf4j logger logger
 * @author zznate
 */
public class Slf4JBatchSubmitter implements BatchSubmitter {

    private Logger log = LoggerFactory.getLogger(Slf4JBatchSubmitter.class);

    private int threadCount = 3;

    private ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    private final Timer addTimer =
            Metrics.newTimer(Slf4JBatchSubmitter.class, "submit_invocation", TimeUnit.MICROSECONDS, TimeUnit.SECONDS);

    @Override
    public Future submit(final SimpleBatcher.Batch batch) {
        return executor.submit(new Callable<Object>() {
            final TimerContext timer = addTimer.time();
            @Override
            public Object call() throws Exception {
                for (Count c : batch.getCounts() ) {
                    log.info("found count {}",c);
                }
                timer.stop();
                return true;
            }
        });

    }

    public void shutdown() {
        log.warn("Shutdown Slf4jBatchSubmitter");
        executor.shutdown();
    }
}
