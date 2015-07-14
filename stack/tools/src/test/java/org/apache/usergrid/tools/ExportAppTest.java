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
package org.apache.usergrid.tools;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.usergrid.ServiceITSetup;
import org.apache.usergrid.ServiceITSetupImpl;
import org.apache.usergrid.ServiceITSuite;
import org.apache.usergrid.management.ApplicationInfo;
import org.apache.usergrid.management.OrganizationOwnerInfo;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityManager;
import org.junit.ClassRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;


public class ExportAppTest {
    static final Logger logger = LoggerFactory.getLogger( ExportAppTest.class );
    
    int NUM_COLLECTIONS = 5;
    int NUM_ENTITIES = 10; 
    int NUM_CONNECTIONS = 1;

    @ClassRule
    public static ServiceITSetup setup = new ServiceITSetupImpl( ServiceITSuite.cassandraResource );

    @org.junit.Test
    public void testBasicOperation() throws Exception {
       
        String rand = RandomStringUtils.randomAlphanumeric( 10 );
        
        // create app with some data

        OrganizationOwnerInfo orgInfo = setup.getMgmtSvc().createOwnerAndOrganization(
                "org_" + rand, "user_" + rand, rand.toUpperCase(), rand + "@example.com", rand );

        ApplicationInfo appInfo = setup.getMgmtSvc().createApplication(
                orgInfo.getOrganization().getUuid(), "app_" + rand );

        final EntityManager em = setup.getEmf().getEntityManager( appInfo.getId() );
        
        // create connected things

        final List<Entity> connectedThings = new ArrayList<Entity>();
        String connectedType = "connected_thing";
        em.createApplicationCollection(connectedType);
        for ( int j=0; j<NUM_CONNECTIONS; j++) {
            final String name = "connected_thing_" + j;
            connectedThings.add( em.create( connectedType, new HashMap<String, Object>() {{
                put( "name", name );
            }} ) );
        }
       
        // create collections of things, every other thing is connected to the connected things

        final AtomicInteger entitiesCount = new AtomicInteger(0);
        final AtomicInteger connectionCount = new AtomicInteger(0);

        ExecutorService execService = Executors.newFixedThreadPool( 50);
        final Scheduler scheduler = Schedulers.from( execService );

        Observable.range( 0, NUM_COLLECTIONS ).flatMap( new Func1<Integer, Observable<?>>() {
            @Override
            public Observable<?> call(Integer i) {
                
                return Observable.just( i ).doOnNext( new Action1<Integer>() {
                    @Override
                    public void call(Integer i) {
                        
                        final String type = "thing_"+i;
                        try {
                            em.createApplicationCollection( type );
                            connectionCount.getAndIncrement();
                            
                        } catch (Exception e) {
                            throw new RuntimeException( "Error creating collection", e );
                        }
                       
                        Observable.range( 0, NUM_ENTITIES ).flatMap( new Func1<Integer, Observable<?>>() {
                            @Override
                            public Observable<?> call(Integer j) {
                                return Observable.just( j ).doOnNext( new Action1<Integer>() {
                                    @Override
                                    public void call(Integer j) {
                                        
                                        final String name = "thing_" + j;
                                        try {
                                            final Entity source = em.create( 
                                                    type, new HashMap<String, Object>() {{ put("name", name); }});
                                            entitiesCount.getAndIncrement();
                                            logger.info( "Created entity {} type {}", name, type );
                                            
                                            for ( Entity target : connectedThings ) {
                                                em.createConnection( source, "has", target );
                                                connectionCount.getAndIncrement();
                                                logger.info( "Created connection from entity {} type {} to {}",
                                                        new Object[]{name, type, target.getName()} );
                                            }


                                        } catch (Exception e) {
                                            throw new RuntimeException( "Error creating collection", e );
                                        }
                                        
                                        
                                    }
                                    
                                } );

                            }
                        }, 50 ).subscribeOn( scheduler ).subscribe(); // toBlocking().last();
                        
                    }
                } );
                

            }
        }, 30 ).subscribeOn( scheduler ).toBlocking().last();

        while ( entitiesCount.get() < NUM_COLLECTIONS * NUM_ENTITIES ) {
            Thread.sleep( 5000 );
            logger.info( "Still working. Created {} entities and {} connections", 
                    entitiesCount.get(), connectionCount.get() );
        }

        logger.info( "Done. Created {} entities and {} connections", entitiesCount.get(), connectionCount.get() );

        long start = System.currentTimeMillis();
        
        String directoryName = "target/export" + rand;

        ExportApp exportApp = new ExportApp();
        exportApp.startTool( new String[]{
                "-application", appInfo.getName(),
                "-readThreads", "50",
                "-writeThreads", "10",
                "-host", "localhost:" + ServiceITSuite.cassandraResource.getRpcPort(),
                "-outputDir", directoryName
        }, false );
        
        logger.info("time = " + (System.currentTimeMillis() - start)/1000 + "s");


        
    }
}