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
package org.apache.usergrid.persistence.index;

import org.apache.usergrid.persistence.model.entity.Id;


/**
 * An edge we can search from
 */
public interface SearchEdge {

    /**
     * Get the node id to be indexed.  In the case of a "TARGET" entity location, this would be the sourceId.  In the case of the "SOURCE" entity location, this would be the targetId.
     * Ultimately this should be the other side of the edge from the entity getting searched
     * @return
     */
    Id getNodeId();

    /**
     * Get the name of the edge to be used
     * @return
     */
    String getEdgeName();

    /**
     * Get the edge type for this search edge.
     *
     * If the nodeId was the source in the edge, set this to source.  If it was the target, set this to target
     * @return
     */
    NodeType getNodeType();


    /**
     * If the nodeId was the source in the edge, set this to source.  If it was the target, set this to target
     *
     */
    enum NodeType {
        SOURCE,
        TARGET
    }


}
