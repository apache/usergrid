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

import com.clearspring.analytics.hash.MurmurHash;
import com.clearspring.analytics.stream.frequency.CountMinSketch;
import com.codahale.metrics.Meter;
import org.apache.usergrid.batch.JobExecution;
import org.apache.usergrid.metrics.MetricsFactory;
import org.apache.usergrid.persistence.*;
import org.apache.usergrid.persistence.entities.Device;
import org.apache.usergrid.persistence.entities.Notification;
import org.apache.usergrid.persistence.entities.Notifier;
import org.apache.usergrid.persistence.entities.Receipt;
import org.apache.usergrid.persistence.index.query.Query;
import org.apache.usergrid.services.notifications.apns.APNsAdapter;
import org.apache.usergrid.services.notifications.gcm.GCMAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Created by ApigeeCorporation on 8/27/14.
 */
public class ApplicationQueueManager implements QueueManager {

    public static  String DEFAULT_QUEUE_NAME = "notifications/queuelistenerv1_20;notifications/queuelistenerv1_21;notifications/queuelistenerv1_22";
    public static final String DEFAULT_QUEUE_PROPERTY = "usergrid.notifications.listener.queueName";
    private static final Logger LOG = LoggerFactory.getLogger(ApplicationQueueManager.class);

    //this is for tests, will not mark initial post complete, set to false for tests

    private static ExecutorService INACTIVE_DEVICE_CHECK_POOL = Executors.newFixedThreadPool(5);
    public static final String NOTIFIER_ID_POSTFIX = ".notifier.id";

    private final EntityManager em;
    private final org.apache.usergrid.mq.QueueManager qm;
    private final JobScheduler jobScheduler;
    private final MetricsFactory metricsFactory;
    private final String[] queueNames;

    HashMap<Object, Notifier> notifierHashMap; // only retrieve notifiers once

    public final Map<String, ProviderAdapter> providerAdapters =   new HashMap<String, ProviderAdapter>(3);
    {
        providerAdapters.put("apple", APNS_ADAPTER);
        providerAdapters.put("google", new GCMAdapter());
        providerAdapters.put("noop", TEST_ADAPTER);
    };
    // these 2 can be static, but GCM can't. future: would be nice to get gcm
    // static as well...
    public static ProviderAdapter APNS_ADAPTER = new APNsAdapter();
    public static ProviderAdapter TEST_ADAPTER = new TestAdapter();


    public ApplicationQueueManager(JobScheduler jobScheduler, EntityManager entityManager, org.apache.usergrid.mq.QueueManager queueManager, MetricsFactory metricsFactory, Properties properties){
        this.em = entityManager;
        this.qm = queueManager;
        this.jobScheduler = jobScheduler;
        this.metricsFactory = metricsFactory;
        this.queueNames = getQueueNames(properties);
    }



    public boolean scheduleQueueJob(Notification notification) throws Exception{
        return jobScheduler.scheduleQueueJob(notification);
    }

