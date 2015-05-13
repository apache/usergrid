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


import java.util.Arrays;
import java.util.Collection;

import org.apache.cassandra.db.marshal.BytesType;

import org.apache.usergrid.persistence.collection.serialization.SerializationFig;
import org.apache.usergrid.persistence.collection.serialization.UniqueValue;
import org.apache.usergrid.persistence.collection.serialization.impl.util.LegacyScopeUtils;
import org.apache.usergrid.persistence.core.astyanax.CassandraFig;
import org.apache.usergrid.persistence.core.astyanax.ColumnTypes;
import org.apache.usergrid.persistence.core.astyanax.IdRowCompositeSerializer;
import org.apache.usergrid.persistence.core.astyanax.MultiTennantColumnFamily;
import org.apache.usergrid.persistence.core.astyanax.MultiTennantColumnFamilyDefinition;
import org.apache.usergrid.persistence.core.astyanax.ScopedRowKey;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.field.Field;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.astyanax.Keyspace;


/**
 * V1 impl with unique value serialization strategy with the collection scope
 */
@Singleton
public class UniqueValueSerializationStrategyV1Impl  extends UniqueValueSerializationStrategyImpl<CollectionPrefixedKey<Field>, CollectionPrefixedKey<Id>> {


    private static final CollectionScopedRowKeySerializer<Field> ROW_KEY_SER =
        new CollectionScopedRowKeySerializer<>( UniqueFieldRowKeySerializer.get() );

    private static final EntityVersionSerializer ENTITY_VERSION_SER = new EntityVersionSerializer();

    private static final MultiTennantColumnFamily<ScopedRowKey<CollectionPrefixedKey<Field>>, EntityVersion>
        CF_UNIQUE_VALUES = new MultiTennantColumnFamily<>( "Unique_Values", ROW_KEY_SER, ENTITY_VERSION_SER );


    private static final IdRowCompositeSerializer ID_SER = IdRowCompositeSerializer.get();


    private static final CollectionScopedRowKeySerializer<Id> ENTITY_ROW_KEY_SER =
        new CollectionScopedRowKeySerializer<>( ID_SER );


    private static final MultiTennantColumnFamily<ScopedRowKey<CollectionPrefixedKey<Id>>, UniqueFieldEntry>
        CF_ENTITY_UNIQUE_VALUE_LOG =
        new MultiTennantColumnFamily<>( "Entity_Unique_Values", ENTITY_ROW_KEY_SER, UniqueFieldEntrySerializer.get() );


    /**
     * Construct serialization strategy for keyspace.
     *
     * @param keyspace Keyspace in which to store Unique Values.
     * @param cassandraFig The cassandra configuration
     * @param serializationFig The serialization configuration
     */
    @Inject
    public UniqueValueSerializationStrategyV1Impl( final Keyspace keyspace, final CassandraFig cassandraFig,
                                                   final SerializationFig serializationFig ) {
        super( keyspace, cassandraFig, serializationFig );
    }


    @Override
    public Collection<MultiTennantColumnFamilyDefinition> getColumnFamilies() {

        final MultiTennantColumnFamilyDefinition uniqueLookupCF =
            new MultiTennantColumnFamilyDefinition( CF_UNIQUE_VALUES, BytesType.class.getSimpleName(),
                ColumnTypes.DYNAMIC_COMPOSITE_TYPE, BytesType.class.getSimpleName(),
                MultiTennantColumnFamilyDefinition.CacheOption.KEYS );

        final MultiTennantColumnFamilyDefinition uniqueLogCF =
            new MultiTennantColumnFamilyDefinition( CF_ENTITY_UNIQUE_VALUE_LOG, BytesType.class.getSimpleName(),
                ColumnTypes.DYNAMIC_COMPOSITE_TYPE, BytesType.class.getSimpleName(),
                MultiTennantColumnFamilyDefinition.CacheOption.KEYS );

        return Arrays.asList( uniqueLookupCF, uniqueLogCF );
    }


    @Override
    protected MultiTennantColumnFamily<ScopedRowKey<CollectionPrefixedKey<Field>>, EntityVersion> getUniqueValuesCF() {
        return CF_UNIQUE_VALUES;
    }


    @Override
    protected MultiTennantColumnFamily<ScopedRowKey<CollectionPrefixedKey<Id>>, UniqueFieldEntry>
    getEntityUniqueLogCF() {
        return CF_ENTITY_UNIQUE_VALUE_LOG;
    }


    @Override
    protected CollectionPrefixedKey<Field> createUniqueValueKey( final Id applicationId,
                                                                 final String type, final Field field) {


        final String collectionName = LegacyScopeUtils.getCollectionScopeNameFromEntityType( type );


        final CollectionPrefixedKey<Field> uniquePrefixedKey =
            new CollectionPrefixedKey<>( collectionName, applicationId, field );

        return uniquePrefixedKey;
    }


    @Override
    protected Field parseRowKey( final ScopedRowKey<CollectionPrefixedKey<Field>> rowKey ) {
        return rowKey.getKey().getSubKey();
    }


    @Override
    protected CollectionPrefixedKey<Id> createEntityUniqueLogKey( final Id applicationId,
                                                                  final Id uniqueValueId ) {


        final String collectionName = LegacyScopeUtils.getCollectionScopeNameFromEntityType( uniqueValueId.getType() );


        final CollectionPrefixedKey<Id> collectionPrefixedEntityKey =
            new CollectionPrefixedKey<>( collectionName, applicationId, uniqueValueId );



        return collectionPrefixedEntityKey;
    }


    @Override
    public int getImplementationVersion() {
        return CollectionDataVersions.INITIAL.getVersion();
    }
}
