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

package org.apache.usergrid.corepersistence.pipeline.read.entity;


import java.util.List;

import org.apache.usergrid.corepersistence.pipeline.read.AbstractPipelineOperation;
import org.apache.usergrid.corepersistence.pipeline.read.Collector;
import org.apache.usergrid.corepersistence.pipeline.read.ResultsPage;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.collection.EntitySet;
import org.apache.usergrid.persistence.collection.MvccEntity;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.inject.Inject;

import rx.Observable;


/**
 * Loads entities from a set of Ids.
 *
 * TODO refactor this into a common command that both ES search and graphSearch can use for repair and verification
 */
public class EntityLoadCollector extends AbstractPipelineOperation<Id, ResultsPage>
    implements Collector<Id, ResultsPage> {

    private final EntityCollectionManagerFactory entityCollectionManagerFactory;


    @Inject
    public EntityLoadCollector( final EntityCollectionManagerFactory entityCollectionManagerFactory ) {
        this.entityCollectionManagerFactory = entityCollectionManagerFactory;
    }


    @Override
    public Observable<ResultsPage> call( final Observable<Id> observable ) {


        final EntityCollectionManager entityCollectionManager =
            entityCollectionManagerFactory.createCollectionManager( pipelineContext.getApplicationScope() );

        final Observable<EntitySet> entitySetObservable = observable.buffer( pipelineContext.getLimit() ).flatMap(
            bufferedIds -> Observable.just( bufferedIds ).flatMap( ids -> entityCollectionManager.load( ids ) ) );


        final Observable<ResultsPage> resultsObservable = entitySetObservable

            .flatMap( entitySet -> {

                //get our entites and filter missing ones, then collect them into a results object
                final Observable<MvccEntity> mvccEntityObservable = Observable.from( entitySet.getEntities() );


                //convert them to our old entity model, then filter abscent, meaning they weren't found
                final Observable<List<Entity>> entitiesPageObservable =
                    mvccEntityObservable.filter( mvccEntity -> mvccEntity.getEntity().isPresent() )
                                        .map( mvccEntity -> mvccEntity.getEntity().get() ).toList();

                //convert them to a list, then map them into results
                return entitiesPageObservable.map( entities -> new ResultsPage( entities ) );
            } );


        return resultsObservable;
    }

    /**
     * Map a new cp entity to an old entity.  May be null if not present
     */


}
