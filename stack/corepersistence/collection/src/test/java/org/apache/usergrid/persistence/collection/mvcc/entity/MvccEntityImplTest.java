package org.apache.usergrid.persistence.collection.mvcc.entity;


import java.util.UUID;

import org.junit.Test;

import org.apache.usergrid.persistence.collection.CollectionContext;
import org.apache.usergrid.persistence.collection.CollectionContextImpl;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.google.common.base.Optional;

import static org.junit.Assert.assertEquals;


/** @author tnine */
public class MvccEntityImplTest {

    @Test( expected = NullPointerException.class )
    public void contextRequired() {
        new MvccEntityImpl( null, UUIDGenerator.newTimeUUID(), UUIDGenerator.newTimeUUID(),
                Optional.of( new Entity() ) );
    }


    @Test( expected = NullPointerException.class )
    public void entityIdRequired() {
        final CollectionContext context =
                new CollectionContextImpl( UUIDGenerator.newTimeUUID(), UUIDGenerator.newTimeUUID(), "test" );

        new MvccEntityImpl( context, null, UUIDGenerator.newTimeUUID(), Optional.of( new Entity() ) );
    }


    @Test( expected = NullPointerException.class )
    public void versionRequired() {
        final CollectionContext context =
                new CollectionContextImpl( UUIDGenerator.newTimeUUID(), UUIDGenerator.newTimeUUID(), "test" );

        new MvccEntityImpl( context, UUIDGenerator.newTimeUUID(), null, Optional.of( new Entity() ) );
    }


    @Test( expected = NullPointerException.class )
    public void entityRequired() {
        final CollectionContext context =
                new CollectionContextImpl( UUIDGenerator.newTimeUUID(), UUIDGenerator.newTimeUUID(), "test" );

        new MvccEntityImpl( context, UUIDGenerator.newTimeUUID(), UUIDGenerator.newTimeUUID(), ( Entity ) null );
    }


    @Test( expected = NullPointerException.class )
    public void optionalRequired() {
        final CollectionContext context =
                new CollectionContextImpl( UUIDGenerator.newTimeUUID(), UUIDGenerator.newTimeUUID(), "test" );

        new MvccEntityImpl( context, UUIDGenerator.newTimeUUID(), UUIDGenerator.newTimeUUID(), ( Optional ) null );
    }


    @Test
    public void correctValueEntity() {
        final CollectionContext context =
                new CollectionContextImpl( UUIDGenerator.newTimeUUID(), UUIDGenerator.newTimeUUID(), "test" );

        final UUID entityId = UUIDGenerator.newTimeUUID();
        final UUID version = UUIDGenerator.newTimeUUID();
        final Entity entity = new Entity( entityId, "test" );

        MvccEntityImpl logEntry = new MvccEntityImpl( context, entityId, version, entity );

        assertEquals( context, logEntry.getContext() );
        assertEquals( entityId, logEntry.getUuid() );
        assertEquals( version, logEntry.getVersion() );
        assertEquals( entity, logEntry.getEntity().get() );
    }


    @Test
    public void correctValueOptional() {
        final CollectionContext context =
                new CollectionContextImpl( UUIDGenerator.newTimeUUID(), UUIDGenerator.newTimeUUID(), "test" );

        final UUID entityId = UUIDGenerator.newTimeUUID();
        final UUID version = UUIDGenerator.newTimeUUID();
        final Entity entity = new Entity( entityId, "test" );

        MvccEntityImpl logEntry = new MvccEntityImpl( context, entityId, version, Optional.of( entity ) );

        assertEquals( context, logEntry.getContext() );
        assertEquals( entityId, logEntry.getUuid() );
        assertEquals( version, logEntry.getVersion() );
        assertEquals( entity, logEntry.getEntity().get() );
    }


    @Test
    public void equals() {
        final CollectionContext context =
                new CollectionContextImpl( UUIDGenerator.newTimeUUID(), UUIDGenerator.newTimeUUID(), "test" );

        final UUID entityId = UUIDGenerator.newTimeUUID();
        final UUID version = UUIDGenerator.newTimeUUID();
        final Entity entity = new Entity( entityId, "test" );

        MvccEntityImpl first = new MvccEntityImpl( context, entityId, version, Optional.of( entity ) );

        MvccEntityImpl second = new MvccEntityImpl( context, entityId, version, Optional.of( entity ) );

        assertEquals( first, second );
    }


    @Test
    public void testHashCode() {
        final CollectionContext context =
                new CollectionContextImpl( UUIDGenerator.newTimeUUID(), UUIDGenerator.newTimeUUID(), "test" );

        final UUID entityId = UUIDGenerator.newTimeUUID();
        final UUID version = UUIDGenerator.newTimeUUID();
        final Entity entity = new Entity( entityId, "test" );

        MvccEntityImpl first = new MvccEntityImpl( context, entityId, version, Optional.of( entity ) );

        MvccEntityImpl second = new MvccEntityImpl( context, entityId, version, Optional.of( entity ) );

        assertEquals( first.hashCode(), second.hashCode() );
    }
}
