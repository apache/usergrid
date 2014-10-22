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

package org.apache.usergrid.persistence.collection.mvcc.entity.impl;


import java.util.UUID;

import org.junit.Test;

import org.apache.usergrid.persistence.collection.MvccEntity;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.google.common.base.Optional;

import static org.junit.Assert.assertEquals;


/** @author tnine */
public class MvccEntityImplTest {


    @Test(expected = NullPointerException.class)
    public void entityIdRequired() {

        new MvccEntityImpl( null, UUIDGenerator.newTimeUUID(),  MvccEntity.Status.COMPLETE,Optional.of( new Entity() ) );
    }


    @Test(expected = NullPointerException.class)
    public void versionRequired() {

        new MvccEntityImpl( new SimpleId( "test" ), null,  MvccEntity.Status.COMPLETE, Optional.of( new Entity() ) );
    }


    @Test(expected = NullPointerException.class)
    public void entityRequired() {

        new MvccEntityImpl( new SimpleId( "test" ), UUIDGenerator.newTimeUUID(), MvccEntity.Status.COMPLETE, ( Entity ) null );
    }


    @Test(expected = NullPointerException.class)
    public void optionalRequired() {

        new MvccEntityImpl( new SimpleId( "test" ), UUIDGenerator.newTimeUUID(), MvccEntity.Status.COMPLETE, ( Optional ) null );
    }


    @Test(expected = NullPointerException.class)
    public void statusRequired() {

        new MvccEntityImpl( new SimpleId( "test" ), UUIDGenerator.newTimeUUID(), null, ( Entity ) null );
    }


    @Test
    public void correctValueEntity() {

        final SimpleId entityId = new SimpleId( "test" );
        final UUID version = UUIDGenerator.newTimeUUID();
        final Entity entity = new Entity( entityId );

        MvccEntityImpl logEntry = new MvccEntityImpl( entityId, version, MvccEntity.Status.COMPLETE, entity );

        assertEquals( entityId, logEntry.getId() );
        assertEquals( version, logEntry.getVersion() );
        assertEquals( entity, logEntry.getEntity().get() );
    }


    @Test
    public void correctValueOptional() {

        final SimpleId entityId = new SimpleId( "test" );
        final UUID version = UUIDGenerator.newTimeUUID();
        final Entity entity = new Entity( entityId );

        MvccEntityImpl logEntry = new MvccEntityImpl( entityId, version,  MvccEntity.Status.COMPLETE,Optional.of( entity ) );

        assertEquals( entityId, logEntry.getId() );
        assertEquals( version, logEntry.getVersion() );
        assertEquals( entity, logEntry.getEntity().get() );
    }


    @Test
    public void equals() {

        final SimpleId entityId = new SimpleId( "test" );
        final UUID version = UUIDGenerator.newTimeUUID();
        final Entity entity = new Entity( entityId );

        MvccEntityImpl first = new MvccEntityImpl( entityId, version,  MvccEntity.Status.COMPLETE, Optional.of( entity ) );

        MvccEntityImpl second = new MvccEntityImpl( entityId, version, MvccEntity.Status.COMPLETE, Optional.of( entity ) );

        assertEquals( first, second );
    }


    @Test
    public void testHashCode() {

        final SimpleId entityId = new SimpleId( "test" );
        final UUID version = UUIDGenerator.newTimeUUID();
        final Entity entity = new Entity( entityId );

        MvccEntityImpl first = new MvccEntityImpl( entityId, version,  MvccEntity.Status.COMPLETE,Optional.of( entity ) );

        MvccEntityImpl second = new MvccEntityImpl( entityId, version, MvccEntity.Status.COMPLETE, Optional.of( entity ) );

        assertEquals( first.hashCode(), second.hashCode() );
    }
}
