/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.usergrid.persistence.core.migration.data;


import org.apache.usergrid.persistence.core.scope.ApplicationEntityGroup;
import rx.Observable;

/**
 * An interface for updating data.  Has 2 basic functions. First it will perform the migration and update the status
 * object.
 *
 * Second it will only migrate a single version.  For instance, it will migrate from 0->1, or from 1->2.  All migrations
 * must follow the following basic rules.
 *
 * <ol>
 *     <li>They must not modify the structure of an existing column family.  If the data format changes, a new
 * implementation and column family must be created.  A proxy must be defined to do dual writes/single reads. </li>
 * <li>The migration must update the progress observer.  This information should be made available cluster wide.</li>
 * <li>In the event a migration fails with an error, we should be able to roll back and remove new column families.  We
 * can then fix the bug, and deploy again.  Hence the need for the proxy, dual writes, and an immutable CF
 * format</li>
 * </ol>
 */


public interface DataMigration <T> {


    /**
     * Migrate the data to the specified version
     * @param observer
     * @throws Throwable
     */
    public Observable migrate(final Observable<T> applicationEntityGroup,final ProgressObserver observer) throws Throwable;

    /**
     * Get the version of this migration.  It must be unique.
     * @return
     */
    public int getVersion();

    public MigrationType getType();

    public interface ProgressObserver{
        /**
         * Mark the migration as failed
         * @param migrationVersion The migration version running during the failure
         * @param reason The reason to save
         */
        public void failed(final int migrationVersion, final String reason);

        /**
         * Mark the migration as failed with a stack trace
         * @param migrationVersion The migration version running during the failure
         * @param reason The error description to save
         * @param throwable The error that happened
         */
        public void failed(final int migrationVersion, final String reason, final Throwable throwable);


        /**
         * Update the status of the migration with the message
         *
         * @param message The message to save for the status
         */
        public void update(final int migrationVersion, final String message);
    }

    public enum MigrationType{
        Entities,
        Applications,
        System,
        Collections
    }

}
