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
package org.apache.usergrid.persistence.index.utils;


import org.apache.usergrid.persistence.index.IndexEdge;
import org.apache.usergrid.persistence.index.SearchEdge;

import com.google.common.base.Preconditions;

import static org.apache.usergrid.persistence.core.util.ValidationUtils.verifyIdentity;
import static org.apache.usergrid.persistence.core.util.ValidationUtils.verifyString;


/**
 * Validation Utilities for collection
 */
public class IndexValidationUtils {



    /**
     * Validate the search edge is correct
     * @param searchEdge
     */
    public static void validateSearchEdge(final SearchEdge searchEdge){
        Preconditions.checkNotNull(searchEdge, "searchEdge is required");
        org.apache.usergrid.persistence.core.util.ValidationUtils.verifyIdentity( searchEdge.getNodeId() );
        Preconditions.checkArgument( searchEdge.getEdgeName() != null && searchEdge.getEdgeName().length() > 0, "search edge name is required" );

    }



    /**
     * Validate the search edge is correct
     * @param indexEdge
     */
    public static void validateIndexEdge(final IndexEdge indexEdge){
        //we don't care about timestamp.  It's a primitive so always present
        validateSearchEdge( indexEdge );
    }



}
