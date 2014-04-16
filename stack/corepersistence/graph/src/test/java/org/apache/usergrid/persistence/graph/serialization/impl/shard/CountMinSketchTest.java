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
package org.apache.usergrid.persistence.graph.serialization.impl.shard;


import java.util.Stack;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.clearspring.analytics.hash.MurmurHash;
import com.clearspring.analytics.stream.frequency.CountMinSketch;
import com.clearspring.analytics.stream.frequency.IFrequency;
import com.netflix.astyanax.serializers.UUIDSerializer;

import static org.junit.Assert.assertTrue;


public class CountMinSketchTest {

    private static final Logger log = LoggerFactory.getLogger( CountMinSketchTest.class );

    private static final int SIZE = 250000;

    private static final UUIDSerializer UUID_SER = UUIDSerializer.get();

    private static final double EPS_OF_TOTAL_COUNT = 0.0075;

    private static final double CONFIDENCE = 0.99;


    private static final int SEED = 7364181;


    @Test
    public void testAccuracy() {

        IFrequency baseSketch = new CountMinSketch( EPS_OF_TOTAL_COUNT, CONFIDENCE, SEED );

        byte[] bytes = UUID_SER.toByteBuffer( UUIDGenerator.newTimeUUID() ).array();

        long hash = MurmurHash.hash64( bytes );


        for ( int i = 0; i < SIZE; i++ ) {
            baseSketch.add( hash, 1 );
        }


        assertCardinality( baseSketch, hash, SIZE, SIZE, EPS_OF_TOTAL_COUNT );
    }


    @Test
    public void testUnion() throws Exception {
        CountMinSketch firstCounter = new CountMinSketch( EPS_OF_TOTAL_COUNT, CONFIDENCE, SEED );

        CountMinSketch secondCounter = new CountMinSketch( EPS_OF_TOTAL_COUNT, CONFIDENCE, SEED );


        byte[] bytes = UUID_SER.toByteBuffer( UUIDGenerator.newTimeUUID() ).array();

        long hash = MurmurHash.hash64( bytes );

        for ( int i = 0; i < SIZE; i++ ) {


            //only offer every other hash
            if ( i % 2 == 0 ) {
                firstCounter.add( hash, 1 );
            }

            else {
                secondCounter.add( hash, 1 );
            }
        }


        int half = SIZE / 2;

        assertCardinality( firstCounter, hash, half, half, EPS_OF_TOTAL_COUNT );

        assertCardinality( secondCounter, hash, half, half, EPS_OF_TOTAL_COUNT );


        //merge both of them into new modules
        byte[] firstCounterBytes = CountMinSketch.serialize( firstCounter );
        byte[] secondCounterBytes = CountMinSketch.serialize( secondCounter );

        log.info( "The hyperLogLogBytes byte size is {}", firstCounterBytes.length );
        log.info( "The secondCounterBytes byte size is {}", secondCounterBytes.length );

        CountMinSketch deSerializedFirst = CountMinSketch.deserialize( firstCounterBytes );

        CountMinSketch deSerializedSecond = CountMinSketch.deserialize( secondCounterBytes );


        //now add them
        CountMinSketch merged = CountMinSketch.merge( deSerializedFirst, deSerializedSecond );


        assertCardinality( merged, hash, SIZE, SIZE, EPS_OF_TOTAL_COUNT );
    }


    @Test
    @Ignore
    public void testLargeEntrySize() throws Exception {


        //create 10 billion unique entries
        final long uniqueEntrySize = 10000000000l;
        //        final long uniqueEntrySize = 1000000;


        /**
         * For each entry, increment its count this number of time
         */
        final int numCountIncrement = 10;

        /**
         * Increment by this amount every time
         */
        final int entryIncrement = 1000;

        /**
         * 2x our number of processors
         */
        final int concurrentWorkers = Runtime.getRuntime().availableProcessors() * 2;


        ExecutorService service = Executors.newFixedThreadPool( concurrentWorkers );


        final long countPerWorker = uniqueEntrySize / concurrentWorkers;


        final CountMinSketch counter = new CountMinSketch( EPS_OF_TOTAL_COUNT, CONFIDENCE, SEED );


        Stack<Future<Void>> futures = new Stack<Future<Void>>();

        final Object mutex = new Object();

        for ( int i = 0; i < concurrentWorkers; i++ ) {

            Future<Void> future = service.submit( new Callable<Void>() {

                @Override
                public Void call() throws Exception {

                    for ( long i = 0; i < countPerWorker; i++ ) {

                        byte[] bytes = UUID_SER.toByteBuffer( UUIDGenerator.newTimeUUID() ).array();

                        long hash = MurmurHash.hash64( bytes );

                        for ( int j = 0; j < numCountIncrement; j++ ) {
                            synchronized ( mutex ) {
                                counter.add( hash, entryIncrement );
                            }
                        }


                        assertCardinality( counter, hash, entryIncrement * numCountIncrement, uniqueEntrySize,
                                EPS_OF_TOTAL_COUNT );
                    }

                    return null;
                }
            } );

            futures.push( future );
        }


        /**
         * Block until done
         */
        for ( Future<Void> future : futures ) {
            future.get();
        }
    }


    /**
     * Verify the cardinality of the log
     */
    private void assertCardinality( IFrequency frequency, long hash, int expectedSize, long totalSize, double eps ) {


        long estimatedCount = frequency.estimateCount( hash );

        double totalDelta = totalSize * eps;

        long min = ( long ) ( expectedSize - totalDelta );

        long max = ( long ) ( expectedSize + totalDelta );

        long delta = Math.abs( expectedSize - estimatedCount );

        //        log.info( "Cardinality is {} for size {}.  Delta is {}", estimatedCount, expectedSize, delta );
        //        log.info( "Expected min is {}", min );
        //        log.info( "Expected max is {}", max );

        //
        //        if ( estimatedCount < min ) {
        //            log.warn( "Low estimated count.  Min expected is {} but was {}", min, estimatedCount );
        //        }
        //
        //        if ( estimatedCount > max ) {
        //            log.warn( "High estimated count.  Max expected is {} but was {}", max, estimatedCount );
        //        }

        //                assertTrue( String.format("Min %d is <= the cardinality %d", min, estimatedCount),
        // min <= estimatedCount );
        //
        //                assertTrue(String.format( "Cardinality %d is <= max %d ", estimatedCount, max),
        // estimatedCount <= max );

        assertTrue( String.format( "Min %d is <= the cardinality %d", min, estimatedCount ), min <= estimatedCount );

        assertTrue( String.format( "Cardinality %d is <= max %d ", estimatedCount, max ), estimatedCount <= max );
    }
}
