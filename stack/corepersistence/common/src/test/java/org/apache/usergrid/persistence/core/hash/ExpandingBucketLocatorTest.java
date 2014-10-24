/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one
 *  * or more contributor license agreements.  See the NOTICE file
 *  * distributed with this work for additional information
 *  * regarding copyright ownership.  The ASF licenses this file
 *  * to you under the Apache License, Version 2.0 (the
 *  * "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied.  See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 *
 */

package org.apache.usergrid.persistence.core.hash;


import java.util.Arrays;

import org.junit.Test;

import junit.framework.TestCase;


/**
 * Tests that we get consistent results when hashing
 */
public class ExpandingBucketLocatorTest extends TestCase {


    @Test
    public void testConsistency(){

        final ExpandingBucketLocator<String> expandingBucketLocator1 = new ExpandingBucketLocator<>( BucketLocatorTest.STRING_FUNNEL, 20, 10, 1 );
        final ExpandingBucketLocator<String> expandingBucketLocator2 = new ExpandingBucketLocator<>( BucketLocatorTest.STRING_FUNNEL, 20, 10, 1 );

        final String key = "mytestkey";


        int[] results1 = expandingBucketLocator1.getAllBuckets(key  );
        int[] results2 = expandingBucketLocator2.getAllBuckets(key  );

        assertTrue( "Same results returned", Arrays.equals(results1, results2));

        assertTrue("Within bounds", results1[0] <= 19);
        assertTrue("Within bounds", results1[1] <= 9);
        assertTrue("Within bounds", results1[2] <= 0);

        //test the first hash
        int newestBucket = expandingBucketLocator1.getCurrentBucket( key );

        assertEquals("Same bucket returned", results1[0], newestBucket);


    }
}
