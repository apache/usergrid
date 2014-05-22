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

import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.exception.CollectionRuntimeException;
import org.apache.usergrid.persistence.collection.mvcc.MvccEntitySerializationStrategy;
import org.apache.usergrid.persistence.collection.mvcc.MvccLogEntrySerializationStrategy;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccLogEntry;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccValidationUtils;
import org.apache.usergrid.persistence.collection.mvcc.entity.impl.MvccEntityDeleteEvent;
import org.apache.usergrid.persistence.collection.mvcc.entity.impl.MvccLogEntryImpl;
import org.apache.usergrid.persistence.collection.mvcc.stage.CollectionIoEvent;
import org.apache.usergrid.persistence.core.consistency.AsyncProcessor;
import org.apache.usergrid.persistence.core.consistency.AsyncProcessorFactory;
import org.apache.usergrid.persistence.core.consistency.AsynchronousMessage;
import org.apache.usergrid.persistence.core.consistency.ConsistencyFig;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import rx.functions.Func1;


/**
 * This phase should invoke any finalization, and mark the entity 
 * as committed in the data store before returning
 */
@Singleton
public class MarkCommit implements Func1<CollectionIoEvent<MvccEntity>, Void> {

    private static final Logger LOG = LoggerFactory.getLogger( MarkCommit.class );

    private final MvccLogEntrySerializationStrategy logStrat;
    private final MvccEntitySerializationStrategy entityStrat;
    private final AsyncProcessor<MvccEntityDeleteEvent> entityEventAsyncProcessor;
    private final ConsistencyFig consistencyFig;

    @Inject
    public MarkCommit( final MvccLogEntrySerializationStrategy logStrat,
                       final MvccEntitySerializationStrategy entityStrat,
                       final AsyncProcessorFactory asyncProcessorFactory,
                       final ConsistencyFig consistencyFig ) {

        Preconditions.checkNotNull( 
                logStrat, "logEntrySerializationStrategy is required" );
        Preconditions.checkNotNull( 
                entityStrat, "entitySerializationStrategy is required" );

        this.logStrat = logStrat;
        this.entityStrat = entityStrat;
        this.entityEventAsyncProcessor = asyncProcessorFactory.getProcessor( MvccEntityDeleteEvent.class );
        this.consistencyFig = consistencyFig;
    }

    private long getTimeout() {
        return consistencyFig.getRepairTimeout() * 2;
    }

    @Override
    public Void call( final CollectionIoEvent<MvccEntity> idIoEvent ) {

        final MvccEntity entity = idIoEvent.getEvent();

        MvccValidationUtils.verifyMvccEntityOptionalEntity( entity );

        final Id entityId = entity.getId();
        final UUID version = entity.getVersion();


        final CollectionScope collectionScope = idIoEvent.getEntityCollection();


        final MvccLogEntry startEntry = new MvccLogEntryImpl( entityId, version,
                org.apache.usergrid.persistence.collection.mvcc.entity.Stage.COMMITTED );

        MutationBatch logMutation = logStrat.write( collectionScope, startEntry );

        //insert a "cleared" value into the versions.  Post processing should actually delete
        MutationBatch entityMutation = entityStrat.mark( collectionScope, entityId, version );

        //merge the 2 into 1 mutation
        logMutation.mergeShallow( entityMutation );

        //set up the post processing queue
        final AsynchronousMessage<MvccEntityDeleteEvent> event = entityEventAsyncProcessor.setVerification(
                new MvccEntityDeleteEvent( collectionScope, version, entity ), getTimeout() );

        try {
            logMutation.execute();
        }
        catch ( ConnectionException e ) {
            LOG.error( "Failed to execute write asynchronously ", e );
            throw new CollectionRuntimeException( entity.getEntity().get(), collectionScope, 
                    "Failed to execute write asynchronously ", e );
        }

        //fork post processing
        entityEventAsyncProcessor.start( event );

        /**
         * We're done executing.
         */

        return null;
    }
}
