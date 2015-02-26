/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one or more
 *  *  contributor license agreements.  The ASF licenses this file to You
 *  * under the Apache License, Version 2.0 (the "License"); you may not
 *  * use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.  For additional information regarding
 *  * copyright in this work, please see the NOTICE file in the top level
 *  * directory of this distribution.
 *
 */
package org.apache.usergrid.persistence.collection.serialization.impl;


import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.EntitySet;
import org.apache.usergrid.persistence.collection.MvccEntity;
import org.apache.usergrid.persistence.collection.mvcc.MvccEntityMigrationStrategy;
import org.apache.usergrid.persistence.collection.serialization.MvccEntitySerializationStrategy;
import org.apache.usergrid.persistence.core.migration.data.CollectionDataMigration;
import org.apache.usergrid.persistence.core.migration.data.DataMigration;
import org.apache.usergrid.persistence.core.migration.data.DataMigrationException;
import org.apache.usergrid.persistence.core.migration.schema.MigrationStrategy;
import org.apache.usergrid.persistence.core.scope.ApplicationEntityGroup;
import org.apache.usergrid.persistence.core.scope.EntityIdScope;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.google.inject.Inject;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;


/**
 * Data migration strategy for entities
 */
public class MvccEntityDataMigrationImpl implements CollectionDataMigration {

    private final Keyspace keyspace;
    private final MvccEntityMigrationStrategy entityMigrationStrategy;


    @Inject
    public MvccEntityDataMigrationImpl( Keyspace keyspace, MvccEntityMigrationStrategy serializationStrategy ) {

        this.keyspace = keyspace;
        this.entityMigrationStrategy = serializationStrategy;
    }


    @Override
    public Observable migrate( final Observable<ApplicationEntityGroup> applicationEntityGroupObservable,
                               final DataMigration.ProgressObserver observer ) {
        final AtomicLong atomicLong = new AtomicLong();


        //capture the time the test starts
        final UUID now = UUIDGenerator.newTimeUUID();

        return applicationEntityGroupObservable.flatMap( new Func1<ApplicationEntityGroup, Observable<Long>>() {
            @Override
            public Observable<Long> call( final ApplicationEntityGroup applicationEntityGroup ) {
                final List<EntityIdScope<CollectionScope>> entityIds = applicationEntityGroup.entityIds;

                //go through each entity in the system, and load it's entire
                // history
                return Observable.from( entityIds ).subscribeOn( Schedulers.io() )
                                 .parallel( new Func1<Observable<EntityIdScope<CollectionScope>>, Observable<Long>>() {


                                     //process the ids in parallel
                                     @Override
                                     public Observable<Long> call(
                                             final Observable<EntityIdScope<CollectionScope>> entityIdScopeObservable
                                                                 ) {

                                         final MutationBatch totalBatch = keyspace.prepareMutationBatch();

                                         return entityIdScopeObservable.doOnNext(
                                                 new Action1<EntityIdScope<CollectionScope>>() {

                                                     //load the entity and add it to the toal mutation
                                                     @Override
                                                     public void call( final EntityIdScope<CollectionScope> idScope ) {

                                                         //load the entity
                                                         MigrationStrategy
                                                                 .MigrationRelationship<MvccEntitySerializationStrategy>
                                                                 migration = entityMigrationStrategy.getMigration();


                                                         CollectionScope currentScope = idScope.getCollectionScope();
                                                         //for each element in the history in the previous
                                                         // version,
                                                         // copy it to the CF in v2
                                                         EntitySet allVersions = migration.from().load( currentScope,
                                                                 Collections.singleton( idScope.getId() ), now );

                                                         final MvccEntity version =
                                                                 allVersions.getEntity( idScope.getId() );

                                                         final MutationBatch versionBatch =
                                                                 migration.to().write( currentScope, version );

                                                         totalBatch.mergeShallow( versionBatch );
                                                     }
                                                 } )
                                                 //every 100 flush the mutation
                                                 .buffer( 100 ).doOnNext(
                                                         new Action1<List<EntityIdScope<CollectionScope>>>() {
                                                             @Override
                                                             public void call(
                                                                     final List<EntityIdScope<CollectionScope>> ids ) {
                                                                 atomicLong.addAndGet( 100 );
                                                                 executeBatch( totalBatch, observer, atomicLong );
                                                             }
                                                         } )
                                                         //count the results
                                                 .reduce( 0l,
                                                         new Func2<Long, List<EntityIdScope<CollectionScope>>, Long>() {
                                                             @Override
                                                             public Long call( final Long aLong,
                                                                               final
                                                                               List<EntityIdScope<CollectionScope>>
                                                                                       ids ) {
                                                                 return aLong + ids.size();
                                                             }
                                                         } );
                                     }
                                 } );
            }
        } );
    }


    protected void executeBatch( final MutationBatch batch, final DataMigration.ProgressObserver po,
                                 final AtomicLong count ) {
        try {
            batch.execute();

            po.update( getVersion(), "Finished copying " + count + " entities to the new format" );
        }
        catch ( ConnectionException e ) {
            po.failed( getVersion(), "Failed to execute mutation in cassandra" );
            throw new DataMigrationException( "Unable to migrate batches ", e );
        }
    }


    @Override
    public int getVersion() {
        return entityMigrationStrategy.getVersion();
    }


    @Override
    public MigrationType getType() {
        return MigrationType.Entities;
    }
}
