package org.apache.usergrid.persistence.collection.mvcc.entity;


import java.util.UUID;

import org.junit.Test;

import org.apache.usergrid.persistence.collection.CollectionContext;
import org.apache.usergrid.persistence.collection.CollectionContextImpl;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import static org.junit.Assert.assertEquals;


/** @author tnine */
public class MvccLogEntryImplTest {

    @Test( expected = NullPointerException.class )
    public void contextRequired() {
        new MvccLogEntryImpl( null, UUIDGenerator.newTimeUUID(), UUIDGenerator.newTimeUUID(), Stage.ACTIVE );
    }


    @Test( expected = NullPointerException.class )
    public void entityIdRequired() {
        final CollectionContext context =
                new CollectionContextImpl( UUIDGenerator.newTimeUUID(), UUIDGenerator.newTimeUUID(), "test" );

        new MvccLogEntryImpl( context, null, UUIDGenerator.newTimeUUID(), Stage.ACTIVE );
    }


    @Test( expected = NullPointerException.class )
    public void versionRequired() {
        final CollectionContext context =
                new CollectionContextImpl( UUIDGenerator.newTimeUUID(), UUIDGenerator.newTimeUUID(), "test" );

        new MvccLogEntryImpl( context, UUIDGenerator.newTimeUUID(), null, Stage.ACTIVE );
    }


    @Test( expected = NullPointerException.class )
    public void stageRequired() {
        final CollectionContext context =
                new CollectionContextImpl( UUIDGenerator.newTimeUUID(), UUIDGenerator.newTimeUUID(), "test" );

        new MvccLogEntryImpl( context, UUIDGenerator.newTimeUUID(), UUIDGenerator.newTimeUUID(), null );
    }


    @Test
    public void correctValue() {
        final CollectionContext context =
                new CollectionContextImpl( UUIDGenerator.newTimeUUID(), UUIDGenerator.newTimeUUID(), "test" );

        final UUID entityId = UUIDGenerator.newTimeUUID();
        final UUID version = UUIDGenerator.newTimeUUID();
        final Stage stage = Stage.COMPLETE;


        MvccLogEntry logEntry = new MvccLogEntryImpl( context, entityId, version, stage );

        assertEquals( context, logEntry.getContext() );
        assertEquals( entityId, logEntry.getEntityId() );
        assertEquals( version, logEntry.getVersion() );
        assertEquals( stage, logEntry.getStage() );
    }


    @Test
    public void equals() {
        final CollectionContext context =
                new CollectionContextImpl( UUIDGenerator.newTimeUUID(), UUIDGenerator.newTimeUUID(), "test" );

        final UUID entityId = UUIDGenerator.newTimeUUID();
        final UUID version = UUIDGenerator.newTimeUUID();
        final Stage stage = Stage.COMPLETE;


        MvccLogEntry first = new MvccLogEntryImpl( context, entityId, version, stage );

        MvccLogEntry second = new MvccLogEntryImpl( context, entityId, version, stage );

        assertEquals( first, second );
    }


    @Test
    public void testHashCode() {
        final CollectionContext context =
                new CollectionContextImpl( UUIDGenerator.newTimeUUID(), UUIDGenerator.newTimeUUID(), "test" );

        final UUID entityId = UUIDGenerator.newTimeUUID();
        final UUID version = UUIDGenerator.newTimeUUID();
        final Stage stage = Stage.COMPLETE;


        MvccLogEntry first = new MvccLogEntryImpl( context, entityId, version, stage );

        MvccLogEntry second = new MvccLogEntryImpl( context, entityId, version, stage );

        assertEquals( first.hashCode(), second.hashCode() );
    }
}
