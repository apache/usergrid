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

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.netflix.astyanax.ColumnListMutation;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.connectionpool.exceptions.NotFoundException;
import com.netflix.astyanax.model.ColumnList;
import java.util.Collections;
import org.apache.cassandra.db.marshal.BytesType;
import org.apache.cassandra.db.marshal.DynamicCompositeType;

import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.astyanax.MultiTennantColumnFamily;
import org.apache.usergrid.persistence.collection.astyanax.MultiTennantColumnFamilyDefinition;
import org.apache.usergrid.persistence.collection.astyanax.ScopedRowKey;
import org.apache.usergrid.persistence.collection.cassandra.ColumnTypes;
import org.apache.usergrid.persistence.collection.migration.Migration;
import org.apache.usergrid.persistence.collection.serialization.impl.CollectionScopedRowKeySerializer;
import org.apache.usergrid.persistence.model.field.Field;


/**
 * Reads and writes to UniqueValues column family.
 */
public class UniqueValueSerializationStrategyImpl implements UniqueValueSerializationStrategy, Migration {

    // TODO: use "real" field serializer here instead once it is ready
    private static final CollectionScopedRowKeySerializer<Field> ROW_KEY_SER =
            new CollectionScopedRowKeySerializer<Field>( FieldSerializer.get() );

    private static final EntityVersionSerializer ENTITY_VERSION_SER = new EntityVersionSerializer();

    private static final MultiTennantColumnFamily<CollectionScope, Field, EntityVersion> CF_UNIQUE_VALUES =
        new MultiTennantColumnFamily<CollectionScope, Field, EntityVersion>( "Unique_Values",
                ROW_KEY_SER,
                ENTITY_VERSION_SER );

    protected final Keyspace keyspace;


    /**
     * Construct serialization strategy for keyspace.
     * @param keyspace Keyspace in which to store Unique Values.
     */
    @Inject
    public UniqueValueSerializationStrategyImpl( final Keyspace keyspace ) {
        this.keyspace = keyspace;
    }


    @Override
    public java.util.Collection getColumnFamilies() {

        MultiTennantColumnFamilyDefinition cf = new MultiTennantColumnFamilyDefinition(
                CF_UNIQUE_VALUES,
                BytesType.class.getSimpleName(),
                ColumnTypes.DYNAMIC_COMPOSITE_TYPE,
                BytesType.class.getSimpleName(), MultiTennantColumnFamilyDefinition.CacheOption.KEYS );

        return Collections.singleton( cf );
    }


    public MutationBatch write( UniqueValue uniqueValue ) {
        return write( uniqueValue, Integer.MAX_VALUE );
    }


    @Override
    public MutationBatch write( UniqueValue value, Integer timeToLive ) {

        Preconditions.checkNotNull( value, "value is required" );
        Preconditions.checkNotNull( timeToLive, "timeToLive is required" );

        final EntityVersion ev = new EntityVersion( value.getEntityId(), value.getEntityVersion() );

        final Integer ttl;
        if ( timeToLive.equals( Integer.MAX_VALUE )) {
            ttl = null;
        } else {
            ttl = timeToLive;
        }

        return doWrite( value.getCollectionScope(), value.getField(),
            new UniqueValueSerializationStrategyImpl.RowOp() {

            @Override
            public void doOp( final ColumnListMutation<EntityVersion> colMutation ) {
                colMutation.putColumn( ev, 0x0, ttl );
            }
        } );
    }


    @Override
    public MutationBatch delete(UniqueValue value) {

        Preconditions.checkNotNull( value, "value is required" );

        final EntityVersion ev = new EntityVersion( value.getEntityId(), value.getEntityVersion() );

        return doWrite( value.getCollectionScope(), value.getField(),
            new UniqueValueSerializationStrategyImpl.RowOp() {

            @Override
            public void doOp( final ColumnListMutation<EntityVersion> colMutation ) {
                colMutation.deleteColumn(ev);
            }
        } );
    }


    /**
     * Do the column update or delete for the given column and row key
     * @param context We need to use this when getting the keyspace
     */
    private MutationBatch doWrite( CollectionScope context, Field field, RowOp op ) {
        final MutationBatch batch = keyspace.prepareMutationBatch();
        op.doOp( batch.withRow( CF_UNIQUE_VALUES, ScopedRowKey.fromKey( context, field ) ) );
        return batch;
    }


    @Override
    public UniqueValue load( CollectionScope colScope, Field field ) throws ConnectionException {

        Preconditions.checkNotNull( field, "field is required" );


        //TODO Dave, this doesn't limit the size.  We should limit it to 1 explicitly, otherwise you can get a huge result set explosion if multiple values are attempting to write the same unique value.
        ColumnList<EntityVersion> result;
        try {
            result = keyspace.prepareQuery( CF_UNIQUE_VALUES )
                .getKey( ScopedRowKey.fromKey( colScope, field ) )
                .execute().getResult();
        }
        catch ( NotFoundException nfe ) {
            return null;
        }

        if ( result.isEmpty() ) {
            return null;
        }

        EntityVersion ev = result.getColumnByIndex(0).getName();
        return new UniqueValueImpl( colScope, field, ev.getEntityId(), ev.getEntityVersion() );
    }


    /**
     * Simple callback to perform puts and deletes with a common row setup code
     */
    private static interface RowOp {
        void doOp( ColumnListMutation<EntityVersion> colMutation );
    }
}
