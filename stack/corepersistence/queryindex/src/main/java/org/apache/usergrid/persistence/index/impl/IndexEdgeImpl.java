/*
 *
 *
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
 *
 *
 */

package org.apache.usergrid.persistence.index.impl;


import org.apache.usergrid.persistence.index.IndexEdge;
import org.apache.usergrid.persistence.model.entity.Id;


/**
 * Implementation of the edge to index
 */
public class IndexEdgeImpl extends SearchEdgeImpl implements IndexEdge {

    protected final long timestamp;


    public IndexEdgeImpl( final Id nodeId, final String name, final NodeType nodeType, final long timestamp ) {
        super( nodeId, name, nodeType );
        this.timestamp = timestamp;
    }


    @Override
    public long getTimestamp() {
        return timestamp;
    }


    @Override
    public boolean equals( final Object o ) {
        if ( this == o ) {
            return true;
        }
        if ( !( o instanceof IndexEdgeImpl ) ) {
            return false;
        }
        if ( !super.equals( o ) ) {
            return false;
        }

        final IndexEdgeImpl indexEdge = ( IndexEdgeImpl ) o;

        return timestamp == indexEdge.timestamp;
    }


    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + ( int ) ( timestamp ^ ( timestamp >>> 32 ) );
        return result;
    }


    @Override
    public String toString() {
        return "IndexEdgeImpl{" +
                "timestamp=" + timestamp +
                "} " + super.toString();
    }
}

