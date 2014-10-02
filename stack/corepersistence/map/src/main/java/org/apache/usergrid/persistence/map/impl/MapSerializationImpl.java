/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.usergrid.persistence.map.impl;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

import com.google.common.base.Preconditions;
import org.apache.cassandra.db.marshal.BytesType;
import org.apache.cassandra.db.marshal.UTF8Type;

import org.apache.usergrid.persistence.core.astyanax.CompositeFieldSerializer;
import org.apache.usergrid.persistence.core.astyanax.MultiTennantColumnFamily;
import org.apache.usergrid.persistence.core.astyanax.MultiTennantColumnFamilyDefinition;
import org.apache.usergrid.persistence.core.astyanax.OrganizationScopedRowKeySerializer;
import org.apache.usergrid.persistence.core.astyanax.ScopedRowKey;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.map.MapScope;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.connectionpool.exceptions.NotFoundException;
import com.netflix.astyanax.model.Column;
import com.netflix.astyanax.model.CompositeBuilder;
import com.netflix.astyanax.model.CompositeParser;
import com.netflix.astyanax.serializers.BooleanSerializer;
import com.netflix.astyanax.serializers.StringSerializer;


@Singleton
public class MapSerializationImpl implements MapSerialization {

    private static final MapKeySerializer KEY_SERIALIZER = new MapKeySerializer();
        private static final OrganizationScopedRowKeySerializer<String> MAP_KEY_SERIALIZER =
                new OrganizationScopedRowKeySerializer<>( KEY_SERIALIZER );


        private static final MapEntrySerializer ENTRY_SERIALIZER = new MapEntrySerializer();
        private static final OrganizationScopedRowKeySerializer<MapEntryKey> MAP_ENTRY_SERIALIZER =
                new OrganizationScopedRowKeySerializer<>( ENTRY_SERIALIZER );


        private static final BooleanSerializer BOOLEAN_SERIALIZER = BooleanSerializer.get();

        private static final StringSerializer STRING_SERIALIZER = StringSerializer.get();


        /**
         * CFs where the row key contains the source node id
         */
        private static final MultiTennantColumnFamily<ApplicationScope, MapEntryKey, Boolean> MAP_ENTRIES =
                new MultiTennantColumnFamily<>( "Map_Entries", MAP_ENTRY_SERIALIZER, BOOLEAN_SERIALIZER );


        /**
         * CFs where the row key contains the source node id
         */
        private static final MultiTennantColumnFamily<ApplicationScope, String, String> MAP_KEYS =
                new MultiTennantColumnFamily<>( "Map_Keys", MAP_KEY_SERIALIZER, STRING_SERIALIZER );



    private final Keyspace keyspace;


    @Inject
    public MapSerializationImpl( final Keyspace keyspace ) {this.keyspace = keyspace;}


    @Override
    public String getString( final MapScope scope, final String key ) {
        Column<Boolean> col = getValue(scope, key);
        return (col !=null) ?  col.getStringValue(): null;
    }


    @Override
    public void putString( final MapScope scope, final String key, final String value ) {
        Preconditions.checkNotNull(scope, "mapscope is required");
        Preconditions.checkNotNull( key, "key is required" );
        Preconditions.checkNotNull( value, "value is required" );

        final MutationBatch batch = keyspace.prepareMutationBatch();

        //add it to the entry
        final ScopedRowKey<ApplicationScope, MapEntryKey> entryRowKey = MapEntryKey.fromKey(scope, key);

        //serialize to the entry
        batch.withRow(MAP_ENTRIES, entryRowKey).putColumn(true, value);

        //add it to the keys

        final ScopedRowKey<ApplicationScope, String> keyRowKey =
                ScopedRowKey.fromKey((ApplicationScope) scope, key);

        //serialize to the entry
        batch.withRow(MAP_KEYS, keyRowKey).putColumn(key, true);

        executeBatch(batch);
    }




    @Override
    public UUID getUuid( final MapScope scope, final String key ) {

        Column<Boolean> col = getValue(scope, key);
        return (col !=null) ?  col.getUUIDValue(): null;
    }


    @Override
    public void putUuid( final MapScope scope, final String key, final UUID putUuid ) {

        Preconditions.checkNotNull(scope, "mapscope is required");
        Preconditions.checkNotNull( key, "key is required" );
        Preconditions.checkNotNull( putUuid, "value is required" );

        final MutationBatch batch = keyspace.prepareMutationBatch();

        //add it to the entry
        final ScopedRowKey<ApplicationScope, MapEntryKey> entryRowKey = MapEntryKey.fromKey(scope, key);

        //serialize to the entry
        batch.withRow(MAP_ENTRIES, entryRowKey).putColumn(true, putUuid);

        //add it to the keys

        final ScopedRowKey<ApplicationScope, String> keyRowKey =
                ScopedRowKey.fromKey((ApplicationScope) scope, key);

        //serialize to the entry
        batch.withRow(MAP_KEYS, keyRowKey).putColumn(key, true);

        executeBatch(batch);

    }


