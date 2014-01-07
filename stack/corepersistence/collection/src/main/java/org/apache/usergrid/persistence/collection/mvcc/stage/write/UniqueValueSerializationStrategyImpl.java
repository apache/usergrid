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
import com.netflix.astyanax.serializers.IntegerSerializer;
import com.netflix.astyanax.serializers.StringSerializer;
import java.util.Collections;
import java.util.UUID;
import org.apache.cassandra.db.marshal.BytesType;
import org.apache.cassandra.db.marshal.IntegerType;
import org.apache.cassandra.db.marshal.UTF8Type;
import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.astyanax.MultiTennantColumnFamily;
import org.apache.usergrid.persistence.collection.astyanax.MultiTennantColumnFamilyDefinition;
import org.apache.usergrid.persistence.collection.astyanax.ScopedRowKey;
import org.apache.usergrid.persistence.collection.migration.Migration;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.field.Field;

/**
 *
 */
public class UniqueValueSerializationStrategyImpl implements UniqueValueSerializationStrategy, Migration {

    private static final UniqueValueRowKeySerializer ROW_KEY_SER = new UniqueValueRowKeySerializer();

    private static final MultiTennantColumnFamily<CollectionScope, Field, String> CF_UNIQUE_VALUES =
        new MultiTennantColumnFamily<CollectionScope, Field, String>( 
            "Unique_Values", ROW_KEY_SER, StringSerializer.get() );

    protected final Keyspace keyspace;
    protected final int timeout;

    @Inject
    public UniqueValueSerializationStrategyImpl( final Keyspace keyspace, final int timeout ) {
        this.keyspace = keyspace;
        this.timeout = timeout;
    }

    public MutationBatch write( UniqueValue value ) {

        Preconditions.checkNotNull( value, "value is required" );

        final StringBuilder sb = new StringBuilder();
        sb.append( value.getEntityId().getUuid().toString() );
        sb.append( "|" );
        sb.append( value.getEntityId().getType() );
        sb.append( "|" );
        sb.append( value.getEntityVersion().toString() );
        final String colName = sb.toString();

        return doWrite( value.getCollectionScope(), value.getField(), 
            new UniqueValueSerializationStrategyImpl.RowOp() {

            @Override
            public void doOp( final ColumnListMutation<String> colMutation ) {
                colMutation.putColumn( colName, 0, IntegerSerializer.get(), null );
            }
        } );
    }


    public MutationBatch delete(UniqueValue uniqueValue) {
        return null;
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

    public UniqueValue load( CollectionScope colScope, Field field ) throws ConnectionException {

        Preconditions.checkNotNull( field, "field is required" );

        ColumnList<String> result;

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

        String colName = result.getColumnByIndex(0).getStringValue();
        String parts[] = colName.split("|");
        Id entityId = new SimpleId( UUID.fromString( parts[0] ), parts[1] );
        UUID entityVersion = UUID.fromString( parts[2] );

        return new UniqueValueImpl( colScope, field, entityId, entityVersion );

    }



    @Override
    public java.util.Collection getColumnFamilies() {
        // create the CF entity data.  We want it reversed b/c we want the most recent version 
        // at the top of the row for fast seeks
        MultiTennantColumnFamilyDefinition cf = new MultiTennantColumnFamilyDefinition( 
                CF_UNIQUE_VALUES, // column family
                BytesType.class.getSimpleName(),
                UTF8Type.class.getSimpleName(),
                IntegerType.class.getSimpleName() );


        return Collections.singleton( cf );
    } 
    
    /**
     * Simple callback to perform puts and deletes with a common row setup code
     */
    private static interface RowOp {
        void doOp( ColumnListMutation<String> colMutation );
    }
}
