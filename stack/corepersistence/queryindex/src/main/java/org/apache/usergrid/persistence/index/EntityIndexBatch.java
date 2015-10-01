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


import java.util.List;
import java.util.UUID;

import org.apache.usergrid.persistence.index.impl.IndexOperationMessage;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import rx.Observable;


public interface EntityIndexBatch {

    /**
     * Create index for Entity
     *
     * @param indexEdge  The edge to index the document into
     * @param entity     Entity to be indexed.
     */
    EntityIndexBatch index( final IndexEdge indexEdge, final Entity entity );

    /**
     * Remove index of entity
     *
     * @param searchEdge  The searchEdge for the entity
     * @param entity Entity to be removed from index.
     */
    EntityIndexBatch deindex( final SearchEdge searchEdge, final Entity entity );

    /**
     * Remove index of entity.
     *
     * @param searchEdge  The searchEdge to use for removal
     * @param result CandidateResult to be removed from index.
     */
    EntityIndexBatch deindex( final SearchEdge searchEdge, final CandidateResult result );

    /**
     * Remove index of entity.
     *
     * @param searchEdge   The searchEdge to remove
     * @param id      Id to be removed from index.
     * @param version Version to be removed from index.
     */
    EntityIndexBatch deindex( final SearchEdge searchEdge, final Id id, final UUID version );




    /**
     * get the batches
     * @return future to guarantee execution
     */
    IndexOperationMessage build();

    /**
     * Get the number of operations in the batch
     * @return
     */
    int size();
}
