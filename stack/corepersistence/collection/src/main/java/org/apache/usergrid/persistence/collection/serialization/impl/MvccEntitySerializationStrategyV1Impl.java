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
import java.util.Arrays;
import java.util.UUID;

import org.apache.usergrid.persistence.collection.MvccEntity;
import org.apache.usergrid.persistence.collection.exception.DataCorruptionException;
import org.apache.usergrid.persistence.collection.serialization.SerializationFig;
import org.apache.usergrid.persistence.core.astyanax.CassandraFig;
import org.apache.usergrid.persistence.core.astyanax.IdRowCompositeSerializer;
import org.apache.usergrid.persistence.core.astyanax.MultiTennantColumnFamily;
import org.apache.usergrid.persistence.core.astyanax.ScopedRowKey;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.model.CompositeBuilder;
import com.netflix.astyanax.model.CompositeParser;
import com.netflix.astyanax.model.Composites;
import com.netflix.astyanax.serializers.AbstractSerializer;
import com.netflix.astyanax.serializers.ByteBufferSerializer;
import com.netflix.astyanax.serializers.BytesArraySerializer;
import com.netflix.astyanax.serializers.UUIDSerializer;


/**
 * Version 1 implementation of entity serialization
 */
@Singleton
public class MvccEntitySerializationStrategyV1Impl extends MvccEntitySerializationStrategyImpl {

    private static final EntitySerializer ENTITY_JSON_SER = new EntitySerializer();


    private static final IdRowCompositeSerializer ID_SER = IdRowCompositeSerializer.get();


    private static final CollectionScopedRowKeySerializer<Id> ROW_KEY_SER =
            new CollectionScopedRowKeySerializer<>( ID_SER );


    private static final MultiTennantColumnFamily<ScopedRowKey<CollectionPrefixedKey<Id>>, UUID> CF_ENTITY_DATA =
                new MultiTennantColumnFamily<>( "Entity_Version_Data", ROW_KEY_SER, UUIDSerializer.get() );



    @Inject
    public MvccEntitySerializationStrategyV1Impl( final Keyspace keyspace, final SerializationFig serializationFig, final CassandraFig cassandraFig ) {
        super( keyspace, serializationFig, cassandraFig );
    }


    @Override
    protected AbstractSerializer<MvccEntitySerializationStrategyImpl.EntityWrapper> getEntitySerializer() {
        return ENTITY_JSON_SER;
    }


    @Override
    protected MultiTennantColumnFamily<ScopedRowKey<CollectionPrefixedKey<Id>>, UUID> getColumnFamily() {
        return CF_ENTITY_DATA;
    }


    @Override
    public int getImplementationVersion() {
        return CollectionDataVersions.INITIAL.getVersion();
    }


    public static class EntitySerializer extends AbstractSerializer<EntityWrapper> {


        private static final ByteBufferSerializer BUFFER_SERIALIZER = ByteBufferSerializer.get();

        private static final BytesArraySerializer BYTES_ARRAY_SERIALIZER = BytesArraySerializer.get();


        public static final SmileFactory f = new SmileFactory();

        public static ObjectMapper mapper;

        private static byte[] STATE_COMPLETE = new byte[] { 0 };
        private static byte[] STATE_DELETED = new byte[] { 1 };
        private static byte[] STATE_PARTIAL = new byte[] { 2 };

        private static byte[] VERSION = new byte[] { 0 };


        public EntitySerializer() {
            try {
                mapper = new ObjectMapper( f );
                //                mapper.enable(SerializationFeature.INDENT_OUTPUT); don't indent output,
                // causes slowness
                mapper.enableDefaultTypingAsProperty( ObjectMapper.DefaultTyping.JAVA_LANG_OBJECT, "@class" );
            }
            catch ( Exception e ) {
                throw new RuntimeException( "Error setting up mapper", e );
            }
        }


        @Override
        public ByteBuffer toByteBuffer( final EntityWrapper wrapper ) {
            if ( wrapper == null ) {
                return null;
            }

            CompositeBuilder builder = Composites.newCompositeBuilder();

            builder.addBytes( VERSION );

            //mark this version as empty
            if ( !wrapper.entity.isPresent() ) {
                //we're empty
                builder.addBytes( STATE_DELETED );

                return builder.build();
            }

            //we have an entity

            if ( wrapper.status == MvccEntity.Status.COMPLETE ) {
                builder.addBytes( STATE_COMPLETE );
            }

            else {
                builder.addBytes( STATE_PARTIAL );
            }

            try {
                final byte[] entityBytes = mapper.writeValueAsBytes( wrapper.entity.get() );
                builder.addBytes( entityBytes );
            }
            catch ( Exception e ) {
                throw new RuntimeException( "Unable to serialize entity", e );
            }

            return builder.build();
        }


        @Override
        public EntityWrapper fromByteBuffer( final ByteBuffer byteBuffer ) {

            /**
             * We intentionally turn data corruption exceptions when we're unable to de-serialize
             * the data in cassandra.  If this occurs, we'll never be able to de-serialize it
             * and it should be considered lost.  This is an error that is occuring due to a bug
             * in serializing the entity.  This is a lazy recognition + repair signal for deployment with
             * existing systems.
             */
            CompositeParser parser;
            try {
                parser = Composites.newCompositeParser( byteBuffer );
            }
            catch ( Exception e ) {
                throw new DataCorruptionException( "Unable to de-serialze entity", e );
            }

            byte[] version = parser.read( BYTES_ARRAY_SERIALIZER );

            if ( !Arrays.equals( VERSION, version ) ) {
                throw new UnsupportedOperationException( "A version of type " + version + " is unsupported" );
            }

            byte[] state = parser.read( BYTES_ARRAY_SERIALIZER );

            // it's been deleted, remove it

            if ( Arrays.equals( STATE_DELETED, state ) ) {
                return new EntityWrapper( MvccEntity.Status.DELETED, Optional.<Entity>absent() );
            }

            Entity storedEntity;

            ByteBuffer jsonBytes = parser.read( BUFFER_SERIALIZER );
            byte[] array = jsonBytes.array();
            int start = jsonBytes.arrayOffset();
            int length = jsonBytes.remaining();

            try {
                storedEntity = mapper.readValue( array, start, length, Entity.class );
            }
            catch ( Exception e ) {
                throw new DataCorruptionException( "Unable to read entity data", e );
            }

            final Optional<Entity> entity = Optional.of( storedEntity );

            if ( Arrays.equals( STATE_COMPLETE, state ) ) {
                return new EntityWrapper( MvccEntity.Status.COMPLETE, entity );
            }

            // it's partial by default
            return new EntityWrapper( MvccEntity.Status.PARTIAL, entity );
        }
    }
}
