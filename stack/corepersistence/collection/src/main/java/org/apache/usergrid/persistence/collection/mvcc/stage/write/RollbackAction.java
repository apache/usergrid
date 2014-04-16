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

import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import java.util.ArrayList;
import java.util.List;
import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.exception.CollectionRuntimeException;
import org.apache.usergrid.persistence.collection.exception.WriteUniqueVerifyException;
import org.apache.usergrid.persistence.collection.mvcc.MvccLogEntrySerializationStrategy;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.field.Field;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Scheduler;
import rx.util.functions.Action1;
import rx.functions.Func1;
import rx.functions.FuncN;
import rx.schedulers.Schedulers;


/**
 * Reverses any changes made on behalf of the specified entity. When an exception is thrown
 * during a write operation this action is called to rollback any changes made. 
 */
public class RollbackAction implements Action1<Throwable> {

    private static final Logger log = LoggerFactory.getLogger(RollbackAction.class);

    private final Scheduler scheduler;
    private final UniqueValueSerializationStrategy uniqueValueStrat;
    private final MvccLogEntrySerializationStrategy logEntryStrat;


    public RollbackAction( 
            MvccLogEntrySerializationStrategy logEntryStrat, 
            UniqueValueSerializationStrategy uniqueValueStrat ) {

        scheduler = Schedulers.io(); //injector.getInstance( Scheduler.class );
        this.uniqueValueStrat = uniqueValueStrat;
        this.logEntryStrat = logEntryStrat;
    }


    public void call( final Throwable t ) {

        if ( t instanceof CollectionRuntimeException ) {

            CollectionRuntimeException cre = (CollectionRuntimeException)t;
            final Entity entity = cre.getEntity();
            final CollectionScope scope = cre.getCollectionScope();

            // Delete all unique values of entity, and do it concurrently 
            List<Observable<FieldDeleteResult>> results
                = new ArrayList<Observable<FieldDeleteResult>>();

            int uniqueFieldCount = 0;
            for (final Field field : entity.getFields()) {

                // if it's unique, create a function to delete it
                if (field.isUnique()) {

                    uniqueFieldCount++;

                    Observable<FieldDeleteResult> result = Observable.from(field)
                            .subscribeOn(scheduler).map(new Func1<Field, FieldDeleteResult>() {

                                @Override
                                public FieldDeleteResult call(Field field) {
                                    UniqueValue toDelete = new UniqueValueImpl(
                                        scope, field, entity.getId(), entity.getVersion());

                                    MutationBatch mb = uniqueValueStrat.delete(toDelete);
                                    try {
                                        mb.execute();
                                    } catch (ConnectionException ex) {
                                        throw new WriteUniqueVerifyException( entity, scope, 
                                            "Rollback error deleting unique value " 
                                                + field.toString(), ex);
                                    }
                                    return new FieldDeleteResult(field.getName());
                                }
                            });

                    results.add(result);
                }
            }

            if (uniqueFieldCount > 0) {

                final FuncN<Boolean> zipFunction = new FuncN<Boolean>() {

                    @Override
                    public Boolean call(final Object... args) {
                        for (Object resultObj : args) {

                            FieldDeleteResult result = (FieldDeleteResult) resultObj;
                            log.debug("Rollback deleted unique value from entity: "
                                    + "{} version: {} name: {}",
                                 new String[]{
                                     entity.getId().toString(),
                                     entity.getVersion().toString(),
                                     result.getName()
                                 });
                        }
                        return true;
                    }
                };

                // "zip" up the concurrent results
                Observable.zip(results, zipFunction).toBlockingObservable().last();
            }

            logEntryStrat.delete( scope, entity.getId(), entity.getVersion() );
        }
    }

    class FieldDeleteResult {

        private final String name;

        public FieldDeleteResult(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }
    }

}
