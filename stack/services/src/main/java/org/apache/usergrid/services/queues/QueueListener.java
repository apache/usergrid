/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.usergrid.services.queues;

import com.codahale.metrics.*;
import com.codahale.metrics.Timer;
import com.google.inject.Injector;

import org.apache.usergrid.persistence.EntityManagerFactory;

import org.apache.usergrid.persistence.core.metrics.MetricsFactory;
import org.apache.usergrid.persistence.queue.*;
import org.apache.usergrid.persistence.queue.LegacyQueueManager;
import org.apache.usergrid.persistence.queue.impl.LegacyQueueScopeImpl;
import org.apache.usergrid.services.ServiceManager;
import org.apache.usergrid.services.ServiceManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Listens to the SQS queue and polls it for more queue messages. Then hands the queue messages off to the
 * QueueProcessorFactory
 */
public abstract class QueueListener  {
    public  final int MESSAGE_TRANSACTION_TIMEOUT =  25 * 1000;
    private final LegacyQueueManagerFactory queueManagerFactory;

    public  long DEFAULT_SLEEP = 5000;

    private static final Logger logger = LoggerFactory.getLogger(QueueListener.class);

    private MetricsFactory metricsService;

    private ServiceManagerFactory smf;

    private EntityManagerFactory emf;


    private Properties properties;


    private ServiceManager svcMgr;

    private long sleepWhenNoneFound = 0;

    private long sleepBetweenRuns = 0;

    private ExecutorService pool;
    private List<Future> futures;

    public  final int MAX_THREADS = 2;
    private Integer batchSize = 10;
    private String queueName;
    private int consecutiveCallsToRemoveDevices;
    private Meter meter;
    private Timer timer;

    /**
     * Initializes the QueueListener.
     * @param smf
     * @param emf
     * @param props
     */
    public QueueListener(ServiceManagerFactory smf, EntityManagerFactory emf, Injector injector, Properties props){
        //TODO: change current injectors to use service module instead of CpSetup
        this.queueManagerFactory = injector.getInstance( LegacyQueueManagerFactory.class );
        this.smf = smf;
        this.emf = injector.getInstance( EntityManagerFactory.class ); //emf;
        this.metricsService = injector.getInstance(MetricsFactory.class);
        this.properties = props;
        meter = metricsService.getMeter(QueueListener.class, "execute.commit");
        timer = metricsService.getTimer(QueueListener.class, "execute.dequeue");

    }