    @Override
    public Long getLong( final MapScope scope, final String key ) {
        Column<Boolean> col = getValue(scope, key);
        return (col !=null) ?  col.getLongValue(): null;
    }




    @Override
    public void putLong( final MapScope scope, final String key, final Long value ) {

        Preconditions.checkNotNull(scope, "mapscope is required");
        Preconditions.checkNotNull( key, "key is required" );
        Preconditions.checkNotNull( value, "value is required" );

        final MutationBatch batch = keyspace.prepareMutationBatch();

        //add it to the entry
        final ScopedRowKey<ApplicationScope, MapEntryKey> entryRowKey = MapEntryKey.fromKey(scope, key);

        //serialize to the entry
        batch.withRow(MAP_ENTRIES, entryRowKey).putColumn(true, value);

        //add it to the keys

        final ScopedRowKey<ApplicationScope, String> keyRowKey =
                ScopedRowKey.fromKey((ApplicationScope) scope, key);

        //serialize to the entry
        batch.withRow(MAP_KEYS, keyRowKey).putColumn(key, true);

        executeBatch(batch);
    }


    @Override
    public void delete( final MapScope scope, final String key ) {
        final MutationBatch batch = keyspace.prepareMutationBatch();
        final ScopedRowKey<ApplicationScope, MapEntryKey> entryRowKey = MapEntryKey.fromKey(scope, key);

        //serialize to the entry
        batch.withRow(MAP_ENTRIES, entryRowKey).delete();

        //add it to the keys

        final ScopedRowKey<ApplicationScope, String> keyRowKey = ScopedRowKey.fromKey((ApplicationScope) scope, key);

        //serialize to the entry
        batch.withRow(MAP_KEYS, keyRowKey).delete();
        executeBatch(batch);
    }


    @Override
    public Collection<MultiTennantColumnFamilyDefinition> getColumnFamilies()
    {
        final MultiTennantColumnFamilyDefinition mapEntries = new MultiTennantColumnFamilyDefinition( MAP_ENTRIES,
                       BytesType.class.getSimpleName(), BytesType.class.getSimpleName(), BytesType.class.getSimpleName(), MultiTennantColumnFamilyDefinition.CacheOption.KEYS );


        final MultiTennantColumnFamilyDefinition mapKeys = new MultiTennantColumnFamilyDefinition( MAP_KEYS,
                               BytesType.class.getSimpleName(), UTF8Type.class.getSimpleName(), BytesType.class.getSimpleName(), MultiTennantColumnFamilyDefinition.CacheOption.KEYS );

        return Arrays.asList( mapEntries, mapKeys );
    }

    private  Column<Boolean> getValue(MapScope scope, String key) {
        //add it to the entry
        final ScopedRowKey<ApplicationScope, MapEntryKey> entryRowKey = MapEntryKey.fromKey(scope, key);


        try {
            final Column<Boolean> result =
                    keyspace.prepareQuery( MAP_ENTRIES ).getKey( entryRowKey ).getColumn( true ).execute().getResult();

            return result;
        }
        catch ( NotFoundException nfe ) {
            //nothing to return
            return null;
        }
        catch ( ConnectionException e ) {
            throw new RuntimeException( "Unable to connect to cassandra", e );
        }
    }

    private void executeBatch(MutationBatch batch) {
        try {
            batch.execute();
        } catch (ConnectionException e) {
            throw new RuntimeException("Unable to connect to cassandra", e);
        }
    }
    /**
     * Inner class to serialize and edgeIdTypeKey
     */


    /**
     * Inner class to serialize and edgeIdTypeKey
     */
    private static class MapKeySerializer implements CompositeFieldSerializer<String> {


        @Override
        public void toComposite( final CompositeBuilder builder, final String key ) {
            builder.addString( key );
        }


        @Override
        public String fromComposite( final CompositeParser composite ) {
            final String key = composite.readString();

            return key;
        }
    }


    /**
     * Inner class to serialize and edgeIdTypeKey
     */
    private static class MapEntrySerializer implements CompositeFieldSerializer<MapEntryKey> {

        @Override
        public void toComposite( final CompositeBuilder builder, final MapEntryKey key ) {

            builder.addString( key.mapName );
            builder.addString( key.key );
        }


        @Override
        public MapEntryKey fromComposite( final CompositeParser composite ) {

            final String mapName = composite.readString();

            final String entryKey = composite.readString();

            return new MapEntryKey( mapName, entryKey );
        }
    }


    /**
     * Entries for serializing map entries and keys to a row
     */
    private static class MapEntryKey {
        public final String mapName;
        public final String key;


        private MapEntryKey( final String mapName, final String key ) {
            this.mapName = mapName;
            this.key = key;
        }


        /**
         * Create a scoped row key from the key
         */
        public static ScopedRowKey<ApplicationScope, MapEntryKey> fromKey( final MapScope mapScope, final String key ) {
            return ScopedRowKey.fromKey( ( ApplicationScope ) mapScope, new MapEntryKey( mapScope.getName(), key ) );
        }
    }
}
