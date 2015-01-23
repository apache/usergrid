/*
 * Copyright 2014 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.usergrid.rest;

import org.apache.usergrid.rest.test.resource2point0.AbstractRestIT;
import org.apache.usergrid.rest.test.resource2point0.model.Collection;
import org.apache.usergrid.rest.test.resource2point0.model.Entity;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.usergrid.rest.test.resource2point0.model.QueryParameters;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 * Test creating, sending and paging through Notifications via the REST API.
 *
 */
public class NotificationsIT extends AbstractRestIT {

    public NotificationsIT() throws Exception { }

    private static final Logger logger = LoggerFactory.getLogger( NotificationsIT.class );

    private static final MetricRegistry registry = new MetricRegistry();
    private static final long writeDelayMs = 15;
    private static final long readDelayMs = 15;

    private Slf4jReporter reporter;

    @Before
    public void startReporting() {

        reporter = Slf4jReporter.forRegistry( registry ).outputTo( logger )
                .convertRatesTo( TimeUnit.SECONDS )
                .convertDurationsTo( TimeUnit.MILLISECONDS ).build();

        reporter.start( 10, TimeUnit.SECONDS );
    }


    @After
    public void printReport() {
        reporter.report();
        reporter.stop();
    }


    /**
     *
     * Creates notifiers and sends notifications
     *
     */
    @Test
    public void testPaging() throws Exception {

        int numDevices = 10;
        int numNotifications = 50; // to send to each device

        // create notifier
        Entity payload = new Entity();
        String notifierName = "mynotifier";
        String provider = "noop";
        payload.put("name", notifierName);
        payload.put("provider", provider);
        Entity entity = this.app().collection("notifier").post(payload);
        assertEquals(entity.get("provider"), provider);
        this.refreshIndex();

        // create devices
        int devicesCount = 0;
        List<String> deviceIds = new ArrayList();
        for (int i=0; i<numDevices; i++) {

            final int deviceNum = i;
            Entity devicePayload = new Entity();
            String deviceName = "device" + deviceNum;
            String deviceModel = "iPhone6+";
            String deviceOSVersion = "iOS 8";
            String devicePlatform = "Apple";
            String deviceOSVersionNum = "8";

            payload.put("name", deviceName);
            payload.put("deviceModel", deviceModel);
            payload.put("deviceModel", deviceModel);
            payload.put("deviceOSVersion", deviceOSVersion);
            payload.put("devicePlatform", devicePlatform);
            payload.put("deviceOSVersion", deviceOSVersionNum);
            Entity device = this.app().collection("devices").post(payload);

            assertEquals( deviceName, device.get("name"));
            deviceIds.add(device.getString("uuid"));
            devicesCount++;
        }
        this.refreshIndex();

        //
        String postMeterName = getClass().getSimpleName() + ".postNotifications";
        Meter postMeter = registry.meter(postMeterName);

        // send notifications
        int notificationCount = 0;
        List<String> notificationUuids = new ArrayList<String>();
        // send numNotifications to each deviceIds
        for (int i=0; i<numNotifications; i++) {
            for (int j=0; j<deviceIds.size(); j++) {

                final String deviceId = deviceIds.get(j);

                //'{"payloads":{"androidDev":"fdsafdsa"},"deliver":null}'
                Entity notifier = new Entity();
                String message = "Hello device " + deviceId;
                notifier.put(notifierName, message);
                Entity payloads = new Entity();
                payloads.put("payloads", notifier);
                payloads.put("deliver", null);
                Entity notification = this.app().collection("devices").uniqueID(deviceId).collection("notifications").post(payloads);

                postMeter.mark();

                Thread.sleep( writeDelayMs );

                notificationUuids.add( notification.getString("uuid"));
            }

            notificationCount++;

            if ( notificationCount % 100 == 0 ) {
                logger.debug("Created {} notifications", notificationCount);
            }
        }

        registry.remove( postMeterName );
        this.refreshIndex();

        // check that all the notifications have been sent and time them
        logger.info("Waiting for all notifications to be sent");
        StopWatch sw = new StopWatch();
        sw.start();
        boolean allSent = false;
        int i = 0;
        int maxRetries = 10;
        while (!allSent) {
            i++;
            Thread.sleep(100);
            int finished = pageThroughAllNotifications("FINISHED");
            if ( finished == (numDevices * numNotifications) ) {
                allSent = true;
            }
            if (i > maxRetries) {
                fail("could not page through all notificaitons in a reasonable amount of time.");
                break;
            }
        }
        sw.stop();
        int nc = numDevices * numNotifications;
        logger.info("Processed {} notifications in {}ms", nc, sw.getTime());
        logger.info("Processing Notifications throughput = {} TPS", ((float)nc) / (sw.getTime()/1000));
    }


    /**
     *
     * Helper method to page through and test all the notifications
     *
     */
    private int pageThroughAllNotifications( String state ) throws IOException, InterruptedException {

        //set up the query parameters and get the first page of data
        QueryParameters queryParameters = new QueryParameters();
        queryParameters.addParam("ql", "select * where state='" + state + "'");
        Collection collection = this.app().collection("notifications").get(queryParameters);
        int count = collection.getNumOfEntities();

        // since we have a cursor, we should have gotten the limit, which defaults to 10
        // or we should get back 0 which indicates no more data
        assertTrue( count == 10 || count == 0 );

        //now loop through the rest of the collection
        while (collection.hasCursor()) {

            collection = this.app().collection("notifications").getNextPage(collection, queryParameters, true);
            int returnCount = collection.getNumOfEntities();
            count += returnCount;
            //verify the return count of each response
            assertTrue( returnCount == 10 || returnCount == 0 );
            Thread.sleep( readDelayMs );
        }

        return count;
    }

}
