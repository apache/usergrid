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
package org.apache.usergrid.services.notifications;

import com.codahale.metrics.*;
import com.codahale.metrics.Timer;
import com.google.inject.Injector;

import org.apache.usergrid.persistence.EntityManagerFactory;

import org.apache.usergrid.persistence.core.metrics.MetricsFactory;
import org.apache.usergrid.persistence.queue.*;
import org.apache.usergrid.persistence.queue.LegacyQueueManager;
import org.apache.usergrid.persistence.queue.impl.LegacyQueueScopeImpl;
import org.apache.usergrid.services.ServiceManagerFactory;
import org.apache.usergrid.services.notifications.impl.ApplicationQueueManagerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;


/**
 * Singleton listens for notifications queue messages
 */
public class QueueListener  {

    private final LegacyQueueManagerFactory queueManagerFactory;

    public static long DEFAULT_SLEEP = 100;

    private static final Logger logger = LoggerFactory.getLogger(QueueListener.class);

    private MetricsFactory metricsService;

    private ServiceManagerFactory smf;

    private EntityManagerFactory emf;
    private ApplicationQueueManagerCache applicationQueueManagerCache;


    private Properties properties;

    private long sleepWhenNoneFound = 0;

    private long sleepBetweenRuns = 0;

    private ExecutorService pool;
    private List<Future> futures;

    private static final int PUSH_CONSUMER_MAX_THREADS = 8;
    public static final int MAX_TAKE = 10;
    private String queueName;
    private int consecutiveCallsToRemoveDevices;

    public QueueListener(ServiceManagerFactory smf, EntityManagerFactory emf, Properties props){
        this.queueManagerFactory = smf.getApplicationContext().getBean( Injector.class ).getInstance(LegacyQueueManagerFactory.class);
        this.smf = smf;
        this.emf = emf;
        this.metricsService = smf.getApplicationContext().getBean( Injector.class ).getInstance(MetricsFactory.class);
        this.properties = props;
        this.applicationQueueManagerCache = smf.getApplicationContext().getBean(Injector.class).getInstance(ApplicationQueueManagerCache.class);

    }

