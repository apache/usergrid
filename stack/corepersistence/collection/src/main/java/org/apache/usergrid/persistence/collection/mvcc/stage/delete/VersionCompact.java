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


import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.collection.MvccLogEntry;
import org.apache.usergrid.persistence.collection.mvcc.stage.CollectionIoEvent;
import org.apache.usergrid.persistence.collection.serialization.MvccEntitySerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.MvccLogEntrySerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.SerializationFig;
import org.apache.usergrid.persistence.core.metrics.MetricsFactory;
import org.apache.usergrid.persistence.core.metrics.ObservableTimer;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.model.entity.Id;

import com.codahale.metrics.Timer;
import com.google.inject.Inject;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import rx.Observable;


/**
 * Compact all versions on the input observable by removing them from the log, and from the
 * versions
 */
public class VersionCompact
    implements Observable.Transformer<CollectionIoEvent<MvccLogEntry>, CollectionIoEvent<MvccLogEntry>> {

    private static final Logger logger = LoggerFactory.getLogger( VersionCompact.class );

    private final MvccLogEntrySerializationStrategy logEntrySerializationStrategy;
    private final MvccEntitySerializationStrategy mvccEntitySerializationStrategy;
    private final SerializationFig serializationFig;
    private final Keyspace keyspace;
    private final Timer compactTimer;


    @Inject
    public VersionCompact( final MvccLogEntrySerializationStrategy logEntrySerializationStrategy,
                           final SerializationFig serializationFig, final Keyspace keyspace,
                           final MetricsFactory metricsFactory,
                           final MvccEntitySerializationStrategy mvccEntitySerializationStrategy ) {
        this.logEntrySerializationStrategy = logEntrySerializationStrategy;
        this.serializationFig = serializationFig;
        this.keyspace = keyspace;
        this.mvccEntitySerializationStrategy = mvccEntitySerializationStrategy;
        this.compactTimer = metricsFactory.getTimer( VersionCompact.class, "version.compact" );
    }


    @Override
    public Observable<CollectionIoEvent<MvccLogEntry>> call(
        final Observable<CollectionIoEvent<MvccLogEntry>> collectionIoEventObservable ) {


        final Observable<CollectionIoEvent<MvccLogEntry>> entryBuffer =
            collectionIoEventObservable.buffer( serializationFig.getBufferSize() ).flatMap(
                buffer -> Observable.from( buffer ).collect( () -> keyspace.prepareMutationBatch(),
                    ( ( mutationBatch, mvccLogEntryCollectionIoEvent ) -> {

                        final ApplicationScope scope = mvccLogEntryCollectionIoEvent.getEntityCollection();
                        final MvccLogEntry mvccLogEntry = mvccLogEntryCollectionIoEvent.getEvent();
                        final Id entityId = mvccLogEntry.getEntityId();
                        final UUID version = mvccLogEntry.getVersion();

                        if ( logger.isDebugEnabled() ) {
                            logger.debug(
                                "Deleting log entry and version data for entity id {} and version {} in app scope {}",
                                new Object[] { entityId, version, scope } );
                        }


                        //delete from our log
                        final MutationBatch logDelete = logEntrySerializationStrategy.delete( scope, entityId, version );

                        mutationBatch.mergeShallow( logDelete );

                        //merge our entity delete in

                        final MutationBatch entityDelete =  mvccEntitySerializationStrategy.delete( scope, entityId, version );

                        mutationBatch.mergeShallow( entityDelete );



                    } ) )
                    //delete from the entities
                    .doOnNext( mutationBatch -> {
                        try {
                            mutationBatch.execute();
                        }
                        catch ( ConnectionException e ) {
                            throw new RuntimeException( "Unable to perform batch mutation" );
                        }
                } ).flatMap( batches -> Observable.from( buffer ) ) );


        return ObservableTimer.time( entryBuffer, compactTimer );
    }
}
