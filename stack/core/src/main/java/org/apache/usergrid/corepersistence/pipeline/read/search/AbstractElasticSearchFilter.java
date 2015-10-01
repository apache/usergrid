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

package org.apache.usergrid.corepersistence.pipeline.read.search;


import org.apache.usergrid.corepersistence.index.IndexLocationStrategyFactory;
import org.apache.usergrid.persistence.index.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.corepersistence.pipeline.cursor.CursorSerializer;
import org.apache.usergrid.corepersistence.pipeline.read.AbstractPathFilter;
import org.apache.usergrid.corepersistence.pipeline.read.FilterResult;
import org.apache.usergrid.persistence.core.metrics.MetricsFactory;
import org.apache.usergrid.persistence.core.metrics.ObservableTimer;
import org.apache.usergrid.persistence.model.entity.Id;

import com.codahale.metrics.Timer;
import com.google.common.base.Optional;

import rx.Observable;

import java.util.Collection;


/**
 * Command for reading graph edges
 */
public abstract class AbstractElasticSearchFilter extends AbstractPathFilter<Id, Candidate, Integer> {

    private static final Logger log = LoggerFactory.getLogger( AbstractElasticSearchFilter.class );

    private final EntityIndexFactory entityIndexFactory;
    private final IndexLocationStrategyFactory indexLocationStrategyFactory;
    private final String query;
    private final Timer searchTimer;


    /**
     * Create a new instance of our command
     */
    public AbstractElasticSearchFilter( final EntityIndexFactory entityIndexFactory,
                                        final MetricsFactory metricsFactory,
                                        final IndexLocationStrategyFactory indexLocationStrategyFactory,
                                        final String query ) {
        this.entityIndexFactory = entityIndexFactory;
        this.indexLocationStrategyFactory = indexLocationStrategyFactory;
        this.query = query;
        this.searchTimer = metricsFactory.getTimer( AbstractElasticSearchFilter.class, "query.search" );
    }


    @Override
    public Observable<FilterResult<Candidate>> call( final Observable<FilterResult<Id>> observable ) {

        //get the graph manager
        final EntityIndex applicationEntityIndex =
            entityIndexFactory.createEntityIndex(indexLocationStrategyFactory.getIndexLocationStrategy(pipelineContext.getApplicationScope()) );


        final int limit = pipelineContext.getLimit();


        final SearchTypes searchTypes = getSearchTypes();


        //return all ids that are emitted from this edge
        return observable.flatMap( idFilterResult -> {

            final SearchEdge searchEdge = getSearchEdge( idFilterResult.getValue() );


            final Observable<FilterResult<Candidate>> candidates = Observable.create( subscriber -> {

                //our offset to our start value.  This will be set the first time we emit
                //after we receive new ids, we want to reset this to 0
                //set our our constant state
                final Optional<Integer> startFromCursor = getSeekValue();

                final int startOffset = startFromCursor.or( 0 );

                int currentOffSet = startOffset;

                subscriber.onStart();

                //emit while we have values from ES and someone is subscribed
                while ( !subscriber.isUnsubscribed() ) {

                    try {
                        final CandidateResults candidateResults =
                            applicationEntityIndex.search( searchEdge, searchTypes, query, limit, currentOffSet );


                        Collection<SelectFieldMapping> fieldMappingCollection = candidateResults.getGetFieldMappings();


                        for( CandidateResult candidateResult: candidateResults){

                            //our subscriber unsubscribed, break out
                            if(subscriber.isUnsubscribed()){
                                return;
                            }

                            final Candidate candidate = new Candidate( candidateResult, searchEdge, fieldMappingCollection );

                            final FilterResult<Candidate>
                                result = createFilterResult( candidate, currentOffSet, idFilterResult.getPath() );

                            subscriber.onNext( result );

                            currentOffSet++;
                        }

                        /**
                         * No candidates, we're done
                         */
                        if (candidateResults.size() < limit) {
                            subscriber.onCompleted();
                            return;
                        }

                    }
                    catch ( Throwable t ) {

                        log.error( "Unable to search candidates", t );
                        subscriber.onError( t );
                    }
                }
            } );


            //add a timer around our observable
            ObservableTimer.time( candidates, searchTimer );

            return candidates;
        } );
    }


    @Override
    protected CursorSerializer<Integer> getCursorSerializer() {
        return ElasticsearchCursorSerializer.INSTANCE;
    }


    /**
     * Get the search edge from the id
     */
    protected abstract SearchEdge getSearchEdge( final Id id );

    /**
     * Get the search types
     */
    protected abstract SearchTypes getSearchTypes();
}
