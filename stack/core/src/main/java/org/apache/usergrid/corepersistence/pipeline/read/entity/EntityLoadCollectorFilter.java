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


import java.io.Serializable;
import java.util.Map;

import org.apache.usergrid.corepersistence.pipeline.read.AbstractFilter;
import org.apache.usergrid.corepersistence.pipeline.read.CollectorFilter;
import org.apache.usergrid.corepersistence.util.CpEntityMapUtils;
import org.apache.usergrid.persistence.EntityFactory;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.collection.EntitySet;
import org.apache.usergrid.persistence.collection.MvccEntity;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.inject.Inject;

import rx.Observable;


/**
 * Loads entities from a set of Ids.
 *
 * TODO refactor this into a common command that both ES search and graphSearch can use for repair and verification
 */
public class EntityLoadCollectorFilter extends AbstractFilter<Results, Serializable>
    implements CollectorFilter<Results> {

    private final EntityCollectionManagerFactory entityCollectionManagerFactory;
    private final ApplicationScope applicationScope;
    private int resultSize;


    @Inject
    public EntityLoadCollectorFilter( final EntityCollectionManagerFactory entityCollectionManagerFactory,
                                      final ApplicationScope applicationScope ) {
        this.entityCollectionManagerFactory = entityCollectionManagerFactory;
        this.applicationScope = applicationScope;
    }


    @Override
    protected Class<Serializable> getCursorClass() {
        return null;
    }


    @Override
    public Observable<Results> call( final Observable<Id> observable ) {


        /**
         * A bit kludgy from old 1.0 -> 2.0 apis.  Refactor this as we clean up our lower levels and create new results
         * objects
         */

        final EntityCollectionManager entityCollectionManager =
            entityCollectionManagerFactory.createCollectionManager( applicationScope );

        final Observable<EntitySet> entitySetObservable = observable.buffer( resultSize ).flatMap(
            bufferedIds -> Observable.just( bufferedIds ).flatMap( ids -> entityCollectionManager.load( ids ) ) );


        final Observable<Results> resultsObservable =  entitySetObservable

            .flatMap( entitySet -> {

            //get our entites and filter missing ones, then collect them into a results object
            final Observable<MvccEntity> mvccEntityObservable = Observable.from( entitySet.getEntities() );

            //convert them to our old entity model, then filter nulls, meaning they weren't found
            return mvccEntityObservable.map( mvccEntity -> mapEntity( mvccEntity ) ).filter( entity -> entity == null )

                //convert them to a list, then map them into results
                .toList().map( entities -> {
                    final Results results = Results.fromEntities( entities );
                    results.setCursor( generateCursor() );

                    return results;
                } )
                //if no results are present, return an empty results
                .singleOrDefault( new Results(  ) );
        } );


        return resultsObservable;
    }

                /**
                 * Map a new cp entity to an old entity.  May be null if not present
                 */


    private org.apache.usergrid.persistence.Entity mapEntity( final MvccEntity mvccEntity ) {
        if ( !mvccEntity.getEntity().isPresent() ) {
            return null;
        }


        final Entity cpEntity = mvccEntity.getEntity().get();
        final Id entityId = cpEntity.getId();

        org.apache.usergrid.persistence.Entity entity =
            EntityFactory.newEntity( entityId.getUuid(), entityId.getType() );

        Map<String, Object> entityMap = CpEntityMapUtils.toMap( cpEntity );
        entity.addProperties( entityMap );

        return entity;
    }


    @Override
    public void setLimit( final int limit ) {
        this.resultSize = limit;
    }
}
