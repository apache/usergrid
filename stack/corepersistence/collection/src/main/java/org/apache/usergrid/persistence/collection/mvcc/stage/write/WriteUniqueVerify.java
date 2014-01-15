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

import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.collection.mvcc.entity.ValidationUtils;
import org.apache.usergrid.persistence.collection.mvcc.stage.CollectionIoEvent;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.apache.usergrid.persistence.collection.exception.CollectionRuntimeException;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.field.Field;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.util.functions.Func1;

/**
 * This phase execute all unique value verification on the MvccEntity.
 */
@Singleton
public class WriteUniqueVerify
        implements Func1<CollectionIoEvent<MvccEntity>, CollectionIoEvent<MvccEntity>> {

    private static final Logger LOG = LoggerFactory.getLogger( WriteUniqueVerify.class );

    private final WriteFig writeFig;

    private final UniqueValueSerializationStrategy uniqueValueStrat;

    private final ExecutorService threadPool;

    @Inject
    public WriteUniqueVerify( WriteFig writeFig, 
            UniqueValueSerializationStrategy uniqueValueSerializiationStrategy ) {
        this.writeFig = writeFig;
        this.uniqueValueStrat = uniqueValueSerializiationStrategy;
        this.threadPool = Executors.newFixedThreadPool( writeFig.getMaxThreadCount() );
    }

    @Override
    public CollectionIoEvent<MvccEntity> call( final CollectionIoEvent<MvccEntity> ioevent ) {

        final Entity entity = ioevent.getEvent().getEntity().get();

        ValidationUtils.verifyMvccEntityWithEntity( ioevent.getEvent() );

        // use simple thread pool to verify fields in parallel
        final List<Future<FieldUniquenessResult>> results = 
                new ArrayList<Future<FieldUniquenessResult>>();

        for ( final Field field : entity.getFields() ) {

            if ( field.isUnique() ) {

                results.add( threadPool.submit( new Callable<FieldUniquenessResult>() {

                    public FieldUniquenessResult call() throws Exception {

                        // use write-first then read strategy 
                        UniqueValue written = new UniqueValueImpl( ioevent.getEntityCollection(), 
                                field, entity.getId(), entity.getVersion() );

                        // use TTL in case something goes wrong before entity is finally committed
                        MutationBatch mb = uniqueValueStrat.write( 
                                written, writeFig.getUniqueValueTimeToLive() );

                        try {
                            mb.execute();
                        } catch ( ConnectionException ex ) {
                            throw new CollectionRuntimeException(
                                    "Error writing unique value " + field.toString(), ex );
                        }

                        // does the database value match what we wrote?
                        UniqueValue loaded;
                        try {
                            loaded = uniqueValueStrat.load(
                                    ioevent.getEntityCollection(), field );

                        } catch ( ConnectionException ex ) {
                            throw new CollectionRuntimeException( ex );
                        }

                        return new FieldUniquenessResult( field, loaded.equals( written ));
                    }

                } ) );

            }
        }

        for ( Future<FieldUniquenessResult> result : results ) {
            try {
                if ( !result.get().isUnique() ) {
                    Field field = result.get().getField();
                    throw new CollectionRuntimeException( "Duplicate field value " 
                            + field.getName() + " = " + field.getValue().toString());
                }
            } catch ( InterruptedException ex ) {
                LOG.error( "Error verifing uniqueness", ex );
            } catch ( ExecutionException ex ) {
                LOG.error( "Error verifing uniqueness", ex );
            }
        }

        return ioevent;
    }

    static class FieldUniquenessResult {
        private Field field;
        private Boolean unique;

        public FieldUniquenessResult( Field f, Boolean u ) {
            this.field = f;
            this.unique = u;
        }

        public Boolean isUnique() {
            return unique;
        }

        public void setUnique( Boolean isUnique ) {
            this.unique = isUnique;
        }

        public Field getField() {
            return field;
        }

        public void setField( Field field ) {
            this.field = field;
        }

    }
}
