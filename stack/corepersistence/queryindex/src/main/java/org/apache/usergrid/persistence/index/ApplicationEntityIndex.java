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


import java.util.UUID;

import org.apache.usergrid.persistence.model.entity.Id;

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
     * @param offset The offset to query on
     * @return
     */
    CandidateResults search( final SearchEdge searchEdge, final SearchTypes searchTypes, final String query,
                             final int limit, final int offset );


    /**
     * Same as search, just iterates all documents that match the index edge exactly.
     * @param edge The edge to search on
     * @param entityId The entity that the searchEdge is connected to.
     * @param limit The limit of the values to return per search.
     * @param offset The offset to page the query on.
     * @return
     */
    CandidateResults getAllEdgeDocuments(final IndexEdge edge, final Id entityId,  final int limit, final int offset);

    /**
     * Returns all entity documents that match the entityId and come before the marked version
     * @param entityId The entityId to match when searching
     * @param markedVersion The version that has been marked for deletion. All version before this one must be deleted.
     * @param limit The limit of the values to return per search.
     * @param offset The offset to page the query on.
     * @return
     */
    CandidateResults getAllEntityVersionsBeforeMarkedVersion( final Id entityId, final UUID markedVersion );

    /**
     * delete all application records
     * @return
     */
    Observable deleteApplication();
}
