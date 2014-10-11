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
package org.apache.usergrid.persistence.collection.serialization.impl;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.cassandra.db.marshal.BytesType;

import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.serialization.UniqueValue;
import org.apache.usergrid.persistence.collection.serialization.UniqueValueSerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.UniqueValueSet;
import org.apache.usergrid.persistence.core.astyanax.ColumnTypes;
import org.apache.usergrid.persistence.core.astyanax.MultiTennantColumnFamily;
import org.apache.usergrid.persistence.core.astyanax.MultiTennantColumnFamilyDefinition;
import org.apache.usergrid.persistence.core.astyanax.ScopedRowKey;
import org.apache.usergrid.persistence.core.migration.Migration;
import org.apache.usergrid.persistence.core.util.ValidationUtils;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.field.Field;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.netflix.astyanax.ColumnListMutation;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.Column;
import com.netflix.astyanax.model.ColumnList;
import com.netflix.astyanax.model.Row;
import com.netflix.astyanax.util.RangeBuilder;


/**
 * Reads and writes to UniqueValues column family.
 */
public class UniqueValueSerializationStrategyImpl implements UniqueValueSerializationStrategy, Migration {

    private static final Logger log = LoggerFactory.getLogger( UniqueValueSerializationStrategyImpl.class );

    // TODO: use "real" field serializer here instead once it is ready
    private static final CollectionScopedRowKeySerializer<Field> ROW_KEY_SER =
            new CollectionScopedRowKeySerializer<>( FieldSerializer.get() );

    private static final EntityVersionSerializer ENTITY_VERSION_SER = new EntityVersionSerializer();

    private static final MultiTennantColumnFamily<CollectionScope, Field, EntityVersion> CF_UNIQUE_VALUES =
            new MultiTennantColumnFamily<CollectionScope, Field, EntityVersion>( "Unique_Values", ROW_KEY_SER,
                    ENTITY_VERSION_SER );

    protected final Keyspace keyspace;


    /**
     * Construct serialization strategy for keyspace.
     *
     * @param keyspace Keyspace in which to store Unique Values.
     */
    @Inject
    public UniqueValueSerializationStrategyImpl( final Keyspace keyspace ) {
        this.keyspace = keyspace;
    }


    @Override
    public Collection<MultiTennantColumnFamilyDefinition> getColumnFamilies() {

        MultiTennantColumnFamilyDefinition cf =
                new MultiTennantColumnFamilyDefinition( CF_UNIQUE_VALUES, BytesType.class.getSimpleName(),
                        ColumnTypes.DYNAMIC_COMPOSITE_TYPE, BytesType.class.getSimpleName(),
                        MultiTennantColumnFamilyDefinition.CacheOption.KEYS );

        return Collections.singleton( cf );
    }


    public MutationBatch write(final CollectionScope scope,  UniqueValue uniqueValue ) {
        return write( scope, uniqueValue, Integer.MAX_VALUE );
    }


    @Override
    public MutationBatch write(final CollectionScope scope,  final UniqueValue value, final Integer timeToLive ) {

        Preconditions.checkNotNull( value, "value is required" );
        Preconditions.checkNotNull( timeToLive, "timeToLive is required" );

        final Id entityId = value.getEntityId();
        final UUID entityVersion = value.getEntityVersion();

        ValidationUtils.verifyIdentity( entityId );
              ValidationUtils.verifyVersion( entityVersion );

        log.debug( "Writing unique value scope={} id={} version={} name={} value={} ttl={} ", new Object[] {
               scope.getName(), entityId, entityVersion,
                value.getField().getName(), value.getField().getValue(), timeToLive
        } );

        final EntityVersion ev = new EntityVersion( value.getEntityId(), value.getEntityVersion() );

        final Integer ttl;
        if ( timeToLive.equals( Integer.MAX_VALUE ) ) {
            ttl = null;
        }
        else {
            ttl = timeToLive;
        }

        return doWrite( scope, value.getField(), new UniqueValueSerializationStrategyImpl.RowOp() {

            @Override
            public void doOp( final ColumnListMutation<EntityVersion> colMutation ) {
                colMutation.putColumn( ev, 0x0, ttl );
            }
        } );
    }


    @Override
    public MutationBatch delete(final CollectionScope scope,  UniqueValue value ) {

        Preconditions.checkNotNull( value, "value is required" );

        final EntityVersion ev = new EntityVersion( value.getEntityId(), value.getEntityVersion() );

        return doWrite( scope, value.getField(), new UniqueValueSerializationStrategyImpl.RowOp() {

            @Override
            public void doOp( final ColumnListMutation<EntityVersion> colMutation ) {
                colMutation.deleteColumn( ev );
            }
        } );
    }


    /**
     * Do the column update or delete for the given column and row key
     *
     * @param context We need to use this when getting the keyspace
     */
    private MutationBatch doWrite( CollectionScope context, Field field, RowOp op ) {
        final MutationBatch batch = keyspace.prepareMutationBatch();
        op.doOp( batch.withRow( CF_UNIQUE_VALUES, ScopedRowKey.fromKey( context, field ) ) );
        return batch;
    }


    @Override
    public UniqueValueSet load(final CollectionScope colScope, final Collection<Field> fields )
            throws ConnectionException {

        Preconditions.checkNotNull( fields, "fields are required" );
        Preconditions.checkArgument( fields.size() > 0, "More than 1 field msut be specified" );

        final List<ScopedRowKey<CollectionScope, Field>> keys = new ArrayList<>( fields.size() );

        for ( Field field : fields ) {
            final ScopedRowKey<CollectionScope, Field> rowKey = ScopedRowKey.fromKey( colScope, field );

            keys.add( rowKey );
        }

        final UniqueValueSetImpl uniqueValueSet = new UniqueValueSetImpl( fields.size() );

        Iterator<Row<ScopedRowKey<CollectionScope, Field>, EntityVersion>> results =
                keyspace.prepareQuery( CF_UNIQUE_VALUES ).getKeySlice( keys )
                        .withColumnRange( new RangeBuilder().setLimit( 1 ).build() ).execute().getResult().iterator();


        while ( results.hasNext() )

        {

            final Row<ScopedRowKey<CollectionScope, Field>, EntityVersion> unique = results.next();


            final Field field = unique.getKey().getKey();

            final Iterator<Column<EntityVersion>> columnList = unique.getColumns().iterator();

            //sanity check, nothing to do, skip it
            if ( !columnList.hasNext()) {
                continue;
            }

            final EntityVersion entityVersion = columnList.next().getName();


            final UniqueValueImpl uniqueValue = new UniqueValueImpl(field, entityVersion.getEntityId(),
                    entityVersion.getEntityVersion() );

            uniqueValueSet.addValue( uniqueValue );
        }

        return uniqueValueSet;
    }


    /**
     * Simple callback to perform puts and deletes with a common row setup code
     */
    private static interface RowOp {
        void doOp( ColumnListMutation<EntityVersion> colMutation );
    }
}
