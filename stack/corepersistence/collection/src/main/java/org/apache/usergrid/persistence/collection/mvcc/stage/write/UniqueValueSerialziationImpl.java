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

import com.google.common.base.Optional;
import com.google.inject.Inject;
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
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.collection.serialization.impl.CollectionScopedRowKeySerializer;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.field.Field;

/**
 *
 */
public class UniqueValueSerialziationImpl implements UniqueValueSerializationStrategy {

    private static final UniqueValueSerialziationImpl.UniqueValueSerializer SER = 
        new UniqueValueSerialziationImpl.UniqueValueSerializer();

    private static final IdRowCompositeSerializer ID_SER = IdRowCompositeSerializer.get();

    private static final CollectionScopedRowKeySerializer<Id> ROW_KEY_SER = 
        new CollectionScopedRowKeySerializer<Id>( ID_SER );

    private static final MultiTennantColumnFamily<CollectionScope, Id, UUID> CF_UNIQUE_VALUES =
        new MultiTennantColumnFamily<CollectionScope, Id, UUID>( 
                "Unique_Values", ROW_KEY_SER, UUIDSerializer.get() );


    protected final Keyspace keyspace;

    @Inject
    public UniqueValueSerialziationImpl( final Keyspace keyspace ) {
        this.keyspace = keyspace;
    }

    public MutationBatch write( CollectionScope context, MvccEntity entity, String fieldName ) {
        throw new UnsupportedOperationException( "Not supported yet." ); 
    }

    public MvccEntity load( CollectionScope context, MvccEntity entity, String fieldName ) {
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

    private static class UniqueValueSerializer  extends AbstractSerializer<Optional<Entity>> {

        public UniqueValueSerializer() {
        }

        @Override
        public ByteBuffer toByteBuffer( Optional<Entity> t ) {
            throw new UnsupportedOperationException( "Not supported yet." ); 
        }

        @Override
        public Optional<Entity> fromByteBuffer( ByteBuffer bb ) {
            throw new UnsupportedOperationException( "Not supported yet." ); 
        }
    }
    
}
