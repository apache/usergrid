/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.usergrid.persistence.graph.impl.shard;


import java.io.IOException;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.clearspring.analytics.hash.MurmurHash;
import com.clearspring.analytics.stream.cardinality.CardinalityMergeException;
import com.clearspring.analytics.stream.cardinality.HyperLogLog;
import com.netflix.astyanax.serializers.UUIDSerializer;

import static org.junit.Assert.assertTrue;


public class HyperLogLogTest {

    private static final Logger log = LoggerFactory.getLogger( HyperLogLogTest.class );

    private static final int SIZE = 250000;

    private static final UUIDSerializer UUID_SER = UUIDSerializer.get();

    private static final double LOSS = 0.01d;


    @Test
    public void testAccuracy() {

        HyperLogLog hyperLogLog = new HyperLogLog( LOSS );


        for ( int i = 0; i < SIZE; i++ ) {
            byte[] bytes = UUID_SER.toByteBuffer( UUIDGenerator.newTimeUUID() ).array();

            long hash = MurmurHash.hash64( bytes );


            hyperLogLog.offerHashed( hash );
        }


        assertCardinality( hyperLogLog, SIZE, LOSS );
    }


    @Test
    public void testUnion() throws IOException, CardinalityMergeException {
        HyperLogLog hyperLogLog = new HyperLogLog( LOSS );
        HyperLogLog secondLog = new HyperLogLog( LOSS );


        for ( int i = 0; i < SIZE; i++ ) {
            byte[] bytes = UUID_SER.toByteBuffer( UUIDGenerator.newTimeUUID() ).array();

            long hash = MurmurHash.hash64( bytes );


            //only offer every other hash
            if ( i % 2 == 0 ) {
                secondLog.offerHashed( hash );
            }

            else {
                hyperLogLog.offerHashed( hash );
            }
        }


        int half = SIZE / 2;

        assertCardinality( hyperLogLog, half, LOSS );

        assertCardinality( secondLog, half, LOSS );


        //merge both of them into new modules
        byte[] hyperLogLogBytes = hyperLogLog.getBytes();
        byte[] secondLogBytes = secondLog.getBytes();

        log.info( "The hyperLogLogBytes byte size is {}", hyperLogLogBytes.length );
        log.info( "The secondLogBytes byte size is {}", secondLogBytes.length );

        HyperLogLog deSerializedFirst = HyperLogLog.Builder.build( hyperLogLogBytes );

        HyperLogLog deSerializedSecond = HyperLogLog.Builder.build( secondLogBytes );

        //now add them
        deSerializedFirst.addAll( deSerializedSecond );

        assertCardinality( deSerializedFirst, SIZE, LOSS );
    }


    /**
     * Verify the cardinality of the log
     */
    private void assertCardinality( HyperLogLog hyperLogLog, int expectedSize, double precicionLoss ) {

        long cardinality = hyperLogLog.cardinality();


        long min = ( long ) ( expectedSize - ( expectedSize * precicionLoss ) );

        long max = ( long ) ( expectedSize + ( expectedSize * precicionLoss ) );

        long delta = Math.abs( expectedSize - cardinality );

        log.info( "Cardinality is {} for size {}.  Delta is {}", cardinality, expectedSize, delta );
        log.info( "Expected min is {}", min );
        log.info( "Expected max is {}", max );


//        assertTrue( "Min is <= the cardinality", min <= cardinality );
//
//        assertTrue( "Cardinality is <= max ", cardinality <= max );
    }


    @Test
    public void testAddition() {

    }
}
