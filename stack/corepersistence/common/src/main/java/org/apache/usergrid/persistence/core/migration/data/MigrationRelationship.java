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


/**
 * Simple relationship that defines the current state of the source and destination data versions.  Note that
 * ina current system, the from and then to will be the same instance
 */
public class MigrationRelationship<T extends VersionedData> {

    //public so it's FAST.  It's also immutable


    public final T from;
    public final T to;

    private final int fromVersion;
    private final int toVersion;


    public MigrationRelationship( T from, T to ) {
        this.from = from;
        this.to = to;

        fromVersion = from.getImplementationVersion();
        toVersion = to.getImplementationVersion();
    }


    /**
     * Returns true if we need to perform dual writes.  IE. the from is not the same as the to
     * @return
     */
    public boolean needsMigration(){
        return fromVersion != toVersion;
    }


    /**
     * Return true if this is the migration relationship we should use.  The version matches the from
     * and is <= the to
     * @param currentVersion
     * @return
     */
    public boolean correctRelationship(final int currentVersion){
        return currentVersion == fromVersion && currentVersion <= toVersion;
    }


    @Override
    public boolean equals( final Object o ) {
        if ( this == o ) {
            return true;
        }
        if ( !( o instanceof MigrationRelationship ) ) {
            return false;
        }

        final MigrationRelationship that = ( MigrationRelationship ) o;

        if ( !from.equals( that.from ) ) {
            return false;
        }
        if ( !to.equals( that.to ) ) {
            return false;
        }

        return true;
    }


    @Override
    public int hashCode() {
        int result = from.hashCode();
        result = 31 * result + to.hashCode();
        return result;
    }
}
