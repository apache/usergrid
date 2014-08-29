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
package org.apache.usergrid.chop.webapp.dao;

import org.apache.usergrid.chop.webapp.dao.model.RunnerGroup;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;

import static org.junit.Assert.*;


public class RunnerGroupTest {

    private static Logger LOG = LoggerFactory.getLogger( RunnerGroupTest.class );


    @Test
    public void testEqualsAndHashCode() {
        LOG.info( "\n=== RunnerGroupTest.testEqualsAndHashcode() ===\n" );

        RunnerGroup runnerGroup1 = new RunnerGroup( "user", "commit", "module" );
        RunnerGroup runnerGroup2 = new RunnerGroup( "user", "commit", "module" );
        RunnerGroup runnerGroup3 = new RunnerGroup( "user1", "commit", "module" );

        assertEquals( runnerGroup1, runnerGroup2 );
        assertEquals( runnerGroup1.hashCode(), runnerGroup2.hashCode() );

        assertNotEquals( runnerGroup1, runnerGroup3 );
        assertNotEquals( runnerGroup1.hashCode(), runnerGroup3.hashCode() );
    }


    @Test
    public void testWithCollections() {
        LOG.info( "\n=== RunnerGroupTest.testWithCollections() ===\n" );

        RunnerGroup runnerGroup1 = new RunnerGroup( "user", "commit", "module" );
        RunnerGroup runnerGroup2 = new RunnerGroup( "user", "commit", "module" );
        RunnerGroup runnerGroup3 = new RunnerGroup( "user1", "commit", "module" );
        HashSet<RunnerGroup> set = new HashSet<RunnerGroup>();

        set.add( runnerGroup1 );
        set.add( runnerGroup2 );
        assertEquals( set.size(), 1 );
        assertTrue( set.contains( runnerGroup1 ) );
        assertTrue( set.contains( runnerGroup2 ) );

        set.add( runnerGroup3 );
        assertEquals( set.size(), 2 );
        assertTrue(set.contains( runnerGroup3 ) );

        set.remove( runnerGroup3 );
        assertTrue( ! set.contains( runnerGroup3 ) );
    }


    @Test
    public void testId() {
        LOG.info( "\n=== RunnerGroupTest.testId() ===\n" );

        RunnerGroup runnerGroup = new RunnerGroup( "user1", "", "" );

        // Make sure the id doesn't contain the dash. The dash can cause issues in ES search.
        assertTrue( ! runnerGroup.getId().contains( "-" ) );
    }


    @Test
    public void testNull() {
        LOG.info( "\n=== RunnerGroupTest.testNull() ===\n" );

        assertTrue( new RunnerGroup( "user", "", "" ).isNull() );
        assertTrue( new RunnerGroup( "", "commit", "" ).isNull() );
        assertTrue( new RunnerGroup( "", "", "module" ).isNull() );
    }
}
