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

import org.apache.usergrid.persistence.*;
import org.apache.usergrid.persistence.entities.Notification;
import org.apache.usergrid.persistence.entities.Receipt;
import org.apache.usergrid.persistence.index.query.Query;
import org.apache.usergrid.services.ServiceManagerFactory;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestName;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.usergrid.services.AbstractServiceIT;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;


public class AbstractServiceNotificationIT extends AbstractServiceIT {
    private NotificationsService ns;
    @Autowired
    private ServiceManagerFactory smf;

    @Autowired
    private EntityManagerFactory emf;
    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void beforeClass() {
    }

    @Before
    public void before() throws Exception {

    }

    protected NotificationsService getNotificationService(){
        ns = (NotificationsService) app.getSm().getService("notifications");
        return ns;
    }



    protected Notification scheduleNotificationAndWait(Notification notification)
            throws Exception {
        long timeout = System.currentTimeMillis() + 60000;
        while (System.currentTimeMillis() < timeout) {
            Thread.sleep(200);
            app.getEm().refreshIndex();
            notification = app.getEm().get(notification.getUuid(), Notification.class);
            if (notification.getFinished() != null) {
                return notification;
            }
        }
        fail("Notification failed to complete");
        return null;
    }

    protected List<EntityRef> getNotificationReceipts(EntityRef notification)
            throws Exception {
        Query query = new Query();
        query.setCollection("receipts");
        query.setLimit(100);
        PathQuery<Receipt> pathQuery = new PathQuery<Receipt>(
                new SimpleEntityRef(app.getEm().getApplicationRef()),
                query
        );
        Iterator<Receipt> it = pathQuery.iterator(app.getEm());
        List<EntityRef> list =new ArrayList<EntityRef>();//get all
        while(it.hasNext()){
            Receipt receipt =it.next();
            if(receipt.getNotificationUUID().equals(notification.getUuid())) {
                list.add(receipt);
            }
        }
        return list;
    }

    protected void checkReceipts(Notification notification, int expected)
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
            Receipt r = app.getEm().get(receipt, Receipt.class);
            assertNotNull(r.getSent());
            assertNotNull(r.getPayload());
            assertNotNull(r.getNotifierId());
            EntityRef source = getNotificationService().getSourceNotification(r);
            assertEquals(source.getUuid(), notification.getUuid());
        }
    }

    protected void checkStatistics(Notification notification, long sent,  long errors) throws Exception{
        Map<String, Long> statistics = null;
        long timeout = System.currentTimeMillis() + 10000;
        while (System.currentTimeMillis() < timeout) {
            Thread.sleep(200);
            statistics = app.getEm().get(notification.getUuid(), Notification.class).getStatistics();
            if (statistics.get("sent")==sent && statistics.get("errors")==errors) {
                break;
            }
        }
        assertEquals(sent, statistics.get("sent").longValue());
        assertEquals(errors, statistics.get("errors").longValue());
    }

}
