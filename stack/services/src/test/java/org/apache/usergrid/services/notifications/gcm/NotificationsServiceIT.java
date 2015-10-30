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

import org.apache.usergrid.persistence.*;
import org.apache.usergrid.persistence.entities.*;
import org.apache.usergrid.services.notifications.*;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.*;

import org.apache.usergrid.services.ServiceAction;

import static org.junit.Assert.*;
import static org.apache.usergrid.services.notifications.ApplicationQueueManager.NOTIFIER_ID_POSTFIX;

public class NotificationsServiceIT extends AbstractServiceNotificationIT {


    private static final Logger logger = LoggerFactory
            .getLogger(NotificationsServiceIT.class);

    /**
     * set to true to run tests against actual GCM servers - but they may not
     * all run correctly
     */
    private static final boolean USE_REAL_CONNECTIONS = false;
    private static final String PROVIDER = USE_REAL_CONNECTIONS ? "google" : "noop";

    private static final String API_KEY = "AIzaSyCIH_7WC0mOqBGMOXyQnFgrBpOePgHvQJM";
    private static final String PUSH_TOKEN = "APA91bGxRGnMK8tKgVPzSlxtCFvwSVqx0xEPjA06sBmiK0k"
            + "QsiwUt6ipSYF0iPRHyUgpXle0P8OlRWJADkQrcN7yxG4pLMg1CVmrqDu8tfSe63mZ-MRU2IW0cOhmo"
            + "sqzC9trl33moS3OvT7qjDjkP4Qq8LYdwwYC5A";

    private Notifier notifier;
    private Device device1, device2;
    private NotificationsService ns;
    private QueueListener listener;





    @BeforeClass
    public static void setup(){


    }
    @Before
    public void before() throws Exception {


        // create gcm notifier //

        app.clear();
        app.put("name", "gcm");
        app.put("provider", PROVIDER);
        app.put("environment", "development");
        app.put("apiKey", API_KEY);

        notifier = (Notifier) app
                .testRequest(ServiceAction.POST, 1, "notifiers").getEntity()
                .toTypedEntity();
        String key = notifier.getName() + NOTIFIER_ID_POSTFIX;

        // create devices //

        app.clear();
        app.put(key, PUSH_TOKEN);

        Entity e = app.testRequest(ServiceAction.POST, 1, "devices") .getEntity();
        app.testRequest(ServiceAction.GET, 1, "devices", e.getUuid());

        device1 = app.getEntityManager().get(e.getUuid(), Device.class);
        assertEquals(device1.getProperty(key), PUSH_TOKEN);

        app.put(key, PUSH_TOKEN);
        e = app.testRequest(ServiceAction.POST, 1, "devices").getEntity();
        device2 = app.getEntityManager().get(e.getUuid(), Device.class);
        ns = getNotificationService();

        listener = new QueueListener(ns.getServiceManagerFactory(), ns.getEntityManagerFactory(), new Properties());
        listener.DEFAULT_SLEEP = 200;
        listener.start();
    }

    @After
    public void after(){
        if(listener!=null) {
            listener.stop();
            listener = null;
        }
    }

    @Test
    public void emptyPushNotification() throws Exception {

        app.clear();
        app.put("name", "foo");
        app.put("provider", PROVIDER);
        app.put("environment", "development");
        app.put("apiKey", API_KEY);
        Notifier n = (Notifier) app
                .testRequest(ServiceAction.POST, 1, "notifiers").getEntity()
                .toTypedEntity();

        app.clear();
        String payload = "Hello, World!";
        Map<String, String> payloads = new HashMap<String, String>(1);
        payloads.put("foo", payload);
        app.put("payloads", payloads);
        app.put("debug",true);
        app.put("queued", System.currentTimeMillis());

        Entity e = app.testRequest(ServiceAction.POST, 1,"devices",device1.getUuid(), "notifications")
                .getEntity();
        app.testRequest(ServiceAction.GET, 1, "notifications", e.getUuid());

        Notification notification = app.getEntityManager().get(e.getUuid(),
                Notification.class);

        // perform push //
        notification = scheduleNotificationAndWait(notification);
        checkReceipts(notification, 0);
    }

