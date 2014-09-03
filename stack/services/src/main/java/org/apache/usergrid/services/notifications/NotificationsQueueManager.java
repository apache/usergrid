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
import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.google.common.cache.*;
import org.apache.usergrid.batch.JobExecution;
import org.apache.usergrid.metrics.MetricsFactory;
import org.apache.usergrid.mq.Message;
import org.apache.usergrid.mq.QueueManager;
import org.apache.usergrid.mq.QueueQuery;
import org.apache.usergrid.mq.QueueResults;
import org.apache.usergrid.persistence.*;
import org.apache.usergrid.persistence.entities.*;
import org.apache.usergrid.persistence.index.query.Query;
import org.apache.usergrid.services.notifications.apns.APNsAdapter;
import org.apache.usergrid.services.notifications.gcm.GCMAdapter;
import org.apache.usergrid.utils.InflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * manages queues for notifications
 */
public class NotificationsQueueManager implements NotificationServiceProxy {
    private static final String NOTIFICATION_CONCURRENT_BATCHES = "notification.concurrent.batches";
    private static final long CONSECUTIVE_EMPTY_QUEUES = 10;

    private static final Logger LOG = LoggerFactory.getLogger(NotificationsQueueManager.class);

    //this is for tests, will not mark initial post complete, set to false for tests
    public static boolean IS_TEST = false;
    private final Meter sendMeter;
    private final Histogram queueSize;
    private final Counter outstandingQueue;
    private static ExecutorService INACTIVE_DEVICE_CHECK_POOL = Executors.newFixedThreadPool(5);
    public static final String NOTIFIER_ID_POSTFIX = ".notifier.id";
    public static int BATCH_SIZE = 1000;
    // timeout for message queue transaction
    public static final long MESSAGE_TRANSACTION_TIMEOUT = TaskManager.MESSAGE_TRANSACTION_TIMEOUT;
    // If this property is set Notifications are automatically expired in
    // the isOkToSent() method after the specified number of milliseconds
    public static final String PUSH_AUTO_EXPIRE_AFTER_PROPNAME = "usergrid.push-auto-expire-after";
    private final EntityManager em;
    private final QueueManager qm;
    private final JobScheduler jobScheduler;
    private final Properties props;
    private final InflectionUtils utils;
    private AtomicLong consecutiveEmptyQueues = new AtomicLong();
    private Long pushAutoExpireAfter = null;
    static final String MESSAGE_PROPERTY_DEVICE_UUID = "deviceUUID";

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

    //cache to retrieve push manager, cached per notifier, so many notifications will get same push manager
    private static LoadingCache<EntityManager, HashMap<Object,Notifier>> notifierCacheMap = CacheBuilder
            .newBuilder()
            .expireAfterWrite(90, TimeUnit.SECONDS)
            .build(new CacheLoader<EntityManager, HashMap<Object, Notifier>>() {
                @Override
                public HashMap<Object, Notifier> load(EntityManager em) {
                    HashMap<Object, Notifier> notifierHashMap = new HashMap<Object, Notifier>();
                    Query query = new Query();
                    query.setCollection("notifiers");
                    query.setLimit(100);
                    PathQuery<Notifier> pathQuery = new PathQuery<Notifier>((SimpleEntityRef)em.getApplicationRef(), query);
                    Iterator<Notifier> notifierIterator = pathQuery.iterator(em);
                    while (notifierIterator.hasNext()) {
                        Notifier notifier = notifierIterator.next();
                        String name = notifier.getName() != null ? notifier.getName() : "" ;
                        UUID uuid = notifier.getUuid() != null ? notifier.getUuid() : UUID.randomUUID();
                        notifierHashMap.put(name.toLowerCase(), notifier);
                        notifierHashMap.put(uuid, notifier);
                        notifierHashMap.put(uuid.toString(), notifier);
                    }
                    return notifierHashMap;
                }
            });;

