package org.apache.usergrid.persistence.index;/*
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


import java.util.UUID;

import org.apache.usergrid.persistence.core.future.BetterFuture;
import org.apache.usergrid.persistence.index.query.CandidateResult;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;


public interface EntityIndexBatch {

    /**
     * Create index for Entity
     *
     * @param indexScope The scope for the index
     * @param entity     Entity to be indexed.
     */
    public EntityIndexBatch index(final IndexScope indexScope, final Entity entity);

    /**
     * Remove index of entity
     *
     * @param scope  The scope for the entity
     * @param entity Entity to be removed from index.
     */
    public EntityIndexBatch deindex(final IndexScope scope, final Entity entity);

    /**
     * Remove index of entity.
     *
     * @param scope  The scope to use for removal
     * @param result CandidateResult to be removed from index.
     */
    public EntityIndexBatch deindex(final IndexScope scope, final CandidateResult result);

    /**
     * Remove index of entity.
     *
     * @param scope   The scope to remove
     * @param id      Id to be removed from index.
     * @param version Version to be removed from index.
     */
    public EntityIndexBatch deindex(final IndexScope scope, final Id id, final UUID version);


    /**
     * Execute the batch
     * @return future to guarantee execution
     */
    public BetterFuture execute();

    /**
     * Get the number of operations in the batch
     * @return
     */
    public int size();
}
