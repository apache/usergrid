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

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.core.MediaType;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.usergrid.rest.test.resource.*;
import org.apache.usergrid.rest.test.resource.endpoints.NamedResource;
import org.apache.usergrid.rest.test.resource.model.*;
import org.apache.usergrid.rest.test.resource.model.ApiResponse;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Test creating, sending and paging through Notifications via the REST API.
 */
public class NotificationsIT extends org.apache.usergrid.rest.test.resource.AbstractRestIT {
    private static final Logger logger = LoggerFactory.getLogger( NotificationsIT.class );

    private static final MetricRegistry registry = new MetricRegistry();

    final String org = "test-organization";
    final String app = "test-app";

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


    @Test
    public void testPaging() throws Exception {
        // create notifier
        Entity notifier = new Entity().chainPut("name", "mynotifier").chainPut("provider", "noop");

        ApiResponse notifierNode = this.pathResource(getOrgAppPath("notifier")).post(ApiResponse.class,notifier);

        //logger.debug("Notifier is: " + notifierNode.toString());
        assertEquals("noop", notifierNode.getEntities().get(0).get("provider").toString());

        int numDevices = 2;
        int numNotifications = 5; // to send to each device

        User user = new User("ed","ed", "ed@anuff.com", "sesame" );
        Entity entity = this.app().collection("users").post(user);
        Token token = this.app().token().post(new Token("ed", "sesame"));
        this.clientSetup.getRestClient().token().setToken(token);

        this.refreshIndex();

        // create devices
        int devicesCount = 0;
        List<String> deviceIds = new ArrayList();
        for (int i=0; i<numDevices; i++) {

            final int deviceNum = i;
            Map<String, Object> device = new HashMap<String, Object>() {{
                put("name", "device" + deviceNum);
                put("deviceModel", "iPhone6+");
                put("deviceOSVersion", "iOS 8");
                put("devicePlatform", "Apple");
                put("deviceOSVersion", "8");
                put("mynotifier.notifier.id", "pushtoken" + deviceNum);
            }};

            Entity deviceNode = this.app().collection( "devices").post(new Entity(device));

            //logger.debug("Device is: " + deviceNode.toString());
            assertEquals("device" + i, deviceNode.getResponse().getEntities().get(0).get("name").toString());

            deviceIds.add(deviceNode.getResponse().getEntities().get(0).get("uuid").toString());
            devicesCount++;
        }

        refreshIndex();

        String postMeterName = getClass().getSimpleName() + ".postNotifications";
        Meter postMeter = registry.meter( postMeterName );

        // send notifications
        int notificationCount = 0;
        List<String> notificationUuids = new ArrayList<String>();

        for (int i=0; i<numNotifications; i++) {

            // send a notificaton to each device
            for (int j=0; j<deviceIds.size(); j++) {

                final String deviceId = deviceIds.get(j);
                Map<String, Object> notification = new HashMap<String, Object>() {{
                    put("payloads", new HashMap<String, Object>() {{
                        put("mynotifier", "Hello device " + deviceId);
                    }});
                }};

                Entity notificationNode = this.app().collection( "notifications")
                    .post(new Entity(notification));

                postMeter.mark();


                //logger.debug("Notification is: " + notificationNode.toString());
                notificationUuids.add(notificationNode.getResponse().getEntities().get(0).get("uuid").toString());
            }

            notificationCount++;

            if ( notificationCount % 100 == 0 ) {
                logger.debug("Created {} notifications", notificationCount);
            }
        }
        registry.remove( postMeterName );

        refreshIndex( );

        logger.info("Waiting for all notifications to be sent");
        StopWatch sw = new StopWatch();
        sw.start();
        boolean allSent = false;
        int count = 0;
        while (!allSent) {

            Thread.sleep(100);
            int finished = pageThroughAllNotifications("FINISHED");
            if ( finished == (numDevices * numNotifications) ) {
                allSent = true;
            }
            count++;
            if(count>100){
                break;
            }
        }
        sw.stop();
        int nc = numDevices * numNotifications;
        logger.info("Processed {} notifications in {}ms", nc, sw.getTime());
        logger.info("Processing Notifications throughput = {} TPS", ((float)nc) / (sw.getTime()/1000));

        logger.info( "Successfully Paged through {} notifications",
            pageThroughAllNotifications("FINISHED"));
    }


    private int pageThroughAllNotifications( String state ) throws IOException, InterruptedException {

        Collection initialNode = this.app().collection("notifications")
            .get(new QueryParameters().setQuery("select * where state='" + state + "'"));

        int count = initialNode.getNumOfEntities();

        if (initialNode.getCursor() != null) {

            String cursor = initialNode.getCursor();

            // since we have a cursor, we should have gotten the limit, which defaults to 10
            // or we should get back 0 which indicates no more data
            assertTrue( count == 10 || count == 0 );

            while (cursor != null) {

                Collection anotherNode = this.app().collection("notifications")
                    .get(new QueryParameters().setQuery("select * where state='" + state + "'").setCursor(cursor));


                int returnCount = anotherNode.getNumOfEntities();

                count += returnCount;

                if (anotherNode.getCursor() != null) {

                    // since we have a cursor, we should have gotten the limit, which defaults to 10
                    // or we should get back 0 which indicates no more data
                    assertTrue( returnCount == 10 || returnCount == 0 );

                    cursor = anotherNode.getCursor();

                    Thread.sleep( readDelayMs );

                } else {
                    cursor = null;
                }

                if(returnCount<=0){
                    cursor = null;
                }
            }
        }
        return count;
    }
}
