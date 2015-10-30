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


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.collection.MvccEntity;
import org.apache.usergrid.persistence.collection.MvccLogEntry;
import org.apache.usergrid.persistence.collection.serialization.MvccEntitySerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.MvccLogEntrySerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.UniqueValue;
import org.apache.usergrid.persistence.collection.serialization.UniqueValueSerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.impl.MvccEntitySerializationStrategyV3Impl;
import org.apache.usergrid.persistence.collection.serialization.impl.UniqueValueImpl;
import org.apache.usergrid.persistence.core.migration.data.DataMigration;
import org.apache.usergrid.persistence.core.migration.data.DataMigrationException;
import org.apache.usergrid.persistence.core.migration.data.MigrationDataProvider;
import org.apache.usergrid.persistence.core.migration.data.MigrationRelationship;
import org.apache.usergrid.persistence.core.migration.data.ProgressObserver;
import org.apache.usergrid.persistence.core.migration.data.VersionedMigrationSet;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.field.Field;
import org.apache.usergrid.persistence.model.util.EntityUtils;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import rx.Observable;
import rx.Subscriber;
import rx.schedulers.Schedulers;


/**
 * Data migration strategy for entities
 */
@Singleton
public class MvccEntityDataMigrationImpl implements DataMigration{


    private static final Logger LOGGER = LoggerFactory.getLogger( MvccEntityDataMigrationImpl.class );

    private final Keyspace keyspace;
    private final VersionedMigrationSet<MvccEntitySerializationStrategy> allVersions;
    private final MvccEntitySerializationStrategyV3Impl mvccEntitySerializationStrategyV3;
    private final UniqueValueSerializationStrategy uniqueValueSerializationStrategy;
    private final MvccLogEntrySerializationStrategy mvccLogEntrySerializationStrategy;
    private final MigrationDataProvider<EntityIdScope> migrationDataProvider;


    @Inject
    public MvccEntityDataMigrationImpl( final Keyspace keyspace,
                                        final VersionedMigrationSet<MvccEntitySerializationStrategy> allVersions,
                                        final MvccEntitySerializationStrategyV3Impl mvccEntitySerializationStrategyV3,
                                        final UniqueValueSerializationStrategy uniqueValueSerializationStrategy,
                                        final MvccLogEntrySerializationStrategy mvccLogEntrySerializationStrategy,
                                        final MigrationDataProvider<EntityIdScope> migrationDataProvider ) {
        this.keyspace = keyspace;
        this.allVersions = allVersions;
        this.mvccEntitySerializationStrategyV3 = mvccEntitySerializationStrategyV3;
        this.uniqueValueSerializationStrategy = uniqueValueSerializationStrategy;
        this.mvccLogEntrySerializationStrategy = mvccLogEntrySerializationStrategy;
        this.migrationDataProvider = migrationDataProvider;
    }


    @Override
    public boolean supports( final int currentVersion ) {
        //we can only migrate up to v3 with this implementation.  Beyond that, we should use a different migration
        return currentVersion < mvccEntitySerializationStrategyV3.getImplementationVersion();
    }


    @Override
    public int getMaxVersion() {
        return mvccEntitySerializationStrategyV3.getImplementationVersion();
    }


