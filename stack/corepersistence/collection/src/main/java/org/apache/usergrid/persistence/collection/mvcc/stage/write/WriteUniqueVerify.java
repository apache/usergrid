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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.exception.WriteUniqueVerifyException;
import org.apache.usergrid.persistence.collection.MvccEntity;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccValidationUtils;
import org.apache.usergrid.persistence.collection.mvcc.stage.CollectionIoEvent;
import org.apache.usergrid.persistence.collection.serialization.SerializationFig;
import org.apache.usergrid.persistence.collection.serialization.UniqueValue;
import org.apache.usergrid.persistence.collection.serialization.UniqueValueSerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.UniqueValueSet;
import org.apache.usergrid.persistence.collection.serialization.impl.UniqueValueImpl;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.field.Field;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import rx.functions.Action1;


/**
 * This phase execute all unique value verification on the MvccEntity.
 */
@Singleton
public class WriteUniqueVerify implements Action1<CollectionIoEvent<MvccEntity>> {

    private static final Logger LOG = LoggerFactory.getLogger( WriteUniqueVerify.class );

    private final UniqueValueSerializationStrategy uniqueValueStrat;

    protected final SerializationFig serializationFig;

    protected final Keyspace keyspace;


    @Inject
    public WriteUniqueVerify( final UniqueValueSerializationStrategy uniqueValueSerializiationStrategy,
                              final SerializationFig serializationFig, final Keyspace keyspace ) {
        this.keyspace = keyspace;

        Preconditions.checkNotNull( uniqueValueSerializiationStrategy, "uniqueValueSerializationStrategy is required" );
        Preconditions.checkNotNull( serializationFig, "serializationFig is required" );

        this.uniqueValueStrat = uniqueValueSerializiationStrategy;
        this.serializationFig = serializationFig;
    }


    @Override
    public void call( final CollectionIoEvent<MvccEntity> ioevent ) {

        MvccValidationUtils.verifyMvccEntityWithEntity( ioevent.getEvent() );

        final MvccEntity mvccEntity = ioevent.getEvent();

        final Id entityId = mvccEntity.getId();

        final UUID entityVersion = mvccEntity.getVersion();


        final Entity entity = mvccEntity.getEntity().get();


        final CollectionScope scope = ioevent.getEntityCollection();

        // use simple thread pool to verify fields in parallel

        final Collection<Field> entityFields = entity.getFields();

        //allocate our max size, worst case
        final List<Field> uniqueFields = new ArrayList<>( entityFields.size() );

        final MutationBatch batch = keyspace.prepareMutationBatch();
        //
        // Construct all the functions for verifying we're unique
        //
        for ( final Field field : entityFields ) {

            // if it's unique, create a function to validate it and add it to the list of 
            // concurrent validations
            if ( field.isUnique() ) {


                // use write-first then read strategy
                final UniqueValue written = new UniqueValueImpl( field, entityId, entityVersion );

                // use TTL in case something goes wrong before entity is finally committed
                final MutationBatch mb = uniqueValueStrat.write( scope, written, serializationFig.getTimeout() );

                batch.mergeShallow( mb );


                uniqueFields.add( field );
            }
        }

        //short circuit nothing to do
        if ( uniqueFields.size() == 0 ) {
            return;
        }


        //perform the write
        try {
            batch.execute();
        }
        catch ( ConnectionException ex ) {
            throw new RuntimeException( "Unable to write to cassandra", ex );
        }


        //now get the set of fields back
        final UniqueValueSet uniqueValues;

        try {
            uniqueValues = uniqueValueStrat.load( scope, uniqueFields );
        }
        catch ( ConnectionException e ) {
            throw new RuntimeException( "Unable to read from cassandra", e );
        }


        final Map<String, Field> uniquenessViolations = new HashMap<>( uniqueFields.size() );


        //loop through each field that was unique
        for ( final Field field : uniqueFields ) {

            final UniqueValue uniqueValue = uniqueValues.getValue( field.getName() );

            if ( uniqueValue == null ) {
                throw new RuntimeException(
                        String.format( "Could not retrieve unique value for field %s, unable to verify",
                                field.getName() ) );
            }


            final Id returnedEntityId = uniqueValue.getEntityId();


            if ( !entityId.equals( returnedEntityId ) ) {
                uniquenessViolations.put( field.getName(), field );
            }
        }


        //We have violations, throw an exception
        if ( !uniquenessViolations.isEmpty() ) {
            throw new WriteUniqueVerifyException( mvccEntity, ioevent.getEntityCollection(), uniquenessViolations );
        }
    }
}
