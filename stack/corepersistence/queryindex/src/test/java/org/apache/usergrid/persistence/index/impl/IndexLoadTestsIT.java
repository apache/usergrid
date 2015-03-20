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
package org.apache.usergrid.persistence.index.impl;


import java.util.List;
import java.util.UUID;

import org.apache.usergrid.persistence.index.ApplicationEntityIndex;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.core.test.UseModules;
import org.apache.usergrid.persistence.index.EntityIndex;
import org.apache.usergrid.persistence.index.EntityIndexFactory;
import org.apache.usergrid.persistence.index.IndexScope;
import org.apache.usergrid.persistence.index.guice.IndexTestFig;
import org.apache.usergrid.persistence.index.guice.TestIndexModule;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.field.IntegerField;

import com.google.inject.Inject;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;


/**
 * TODO: make CorePerformanceIT configurable, add CHOP markup.
 */
@RunWith( EsRunner.class )
@UseModules( { TestIndexModule.class } )
@Ignore( "Should only be run during load tests of elasticsearch" )
public class IndexLoadTestsIT extends BaseIT {
    private static final Logger log = LoggerFactory.getLogger( IndexLoadTestsIT.class );

    @ClassRule
    public static ElasticSearchResource es = new ElasticSearchResource();


    @Inject
    public IndexTestFig indexTestFig;

    @Inject
    public EntityIndexFactory entityIndexFactory;

    @Inject
    public EntityIndex index;


    @Test
    public void testHeavyLoad() {

        final UUID applicationUUID = UUID.fromString( indexTestFig.getApplicationId() );

        final Id applicationId = new SimpleId( applicationUUID, "application" );
        final ApplicationScope scope = new ApplicationScopeImpl( applicationId );


        //create our index if it doesn't exist
        index.initializeIndex();

        ApplicationEntityIndex applicationEntityIndex = entityIndexFactory.createApplicationEntityIndex(scope);
        final Observable<Entity> createEntities = createStreamFromWorkers( applicationEntityIndex, applicationId );

        //run them all
        createEntities.toBlocking().last();
    }


    public Observable<Entity> createStreamFromWorkers( final ApplicationEntityIndex entityIndex, final Id ownerId ) {

        //create a sequence of observables.  Each index will be it's own worker thread using the Schedulers.newthread()
        return Observable.range( 0, indexTestFig.getNumberOfWorkers() ).flatMap(
            integer -> createWriteObservable( entityIndex, ownerId, integer ).subscribeOn( Schedulers.newThread() ) );
    }


    private Observable<Entity> createWriteObservable( final ApplicationEntityIndex entityIndex, final Id ownerId,
                                                      final int workerIndex ) {


        final IndexScope scope = new IndexScopeImpl( ownerId, "test" );


        return Observable.range( 0, indexTestFig.getNumberOfRecords() )

            //create our entity
            .map( new Func1<Integer, Entity>() {
                @Override
                public Entity call( final Integer integer ) {
                    final Entity entity = new Entity( "test" );

                    entity.setField( new IntegerField( "workerIndex", workerIndex ) );
                    entity.setField( new IntegerField( "ordinal", integer ) );

                    return entity;
                }
            } ).buffer( indexTestFig.getBufferSize() ).doOnNext( new Action1<List<Entity>>() {
                @Override
                public void call( final List<Entity> entities ) {
                    //take our entities and roll them into a batch
                    Observable.from( entities )
                              .collect( () -> entityIndex.createBatch(), ( entityIndexBatch, entity ) -> {

                                  entityIndexBatch.index( scope, entity );
                              } ).doOnNext( entityIndexBatch -> {
                        entityIndexBatch.execute();
                    } ).toBlocking().last();
                }
            } )

                //translate back into a stream of entities for the caller to use
            .flatMap(entities -> Observable.from( entities ) );
    }
}
