/*
 *
 *  *
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
 *  *
 *
 */

package org.apache.usergrid.persistence.core.migration.data;


import org.junit.Test;

import org.apache.usergrid.persistence.core.migration.data.MigrationRelationship;
import org.apache.usergrid.persistence.core.migration.data.VersionedData;
import org.apache.usergrid.persistence.core.migration.data.VersionedMigrationSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


public class VersionedMigrationSetTest {


    /**
     * a single version that goes from 1 to 3, and 2 to 3. then current as 3, 3.  1, 2 and 3, should have this in
     * their range
     */
    @Test
    public void singleVersion() {
        //
        final MigrationRelationship<TestVersionImpl> relationship1_3 =
                new MigrationRelationship<>( new TestVersionImpl( 1 ), new TestVersionImpl( 3 ) );

        final MigrationRelationship<TestVersionImpl> relationship2_3 =
                new MigrationRelationship<>( new TestVersionImpl( 2 ), new TestVersionImpl( 3 ) );

        //our current state, a necessity based on the data structure

        final MigrationRelationship<TestVersionImpl> current =
                new MigrationRelationship<>( new TestVersionImpl( 3 ), new TestVersionImpl( 3 ) );


        final VersionedMigrationSet<TestVersionImpl> set =
                new VersionedMigrationSet<>( relationship1_3, relationship2_3, current );


        try {
            set.getMigrationRelationship( 0 );
            fail( "I should throw an exception" );
        }
        catch ( IllegalArgumentException iae ) {
            //swallow, it's outside the range
        }

        MigrationRelationship<TestVersionImpl> migrationRelationship = set.getMigrationRelationship( 1 );

        assertEquals( relationship1_3, migrationRelationship );


        migrationRelationship = set.getMigrationRelationship( 2 );

        assertEquals( relationship2_3, migrationRelationship );

        migrationRelationship = set.getMigrationRelationship( 3 );

        assertEquals( current, migrationRelationship );

        try {
            set.getMigrationRelationship( 4 );
            fail( "I should throw an exception" );
        }
        catch ( IllegalArgumentException iae ) {
            //swallow, it's outside the range
        }
    }


    /**
     * a single version that goes from 1 to 3.  versions that go from 2 to 3, then a barrier migration that must be run.
     * This can happen during a massive data change.  From there we can move on from 3 forward, so we go from 3:6,  4:6,
     * 5:6, and 6:6,   This should mean an older system say on v2, can jump from v2 to v3, then from v3 directly to v6.
     */
    @Test
    public void versionBounds() {
        //
        final MigrationRelationship<TestVersionImpl> relationship1_3 =
                new MigrationRelationship<>( new TestVersionImpl( 1 ), new TestVersionImpl( 3 ) );

        final MigrationRelationship<TestVersionImpl> relationship2_3 =
                new MigrationRelationship<>( new TestVersionImpl( 2 ), new TestVersionImpl( 3 ) );

        //our current state, a necessity based on the data structure
        final MigrationRelationship<TestVersionImpl> relationship3_6 =
                new MigrationRelationship<>( new TestVersionImpl( 3 ), new TestVersionImpl( 6 ) );


        final MigrationRelationship<TestVersionImpl> relationship4_6 =
                new MigrationRelationship<>( new TestVersionImpl( 4 ), new TestVersionImpl( 6 ) );


        final MigrationRelationship<TestVersionImpl> relationship5_6 =
                new MigrationRelationship<>( new TestVersionImpl( 5 ), new TestVersionImpl( 6 ) );


        final MigrationRelationship<TestVersionImpl> current =
                new MigrationRelationship<>( new TestVersionImpl( 6 ), new TestVersionImpl( 6 ) );


        final VersionedMigrationSet<TestVersionImpl> set =
                new VersionedMigrationSet<>( relationship1_3, relationship2_3, relationship3_6, relationship4_6,
                        relationship5_6, current );


        try {
            set.getMigrationRelationship( 0 );
            fail( "I should throw an exception" );
        }
        catch ( IllegalArgumentException iae ) {
            //swallow, it's outside the range
        }

        MigrationRelationship<TestVersionImpl> migrationRelationship = set.getMigrationRelationship( 1 );

        assertEquals( relationship1_3, migrationRelationship );

        migrationRelationship = set.getMigrationRelationship( 2 );

        assertEquals( relationship2_3, migrationRelationship );


        //now go from v3, we should get 3 to 6

        migrationRelationship = set.getMigrationRelationship( 3 );

        assertEquals( relationship3_6, migrationRelationship );

        migrationRelationship = set.getMigrationRelationship( 4 );

        assertEquals( relationship4_6, migrationRelationship );

        migrationRelationship = set.getMigrationRelationship( 5 );

        assertEquals( relationship5_6, migrationRelationship );

        migrationRelationship = set.getMigrationRelationship( 6 );

        assertEquals( current, migrationRelationship );


        try {
            set.getMigrationRelationship( 7 );
            fail( "I should throw an exception" );
        }
        catch ( IllegalArgumentException iae ) {
            //swallow, it's outside the range
        }
    }


    @Test( expected = IllegalArgumentException.class )
    public void testNoInput() {
        new VersionedMigrationSet<TestVersionImpl>();
    }


    /**
     * Create the test version impl.  Just returns the version provided
     */
    private static final class TestVersionImpl implements VersionedData {

        private final int version;


        private TestVersionImpl( final int version ) {this.version = version;}


        @Override
        public int getImplementationVersion() {
            return version;
        }
    }
}
