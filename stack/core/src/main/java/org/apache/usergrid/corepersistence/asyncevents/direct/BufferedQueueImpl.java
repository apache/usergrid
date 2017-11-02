/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.usergrid.corepersistence.asyncevents.direct;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Bufferes events and dispatched then in batches.
 * Ensures that the callback will be called at a min interval.
 */
public class BufferedQueueImpl<T> implements BufferedQueue<T> {

    private String fileName = "my_file_name.txt";
    private Consumer<List<T>> consumer;

    ScheduledThreadPoolExecutor threadPool = new ScheduledThreadPoolExecutor(1);

    private final LinkedBlockingQueue<PendingDispatch> queue;
    private final long intervalNanos;
    private long timeOfLastDispatch = 0L;

    public BufferedQueueImpl(int size, long interval , TimeUnit intervalTimeUnit) {

        Runtime.getRuntime().addShutdownHook(new Thread(new DispatchTask()));

        this.intervalNanos = intervalTimeUnit.toNanos(interval);
        threadPool.scheduleAtFixedRate(new DispatchTask(), intervalNanos,intervalNanos, TimeUnit.NANOSECONDS);
        readBatchFile();
        queue = new LinkedBlockingQueue<>(size);
    }

    public boolean offer(T t) {
        PendingDispatch pd = new PendingDispatch(t);
        if (timeOfLastDispatch + intervalNanos < System.nanoTime()) {
            dispatchOne(pd);
            return true;
        }
        try {
            return queue.offer(pd, intervalNanos, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            return false;
        }
    }

    public void setConsumer(Consumer<List<T>> consumer) {
        this.consumer = consumer;
    }


    private void dispatchOne(PendingDispatch pd) {
        List<PendingDispatch> messages = new ArrayList<>();
        messages.add(pd);
        dispatchMessages(messages);
    }

    protected void dispatchAll() {
        if (!queue.isEmpty()) {
            List<PendingDispatch> messages = new ArrayList<>();
            queue.drainTo(messages);
            dispatchMessages(messages);
        }
    }

    private void dispatchMessages(List<PendingDispatch> messages) {
        List<T> m = new ArrayList<>();
        for (PendingDispatch pd : messages) {
            if (!pd.isCancelled()) {
                m.add(pd.getWrapped());
            }
        }
        timeOfLastDispatch = System.nanoTime();
        Boolean sent = Boolean.TRUE;
        try {
            consumer.accept(m);
        } catch (Exception e) {
            sent = Boolean.FALSE;
        }
        for (PendingDispatch pd : messages) {
            pd.setResult(sent);
        }
    }


    public int size() {
        return queue.size();
    }

    private void readBatchFile() {

    }


    //
    // Internal Helper classes
    //



    private class PendingDispatch implements Future<Boolean> {
        T wrapped;
        boolean canceled;
        boolean done;
        Boolean result = null;

        PendingDispatch(T wrapped) {
            this.wrapped = wrapped;
            canceled = false;
            done = false;
        }

        T getWrapped() {
            return wrapped;
        }

        void setResult(Boolean b) {
            result = b;
            done = true;
            synchronized (this) {
                notify();
            }
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            canceled = true;
            return canceled;
        }

        @Override
        public boolean isCancelled() {
            return canceled;
        }

        @Override
        public boolean isDone() {
            return done;
        }

        @Override
        public Boolean get() throws InterruptedException, ExecutionException {
            while (!done) {
                synchronized (this) {
                    wait(100);
                }
            }
            return result;
        }

        @Override
        public Boolean get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            if (!done) {
                synchronized (this) {
                    wait(unit.toMillis(timeout));
                }
            }
            return result;
        }
    }


    private class DispatchTask implements Runnable  {
        @Override
        public void run() {
            try {
                dispatchAll();
            } catch (Throwable t) {
            }
        }
    }

}
