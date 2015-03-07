/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one or more
 *  *  contributor license agreements.  The ASF licenses this file to You
 *  * under the Apache License, Version 2.0 (the "License"); you may not
 *  * use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.  For additional information regarding
 *  * copyright in this work, please see the NOTICE file in the top level
 *  * directory of this distribution.
 *
 */

package org.apache.usergrid.services.notifications.wns;

import ar.com.fernandospr.wns.WnsService;
import ar.com.fernandospr.wns.model.WnsBadge;
import ar.com.fernandospr.wns.model.WnsRaw;
import ar.com.fernandospr.wns.model.WnsToast;
import ar.com.fernandospr.wns.model.builders.WnsBadgeBuilder;
import ar.com.fernandospr.wns.model.builders.WnsToastBuilder;
import com.sun.jersey.api.client.ClientHandlerException;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.entities.Notification;
import org.apache.usergrid.persistence.entities.Notifier;
import org.apache.usergrid.services.ServicePayload;
import org.apache.usergrid.services.notifications.ConnectionException;
import org.apache.usergrid.services.notifications.ProviderAdapter;
import org.apache.usergrid.services.notifications.TaskTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Windows Notifications Service adapter to send windows notifications
 */
public class WNSAdapter implements ProviderAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(WNSAdapter.class);

    private final EntityManager entityManager;
    private final Notifier notifier;
    private final WnsService service;

    public WNSAdapter(EntityManager entityManager, Notifier notifier) {
        this.entityManager = entityManager;
        this.notifier = notifier;
        this.service = new WnsService(notifier.getSid(), notifier.getApiKey(), notifier.getLogging());
    }

    @Override
    public void testConnection() throws ConnectionException {
        WnsToast toast = new WnsToastBuilder().bindingTemplateToastText01("test").build();
        try{
            //this fails every time due to jax error which is ok
            service.pushToast("s-1-15-2-2411381248-444863693-3819932088-4077691928-1194867744-112853457-373132695",toast);
        }catch (ClientHandlerException e){
            LOG.info("Windows Phone notifier added: "+e.toString());
        }
    }

    @Override
    public void sendNotification(String providerId, Object payload, Notification notification, TaskTracker tracker) throws Exception {
        try {
            List<TranslatedNotification> translatedNotifications = ( List<TranslatedNotification>) payload;
            for(TranslatedNotification translatedNotification : translatedNotifications) {
                switch (translatedNotification.getType()) {
                    case "toast":
                        WnsToast toast = new WnsToastBuilder().bindingTemplateToastText01(translatedNotification.getMessage().toString()).build();
                        service.pushToast(providerId, toast);
                        break;
                    case "badge":
                        WnsBadge badge;
                        if (translatedNotification.getMessage() instanceof Integer) {
                            badge = new WnsBadgeBuilder().value((Integer) translatedNotification.getMessage()).build();
                        } else {
                            badge = new WnsBadgeBuilder().value(translatedNotification.getMessage().toString()).build();
                        }
                        service.pushBadge(providerId, badge);
                        break;
                    case "raw" :
                        WnsRaw raw = new WnsRaw();
                        raw.stream = toBytes( translatedNotification.getMessage() ) ;
                        service.pushRaw(providerId, raw);
                        break;
                    default : throw new IllegalArgumentException(translatedNotification.getType()+" does not match a valid notification type (toast,badge).");
                }
            }
            tracker.completed();
        } catch (Exception e) {
            tracker.failed(0,e.toString());
            LOG.error("Failed to send notification",e);
        }
    }

    private byte[] toBytes(Object message) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = null;
        try {
            if(message instanceof Serializable) {
                out = new ObjectOutputStream(bos);
                out.writeObject(message);
                byte[] yourBytes = bos.toByteArray();
                return yourBytes;
            }else{
                throw new RuntimeException("message is not serializable");
            }
        }catch (IOException e){
            throw new RuntimeException(e);
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException ex) {
                // ignore close exception
            }
            try {
                bos.close();
            } catch (IOException ex) {
                // ignore close exception
            }
        }
    }
    @Override
    public void doneSendingNotifications() throws Exception {

    }

    @Override
    public void removeInactiveDevices() throws Exception {

    }

    @Override
    public Object translatePayload(Object payload) throws Exception {
        //TODO: allow for badges and toasts at same time
        List<TranslatedNotification> translatedNotifications = new ArrayList<>();
        if (payload instanceof Map) {
            //{payloads:{winphone:{toast:"mymessage",badge:1}}}
            Map<String, Object> map = (Map<String, Object>) payload;
            if (map.containsKey("toast")) {
                translatedNotifications.add(new TranslatedNotification(map.get("toast").toString(), "toast"));
            }
            if (map.containsKey("badge")) {
                translatedNotifications.add(new TranslatedNotification(map.get("badge"), "badge"));
            }
            if (map.containsKey("raw")) {
                translatedNotifications.add(new TranslatedNotification(map.get("raw"), "raw"));
            }

        } else {
            //{payloads:{winphone:"mymessage"}}
            //make it a toast if its just a string
            if (payload instanceof String) {
                translatedNotifications.add(new TranslatedNotification( (String) payload,"toast"));
            }else{
                throw new IllegalArgumentException("format is messed up");
            }
        }
        return translatedNotifications;
    }

    @Override
    public void validateCreateNotifier(ServicePayload payload) throws Exception {
        String apiKey = payload.getStringProperty("apiKey");
        String sid = payload.getStringProperty("sid");
        Object logging = payload.getProperty("logging");
        if(sid == null){
            throw new IllegalArgumentException("sid is missing");
        }
        if(logging == null){
            throw new IllegalArgumentException("logging is missing");
        }
        if(apiKey == null){
            throw new IllegalArgumentException("apiKey is missing");
        }
    }

    @Override
    public void stop() {
        //Do nothing
    }

    @Override
    public Notifier getNotifier() {
        return notifier;
    }


}
