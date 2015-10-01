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


import org.junit.Test;

import org.apache.usergrid.persistence.index.query.CounterResolution;

import static org.junit.Assert.assertEquals;



public class UtilsTest {
    @Test
    public void testCounterResolution() throws Exception {
        assertEquals( CounterResolution.ALL, CounterResolution.fromString( "foo" ) );
        assertEquals( CounterResolution.MINUTE, CounterResolution.fromString( "MINUTE" ) );
        assertEquals( CounterResolution.MINUTE, CounterResolution.fromString( "minute" ) );
        assertEquals( CounterResolution.MINUTE, CounterResolution.fromString( "1" ) );
        assertEquals( CounterResolution.HALF_HOUR, CounterResolution.fromString( "30" ) );
        assertEquals( CounterResolution.HALF_HOUR, CounterResolution.fromString( "31" ) );
        assertEquals( CounterResolution.FIVE_MINUTES, CounterResolution.fromString( "29" ) );
        assertEquals( CounterResolution.HOUR, CounterResolution.fromString( "60" ) );
    }
}
