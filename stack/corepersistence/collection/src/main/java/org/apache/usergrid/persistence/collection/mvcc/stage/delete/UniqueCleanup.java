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

package org.apache.usergrid.persistence.collection.mvcc.stage.delete;


import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.collection.MvccEntity;
import org.apache.usergrid.persistence.collection.mvcc.stage.CollectionIoEvent;
import org.apache.usergrid.persistence.collection.serialization.SerializationFig;
import org.apache.usergrid.persistence.collection.serialization.UniqueValue;
import org.apache.usergrid.persistence.collection.serialization.UniqueValueSerializationStrategy;
import org.apache.usergrid.persistence.core.metrics.MetricsFactory;
import org.apache.usergrid.persistence.core.metrics.ObservableTimer;
import org.apache.usergrid.persistence.core.rx.ObservableIterator;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.model.entity.Id;

import com.codahale.metrics.Timer;
import com.fasterxml.uuid.UUIDComparator;
import com.google.inject.Inject;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import rx.Observable;


/**
 * Runs on an entity that as just be mark committed, and removes all unique values <= this entity
 */
public class UniqueCleanup
    implements Observable.Transformer<CollectionIoEvent<MvccEntity>, CollectionIoEvent<MvccEntity>> {


    private static final Logger logger = LoggerFactory.getLogger( UniqueCleanup.class );
    private final Timer uniqueCleanupTimer;


    private final UniqueValueSerializationStrategy uniqueValueSerializationStrategy;
    private final Keyspace keyspace;

    private final SerializationFig serializationFig;


    @Inject
    public UniqueCleanup( final SerializationFig serializationFig,
                          final UniqueValueSerializationStrategy uniqueValueSerializationStrategy,
                          final Keyspace keyspace, final MetricsFactory metricsFactory ) {

        this.serializationFig = serializationFig;
        this.uniqueValueSerializationStrategy = uniqueValueSerializationStrategy;
        this.keyspace = keyspace;
        this.uniqueCleanupTimer = metricsFactory.getTimer( UniqueCleanup.class, "uniquecleanup.base" );
    }


    @Override
    public Observable<CollectionIoEvent<MvccEntity>> call(
        final Observable<CollectionIoEvent<MvccEntity>> collectionIoEventObservable ) {

        final Observable<CollectionIoEvent<MvccEntity>> outputObservable =
            collectionIoEventObservable.flatMap( mvccEntityCollectionIoEvent -> {

                final Id entityId = mvccEntityCollectionIoEvent.getEvent().getId();
                final ApplicationScope applicationScope = mvccEntityCollectionIoEvent.getEntityCollection();
                final UUID entityVersion = mvccEntityCollectionIoEvent.getEvent().getVersion();


                //TODO Refactor this logic into a a class that can be invoked from anywhere
                //iterate all unique values
                final Observable<CollectionIoEvent<MvccEntity>> uniqueValueCleanup =
                    Observable.create( new ObservableIterator<UniqueValue>( "Unique value load" ) {
                        @Override
                        protected Iterator<UniqueValue> getIterator() {
                            return uniqueValueSerializationStrategy.getAllUniqueFields( applicationScope, entityId );
                        }
                    } )

                        //skip  versions > the specified version
                        //TODO: does this emit for every version before the staticComparator?
                        .skipWhile( uniqueValue -> {

                            logger.debug( "Cleaning up version:{} in UniqueCleanup", entityVersion );
                            final UUID uniqueValueVersion = uniqueValue.getEntityVersion();
                            //TODO: should this be equals? That way we clean up the one marked as well
                            return UUIDComparator.staticCompare( uniqueValueVersion, entityVersion ) > 0;
                        } )

                            //buffer our buffer size, then roll them all up in a single batch mutation
                        .buffer( serializationFig.getBufferSize() )

                            //roll them up

                        .doOnNext( uniqueValues -> {
                            final MutationBatch uniqueCleanupBatch = keyspace.prepareMutationBatch();


                            for ( UniqueValue value : uniqueValues ) {
                                logger
                                    .debug( "Deleting value:{} from application scope: {} ", value, applicationScope );
                                uniqueCleanupBatch
                                    .mergeShallow( uniqueValueSerializationStrategy.delete( applicationScope, value ) );
                            }

                            try {
                                uniqueCleanupBatch.execute();
                            }
                            catch ( ConnectionException e ) {
                                throw new RuntimeException( "Unable to execute batch mutation", e );
                            }
                        } ).lastOrDefault( Collections.emptyList() ).map( list -> mvccEntityCollectionIoEvent );

                return ObservableTimer.time( uniqueValueCleanup, uniqueCleanupTimer );
            } );



        return outputObservable;
    }
}
