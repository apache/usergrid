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

package org.apache.usergrid.persistence.index;


import org.apache.usergrid.persistence.core.CPManager;
import org.apache.usergrid.persistence.core.util.Health;
import org.apache.usergrid.persistence.model.entity.Id;
import rx.Observable;

import java.util.Map;


/**
 * Provides management operations for single index
 */
public interface EntityIndex extends CPManager {


    int MAX_LIMIT = 1000;

    /**
     * Create an index and add to alias, will create alias and remove any old index from write alias if alias already exists
     *
     * @param indexSuffix      index name
     * @param shards
     * @param replicas
     * @param writeConsistency
     */
    void addIndex(
        final String indexSuffix,
        final int shards,
        final int replicas,
        final String writeConsistency
    );

    /**
     * Refresh the index.
     */
    Observable<IndexRefreshCommandInfo> refreshAsync();


    /**
     * Check health of cluster.
     */
    Health getClusterHealth();

    /**
     * Check health of this specific index.
     */
    Health getIndexHealth();


    /**
     * get total entity size by an edge ->   "term":{"edgeName":"zzzcollzzz|roles"}
     *
     * @param edge
     * @return
     */
    long getTotalEntitySizeInBytes(final SearchEdge edge);

    /**
     * Initialize the index if necessary.  This is an idempotent operation and should not create an index
     * if a write and read alias already exist
     */
    void initialize();


    /**
     * Create the index batch.
     */
    EntityIndexBatch createBatch();

    /**
     * Search on every document in the specified search edge.  Also search by the types if specified
     *
     * @param searchEdge  The edge to search on
     * @param searchTypes The search types to search
     * @param query       The query to execute
     * @param limit       The limit of values to return
     * @param offset      The offset to query on
     * @param analyzeOnly This optional param will instruct the query processing to only analyze the query and
     *                          provide info but not actually execute the query.
     * @return
     */
    CandidateResults search(final SearchEdge searchEdge, final SearchTypes searchTypes, final String query,
                            final int limit, final int offset, final boolean analyzeOnly);

    /**
     * Search on every document in the specified search edge.  Also search by the types if specified
     *
     * @param searchEdge        The edge to search on
     * @param searchTypes       The search types to search
     * @param query             The query to execute
     * @param limit             The limit of values to return
     * @param offset            The offset to query on
     * @param fieldsWithType    An optional param that allows the caller to provide schema related info which might
     *                          relate to data in the query, such as sort predicate types
     * @param analyzeOnly       This optional param will instruct the query processing to only analyze the query and
     *                          provide info but not actually execute the query.
     * @return
     */
    CandidateResults search(final SearchEdge searchEdge, final SearchTypes searchTypes, final String query,
                            final int limit, final int offset, final Map<String, Class> fieldsWithType,
                            final boolean analyzeOnly);

    /**
     * Search on every document in the specified search edge.  Also search by the types if specified
     *
     * @param searchEdge        The edge to search on
     * @param searchTypes       The search types to search
     * @param query             The query to execute
     * @param limit             The limit of values to return
     * @param offset            The offset to query on
     * @param fieldsWithType    An optional param that allows the caller to provide schema related info which might
     *                          relate to data in the query, such as sort predicate types
     * @param analyzeOnly       This optional param will instruct the query processing to only analyze the query and
     *                          provide info but not actually execute the query.
     * @param returnQuery       This optional param will cause the index query to be returned instead of run.
     * @return
     */
    CandidateResults search(final SearchEdge searchEdge, final SearchTypes searchTypes, final String query,
                            final int limit, final int offset, final Map<String, Class> fieldsWithType,
                            final boolean analyzeOnly, final boolean returnQuery);


    /**
     * Same as search, just iterates all documents that match the index edge exactly.
     *
     * @param edge     The edge to search on
     * @param entityId The entity that the searchEdge is connected to.
     * @return
     */
    CandidateResults getAllEdgeDocuments(final IndexEdge edge, final Id entityId);

    /**
     * delete all application records
     *
     * @return
     */
    Observable deleteApplication();

    /**
     * Get the indexes for an alias
     *
     * @param aliasType name of alias
     * @return list of index names
     */
    String[] getIndexes(final AliasType aliasType);

    /**
     * get all unique indexes
     *
     * @return
     */
    String[] getIndexes();

    /**
     * type of alias
     */
    enum AliasType {
        Read, Write
    }
    class IndexRefreshCommandInfo{
        private final boolean hasFinished;
        private final long executionTime;

        public IndexRefreshCommandInfo(boolean hasFinished, long executionTime){
            this.hasFinished = hasFinished;
            this.executionTime = executionTime;
        }

        public boolean hasFinished() {
            return hasFinished;
        }

        public long getExecutionTime() {
            return executionTime;
        }
    }

}