    public NotificationsQueueManager(JobScheduler jobScheduler, EntityManager entityManager, Properties props, QueueManager queueManager, MetricsFactory metricsFactory){
        this.em = entityManager;
        this.qm = queueManager;
        this.jobScheduler = jobScheduler;
        this.props = props;
        this.sendMeter = metricsFactory.getMeter(NotificationsService.class, "send");
        this.queueSize = metricsFactory.getHistogram(NotificationsService.class, "queue_size");
        this.outstandingQueue = metricsFactory.getCounter(NotificationsService.class,"current_queue");
        utils = new InflectionUtils();
    }

    public boolean scheduleQueueJob(Notification notification) throws Exception{
        return jobScheduler.scheduleQueueJob(notification);
    }

    public void queueNotification(final Notification notification, final JobExecution jobExecution) throws Exception {
        if (notification.getCanceled() == Boolean.TRUE) {
            LOG.info("notification " + notification.getUuid() + " canceled");
            if (jobExecution != null) {
                jobExecution.killed();
            }
            return;
        }

        long startTime = System.currentTimeMillis();
        LOG.info("notification {} start queuing", notification.getUuid());
        final PathQuery<Device> pathQuery = notification.getPathQuery(); //devices query
        final AtomicInteger deviceCount = new AtomicInteger(); //count devices so you can make a judgement on batching
        final AtomicInteger batchCount = new AtomicInteger(); //count devices so you can make a judgement on batching
        final int numCurrentBatchesConfig = getNumConcurrentBatches();
        final ConcurrentLinkedQueue<String> errorMessages = new ConcurrentLinkedQueue<String>(); //build up list of issues

        //get devices in querystring, and make sure you have access
        if (pathQuery != null) {
            final Iterator<Device> iterator = pathQuery.iterator(em);
            //if there are more pages (defined by PAGE_SIZE) you probably want this to be async, also if this is already a job then don't reschedule
            if (iterator instanceof ResultsIterator && ((ResultsIterator) iterator).hasPages() && jobExecution == null) {
                jobScheduler.scheduleQueueJob(notification, true);
                return;
            }
            final CountMinSketch sketch = new CountMinSketch(0.0001,.99,7364181); //add probablistic counter to find dups

            rx.Observable.create(new IteratorObservable<Entity>(iterator)).parallel(new Func1<rx.Observable<Entity>, rx.Observable<Entity>>() {
                @Override
                public rx.Observable<Entity> call(rx.Observable<Entity> deviceObservable) {
                    return deviceObservable.map( new Func1<Entity,Entity>() {
                        @Override
                        public Entity call(Entity entity) {
                            try {
                                List<EntityRef> devicesRef = getDevices(entity); // resolve group
                                String queueName = getJobQueueName(notification);
                                boolean maySchedule = false;
                                for (EntityRef deviceRef : devicesRef) {
                                    long hash = MurmurHash.hash(deviceRef.getUuid());
                                    if(sketch.estimateCount(hash)>0){
                                        LOG.debug("Maybe Found duplicate device: {}", deviceRef.getUuid());
                                        continue;
                                    }else {
                                        sketch.add(hash,1);
                                    }
                                    maySchedule |= deviceCount.incrementAndGet() % BATCH_SIZE == 0;
                                    Message message = new Message();
                                    message.setProperty(MESSAGE_PROPERTY_DEVICE_UUID, deviceRef.getUuid());
                                    qm.postToQueue(queueName, message);
                                    if(notification.getQueued() == null){
                                        // update queued time
                                        notification.setQueued(System.currentTimeMillis());
                                        em.update(notification);
                                    }
                                }

                                //start working if you are on a large batch,
                                if (maySchedule && numCurrentBatchesConfig >= batchCount.incrementAndGet()) {
                                    processBatchAndReschedule(notification, null);
                                }

                                if(devicesRef.size() <= 0){
                                    errorMessages.add("Could not find devices for entity: "+entity.getUuid());
                                }

                            } catch (Exception deviceLoopException) {
                                LOG.error("Failed to add devices", deviceLoopException);
                                errorMessages.add("Failed to add devices for entity: "+entity.getUuid() + " error:"+ deviceLoopException);
                            }
                            return entity;
                        }
                    });
                }
            }, Schedulers.io()).toBlocking().lastOrDefault(null);
        }

        batchCount.set(Math.min(numCurrentBatchesConfig, batchCount.get()));
        // update queued time
        Map<String, Object> properties = new HashMap<String, Object>(2);
        properties.put("queued", notification.getQueued());
        properties.put("state", notification.getState());

        //do i have devices, and have i already started batching.
        if (deviceCount.get()>0 ) {
            if(batchCount.get() <= 0) {
                processBatchAndReschedule(notification, jobExecution);
            }
        } else {
            //if i'm in a test value will be false, do not mark finished for test orchestration, not ideal need real tests
            if(!IS_TEST) {
                finishedBatch(notification, 0, 0);
                errorMessages.add("No devices for notification " + notification.getUuid());
            }
        }

        if(!IS_TEST && errorMessages.size()>0){
            properties.put("deliveryErrors", errorMessages.toArray());
            if(notification.getErrorMessage()==null){
                notification.setErrorMessage("There was a problem delivering all of your notifications. See deliveryErrors in properties");
            }
        }
        notification.addProperties(properties);
        em.update(notification);

        if (LOG.isInfoEnabled()) {
            long elapsed = notification.getQueued() != null ? notification.getQueued() - startTime : 0;
            StringBuilder sb = new StringBuilder();
            sb.append("notification ").append(notification.getUuid());
            sb.append(" done queuing to ").append(deviceCount);
            sb.append(" devices in ").append(elapsed).append(" ms");
            LOG.info(sb.toString());
        }
    }


