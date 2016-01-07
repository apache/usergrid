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

import com.clearspring.analytics.hash.MurmurHash;
import com.google.android.gcm.server.*;
import org.apache.usergrid.persistence.entities.Notification;
import org.apache.usergrid.persistence.entities.Notifier;
import org.apache.usergrid.services.notifications.InactiveDeviceManager;
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
import java.util.concurrent.ConcurrentHashMap;

public class GCMAdapter implements ProviderAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(GCMAdapter.class);
    private static final int SEND_RETRIES = 3;
    private static int BATCH_SIZE = 1000;
    private final Notifier notifier;
    private EntityManager entityManager;

    private ConcurrentHashMap<Long,Batch> batches;

    private static final String ttlKey = "time_to_live";
    private static final String priorityKey = "priority";
    private static final String dataKey = "data";


    public GCMAdapter(EntityManager entityManager,Notifier notifier){
        this.notifier = notifier;
        this.entityManager = entityManager;
        batches = new ConcurrentHashMap<>();
    }
    @Override
    public void testConnection() throws Exception {
        Sender sender = new Sender(notifier.getApiKey());
        Message message = new Message.Builder().addData("registration_id", "").build();
        List<String> ids = new ArrayList<String>();
        ids.add("device_token");
        try {
            MulticastResult result = sender.send(message, ids, 1);
            LOG.debug("testConnection result: {}", result);
        } catch (InvalidRequestException e){
            // do nothing, we don't have a valid device token to test with
            LOG.debug("here for testing only");
        }
        catch (IOException e) {
            if(isInvalidRequestException(e)){
                throw new InvalidRequestException(401, Constants.ERROR_INVALID_REGISTRATION);
            }else {
                throw new ConnectionException(e.getMessage(), e);
            }
        }
    }

    @Override
    public void sendNotification(String providerId, Object payload, Notification notification, TaskTracker tracker)
            throws Exception {
        Map<String,Object> map = (Map<String, Object>) payload;
        if(!map.containsKey(ttlKey) && notification.getExpire() != null){
            // ttl provided to GCM is in seconds.  calculate the difference from now
            long ttlSeconds = notification.getExpireTTLSeconds();
            // max ttl for gcm is 4 weeks - https://developers.google.com/cloud-messaging/http-server-ref
            ttlSeconds = ttlSeconds <= 2419200 ? ttlSeconds : 2419200;
            map.put(ttlKey, (int)ttlSeconds);//needs to be int
        }
        if(!map.containsKey(priorityKey) && notification.getPriority() != null){
            map.put(priorityKey, notification.getPriority());
        }
        Batch batch = getBatch( map);
        batch.add(providerId, tracker);
    }

    private Batch getBatch( Map<String, Object> payload) {
        synchronized (this) {
            long hash = MurmurHash.hash64(payload);
            Batch batch = batches.get(hash);
            if (batch == null && payload != null) {
                batch = new Batch(notifier, payload);
                batches.put(hash, batch);
            }
            return batch;
        }
    }

    @Override
    public void doneSendingNotifications() throws Exception {
        synchronized (this) {
            for (Batch batch : batches.values()) {
                batch.send();
            }
        }
    }

    @Override
    public void removeInactiveDevices( ) throws Exception {
        Batch batch = getBatch( null);
        if(batch != null) {
            Map<String,Date> map = batch.getAndClearInactiveDevices();
            InactiveDeviceManager deviceManager = new InactiveDeviceManager(notifier,entityManager);
            deviceManager.removeInactiveDevices(map);
        }

    }

    @Override
    public Map<String, Object> translatePayload(Object payload)
            throws Exception {
        Map<String, Object> mapPayload = new HashMap<String, Object>();
        if (payload instanceof Map) {
            mapPayload = (Map<String, Object>) payload;
        } else if (payload instanceof String) {
            mapPayload.put(dataKey, payload);
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

    @Override
    public void stop() {
        try {
            synchronized (this) {
                for (Batch batch : batches.values()) {
                    batch.send();
                }
            }
        }catch (Exception e){
            LOG.error("error while trying to send on stop",e);
        }
    }

    @Override
    public Notifier getNotifier() {
        return notifier;
    }

    // this is a hack because Google library can't parse exceptions properly when you have a bad API key
    private boolean isInvalidRequestException(IOException ie){
        String message = ie.getMessage();
        return message.contains("Could not post JSON requests to GCM");
    }

    private class Batch {
        private Notifier notifier;
        private Map payload;
        private List<String> ids;
        private List<TaskTracker> trackers;
        private Map<String, Date> inactiveDevices = new HashMap<String, Date>();

        Batch(Notifier notifier, Map<String,Object> payload) {
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

        void add(String id, TaskTracker tracker) throws Exception {
            synchronized (this) {
                if(!ids.contains(id)) { //dedupe to a device
                    ids.add(id);
                    trackers.add(tracker);
                    if (ids.size() == BATCH_SIZE) {
                        send();
                    }
                }else{
                    tracker.completed();
                }

            }
        }


        void send() throws Exception {
            synchronized (this) {
                if (ids.size() == 0)
                    return;
                Sender sender = new Sender(notifier.getApiKey());
                Message.Builder builder = new Message.Builder();
                if(payload.containsKey(ttlKey)){
                    builder.timeToLive((int)payload.get(ttlKey));
                    payload.remove(ttlKey);
                }
                if(payload.containsKey(priorityKey)){

                    try{
                        builder.priority(Message.Priority.valueOf(payload.get(priorityKey).toString().toUpperCase()));
                    }catch(Exception e){
                        // couldn't determine the priority from the notification, default to "normal"
                        builder.priority(Message.Priority.NORMAL);
                    }
                    payload.remove(priorityKey);

                }

                // add our source notification payload data into the Message Builder
                // Message.Builder requires the payload to be Map<String,String> so blindly cast
                Map<String,String> dataMap = (Map<String,String>) payload;
                dataMap.forEach( (key, value) -> builder.addData(key, value));

                Message message = builder.build();
                MulticastResult multicastResult;
                try{

                    multicastResult = sender.send(message, ids, SEND_RETRIES);

                }catch (IOException e) {
                    if(isInvalidRequestException(e)){
                        String error = Constants.ERROR_INVALID_REGISTRATION;
                        for(int i=0; i < ids.size(); i++){
                            trackers.get(i).failed(error, error);
                        }
                        this.ids.clear();
                        this.trackers.clear();
                        return;
                        //throw new InvalidRequestException(401, Constants.ERROR_INVALID_REGISTRATION);
                    }else {
                        throw new ConnectionException(e.getMessage(), e);
                    }
                }


                LOG.debug("sendNotification result: {}", multicastResult);

                for (int i = 0; i < multicastResult.getResults().size(); i++) {
                    Result result = multicastResult.getResults().get(i);

                    if (result.getMessageId() != null) {
                        String canonicalRegId = result.getCanonicalRegistrationId();
                        trackers.get(i).completed(canonicalRegId);
                    } else {
                        String error = result.getErrorCodeName();
                        trackers.get(i).failed(error, error);
                        if (Constants.ERROR_NOT_REGISTERED.equals(error) || Constants.ERROR_INVALID_REGISTRATION.equals(error)) {
                            inactiveDevices.put(ids.get(i), new Date());
                        }
                    }
                }
                this.ids.clear();
                this.trackers.clear();
            }
        }
    }
}
