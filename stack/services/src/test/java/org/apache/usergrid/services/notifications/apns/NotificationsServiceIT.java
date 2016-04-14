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
package org.apache.usergrid.services.notifications.apns;

import com.relayrides.pushy.apns.util.*;
import net.jcip.annotations.NotThreadSafe;
import org.apache.commons.io.IOUtils;
import org.apache.usergrid.persistence.*;
import org.apache.usergrid.persistence.entities.*;
import org.apache.usergrid.persistence.Query;
import org.apache.usergrid.services.exceptions.ForbiddenServiceOperationException;
import org.apache.usergrid.services.notifications.*;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.*;

import org.apache.usergrid.services.ServiceAction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import static org.apache.usergrid.services.notifications.impl.ApplicationQueueManagerImpl.NOTIFIER_ID_POSTFIX;


@NotThreadSafe
public class NotificationsServiceIT extends AbstractServiceNotificationIT {

    private static final Logger logger = LoggerFactory.getLogger(NotificationsServiceIT.class);

    /**
     * set to true to run tests against actual Apple servers - but they may not
     * all run correctly
     */
    private static final boolean USE_REAL_CONNECTIONS = false;
    private static final String PROVIDER = USE_REAL_CONNECTIONS ? "apple" : "noop";

    private static final String PUSH_TOKEN = "29026b5a4d2761ef13843e8bcab9fc83b47f1dfbd1d977d225ab296153ce06d6";

    private Notifier notifier;
    private Device device1, device2;
    private User user1, user2;
    private NotificationsService ns;
    private QueueListener listener;
    private String  notifierName = "apNs";

    @BeforeClass
    public static void setup(){

    }

    @Before
    public void before() throws Exception {

        // create apns notifier //
        app.clear();
        app.put("name", notifierName);
        app.put("provider",PROVIDER);
        app.put("environment", USE_REAL_CONNECTIONS ? "development" : "mock");
        InputStream fis = getClass().getClassLoader().getResourceAsStream( "pushtest_dev_recent.p12");
        byte[] certBytes = IOUtils.toByteArray(fis);
        app.put("p12Certificate", certBytes);
        fis.close();

        Entity e = app.testRequest(ServiceAction.POST, 1, "notifiers").getEntity();
        notifier = app.getEntityManager().get(e.getUuid(), Notifier.class);
        final String notifierKey = notifier.getName() + NOTIFIER_ID_POSTFIX;

        // create devices //

        app.clear();
        app.put(notifierKey, PUSH_TOKEN);
        app.put("name", "device1");
        e = app.testRequest(ServiceAction.POST, 1, "devices").getEntity();
        app.testRequest(ServiceAction.GET, 1, "devices", e.getUuid());
        device1 = app.getEntityManager().get(e.getUuid(), Device.class);
        assertEquals(device1.getProperty(notifierKey), PUSH_TOKEN);

        app.clear();
        app.put(notifierKey, PUSH_TOKEN);
        app.put("name", "device2");
        e = app.testRequest(ServiceAction.POST, 1, "devices").getEntity();
        device2 = app.getEntityManager().get(e.getUuid(), Device.class);
        Map<String, Object> props = app.getEntityManager().getProperties(e);
        assertEquals(device2.getProperty(notifierKey), PUSH_TOKEN);
                setup.getEntityIndex().refresh(app.getId());

        // create User
        user1 = new User();
        user1.setUsername("user1");
        user1.setEmail("user1@usergrid.org");
        user1 = app.getEntityManager().create(user1);

        // create User
        user2 = new User();
        user2.setUsername("user2");
        user2.setEmail("user2@usergrid.org");
        user2 = app.getEntityManager().create(user2);

        ns = getNotificationService();


                setup.getEntityIndex().refresh(app.getId());

        listener = new QueueListener(ns.getServiceManagerFactory(),ns.getEntityManagerFactory(), new Properties());
        listener.DEFAULT_SLEEP = 200;
        listener.start();
    }

    @After
    public void after() throws Exception {
        if(listener != null) {
            listener.stop();
        }
    }

