package org.apache.usergrid.persistence.collection.mvcc.stage;


import java.util.UUID;

import org.junit.Test;

import org.apache.usergrid.persistence.collection.CollectionContext;
import org.apache.usergrid.persistence.collection.CollectionContextImpl;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntityImpl;
import org.apache.usergrid.persistence.collection.serialization.MvccLogEntrySerializationStrategy;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import static org.mockito.Mockito.mock;


/** @author tnine */
public class StartTest {

    @Test
    public void testStartStage(){

        final MvccLogEntrySerializationStrategy logStrategy = mock(MvccLogEntrySerializationStrategy.class);

        Start start = new Start( logStrategy );

        //set up the data

        final UUID applicationId = UUIDGenerator.newTimeUUID();

        final UUID ownerId = UUIDGenerator.newTimeUUID();

        final UUID entityId = UUIDGenerator.newTimeUUID();

        final UUID version = UUIDGenerator.newTimeUUID();

        final String name = "tests";


        CollectionContextImpl collection = new CollectionContextImpl( applicationId, ownerId, name );


        Entity entity = new Entity();

        MvccEntityImpl mvccEntity = new MvccEntityImpl(collection, entityId, version, entity  );


    }

}
