/*******************************************************************************
 * Copyright 2012 Apigee Corporation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.usergrid.count;

import com.usergrid.count.common.Count;
import com.usergrid.count.AbstractBatcher.Batch;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.core.TimerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

/**
 * Submit {@link Batch} objects to the Kafka queue
 * @author zznate
 */
public class KafkaBatchSubmitter implements BatchSubmitter {

    private Logger log = LoggerFactory.getLogger(KafkaBatchSubmitter.class);

    // TODO pull up
    private int threadCount = 3;
    private ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    private final Timer addTimer =
            Metrics.newTimer(KafkaBatchSubmitter.class, "submit_invocation", TimeUnit.MICROSECONDS, TimeUnit.SECONDS);

    private KafkaCountProducer countProducer;

    public KafkaBatchSubmitter(KafkaCountProducer countProducer) {
        this.countProducer = countProducer;
    }

    @Override
    public Future<?> submit(final Batch batch) {
        return executor.submit(new Callable<Object>() {
            final TimerContext timer = addTimer.time();
            @Override
            public Object call() throws Exception {
                for (Count c : batch.getCounts() ) {
                    countProducer.send(c);
                }
                timer.stop();
                return true;
            }
        });
    }

    @Override
    public void shutdown() {
        log.warn("Shutdown KafkaBatchSubmitter");
        executor.shutdown();
    }
}
