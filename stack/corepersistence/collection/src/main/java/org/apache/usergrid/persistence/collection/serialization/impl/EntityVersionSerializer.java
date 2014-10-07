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


import java.nio.ByteBuffer;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.model.entity.SimpleId;

import com.google.common.base.Preconditions;
import com.netflix.astyanax.model.CompositeBuilder;
import com.netflix.astyanax.model.Composites;
import com.netflix.astyanax.model.DynamicComposite;
import com.netflix.astyanax.serializers.AbstractSerializer;
import com.netflix.astyanax.serializers.StringSerializer;
import com.netflix.astyanax.serializers.UUIDSerializer;

/**
 * Serialize EntityVersion, entity ID and version, for use a column name in Unique Values Column Family. 
 */
public class EntityVersionSerializer extends AbstractSerializer<EntityVersion> {

    private static final Logger LOG = LoggerFactory.getLogger( EntityVersionSerializer.class );

    @Override
    public ByteBuffer toByteBuffer(final EntityVersion ev) {

        CompositeBuilder builder = Composites.newDynamicCompositeBuilder();

        builder.addTimeUUID( ev.getEntityVersion() );
        builder.addTimeUUID( ev.getEntityId().getUuid() );
        builder.addString( ev.getEntityId().getType() );

        return builder.build();
    }

    @Override
    public EntityVersion fromByteBuffer(final ByteBuffer byteBuffer) {

        // would use Composites.newDynamicCompositeParser(byteBuffer) but it is not implemented

        DynamicComposite composite = DynamicComposite.fromByteBuffer(byteBuffer);
        Preconditions.checkArgument(composite.size() == 3, "Composite should have 3 elements");

        final UUID version      = composite.get( 0, UUIDSerializer.get() );
        final UUID entityId     = composite.get( 1, UUIDSerializer.get() );
        final String entityType = composite.get( 2, StringSerializer.get() );
        
        return new EntityVersion( new SimpleId( entityId, entityType ), version);
    }
    
}
