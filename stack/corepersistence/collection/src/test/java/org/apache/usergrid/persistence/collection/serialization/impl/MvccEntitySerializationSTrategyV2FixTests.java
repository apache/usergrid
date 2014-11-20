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


import java.util.Iterator;
import java.util.UUID;

import org.junit.Test;

import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.MvccEntity;
import org.apache.usergrid.persistence.collection.impl.CollectionScopeImpl;
import org.apache.usergrid.persistence.collection.mvcc.entity.impl.MvccEntityImpl;
import org.apache.usergrid.persistence.collection.util.EntityHelper;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public abstract class MvccEntitySerializationSTrategyV2FixTests extends MvccEntitySerializationStrategyImplTest {


    /**
     * Tests an entity with more than  65535 bytes worth of data is successfully stored and retrieved
     */
    @Test
    public void largeEntityWriteRead() throws ConnectionException {
        final int setSize = 65535 * 2;

        final Entity entity = EntityHelper.generateEntity( setSize );

        //now we have one massive, entity, save it and retrieve it.
        CollectionScope context =
                new CollectionScopeImpl( new SimpleId( "organization" ), new SimpleId( "test" ), "test" );


        final Id simpleId = new SimpleId( "test" );
        final UUID version = UUIDGenerator.newTimeUUID();
        final MvccEntity.Status status = MvccEntity.Status.COMPLETE;

        final MvccEntity mvccEntity = new MvccEntityImpl( simpleId, version, status, entity );


        getMvccEntitySerializationStrategy().write( context, mvccEntity ).execute();

        //now load it
        final Iterator<MvccEntity> loaded =
                getMvccEntitySerializationStrategy().loadHistory( context, entity.getId(), version, 100 );

        assertTrue( loaded.hasNext() );

        final MvccEntity loadedEntity = loaded.next();

        assertEquals( "The loaded entity should match the stored entity", mvccEntity, loadedEntity );

        EntityHelper.verifySame( entity, loadedEntity.getEntity().get() );
    }
}
