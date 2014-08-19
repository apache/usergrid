/*******************************************************************************
 * Copyright 2012 Apigee Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.apache.usergrid.services.notifications.gcm;

import org.apache.usergrid.services.notifications.AbstractServiceNotificationTest;
import org.apache.usergrid.persistence.entities.Notification;
import org.apache.usergrid.persistence.entities.Notifier;
import org.apache.usergrid.persistence.entities.Receipt;
import org.apache.usergrid.services.notifications.NotificationsService;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.usergrid.services.notifications.ConnectionException;
import org.apache.usergrid.services.notifications.TaskTracker;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityRef;
import org.apache.usergrid.persistence.entities.Device;
import org.apache.usergrid.services.ServiceAction;

import static org.junit.Assert.*;
import static org.apache.usergrid.services.notifications.NotificationsService.NOTIFIER_ID_POSTFIX;

public class NotificationsServiceTest extends AbstractServiceNotificationTest {

    private static final Logger LOG = LoggerFactory
            .getLogger(NotificationsServiceTest.class);

    /**
     * set to true to run tests against actual GCM servers - but they may not
     * all run correctly
     */
    private static final boolean USE_REAL_CONNECTIONS = false;
    private static final String PROVIDER = USE_REAL_CONNECTIONS ? "google"
            : "noop";

    private static final String API_KEY = "AIzaSyCIH_7WC0mOqBGMOXyQnFgrBpOePgHvQJM";
    private static final String PUSH_TOKEN = "APA91bGxRGnMK8tKgVPzSlxtCFvwSVqx0xEPjA06sBmiK0kQsiwUt6ipSYF0iPRHyUgpXle0P8OlRWJADkQrcN7yxG4pLMg1CVmrqDu8tfSe63mZ-MRU2IW0cOhmosqzC9trl33moS3OvT7qjDjkP4Qq8LYdwwYC5A";

    private Notifier notifier;
    private Device device1, device2;
    private NotificationsService ns;

    @Override
    @Before
    public void before() throws Exception {
        super.before();

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

        Entity e = app.testRequest(ServiceAction.POST, 1, "devices")
                .getEntity();
        app.testRequest(ServiceAction.GET, 1, "devices", e.getUuid());

        device1 = app.getEm().get(e.getUuid(), Device.class);
        assertEquals(device1.getProperty(key), PUSH_TOKEN);

        app.put(key, PUSH_TOKEN);
        e = app.testRequest(ServiceAction.POST, 1, "devices").getEntity();
        device2 = app.getEm().get(e.getUuid(), Device.class);
        ns = getNotificationService();
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
        app.put("queued", System.currentTimeMillis());

        Entity e = app.testRequest(ServiceAction.POST, 1, "notifications")
                .getEntity();
        app.testRequest(ServiceAction.GET, 1, "notifications", e.getUuid());

        Notification notification = app.getEm().get(e.getUuid(),
                Notification.class);
        ns.addDevice(notification, device1);

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

        Entity e = app.testRequest(ServiceAction.POST, 1, "notifications")
                .getEntity();
        app.testRequest(ServiceAction.GET, 1, "notifications", e.getUuid());

        Notification notification = app.getEm().get(e.getUuid(),
                Notification.class);
        assertEquals(
                notification.getPayloads().get(notifier.getUuid().toString()),
                payload);

        ns.addDevice(notification, device1);

        // perform push //
        notification = scheduleNotificationAndWait(notification);
        checkReceipts(notification, 1);
    }

    @Test
    public void singlePushNotificationViaUser() throws Exception {

        app.clear();

        app.put("username", "asdf");
        app.put("email", "asdf@adsf.com");
        Entity user = app.testRequest(ServiceAction.POST, 1, "users")
                .getEntity();
        assertNotNull(user);
        Entity device = app.testRequest(ServiceAction.POST, 1, "users",
                user.getUuid(), "devices", device1.getUuid()).getEntity();
        assertEquals(device.getUuid(), device1.getUuid());

        String payload = "Hello, World!";
        Map<String, String> payloads = new HashMap<String, String>(1);
        payloads.put(notifier.getUuid().toString(), payload);
        app.put("payloads", payloads);
        app.put("queued", System.currentTimeMillis());

        Entity e = app.testRequest(ServiceAction.POST, 1, "notifications")
                .getEntity();
        app.testRequest(ServiceAction.GET, 1, "notifications", e.getUuid());

        Notification notification = app.getEm().get(e.getUuid(),
                Notification.class);
        ns.addDevice(notification, device1);

        // perform push //
        notification = scheduleNotificationAndWait(notification);
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

        Entity e = app.testRequest(ServiceAction.POST, 1, "notifications")
                .getEntity();
        app.testRequest(ServiceAction.GET, 1, "notifications", e.getUuid());

        Notification notification = app.getEm().get(e.getUuid(),
                Notification.class);
        assertEquals(
                notification.getPayloads().get(notifier.getUuid().toString()),
                payload);

        ns.addDevice(notification, device2);
        ns.addDevice(notification, device1);

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

    @Ignore
    // todo: how can I mock this?
    @Test
    public void providerIdUpdate() throws Exception {

        // mock action (based on verified actual behavior) //
        final String newProviderId = "newProviderId";
        ns.providerAdapters.put("google", new MockSuccessfulProviderAdapter() {
            @Override
            public void sendNotification(String providerId, Notifier notifier,
                    Object payload, Notification notification,
                    TaskTracker tracker) throws Exception {
                tracker.completed(newProviderId);
            }
        });

        // create push notification //

        app.clear();
        String payload = "Hello, World!";
        Map<String, String> payloads = new HashMap<String, String>(1);
        payloads.put(notifier.getUuid().toString(), payload);
        app.put("payloads", payloads);
        app.put("queued", System.currentTimeMillis());

        Entity e = app.testRequest(ServiceAction.POST, 1, "notifications")
                .getEntity();
        app.testRequest(ServiceAction.GET, 1, "notifications", e.getUuid());

        Notification notification = app.getEm().get(e.getUuid(),
                Notification.class);
        assertEquals(
                notification.getPayloads().get(notifier.getUuid().toString()),
                payload);

        ns.addDevice(notification, device1);

        // perform push //
        notification = scheduleNotificationAndWait(notification);
        checkReceipts(notification, 1);

        Device device = (Device) app.getEm().get(device1).toTypedEntity();
        assertEquals(newProviderId,
                device.getProperty(notifier.getName() + NOTIFIER_ID_POSTFIX));
    }

    @Test
    public void badPayloads() throws Exception {

        // bad payloads format

        app.clear();
        app.put("payloads", "{asdf}");

        try {
            app.testRequest(ServiceAction.POST, 1, "notifications");
            fail("invalid payload should have been rejected");
        } catch (IllegalArgumentException ex) {
            // ok
        }

        // bad notifier

        Map<String, String> payloads = new HashMap<String, String>(2);
        app.put("payloads", payloads);
        payloads.put("xxx", "");
        try {
            app.testRequest(ServiceAction.POST, 1, "notifications");
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
        Notifier notifier2 = app.getEm().get(e.getUuid(), Notifier.class);

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

        try {
            app.testRequest(ServiceAction.POST, 1, "notifications");
            fail("invalid payload should have been rejected");
        } catch (Exception ex) {
            assertEquals("java.lang.IllegalArgumentException: GCM payloads must be 4096 characters or less",
                    ex.getMessage());
            // ok
        }
    }

    @Ignore
    // todo: how can I mock this?
    @Test
    public void badToken() throws Exception {

        // mock action (based on verified actual behavior) //
        if (!USE_REAL_CONNECTIONS) {
            ns.providerAdapters.put("google",
                    new MockSuccessfulProviderAdapter() {
                        @Override
                        public void sendNotification(String providerId,
                                Notifier notifier, Object payload,
                                Notification notification, TaskTracker tracker)
                                throws Exception {
                            tracker.failed("InvalidRegistration",
                                    "InvalidRegistration");
                        }
                    });
        }

        // create push notification //

        app.clear();
        String payload = "Hello, World!";
        Map<String, String> payloads = new HashMap<String, String>(1);
        payloads.put(notifier.getUuid().toString(), payload);
        app.put("payloads", payloads);
        app.put("queued", System.currentTimeMillis());

        Entity e = app.testRequest(ServiceAction.POST, 1, "notifications")
                .getEntity();
        app.testRequest(ServiceAction.GET, 1, "notifications", e.getUuid());

        Notification notification = app.getEm().get(e.getUuid(),
                Notification.class);
        assertEquals(
                notification.getPayloads().get(notifier.getUuid().toString()),
                payload);

        // device w/ bad token
        app.clear();
        app.put(notifier.getName() + NOTIFIER_ID_POSTFIX, PUSH_TOKEN + "x");

        e = app.testRequest(ServiceAction.POST, 1, "devices").getEntity();
        device1 = app.getEm().get(e.getUuid(), Device.class);

        ns.addDevice(notification, device1);

        // perform push //
        notification = scheduleNotificationAndWait(notification);

        List<EntityRef> receipts = getNotificationReceipts(notification);
        assertEquals(1, receipts.size());
        Receipt receipt = app.getEm().get(receipts.get(0), Receipt.class);
        assertEquals("InvalidRegistration", receipt.getErrorCode());
    }

    @Ignore
    // todo: how can I mock this?
    @Test
    public void badAPIKey() throws Exception {

        if (!USE_REAL_CONNECTIONS) {
            // mock action (based on verified actual behavior) //
            ns.providerAdapters.put("google",
                    new MockSuccessfulProviderAdapter() {
                        @Override
                        public void sendNotification(String providerId,
                                Notifier notifier, Object payload,
                                Notification notification, TaskTracker tracker)
                                throws Exception {
                            Exception e = new IOException();
                            throw new ConnectionException(e.getMessage(), e);
                        }
                    });
        }

        // create push notification //

        app.clear();
        String payload = "Hello, World!";
        Map<String, String> payloads = new HashMap<String, String>(1);
        payloads.put(notifier.getUuid().toString(), payload);
        app.put("payloads", payloads);
        app.put("queued", System.currentTimeMillis());

        Entity e = app.testRequest(ServiceAction.POST, 1, "notifications")
                .getEntity();
        app.testRequest(ServiceAction.GET, 1, "notifications", e.getUuid());

        Notification notification = app.getEm().get(e.getUuid(),
                Notification.class);
        assertEquals(
                notification.getPayloads().get(notifier.getUuid().toString()),
                payload);

        ns.addDevice(notification, device1);

        // save bad API key
        app.getEm().setProperty(notifier, "apiKey", API_KEY + "x");

        // perform push //

        try {
            ns.getQueueManager().processBatchAndReschedule(notification, null);
            fail("Should have received a ConnectionException");
        } catch (ConnectionException ex) {
            // good
        }
    }

    @Ignore("Run only if you need to.")
    @Test
    public void loadTest() throws Exception {

        final int NUM_DEVICES = 10000;

        // create notification //

        HashMap<String, Object> properties = new LinkedHashMap<String, Object>();
        String payload = "Hello, World!";
        Map<String, String> payloads = new HashMap<String, String>(1);
        payloads.put(notifier.getUuid().toString(), payload);
        properties.put("payloads", payloads);
        properties.put("queued", System.currentTimeMillis());

        Entity e = app.testRequest(ServiceAction.POST, 1, "notifications")
                .getEntity();
        app.testRequest(ServiceAction.GET, 1, "notifications", e.getUuid());

        Notification notification = app.getEm().get(e.getUuid(),
                Notification.class);
        assertEquals(
                notification.getPayloads().get(notifier.getUuid().toString()),
                payload);

        // create a bunch of devices and add them to the notification
        properties = new LinkedHashMap<String, Object>();
        properties.put(notifier.getName() + NOTIFIER_ID_POSTFIX, PUSH_TOKEN);
        for (int i = 0; i < NUM_DEVICES; i++) {
            Entity entity = app.getEm().create("device", properties);
            ns.addDevice(notification, entity);
        }

        long time = System.currentTimeMillis();
        LOG.error("START DELIVERY OF {} NOTIFICATIONS", NUM_DEVICES);

        // perform push //
        notification = scheduleNotificationAndWait(notification);
        LOG.error("END DELIVERY OF {} NOTIFICATIONS ({})", NUM_DEVICES,
                System.currentTimeMillis() - time);

        // check receipts //
        checkReceipts(notification, NUM_DEVICES);
        checkStatistics(notification, NUM_DEVICES, 0);
    }

    // @Test
    // public void inactiveDeviceUpdate() throws Exception {
    //
    // if (!USE_REAL_CONNECTIONS) {
    // // mock action (based on verified actual behavior) //
    // NotificationsService.providerAdapters.put("apple", new
    // MockSuccessfulProviderAdapter() {
    // public Map<String,Date> getInactiveDevices(Notifier notifier,
    // EntityManager em) throws Exception {
    // return Collections.singletonMap(PUSH_TOKEN, new Date());
    // }
    // });
    // }
    //
    // // create push notification //
    //
    // HashMap<String, Object> properties = new LinkedHashMap<String, Object>();
    // String payload =
    // APNS.newPayload().alertBody("Hello, World!").sound("chime").build();
    // Map<String, String> payloads = new HashMap<String, String>(1);
    // payloads.put(notifier.getUuid().toString(), payload);
    // properties.put("payloads", payloads);
    // properties.put("queued", System.currentTimeMillis());
    //
    // Entity e = testRequest(sm, ServiceAction.POST, 1, properties,
    // "notifications").getEntity();
    // testRequest(sm, ServiceAction.GET, 1, null, "notifications",
    // e.getUuid());
    //
    // Notification notification = em.get(e.getUuid(), Notification.class);
    // assertEquals(notification.getPayloads().get(notifier.getUuid().toString()),
    // payload);
    //
    // ns.addDevice(notification, device1);
    // ns.addDevice(notification, device2);
    //
    // assertNotNull(device1.getProperty(notifier.getName() +
    // NOTIFIER_ID_POSTFIX));
    // assertNotNull(device2.getProperty(notifier.getName() +
    // NOTIFIER_ID_POSTFIX));
    //
    // // perform push //
    // notification = scheduleNotificationAndWait(notification);
    //
    // // check provider IDs //
    //
    // device1 = em.get(device1, Device.class);
    // assertNull(device1.getProperty(notifier.getName() +
    // NOTIFIER_ID_POSTFIX));
    // device2 = em.get(device2, Device.class);
    // assertNull(device2.getProperty(notifier.getName() +
    // NOTIFIER_ID_POSTFIX));
    // }
}
