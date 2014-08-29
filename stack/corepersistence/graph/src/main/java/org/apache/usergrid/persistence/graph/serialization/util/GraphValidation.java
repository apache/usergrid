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

package org.apache.usergrid.persistence.graph.serialization.util;


import org.apache.usergrid.persistence.core.util.ValidationUtils;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.SearchByEdge;
import org.apache.usergrid.persistence.graph.SearchByEdgeType;
import org.apache.usergrid.persistence.graph.SearchByIdType;
import org.apache.usergrid.persistence.graph.SearchEdgeType;
import org.apache.usergrid.persistence.graph.SearchIdType;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.DirectedEdgeMeta;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.Shard;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.ShardEntryGroup;

import com.google.common.base.Preconditions;


/**
 *
 *
 */
public class GraphValidation {

    /**
     * Validate an edge input
     *
     * @param e The edge to validate
     */
    public static void validateEdge( Edge e ) {
        Preconditions.checkNotNull( e, "edge is required" );
        ValidationUtils.verifyIdentity( e.getSourceNode() );
        ValidationUtils.verifyIdentity( e.getTargetNode() );
        ValidationUtils.verifyString( e.getType(), "type" );
        validateTimestamp( e.getTimestamp(), "timestamp" );

    }


    /**
     * Validate the timestamp is set
     * @param value
     * @param fieldName
     */
    public static void validateTimestamp(final long value, final String fieldName){
        Preconditions.checkArgument( value > -1, fieldName );
    }

    /**
     * Validate the search edge
     */
    public static void validateSearchEdgeType( final SearchEdgeType search ) {
        Preconditions.checkNotNull( search, "search is required" );

        ValidationUtils.verifyIdentity( search.getNode() );
    }


    /**
     * Validate the search edge
     */
    public static void validateSearchEdgeIdType( final SearchIdType search ) {
        Preconditions.checkNotNull( search, "search is required" );

        validateSearchEdgeType( search );

        ValidationUtils.verifyString( search.getEdgeType(), "edgeType" );
    }

    /**
     * Validate the search edge
     */
    public static void validateSearchByEdgeType( final SearchByEdgeType search ) {
        Preconditions.checkNotNull( search, "search is required" );

        ValidationUtils.verifyIdentity( search.getNode() );
        ValidationUtils.verifyString( search.getType(), "type" );
        validateTimestamp( search.getMaxTimestamp(), "maxTimestamp" );

        //only validate if the value is present
        if(search.last().isPresent()){
            validateEdge( search.last().get() );
        }

    }

    /**
        * Validate the search edge
        */
       public static void validateSearchByEdge( final SearchByEdge search ) {
           Preconditions.checkNotNull( search, "search is required" );

           ValidationUtils.verifyIdentity( search.sourceNode() );
           ValidationUtils.verifyIdentity( search.targetNode() );
           ValidationUtils.verifyString( search.getType(), "type" );
           validateTimestamp( search.getMaxTimestamp(), "maxTimestamp" );

           //only validate if the value is present
           if(search.last().isPresent()){
               validateEdge( search.last().get() );
           }

       }


    /**
     * Validate the search
     * @param search
     */
    public static void validateSearchByIdType(final SearchByIdType search){
        validateSearchByEdgeType( search );

        ValidationUtils.verifyString(search.getIdType(), "id type");

    }


    /**
     * Validate the directed edge meta data
     * @param directedEdgeMeta
     */
    public static void validateDirectedEdgeMeta(final DirectedEdgeMeta directedEdgeMeta){

        Preconditions.checkNotNull( directedEdgeMeta, "directedEdgeMeta must not be null" );

        final DirectedEdgeMeta.NodeMeta[] nodes = directedEdgeMeta.getNodes();

        Preconditions.checkArgument( nodes.length > 0, "At least one node must be present" );

        for( DirectedEdgeMeta.NodeMeta node : nodes){
            ValidationUtils.verifyIdentity( node.getId());
            Preconditions.checkNotNull( node.getNodeType(), "NodeType must not be null" );
        }

        final String[] types = directedEdgeMeta.getTypes();

        Preconditions.checkArgument( types.length > 0, "At least one type must be present" );

        for(String type: types){
            Preconditions.checkNotNull( type, "You cannot have a null type" );
        }


    }


    /**
     * Validate the directed edge meta data
     * @param shardEntryGroup
     */
    public static void validateShardEntryGroup(final ShardEntryGroup shardEntryGroup){

        Preconditions.checkNotNull( shardEntryGroup, "shardEntryGroup must not be null" );

        Preconditions.checkArgument( shardEntryGroup.entrySize() > 0, "shardEntryGroups must contain at least 1 shard");

    }


    /**
     * Validate our shard
     * @param shard
     */
    public static void valiateShard(final Shard shard){
        Preconditions.checkNotNull( shard, "shard must not be null" );
        Preconditions.checkArgument( shard.getShardIndex() > -1, "shardid must be greater than -1" );
        Preconditions.checkArgument( shard.getCreatedTime() > -1, "createdTime must be greater than -1" );

    }

}
