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

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class TaskManager {

    private static final Logger logger = LoggerFactory.getLogger(TaskManager.class);

    private Notification notification;
    private AtomicLong successes = new AtomicLong();
    private AtomicLong failures = new AtomicLong();
    private EntityManager em;

    public TaskManager(EntityManager em, Notification notification) {
        this.em = em;
        this.notification = notification;
    }

    public long getSuccesses(){return successes.get();}

    public long getFailures(){ return failures.get();}

    public void completed(Notifier notifier, UUID deviceUUID) throws Exception {
        completed(notifier,null,deviceUUID,null);
    }
    public void completed(Notifier notifier, Receipt receipt, UUID deviceUUID, String newProviderId) throws Exception {

        successes.incrementAndGet();


        try {
            //.{year}.{month}.{day}.{HH24} possibly minute.
            //random date and time for format


            //incrementNotificationCounter( "completed" );

            EntityRef deviceRef = new SimpleEntityRef(Device.ENTITY_TYPE, deviceUUID);

            if (receipt != null) {

                receipt.setSent(System.currentTimeMillis());
                this.saveReceipt(notification, deviceRef, receipt,false);
                if (logger.isTraceEnabled()) {
                    logger.trace("Notification {} receipt saved for device {}", notification.getUuid(), deviceUUID);
                }

            }

            if (newProviderId != null) {
                if (logger.isTraceEnabled()) {
                    logger.trace("Notification {} replacing notifier id for device {} ", notification.getUuid(), deviceUUID);
                }
                replaceProviderId(deviceRef, notifier, newProviderId);
            }

            if (logger.isTraceEnabled()) {
                logger.trace("Notification {} sending completed for device {}", notification.getUuid(), deviceUUID);
            }

        } catch(Exception e) {

            logger.error("Unable to mark notification {} as completed due to: {}", notification.getUuid(), e);

        }
    }

    public void failed(Notifier notifier, Receipt receipt, UUID deviceUUID, Object code, String message) throws Exception {

        failures.incrementAndGet();

        try {

            //incrementNotificationCounter( "failed" );

            if (logger.isDebugEnabled()) {
                logger.debug("Notification {} for device {} got error {}", notification.getUuid(), deviceUUID, code);
            }

            if(receipt != null) {
                receipt.setErrorCode( code );
                receipt.setErrorMessage( message );
                this.saveReceipt( notification, new SimpleEntityRef( Device.ENTITY_TYPE, deviceUUID ), receipt, true );
            }

            finishedBatch();

        } catch (Exception e){

            logger.error("Unable to finish marking notification {} as failed due to error: ", notification.getUuid(), e);

        }
    }

    /**
    * Called from TaskManager - Creates a persistent receipt
    *
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

            if ( logger.isDebugEnabled() ) {
                logger.debug( "Notification {} receipt saved for device {}", notification.getUuid(), device.getUuid() );
            }

        }

    }

    private void replaceProviderId(EntityRef device, Notifier notifier,
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

    public void incrementNotificationCounter(String status){
        em.incrementAggregateCounters( null,null,null,"counters.notifications."+notification.getUuid()+"."+status,1 );

        LocalDateTime localDateTime = LocalDateTime.now();
        StringBuilder currentDate = new StringBuilder(  );
        currentDate.append( "counters.notifications.aggregate."+status+"." );
        currentDate.append( localDateTime.getYear()+"." );
        currentDate.append( localDateTime.getMonth()+"." );
        currentDate.append( localDateTime.getDayOfMonth()+"." );
        currentDate.append( localDateTime.getMinute() );
        em.incrementAggregateCounters( null,null,null,currentDate.toString(),1 );

    }


    public void finishedBatch() throws Exception {

        long successes = this.successes.get();
        long failures = this.failures.get();

        // reset the counters
        this.successes.set(0);
        this.failures.set(0);

        // get the latest notification info
        notification = em.get(this.notification.getUuid(), Notification.class);

        notification.updateStatistics(successes, failures);
        notification.setModified(System.currentTimeMillis());
        notification.setFinished(notification.getModified());

        em.update(notification);

    }
}
