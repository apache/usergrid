/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one or more
 *  *  contributor license agreements.  The ASF licenses this file to You
 *  * under the Apache License, Version 2.0 (the "License"); you may not
 *  * use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.  For additional information regarding
 *  * copyright in this work, please see the NOTICE file in the top level
 *  * directory of this distribution.
 *
 */

package org.apache.usergrid.persistence.collection.serialization;


import java.util.Arrays;
import java.util.UUID;

import org.junit.Test;

import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.impl.CollectionScopeImpl;
import org.apache.usergrid.persistence.collection.mvcc.MvccEntitySerializationStrategy;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.collection.mvcc.entity.impl.MvccEntityImpl;
import org.apache.usergrid.persistence.collection.serialization.impl.EntityRepairImpl;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.field.StringField;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 * Tests the entity repair task
 */
public class EntityRepairImplTest {


    /**
     * Tests changing from a full version to 2 updates, ensures we have a proper output
     */
    @Test
    public void testSimpleRolling() {

        final SerializationFig serializationFig = mock( SerializationFig.class );

        when( serializationFig.getBufferSize() ).thenReturn( 10 );


        final Id simpleId = new SimpleId( "entity" );

        final Entity v1Entity = new Entity( simpleId );


        v1Entity.setField( new StringField( "field1", "value1" ) );
        v1Entity.setField( new StringField( "field2", "value2" ) );


        final MvccEntityImpl v1 =
                new MvccEntityImpl( simpleId, UUIDGenerator.newTimeUUID(), MvccEntity.Status.COMPLETE, v1Entity );


        final Entity v2Entity = new Entity( simpleId );
        v2Entity.setField( new StringField( "field1", "value1.1" ) );

        final MvccEntityImpl v2 =
                new MvccEntityImpl( simpleId, UUIDGenerator.newTimeUUID(), MvccEntity.Status.PARTIAL, v2Entity );


        final Entity v3Entity = new Entity( simpleId );
        v3Entity.setField( new StringField( "field2", "value1.2" ) );

        final MvccEntityImpl v3 =
                new MvccEntityImpl( simpleId, UUIDGenerator.newTimeUUID(), MvccEntity.Status.PARTIAL, v3Entity );


        final MvccEntitySerializationStrategy mvccEntitySerializationStrategy =
                mock( MvccEntitySerializationStrategy.class );


        final Id applicationId = new SimpleId( "application" );

        final CollectionScope scope = new CollectionScopeImpl( applicationId, applicationId, "users" );

        //mock up returning
        when( mvccEntitySerializationStrategy
                .load( scope, simpleId, v3.getVersion(), serializationFig.getBufferSize() ) )
                .thenReturn( Arrays.<MvccEntity>asList( v3, v2, v1 ).iterator() );


        EntityRepairImpl entityRepair = new EntityRepairImpl( mvccEntitySerializationStrategy, serializationFig );

        final MvccEntity returned = entityRepair.maybeRepair( scope, v3 );

        final UUID version = returned.getVersion();

        assertEquals( "Versions should match", v3.getVersion(), version );



        final Id entityId = returned.getId();

        assertEquals( "Entity Id's match", simpleId, entityId );



        final Entity finalVersion = returned.getEntity().get();

        final Object expectedField1Value = v2.getEntity().get().getField( "field1" ).getValue();

        final Object returnedField1Value = finalVersion.getField( "field1" ).getValue();

        assertEquals( "Same field value", expectedField1Value, returnedField1Value );



        final Object expectedField2Value = v3.getEntity().get().getField( "field2" ).getValue();

        final Object returnedField2Value = finalVersion.getField( "field2" ).getValue();

        assertEquals( "Same field value", expectedField2Value, returnedField2Value );
    }
}
