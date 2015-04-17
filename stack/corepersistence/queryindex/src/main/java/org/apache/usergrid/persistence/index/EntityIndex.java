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

import java.util.UUID;

import org.apache.usergrid.persistence.core.CPManager;
import org.apache.usergrid.persistence.core.util.Health;
import org.apache.usergrid.persistence.index.query.Query;
import org.apache.usergrid.persistence.index.query.CandidateResults;
import org.apache.usergrid.persistence.model.entity.Id;
import org.elasticsearch.action.ListenableActionFuture;
import rx.Observable;

import java.util.Map;
import java.util.concurrent.Future;


/**
 * Provides management operations for single index
 */
public interface EntityIndex extends CPManager {


    public static final int MAX_LIMIT = 1000;

    /**
     * Create an index and add to alias, will create alias and remove any old index from write alias if alias already exists
     * @param indexSuffix index name
     * @param shards
     * @param replicas
     * @param writeConsistency
     */
     void addIndex(final String indexSuffix, final int shards, final int replicas, final String writeConsistency);


    /**
     * Refresh the index.
     */
     void refresh();



    /**
     * Check health of cluster.
     */
     Health getClusterHealth();

    /**
     * Check health of this specific index.
     */
     Health getIndexHealth();


    void initialize();

    boolean shouldInitialize();
}


