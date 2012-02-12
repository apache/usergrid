package com.usergrid.count;

import com.usergrid.count.common.Count;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * A BatchSubmitter that prints contents to the configured slf4j logger logger
 * @author zznate
 */
public class Slf4JBatchSubmitter implements BatchSubmitter {

    private Logger log = LoggerFactory.getLogger(Slf4JBatchSubmitter.class);

    private int threadCount = 3;

    private ExecutorService executor = Executors.newFixedThreadPool(threadCount);

    @Override
    public Future submit(final SimpleBatcher.Batch batch) {
        return executor.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                for (Count c : batch.getCounts() ) {
                    log.info("Found count {}",c);
                }
                return true;
            }
        });

    }
}