    public void queueNotification(final Notification notification, final JobExecution jobExecution) throws Exception {
        if(scheduleQueueJob(notification)){
            em.update(notification);
            return;
        }
        final Meter queueMeter = metricsFactory.getMeter(ApplicationQueueManager.class,"queue");
        long startTime = System.currentTimeMillis();

        if (notification.getCanceled() == Boolean.TRUE) {
            LOG.info("ApplicationQueueMessage: notification " + notification.getUuid() + " canceled");
            if (jobExecution != null) {
                jobExecution.killed();
            }
            return;
        }

        LOG.info("ApplicationQueueMessage: notification {} start queuing", notification.getUuid());

        final PathQuery<Device> pathQuery = notification.getPathQuery() ; //devices query
        final AtomicInteger deviceCount = new AtomicInteger(); //count devices so you can make a judgement on batching
        final ConcurrentLinkedQueue<String> errorMessages = new ConcurrentLinkedQueue<String>(); //build up list of issues

        final HashMap<Object,Notifier> notifierMap =  getNotifierMap();
        final String queueName = getRandomQueue(queueNames);

        //get devices in querystring, and make sure you have access
        if (pathQuery != null) {
            LOG.info("ApplicationQueueMessage: notification {} start query", notification.getUuid());
            final Iterator<Device> iterator = pathQuery.iterator(em);
            //if there are more pages (defined by PAGE_SIZE) you probably want this to be async, also if this is already a job then don't reschedule
            if (iterator instanceof ResultsIterator && ((ResultsIterator) iterator).hasPages() && jobExecution == null) {
                jobScheduler.scheduleQueueJob(notification, true);
                em.update(notification);
                return;
            }
            final CountMinSketch sketch = new CountMinSketch(0.0001,.99,7364181); //add probablistic counter to find dups
            final UUID appId = em.getApplication().getUuid();
            final Map<String,Object> payloads = notification.getPayloads();


            final Func1<Entity,Entity> entityListFunct = new Func1<Entity, Entity>() {
                @Override
                public Entity call(Entity entity) {

                    try {

                        long now = System.currentTimeMillis();
                        List<EntityRef> devicesRef = getDevices(entity); // resolve group

                        LOG.info("ApplicationQueueMessage: notification {} queue  {} devices, duration "+(System.currentTimeMillis()-now)+" ms", notification.getUuid(), devicesRef.size());

                        for (EntityRef deviceRef : devicesRef) {
                            LOG.info("ApplicationQueueMessage: notification {} starting to queue device {} ", notification.getUuid(), deviceRef.getUuid());
                            long hash = MurmurHash.hash(deviceRef.getUuid());
                            if (sketch.estimateCount(hash) > 0) { //look for duplicates
                                LOG.debug("ApplicationQueueMessage: Maybe Found duplicate device: {}", deviceRef.getUuid());
                                continue;
                            } else {
                                sketch.add(hash, 1);
                            }
                            String notifierId = null;
                            String notifierKey = null;

                            //find the device notifier info, match it to the payload
                            for (Map.Entry<String, Object> entry : payloads.entrySet()) {
                                Notifier notifier = notifierMap.get(entry.getKey().toLowerCase());
                                now = System.currentTimeMillis();
                                String providerId = getProviderId(deviceRef, notifier);
                                if (providerId != null) {
                                    notifierId = providerId;
                                    notifierKey = entry.getKey().toLowerCase();
                                    break;
                                }
                                LOG.info("ApplicationQueueMessage: Provider query for notification {} device {} took "+(System.currentTimeMillis()-now)+" ms",notification.getUuid(),deviceRef.getUuid());
                            }

                            if (notifierId == null) {
                                LOG.debug("ApplicationQueueMessage: Notifier did not match for device {} ", deviceRef);
                                continue;
                            }

                            ApplicationQueueMessage message = new ApplicationQueueMessage(appId, notification.getUuid(), deviceRef.getUuid(), notifierKey, notifierId);
                            if (notification.getQueued() == null) {
                                // update queued time
                                now = System.currentTimeMillis();
                                notification.setQueued(System.currentTimeMillis());
                                LOG.info("ApplicationQueueMessage: notification {} device {} queue time set. duration "+(System.currentTimeMillis()-now)+" ms", notification.getUuid(), deviceRef.getUuid());
                            }
                            now = System.currentTimeMillis();
                            qm.postToQueue(queueName, message);
                            LOG.info("ApplicationQueueMessage: notification {} post-queue to device {} duration " + (System.currentTimeMillis() - now) + " ms", notification.getUuid(), deviceRef.getUuid());
                            deviceCount.incrementAndGet();
                            queueMeter.mark();
                        }
                    } catch (Exception deviceLoopException) {
                        LOG.error("Failed to add devices", deviceLoopException);
                        errorMessages.add("Failed to add devices for entity: " + entity.getUuid() + " error:" + deviceLoopException);
                    }
                    return entity;
                }
            };

            long now = System.currentTimeMillis();
            Observable o = rx.Observable.create(new IteratorObservable<Entity>(iterator))
                    .parallel(new Func1<Observable<Entity>, Observable<Entity>>() {
                        @Override
                        public rx.Observable<Entity> call(rx.Observable<Entity> deviceObservable) {
                            return deviceObservable.map(entityListFunct);
                        }
                    }, Schedulers.io())
                    .doOnError(new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            LOG.error("Failed while writing", throwable);
                        }
                    });
            o.toBlocking().lastOrDefault(null);
            LOG.info("ApplicationQueueMessage: notification {} done queueing duration {} ms", notification.getUuid(),System.currentTimeMillis() - now);


        }

        // update queued time
        Map<String, Object> properties = new HashMap<String, Object>(2);
        properties.put("queued", notification.getQueued());
        properties.put("state", notification.getState());


        if(errorMessages.size()>0){
            if (notification.getErrorMessage() == null) {
                notification.setErrorMessage("There was a problem delivering all of your notifications. See deliveryErrors in properties");
            }
        }

        notification.setExpectedCount(deviceCount.get());
        notification.addProperties(properties);
        long now = System.currentTimeMillis();

        em.update(notification);

        LOG.info("ApplicationQueueMessage: notification {} updated notification duration {} ms", notification.getUuid(),System.currentTimeMillis() - now);

        //do i have devices, and have i already started batching.
        if (deviceCount.get() <= 0) {
            SingleQueueTaskManager taskManager = new SingleQueueTaskManager(em, qm, this, notification,queueName);
            //if i'm in a test value will be false, do not mark finished for test orchestration, not ideal need real tests
            taskManager.finishedBatch();
        }

        if (LOG.isInfoEnabled()) {
            long elapsed = notification.getQueued() != null ? notification.getQueued() - startTime : 0;
            LOG.info("ApplicationQueueMessage: notification {} done queuing to {} devices in "+elapsed+" ms",notification.getUuid().toString(),deviceCount.get());
        }

    }

    /**
     * only need to get notifiers once. will reset on next batch
     * @return
     */
    public HashMap<Object,Notifier> getNotifierMap(){
        if(notifierHashMap == null) {
            long now = System.currentTimeMillis();
            notifierHashMap = new HashMap<Object, Notifier>();
            Query query = new Query();
            query.setCollection("notifiers");
            query.setLimit(100);
            PathQuery<Notifier> pathQuery = new PathQuery<Notifier>(
                    new SimpleEntityRef(em.getApplicationRef()),
                    query
            );
            Iterator<Notifier> notifierIterator = pathQuery.iterator(em);
            int count = 0;
            while (notifierIterator.hasNext()) {
                Notifier notifier = notifierIterator.next();
                String name = notifier.getName() != null ? notifier.getName() : "";
                UUID uuid = notifier.getUuid() != null ? notifier.getUuid() : UUID.randomUUID();
                notifierHashMap.put(name.toLowerCase(), notifier);
                notifierHashMap.put(uuid, notifier);
                notifierHashMap.put(uuid.toString(), notifier);
                if(count++ >= 100){
                    LOG.error("ApplicationQueueManager: too many notifiers...breaking out ", notifierHashMap.size());
                    break;
                }
            }
            LOG.info("ApplicationQueueManager: fetching notifiers finished size={}, duration {} ms", notifierHashMap.size(),System.currentTimeMillis() - now);
        }
        return notifierHashMap;
    }

    /**
     * send batches of notifications to provider
     * @param messages
     * @throws Exception
     */
    public Observable sendBatchToProviders( final List<ApplicationQueueMessage> messages, final String queuePath) {
        LOG.info("sending batch of {} notifications.", messages.size());
        final Meter sendMeter = metricsFactory.getMeter(NotificationsService.class, "send");

        final Map<Object, Notifier> notifierMap = getNotifierMap();
        final QueueManager proxy = this;
        final ConcurrentHashMap<UUID,SingleQueueTaskManager> taskMap = new ConcurrentHashMap<UUID, SingleQueueTaskManager>(messages.size());
        final ConcurrentHashMap<UUID,Notification> notificationMap = new ConcurrentHashMap<UUID, Notification>(messages.size());

        final Func1<ApplicationQueueMessage, ApplicationQueueMessage> func = new Func1<ApplicationQueueMessage, ApplicationQueueMessage>() {
            @Override
            public ApplicationQueueMessage call(ApplicationQueueMessage message) {
                try {
                    LOG.info("start sending notification for device {} for Notification: {} on thread "+Thread.currentThread().getId(), message.getDeviceId(), message.getNotificationId());

                    UUID deviceUUID = message.getDeviceId();

                    Notification notification = notificationMap.get(message.getNotificationId());
                    if (notification == null) {
                        notification = em.get(message.getNotificationId(), Notification.class);
                        notificationMap.put(message.getNotificationId(), notification);
                    }
                    SingleQueueTaskManager taskManager;
                    taskManager = taskMap.get(message.getNotificationId());
                    if (taskManager == null) {
                        taskManager = new SingleQueueTaskManager(em, qm, proxy, notification,queuePath);
                        taskMap.putIfAbsent(message.getNotificationId(), taskManager);
                        taskManager = taskMap.get(message.getNotificationId());
                    }

                    final Map<String, Object> payloads = notification.getPayloads();
                    final Map<String, Object> translatedPayloads = translatePayloads(payloads, notifierMap);
                    LOG.info("sending notification for device {} for Notification: {}", deviceUUID, notification.getUuid());

                    taskManager.addMessage(deviceUUID,message);
                    try {
                        String notifierName = message.getNotifierKey().toLowerCase();
                        Notifier notifier = notifierMap.get(notifierName.toLowerCase());
                        Object payload = translatedPayloads.get(notifierName);
                        Receipt receipt = new Receipt(notification.getUuid(), message.getNotifierId(), payload, deviceUUID);
                        TaskTracker tracker = new TaskTracker(notifier, taskManager, receipt, deviceUUID);
                        if(!isOkToSend(notification)){
                             tracker.failed(0, "Notification is duplicate/expired/cancelled.");
                        }else {
                            if (payload == null) {
                                LOG.debug("selected device {} for notification {} doesn't have a valid payload. skipping.", deviceUUID, notification.getUuid());
                                tracker.failed(0, "failed to match payload to " + message.getNotifierId() + " notifier");

                            } else {
                                long now = System.currentTimeMillis();
                                try {
                                    ProviderAdapter providerAdapter = providerAdapters.get(notifier.getProvider());
                                    providerAdapter.sendNotification(message.getNotifierId(), notifier, payload, notification, tracker);
                                } catch (Exception e) {
                                    tracker.failed(0, e.getMessage());
                                } finally {
                                    LOG.info("sending to device {} for Notification: {} duration " + (System.currentTimeMillis() - now) + " ms", deviceUUID, notification.getUuid());
                                }
                            }
                        }
                    } finally {
                        sendMeter.mark();
                    }

                } catch (Exception e) {
                    LOG.error("Failure while sending",e);
                }
                return message;
            }
        };
        Observable o = rx.Observable.from(messages)
                .parallel(new Func1<rx.Observable<ApplicationQueueMessage>, rx.Observable<ApplicationQueueMessage>>() {
                    @Override
                    public rx.Observable<ApplicationQueueMessage> call(rx.Observable<ApplicationQueueMessage> messageObservable) {
                        return messageObservable.map(func);
                    }
                }, Schedulers.io())
                .buffer(messages.size())
                .map(new Func1<List<ApplicationQueueMessage>, HashMap<UUID, ApplicationQueueMessage>>() {
                    @Override
                    public HashMap<UUID, ApplicationQueueMessage> call(List<ApplicationQueueMessage> queueMessages) {
                        //for gcm this will actually send notification
                        for (ProviderAdapter providerAdapter : providerAdapters.values()) {
                            try {
                                providerAdapter.doneSendingNotifications();
                            } catch (Exception e) {
                                LOG.error("providerAdapter.doneSendingNotifications: ", e);
                            }
                        }
                        //TODO: check if a notification is done and mark it
                        HashMap<UUID, ApplicationQueueMessage> notifications = new HashMap<UUID, ApplicationQueueMessage>();
                        for (ApplicationQueueMessage message : queueMessages) {
                            if (notifications.get(message.getNotificationId()) == null) {
                                try {
                                    SingleQueueTaskManager taskManager = taskMap.get(message.getNotificationId());
                                    notifications.put(message.getNotificationId(), message);
                                    taskManager.finishedBatch();
                                } catch (Exception e) {
                                    LOG.error("Failed to finish batch", e);
                                }
                            }

                        }
                        return notifications;
                    }
                })
                .doOnError(new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        LOG.error("Failed while sending",throwable);
                    }
                });
        return o;
    }


    /**
     * Call the adapter with the notifier
     */
    private Map<String, Object> translatePayloads(Map<String, Object> payloads, Map<Object, Notifier> notifierMap) throws Exception {
        Map<String, Object> translatedPayloads = new HashMap<String, Object>(
                payloads.size());
        for (Map.Entry<String, Object> entry : payloads.entrySet()) {
            String payloadKey = entry.getKey().toLowerCase();
            Object payloadValue = entry.getValue();
            Notifier notifier = notifierMap.get(payloadKey);
            if (notifier != null) {
                ProviderAdapter providerAdapter = providerAdapters.get(notifier.getProvider());
                if (providerAdapter != null) {
                    Object translatedPayload = payloadValue != null ? providerAdapter.translatePayload(payloadValue) : null;
                    if (translatedPayload != null) {
                        translatedPayloads.put(payloadKey, translatedPayload);
                    }
                }
            }
        }
        return translatedPayloads;
    }

    public static String[] getQueueNames(Properties properties) {
        String[] names = properties.getProperty(ApplicationQueueManager.DEFAULT_QUEUE_PROPERTY,ApplicationQueueManager.DEFAULT_QUEUE_NAME).split(";");
        return names;
    }
    public static String getRandomQueue(String[] queueNames) {
        int size = queueNames.length;
        Random random = new Random();
        String name = queueNames[random.nextInt(size)];
        return name;
    }

    private static final class IteratorObservable<T> implements rx.Observable.OnSubscribe<T> {
        private final Iterator<T> input;
        private IteratorObservable( final Iterator input ) {this.input = input;}

        @Override
        public void call( final Subscriber<? super T> subscriber ) {

            /**
             * You would replace this code with your file reading.  Instead of emitting from an iterator,
             * you would create a bean object that represents the entity, and then emit it
             */

            try {
                while ( !subscriber.isUnsubscribed() && input.hasNext() ) {
                    //send our input to the next
                    subscriber.onNext( (T) input.next() );
                }

                //tell the subscriber we don't have any more data
                subscriber.onCompleted();
            }
            catch ( Throwable t ) {
                LOG.error("failed on subscriber",t);
                subscriber.onError( t );
            }
        }
    }

    public void asyncCheckForInactiveDevices(Set<Notifier> notifiers)  throws Exception {
        for (final Notifier notifier : notifiers) {
            INACTIVE_DEVICE_CHECK_POOL.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        checkForInactiveDevices(notifier);
                    } catch (Exception e) {
                        LOG.error("checkForInactiveDevices", e); // not
                        // essential so
                        // don't fail,
                        // but log
                    }
                }
            });
        }
    }

    /** gets the list of inactive devices from the Provider and updates them */
    private void checkForInactiveDevices(Notifier notifier) throws Exception {
        ProviderAdapter providerAdapter = providerAdapters.get(notifier
                .getProvider());
        if (providerAdapter != null) {
            LOG.debug("checking notifier {} for inactive devices", notifier);
            Map<String, Date> inactiveDeviceMap = providerAdapter
                    .getInactiveDevices(notifier, em);

            if (inactiveDeviceMap != null && inactiveDeviceMap.size() > 0) {
                LOG.debug("processing {} inactive devices",
                        inactiveDeviceMap.size());
                Map<String, Object> clearPushtokenMap = new HashMap<String, Object>(
                        2);
                clearPushtokenMap.put(notifier.getName() + NOTIFIER_ID_POSTFIX,
                        "");
                clearPushtokenMap.put(notifier.getUuid() + NOTIFIER_ID_POSTFIX,
                        "");

                // todo: this could be done in a single query
                for (Map.Entry<String, Date> entry : inactiveDeviceMap
                        .entrySet()) {
                    // name
                    Query query = new Query();
                    query.addEqualityFilter(notifier.getName()
                            + NOTIFIER_ID_POSTFIX, entry.getKey());
                    Results results = em.searchCollection(em.getApplication(),
                            "devices", query);
                    for (Entity e : results.getEntities()) {
                        em.updateProperties(e, clearPushtokenMap);
                    }
                    // uuid
                    query = new Query();
                    query.addEqualityFilter(notifier.getUuid()
                            + NOTIFIER_ID_POSTFIX, entry.getKey());
                    results = em.searchCollection(em.getApplication(),
                            "devices", query);
                    for (Entity e : results.getEntities()) {
                        em.updateProperties(e, clearPushtokenMap);
                    }
                }
            }
            LOG.debug("finished checking notifier {} for inactive devices",
                    notifier);
        }
    }

    private boolean isOkToSend(Notification notification) {
        if (notification.getFinished() != null) {
            LOG.info("notification {} already processed. not sending.",
                    notification.getUuid());
            return false;
        }
        if (notification.getCanceled() == Boolean.TRUE) {
            LOG.info("notification {} canceled. not sending.",
                    notification.getUuid());
            return false;
        }
        if (notification.isExpired()) {
            LOG.info("notification {} expired. not sending.",
                    notification.getUuid());
            return false;
        }
        return true;
    }

    private List<EntityRef> getDevices(EntityRef ref) throws Exception {
        List<EntityRef> devices = Collections.EMPTY_LIST;
        if ("device".equals(ref.getType())) {
            devices = Collections.singletonList(ref);
        } else if ("user".equals(ref.getType())) {
            devices = em.getCollection(ref, "devices", null, Query.MAX_LIMIT,
                    Query.Level.REFS, false).getRefs();
        } else if ("group".equals(ref.getType())) {
            devices = new ArrayList<EntityRef>();
            for (EntityRef r : em.getCollection(ref, "users", null,
                    Query.MAX_LIMIT, Query.Level.REFS, false).getRefs()) {
                devices.addAll(getDevices(r));
            }
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
            LOG.error("Errer getting provider ID, proceding with rest of batch", e);
            return null;
        }
    }


}