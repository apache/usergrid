/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.usergrid.persistence.collection.mvcc.stage.delete;


import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.collection.MvccEntity;
import org.apache.usergrid.persistence.collection.MvccLogEntry;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccValidationUtils;
import org.apache.usergrid.persistence.collection.mvcc.entity.Stage;
import org.apache.usergrid.persistence.collection.mvcc.entity.impl.MvccLogEntryImpl;
import org.apache.usergrid.persistence.collection.mvcc.stage.CollectionIoEvent;
import org.apache.usergrid.persistence.collection.serialization.MvccEntitySerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.MvccLogEntrySerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.SerializationFig;
import org.apache.usergrid.persistence.collection.serialization.UniqueValueSerializationStrategy;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import rx.functions.Action1;


/**
 * This phase should invoke any finalization, and mark the entity as committed in the data store before returning
 */
@Singleton
public class MarkCommit implements Action1<CollectionIoEvent<MvccEntity>> {

    private static final Logger LOG = LoggerFactory.getLogger( MarkCommit.class );

    private final MvccLogEntrySerializationStrategy logStrat;
    private final MvccEntitySerializationStrategy entityStrat;
    private final SerializationFig serializationFig;
    private final UniqueValueSerializationStrategy uniqueValueStrat;
    private final Keyspace keyspace;


    @Inject
    public MarkCommit( final MvccLogEntrySerializationStrategy logStrat,
                       final MvccEntitySerializationStrategy entityStrat,
                       final UniqueValueSerializationStrategy uniqueValueStrat, final SerializationFig serializationFig,
                       final Keyspace keyspace ) {


        Preconditions.checkNotNull( logStrat, "logEntrySerializationStrategy is required" );
        Preconditions.checkNotNull( entityStrat, "entitySerializationStrategy is required" );

        this.logStrat = logStrat;
        this.entityStrat = entityStrat;
        this.serializationFig = serializationFig;
        this.uniqueValueStrat = uniqueValueStrat;
        this.keyspace = keyspace;
    }


    @Override
    public void call( final CollectionIoEvent<MvccEntity> idIoEvent ) {

        final MvccEntity entity = idIoEvent.getEvent();

        MvccValidationUtils.verifyMvccEntityOptionalEntity( entity );

        final Id entityId = entity.getId();
        final UUID version = entity.getVersion();


        final ApplicationScope applicationScope = idIoEvent.getEntityCollection();


        LOG.debug("Inserting tombstone for entity {} at version {}", entityId, version );

        final MvccLogEntry startEntry =
                new MvccLogEntryImpl( entityId, version, Stage.COMMITTED, MvccLogEntry.State.DELETED );

        final MutationBatch entityStateBatch = logStrat.write( applicationScope, startEntry );

        //insert a "cleared" value into the versions.  Post processing should actually delete

        try {
            final MutationBatch entityBatch = entityStrat.mark( applicationScope, entityId, version );
            entityStateBatch.mergeShallow( entityBatch );
            entityStateBatch.execute();
        }
        catch ( ConnectionException e ) {
            throw new RuntimeException( "Unable to mark entry as deleted" );
        }
    }
}

//
//
//        //TODO Refactor this logic into a a class that can be invoked from anywhere
//        //load every entity we have history of
//        Observable<List<MvccEntity>> deleteFieldsObservable =
//                Observable.create( new ObservableIterator<MvccEntity>( "deleteColumns" ) {
//                    @Override
//                    protected Iterator<MvccEntity> getIterator() {
//                        Iterator<MvccEntity> entities =
//                                entityStrat.load( collectionScope, entityId, entity.getVersion(), 100 );
//
//                        return entities;
//                    }
//                } )       //buffer them for efficiency
//                          .buffer( serializationFig.getBufferSize() ).doOnNext(
//
//                        new Action1<List<MvccEntity>>() {
//                            @Override
//                            public void call( final List<MvccEntity> mvccEntities ) {
//
//
//                                final MutationBatch batch = keyspace.prepareMutationBatch();
//
//                                for ( MvccEntity mvccEntity : mvccEntities ) {
//                                    if ( !mvccEntity.getEntity().isPresent() ) {
//                                        continue;
//                                    }
//
//                                    final UUID entityVersion = mvccEntity.getVersion();
//
//                                    final Entity entity = mvccEntity.getEntity().get();
//
//                                    //remove all unique fields from the index
//                                    for ( final Field field : entity.getFields() ) {
//
//                                        if(!field.isUnique()){
//                                            continue;
//                                        }
//
//                                        final UniqueValue unique = new UniqueValueImpl( field, entityId, entityVersion );
//
//                                        final MutationBatch deleteMutation = uniqueValueStrat.delete(collectionScope,  unique );
//
//                                        batch.mergeShallow( deleteMutation );
//                                    }
//                                }
//
//                                try {
//                                    batch.execute();
//                                }
//                                catch ( ConnectionException e1 ) {
//                                    throw new RuntimeException( "Unable to execute " +
//                                            "unique value " +
//                                            "delete", e1 );
//                                }
//                            }
//                        }
//
//
//                                                                       );
//
//        final int removedCount = deleteFieldsObservable.count().toBlocking().last();
//
//        LOG.debug("Removed unique values for {} entities of entity {}", removedCount, entityId );
