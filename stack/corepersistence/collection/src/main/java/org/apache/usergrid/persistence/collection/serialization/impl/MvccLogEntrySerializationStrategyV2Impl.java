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


import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

import org.apache.cassandra.db.marshal.BytesType;
import org.apache.cassandra.db.marshal.IntegerType;
import org.apache.cassandra.db.marshal.ReversedType;
import org.apache.cassandra.db.marshal.UUIDType;

import org.apache.usergrid.persistence.collection.serialization.SerializationFig;
import org.apache.usergrid.persistence.core.astyanax.IdRowCompositeSerializer;
import org.apache.usergrid.persistence.core.astyanax.MultiTennantColumnFamily;
import org.apache.usergrid.persistence.core.astyanax.MultiTennantColumnFamilyDefinition;
import org.apache.usergrid.persistence.core.astyanax.ScopedRowKey;
import org.apache.usergrid.persistence.core.astyanax.ScopedRowKeySerializer;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.serializers.UUIDSerializer;


/**
 * Serialization strategy V2 that doesn't contain collection scope
 */
@Singleton
public class MvccLogEntrySerializationStrategyV2Impl extends MvccLogEntrySerializationStrategyImpl<Id> {


    private static final IdRowCompositeSerializer ID_SER = IdRowCompositeSerializer.get();

    private static final ScopedRowKeySerializer<Id> ROW_KEY_SER = new ScopedRowKeySerializer<>( ID_SER );


    private static final MultiTennantColumnFamily<ScopedRowKey<Id>, UUID> CF_ENTITY_LOG_V2 =
        new MultiTennantColumnFamily<>( "Entity_Log_V2", ROW_KEY_SER, UUIDSerializer.get() );


    @Inject
    public MvccLogEntrySerializationStrategyV2Impl( final Keyspace keyspace, final SerializationFig fig ) {
        super( keyspace, fig );
    }


    @Override
    public int getImplementationVersion() {
        return CollectionDataVersions.LOG_REMOVAL.getVersion();
    }


    @Override
    protected MultiTennantColumnFamily<ScopedRowKey<Id>, UUID> getColumnFamily() {
        return CF_ENTITY_LOG_V2;
    }


    @Override
    protected ScopedRowKey<Id> createKey( final Id applicationId, final Id entityId ) {
        final ScopedRowKey<Id> rowKey = ScopedRowKey.fromKey( applicationId, entityId );

        return rowKey;
    }


    @Override
    protected Id getEntityIdFromKey( final ScopedRowKey<Id> key ) {
        return key.getKey();
    }


    @Override
    public Collection<MultiTennantColumnFamilyDefinition> getColumnFamilies() {
        //create the CF entity data.  We want it reversed b/c we want the most recent version at the top of the
        //row for fast seeks
        MultiTennantColumnFamilyDefinition cf =
            new MultiTennantColumnFamilyDefinition( CF_ENTITY_LOG_V2, BytesType.class.getSimpleName(),
                ReversedType.class.getSimpleName() + "(" + UUIDType.class.getSimpleName() + ")",
                IntegerType.class.getSimpleName(), MultiTennantColumnFamilyDefinition.CacheOption.KEYS );


        return Collections.singleton( cf );
    }
}
