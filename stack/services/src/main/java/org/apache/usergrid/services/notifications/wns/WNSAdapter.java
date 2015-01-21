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
import ar.com.fernandospr.wns.exceptions.WnsException;
import ar.com.fernandospr.wns.model.WnsToast;
import ar.com.fernandospr.wns.model.builders.WnsToastBuilder;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.entities.Notification;
import org.apache.usergrid.persistence.entities.Notifier;
import org.apache.usergrid.services.ServicePayload;
import org.apache.usergrid.services.notifications.ConnectionException;
import org.apache.usergrid.services.notifications.ProviderAdapter;
import org.apache.usergrid.services.notifications.TaskTracker;
import org.mortbay.util.ajax.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
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
            service.pushToast("ms-app://s-1-15-2-2411381248-444863693-3819932088-4077691928-1194867744-112853457-373132695",toast);
        }catch (Exception e){
            LOG.error(e.toString());
        }
    }

    @Override
    public void sendNotification(String providerId, Object payload, Notification notification, TaskTracker tracker) throws Exception {
        try {
            WnsToast toast = new WnsToastBuilder().bindingTemplateToastText01(payload.toString()).build();
            service.pushToast(providerId, toast);
            tracker.completed();
        } catch (Exception e) {
            tracker.failed(0,e.toString());
            LOG.error("Failed to send notification",e);
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
        String toast = "";
        if (payload instanceof Map) {
            toast = ((Map<String, Object>) payload).get("toast").toString();
        } else {
            if (payload instanceof String) {
                toast = (String) payload;
            }else{
                throw new IllegalArgumentException("format is messed up");
            }
        }
        return toast;
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
