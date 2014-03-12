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
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import java.util.ArrayList;
import java.util.List;
import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.exception.CollectionRuntimeException;
import org.apache.usergrid.persistence.collection.exception.WriteOptimisticVerifyException;
import org.apache.usergrid.persistence.collection.exception.WriteUniqueVerifyException;
import org.apache.usergrid.persistence.collection.mvcc.MvccLogEntrySerializationStrategy;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccLogEntry;
import org.apache.usergrid.persistence.collection.mvcc.entity.Stage;
import org.apache.usergrid.persistence.collection.mvcc.entity.ValidationUtils;
import org.apache.usergrid.persistence.collection.mvcc.entity.impl.MvccLogEntryImpl;
import org.apache.usergrid.persistence.collection.mvcc.stage.CollectionIoEvent;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.field.Field;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Scheduler;
import rx.util.functions.Func1;
import rx.util.functions.FuncN;


/**
 * This phase should execute any optimistic verification on the MvccEntity
 */
@Singleton
public class WriteOptimisticVerify 
    implements Func1<CollectionIoEvent<MvccEntity>, CollectionIoEvent<MvccEntity>> {

    private static final Logger log = LoggerFactory.getLogger( WriteOptimisticVerify.class );

    private final MvccLogEntrySerializationStrategy logEntryStrat;

    private final UniqueValueSerializationStrategy uniqueValueStrat;

    private final Scheduler scheduler;

    @Inject
    public WriteOptimisticVerify( MvccLogEntrySerializationStrategy logEntryStrat, 
            final UniqueValueSerializationStrategy uniqueValueStrat, final Scheduler scheduler ) {

        this.logEntryStrat = logEntryStrat;
        this.uniqueValueStrat = uniqueValueStrat; 
        this.scheduler = scheduler;
    }


    @Override
    public CollectionIoEvent<MvccEntity> call( final CollectionIoEvent<MvccEntity> ioevent ) {
        ValidationUtils.verifyMvccEntityWithEntity( ioevent.getEvent() );

        // If the version was included on the entity write operation (delete or write) we need
        // to read back the entity log, and ensure that our "new" version is the only version
        // entry since the last commit.
        //
        // If not, fail fast, signal to the user their entity is "stale".

        MvccEntity mvccEntity = ioevent.getEvent();
        final Entity entity = mvccEntity.getEntity().get();

        CollectionScope collectionScope = ioevent.getEntityCollection();

        try {
            List<MvccLogEntry> versions = logEntryStrat.load( 
                collectionScope, entity.getId(), entity.getVersion(), 2 );

            // Previous log entry must be committed, otherwise somebody is already writing
            if ( versions.size() > 1 
                    && versions.get(1).getStage().ordinal() < Stage.COMMITTED.ordinal() ) {

                log.debug("Conflict writing entity id {} version {}", 
                    entity.getId().toString(), entity.getVersion().toString());
            
                // We're not the first writer, set ROLLBACK, cleanup and throw exception

                final MvccLogEntry rollbackEntry = 
                    new MvccLogEntryImpl( entity.getId(), entity.getVersion(), Stage.ROLLBACK);
                logEntryStrat.write( collectionScope, rollbackEntry );

                // Delete all unique values of entity, and do it concurrently 

                List<Observable<FieldDeleteResult>> results = 
                    new ArrayList<Observable<FieldDeleteResult>>();

                int uniqueFieldCount = 0;
                for ( final Field field : entity.getFields() ) {

                    // if it's unique, create a function to delete it
                    if ( field.isUnique() ) {

                        uniqueFieldCount++;

                        Observable<FieldDeleteResult> result =  Observable.from( field )
                            .subscribeOn( scheduler ).map(new Func1<Field,  FieldDeleteResult>() {

                            @Override
                            public FieldDeleteResult call(Field field ) {

                                UniqueValue toDelete = new UniqueValueImpl( 
                                    ioevent.getEntityCollection(), field, 
                                    entity.getId(), entity.getVersion() );

                                MutationBatch mb = uniqueValueStrat.delete( toDelete );
                                try {
                                    mb.execute();
                                }
                                catch ( ConnectionException ex ) {
                                    throw new WriteUniqueVerifyException( 
                                        "Error deleting unique value " + field.toString(), ex );
                                }
                                return new FieldDeleteResult( field.getName() );
                            }
                        });

                        results.add( result ); 
                    }
                }

                if ( uniqueFieldCount > 0 ) {

                    final FuncN<Boolean> zipFunction = new FuncN<Boolean>() {

                        @Override
                        public Boolean call( final Object... args ) {
                            for ( Object resultObj : args ) {

                                FieldDeleteResult result = ( FieldDeleteResult ) resultObj;
                                log.debug("Rollback deleted field from entity: {} version: {} name: {}",
                                    new String[] { 
                                        entity.getId().toString(), 
                                        entity.getVersion().toString(),
                                        result.getName()
                                    });
                            }
                            return true;
                        }
                    };
   
                    // "zip" up the concurrent results
                    Observable.zip( results, zipFunction ).toBlockingObservable().last();
                }

                throw new WriteOptimisticVerifyException("Change conflict, not first writer");
            }


        } catch ( ConnectionException e ) {
            log.error( "Error reading entity log", e );
            throw new CollectionRuntimeException( "Error reading entity log", e );
        }

        // No op, just emit the value
        return ioevent;
    }


    class FieldDeleteResult {
        private final String name;

        public FieldDeleteResult( String name ) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }
    }
}
