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


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.collection.MvccEntity;
import org.apache.usergrid.persistence.collection.exception.CollectionRuntimeException;
import org.apache.usergrid.persistence.collection.serialization.MvccLogEntrySerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.UniqueValue;
import org.apache.usergrid.persistence.collection.serialization.UniqueValueSerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.impl.UniqueValueImpl;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.field.Field;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import rx.Scheduler;
import rx.functions.Action1;
import rx.schedulers.Schedulers;


/**
 * Reverses any changes made on behalf of the specified entity. When an exception is thrown during a write operation
 * this action is called to rollback any changes made.
 */
public class RollbackAction implements Action1<Throwable> {

    private static final Logger log = LoggerFactory.getLogger( RollbackAction.class );

    private final Scheduler scheduler;
    private final UniqueValueSerializationStrategy uniqueValueStrat;
    private final MvccLogEntrySerializationStrategy logEntryStrat;


    @Inject
    public RollbackAction(MvccLogEntrySerializationStrategy logEntryStrat,
                           UniqueValueSerializationStrategy uniqueValueStrat ) {

        scheduler = Schedulers.io();
        this.uniqueValueStrat = uniqueValueStrat;
        this.logEntryStrat = logEntryStrat;
    }


    public void call( final Throwable t ) {

        if ( t instanceof CollectionRuntimeException ) {

            CollectionRuntimeException cre = ( CollectionRuntimeException ) t;
            final MvccEntity mvccEntity = cre.getEntity();
            final ApplicationScope scope = cre.getApplicationScope();

            // one batch to handle rollback
            MutationBatch rollbackMb = null;
            final Optional<Entity> entity = mvccEntity.getEntity();

            if ( entity.isPresent() ) {
                for ( final Field field : entity.get().getFields() ) {

                    // if it's unique, add its deletion to the rollback batch
                    if ( field.isUnique() ) {

                        UniqueValue toDelete =
                                new UniqueValueImpl( field, entity.get().getId(), mvccEntity.getVersion() );

                        MutationBatch deleteMb = uniqueValueStrat.delete(scope,  toDelete );

                        if ( rollbackMb == null ) {
                            rollbackMb = deleteMb;
                        }
                        else {
                            rollbackMb.mergeShallow( deleteMb );
                        }
                    }
                }


                if ( rollbackMb != null ) {
                    try {
                        rollbackMb.execute();
                    }
                    catch ( ConnectionException ex ) {
                        throw new RuntimeException( "Error rolling back changes", ex );
                    }
                }

                logEntryStrat.delete( scope, entity.get().getId(), mvccEntity.getVersion() );
            }
        }
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
