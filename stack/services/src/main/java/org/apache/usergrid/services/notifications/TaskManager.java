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

import org.apache.usergrid.persistence.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.usergrid.mq.Message;
import org.apache.usergrid.mq.QueueManager;
import org.apache.usergrid.mq.QueueResults;
import org.apache.usergrid.persistence.entities.Device;

/**
 * When all Tasks are "complete", this calls notifyAll(). Note: This may not
 * mean that all work is done, however, as delivery errors may come in after a
 * notification is "sent."
 */
public class TaskManager {

    // period to poll for batch completion (keep well under Job scheduler
    // heartbeat value!)
    private static final long BATCH_POLL_PERIOD = 60 * 1000;
    // period to tell job scheduler to wait between heartbeats before timing out
    // this transaction
    public static final long SCHEDULER_HEARTBEAT_PERIOD = 5 * 60 * 1000;
    // period at which the batch is considered dead without activity (10
    // minutes)
    // setting it high means that a batch that is dead will hang for longer
    // but setting it too low may cause duplicates to be sent.
    // also used for Delay before another Job will be attempted - thus total
    // time
    // before a job might be restarted could be as long as 2 x
    // BATCH_DEATH_PERIOD
    static final long BATCH_DEATH_PERIOD = 10 * 60 * 1000;
    public static final long MESSAGE_TRANSACTION_TIMEOUT = SCHEDULER_HEARTBEAT_PERIOD;

    private static final Logger LOG = LoggerFactory
            .getLogger(TaskManager.class);
    private final String path;

    private Notification notification;
    private  ConcurrentHashMap<UUID,Message> remaining;
    private AtomicLong successes = new AtomicLong();
    private AtomicLong failures = new AtomicLong();
    private AtomicLong skips = new AtomicLong();
    private QueueManager qm;
    private EntityManager em;
    private NotificationServiceProxy ns;

    public TaskManager(  EntityManager em, NotificationServiceProxy ns,QueueManager qm, Notification notification, QueueResults queueResults) {
        this.em = em;
        this.qm = qm;
        this.ns = ns;
        this.path = queueResults.getPath();
        this.notification = notification;
        this.remaining = new ConcurrentHashMap<UUID,Message>();
        for (Message m : queueResults.getMessages()) {
            remaining
                    .put((UUID) m.getObjectProperty("deviceUUID"), m);
        }
    }

    public void skip(UUID deviceUUID) throws Exception {
        if (LOG.isDebugEnabled()) {
            LOG.debug("notification {} skipped device {}",
                    notification.getUuid(), deviceUUID);
        }
        skips.incrementAndGet();
        completed(null, null, deviceUUID, null);
    }

    public void completed(Notifier notifier, Receipt receipt, UUID deviceUUID,
            String newProviderId) throws Exception {

            LOG.debug("REMOVED {}", deviceUUID);
            try {
                EntityRef deviceRef = new SimpleEntityRef(Device.ENTITY_TYPE,
                        deviceUUID);

                if (receipt != null) {
                    LOG.debug("notification {} sent to device {}. saving receipt.",
                            notification.getUuid(), deviceUUID);
                    successes.incrementAndGet();
                    receipt.setSent(System.currentTimeMillis());
                    this.saveReceipt(notification, deviceRef, receipt);
                    LOG.debug("notification {} receipt saved for device {}",
                            notification.getUuid(), deviceUUID);
                }

                if (remaining.containsKey(deviceUUID)) {
                    LOG.debug("notification {} removing device {} from remaining", notification.getUuid(), deviceUUID);
                    qm.commitTransaction(path, remaining.get(deviceUUID).getTransaction(), null);
                }

                if (newProviderId != null) {
                    LOG.debug("notification {} replacing device {} notifierId", notification.getUuid(), deviceUUID);
                    replaceProviderId(deviceRef, notifier, newProviderId);
                }

                LOG.debug("notification {} completed device {}", notification.getUuid(), deviceUUID);

            } finally {
                LOG.debug("COUNT is: {}", successes.get());
                remaining.remove(deviceUUID);
                // note: stats are transient for the duration of the batch
                if (remaining.size() == 0) {
                    long successesCopy = successes.get();
                    long failuresCopy = failures.get();
                    if (successesCopy > 0 || failuresCopy > 0 || skips.get()>0) {
                        ns.finishedBatch(notification, successesCopy, failuresCopy);
                    }
                }

            }
    }

    public void failed(Notifier notifier, Receipt receipt, UUID deviceUUID, Object code,
            String message) throws Exception {

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
            this.saveReceipt(notification, new SimpleEntityRef( Device.ENTITY_TYPE, deviceUUID), receipt);
            LOG.debug("notification {} receipt saved for device {}",  notification.getUuid(), deviceUUID);
        } finally {
            completed(notifier, null, deviceUUID, null);
        }
    }

    /*
    * called from TaskManager - creates a persistent receipt and updates the
    * passed one w/ the UUID
    */
    public void saveReceipt(EntityRef notification, EntityRef device,
                            Receipt receipt) throws Exception {
        if (receipt.getUuid() == null) {
            Receipt savedReceipt = em.create(receipt);
            receipt.setUuid(savedReceipt.getUuid());

            List<EntityRef> entities = Arrays.asList(notification, device);
            em.addToCollections(entities, Notification.RECEIPTS_COLLECTION,  savedReceipt);
        } else {
            em.update(receipt);
        }
    }

    protected void replaceProviderId(EntityRef device, Notifier notifier,
                                     String newProviderId) throws Exception {
        Object value = em.getProperty(device, notifier.getName()
                + NotificationsService.NOTIFIER_ID_POSTFIX);
        if (value != null) {
            em.setProperty(device, notifier.getName() + NotificationsService.NOTIFIER_ID_POSTFIX,  newProviderId);
        } else {
            value = em.getProperty(device, notifier.getUuid()
                    + NotificationsService.NOTIFIER_ID_POSTFIX);
            if (value != null) {
                em.setProperty(device,  notifier.getUuid() + NotificationsService.NOTIFIER_ID_POSTFIX, newProviderId);
            }
        }
    }


}