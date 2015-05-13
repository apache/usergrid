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


import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.collection.MvccEntity;
import org.apache.usergrid.persistence.collection.MvccLogEntry;
import org.apache.usergrid.persistence.collection.exception.WriteOptimisticVerifyException;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccValidationUtils;
import org.apache.usergrid.persistence.collection.mvcc.entity.Stage;
import org.apache.usergrid.persistence.collection.mvcc.stage.CollectionIoEvent;
import org.apache.usergrid.persistence.collection.serialization.MvccLogEntrySerializationStrategy;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.model.entity.Entity;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import rx.functions.Action1;


/**
 * This phase should execute any optimistic verification on the MvccEntity
 */
@Singleton
public class WriteOptimisticVerify implements Action1<CollectionIoEvent<MvccEntity>> {

    private static final Logger log = LoggerFactory.getLogger( WriteOptimisticVerify.class );

    private final MvccLogEntrySerializationStrategy logEntryStrat;


    @Inject
    public WriteOptimisticVerify( MvccLogEntrySerializationStrategy logEntryStrat ) {
        this.logEntryStrat = logEntryStrat;
    }


    @Override
    public void call( final CollectionIoEvent<MvccEntity> ioevent ) {
        MvccValidationUtils.verifyMvccEntityWithEntity( ioevent.getEvent() );

        // If the version was included on the entity write operation (delete or write) we need
        // to read back the entity log, and ensure that our "new" version is the only version
        // entry since the last commit.
        //
        // If not, fail fast, signal to the user their entity is "stale".

        MvccEntity mvccEntity = ioevent.getEvent();
        final Entity entity = mvccEntity.getEntity().get();

        ApplicationScope applicationScope = ioevent.getEntityCollection();

        if ( entity.getVersion() == null ) {
            return;
        }


        List<MvccLogEntry> versions = logEntryStrat.load( applicationScope, entity.getId(), entity.getVersion(), 2 );

        // Previous log entry must be committed, otherwise somebody is already writing
        if ( versions.size() > 1 && versions.get( 1 ).getStage().ordinal() < Stage.COMMITTED.ordinal() ) {

            log.debug( "Conflict writing entity id {} version {}", entity.getId().toString(),
                    entity.getVersion().toString() );

            throw new WriteOptimisticVerifyException( mvccEntity, applicationScope,
                    "Change conflict, not first writer" );
        }
    }
}
