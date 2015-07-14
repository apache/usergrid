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
import rx.Scheduler;
import rx.schedulers.Schedulers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;


public class ExportAppTest {
    static final Logger logger = LoggerFactory.getLogger( ExportAppTest.class );
    
    int NUM_COLLECTIONS = 20;
    int NUM_ENTITIES = 200; 
    int NUM_CONNECTIONS = 5;

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

        for (int i = 0; i < NUM_COLLECTIONS; i++) {

            final String type = "thing_" + i;
            em.createApplicationCollection( type );
            connectionCount.getAndIncrement();

            for (int j = 0; j < NUM_ENTITIES; j++) {
                final String name = "thing_" + j;
                final Entity source = em.create(
                        type, new HashMap<String, Object>() {{
                    put( "name", name );
                }} );
                entitiesCount.getAndIncrement();

                for (Entity target : connectedThings) {
                    em.createConnection( source, "has", target );
                    connectionCount.getAndIncrement();
                }
            }
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