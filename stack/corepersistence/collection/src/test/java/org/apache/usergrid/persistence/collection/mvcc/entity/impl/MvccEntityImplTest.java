package org.apache.usergrid.persistence.collection.mvcc.entity.impl;


import java.util.UUID;

import org.junit.Test;

import org.apache.usergrid.persistence.collection.mvcc.entity.impl.MvccEntityImpl;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.google.common.base.Optional;

import static org.junit.Assert.assertEquals;


/** @author tnine */
public class MvccEntityImplTest {


    @Test( expected = NullPointerException.class )
    public void entityIdRequired() {

        new MvccEntityImpl( null, UUIDGenerator.newTimeUUID(), Optional.of( new Entity() ) );
    }


    @Test( expected = NullPointerException.class )
    public void versionRequired() {

        new MvccEntityImpl( UUIDGenerator.newTimeUUID(), null, Optional.of( new Entity() ) );
    }


    @Test( expected = NullPointerException.class )
    public void entityRequired() {

        new MvccEntityImpl( UUIDGenerator.newTimeUUID(), UUIDGenerator.newTimeUUID(), ( Entity ) null );
    }


    @Test( expected = NullPointerException.class )
    public void optionalRequired() {

        new MvccEntityImpl( UUIDGenerator.newTimeUUID(), UUIDGenerator.newTimeUUID(), ( Optional ) null );
    }


    @Test
    public void correctValueEntity() {

        final UUID entityId = UUIDGenerator.newTimeUUID();
        final UUID version = UUIDGenerator.newTimeUUID();
        final Entity entity = new Entity( entityId, "test" );

        MvccEntityImpl logEntry = new MvccEntityImpl( entityId, version, entity );

        assertEquals( entityId, logEntry.getUuid() );
        assertEquals( version, logEntry.getVersion() );
        assertEquals( entity, logEntry.getEntity().get() );
    }


    @Test
    public void correctValueOptional() {

        final UUID entityId = UUIDGenerator.newTimeUUID();
        final UUID version = UUIDGenerator.newTimeUUID();
        final Entity entity = new Entity( entityId, "test" );

        MvccEntityImpl logEntry = new MvccEntityImpl( entityId, version, Optional.of( entity ) );

        assertEquals( entityId, logEntry.getUuid() );
        assertEquals( version, logEntry.getVersion() );
        assertEquals( entity, logEntry.getEntity().get() );
    }


    @Test
    public void equals() {

        final UUID entityId = UUIDGenerator.newTimeUUID();
        final UUID version = UUIDGenerator.newTimeUUID();
        final Entity entity = new Entity( entityId, "test" );

        MvccEntityImpl first = new MvccEntityImpl( entityId, version, Optional.of( entity ) );

        MvccEntityImpl second = new MvccEntityImpl( entityId, version, Optional.of( entity ) );

        assertEquals( first, second );
    }


    @Test
    public void testHashCode() {

        final UUID entityId = UUIDGenerator.newTimeUUID();
        final UUID version = UUIDGenerator.newTimeUUID();
        final Entity entity = new Entity( entityId, "test" );

        MvccEntityImpl first = new MvccEntityImpl( entityId, version, Optional.of( entity ) );

        MvccEntityImpl second = new MvccEntityImpl( entityId, version, Optional.of( entity ) );

        assertEquals( first.hashCode(), second.hashCode() );
    }
}
