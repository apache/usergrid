/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.usergrid.corepersistence.service;


import org.apache.usergrid.corepersistence.pipeline.builder.EntityBuilder;
import org.apache.usergrid.corepersistence.pipeline.builder.IdBuilder;
import org.apache.usergrid.corepersistence.pipeline.builder.PipelineBuilderFactory;
import org.apache.usergrid.corepersistence.pipeline.read.ResultsPage;
import org.apache.usergrid.persistence.ConnectionRef;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import rx.Observable;


@Singleton
public class ConnectionServiceImpl implements ConnectionService {

    private final PipelineBuilderFactory pipelineBuilderFactory;


    @Inject
    public ConnectionServiceImpl( final PipelineBuilderFactory pipelineBuilderFactory ) {
        this.pipelineBuilderFactory = pipelineBuilderFactory;
    }


    @Override
    public Observable<ResultsPage<Entity>> searchConnection( final ConnectionSearch search ) {
        //set startid -- graph | es query filter -- load entities filter (verifies exists) --> results page collector
        // -> 1.0 results

        //  startid -- graph edge load -- entity load (verify) from ids -> results page collector
        // startid -- eq query candiddate -- entity load (verify) from canddiates -> results page collector

        //startid -- graph edge load -- entity id verify --> filter to connection ref --> connection ref collector
        //startid -- eq query candiddate -- candidate id verify --> filter to connection ref --> connection ref
        // collector


        final Optional<String> query = search.getQuery();

        final IdBuilder pipelineBuilder =
            pipelineBuilderFactory.create( search.getApplicationScope() ).withCursor( search.getCursor() )
                                  .withLimit( search.getLimit() ).fromId( search.getSourceNodeId() );


        //we want to load all entities

        final EntityBuilder results;


        if ( !query.isPresent() ) {
            results =
                pipelineBuilder.traverseConnection( search.getConnectionName(), search.getEntityType() ).loadEntities();
        }

        else {

            results =
                pipelineBuilder.searchConnection( search.getConnectionName(), query.get(), search.getEntityType() )
                               .loadEntities();
        }


        return results.build();
    }


    @Override
    public Observable<ResultsPage<ConnectionRef>> searchConnectionAsRefs( final ConnectionSearch search ) {

        final Optional<String> query = search.getQuery();

        final Id sourceNodeId = search.getSourceNodeId();

        final IdBuilder pipelineBuilder =
            pipelineBuilderFactory.create( search.getApplicationScope() ).withCursor( search.getCursor() )
                                  .withLimit( search.getLimit() ).fromId( sourceNodeId );


        final IdBuilder traversedIds;
        final String connectionName = search.getConnectionName();

        if ( !query.isPresent() ) {
            traversedIds = pipelineBuilder.traverseConnection( connectionName, search.getEntityType() );
        }
        else {
            traversedIds =
                pipelineBuilder.searchConnection( connectionName, query.get(), search.getEntityType() ).loadIds();
        }

        //create connection refs

        final Observable<ResultsPage<ConnectionRef>> results =
            traversedIds.loadConnectionRefs( sourceNodeId, connectionName ).build();

        return results;
    }
}
