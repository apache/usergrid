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


import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import java.util.List;
import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.exception.WriteOptimisticVerifyException;
import org.apache.usergrid.persistence.collection.mvcc.MvccLogEntrySerializationStrategy;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccLogEntry;
import org.apache.usergrid.persistence.collection.mvcc.entity.Stage;
import org.apache.usergrid.persistence.collection.mvcc.entity.ValidationUtils;
import org.apache.usergrid.persistence.collection.mvcc.entity.impl.MvccLogEntryImpl;
import org.apache.usergrid.persistence.collection.mvcc.stage.CollectionIoEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.util.functions.Func1;


/**
 * This phase should execute any optimistic verification on the MvccEntity
 */
@Singleton
public class WriteOptimisticVerify 
    implements Func1<CollectionIoEvent<MvccEntity>, CollectionIoEvent<MvccEntity>> {

    private static final Logger LOG = LoggerFactory.getLogger( WriteOptimisticVerify.class );

    private final MvccLogEntrySerializationStrategy logEntryStrat;

    @Inject
    public WriteOptimisticVerify( MvccLogEntrySerializationStrategy logEntryStrat ) {
        this.logEntryStrat = logEntryStrat;
    }


    @Override
    public CollectionIoEvent<MvccEntity> call( final CollectionIoEvent<MvccEntity> ioevent ) {
        ValidationUtils.verifyMvccEntityWithEntity( ioevent.getEvent() );

        // If the version was included on the entity write operation (delete or write) we need
        // to read back the entity log, and ensure that our "new" version is the only version
        // entry since the last commit.
        //
        // If not, fail fast, signal to the user their entity is "stale".

        MvccEntity entity = ioevent.getEvent();
        CollectionScope collectionScope = ioevent.getEntityCollection();

        try {
            List<MvccLogEntry> versions = logEntryStrat.load( 
                collectionScope, entity.getId(), entity.getVersion(), 2 );

            // Previous log entry must be committed, otherwise somebody is already writing
            if ( versions.size() > 1 
                    && versions.get(1).getStage().ordinal() < Stage.COMMITTED.ordinal() ) {
            
                // We're not the first writer, rollback and throw-up
                final MvccLogEntry rollbackEntry = 
                        new MvccLogEntryImpl( entity.getId(), entity.getVersion(), Stage.ROLLBACK);
                logEntryStrat.write( collectionScope, rollbackEntry );
                throw new WriteOptimisticVerifyException("Change conflict, not first writer");
            }


        } catch ( ConnectionException e ) {
            LOG.error( "Error reading entity log", e );
            throw new WriteOptimisticVerifyException( "Error reading entity log", e );
        }

        // No op, just emit the value
        return ioevent;
    }
}
