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


import java.util.*;

import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.services.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.mq.Message;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityManagerFactory;
import org.apache.usergrid.persistence.EntityRef;
import org.apache.usergrid.persistence.SimpleEntityRef;
import org.apache.usergrid.persistence.core.metrics.MetricsFactory;
import org.apache.usergrid.persistence.entities.Notification;
import org.apache.usergrid.persistence.entities.Notifier;
import org.apache.usergrid.persistence.entities.Receipt;
import org.apache.usergrid.persistence.exceptions.RequiredPropertyNotFoundException;
import org.apache.usergrid.persistence.index.query.Identifier;
import org.apache.usergrid.persistence.Query;
import org.apache.usergrid.persistence.queue.QueueManager;
import org.apache.usergrid.persistence.queue.QueueManagerFactory;
import org.apache.usergrid.persistence.queue.QueueScope;
import org.apache.usergrid.persistence.queue.impl.QueueScopeImpl;
import org.apache.usergrid.services.exceptions.ForbiddenServiceOperationException;
import org.apache.usergrid.services.notifications.impl.ApplicationQueueManagerImpl;

import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import com.google.inject.Injector;

import rx.Observable;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import static org.apache.usergrid.utils.InflectionUtils.pluralize;

public class NotificationsService extends AbstractCollectionService {


    private MetricsFactory metricsService;
    private Meter postMeter;
    private Timer postTimer;

    private static final int PAGE = 100;
    private static final Logger logger = LoggerFactory.getLogger(NotificationsService.class);
    //need a mocking framework, this is to substitute for no mocking

    static final String MESSAGE_PROPERTY_DEVICE_UUID = "deviceUUID";

    static {
        Message.MESSAGE_PROPERTIES.put(
                MESSAGE_PROPERTY_DEVICE_UUID, UUID.class);
    }

//not really a queue manager at all
    private ApplicationQueueManager notificationQueueManager;
    private long gracePeriod;
    private ServiceManagerFactory smf;
    private EntityManagerFactory emf;
    private QueueManagerFactory queueManagerFactory;
    private ApplicationQueueManagerCache applicationQueueManagerCache;

    public NotificationsService() {
        if (logger.isTraceEnabled()) {
            logger.trace("/notifications");
        }
    }

    @Override
    public void init( ServiceInfo info ) {
        super.init(info);
        smf = getApplicationContext().getBean(ServiceManagerFactory.class);
        emf = getApplicationContext().getBean(EntityManagerFactory.class);

        Properties props = (Properties)getApplicationContext().getBean("properties");
        metricsService = getApplicationContext().getBean(Injector.class).getInstance(MetricsFactory.class);
        postMeter = metricsService.getMeter(NotificationsService.class, "collection.post_requests");
        postTimer = metricsService.getTimer(this.getClass(), "collection.post_requests");
        JobScheduler jobScheduler = new JobScheduler(sm,em);
        String name = ApplicationQueueManagerImpl.getQueueNames( props );
        QueueScope queueScope = new QueueScopeImpl( name, QueueScope.RegionImplementation.LOCAL);
        queueManagerFactory = getApplicationContext().getBean( Injector.class ).getInstance(QueueManagerFactory.class);
        QueueManager queueManager = queueManagerFactory.getQueueManager(queueScope);
        applicationQueueManagerCache = getApplicationContext().getBean(Injector.class).getInstance(ApplicationQueueManagerCache.class);
        notificationQueueManager = applicationQueueManagerCache
            .getApplicationQueueManager(em,queueManager, jobScheduler, metricsService ,props);

        gracePeriod = JobScheduler.SCHEDULER_GRACE_PERIOD;
    }

    public ApplicationQueueManager getQueueManager(){
        return notificationQueueManager;
    }


    @Override
    public ServiceContext getContext(ServiceAction action,
            ServiceRequest request, ServiceResults previousResults,
            ServicePayload payload) throws Exception {

        ServiceContext context = super.getContext(action, request, previousResults, payload);

        if (action == ServiceAction.POST) {
            context.setQuery(null); // we don't use this, and it must be null to
                                    // force the correct execution path
        }
        return context;
    }

