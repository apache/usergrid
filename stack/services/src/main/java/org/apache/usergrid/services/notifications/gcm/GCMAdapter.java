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
package org.apache.usergrid.services.notifications.gcm;

import com.google.android.gcm.server.*;
import org.apache.usergrid.persistence.Notification;
import org.apache.usergrid.persistence.Notifier;
import org.mortbay.util.ajax.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.services.ServicePayload;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.exceptions.RequiredPropertyNotFoundException;

import org.apache.usergrid.services.notifications.ConnectionException;
import org.apache.usergrid.services.notifications.ProviderAdapter;
import org.apache.usergrid.services.notifications.TaskTracker;

import java.io.IOException;
import java.util.*;

public class GCMAdapter implements ProviderAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(GCMAdapter.class);
    private static final int SEND_RETRIES = 3;
    private static int BATCH_SIZE = 1000;

    private Map<Notifier, Batch> notifierBatches = new HashMap<Notifier, Batch>();

    @Override
    public void testConnection(Notifier notifier) throws ConnectionException {
        if(isMock(notifier)){
            try{Thread.sleep(200);}catch (Exception ie){}
            return;
        }
        Sender sender = new Sender(notifier.getApiKey());
        Message message = new Message.Builder().build();
        try {
            Result result = sender.send(message, "device_token", 1);
            LOG.debug("testConnection result: {}", result);
        } catch (IOException e) {
            throw new ConnectionException(e.getMessage(), e);
        }
    }

    @Override
    public void sendNotification(String providerId, Notifier notifier,
            Object payload, Notification notification, TaskTracker tracker)
            throws Exception {
        Map<String,Object> map = (Map<String, Object>) payload;
        final String expiresKey = "time_to_live";
        if(!map.containsKey(expiresKey) && notification.getExpire() != null){
            int expireSeconds = notification.getExpireTimeInSeconds();
            expireSeconds = expireSeconds <= 2419200 ? expireSeconds : 2419200; //send the max gcm value documented here http://developer.android.com/google/gcm/adv.html#ttl
            map.put(expiresKey, expireSeconds);
        }
        Batch batch = getBatch(notifier, map);
        batch.add(providerId, tracker);
    }

    synchronized private Batch getBatch(Notifier notifier,
            Map<String, Object> payload) {
        Batch batch = notifierBatches.get(notifier);
        if (batch == null && payload != null) {
            batch = new Batch(notifier, payload);
            notifierBatches.put(notifier, batch);
        }
        return batch;
    }

    @Override
    synchronized public void doneSendingNotifications() throws Exception {
        for (Batch batch : notifierBatches.values()) {
            batch.send();
        }
    }

    @Override
    public Map<String, Date> getInactiveDevices(Notifier notifier,
            EntityManager em) throws Exception {
        Batch batch = getBatch(notifier, null);
        Map<String,Date> map = null;
        if(batch != null) {
            map = batch.getAndClearInactiveDevices();
        }
        return map;
    }

    @Override
    public Map<String, Object> translatePayload(Object payload)
            throws Exception {
        Map<String, Object> mapPayload = new HashMap<String, Object>();
        if (payload instanceof Map) {
            mapPayload = (Map<String, Object>) payload;
        } else if (payload instanceof String) {
            mapPayload.put("data", payload);
        } else {
            throw new IllegalArgumentException(
                    "GCM Payload must be either a Map or a String");
        }
        if (JSON.toString(mapPayload).length() > 4096) {
            throw new IllegalArgumentException(
                    "GCM payloads must be 4096 characters or less");
        }
        return mapPayload;
    }

    @Override
    public void validateCreateNotifier(ServicePayload payload) throws Exception {
        if (payload.getProperty("apiKey") == null) {
            throw new RequiredPropertyNotFoundException("notifier", "apiKey");
        }
    }

    private class Batch {
        private Notifier notifier;
        private Map payload;
        private List<String> ids;
        private List<TaskTracker> trackers;
        private Map<String, Date> inactiveDevices = new HashMap<String, Date>();

        Batch(Notifier notifier, Map<String, Object> payload) {
            this.notifier = notifier;
            this.payload = payload;
            this.ids = new ArrayList<String>();
            this.trackers = new ArrayList<TaskTracker>();
        }

        synchronized Map<String, Date> getAndClearInactiveDevices() {
            Map<String, Date> map = inactiveDevices;
            inactiveDevices = new HashMap<String, Date>();
            return map;
        }

        synchronized void add(String id, TaskTracker tracker) throws Exception {
            ids.add(id);
            trackers.add(tracker);

            if (ids.size() == BATCH_SIZE) {
                send();
            }
        }

        // Message.Builder requires the payload to be Map<String,String> for no
        // good reason, so I just blind cast it.
        // What actually happens is: "JSONValue.toJSONString(payload);" so
        // anything that JSONValue can handle is fine.
        // (What is necessary here is that the Map needs to have a nested
        // structure.)
        synchronized void send() throws Exception {
            if (ids.size() == 0)
                return;
            Sender sender = new Sender(notifier.getApiKey());
            Message.Builder builder = new Message.Builder();
            builder.setData(payload);
            Message message = builder.build();
            if(isMock(notifier)){
                delayRandom(notifier);
                for(TaskTracker tracker : trackers){
                    tracker.completed("Mocked!");
                }
                return;
            }else {
                MulticastResult multicastResult = sender.send(message, ids,
                        SEND_RETRIES);
                LOG.debug("sendNotification result: {}", multicastResult);

                for (int i = 0; i < multicastResult.getTotal(); i++) {
                    Result result = multicastResult.getResults().get(i);

                    if (result.getMessageId() != null) {
                        String canonicalRegId = result.getCanonicalRegistrationId();
                        trackers.get(i).completed(canonicalRegId);
                    } else {
                        String error = result.getErrorCodeName();
                        trackers.get(i).failed(error, error);
                        if (Constants.ERROR_NOT_REGISTERED.equals(error)
                                || Constants.ERROR_INVALID_REGISTRATION
                                .equals(error)) {
                            inactiveDevices.put(ids.get(i), new Date());
                        }
                    }
                }
            }
            this.ids.clear();
            this.trackers.clear();
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
}