    /**
     * Start the service and begin consuming messages
     */
    public void start(){
        //TODO refactor this into a central component that will start/stop services

            if (logger.isDebugEnabled()) {
                logger.debug("QueueListener: starting.");
            }
            int threadCount = 0;

            try {

                sleepBetweenRuns = new Long(properties.getProperty("usergrid.push.sleep", "" + DEFAULT_SLEEP));
                sleepWhenNoneFound = new Long(properties.getProperty("usergrid.push.sleep", "" + DEFAULT_SLEEP));
                consecutiveCallsToRemoveDevices = new Integer(properties.getProperty("usergrid.notifications.inactive.interval", ""+200));
                queueName = ApplicationQueueManagerImpl.getQueueNames(properties);

                int maxThreads = new Integer(properties.getProperty("usergrid.push.worker_count", ""+PUSH_CONSUMER_MAX_THREADS));

                futures = new ArrayList<>(maxThreads);

                //create our thread pool based on our threadcount.

                pool = Executors.newFixedThreadPool(maxThreads);

                while (threadCount++ < maxThreads) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("QueueListener: Starting thread {}.", threadCount);
                    }
                    final int threadNumber = threadCount;
                    Runnable task = new Runnable() {
                        @Override
                        public void run() {
                            try {
                                execute(threadNumber);
                            } catch (Exception e) {

                                if(pool.isShutdown()){
                                    logger.warn("QueueListener: push listener pool already shut down.");
                                }else{
                                    logger.error("QueueListener: threads interrupted", e);
                                }
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
    }

    private void execute(int threadNumber){

        if(Thread.currentThread().isDaemon()) {
            Thread.currentThread().setDaemon(true);
        }

        Thread.currentThread().setName(getClass().getSimpleName()+"_PushNotifications-"+threadNumber);

        final AtomicInteger consecutiveExceptions = new AtomicInteger();

        if (logger.isTraceEnabled()) {
            logger.trace("QueueListener: Starting execute process.");
        }

        Meter meter = metricsService.getMeter(QueueListener.class, "execute.commit");
        com.codahale.metrics.Timer timer = metricsService.getTimer(QueueListener.class, "execute.dequeue");

        if (logger.isTraceEnabled()) {
            logger.trace("getting from queue {} ", queueName);
        }

        LegacyQueueScope queueScope = new LegacyQueueScopeImpl( queueName, LegacyQueueScope.RegionImplementation.LOCAL);
        LegacyQueueManager legacyQueueManager = queueManagerFactory.getQueueManager(queueScope);

        // run until there are no more active jobs
        final AtomicLong runCount = new AtomicLong(0);

        while ( true ) {

                Timer.Context timerContext = timer.time();
                rx.Observable.from( legacyQueueManager.getMessages(MAX_TAKE, ApplicationQueueMessage.class))
                    .buffer(MAX_TAKE)
                    .doOnNext(messages -> {

                        try {
                            if (logger.isTraceEnabled()) {
                                logger.trace("retrieved batch of {} messages from queue {}", messages.size(), queueName);
                            }

                            if (messages.size() > 0) {
                                HashMap<UUID, List<LegacyQueueMessage>> messageMap = new HashMap<>(messages.size());

                                //group messages into hash map by app id
                                for (LegacyQueueMessage message : messages) {
                                    //TODO: stop copying around this area as it gets notification specific.
                                    ApplicationQueueMessage queueMessage = (ApplicationQueueMessage) message.getBody();
                                    UUID applicationId = queueMessage.getApplicationId();

                                    //Groups queue messages by application Id, ( they are all probably going to the same place )
                                    if (!messageMap.containsKey(applicationId)) {
                                        //For each app id it sends the set.
                                        List<LegacyQueueMessage> applicationQueueMessages = new ArrayList<LegacyQueueMessage>();
                                        applicationQueueMessages.add(message);
                                        messageMap.put(applicationId, applicationQueueMessages);
                                    } else {
                                        messageMap.get(applicationId).add(message);
                                    }
                                }

                                long now = System.currentTimeMillis();
                                Observable merge = null;

                                //send each set of app ids together
                                for (Map.Entry<UUID, List<LegacyQueueMessage>> entry : messageMap.entrySet()) {
                                    UUID applicationId = entry.getKey();

                                    ApplicationQueueManager manager = applicationQueueManagerCache
                                        .getApplicationQueueManager(
                                            emf.getEntityManager(applicationId),
                                            legacyQueueManager,
                                            new JobScheduler(smf.getServiceManager(applicationId), emf.getEntityManager(applicationId)),
                                            metricsService,
                                            properties
                                        );

                                    if (logger.isTraceEnabled()) {
                                        logger.trace("send batch for app {} of {} messages", entry.getKey(), entry.getValue().size());
                                    }
                                    Observable current = manager.sendBatchToProviders(entry.getValue(),queueName);

                                    if(merge == null)
                                        merge = current;
                                    else {
                                        merge = Observable.merge(merge,current);
                                    }
                                }

                                if(merge!=null) {
                                    merge.toBlocking().lastOrDefault(null);
                                }
                                legacyQueueManager.commitMessages(messages);

                                meter.mark(messages.size());
                                if (logger.isTraceEnabled()) {
                                    logger.trace("sent batch {} messages duration {} ms", messages.size(), System.currentTimeMillis() - now);
                                }

                                if(sleepBetweenRuns > 0) {
                                    if (logger.isTraceEnabled()) {
                                        logger.trace("sleep between rounds...sleep...{}", sleepBetweenRuns);
                                    }
                                    Thread.sleep(sleepBetweenRuns);
                                }

                                if(runCount.incrementAndGet() % consecutiveCallsToRemoveDevices == 0){
                                    for(ApplicationQueueManager applicationQueueManager : applicationQueueManagerCache.asMap().values()){
                                        try {
                                            applicationQueueManager.asyncCheckForInactiveDevices();
                                        }catch (Exception inactiveDeviceException){
                                            logger.error("Inactive Device Get failed",inactiveDeviceException);
                                        }
                                    }
                                    //clear everything
                                    runCount.set(0);
                                }
                            }

                            else{
                                if (logger.isTraceEnabled()) {
                                    logger.trace("no messages...sleep...{}", sleepWhenNoneFound);
                                }
                                Thread.sleep(sleepWhenNoneFound);
                            }
                            timerContext.stop();
                            //send to the providers
                            consecutiveExceptions.set(0);
                        }catch (Exception ex){
                            logger.error("failed to dequeue",ex);
                            try {
                                long sleeptime = sleepWhenNoneFound*consecutiveExceptions.incrementAndGet();
                                long maxSleep = 15000;
                                sleeptime = sleeptime > maxSleep ? maxSleep : sleeptime ;
                                logger.info("sleeping due to failures {} ms", sleeptime);
                                Thread.sleep(sleeptime);
                            }catch (InterruptedException ie){
                                if (logger.isTraceEnabled()) {
                                    logger.info("sleep interrupted");
                                }
                            }
                        }
                    })
                    .toBlocking().lastOrDefault(null);


        }
    }

    public void stop(){
        if (logger.isDebugEnabled()) {
            logger.debug("stop processes");
        }

        if(futures == null){
            return;
        }
        for(Future future : futures){
            future.cancel(true);
        }

        pool.shutdownNow();
    }

}
