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


import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.MvccEntity;
import org.apache.usergrid.persistence.collection.serialization.MvccEntitySerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.UniqueValue;
import org.apache.usergrid.persistence.collection.serialization.UniqueValueSerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.impl.UniqueValueImpl;
import org.apache.usergrid.persistence.collection.util.EntityUtils;
import org.apache.usergrid.persistence.core.migration.data.DataMigrationException;
import org.apache.usergrid.persistence.core.migration.data.MigrationInfoSerialization;
import org.apache.usergrid.persistence.core.migration.data.newimpls.DataMigration2;
import org.apache.usergrid.persistence.core.migration.data.newimpls.MigrationDataProvider;
import org.apache.usergrid.persistence.core.migration.data.newimpls.MigrationRelationship;
import org.apache.usergrid.persistence.core.migration.data.newimpls.ProgressObserver;
import org.apache.usergrid.persistence.core.migration.data.newimpls.VersionedMigrationSet;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.field.Field;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.google.inject.Inject;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;


/**
 * Data migration strategy for entities
 */
public class MvccEntityDataMigrationImpl implements DataMigration2<EntityIdScope> {

    private final Keyspace keyspace;
    private final VersionedMigrationSet<MvccEntitySerializationStrategy> allVersions;
    private final UniqueValueSerializationStrategy uniqueValueSerializationStrategy;
    private final MigrationInfoSerialization migrationInfoSerialization;


    @Inject
    public MvccEntityDataMigrationImpl( final Keyspace keyspace,
                                        final VersionedMigrationSet<MvccEntitySerializationStrategy> allVersions,
                                        final UniqueValueSerializationStrategy uniqueValueSerializationStrategy,
                                        final MigrationInfoSerialization migrationInfoSerialization ) {

        this.keyspace = keyspace;
        this.allVersions = allVersions;
        this.uniqueValueSerializationStrategy = uniqueValueSerializationStrategy;
        this.migrationInfoSerialization = migrationInfoSerialization;
    }


    @Override
    public int getVersion() {
        //get the max implementation version, since that's what we're going to
        return allVersions.getMaxVersion( migrationInfoSerialization.getVersion( CollectionMigrationPlugin.PLUGIN_NAME ) );
    }


    @Override
    public void migrate( final MigrationDataProvider<EntityIdScope> migrationDataProvider,   final ProgressObserver observer ) {

        final AtomicLong atomicLong = new AtomicLong();

        //capture the time the test starts

        final UUID startTime = UUIDGenerator.newTimeUUID();

        final MigrationRelationship<MvccEntitySerializationStrategy>
                migration = allVersions.getMigrationRelationship( getCurrentSystemVersion() );

        final long migrated = migrationDataProvider.getData().subscribeOn( Schedulers.io() )
                   .parallel( new Func1<Observable<EntityIdScope>, Observable<Long>>() {


                       //process the ids in parallel
                       @Override
                       public Observable<Long> call( final Observable<EntityIdScope> entityIdScopeObservable ) {


                           return entityIdScopeObservable
                                   .flatMap( new Func1<EntityIdScope, Observable<EntityToSaveMessage>>() {


                                       @Override
                                       public Observable<EntityToSaveMessage> call(
                                               final EntityIdScope entityIdScope ) {

                                           //load the entity
                                           final CollectionScope currentScope = entityIdScope.getCollectionScope();



                                           //for each element in our history, we need to copy it to v2.  Note that this migration
                                           //won't support anything beyond V2

                                           final Iterator<MvccEntity> allVersions = migration.from
                                                   .loadAscendingHistory( currentScope, entityIdScope.getId(),
                                                           startTime, 100 );

                                           //emit all the entities
                                           return Observable.create( new Observable.OnSubscribe<EntityToSaveMessage>() {
                                               @Override
                                               public void call(
                                                       final Subscriber<? super EntityToSaveMessage> subscriber ) {

                                                   while ( allVersions.hasNext() ) {
                                                       final EntityToSaveMessage message =
                                                               new EntityToSaveMessage( currentScope,
                                                                       allVersions.next() );
                                                       subscriber.onNext( message );
                                                   }

                                                   subscriber.onCompleted();
                                               }
                                           } );
                                       }
                                   } ).buffer( 100 ).doOnNext( new Action1<List<EntityToSaveMessage>>() {
                                       @Override
                                       public void call( final List<EntityToSaveMessage> messages ) {
                                           atomicLong.addAndGet( messages.size() );

                                           final MutationBatch totalBatch = keyspace.prepareMutationBatch();


                                           for ( EntityToSaveMessage message : messages ) {

                                               final MutationBatch entityRewrite =
                                                       migration.to.write( message.scope, message.entity );

                                               //add to the total batch
                                               totalBatch.mergeShallow( entityRewrite );

                                               //write the unique values

                                               if ( message.entity.getEntity().isPresent() ) {

                                                   final Entity entity = message.entity.getEntity().get();

                                                   final Id entityId = entity.getId();

                                                   final UUID version = message.entity.getVersion();

                                                   // re-write the unique values but this time with no TTL
                                                   for ( Field field : EntityUtils
                                                           .getUniqueFields( message.entity.getEntity().get() ) ) {

                                                       UniqueValue written =
                                                               new UniqueValueImpl( field, entityId, version );

                                                       MutationBatch mb =
                                                               uniqueValueSerializationStrategy.write( message.scope, written );


                                                       // merge into our existing mutation batch
                                                       totalBatch.mergeShallow( mb );
                                                   }


                                               }
                                           }


                                           executeBatch( totalBatch, observer, atomicLong );
                                       }
                                   } )
                                           //count the results
                                   .reduce( 0l, new Func2<Long, List<EntityToSaveMessage>, Long>() {
                                       @Override
                                       public Long call( final Long aLong, final List<EntityToSaveMessage> ids ) {
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

    private int getCurrentSystemVersion(){
       return migrationInfoSerialization.getVersion( CollectionMigrationPlugin.PLUGIN_NAME );
    }

    private static final class EntityToSaveMessage{
        private final CollectionScope scope;
        private final MvccEntity entity;


        private EntityToSaveMessage( final CollectionScope scope, final MvccEntity entity ) {
            this.scope = scope;
            this.entity = entity;
        }
    }
}
