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

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Invokes {@link BatchSubmitter#submit(com.usergrid.count.AbstractBatcher.Batch)}
 * every batchInterval seconds.
 *
 * @author zznate
 */
public class ScheduledBatcher extends AbstractBatcher {

    private int batchInterval;
    private volatile long currentMillis;
    private AtomicLong batchSubmissionCount = new AtomicLong();

    /**
     * Initializes the scheduledExecutor with the interval (in seconds) at which this executor will fire
     * @param batchInterval
     */
    public ScheduledBatcher(int queueSize, int batchInterval) {
        super(queueSize);
        this.batchInterval = batchInterval;
        this.currentMillis = System.currentTimeMillis();
    }

    protected boolean shouldSubmit(Batch batch) {
      long now = System.currentTimeMillis();
      return (now > ((1000 * batchInterval) + currentMillis));
    }

    protected void submit(Batch batch) {
        currentMillis = System.currentTimeMillis();
        batchSubmitter.submit(batch);
        batchSubmissionCount.incrementAndGet();
    }

    public long getBatchSubmissionCount() {
        return batchSubmissionCount.get();
    }
}
