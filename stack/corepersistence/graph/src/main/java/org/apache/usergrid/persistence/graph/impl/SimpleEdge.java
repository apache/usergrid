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
import org.apache.usergrid.persistence.graph.serialization.util.GraphValidation;
import org.apache.usergrid.persistence.model.entity.Id;


/**
 * Simple bean to represent our edge
 * @author tnine
 */
public class SimpleEdge implements Edge {

    protected Id sourceNode;
    protected String type;
    protected Id targetNode;
    protected long timestamp;


    /**
     * Used for SMILE.  Do not remove
     */
    @SuppressWarnings( "unused" )
    public SimpleEdge(){

    }

    public SimpleEdge( final Id sourceNode, final String type, final Id targetNode, final long timestamp ) {
        this.sourceNode = sourceNode;
        this.type = type;
        this.targetNode = targetNode;
        this.timestamp = timestamp;

        GraphValidation.validateEdge( this );
    }


    @Override
    public Id getSourceNode() {
        return sourceNode;
    }


    @Override
    public String getType() {
        return type;
    }


    @Override
    public Id getTargetNode() {
        return targetNode;
    }


    public long getTimestamp() {
        return timestamp;
    }


    @Override
    public boolean equals( final Object o ) {
        if ( this == o ) {
            return true;
        }
        if ( !( o instanceof SimpleEdge ) ) {
            return false;
        }

        final SimpleEdge that = ( SimpleEdge ) o;

        if ( timestamp != that.timestamp ) {
            return false;
        }
        if ( !sourceNode.equals( that.sourceNode ) ) {
            return false;
        }
        if ( !targetNode.equals( that.targetNode ) ) {
            return false;
        }
        if ( !type.equals( that.type ) ) {
            return false;
        }

        return true;
    }


    @Override
    public int hashCode() {
        int result = sourceNode.hashCode();
        result = 31 * result + type.hashCode();
        result = 31 * result + targetNode.hashCode();
        result = 31 * result + ( int ) ( timestamp ^ ( timestamp >>> 32 ) );
        return result;
    }


    @Override
    public String toString() {
        return "SimpleEdge{" +
                "sourceNode=" + sourceNode +
                ", type='" + type + '\'' +
                ", targetNode=" + targetNode +
                ", timestamp=" + timestamp +
                '}';
    }
}
