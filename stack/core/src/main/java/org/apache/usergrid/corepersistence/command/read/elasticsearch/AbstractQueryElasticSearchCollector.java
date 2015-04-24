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

package org.apache.usergrid.corepersistence.command.read.elasticsearch;


import java.util.Iterator;

import org.apache.usergrid.corepersistence.command.read.AbstractCommand;
import org.apache.usergrid.corepersistence.command.read.Collector;
import org.apache.usergrid.corepersistence.command.read.elasticsearch.impl.ElasticSearchQueryExecutor;
import org.apache.usergrid.corepersistence.results.QueryExecutor;
import org.apache.usergrid.corepersistence.command.read.elasticsearch.impl.ResultsLoaderFactory;
import org.apache.usergrid.persistence.Query;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.index.ApplicationEntityIndex;
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
 * 1) An observable that emits candidate results
 * 2) An observable that validates versions by uuid (for Traverse commands)
 * 3) An observbale that emits Results as a collector (for final commands)
 */
public abstract class AbstractQueryElasticSearchCollector extends AbstractCommand<Results, Integer> implements
    Collector<Results> {


    private final ResultsLoaderFactory resultsLoaderFactory;
    private final ApplicationEntityIndex entityIndex;
                                           private final ApplicationScope applicationScope;
    private final SearchEdge indexScope;
    private final SearchTypes searchTypes;
    private final Query query;
    private int limit;



    protected AbstractQueryElasticSearchCollector( final ApplicationEntityIndex entityIndex,
                                                   final ApplicationScope applicationScope, final SearchEdge indexScope,
                                                   final SearchTypes searchTypes, final Query query ) {
        this.entityIndex = entityIndex;
        this.applicationScope = applicationScope;
        this.indexScope = indexScope;
        this.searchTypes = searchTypes;
        this.query = query;
        this.resultsLoaderFactory = getResultsLoaderFactory();
    }


    @Override
    public Observable<Results> call( final Observable<Id> idObservable ) {
       final Iterable<Results> executor =
            new ElasticSearchQueryExecutor( resultsLoaderFactory, entityIndex, applicationScope, indexScope, searchTypes,
                query.withLimit( limit ));

        return Observable.from(executor);
    }


    /**
     * Get the results loader factor
     * @return
     */
    protected abstract ResultsLoaderFactory getResultsLoaderFactory();

    @Override
    protected Class<Integer> getCursorClass() {
        return Integer.class;
    }


    @Override
    public void setLimit( final int limit ) {
        this.limit = limit;
    }
}
