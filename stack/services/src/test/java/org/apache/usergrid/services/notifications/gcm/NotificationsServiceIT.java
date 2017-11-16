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

import com.google.android.gcm.server.Constants;
import com.google.android.gcm.server.InvalidRequestException;
import net.jcip.annotations.NotThreadSafe;

import org.apache.usergrid.ServiceApplication;
import org.apache.usergrid.persistence.*;
import org.apache.usergrid.persistence.entities.*;
import org.apache.usergrid.persistence.index.query.CounterResolution;
import org.apache.usergrid.services.ServiceResults;
import org.apache.usergrid.services.notifications.*;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.*;

import org.apache.usergrid.services.ServiceAction;

import static org.junit.Assert.*;
import static org.apache.usergrid.services.notifications.ApplicationQueueManager.NOTIFIER_ID_POSTFIX;

@NotThreadSafe
public class NotificationsServiceIT extends AbstractServiceNotificationIT {


    private static final Logger logger = LoggerFactory
        .getLogger(NotificationsServiceIT.class);

    /**
     * set to true to use actual connections to GCM servers
     */
    private static final boolean USE_REAL_CONNECTIONS = true;
    private static final String PROVIDER = USE_REAL_CONNECTIONS ? "google" : "noop";

    private static final String API_KEY = "AIzaSyCIH_7WC0mOqBGMOXyQnFgrBpOePgHvQJM";
    private static final String PUSH_TOKEN = "APA91bGxRGnMK8tKgVPzSlxtCFvwSVqx0xEPjA06sBmiK0k"
        + "QsiwUt6ipSYF0iPRHyUgpXle0P8OlRWJADkQrcN7yxG4pLMg1CVmrqDu8tfSe63mZ-MRU2IW0cOhmo"
        + "sqzC9trl33moS3OvT7qjDjkP4Qq8LYdwwYC5A";

    private Notifier notifier;
    private Device device1, device2;
    private NotificationsService ns;
    private QueueListener listener;

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

        Entity e = app.testRequest(ServiceAction.POST, 1, "devices").getEntity();
        app.testRequest(ServiceAction.GET, 1, "devices", e.getUuid());

        device1 = app.getEntityManager().get(e.getUuid(), Device.class);
        assertEquals(device1.getProperty(key), PUSH_TOKEN);

        app.put(key, PUSH_TOKEN);
        e = app.testRequest(ServiceAction.POST, 1, "devices").getEntity();
        device2 = app.getEntityManager().get(e.getUuid(), Device.class);
        ns = getNotificationService();

