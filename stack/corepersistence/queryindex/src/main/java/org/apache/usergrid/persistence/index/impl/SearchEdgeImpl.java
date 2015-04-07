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
package org.apache.usergrid.persistence.index.impl;

import org.apache.usergrid.persistence.index.SearchEdge;
import org.apache.usergrid.persistence.model.entity.Id;


/**
 * The edge to search on.  Can be either a source--> target edge or a target<-- source edge.  The entiies returned
 * will be on the opposite side of the edge from the specified nodeId
 */
public class SearchEdgeImpl implements SearchEdge {
    protected final Id nodeId;
    protected final String name;
    protected final NodeType nodeType;


    public SearchEdgeImpl( final Id nodeId, final String name, final NodeType nodeType ) {
        this.nodeId = nodeId;
        this.name = name;
        this.nodeType = nodeType;
    }


    @Override
    public Id getNodeId() {
        return nodeId;
    }


    @Override
    public String getEdgeName() {
        return name;
    }


    @Override
    public NodeType getNodeType() {
        return nodeType;
    }


    @Override
    public boolean equals( final Object o ) {
        if ( this == o ) {
            return true;
        }
        if ( !( o instanceof SearchEdgeImpl ) ) {
            return false;
        }

        final SearchEdgeImpl that = ( SearchEdgeImpl ) o;

        if ( !nodeId.equals( that.nodeId ) ) {
            return false;
        }
        if ( !name.equals( that.name ) ) {
            return false;
        }
        return nodeType == that.nodeType;
    }


    @Override
    public int hashCode() {
        int result = nodeId.hashCode();
        result = 31 * result + name.hashCode();
        result = 31 * result + nodeType.hashCode();
        return result;
    }


    @Override
    public String toString() {
        return "SearchEdgeImpl{" +
                "nodeId=" + nodeId +
                ", name='" + name + '\'' +
                ", nodeType=" + nodeType +
                '}';
    }
}