    @Test
    public void singlePushNotification() throws Exception {

        app.clear();
        String payload = "Hello, World!";
        Map<String, String> payloads = new HashMap<String, String>(1);
        payloads.put(notifier.getUuid().toString(), payload);
        app.put("payloads", payloads);
        app.put("queued", System.currentTimeMillis());
        app.put("debug",true);
        app.put("expire", System.currentTimeMillis() + 300000); // add 5 minutes to current time

        Entity e = app.testRequest(ServiceAction.POST, 1, "devices",device1.getUuid(),"notifications").getEntity();
        app.testRequest(ServiceAction.GET, 1, "notifications", e.getUuid());

        Notification notification = app.getEntityManager().get(e.getUuid(), Notification.class);
        assertEquals(
                notification.getPayloads().get(notifier.getUuid().toString()),
                payload);

        // perform push //
        notification = scheduleNotificationAndWait(notification);
        checkReceipts(notification, 2);
    }

    @Test
    public void singlePushNotificationViaUser() throws Exception {

        app.clear();

        // create user asdf
        app.put("username", "asdf");
        app.put("email", "asdf@adsf.com");
        User user = (User) app.testRequest(ServiceAction.POST, 1, "users").getEntity();
        assertNotNull(user);

        // post an existing device to user's devices collection
        Entity device = app.testRequest(ServiceAction.POST, 1, "users",  user.getUuid(), "devices", device1.getUuid()).getEntity();
        assertEquals(device.getUuid(), device1.getUuid());

        // create and post notification
        String payload = "Hello, World!";
        Map<String, String> payloads = new HashMap<String, String>(1);
        payloads.put(notifier.getUuid().toString(), payload);
        app.put("payloads", payloads);
        app.put("queued", System.currentTimeMillis());
        app.put("debug",true);
        Entity e = app.testRequest(ServiceAction.POST, 1,"users",user.getUuid(), "notifications").getEntity();
        app.testRequest(ServiceAction.GET, 1, "notifications", e.getUuid());

        setup.getEntityIndex().refresh(app.getId());

        // perform push //
        Notification notification = app.getEntityManager().get(e.getUuid(), Notification.class);
        notification = scheduleNotificationAndWait(notification);

        setup.getEntityIndex().refresh(app.getId());

        checkReceipts(notification, 1);
    }

    @Test
    public void twoBatchNotification() throws Exception {

        app.clear();
        String payload = "Hello, World!";
        Map<String, String> payloads = new HashMap<String, String>(1);
        payloads.put(notifier.getUuid().toString(), payload);
        app.put("payloads", payloads);
        app.put("queued", System.currentTimeMillis());
        app.put("debug",true);

        Entity e = app.testRequest(ServiceAction.POST, 1, "devices","notifications")   .getEntity();
        app.testRequest(ServiceAction.GET, 1, "notifications", e.getUuid());

        Notification notification = app.getEntityManager().get(e.getUuid(),  Notification.class);
        assertEquals(
                notification.getPayloads().get(notifier.getUuid().toString()),
                payload);

        // reduce Batch size to 1
        Field field = GCMAdapter.class.getDeclaredField("BATCH_SIZE");
        field.setAccessible(true);
        int multicastSize = field.getInt(GCMAdapter.class);
        try {
            field.setInt(GCMAdapter.class, 1);

            // perform push //
            notification = scheduleNotificationAndWait(notification);

            checkReceipts(notification, 2);
        } finally {
            field.setInt(GCMAdapter.class, multicastSize);
        }
    }


