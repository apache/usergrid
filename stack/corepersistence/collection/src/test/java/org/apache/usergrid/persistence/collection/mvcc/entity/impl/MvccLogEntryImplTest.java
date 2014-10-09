package org.apache.usergrid.persistence.collection.mvcc.entity.impl;


import java.util.UUID;

import org.junit.Test;

import org.apache.usergrid.persistence.collection.MvccLogEntry;
import org.apache.usergrid.persistence.collection.mvcc.entity.Stage;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import static org.junit.Assert.assertEquals;


/** @author tnine */
public class MvccLogEntryImplTest {


    @Test(expected = NullPointerException.class)
    public void entityIdRequired() {
        new MvccLogEntryImpl( null, UUIDGenerator.newTimeUUID(), Stage.ACTIVE, MvccLogEntry.State.COMPLETE );
    }


    @Test(expected = NullPointerException.class)
    public void versionRequired() {
        new MvccLogEntryImpl( new SimpleId( "test" ), null, Stage.ACTIVE, MvccLogEntry.State.COMPLETE );
    }


    @Test(expected = NullPointerException.class)
    public void stageRequired() {
        new MvccLogEntryImpl( new SimpleId( "test" ), UUIDGenerator.newTimeUUID(), null, MvccLogEntry.State.COMPLETE );
    }


    @Test
    public void correctValue() {

        final SimpleId entityId = new SimpleId( "test" );
        final UUID version = UUIDGenerator.newTimeUUID();
        final Stage stage = Stage.COMPLETE;
        final MvccLogEntry.State state = MvccLogEntry.State.COMPLETE;

        MvccLogEntry logEntry = new MvccLogEntryImpl( entityId, version, stage, state );

        assertEquals( entityId, logEntry.getEntityId() );
        assertEquals( version, logEntry.getVersion() );
        assertEquals( stage, logEntry.getStage() );
    }


    @Test
    public void equals() {

        final SimpleId entityId = new SimpleId( "test" );
        final UUID version = UUIDGenerator.newTimeUUID();
        final Stage stage = Stage.COMPLETE;
        final MvccLogEntry.State state = MvccLogEntry.State.COMPLETE;


        MvccLogEntry first = new MvccLogEntryImpl( entityId, version, stage, state );

        MvccLogEntry second = new MvccLogEntryImpl( entityId, version, stage, state );

        assertEquals( first, second );
    }


    @Test
    public void testHashCode() {

        final SimpleId entityId = new SimpleId( "test" );
        final UUID version = UUIDGenerator.newTimeUUID();
        final Stage stage = Stage.COMPLETE;
        final MvccLogEntry.State state = MvccLogEntry.State.COMPLETE;


        MvccLogEntry first = new MvccLogEntryImpl( entityId, version, stage, state );

        MvccLogEntry second = new MvccLogEntryImpl( entityId, version, stage, state );

        assertEquals( first.hashCode(), second.hashCode() );
    }
}
