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
package org.apache.usergrid.services.notifications.apns;

import com.google.common.cache.*;

import com.relayrides.pushy.apns.*;
import com.relayrides.pushy.apns.util.*;

import org.apache.usergrid.persistence.entities.Notification;
import org.apache.usergrid.persistence.entities.Notifier;
import org.mortbay.util.ajax.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.exceptions.RequiredPropertyNotFoundException;
import org.apache.usergrid.services.ServicePayload;
import org.apache.usergrid.services.notifications.ConnectionException;
import org.apache.usergrid.services.notifications.ProviderAdapter;
import org.apache.usergrid.services.notifications.TaskTracker;

import javax.net.ssl.SSLContext;

/**
 * Adapter for Apple push notifications
 */
public class APNsAdapter implements ProviderAdapter {

    private static final Logger logger = LoggerFactory
            .getLogger(APNsAdapter.class);

    public static int MAX_CONNECTION_POOL_SIZE = 15;
    private static final Set<String> validEnvironments = new HashSet<String>();
    private static final String TEST_TOKEN = "ff026b5a4d2761ef13843e8bcab9fc83b47f1dfbd1d977d225ab296153ce06d6";
    private static final String TEST_PAYLOAD = "{}";

    static {
        validEnvironments.add("development");
        validEnvironments.add("production");
        validEnvironments.add("mock");
    }

    public APNsAdapter(){}

    @Override
    public void testConnection(Notifier notifier) throws ConnectionException {
        if(isMock(notifier)){
            delayRandom(notifier); return;
        }
        TestAPNsNotification notification =  TestAPNsNotification.create(TEST_TOKEN, TEST_PAYLOAD);
        try {
            CountDownLatch latch = new CountDownLatch(1);
            notification.setLatch(latch);
                PushManager<SimpleApnsPushNotification> pushManager = getPushManager(notifier);
                addToQueue(pushManager, notification);
                latch.await(10000,TimeUnit.MILLISECONDS);
                if(notification.hasFailed()){
                    // this is expected with a bad certificate (w/message: comes from failedconnectionlistener
                    throw new ConnectionException("Bad certificate. Double-check your environment.",notification.getCause() != null ? notification.getCause() : new Exception("Bad certificate."));
                }
                notification.finished();
            } catch (Exception e) {
                notification.finished();

                if (e instanceof ConnectionException) {
                throw (ConnectionException) e;
            }
            if (e instanceof InterruptedException) {
                throw new ConnectionException("Test notification timed out", e);
            }
            logger.warn("testConnection got non-fatal error", e.getCause());
        }
    }

    private BlockingQueue<SimpleApnsPushNotification> addToQueue(PushManager<SimpleApnsPushNotification> pushManager, SimpleApnsPushNotification notification) throws InterruptedException {
        BlockingQueue<SimpleApnsPushNotification> queue = pushManager.getQueue();
        queue.offer(notification,2500,TimeUnit.MILLISECONDS);
        return queue;
    }

    @Override
    public void sendNotification(String providerId, Notifier notifier,
            Object payload, Notification notification, TaskTracker tracker)
            throws Exception {
        APNsNotification apnsNotification = APNsNotification.create(providerId, payload.toString(), notification, tracker);
        PushManager<SimpleApnsPushNotification> pushManager = getPushManager(notifier);
        try {
            addToQueue(pushManager, apnsNotification);
            apnsNotification.messageSent();
        }catch (InterruptedException ie){
            apnsNotification.messageSendFailed(ie);
            throw ie;
        }
    }

    @Override
    public void doneSendingNotifications() throws Exception {
        // do nothing - no batching
    }

    @Override
    public Map<String, Date> getInactiveDevices(Notifier notifier,
            EntityManager em) throws Exception {
        Map<String,Date> map = new HashMap<String,Date>();
        if(isMock(notifier)){
            return map;
        }
        PushManager<SimpleApnsPushNotification> pushManager = getPushManager(notifier);

        List<ExpiredToken> tokens = null;
        try {
            tokens = pushManager.getExpiredTokens();
        }catch (FeedbackConnectionException fce){
            logger.debug("Failed to get tokens",fce);
            return map;
        }
        for(ExpiredToken token : tokens){
            String expiredToken = new String(token.getToken());
            map.put(expiredToken, token.getExpiration());
        }
        return map;
    }

    private PushManager<SimpleApnsPushNotification> getPushManager(Notifier notifier) throws ExecutionException {
        PushManager<SimpleApnsPushNotification> pushManager = apnsServiceMap.get(notifier);
        if(pushManager != null &&  !pushManager.isStarted() && pushManager.isShutDown()){
            try{
                pushManager = createApnsService(notifier);
            }catch(Exception e){
                logger.error("could not instantiate push manager.");
                throw new ExecutionException(e);
            }
            apnsServiceMap.put(notifier,pushManager);
        }
        try {
            if (!pushManager.isStarted()) { //ensure manager is started
                pushManager.start();
            }
        }catch(IllegalStateException ise){
            logger.debug("failed to start",ise);//could have failed because its started
        }
        return pushManager;
    }