    @Override
    public int migrate( final int currentVersion,  final ProgressObserver observer ) {

        final AtomicLong atomicLong = new AtomicLong();

        //capture the time the test starts

        final UUID startTime = UUIDGenerator.newTimeUUID();

        final MigrationRelationship<MvccEntitySerializationStrategy> migration =
            allVersions.getMigrationRelationship( currentVersion );


        final Observable<List<EntityToSaveMessage>> migrated =
            migrationDataProvider.getData().subscribeOn( Schedulers.io() ).flatMap( entityToSaveList -> Observable.just( entityToSaveList ).flatMap( entityIdScope -> {

                //load the entity
                final ApplicationScope currentScope = entityIdScope.getApplicationScope();


                //for each element in our
                // history, we need to copy it
                // to v2.
                // Note that
                // this migration
                //won't support anything beyond V2

                final Iterator<MvccEntity> allVersions =
                    migration.from.loadAscendingHistory( currentScope, entityIdScope.getId(), startTime, 100 );

                //emit all the entity versions
                return Observable.create( new Observable.OnSubscribe<EntityToSaveMessage>() {
                    @Override
                    public void call( final Subscriber<? super
                        EntityToSaveMessage> subscriber ) {

                        while ( allVersions.hasNext() ) {
                            try {
                                final EntityToSaveMessage message =
                                    new EntityToSaveMessage(currentScope, allVersions.next());
                                subscriber.onNext(message);
                            }catch (Exception e){
                                LOGGER.error("Failed to load entity " +entityIdScope.getId(),e);
                            }
                        }

                        subscriber.onCompleted();
                    }
                } ).buffer( 100 ).doOnNext( entities -> {

                        final MutationBatch totalBatch = keyspace.prepareMutationBatch();

                        atomicLong.addAndGet( entities.size() );

                        final List<Id> toSaveIds = new ArrayList<>( entities.size() );


                        for ( EntityToSaveMessage message : entities ) {
                            try {
                                final MutationBatch entityRewrite = migration.to.write(message.scope, message.entity);

                                //add to
                                // the
                                // total
                                // batch
                                totalBatch.mergeShallow(entityRewrite);

                                //write
                                // the
                                // unique values

                                if (!message.entity.getEntity().isPresent()) {
                                    return;
                                }

                                final Entity entity = message.entity.getEntity().get();

                                final Id entityId = entity.getId();

                                final UUID version = message.entity.getVersion();


                                toSaveIds.add(entityId);

                                // re-write the unique
                                // values
                                // but this
                                // time with
                                // no TTL so that cleanup can clean up
                                // older values
                                for (final Field field : EntityUtils.getUniqueFields(message.entity.getEntity().get())) {

                                    final UniqueValue written = new UniqueValueImpl(field, entityId, version);

                                    final MutationBatch mb = uniqueValueSerializationStrategy.write(message.scope, written);


                                    // merge into our
                                    // existing mutation
                                    // batch
                                    totalBatch.mergeShallow(mb);
                                }


                                //add all our log entries
                                final List<MvccLogEntry> logEntries = mvccLogEntrySerializationStrategy.load(message.scope,
                                    message.entity.getId(), version, 1000);

                                /**
                                 * Migrate the log entry to the new format
                                 */
                                for (final MvccLogEntry entry : logEntries) {
                                    final MutationBatch mb = mvccLogEntrySerializationStrategy.write(message.scope, entry);

                                    totalBatch.mergeShallow(mb);
                                }
                            }catch (Exception e){
                                LOGGER.error("Failed to migrate entity "+ message.entity.getId().getUuid()+ " :: " + message.entity.getId().getType(),e);
                            }




                        }

                        executeBatch( migration.to.getImplementationVersion(), totalBatch, observer, atomicLong );

                        //now run our cleanup task

                        for ( Id updatedId : toSaveIds ) {




                        }
                    } ).subscribeOn(Schedulers.io());

            }, 10) );

        migrated.toBlocking().lastOrDefault(null);

        return migration.to.getImplementationVersion();
    }


    protected void executeBatch( final int targetVersion, final MutationBatch batch, final ProgressObserver po,
                                 final AtomicLong count ) {
        try {
            batch.execute();

            po.update( targetVersion, "Finished copying " + count + " entities to the new format" );
        }
        catch ( ConnectionException e ) {
            po.failed( targetVersion, "Failed to execute mutation in cassandra" );
            throw new DataMigrationException( "Unable to migrate batches ", e );
        }
    }


    private static final class EntityToSaveMessage {
        private final ApplicationScope scope;
        private final MvccEntity entity;


        private EntityToSaveMessage( final ApplicationScope scope, final MvccEntity entity ) {
            this.scope = scope;
            this.entity = entity;
        }
    }

}
