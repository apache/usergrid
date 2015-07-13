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
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.model.entity.Entity;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import rx.Observable;


/**
 * Implementation of the collection service
 */
@Singleton
public class CollectionServiceImpl implements CollectionService {


    private final PipelineBuilderFactory pipelineBuilderFactory;


    @Inject
    public CollectionServiceImpl( final PipelineBuilderFactory pipelineBuilderFactory ) {
        this.pipelineBuilderFactory = pipelineBuilderFactory;
    }


    @Override
    public Observable<ResultsPage<Entity>> searchCollection( final CollectionSearch search ) {


        final ApplicationScope applicationScope = search.getApplicationScope();
        final String collectionName = search.getCollectionName();
        final Optional<String> query = search.getQuery();

        final IdBuilder pipelineBuilder =
            pipelineBuilderFactory.create( applicationScope ).withCursor( search.getCursor() )
                                  .withLimit( search.getLimit() ).fromId( search.getCollectionOwnerId() );


        final EntityBuilder results;

        if ( !query.isPresent()) {
            results = pipelineBuilder.traverseCollection( collectionName ).loadEntities();
        }
        else {
            results = pipelineBuilder.searchCollection( collectionName, query.get(),search.getEntityType()).loadEntities();
        }


        return results.build();
    }
}
