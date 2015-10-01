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

import com.clearspring.analytics.hash.MurmurHash;
import com.clearspring.analytics.stream.frequency.CountMinSketch;
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
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;


public class ApplicationQueueManagerImpl implements ApplicationQueueManager {

    private static final Logger LOG = LoggerFactory.getLogger(ApplicationQueueManagerImpl.class);

    //this is for tests, will not mark initial post complete, set to false for tests

    private final EntityManager em;
    private final QueueManager qm;
    private final JobScheduler jobScheduler;
    private final MetricsFactory metricsFactory;
    private final String queueName;
    private final Meter queueMeter;
    private final Meter sendMeter;

    HashMap<Object, ProviderAdapter> notifierHashMap; // only retrieve notifiers once


    public ApplicationQueueManagerImpl(JobScheduler jobScheduler, EntityManager entityManager, QueueManager queueManager, MetricsFactory metricsFactory, Properties properties){
        this.em = entityManager;
        this.qm = queueManager;
        this.jobScheduler = jobScheduler;
        this.metricsFactory = metricsFactory;
        this.queueName = getQueueNames(properties);
        queueMeter = metricsFactory.getMeter(ApplicationQueueManagerImpl.class, "notification.queue");
        sendMeter = metricsFactory.getMeter(NotificationsService.class, "queue.send");

    }

    private boolean scheduleQueueJob(Notification notification) throws Exception{
        return jobScheduler.scheduleQueueJob(notification);
    }

    @Override
    public void queueNotification(final Notification notification, final JobExecution jobExecution) throws Exception {
        if(scheduleQueueJob(notification)){
            em.update(notification);
            return;
        }

        long startTime = System.currentTimeMillis();

        if (notification.getCanceled() == Boolean.TRUE) {
            LOG.info("notification " + notification.getUuid() + " canceled");
            if (jobExecution != null) {
                jobExecution.killed();
            }
            return;
        }

        LOG.info("notification {} start queuing", notification.getUuid());

        final PathQuery<Device> pathQuery = notification.getPathTokens().getPathQuery() ; //devices query
        final AtomicInteger deviceCount = new AtomicInteger(); //count devices so you can make a judgement on batching
        final ConcurrentLinkedQueue<String> errorMessages = new ConcurrentLinkedQueue<String>(); //build up list of issues


        //get devices in querystring, and make sure you have access
        if (pathQuery != null) {
            final HashMap<Object,ProviderAdapter> notifierMap =  getAdapterMap();
            LOG.info("notification {} start query", notification.getUuid());
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

            final Func1<Entity,Entity> entityListFunct = entity -> {

                try {

                    long now = System.currentTimeMillis();
                    List<EntityRef> devicesRef = getDevices(entity); // resolve group

                    LOG.info("notification {} queue  {} devices, duration "+(System.currentTimeMillis()-now)+" ms", notification.getUuid(), devicesRef.size());

                    for (EntityRef deviceRef : devicesRef) {
                        LOG.info("notification {} starting to queue device {} ", notification.getUuid(), deviceRef.getUuid());
                        long hash = MurmurHash.hash(deviceRef.getUuid());
                        if (sketch.estimateCount(hash) > 0) { //look for duplicates
                            LOG.warn("Maybe Found duplicate device: {}", deviceRef.getUuid());
                            continue;
                        } else {
                            sketch.add(hash, 1);
                        }
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
                            LOG.info("Provider query for notification {} device {} took "+(System.currentTimeMillis()-now)+" ms",notification.getUuid(),deviceRef.getUuid());
                        }

                        if (notifierId == null) {
                            LOG.info("Notifier did not match for device {} ", deviceRef);
                            continue;
                        }

                        ApplicationQueueMessage message = new ApplicationQueueMessage(appId, notification.getUuid(), deviceRef.getUuid(), notifierKey, notifierId);
                        if (notification.getQueued() == null) {
                            // update queued time
                            now = System.currentTimeMillis();
                            notification.setQueued(System.currentTimeMillis());
                            LOG.info("notification {} device {} queue time set. duration "+(System.currentTimeMillis()-now)+" ms", notification.getUuid(), deviceRef.getUuid());
                        }
                        now = System.currentTimeMillis();
                        qm.sendMessage(message);
                        LOG.info("notification {} post-queue to device {} duration " + (System.currentTimeMillis() - now) + " ms "+queueName+" queue", notification.getUuid(), deviceRef.getUuid());
                        deviceCount.incrementAndGet();
                        queueMeter.mark();
                    }
                } catch (Exception deviceLoopException) {
                    LOG.error("Failed to add devices", deviceLoopException);
                    errorMessages.add("Failed to add devices for entity: " + entity.getUuid() + " error:" + deviceLoopException);
                }
                return entity;
            };

            long now = System.currentTimeMillis();


            //process up to 10 concurrently
            Observable o = rx.Observable.create( new IteratorObservable<Entity>( iterator ) )

                                        .flatMap(entity -> Observable.just(entity).map(entityListFunct)
                                            .doOnError(throwable -> {
                                                LOG.error("Failed while writing",
                                                    throwable);
                                            })
                                            , 10);

            o.toBlocking().lastOrDefault( null );
            LOG.info( "notification {} done queueing duration {} ms", notification.getUuid(), System.currentTimeMillis() - now);
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


        LOG.info("notification {} updated notification duration {} ms", notification.getUuid(), System.currentTimeMillis() - now);

        //do i have devices, and have i already started batching.
        if (deviceCount.get() <= 0 || !notification.getDebug()) {
            TaskManager taskManager = new TaskManager(em, notification);
            //if i'm in a test value will be false, do not mark finished for test orchestration, not ideal need real tests
            taskManager.finishedBatch(false,true);
        }else {
            em.update(notification);
        }

        long elapsed = notification.getQueued() != null ? notification.getQueued() - startTime : 0;
        LOG.info("notification {} done queuing to {} devices in " + elapsed + " ms", notification.getUuid().toString(), deviceCount.get());
    }