    private HashMap<Object,Notifier> getNotifierMap(){
        try{
            HashMap<Object,Notifier> map = notifierCacheMap.get(em);
            return map;
        }catch (ExecutionException ee){
            LOG.error("failed to get from cache",ee);
            return new HashMap<Object, Notifier>();
        }
    }
    private void clearNotifierMap(){
        notifierCacheMap.invalidate(em);
    }
    /*
        * returns partial list of Device EntityRefs (up to BATCH_SIZE) - empty when
        * done w/ delivery
        */
    private QueueResults getDeliveryBatch(EntityRef notification, int batchSize) throws Exception {

        QueueQuery qq = new QueueQuery();
        qq.setLimit(batchSize);
        qq.setTimeout(MESSAGE_TRANSACTION_TIMEOUT);
        QueueResults results = qm.getFromQueue(getJobQueueName(notification), qq);
        LOG.debug("got batch of {} devices for notification {}", results.size(), notification.getUuid());
        return results;
    }
    /**
     * executes a Notification batch and schedules the next one - called by
     * NotificationBatchJob
     */
    public void processBatchAndReschedule(Notification notification, JobExecution jobExecution) throws Exception {
        if (!isOkToSend(notification,jobExecution)) {
            LOG.info("notification " + notification.getUuid() + " canceled");
            if (jobExecution != null) {
                jobExecution.killed();
            }
            return;
        }

        LOG.debug("processing batch {}", notification.getUuid());

        QueueResults queueResults = getDeliveryBatch(notification, jobExecution == null ? BATCH_SIZE/2 : BATCH_SIZE ); //if first run then throttle the batch down by factor of 2 if its a job then try to grab alot of notifications and run them all

        long reschedule_delay = jobScheduler.SCHEDULER_GRACE_PERIOD;
        final TaskManager taskManager =  new TaskManager( em, this, qm, notification, queueResults);
        if (queueResults.size() > 0) {
            consecutiveEmptyQueues.set(0);
            sendBatchToProviders(taskManager, notification, queueResults.getMessages());
        }else{
            consecutiveEmptyQueues.incrementAndGet();
        }
        if (qm.hasPendingReads(getJobQueueName(notification), null) && consecutiveEmptyQueues.get() <= CONSECUTIVE_EMPTY_QUEUES) {
            if(jobExecution==null) {
                jobScheduler.scheduleBatchJob(notification, reschedule_delay);
            }else{
                processBatchAndReschedule(notification, jobExecution);
            }
        } else {
            consecutiveEmptyQueues.set(0);
            finishedBatch(notification, 0, 0,true);
        }

        LOG.debug("finished processing batch");
    }

