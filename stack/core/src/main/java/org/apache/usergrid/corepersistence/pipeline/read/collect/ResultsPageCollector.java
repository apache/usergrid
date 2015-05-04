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

import org.apache.usergrid.corepersistence.pipeline.cursor.ResponseCursor;
import org.apache.usergrid.corepersistence.pipeline.read.Collector;
import org.apache.usergrid.corepersistence.pipeline.read.EdgePath;
import org.apache.usergrid.corepersistence.pipeline.read.FilterResult;
import org.apache.usergrid.corepersistence.pipeline.read.ResultsPage;
import org.apache.usergrid.persistence.model.entity.Entity;

import com.google.common.base.Optional;

import rx.Observable;


/**
 * Takes entities and collects them into results.  This mostly exists for 1.0 compatibility.  Eventually this will
 * become the only collector in our pipline and be used when rendering results, both on GET, PUT and POST.
 */
public class ResultsPageCollector extends AbstractCollector<Entity, ResultsPage>
    implements Collector<Entity, ResultsPage> {


    @Override
    public Observable<ResultsPage> call( final Observable<FilterResult<Entity>> filterResultObservable ) {

        final int limit = pipelineContext.getLimit();

        return filterResultObservable.buffer( limit ).flatMap( buffer -> Observable.from( buffer ).collect(
            () -> new ResultsPageWithCursorCollector( limit ), ( collector, entity ) -> {
                collector.add( entity );
            } ) ).map( resultsPageCollector -> new ResultsPage( resultsPageCollector.results,
            new ResponseCursor( resultsPageCollector.lastPath ), pipelineContext.getLimit() ) );
    }


    /**
     * A collector that will aggregate our results together
     */
    private static class ResultsPageWithCursorCollector {


        private final List<Entity> results;

        private Optional<EdgePath> lastPath;


        private ResultsPageWithCursorCollector( final int limit ) {
            this.results = new ArrayList<>( limit );
        }


        public void add( final FilterResult<Entity> result ) {
            this.results.add( result.getValue() );
            this.lastPath = result.getPath();
        }
    }
}
