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
package org.apache.usergrid.services.notifications.impl;

import com.codahale.metrics.Meter;
import org.apache.usergrid.batch.JobExecution;
import org.apache.usergrid.persistence.*;
import org.apache.usergrid.persistence.core.executor.TaskExecutorFactory;
import org.apache.usergrid.persistence.core.metrics.MetricsFactory;
import org.apache.usergrid.persistence.entities.*;
import org.apache.usergrid.persistence.Query;
import org.apache.usergrid.persistence.queue.QueueManager;
import org.apache.usergrid.persistence.queue.QueueMessage;
import org.apache.usergrid.services.notifications.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;


public class ApplicationQueueManagerImpl implements ApplicationQueueManager {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationQueueManagerImpl.class);

    //this is for tests, will not mark initial post complete, set to false for tests

    private final EntityManager em;
    private final QueueManager qm;
    private final JobScheduler jobScheduler;
    private final MetricsFactory metricsFactory;
    private final String queueName;
    private final Meter queueMeter;
    private final Meter sendMeter;

    private final static String PUSH_PROCESSING_MAXTHREADS_PROP = "usergrid.push.async.processing.threads";
    private final static String PUSH_PROCESSING_QUEUESIZE_PROP = "usergrid.push.async.processing.queue.size";
    private final static String PUSH_PROCESSING_CONCURRENCY_PROP = "usergrid.push.async.processing.concurrency";

    HashMap<Object, ProviderAdapter> notifierHashMap; // only retrieve notifiers once



    private final ExecutorService asyncExecutor;




    public ApplicationQueueManagerImpl( JobScheduler jobScheduler, EntityManager entityManager,
                                        QueueManager queueManager, MetricsFactory metricsFactory,
                                        Properties properties) {
        this.em = entityManager;
        this.qm = queueManager;
        this.jobScheduler = jobScheduler;
        this.metricsFactory = metricsFactory;
        this.queueName = getQueueNames(properties);
        this.queueMeter = metricsFactory.getMeter(ApplicationQueueManagerImpl.class, "notification.queue");
        this.sendMeter = metricsFactory.getMeter(NotificationsService.class, "queue.send");

        int maxAsyncThreads;
        int workerQueueSize;

        try {

            maxAsyncThreads = Integer.valueOf(System.getProperty(PUSH_PROCESSING_MAXTHREADS_PROP, "200"));
            workerQueueSize = Integer.valueOf(System.getProperty(PUSH_PROCESSING_QUEUESIZE_PROP, "2000"));

        } catch (Exception e){

            // if junk is passed into the property, just default the values
            maxAsyncThreads = 200;
            workerQueueSize = 2000;

        }


        // create our own executor which has a bounded queue w/ caller runs policy for rejected tasks
        this.asyncExecutor = TaskExecutorFactory
            .createTaskExecutor( "push-device-io", maxAsyncThreads, workerQueueSize,
                TaskExecutorFactory.RejectionAction.CALLERRUNS );


    }

    private boolean scheduleQueueJob(Notification notification) throws Exception {
        return jobScheduler.scheduleQueueJob(notification);
    }

    @Override
    public void queueNotification(final Notification notification, final JobExecution jobExecution) throws Exception {
        if (scheduleQueueJob(notification)) {
            em.update(notification);
            return;
        }

        long startTime = System.currentTimeMillis();

        if (notification.getCanceled() == Boolean.TRUE) {
            if (logger.isDebugEnabled()) {
                logger.debug("notification " + notification.getUuid() + " canceled");
            }
            if (jobExecution != null) {
                jobExecution.killed();
            }
            return;
        }

        final PathQuery<Device> pathQuery = notification.getPathQuery().buildPathQuery(); //devices query
        final AtomicInteger deviceCount = new AtomicInteger(); //count devices so you can make a judgement on batching
        final ConcurrentLinkedQueue<String> errorMessages = new ConcurrentLinkedQueue<>(); //build up list of issues

        // Get devices in querystring, and make sure you have access
        if (pathQuery != null) {
            final HashMap<Object, ProviderAdapter> notifierMap = getAdapterMap();
            if (logger.isTraceEnabled()) {
                logger.trace("notification {} start query", notification.getUuid());
            }

            logger.info("Notification {} started processing", notification.getUuid());



            // The main iterator can use graph traversal or index querying based on payload property. Default is Index.
            final Iterator<Device> iterator;
            if( notification.getUseGraph()){
                iterator = pathQuery.graphIterator(em);
            }else{
                iterator = pathQuery.iterator(em);
            }

            /**** Old code to scheduler large sets of data, but now the processing is fired off async in the background.
                Leaving this only a reference of how to do it, if needed in future.

                    //if there are more pages (defined by PAGE_SIZE) you probably want this to be async,
                    //also if this is already a job then don't reschedule

                    if (iterator instanceof ResultsIterator
                                && ((ResultsIterator) iterator).hasPages() && jobExecution == null) {

                        if(logger.isTraceEnabled()){
                            logger.trace("Scheduling notification job as it has multiple pages of devices.");
                        }
                        jobScheduler.scheduleQueueJob(notification, true);
                        em.update(notification);
                        return;
                     }
             ****/

            final UUID appId = em.getApplication().getUuid();
            final Map<String, Object> payloads = notification.getPayloads();

            final Func1<EntityRef, Optional<ApplicationQueueMessage>> sendMessageFunction = deviceRef -> {

                try {

                    //logger.info("Preparing notification queue message for device: {}", deviceRef.getUuid());

                    long now = System.currentTimeMillis();

                    String notifierId = null;
                    String notifierKey = null;

                    //find the device notifier info, match it to the payload
                    for (Map.Entry<String, Object> entry : payloads.entrySet()) {
                        ProviderAdapter adapter = notifierMap.get(entry.getKey().toLowerCase());
                        now = System.currentTimeMillis();
                        String providerId = getProviderId(deviceRef, adapter.getNotifier());
                        if (providerId != null) {
                            notifierId = providerId;
                            notifierKey = entry.getKey().toLowerCase();
                            break;
                        }
                        if (logger.isTraceEnabled()) {
                            logger.trace("Provider query for notification {} device {} took {} ms", notification.getUuid(), deviceRef.getUuid(), (System.currentTimeMillis() - now));
                        }
                    }

                    if (notifierId == null) {
                        //TODO need to leverage optional here
                        return Optional.empty();
                    }

                    ApplicationQueueMessage message = new ApplicationQueueMessage(appId, notification.getUuid(), deviceRef.getUuid(), notifierKey, notifierId);
                    if (notification.getQueued() == null) {

                        // update queued time
                        notification.setQueued(System.currentTimeMillis());

                    }

                    deviceCount.incrementAndGet();

                    return Optional.of(message);


                } catch (Exception deviceLoopException) {
                    logger.error("Failed to add device", deviceLoopException);
                    errorMessages.add("Failed to add device: " + deviceRef.getUuid() + ", error:" + deviceLoopException);

                    return Optional.empty();
                }

            };


            final Map<String, Object> filters = notification.getFilters();

            Observable processMessagesObservable = Observable.create(new IteratorObservable<EntityRef>(iterator))

                .flatMap( entityRef -> {

                    return Observable.just(entityRef).flatMap(ref->{

                        List<Entity> entities = new ArrayList<>();

                            if( ref.getType().equals(User.ENTITY_TYPE)){

                                Query devicesQuery = new Query();
                                devicesQuery.setCollection("devices");
                                devicesQuery.setResultsLevel(Query.Level.CORE_PROPERTIES);

                                try {

                                   entities = em.searchCollection(new SimpleEntityRef("user", ref.getUuid()), devicesQuery.getCollection(), devicesQuery).getEntities();

                                }catch (Exception e){

                                    logger.error("Unable to load devices for user: {}", ref.getUuid());
                                    return Observable.empty();
                                }


                            }else if ( ref.getType().equals(Device.ENTITY_TYPE)){

                                try{
                                    entities.add(em.get(ref));

                                }catch(Exception e){

                                    logger.error("Unable to load device: {}", ref.getUuid());
                                    return Observable.empty();

                                }

                            }
                        return Observable.from(entities);

                        })
                        .distinct( deviceRef -> deviceRef.getUuid())
                        .filter( device -> {

                            if(logger.isTraceEnabled()) {
                                logger.trace("Filtering device: {}", device.getUuid());
                            }

                            if(notification.getUseGraph() && filters.size() > 0 ) {

                                for (Map.Entry<String, Object> entry : filters.entrySet()) {

                                    if ((device.getDynamicProperties().get(entry.getKey()) != null &&
                                        device.getDynamicProperties().get(entry.getKey()).equals(entry.getValue())) ||

                                        (device.getProperties().get(entry.getKey()) != null &&
                                            device.getProperties().get(entry.getKey()).equals(entry.getValue()))

                                        ) {


                                        return true;
                                    }

                                }
                                if(logger.isTraceEnabled()) {
                                    logger.trace("Push notification filter did not match for notification {}, so removing from notification",
                                        device.getUuid(), notification.getUuid());
                                }
                                return false;


                            }

                            return true;

                        })
                        .map(sendMessageFunction)
                        .doOnNext( message -> {
                            try {

                                if(message.isPresent()){

                                    if(logger.isTraceEnabled()) {
                                        logger.trace("Queueing notification message for device: {}", message.get().getDeviceId());
                                    }
                                    qm.sendMessage( message.get() );
                                    queueMeter.mark();
                                }

                            } catch (IOException e) {

                                if(message.isPresent()){
                                    logger.error("Unable to queue notification for notification UUID {} and device UUID {} ",
                                        message.get().getNotificationId(), message.get().getDeviceId());
                                }
                                else{
                                    logger.error("Unable to queue notification as it's not present when trying to send to queue");
                                }

                            }


                        }).subscribeOn(Schedulers.from(asyncExecutor));

                }, Integer.valueOf(System.getProperty(PUSH_PROCESSING_CONCURRENCY_PROP, "50")))
                .doOnError(throwable -> {

                    logger.error("Error while processing devices for notification : {}", notification.getUuid());
                    notification.setProcessingFinished(-1L);
                    notification.setDeviceProcessedCount(deviceCount.get());
                    logger.warn("Partial notification. Only {} devices processed for notification {}",
                        deviceCount.get(), notification.getUuid());
                    try {
                        em.update(notification);
                    }catch (Exception e){
                        logger.error("Error updating negative processing status when processing failed.");
                    }

                })
                .doOnCompleted( () -> {

                    try {
                        notification.setProcessingFinished(System.currentTimeMillis());
                        notification.setDeviceProcessedCount(deviceCount.get());
                        em.update(notification);
                        logger.info("Notification {} finished processing {} device(s)", notification.getUuid(), deviceCount.get());

                    } catch (Exception e) {
                        logger.error("Unable to set processing finished timestamp for notification");
                    }

                });

            processMessagesObservable.subscribeOn(Schedulers.from(asyncExecutor)).subscribe(); // fire the queuing into the background

        }

        // update queued time
        Map<String, Object> properties = new HashMap<>(2);
        properties.put("queued", notification.getQueued());
        properties.put("state", notification.getState());
        if (errorMessages.size() > 0) {
            if (notification.getErrorMessage() == null) {
                notification.setErrorMessage("There was a problem delivering all of your notifications. See deliveryErrors in properties");
            }
        }

        notification.addProperties(properties);
        em.update(notification);


        // if no devices, go ahead and mark the batch finished
        if (deviceCount.get() <= 0 ) {
            TaskManager taskManager = new TaskManager(em, notification);
            taskManager.finishedBatch();
        }


    }

    /**
     * only need to get notifiers once. will reset on next batch
     *
     * @return
     */
    private HashMap<Object, ProviderAdapter> getAdapterMap() {
        if (notifierHashMap == null) {
            long now = System.currentTimeMillis();
            notifierHashMap = new HashMap<>();
            Query query = new Query();
            query.setCollection("notifiers");
            query.setLimit(100);
            PathQuery<Notifier> pathQuery = new PathQuery<>(
                new SimpleEntityRef(em.getApplicationRef()),
                query
            );
            Iterator<Notifier> notifierIterator = pathQuery.iterator(em);
            int count = 0;
            while (notifierIterator.hasNext()) {
                Notifier notifier = notifierIterator.next();
                String name = notifier.getName() != null ? notifier.getName() : "";
                UUID uuid = notifier.getUuid() != null ? notifier.getUuid() : UUID.randomUUID();
                ProviderAdapter providerAdapter = ProviderAdapterFactory.getProviderAdapter(notifier, em);
                notifierHashMap.put(name.toLowerCase(), providerAdapter);
                notifierHashMap.put(uuid, providerAdapter);
                notifierHashMap.put(uuid.toString(), providerAdapter);
                if (count++ >= 100) {
                    logger.error("ApplicationQueueManager: too many notifiers...breaking out ", notifierHashMap.size());
                    break;
                }
            }
        }
        return notifierHashMap;
    }

    /**
     * send batches of notifications to provider
     *
     * @param messages
     * @throws Exception
     */
    @Override
    public Observable sendBatchToProviders(final List<QueueMessage> messages, final String queuePath) {
        if (logger.isTraceEnabled()) {
            logger.trace("sending batch of {} notifications.", messages.size());
        }

        final Map<Object, ProviderAdapter> notifierMap = getAdapterMap();
        final ApplicationQueueManagerImpl proxy = this;
        final ConcurrentHashMap<UUID, TaskManager> taskMap = new ConcurrentHashMap<UUID, TaskManager>(messages.size());
        final ConcurrentHashMap<UUID, Notification> notificationMap = new ConcurrentHashMap<UUID, Notification>(messages.size());

        final Func1<QueueMessage, ApplicationQueueMessage> func = queueMessage -> {
            boolean messageCommitted = false;
            ApplicationQueueMessage message = null;
            try {
                message = (ApplicationQueueMessage) queueMessage.getBody();
                if (logger.isTraceEnabled()) {
                    logger.trace("start sending notification for device {} for Notification: {} on thread {}", message.getDeviceId(), message.getNotificationId(), Thread.currentThread().getId());
                }

                UUID deviceUUID = message.getDeviceId();

                Notification notification = notificationMap.get(message.getNotificationId());
                if (notification == null) {
                    notification = em.get(message.getNotificationId(), Notification.class);
                    notificationMap.putIfAbsent(message.getNotificationId(), notification);
                }
                TaskManager taskManager = taskMap.get(message.getNotificationId());
                if (taskManager == null) {
                    taskManager = new TaskManager(em, notification);
                    taskMap.putIfAbsent(message.getNotificationId(), taskManager);
                    taskManager = taskMap.get(message.getNotificationId());
                }

                final Map<String, Object> payloads = notification.getPayloads();
                final Map<String, Object> translatedPayloads = translatePayloads(payloads, notifierMap);
                if (logger.isTraceEnabled()) {
                    logger.trace("sending notification for device {} for Notification: {}", deviceUUID, notification.getUuid());
                }

                try {
                    String notifierName = message.getNotifierKey().toLowerCase();
                    ProviderAdapter providerAdapter = notifierMap.get(notifierName.toLowerCase());
                    Object payload = translatedPayloads.get(notifierName);

                    TaskTracker tracker = null;

                    if(notification.getSaveReceipts()){

                        final Receipt receipt =
                            new Receipt( notification.getUuid(), message.getNotifierId(), payload, deviceUUID );
                        tracker =
                            new TaskTracker( providerAdapter.getNotifier(), taskManager, receipt, deviceUUID );

                    }
                    else {

                        tracker =
                            new TaskTracker( providerAdapter.getNotifier(), taskManager, null, deviceUUID );
                    }
                    if (!isOkToSend(notification)) {
                        tracker.failed(0, "Notification is duplicate/expired/cancelled.");
                    } else {
                        if (payload == null) {
                            if (logger.isDebugEnabled()) {
                                logger.debug("selected device {} for notification {} doesn't have a valid payload. skipping.", deviceUUID, notification.getUuid());
                            }
                            tracker.failed(0, "failed to match payload to " + message.getNotifierId() + " notifier");
                        } else {
                            long now = System.currentTimeMillis();
                            try {
                                providerAdapter.sendNotification(message.getNotifierId(), payload, notification, tracker);
                            } catch (Exception e) {
                                tracker.failed(0, e.getMessage());
                            } finally {
                                if (logger.isTraceEnabled()) {
                                    logger.trace("sending to device {} for Notification: {} duration {} ms", deviceUUID, notification.getUuid(), (System.currentTimeMillis() - now));
                                }
                            }
                        }
                    }
                    messageCommitted = true;
                } finally {
                    sendMeter.mark();
                }

            } catch (Exception e) {
                logger.error("Failure while sending", e);
                try {
                    if (!messageCommitted && queuePath != null) {
                        qm.commitMessage(queueMessage);
                    }
                } catch (Exception queueException) {
                    logger.error("Failed to commit message.", queueException);
                }
            }
            return message;
        };

        //from each queue message, process them in parallel up to 10 at a time
        Observable queueMessageObservable = Observable.from(messages).flatMap(queueMessage -> {


            return Observable.just(queueMessage).map(func).buffer(messages.size()).map(queueMessages -> {
                //for gcm this will actually send notification
                for (ProviderAdapter providerAdapter : notifierMap.values()) {
                    try {
                        providerAdapter.doneSendingNotifications();
                    } catch (Exception e) {
                        logger.error("providerAdapter.doneSendingNotifications: ", e);
                    }
                }
                //TODO: check if a notification is done and mark it
                HashMap<UUID, ApplicationQueueMessage> notifications = new HashMap<>();
                for (ApplicationQueueMessage message : queueMessages) {
                    if (notifications.get(message.getNotificationId()) == null) {
                        try {
                            TaskManager taskManager = taskMap.get(message.getNotificationId());
                            notifications.put(message.getNotificationId(), message);
                            taskManager.finishedBatch();
                        } catch (Exception e) {
                            logger.error("Failed to finish batch", e);
                        }
                    }
                }
                return notifications;
            }).doOnError(throwable -> logger.error("Failed while sending", throwable));
        }, 10);

        return queueMessageObservable;
    }

    @Override
    public void stop() {
        for (ProviderAdapter adapter : getAdapterMap().values()) {
            try {
                adapter.stop();
            } catch (Exception e) {
                logger.error("failed to stop adapter", e);
            }
        }
    }


    /**
     *  Validates that a notifier and adapter exists to send notifications to. For the example payload
     *
     *  { "payloads" : {"winphone":"mymessage","apple":"mymessage"} }
     *
     *  Notifiers with name "winphone" and "apple" must exist.
     */
    private Map<String, Object> translatePayloads(Map<String, Object> payloads,
                                                  Map<Object, ProviderAdapter> notifierMap) throws Exception {

        final Map<String, Object> translatedPayloads = new HashMap<String, Object>(payloads.size());

        for (Map.Entry<String, Object> entry : payloads.entrySet()) {

            String payloadKey = entry.getKey().toLowerCase();
            Object payloadValue = entry.getValue();

            //look for adapter from payload map
            ProviderAdapter providerAdapter = notifierMap.get(payloadKey);
            if (providerAdapter != null) {

                //translate payload to usable information
                Object translatedPayload = payloadValue != null ? providerAdapter.translatePayload(payloadValue) : null;
                if (translatedPayload != null) {
                    translatedPayloads.put(payloadKey, translatedPayload);
                }

            }
        }
        return translatedPayloads;
    }

    public static String getQueueNames(Properties properties) {

        String name = properties.getProperty(ApplicationQueueManagerImpl.DEFAULT_QUEUE_PROPERTY,
            ApplicationQueueManagerImpl.DEFAULT_QUEUE_NAME);
        return name;

    }

    private static final class IteratorObservable<T> implements rx.Observable.OnSubscribe<T> {
        private final Iterator<T> input;

        private IteratorObservable(final Iterator input) {
            this.input = input;
        }

        @Override
        public void call(final Subscriber<? super T> subscriber) {

            /**
             * You would replace this code with your file reading.  Instead of emitting from an iterator,
             * you would create a bean object that represents the entity, and then emit it
             */

            try {
                while (!subscriber.isUnsubscribed() && input.hasNext()) {

                    //send our input to the next
                    subscriber.onNext((T) input.next());

                }

                //tell the subscriber we don't have any more data
                subscriber.onCompleted();

            } catch (Throwable t) {
                logger.error("failed on subscriber", t);
                subscriber.onError(t);
            }
        }
    }

    @Override
    public void asyncCheckForInactiveDevices() throws Exception {
        Collection<ProviderAdapter> providerAdapters = getAdapterMap().values();
        for (final ProviderAdapter providerAdapter : providerAdapters) {
            try {
                if (providerAdapter != null) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("checking notifier {} for inactive devices", providerAdapter.getNotifier());
                    }
                    providerAdapter.removeInactiveDevices();

                    if (logger.isTraceEnabled()) {
                        logger.trace("finished checking notifier {} for inactive devices", providerAdapter.getNotifier());
                    }
                }
            } catch (Exception e) {
                // not essential so don't fail, but log
                logger.error("checkForInactiveDevices", e);

            }
        }
    }


    private boolean isOkToSend(Notification notification) {

        if (notification.getCanceled() == Boolean.TRUE) {
            if (logger.isDebugEnabled()) {
                logger.debug("Notification {} canceled. Not sending.",
                    notification.getUuid());
            }
            return false;
        }
        if (notification.isExpired()) {
            if (logger.isDebugEnabled()) {
                logger.debug("Notification {} expired. Not sending.",
                    notification.getUuid());
            }
            return false;
        }
        return true;
    }


    private String getProviderId(EntityRef device, Notifier notifier) throws Exception {
        try {
            Object value = em.getProperty(device, notifier.getName() + NOTIFIER_ID_POSTFIX);
            if (value == null) {
                value = em.getProperty(device, notifier.getUuid() + NOTIFIER_ID_POSTFIX);
            }
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            logger.error("Error getting notifier for device {}, proceeding with rest of batch", device, e);
            return null;
        }
    }


}
