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

import org.apache.usergrid.metrics.MetricsFactory;
import org.apache.usergrid.mq.*;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.EntityManagerFactory;

import org.apache.usergrid.services.ServiceManager;
import org.apache.usergrid.services.ServiceManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import rx.Observable;
import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

public class QueueListener  {
    public static int MAX_CONSECUTIVE_FAILS = 10;

    public static final long MESSAGE_TRANSACTION_TIMEOUT = 60 * 5 * 1000;

    private static final Logger LOG = LoggerFactory.getLogger(QueueListener.class);

    @Autowired
    private MetricsFactory metricsService;

    @Autowired
    private ServiceManagerFactory smf;

    @Autowired
    private EntityManagerFactory emf;

    @Autowired
    private Properties properties;

    private org.apache.usergrid.mq.QueueManager queueManager;

    private ServiceManager svcMgr;

    private long sleepWhenNoneFound = 0;

    private long sleepBetweenRuns = 5000;

    ExecutorService pool;
    List<Future> futures;

    public static final String MAX_THREADS = "1";
    private Integer batchSize = 1000;
    private String queueName;

    public QueueListener() {
        pool = Executors.newFixedThreadPool(1);
    }
    public QueueListener(ServiceManagerFactory smf, EntityManagerFactory emf, MetricsFactory metricsService, Properties props){
        this();
        this.smf = smf;
        this.emf = emf;
        this.metricsService = metricsService;
        this.properties = props;
    }

    @PostConstruct
    void init() {
        run();
    }

    public void run(){
        boolean shouldRun = new Boolean(properties.getProperty("usergrid.notifications.listener.run", "true"));

        if(shouldRun) {
            LOG.info("QueueListener: starting.");
            int threadCount = 0;

            try {
                sleepBetweenRuns = new Long(properties.getProperty("usergrid.notifications.listener.sleep.between", "0")).longValue();
                sleepWhenNoneFound = new Long(properties.getProperty("usergrid.notifications.listener.sleep.after", "5000")).longValue();
                batchSize = new Integer(properties.getProperty("usergrid.notifications.listener.batchSize", (""+batchSize)));
                queueName = properties.getProperty(ApplicationQueueManager.DEFAULT_QUEUE_PROPERTY,ApplicationQueueManager.DEFAULT_QUEUE_NAME);

                int maxThreads = new Integer(properties.getProperty("usergrid.notifications.listener.maxThreads", MAX_THREADS));
                futures = new ArrayList<Future>(maxThreads);
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
            LOG.info("QueueListener: never started due to config value usergrid.notifications.listener.run.");
        }

    }

    private void execute(){
        Thread.currentThread().setName("Notifications_Processor"+UUID.randomUUID());
        svcMgr = smf.getServiceManager(smf.getManagementAppId());
        queueManager = svcMgr.getQueueManager();
        final AtomicInteger consecutiveExceptions = new AtomicInteger();
        LOG.info("QueueListener: Starting execute process.");

        // run until there are no more active jobs
        while ( true ) {
            try {
                QueueResults results = getDeliveryBatch(queueManager);
                LOG.info("QueueListener: retrieved batch of {} messages", results.size());

                List<Message> messages = results.getMessages();
                if (messages.size() > 0) {
                    HashMap<UUID, List<ApplicationQueueMessage>> messageMap = new HashMap<>(messages.size());
                    //group messages into hash map by app id
                    for (Message message : messages) {
                        ApplicationQueueMessage queueMessage = ApplicationQueueMessage.generate(message);
                        UUID applicationId = queueMessage.getApplicationId();
                        if (!messageMap.containsKey(applicationId)) {
                            List<ApplicationQueueMessage> applicationQueueMessages = new ArrayList<ApplicationQueueMessage>();
                            applicationQueueMessages.add(queueMessage);
                            messageMap.put(applicationId, applicationQueueMessages);
                        } else {
                            messageMap.get(applicationId).add(queueMessage);
                        }
                    }
                    long now = System.currentTimeMillis();
                    Observable merge = null;
                    //send each set of app ids together
                    for (Map.Entry<UUID, List<ApplicationQueueMessage>> entry : messageMap.entrySet()) {
                        UUID applicationId = entry.getKey();
                        EntityManager entityManager = emf.getEntityManager(applicationId);
                        ServiceManager serviceManager = smf.getServiceManager(applicationId);
                        final ApplicationQueueManager manager = new ApplicationQueueManager(
                                new JobScheduler(serviceManager, entityManager),
                                entityManager,
                                queueManager,
                                metricsService,
                                properties
                        );

                        LOG.info("QueueListener: send batch for app {} of {} messages", entry.getKey(), entry.getValue().size());
                        Observable current = manager.sendBatchToProviders(entry.getValue());
                        if(merge == null)
                            merge = current;
                        else {
                            merge = Observable.merge(merge,current);
                        }
                    }
                    if(merge!=null) {
                        merge.toBlocking().lastOrDefault(null);
                    }
                    LOG.info("QueueListener: sent batch {} messages duration {} ms", messages.size(),System.currentTimeMillis() - now);

                    if(sleepBetweenRuns > 0) {
                        Thread.sleep(sleepBetweenRuns);
                    }
                }
                else{
                    LOG.info("QueueListener: no messages...sleep...", results.size());
                    Thread.sleep(sleepWhenNoneFound);
                }
                //send to the providers
                consecutiveExceptions.set(0);
            }catch (Exception ex){
                LOG.error("failed to dequeue",ex);
                try {
                    Thread.sleep(sleepWhenNoneFound);
                }catch (InterruptedException ie){
                    LOG.info("sleep interupted");
                }
                if(consecutiveExceptions.getAndIncrement() > MAX_CONSECUTIVE_FAILS){
                    LOG.error("killing message listener; too many failures");
                    break;
                }
            }
        }
    }

    public void stop(){
        LOG.info("QueueListener: stop processes");

        for(Future future : futures){
            future.cancel(true);
        }
    }

    private  QueueResults getDeliveryBatch(org.apache.usergrid.mq.QueueManager queueManager) throws Exception {
        QueueQuery qq = new QueueQuery();
        qq.setLimit(this.getBatchSize());
        qq.setTimeout(MESSAGE_TRANSACTION_TIMEOUT);
        QueueResults results = queueManager.getFromQueue(queueName, qq);
        LOG.debug("got batch of {} devices", results.size());
        return results;
    }

    public void setBatchSize(int batchSize){
        this.batchSize = batchSize;
    }
    public int getBatchSize(){return batchSize;}

}
