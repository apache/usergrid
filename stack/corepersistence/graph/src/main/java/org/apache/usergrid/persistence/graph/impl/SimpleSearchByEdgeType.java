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


import org.apache.usergrid.persistence.core.util.ValidationUtils;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.SearchByEdgeType;
import org.apache.usergrid.persistence.graph.serialization.util.GraphValidation;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;


/**
 *
 *
 */
public class SimpleSearchByEdgeType implements SearchByEdgeType{

    private final Id node;
    private final String type;
    private final long maxTimestamp;
    private final Optional<Edge> last;
    private final Order order;
    private final boolean filterMarked;


    /**
     * Create the search modules
     * @param node The node to search from
     * @param type The edge type
     * @param maxTimestamp The maximum timestamp to return
     * @param order The order order.  Descending is most efficient
     * @param last The value to start seeking from.  Must be >= this value
     * @param order
     *
     * //TODO, make last an optional
     */
    public SimpleSearchByEdgeType( final Id node, final String type, final long maxTimestamp, final Order order, final Edge last ) {
        this(node, type, maxTimestamp, order, Optional.fromNullable(last), true);
    }


    /**
     * Create the search modules
     * @param node The node to search from
     * @param type The edge type
     * @param maxTimestamp The maximum timestamp to return
     * @param order The order order.  Descending is most efficient
     * @param last The value to start seeking from.  Must be >= this value
     * @param order
     *
     * //TODO, make last an optional
     */
    public SimpleSearchByEdgeType( final Id node, final String type, final long maxTimestamp, final Order order,
                                   final Optional<Edge> last ) {
        this( node, type, maxTimestamp, order, last, true );
    }


    /**
     * Create the search modules
     * @param node The node to search from
     * @param type The edge type
     * @param maxTimestamp The maximum timestamp to return
     * @param order The order order.  Descending is most efficient
     * @param last The value to start seeking from.  Must be >= this value
     * @param filterMarked
     */
    public SimpleSearchByEdgeType( final Id node, final String type, final long maxTimestamp, final Order order,
                                   final Optional<Edge> last, final boolean filterMarked ) {


        Preconditions.checkNotNull( order, "order is required");
        ValidationUtils.verifyIdentity( node );
        ValidationUtils.verifyString( type, "type" );
        GraphValidation.validateTimestamp( maxTimestamp, "maxTimestamp" );
        Preconditions.checkNotNull( last, "last is required" );


        this.node = node;
        this.type = type;
        this.maxTimestamp = maxTimestamp;
        this.order = order;
        this.last = last;
        this.filterMarked = filterMarked;
    }


    @Override
    public Id getNode() {
        return node;
    }


    @Override
    public String getType() {
        return type;
    }


    @Override
    public long getMaxTimestamp() {
        return maxTimestamp;
    }


    @Override
    public Optional<Edge> last() {
        return last;
    }


    @Override
    public Order getOrder() {
        return order;
    }


    @Override
    public boolean filterMarked() {
        return filterMarked;
    }


    @Override
    public boolean equals( final Object o ) {
        if ( this == o ) {
            return true;
        }
        if ( !( o instanceof SimpleSearchByEdgeType ) ) {
            return false;
        }

        final SimpleSearchByEdgeType that = ( SimpleSearchByEdgeType ) o;

        if ( maxTimestamp != that.maxTimestamp ) {
            return false;
        }
        if ( !last.equals( that.last ) ) {
            return false;
        }
        if ( !node.equals( that.node ) ) {
            return false;
        }
        if ( !type.equals( that.type ) ) {
            return false;
        }

        return true;
    }


    @Override
    public int hashCode() {
        int result = node.hashCode();
        result = 31 * result + type.hashCode();
        result = 31 * result + ( int ) ( maxTimestamp ^ ( maxTimestamp >>> 32 ) );
        result = 31 * result + last.hashCode();
        return result;
    }
}
