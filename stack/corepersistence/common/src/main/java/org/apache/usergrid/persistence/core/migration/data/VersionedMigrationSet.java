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


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Preconditions;


/**
 * A set that represents a set of tuples that are used for
 * @param <T>
 */
public class VersionedMigrationSet<T extends VersionedData> {


    /**
     * Cache so that after our initial lookup, it O(1) since this will be used heavily
     *
     */
    private Map<Integer, MigrationRelationship<T>> cacheVersion = new HashMap<>();

    private List<MigrationRelationship<T>> orderedVersions = new ArrayList<>();


    /**
     * Construct this set from a group of tuples.  Imagine the following versions
     *
     * v1,
     * v2,
     * v3,
     * v4
     *
     * Migrations can jump from v1->v3, but not directly to v4 without an extraneous migration.  This would have 2 relationships
     *
     * v1, v3
     * v2, v3
     * and
     * v3, v4
     *
     *
     * @param migrations
     */
    public VersionedMigrationSet( final MigrationRelationship<T>... migrations ){
        Preconditions.checkNotNull(migrations, "versions must not be null");
        Preconditions.checkArgument( migrations.length > 0, "You must specify at least 1 migrationrelationship" );

        orderedVersions.addAll( Arrays.asList(migrations ) );

        Collections.sort( orderedVersions, new VersionedDataComparator() );

    }


    /**
     * Get the migration relationship based on our current version. This will return a range that includes the current
     * system version as the source, and the highest version we can roll to in the to field
     *
     * @return The MigrationRelationship.  Note the from and the to could be the same version in a current system.
     */
    public MigrationRelationship<T> getMigrationRelationship( final int currentVersion ) {

        final MigrationRelationship<T> relationship = cacheVersion.get( currentVersion );

        if ( relationship != null ) {
            return relationship;
        }

        //not there, find it.  Not the most efficient, but it happens once per version, which rarely changes, so not
        // a big deal

        int lastSpan = Integer.MAX_VALUE;
        MigrationRelationship<T> toUse = null;

        for ( MigrationRelationship<T> current : orderedVersions ) {

            //not our instance, the from is too high
            //our from is this instance, so we support this tuple.  Our future is >= as well, so we can perform this I/O

            final int newSpan = current.getSpan( currentVersion );

            if ( newSpan < lastSpan ) {
                lastSpan = newSpan;
                toUse = current;
            }
        }

        //if we get here, something is wrong
        if ( lastSpan == Integer.MAX_VALUE ) {
            throw new IllegalArgumentException(
                "Could not find a migration version for version " + currentVersion + " min found was " + orderedVersions
                    .get( orderedVersions.size() - 1 ) );
        }


        cacheVersion.put( currentVersion, toUse );
        return toUse;
    }


    /**
     * Given the current system version, return the maximum migration version we can move to
     * @param currentVersion
     * @return
     */
    public int getMaxVersion(final int currentVersion){
        return getMigrationRelationship( currentVersion ).to.getImplementationVersion();
    }



    /**
     * Orders from high to low
     */
    private final class VersionedDataComparator implements Comparator<MigrationRelationship<T>>
    {

        @Override
        public int compare( final MigrationRelationship<T> o1, final MigrationRelationship<T> o2 ) {
            //Put higher target version first, since that's what we want to match based on current state and source

            //order by the source.  Put highest source first
            int  compare = Integer.compare( o1.to.getImplementationVersion(), o2.to.getImplementationVersion() ) *-1;


            //put higher from first, if we fall within a range here we're good
            if(compare == 0){
                compare =  Integer.compare( o1.from.getImplementationVersion(), o2.from.getImplementationVersion() ) *-1;
            }

            return compare;
        }

    }

}
