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
package org.apache.usergrid.persistence.collection.mvcc.stage.write.uniquevalues;

import com.google.common.base.Preconditions;
import com.netflix.astyanax.model.DynamicComposite;
import com.netflix.astyanax.serializers.AbstractSerializer;
import com.netflix.astyanax.serializers.UUIDSerializer;
import java.nio.ByteBuffer;
import java.util.UUID;
import org.apache.usergrid.persistence.astyanax.IdColDynamicCompositeSerializer;
import org.apache.usergrid.persistence.model.entity.Id;

/**
 * Serialize EntityVersion, entity ID and version, for use a column name in Unique Values Column Family. 
 */
class EntityVersionSerializer extends AbstractSerializer<EntityVersion> {

    private static final IdColDynamicCompositeSerializer ID_COL_SERIALIZER = 
            IdColDynamicCompositeSerializer.get();

    private static final UUIDSerializer UUID_SERIALIZER = UUIDSerializer.get();

    @Override
    public ByteBuffer toByteBuffer(final EntityVersion ev) {

        DynamicComposite composite = new DynamicComposite();
        
        ID_COL_SERIALIZER.toComposite(composite, ev.getEntityId());
        composite.addComponent(ev.getEntityVersion(), UUID_SERIALIZER);
        
        return composite.serialize();
    }

    @Override
    public EntityVersion fromByteBuffer(final ByteBuffer byteBuffer) {

        DynamicComposite composite = DynamicComposite.fromByteBuffer(byteBuffer);
        Preconditions.checkArgument(composite.size() == 2, "Composite should have 2 elements");

        final Id id = ID_COL_SERIALIZER.fromComposite(composite, 0);
        final UUID version = composite.get(1, UUID_SERIALIZER);
        
        return new EntityVersion(id, version);
    }
    
}