    @Override
    public ServiceResults postCollection(ServiceContext context) throws Exception {
        if (logger.isTraceEnabled()) {
            logger.trace("NotificationService: start request.");
        }
        Timer.Context timer = postTimer.time();
        postMeter.mark();
        try {

            validate(null, context.getPayload());

            // perform some input validates on useGraph payload property vs. ql= path query
            final List<ServiceParameter> parameters = context.getRequest().getOriginalParameters();
            for (ServiceParameter parameter : parameters){
                if( parameter instanceof ServiceParameter.QueryParameter
                    && context.getProperties().get("useGraph") != null
                      && context.getProperties().get("useGraph").equals(true)){

                    throw new IllegalArgumentException("Query ql parameter cannot be used with useGraph:true property value");
                }
            }

            Notification.PathTokens pathTokens = getPathTokens(parameters);

            // set defaults
            context.getProperties().put("filters", context.getProperties().getOrDefault("filters", new HashMap<>()));
            context.getProperties().put("useGraph", context.getProperties().getOrDefault("useGraph", false));
            context.getProperties().put("saveReceipts", context.getProperties().getOrDefault("saveReceipts", true));
            context.getProperties().put("processingFinished", 0L); // defaulting processing finished to 0
            context.getProperties().put("deviceProcessedCount", 0); // defaulting processing finished to 0
            context.getProperties().put("state", Notification.State.CREATED);
            context.getProperties().put("pathQuery", pathTokens);
            context.setOwner(sm.getApplication());
            ServiceResults results = super.postCollection(context);
            Notification notification = (Notification) results.getEntity();

            // update Notification properties
            if (notification.getStarted() == null || notification.getStarted() == 0) {
                long now = System.currentTimeMillis();
                notification.setStarted(System.currentTimeMillis());
                Map<String, Object> properties = new HashMap<String, Object>(2);
                properties.put("started", notification.getStarted());
                properties.put("state", notification.getState());
                notification.addProperties(properties);
                if (logger.isTraceEnabled()) {
                    logger.trace("ApplicationQueueMessage: notification {} properties updated in duration {} ms", notification.getUuid(), System.currentTimeMillis() - now);
                }
            }

            long now = System.currentTimeMillis();
            notificationQueueManager.queueNotification(notification, null);
            if (logger.isTraceEnabled()) {
                logger.trace("NotificationService: notification {} post queue duration {} ms ", notification.getUuid(), System.currentTimeMillis() - now);
            }
            // future: somehow return 202?
            return results;
        }catch (Exception e){
            logger.error(e.getMessage());
            throw e;
        }finally {
            timer.stop();
        }
    }

    private Notification.PathTokens getPathTokens(List<ServiceParameter> parameters){

        Notification.PathTokens pathTokens = new Notification.PathTokens();
        pathTokens.setApplicationRef((SimpleEntityRef)em.getApplicationRef());

        // first parameter is always collection name, start parsing after that
        for (int i = 0; i < parameters.size() - 1; i += 2 ) {
            String collection = pluralize(parameters.get(i).getName());
            Identifier identifier = null;
            String ql = null;
            ServiceParameter sp = parameters.get(i + 1);

            // if the next param is a query, add a token with the query
            if(sp.isQuery()){
                ql = sp.getQuery().getQl().get();
                pathTokens.getPathTokens().add(new Notification.PathToken( collection, ql));
            }else{
                // if the next param is "notifications", it's the end let identifier be null
                if(sp.isName() && !sp.getName().equalsIgnoreCase("notifications") || sp.isId()){
                    identifier = sp.getIdentifier();
                }
                pathTokens.getPathTokens().add(new Notification.PathToken( collection, identifier));
            }


        }
        return pathTokens;
    }

    @Override
    public ServiceResults postItemsByQuery(ServiceContext context, Query query) throws Exception {
        return postCollection(context);
    }

    @Override
    public Entity updateEntity(ServiceRequest request, EntityRef ref,
            ServicePayload payload) throws Exception {

        validate(ref, payload);

        Notification notification = em.get(ref, Notification.class);

        if ("restart".equals(payload.getProperty("restart"))) { // for emergency
                                                                // use only
            payload.getProperties().clear();
            payload.setProperty("restart", "");
            payload.setProperty("errorMessage", "");
            payload.setProperty("deliver", System.currentTimeMillis() + gracePeriod);

            // once finished, immutable
        } else if (notification.getFinished() != null) {
            throw new ForbiddenServiceOperationException(request,
                    "Notification immutable once sent.");

            // once started, only cancel is allowed
        } else if (notification.getStarted() != null) {
            if (payload.getProperty("canceled") != Boolean.TRUE) {
                throw new ForbiddenServiceOperationException(request,
                        "Notification has started. You may only set canceled.");
            }
            payload.getProperties().clear();
            payload.setProperty("canceled", Boolean.TRUE);
        }

        Entity response = super.updateEntity(request, ref, payload);

        Long deliver = (Long) payload.getProperty("deliver");
        if (deliver != null) {
            if (!deliver.equals(notification.getDeliver())) {
                notificationQueueManager.queueNotification((Notification) response, null);
            }
        }
        return response;
    }

