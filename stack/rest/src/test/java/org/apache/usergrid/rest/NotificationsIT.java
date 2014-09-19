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

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.MediaType;
import org.apache.commons.lang3.time.StopWatch;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test Notification end-points.
 */
public class NotificationsIT extends AbstractRestIT {
    private static final Logger logger = LoggerFactory.getLogger( NotificationsIT.class );
  
    @Test
    public void testBasicOperation() throws Exception {

        int numDevices = 10;
        int numNotifications = 50; // to send to each device

        String token = userToken( "ed@anuff.com", "sesame" );
        String org = "test-organization";
        String app = "test-app";
        String orgapp = org + "/" + app;

        // create notifier
        Map<String, Object> notifier = new HashMap<String, Object>() {{
            put("name", "mynotifier");
            put("provider", "noop");
        }};
        JsonNode notifierNode = mapper.readTree(resource().path( orgapp + "/notifier")
            .queryParam( "access_token", token )
            .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON )
            .post(String.class, notifier ) );

        logger.debug("Notifier is: " + notifierNode.toString());
        assertEquals( "noop", notifierNode.withArray("entities").get(0).get("provider").asText()); 
        
        refreshIndex( org, app );

        // create devices
        StopWatch sw = new StopWatch();
        sw.start();
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

            JsonNode deviceNode = mapper.readTree(resource().path( orgapp + "/devices")
                .queryParam( "access_token", token )
                .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON )
                .post(String.class, device ) );

            logger.debug("Device is: " + deviceNode.toString());
            assertEquals( "device"+i, deviceNode.withArray("entities").get(0).get("name").asText()); 

            deviceIds.add(deviceNode.withArray("entities").get(0).get("uuid").asText());
            devicesCount++;
        }
        sw.stop();
        logger.info("Created {} devices in {}ms", devicesCount, sw.getTime());

        refreshIndex( org, app );

        // send notifications 
        sw.reset();
        sw.start();
        int notificationCount = 0;
        for (int i=0; i<numNotifications; i++) {

            // send a notificaton to each device
            for (int j=0; j<deviceIds.size(); j++) {

                final String deviceId = deviceIds.get(j);
                Map<String, Object> notification = new HashMap<String, Object>() {{
                    put("payloads", new HashMap<String, Object>() {{
                        put("mynotifier", "Hello device " + deviceId);
                    }});
                }};

                JsonNode notificationNode = mapper.readTree(resource().path( orgapp + "/notifications")
                    .queryParam( "access_token", token )
                    .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON )
                    .post( String.class, notification ) );

                logger.debug("Notification is: " + notificationNode.toString());
            }
            notificationCount++;
        }
        sw.stop();
        logger.info("Created {} notifications in {}ms", notificationCount, sw.getTime());
        logger.info("Post Notification throughput = {} TPS", ((float)notificationCount) / (sw.getTime()/1000));

        refreshIndex( org, app );

        logger.info("Waiting for all notifications to be sent");
        sw.reset();
        sw.start();
        boolean allSent = false;
        while (!allSent) {

            Thread.sleep(1000); 

            JsonNode finishedNode = mapper.readTree( resource().path(orgapp + "/notifications")
                .queryParam("ql", "select * where state='FINISHED'")
                .queryParam("access_token", token)
                .accept(MediaType.APPLICATION_JSON)
                .get(String.class));

            int finished = finishedNode.get("count").asInt();
            if ( finishedNode.get("cursor") != null ) {
                String cursor = finishedNode.get("cursor").asText();
                while ( cursor != null ) {

                    JsonNode moreNode = mapper.readTree( resource().path(orgapp + "/notifications")
                        .queryParam("ql", "select * where state='FINISHED'")
                        .queryParam("access_token", token)
                        .queryParam("cursor", cursor)
                        .accept(MediaType.APPLICATION_JSON)
                        .get(String.class));

                    finished += moreNode.get("count").asInt();

                    if ( moreNode.get("cursor") != null ) {
                        cursor = moreNode.get("cursor").asText();
                    } else {
                        cursor = null;
                    }
                } 
            }

            if ( finished == (numDevices * numNotifications) ) {
                allSent = true;
            }
        }
        notificationCount = numDevices * numNotifications; 
        logger.info("Finished {} notifications in {}ms", notificationCount, sw.getTime());
        logger.info("Finished Notification throughput = {} TPS", ((float)notificationCount) / (sw.getTime()/1000));
    }
}
