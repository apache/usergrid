/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.usergrid.corepersistence.pipeline.builder;


import org.apache.usergrid.corepersistence.pipeline.read.FilterFactory;
import org.apache.usergrid.corepersistence.pipeline.Pipeline;
import org.apache.usergrid.corepersistence.pipeline.read.FilterResult;
import org.apache.usergrid.corepersistence.pipeline.read.search.Candidate;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;


public class CandidateBuilder {


    private final Pipeline<FilterResult<Candidate>> pipeline;
    private final FilterFactory filterFactory;


    public CandidateBuilder( final Pipeline<FilterResult<Candidate>> pipeline,
                             final FilterFactory filterFactory ) {
        this.pipeline = pipeline;
        this.filterFactory = filterFactory;
    }


    /**
     * Validates all candidates for the versions by id and sets them
     * @return
     */
    public IdBuilder loadIds(){

        final Pipeline<FilterResult<Id>> newFilter = pipeline.withFilter( filterFactory.candidateResultsIdVerifyFilter() );

        return new IdBuilder( newFilter, filterFactory );
    }


    /**
     * Load all the candidates as entities and return them
     * @return
     */
    public EntityBuilder loadEntities(){

        final Pipeline<FilterResult<Entity>> newFilter = pipeline.withFilter( filterFactory.candidateEntityFilter() );

        return new EntityBuilder(newFilter  );
    }
}
