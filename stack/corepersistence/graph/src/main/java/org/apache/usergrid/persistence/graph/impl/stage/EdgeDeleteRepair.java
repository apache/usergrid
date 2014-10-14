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
import org.apache.usergrid.persistence.graph.MarkedEdge;

import rx.Observable;


/**
 * Interface to perform repair operations on an edge when it is written
 */
public interface EdgeDeleteRepair {


    /**
     * Repair this edge.  Remove previous entries
     * @param scope The scope to use
     * @param edge The last edge to retain.  All versions  <= this edge's version  will be deleted
     * @param timestamp The timestamp this operation was performed
     *
     * @return An observable that emits every version of the edge we delete.  Note that it may emit duplicates
     * since this is a streaming API.
     */
    public Observable<MarkedEdge> repair( ApplicationScope scope, MarkedEdge edge, UUID timestamp );
}
