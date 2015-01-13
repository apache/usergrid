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

import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.EntityRef;
import org.apache.usergrid.persistence.SimpleEntityRef;
import org.apache.usergrid.persistence.entities.Device;
import org.apache.usergrid.persistence.entities.Notification;
import org.apache.usergrid.persistence.entities.Notifier;
import org.apache.usergrid.persistence.entities.Receipt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class TaskManager {

    private static final Logger LOG = LoggerFactory.getLogger(TaskManager.class);

    private Notification notification;
    private AtomicLong successes = new AtomicLong();
    private AtomicLong failures = new AtomicLong();
    private EntityManager em;
    private boolean hasFinished;

    public TaskManager(EntityManager em, Notification notification) {
        this.em = em;
        this.notification = notification;
        hasFinished = false;
    }

    public long getSuccesses(){return successes.get();}

    public long getFailures(){ return failures.get();}

    public void completed(Notifier notifier, UUID deviceUUID) throws Exception {
        completed(notifier,null,deviceUUID,null);
    }
    public void completed(Notifier notifier, Receipt receipt, UUID deviceUUID, String newProviderId) throws Exception {
        LOG.debug("REMOVED {}", deviceUUID);
        try {
            LOG.debug("notification {} removing device {} from remaining", notification.getUuid(), deviceUUID);

            EntityRef deviceRef = new SimpleEntityRef(Device.ENTITY_TYPE, deviceUUID);
            if (receipt != null) {
                LOG.debug("notification {} sent to device {}. saving receipt.", notification.getUuid(), deviceUUID);
                receipt.setSent(System.currentTimeMillis());
                this.saveReceipt(notification, deviceRef, receipt,false);
                LOG.debug("notification {} receipt saved for device {}", notification.getUuid(), deviceUUID);
                successes.incrementAndGet();
            }

            if (newProviderId != null) {
                LOG.debug("notification {} replacing device {} notifierId", notification.getUuid(), deviceUUID);
                replaceProviderId(deviceRef, notifier, newProviderId);
            }

            LOG.debug("notification {} completed device {}", notification.getUuid(), deviceUUID);

        } finally {
            LOG.debug("COUNT is: {}", successes.get());
            if (hasFinished) { //process has finished but notifications are still coming in
                finishedBatch();

            }
        }
    }

    public void failed(Notifier notifier, Receipt receipt, UUID deviceUUID, Object code, String message) throws Exception {

        try {
            if (LOG.isDebugEnabled()) {
                StringBuilder sb = new StringBuilder();
                sb.append("notification ").append(notification.getUuid());
                sb.append(" for device ").append(deviceUUID);
                sb.append(" got error ").append(code);
                LOG.debug(sb.toString());
            }

            failures.incrementAndGet();
            if (receipt.getUuid() != null) {
                successes.decrementAndGet();
            }
            receipt.setErrorCode(code);
            receipt.setErrorMessage(message);
            this.saveReceipt(notification, new SimpleEntityRef(Device.ENTITY_TYPE, deviceUUID), receipt,true);
            LOG.debug("notification {} receipt saved for device {}", notification.getUuid(), deviceUUID);
        } finally {
            completed(notifier, deviceUUID);
        }
    }

    /*
    * called from TaskManager - creates a persistent receipt and updates the
    * passed one w/ the UUID
    */
    private void saveReceipt(EntityRef notification, EntityRef device, Receipt receipt, boolean hasError) throws Exception {
        if (this.notification.getDebug() || hasError) {
            if (receipt.getUuid() == null) {
                Receipt savedReceipt = em.create(receipt);
                receipt.setUuid(savedReceipt.getUuid());
                List<EntityRef> entities = Arrays.asList(notification, device);
//              em.addToCollections(entities, Notification.RECEIPTS_COLLECTION, savedReceipt);
            } else {
                em.update(receipt);
            }
        }

    }

    protected void replaceProviderId(EntityRef device, Notifier notifier,
                                     String newProviderId) throws Exception {
        Object value = em.getProperty(device, notifier.getName()
                + ApplicationQueueManager.NOTIFIER_ID_POSTFIX);
        if (value != null) {
            em.setProperty(device, notifier.getName() + ApplicationQueueManager.NOTIFIER_ID_POSTFIX, newProviderId);
        } else {
            value = em.getProperty(device, notifier.getUuid()
                    + ApplicationQueueManager.NOTIFIER_ID_POSTFIX);
            if (value != null) {
                em.setProperty(device, notifier.getUuid() + ApplicationQueueManager.NOTIFIER_ID_POSTFIX, newProviderId);
            }
        }
    }
    public void finishedBatch() throws Exception {
        finishedBatch(true,false);
    }
    public void finishedBatch(boolean fetch, boolean force) throws Exception {
        if (notification.getDebug() || getFailures() > 0 || force) {
            long successes = this.successes.get(); //reset counters
            long failures = this.failures.get(); //reset counters
            for (int i = 0; i < successes; i++) {
                this.successes.decrementAndGet();
            }
            for (int i = 0; i < failures; i++) {
                this.failures.decrementAndGet();
            }

            this.hasFinished = true;

            // refresh notification
            if (fetch)
                notification = em.get(this.notification.getUuid(), Notification.class);

            //and write them out again, this will produce the most accurate count
            Map<String, Long> stats = new HashMap<>(2);
            stats.put("sent", successes);
            stats.put("errors", failures);
            notification.updateStatistics(successes, failures);

            long totals = (notification.getStatistics().get("sent") + notification.getStatistics().get("errors"));
            //none of this is known and should you ever do this
            notification.setModified(System.currentTimeMillis());
            notification.setFinished(notification.getModified());

            Map<String, Object> properties = new HashMap<>();
            properties.put("finished", notification.getModified());
            properties.put("state", notification.getState());
            notification.addProperties(properties);

            long latency = notification.getFinished() - notification.getStarted();
            LOG.info("notification finished batch: {} of {} devices in " + latency + "ms", notification.getUuid(), totals);

            em.update(notification);
//        Set<Notifier> notifiers = new HashSet<>(proxy.getAdapterMap().values()); // remove dups
//        proxy.asyncCheckForInactiveDevices(notifiers);
        }
    }
}