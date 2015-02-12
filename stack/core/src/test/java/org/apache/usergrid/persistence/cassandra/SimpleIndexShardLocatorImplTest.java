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
package org.apache.usergrid.persistence.cassandra;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import org.apache.usergrid.persistence.IndexBucketLocator.IndexType;
import org.apache.usergrid.utils.UUIDUtils;

import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.core.TimerContext;

import static org.junit.Assert.assertEquals;


/** @author tnine */

public class SimpleIndexShardLocatorImplTest {
    @Test
    public void oneBucket() {

        UUID appId = UUIDUtils.newTimeUUID();
        String entityType = "user";
        String propName = "firstName";

        SimpleIndexBucketLocatorImpl locator = new SimpleIndexBucketLocatorImpl( 1 );

        List<String> buckets = locator.getBuckets( appId, IndexType.COLLECTION, entityType, propName );

        assertEquals( 1, buckets.size() );

        UUID testId1 = UUIDUtils.minTimeUUID( 0l );

        UUID testId2 = UUIDUtils.minTimeUUID( Long.MAX_VALUE / 2 );

        UUID testId3 = UUIDUtils.minTimeUUID( Long.MAX_VALUE );

        String bucket1 = locator.getBucket( appId, IndexType.COLLECTION, testId1, entityType, propName );

        String bucket2 = locator.getBucket( appId, IndexType.COLLECTION, testId2, entityType, propName );

        String bucket3 = locator.getBucket( appId, IndexType.COLLECTION, testId3, entityType, propName );

        assertEquals( bucket1, "000000000000000000000000000000000000000" );
        assertEquals( bucket2, "000000000000000000000000000000000000000" );
        assertEquals( bucket3, "000000000000000000000000000000000000000" );
    }


    @Test
    public void twoBuckets() {

        UUID appId = UUIDUtils.newTimeUUID();
        String entityType = "user";
        String propName = "firstName";

        SimpleIndexBucketLocatorImpl locator = new SimpleIndexBucketLocatorImpl( 2 );

        List<String> buckets = locator.getBuckets( appId, IndexType.COLLECTION, entityType, propName );

        assertEquals( 2, buckets.size() );

        UUID testId1 = UUIDUtils.minTimeUUID( 0l );

        UUID testId2 = UUIDUtils.maxTimeUUID( Long.MAX_VALUE / 2 );

        UUID testId3 = UUIDUtils.minTimeUUID( Long.MAX_VALUE );

        String bucket1 = locator.getBucket( appId, IndexType.COLLECTION, testId1, entityType, propName );

        String bucket2 = locator.getBucket( appId, IndexType.COLLECTION, testId2, entityType, propName );

        String bucket3 = locator.getBucket( appId, IndexType.COLLECTION, testId3, entityType, propName );

        assertEquals( bucket1, "000000000000000000000000000000000000000" );
        assertEquals( bucket2, "085070591730234615865843651857942052863" );
        assertEquals( bucket3, "000000000000000000000000000000000000000" );
    }


    @Test
    public void evenDistribution() {

        UUID appId = UUIDUtils.newTimeUUID();
        String entityType = "user";
        String propName = "firstName";

        int bucketSize = 20;
        float distributionPercentage = .05f;

        // test 100 elements
        SimpleIndexBucketLocatorImpl locator = new SimpleIndexBucketLocatorImpl( bucketSize );

        List<String> buckets = locator.getBuckets( appId, IndexType.COLLECTION, entityType, propName );

        assertEquals( bucketSize, buckets.size() );

        int testSize = 2000000;

        Map<String, Float> counts = new HashMap<String, Float>();

        final Timer hashes =
                Metrics.newTimer( SimpleIndexShardLocatorImplTest.class, "responses", TimeUnit.MILLISECONDS,
                        TimeUnit.SECONDS );

        // ConsoleReporter.enable(1, TimeUnit.SECONDS);

        /**
         * Loop through each new UUID and add it's hash to our map
         */
        for ( int i = 0; i < testSize; i++ ) {
            UUID id = UUIDUtils.newTimeUUID();

            final TimerContext context = hashes.time();

            String bucket = locator.getBucket( appId, IndexType.COLLECTION, id, entityType, propName );

            context.stop();

            Float count = counts.get( bucket );

            if ( count == null ) {
                count = 0f;
            }

            counts.put( bucket, ++count );
        }

        /**
         * Check each entry is within +- 5% of every subsequent entry
         */
        List<String> keys = new ArrayList<String>( counts.keySet() );
        int keySize = keys.size();

        assertEquals( bucketSize, keySize );

        for ( int i = 0; i < keySize; i++ ) {

            float sourceCount = counts.get( keys.get( i ) );

            for ( int j = i + 1; j < keySize; j++ ) {
                float destCount = counts.get( keys.get( j ) );

                // find the maximum allowed value for the assert based on the
                // largest value in the pair
                float maxDelta = Math.max( sourceCount, destCount ) * distributionPercentage;

                assertEquals(
                        String.format( "Not within %f as percentage for keys '%s' and '%s'", distributionPercentage,
                                keys.get( i ), keys.get( j ) ), sourceCount, destCount, maxDelta );
            }
        }
    }
}
