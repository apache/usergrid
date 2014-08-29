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


import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.base.Optional;


/**
 * Defines parameters for a search operation where searching from a node.
 * Useful for getting all types of edges relating to the node
 *
 *
 * @author tnine */
public interface SearchEdgeType {

    /**
     * Get the Id of the node of this edge
     * @return
     */
    Id getNode();


    /**
     * Search edge types with an optional prefix
     * @return
     */
    Optional<String> prefix();

    /**
     * Return the last value returned.  All returned types will be >= this value
     * @return
     */
    Optional<String> getLast();



}
