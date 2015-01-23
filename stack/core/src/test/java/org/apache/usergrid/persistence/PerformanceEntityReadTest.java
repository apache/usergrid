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
package org.apache.usergrid.persistence;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.AbstractCoreIT;
import org.apache.usergrid.Application;
import org.apache.usergrid.CoreApplication;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;


//@RunWith(JukitoRunner.class)
//@UseModules({ GuiceModule.class })

@Ignore("Kills embedded cassandra")
public class PerformanceEntityReadTest extends AbstractCoreIT {
    private static final Logger logger = LoggerFactory.getLogger(PerformanceEntityReadTest.class );

    private static final MetricRegistry registry = new MetricRegistry();

    private static final long RUNTIME = TimeUnit.MINUTES.toMillis( 1 );

    private static final long writeDelayMs = 7;
    private static final long readDelayMs = 7;

    @Rule
    public Application app = new CoreApplication( setup );

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
    public void simpleReadUUID() throws Exception {

        logger.info("Starting simpleReadUUID()");

        final EntityManager em = app.getEntityManager();
        final long stopTime = System.currentTimeMillis() + RUNTIME;
        final Map<String, Object> entityMap = new HashMap<>();

        entityMap.put( "key1", 1000 );
        entityMap.put( "key2", 2000 );
        entityMap.put( "key3", "Some value" );

        List<UUID> uuids = new ArrayList<UUID>();

        int i = 0;
        while ( System.currentTimeMillis() < stopTime ) {

            entityMap.put( "key", i );
            final Entity created = em.create("testType", entityMap );

            uuids.add( created.getUuid() );

            i++;

            if ( i % 1000 == 0 ) {
                logger.debug("simpleReadUUID() Created {} entities",i );
            }
            Thread.sleep( writeDelayMs );
        }
        logger.info("simpleReadUUID() Created {} entities", i);

        final String meterName = this.getClass().getSimpleName() + ".simpleReadUUID";
        final Meter meter = registry.meter( meterName );

        for ( UUID uuid : uuids ) {
            Entity entity = em.get( uuid );
            meter.mark();
            Thread.sleep( readDelayMs );
        }

        registry.remove( meterName );
        logger.info("Finished simpleReadUUID()");
    }

    @Test
    public void simpleReadEntityRef() throws Exception {

        logger.info("Started simpleReadEnityRef()");

        final EntityManager em = app.getEntityManager();
        final long stopTime = System.currentTimeMillis() + RUNTIME;
        final Map<String, Object> entityMap = new HashMap<>();

        entityMap.put( "key1", 1000 );
        entityMap.put( "key2", 2000 );
        entityMap.put( "key3", "Some value" );

        List<EntityRef> entityRefs = new ArrayList<EntityRef>();

        int i = 0;
        while ( System.currentTimeMillis() < stopTime ) {

            entityMap.put( "key", i );
            final Entity created = em.create("testType", entityMap );

            entityRefs.add( new SimpleEntityRef( created.getType(), created.getUuid() ) );

            i++;

            if ( i % 1000 == 0 ) {
                logger.debug("simpleReadEntityRef() Created {} entities",i );
            }
            Thread.sleep( writeDelayMs );
        }
        logger.info("simpleReadEntityRef() Created {} entities", i);

        final String meterName = this.getClass().getSimpleName() + ".simpleReadEntityRef";
        final Meter meter = registry.meter( meterName );

        for ( EntityRef entityRef : entityRefs ) {
            Entity entity = em.get( entityRef );
            meter.mark();
            Thread.sleep( readDelayMs );
        }

        registry.remove( meterName );
        logger.info("Finished simpleReadEntityRef()");
    }
}
