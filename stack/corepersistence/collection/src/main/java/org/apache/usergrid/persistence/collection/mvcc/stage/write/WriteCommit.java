/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 */
package org.apache.usergrid.persistence.collection.mvcc.stage.write;


import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.collection.MvccEntity;
import org.apache.usergrid.persistence.collection.MvccLogEntry;
import org.apache.usergrid.persistence.collection.exception.WriteCommitException;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccValidationUtils;
import org.apache.usergrid.persistence.collection.mvcc.entity.Stage;
import org.apache.usergrid.persistence.collection.mvcc.entity.impl.MvccLogEntryImpl;
import org.apache.usergrid.persistence.collection.mvcc.stage.CollectionIoEvent;
import org.apache.usergrid.persistence.collection.serialization.MvccEntitySerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.MvccLogEntrySerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.UniqueValue;
import org.apache.usergrid.persistence.collection.serialization.UniqueValueSerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.impl.UniqueValueImpl;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.util.ValidationUtils;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.field.Field;
import org.apache.usergrid.persistence.model.util.EntityUtils;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import rx.functions.Func1;


/**
 * This phase should invoke any finalization, and mark the entity as committed in the
 * data store before returning
 */
@Singleton
public class WriteCommit implements Func1<CollectionIoEvent<MvccEntity>, Entity> {

    private static final Logger LOG = LoggerFactory.getLogger( WriteCommit.class );

    @Inject
    private UniqueValueSerializationStrategy uniqueValueStrat;

    private final MvccLogEntrySerializationStrategy logEntryStrat;

    private final MvccEntitySerializationStrategy entityStrat;


    @Inject
    public WriteCommit( final MvccLogEntrySerializationStrategy logStrat,
                        final MvccEntitySerializationStrategy entryStrat,
                        final UniqueValueSerializationStrategy uniqueValueStrat) {

        Preconditions.checkNotNull( logStrat, "MvccLogEntrySerializationStrategy is required" );
        Preconditions.checkNotNull( entryStrat, "MvccEntitySerializationStrategy is required" );
        Preconditions.checkNotNull( uniqueValueStrat, "UniqueValueSerializationStrategy is required");

        this.logEntryStrat = logStrat;
        this.entityStrat = entryStrat;
        this.uniqueValueStrat = uniqueValueStrat;
    }


    @Override
    public Entity call( final CollectionIoEvent<MvccEntity> ioEvent ) {

        final MvccEntity mvccEntity = ioEvent.getEvent();
        MvccValidationUtils.verifyMvccEntityWithEntity( mvccEntity );

        final Id entityId = mvccEntity.getId();
        final UUID version = mvccEntity.getVersion();
        final ApplicationScope applicationScope = ioEvent.getEntityCollection();

        //set the version into the entity
        EntityUtils.setVersion( mvccEntity.getEntity().get(), version );

        MvccValidationUtils.verifyMvccEntityWithEntity( ioEvent.getEvent() );
        ValidationUtils.verifyTimeUuid( version ,"version" );

        final MvccLogEntry startEntry = new MvccLogEntryImpl( entityId, version, Stage.COMMITTED, MvccLogEntry.State.COMPLETE );

        MutationBatch logMutation = logEntryStrat.write( applicationScope, startEntry );

        // now get our actual insert into the entity data
        MutationBatch entityMutation = entityStrat.write( applicationScope, mvccEntity );

        // merge the 2 into 1 mutation
        logMutation.mergeShallow( entityMutation );

        // re-write the unique values but this time with no TTL
        for ( Field field : EntityUtils.getUniqueFields(mvccEntity.getEntity().get()) ) {

                UniqueValue written  = new UniqueValueImpl( field,
                    entityId,version);

                MutationBatch mb = uniqueValueStrat.write(applicationScope,  written );

                LOG.debug("Finalizing {} unqiue value {}", field.getName(), field.getValue().toString());

                // merge into our existing mutation batch
                logMutation.mergeShallow( mb );
        }

        try {
            // TODO: Async execution
            logMutation.execute();
        }
        catch ( ConnectionException e ) {
            LOG.error( "Failed to execute write asynchronously ", e );
            throw new WriteCommitException( mvccEntity, applicationScope,
                "Failed to execute write asynchronously ", e );
        }


        return mvccEntity.getEntity().get();
    }
}
