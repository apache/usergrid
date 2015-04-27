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

package org.apache.usergrid.corepersistence.pipeline.read.elasticsearch;


import org.apache.usergrid.corepersistence.pipeline.read.AbstractFilter;
import org.apache.usergrid.corepersistence.pipeline.read.CollectorFilter;
import org.apache.usergrid.corepersistence.pipeline.read.elasticsearch.impl.ElasticSearchQueryExecutor;
import org.apache.usergrid.corepersistence.pipeline.read.elasticsearch.impl.ResultsLoaderFactory;
import org.apache.usergrid.persistence.EntityRef;
import org.apache.usergrid.persistence.Query;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.persistence.SimpleEntityRef;
import org.apache.usergrid.persistence.index.ApplicationEntityIndex;
import org.apache.usergrid.persistence.index.EntityIndexFactory;
import org.apache.usergrid.persistence.index.SearchEdge;
import org.apache.usergrid.persistence.index.SearchTypes;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.inject.Inject;

import rx.Observable;


/**
 * A command that will query and load elasticsearch
 *
 * On future iteration, this needs to be split into 2 commands 1 that loads the candidate results and validates the
 * versions then another that will search and load
 *
 * TODO, split this into 3 seperate observables
 *
 * 1) An observable that emits candidate results 2) An observable that validates versions by uuid (for Traverse
 * commands) 3) An observbale that emits Results as a collector (for final commands)
 */
public abstract class AbstractQueryElasticSearchCollectorFilter extends AbstractFilter<Results, Integer>
    implements CollectorFilter<Results> {


    protected final EntityIndexFactory applicationEntityIndex;
    protected final Query query;
    private int limit;


    @Inject
    protected AbstractQueryElasticSearchCollectorFilter( final EntityIndexFactory applicationEntityIndex, final Query query ) {
        this.applicationEntityIndex = applicationEntityIndex;
        this.query = query;
    }


    @Override
    public Observable<Results> call( final Observable<Id> idObservable ) {


        final ApplicationEntityIndex
            entityIndex = applicationEntityIndex.createApplicationEntityIndex( applicationScope );

        return idObservable.flatMap( id -> {

            //TODO, refactor this logic to use Observables.  make this a TraverseFilter and load entities with the entity loader collector
            final ResultsLoaderFactory resultsLoaderFactory = getResultsLoaderFactory( id );
            final SearchEdge searchEdge = getSearchEdge( id );
            final SearchTypes searchTypes = getSearchTypes();



            final Iterable<Results> executor =
                new ElasticSearchQueryExecutor( resultsLoaderFactory, entityIndex, applicationScope,
                    searchEdge, searchTypes, query.withLimit( limit ) );

            return Observable.from( executor );
        } );
    }


    /**
     * Get the search types
     */
    protected abstract SearchTypes getSearchTypes();

    /**
     * Get the search edge
     */
    protected abstract SearchEdge getSearchEdge(final Id incomingId);


    /**
     * Get the results loader factor
     */
    protected abstract ResultsLoaderFactory getResultsLoaderFactory( final Id incomingId );


    @Override
    protected Class<Integer> getCursorClass() {
        return Integer.class;
    }


    @Override
    public void setLimit( final int limit ) {
        this.limit = limit;
    }


    /**
     * Get an entiity ref from the Id.  TODO refactor this away
     * @param id
     * @return
     */
    protected EntityRef getRef(final Id id){
        return new SimpleEntityRef( id.getType(), id.getUuid() );
    }
}
