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

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.codahale.metrics.MetricRegistry.name;


public class UniqueValuesIT {
    private static final Logger logger = LoggerFactory.getLogger( UniqueValuesIT.class );

    private static final AtomicInteger successCounter = new AtomicInteger( 0 );
    private static final AtomicInteger errorCounter = new AtomicInteger( 0 );
    private static final AtomicInteger dupCounter = new AtomicInteger( 0 );

    @Test
    public void testBasicOperation() throws Exception {

        int numThreads = 3;
        int poolsize = 40;
        int numUsers = 10;

        Multimap<String, Form> usersCreated = Multimaps.synchronizedMultimap( HashMultimap.create() );
        Multimap<String, Form> dupsRejected = Multimaps.synchronizedMultimap( HashMultimap.create() );

        ExecutorService execService = Executors.newFixedThreadPool( poolsize );

        Client client = ClientBuilder.newClient();

        final MetricRegistry metrics = new MetricRegistry();
        final Timer responses = metrics.timer(name(UniqueValuesIT.class, "responses"));
        long startTime = System.currentTimeMillis();

        final AtomicBoolean failed = new AtomicBoolean(false);

        String randomizer = RandomStringUtils.randomAlphanumeric( 20 );

        String[] targetHosts = {"http://localhost:9090","http://localhost:9090"};

        for (int i = 0; i < numUsers; i++) {

            if ( failed.get() ) { break; }

            // multiple threads simultaneously trying to create a user with the same propertyName
            for (int j = 0; j < numThreads; j++) {

                if ( failed.get() ) { break; }

                String username = "user_" + randomizer + "_" + i;
                final String host = targetHosts[ j % targetHosts.length ];

                execService.submit( () -> {

                    Form form = new Form();
                    form.param( "name", username );
                    form.param( "username", username );
                    form.param( "email", username + "@example.org" );
                    form.param( "password", "s3cr3t" );

                    Timer.Context time = responses.time();
                    try {
                        WebTarget target = client.target( host ).path( "/management/users" );

                        //logger.info("Posting user {} to host {}", propertyName, host);

                        Response response = target.request()
                            .post( Entity.entity( form, MediaType.APPLICATION_FORM_URLENCODED ));

                        if ( response.getStatus() == 200 || response.getStatus() == 201 ) {
                            usersCreated.put( username, form );
                            successCounter.incrementAndGet();

                        } else if ( response.getStatus() == 400 ) {
                            dupsRejected.put( username, form );
                            dupCounter.incrementAndGet();

                        } else {
                            String responseAsString = response.readEntity( String.class );
                            logger.error("User creation failed status {} message {}",
                                    response.getStatus(), responseAsString );
                            errorCounter.incrementAndGet();
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

        logger.info( "Dup count = {}", dupCounter.get() );

//        for ( String username : usersCreated.keys() ) {
//            System.out.println( username );
//            Collection<User> users = usersCreated.get( username );
//            for ( User user : users ) {
//                System.out.println("   " + user.getUuid() );
//            }
//        }

//        int count = 0;
//        for ( String username : dupsRejected.keySet() ) {
//            System.out.println( username );
//            Collection<User> users = dupsRejected.get( username );
//            for ( User user : users ) {
//                System.out.println("   " + (count++) + " rejected " + user.getUsername() + ":" + user.getUuid() );
//            }
//        }

        int userCount = 0;
        int usernamesWithDuplicates = 0;
        for ( String username : usersCreated.keySet() ) {
            Collection<Form> forms = usersCreated.get( username );
            if ( forms.size() > 1 ) {
                usernamesWithDuplicates++;
            }
            userCount++;
        }
        Assert.assertEquals( 0, usernamesWithDuplicates );
        Assert.assertEquals( 0, errorCounter.get() );
        Assert.assertEquals( numUsers, successCounter.get() );
        Assert.assertEquals( numUsers, userCount );


    }

}
