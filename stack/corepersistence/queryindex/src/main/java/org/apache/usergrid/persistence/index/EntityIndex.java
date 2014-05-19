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

import org.apache.usergrid.persistence.index.impl.CandidateResult;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.index.query.Query;
import org.apache.usergrid.persistence.index.query.CandidateResults;
import org.apache.usergrid.persistence.model.entity.Id;

import java.util.UUID;


/**
 * Provides indexing of Entities within a scope.
 */
public interface EntityIndex {

    /** 
     * Create index for Entity
     * @param entity Entity to be indexed.
     */
    public void index(  Entity entity );

    /**
     * Remove index of entity.
     * @param entity Entity to be removed from index. 
     */
    public void deindex( Entity entity );

    /**
     * Remove index of entity.
     * @param result CandidateResult to be removed from index.
     */
    public void deindex( CandidateResult result );

    /**
     * Remove index of entity.
     * @param id Id to be removed from index.
     * @param version Version to be removed from index.
     */
    public void deindex( Id id, UUID version);

    /**
     * Execute query in Usergrid syntax.
     */

    public CandidateResults search( Query query );

    /**
     * Force refresh of index (should be used for testing purposes only).
     */
    public void refresh();

    public CandidateResults getEntityVersions(Id id);

}
