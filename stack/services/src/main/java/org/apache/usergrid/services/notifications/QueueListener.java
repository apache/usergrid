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
    public  final long MESSAGE_TRANSACTION_TIMEOUT =  1 * 60 * 1000;

    public   long DEFAULT_SLEEP = 5000;

    private static final Logger LOG = LoggerFactory.getLogger(QueueListener.class);

    private MetricsFactory metricsService;

    private ServiceManagerFactory smf;

    private EntityManagerFactory emf;


    private Properties properties;

    private org.apache.usergrid.mq.QueueManager queueManager;

    private ServiceManager svcMgr;

    private long sleepWhenNoneFound = 0;

    private long sleepBetweenRuns = 5000;

    private ExecutorService pool;
    private List<Future> futures;

    public  final String MAX_THREADS = "2";
    private Integer batchSize = 100;
    private String[] queueNames;



    public QueueListener(ServiceManagerFactory smf, EntityManagerFactory emf, MetricsFactory metricsService, Properties props){
        this.smf = smf;
        this.emf = emf;
        this.metricsService = metricsService;
        this.properties = props;
    }

    @PostConstruct
    public void start(){
        boolean shouldRun = new Boolean(properties.getProperty("usergrid.notifications.listener.run", "true"));

        if(shouldRun) {
            LOG.info("QueueListener: starting.");
            int threadCount = 0;

            try {
                sleepBetweenRuns = new Long(properties.getProperty("usergrid.notifications.listener.sleep.between", "0")).longValue();
                sleepWhenNoneFound = new Long(properties.getProperty("usergrid.notifications.listener.sleep.after", ""+DEFAULT_SLEEP)).longValue();
                batchSize = new Integer(properties.getProperty("usergrid.notifications.listener.batchSize", (""+batchSize)));
                queueNames = ApplicationQueueManager.getQueueNames(properties);

                int maxThreads = new Integer(properties.getProperty("usergrid.notifications.listener.maxThreads", MAX_THREADS));
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
            LOG.info("QueueListener: never started due to config value usergrid.notifications.listener.run.");
        }

    }

    private void execute(){
        Thread.currentThread().setName("Notifications_Processor"+UUID.randomUUID());

        final AtomicInteger consecutiveExceptions = new AtomicInteger();
        LOG.info("QueueListener: Starting execute process.");

        // run until there are no more active jobs
        while ( true ) {
            try {
                svcMgr = smf.getServiceManager(smf.getManagementAppId());
                queueManager = svcMgr.getQueueManager();
                String queueName = ApplicationQueueManager.getRandomQueue(queueNames);
                QueueResults results = getDeliveryBatch(queueManager,queueName);
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
                    LOG.info("QueueListener: sent batch {} messages duration {} ms", messages.size(),System.currentTimeMillis() - now);

                    if(sleepBetweenRuns > 0) {
                        LOG.info("QueueListener: sleep between rounds...sleep...{}", sleepBetweenRuns);
                        Thread.sleep(sleepBetweenRuns);
                    }
                }
                else{
                    LOG.info("QueueListener: no messages...sleep...{}", sleepWhenNoneFound);
                    Thread.sleep(sleepWhenNoneFound);
                }
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
        LOG.info("QueueListener: stop processes");

        for(Future future : futures){
            future.cancel(true);
        }

        pool.shutdownNow();
    }

    private  QueueResults getDeliveryBatch(org.apache.usergrid.mq.QueueManager queueManager,String queuePath) throws Exception {
        QueueQuery qq = new QueueQuery();
        qq.setLimit(this.getBatchSize());
        qq.setTimeout(MESSAGE_TRANSACTION_TIMEOUT);
        LOG.debug("getting from queue {} ", queuePath);
        QueueResults results = queueManager.getFromQueue(queuePath, qq);
        LOG.debug("got batch of {} devices from queue {} ", results.size(), queuePath);
        return results;
    }

    public void setBatchSize(int batchSize){
        this.batchSize = batchSize;
    }
    public int getBatchSize(){return batchSize;}

}