    /**
     * only need to get notifiers once. will reset on next batch
     * @return
     */
    private HashMap<Object,ProviderAdapter> getAdapterMap(){
        if(notifierHashMap == null) {
            long now = System.currentTimeMillis();
            notifierHashMap = new HashMap<Object, ProviderAdapter>();
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
                ProviderAdapter providerAdapter = ProviderAdapterFactory.getProviderAdapter(notifier,em);
                notifierHashMap.put(name.toLowerCase(), providerAdapter);
                notifierHashMap.put(uuid, providerAdapter);
                notifierHashMap.put(uuid.toString(), providerAdapter);
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
    @Override
    public Observable sendBatchToProviders(final List<QueueMessage> messages, final String queuePath) {
        LOG.info("sending batch of {} notifications.", messages.size());

        final Map<Object, ProviderAdapter> notifierMap = getAdapterMap();
        final ApplicationQueueManagerImpl proxy = this;
        final ConcurrentHashMap<UUID,TaskManager> taskMap = new ConcurrentHashMap<UUID, TaskManager>(messages.size());
        final ConcurrentHashMap<UUID,Notification> notificationMap = new ConcurrentHashMap<UUID, Notification>(messages.size());

        final Func1<QueueMessage, ApplicationQueueMessage> func = new Func1<QueueMessage, ApplicationQueueMessage>() {
            @Override
            public ApplicationQueueMessage call(QueueMessage queueMessage) {
                boolean messageCommitted = false;
                ApplicationQueueMessage message = null;
                try {
                    message = (ApplicationQueueMessage) queueMessage.getBody();
                    LOG.info("start sending notification for device {} for Notification: {} on thread "+Thread.currentThread().getId(), message.getDeviceId(), message.getNotificationId());

                    UUID deviceUUID = message.getDeviceId();

                    Notification notification = notificationMap.get(message.getNotificationId());
                    if (notification == null) {
                        notification = em.get(message.getNotificationId(), Notification.class);
                        notificationMap.put(message.getNotificationId(), notification);
                    }
                    TaskManager taskManager = taskMap.get(message.getNotificationId());
                    if (taskManager == null) {
                        taskManager = new TaskManager(em, notification);
                        taskMap.putIfAbsent(message.getNotificationId(), taskManager);
                        taskManager = taskMap.get(message.getNotificationId());
                    }

                    final Map<String, Object> payloads = notification.getPayloads();
                    final Map<String, Object> translatedPayloads = translatePayloads(payloads, notifierMap);
                    LOG.info("sending notification for device {} for Notification: {}", deviceUUID, notification.getUuid());

                    try {
                        String notifierName = message.getNotifierKey().toLowerCase();
                        ProviderAdapter providerAdapter = notifierMap.get(notifierName.toLowerCase());
                        Object payload = translatedPayloads.get(notifierName);
                        Receipt receipt = new Receipt(notification.getUuid(), message.getNotifierId(), payload, deviceUUID);
                        TaskTracker tracker = new TaskTracker(providerAdapter.getNotifier(), taskManager, receipt, deviceUUID);
                        if(!isOkToSend(notification)){
                             tracker.failed(0, "Notification is duplicate/expired/cancelled.");
                        }else {
                            if (payload == null) {
                                LOG.debug("selected device {} for notification {} doesn't have a valid payload. skipping.", deviceUUID, notification.getUuid());
                                tracker.failed(0, "failed to match payload to " + message.getNotifierId() + " notifier");
                            } else {
                                long now = System.currentTimeMillis();
                                try {
                                    providerAdapter.sendNotification(message.getNotifierId(), payload, notification, tracker);
                                } catch (Exception e) {
                                    tracker.failed(0, e.getMessage());
                                } finally {
                                    LOG.info("sending to device {} for Notification: {} duration " + (System.currentTimeMillis() - now) + " ms", deviceUUID, notification.getUuid());
                                }
                            }
                        }
                        messageCommitted = true;
                    } finally {
                        sendMeter.mark();
                    }

                } catch (Exception e) {
                    LOG.error("Failure while sending",e);
                    try {
                        if(!messageCommitted && queuePath != null) {
                            qm.commitMessage(queueMessage);
                        }
                    }catch (Exception queueException){
                        LOG.error("Failed to commit message.",queueException);
                    }
                }
                return message;
            }
        };

        //from each queue message, process them in parallel up to 10 at a time
        Observable o = rx.Observable.from( messages ).flatMap( queueMessage -> {


            return Observable.just( queueMessage ).map( func ).buffer( messages.size() ).map( queueMessages -> {
                //for gcm this will actually send notification
                for ( ProviderAdapter providerAdapter : notifierMap.values() ) {
                    try {
                        providerAdapter.doneSendingNotifications();
                    }
                    catch ( Exception e ) {
                        LOG.error( "providerAdapter.doneSendingNotifications: ", e );
                    }
                }
                //TODO: check if a notification is done and mark it
                HashMap<UUID, ApplicationQueueMessage> notifications = new HashMap<>();
                for ( ApplicationQueueMessage message : queueMessages ) {
                    if ( notifications.get( message.getNotificationId() ) == null ) {
                        try {
                            TaskManager taskManager = taskMap.get( message.getNotificationId() );
                            notifications.put( message.getNotificationId(), message );
                            taskManager.finishedBatch();
                        }
                        catch ( Exception e ) {
                            LOG.error( "Failed to finish batch", e );
                        }
                    }
                }
                return notifications;
            } ).doOnError( throwable -> LOG.error( "Failed while sending", throwable ) );
        }, 10 );

        return o;
    }

    @Override
    public void stop(){
        for(ProviderAdapter adapter : getAdapterMap().values()){
            try {
                adapter.stop();
            }catch (Exception e){
                LOG.error("failed to stop adapter",e);
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
        Map<String, Object> translatedPayloads = new HashMap<String, Object>(  payloads.size());
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

    @Override
    public void asyncCheckForInactiveDevices() throws Exception {
        Collection<ProviderAdapter> providerAdapters = getAdapterMap().values();
        for (final ProviderAdapter providerAdapter : providerAdapters) {
            try {
                if (providerAdapter != null) {
                    LOG.debug("checking notifier {} for inactive devices", providerAdapter.getNotifier());
                    providerAdapter.removeInactiveDevices();

                    LOG.debug("finished checking notifier {} for inactive devices",providerAdapter.getNotifier());
                }
            } catch (Exception e) {
                LOG.error("checkForInactiveDevices", e); // not
                // essential so
                // don't fail,
                // but log
            }
        }
    }


    private boolean isOkToSend(Notification notification) {
        Map<String,Long> stats = notification.getStatistics();
        if (stats != null && notification.getExpectedCount() == (stats.get("sent")+ stats.get("errors"))) {
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