    /**
     * send batches of notifications to provider
     * @param taskManager
     * @param notification
     * @param queueResults
     * @throws Exception
     */
    private void sendBatchToProviders(final TaskManager taskManager, final Notification notification, List<Message> queueResults) throws Exception {

        LOG.info("sending batch of {} devices for Notification: {}", queueResults.size(), notification.getUuid());
        final Map<Object, Notifier> notifierMap = getNotifierMap();
        queueSize.update(queueResults.size());
        final Map<String, Object> payloads = notification.getPayloads();
        final Map<String, Object> translatedPayloads = translatePayloads(payloads, notifierMap);
        try {
            rx.Observable
                    .from(queueResults)
                    .parallel(new Func1<rx.Observable<Message>, rx.Observable<Message>>() {
                        @Override
                        public rx.Observable<Message> call(rx.Observable<Message> messageObservable) {
                            return messageObservable.map(new Func1<Message, Message>() {
                                @Override
                                public Message call(Message message) {
                                    UUID deviceUUID = (UUID) message.getObjectProperty(MESSAGE_PROPERTY_DEVICE_UUID);
                                    boolean foundNotifier = false;
                                    for (Map.Entry<String, Object> entry : payloads.entrySet()) {
                                        try {
                                            String payloadKey = entry.getKey();
                                            Notifier notifier = notifierMap.get(payloadKey.toLowerCase());
                                            EntityRef deviceRef = new SimpleEntityRef(Device.ENTITY_TYPE, deviceUUID);

                                            String providerId;
                                            try {
                                                providerId = getProviderId(deviceRef, notifier);
                                                if (providerId == null) {
                                                    LOG.debug("Provider not found.{} {}", deviceRef,notifier.getName());
                                                    continue;
                                                }
                                            } catch (Exception providerException) {
                                                LOG.error("Exception getting provider.", providerException);
                                                continue;
                                            }
                                            Object payload = translatedPayloads.get(payloadKey);

                                            Receipt receipt = new Receipt(notification.getUuid(), providerId, payload,deviceUUID);
                                            TaskTracker tracker = new TaskTracker(notifier, taskManager, receipt, deviceUUID);
                                            if (payload == null) {
                                                LOG.debug("selected device {} for notification {} doesn't have a valid payload. skipping.", deviceUUID, notification.getUuid());
                                                try {
                                                    tracker.failed(0, "failed to match payload to " + payloadKey + " notifier");
                                                } catch (Exception e) {
                                                    LOG.debug("failed to mark device failed" + e);
                                                }
                                                continue;
                                            }

                                            if (LOG.isDebugEnabled()) {
                                                StringBuilder sb = new StringBuilder();
                                                sb.append("sending notification ").append(notification.getUuid());
                                                sb.append(" to device ").append(deviceUUID);
                                                LOG.debug(sb.toString());
                                            }

                                            try {
                                                ProviderAdapter providerAdapter = providerAdapters.get(notifier.getProvider());
                                                providerAdapter.sendNotification(providerId, notifier, payload, notification, tracker);

                                            } catch (Exception e) {
                                                try {
                                                    tracker.failed(0, e.getMessage());
                                                } catch (Exception trackerException) {
                                                    LOG.error("tracker failed", trackerException);
                                                }
                                            }
                                            foundNotifier = true;
                                        } finally {
                                            sendMeter.mark();
                                        }
                                    }
                                    if (!foundNotifier) {
                                        try {
                                            taskManager.skip(deviceUUID);
                                        } catch (Exception trackerException) {
                                            LOG.error("failed on skip", trackerException);
                                        }
                                    }
                                    return message;
                                }
                            });
                        }
                    }, Schedulers.io())
                    .toBlocking()
                    .lastOrDefault(null);

            //for gcm this will actually send notification
            for (ProviderAdapter providerAdapter : providerAdapters.values()) {
                try {
                    providerAdapter.doneSendingNotifications();
                } catch (Exception e) {
                    LOG.error("providerAdapter.doneSendingNotifications: ", e);
                }
            }

        } finally {
            outstandingQueue.dec();
            LOG.info("finished sending batch for notification {}", notification.getUuid());
        }

    }