    @Test
    public void singlePushNotification() throws Exception {


        app.clear();
        String payload = getPayload();
        Map<String, String> payloads = new HashMap<String, String>(1);
        payloads.put(notifier.getName(), payload);
        app.put("payloads", payloads);
        app.put("queued", System.currentTimeMillis());
        app.put("debug",true);
        app.put("expire", System.currentTimeMillis() + 300000); // add 5 minutes to current time

        // post notification to service manager
        Entity e = app.testRequest(ServiceAction.POST, 1,"devices",device1.getUuid(), "notifications").getEntity();
        app.testRequest(ServiceAction.GET, 1, "notifications", e.getUuid());

        // ensure notification has expected name
        Notification notification = app.getEntityManager().get(e.getUuid(), Notification.class);
        assertEquals(
                notification.getPayloads().get(notifier.getName().toString()),
                payload);


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
        app.put("receipts",false );
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
    public void pushWithNoValidDevicesShouldComplete() throws Exception {

        // create unrelated notifier

        app.clear();
        app.put("name", "gcm");
        app.put("provider", PROVIDER);
        app.put("environment", "development");
        app.put("apiKey", "xxx");
        InputStream fis = getClass().getClassLoader().getResourceAsStream( "pushtest_dev_recent.p12");
        byte[] certBytes = IOUtils.toByteArray(fis);
        app.put("p12Certificate", certBytes);

        app.testRequest(ServiceAction.POST, 1, "notifiers").getEntity().toTypedEntity();
        String key = "gcm" + NOTIFIER_ID_POSTFIX;

        // create unrelated device

        app.clear();
        app.put(key, PUSH_TOKEN);
        Entity e = app.testRequest(ServiceAction.POST, 1, "devices").getEntity();
        app.testRequest(ServiceAction.GET, 1, "devices", e.getUuid());

        Device device = app.getEntityManager().get(e.getUuid(), Device.class);
        assertEquals(device.getProperty(key), PUSH_TOKEN);

        // create push notification //

        app.clear();
        String payload = getPayload();


        Map<String, String> payloads = new HashMap<String, String>(1);
        payloads.put(notifier.getUuid().toString(), payload);
        app.put("payloads", payloads);
        app.put("started", System.currentTimeMillis());
        app.put("queued", System.currentTimeMillis());
        app.put("debug",true);

        e = app.testRequest(ServiceAction.POST, 1, "notifications").getEntity();
        app.testRequest(ServiceAction.GET, 1,  "notifications", e.getUuid());

        Notification notification = app.getEntityManager().get(e.getUuid(),  Notification.class);
        assertEquals(
                notification.getPayloads().get(notifier.getUuid().toString()),
                payload
        );


        // verify Query for CREATED state
        Query query =  Query.fromEquals( "state", Notification.State.FINISHED.toString() );
        Results results = app.getEntityManager().searchCollection(app.getEntityManager().getApplicationRef(), "notifications", query);
        Entity entity = results.getEntitiesMap().get(notification.getUuid());
        assertNotNull(entity);

        notificationWaitForComplete(notification);

        // perform push //

        //ns.getQueueManager().processBatchAndReschedule(notification, null);
        notification = app.getEntityManager().get(e.getUuid(), Notification.class);

        // verify Query for FINISHED state
        query = Query.fromEquals("state", Notification.State.FINISHED.toString());
        results = app.getEntityManager().searchCollection(app.getEntityManager().getApplicationRef(),
                "notifications", query);
        entity = results.getEntitiesMap().get(notification.getUuid());
        assertNotNull(entity);

        notification = (Notification) entity.toTypedEntity();
        checkReceipts(notification, 0);

        // Statistics are not accurate -- see https://issues.apache.org/jira/browse/USERGRID-1207
        //checkStatistics(notification, 0, 0);
    }

    @Test
    public void scheduledNotification() throws Exception {

        // create push notification //
        app.clear();
        String payload = getPayload();
        Map<String, String> payloads = new HashMap<String, String>(1);
        payloads.put(notifier.getUuid().toString(), payload);
        app.put("payloads", payloads);
        app.put("deliver", System.currentTimeMillis() + 240000);
        app.put("debug",true);

        Entity e = app.testRequest(ServiceAction.POST, 1,"devices",device1.getUuid(), "notifications")
                .getEntity();
        app.testRequest(ServiceAction.GET, 1, "notifications", e.getUuid());


        Notification notification = app.getEntityManager().get(e.getUuid(),
                Notification.class);
        assertEquals(
                notification.getPayloads().get(notifier.getUuid().toString()),
                payload);


        // delay until the scheduler has time to run
        Thread.sleep(500);

        notification = app.getEntityManager().get(e.getUuid(), Notification.class);
        assertEquals(Notification.State.SCHEDULED, notification.getState());


//        try {
//            e = app.testRequest(ServiceAction.DELETE, 1, "notifications",
//                    e.getUuid()).getEntity();
//        }catch (Exception deleteException){
//            LOG.error("Couldn't delete",deleteException);
//        }
//        app.getEntityManager().get(e.getUuid(), Notification.class);
    }

    @Test
    public void badPayloads() throws Exception {

        // bad payloads format

        app.clear();
        app.put("payloads", "{asdf}");
        app.put("debug",true);

        try {
            Entity e = app.testRequest(ServiceAction.POST, 1, "notifications")
                    .getEntity();
            fail("invalid payload should have been rejected");
        } catch (IllegalArgumentException ex) {
            // ok
        }

        // bad notifier

        Map<String, String> payloads = new HashMap<String, String>(2);
        app.put("payloads", payloads);
        payloads.put("xxx", "");
        try {
            Entity e = app.testRequest(ServiceAction.POST, 1,"devices",device1.getUuid(), "notifications")
                    .getEntity();
            fail("invalid payload should have been rejected");
        } catch (IllegalArgumentException ex) {
            // ok
        }

        // payload too long

        // need the real provider for this one...
        app.clear();
        app.put("name", "apNS2");
        app.put("provider", "apple");
        app.put("environment", "development");
        InputStream fis = getClass().getClassLoader().getResourceAsStream(
                "pushtest_dev_recent.p12");
        byte[] certBytes = IOUtils.toByteArray(fis);
        app.put("p12Certificate", certBytes);
        fis.close();
        Entity e = app.testRequest(ServiceAction.POST, 1, "notifiers")
                .getEntity();
        Notifier notifier2 = app.getEntityManager().get(e.getUuid(), Notifier.class);

        payloads.clear();
        StringBuilder sb = new StringBuilder();
        sb.append("{\"x\":\"");
        while (sb.length() < 2048) {
            sb.append("x");
        }
        sb.append("\"}");
        payloads.put(notifier2.getUuid().toString(), sb.toString());

        app.clear();
        app.put("payloads", payloads);

        try {
            app.testRequest(ServiceAction.POST, 1, "notifications").getEntity();
            fail("invalid payload should have been rejected");
        } catch (Exception ex) {
            assertEquals(ex.getMessage(),
                    "java.lang.IllegalArgumentException: Apple APNs payloads must be 2048 characters or less");
            // ok
        }
    }

    @Test
    public void badToken() throws Exception {

        // test adapter doesn't currently support this behavior
        if(!USE_REAL_CONNECTIONS){
            return;
        }

        // create push notification //
        app.clear();
        String payload = getPayload();
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

        // perform push //
        notification = notificationWaitForComplete(notification);

        // Statistics are not accurate -- see https://issues.apache.org/jira/browse/USERGRID-1207
        //checkStatistics(notification, 0, 1);

        notification = (Notification) app.getEntityManager().get(notification)
                .toTypedEntity();
        checkReceipts(notification, 1);
        List<EntityRef> receipts = getNotificationReceipts(notification);
        Receipt receipt = app.getEntityManager().get(receipts.get(0), Receipt.class);
        assertEquals(8, ((Long) receipt.getErrorCode()).longValue());
    }

    @Test
    public void twoDevicesOneNotifier() throws Exception {

        // create push notification //
        app.clear();
        String payload = getPayload();
        Map<String, String> payloads = new HashMap<String, String>(1);
        payloads.put(notifierName, payload);
        app.put("payloads", payloads);
        app.put("queued", System.currentTimeMillis());
        app.put("debug",true);

        Entity e = app.testRequest(ServiceAction.POST, 1, "devices","notifications").getEntity();
        app.testRequest(ServiceAction.GET, 1, "notifications", e.getUuid());

        Notification notification = app.getEntityManager().get(e.getUuid(),Notification.class);
        //assertEquals(notification.getPayloads().get(notifier.getUuid().toString()),payload);


        // perform push //
        notification = notificationWaitForComplete(notification);

        checkReceipts(notification, 2);
    }

    @Test
    public void twoDevicesTwoNotifiers() throws Exception {

        String notifier2Name = "apNs2";
        // create a 2nd notifier //
        app.clear();
        app.put("name", notifier2Name);
        app.put("provider", PROVIDER);
        app.put("environment", "development");
        InputStream fis = getClass().getClassLoader().getResourceAsStream(
                "pushtest_dev_recent.p12");
        byte[] certBytes = IOUtils.toByteArray(fis);
        app.put("p12Certificate", certBytes);
        fis.close();

        Entity e = app.testRequest(ServiceAction.POST, 1, "notifiers")
                .getEntity();
                setup.getEntityIndex().refresh(app.getId());

        app.testRequest(ServiceAction.GET, 1, "notifiers", "apNs2");

        Notifier notifier2 = app.getEntityManager().get(e.getUuid(), Notifier.class);
        assertEquals(notifier2.getName(), "apNs2");
        assertEquals(notifier2.getProvider(), PROVIDER);
        assertEquals(notifier2.getEnvironment(), "development");

        String key = notifierName + NOTIFIER_ID_POSTFIX;
        String key2 = notifier2Name + NOTIFIER_ID_POSTFIX;
        device2.setProperty(key, null);
        device2.setProperty(key2, PUSH_TOKEN);
        app.getEntityManager().update(device2);

                setup.getEntityIndex().refresh(app.getId());

        // create push notification //

        app.clear();
        String payload = getPayload();
        Map<String, String> payloads = new HashMap<String, String>(1);
        payloads.put(notifierName, payload);
        payloads.put(notifier2Name, payload);
        app.put("payloads", payloads);
        app.put("queued", System.currentTimeMillis());
        app.put("debug",true);

        e = app.testRequest(ServiceAction.POST, 1, "devices","notifications").getEntity();
        app.testRequest(ServiceAction.GET, 1, "notifications", e.getUuid());

                setup.getEntityIndex().refresh(app.getId());

        Notification notification = app.getEntityManager().get(e.getUuid(),  Notification.class);
        assertEquals(notification.getPayloads().get(notifierName), payload);

        // perform push //
        notification = notificationWaitForComplete(notification);

                setup.getEntityIndex().refresh(app.getId());

        checkReceipts(notification, 2); //the second notifier isn't associated correctly so its 3 instead of 4
    }

    @Test
    public void oneDeviceTwoNotifiers() throws Exception {

        // This test should configure 2 notifiers on a device and ensure that we can send to one of them

        // create a 2nd notifier //
        Object notifierName1 = "apNs2";
        Object environment1 = "development";

        app.clear();
        app.put("name", notifierName1);
        app.put("provider", PROVIDER);
        app.put("environment", environment1);
        InputStream fis = getClass().getClassLoader().getResourceAsStream(
                "pushtest_dev_recent.p12");
        byte[] certBytes = IOUtils.toByteArray(fis);
        app.put("p12Certificate", certBytes);
        fis.close();

        Entity e = app.testRequest(ServiceAction.POST, 1, "notifiers").getEntity();
        app.testRequest(ServiceAction.GET, 1, "notifiers", notifierName1);

        Notifier notifier2 = app.getEntityManager().get(e.getUuid(), Notifier.class);
        assertEquals(notifier2.getName(), notifierName1);
        assertEquals(notifier2.getProvider(), PROVIDER);
        assertEquals(notifier2.getEnvironment(), environment1);

        String key2 = notifier2.getName() + NOTIFIER_ID_POSTFIX;
        device1.setProperty(key2, PUSH_TOKEN);
        app.getEntityManager().update(device1);
        setup.getEntityIndex().refresh(app.getId()); // need to refresh the index after an update


        // create push notification //
        app.clear();
        String payload = getPayload();
        Map<String, String> payloads = new HashMap<String, String>(1);
        payloads.put(notifier.getUuid().toString(), payload);
        app.put("payloads", payloads);
        app.put("debug",true);

        Entity notificationEntity = app.testRequest(ServiceAction.POST, 1,"devices",device1.getUuid(),
            "notifications").getEntity();
        app.testRequest(ServiceAction.GET, 1, "notifications", notificationEntity.getUuid());

        Notification notification = app.getEntityManager().get(notificationEntity.getUuid(),
                Notification.class);
        assertEquals(
                notification.getPayloads().get(notifier.getUuid().toString()),
                payload);

        // perform push //
        notification = notificationWaitForComplete(notification);
        checkReceipts(notification, 1);
    }

    @Test
    public void badCertificate() throws Exception {


        // if we're not using real connections, then adding a notifier with bad cert is noop as certs don't exist
        // for the test adapter
        if(!USE_REAL_CONNECTIONS){
            return;
        }

        // create an apns notifier with the wrong certificate //
        app.clear();
        app.put("name", "prod_apns");
        app.put("provider", PROVIDER);
        app.put("environment", "development");

        InputStream fis = getClass().getClassLoader().getResourceAsStream("pushtest_dev_recent.p12");
        byte[] certBytes = IOUtils.toByteArray(fis);
        app.put("p12Certificate", certBytes);
        fis.close();

        Entity e = app.testRequest(ServiceAction.POST, 1, "notifiers")
                .getEntity();
        notifier = app.getEntityManager().get(e.getUuid(), Notifier.class);


        // create push notification //

        app.clear();
        String payload = getPayload();
        Map<String, String> payloads = new HashMap<String, String>(1);
        payloads.put(notifier.getUuid().toString(), payload);
        app.put("payloads", payloads);
        app.put("queued", System.currentTimeMillis());
        app.put("debug",true);

        e = app.testRequest(ServiceAction.POST, 1,"devices",device1.getUuid(), "notifications").getEntity();
        app.testRequest(ServiceAction.GET, 1, "notifications", e.getUuid());

        Notification notification = app.getEntityManager().get(e.getUuid(),
                Notification.class);
        assertEquals(
                notification.getPayloads().get(notifier.getUuid().toString()),
                payload);

        // perform push //
        try {
            notificationWaitForComplete(notification);
            fail("testConnection() should have failed");
        } catch (Exception ex) {
            // good, there should be an error
        }

        // verify Query for FAILED state
        Query query = Query.fromEquals("state", Notification.State.FAILED.toString());
        Results results = app.getEntityManager().searchCollection(
                app.getEntityManager().getApplicationRef(), "notifications", query);
        Entity entity = results.getEntitiesMap().get(notification.getUuid());
        assertNotNull(entity);
    }

    @Test
    public void inactiveDeviceUpdate() throws Exception {

        // test adapter doesn't currently support this
        if (!USE_REAL_CONNECTIONS) {
            return;
        }

        // create push notification //

        app.clear();
        String payload = getPayload();
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

        assertNotNull(device1.getProperty(notifier.getName()
                + NOTIFIER_ID_POSTFIX));
        assertNotNull(device2.getProperty(notifier.getName()
                + NOTIFIER_ID_POSTFIX));

        // perform push //
        notificationWaitForComplete(notification);

        // check provider IDs //

        device1 = app.getEntityManager().get(device1, Device.class);
        assertNull(device1 .getProperty(notifier.getName() + NOTIFIER_ID_POSTFIX));
        device2 = app.getEntityManager().get(device2, Device.class);
        assertNull(device2 .getProperty(notifier.getName() + NOTIFIER_ID_POSTFIX));
    }

    @Test
    public void singleUserMultipleDevices() throws Exception{

        app.testRequest(ServiceAction.POST, 1, "users",user1.getUuid(),"devices",device1.getUuid()).getEntity();
        app.testRequest(ServiceAction.POST, 1, "users",user1.getUuid(),"devices",device2.getUuid()).getEntity();

        List device1Users = app.getEntityManager().getCollection(device1,"users",null,100, Query.Level.REFS,false).getEntities();
        assertEquals(device1Users.size(),1);
        List user1Devices = app.getEntityManager().getCollection(user1,"devices",null,100, Query.Level.REFS,false).getEntities();
        assertEquals(user1Devices.size(),2);


        app.clear();
        // create a "hello world" notification
        String payload = getPayload();
        Map<String, String> payloads = new HashMap<String, String>(1);
        payloads.put(notifier.getName(), payload);
        app.put("payloads", payloads);
        app.put("debug",true);

        // post notification to service manager
        Entity e = app.testRequest(ServiceAction.POST, 1,"users",user1.getUuid(), "notifications").getEntity();
        // ensure notification it was created
        app.testRequest(ServiceAction.GET, 1, "notifications", e.getUuid());

        // ensure notification has expected name
        Notification notification = app.getEntityManager().get(e.getUuid(), Notification.class);
        assertEquals(
                notification.getPayloads().get(notifier.getName()),
                payload);

        // perform push //
        notification = notificationWaitForComplete(notification);
        checkReceipts(notification, 2);

        // Statistics are not accurate.  See - https://issues.apache.org/jira/browse/USERGRID-1207
        //checkStatistics(notification, 2, 0);

    }

    @Test
    public void batchTest() throws Exception {

        final int NUM_DEVICES = 50;
        // perform push //
        int oldBatchSize = QueueListener.MAX_TAKE;

        app.clear();
        app.put("name", UUID.randomUUID().toString());
        app.put("provider","noop");
        app.put("environment", "mock");
        Entity entity = app.testRequest(ServiceAction.POST, 1, "notifiers").getEntity();
        Notifier notifier = app.getEntityManager().get(entity.getUuid(), Notifier.class);
        final String notifierKey = notifier.getName() + NOTIFIER_ID_POSTFIX;
                setup.getEntityIndex().refresh(app.getId());

        // create a bunch of devices and add them to the notification

        for (int i = 0; i < NUM_DEVICES; i++) {
            app.clear();
            app.put(notifierKey, PUSH_TOKEN);
            app.put("name", "device"+i*10);
            app.testRequest(ServiceAction.POST, 1, "devices").getEntity();
                    setup.getEntityIndex().refresh(app.getId());

        }

        app.clear();
        String payload = getPayload();
        Map<String, String> payloads = new HashMap<String, String>(1);
        payloads.put(notifier.getUuid().toString(), payload);
        app.put("payloads", payloads);
        app.put("queued", System.currentTimeMillis());
        app.put("debug",true);

        // create a notification
        entity = app.testRequest(ServiceAction.POST, 1,  "devices","notifications").getEntity();
        app.testRequest(ServiceAction.GET, 1, "notifications", entity.getUuid());
                setup.getEntityIndex().refresh(app.getId());

        final Notification notification = (Notification) entity.toTypedEntity();

        try {
            notificationWaitForComplete(notification);
        } finally {
            //noop
        }

        // check receipts //
        checkReceipts(notification, NUM_DEVICES);

        // Statistics are not accurate.  See - https://issues.apache.org/jira/browse/USERGRID-1207
        // checkStatistics(notification, NUM_DEVICES, 0);
    }

    @Test
    public void testDeleteNotification() throws Exception {

        // create push notification //
        app.clear();
        String payload = getPayload();
        Map<String, String> payloads = new HashMap<String, String>(1);
        payloads.put(notifier.getName(), payload);
        app.put("payloads", payloads);
        app.put("debug", true);

        // stop the queue listener to it can't process the notification
        listener.stop();
        Entity sentNotification = app.testRequest(ServiceAction.POST, 1, "devices",device1.getUuid(), "notifications")
            .getEntity();

        // can't delete before it's finished
        try {
            app.testRequest(ServiceAction.DELETE, 1, "notifications", sentNotification.getUuid());
        } catch (Exception e) {
            assertEquals(e.getClass(), ForbiddenServiceOperationException.class);
        }

        // start the queue listener and send another notification that should complete properly
        listener.start();
        app.clear();
        app.put("payloads", payloads);
        app.put("debug", true);
        Entity sentNotification1 = app.testRequest(ServiceAction.POST, 1, "devices",device1.getUuid(), "notifications")
            .getEntity();
        Entity fetchedNotification = app.testRequest(ServiceAction.GET, 1, "notifications",
            sentNotification1.getUuid()).getEntity();

        Notification notification = app.getEntityManager().get(fetchedNotification.getUuid(),
            Notification.class);

        // perform push //
        notification = notificationWaitForComplete(notification);

        try {
            notification = app.getEntityManager().get(notification.getUuid(), Notification.class);
            assertEquals(Notification.State.FINISHED, notification.getState());

            //try to delete again after it is in the FINISHED STATE
            app.testRequest(ServiceAction.DELETE, 1, "notifications", notification.getUuid());
        } catch (Exception e) {
            fail("Delete should be successful after notification has finished.");
        }
    }

    private String getPayload(){
        ApnsPayloadBuilder builder = new ApnsPayloadBuilder();
        builder.setAlertBody("Hello, World!");
        builder.setSoundFileName("chime");
        return builder.buildWithDefaultMaximumLength();
    }


}
