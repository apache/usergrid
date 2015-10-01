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
package org.apache.usergrid.persistence.collection;


import java.util.UUID;

import org.apache.usergrid.persistence.collection.mvcc.entity.Stage;
import org.apache.usergrid.persistence.model.entity.Id;


/**
 * A Marker interface for an in flight update to allow context information to be passed between states
 */
public interface MvccLogEntry {


    /**
     * Get the stage for the current version
     */
    Stage getStage();

    /**
     * Get the entity to add info to the log
     */
    Id getEntityId();

    /**
     * Get the version of the entity
     */
    UUID getVersion();

    /**
     * Get the status of the entity
     */
    State getState();



    /**
     * The state of the entity.  Is it a complete entity, a partial entity, or a deleted?
     */
    public enum State {

        /**
         * The logentry being written represents a complete entity
         */
        COMPLETE(0),
        /**
         * The logentry being written represents a partial entity
         */
        @Deprecated//removed in v3
        PARTIAL(1),

        /**
         * This logentry has been marked as deleted
         */
        DELETED(2)
        ;

        private final int id;


        private State( final int id ) {
            this.id = id;
        }


        /**
         * Returns true if this stage is transient and should not be retained in the datastore permanently Stages such as
         * start and write don't need to be retained, but can be used to signal "in flight" updates
         */


        public int getId() {
            return this.id;
        }
    }
}
