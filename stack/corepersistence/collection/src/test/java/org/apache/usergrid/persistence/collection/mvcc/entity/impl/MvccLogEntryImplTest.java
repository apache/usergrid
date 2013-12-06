package org.apache.usergrid.persistence.collection.mvcc.entity.impl;


import java.util.UUID;

import org.junit.Test;

import org.apache.usergrid.persistence.collection.mvcc.entity.MvccLogEntry;
import org.apache.usergrid.persistence.collection.mvcc.entity.Stage;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import static org.junit.Assert.assertEquals;


/** @author tnine */
public class MvccLogEntryImplTest {


    @Test(expected = NullPointerException.class)
    public void entityIdRequired() {
        new MvccLogEntryImpl( null, UUIDGenerator.newTimeUUID(), Stage.ACTIVE );
    }


    @Test(expected = NullPointerException.class)
    public void versionRequired() {
        new MvccLogEntryImpl( new SimpleId( "test" ), null, Stage.ACTIVE );
    }


    @Test(expected = NullPointerException.class)
    public void stageRequired() {
        new MvccLogEntryImpl( new SimpleId( "test" ), UUIDGenerator.newTimeUUID(), null );
    }


    @Test
    public void correctValue() {

        final SimpleId entityId = new SimpleId( "test" );
        final UUID version = UUIDGenerator.newTimeUUID();
        final Stage stage = Stage.COMPLETE;


        MvccLogEntry logEntry = new MvccLogEntryImpl( entityId, version, stage );

        assertEquals( entityId, logEntry.getEntityId() );
        assertEquals( version, logEntry.getVersion() );
        assertEquals( stage, logEntry.getStage() );
    }


    @Test
    public void equals() {

        final SimpleId entityId = new SimpleId( "test" );
        final UUID version = UUIDGenerator.newTimeUUID();
        final Stage stage = Stage.COMPLETE;


        MvccLogEntry first = new MvccLogEntryImpl( entityId, version, stage );

        MvccLogEntry second = new MvccLogEntryImpl( entityId, version, stage );

        assertEquals( first, second );
    }


    @Test
    public void testHashCode() {

        final SimpleId entityId = new SimpleId( "test" );
        final UUID version = UUIDGenerator.newTimeUUID();
        final Stage stage = Stage.COMPLETE;


        MvccLogEntry first = new MvccLogEntryImpl( entityId, version, stage );

        MvccLogEntry second = new MvccLogEntryImpl( entityId, version, stage );

        assertEquals( first.hashCode(), second.hashCode() );
    }
}
