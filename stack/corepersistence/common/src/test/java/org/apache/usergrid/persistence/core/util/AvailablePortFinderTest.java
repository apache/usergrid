/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.usergrid.persistence.core.util;


import java.util.Set;

import org.junit.Test;


import static org.junit.Assert.assertTrue;

public class AvailablePortFinderTest {

    /**
     * Test of getAvailablePorts method, of class AvailablePortFinder.
     */
    @Test
    public void testGetAvailablePorts_0args() {
        Set<Integer> result = AvailablePortFinder.getAvailablePorts();
        assertTrue( !result.isEmpty() );
    }

    /**
     * Test of getNextAvailable method, of class AvailablePortFinder.
     */
    @Test
    public void testGetNextAvailable_0args() {
        int result = AvailablePortFinder.getNextAvailable();
        assertTrue( result > 0 );
    }

    /**
     * Test of getNextAvailable method, of class AvailablePortFinder.
     */
    @Test
    public void testGetNextAvailable_int() {
        int result = AvailablePortFinder.getNextAvailable( 2000 );
        assertTrue( result >= 2000 );
    }

    /**
     * Test of available method, of class AvailablePortFinder.
     */
    @Test
    public void testAvailable() {
        int port = 2000;
        boolean result = AvailablePortFinder.available( port );
        assertTrue( result );
    }

    /**
     * Test of getAvailablePorts method, of class AvailablePortFinder.
     */
    @Test
    public void testGetAvailablePorts_int_int() {
        Set<Integer> result = AvailablePortFinder.getAvailablePorts( 1000, 2000 );
        assertTrue( !result.isEmpty() );
    }
    
}