    public void finishedBatch(Notification notification, long successes, long failures) throws Exception {
        finishedBatch(notification,successes,failures,false);
    }
    public void finishedBatch(Notification notification, long successes, long failures, boolean overrideComplete) throws Exception {
        if (LOG.isDebugEnabled()) {
            StringBuilder sb = new StringBuilder();
            sb.append("finishedBatch ").append(notification.getUuid());
            sb.append(" successes: ").append(successes);
            sb.append(" failures: ").append(failures);
            LOG.info(sb.toString());
        }

        notification = em.get(notification, Notification.class); // refresh data
        notification.updateStatistics(successes, failures);
        notification.setModified(System.currentTimeMillis());
        Map<String, Object> properties = new HashMap<String, Object>(4);
        properties.put("statistics", notification.getStatistics());
        properties.put("modified", notification.getModified());

        if (isNotificationDeliveryComplete(notification) || overrideComplete) {
            notification.setFinished(notification.getModified());
            properties.put("finished", notification.getModified());
            properties.put("state", notification.getState());
            long elapsed = notification.getFinished()
                    - notification.getStarted();
            long sent = notification.getStatistics().get("sent");
            long errors = notification.getStatistics().get("errors");

            if (LOG.isInfoEnabled()) {
                StringBuilder sb = new StringBuilder();
                sb.append("notification ").append(notification.getUuid());
                sb.append(" done sending to ").append(sent + errors);
                sb.append(" devices in ").append(elapsed).append(" ms");
                LOG.info(sb.toString());
            }

        } else {
            LOG.info("notification finished batch: {}",
                    notification.getUuid());
        }
        em.updateProperties(notification, properties);

        Set<Notifier> notifiers = new HashSet<Notifier>(getNotifierMap().values()); // remove dups
        asyncCheckForInactiveDevices(notifiers);
    }
    /**
     * Call the adapter with the notifier
     */
    private Map<String, Object> translatePayloads(Map<String, Object> payloads, Map<Object, Notifier> notifierMap) throws Exception {
        Map<String, Object> translatedPayloads = new HashMap<String, Object>(
                payloads.size());
        for (Map.Entry<String, Object> entry : payloads.entrySet()) {
            String payloadKey = entry.getKey();
            Object payloadValue = entry.getValue();
            Notifier notifier = notifierMap.get(payloadKey);
            if(notifier==null){
                clearNotifierMap();
                notifierMap = getNotifierMap();
                notifier = notifierMap.get(payloadKey);
            }
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

    private int getNumConcurrentBatches() {
        return Integer.parseInt(props.getProperty(NOTIFICATION_CONCURRENT_BATCHES, "1"));
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

    private boolean isOkToSend(Notification notification,
                               JobExecution jobExecution) {
        String autoExpireAfterString = props.getProperty(PUSH_AUTO_EXPIRE_AFTER_PROPNAME);

        if (autoExpireAfterString != null) {
            pushAutoExpireAfter = Long.parseLong(autoExpireAfterString);
        }
        if (pushAutoExpireAfter != null) {
            if (notification.getCreated() < System.currentTimeMillis() - pushAutoExpireAfter) {
                notification.setExpire(System.currentTimeMillis() - 1L);
            }
        }

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
        if (jobExecution != null
                && notification.getDeliver() != null
                && !notification.getDeliver().equals(jobExecution.getJobData().getProperty("deliver"))
                ) {
            LOG.info("notification {} was rescheduled. not sending.",
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


    private boolean isNotificationDeliveryComplete(Notification notification) throws Exception {
        if(notification.getQueued() == null){
            return false;
        }
        String queuePath = getJobQueueName(notification);
        return !qm.hasPendingReads(queuePath, null)
                && !qm.hasOutstandingTransactions(queuePath, null)
                && !qm.hasMessagesInQueue(queuePath, null);
    }

    private String getJobQueueName(EntityRef entityRef) {
        return utils.pluralize(entityRef.getType()) + "/" + entityRef.getUuid();
    }
}
