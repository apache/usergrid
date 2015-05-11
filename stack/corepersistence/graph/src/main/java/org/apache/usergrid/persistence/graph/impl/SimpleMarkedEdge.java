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

package org.apache.usergrid.persistence.graph.impl;


import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.MarkedEdge;
import org.apache.usergrid.persistence.model.entity.Id;

import com.fasterxml.jackson.annotation.JsonIgnore;


/**
 * Simple bean to represent our edge
 * @author tnine
 */
public class SimpleMarkedEdge extends  SimpleEdge implements MarkedEdge {

    private final boolean deleted;


    public SimpleMarkedEdge( final Id sourceNode, final String type, final Id targetNode, final long timestamp, final boolean deleted) {

        super(sourceNode, type, targetNode, timestamp);
        this.deleted = deleted;
    }


    public SimpleMarkedEdge(final Edge edge, final boolean deleted){
        this(edge.getSourceNode(), edge.getType(), edge.getTargetNode(), edge.getTimestamp(), deleted);
    }


    @Override
    @JsonIgnore
    public boolean isDeleted() {
        return deleted;
    }


    @Override
    public boolean equals( final Object o ) {
        if ( this == o ) {
            return true;
        }
        if ( !( o instanceof SimpleMarkedEdge ) ) {
            return false;
        }
        if ( !super.equals( o ) ) {
            return false;
        }

        final SimpleMarkedEdge that = ( SimpleMarkedEdge ) o;

        if ( deleted != that.deleted ) {
            return false;
        }

        return true;
    }


    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + ( deleted ? 1 : 0 );
        return result;
    }


    @Override
    public String toString() {
        return "SimpleMarkedEdge{" +
                "deleted=" + deleted +
                "} " + super.toString();
    }
}