    /**
     * Start the queueListener. Initializes queue settings before starting the queue.
     */ //TODO: make use guice. Currently on spring.  Needs to run and finish for main thread.
    @PostConstruct
    public void start(){
        boolean shouldRun = new Boolean(properties.getProperty("usergrid.queues.listener.run", "true"));

        if(shouldRun) {
            if (logger.isTraceEnabled()) {
                logger.trace("QueueListener: starting.");
            }
            int threadCount = 0;

            try {
                sleepBetweenRuns = new Long(properties.getProperty("usergrid.queues.listener.sleep.between", ""+sleepBetweenRuns)).longValue();
                sleepWhenNoneFound = new Long(properties.getProperty("usergrid.queues.listener.sleep.after", ""+DEFAULT_SLEEP)).longValue();
                batchSize = new Integer(properties.getProperty("usergrid.queues.listener.MAX_TAKE", (""+batchSize)));
                consecutiveCallsToRemoveDevices = new Integer(properties.getProperty("usergrid.queues.inactive.interval", ""+200));
                queueName = getQueueName();

                int maxThreads = new Integer(properties.getProperty("usergrid.queues.listener.maxThreads", ""+MAX_THREADS));

                futures = new ArrayList<Future>(maxThreads);

                //create our thread pool based on our threadcount.

                pool = Executors.newFixedThreadPool(maxThreads);

                while (threadCount++ < maxThreads) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("QueueListener: Starting thread {}.", threadCount);
                    }
                    Runnable task = new Runnable() {
                        @Override
                        public void run() {
                            try {
                                execute();
                            } catch (Exception e) {
                                logger.warn("failed to start push", e);
                            }
                        }
                    };
                    futures.add( pool.submit(task));
                }
            } catch (Exception e) {
                logger.error("QueueListener: failed to start:", e);
            }
            if (logger.isTraceEnabled()) {
                logger.trace("QueueListener: done starting.");
            }
        }else{
            logger.info("QueueListener: never started due to config value usergrid.queues.listener.run.");
        }

    }


    /**
     * Queue Processor
     * Polls the queue for messages, then processes the messages that it gets.
     */
    private void execute(){
        if(Thread.currentThread().isDaemon()) {
            Thread.currentThread().setDaemon(true);
        }
        Thread.currentThread().setName("queues_Processor"+UUID.randomUUID());

        final AtomicInteger consecutiveExceptions = new AtomicInteger();
        if (logger.isTraceEnabled()) {
            logger.trace("QueueListener: Starting execute process.");
        }
        svcMgr = smf.getServiceManager(smf.getManagementAppId());
        if (logger.isTraceEnabled()) {
            logger.trace("getting from queue {} ", queueName);
        }
        LegacyQueueScope queueScope = new LegacyQueueScopeImpl( queueName, LegacyQueueScope.RegionImplementation.LOCAL);
        LegacyQueueManager legacyQueueManager = queueManagerFactory.getQueueManager(queueScope);
        // run until there are no more active jobs
        long runCount = 0;


        while ( true ) {


                Timer.Context timerContext = timer.time();
                //Get the messages out of the queue.
                //TODO: a model class to get generic queueMessages out of the queueManager. Ask Shawn what should go here.
                rx.Observable.from( legacyQueueManager.getMessages(getBatchSize(), ImportQueueMessage.class))
                    .buffer(getBatchSize())
                    .doOnNext(messages -> {
                        try {
                            if (logger.isTraceEnabled()) {
                                logger.trace("retrieved batch of {} messages from queue {} ", messages.size(), queueName);
                            }

                            if (messages.size() > 0) {

                                long now = System.currentTimeMillis();
                                //TODO: make sure this has a way to determine which QueueListener needs to be used
                                // ideally this is done by checking which type the messages have then
                                // asking for a onMessage call.
                                onMessage(messages);

                                legacyQueueManager.commitMessages(messages);

                                meter.mark(messages.size());
                                if (logger.isTraceEnabled()) {
                                    logger.trace("sent batch {} messages duration {} ms", messages.size(), System.currentTimeMillis() - now);
                                }

                                if (sleepBetweenRuns > 0) {
                                    if (logger.isTraceEnabled()) {
                                        logger.trace("sleep between rounds...sleep...{}", sleepBetweenRuns);
                                    }
                                    Thread.sleep(sleepBetweenRuns);
                                }

                            } else {
                                if (logger.isTraceEnabled()) {
                                    logger.trace("no messages...sleep...{}", sleepWhenNoneFound);
                                }
                                Thread.sleep(sleepWhenNoneFound);
                            }
                            timerContext.stop();
                            //send to the providers
                            consecutiveExceptions.set(0);
                        } catch (Exception ex) {
                            logger.error("failed to dequeue", ex);
                            try {
                                long sleeptime = sleepWhenNoneFound * consecutiveExceptions.incrementAndGet();
                                long maxSleep = 15000;
                                sleeptime = sleeptime > maxSleep ? maxSleep : sleeptime;
                                if (logger.isTraceEnabled()) {
                                    logger.trace("sleeping due to failures {} ms", sleeptime);
                                }
                                Thread.sleep(sleeptime);
                            } catch (InterruptedException ie) {
                                if (logger.isTraceEnabled()) {
                                    logger.trace("sleep interrupted");
                                }
                            }
                        }
                    }).toBlocking().lastOrDefault(null);
        }
    }


    public void stop(){
        if (logger.isTraceEnabled()) {
            logger.trace("stop processes");
        }

        if(futures == null){
            return;
        }
        for(Future future : futures){
            future.cancel(true);
        }

        pool.shutdownNow();
    }


    public void setBatchSize(int batchSize){
        this.batchSize = batchSize;
    }
    public int getBatchSize(){return batchSize;}


    /**
     * This will be the method that does the job dependant execution.
     * @param messages
     */
    public abstract void onMessage(List<LegacyQueueMessage> messages) throws Exception;

    public abstract String getQueueName();

}
