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


import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
public class PerformanceEntityWriteTest extends AbstractCoreIT {
    private static final Logger LOG = LoggerFactory.getLogger( PerformanceEntityWriteTest.class );


    private static final MetricRegistry registry = new MetricRegistry();



    private static final long RUNTIME = TimeUnit.MINUTES.toMillis( 2 );


    @Rule
    public Application app = new CoreApplication( setup );

    private Slf4jReporter reporter;


    @Before
    public void startReporting() {

        reporter = Slf4jReporter.forRegistry( registry ).outputTo( LOG ).convertRatesTo( TimeUnit.SECONDS )
                              .convertDurationsTo( TimeUnit.MILLISECONDS ).build();

        reporter.start( 10, TimeUnit.SECONDS );
    }


    @After
    public void printReport() {
        reporter.report();
        reporter.stop();
    }



    @Test
    public void simpleCreate() throws Exception {


        final EntityManager em = app.getEntityManager();

        final long stopTime = System.currentTimeMillis() + RUNTIME;

        final Map<String, Object> addToCollectionEntity = new HashMap<>();

        addToCollectionEntity.put( "key1", 1000 );
        addToCollectionEntity.put( "key2", 2000 );
        addToCollectionEntity.put( "key3", "Some value" );

        int i = 0;


        final String meterName = this.getClass().getSimpleName() + ".simpleCreate";
        final Meter meter = registry.meter( meterName );

        while ( System.currentTimeMillis() < stopTime ) {

            addToCollectionEntity.put( "key", i );

            final Entity created = em.create( "testType", addToCollectionEntity );


            meter.mark();
            i++;
        }


        registry.remove( meterName );
    }


    @Test
    public void addToMultipleOwners() throws Exception {


        final EntityManager em = app.getEntityManager();

        final String meterName = this.getClass().getSimpleName() + ".addToMultipleOwners";
        final Meter meter = registry.meter( meterName );


        final long stopTime = System.currentTimeMillis() + RUNTIME;


        final DynamicEntity toCreate = new DynamicEntity();
        toCreate.setType( "toCreate" );


        //now create the first entity
        final Entity owner1 = em.create( "1owner", new HashMap<String, Object>() {{
            put( "key", "owner1" );
        }} );
        final Entity owner2 = em.create( "2owner", new HashMap<String, Object>() {{
            put( "key", "owner2" );
        }} );


        final Map<String, Object> addToCollectionEntity = new HashMap<>();
        addToCollectionEntity.put( "key1", 1000 );
        addToCollectionEntity.put( "key2", 2000 );
        addToCollectionEntity.put( "key3", "Some value" );

        final List<EntityRef> owners = Arrays.<EntityRef>asList( owner1, owner2 );

        int i = 0;


        while ( System.currentTimeMillis() < stopTime ) {

            addToCollectionEntity.put( "key", i );

            final Entity created = em.create( "testType", addToCollectionEntity );

            em.addToCollections( owners, "test", created );

            meter.mark();

            i++;
        }

        registry.remove( meterName );
    }
}
