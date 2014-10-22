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

import org.apache.usergrid.persistence.index.query.Query;
import org.apache.usergrid.persistence.index.query.CandidateResults;
import org.apache.usergrid.persistence.model.entity.Id;


/**
 * Provides indexing of Entities within a scope.
 */
public interface EntityIndex {

    /**
     * This should ONLY ever be called once on application create.  Otherwise we're introducing slowness into our system
     *
     */
    public void initializeIndex();

    /**
     * Create the index batch
     * @return
     */
    public EntityIndexBatch createBatch();

    /**
     * Execute query in Usergrid syntax.
     */

    public CandidateResults search(final IndexScope indexScope,  Query query );

    /**
     * Get the candidate results of all versions of the entity for this id
     * @param id
     * @return
     */
    public CandidateResults getEntityVersions(final IndexScope indexScope, Id id);

    /**
     * Refresh the index
     */
    public void refresh();

}
