/*
 *
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
 *
 */

package org.apache.usergrid.persistence.graph.serialization.impl.shard;


import java.util.Set;

import org.apache.usergrid.persistence.core.scope.ApplicationScope;

import rx.Observable;


/**
 * Defines tasks for running compaction
 *
 *
 */
public interface ShardGroupCompaction {


    /**
     * Execute the compaction task.  Will return the number of edges that have
     * @param group The shard entry group to compact
     * @return The shards that were compacted
     */
    public Set<Shard> compact(final ApplicationScope scope, final DirectedEdgeMeta edgeMeta, final ShardEntryGroup group);

    /**
     * Possibly audit the shard entry group.  This is asynchronous and returns immediately
     * @param group
     * @return
     */
    public AuditResult evaluateShardGroup( final ApplicationScope scope, final DirectedEdgeMeta edgeMeta,
                                           final ShardEntryGroup group );


    public enum AuditResult{
        /**
         * We didn't check this shard
         */
        NOT_CHECKED,
        /**
         * This shard was checked, but nothing was allocated
         */
        CHECKED_NO_OP,

        /**
         * We checked and created a new shard
         */
        CHECKED_CREATED,

        /**
         * The shard group is already compacting
         */
        COMPACTING
    }


}
