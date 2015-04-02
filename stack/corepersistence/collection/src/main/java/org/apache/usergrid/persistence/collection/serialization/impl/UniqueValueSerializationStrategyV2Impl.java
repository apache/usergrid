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
import org.apache.usergrid.persistence.collection.serialization.impl.util.LegacyScopeUtils;
import org.apache.usergrid.persistence.core.astyanax.CassandraFig;
import org.apache.usergrid.persistence.core.astyanax.ColumnTypes;
import org.apache.usergrid.persistence.core.astyanax.IdRowCompositeSerializer;
import org.apache.usergrid.persistence.core.astyanax.MultiTennantColumnFamily;
import org.apache.usergrid.persistence.core.astyanax.MultiTennantColumnFamilyDefinition;
import org.apache.usergrid.persistence.core.astyanax.ScopedRowKey;
import org.apache.usergrid.persistence.core.astyanax.ScopedRowKeySerializer;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.field.Field;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.astyanax.Keyspace;


/**
 * V1 impl with unique value serialization strategy with the collection scope
 */
@Singleton
public class UniqueValueSerializationStrategyV2Impl  extends UniqueValueSerializationStrategyImpl<Field, Id> {


    private static final ScopedRowKeySerializer<Field>  ROW_KEY_SER = new ScopedRowKeySerializer<>( UniqueFieldRowKeySerializer.get() );


    private static final EntityVersionSerializer ENTITY_VERSION_SER = new EntityVersionSerializer();

    private static final MultiTennantColumnFamily<ScopedRowKey<Field>, EntityVersion>
        CF_UNIQUE_VALUES = new MultiTennantColumnFamily<>( "Unique_Values_V2", ROW_KEY_SER, ENTITY_VERSION_SER );


    private static final IdRowCompositeSerializer ID_SER = IdRowCompositeSerializer.get();


    private static final ScopedRowKeySerializer<Id> ENTITY_ROW_KEY_SER =
        new ScopedRowKeySerializer<>( ID_SER );


    private static final MultiTennantColumnFamily<ScopedRowKey<Id>, UniqueFieldEntry>
        CF_ENTITY_UNIQUE_VALUE_LOG =
        new MultiTennantColumnFamily<>( "Entity_Unique_Values_V2", ENTITY_ROW_KEY_SER, UniqueFieldEntrySerializer.get() );


    /**
     * Construct serialization strategy for keyspace.
     *
     * @param keyspace Keyspace in which to store Unique Values.
     * @param cassandraFig The cassandra configuration
     * @param serializationFig The serialization configuration
     */
    @Inject
    public UniqueValueSerializationStrategyV2Impl( final Keyspace keyspace, final CassandraFig cassandraFig,
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
    protected MultiTennantColumnFamily<ScopedRowKey<Field>, EntityVersion> getUniqueValuesCF() {
        return CF_UNIQUE_VALUES;
    }


    @Override
    protected MultiTennantColumnFamily<ScopedRowKey<Id>, UniqueFieldEntry>
    getEntityUniqueLogCF() {
        return CF_ENTITY_UNIQUE_VALUE_LOG;
    }


    @Override
    protected Field createUniqueValueKey( final Id applicationId,  final String type, final Field field) {
        return field;
    }


    @Override
    protected Field parseRowKey( final ScopedRowKey<Field> rowKey ) {
        return rowKey.getKey();
    }


    @Override
    protected Id createEntityUniqueLogKey( final Id applicationId, final Id uniqueValueId ) {
       return uniqueValueId;
    }


    @Override
    public int getImplementationVersion() {
        return CollectionDataVersions.LOG_REMOVAL.getVersion();
    }
}
