package org.apache.usergrid.persistence.collection.mvcc.entity.impl;


import java.util.UUID;

import org.junit.Test;

import org.apache.usergrid.persistence.collection.CollectionContext;
import org.apache.usergrid.persistence.collection.impl.CollectionContextImpl;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccLogEntry;
import org.apache.usergrid.persistence.collection.mvcc.entity.Stage;
import org.apache.usergrid.persistence.collection.mvcc.entity.impl.MvccLogEntryImpl;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import static org.junit.Assert.assertEquals;


/** @author tnine */
public class MvccLogEntryImplTest {


    @Test( expected = NullPointerException.class )
    public void entityIdRequired() {
        final CollectionContext context =
                new CollectionContextImpl( UUIDGenerator.newTimeUUID(), UUIDGenerator.newTimeUUID(), "test" );

        new MvccLogEntryImpl( null, UUIDGenerator.newTimeUUID(), Stage.ACTIVE );
    }


    @Test( expected = NullPointerException.class )
    public void versionRequired() {
        final CollectionContext context =
                new CollectionContextImpl( UUIDGenerator.newTimeUUID(), UUIDGenerator.newTimeUUID(), "test" );

        new MvccLogEntryImpl( UUIDGenerator.newTimeUUID(), null, Stage.ACTIVE );
    }


    @Test( expected = NullPointerException.class )
    public void stageRequired() {
        final CollectionContext context =
                new CollectionContextImpl( UUIDGenerator.newTimeUUID(), UUIDGenerator.newTimeUUID(), "test" );

        new MvccLogEntryImpl( UUIDGenerator.newTimeUUID(), UUIDGenerator.newTimeUUID(), null );
    }


    @Test
    public void correctValue() {

        final UUID entityId = UUIDGenerator.newTimeUUID();
        final UUID version = UUIDGenerator.newTimeUUID();
        final Stage stage = Stage.COMPLETE;


        MvccLogEntry logEntry = new MvccLogEntryImpl( entityId, version, stage );

        assertEquals( entityId, logEntry.getEntityId() );
        assertEquals( version, logEntry.getVersion() );
        assertEquals( stage, logEntry.getStage() );
    }


    @Test
    public void equals() {

        final UUID entityId = UUIDGenerator.newTimeUUID();
        final UUID version = UUIDGenerator.newTimeUUID();
        final Stage stage = Stage.COMPLETE;


        MvccLogEntry first = new MvccLogEntryImpl( entityId, version, stage );

        MvccLogEntry second = new MvccLogEntryImpl( entityId, version, stage );

        assertEquals( first, second );
    }


    @Test
    public void testHashCode() {

        final UUID entityId = UUIDGenerator.newTimeUUID();
        final UUID version = UUIDGenerator.newTimeUUID();
        final Stage stage = Stage.COMPLETE;


        MvccLogEntry first = new MvccLogEntryImpl( entityId, version, stage );

        MvccLogEntry second = new MvccLogEntryImpl( entityId, version, stage );

        assertEquals( first.hashCode(), second.hashCode() );
    }
}