    @Override
    protected boolean isDeleteAllowed(ServiceContext context, Entity entity) {
        Notification notification = (Notification) entity;
        Notification.State state = notification.getState();
        return !(state.equals(Notification.State.CREATED) || state.equals(Notification.State.STARTED) ||
            state.equals(Notification.State.SCHEDULED));
    }

    // validate payloads
    private void validate(EntityRef ref, ServicePayload servicePayload)
            throws Exception {
        Object obj_payloads = servicePayload.getProperty("payloads");
        if (obj_payloads == null && ref == null) {
            throw new RequiredPropertyNotFoundException("notification",
                    "payloads");
        }
        if (obj_payloads != null) {
            if (!(obj_payloads instanceof Map)) {
                throw new IllegalArgumentException(
                        "payloads must be a JSON Map");
            }
            final Map<String, Object> payloads = (Map<String, Object>) obj_payloads;
            final Map<Object, Notifier> notifierMap = getNotifierMap(payloads);
            Observable t = Observable.from(payloads.entrySet()).subscribeOn(Schedulers.io()).map(new Func1<Map.Entry<String, Object>, Object>() {
                @Override
                public Object call(Map.Entry<String, Object> entry) {
                    String notifierId = entry.getKey();
                    Notifier notifier = notifierMap.get(notifierId);
                    if (notifier == null) {
                        throw new IllegalArgumentException("notifier \""
                                + notifierId + "\" not found");
                    }
                    ProviderAdapter providerAdapter = ProviderAdapterFactory.getProviderAdapter(notifier, em);
                    Object payload = entry.getValue();
                    try {
                        return providerAdapter.translatePayload(payload); // validate
                        // specifically to
                        // provider
                    } catch (Exception e) {
                        return e;
                    }
                }
            });
            Object e = t.toBlocking().lastOrDefault(null);
            if(e instanceof Throwable){
                throw new Exception((Exception)e);
            }

        }
    }



    public String getJobQueueName(EntityRef notification) {
        return "notifications/" + notification.getUuid();
    }



    /* adds a single device for delivery - used only by tests */
    public void addDevice(EntityRef notification, EntityRef device) throws Exception {

        String jobQueueName = getJobQueueName(notification);
        Message message = new Message();
        message.setObjectProperty(MESSAGE_PROPERTY_DEVICE_UUID,
                device.getUuid());
        sm.getQueueManager().postToQueue(jobQueueName, message);
    }

    public Notification getSourceNotification(EntityRef receipt)
            throws Exception {
        Receipt r = em.get(receipt.getUuid(), Receipt.class);
        return em.get(r.getNotificationUUID(), Notification.class);
    }


    /* create a map of Notifier UUIDs and/or names to Notifiers */
    protected Map<Object, Notifier> getNotifierMap(Map payloads)
            throws Exception {
        Map<Object, Notifier> notifiers = new HashMap<Object, Notifier>(
                payloads.size() * 2);
        for (Object id : payloads.keySet()) {
            Identifier identifier = Identifier.from(id);
            Notifier notifier;
            if (identifier.isUUID()) {
                notifier = em.get(identifier.getUUID(), Notifier.class);
            } else {
                EntityRef ref = em.getAlias("notifier", identifier.getName());
                notifier = em.get(ref, Notifier.class);
            }
            if (notifier != null) {
                notifiers.put(notifier.getUuid(), notifier);
                notifiers.put(notifier.getUuid().toString(), notifier);
                if (notifier.getName() != null) {
                    notifiers.put(notifier.getName(), notifier);
                }
            }
        }
        return notifiers;
    }


    /**
     * attempts to test the providers connections - throws an Exception on
     * failure
     */
    public void testConnection(Notifier notifier) throws Exception {
        ProviderAdapter providerAdapter = ProviderAdapterFactory.getProviderAdapter(notifier,em);
        if (providerAdapter != null) {
            providerAdapter.testConnection();
        }
    }


    public ServiceManagerFactory getServiceManagerFactory(){
        return this.smf;
    }
    public EntityManagerFactory getEntityManagerFactory(){
        return this.emf;
    }


    public MetricsFactory getMetricsFactory() {
        return metricsService;
    }
}
