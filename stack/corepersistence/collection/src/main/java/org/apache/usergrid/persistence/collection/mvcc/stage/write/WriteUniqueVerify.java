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


import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.collection.mvcc.entity.ValidationUtils;
import org.apache.usergrid.persistence.collection.mvcc.stage.CollectionIoEvent;
import org.apache.usergrid.persistence.collection.serialization.SerializationFig;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.field.Field;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import java.util.HashMap;
import java.util.Map;
import org.apache.usergrid.persistence.collection.exception.WriteUniqueVerifyException;

import rx.Observable;
import rx.functions.Func1;
import rx.functions.FuncN;
import rx.schedulers.Schedulers;


/**
 * This phase execute all unique value verification on the MvccEntity.
 */
@Singleton
public class WriteUniqueVerify implements 
        Func1<CollectionIoEvent<MvccEntity>, Observable<? extends CollectionIoEvent<MvccEntity>>> {

    private static final Logger LOG = LoggerFactory.getLogger( WriteUniqueVerify.class );

    private final UniqueValueSerializationStrategy uniqueValueStrat;

    protected final SerializationFig serializationFig;


    @Inject
    public WriteUniqueVerify( final UniqueValueSerializationStrategy uniqueValueSerializiationStrategy, final SerializationFig serializationFig ) {

        Preconditions.checkNotNull( uniqueValueSerializiationStrategy, "uniqueValueSerializationStrategy is required" );
        Preconditions.checkNotNull( serializationFig, "serializationFig is required" );

        this.uniqueValueStrat = uniqueValueSerializiationStrategy;
        this.serializationFig = serializationFig;
    }


    @Override
    public Observable<? extends CollectionIoEvent<MvccEntity>> 
        call(final CollectionIoEvent<MvccEntity> ioevent ) {

        ValidationUtils.verifyMvccEntityWithEntity( ioevent.getEvent() );

        final Entity entity = ioevent.getEvent().getEntity().get();

        // use simple thread pool to verify fields in parallel

        // We want to use concurrent to fork all validations this way they're wrapped by timeouts 
        // and Hystrix thread pools for JMX operations.  See the WriteCommand in the 
        // EntityCollectionManagerImpl. 
        // TODO: still needs to be added to the Concurrent utility class?

        final List<Observable<FieldUniquenessResult>> fields =
                new ArrayList<Observable<FieldUniquenessResult>>();

        //
        // Construct all the functions for verifying we're unique
        //
        for ( final Field field : entity.getFields() ) {

            // if it's unique, create a function to validate it and add it to the list of 
            // concurrent validations
            if ( field.isUnique() ) {

                Observable<FieldUniquenessResult> result =  Observable.from( field ).subscribeOn( Schedulers.io() ).map(new Func1<Field,  FieldUniquenessResult>() {
                    @Override
                    public FieldUniquenessResult call(Field field ) {

                        // use write-first then read strategy
                        UniqueValue written = new UniqueValueImpl( 
                            ioevent.getEntityCollection(), field, entity.getId(), entity.getVersion() );

                        // use TTL in case something goes wrong before entity is finally committed
                        MutationBatch mb = uniqueValueStrat.write( written, serializationFig.getTimeout() );

                        try {
                            mb.execute();
                        }
                        catch ( ConnectionException ex ) {
                            throw new WriteUniqueVerifyException( 
                                entity, ioevent.getEntityCollection(), 
                                    "Error writing unique value " + field.toString(), ex );
                        }

                        // does the database value match what we wrote?
                        UniqueValue loaded;
                        try {
                            loaded = uniqueValueStrat.load( ioevent.getEntityCollection(), field );
                        }
                        catch ( ConnectionException ex ) {
                            throw new WriteUniqueVerifyException( entity, ioevent.getEntityCollection(), 
                                    "Error verifying write", ex );
                        }

                        return new FieldUniquenessResult( field, loaded.equals( written ) );
                    }
                } );

                fields.add(result);
            }
        }

        //short circuit.  If we zip up nothing, we block forever.
        if(fields.size() == 0){
            return Observable.from(ioevent ).subscribeOn( Schedulers.io() );
        }

        //
        // Zip the results up
        //
        final FuncN<CollectionIoEvent<MvccEntity>> zipFunction = 
                new FuncN<CollectionIoEvent<MvccEntity>>() {
            
            @Override
            public CollectionIoEvent<MvccEntity> call( final Object... args ) {

                Map<String, Field> uniquenessVioloations = new HashMap<String, Field>();

                for ( Object resultObj : args ) {
                    FieldUniquenessResult result = ( FieldUniquenessResult ) resultObj;
                    if ( !result.isUnique() ) {
                        Field field = result.getField();
                        uniquenessVioloations.put( field.getName(), field );
                    }
                }

                if ( !uniquenessVioloations.isEmpty() ) {
                    throw new WriteUniqueVerifyException( 
                        entity, ioevent.getEntityCollection(), uniquenessVioloations );
                }
                    
                //return the original event
                return ioevent;
            }
        };



        return Observable.zip( fields, zipFunction );

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
