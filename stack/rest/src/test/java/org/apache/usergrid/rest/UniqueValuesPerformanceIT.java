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
package org.apache.usergrid.rest;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.text.DecimalFormat;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.codahale.metrics.MetricRegistry.name;


/**
 * Unique values performance test, not intended for use in normal JUnit testing.
 *
 * Just tests how fast we can create users, optionally against multiple end-points.
 * (for multi-region testing).
 */
public class UniqueValuesPerformanceIT {
    private static final Logger logger = LoggerFactory.getLogger( UniqueValuesPerformanceIT.class );

    private static final AtomicInteger successCounter = new AtomicInteger( 0 );
    private static final AtomicInteger errorCounter = new AtomicInteger( 0 );


    @Test
    @Ignore("Intended for use against  prod-like cluster")
    public void testBasicOperation() throws Exception {

        int numUsers = 1000;
        int numThreads = 1000;
        int poolsize = 50;

        ExecutorService execService = Executors.newFixedThreadPool( poolsize );

        Multimap<String, Form> usersCreated =
                Multimaps.synchronizedListMultimap( ArrayListMultimap.create( numUsers, 1 ) );

        Client client = ClientBuilder.newClient();

        String randomizer = RandomStringUtils.randomAlphanumeric( 8 );

        String[] targetHosts = {"http://localhost:8080","http://localhost:9090"};

        final MetricRegistry metrics = new MetricRegistry();
        final Timer responses = metrics.timer( name( UniqueValuesPerformanceIT.class, "responses" ) );
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < numThreads; i++) {

            execService.submit( () -> {

                for (int j = 0; j < numUsers / numThreads; j++) {

                    // every user gets unique name, no duplicates in this test
                    UUID uuid = UUID.randomUUID();
                    String username = "uv_test_user_" + uuid;

                    Form form = new Form();
                    form.param( "name", username );
                    form.param( "username", username );
                    form.param( "email", username + "@example.org" );
                    form.param( "password", "s3cr3t" );

                    Timer.Context time = responses.time();
                    try {
                        final String host = targetHosts[j % targetHosts.length];
                        WebTarget target = client.target( host ).path( "/management/users" );

                        Response response = target.request()
                            .post( Entity.entity( form, MediaType.APPLICATION_FORM_URLENCODED ));

                        if (response.getStatus() == 200 || response.getStatus() == 201) {
                            usersCreated.put( username, form);
                            successCounter.incrementAndGet();

                        } else {
                            String responseBody = response.readEntity( String.class );
                            logger.error( "User creation failed status {} - {}", response.getStatus(), responseBody );
                            errorCounter.incrementAndGet();
                        }

                    } catch (Exception e) {
                        errorCounter.incrementAndGet();
                        logger.error( "Error", e );
                    }
                    time.stop();
                }

            } );
        }
        execService.shutdown();

        try {
            while (!execService.awaitTermination( 30, TimeUnit.SECONDS )) {
                System.out.println( "Waiting..." );
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        long endTime = System.currentTimeMillis();

        logger.info( "Total time {}s", (endTime - startTime) / 1000 );

        DecimalFormat format = new DecimalFormat("##.###");

        logger.info( "Timed {} requests:\n" +
                "mean rate {}/s\n" +
                "min       {}s\n" +
                "max       {}s\n" +
                "mean      {}s",
                responses.getCount(),
                format.format( responses.getMeanRate() ),
                format.format( (double)responses.getSnapshot().getMin()  / 1000000000 ),
                format.format( (double)responses.getSnapshot().getMax()  / 1000000000 ),
                format.format( responses.getSnapshot().getMean()         / 1000000000 )
        );

        logger.info( "Error count {} ratio = {}",
                errorCounter.get(), (float) errorCounter.get() / (float) responses.getCount() );

        logger.info( "Success count = {}",
                successCounter.get() );


        Assert.assertEquals( 0, errorCounter.get() );
        Assert.assertEquals( numUsers, successCounter.get() );
    }
}
