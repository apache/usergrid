/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.usergrid.persistence.collection.serialization.impl;


import java.nio.ByteBuffer;
import java.util.UUID;

import org.apache.usergrid.persistence.collection.MvccEntity;
import org.apache.usergrid.persistence.collection.exception.DataCorruptionException;
import org.apache.usergrid.persistence.collection.exception.EntityTooLargeException;
import org.apache.usergrid.persistence.collection.serialization.SerializationFig;
import org.apache.usergrid.persistence.core.astyanax.CassandraFig;
import org.apache.usergrid.persistence.core.astyanax.FieldBuffer;
import org.apache.usergrid.persistence.core.astyanax.FieldBufferBuilder;
import org.apache.usergrid.persistence.core.astyanax.FieldBufferParser;
import org.apache.usergrid.persistence.core.astyanax.FieldBufferSerializer;
import org.apache.usergrid.persistence.core.astyanax.IdRowCompositeSerializer;
import org.apache.usergrid.persistence.core.astyanax.MultiTennantColumnFamily;
import org.apache.usergrid.persistence.core.astyanax.ScopedRowKey;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.serializers.AbstractSerializer;
import com.netflix.astyanax.serializers.UUIDSerializer;


/**
 * Version 1 implementation of entity serialization
 */
@Singleton
public class MvccEntitySerializationStrategyV2Impl extends MvccEntitySerializationStrategyImpl {


    private static final IdRowCompositeSerializer ID_SER = IdRowCompositeSerializer.get();


    private static final CollectionScopedRowKeySerializer<Id> ROW_KEY_SER =
            new CollectionScopedRowKeySerializer<>( ID_SER );


    private static final MultiTennantColumnFamily<ScopedRowKey<CollectionPrefixedKey<Id>>, UUID> CF_ENTITY_DATA =
            new MultiTennantColumnFamily<>( "Entity_Version_Data_V2", ROW_KEY_SER, UUIDSerializer.get() );

    private static final FieldBufferSerializer FIELD_BUFFER_SERIALIZER = FieldBufferSerializer.get();


    private final EntitySerializer entitySerializer;


    @Inject
    public MvccEntitySerializationStrategyV2Impl( final Keyspace keyspace, final SerializationFig serializationFig, final CassandraFig cassandraFig ) {
        super( keyspace, serializationFig, cassandraFig );
        entitySerializer = new EntitySerializer( serializationFig );
    }


    @Override
    protected AbstractSerializer<EntityWrapper> getEntitySerializer() {
        return entitySerializer;
    }


    @Override
    protected MultiTennantColumnFamily<ScopedRowKey<CollectionPrefixedKey<Id>>, UUID> getColumnFamily() {
        return CF_ENTITY_DATA;
    }


    @Override
    public int getImplementationVersion() {
        return CollectionDataVersions.BUFFER_SHORT_FIX.getVersion();
    }


    /**
     * We should only ever create this once, since this impl is a singleton
     */
    public final class EntitySerializer extends AbstractSerializer<EntityWrapper> {


        private final SmileFactory SMILE_FACTORY = new SmileFactory();

        private final ObjectMapper MAPPER = new ObjectMapper( SMILE_FACTORY );


        private SerializationFig serializationFig;


        private byte STATE_COMPLETE = 0;
        private byte STATE_DELETED = 1;
        private byte STATE_PARTIAL = 2;

        private byte VERSION = 1;


        public EntitySerializer( final SerializationFig serializationFig ) {
            this.serializationFig = serializationFig;

            //                mapper.enable(SerializationFeature.INDENT_OUTPUT); don't indent output,
            // causes slowness
            MAPPER.enableDefaultTypingAsProperty( ObjectMapper.DefaultTyping.JAVA_LANG_OBJECT, "@class" );
        }


        @Override
        public ByteBuffer toByteBuffer( final EntityWrapper wrapper ) {
            if ( wrapper == null ) {
                return null;
            }

            //we always have a max of 3 fields
            FieldBufferBuilder builder = new FieldBufferBuilder( 3 );

            builder.addByte( VERSION );

            //mark this version as empty
            if ( !wrapper.entity.isPresent() ) {
                //we're empty
                builder.addByte( STATE_DELETED );


                return FIELD_BUFFER_SERIALIZER.toByteBuffer( builder.build() );
            }

            //we have an entity

            if ( wrapper.status == MvccEntity.Status.COMPLETE ) {
                builder.addByte( STATE_COMPLETE );
            }

            else {
                builder.addByte( STATE_PARTIAL );
            }


            final Entity entity = wrapper.entity.get();
            final byte[] entityBytes;

            try {
                entityBytes = MAPPER.writeValueAsBytes( entity );
            }
            catch ( JsonProcessingException e ) {
                throw new RuntimeException( "Unable to serialize entity", e );
            }


            final int maxEntrySize = serializationFig.getMaxEntitySize();

            if ( entityBytes.length > maxEntrySize ) {
                throw new EntityTooLargeException( entity, maxEntrySize, entityBytes.length,
                        "Your entity cannot exceed " + maxEntrySize + " bytes. The entity you tried to save was "
                                + entityBytes.length + " bytes" );
            }

            builder.addBytes( entityBytes );

            return FIELD_BUFFER_SERIALIZER.toByteBuffer( builder.build() );
        }


        @Override
        public EntityWrapper fromByteBuffer( final ByteBuffer byteBuffer ) {

            /**
             * We intentionally turn data corruption exceptions when we're unable to de-serialize
             * the data in cassandra.  If this occurs, we'll never be able to de-serialize it
             * and it should be considered lost.  This is an error that is occurring due to a bug
             * in serializing the entity.  This is a lazy recognition + repair signal for deployment with
             * existing systems.
             */

            final FieldBuffer fieldBuffer;

            try {
                fieldBuffer = FIELD_BUFFER_SERIALIZER.fromByteBuffer( byteBuffer );
            }
            catch ( Exception e ) {
                throw new DataCorruptionException( "Unable to de-serialze entity", e );
            }

            FieldBufferParser parser = new FieldBufferParser( fieldBuffer );


            byte version = parser.readByte();

            if ( VERSION != version ) {
                throw new UnsupportedOperationException( "A version of type " + version + " is unsupported" );
            }

            byte state = parser.readByte();

            // it's been deleted, remove it

            if ( STATE_DELETED == state ) {
                return new EntityWrapper( MvccEntity.Status.DELETED, Optional.<Entity>absent() );
            }

            Entity storedEntity;

            byte[] array = parser.readBytes();

            try {
                storedEntity = MAPPER.readValue( array, Entity.class );
            }
            catch ( Exception e ) {
                throw new DataCorruptionException( "Unable to read entity data", e );
            }

            final Optional<Entity> entity = Optional.of( storedEntity );

            if ( STATE_COMPLETE == state ) {
                return new EntityWrapper( MvccEntity.Status.COMPLETE, entity );
            }

            // it's partial by default
            return new EntityWrapper( MvccEntity.Status.PARTIAL, entity );
        }
    }
}
