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
 * The listener for node delete events.  Performs post proessing
 */
public interface NodeDeleteListener {

    /**
       * Removes this node from the graph.
       *
       * @param scope The scope of the application
       * @param node The node that was deleted
       * @param timestamp The timestamp of the event
       *
       * @return An observable that emits the total number of edges that have been removed with this node both as the
       *         target and source
       */
    Observable<Integer> receive( final ApplicationScope scope, final Id node, final UUID timestamp );
}