        listener = new QueueListener(ns.getServiceManagerFactory(), ns.getEntityManagerFactory(), new Properties());
        listener.start();
    }

    @After
    public void after() {
        if (listener != null) {
            listener.stop();
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
        app.put("debug", true);
        app.put("queued", System.currentTimeMillis());

        Entity e = app.testRequest(ServiceAction.POST, 1, "devices", device1.getUuid(), "notifications")
            .getEntity();
        app.testRequest(ServiceAction.GET, 1, "notifications", e.getUuid());

        Notification notification = app.getEntityManager().get(e.getUuid(),
            Notification.class);

        // perform push //
        notification = notificationWaitForComplete(notification);
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
        app.put("debug", true);
        app.put("expire", System.currentTimeMillis() + 300000); // add 5 minutes to current time

        Entity e = app.testRequest(ServiceAction.POST, 1, "devices", device1.getUuid(), "notifications").getEntity();
        app.testRequest(ServiceAction.GET, 1, "notifications", e.getUuid());

        Notification notification = app.getEntityManager().get(e.getUuid(), Notification.class);
        assertEquals(
            notification.getPayloads().get(notifier.getUuid().toString()),
            payload);

        // perform push //
        notification = notificationWaitForComplete(notification);
        checkReceipts(notification, 1);
    }

    @Test
    public void singlePushNotificationMapPayload() throws Exception {

        app.clear();
        Map<String, Object> topLevel = new HashMap<>();
        Map<String, String> mapPayload = new HashMap<String, String>(){{
            put("key1", "value1");
            put("key2", "value2");

        }};
        topLevel.put("enabler", mapPayload);
        Map<String, Object> payloads = new HashMap<>(1);
        payloads.put(notifier.getUuid().toString(), topLevel);
        app.put("payloads", payloads);
        app.put("queued", System.currentTimeMillis());
        app.put("debug", true);
        app.put("expire", System.currentTimeMillis() + 300000); // add 5 minutes to current time

        Entity e = app.testRequest(ServiceAction.POST, 1, "devices", device1.getUuid(), "notifications").getEntity();
        app.testRequest(ServiceAction.GET, 1, "notifications", e.getUuid());

        Notification notification = app.getEntityManager().get(e.getUuid(), Notification.class);

        //assertEquals(
        //    notification.getPayloads().get(notifier.getUuid().toString()),
        //    payload);

        // perform push //
        notification = notificationWaitForComplete(notification);
        checkReceipts(notification, 1);
    }

    @Test
    public void singlePushNotificationNoReceipts() throws Exception {

        app.clear();
        String payload = "Hello, World!";
        Map<String, String> payloads = new HashMap<String, String>(1);
        payloads.put(notifier.getUuid().toString(), payload);
        app.put("payloads", payloads);
        app.put("queued", System.currentTimeMillis());
        app.put("debug", true);
        app.put("saveReceipts",false );
        app.put("expire", System.currentTimeMillis() + 300000); // add 5 minutes to current time

        Entity e = app.testRequest(ServiceAction.POST, 1, "devices", device1.getUuid(), "notifications").getEntity();
        app.testRequest(ServiceAction.GET, 1, "notifications", e.getUuid());

        Notification notification = app.getEntityManager().get(e.getUuid(), Notification.class);
        assertEquals(
            notification.getPayloads().get(notifier.getUuid().toString()),
            payload);

        // perform push //
        notification = notificationWaitForComplete(notification);
        checkReceipts(notification, 0);
    }

    @Test
    public void singlePushNotificationHighPriority() throws Exception {

        app.clear();
        String payload = "Hello, World!";
        Map<String, String> payloads = new HashMap<String, String>(1);
        payloads.put(notifier.getUuid().toString(), payload);
        app.put("payloads", payloads);
        app.put("queued", System.currentTimeMillis());
        app.put("debug", true);
        app.put("expire", System.currentTimeMillis() + 300000); // add 5 minutes to current time
        app.put("priority", "high");

        Entity e = app.testRequest(ServiceAction.POST, 1, "devices", device1.getUuid(), "notifications").getEntity();
        app.testRequest(ServiceAction.GET, 1, "notifications", e.getUuid());

        Notification notification = app.getEntityManager().get(e.getUuid(), Notification.class);
        assertEquals(
            notification.getPayloads().get(notifier.getUuid().toString()),
            payload);

        // perform push //
        notification = notificationWaitForComplete(notification);
        assertEquals("high", notification.getPriority());
        checkReceipts(notification, 1);
    }

    @Test
    public void singlePushNotificationWithInvalidPriority() throws Exception {

        app.clear();
        String payload = "Hello, World!";
        Map<String, String> payloads = new HashMap<String, String>(1);
        payloads.put(notifier.getUuid().toString(), payload);
        app.put("payloads", payloads);
        app.put("queued", System.currentTimeMillis());
        app.put("debug", true);
        app.put("expire", System.currentTimeMillis() + 300000); // add 5 minutes to current time
        app.put("priority", "not_a_priority");

        Entity e = app.testRequest(ServiceAction.POST, 1, "devices", device1.getUuid(), "notifications").getEntity();
        app.testRequest(ServiceAction.GET, 1, "notifications", e.getUuid());

        Notification notification = app.getEntityManager().get(e.getUuid(), Notification.class);
        assertEquals(
            notification.getPayloads().get(notifier.getUuid().toString()),
            payload);

        // perform push //
        notification = notificationWaitForComplete(notification);
        // if priority is invalid, it should default to normal
        assertEquals("normal", notification.getPriority());
        checkReceipts(notification, 1);
    }

    @Test
    public void singlePushNotificationWithMapPayload() throws Exception {

        app.clear();
        String payload = "{\"message\":\"Hello, World!\", \"campaign\":\"Hello Campaign\"}";
        Map<String, String> payloads = new HashMap<String, String>(1);
        payloads.put(notifier.getUuid().toString(), payload);
        app.put("payloads", payloads);
        app.put("queued", System.currentTimeMillis());
        app.put("debug", true);
        app.put("expire", System.currentTimeMillis() + 300000); // add 5 minutes to current time

        Entity e = app.testRequest(ServiceAction.POST, 1, "devices", device1.getUuid(), "notifications").getEntity();
        app.testRequest(ServiceAction.GET, 1, "notifications", e.getUuid());

        Notification notification = app.getEntityManager().get(e.getUuid(), Notification.class);
        assertEquals(
            notification.getPayloads().get(notifier.getUuid().toString()),
            payload);

        // perform push //
        notification = notificationWaitForComplete(notification);
        checkReceipts(notification, 1);
    }

    @Ignore("turn this on and run individually when you want to verify. there is an issue with the aggregate counter, because of all the other tests"
        + "AND, there is an issue with the batcher where we have to turn it up/down to see the results in time. ")
    @Test
    public void singlePushNotificationWithCounters() throws Exception {

        long ts = System.currentTimeMillis() - ( 24 * 60 * 60 * 1000 );
        app.clear();
        String payload = "Hello, World!";
        Map<String, String> payloads = new HashMap<String, String>(1);
        payloads.put(notifier.getUuid().toString(), payload);
        app.put("payloads", payloads);
        app.put("queued", System.currentTimeMillis());
        app.put("debug", false);
        app.put("expire", System.currentTimeMillis() + 300000); // add 5 minutes to current time

        Entity e = app.testRequest(ServiceAction.POST, 1, "devices", device1.getUuid(), "notifications").getEntity();
        app.testRequest(ServiceAction.GET, 1, "notifications", e.getUuid());

        Notification notification = app.getEntityManager().get(e.getUuid(), Notification.class);
        assertEquals(
            notification.getPayloads().get(notifier.getUuid().toString()),
            payload);

        // perform push //
        notification = notificationWaitForComplete(notification);
        //        checkReceipts(notification, 1);


        verifyNotificationCounter( notification,"completed",ts,1 );
        verifyNotificationCounter( notification,"failed",ts, 0 );




    }

    public void verifyNotificationCounter(Notification notification,String status,Long timestamp, int expected){

        Results countersResults = app.getEntityManager().getAggregateCounters( null,null,null,"counters.notifications."+notification.getUuid()+"."+status,
            CounterResolution.ALL,timestamp,System.currentTimeMillis(),false ) ;

        assertEquals( 1, countersResults.getCounters().size() );
        if(expected > 0) {
            assertEquals( expected, countersResults.getCounters().get( 0 ).getValues().get( 0 ).getValue() );
        }else if (expected == 0){
            assertEquals( 0,countersResults.getCounters().get( 0 ).getValues().size());
        }

        LocalDateTime localDateTime = LocalDateTime.now();
        StringBuilder currentDate = new StringBuilder(  );
        currentDate.append( "counters.notifications.aggregate."+status+"." );
        currentDate.append( localDateTime.getYear()+"." );
        currentDate.append( localDateTime.getMonth()+"." );
        currentDate.append( localDateTime.getDayOfMonth()); //+"." );

        countersResults = app.getEntityManager().getAggregateCounters( null,null,null,currentDate.toString(),
            CounterResolution.ALL,timestamp,System.currentTimeMillis(),false ) ;

        //checks to see that it exists
        assertEquals( 1, countersResults.getCounters().size() );
        if(expected > 0) {
            assertEquals( expected, countersResults.getCounters().get( 0 ).getValues().get( 0 ).getValue() );

        }
        else if (expected == 0){
            assertEquals( 0,countersResults.getCounters().get( 0 ).getValues().size());
        }

    }



    @Test
    public void singlePushNotificationMultipleDevices() throws Exception {

        app.clear();
        String payload = "Hello, World!";
        Map<String, String> payloads = new HashMap<String, String>(1);
        payloads.put(notifier.getUuid().toString(), payload);
        app.put("payloads", payloads);
        app.put("queued", System.currentTimeMillis());
        app.put("debug", true);
        app.put("expire", System.currentTimeMillis() + 300000); // add 5 minutes to current time

        Entity e = app.testRequest(ServiceAction.POST, 1, "devices", "*", "notifications").getEntity();
        app.testRequest(ServiceAction.GET, 1, "notifications", e.getUuid());

        Notification notification = app.getEntityManager().get(e.getUuid(), Notification.class);
        assertEquals(
            notification.getPayloads().get(notifier.getUuid().toString()),
            payload);

        // perform push //
        notification = notificationWaitForComplete(notification);
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
        Entity device = app.testRequest(ServiceAction.POST, 1, "users", user.getUuid(), "devices", device1.getUuid()).getEntity();
        assertEquals(device.getUuid(), device1.getUuid());

        // create and post notification
        String payload = "Hello, World!";
        Map<String, String> payloads = new HashMap<String, String>(1);
        payloads.put(notifier.getUuid().toString(), payload);
        app.put("payloads", payloads);
        app.put("queued", System.currentTimeMillis());
        app.put("debug", true);
        Entity e = app.testRequest(ServiceAction.POST, 1, "users", user.getUuid(), "notifications").getEntity();
        app.testRequest(ServiceAction.GET, 1, "notifications", e.getUuid());


        // perform push //
        Notification notification = app.getEntityManager().get(e.getUuid(), Notification.class);
        notification = notificationWaitForComplete(notification);

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
        app.put("debug", true);

        Entity e = app.testRequest(ServiceAction.POST, 1, "devices", "notifications").getEntity();
        app.testRequest(ServiceAction.GET, 1, "notifications", e.getUuid());

        Notification notification = app.getEntityManager().get(e.getUuid(), Notification.class);
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
            notification = notificationWaitForComplete(notification);

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
        app.put("debug", true);

        try {
            app.testRequest(ServiceAction.POST, 1, "devices", device1.getUuid(), "notifications");
            fail("invalid payload should have been rejected");
        } catch (IllegalArgumentException ex) {
            // ok
        }

        // bad notifier

        Map<String, String> payloads = new HashMap<String, String>(2);
        app.put("payloads", payloads);
        payloads.put("xxx", "");
        try {
            app.testRequest(ServiceAction.POST, 1, "devices", device1.getUuid(), "notifications");
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
        app.put("debug", true);

        try {
            app.testRequest(ServiceAction.POST, 1, "devices", device1.getUuid(), "notifications");
            fail("invalid payload should have been rejected");
        } catch (Exception ex) {
            assertEquals("java.lang.IllegalArgumentException: GCM payloads must be 4096 characters or less",
                ex.getMessage());
            // ok
        }
    }

    @Test
    public void badToken() throws Exception {

        // create device w/ bad token
        app.put(notifier.getName() + NOTIFIER_ID_POSTFIX, PUSH_TOKEN + "x");
        Entity badDeviceEntity = app.testRequest(ServiceAction.POST, 1, "devices").getEntity();
        Device badDevice = app.getEntityManager().get(badDeviceEntity.getUuid(), Device.class);

        // create notification payload
        app.clear();
        String payload = "Hello, World!";
        Map<String, String> payloads = new HashMap<String, String>(1);
        payloads.put(notifier.getUuid().toString(), payload);
        app.put("payloads", payloads);
        app.put("queued", System.currentTimeMillis());
        app.put("debug", true);

        // create push notification
        Entity e = app.testRequest(ServiceAction.POST, 1, "devices", badDevice.getUuid(), "notifications")
            .getEntity();

        // validate notification  was created successfully
        app.testRequest(ServiceAction.GET, 1, "notifications", e.getUuid());
        Notification notification = app.getEntityManager().get(e.getUuid(), Notification.class);
        assertEquals(
            notification.getPayloads().get(notifier.getUuid().toString()),
            payload);

        // wait for notification to be marked finished
        notification = notificationWaitForComplete(notification);

        // receipts are created and queried, wait a bit longer for this to happen as indexing
        app.waitForQueueDrainAndRefreshIndex(500);

        // get the receipts entity IDs
        List<EntityRef> receipts = getNotificationReceipts(notification);
        assertEquals(1, receipts.size());

        // Validate the error is the correct type InvalidRegistration
        Receipt receipt = app.getEntityManager().get(receipts.get(0), Receipt.class);
        assertEquals("InvalidRegistration", receipt.getErrorCode());
    }

    @Test
    public void createGoogleNotifierWithBadAPIKey() throws Exception {

        final String badKey = API_KEY + "bad";

        // create notifier with bad API key
        app.clear();
        app.put("name", "gcm_bad_key");
        app.put("provider", PROVIDER);
        app.put("environment", "development");
        app.put("apiKey", badKey);

        try {
            notifier = (Notifier) app
                .testRequest(ServiceAction.POST, 1, "notifiers").getEntity()
                .toTypedEntity();
        } catch (InvalidRequestException e) {
            assertEquals(Constants.ERROR_INVALID_REGISTRATION, e.getDescription());
        }

    }

    @Test
    public void sendNotificationWithBadAPIKey() throws Exception {
        final String badKey = API_KEY + "bad";

        // update an existing notifier with a bad API key
        app.clear();
        app.put("apiKey", badKey);
        notifier = (Notifier) app
            .testRequest(ServiceAction.PUT, 1, "notifiers", notifier.getUuid()).getEntity()
            .toTypedEntity();

        // create notification payload
        app.clear();
        String payload = "Hello, World!";
        Map<String, String> payloads = new HashMap<String, String>(1);
        payloads.put(notifier.getUuid().toString(), payload);
        app.put("payloads", payloads);
        app.put("queued", System.currentTimeMillis());
        app.put("debug", true);

        // create notification
        Entity e = app.testRequest(ServiceAction.POST, 1, "devices", device1.getUuid(), "notifications")
            .getEntity();


        // validate notification  was created successfully
        Notification notification = app.getEntityManager().get(e.getUuid(), Notification.class);
        assertEquals(
            notification.getPayloads().get(notifier.getUuid().toString()),
            payload);

        // wait for notification to be marked finished and retrieve it back
        notification = notificationWaitForComplete(notification);
        app.testRequest(ServiceAction.GET, 1, "notifications", e.getUuid());


        // receipts are created and queried, wait a bit longer for this to happen as indexing
        app.waitForQueueDrainAndRefreshIndex(2000);

        // get the receipts entity IDs
        List<EntityRef> receipts = getNotificationReceipts(notification);

        int retry = 30;
        while (receipts.size() == 0 && --retry >=0 ) {
            app.waitForQueueDrainAndRefreshIndex(1000);
            receipts = getNotificationReceipts(notification);
        }

        assertEquals(1, receipts.size());

        // Validate the error is the correct type InvalidRegistration
        Receipt receipt = app.getEntityManager().get(receipts.get(0), Receipt.class);
        assertEquals("InvalidRegistration", receipt.getErrorCode());

    }

}
