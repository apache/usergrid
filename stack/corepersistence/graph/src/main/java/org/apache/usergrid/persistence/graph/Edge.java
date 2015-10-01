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

package org.apache.usergrid.persistence.graph;


import java.io.Serializable;

import org.apache.usergrid.persistence.graph.impl.SimpleEdge;
import org.apache.usergrid.persistence.model.entity.Id;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;


/**
 * Defines a directed edge from the source node to the target node.  Edges are considered immutable.
 * Once created, their data cannot be modified.
 *
 * @author tnine
 */
@JsonDeserialize(as = SimpleEdge.class)
public interface Edge extends Serializable {

    /**
     * Get the Id of the source node of this edge
     */
    Id getSourceNode();


    /**
     * Get the name of the edge
     */
    String getType();

    /**
     * Get the id of the target node of this edge
     */
    Id getTargetNode();

    /**
     * Get the version (as a type 1 time uuid) of this edge
     */
    long getTimestamp();
}
