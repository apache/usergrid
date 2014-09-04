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
import rx.functions.Action1;
import rx.functions.Func1;
import rx.observables.GroupedObservable;
import rx.schedulers.Schedulers;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

public class QueueListener  {
    public static int MAX_CONSECUTIVE_FAILS = 10;


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

    private long sleepPeriod = 5000;

    ExecutorService pool;
    List<Future> futures;

    public static final String MAX_THREADS = "1";

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
        int threadCount = 0;
        try {
            sleepPeriod = new Long(properties.getProperty("usergrid.notifications.listener.sleep", "5000")).longValue();
            int maxThreads = new Integer(properties.getProperty("usergrid.notifications.listener.maxThreads", MAX_THREADS));
            futures = new ArrayList<Future>(maxThreads);
            while (threadCount++ < maxThreads) {
                futures.add(
                        pool.submit(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    execute();
                                } catch (Exception e) {
                                    LOG.error("failed to start push", e);
                                }
                            }
                        })
                );
            }
        }catch (Exception e){
            LOG.error("QueueListener failed to start:", e);
        }
    }

    private void execute(){
        svcMgr = smf.getServiceManager(smf.getManagementAppId());
        queueManager = svcMgr.getQueueManager();
        final AtomicInteger consecutiveExceptions = new AtomicInteger();
        // run until there are no more active jobs
        while ( true ) {
            try {
                QueueResults results = ApplicationQueueManager.getDeliveryBatch(queueManager);
                List<Message> messages = results.getMessages();
                if(messages.size()>0) {
                    Observable.from(messages) //observe all messages
                            .subscribeOn(Schedulers.io())
                            .map(new Func1<Message, ApplicationQueueMessage>() { //map a message to a typed message
                                @Override
                                public ApplicationQueueMessage call(Message message) {
                                    return ApplicationQueueMessage.generate(message);
                                }
                            })
                            .groupBy(new Func1<ApplicationQueueMessage, UUID>() { //group all of the messages together by app id
                                @Override
                                public UUID call(ApplicationQueueMessage message) {
                                    return message.getApplicationId();
                                }
                            })
                            .flatMap(new Func1<GroupedObservable<UUID, ApplicationQueueMessage>, Observable<?>>() { //take the observable and buffer in
                                @Override
                                public Observable<?> call(GroupedObservable<UUID, ApplicationQueueMessage> groupedObservable) {
                                    UUID appId = groupedObservable.getKey();
                                    EntityManager entityManager = emf.getEntityManager(appId);
                                    ServiceManager serviceManager = smf.getServiceManager(appId);
                                    final ApplicationQueueManager manager = new ApplicationQueueManager(
                                            new JobScheduler(serviceManager, entityManager),
                                            entityManager,
                                            queueManager,
                                            metricsService
                                    );

                                    return groupedObservable //buffer all of your notifications into a sender and send.
                                            .buffer(ApplicationQueueManager.BATCH_SIZE)
                                            .flatMap(new Func1<List<ApplicationQueueMessage>, Observable<?>>() {
                                                @Override
                                                public Observable<?> call(List<ApplicationQueueMessage> queueMessages) {
                                                    return manager.sendBatchToProviders(queueMessages);
                                                }
                                            });
                                }
                            })
                            .doOnError(new Action1<Throwable>() {
                                @Override
                                public void call(Throwable throwable) {
                                    LOG.error("Failed while listening",throwable);
                                }
                            })
                            .toBlocking()
                            .last();
                    LOG.info("Messages sent in batch");

                }
                else{
                    Thread.sleep(sleepPeriod);
                }
                //send to the providers
                consecutiveExceptions.set(0);
            }catch (Exception ex){
                LOG.error("failed to dequeue",ex);
                if(consecutiveExceptions.getAndIncrement() > MAX_CONSECUTIVE_FAILS){
                    LOG.error("killing message listener; too many failures");
                    break;
                }
            }
        }
    }

    public void stop(){
        for(Future future : futures){
            future.cancel(true);
        }
    }



}
