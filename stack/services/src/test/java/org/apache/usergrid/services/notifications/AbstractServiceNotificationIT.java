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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.usergrid.persistence.EntityRef;
import org.apache.usergrid.persistence.PathQuery;
import org.apache.usergrid.persistence.Query;
import org.apache.usergrid.persistence.SimpleEntityRef;
import org.apache.usergrid.persistence.entities.Notification;
import org.apache.usergrid.persistence.entities.Receipt;
import org.apache.usergrid.services.AbstractServiceIT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;


public abstract class AbstractServiceNotificationIT extends AbstractServiceIT {
    private NotificationsService ns;

    private static final Logger logger = LoggerFactory
        .getLogger(AbstractServiceNotificationIT.class);

    protected NotificationsService getNotificationService(){
        ns = (NotificationsService) app.getSm().getService("notifications");
        return ns;
    }

    protected Notification notificationWaitForComplete(Notification notification)
            throws Exception {

        int retry = 18;  // 3 mins  18 * 10 seconds
        while (-- retry > 0) {
            logger.info("notificationWaitForComplete {} retry {}", notification.getUuid(), retry);
            app.waitForQueueDrainAndRefreshIndex(10000);
            notification = app.getEntityManager().get(notification.getUuid(), Notification.class);
            if (notification.getFinished() != null) {
                return notification;
            }
        }
        fail("Notification failed to complete error message " + notification.getErrorMessage());
        return null;
    }

    protected List<EntityRef> getNotificationReceipts(EntityRef notification)
            throws Exception {
        Query query = new Query();
        query.setCollection("receipts");
        query.setLimit(100);
        PathQuery<Receipt> pathQuery = new PathQuery<Receipt>(
                new SimpleEntityRef(app.getEntityManager().getApplicationRef()),
                query
        );
        Iterator<Receipt> it = pathQuery.iterator(app.getEntityManager());
        List<EntityRef> list =new ArrayList<EntityRef>();//get all
        while(it.hasNext()){
            Receipt receipt =it.next();
            if(receipt.getNotificationUUID().equals(notification.getUuid())) {
                list.add(receipt);
            }
        }
        return list;
    }

    protected void  checkReceipts(Notification notification, int expected)
            throws Exception {
        List<EntityRef> receipts = getNotificationReceipts(notification);
        long timeout = System.currentTimeMillis() + 10000;
        while (System.currentTimeMillis() < timeout) {
            Thread.sleep(200);
            receipts =getNotificationReceipts(notification);
            if (receipts.size()==expected) {
                break;
            }
        }


        assertEquals(expected, receipts.size());


        for (EntityRef receipt : receipts) {

            logger.info("checkReceipts - receipt uuid: {}, notification uuid: {}", receipt.getUuid(), notification.getUuid());

            Receipt r = app.getEntityManager().get(receipt, Receipt.class);
            assertNotNull(r.getSent());
            assertNotNull(r.getPayload());
            assertNotNull(r.getNotifierId());
            EntityRef source = getNotificationService().getSourceNotification(r);
            assertEquals(source.getUuid(), notification.getUuid());
        }
    }

    protected void checkStatistics(Notification notification, long sent,  long errors) throws Exception{
        Map<String, Object> statistics = null;
        long timeout = System.currentTimeMillis() + 10000;
        while (System.currentTimeMillis() < timeout) {
            Thread.sleep(200);
            statistics = app.getEntityManager().get(notification.getUuid(), Notification.class).getStatistics();
            if ((Long)statistics.get("sent")==sent && (Long)statistics.get("errors")==errors) {
                break;
            }
        }
        assertEquals(sent, ((Long)statistics.get("sent")).longValue());
        assertEquals(errors, ((Long)statistics.get("errors")).longValue());
    }

}
