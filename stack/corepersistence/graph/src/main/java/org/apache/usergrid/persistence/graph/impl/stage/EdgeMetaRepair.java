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

package org.apache.usergrid.persistence.graph.impl.stage;


import java.util.UUID;

import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.model.entity.Id;

import rx.Observable;


/**
 * Audits edge meta data and removes them if they're obscelete
 */
public interface EdgeMetaRepair {

    /**
     * Validate that the source types can be cleaned for the given info
     *
     * @param scope The scope to use
     * @param sourceId The source Id to use
     * @param edgeType The edge type
     * @param version The max version to clean
     *
     * @return An observable that emits the total number of sub types still in use.  0 implies the type and subtypes
     *         have been removed.  Anything > 0 implies the edgeType and subTypes are still in use
     */
    public Observable<Integer> repairSources( ApplicationScope scope, Id sourceId, String edgeType, UUID version );


    /**
     * Remove all source id types that are empty, as well as the edge type if there are no more edges for it
     *
     * @param scope The scope to use
     * @param targetId The target Id to use
     * @param edgeType The edge type
     * @param version The max version to clean
     *
     * @return An observable that emits the total number of sub types still in use.  0 implies the type and subtypes
     *         have been removed.  Anything > 0 implies the edgeType and subTypes are still in use
     */
    public Observable<Integer> repairTargets( ApplicationScope scope, Id targetId, String edgeType, UUID version );
}
