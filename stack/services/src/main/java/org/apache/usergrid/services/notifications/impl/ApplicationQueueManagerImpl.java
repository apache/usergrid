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
import org.apache.usergrid.persistence.core.metrics.MetricsFactory;
import org.apache.usergrid.persistence.entities.Device;
import org.apache.usergrid.persistence.entities.Notification;
import org.apache.usergrid.persistence.entities.Notifier;
import org.apache.usergrid.persistence.entities.Receipt;
import org.apache.usergrid.persistence.Query;
import org.apache.usergrid.persistence.queue.QueueManager;
import org.apache.usergrid.persistence.queue.QueueMessage;
import org.apache.usergrid.services.notifications.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;

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

    HashMap<Object, ProviderAdapter> notifierHashMap; // only retrieve notifiers once


    public ApplicationQueueManagerImpl(JobScheduler jobScheduler, EntityManager entityManager, QueueManager queueManager, MetricsFactory metricsFactory, Properties properties) {
        this.em = entityManager;
        this.qm = queueManager;
        this.jobScheduler = jobScheduler;
        this.metricsFactory = metricsFactory;
        this.queueName = getQueueNames(properties);
        queueMeter = metricsFactory.getMeter(ApplicationQueueManagerImpl.class, "notification.queue");
        sendMeter = metricsFactory.getMeter(NotificationsService.class, "queue.send");

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

        if (logger.isTraceEnabled()) {
            logger.trace("notification {} start queuing", notification.getUuid());
        }

        final PathQuery<Device> pathQuery = notification.getPathQuery().buildPathQuery(); //devices query
        final AtomicInteger deviceCount = new AtomicInteger(); //count devices so you can make a judgement on batching
        final ConcurrentLinkedQueue<String> errorMessages = new ConcurrentLinkedQueue<>(); //build up list of issues


        //get devices in querystring, and make sure you have access
        if (pathQuery != null) {
            final HashMap<Object, ProviderAdapter> notifierMap = getAdapterMap();
            if (logger.isTraceEnabled()) {
                logger.trace("notification {} start query", notification.getUuid());
            }
            final Iterator<Device> iterator = pathQuery.iterator(em);

            //if there are more pages (defined by PAGE_SIZE) you probably want this to be async, also if this is already a job then don't reschedule
            if (iterator instanceof ResultsIterator && ((ResultsIterator) iterator).hasPages() && jobExecution == null) {
                if(logger.isTraceEnabled()){
                    logger.trace("Scheduling notification job as it has multiple pages of devices.");
                }
                jobScheduler.scheduleQueueJob(notification, true);
                em.update(notification);
                return;
            }
            final UUID appId = em.getApplication().getUuid();
            final Map<String, Object> payloads = notification.getPayloads();

            final Func1<EntityRef, Optional<ApplicationQueueMessage>> sendMessageFunction = deviceRef -> {
                try {

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



            //process up to 10 concurrently
            Observable processMessagesObservable = Observable.create(new IteratorObservable<Entity>(iterator))
                .flatMap(entity -> {
                    return Observable.from(getDevices(entity));
                }, 10)
                .distinct(ref -> ref.getUuid())
                .map(sendMessageFunction)
                .buffer(100)
                .doOnNext( applicationQueueMessages -> {

                    applicationQueueMessages.forEach( message -> {

                        try {
                            if(message.isPresent()){
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

                    });


                })
                .doOnError(throwable -> logger.error("Failed while trying to send notification", throwable));

            processMessagesObservable.toBlocking().lastOrDefault(null); // let this run and block the async thread, messages are queued

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

        notification.setExpectedCount(deviceCount.get());
        notification.addProperties(properties);
        em.update(notification);


        // if no devices, go ahead and mark the batch finished
        if (deviceCount.get() <= 0 ) {
            TaskManager taskManager = new TaskManager(em, notification);
            taskManager.finishedBatch(true);
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
                    Receipt receipt = new Receipt(notification.getUuid(), message.getNotifierId(), payload, deviceUUID);
                    TaskTracker tracker = new TaskTracker(providerAdapter.getNotifier(), taskManager, receipt, deviceUUID);
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
     * Validates that a notifier and adapter exists to send notifications to;
     * {"winphone":"mymessage","apple":"mymessage"}
     * TODO: document this method better
     */
    private Map<String, Object> translatePayloads(Map<String, Object> payloads, Map<Object, ProviderAdapter>
        notifierMap) throws Exception {
        Map<String, Object> translatedPayloads = new HashMap<String, Object>(payloads.size());
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
        String name = properties.getProperty(ApplicationQueueManagerImpl.DEFAULT_QUEUE_PROPERTY, ApplicationQueueManagerImpl.DEFAULT_QUEUE_NAME);
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
                logger.error("checkForInactiveDevices", e); // not
                // essential so
                // don't fail,
                // but log
            }
        }
    }


    private boolean isOkToSend(Notification notification) {
        Map<String, Long> stats = notification.getStatistics();
        if (stats != null && notification.getExpectedCount() == (stats.get("sent") + stats.get("errors"))) {
            if (logger.isDebugEnabled()) {
                logger.debug("notification {} already processed. not sending.",
                    notification.getUuid());
            }
            return false;
        }
        if (notification.getCanceled() == Boolean.TRUE) {
            if (logger.isDebugEnabled()) {
                logger.debug("notification {} canceled. not sending.",
                    notification.getUuid());
            }
            return false;
        }
        if (notification.isExpired()) {
            if (logger.isDebugEnabled()) {
                logger.debug("notification {} expired. not sending.",
                    notification.getUuid());
            }
            return false;
        }
        return true;
    }

    private List<EntityRef> getDevices(EntityRef ref) {

        List<EntityRef> devices = new ArrayList<>();

        final int LIMIT = Query.MID_LIMIT;


        try {

            if ("device".equals(ref.getType())) {

                devices = Collections.singletonList(ref);

            } else if ("user".equals(ref.getType())) {

                UUID start = null;
                boolean initial = true;
                int resultSize = 0;
                while( initial || resultSize >= Query.DEFAULT_LIMIT) {

                    initial = false;

                    final List<EntityRef> mydevices = em.getCollection(ref, "devices", start, LIMIT,
                        Query.Level.REFS, true).getRefs();

                    resultSize = mydevices.size();

                    if(mydevices.size() > 0){
                        start = mydevices.get(mydevices.size() - 1 ).getUuid();
                    }

                    devices.addAll( mydevices  );

                }

            } else if ("group".equals(ref.getType())) {

                UUID start = null;
                boolean initial = true;
                int resultSize = 0;

                while( initial || resultSize >= LIMIT){

                    initial = false;
                    final List<EntityRef> myusers =  em.getCollection(ref, "users", start,
                        LIMIT, Query.Level.REFS, true).getRefs();

                    resultSize = myusers.size();

                    if(myusers.size() > 0){
                        start = myusers.get(myusers.size() - 1 ).getUuid();
                    }


                    // don't allow a single user to have more than 100 devices?
                    for (EntityRef user : myusers) {

                        devices.addAll( em.getCollection(user, "devices", null, 100,
                            Query.Level.REFS, true).getRefs() );

                    }

                }

            }
        } catch (Exception e) {

            if (ref != null){
                logger.error("Error while retrieving devices for entity type {} and uuid {}. Error: {}",
                    ref.getType(), ref.getUuid(), e);
            }else{
                logger.error("Error while retrieving devices. Entity ref was null.");
            }

            throw new RuntimeException("Unable to retrieve devices for EntityRef", e);

        }

        return devices;
    }


    private String getProviderId(EntityRef device, Notifier notifier) throws Exception {
        try {
            Object value = em.getProperty(device, notifier.getName() + NOTIFIER_ID_POSTFIX);
            if (value == null) {
                value = em.getProperty(device, notifier.getUuid() + NOTIFIER_ID_POSTFIX);
            }
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            logger.error("Error getting provider ID, proceeding with rest of batch", e);
            return null;
        }
    }


}
