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
package org.apache.usergrid.persistence.collection.serialization.impl.migration;


import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.EntitySet;
import org.apache.usergrid.persistence.collection.MvccEntity;
import org.apache.usergrid.persistence.collection.mvcc.MvccEntityMigrationStrategy;
import org.apache.usergrid.persistence.collection.serialization.MvccEntitySerializationStrategy;
import org.apache.usergrid.persistence.core.migration.data.DataMigrationException;
import org.apache.usergrid.persistence.core.migration.data.newimpls.DataMigration2;
import org.apache.usergrid.persistence.core.migration.data.newimpls.MigrationDataProvider;
import org.apache.usergrid.persistence.core.migration.data.newimpls.ProgressObserver;
import org.apache.usergrid.persistence.core.migration.schema.MigrationStrategy;
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
public class MvccEntityDataMigrationImpl implements DataMigration2<EntityIdScope> {

    private final Keyspace keyspace;
    private final MvccEntityMigrationStrategy entityMigrationStrategy;


    @Inject
    public MvccEntityDataMigrationImpl( Keyspace keyspace, MvccEntityMigrationStrategy serializationStrategy ) {

        this.keyspace = keyspace;
        this.entityMigrationStrategy = serializationStrategy;
    }


    @Override
    public int getVersion() {
        return entityMigrationStrategy.getVersion();
    }


    @Override
    public void migrate( final MigrationDataProvider<EntityIdScope> migrationDataProvider,   final ProgressObserver observer ) {

        final AtomicLong atomicLong = new AtomicLong();


        //capture the time the test starts

        final UUID startTime = UUIDGenerator.newTimeUUID();

        final long migrated = migrationDataProvider.getData().subscribeOn( Schedulers.io() )
                   .parallel( new Func1<Observable<EntityIdScope>, Observable<Long>>() {


                       //process the ids in parallel
                       @Override
                       public Observable<Long> call(
                               final Observable<EntityIdScope>
                                       entityIdScopeObservable ) {

                           final MutationBatch totalBatch =
                                   keyspace.prepareMutationBatch();

                           return entityIdScopeObservable.doOnNext(
                                   new Action1<EntityIdScope>() {

                                       //load the entity and add it to the toal mutation
                                       @Override
                                       public void call( final EntityIdScope idScope ) {

                                           //load the entity
                                           MigrationStrategy
                                                   .MigrationRelationship<MvccEntitySerializationStrategy>
                                                   migration = entityMigrationStrategy
                                                   .getMigration();


                                           CollectionScope currentScope =
                                                   idScope.getCollectionScope();

                                           //for each element in the history in the
                                           // previous
                                           // version,
                                           // copy it to the CF in v2


                                           EntitySet allVersions = migration.from()
                                                                            .load( currentScope,
                                                                                    Collections
                                                                                            .singleton(
                                                                                                    idScope.getId() ),
                                                                                    startTime );

                                           final MvccEntity version = allVersions
                                                   .getEntity( idScope.getId() );

                                           final MutationBatch versionBatch =
                                                   migration.to().write( currentScope,
                                                           version );

                                           totalBatch.mergeShallow( versionBatch );
                                       }
                                   } )
                                   //every 100 flush the mutation
                                   .buffer( 100 ).doOnNext(
                                           new Action1<List<EntityIdScope>>() {
                                               @Override
                                               public void call(
                                                       final List<EntityIdScope> ids ) {
                                                   atomicLong.addAndGet( 100 );
                                                   executeBatch( totalBatch, observer,
                                                           atomicLong );
                                               }
                                           } )
                                           //count the results
                                   .reduce( 0l,
                                           new Func2<Long, List<EntityIdScope>, Long>
                                                   () {
                                               @Override
                                               public Long call( final Long aLong,
                                                                 final List<EntityIdScope> ids ) {
                                                   return aLong + ids.size();
                                               }
                                           } );
                       }
                   } ).toBlocking().last();

        //now we update the progress observer

        observer.update( getVersion(), "Finished for this step.  Migrated " + migrated + "entities total. ");
    }


    protected void executeBatch( final MutationBatch batch, final ProgressObserver po, final AtomicLong count ) {
        try {
            batch.execute();

            po.update( getVersion(), "Finished copying " + count + " entities to the new format" );
        }
        catch ( ConnectionException e ) {
            po.failed( getVersion(), "Failed to execute mutation in cassandra" );
            throw new DataMigrationException( "Unable to migrate batches ", e );
        }
    }
}