    @Test
    public void badPayloads() throws Exception {

        // bad payloads format

        app.clear();
        app.put("payloads", "{asdf}");
        app.put("debug",true);

        try {
            app.testRequest(ServiceAction.POST, 1,"devices",device1.getUuid(), "notifications");
            fail("invalid payload should have been rejected");
        } catch (IllegalArgumentException ex) {
            // ok
        }

        // bad notifier

        Map<String, String> payloads = new HashMap<String, String>(2);
        app.put("payloads", payloads);
        payloads.put("xxx", "");
        try {
            app.testRequest(ServiceAction.POST, 1,"devices",device1.getUuid(), "notifications");
            fail("invalid payload should have been rejected");
        } catch (IllegalArgumentException ex) {
            // ok
        }

        // payload too long

        // need the real provider for this one...
        app.clear();
        app.put("name", "gcm2");
        app.put("provider", "google");
        app.put("environment", "development");
        app.put("apiKey", API_KEY);
        Entity e = app.testRequest(ServiceAction.POST, 1, "notifiers")
                .getEntity();
        Notifier notifier2 = app.getEntityManager().get(e.getUuid(), Notifier.class);

        payloads.clear();
        StringBuilder sb = new StringBuilder();
        sb.append("{\"x\":\"");
        while (sb.length() < 4080) {
            sb.append("x");
        }
        sb.append("\"}");
        payloads.put(notifier2.getUuid().toString(), sb.toString());

        app.clear();
        app.put("payloads", payloads);
        app.put("debug",true);

        try {
            app.testRequest(ServiceAction.POST, 1, "devices",device1.getUuid(),"notifications");
            fail("invalid payload should have been rejected");
        } catch (Exception ex) {
            assertEquals("java.lang.IllegalArgumentException: GCM payloads must be 4096 characters or less",
                    ex.getMessage());
            // ok
        }
    }

    @Ignore("todo: how can I mock this?")
    @Test
    public void badToken() throws Exception {


        // create push notification //

        app.clear();
        String payload = "Hello, World!";
        Map<String, String> payloads = new HashMap<String, String>(1);
        payloads.put(notifier.getUuid().toString(), payload);
        app.put("payloads", payloads);
        app.put("queued", System.currentTimeMillis());
        app.put("debug",true);

        Entity e = app.testRequest(ServiceAction.POST, 1, "devices",device1.getUuid(),"notifications")
                .getEntity();
        app.testRequest(ServiceAction.GET, 1, "notifications", e.getUuid());

        Notification notification = app.getEntityManager().get(e.getUuid(),
                Notification.class);
        assertEquals(
                notification.getPayloads().get(notifier.getUuid().toString()),
                payload);

        // device w/ bad token
        app.clear();
        app.put(notifier.getName() + NOTIFIER_ID_POSTFIX, PUSH_TOKEN + "x");

        e = app.testRequest(ServiceAction.POST, 1, "devices").getEntity();
        device1 = app.getEntityManager().get(e.getUuid(), Device.class);

        ns.addDevice(notification, device1);

        // perform push //
        notification = scheduleNotificationAndWait(notification);

        List<EntityRef> receipts = getNotificationReceipts(notification);
        assertEquals(1, receipts.size());
        Receipt receipt = app.getEntityManager().get(receipts.get(0), Receipt.class);
        assertEquals("InvalidRegistration", receipt.getErrorCode());
    }

    @Ignore("todo: how can I mock this?")
    @Test
    public void badAPIKey() throws Exception {

        // create push notification //

        app.clear();
        String payload = "Hello, World!";
        Map<String, String> payloads = new HashMap<String, String>(1);
        payloads.put(notifier.getUuid().toString(), payload);
        app.put("payloads", payloads);
        app.put("queued", System.currentTimeMillis());
        app.put("debug",true);

        Entity e = app.testRequest(ServiceAction.POST, 1,"devices",device1.getUuid(), "notifications")
                .getEntity();
        app.testRequest(ServiceAction.GET, 1, "notifications", e.getUuid());

        Notification notification = app.getEntityManager().get(e.getUuid(),
                Notification.class);
        assertEquals(
                notification.getPayloads().get(notifier.getUuid().toString()),
                payload);

        ns.addDevice(notification, device1);

        // save bad API key
        app.getEntityManager().setProperty(notifier, "apiKey", API_KEY + "x");

        // perform push //

        // ns.getQueueManager().processBatchAndReschedule(notification, null);
        fail("Should have received a ConnectionException");
    }

}
