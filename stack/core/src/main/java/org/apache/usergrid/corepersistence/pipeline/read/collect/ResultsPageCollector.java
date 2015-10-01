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

package org.apache.usergrid.corepersistence.pipeline.read.collect;


import java.util.ArrayList;
import java.util.List;

import org.apache.usergrid.corepersistence.pipeline.PipelineContext;
import org.apache.usergrid.corepersistence.pipeline.cursor.ResponseCursor;
import org.apache.usergrid.corepersistence.pipeline.read.AbstractFilter;
import org.apache.usergrid.corepersistence.pipeline.read.EdgePath;
import org.apache.usergrid.corepersistence.pipeline.read.FilterResult;
import org.apache.usergrid.corepersistence.pipeline.read.ResultsPage;

import com.google.common.base.Optional;

import rx.Observable;


/**
 * Takes entities and collects them into results.  This mostly exists for 1.0 compatibility.  Eventually this will
 * become the only collector in our pipeline and be used when rendering results, both on GET, PUT and POST.
 *
 *
 *
 * @param T the type of element to be collected
 */
public class ResultsPageCollector<T> extends AbstractFilter<FilterResult<T>, ResultsPage<T>> {


    protected PipelineContext pipelineContext;


    @Override
    public void setContext( final PipelineContext pipelineContext ) {
        this.pipelineContext = pipelineContext;
    }



    @Override
    public Observable<ResultsPage<T>> call( final Observable<FilterResult<T>> filterResultObservable ) {

        final int limit = pipelineContext.getLimit();

        return filterResultObservable
            .buffer( limit )
            .flatMap( buffer
                -> Observable
                    .from( buffer )
                    .collect(() -> new ResultsPageWithCursorCollector( limit ), ( collector, element ) -> collector.add( element ) )
            )
            .map( resultsPageCollector ->
                new ResultsPage(
                    resultsPageCollector.results,
                    new ResponseCursor( resultsPageCollector.lastPath ), pipelineContext.getLimit()
                )
            );
    }


    /**
     * A collector that will aggregate our results together
     */
    private class ResultsPageWithCursorCollector {


        private final List<T> results;

        private Optional<EdgePath> lastPath;


        private ResultsPageWithCursorCollector( final int limit ) {
            this.results = new ArrayList<>( limit );
        }


        public void add( final FilterResult<T> result ) {
            this.results.add( result.getValue() );
            this.lastPath = result.getPath();
        }
    }
}
