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

package org.apache.usergrid.corepersistence.command.read.entity;


import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.apache.usergrid.corepersistence.command.read.AbstractCommand;
import org.apache.usergrid.corepersistence.command.read.CollectCommand;
import org.apache.usergrid.corepersistence.util.CpEntityMapUtils;
import org.apache.usergrid.persistence.EntityFactory;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.collection.EntitySet;
import org.apache.usergrid.persistence.collection.MvccEntity;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;

import rx.Observable;
import rx.functions.Func1;
import rx.observables.GroupedObservable;

import static org.apache.usergrid.corepersistence.util.CpNamingUtils.getCollectionScopeNameFromEntityType;


/**
 * Loads entities from a set of Ids.
 *
 * TODO refactor this into a common command that both ES search and graphSearch can use for repair and verification
 */
public class EntityLoadCommand extends AbstractCommand<Results, Serializable> implements CollectCommand<Results> {

    private final EntityCollectionManagerFactory entityCollectionManagerFactory;

    //TODO get rid of this when merged into 2.0 dev
    private final ApplicationScope applicationScope;
    private int resultSize;


    public EntityLoadCommand( final EntityCollectionManagerFactory entityCollectionManagerFactory,
                              final ApplicationScope applicationScope ) {
        this.entityCollectionManagerFactory = entityCollectionManagerFactory;
        this.applicationScope = applicationScope;
    }


    @Override
    protected Class<Serializable> getCursorClass() {
        return null;
    }


    @Override
    public Observable<Results> call( final Observable<? extends Id> observable ) {


        /**
         * A bit kludgy from old 1.0 -> 2.0 apis.  Refactor this as we clean up our lower levels and create new results
         * objects
         */


        return observable.buffer( resultSize ).flatMap( new Func1<List<? extends Id>, Observable<Results>>() {
            @Override
            public Observable<Results> call( final List<? extends Id> ids ) {

                return Observable.from( ids )
                    //group them by type so we can load them, in 2.0 dev this step will be removed
                    .groupBy( new Func1<Id, String>() {
                        @Override
                        public String call( final Id id ) {
                            return id.getType();
                        }
                    } )

                        //take all our groups and load them as id sets

                    .flatMap( new Func1<GroupedObservable<String, Id>, Observable<EntitySet>>() {
                        @Override
                        public Observable<EntitySet> call(
                            final GroupedObservable<String, Id> stringIdGroupedObservable ) {


                            final String entityType = stringIdGroupedObservable.getKey();

                            final CollectionScope collectionScope =
                                getCollectionScopeNameFromEntityType( applicationScope.getApplication(), entityType );


                            return stringIdGroupedObservable.toList()
                                                            .flatMap( new Func1<List<Id>, Observable<EntitySet>>() {
                                                                @Override
                                                                public Observable<EntitySet> call(
                                                                    final List<Id> ids ) {

                                                                    final EntityCollectionManager ecm =
                                                                        entityCollectionManagerFactory
                                                                            .createCollectionManager( collectionScope );
                                                                    return ecm.load( ids );
                                                                }
                                                            } );
                        }
                    } )
                        //emit our groups of entities as a stream of entities
                    .flatMap( new Func1<EntitySet, Observable<org.apache.usergrid.persistence.Entity>>() {
                        @Override
                        public Observable<org.apache.usergrid.persistence.Entity> call( final EntitySet entitySet ) {
                            //emit our entities, and filter out deleted entites
                            return Observable.from( entitySet.getEntities() ).map(
                                new Func1<MvccEntity, org.apache.usergrid.persistence.Entity>() {

                                    @Override
                                    public org.apache.usergrid.persistence.Entity call( final MvccEntity mvccEntity ) {
                                        return mapEntity( mvccEntity );
                                    }
                                } )
                                //filter null entities
                                .filter( new Func1<org.apache.usergrid.persistence.Entity, Boolean>() {
                                    @Override
                                    public Boolean call( final org.apache.usergrid.persistence.Entity entity ) {
                                        return entity == null;
                                    }
                                } );
                        }
                    } )

                        //convert them to a list, then map them into results
                    .toList().map( new Func1<List<org.apache.usergrid.persistence.Entity>, Results>() {
                        @Override
                        public Results call( final List<org.apache.usergrid.persistence.Entity> entities ) {
                            final Results results = Results.fromEntities( entities );
                            results.setCursor( generateCursor() );

                            return results;
                        }
                    } );
            }
        } );
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
    public void setLimit( final int resultSize ) {
        this.resultSize = resultSize;
    }
}
