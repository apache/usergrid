/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one or more
 *  *  contributor license agreements.  The ASF licenses this file to You
 *  * under the Apache License, Version 2.0 (the "License"); you may not
 *  * use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.  For additional information regarding
 *  * copyright in this work, please see the NOTICE file in the top level
 *  * directory of this distribution.
 *
 */
package org.apache.usergrid.persistence.index;


import rx.Observable;

/**
 * Entity Index for an Application.
 */
public interface ApplicationEntityIndex {


    /**
     * Create the index batch.
     */
    EntityIndexBatch createBatch();

    /**
     * Search on every document in the specified search edge.  Also search by the types if specified
     * @param searchEdge The edge to search on
     * @param searchTypes The search types to search
     * @param query The query to execute
     * @param limit The limit of values to return
     * @return
     */
    CandidateResults search( final SearchEdge searchEdge, final SearchTypes searchTypes, final String query,
                             final int limit );



    /**
     * Get next page of results from a previous cursor.  Note that limit used here should be the same limit as the initial
     * Cursor.  Failure to do so can result in strange cursor behavior on the response.
     *
     * @param cursor The cursor from the original search
     * @return The next page of candidate results
     */
    CandidateResults getNextPage( final String cursor);

    /**
     * delete all application records
     * @return
     */
    Observable deleteApplication();
}
