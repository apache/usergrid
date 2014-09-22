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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class SingleQueueTaskManager implements NotificationsTaskManager {

    private static final Logger LOG = LoggerFactory
            .getLogger(SingleQueueTaskManager.class);
    private final String path;
    private final QueueManager proxy;

    private Notification notification;
    private AtomicLong successes = new AtomicLong();
    private AtomicLong failures = new AtomicLong();
    private org.apache.usergrid.mq.QueueManager qm;
    private EntityManager em;
    private ConcurrentHashMap<UUID, ApplicationQueueMessage> messageMap;
    private boolean hasFinished;

    public SingleQueueTaskManager(EntityManager em, org.apache.usergrid.mq.QueueManager qm, QueueManager proxy, Notification notification) {
        this.em = em;
        this.qm = qm;
        this.path = proxy.getQueuePath();
        this.notification = notification;
        this.proxy = proxy;
        this.messageMap = new ConcurrentHashMap<UUID, ApplicationQueueMessage>();
        hasFinished = false;
    }

    public void addMessage(UUID deviceId, ApplicationQueueMessage message) {
        messageMap.put(deviceId, message);
    }

    public void completed(Notifier notifier, Receipt receipt, UUID deviceUUID, String newProviderId) throws Exception {
        LOG.debug("REMOVED {}", deviceUUID);
        try {
            EntityRef deviceRef = new SimpleEntityRef(Device.ENTITY_TYPE, deviceUUID);
            if (receipt != null) {
                LOG.debug("notification {} sent to device {}. saving receipt.", notification.getUuid(), deviceUUID);
                receipt.setSent(System.currentTimeMillis());
                this.saveReceipt(notification, deviceRef, receipt);
                LOG.debug("notification {} receipt saved for device {}", notification.getUuid(), deviceUUID);
                successes.incrementAndGet();
            }

            LOG.debug("notification {} removing device {} from remaining", notification.getUuid(), deviceUUID);
            qm.commitTransaction(path, messageMap.get(deviceUUID).getTransaction(), null);
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
            this.saveReceipt(notification, new SimpleEntityRef(Device.ENTITY_TYPE, deviceUUID), receipt);
            LOG.debug("notification {} receipt saved for device {}", notification.getUuid(), deviceUUID);
        } finally {
            completed(notifier, null, deviceUUID, null);
        }
    }

    /*
    * called from TaskManager - creates a persistent receipt and updates the
    * passed one w/ the UUID
    */
    private void saveReceipt(EntityRef notification, EntityRef device, Receipt receipt) throws Exception {
        if (receipt.getUuid() == null) {
            Receipt savedReceipt = em.create(receipt);
            receipt.setUuid(savedReceipt.getUuid());

            List<EntityRef> entities = Arrays.asList(notification, device);
//            em.addToCollections(entities, Notification.RECEIPTS_COLLECTION, savedReceipt);
        } else {
            em.update(receipt);
        }
    }

    protected void replaceProviderId(EntityRef device, Notifier notifier,
                                     String newProviderId) throws Exception {
        Object value = em.getProperty(device, notifier.getName()
                + NotificationsService.NOTIFIER_ID_POSTFIX);
        if (value != null) {
            em.setProperty(device, notifier.getName() + NotificationsService.NOTIFIER_ID_POSTFIX, newProviderId);
        } else {
            value = em.getProperty(device, notifier.getUuid()
                    + NotificationsService.NOTIFIER_ID_POSTFIX);
            if (value != null) {
                em.setProperty(device, notifier.getUuid() + NotificationsService.NOTIFIER_ID_POSTFIX, newProviderId);
            }
        }
    }

    public void finishedBatch() throws Exception {
        long successes = this.successes.getAndSet(0); //reset counters
        long failures = this.failures.getAndSet(0); //reset counters
        this.hasFinished = true;

        // refresh notification
        Notification notification = em.get(this.notification.getUuid(), Notification.class);
        notification.setModified(System.currentTimeMillis());

        long sent = successes,errors = failures;
        //and write them out again, this will produce the most accurate count
        Map<String, Long> stats;
        stats = new HashMap<String, Long>(2);
        stats.put("sent", sent);
        stats.put("errors", errors);
        notification.setStatistics(stats);

        LOG.info("notification {} sending to {}", notification.getUuid(), sent + errors);

        //none of this is known and should you ever do this
        if (notification.getExpectedCount() <= (errors + sent)) {
            Map<String, Object> properties = new HashMap<>();
            notification.setFinished(notification.getModified());
            properties.put("finished", notification.getModified());
            properties.put("state", notification.getState());
            LOG.info("done sending to devices in {} ms", notification.getFinished() - notification.getStarted());
            notification.addProperties(properties);
        }
        LOG.info("notification finished batch: {}", notification.getUuid());
        em.update(notification);

//        Set<Notifier> notifiers = new HashSet<>(proxy.getNotifierMap().values()); // remove dups
//        proxy.asyncCheckForInactiveDevices(notifiers);
    }
}