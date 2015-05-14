/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one or more
 *  *  contributor license agreements.  The ASF licenses this file to You
 *  * under the Apache License, Version 2.0 (the "License"); you may not
 *  * use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.  For additional information regarding
 *  * copyright in this work, please see the NOTICE file in the top level
 *  * directory of this distribution.
 *
 */
package org.apache.usergrid.persistence.graph.serialization;

import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.GraphManager;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.base.Optional;

import rx.Observable;

/**
 * Get all edges from source
 */
public interface EdgesObservable {

    /**
     * Return an observable of all edges from a source
     * @param gm
     * @param sourceNode
     * @return
     */
    Observable<Edge> edgesFromSourceDescending( final GraphManager gm, final Id sourceNode );


    /**
     * Get all edges from the source node with the target type
     * @param gm
     * @param sourceNode
     * @param targetType
     * @return
     */
    Observable<Edge> getEdgesFromSource(final GraphManager gm, final Id sourceNode, final String targetType );

    /**
     * Return an observable of all edges to a target
     * @param gm
     * @param targetNode
     * @return
     */
    Observable<Edge> edgesToTarget(final GraphManager gm,  final Id targetNode);

    /**
     * Return an observable of all edges from a source node.  Ordered ascending, from the startTimestamp if specified
     * @param gm
     * @param sourceNode
     * @param edgeType The edge type if specified.  Otherwise all types will be used
     * @param resume The edge to start seeking after.  Otherwise starts at the most recent
     * @return
     */
    Observable<Edge> edgesFromSourceDescending( final GraphManager gm, final Id sourceNode,
                                                final Optional<String> edgeType, final Optional<Edge> resume );
}
