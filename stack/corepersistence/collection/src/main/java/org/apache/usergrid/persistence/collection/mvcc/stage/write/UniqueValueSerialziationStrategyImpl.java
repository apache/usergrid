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
import com.netflix.astyanax.serializers.AbstractSerializer;
import com.netflix.astyanax.serializers.UUIDSerializer;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.UUID;
import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.astynax.IdRowCompositeSerializer;
import org.apache.usergrid.persistence.collection.astynax.MultiTennantColumnFamily;
import org.apache.usergrid.persistence.collection.astynax.ScopedRowKey;
import org.apache.usergrid.persistence.collection.serialization.impl.CollectionScopedRowKeySerializer;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.field.Field;

/**
 *
 */
public class UniqueValueSerialziationStrategyImpl implements UniqueValueSerializationStrategy {

    private static final UniqueValueSerialziationStrategyImpl.FieldSerializer SER = 
        new UniqueValueSerialziationStrategyImpl.FieldSerializer();

    private static final IdRowCompositeSerializer ID_SER = IdRowCompositeSerializer.get();

    private static final CollectionScopedRowKeySerializer<Id> ROW_KEY_SER = 
        new CollectionScopedRowKeySerializer<Id>( ID_SER );

    private static final MultiTennantColumnFamily<CollectionScope, Id, UUID> CF_UNIQUE_VALUES =
        new MultiTennantColumnFamily<CollectionScope, Id, UUID>( 
            "Unique_Values", ROW_KEY_SER, UUIDSerializer.get() );


    protected final Keyspace keyspace;
    protected final int timeout;

    @Inject
    public UniqueValueSerialziationStrategyImpl( final Keyspace keyspace, final int timeout ) {
        this.keyspace = keyspace;
        this.timeout = timeout;
    }

    public MutationBatch write( CollectionScope collectionScope, UniqueValue value, String fieldName ) {

        Preconditions.checkNotNull( collectionScope, "collectionScope is required" );
        Preconditions.checkNotNull( value, "value is required" );

        final UUID colName = value.getEntityVersion();
        final Field field = value.getField();

        return doWrite( collectionScope, value.getEntityId(), value.getEntityVersion(), 
                new UniqueValueSerialziationStrategyImpl.RowOp() {

            @Override
            public void doOp( final ColumnListMutation<UUID> colMutation ) {
                colMutation.putColumn( colName, field, SER, null );
            }
        } );
    }

    public UniqueValue load( CollectionScope context, UniqueValue entity, String fieldName ) {
        throw new UnsupportedOperationException( "Not supported yet." );
    }

    public List<Field> load( CollectionScope context, Id entityId, String fieldName ) {
        throw new UnsupportedOperationException( "Not supported yet." ); 
    }

    public MutationBatch clear( CollectionScope context, Id entityId, String fieldName ) {
        throw new UnsupportedOperationException( "Not supported yet." ); 
    }

    public MutationBatch delete( CollectionScope context, Id entityId, String fieldName ) {
        throw new UnsupportedOperationException( "Not supported yet." ); 
    }

    private static class FieldSerializer  extends AbstractSerializer<Field> {

        public FieldSerializer() {
        }

        @Override
        public ByteBuffer toByteBuffer( Field field ) {
            throw new UnsupportedOperationException( "Not supported yet." ); 
        }

        @Override
        public Field fromByteBuffer(ByteBuffer bb) {
            throw new UnsupportedOperationException("Not supported yet."); 
        }
    }

    /**
     * Simple callback to perform puts and deletes with a common row setup code
     */
    private static interface RowOp {

        /**
         * The operation to perform on the row
         */
        void doOp( ColumnListMutation<UUID> colMutation );
    }


    /**
     * Do the column update or delete for the given column and row key
     * @param context We need to use this when getting the keyspace
     */
    private MutationBatch doWrite( CollectionScope context, Id entityId, UUID version, RowOp op ) {

        final MutationBatch batch = keyspace.prepareMutationBatch();

        op.doOp( batch.withRow( CF_UNIQUE_VALUES, 
            ScopedRowKey.fromKey( context, entityId ) ).setTimestamp( version.timestamp() ) );

        return batch;
    }
    
}
