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

    private static final Logger logger = LoggerFactory.getLogger(TaskManager.class);

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
        if (logger.isTraceEnabled()) {
            logger.trace("REMOVED {}", deviceUUID);
        }
        try {
            if (logger.isTraceEnabled()) {
                logger.trace("notification {} removing device {} from remaining", notification.getUuid(), deviceUUID);
            }

            EntityRef deviceRef = new SimpleEntityRef(Device.ENTITY_TYPE, deviceUUID);
            if (receipt != null) {
                if (logger.isTraceEnabled()) {
                    logger.trace("notification {} sent to device {}. saving receipt.", notification.getUuid(), deviceUUID);
                }
                receipt.setSent(System.currentTimeMillis());
                this.saveReceipt(notification, deviceRef, receipt,false);
                if (logger.isTraceEnabled()) {
                    logger.trace("notification {} receipt saved for device {}", notification.getUuid(), deviceUUID);
                }
                successes.incrementAndGet();
            }

            if (newProviderId != null) {
                if (logger.isTraceEnabled()) {
                    logger.trace("notification {} replacing device {} notifierId", notification.getUuid(), deviceUUID);
                }
                replaceProviderId(deviceRef, notifier, newProviderId);
            }

            if (logger.isTraceEnabled()) {
                logger.trace("notification {} completed device {}", notification.getUuid(), deviceUUID);
            }

        } finally {
            if (logger.isTraceEnabled()) {
                logger.trace("COUNT is: {}", successes.get());
            }
//            if (hasFinished) { //process has finished but notifications are still coming in
//                finishedBatch();
//
//            }
        }
    }

    public void failed(Notifier notifier, Receipt receipt, UUID deviceUUID, Object code, String message) throws Exception {

        try {
            if (logger.isDebugEnabled()) {
                logger.debug("notification {} for device {} got error {}", notification.getUuid(), deviceUUID, code);
            }

            failures.incrementAndGet();
            if(receipt!=null) {
                if ( receipt.getUuid() != null ) {
                    successes.decrementAndGet();
                }
                receipt.setErrorCode( code );
                receipt.setErrorMessage( message );
                this.saveReceipt( notification, new SimpleEntityRef( Device.ENTITY_TYPE, deviceUUID ), receipt, true );
                if ( logger.isDebugEnabled() ) {
                    logger.debug( "notification {} receipt saved for device {}", notification.getUuid(), deviceUUID );
                }
            }
        } finally {
            completed(notifier, deviceUUID);
            finishedBatch();
        }
    }

    /*
    * called from TaskManager - creates a persistent receipt and updates the
    * passed one w/ the UUID
    */
    private void saveReceipt(EntityRef notification, EntityRef device, Receipt receipt, boolean hasError) throws Exception {

        boolean debug = false;
        if(this.notification != null){
            debug = this.notification.getDebug();
        }

        if ( debug || hasError) {

            List<EntityRef> entities = Arrays.asList(notification, device);

            if (receipt.getUuid() == null) {
                Receipt savedReceipt = em.create(receipt);
                em.addToCollections(entities, Notification.RECEIPTS_COLLECTION, savedReceipt);
            } else {
                em.addToCollections(entities, Notification.RECEIPTS_COLLECTION, receipt);
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
        finishedBatch(true);
    }

    public void finishedBatch(boolean refreshNotification) throws Exception {

        long successes = this.successes.get(); //reset counters
        long failures = this.failures.get(); //reset counters

        for (int i = 0; i < successes; i++) {
            this.successes.decrementAndGet();
        }
        for (int i = 0; i < failures; i++) {
            this.failures.decrementAndGet();
        }

        this.hasFinished = true;

        // force refresh notification by fetching it
        if (refreshNotification) {
            notification = em.get(this.notification.getUuid(), Notification.class);
        }

        notification.updateStatistics(successes, failures);
        notification.setModified(System.currentTimeMillis());
        notification.setFinished(notification.getModified());

        em.update(notification);
    }
}
