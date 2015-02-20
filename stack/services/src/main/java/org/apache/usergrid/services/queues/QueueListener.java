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
import com.google.common.cache.*;
import com.google.inject.Injector;

import org.apache.usergrid.corepersistence.CpSetup;
import org.apache.usergrid.metrics.MetricsFactory;

import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.EntityManagerFactory;

import org.apache.usergrid.persistence.queue.*;
import org.apache.usergrid.persistence.queue.QueueManager;
import org.apache.usergrid.services.ServiceManager;
import org.apache.usergrid.services.ServiceManagerFactory;
import org.apache.usergrid.services.notifications.impl.ApplicationQueueManagerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
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
    private final QueueManagerFactory queueManagerFactory;
    private final QueueScopeFactory queueScopeFactory;

    public  long DEFAULT_SLEEP = 5000;

    private static final Logger LOG = LoggerFactory.getLogger(QueueListener.class);

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
    public QueueManager TEST_QUEUE_MANAGER;
    private int consecutiveCallsToRemoveDevices;


    /**
     * Initializes the QueueListener.
     * @param smf
     * @param emf
     * @param metricsService
     * @param props
     */
    public QueueListener(ServiceManagerFactory smf, EntityManagerFactory emf, MetricsFactory metricsService, Injector injector, Properties props){
        //TODO: change current injectors to use service module instead of CpSetup
        this.queueManagerFactory = injector.getInstance( QueueManagerFactory.class );
        this.smf = smf;
        this.emf = injector.getInstance( EntityManagerFactory.class ); //emf;
        this.metricsService = metricsService;
        this.properties = props;
        this.queueScopeFactory = injector.getInstance(QueueScopeFactory.class);

    }


    /**
     * Start the queueListener. Initializes queue settings before starting the queue.
     */ //TODO: make use guice. Currently on spring.  Needs to run and finish for main thread.
    @PostConstruct
    public void start(){
        boolean shouldRun = new Boolean(properties.getProperty("usergrid.queues.listener.run", "true"));

        if(shouldRun) {
            LOG.info("QueueListener: starting.");
            int threadCount = 0;

            try {
                sleepBetweenRuns = new Long(properties.getProperty("usergrid.queues.listener.sleep.between", ""+sleepBetweenRuns)).longValue();
                sleepWhenNoneFound = new Long(properties.getProperty("usergrid.queues.listener.sleep.after", ""+DEFAULT_SLEEP)).longValue();
                batchSize = new Integer(properties.getProperty("usergrid.queues.listener.batchSize", (""+batchSize)));
                consecutiveCallsToRemoveDevices = new Integer(properties.getProperty("usergrid.queues.inactive.interval", ""+200));
                queueName = getQueueName();

                int maxThreads = new Integer(properties.getProperty("usergrid.queues.listener.maxThreads", ""+MAX_THREADS));

                futures = new ArrayList<Future>(maxThreads);

                //create our thread pool based on our threadcount.

                pool = Executors.newFixedThreadPool(maxThreads);

                while (threadCount++ < maxThreads) {
                    LOG.info("QueueListener: Starting thread {}.", threadCount);
                    Runnable task = new Runnable() {
                        @Override
                        public void run() {
                            try {
                                execute();
                            } catch (Exception e) {
                                LOG.error("failed to start push", e);
                            }
                        }
                    };
                    futures.add( pool.submit(task));
                }
            } catch (Exception e) {
                LOG.error("QueueListener: failed to start:", e);
            }
            LOG.info("QueueListener: done starting.");
        }else{
            LOG.info("QueueListener: never started due to config value usergrid.queues.listener.run.");
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
        LOG.info("QueueListener: Starting execute process.");
        Meter meter = metricsService.getMeter(QueueListener.class, "queue");
        com.codahale.metrics.Timer timer = metricsService.getTimer(QueueListener.class, "dequeue");
        svcMgr = smf.getServiceManager(smf.getManagementAppId());
        LOG.info("getting from queue {} ", queueName);
        QueueScope queueScope = queueScopeFactory.getScope(smf.getManagementAppId(), queueName);
        QueueManager queueManager = TEST_QUEUE_MANAGER != null ? TEST_QUEUE_MANAGER : queueManagerFactory.getQueueManager(queueScope);
        // run until there are no more active jobs
        long runCount = 0;


        while ( true ) {
            try {

                Timer.Context timerContext = timer.time();
                //Get the messages out of the queue.
                //TODO: a model class to get generic queueMessages out of the queueManager. Ask Shawn what should go here.
                List<QueueMessage> messages = queueManager.getMessages(getBatchSize(), MESSAGE_TRANSACTION_TIMEOUT, 5000, ImportQueueMessage.class);
                LOG.info("retrieved batch of {} messages from queue {} ", messages.size(),queueName);

                if (messages.size() > 0) {

                    long now = System.currentTimeMillis();
                    //TODO: make sure this has a way to determine which QueueListener needs to be used
                    // ideally this is done by checking which type the messages have then
                    // asking for a onMessage call.
                    onMessage( messages );

                    queueManager.commitMessages(messages);

                    meter.mark(messages.size());
                    LOG.info("sent batch {} messages duration {} ms", messages.size(),System.currentTimeMillis() - now);

                    if(sleepBetweenRuns > 0) {
                        LOG.info("sleep between rounds...sleep...{}", sleepBetweenRuns);
                        Thread.sleep(sleepBetweenRuns);
                    }

                }
                else{
                    LOG.info("no messages...sleep...{}", sleepWhenNoneFound);
                    Thread.sleep(sleepWhenNoneFound);
                }
                timerContext.stop();
                //send to the providers
                consecutiveExceptions.set(0);
            }catch (Exception ex){
                LOG.error("failed to dequeue",ex);
                try {
                    long sleeptime = sleepWhenNoneFound*consecutiveExceptions.incrementAndGet();
                    long maxSleep = 15000;
                    sleeptime = sleeptime > maxSleep ? maxSleep : sleeptime ;
                    LOG.info("sleeping due to failures {} ms", sleeptime);
                    Thread.sleep(sleeptime);
                }catch (InterruptedException ie){
                    LOG.info("sleep interrupted");
                }
            }
        }
    }


    public void stop(){
        LOG.info("stop processes");

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
    public abstract void onMessage(List<QueueMessage> messages) throws Exception;

    public abstract String getQueueName();

}
