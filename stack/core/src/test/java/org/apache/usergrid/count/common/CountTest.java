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
package org.apache.usergrid.count.common;


import org.junit.Test;

import static junit.framework.Assert.assertEquals;


/** Unit test for count object machinations */
public class CountTest {

    @Test
    public void testCounterName() {
        Count count = new Count( "Counters", "k1", "c1", 1 );
        assertEquals( "Counters:6b31:6331", count.getCounterName() );
    }


    @Test
    public void testApplyCount() {
        Count count = new Count( "Counters", "k1", "c1", 1 );
        Count c2 = new Count( "Counters", "k1", "c1", 1 );
        Count c3 = new Count( "Counters", "k1", "c1", 1 );
        count.apply( c2 ).apply( c3 );
        assertEquals( 3, count.getValue() );
    }


    @Test
    public void testApplyCountMixedTypes() {
        Count count = new Count( "Counters", 1, 3, 1 );
        Count c2 = new Count( "Counters", 1, 3, 1 );
        Count c3 = new Count( "Counters", 1, 3, 1 );
        count.apply( c3 ).apply( c2 );
        assertEquals( 3, count.getValue() );
    }


    @Test(expected = IllegalArgumentException.class)
    public void testApplyFail_onKeyname() {
        Count count = new Count( "Counters", "k1", "c1", 1 );
        Count c2 = new Count( "Coutenrs", "k2", "c1", 1 );
        count.apply( c2 );
    }


    @Test(expected = IllegalArgumentException.class)
    public void testApplyFail_onColumnname() {
        Count count = new Count( "Counters", "k1", "c1", 1 );
        Count c2 = new Count( "Counters", "k1", "c2", 1 );
        count.apply( c2 );
    }
}
