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

import com.google.common.base.Preconditions;


/**
 *
 *
 */
public class EdgeUtils {

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
        ValidationUtils.verifyTimeUuid( e.getTimestamp(), "version" );
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
        ValidationUtils.verifyTimeUuid( search.getMaxVersion(), "maxVersion" );

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
           ValidationUtils.verifyTimeUuid( search.getMaxVersion(), "maxVersion" );

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

}
