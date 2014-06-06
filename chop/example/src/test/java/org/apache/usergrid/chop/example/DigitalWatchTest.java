/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.usergrid.chop.example;


import org.jukito.JukitoRunner;
import org.jukito.UseModules;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.apache.usergrid.chop.api.IterationChop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.chop.stack.ChopCluster;
import org.apache.usergrid.chop.stack.ICoordinatedCluster;
import org.apache.usergrid.chop.stack.Instance;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.assertEquals;


/**
 * Jukito (iteration) chopped digital watch test with member injection.
 */
@RunWith( JukitoRunner.class )
@UseModules( DigitalWatchModule.class )
@IterationChop( iterations = 10, threads = 4 )
public class DigitalWatchTest {
    private static final Logger LOG = LoggerFactory.getLogger( DigitalWatchTest.class );

    @ChopCluster( name = "TestCluster" )
    public static ICoordinatedCluster testCluster;


    @Test
    public void testCreation( Watch watch ) {
        assertNotNull( watch );
        assertFalse( watch.isDead() );
        assertEquals( Type.DIGITAL, watch.getType() );
    }


    @Test
    public void testBattery( Watch watch ) throws InterruptedException {
        assertFalse( watch.isDead() );
        while ( ! watch.isDead() ) {
            Thread.sleep( 1000L );
        }
        assertTrue( watch.isDead() );

        try {
            watch.getTime();
        }
        catch ( IllegalStateException e ) {
            LOG.debug( "Watch is dead, can't read the time." );
        }

        watch.addPowerSource( new RechargeableBattery() );
        assertFalse( watch.isDead() );
        watch.getTime();
    }


    @Test
    public void testCluster() {
        if( testCluster == null ) {
            LOG.info( "Test cluster is null, skipping testCluster()..." );
            return;
        }
        assertEquals( "TestCluster", testCluster.getName() );
        assertEquals( 2, testCluster.getSize() );
        assertEquals( 2, testCluster.getInstances().size() );

        for( Instance instance : testCluster.getInstances() ) {
            LOG.info( "Instance is at {} {}", instance.getPublicDnsName(), instance.getPublicIpAddress() );
        }
    }
}
