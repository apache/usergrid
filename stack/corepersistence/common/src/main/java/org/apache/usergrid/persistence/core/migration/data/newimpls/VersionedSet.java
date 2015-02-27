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

package org.apache.usergrid.persistence.core.migration.data.newimpls;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.google.inject.Inject;


public class VersionedSet<T extends VersionedData> {


    /**
     * Cache so that after our initial lookup, it O(1) since this will be used heavily
     *
     */
    private Map<Integer, MigrationRelationship<T>> cacheVersion = new HashMap<>();

    private List<T> orderedVersions = new ArrayList<>();




    public VersionedSet(final Collection<T> versions){

        orderedVersions.addAll(versions  );
        Collections.sort( orderedVersions, new VersionedDataComparator() );

    }


    /**
     * Get the migration relationship based on our current version
     * @param currentVersion
     * @return
     */
    public MigrationRelationship<T> getCurrentReadVersion(final int currentVersion){

        final MigrationRelationship<T> relationship = cacheVersion.get( currentVersion );

        if(relationship != null){
            return relationship;
        }

        //not there, find it.  Not the most efficient, but it happens once per version, which rarely changes, so not a big deal


        for(T current: orderedVersions){
            //not our instance
            if(current.getImplementationVersion() > currentVersion){
                continue;
            }


            //we always go from our first match to our highest version.  Any versions between can be skipped
            final MigrationRelationship<T> migrationRelationship = new MigrationRelationship<>( current, orderedVersions.get( 0 )  );

            cacheVersion.put( currentVersion, migrationRelationship );

            return migrationRelationship;

        }

        //if we get here, something is wrong
        throw new IllegalArgumentException( "Could not find a migration version for version " + currentVersion + " min found was " + orderedVersions.get( orderedVersions.size()-1 ) );


    }


    /**
     * Orders from high to low
     */
    private static final class VersionedDataComparator implements Comparator<VersionedData>
    {

        @Override
        public int compare( final VersionedData o1, final VersionedData o2 ) {
            return Integer.compare( o1.getImplementationVersion(), o2.getImplementationVersion())*-1;
        }
    }

}