    //cache to retrieve push manager, cached per notifier, so many notifications will get same push manager
    private static LoadingCache<Notifier, PushManager<SimpleApnsPushNotification>> apnsServiceMap = CacheBuilder
            .newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .removalListener(new RemovalListener<Notifier, PushManager<SimpleApnsPushNotification>>() {
                @Override
                public void onRemoval(
                        RemovalNotification<Notifier, PushManager<SimpleApnsPushNotification>> notification) {
                    try {
                        PushManager<SimpleApnsPushNotification> manager = notification.getValue();
                        if (!manager.isShutDown()) {
                            List<SimpleApnsPushNotification> notifications = manager.shutdown(3000);
                            for (SimpleApnsPushNotification notification1 : notifications) {
                                try {
                                    ((APNsNotification) notification1).messageSendFailed(new Exception("Cache Expired: Shutting down sender"));
                                }catch (Exception e){
                                    logger.error("Failed to mark notification",e);
                                }
                            }
                        }
                    } catch (Exception ie) {
                        logger.error("Failed to shutdown from cache", ie);
                    }
                }
            }).build(new CacheLoader<Notifier, PushManager<SimpleApnsPushNotification>>() {
                @Override
                public PushManager<SimpleApnsPushNotification> load(Notifier notifier) {
                    try {
                        return createApnsService(notifier);
                    } catch (KeyStoreException ke) {
                        logger.error("Could not instantiate pushmanager", ke);
                        return null;
                    }
                }
            });


    protected static PushManager<SimpleApnsPushNotification> createApnsService(Notifier notifier) throws KeyStoreException{
        LinkedBlockingQueue<SimpleApnsPushNotification> queue = new LinkedBlockingQueue<SimpleApnsPushNotification>();
        PushManagerConfiguration config = new PushManagerConfiguration();
        config.setConcurrentConnectionCount(Runtime.getRuntime().availableProcessors() * 2);
        PushManager<SimpleApnsPushNotification> pushManager =  new PushManager<SimpleApnsPushNotification>(getApnsEnvironment(notifier), getSSLContext(notifier), null, null, queue, config);
        //only tested when a message is sent
        pushManager.registerRejectedNotificationListener(new RejectedAPNsListener());
        //this will get tested when start is called
        pushManager.registerFailedConnectionListener(new FailedConnectionListener());
        return pushManager;
    }

    @Override
    public Object translatePayload(Object objPayload) throws Exception {
        String payload;
        if (objPayload instanceof String) {
            payload = (String) objPayload;
            if (!payload.startsWith("{")) {
                payload = "{\"aps\":{\"alert\":\"" + payload + "\"}}";
            }
        } else {
            payload = JSON.toString(objPayload);
        }
        if (payload.length() > 2048) {
            throw new IllegalArgumentException(
                    "Apple APNs payloads must be 2048 characters or less");
        }
        return payload;
    }


    @Override
    public void validateCreateNotifier(ServicePayload payload) throws Exception {
        String environment = payload.getStringProperty("environment");
        if (!validEnvironments.contains(environment)) {
            throw new IllegalArgumentException("environment must be one of: "
                    + Arrays.toString(validEnvironments.toArray()));
        }

        if (payload.getProperty("p12Certificate") == null) {
            throw new RequiredPropertyNotFoundException("notifier",
                    "p12Certificate");
        }
    }
    public boolean isMock(Notifier notifier){
        return notifier.getEnvironment() !=null ? notifier.getEnvironment().equals("mock") : false ;
    }
    public boolean delayRandom(Notifier notifier) {
        boolean wasDelayed = false;
        if (isMock(notifier)) {
            try {
                Thread.sleep(
                        new Random().nextInt(300)
                );
                wasDelayed = true;
            } catch (InterruptedException ie) {
                //delay was stopped
            }
        }
        return wasDelayed;
    }

    private static ApnsEnvironment getApnsEnvironment(Notifier notifier){
        return  notifier.isProduction()
                ? ApnsEnvironment.getProductionEnvironment()
                : ApnsEnvironment.getSandboxEnvironment();
    }


    private static SSLContext getSSLContext(Notifier notifier) {
        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            String password = notifier.getCertificatePassword();
            char[] passChars =(password != null ? password : "").toCharArray();
            InputStream stream = notifier.getP12CertificateStream();
            keyStore.load(stream,passChars);
            SSLContext context =  SSLContextUtil.createDefaultSSLContext(keyStore, passChars);
            return context;
        }catch (Exception e){
            throw new RuntimeException("Error getting certificate",e);
        }
    }
}
