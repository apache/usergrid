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
import org.apache.usergrid.persistence.collection.exception.CollectionRuntimeException;
import org.apache.usergrid.persistence.collection.mvcc.entity.Stage;
import org.apache.usergrid.persistence.collection.mvcc.entity.impl.MvccEntityImpl;
import org.apache.usergrid.persistence.collection.mvcc.entity.impl.MvccLogEntryImpl;
import org.apache.usergrid.persistence.collection.mvcc.stage.CollectionIoEvent;
import org.apache.usergrid.persistence.collection.serialization.MvccLogEntrySerializationStrategy;
import org.apache.usergrid.persistence.collection.service.UUIDService;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.util.ValidationUtils;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import rx.functions.Func1;


/**
 * This is the first stage and should be invoked immediately when a write is started.
 * It should persist the start of a new write in the data store for
 * a checkpoint and recovery
 */
@Singleton
public class MarkStart implements Func1<CollectionIoEvent<Id>, CollectionIoEvent<MvccEntity>> {

    private static final Logger LOG = LoggerFactory.getLogger( MarkStart.class );


    private final MvccLogEntrySerializationStrategy logStrategy;
    private final UUIDService uuidService;


    /**
     * Create a new stage with the current context
     */
    @Inject
    public MarkStart(final MvccLogEntrySerializationStrategy logStrategy, final UUIDService uuidService ) {

        Preconditions.checkNotNull( logStrategy, "logStrategy is required" );
        Preconditions.checkNotNull( uuidService, "uuidService is required" );

        this.logStrategy = logStrategy;
        this.uuidService = uuidService;
    }


    @Override
    public CollectionIoEvent<MvccEntity> call( final CollectionIoEvent<Id> entityIoEvent ) {
        final Id entityId = entityIoEvent.getEvent();

        ValidationUtils.verifyIdentity( entityId );

        final UUID version = uuidService.newTimeUUID();


        final ApplicationScope applicationScope = entityIoEvent.getEntityCollection();


        final MvccLogEntry startEntry = new MvccLogEntryImpl( entityId, version, Stage.ACTIVE, MvccLogEntry.State.DELETED );

        MutationBatch write = logStrategy.write( applicationScope, startEntry );


        try {
            write.execute();
        }
        catch ( ConnectionException e ) {
            LOG.error( "Failed to execute write asynchronously ", e );
            throw new CollectionRuntimeException( null, applicationScope,
                    "Failed to execute write asynchronously ", e );
        }


        //create the mvcc entity for the next stage
        final MvccEntityImpl nextStage = new MvccEntityImpl(
            entityId, version, MvccEntity.Status.COMPLETE, Optional.<Entity>absent() );


        return new CollectionIoEvent<MvccEntity>( applicationScope, nextStage );
    }
}
