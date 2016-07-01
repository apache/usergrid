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
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.ConnectException;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.codahale.metrics.MetricRegistry.name;


/**
 * Tests that Catgrid will not allow creation of entities with duplicate names.
 *
 * Intended for use against a production-like cluster, not run during normal JUnit testing.
 *
 * Comment out the @Ignore annotation below and edit to add your target hosts.
 */
public class UniqueCatsIT {
    private static final Logger logger = LoggerFactory.getLogger( UniqueCatsIT.class );

    private static final AtomicInteger successCounter = new AtomicInteger( 0 );
    private static final AtomicInteger errorCounter = new AtomicInteger( 0 );
    private static final AtomicInteger dupCounter = new AtomicInteger( 0 );

    @Test
    //@Ignore("Intended for use against  prod-like cluster")
    public void testDuplicatePrevention() throws Exception {

        int numThreads = 20;
        int poolSize = 20;
        int numCats = 100;

        Multimap<String, String> catsCreated = Multimaps.synchronizedMultimap( HashMultimap.create() );
        Multimap<String, Map<String, Object>> dupsRejected = Multimaps.synchronizedMultimap( HashMultimap.create() );

        ExecutorService execService = Executors.newFixedThreadPool( poolSize );

        Client client = ClientBuilder.newClient();

        final MetricRegistry metrics = new MetricRegistry();
        final Timer responses = metrics.timer(name(UniqueCatsIT.class, "responses"));
        long startTime = System.currentTimeMillis();

        final AtomicBoolean failed = new AtomicBoolean(false);

        //String[] targetHosts = {"http://localhost:8080"};

        String[] targetHosts = {
            "https://ug21-west.e2e.apigee.net",
            "https://ug21-east.e2e.apigee.net"
        };

        for (int i = 0; i < numCats; i++) {

            if ( failed.get() ) { break; }

            String randomizer = RandomStringUtils.randomAlphanumeric( 8 );

            // multiple threads simultaneously trying to create a cat with the same propertyName
            for (int j = 0; j < numThreads; j++) {

                if ( failed.get() ) { break; }

                final String name = "uv_test_cat_" + randomizer;
                final String host = targetHosts[ j % targetHosts.length ];

                execService.submit( () -> {

                    Map<String, Object> form = new HashMap<String, Object>() {{
                        put("name", name);
                    }};

                    Timer.Context time = responses.time();
                    try {
                        WebTarget target = client.target( host ).path(
                            //"/test-organization/test-app/cats" );
                            "/dmjohnson/sandbox/cats" );

                        //logger.info("Posting cat {} to host {}", catname, host);

                        Response response = target.request()
                            //.post( Entity.entity( form, MediaType.APPLICATION_FORM_URLENCODED ));
                            .post( Entity.entity( form, MediaType.APPLICATION_JSON));

                        org.apache.usergrid.rest.test.resource.model.ApiResponse apiResponse = null;
                        String responseAsString = "";
                        if ( response.getStatus() >= 400 ) {
                            responseAsString = response.readEntity( String.class );
                        } else {
                            apiResponse = response.readEntity(
                                org.apache.usergrid.rest.test.resource.model.ApiResponse.class );
                        }

                        if ( response.getStatus() == 200 || response.getStatus() == 201 ) {
                            catsCreated.put( name, apiResponse.getEntity().getUuid().toString() );
                            successCounter.incrementAndGet();

                        } else if ( response.getStatus() == 400
                                && responseAsString.contains("DuplicateUniquePropertyExistsException")) {
                            dupsRejected.put( name, form );
                            dupCounter.incrementAndGet();

                        } else {
                            logger.error("Cat creation failed status {} message {}",
                                response.getStatus(), responseAsString );
                            errorCounter.incrementAndGet();
                        }

                    } catch ( ProcessingException e ) {
                        errorCounter.incrementAndGet();
                        if ( e.getCause() instanceof ConnectException ) {
                            logger.error("Error connecting to " + host);
                        } else {
                            logger.error( "Error", e );
                        }

                    } catch ( Exception e ) {
                        errorCounter.incrementAndGet();
                        logger.error("Error", e);
                    }
                    time.stop();

                } );
            }
        }
        execService.shutdown();

        try {
            while (!execService.awaitTermination( 60, TimeUnit.SECONDS )) {
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
                format.format( responses.getSnapshot().getMean() / 1000000000 )
        );

        logger.info( "Error count {} ratio = {}",
                errorCounter.get(), (float) errorCounter.get() / (float) responses.getCount() );

        logger.info( "Success count = {}", successCounter.get() );

        logger.info( "Rejected dup count = {}", dupCounter.get() );

//        for ( String catname : catsCreated.keys() ) {
//            System.out.println( catname );
//            Collection<Cat> cats = catsCreated.get( catname );
//            for ( Cat cat : cats ) {
//                System.out.println("   " + cat.getUuid() );
//            }
//        }

//        int count = 0;
//        for ( String catname : dupsRejected.keySet() ) {
//            System.out.println( catname );
//            Collection<Cat> cats = dupsRejected.get( catname );
//            for ( Cat cat : cats ) {
//                System.out.println("   " + (count++) + " rejected " + cat.getCatname() + ":" + cat.getUuid() );
//            }
//        }

        int catCount = 0;
        int catnamesWithDuplicates = 0;
        for ( String name : catsCreated.keySet() ) {
            //Collection<Map<String, String>> forms =
            Collection<String> forms = catsCreated.get( name );
            if ( forms.size() > 1 ) {
                catnamesWithDuplicates++;
                logger.info("Duplicate " + name);
            }
            catCount++;
        }
        Assert.assertEquals( 0, catnamesWithDuplicates );
        Assert.assertEquals( 0, errorCounter.get() );
        Assert.assertEquals( numCats, successCounter.get() );
        Assert.assertEquals( numCats, catCount );


    }

}
